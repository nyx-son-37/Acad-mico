package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.core.OperationType
import com.example.core.handleFirestoreError
import com.example.domain.model.AiVerdict
import com.example.domain.model.Assignment
import com.example.domain.model.AssignmentAttachment
import com.example.domain.model.AssignmentStatus
import com.example.domain.model.Category
import com.example.domain.model.Course
import com.example.domain.model.Institution
import com.example.domain.model.PriorityType
import com.example.domain.model.StudyNote
import com.example.domain.model.UserProfile
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class AcademicRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun userDoc(userId: String) = db.collection("users").document(userId)
    private fun institutionsCol(userId: String) = userDoc(userId).collection("institutions")
    private fun coursesCol(userId: String) = userDoc(userId).collection("courses")
    private fun categoriesCol(userId: String) = userDoc(userId).collection("categories")
    private fun assignmentsCol(userId: String) = userDoc(userId).collection("assignments")
    private fun notesCol(userId: String) = userDoc(userId).collection("notes")
    private fun attachmentsCol(userId: String, assignmentId: String) =
        assignmentsCol(userId).document(assignmentId).collection("attachments")

    private inline fun <T> Flow<T>.handleStreamError(
        operationType: OperationType,
        path: String
    ): Flow<T> = catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        handleFirestoreError(firestoreError ?: error, operationType, path)
        throw error
    }

    // --- User Profile ---
    fun observeUserProfile(userId: String): Flow<UserProfile?> =
        userDoc(userId)
            .snapshots(MetadataChanges.EXCLUDE)
            .map { snapshot ->
                if (!snapshot.exists()) null
                else snapshot.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .handleStreamError(OperationType.GET, "users/$userId")

    suspend fun ensureUserProfileAndDefaults(userId: String, displayName: String) {
        val docRef = userDoc(userId)
        try {
            val snap = docRef.get().await()
            if (!snap.exists()) {
                val payload = mapOf(
                    "userId" to userId,
                    "displayName" to displayName.take(100),
                    "defaultPriorityMode" to PriorityType.GRADE_A.value,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
                docRef.set(payload).await()
                seedDefaultCategoriesIfEmpty(userId)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    suspend fun updateDefaultPriorityMode(userId: String, priorityMode: String) {
        val docRef = userDoc(userId)
        try {
            docRef.update(
                mapOf(
                    "defaultPriorityMode" to priorityMode,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    private suspend fun seedDefaultCategoriesIfEmpty(userId: String) {
        val existing = categoriesCol(userId).limit(1).get().await()
        if (!existing.isEmpty) return

        val defaults = listOf(
            Category(name = "Prova", iconKey = "quiz", colorHex = "#991B1B", requiresAttachmentDefault = false),
            Category(name = "Artigo / Paper", iconKey = "article", colorHex = "#1E3A5F", requiresAttachmentDefault = true),
            Category(name = "Projeto Prático", iconKey = "code", colorHex = "#0F766E", requiresAttachmentDefault = true),
            Category(name = "Seminário", iconKey = "co_present", colorHex = "#B45309", requiresAttachmentDefault = true),
            Category(name = "Lista de Exercícios", iconKey = "checklist", colorHex = "#4338CA", requiresAttachmentDefault = true),
        )
        defaults.forEach { saveCategory(userId, it) }
    }

    // --- Institutions ---
    fun observeInstitutions(userId: String): Flow<List<Institution>> =
        institutionsCol(userId)
            .snapshots(MetadataChanges.EXCLUDE)
            .map { snap ->
                snap.toObjects(Institution::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedBy { it.name.lowercase() }
            }
            .handleStreamError(OperationType.LIST, "users/$userId/institutions")

    suspend fun saveInstitution(userId: String, institution: Institution): String {
        val isNew = institution.id.isBlank()
        val docRef = if (isNew) institutionsCol(userId).document() else institutionsCol(userId).document(institution.id)
        try {
            if (isNew) {
                val payload = mapOf(
                    "userId" to userId,
                    "name" to institution.name.trim().take(100),
                    "shortName" to institution.shortName.trim().take(20),
                    "type" to institution.type,
                    "colorHex" to institution.colorHex,
                    "notes" to institution.notes.trim().take(500),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
                docRef.set(payload).await()
            } else {
                val payload = mapOf(
                    "name" to institution.name.trim().take(100),
                    "shortName" to institution.shortName.trim().take(20),
                    "type" to institution.type,
                    "colorHex" to institution.colorHex,
                    "notes" to institution.notes.trim().take(500),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
                docRef.update(payload).await()
            }
            return docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, if (isNew) OperationType.CREATE else OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteInstitution(userId: String, institutionId: String) {
        val docRef = institutionsCol(userId).document(institutionId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // --- Courses ---
    fun observeCourses(userId: String): Flow<List<Course>> =
        coursesCol(userId)
            .snapshots(MetadataChanges.EXCLUDE)
            .map { snap ->
                snap.toObjects(Course::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedBy { it.name.lowercase() }
            }
            .handleStreamError(OperationType.LIST, "users/$userId/courses")

    suspend fun saveCourse(userId: String, course: Course): String {
        val isNew = course.id.isBlank()
        val docRef = if (isNew) coursesCol(userId).document() else coursesCol(userId).document(course.id)
        try {
            if (isNew) {
                val payload = mapOf(
                    "userId" to userId,
                    "institutionId" to course.institutionId,
                    "institutionName" to course.institutionName.trim().take(100),
                    "name" to course.name.trim().take(120),
                    "code" to course.code.trim().take(30),
                    "professor" to course.professor.trim().take(80),
                    "term" to course.term.trim().take(40),
                    "targetGrade" to course.targetGrade.coerceIn(0.0, 10000.0),
                    "maxScale" to course.maxScale.coerceIn(1.0, 10000.0),
                    "colorHex" to course.colorHex,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
                docRef.set(payload).await()
            } else {
                val payload = mapOf(
                    "institutionId" to course.institutionId,
                    "institutionName" to course.institutionName.trim().take(100),
                    "name" to course.name.trim().take(120),
                    "code" to course.code.trim().take(30),
                    "professor" to course.professor.trim().take(80),
                    "term" to course.term.trim().take(40),
                    "targetGrade" to course.targetGrade.coerceIn(0.0, 10000.0),
                    "maxScale" to course.maxScale.coerceIn(1.0, 10000.0),
                    "colorHex" to course.colorHex,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
                docRef.update(payload).await()
            }
            return docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, if (isNew) OperationType.CREATE else OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteCourse(userId: String, courseId: String) {
        val docRef = coursesCol(userId).document(courseId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // --- Categories ---
    fun observeCategories(userId: String): Flow<List<Category>> =
        categoriesCol(userId)
            .snapshots(MetadataChanges.EXCLUDE)
            .map { snap ->
                snap.toObjects(Category::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedBy { it.name.lowercase() }
            }
            .handleStreamError(OperationType.LIST, "users/$userId/categories")

    suspend fun saveCategory(userId: String, category: Category): String {
        val isNew = category.id.isBlank()
        val docRef = if (isNew) categoriesCol(userId).document() else categoriesCol(userId).document(category.id)
        try {
            if (isNew) {
                val payload = mapOf(
                    "userId" to userId,
                    "name" to category.name.trim().take(60),
                    "iconKey" to category.iconKey,
                    "colorHex" to category.colorHex,
                    "requiresAttachmentDefault" to category.requiresAttachmentDefault,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
                docRef.set(payload).await()
            } else {
                val payload = mapOf(
                    "name" to category.name.trim().take(60),
                    "iconKey" to category.iconKey,
                    "colorHex" to category.colorHex,
                    "requiresAttachmentDefault" to category.requiresAttachmentDefault,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
                docRef.update(payload).await()
            }
            return docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, if (isNew) OperationType.CREATE else OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteCategory(userId: String, categoryId: String) {
        val docRef = categoriesCol(userId).document(categoryId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // --- Assignments ---
    fun observeAssignments(userId: String): Flow<List<Assignment>> =
        assignmentsCol(userId)
            .snapshots(MetadataChanges.EXCLUDE)
            .map { snap ->
                snap.toObjects(Assignment::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .handleStreamError(OperationType.LIST, "users/$userId/assignments")

    suspend fun getAssignment(userId: String, assignmentId: String): Assignment? {
        val docRef = assignmentsCol(userId).document(assignmentId)
        return try {
            docRef.get().await()
                .toObject(Assignment::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }
    }

    suspend fun saveAssignment(userId: String, assignment: Assignment): String {
        val isNew = assignment.id.isBlank()
        val docRef = if (isNew) assignmentsCol(userId).document() else assignmentsCol(userId).document(assignment.id)
        try {
            if (isNew) {
                val payload = buildMap<String, Any> {
                    put("userId", userId)
                    put("institutionId", assignment.institutionId)
                    put("courseId", assignment.courseId)
                    put("categoryId", assignment.categoryId)
                    put("title", assignment.title.trim().take(150))
                    put("description", assignment.description.trim().take(3000))
                    put("maxPoints", assignment.maxPoints.coerceIn(0.0, 10000.0))
                    assignment.earnedPoints?.let { put("earnedPoints", it.coerceIn(0.0, 10000.0)) }
                    put("dueDateMillis", assignment.dueDateMillis.coerceAtLeast(1L))
                    put("priorityType", assignment.priorityType)
                    put("reminderEnabled", assignment.reminderEnabled)
                    put("reminderHoursBefore", assignment.reminderHoursBefore.coerceIn(1L, 720L))
                    put("status", assignment.status)
                    put("requiresAttachment", assignment.requiresAttachment)
                    put("requiredAttachmentSpec", assignment.requiredAttachmentSpec.trim().take(1000))
                    put("aiVerdict", assignment.aiVerdict.ifBlank { AiVerdict.NONE.value })
                    put("aiSummary", assignment.aiSummary.take(3000))
                    put("aiEstimatedPercent", assignment.aiEstimatedPercent.coerceIn(0L, 100L))
                    put("createdAt", FieldValue.serverTimestamp())
                    put("updatedAt", FieldValue.serverTimestamp())
                }
                docRef.set(payload).await()
            } else {
                val payload = mutableMapOf<String, Any?>(
                    "institutionId" to assignment.institutionId,
                    "courseId" to assignment.courseId,
                    "categoryId" to assignment.categoryId,
                    "title" to assignment.title.trim().take(150),
                    "description" to assignment.description.trim().take(3000),
                    "maxPoints" to assignment.maxPoints.coerceIn(0.0, 10000.0),
                    "earnedPoints" to assignment.earnedPoints?.coerceIn(0.0, 10000.0),
                    "dueDateMillis" to assignment.dueDateMillis.coerceAtLeast(1L),
                    "priorityType" to assignment.priorityType,
                    "reminderEnabled" to assignment.reminderEnabled,
                    "reminderHoursBefore" to assignment.reminderHoursBefore.coerceIn(1L, 720L),
                    "status" to assignment.status,
                    "requiresAttachment" to assignment.requiresAttachment,
                    "requiredAttachmentSpec" to assignment.requiredAttachmentSpec.trim().take(1000),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
                docRef.update(payload).await()
            }
            return docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, if (isNew) OperationType.CREATE else OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun updateAssignmentScoreAndStatus(
        userId: String,
        assignmentId: String,
        earnedPoints: Double?,
        status: String
    ) {
        val docRef = assignmentsCol(userId).document(assignmentId)
        try {
            val payload = mutableMapOf<String, Any?>(
                "earnedPoints" to earnedPoints?.coerceIn(0.0, 10000.0),
                "status" to status,
                "updatedAt" to FieldValue.serverTimestamp(),
            )
            docRef.update(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun updateAssignmentAiEvaluation(
        userId: String,
        assignmentId: String,
        requiresAttachment: Boolean,
        requiredAttachmentSpec: String,
        aiVerdict: String,
        aiSummary: String,
        aiEstimatedPercent: Long
    ) {
        val docRef = assignmentsCol(userId).document(assignmentId)
        try {
            val payload = mapOf<String, Any>(
                "requiresAttachment" to requiresAttachment,
                "requiredAttachmentSpec" to requiredAttachmentSpec.trim().take(1000),
                "aiVerdict" to aiVerdict,
                "aiSummary" to aiSummary.trim().take(3000),
                "aiEstimatedPercent" to aiEstimatedPercent.coerceIn(0L, 100L),
                "updatedAt" to FieldValue.serverTimestamp(),
            )
            docRef.update(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteAssignment(userId: String, assignmentId: String) {
        val docRef = assignmentsCol(userId).document(assignmentId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // --- Attachments ---
    fun observeAttachments(userId: String, assignmentId: String): Flow<List<AssignmentAttachment>> =
        attachmentsCol(userId, assignmentId)
            .snapshots(MetadataChanges.EXCLUDE)
            .map { snap ->
                snap.toObjects(AssignmentAttachment::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .handleStreamError(OperationType.LIST, "users/$userId/assignments/$assignmentId/attachments")

    suspend fun saveAttachment(
        userId: String,
        assignmentId: String,
        attachment: AssignmentAttachment
    ): String {
        val docRef = attachmentsCol(userId, assignmentId).document()
        try {
            val payload = mapOf(
                "userId" to userId,
                "assignmentId" to assignmentId,
                "fileName" to attachment.fileName.trim().take(255),
                "mimeType" to attachment.mimeType.trim().take(120),
                "sizeBytes" to attachment.sizeBytes.coerceIn(0L, 104857600L),
                "localFilePath" to attachment.localFilePath.take(500),
                "textNote" to attachment.textNote.trim().take(3000),
                "aiStatus" to attachment.aiStatus,
                "aiFeedback" to attachment.aiFeedback.take(4000),
                "aiEstimatedPercent" to attachment.aiEstimatedPercent.coerceIn(0L, 100L),
                "aiStrengths" to attachment.aiStrengths.take(15),
                "aiGaps" to attachment.aiGaps.take(15),
                "aiNextSteps" to attachment.aiNextSteps.take(15),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
            )
            docRef.set(payload).await()
            return docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updateAttachmentAiEvaluation(
        userId: String,
        assignmentId: String,
        attachmentId: String,
        aiStatus: String,
        aiFeedback: String,
        aiEstimatedPercent: Long,
        strengths: List<String>,
        gaps: List<String>,
        nextSteps: List<String>
    ) {
        val docRef = attachmentsCol(userId, assignmentId).document(attachmentId)
        try {
            val payload = mapOf(
                "aiStatus" to aiStatus,
                "aiFeedback" to aiFeedback.trim().take(4000),
                "aiEstimatedPercent" to aiEstimatedPercent.coerceIn(0L, 100L),
                "aiStrengths" to strengths.take(15),
                "aiGaps" to gaps.take(15),
                "aiNextSteps" to nextSteps.take(15),
                "updatedAt" to FieldValue.serverTimestamp(),
            )
            docRef.update(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteAttachment(userId: String, assignmentId: String, attachmentId: String) {
        val docRef = attachmentsCol(userId, assignmentId).document(attachmentId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // --- Quick Study Notes ---
    fun observeNotes(userId: String): Flow<List<StudyNote>> =
        notesCol(userId)
            .snapshots(MetadataChanges.EXCLUDE)
            .map { snap ->
                snap.toObjects(StudyNote::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedWith(
                        compareByDescending<StudyNote> { it.pinned }
                            .thenByDescending { it.updatedAt?.seconds ?: it.createdAt?.seconds ?: 0L }
                    )
            }
            .handleStreamError(OperationType.LIST, "users/$userId/notes")

    suspend fun saveNote(userId: String, note: StudyNote): String {
        val isNew = note.id.isBlank()
        val docRef = if (isNew) notesCol(userId).document() else notesCol(userId).document(note.id)
        val cleanContent = note.content.trim().take(4000)
        val cleanTitle = note.title.trim().ifBlank {
            cleanContent.lineSequence().firstOrNull()?.take(48)?.ifBlank { "Anotação rápida" } ?: "Anotação rápida"
        }.take(120)
        try {
            if (isNew) {
                val payload = mapOf(
                    "userId" to userId,
                    "title" to cleanTitle,
                    "content" to cleanContent,
                    "courseId" to note.courseId,
                    "assignmentId" to note.assignmentId,
                    "pinned" to note.pinned,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
                docRef.set(payload).await()
            } else {
                val payload = mapOf(
                    "title" to cleanTitle,
                    "content" to cleanContent,
                    "courseId" to note.courseId,
                    "assignmentId" to note.assignmentId,
                    "pinned" to note.pinned,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
                docRef.update(payload).await()
            }
            return docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, if (isNew) OperationType.CREATE else OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun toggleNotePin(userId: String, noteId: String, pinned: Boolean) {
        val docRef = notesCol(userId).document(noteId)
        try {
            docRef.update(
                mapOf(
                    "pinned" to pinned,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteNote(userId: String, noteId: String) {
        val docRef = notesCol(userId).document(noteId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }
}
