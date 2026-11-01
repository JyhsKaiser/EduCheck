// Bootstrap tool: sets the `role` custom claim (and creates/updates users/{uid}) with the service account.
// Use it ONCE to create the first ADMIN of the real project; afterwards roles are managed by the
// `set-role` Edge Function from the ADMIN app.
//
//   node set-claim.mjs <path-to-service-account.json> <email-or-uid> <ROLE>
//
// The Auth account must already exist (Firebase console > Authentication > Add user).
import admin from 'firebase-admin';
import { readFileSync } from 'node:fs';

const [, , keyPath, target, role] = process.argv;
const ROLES = ['STUDENT', 'GUARDIAN', 'TEACHER', 'ADMIN'];
if (!keyPath || !target || !ROLES.includes(role)) {
  console.error('Usage: node set-claim.mjs <service-account.json> <email-or-uid> <' + ROLES.join('|') + '>');
  process.exit(1);
}

admin.initializeApp({ credential: admin.credential.cert(JSON.parse(readFileSync(keyPath, 'utf8'))) });
const auth = admin.auth();
const db = admin.firestore();

const user = target.includes('@') ? await auth.getUserByEmail(target) : await auth.getUser(target);
await auth.setCustomUserClaims(user.uid, { role });

const ref = db.doc(`users/${user.uid}`);
const snap = await ref.get();
const profile = { role, email: user.email ?? '', name: user.displayName ?? user.email ?? user.uid, active: true };
if (!snap.exists) profile.createdAt = admin.firestore.Timestamp.now();
await ref.set(profile, { merge: true });

console.log(`OK: ${user.email ?? user.uid} (${user.uid}) is now ${role}. Sign in again to get the new token.`);
