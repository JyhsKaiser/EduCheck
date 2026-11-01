// create-user (RF-AD02): the ADMIN creates an account (Firebase Auth), assigns its role claim and
// writes users/{uid}. A STUDENT account always has a student record (students/{id}) with
// studentUid = the new uid. The record is either created together with the account (`newStudent`)
// or an existing record without an account is linked (`studentId`).
// Deployed with --no-verify-jwt: the Firebase ID token is validated here.
//
// POST /create-user   Authorization: Bearer <Firebase ID token of an ADMIN>
// Body: { email, password, name, role, studentId? , newStudent?: { studentNumber, firstNames, lastNames } }
import { createAuthUser, deleteAuthUser, ROLES, Role, setRoleClaim, verifyCaller } from "../_shared/firebase.ts";
import { fsCommit, fsExistsWhereEquals, fsGetFields, fsWrite } from "../_shared/firestore.ts";
import { errorResponse, HttpError, json } from "../_shared/http.ts";

const EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const ID = /^[A-Za-z0-9_-]{1,128}$/;
const STUDENT_NUMBER = /^[A-Za-z0-9._-]{1,30}$/;

function text(v: unknown, max: number): string {
  return typeof v === "string" ? v.trim().slice(0, max) : "";
}

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
    const role = body.role;

    if (!EMAIL.test(email) || email.length > 254) throw new HttpError(400, "invalid_email");
    if (password.length < 6 || password.length > 128) throw new HttpError(400, "weak_password");
    if (typeof role !== "string" || !(ROLES as readonly string[]).includes(role)) {
      throw new HttpError(400, "invalid_role");
    }

    // ---- student record (STUDENT accounts only)
    const linkStudentId = body.studentId === undefined || body.studentId === null ? null : body.studentId;
    const rawNewStudent = body.newStudent === undefined || body.newStudent === null
      ? null
      : body.newStudent as Record<string, unknown>;
    if (role !== "STUDENT" && (linkStudentId !== null || rawNewStudent !== null)) {
      throw new HttpError(400, "student_link_requires_student_role");
    }
    if (role === "STUDENT") {
      if ((linkStudentId === null) === (rawNewStudent === null)) throw new HttpError(400, "student_required");
    }
    if (linkStudentId !== null && (typeof linkStudentId !== "string" || !ID.test(linkStudentId))) {
      throw new HttpError(400, "invalid_student_id");
    }

    let name = text(body.name, 100);
    let newStudent: { studentNumber: string; firstNames: string; lastNames: string } | null = null;
    if (rawNewStudent !== null) {
      newStudent = {
        studentNumber: text(rawNewStudent.studentNumber, 30),
        firstNames: text(rawNewStudent.firstNames, 60),
        lastNames: text(rawNewStudent.lastNames, 60),
      };
      if (!STUDENT_NUMBER.test(newStudent.studentNumber) || !newStudent.firstNames || !newStudent.lastNames) {
        throw new HttpError(400, "invalid_student");
      }
      if (await fsExistsWhereEquals("students", "studentNumber", newStudent.studentNumber)) {
        throw new HttpError(409, "student_number_exists");
      }
      name = `${newStudent.firstNames} ${newStudent.lastNames}`.slice(0, 100);
    }
    if (name.length < 1) throw new HttpError(400, "invalid_name");

    if (typeof linkStudentId === "string") {
      const student = await fsGetFields(`students/${linkStudentId}`);
      if (!student) throw new HttpError(404, "student_not_found");
      if (typeof student.studentUid?.stringValue === "string") throw new HttpError(409, "student_already_linked");
    }

    const uid = await createAuthUser({ email, password, displayName: name });
    try {
      await setRoleClaim(uid, role as Role);
      const newStudentId = newStudent ? crypto.randomUUID().replaceAll("-", "").slice(0, 20) : null;
      const studentId = (typeof linkStudentId === "string" ? linkStudentId : newStudentId) ?? undefined;
      const writes = [
        fsWrite(`users/${uid}`, { name, email, role, active: true, createdAt: new Date(), studentId }, { exists: false }),
      ];
      if (newStudent && newStudentId) {
        writes.push(fsWrite(`students/${newStudentId}`, {
          studentNumber: newStudent.studentNumber,
          firstNames: newStudent.firstNames,
          lastNames: newStudent.lastNames,
          studentUid: uid,
          guardianUids: [],
          active: true,
        }, { exists: false }));
      } else if (typeof linkStudentId === "string") {
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
