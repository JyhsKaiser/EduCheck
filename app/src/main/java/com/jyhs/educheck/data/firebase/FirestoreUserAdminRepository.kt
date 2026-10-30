package com.jyhs.educheck.data.firebase

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.jyhs.educheck.core.common.AppResult
import com.jyhs.educheck.core.common.map
import com.jyhs.educheck.data.remote.CreateUserRequest
import com.jyhs.educheck.data.remote.EdgeFunctionsApi
import com.jyhs.educheck.data.remote.SetUserActiveRequest
import com.jyhs.educheck.data.remote.edgeCall
import com.jyhs.educheck.domain.model.AppUser
import com.jyhs.educheck.domain.model.NewUser
import com.jyhs.educheck.domain.model.Role
import com.jyhs.educheck.domain.repository.UserAdminRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

@Singleton
class FirestoreUserAdminRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val api: EdgeFunctionsApi,
    private val json: Json,
) : UserAdminRepository {

    override fun observeUsers(): Flow<List<AppUser>> =
        firestore.collection(FirestoreCollections.USERS)
            .orderBy(FirestoreFields.NAME)
            .limit(MAX_USERS)
            .asFlow()
            .map { snapshot -> snapshot.documents.mapNotNull { it.toAppUser() } }

    override suspend fun createUser(newUser: NewUser): AppResult<String> =
        edgeCall(json) {
            api.createUser(
                CreateUserRequest(
                    email = newUser.email,
                    password = newUser.password,
                    name = newUser.name,
                    role = newUser.role.name,
                    studentId = newUser.studentId,
                ),
            )
        }.map { it.uid }

    override suspend fun setUserActive(uid: String, active: Boolean): AppResult<Unit> =
        edgeCall(json) { api.setUserActive(SetUserActiveRequest(uid, active)) }.map { }

    private fun DocumentSnapshot.toAppUser(): AppUser? {
        val role = getString(FirestoreFields.ROLE)?.let { name -> Role.entries.firstOrNull { it.name == name } }
            ?: return null
        return AppUser(
            uid = id,
            name = getString(FirestoreFields.NAME).orEmpty(),
            email = getString(FirestoreFields.EMAIL).orEmpty(),
            role = role,
            active = getBoolean(FirestoreFields.ACTIVE) ?: false,
            studentId = getString(FirestoreFields.STUDENT_ID),
        )
    }

    private companion object {
        const val MAX_USERS = 200L
    }
}
