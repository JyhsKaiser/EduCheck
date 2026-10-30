# EduCheck · Firestore data model and rules (v1.4)

Source of truth: Firestore (Spark plan). Designed for the **free quota** (50,000 reads and 20,000 writes per day).
Server-side work (cron, FCM push, signed URLs, custom claims) runs in **Supabase Edge Functions** (section 6).
Identifiers (collections, fields, enum values) are in English; the UI is in Spanish.

## 1. Design principles
1. **Deterministic ids** (`{studentId}_{date}`, `{scheduleId}_{date}`): one presence per student and day and one class session per schedule and day, without transactions.
2. **One write per action**: roll call stores the *whole* list in one document (map), not one document per student.
3. **Denormalize for rules**: `guardianUids` is copied to every presence, and `teacherUids` plus the student list live in `groups`. Query rules cannot follow relations, so each reader needs a field to validate against.
4. **Rules validate what matters**: inside the geofence (`latlng.distance`), `ACTIVE` device, reasonable time, structure and ownership. Clients never write `ABSENT`, notices or grades.
5. **Counters without extra writes**: the dashboard uses `count()` queries (1 read per 1,000 documents).
6. **Single campus**: geofence, BSSIDs and tolerances live in one document, `config/campus`. There is no `campusId` anywhere.

## 2. Collection map
| Collection | docId | Main content | Writes | Reads |
|---|---|---|---|---|
| `config/campus` | `campus` | `name`, `center` (GeoPoint), `radiusMeters`, `toleranceMeters` (100), `toleranceMinutes` (60), `timeZone`, `bssids[]` | ADMIN | any signed-in user |
| `users` | Auth uid | `name`, `email`, `role` (STUDENT/GUARDIAN/TEACHER/ADMIN), `active`, `studentId?` | ADMIN | the user; ADMIN |
| `devices` | `installId` (app UUID) | `userUid`, `studentId?`, `role`, `status` (PENDING/ACTIVE/REVOKED), `model`, `fcmToken`, `createdAt` | the app (registration and token); ADMIN (status) | owner; ADMIN |
| `students` | id | `studentNumber`, `firstNames`, `lastNames`, `groupId`, `studentUid` (nullable), `guardianUids[]`, `active` | ADMIN | student, guardians, ADMIN |
| `presences` | `{studentId}_{yyyy-MM-dd}` | `studentId`, `studentUid`, `date`, `status`, `method`, `confidence`, `location`, `bssid?`, `entryTime`, `exitTime?`, `trustedClock`, `registeredBy?`, `guardianUids[]`, `deviceId`, `clientUuid`, `createdAt` | student (entry and exit); ADMIN (manual) | student, guardians, ADMIN |
| `absences` | `{studentId}_{date}` | closing-window result written by the backend | **backend only** | guardians, ADMIN |
| `justifications` | id | `studentId`, `guardianUid`, `type`, `startDate`, `endDate`, `reason`, `evidencePath?`, `evidenceType?`, `status`, `reviewComment?`, `reviewedBy?`, `reviewedAt?`, `clientUuid`, `createdAt` | guardian (create); ADMIN (resolve) | guardian, ADMIN |
| `groups` | id | `name`, `semester`, `shift`, `period`, `teacherUids[]`, `students[{id,name,studentNumber}]` | ADMIN | group teachers, ADMIN |
| `subjects` | id | `code`, `name` | ADMIN | any signed-in user |
| `assignments` | id | `groupId`, `subjectId`, `subjectName`, `groupName`, `teacherUid` | ADMIN | its teacher, ADMIN |
| `schedules` | id | `assignmentId`, `groupId`, `teacherUid`, `subjectName`, `groupName`, `weekday` (1–7), `startTime`, `endTime`, `room` | ADMIN | its teacher, ADMIN |
| `class_sessions` | `{scheduleId}_{yyyy-MM-dd}` | `scheduleId`, `assignmentId`, `groupId`, `teacherUid`, `date`, `subjectName`, `groupName`, `startTime`, `endTime`, `status` (OPEN/CLOSED), `roster{studentId:{status,justified}}` | teacher (while OPEN); ADMIN | its teacher, ADMIN |
| `grades` | `{assignmentId}_{studentId}` | `assignmentId`, `studentId`, `teacherUid`, `partials{1,2,3}`, `importedAt` | ADMIN (CSV import) | its teacher, ADMIN |
| `emergencies` | id | `teacherUid`, `classSessionId?`, `message`, `status` (OPEN/ATTENDED), `createdAt`, `attendedBy?`, `attendedAt?` | teacher (create); ADMIN (attend) | author, ADMIN |
| `notifications` | id | `recipientUid`, `type`, `title`, `body`, `studentId?`, `createdAt`, `read` | **backend only** | recipient (may mark `read`) |

Values: presence `status` = PENDING / PRESENT / ABSENT; presence `method` = GEOFENCE / GEOFENCE_WIFI / ADMIN_MANUAL; roster `status` = ATTENDED / NOT_ATTENDED with `justified` flag. ADMIN is also the academic coordinator and covers the prefecture duties.

