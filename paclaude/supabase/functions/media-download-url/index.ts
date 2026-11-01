// POST { path } · ADMIN (cualquier evidencia) o TUTOR (solo las suyas)
// Devuelve una URL firmada de solo lectura que vence en 5 minutos.
import { exigirTexto, HttpError, json, leerJson, manejar } from "../_shared/http.ts";
import { exigirRol } from "../_shared/roles.ts";
import { getDb } from "../_shared/db.ts";

Deno.serve(manejar(async (req) => {
  const perfil = await exigirRol(req, "ADMIN", "TUTOR");
  const cuerpo = await leerJson<{ path?: string }>(req);
  const path = exigirTexto(cuerpo.path, "path", 300);

  const db = getDb();
  const { data: fila, error } = await db.from("archivos").select("owner_uid").eq("path", path).maybeSingle();
  if (error) throw new Error("archivos: " + error.message);
  if (!fila) throw new HttpError(404, "El archivo no existe o ya fue eliminado", "ARCHIVO_NO_EXISTE");
  if (perfil.rol !== "ADMIN" && fila.owner_uid !== perfil.uid) throw new HttpError(403, "No puedes ver este archivo");

  const { data, error: errorUrl } = await db.storage.from("evidencias").createSignedUrl(path, 300);
  if (errorUrl || !data) throw new Error("Storage: " + (errorUrl?.message ?? "sin respuesta"));
  return json({ url: data.signedUrl, expiraEnSegundos: 300 });
}));
