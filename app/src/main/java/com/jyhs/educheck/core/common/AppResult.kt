package com.jyhs.educheck.core.common

/** Result of an operation that can fail for a known reason. */
sealed interface AppResult<out T> {
    data class Ok<out T>(val value: T) : AppResult<T>
    data class Error(val reason: ErrorReason, val detail: String? = null) : AppResult<Nothing>
}

enum class ErrorReason {
    NO_NETWORK,
    INVALID_CREDENTIALS,
    NO_PROFILE,
    ACCOUNT_INACTIVE,
    EMAIL_ALREADY_EXISTS,
    WEAK_PASSWORD,
    INVALID_INPUT,
    FORBIDDEN,
    NOT_FOUND,
    ALREADY_LINKED,
    INVALID_TIME,
    SCHEDULE_CONFLICT,
    HAS_DEPENDENTS,
    DUPLICATE,
    LIMIT_EXCEEDED,
    PERMISSION_DENIED,
    LOCATION_UNAVAILABLE,
    WIFI_UNAVAILABLE,
    FILE_UNREADABLE,
    UNKNOWN,
}

inline fun <T, R> AppResult<T>.map(transform: (T) -> R): AppResult<R> = when (this) {
    is AppResult.Ok -> AppResult.Ok(transform(value))
    is AppResult.Error -> this
}
