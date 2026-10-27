package com.jyhs.educheck.data.firebase

/** Nombres de colecciones de Firestore (ver docs/MODELO_FIRESTORE.md). */
object FirestoreColecciones {
    const val USUARIOS = "usuarios"
    const val PLANTELES = "planteles"
    const val DISPOSITIVOS = "dispositivos"
    const val ALUMNOS = "alumnos"
    const val SECRETOS = "secretos"            // subcolección de alumnos
    const val PRESENCIAS = "presencias"
    const val AUSENCIAS = "ausencias"          // la escribe el backend
    const val JUSTIFICANTES = "justificantes"
    const val GRUPOS = "grupos"
    const val MATERIAS = "materias"
    const val ASIGNACIONES = "asignaciones"
    const val HORARIOS = "horarios"
    const val SESIONES = "sesiones"
    const val CALIFICACIONES = "calificaciones"
    const val EMERGENCIAS = "emergencias"
    const val NOTIFICACIONES = "notificaciones"
}

/** Campos usados en más de un lugar. El resto se agrega junto con cada módulo. */
object FirestoreCampos {
    // usuarios/{uid}
    const val ROL = "rol"
    const val PLANTEL_ID = "plantelId"
    const val ACTIVO = "activo"
    const val ALUMNO_ID = "alumnoId"

    // estados y métodos
    const val ESTADO = "estado"
    const val FECHA = "fecha"
    const val TUTOR_UIDS = "tutorUids"
}

/** Identificadores deterministas: evitan duplicados (una presencia por alumno y día, una sesión por horario y día). */
object FirestoreIds {
    fun presencia(alumnoId: String, fecha: String): String = alumnoId + "_" + fecha
    fun sesion(horarioId: String, fecha: String): String = horarioId + "_" + fecha
    fun calificacion(asignacionId: String, alumnoId: String): String = asignacionId + "_" + alumnoId
}
