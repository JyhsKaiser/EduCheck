package com.jyhs.educheck.di

import com.jyhs.educheck.data.firebase.FirestorePushTokenRepository
import com.jyhs.educheck.data.push.FirebasePushTokenProvider
import com.jyhs.educheck.data.push.WorkManagerPushTokenScheduler
import com.jyhs.educheck.domain.repository.PushTokenProvider
import com.jyhs.educheck.domain.repository.PushTokenRepository
import com.jyhs.educheck.domain.repository.PushTokenScheduler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PushModule {
    @Binds @Singleton
    abstract fun bindTokenProvider(impl: FirebasePushTokenProvider): PushTokenProvider

    @Binds @Singleton
    abstract fun bindTokenRepository(impl: FirestorePushTokenRepository): PushTokenRepository

    @Binds @Singleton
    abstract fun bindTokenScheduler(impl: WorkManagerPushTokenScheduler): PushTokenScheduler
}
