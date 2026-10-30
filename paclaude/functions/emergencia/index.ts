// POST { emergenciaId } · PROFESOR
// El profesor crea el documento emergencias/{id} en Firestore (las reglas lo validan) y llama aquí
// para avisar con prioridad alta a los administradores (coordinación académica).
import { exigirTexto, HttpError, json, leerJson, manejar } from "../_shared/http.ts";
import { exigirRol } from "../_shared/roles.ts";
import { consultar, leerDoc } from "../_shared/firestore.ts";
import { enviarAUsuarios, marcarEnviado } from "../_shared/avisos.ts";

Deno.serve(manejar(async (req) => {
  const perfil = await exigirRol(req, "PROFESOR");
  const cuerpo = await leerJson<{ emergenciaId?: string }>(req);
  const id = exigirTexto(cuerpo.emergenciaId, "emergenciaId", 200);

  const doc = await leerDoc("emergencias/" + id);
  if (!doc) throw new HttpError(404, "La emergencia aún no existe en Firestore", "EMERGENCIA_NO_EXISTE");
  if (doc.data.profesorUid !== perfil.uid) throw new HttpError(403, "Esta emergencia no es tuya");
  if (doc.data.estado !== "ABIERTA") throw new HttpError(409, "La emergencia ya no está abierta");

  if (!(await marcarEnviado("EMERGENCIA:" + id, "EMERGENCIA"))) return json({ ok: true, duplicado: true });

  const admins = await consultar("usuarios", [
    ["rol", "ADMIN"],
    ["activo", true],
    ["plantelId", String(doc.data.plantelId)],
  ]);
  const enviados = await enviarAUsuarios(admins.map((a) => a.id), {
    tipo: "EMERGENCIA_PROFESOR",
    titulo: "Emergencia reportada por " + (perfil.nombre || "un profesor"),
    cuerpo: String(doc.data.mensaje).slice(0, 140),
    canal: "emergencias",
    alta: true,
    datos: { emergenciaId: id },
  });
  return json({ ok: true, enviados, destinatarios: admins.length });
}));
