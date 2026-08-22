import * as logger from "firebase-functions/logger";
import {
  POSTHOG_BASE_URL,
  POSTHOG_PROJECT_ID,
  POSTHOG_PAGE_SIZE,
} from "./config";

interface PostHogPerson {
  id: string;
  distinct_ids: string[];
  properties: Record<string, unknown>;
}

interface PostHogPersonsResponse {
  next: string | null;
  results: PostHogPerson[];
}

/**
 * Fetches every person in a given cohort and returns the distinct set of
 * valid fcm_token values found on them. Paginates through the full cohort —
 * do not assume it fits in one page.
 */
export async function fetchTokensForCohort(
  cohortId: number,
  apiKey: string
): Promise<Set<string>> {
  const tokens = new Set<string>();

  let url: string | null =
    `${POSTHOG_BASE_URL}/api/projects/${POSTHOG_PROJECT_ID}/persons/` +
    `?cohort=${cohortId}&limit=${POSTHOG_PAGE_SIZE}`;

  let pageCount = 0;

  while (url) {
    pageCount += 1;
    const response = await fetch(url, {
      headers: { Authorization: `Bearer ${apiKey}` },
    });

    if (!response.ok) {
      const body = await response.text();
      throw new Error(
        `PostHog persons fetch failed for cohort ${cohortId} ` +
          `(status ${response.status}): ${body}`
      );
    }

    const data = (await response.json()) as PostHogPersonsResponse;

    for (const person of data.results) {
      const raw = person.properties?.["fcm_token"];

      // Be defensive — property could theoretically be missing, null,
      // an empty string, or (if a client ever double-sets it) an array.
      if (typeof raw === "string" && raw.trim().length > 0) {
        tokens.add(raw.trim());
      } else if (Array.isArray(raw)) {
        for (const t of raw) {
          if (typeof t === "string" && t.trim().length > 0) {
            tokens.add(t.trim());
          }
        }
      }
    }

    url = data.next;
  }

  logger.info(
    `Cohort ${cohortId}: scanned ${pageCount} page(s), ` +
      `found ${tokens.size} usable fcm_token(s)`
  );

  return tokens;
}
