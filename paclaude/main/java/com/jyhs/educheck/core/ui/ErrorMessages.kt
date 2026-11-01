package com.jyhs.educheck.core.ui

import androidx.annotation.StringRes
import com.jyhs.educheck.R
import com.jyhs.educheck.core.common.ErrorReason

@StringRes
fun ErrorReason.messageRes(): Int = when (this) {
    ErrorReason.NO_NETWORK -> R.string.error_no_network
    ErrorReason.INVALID_CREDENTIALS -> R.string.error_invalid_credentials
    ErrorReason.NO_PROFILE -> R.string.error_no_profile
    ErrorReason.ACCOUNT_INACTIVE -> R.string.error_account_inactive
    ErrorReason.EMAIL_ALREADY_EXISTS -> R.string.error_email_exists
    ErrorReason.WEAK_PASSWORD -> R.string.error_weak_password
    ErrorReason.INVALID_INPUT -> R.string.error_invalid_input
    ErrorReason.FORBIDDEN -> R.string.error_forbidden
    ErrorReason.NOT_FOUND -> R.string.error_not_found
    ErrorReason.ALREADY_LINKED -> R.string.error_already_linked
    ErrorReason.INVALID_TIME -> R.string.error_invalid_time
    ErrorReason.SCHEDULE_CONFLICT -> R.string.error_schedule_conflict
    ErrorReason.HAS_DEPENDENTS -> R.string.error_has_dependents
    ErrorReason.DUPLICATE -> R.string.error_duplicate
    ErrorReason.LIMIT_EXCEEDED -> R.string.error_limit_exceeded
    ErrorReason.PERMISSION_DENIED -> R.string.error_permission_denied
    ErrorReason.LOCATION_UNAVAILABLE -> R.string.error_location_unavailable
    ErrorReason.WIFI_UNAVAILABLE -> R.string.error_wifi_unavailable
    ErrorReason.FILE_UNREADABLE -> R.string.error_file_unreadable
    ErrorReason.UNKNOWN -> R.string.error_unknown
}
