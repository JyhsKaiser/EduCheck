// POST { justificanteId, accion: "CREADO" | "RESUELTO" }
//  - CREADO  (TUTOR): confirma la evidencia subida y avisa a los administradores.
//  - RESUELTO (ADMIN): avisa al tutor que su justificante fue aceptado o rechazado.
import { exigirTexto, HttpError, json, leerJson, manejar } from "../_shared/http.ts";
import { exigirRol } from "../_shared/roles.ts";
import { consultar, leerDoc } from "../_shared/firestore.ts";
import { enviarAUsuarios, marcarEnviado } from "../_shared/avisos.ts";
import { getDb } from "../_shared/db.ts";

Deno.serve(manejar(async (req) => {
  const cuerpo = await leerJson<{ justificanteId?: string; accion?: string }>(req);
  const id = exigirTexto(cuerpo.justificanteId, "justificanteId", 200);
  if (cuerpo.accion !== "CREADO" && cuerpo.accion !== "RESUELTO") {
    throw new HttpError(400, "accion debe ser CREADO o RESUELTO");
  }
  const perfil = await exigirRol(req, cuerpo.accion === "CREADO" ? "TUTOR" : "ADMIN");

  const doc = await leerDoc("justificantes/" + id);
  if (!doc) throw new HttpError(404, "El justificante aún no existe en Firestore", "JUSTIFICANTE_NO_EXISTE");
  const j = doc.data;

  if (cuerpo.accion === "CREADO") {
    if (j.tutorUid !== perfil.uid) throw new HttpError(403, "Este justificante no es tuyo");

    const ruta = typeof j.evidenciaBlobPath === "string" ? j.evidenciaBlobPath : null;
    if (ruta) {
      if (!ruta.startsWith(perfil.uid + "/")) throw new HttpError(403, "La evidencia no te pertenece");
      const { data, error } = await getDb().from("archivos")
        .update({ confirmado_en: new Date().toISOString(), justificante_id: id })
        .eq("path", ruta).eq("owner_uid", perfil.uid).select("path");
      if (error) throw new Error("archivos: " + error.message);
      if (!data || data.length === 0) throw new HttpError(400, "La evidencia no se subió con este sistema", "EVIDENCIA_DESCONOCIDA");
    }

    if (!(await marcarEnviado("JUSTIFICANTE_NUEVO:" + id, "JUSTIFICANTE_NUEVO"))) return json({ ok: true, duplicado: true });
    const admins = await consultar("usuarios", [["rol", "ADMIN"], ["activo", true], ["plantelId", String(j.plantelId)]]);
    const enviados = await enviarAUsuarios(admins.map((a) => a.id), {
      tipo: "JUSTIFICANTE_NUEVO",
      titulo: "Nuevo justificante",
      cuerpo: "Hay un justificante pendiente de revisión.",
      canal: "justificantes",
      datos: { justificanteId: id, alumnoId: String(j.alumnoId) },
    });
    return json({ ok: true, enviados });
  }

  // RESUELTO
  const estado = String(j.estado);
  if (estado !== "ACEPTADO" && estado !== "RECHAZADO") throw new HttpError(409, "El justificante sigue pendiente");
  if (!(await marcarEnviado(`JUSTIFICANTE_RESUELTO:${id}:${estado}`, "JUSTIFICANTE_RESUELTO"))) {
    return json({ ok: true, duplicado: true });
  }
  const enviados = await enviarAUsuarios([String(j.tutorUid)], {
    tipo: "JUSTIFICANTE_RESUELTO",
    titulo: estado === "ACEPTADO" ? "Justificante aceptado" : "Justificante rechazado",
    cuerpo: typeof j.comentarioRevision === "string" && j.comentarioRevision !== ""
      ? j.comentarioRevision.slice(0, 140)
      : "Revisa el detalle en la aplicación.",
    canal: "justificantes",
    datos: { justificanteId: id, estado },
  });
  return json({ ok: true, enviados });
}));
