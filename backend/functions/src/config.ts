/**
 * EDIT THIS FILE ONLY when you want to change who gets targeted or what
 * the notification says. Nothing else in this folder should need touching
 * for day-to-day changes.
 */

export interface CohortPushConfig {
  /** PostHog cohort ID — get this from the cohort URL, e.g.../cohorts/505078*/
  cohortId: number;
  /** Just for your own logs/readability, doesn't affect behavior */
  label: string;
  title: string;
  body: string;
  /** Optional deep link / data payload delivered with the notification */
  data?: Record<string, string>;
}

// TODO: fill in the cohort IDs you actually want to target.
// From your project right now, the candidates were:
//   505206  New -> active user (not stable)      — 2 people
//   505078  Active User -> Dropped off             — 45 people
//   505007  New user activation                    — 0 people (currently empty)
//   504883  Weekly active user                      — 60 people
export const COHORT_TARGETS: CohortPushConfig[] = [
  {
    cohortId: 505030,
    label: "me",
    title: "BOOOOOMMMMM", // TODO: real copy
    body: "Hi from firebase", // TODO: real copy
    // data: { screen: "home" },
  },
  // TODO: add more { cohortId, label, title, body } entries for the other
  // cohorts you decide to target. Any cohort NOT listed here is never
  // fetched or messaged — this array is the single source of truth.
];

export const POSTHOG_PROJECT_ID = "330205";
export const POSTHOG_BASE_URL = "https://us.posthog.com";

// FCM hard limit per sendEachForMulticast call — do not change.
export const FCM_MULTICAST_CHUNK_SIZE = 500;

// PostHog Persons API page size per request.
export const POSTHOG_PAGE_SIZE = 400;
