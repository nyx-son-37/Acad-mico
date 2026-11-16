package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.domain.model.Assignment
import com.example.domain.model.AssignmentAttachment
import com.example.domain.model.Course
import com.example.domain.model.Institution
import com.example.domain.model.PriorityType
import com.example.domain.model.StudyNote
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class AcademicRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun fullAcademicChain_authenticatedOwner_createsAndObservesSuccessfully() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = AcademicRepository(firestore)

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.ensureUserProfileAndDefaults(aliceUid, "Alice")
        }

        val categories = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeCategories(aliceUid).first { it.isNotEmpty() }
        }
        val categoryId = categories.first().id

        val instId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.saveInstitution(
                aliceUid,
                Institution(name = "Universidade de São Paulo", shortName = "USP", type = "university")
            )
        }
        assertTrue(instId.isNotBlank())

        val courseId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.saveCourse(
                aliceUid,
                Course(
                    institutionId = instId,
                    institutionName = "USP",
                    name = "Engenharia de Software",
                    code = "MAC0332",
                    targetGrade = 7.0,
                    maxScale = 10.0
                )
            )
        }
        assertTrue(courseId.isNotBlank())

        val assignmentId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.saveAssignment(
                aliceUid,
                Assignment(
                    institutionId = instId,
                    courseId = courseId,
                    categoryId = categoryId,
                    title = "Relatório de Arquitetura",
                    description = "Enviar diagrama e análise de requisitos em PDF",
                    maxPoints = 4.0,
                    dueDateMillis = System.currentTimeMillis() + 172_800_000L,
                    priorityType = PriorityType.GRADE_A.value,
                    requiresAttachment = true,
                    requiredAttachmentSpec = "PDF com diagrama de componentes"
                )
            )
        }
        assertTrue(assignmentId.isNotBlank())

        val observedAssignments = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeAssignments(aliceUid).first { list -> list.any { it.id == assignmentId } }
        }
        assertEquals(1, observedAssignments.count { it.id == assignmentId })

        val attId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.saveAttachment(
                aliceUid,
                assignmentId,
                AssignmentAttachment(
                    assignmentId = assignmentId,
                    fileName = "arquitetura.pdf",
                    mimeType = "application/pdf",
                    sizeBytes = 10240L
                )
            )
        }
        assertTrue(attId.isNotBlank())

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.updateAttachmentAiEvaluation(
                userId = aliceUid,
                assignmentId = assignmentId,
                attachmentId = attId,
                aiStatus = "approved",
                aiFeedback = "Excelente diagrama.",
                aiEstimatedPercent = 95L,
                strengths = listOf("Clareza estrutural"),
                gaps = emptyList(),
                nextSteps = listOf("Revisar legenda")
            )
        }

        val observedAttachments = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeAttachments(aliceUid, assignmentId)
                .first { list -> list.any { it.id == attId && it.aiStatus == "approved" } }
        }
        assertEquals(95L, observedAttachments.first { it.id == attId }.aiEstimatedPercent)

        val noteId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.saveNote(
                aliceUid,
                StudyNote(
                    title = "Lembrar diagrama C4",
                    content = "Incluir camadas de contexto e container no PDF.",
                    courseId = courseId,
                    assignmentId = assignmentId,
                    pinned = true
                )
            )
        }
        assertTrue(noteId.isNotBlank())

        val observedNotes = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeNotes(aliceUid).first { list -> list.any { it.id == noteId } }
        }
        assertTrue(observedNotes.first { it.id == noteId }.pinned)
    }

    @Test
    fun crossUserAccess_bobCannotReadOrWriteAliceData() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = AcademicRepository(firestore)
        val instId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.saveInstitution(
                aliceUid,
                Institution(name = "PUC", shortName = "PUC", type = "university")
            )
        }
        assertTrue(instId.isNotBlank())

        signInTestUser(BOB_EMAIL)
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeInstitutions(aliceUid).first()
            }
            fail("Expected PERMISSION_DENIED for cross-user read")
        } catch (e: Exception) {
            val firestoreEx = checkNotNull(e.unwrapFirestoreException()) {
                "Expected nested FirebaseFirestoreException, got $e"
            }
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx.code)
        }
    }

    @Test
    fun unauthenticatedUser_cannotObserveAssignments() = runBlocking {
        auth.signOut()
        val repository = AcademicRepository(firestore)
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeAssignments("alice_unauth").first()
            }
            fail("Expected PERMISSION_DENIED for unauthenticated caller")
        } catch (e: Exception) {
            val firestoreEx = checkNotNull(e.unwrapFirestoreException()) {
                "Expected nested FirebaseFirestoreException, got $e"
            }
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx.code)
        }
    }

    private companion object {
        const val ALICE_EMAIL = "alice.academico@test.com"
        const val BOB_EMAIL = "bob.academico@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
