package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Grading
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PresentToAll
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.model.AiVerdict
import com.example.domain.model.Assignment
import com.example.domain.model.AssignmentStatus
import com.example.domain.model.Category
import com.example.domain.model.Course
import com.example.domain.model.CoursePerformance
import com.example.domain.model.Institution
import com.example.domain.model.PriorityType
import com.example.ui.theme.academicColors
import com.example.ui.theme.parseHexColor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val ptBrLocale: Locale = Locale.forLanguageTag("pt-BR")

fun categoryIconForKey(key: String): ImageVector = when (key) {
    "quiz" -> Icons.Rounded.Quiz
    "code" -> Icons.Rounded.Code
    "science" -> Icons.Rounded.Science
    "co_present" -> Icons.Rounded.PresentToAll
    "checklist" -> Icons.AutoMirrored.Rounded.Grading
    "folder" -> Icons.Rounded.Folder
    else -> Icons.Rounded.Description
}

fun formatNumberClean(value: Double): String {
    return if (value % 1.0 == 0.0) value.toInt().toString()
    else String.format(ptBrLocale, "%.1f", value)
}

fun formatDueDateShort(millis: Long): String {
    if (millis <= 0L) return "Sem data"
    return SimpleDateFormat("dd/MM · HH:mm", ptBrLocale).format(Date(millis))
}

fun formatCountdownLabel(millis: Long, status: String): Pair<String, Boolean> {
    if (status == AssignmentStatus.GRADED.value) return "Avaliado" to false
    if (status == AssignmentStatus.SUBMITTED.value) return "Entregue" to false
    val diff = millis - System.currentTimeMillis()
    if (diff < 0L) {
        val hoursLate = (-diff / 3_600_000L).coerceAtLeast(1L)
        return if (hoursLate >= 24L) "Atrasado ${hoursLate / 24}d" to true
        else "Atrasado ${hoursLate}h" to true
    }
    val hours = diff / 3_600_000L
    return when {
        hours < 1L -> "Menos de 1h" to true
        hours < 24L -> "Em ${hours}h" to true
        hours < 48L -> "Amanhã" to false
        else -> "Em ${hours / 24}d" to false
    }
}

