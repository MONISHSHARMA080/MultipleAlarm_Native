import { getMessaging } from "firebase-admin/messaging";
import * as logger from "firebase-functions/logger";
import { FCM_MULTICAST_CHUNK_SIZE } from "./config";

// Error codes that mean "this token will never work again." These are only
// logged — nothing is stored or cleaned up automatically. If you want
// actual cleanup later, that's a separate decision to make deliberately.
const PERMANENTLY_DEAD_CODES = new Set([
  "messaging/registration-token-not-registered",
  "messaging/invalid-registration-token",
  "messaging/invalid-argument",
]);

interface SendResult {
  successCount: number;
  failureCount: number;
  staleTokenCount: number;
}

// eslint-disable-next-line valid-jsdoc
/**
 * Sends the same title/body/data payload to every token, chunking into
 * batches of FCM_MULTICAST_CHUNK_SIZE (FCM's hard per-call limit).
 * Dead tokens are logged and otherwise ignored — no storage, no side effects.
 */
export async function sendPushToTokens(
  tokens: string[],
  payload: { title: string; body: string; data?: Record<string, string> }
): Promise<SendResult> {
  const messaging = getMessaging();
  const total: SendResult = {successCount: 0, failureCount: 0, staleTokenCount: 0};

  const chunks: string[][] = [];
  for (let i = 0; i < tokens.length; i += FCM_MULTICAST_CHUNK_SIZE) {
    chunks.push(tokens.slice(i, i + FCM_MULTICAST_CHUNK_SIZE));
  }

  for (const [index, chunk] of chunks.entries()) {
    const response = await messaging.sendEachForMulticast({
      tokens: chunk,
      notification: { title: payload.title, body: payload.body },
      data: payload.data,
    });

    total.successCount += response.successCount;
    total.failureCount += response.failureCount;

    response.responses.forEach((r, i) => {
      if (!r.success && r.error) {
        const code = r.error.code;
        if (PERMANENTLY_DEAD_CODES.has(code)) {
          total.staleTokenCount += 1;
          logger.warn(`Stale token, skipping: ${chunk[i]} (${code})`);
        } else {
          logger.warn(`Transient FCM failure (${code}) for a token, will retry next run`);
        }
      }
    });

    logger.info(
      `Chunk ${index + 1}/${chunks.length}: ` +
        `${response.successCount} sent, ${response.failureCount} failed`
    );
  }

  return total;
}
