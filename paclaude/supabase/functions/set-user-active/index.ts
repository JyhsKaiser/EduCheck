// set-user-active (RF-AD02): the ADMIN activates or deactivates an account.
// Deactivating disables the Firebase Auth account (it cannot sign in or refresh its token),
// revokes its refresh tokens and sets users/{uid}.active = false.
// An ID token already issued keeps working until it expires (at most one hour).
//
// POST /set-user-active   Authorization: Bearer <Firebase ID token of an ADMIN>
// Body: { uid, active }
import { setAuthUserDisabled, verifyCaller } from "../_shared/firebase.ts";
import { fsCommit, fsWrite } from "../_shared/firestore.ts";
import { errorResponse, HttpError, json } from "../_shared/http.ts";

Deno.serve(async (req) => {
  try {
    if (req.method !== "POST") throw new HttpError(405, "method_not_allowed");
    const caller = await verifyCaller(req);
    if (caller.role !== "ADMIN") throw new HttpError(403, "forbidden");

    let body: { uid?: unknown; active?: unknown };
    try {
      body = await req.json();
    } catch (_e) {
      throw new HttpError(400, "invalid_json");
    }
    const { uid, active } = body;
    if (typeof uid !== "string" || !/^[A-Za-z0-9:_-]{1,128}$/.test(uid)) throw new HttpError(400, "invalid_uid");
    if (typeof active !== "boolean") throw new HttpError(400, "invalid_active");
    if (uid === caller.uid) throw new HttpError(400, "cannot_change_own_status");

    await setAuthUserDisabled(uid, !active);
    try {
      await fsCommit([fsWrite(`users/${uid}`, { active }, { mask: ["active"], exists: true })]);
    } catch (e) {
      if (e instanceof HttpError && e.status === 404) throw new HttpError(404, "user_not_found");
      throw e;
    }
    return json({ ok: true, uid, active });
  } catch (e) {
    return errorResponse(e);
  }
});
