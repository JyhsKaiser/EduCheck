package com.jyhs.educheck.di

import com.jyhs.educheck.data.device.AndroidWifiProvider
import com.jyhs.educheck.data.device.ContentResolverTextFileReader
import com.jyhs.educheck.data.device.FusedLocationProvider
import com.jyhs.educheck.data.firebase.FirestoreAssignmentRepository
import com.jyhs.educheck.data.firebase.FirestoreCampusRepository
import com.jyhs.educheck.data.firebase.FirestoreDeviceRepository
import com.jyhs.educheck.data.firebase.FirestoreGradeRepository
import com.jyhs.educheck.data.firebase.FirestoreGroupRepository
import com.jyhs.educheck.data.firebase.FirestoreScheduleRepository
import com.jyhs.educheck.data.firebase.FirestoreStudentRepository
import com.jyhs.educheck.data.firebase.FirestoreSubjectRepository
import com.jyhs.educheck.data.firebase.FirestoreUserAdminRepository
import com.jyhs.educheck.domain.repository.AssignmentRepository
import com.jyhs.educheck.domain.repository.CampusRepository
import com.jyhs.educheck.domain.repository.DeviceRepository
import com.jyhs.educheck.domain.repository.GradeRepository
import com.jyhs.educheck.domain.repository.GroupRepository
import com.jyhs.educheck.domain.repository.LocationProvider
import com.jyhs.educheck.domain.repository.ScheduleRepository
import com.jyhs.educheck.domain.repository.StudentRepository
import com.jyhs.educheck.domain.repository.SubjectRepository
import com.jyhs.educheck.domain.repository.TextFileReader
import com.jyhs.educheck.domain.repository.WifiProvider
import com.jyhs.educheck.domain.repository.UserAdminRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AdminRepositoryModule {
    @Binds @Singleton
    abstract fun bindUserAdmin(impl: FirestoreUserAdminRepository): UserAdminRepository

    @Binds @Singleton
    abstract fun bindStudents(impl: FirestoreStudentRepository): StudentRepository

    @Binds @Singleton
    abstract fun bindDevices(impl: FirestoreDeviceRepository): DeviceRepository

    @Binds @Singleton
    abstract fun bindSubjects(impl: FirestoreSubjectRepository): SubjectRepository

    @Binds @Singleton
    abstract fun bindGroups(impl: FirestoreGroupRepository): GroupRepository

    @Binds @Singleton
    abstract fun bindAssignments(impl: FirestoreAssignmentRepository): AssignmentRepository

    @Binds @Singleton
    abstract fun bindSchedules(impl: FirestoreScheduleRepository): ScheduleRepository

    @Binds @Singleton
    abstract fun bindCampus(impl: FirestoreCampusRepository): CampusRepository

    @Binds @Singleton
    abstract fun bindGrades(impl: FirestoreGradeRepository): GradeRepository

    @Binds @Singleton
    abstract fun bindLocation(impl: FusedLocationProvider): LocationProvider

    @Binds @Singleton
    abstract fun bindWifi(impl: AndroidWifiProvider): WifiProvider

    @Binds @Singleton
    abstract fun bindTextFileReader(impl: ContentResolverTextFileReader): TextFileReader
}
