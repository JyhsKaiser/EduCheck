// POST (sin cuerpo) · ALUMNO
// Latido ligero de contacto: solo guarda la hora. No envía ni guarda ubicación.
import { json, manejar } from "../_shared/http.ts";
import { exigirRol } from "../_shared/roles.ts";
import { getDb } from "../_shared/db.ts";
import { fechaLocal } from "../_shared/tiempo.ts";
import { zonaPlantel } from "../_shared/env.ts";

Deno.serve(manejar(async (req) => {
  const perfil = await exigirRol(req, "ALUMNO");
  const hoy = fechaLocal(new Date(), zonaPlantel());
  const { data, error } = await getDb()
    .from("estado_dia")
    .update({ ultimo_contacto: new Date().toISOString() })
    .eq("alumno_uid", perfil.uid)
    .eq("fecha", hoy)
    .is("salida_en", null)
    .select("alumno_id");
  if (error) throw new Error("estado_dia: " + error.message);
  // activo=false: aún no hay entrada registrada hoy (o ya salió); la app puede dejar de mandar latidos
  return json({ ok: true, activo: (data?.length ?? 0) > 0 });
}));
