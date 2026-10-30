// Sample data ONLY for the Firebase Emulator Suite (never touches production).
//
//   1) firebase emulators:start --project <YOUR_PROJECT_ID>
//   2) cd firebase/seed && npm install
//   3) node seed.mjs <YOUR_PROJECT_ID>
//
// Creates Auth accounts (shared password) and the documents the rules require.
import admin from 'firebase-admin';

const projectId = process.argv[2];
if (!projectId) {
  console.error('Usage: node seed.mjs <PROJECT_ID>');
  process.exit(1);
}

// Setting these hosts makes the Admin SDK talk to the emulators instead of the cloud.
process.env.FIRESTORE_EMULATOR_HOST ??= '127.0.0.1:8080';
process.env.FIREBASE_AUTH_EMULATOR_HOST ??= '127.0.0.1:9099';

admin.initializeApp({ projectId });
const db = admin.firestore();
const auth = admin.auth();
const { GeoPoint, Timestamp } = admin.firestore;

const PASSWORD = 'Educheck123!';

const users = [
  { uid: 'u-admin', email: 'admin@educheck.test', role: 'ADMIN', name: 'Admin Demo' },
  { uid: 'u-teacher', email: 'teacher@educheck.test', role: 'TEACHER', name: 'Teacher Demo' },
  { uid: 'u-guardian', email: 'guardian@educheck.test', role: 'GUARDIAN', name: 'Guardian Demo' },
  { uid: 'u-student', email: 'student@educheck.test', role: 'STUDENT', name: 'Ana Pérez', studentId: 'student-1' },
];

for (const u of users) {
  try {
    await auth.createUser({ uid: u.uid, email: u.email, password: PASSWORD, displayName: u.name });
  } catch (e) {
    if (e.code === 'auth/uid-already-exists' || e.code === 'auth/email-already-exists') {
      await auth.updateUser(u.uid, { password: PASSWORD });
    } else {
      throw e;
    }
  }
  // The role lives in a custom claim (this is what the Firestore rules read).
  await auth.setCustomUserClaims(u.uid, { role: u.role });
  const profile = {
    name: u.name,
    email: u.email,
    role: u.role,
    active: true,
    createdAt: Timestamp.now(),
  };
  if (u.studentId) profile.studentId = u.studentId;
  await db.doc(`users/${u.uid}`).set(profile);
}

// Sample coordinates (Zócalo, Mexico City): replace them with the real campus.
await db.doc('config/campus').set({
  name: 'Sample campus',
  center: new GeoPoint(19.4326, -99.1332),
  radiusMeters: 150,
  toleranceMeters: 100,
  toleranceMinutes: 60,
  timeZone: 'America/Mexico_City',
  bssids: ['AA:BB:CC:DD:EE:01'],
});

await db.doc('students/student-1').set({
  studentNumber: '2026-0001',
  firstNames: 'Ana',
  lastNames: 'Pérez',
  groupId: 'group-1',
  studentUid: 'u-student',
  guardianUids: ['u-guardian'],
  active: true,
});

// Student device already approved, so presence registration can be tested.
await db.doc('devices/install-demo-1').set({
  userUid: 'u-student',
  studentId: 'student-1',
  role: 'STUDENT',
  status: 'ACTIVE',
  model: 'Emulator',
  createdAt: Timestamp.now(),
});

await db.doc('groups/group-1').set({
  name: '3A',
  semester: 3,
  shift: 'MORNING',
  period: '2026-B',
  active: true,
  teacherUids: ['u-teacher'],
  students: [{ id: 'student-1', name: 'Ana Pérez', studentNumber: '2026-0001' }],
});

await db.doc('subjects/mat-301').set({ code: 'MAT-301', name: 'Matemáticas III' });

await db.doc('assignments/assignment-1').set({
  groupId: 'group-1',
  groupName: '3A',
  subjectId: 'mat-301',
  subjectName: 'Matemáticas III',
  teacherUid: 'u-teacher',
});

const today = ((new Date().getDay() + 6) % 7) + 1; // 1 = Monday ... 7 = Sunday
await db.doc('schedules/schedule-1').set({
  assignmentId: 'assignment-1',
  groupId: 'group-1',
  groupName: '3A',
  subjectName: 'Matemáticas III',
  teacherUid: 'u-teacher',
  weekday: today,
  startTime: '07:00',
  endTime: '08:00',
  room: 'A-101',
});

await db.doc('grades/assignment-1_student-1').set({
  assignmentId: 'assignment-1',
  studentId: 'student-1',
  teacherUid: 'u-teacher',
  partials: { 1: 8.5, 2: 9.0 },
  importedAt: Timestamp.now(),
});

console.log('Done. Sample accounts (password: ' + PASSWORD + '):');
for (const u of users) console.log(`  ${u.role.padEnd(9)} ${u.email}`);
