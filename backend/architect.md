# Cohort Push Notification Cron Job

## What this is

A scheduled Firebase Cloud Function that runs daily at 9:00 AM IST. For each
PostHog cohort you've hand-picked in `config.ts`, it fetches the people in
that cohort, pulls their `fcm_token` person property, and sends them a push
notification via FCM. No dedicated backend server — PostHog is the source of
truth for "who", Firebase Admin SDK is the mechanism for "send".

## Why fcm_token as the identifier

PostHog's own identifiers (`distinct_id`, `person_id`) only make sense
inside PostHog — they identify a person/event stream, not a device. FCM
needs the actual device registration token to deliver a push. So the app
client already writes `fcm_token` onto the person as a property, and this
job just reads that property back off. This is the join key between
"PostHog knows who's in this cohort" and "FCM knows how to reach that
device."

## Architecture

```
Cloud Scheduler (cron: 0 9 * * *, Asia/Kolkata)
        |
        v
Firebase Cloud Function: sendCohortPushNotifications (region: asia-south1)
        |
        |  for each cohort in COHORT_TARGETS (config.ts):
        |
        v
  PostHog Persons API  ---->  paginate through cohort, collect
  GET /api/projects/:id/          fcm_token off each person
  persons/?cohort=<id>            (fetchCohortTokens.ts)
        |
        v
  Dedupe tokens within this cohort (Set)
        |
        v
  Chunk into batches of 500 (FCM hard limit)
        |
        v
  Firebase Admin SDK: messaging().sendEachForMulticast()
  (sendPush.ts)
        |
        v
  Per-token result: success / transient failure / permanently dead
  Dead tokens -> logger.warn only. Nothing stored, nothing else touched.
```

## Components

| File | Responsibility |
|---|---|
| `config.ts` | The only file you edit day-to-day. Cohort IDs to target, per-cohort title/body/data, PostHog project ID, FCM/pagination constants. |
| `fetchCohortTokens.ts` | Talks to PostHog's Persons API for one cohort ID. Paginates fully. Extracts and dedupes `fcm_token` values. Defensive against missing/null/array property shapes. |
| `sendPush.ts` | Chunks a token list into FCM-legal batch sizes, calls `sendEachForMulticast`, logs dead tokens (`messaging/registration-token-not-registered` etc.) as warnings, logs transient failures separately so they're retried next run instead of discarded. |
| `index.ts` | The actual `onSchedule` entrypoint. Loops over `COHORT_TARGETS`, wraps each cohort in try/catch so one cohort failing doesn't stop the others, calls the fetch and send steps, logs a summary per cohort. |

## Explicit non-goals (things this deliberately does NOT do)

- Does not write anything to Firestore or any other datastore. Fully
  stateless between runs.
- Does not mutate PostHog. Never writes back to `fcm_token` or any person
  property. Read-only against PostHog.
- Does not dedupe a person who belongs to two *different* target cohorts
  in the same run — they'll get two separate notifications, once per
  cohort, since each cohort can have different copy. If you ever want a
  single combined send instead, that's a deliberate change to make later,
  not a default.
- Does not clean up dead tokens anywhere. It only logs them. Cleaning
  `fcm_token` off a PostHog person (if you ever want that) is a separate
  job with its own risk profile — don't fold it into this one.

## Known limitations to keep in mind

- PostHog dynamic cohort membership is recalculated on PostHog's own
  schedule, not live — expect membership to lag real user behavior by
  minutes to a few hours.
- No retry/backoff beyond what `retryCount: 1` on the scheduled function
  gives you (one automatic re-invocation if the whole function throws).
- A cohort with 0 members (e.g. `New user activation` at time of writing)
  will just silently send nothing — that's expected, not a bug.

---

## Deployment

### Secrets — one-time setup, not part of every deploy

The PostHog API key is stored in **Google Secret Manager**, tied to your
Firebase/GCP project — not in GitHub Actions secrets, not in `.env`, not in
code. Set it once from your local machine (needs `firebase-tools` installed
and you logged into the right project):

```bash
firebase functions:secrets:set POSTHOG_API_KEY
# paste the PostHog Personal API key when prompted (cohort:read + person:read scope only)
```

This only needs to be re-run if you rotate the key. It is **not** something
your GitHub Actions workflow needs to know about or touch — the deployed
Cloud Function reads it directly from Secret Manager at runtime via
`defineSecret`.

### Where functions live vs. where the app lives

Since your Firebase project sits at the root of your repo alongside the
Android app's `app/src/main/java/...` tree, the functions code should be
under something like `<repo-root>/functions/` (Firebase's default expected
layout — `firebase.json` at repo root points `functions.source` at that
folder). This is a **completely separate deploy target** from your Android
build — different toolchain (Node/TypeScript vs. Gradle/Kotlin), different
artifact, different destination (Google Cloud vs. Google Play).

### Should this deploy from the same GitHub Actions workflow as the app?

**No — keep them as two separate workflows.** They have nothing in common:
your `release.yml` builds an APK/AAB and is presumably triggered by
version tags or a release branch. Functions deploy should be triggered by
changes to the `functions/` directory, independent of app release cadence.
Mixing them means an unrelated app release could accidentally redeploy
functions, or vice versa.

Add a second workflow, e.g. `.github/workflows/deploy-functions.yml`:

```yaml
name: Deploy Firebase Functions

on:
  push:
    branches: [main]
    paths:
      - "functions/**"

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: 20
      - run: npm ci
        working-directory: functions
      - run: npm run build
        working-directory: functions
      - uses: w9jds/firebase-action@master
        with:
          args: deploy --only functions
        env:
          GCP_SA_KEY: ${{ secrets.FIREBASE_SERVICE_ACCOUNT }}
          PROJECT_ID: your-firebase-project-id
```

For `FIREBASE_SERVICE_ACCOUNT`: in the Firebase console, go to Project
Settings → Service Accounts → Generate new private key. Paste the entire
JSON contents as a GitHub Actions repo secret with that name. This is a
long-lived credential — scope it to only what's needed if you can, and
treat that GitHub secret as sensitive as a production database password.

(A more modern alternative is Workload Identity Federation, which avoids
storing a long-lived JSON key in GitHub at all — worth it if you want to
harden this later, but the service-account-key approach above is the
standard path and fine for a solo project to start with.)

### Testing before you trust the schedule

After the first deploy, don't wait for 9 AM tomorrow to find out if it
works. Trigger it manually from the Google Cloud Console: Cloud Scheduler →
find the job (named after your function) → "Run now". Then check Cloud
Functions logs for the per-cohort summary lines.
