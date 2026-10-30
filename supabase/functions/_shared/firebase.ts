// Firebase helpers for the Edge Functions: ID token verification and calls to the Google APIs
// with the service account (never exposed to the client).
//
// Secret required (Supabase): FIREBASE_SERVICE_ACCOUNT_B64 = base64 of the service account JSON.
import { createRemoteJWKSet, importPKCS8, jwtVerify, SignJWT } from "npm:jose@5.9.6";
import { HttpError } from "./http.ts";

export const ROLES = ["STUDENT", "GUARDIAN", "TEACHER", "ADMIN"] as const;
export type Role = typeof ROLES[number];

type ServiceAccount = { project_id: string; client_email: string; private_key: string };

let cachedServiceAccount: ServiceAccount | null = null;

export function serviceAccount(): ServiceAccount {
  if (cachedServiceAccount) return cachedServiceAccount;
  const b64 = Deno.env.get("FIREBASE_SERVICE_ACCOUNT_B64");
  if (!b64) throw new Error("FIREBASE_SERVICE_ACCOUNT_B64 is not configured");
  const bytes = Uint8Array.from(atob(b64), (c) => c.charCodeAt(0));
  cachedServiceAccount = JSON.parse(new TextDecoder().decode(bytes)) as ServiceAccount;
  return cachedServiceAccount;
}

// ---------------------------------------------------------------- ID token verification
const JWKS = createRemoteJWKSet(
  new URL("https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com"),
);

export type Caller = { uid: string; role: string | null };

/** Verifies the Firebase ID token in `Authorization: Bearer ...`. Every function must call this first. */
export async function verifyCaller(req: Request): Promise<Caller> {
  const match = (req.headers.get("Authorization") ?? "").match(/^Bearer (.+)$/i);
  if (!match) throw new HttpError(401, "missing_token");
  const projectId = serviceAccount().project_id;
  try {
    const { payload } = await jwtVerify(match[1], JWKS, {
      issuer: `https://securetoken.google.com/${projectId}`,
      audience: projectId,
      algorithms: ["RS256"],
    });
    if (typeof payload.sub !== "string" || payload.sub.length === 0) throw new Error("no sub");
    return { uid: payload.sub, role: typeof payload.role === "string" ? payload.role : null };
  } catch (_e) {
    throw new HttpError(401, "invalid_token");
  }
}

// ---------------------------------------------------------------- service account access token
const SCOPES = [
  "https://www.googleapis.com/auth/identitytoolkit",
  "https://www.googleapis.com/auth/datastore",
].join(" ");

let cachedToken: { value: string; expiresAtMs: number } | null = null;

async function accessToken(): Promise<string> {
  if (cachedToken && cachedToken.expiresAtMs - 60_000 > Date.now()) return cachedToken.value;
  const sa = serviceAccount();
  const key = await importPKCS8(sa.private_key, "RS256");
  const now = Math.floor(Date.now() / 1000);
  const assertion = await new SignJWT({ scope: SCOPES })
    .setProtectedHeader({ alg: "RS256", typ: "JWT" })
    .setIssuer(sa.client_email)
    .setSubject(sa.client_email)
    .setAudience("https://oauth2.googleapis.com/token")
    .setIssuedAt(now)
    .setExpirationTime(now + 3600)
    .sign(key);
  const res = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
      assertion,
    }),
  });
  if (!res.ok) throw new Error(`oauth token request failed: ${res.status} ${await res.text()}`);
  const data = await res.json() as { access_token: string; expires_in: number };
  cachedToken = { value: data.access_token, expiresAtMs: Date.now() + data.expires_in * 1000 };
  return cachedToken.value;
}

// ---------------------------------------------------------------- role management
/** Updates `role` in users/{uid}. Fails with 404 if the profile does not exist (no upsert). */
export async function setProfileRole(uid: string, role: Role): Promise<void> {
  const projectId = serviceAccount().project_id;
  const url = `https://firestore.googleapis.com/v1/projects/${projectId}/databases/(default)/documents/users/${
    encodeURIComponent(uid)
  }?updateMask.fieldPaths=role&currentDocument.exists=true`;
  const res = await fetch(url, {
    method: "PATCH",
    headers: { Authorization: `Bearer ${await accessToken()}`, "Content-Type": "application/json" },
    body: JSON.stringify({ fields: { role: { stringValue: role } } }),
  });
  if (res.status === 404 || res.status === 400 && (await res.clone().text()).includes("NOT_FOUND")) {
    throw new HttpError(404, "user_not_found");
  }
  if (!res.ok) throw new Error(`firestore update failed: ${res.status} ${await res.text()}`);
}

/** Sets the `role` custom claim (the same call the Admin SDK makes). Takes effect on the next ID token. */
export async function setRoleClaim(uid: string, role: Role): Promise<void> {
  const projectId = serviceAccount().project_id;
  const res = await fetch(`https://identitytoolkit.googleapis.com/v1/projects/${projectId}/accounts:update`, {
    method: "POST",
    headers: { Authorization: `Bearer ${await accessToken()}`, "Content-Type": "application/json" },
    body: JSON.stringify({ localId: uid, customAttributes: JSON.stringify({ role }) }),
  });
  if (!res.ok) {
    const text = await res.text();
    if (text.includes("USER_NOT_FOUND")) throw new HttpError(404, "user_not_found");
    throw new Error(`set claim failed: ${res.status} ${text}`);
  }
}
