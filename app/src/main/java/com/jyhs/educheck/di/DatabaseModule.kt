package com.jyhs.educheck.di

import android.content.Context
import androidx.room.Room
import com.jyhs.educheck.data.local.dao.CacheDao
import com.jyhs.educheck.data.local.dao.PresenceDao
import com.jyhs.educheck.data.local.db.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "educheck.db")
            // Development stage: a schema change rebuilds the local database (no data worth migrating yet).
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun providePresenceDao(db: AppDatabase): PresenceDao = db.presenceDao()

    @Provides
    fun provideCacheDao(db: AppDatabase): CacheDao = db.cacheDao()
}
