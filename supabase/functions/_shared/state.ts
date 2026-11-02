// Small bookkeeping in Supabase Postgres (table public.push_state), through PostgREST with the service role.
// Firestore is not used for it: it would cost writes of the free quota every minute.

function config(): { url: string; key: string } {
  const url = Deno.env.get("SUPABASE_URL");
  const key = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
  if (!url || !key) throw new Error("SUPABASE_URL / SUPABASE_SERVICE_ROLE_KEY are not available");
  return { url, key };
}

const headers = (key: string) => ({ apikey: key, Authorization: `Bearer ${key}`, "Content-Type": "application/json" });

export async function getCursor(name: string): Promise<Date | null> {
  const { url, key } = config();
  const res = await fetch(`${url}/rest/v1/push_state?name=eq.${encodeURIComponent(name)}&select=cursor`, { headers: headers(key) });
  if (!res.ok) throw new Error(`push_state read failed: ${res.status} ${await res.text()} (was the migration applied?)`);
  const rows = await res.json() as Array<{ cursor: string }>;
  return rows.length > 0 ? new Date(rows[0].cursor) : null;
}

export async function setCursor(name: string, cursor: Date): Promise<void> {
  const { url, key } = config();
  const res = await fetch(`${url}/rest/v1/push_state`, {
    method: "POST",
    headers: { ...headers(key), Prefer: "resolution=merge-duplicates" },
    body: JSON.stringify([{ name, cursor: cursor.toISOString(), updated_at: new Date().toISOString() }]),
  });
  if (!res.ok) throw new Error(`push_state write failed: ${res.status} ${await res.text()}`);
}
