package com.jyhs.educheck.core.common

/** Resultado de una operación que puede fallar por una causa conocida. */
sealed interface Resultado<out T> {
    data class Ok<out T>(val valor: T) : Resultado<T>
    data class Error(val motivo: MotivoError) : Resultado<Nothing>
}

enum class MotivoError {
    SIN_RED,
    CREDENCIALES,
    SIN_PERFIL,
    DESCONOCIDO,
}
