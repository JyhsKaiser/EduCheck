// set-role (RF-B07): assigns a user's role as a Firebase custom claim and mirrors it in users/{uid}.
// Only an ADMIN can call it. Deployed with --no-verify-jwt: the Firebase ID token is validated here.
//
// POST /set-role   Authorization: Bearer <Firebase ID token>
// Body: { "uid": "<target uid>", "role": "STUDENT" | "GUARDIAN" | "TEACHER" | "ADMIN" }
import { ROLES, Role, setProfileRole, setRoleClaim, verifyCaller } from "../_shared/firebase.ts";
import { errorResponse, HttpError, json } from "../_shared/http.ts";

Deno.serve(async (req) => {
  try {
    if (req.method !== "POST") throw new HttpError(405, "method_not_allowed");

    const caller = await verifyCaller(req);
    if (caller.role !== "ADMIN") throw new HttpError(403, "forbidden");

    let body: { uid?: unknown; role?: unknown };
    try {
      body = await req.json();
    } catch (_e) {
      throw new HttpError(400, "invalid_json");
    }
    const uid = body.uid;
    const role = body.role;
    if (typeof uid !== "string" || !/^[A-Za-z0-9:_-]{1,128}$/.test(uid)) throw new HttpError(400, "invalid_uid");
    if (typeof role !== "string" || !(ROLES as readonly string[]).includes(role)) {
      throw new HttpError(400, "invalid_role");
    }
    // An admin cannot change their own role (avoids leaving the system without an ADMIN).
    if (uid === caller.uid) throw new HttpError(400, "cannot_change_own_role");

    await setProfileRole(uid, role as Role);
    await setRoleClaim(uid, role as Role);
    return json({ ok: true, uid, role });
  } catch (e) {
    return errorResponse(e);
  }
});
