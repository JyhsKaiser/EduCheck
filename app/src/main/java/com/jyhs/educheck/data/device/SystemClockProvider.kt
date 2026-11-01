package com.jyhs.educheck.data.device

import android.content.Context
import android.provider.Settings
import com.jyhs.educheck.domain.repository.ClockProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SystemClockProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) : ClockProvider {

    override fun nowMs(): Long = System.currentTimeMillis()

    override fun isAutomatic(): Boolean =
        Settings.Global.getInt(context.contentResolver, Settings.Global.AUTO_TIME, 0) == 1
}
