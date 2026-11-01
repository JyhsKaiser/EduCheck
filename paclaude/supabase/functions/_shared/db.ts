import { createClient, type SupabaseClient } from "npm:@supabase/supabase-js@2";
import { env } from "./env.ts";

let cliente: SupabaseClient | null = null;

/** Cliente con la clave de servicio: acceso total a Postgres y Storage, solo dentro de las funciones. */
export function getDb(): SupabaseClient {
  if (!cliente) {
    // Supabase inyecta SUPABASE_SERVICE_ROLE_KEY; si tu proyecto no la expone, define EDUCHECK_SERVICE_KEY como secreto.
    const claveServicio = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? env("EDUCHECK_SERVICE_KEY");
    cliente = createClient(env("SUPABASE_URL"), claveServicio, {
      auth: { persistSession: false, autoRefreshToken: false },
    });
  }
  return cliente;
}
