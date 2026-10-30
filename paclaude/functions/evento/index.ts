// POST { tipo: "ENTRADA" | "SALIDA", presenciaId } · ALUMNO (dueño de la presencia) o ADMIN (registro por QR)
// La app escribe la presencia en Firestore (las reglas la validan) y después llama aquí para
// avisar a los tutores. La función comprueba el documento real: no se fía del cliente.
import { exigirTexto, HttpError, json, leerJson, manejar } from "../_shared/http.ts";
import { exigirRol } from "../_shared/roles.ts";
import { getDb } from "../_shared/db.ts";
import { borrarDoc, leerDoc } from "../_shared/firestore.ts";
import { enviarAUsuarios, marcarEnviado } from "../_shared/avisos.ts";
import { hhmmAMinutos, horaTexto, minutosLocales } from "../_shared/tiempo.ts";
import { zonaPlantel } from "../_shared/env.ts";

Deno.serve(manejar(async (req) => {
  const perfil = await exigirRol(req, "ALUMNO", "ADMIN");
  const cuerpo = await leerJson<{ tipo?: string; presenciaId?: string }>(req);
  if (cuerpo.tipo !== "ENTRADA" && cuerpo.tipo !== "SALIDA") {
    throw new HttpError(400, "tipo debe ser ENTRADA o SALIDA");
  }
  const presenciaId = exigirTexto(cuerpo.presenciaId, "presenciaId", 200);

  const presencia = await leerDoc("presencias/" + presenciaId);
  if (!presencia) {
    throw new HttpError(404, "La presencia aún no existe en Firestore", "PRESENCIA_NO_EXISTE");
  }
  const p = presencia.data;
  if (perfil.rol === "ALUMNO" && p.alumnoUid !== perfil.uid) throw new HttpError(403, "La presencia no es tuya");
  if (p.estado !== "PRESENTE") throw new HttpError(409, "La presencia no está en estado PRESENTE");

  const alumnoId = String(p.alumnoId);
  const fecha = String(p.fecha);
  const tutores = Array.isArray(p.tutorUids) ? (p.tutorUids as string[]) : [];
  const alumno = await leerDoc("alumnos/" + alumnoId);
  const nombre = alumno ? `${alumno.data.nombres} ${alumno.data.apellidos}` : "El alumno";
  const zona = zonaPlantel();
  const db = getDb();

  // ------------------------------------------------------------------ ENTRADA
  if (cuerpo.tipo === "ENTRADA") {
    if (!(await marcarEnviado(`ENTRADA:${alumnoId}:${fecha}`, "ENTRADA"))) {
      return json({ ok: true, duplicado: true });
    }
    const horaEntrada = p.horaEntrada instanceof Date ? p.horaEntrada : new Date();
    const { error } = await db.from("estado_dia").upsert({
      alumno_id: alumnoId,
      fecha,
      alumno_uid: typeof p.alumnoUid === "string" ? p.alumnoUid : null,
      plantel_id: String(p.plantelId),
      grupo_id: alumno && alumno.data.grupoId ? String(alumno.data.grupoId) : null,
      nombre,
      tutor_uids: tutores,
      entrada_en: horaEntrada.toISOString(),
      ultimo_contacto: new Date().toISOString(),
    }, { onConflict: "alumno_id,fecha" });
    if (error) throw new Error("estado_dia: " + error.message);

    // Si el sistema ya lo había marcado ausente, se corrige
    const ausencia = await leerDoc(`ausencias/${alumnoId}_${fecha}`);
    if (ausencia) await borrarDoc(`ausencias/${alumnoId}_${fecha}`);

    const enviados = await enviarAUsuarios(tutores, ausencia
      ? {
        tipo: "CORRECCION_ASISTENCIA",
        titulo: "Asistencia corregida",
        cuerpo: `${nombre} sí llegó al plantel a las ${horaTexto(horaEntrada, zona)}.`,
        canal: "ingresos",
        datos: { alumnoId, fecha },
      }
      : {
        tipo: "INGRESO_ALUMNO",
        titulo: "Llegada al plantel",
        cuerpo: `${nombre} llegó al plantel a las ${horaTexto(horaEntrada, zona)}.`,
        canal: "ingresos",
        datos: { alumnoId, fecha },
      });
    return json({ ok: true, enviados, correccion: Boolean(ausencia) });
  }

  // ------------------------------------------------------------------ SALIDA
  const horaSalida = p.horaSalida instanceof Date ? p.horaSalida : null;
  if (!horaSalida) throw new HttpError(409, "La presencia no tiene hora de salida");
  if (!(await marcarEnviado(`SALIDA:${alumnoId}:${fecha}:${horaSalida.getTime()}`, "SALIDA"))) {
    return json({ ok: true, duplicado: true });
  }
  await db.from("estado_dia").update({ salida_en: horaSalida.toISOString() })
    .eq("alumno_id", alumnoId).eq("fecha", fecha);

  // Solo se avisa si la salida ocurre dentro del horario de clases del grupo
  const grupoId = alumno && alumno.data.grupoId ? String(alumno.data.grupoId) : null;
  if (!grupoId) return json({ ok: true, enviados: 0, motivo: "sin_grupo" });
  const { data: h, error: errorH } = await db.from("horarios_dia").select("inicio,fin,zona_horaria")
    .eq("fecha", fecha).eq("grupo_id", grupoId).maybeSingle();
  if (errorH) throw new Error("horarios_dia: " + errorH.message);
  if (!h) return json({ ok: true, enviados: 0, motivo: "sin_horario" });
  const minutos = minutosLocales(horaSalida, String(h.zona_horaria));
  if (minutos < hhmmAMinutos(String(h.inicio)) || minutos > hhmmAMinutos(String(h.fin))) {
    return json({ ok: true, enviados: 0, motivo: "fuera_de_horario" });
  }
  const enviados = await enviarAUsuarios(tutores, {
    tipo: "SALIDA_PLANTEL",
    titulo: "Salida del plantel",
    cuerpo: `${nombre} salió del plantel a las ${horaTexto(horaSalida, zona)}.`,
    canal: "ingresos",
    datos: { alumnoId, fecha },
  });
  return json({ ok: true, enviados });
}));
