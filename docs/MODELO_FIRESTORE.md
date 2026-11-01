# EduCheck · Firestore data model and rules (v1.9)

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
| `devices` | `{installId}_{uid}` (app UUID + account) | `userUid`, `studentId?`, `role`, `status` (PENDING/ACTIVE/REVOKED), `model`, `fcmToken`, `createdAt` | the app (registration and token); ADMIN (status) | owner; ADMIN |
| `students` | id | `studentNumber`, `firstNames`, `lastNames`, `groupId`, `studentUid` (nullable), `guardianUids[]`, `active` | ADMIN | student, guardians, ADMIN |
| `presences` | `{studentId}_{yyyy-MM-dd}` | `studentId`, `studentUid`, `date`, `status`, `method`, `confidence`, `location`, `bssid?`, `entryTime`, `exitTime?`, `trustedClock`, `registeredBy?`, `guardianUids[]`, `deviceId`, `clientUuid`, `createdAt` | student (entry and exit); ADMIN (manual) | student, guardians, ADMIN |
| `absences` | `{studentId}_{date}` | closing-window result written by the backend | **backend only** | guardians, ADMIN |
| `justifications` | id | `studentId`, `guardianUid`, `type`, `startDate`, `endDate`, `reason`, `evidencePath?`, `evidenceType?`, `status`, `reviewComment?`, `reviewedBy?`, `reviewedAt?`, `clientUuid`, `createdAt` | guardian (create); ADMIN (resolve) | guardian, ADMIN |
| `groups` | id | `name`, `semester`, `shift`, `period`, `teacherUids[]`, `students[{id,name,studentNumber}]` | ADMIN | group teachers, ADMIN |
| `subjects` | id | `code`, `name` | ADMIN | any signed-in user |
| `assignments` | id | `groupId`, `subjectId`, `subjectName`, `groupName`, `teacherUid` | ADMIN | its teacher, ADMIN |
| `schedules` | id | `assignmentId`, `groupId`, `subjectId`, `teacherUid`, `subjectName`, `groupName`, `weekday` (1–7), `startTime`, `endTime`, `room` | ADMIN | its teacher, ADMIN |
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
| RF-AD02 user creation | `create-user` | implemented (Sprint 2a, extended in 2c): the ADMIN app calls it; it validates the ADMIN claim, creates the Auth account through the Firebase Auth REST API with the service account, sets the role claim and writes `users/{uid}`. A STUDENT account always gets a student record: it creates `students/{id}` (`newStudent`, rejecting a repeated `studentNumber`) or links an existing one without account (`studentId`), in the same atomic commit |
| RF-AD02 activate / deactivate | `set-user-active` | implemented (Sprint 2a): disables the Auth account, revokes its refresh tokens and sets `users.active`; an ID token already issued works until it expires (at most 1 h) |

## 7. Open decisions
1. ~~Where to store heartbeats~~ **Decided:** Supabase Postgres `last_seen` table (see section 6). Schema and reporting interval are defined in Sprint 6.
2. **Mock location:** rules validate the radius but cannot detect a simulated location; complement with `isMock` on the client and roll call.
3. **Group student list** (`groups.students`) is maintained by ADMIN together with `students.groupId`; both must be updated in one batch.
4. **Class session date** is not validated against the server clock (only the creation time).
5. **Visibility for guardians and students** of per-class attendance and grades: they have no access today.
6. ~~User creation from the ADMIN app~~ **Implemented:** `create-user` Edge Function instead of a secondary `FirebaseApp` instance (see section 6).
7. **`devices.publicKey`** existed for the QR flow and was removed; reconsider only if device attestation is needed.

## 8. Academic structure: integrity rules (Sprint 2b)
Copies of names and lists exist so each reader needs a single read. The ADMIN app keeps them consistent:
1. **One group per student.** `students.groupId` and `groups.students` change in one batch (`setGroupStudents`). Selecting a student for a group removes them from their previous group. Maximum 80 students per group (the roll-call rule accepts at most 80 entries).
2. **Student rename.** Editing a student updates their entry in `groups.students` (same batch).
3. **Teacher access.** Creating an assignment adds the teacher to `groups.teacherUids` (same batch); deleting the teacher's last assignment in that group removes them. The rules let a teacher read a group only through that list.
4. **Renames propagate.** Renaming a group updates `groupName` in its assignments and schedules; renaming a subject updates `subjectName` in its assignments and schedules (this is why schedules store `subjectId`). `class_sessions` keep the names they had when the class was held.
5. **Deleting.** A subject or a group with assignments, and an assignment with schedules, cannot be deleted (`HAS_DEPENDENTS`). Deleting a group clears `groupId` of its students.
6. **No duplicates.** A group cannot have the same subject twice, and subject codes are unique.
7. **Schedule clashes.** A schedule is rejected when, on the same weekday, its hours overlap another class that shares the teacher, the group or the room. The check runs in the app (domain use case) before writing; it is not enforced by the rules.
8. **To change an assignment's teacher**, delete its schedules and the assignment, then create it again (no edit operation yet).

