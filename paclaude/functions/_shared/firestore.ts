// Minimal Firestore REST helpers (service account, bypasses the security rules).
import { accessToken, serviceAccount } from "./firebase.ts";
import { HttpError } from "./http.ts";

export type FsValue = string | boolean | Date | null;

const base = () => `projects/${serviceAccount().project_id}/databases/(default)/documents`;

function encode(v: FsValue): Record<string, unknown> {
  if (v === null) return { nullValue: null };
  if (typeof v === "string") return { stringValue: v };
  if (typeof v === "boolean") return { booleanValue: v };
  return { timestampValue: v.toISOString() };
}

/** Builds the `fields` object of a document; undefined values are skipped. */
export function fsFields(obj: Record<string, FsValue | undefined>): Record<string, unknown> {
  const out: Record<string, unknown> = {};
  for (const [k, v] of Object.entries(obj)) if (v !== undefined) out[k] = encode(v);
  return out;
}

export type FsWrite = Record<string, unknown>;

/**
 * One document write for fsCommit. `mask` limits which fields are touched (merge);
 * `exists` is a precondition: true = must exist, false = must not exist.
 */
export function fsWrite(
  path: string,
  fields: Record<string, FsValue | undefined>,
  opts: { mask?: string[]; exists?: boolean } = {},
): FsWrite {
  const w: FsWrite = { update: { name: `${base()}/${path}`, fields: fsFields(fields) } };
  if (opts.mask) w.updateMask = { fieldPaths: opts.mask };
  if (opts.exists !== undefined) w.currentDocument = { exists: opts.exists };
  return w;
}

/** Applies several writes atomically. */
export async function fsCommit(writes: FsWrite[]): Promise<void> {
  const res = await fetch(`https://firestore.googleapis.com/v1/${base()}:commit`, {
    method: "POST",
    headers: { Authorization: `Bearer ${await accessToken()}`, "Content-Type": "application/json" },
    body: JSON.stringify({ writes }),
  });
  if (res.ok) return;
  if (res.status === 404) throw new HttpError(404, "not_found");
  if (res.status === 409) throw new HttpError(409, "already_exists");
  throw new Error(`firestore commit failed: ${res.status} ${await res.text()}`);
}

/** Returns the raw `fields` of a document, or null if it does not exist. */
export async function fsGetFields(path: string): Promise<Record<string, Record<string, unknown>> | null> {
  const res = await fetch(`https://firestore.googleapis.com/v1/${base()}/${path}`, {
    headers: { Authorization: `Bearer ${await accessToken()}` },
  });
  if (res.status === 404) return null;
  if (!res.ok) throw new Error(`firestore get failed: ${res.status} ${await res.text()}`);
  const doc = await res.json() as { fields?: Record<string, Record<string, unknown>> };
  return doc.fields ?? {};
}
