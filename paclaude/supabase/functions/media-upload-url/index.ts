// POST { tipo: "IMAGEN" | "VIDEO", bytes } · TUTOR
// Devuelve una URL firmada para subir UNA evidencia al bucket privado «evidencias».
// La app hace después: PUT <url> con el archivo y la cabecera Content-Type indicada.
import { HttpError, json, leerJson, manejar } from "../_shared/http.ts";
import { exigirRol } from "../_shared/roles.ts";
import { getDb } from "../_shared/db.ts";

const MAXIMO_BYTES = { IMAGEN: 1_500_000, VIDEO: 10_000_000 } as const;   // el bucket admite hasta 10 MB
const MAXIMO_POR_DIA = 20;

Deno.serve(manejar(async (req) => {
  const perfil = await exigirRol(req, "TUTOR");
  const cuerpo = await leerJson<{ tipo?: string; bytes?: number }>(req);

  if (cuerpo.tipo !== "IMAGEN" && cuerpo.tipo !== "VIDEO") throw new HttpError(400, "tipo debe ser IMAGEN o VIDEO");
  const tipo = cuerpo.tipo;
  const bytes = cuerpo.bytes;
  if (typeof bytes !== "number" || !Number.isInteger(bytes) || bytes <= 0) throw new HttpError(400, "bytes inválido");
  if (bytes > MAXIMO_BYTES[tipo]) {
    throw new HttpError(413, `El archivo excede el máximo de ${MAXIMO_BYTES[tipo]} bytes`, "ARCHIVO_MUY_GRANDE");
  }

  const db = getDb();
  const inicioDelDia = new Date();
  inicioDelDia.setUTCHours(0, 0, 0, 0);
  const { count, error: errorCuenta } = await db.from("archivos")
    .select("path", { count: "exact", head: true })
    .eq("owner_uid", perfil.uid)
    .gte("creado_en", inicioDelDia.toISOString());
  if (errorCuenta) throw new Error("archivos: " + errorCuenta.message);
  if ((count ?? 0) >= MAXIMO_POR_DIA) throw new HttpError(429, "Límite diario de subidas alcanzado", "LIMITE_DIARIO");

  const extension = tipo === "IMAGEN" ? "jpg" : "mp4";
  const contentType = tipo === "IMAGEN" ? "image/jpeg" : "video/mp4";
  const path = `${perfil.uid}/${crypto.randomUUID()}.${extension}`;

  const { error: errorFila } = await db.from("archivos").insert({
    path,
    owner_uid: perfil.uid,
    tipo,
    bytes_declarados: bytes,
  });
  if (errorFila) throw new Error("archivos: " + errorFila.message);

  const { data, error } = await db.storage.from("evidencias").createSignedUploadUrl(path);
  if (error || !data) {
    await db.from("archivos").delete().eq("path", path);
    throw new Error("Storage: " + (error?.message ?? "sin respuesta"));
  }
  return json({ url: data.signedUrl, path, contentType, metodo: "PUT", expiraEnSegundos: 7200 });
}));
