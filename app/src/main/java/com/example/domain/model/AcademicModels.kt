package com.example.domain.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val defaultPriorityMode: String = PriorityType.GRADE_A.value,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

enum class InstitutionType(val value: String, val labelPt: String) {
    UNIVERSITY("university", "Universidade"),
    COLLEGE("college", "Faculdade"),
    SCHOOL("school", "Escola / Médio"),
    COURSE("course", "Curso / Plataforma"),
    OTHER("other", "Outro");

    companion object {
        fun fromValue(value: String): InstitutionType =
            entries.firstOrNull { it.value == value } ?: UNIVERSITY
    }
}

@IgnoreExtraProperties
data class Institution(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val shortName: String = "",
    val type: String = InstitutionType.UNIVERSITY.value,
    val colorHex: String = "#1E3A5F",
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class Course(
    @DocumentId val id: String = "",
    val userId: String = "",
    val institutionId: String = "",
    val institutionName: String = "",
    val name: String = "",
    val code: String = "",
    val professor: String = "",
    val term: String = "",
    val targetGrade: Double = 7.0,
    val maxScale: Double = 10.0,
    val colorHex: String = "#0F766E",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class Category(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val iconKey: String = "article",
    val colorHex: String = "#B45309",
    val requiresAttachmentDefault: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

enum class PriorityType(val value: String, val labelPt: String, val descriptionPt: String) {
    GRADE_A(
        "grade_a",
        "Prioridade A · Nota",
        "Prioriza o impacto na média e o peso da pontuação"
    ),
    DEADLINE_B(
        "deadline_b",
        "Prioridade B · Entrega",
        "Prioriza a urgência da data e hora de entrega"
    );

    companion object {
        fun fromValue(value: String): PriorityType =
            entries.firstOrNull { it.value == value } ?: GRADE_A
    }
}

enum class AssignmentStatus(val value: String, val labelPt: String) {
    PENDING("pending", "Pendente"),
    IN_PROGRESS("in_progress", "Em andamento"),
    SUBMITTED("submitted", "Entregue"),
    GRADED("graded", "Avaliado");

    companion object {
        fun fromValue(value: String): AssignmentStatus =
            entries.firstOrNull { it.value == value } ?: PENDING
    }
}

enum class AiVerdict(val value: String, val labelPt: String) {
    NONE("none", "Sem avaliação IA"),
    APPROVED("approved", "Anexo aprovado"),
    NEEDS_REVISION("needs_revision", "Precisa de ajustes"),
    MISSING_REQUIREMENTS("missing_requirements", "Requisitos pendentes"),
    REJECTED("rejected", "Inadequado ao enunciado");

    companion object {
        fun fromValue(value: String): AiVerdict =
            entries.firstOrNull { it.value == value } ?: NONE
    }
}

@IgnoreExtraProperties
data class Assignment(
    @DocumentId val id: String = "",
    val userId: String = "",
    val institutionId: String = "",
    val courseId: String = "",
    val categoryId: String = "",
    val title: String = "",
    val description: String = "",
    val maxPoints: Double = 10.0,
    val earnedPoints: Double? = null,
    val dueDateMillis: Long = 0L,
    val priorityType: String = PriorityType.GRADE_A.value,
    val reminderEnabled: Boolean = true,
    val reminderHoursBefore: Long = 24L,
    val status: String = AssignmentStatus.PENDING.value,
    val requiresAttachment: Boolean = true,
    val requiredAttachmentSpec: String = "",
    val aiVerdict: String = AiVerdict.NONE.value,
    val aiSummary: String = "",
    val aiEstimatedPercent: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class AssignmentAttachment(
    @DocumentId val id: String = "",
    val userId: String = "",
    val assignmentId: String = "",
    val fileName: String = "",
    val mimeType: String = "",
    val sizeBytes: Long = 0L,
    val localFilePath: String = "",
    val textNote: String = "",
    val aiStatus: String = "pending",
    val aiFeedback: String = "",
    val aiEstimatedPercent: Long = 0L,
    val aiStrengths: List<String> = emptyList(),
    val aiGaps: List<String> = emptyList(),
    val aiNextSteps: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class StudyNote(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val content: String = "",
    val courseId: String = "",
    val assignmentId: String = "",
    val pinned: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

data class CoursePerformance(
    val course: Course,
    val earnedPoints: Double,
    val gradedPossiblePoints: Double,
    val totalPlannedPoints: Double,
    val totalAssignments: Int,
    val completedAssignments: Int,
    val gradedAssignments: Int,
    val pendingAssignments: Int,
    val overdueAssignments: Int,
    val pendingRequiredAttachments: Int,
) {
    val averagePercent: Int
        get() = if (gradedPossiblePoints > 0.0) {
            ((earnedPoints / gradedPossiblePoints) * 100.0).toInt().coerceIn(0, 100)
        } else 0

    val completionPercent: Int
        get() = if (totalAssignments > 0) {
            ((completedAssignments.toDouble() / totalAssignments.toDouble()) * 100.0).toInt().coerceIn(0, 100)
        } else 0

    val projectedOnScale: Double
        get() = if (gradedPossiblePoints > 0.0) {
            (earnedPoints / gradedPossiblePoints) * course.maxScale
        } else 0.0

    val isBelowTarget: Boolean
        get() = gradedPossiblePoints > 0.0 && projectedOnScale < course.targetGrade

    val attentionReasons: List<String>
        get() = buildList {
            if (isBelowTarget) {
                add("Média atual abaixo da meta (${String.format("%.1f", projectedOnScale)} / ${String.format("%.1f", course.targetGrade)})")
            }
            if (overdueAssignments > 0) {
                add("$overdueAssignments trabalho(s) atrasado(s)")
            }
            if (pendingRequiredAttachments > 0) {
                add("$pendingRequiredAttachments anexo(s) exigido(s) pendente(s)")
            }
        }

    val needsAttention: Boolean
        get() = attentionReasons.isNotEmpty()
}

data class AssignmentAttentionItem(
    val assignment: Assignment,
    val reasons: List<String>,
    val urgencyLevel: Int // 3 = critical (overdue/rejected), 2 = high (soon/missing attachment), 1 = medium (high grade weight)
)