## 3. Relevant rules (`firestore.rules`)
- **Role:** from the custom claim `request.auth.token.role`, set by the `set-role` Edge Function (RF-B07). It costs no Firestore read. A user without the claim is denied everywhere. The `users/{uid}.role` field mirrors the claim for display and is kept in sync by `set-role`. To deactivate an account, disable it in Firebase Auth (`create-user`/admin tools, Sprint 2); `students.active` is still checked by the presence rule.
- **Student presence (create):** allowed and required fields; `studentUid == request.auth.uid`; `guardianUids` equal to the student's; `status == 'PRESENT'`; `method` in {`GEOFENCE`, `GEOFENCE_WIFI`}; `confidence` consistent with the method; `location.distance(config/campus.center) <= radiusMeters + toleranceMeters`; with Wi-Fi, `bssid` must be in `config/campus.bssids`; `entryTime` not in the future nor older than 24 h (to support the offline queue); `createdAt == request.time`; and the device must exist, be `ACTIVE` and belong to the student. Uses 3 `get()` calls (limit: 10).
- **Manual presence (create):** ADMIN only, `method == 'ADMIN_MANUAL'`, `registeredBy == uid`.
- **Exit (update):** student only, only `exitTime`, never backwards.
- **Roll call:** the teacher creates `class_sessions/{scheduleId}_{date}` only if the schedule is theirs; can correct while `OPEN`; afterwards only ADMIN.
- **Justifications:** the student's guardian creates with `status == 'PENDING'` and an evidence path that starts with their uid; ADMIN resolves (`ACCEPTED`/`REJECTED`) and leaves their uid and the server time.
- **Devices:** the student requests with `status == 'PENDING'`; other roles are `ACTIVE`. The owner only changes `fcmToken`; ADMIN approves or revokes.
- **Everything not listed** stays closed.

## 4. Queries per screen and indexes (`firestore.indexes.json`)
| Screen | Query | Index |
|---|---|---|
| Guardian · board | `students` with `guardianUids array-contains uid`; `presences` with `guardianUids array-contains uid` and `date == today` | composite `presences(guardianUids, date)` |
| Guardian · history | same, with `date >= from` | same index |
| Admin · dashboard | `count()` of `presences` with `date`, `status` (equalities) | none |
| Admin · live | `presences` with `date == today`, ordered by `entryTime` desc | `presences(date, entryTime)` |
| Teacher · my day | `schedules` with `teacherUid` and `weekday == today`, ordered by `startTime`; `class_sessions` with `teacherUid` and `date == today` | `schedules(teacherUid, weekday, startTime)` and `class_sessions(teacherUid, date, startTime)` |
| Teacher · roll call | read `groups/{groupId}` (student list) | none |
| Admin · justifications | `status == PENDING`, ordered by `createdAt` desc | `justifications(status, createdAt)` |
| Guardian · my justifications | `guardianUid`, ordered by `createdAt` desc | `justifications(guardianUid, createdAt)` |
| Admin · emergencies | `status == OPEN`, ordered by `createdAt` desc | `emergencies(status, createdAt)` |

## 5. Quota
Academic project with minimal test data: a few dozen reads per test session, far below the Spark limits. If it ever grew, the original estimate (500 students, 400 guardians, about 10,000 reads and 1,150 writes per day) stays valid in order of magnitude. Cares: a 3,000-document grade import uses 15 % of the daily writes, and heartbeats must **not** be written to Firestore.

## 6. Server side: Supabase Edge Functions
| Requirement | Function | Notes |
|---|---|---|
| RF-B01 window closing | `close-window` (cron every minute, pg_cron) | writes `absences`, changes PENDING to ABSENT, triggers push |
| RF-T02, T03, B04, B05, PF04 push | `send-push` | FCM HTTP v1; Firebase service account stored as a Supabase secret; the client never sends push |
| RF-B06 evidence | `signed-url` | validates the Firebase ID token and returns signed upload/download URLs; the bucket is private |
| RF-B07 roles | `set-role` | sets the `role` custom claim through the Firebase Auth API |
| RF-B05 loss of contact | `heartbeat` function + `last_seen` table in Supabase Postgres | decided: the app reports the last contact per student; Edge Functions are stateless, so it is stored in Supabase Postgres, never in Firestore. `close-window` reads it every minute |
| RF-AD02 user creation | `create-user` | decided: the ADMIN app calls it; it validates the ADMIN claim, creates the Auth account through the Firebase Auth REST API with the service account, sets the role claim and writes `users/{uid}` |

## 7. Open decisions
1. ~~Where to store heartbeats~~ **Decided:** Supabase Postgres `last_seen` table (see section 6). Schema and reporting interval are defined in Sprint 6.
2. **Mock location:** rules validate the radius but cannot detect a simulated location; complement with `isMock` on the client and roll call.
3. **Group student list** (`groups.students`) is maintained by ADMIN together with `students.groupId`; both must be updated in one batch.
4. **Class session date** is not validated against the server clock (only the creation time).
5. **Visibility for guardians and students** of per-class attendance and grades: they have no access today.
6. ~~User creation from the ADMIN app~~ **Decided:** `create-user` Edge Function instead of a secondary `FirebaseApp` instance (see section 6).
7. **`devices.publicKey`** existed for the QR flow and was removed; reconsider only if device attestation is needed.
