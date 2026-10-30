package com.jyhs.educheck.data.firebase

import com.google.firebase.firestore.DocumentSnapshot
import com.jyhs.educheck.domain.model.GroupMember

/** groups/{id}.students is a list of maps: [{id, name, studentNumber}]. */
internal fun GroupMember.toMap(): Map<String, Any> = mapOf(
    "id" to id,
    "name" to name,
    "studentNumber" to studentNumber,
)

internal fun DocumentSnapshot.groupMembers(): List<GroupMember> =
    (get(FirestoreFields.GROUP_STUDENTS) as? List<*>).orEmpty().mapNotNull { item ->
        val map = item as? Map<*, *> ?: return@mapNotNull null
        val memberId = map["id"] as? String ?: return@mapNotNull null
        GroupMember(
            id = memberId,
            name = map["name"] as? String ?: "",
            studentNumber = map["studentNumber"] as? String ?: "",
        )
    }
