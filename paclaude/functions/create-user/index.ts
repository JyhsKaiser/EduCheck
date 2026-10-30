// create-user (RF-AD02): the ADMIN creates an account (Firebase Auth), assigns its role claim,
// writes users/{uid} and, for STUDENT accounts, optionally links it to a students/{id} record.
// Deployed with --no-verify-jwt: the Firebase ID token is validated here.
//
// POST /create-user   Authorization: Bearer <Firebase ID token of an ADMIN>
// Body: { email, password, name, role, studentId? }
import { createAuthUser, deleteAuthUser, ROLES, Role, setRoleClaim, verifyCaller } from "../_shared/firebase.ts";
import { fsCommit, fsGetFields, fsWrite } from "../_shared/firestore.ts";
import { errorResponse, HttpError, json } from "../_shared/http.ts";

const EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const ID = /^[A-Za-z0-9_-]{1,128}$/;

Deno.serve(async (req) => {
  try {
    if (req.method !== "POST") throw new HttpError(405, "method_not_allowed");
    const caller = await verifyCaller(req);
    if (caller.role !== "ADMIN") throw new HttpError(403, "forbidden");

    let body: Record<string, unknown>;
    try {
      body = await req.json();
    } catch (_e) {
      throw new HttpError(400, "invalid_json");
    }

    const email = typeof body.email === "string" ? body.email.trim().toLowerCase() : "";
    const password = typeof body.password === "string" ? body.password : "";
    const name = typeof body.name === "string" ? body.name.trim() : "";
    const role = body.role;
    const studentId = body.studentId;

    if (!EMAIL.test(email) || email.length > 254) throw new HttpError(400, "invalid_email");
    if (password.length < 6 || password.length > 128) throw new HttpError(400, "weak_password");
    if (name.length < 1 || name.length > 100) throw new HttpError(400, "invalid_name");
    if (typeof role !== "string" || !(ROLES as readonly string[]).includes(role)) {
      throw new HttpError(400, "invalid_role");
    }
    if (studentId !== undefined && studentId !== null) {
      if (role !== "STUDENT") throw new HttpError(400, "student_link_requires_student_role");
      if (typeof studentId !== "string" || !ID.test(studentId)) throw new HttpError(400, "invalid_student_id");
    }
    const linkStudentId = typeof studentId === "string" ? studentId : null;

    if (linkStudentId) {
      const student = await fsGetFields(`students/${linkStudentId}`);
      if (!student) throw new HttpError(404, "student_not_found");
      if (typeof student.studentUid?.stringValue === "string") throw new HttpError(409, "student_already_linked");
    }

    const uid = await createAuthUser({ email, password, displayName: name });
    try {
      await setRoleClaim(uid, role as Role);
      const writes = [
        fsWrite(`users/${uid}`, {
          name,
          email,
          role,
          active: true,
          createdAt: new Date(),
          studentId: linkStudentId ?? undefined,
        }, { exists: false }),
      ];
      if (linkStudentId) {
        writes.push(fsWrite(`students/${linkStudentId}`, { studentUid: uid }, { mask: ["studentUid"], exists: true }));
      }
      await fsCommit(writes);
    } catch (e) {
      // Do not leave an Auth account without a profile.
      await deleteAuthUser(uid).catch((err) => console.error("rollback failed:", err));
      throw e;
    }
    return json({ ok: true, uid, role }, 201);
  } catch (e) {
    return errorResponse(e);
  }
});
