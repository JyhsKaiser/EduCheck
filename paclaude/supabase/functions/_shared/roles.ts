import { leerDoc } from "./firestore.ts";
import { verificarToken } from "./firebase-auth.ts";
import { HttpError, igualesSeguro } from "./http.ts";
import { env } from "./env.ts";

export type Rol = "ALUMNO" | "TUTOR" | "PROFESOR" | "ADMIN";

export interface Perfil {
  uid: string;
  rol: Rol;
  plantelId: string;
  nombre: string;
  alumnoId?: string;
}

// Caché breve para no leer usuarios/{uid} en cada llamada (la cuota de lecturas es limitada).
const cache = new Map<string, { perfil: Perfil; expiraEnMs: number }>();
const TTL_MS = 60_000;

/** Valida el token de Firebase y el perfil en Firestore; opcionalmente exige uno de los roles. */
export async function exigirRol(req: Request, ...permitidos: Rol[]): Promise<Perfil> {
  const usuario = await verificarToken(req);

  let perfil = cache.get(usuario.uid)?.perfil;
  if (!perfil || cache.get(usuario.uid)!.expiraEnMs < Date.now()) {
    const doc = await leerDoc("usuarios/" + usuario.uid);
    if (!doc || doc.data.activo !== true) throw new HttpError(403, "Cuenta inexistente o desactivada");
    perfil = {
      uid: usuario.uid,
      rol: String(doc.data.rol) as Rol,
      plantelId: String(doc.data.plantelId ?? ""),
      nombre: String(doc.data.nombre ?? ""),
      alumnoId: doc.data.alumnoId ? String(doc.data.alumnoId) : undefined,
    };
    cache.set(usuario.uid, { perfil, expiraEnMs: Date.now() + TTL_MS });
  }

  if (permitidos.length > 0 && !permitidos.includes(perfil.rol)) {
    throw new HttpError(403, "No tienes permiso para esta acción");
  }
  return perfil;
}

/** Las funciones programadas se invocan con la cabecera x-cron-secret (la manda pg_cron). */
export function exigirCron(req: Request): void {
  const recibido = req.headers.get("x-cron-secret") ?? "";
  if (!igualesSeguro(recibido, env("CRON_SECRET"))) throw new HttpError(401, "No autorizado");
}