@Composable
fun PriorityModeSelector(
    activeMode: PriorityType,
    onSelectMode: (PriorityType) -> Unit,
    modifier: Modifier = Modifier
) {
    val academic = MaterialTheme.academicColors
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            PriorityType.entries.forEach { mode ->
                val selected = mode == activeMode
                val bgColor by animateColorAsState(
                    targetValue = if (selected) {
                        if (mode == PriorityType.GRADE_A) academic.priorityA else academic.priorityB
                    } else Color.Transparent,
                    label = "priorityTabBg"
                )
                val contentColor = if (selected) {
                    if (academic.isDark) Color(0xFF1A130D) else Color(0xFFFAF5EC)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
                val icon = if (mode == PriorityType.GRADE_A) Icons.AutoMirrored.Rounded.Grading else Icons.Rounded.Schedule

                Surface(
                    color = bgColor,
                    shape = RoundedCornerShape(9.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(9.dp))
                        .clickable { onSelectMode(mode) }
                        .testTag("priority_mode_${mode.value}")
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = contentColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (mode == PriorityType.GRADE_A) "Prioridade A · Nota" else "Prioridade B · Entrega",
                            style = MaterialTheme.typography.labelMedium,
                            color = contentColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CoursePerformanceCard(
    performance: CoursePerformance,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = parseHexColor(performance.course.colorHex, MaterialTheme.colorScheme.secondary)
    val progress = (performance.averagePercent / 100f).coerceIn(0f, 1f)

    Card(
        onClick = onClick,
        shape = OxfordCardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = oxfordCardBorder(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
            .width(232.dp)
            .testTag("course_perf_card_${performance.course.id}")
    ) {
        Column(
            modifier = Modifier.padding(15.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    color = accent.copy(alpha = 0.14f),
                    shape = OxfordChipShape
                ) {
                    Text(
                        text = performance.course.institutionName,
                        style = MaterialTheme.typography.labelSmall,
                        color = accent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Text(
                    text = "${performance.pendingAssignments} pend.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column {
                Text(
                    text = performance.course.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (performance.course.code.isNotBlank()) {
                    Text(
                        text = performance.course.code,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = if (performance.gradedPossiblePoints > 0.0) {
                            "Média ${formatNumberClean(performance.projectedOnScale)} / ${formatNumberClean(performance.course.maxScale)}"
                        } else {
                            "Meta: ${formatNumberClean(performance.course.targetGrade)}"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${performance.averagePercent}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = accent,
                        fontWeight = FontWeight.Bold
                    )
                }
                LinearProgressIndicator(
                    progress = { progress },
                    color = accent,
                    trackColor = accent.copy(alpha = 0.16f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(CircleShape)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AssignmentCard(
    assignment: Assignment,
    institution: Institution?,
    course: Course?,
    category: Category?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val academic = MaterialTheme.academicColors
    val isGradeA = assignment.priorityType == PriorityType.GRADE_A.value
    val priorityBg = if (isGradeA) academic.priorityASoft else academic.priorityBSoft
    val priorityFg = if (isGradeA) academic.priorityA else academic.priorityB
    val (countdown, isUrgent) = formatCountdownLabel(assignment.dueDateMillis, assignment.status)
    val courseColor = course?.let { parseHexColor(it.colorHex, MaterialTheme.colorScheme.primary) }
        ?: MaterialTheme.colorScheme.primary
    val catColor = category?.let { parseHexColor(it.colorHex, MaterialTheme.colorScheme.tertiary) }
        ?: MaterialTheme.colorScheme.tertiary

    Card(
        onClick = onClick,
        shape = OxfordCardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            width = 1.dp,
            color = if (isUrgent && assignment.status != AssignmentStatus.GRADED.value) {
                academic.priorityB.copy(alpha = 0.45f)
            } else {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("assignment_card_${assignment.id}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 4.dp, height = 14.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(courseColor)
                    )
                    Text(
                        text = buildString {
                            append(institution?.shortName?.ifBlank { institution.name } ?: "Instituição")
                            append(" · ")
                            append(course?.name ?: "Disciplina")
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    color = priorityBg,
                    shape = OxfordChipShape
                ) {
                    Text(
                        text = if (isGradeA) "A · ${formatNumberClean(assignment.maxPoints)} pts" else "B · $countdown",
                        style = MaterialTheme.typography.labelSmall,
                        color = priorityFg,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Text(
                text = assignment.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (category != null) {
                    Surface(
                        color = catColor.copy(alpha = 0.12f),
                        shape = OxfordChipShape
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = categoryIconForKey(category.iconKey),
                                contentDescription = null,
                                tint = catColor,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = category.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = catColor
                            )
                        }
                    }
                }

                // Due date chip
                Surface(
                    color = if (isUrgent) academic.priorityBSoft else MaterialTheme.colorScheme.surfaceVariant,
                    shape = OxfordChipShape
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Event,
                            contentDescription = null,
                            tint = if (isUrgent) academic.priorityB else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = formatDueDateShort(assignment.dueDateMillis),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isUrgent) academic.priorityB else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Points chip
                val scoreLabel = if (assignment.earnedPoints != null) {
                    "Nota: ${formatNumberClean(assignment.earnedPoints)} / ${formatNumberClean(assignment.maxPoints)}"
                } else {
                    "Vale ${formatNumberClean(assignment.maxPoints)} pts"
                }
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                    shape = OxfordChipShape
                ) {
                    Text(
                        text = scoreLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                if (assignment.reminderEnabled) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
                        shape = OxfordChipShape
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.NotificationsActive,
                                contentDescription = "Lembrete ativo",
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "-${assignment.reminderHoursBefore}h",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }

                if (assignment.requiresAttachment || assignment.aiVerdict != AiVerdict.NONE.value) {
                    AiVerdictChip(verdict = AiVerdict.fromValue(assignment.aiVerdict), requiresAttachment = assignment.requiresAttachment)
                }
            }
        }
    }
}

@Composable
fun AiVerdictChip(
    verdict: AiVerdict,
    requiresAttachment: Boolean,
    modifier: Modifier = Modifier
) {
    val academic = MaterialTheme.academicColors
    val (bg, fg, icon, text) = when (verdict) {
        AiVerdict.APPROVED -> Quad(academic.approvedSoft, academic.approved, Icons.Rounded.CheckCircle, "IA: Aprovado")
        AiVerdict.NEEDS_REVISION -> Quad(academic.revisionSoft, academic.revision, Icons.Rounded.WarningAmber, "IA: Ajustar")
        AiVerdict.REJECTED -> Quad(academic.priorityBSoft, academic.priorityB, Icons.Rounded.ErrorOutline, "IA: Inadequado")
        AiVerdict.MISSING_REQUIREMENTS -> Quad(academic.revisionSoft, academic.revision, Icons.Rounded.AttachFile, "Anexo exigido")
        AiVerdict.NONE -> if (requiresAttachment) {
            Quad(MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer, Icons.Rounded.AutoAwesome, "Exige anexo IA")
        } else {
            Quad(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant, Icons.Rounded.AutoAwesome, "IA pronta")
        }
    }

    Surface(
        color = bg,
        shape = OxfordChipShape,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = fg, modifier = Modifier.size(13.dp))
            Text(text = text, style = MaterialTheme.typography.labelSmall, color = fg)
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
fun EmptyAcademicState(
    icon: ImageVector = Icons.Rounded.School,
    title: String,
    description: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        shape = OxfordCardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = oxfordCardBorder(),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f),
                shape = CircleShape,
                modifier = Modifier.size(54.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Button(onClick = onAction, shape = RoundedCornerShape(10.dp)) {
                    Text(actionLabel)
                }
            }
        }
    }
}

@Composable
fun InstitutionBadge(
    institution: Institution,
    modifier: Modifier = Modifier
) {
    val color = parseHexColor(institution.colorHex, MaterialTheme.colorScheme.primary)
    Surface(
        color = color.copy(alpha = 0.14f),
        shape = OxfordChipShape,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.AccountBalance,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = institution.shortName.ifBlank { institution.name },
                style = MaterialTheme.typography.labelMedium,
                color = color
            )
        }
    }
}
