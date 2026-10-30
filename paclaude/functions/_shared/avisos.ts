import { getDb } from "./db.ts";
import { env } from "./env.ts";
import { googleFetch } from "./google.ts";

/**
 * Marca un aviso como enviado. Devuelve true si es la primera vez (hay que enviarlo)
 * y false si ya existía: evita duplicados ante reintentos de la app o del cron.
 */
export async function marcarEnviado(clave: string, tipo: string): Promise<boolean> {
  const { data, error } = await getDb()
    .from("avisos_enviados")
    .upsert({ clave, tipo }, { onConflict: "clave", ignoreDuplicates: true })
    .select("clave");
  if (error) throw new Error("avisos_enviados: " + error.message);
  return (data?.length ?? 0) > 0;
}

/** Libera la marca (para reintentar si el envío falló por completo). */
export async function desmarcar(clave: string): Promise<void> {
  await getDb().from("avisos_enviados").delete().eq("clave", clave);
}

export interface Aviso {
  tipo: string;
  titulo: string;
  cuerpo: string;
  canal: string;                 // id del canal de notificación de Android
  alta?: boolean;                // prioridad alta de FCM
  datos?: Record<string, string>;
}

/** Envía un push (FCM HTTP v1, solo data payload) a todos los dispositivos de los usuarios indicados. */
export async function enviarAUsuarios(uids: string[], aviso: Aviso): Promise<number> {
  const unicos = [...new Set(uids.filter((u) => typeof u === "string" && u !== ""))];
  if (unicos.length === 0) return 0;

  const db = getDb();
  const { data, error } = await db.from("fcm_tokens").select("token").in("uid", unicos);
  if (error) throw new Error("fcm_tokens: " + error.message);

  const url = "https://fcm.googleapis.com/v1/projects/" + env("FIREBASE_PROJECT_ID") + "/messages:send";
  let enviados = 0;
  for (const fila of data ?? []) {
    const token = String(fila.token);
    const respuesta = await googleFetch(url, {
      method: "POST",
      body: JSON.stringify({
        message: {
          token,
          data: { tipo: aviso.tipo, titulo: aviso.titulo, cuerpo: aviso.cuerpo, canal: aviso.canal, ...(aviso.datos ?? {}) },
          android: { priority: aviso.alta ? "HIGH" : "NORMAL" },
        },
      }),
    });
    if (respuesta.ok) {
      enviados++;
      continue;
    }
    const detalle = await respuesta.text();
    if (respuesta.status === 404 || detalle.includes("UNREGISTERED")) {
      await db.from("fcm_tokens").delete().eq("token", token);   // token caducado: se elimina
    } else {
      console.error("FCM falló (" + respuesta.status + "): " + detalle);
    }
  }
  return enviados;
}
