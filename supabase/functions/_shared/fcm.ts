// Firebase Cloud Messaging (HTTP v1) with the service account. The client never sends pushes (RF-B08).
import { accessToken, serviceAccount } from "./firebase.ts";
import { fsCommit, fsQueryIn, fsString, fsWrite } from "./firestore.ts";

export type PushMessage = {
  /** CHECK_IN, ABSENCE, JUSTIFICATION, EMERGENCY or ADMIN: the app maps it to a notification channel. */
  type: string;
  title: string;
  body: string;
  /** Extra values for the app (all strings). */
  data?: Record<string, string>;
};

export type DeviceToken = { deviceId: string; uid: string; token: string };

export type SendResult = "sent" | "invalid_token" | "failed";

/** Active devices with a token for the given users (a signed-out phone keeps an empty token). */
export async function findDeviceTokens(uids: string[]): Promise<DeviceToken[]> {
  if (uids.length === 0) return [];
  const docs = await fsQueryIn("devices", "userUid", uids);
  const tokens: DeviceToken[] = [];
  for (const d of docs) {
    const token = fsString(d.fields, "fcmToken");
    const uid = fsString(d.fields, "userUid");
    if (token && uid && fsString(d.fields, "status") === "ACTIVE") tokens.push({ deviceId: d.id, uid, token });
  }
  return tokens;
}

/** Sends one data message with high priority; the app builds the notification from the data. */
export async function sendToToken(token: string, msg: PushMessage): Promise<SendResult> {
  const projectId = serviceAccount().project_id;
  const res = await fetch(`https://fcm.googleapis.com/v1/projects/${projectId}/messages:send`, {
    method: "POST",
    headers: { Authorization: `Bearer ${await accessToken()}`, "Content-Type": "application/json" },
    body: JSON.stringify({
      message: {
        token,
        data: { ...(msg.data ?? {}), type: msg.type, title: msg.title, body: msg.body },
        android: { priority: "HIGH", ttl: "3600s" },
      },
    }),
  });
  if (res.ok) return "sent";
  const text = await res.text();
  // The token belongs to an uninstalled app or an expired registration.
  if (res.status === 404 || text.includes("UNREGISTERED")) return "invalid_token";
  console.error(`fcm send failed: ${res.status} ${text}`);
  return "failed";
}

/** An invalid token is blanked so it is not used again. */
export async function clearToken(deviceId: string): Promise<void> {
  try {
    await fsCommit([fsWrite(`devices/${deviceId}`, { fcmToken: "" }, { mask: ["fcmToken"], exists: true })]);
  } catch (e) {
    console.error("could not clear token:", e);
  }
}

/** Sends the message to every active device of the given users. */
export async function pushToUsers(uids: string[], msg: PushMessage): Promise<{ sent: number; invalid: number; failed: number }> {
  const tokens = await findDeviceTokens(uids);
  const outcomes = await Promise.all(tokens.map(async (t) => {
    const result = await sendToToken(t.token, msg);
    if (result === "invalid_token") await clearToken(t.deviceId);
    return result;
  }));
  return {
    sent: outcomes.filter((o) => o === "sent").length,
    invalid: outcomes.filter((o) => o === "invalid_token").length,
    failed: outcomes.filter((o) => o === "failed").length,
  };
}

/** In-app copy of the notification (notifications/{id}); the deterministic id makes it idempotent. */
export function notificationWrite(
  id: string,
  n: { recipientUid: string; type: string; title: string; body: string; studentId?: string },
) {
  return fsWrite(`notifications/${id}`, { ...n, createdAt: new Date(), read: false }, { exists: false });
}
