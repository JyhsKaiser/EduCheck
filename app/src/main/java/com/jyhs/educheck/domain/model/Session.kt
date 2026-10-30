package com.jyhs.educheck.domain.model

data class Session(
    val uid: String,
    val email: String,
    val role: Role,
)
