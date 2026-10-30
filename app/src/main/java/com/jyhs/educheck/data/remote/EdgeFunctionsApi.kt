package com.jyhs.educheck.data.remote

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.POST

/** Supabase Edge Functions. Every call carries the Firebase ID token (see NetworkModule). */
interface EdgeFunctionsApi {
    @POST("create-user")
    suspend fun createUser(@Body body: CreateUserRequest): CreateUserResponse

    @POST("set-user-active")
    suspend fun setUserActive(@Body body: SetUserActiveRequest): OkResponse

    @POST("set-role")
    suspend fun setRole(@Body body: SetRoleRequest): OkResponse
}

@Serializable
data class CreateUserRequest(
    val email: String,
    val password: String,
    val name: String,
    val role: String,
    val studentId: String? = null,
)

@Serializable
data class CreateUserResponse(val ok: Boolean, val uid: String)

@Serializable
data class SetUserActiveRequest(val uid: String, val active: Boolean)

@Serializable
data class SetRoleRequest(val uid: String, val role: String)

@Serializable
data class OkResponse(val ok: Boolean)

@Serializable
data class EdgeErrorBody(val error: String? = null)
