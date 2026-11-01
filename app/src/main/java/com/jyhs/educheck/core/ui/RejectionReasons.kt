package com.jyhs.educheck.core.ui

import androidx.annotation.StringRes
import com.jyhs.educheck.R
import com.jyhs.educheck.core.common.ErrorReason

/** Short reason (in the middle of a sentence) why a queued entry could not be sent. */
@StringRes
fun ErrorReason.rejectionReasonRes(): Int = when (this) {
    ErrorReason.DEVICE_NOT_APPROVED -> R.string.reason_device_not_approved
    ErrorReason.OUTSIDE_CAMPUS -> R.string.reason_outside_campus
    ErrorReason.ENTRY_EXPIRED -> R.string.reason_entry_expired
    ErrorReason.FORBIDDEN -> R.string.reason_forbidden
    else -> R.string.reason_unknown
}
