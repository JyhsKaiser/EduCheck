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
    ErrorReason.UNKNOWN -> R.string.error_unknown
}