## 9. Campus configuration and grades import (Sprint 2c)
**`config/campus`** (single document, written by ADMIN from Gestión → Plantel): `name`, `center` (GeoPoint), `radiusMeters`, `toleranceMeters` (default 100), `toleranceMinutes` (default 60), `timeZone` (default `America/Mexico_City`) and `bssids[]`. The center is chosen on a Google map (tap) or with "Use my current location". BSSIDs are stored in upper case with colons (`AA:BB:CC:DD:EE:FF`), the format the presence rule compares against; "Detect current Wi-Fi" reads the BSSID of the connected network (needs the location permission and location services on). Limits: radius 20–5000 m, distance tolerance 0–1000 m, time tolerance 0–240 min.

**Grades CSV** (Gestión → Importar calificaciones). Header, in any order and case-insensitive: `studentNumber, subjectCode, groupName, partial1, partial2, partial3`. Separator comma or semicolon; decimals with dot (or comma when the separator is a semicolon); grades 0–10; blank partial = not stored; maximum 2,000 rows and 1 MB. Every row is validated before anything is written: the student, subject and group must exist, the group must have that subject (assignment) and the student must belong to the group. Rows with errors are reported by line and skipped; valid rows are written to `grades/{assignmentId}_{studentId}` (`partials` as a map with keys "1", "2", "3", `teacherUid` copied from the assignment, `importedAt`). Re-importing a student replaces their previous grades for that assignment.

## 10. Student core: entry registration (Sprint 3, online)
**Flow.** The student opens the app (tab "Hoy"): the app finds the student's record (`users/{uid}.studentId` → `students/{id}`), registers this phone in `devices/{installId}_{uid}` as `PENDING` if it is new, and reads today's presence. The ADMIN approves the phone in Gestión → Dispositivos. Only an `ACTIVE` phone can register an entry (the rules check it).

**Detection.** With the location permission the app registers a Play Services geofence on the campus circle (`radiusMeters`, never smaller than 100 m). Android wakes the app on ENTER (needs "Allow all the time"); a WorkManager worker then takes the position given by the system and the connected BSSID and runs the same use case as the "Registrar mi entrada ahora" button. If the phone is already inside when the geofence is registered, the entry is reported at once. Without the background permission the entry is registered only when the app is opened inside the campus (automatic attempt on opening, or the button). Geofences are registered again after a reboot, an app update and every time the "Hoy" screen is opened.

**What is written** (`presences/{studentId}_{yyyy-MM-dd}`, date in the campus time zone): `status = PRESENT`; `method = GEOFENCE_WIFI` (confidence 1, the connected BSSID is one of `config/campus.bssids`) or `GEOFENCE` (confidence 0, no school Wi-Fi: it counts as present all the same, RF-A02); `location`; `entryTime` (the moment of the event, never in the future nor older than 23 h); `trustedClock` (true when the phone's automatic date and time is on); `guardianUids` copied from the student; `deviceId`; `clientUuid`; `createdAt` (server time). One entry per student per day: if it already exists nothing is written and no notification is shown.

**Checks before writing** (the same ones the rules enforce, so the student gets a clear message instead of a rejection): student active, campus configured, phone approved, distance to the center ≤ `radiusMeters + toleranceMeters` (haversine). After a successful write the student gets a local notification (RF-A05).

**Not yet.** Offline queue and automatic sync (Sprint 4), exit time and the alerts to guardians (Sprints 5–6). Until Sprint 4, an entry that cannot be written because there is no connection is retried by WorkManager (up to 5 times) when the geofence triggered it, and is simply attempted again the next time the app is opened.

## 11. Offline-first and synchronization (Sprint 4)
**Local store.** Room is the only local store and the source of truth of the student screens. Firestore keeps just an in-memory cache (its disk persistence is turned off), so there are not two caches. Tables: `local_presence` (the queue of entries) and `cache_entry` (last server value of the profile, the device status and the campus configuration, as JSON, keyed `profile:{uid}`, `device:{uid}` and `campus`). The local database version is 2; while the project is in development a schema change rebuilds it.

**Policy per type of data** (RA-09):
| Data | Where it is written | While offline | Conflict rule |
|---|---|---|---|
| Entry of the day (`presences`) | Room first, then sent by WorkManager | Saved with the real time of the event and queued | Append-only, one per student and day (deterministic id). **The server wins**: if it already has the day's entry, the local copy is marked as synced and nothing is written. A rejected entry can be registered again. |
| Student profile, device status, campus configuration | Server only (the app never writes them back) | Last cached value is used | Server always wins; the cache is refreshed every time it is read online. |
| Administration screens (users, groups, schedules, grades...) | Firestore directly | Not available offline by design (online-only) | Last write wins (Firestore). |

**Arriving without signal.** The geofence works without connection. The entry is queued and the student gets the local notification. The checks that need the server are repeated when it is sent: if the phone turns out not to be approved the entry is discarded (REJECTED) and the student is told, with the reason, in a notification and on the "Hoy" screen.

**Sending.** `requestSync` enqueues a unique WorkManager job with a "connected" constraint and exponential backoff (30 s); it is requested when an entry is saved and every time the "Hoy" screen is opened. For each queued entry of the signed-in student, with fresh data: older than 23.5 h → REJECTED; the server already has it → SYNCED; outside the campus with the current configuration → REJECTED; school Wi-Fi no longer listed → sent as GEOFENCE (confidence 0, no BSSID); rules refuse it → REJECTED; no connection → stays PENDING and is retried without limit; any other temporary error → retried up to 8 attempts, then REJECTED. The original `clientUuid` is written to Firestore, so a duplicate send is harmless.

**States** of an entry: PENDING (saved, not sent), SYNCED, REJECTED (reason in `lastError`).
