package com.jyhs.educheck.data.remote

import android.util.Log
import com.jyhs.educheck.core.common.AppResult
import com.jyhs.educheck.core.common.ErrorReason
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.serialization.json.Json
import retrofit2.HttpException

/** Runs an Edge Function call and turns HTTP and network failures into an [AppResult]. */
suspend fun <T> edgeCall(json: Json, block: suspend () -> T): AppResult<T> {
    return try {
        AppResult.Ok(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: HttpException) {
        AppResult.Error(reasonFrom(e, json))
    } catch (e: IOException) {
        AppResult.Error(ErrorReason.NO_NETWORK)
    } catch (e: Exception) {
        Log.e("EdgeCall", "Unexpected error calling an Edge Function", e)
        AppResult.Error(ErrorReason.UNKNOWN)
    }
}

private fun reasonFrom(e: HttpException, json: Json): ErrorReason {
    val code = try {
        e.response()?.errorBody()?.string()?.let { json.decodeFromString<EdgeErrorBody>(it).error }
    } catch (_: Exception) {
        null
    }
    return when (code) {
        "email_exists" -> ErrorReason.EMAIL_ALREADY_EXISTS
        "weak_password" -> ErrorReason.WEAK_PASSWORD
        "student_already_linked" -> ErrorReason.ALREADY_LINKED
        "user_not_found", "student_not_found", "not_found" -> ErrorReason.NOT_FOUND
        "forbidden", "missing_token", "invalid_token" -> ErrorReason.FORBIDDEN
        "internal_error", null -> ErrorReason.UNKNOWN
        else -> if (e.code() in 400..499) ErrorReason.INVALID_INPUT else ErrorReason.UNKNOWN
    }
}
