// POST { token, installId? } · cualquier usuario autenticado
// Registra (o renueva) el token FCM del dispositivo. Los tokens viven en Postgres, no en Firestore.
import { exigirTexto, json, leerJson, manejar } from "../_shared/http.ts";
import { exigirRol } from "../_shared/roles.ts";
import { getDb } from "../_shared/db.ts";

Deno.serve(manejar(async (req) => {
  const perfil = await exigirRol(req);
  const cuerpo = await leerJson<{ token?: string; installId?: string }>(req);
  const token = exigirTexto(cuerpo.token, "token", 4096);
  const installId = cuerpo.installId ? exigirTexto(cuerpo.installId, "installId", 100) : null;

  const db = getDb();
  const { error } = await db.from("fcm_tokens").upsert(
    { token, uid: perfil.uid, install_id: installId, rol: perfil.rol, actualizado_en: new Date().toISOString() },
    { onConflict: "token" },
  );
  if (error) throw new Error("fcm_tokens: " + error.message);

  // Si el dispositivo rotó su token, se elimina el anterior
  if (installId) {
    await db.from("fcm_tokens").delete().eq("uid", perfil.uid).eq("install_id", installId).neq("token", token);
  }
  return json({ ok: true });
}));
