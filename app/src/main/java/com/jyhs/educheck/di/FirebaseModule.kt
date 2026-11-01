package com.jyhs.educheck.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.MemoryCacheSettings
import com.jyhs.educheck.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Firebase is used for Authentication, Firestore and FCM (no Storage, no Cloud Functions). */
@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    // 10.0.2.2 is the development machine as seen from the Android emulator.
    private const val EMULATOR_HOST = "10.0.2.2"

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        val auth = FirebaseAuth.getInstance()
        if (BuildConfig.USE_FIREBASE_EMULATOR) auth.useEmulator(EMULATOR_HOST, 9099)
        return auth
    }

    @Provides
    @Singleton
    fun provideFirestore(): FirebaseFirestore {
        val db = FirebaseFirestore.getInstance()
        // Room is the only local store of the app: Firestore keeps just an in-memory cache.
        // (Set before useEmulator, which builds on top of these settings.)
        db.firestoreSettings = FirebaseFirestoreSettings.Builder(db.firestoreSettings)
            .setLocalCacheSettings(MemoryCacheSettings.newBuilder().build())
            .build()
        if (BuildConfig.USE_FIREBASE_EMULATOR) db.useEmulator(EMULATOR_HOST, 8080)
        return db
    }
}
