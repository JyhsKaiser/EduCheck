// send-push (RF-B08): an ADMIN sends a test push to every active device of a user.
// The real notifications are sent by the backend functions (dispatch-notifications and the ones of later sprints).
//
// POST /send-push   Authorization: Bearer <Firebase ID token of an ADMIN>
// Body: { "uid": "<recipient uid>", "title"?: "...", "body"?: "..." }
import { verifyCaller } from "../_shared/firebase.ts";
import { pushToUsers } from "../_shared/fcm.ts";
import { errorResponse, HttpError, json } from "../_shared/http.ts";

Deno.serve(async (req) => {
  try {
    if (req.method !== "POST") throw new HttpError(405, "method_not_allowed");
    const caller = await verifyCaller(req);
    if (caller.role !== "ADMIN") throw new HttpError(403, "forbidden");

    let body: { uid?: unknown; title?: unknown; body?: unknown };
    try {
      body = await req.json();
    } catch (_e) {
      throw new HttpError(400, "invalid_json");
    }
    if (typeof body.uid !== "string" || !/^[A-Za-z0-9:_-]{1,128}$/.test(body.uid)) throw new HttpError(400, "invalid_uid");
    const title = typeof body.title === "string" && body.title.trim() ? body.title.trim().slice(0, 100) : "Prueba de notificación";
    const text = typeof body.body === "string" && body.body.trim() ? body.body.trim().slice(0, 300) : "Las notificaciones de EduCheck funcionan.";

    const result = await pushToUsers([body.uid], { type: "ADMIN", title, body: text });
    if (result.sent + result.invalid + result.failed === 0) throw new HttpError(404, "no_active_device_with_token");
    return json({ ok: true, ...result });
  } catch (e) {
    return errorResponse(e);
  }
});
