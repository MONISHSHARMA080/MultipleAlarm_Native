import { onSchedule } from "firebase-functions/v2/scheduler";
import { defineSecret } from "firebase-functions/params";
import * as logger from "firebase-functions/logger";
import { COHORT_TARGETS } from "./config";
import { fetchTokensForCohort } from "./fetchTokensForCohort";
// import * as a from "./fetchTokensForCohort";
import { sendPushToTokens } from "./sendPush";

// Set this once via:
//   firebase functions:secrets:set POSTHOG_API_KEY
// Use a PostHog Personal API Key scoped to cohort:read + person:read ONLY.
const POSTHOG_API_KEY = defineSecret("POSTHOG_API_KEY");

export const sendCohortPushNotifications = onSchedule(
  {
    schedule: "0 9 * * *",
    timeZone: "Asia/Kolkata",
    region: "asia-south1", // co-locate with your India-majority audience
    secrets: [POSTHOG_API_KEY],
    retryCount: 1,
    timeoutSeconds: 300,
  },
  async () => {
    if (COHORT_TARGETS.length === 0) {
      logger.warn("COHORT_TARGETS is empty — nothing to do. Fill in config.ts.");
      return;
    }

    const apiKey = POSTHOG_API_KEY.value();

    for (const target of COHORT_TARGETS) {
      try {
        logger.info(`Starting cohort "${target.label}" (id ${target.cohortId})`);

        const tokenSet = await fetchTokensForCohort(target.cohortId, apiKey);
        const tokens = Array.from(tokenSet);

        if (tokens.length === 0) {
          logger.info(`Cohort "${target.label}" has no usable tokens, skipping send`);
          continue;
        }

        const result = await sendPushToTokens(tokens, {
          title: target.title,
          body: target.body,
          data: target.data,
        });

        logger.info(
          `Finished cohort "${target.label}": ${result.successCount} sent, ` +
          `${result.failureCount} failed, ${result.staleTokenCount} newly stale`
        );
      } catch (err) {
        // One cohort failing (e.g. a transient PostHog API error) should
        // not stop the others from running.
        logger.error(`Cohort "${target.label}" (id ${target.cohortId}) failed`, err);
      }
    }
  }
);
