package com.jyhs.educheck.core.ui

import androidx.annotation.StringRes
import com.jyhs.educheck.R
import com.jyhs.educheck.domain.model.Shift

@StringRes
fun Shift.labelRes(): Int = when (this) {
    Shift.MORNING -> R.string.shift_morning
    Shift.AFTERNOON -> R.string.shift_afternoon
}
