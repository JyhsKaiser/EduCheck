// POST · llamada por pg_cron a diario (cabecera x-cron-secret)
// Borra evidencias huérfanas (subidas pero nunca asociadas a un justificante, > 24 h) y las que superan
// la retención (RETENCION_DIAS, 30 por defecto), y limpia tablas de apoyo para no agotar los 500 MB.
import { json, manejar } from "../_shared/http.ts";
import { exigirCron } from "../_shared/roles.ts";
import { getDb } from "../_shared/db.ts";
import { retencionDias } from "../_shared/env.ts";

const DIA_MS = 86_400_000;

Deno.serve(manejar(async (req) => {
  exigirCron(req);
  const db = getDb();
  const ahora = Date.now();

  const { data: huerfanos, error: e1 } = await db.from("archivos").select("path")
    .is("confirmado_en", null).lt("creado_en", new Date(ahora - DIA_MS).toISOString()).limit(500);
  if (e1) throw new Error("archivos: " + e1.message);
  const { data: vencidos, error: e2 } = await db.from("archivos").select("path")
    .lt("creado_en", new Date(ahora - retencionDias() * DIA_MS).toISOString()).limit(500);
  if (e2) throw new Error("archivos: " + e2.message);

  const rutas = [...new Set([...(huerfanos ?? []), ...(vencidos ?? [])].map((f) => String(f.path)))];
  let borrados = 0;
  for (let i = 0; i < rutas.length; i += 100) {
    const lote = rutas.slice(i, i + 100);
    const { error } = await db.storage.from("evidencias").remove(lote);
    if (error) {
      console.error("Storage remove:", error.message);
      continue;                                   // no se borra la fila: se reintenta mañana
    }
    await db.from("archivos").delete().in("path", lote);
    borrados += lote.length;
  }

  const fechaCorte = new Date(ahora - 7 * DIA_MS).toISOString().substring(0, 10);
  await db.from("avisos_enviados").delete().lt("creado_en", new Date(ahora - 60 * DIA_MS).toISOString());
  await db.from("qr_usados").delete().lt("usado_en", new Date(ahora - DIA_MS).toISOString());
  await db.from("estado_dia").delete().lt("fecha", fechaCorte);
  await db.from("horarios_dia").delete().lt("fecha", fechaCorte);

  return json({ ok: true, archivosBorrados: borrados });
}));
