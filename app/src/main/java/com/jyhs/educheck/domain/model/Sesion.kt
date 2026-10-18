package com.jyhs.educheck.domain.model

data class Sesion(
    val uid: String,
    val email: String,
    val rol: Rol,
    val plantelId: String?,
)
