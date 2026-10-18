package com.jyhs.educheck.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.jyhs.educheck.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Firebase se usa para Authentication, Firestore y FCM (sin Storage ni Cloud Functions). */
@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    // 10.0.2.2 es el host de la computadora vista desde el emulador de Android.
    private const val HOST_EMULADOR = "10.0.2.2"

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        val auth = FirebaseAuth.getInstance()
        if (BuildConfig.USE_FIREBASE_EMULATOR) auth.useEmulator(HOST_EMULADOR, 9099)
        return auth
    }

    @Provides
    @Singleton
    fun provideFirestore(): FirebaseFirestore {
        val db = FirebaseFirestore.getInstance()
        if (BuildConfig.USE_FIREBASE_EMULATOR) db.useEmulator(HOST_EMULADOR, 8080)
        return db
    }
}
