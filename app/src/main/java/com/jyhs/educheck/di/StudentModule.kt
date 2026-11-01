package com.jyhs.educheck.di

import com.jyhs.educheck.data.device.AndroidConnectivityProvider
import com.jyhs.educheck.data.device.SystemClockProvider
import com.jyhs.educheck.data.firebase.FirestoreMyDeviceRepository
import com.jyhs.educheck.data.firebase.FirestorePresenceRepository
import com.jyhs.educheck.data.firebase.FirestoreStudentSelfRepository
import com.jyhs.educheck.data.geofence.WorkManagerPresenceTracker
import com.jyhs.educheck.data.local.RoomPresenceOutbox
import com.jyhs.educheck.data.push.LocalEntryNotifier
import com.jyhs.educheck.data.sync.WorkManagerSyncScheduler
import com.jyhs.educheck.domain.repository.ClockProvider
import com.jyhs.educheck.domain.repository.ConnectivityProvider
import com.jyhs.educheck.domain.repository.EntryNotifier
import com.jyhs.educheck.domain.repository.MyDeviceRepository
import com.jyhs.educheck.domain.repository.PresenceOutbox
import com.jyhs.educheck.domain.repository.PresenceRepository
import com.jyhs.educheck.domain.repository.PresenceTracker
import com.jyhs.educheck.domain.repository.StudentSelfRepository
import com.jyhs.educheck.domain.repository.SyncScheduler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class StudentModule {
    @Binds @Singleton
    abstract fun bindStudentSelf(impl: FirestoreStudentSelfRepository): StudentSelfRepository

    @Binds @Singleton
    abstract fun bindMyDevice(impl: FirestoreMyDeviceRepository): MyDeviceRepository

    @Binds @Singleton
    abstract fun bindPresence(impl: FirestorePresenceRepository): PresenceRepository

    @Binds @Singleton
    abstract fun bindClock(impl: SystemClockProvider): ClockProvider

    @Binds @Singleton
    abstract fun bindNotifier(impl: LocalEntryNotifier): EntryNotifier

    @Binds @Singleton
    abstract fun bindTracker(impl: WorkManagerPresenceTracker): PresenceTracker

    @Binds @Singleton
    abstract fun bindOutbox(impl: RoomPresenceOutbox): PresenceOutbox

    @Binds @Singleton
    abstract fun bindConnectivity(impl: AndroidConnectivityProvider): ConnectivityProvider

    @Binds @Singleton
    abstract fun bindSyncScheduler(impl: WorkManagerSyncScheduler): SyncScheduler
}
