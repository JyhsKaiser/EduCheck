// Datos de ejemplo SOLO para el Firebase Emulator Suite (nunca toca producción).
//
//   1) firebase emulators:start --project <ID_DE_TU_PROYECTO>
//   2) cd firebase/seed && npm install
//   3) node seed.mjs <ID_DE_TU_PROYECTO>
//
// Crea cuentas de Auth (contraseña común) y los documentos que exigen las reglas.
import admin from 'firebase-admin';

const projectId = process.argv[2];
if (!projectId) {
  console.error('Uso: node seed.mjs <ID_DEL_PROYECTO>');
  process.exit(1);
}

// Al fijar estos hosts, el Admin SDK habla con los emuladores y no con la nube.
process.env.FIRESTORE_EMULATOR_HOST ??= '127.0.0.1:8080';
process.env.FIREBASE_AUTH_EMULATOR_HOST ??= '127.0.0.1:9099';

admin.initializeApp({ projectId });
const db = admin.firestore();
const auth = admin.auth();
const { GeoPoint, Timestamp } = admin.firestore;

const CLAVE = 'Educheck123!';
const PLANTEL = 'plantel-1';

const usuarios = [
  { uid: 'u-admin', email: 'admin@educheck.test', rol: 'ADMIN', nombre: 'Admin Demo' },
  { uid: 'u-profesor', email: 'profesor@educheck.test', rol: 'PROFESOR', nombre: 'Profesor Demo' },
  { uid: 'u-tutor', email: 'tutor@educheck.test', rol: 'TUTOR', nombre: 'Tutor Demo' },
  { uid: 'u-alumno', email: 'alumno@educheck.test', rol: 'ALUMNO', nombre: 'Ana Pérez', alumnoId: 'alumno-1' },
];

for (const u of usuarios) {
  try {
    await auth.createUser({ uid: u.uid, email: u.email, password: CLAVE, displayName: u.nombre });
  } catch (e) {
    if (e.code === 'auth/uid-already-exists' || e.code === 'auth/email-already-exists') {
      await auth.updateUser(u.uid, { password: CLAVE });
    } else {
      throw e;
    }
  }
  const perfil = {
    nombre: u.nombre,
    email: u.email,
    rol: u.rol,
    plantelId: PLANTEL,
    activo: true,
    creadoEn: Timestamp.now(),
  };
  if (u.alumnoId) perfil.alumnoId = u.alumnoId;
  await db.doc(`usuarios/${u.uid}`).set(perfil);
}

// Coordenadas de ejemplo (Zócalo de la CDMX): reemplázalas por las del plantel real.
await db.doc(`planteles/${PLANTEL}`).set({
  nombre: 'Plantel de ejemplo',
  centro: new GeoPoint(19.4326, -99.1332),
  radioMetros: 150,
  toleranciaMetros: 100,
  toleranciaMinutos: 60,
  zonaHoraria: 'America/Mexico_City',
  bssids: ['AA:BB:CC:DD:EE:01'],
  activo: true,
});

await db.doc('alumnos/alumno-1').set({
  matricula: '2026-0001',
  nombres: 'Ana',
  apellidos: 'Pérez',
  plantelId: PLANTEL,
  grupoId: 'grupo-1',
  alumnoUid: 'u-alumno',
  tutorUids: ['u-tutor'],
  activo: true,
});

// Dispositivo del alumno ya aprobado, para poder probar el registro de presencia.
await db.doc('dispositivos/install-demo-1').set({
  usuarioUid: 'u-alumno',
  alumnoId: 'alumno-1',
  rol: 'ALUMNO',
  estado: 'ACTIVO',
  clavePublica: 'demo',
  modelo: 'Emulador',
  creadoEn: Timestamp.now(),
});

await db.doc('grupos/grupo-1').set({
  plantelId: PLANTEL,
  nombre: '3A',
  semestre: 3,
  turno: 'MATUTINO',
  periodo: '2026-B',
  activo: true,
  profesorUids: ['u-profesor'],
  alumnos: [{ id: 'alumno-1', nombre: 'Ana Pérez', matricula: '2026-0001', tutorUids: ['u-tutor'] }],
});

await db.doc('materias/mat-301').set({ clave: 'MAT-301', nombre: 'Matemáticas III' });

await db.doc('asignaciones/asig-1').set({
  plantelId: PLANTEL,
  grupoId: 'grupo-1',
  grupoNombre: '3A',
  materiaId: 'mat-301',
  materiaNombre: 'Matemáticas III',
  profesorUid: 'u-profesor',
});

const hoy = ((new Date().getDay() + 6) % 7) + 1; // 1 = lunes ... 7 = domingo
await db.doc('horarios/horario-1').set({
  plantelId: PLANTEL,
  asignacionId: 'asig-1',
  grupoId: 'grupo-1',
  grupoNombre: '3A',
  materiaNombre: 'Matemáticas III',
  profesorUid: 'u-profesor',
  diaSemana: hoy,
  horaInicio: '07:00',
  horaFin: '08:00',
  aula: 'A-101',
});

await db.doc('calificaciones/asig-1_alumno-1').set({
  plantelId: PLANTEL,
  asignacionId: 'asig-1',
  alumnoId: 'alumno-1',
  profesorUid: 'u-profesor',
  parciales: { 1: 8.5, 2: 9.0 },
  importadoEn: Timestamp.now(),
});

console.log('Listo. Cuentas de ejemplo (contraseña: ' + CLAVE + '):');
for (const u of usuarios) console.log(`  ${u.rol.padEnd(9)} ${u.email}`);
