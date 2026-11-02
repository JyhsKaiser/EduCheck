package com.jyhs.educheck.data.push

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.jyhs.educheck.domain.repository.PushTokenScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkManagerPushTokenScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) : PushTokenScheduler {

    override fun requestRegistration() = enqueue(context)

    companion object {
        /** Also used by the FCM service, which is not injected. */
        fun enqueue(context: Context) {
            val request = OneTimeWorkRequestBuilder<PushTokenWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(PushTokenWorker.UNIQUE_NAME, ExistingWorkPolicy.REPLACE, request)
        }
    }
}
