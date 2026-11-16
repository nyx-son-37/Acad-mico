package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.AssignmentTurnedIn
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.PendingActions
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.model.AssignmentAttentionItem
import com.example.domain.model.AssignmentStatus
import com.example.domain.model.CoursePerformance
import com.example.ui.components.EmptyAcademicState
import com.example.ui.components.OxfordCardShape
import com.example.ui.components.OxfordChipShape
import com.example.ui.components.OxfordFolioRule
import com.example.ui.components.formatDueDateShort
import com.example.ui.components.formatNumberClean
import com.example.ui.components.oxfordCardBorder
import com.example.ui.theme.academicColors
import com.example.ui.theme.parseHexColor
import com.example.ui.viewmodel.AcademicDashboardState

@Composable
fun ProgressScreen(
    state: AcademicDashboardState,
    onOpenAssignment: (String) -> Unit,
    onOpenCourseAssignments: (String) -> Unit,
    onNavigateToStructure: () -> Unit,
    modifier: Modifier = Modifier
) {
    val academic = MaterialTheme.academicColors
    val totalAssignments = state.assignments.size
    val gradedCount = state.assignments.count { it.status == AssignmentStatus.GRADED.value }
    val submittedCount = state.assignments.count { it.status == AssignmentStatus.SUBMITTED.value }
    val inProgressCount = state.assignments.count { it.status == AssignmentStatus.IN_PROGRESS.value }
    val pendingCount = state.assignments.count { it.status == AssignmentStatus.PENDING.value }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .widthIn(max = 840.dp)
            .testTag("progress_screen_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "LIVRO DE DESEMPENHO",
                    style = MaterialTheme.typography.labelSmall,
                    color = academic.brassAccent
                )
                Text(
                    text = "Painel de Progresso",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = "Métricas consolidadas por disciplina, entregas concluídas e alertas de atenção.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Visual General Progress Summary Card
        item {
            OverallProgressVisualCard(
                averagePercent = state.overallAveragePercent,
                completionPercent = state.overallCompletionPercent,
                earnedPoints = state.overallEarnedPoints,
                gradedPossiblePoints = state.overallGradedPossiblePoints,
                completedCount = state.completedAssignmentsCount,
                pendingCount = state.pendingAssignmentsCount,
                totalCount = totalAssignments,
                gradedCount = gradedCount,
                submittedCount = submittedCount,
                inProgressCount = inProgressCount,
                pendingOnlyCount = pendingCount
            )
        }

        // Special Attention Section (Courses + Tasks)
        val hasAttention = state.attentionCourses.isNotEmpty() || state.attentionAssignments.isNotEmpty()
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (hasAttention) Icons.Rounded.WarningAmber else Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = if (hasAttention) academic.priorityB else academic.approved
                )
                Text(
                    text = "Requer Atenção Especial",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        if (!hasAttention) {
            item {
                Card(
                    shape = OxfordCardShape,
                    colors = CardDefaults.cardColors(containerColor = academic.approvedSoft.copy(alpha = 0.72f)),
                    border = BorderStroke(1.dp, academic.approved.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = academic.approved)
                        Column {
                            Text(
                                text = "Tudo sob controle!",
                                style = MaterialTheme.typography.titleSmall,
                                color = academic.approved
                            )
                            Text(
                                text = "Nenhuma disciplina abaixo da meta ou tarefa crítica pendente no momento.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        } else {
            items(state.attentionCourses, key = { "att_course_${it.course.id}" }) { perf ->
                AttentionCourseCard(
                    performance = perf,
                    onClick = { onOpenCourseAssignments(perf.course.id) }
                )
            }

            items(state.attentionAssignments.take(5), key = { "att_assign_${it.assignment.id}" }) { item ->
                AttentionAssignmentCard(
                    item = item,
                    courseName = state.courseById[item.assignment.courseId]?.name ?: "Disciplina",
                    onClick = { onOpenAssignment(item.assignment.id) }
                )
            }
        }

        item { OxfordFolioRule() }

        // Per-Course Detailed Performance Metrics
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Desempenho por Disciplina (${state.coursePerformances.size})",
                    style = MaterialTheme.typography.titleMedium
                )
                TextButton(onClick = onNavigateToStructure) {
                    Text("Disciplinas")
                }
            }
        }

        if (state.coursePerformances.isEmpty()) {
            item {
                EmptyAcademicState(
                    icon = Icons.Rounded.School,
                    title = "Nenhuma disciplina cadastrada",
                    description = "Cadastre suas instituições e disciplinas para acompanhar notas, conclusões e metas de aprovação.",
                    actionLabel = "Configurar Disciplinas",
                    onAction = onNavigateToStructure
                )
            }
        } else {
            items(state.coursePerformances, key = { "detail_course_${it.course.id}" }) { perf ->
                CourseProgressDetailCard(
                    performance = perf,
                    onClick = { onOpenCourseAssignments(perf.course.id) }
                )
            }
        }

        item { Spacer(modifier = Modifier.height(72.dp)) }
    }
}

@Composable
private fun OverallProgressVisualCard(
    averagePercent: Int,
    completionPercent: Int,
    earnedPoints: Double,
    gradedPossiblePoints: Double,
    completedCount: Int,
    pendingCount: Int,
    totalCount: Int,
    gradedCount: Int,
    submittedCount: Int,
    inProgressCount: Int,
    pendingOnlyCount: Int
) {
    val academic = MaterialTheme.academicColors
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    Card(
        shape = OxfordCardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = oxfordCardBorder(emphasized = true),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Dual concentric ring visual indicator
                Box(
                    modifier = Modifier.size(116.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeOuter = 10.dp.toPx()
                        val strokeInner = 7.dp.toPx()
                        val padOuter = strokeOuter / 2f
                        drawArc(
                            color = trackColor,
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = Offset(padOuter, padOuter),
                            size = Size(size.width - strokeOuter, size.height - strokeOuter),
                            style = Stroke(width = strokeOuter, cap = StrokeCap.Round)
                        )
                        drawArc(
                            color = primaryColor,
                            startAngle = -90f,
                            sweepAngle = (completionPercent / 100f) * 360f,
                            useCenter = false,
                            topLeft = Offset(padOuter, padOuter),
                            size = Size(size.width - strokeOuter, size.height - strokeOuter),
                            style = Stroke(width = strokeOuter, cap = StrokeCap.Round)
                        )

                        val inset = strokeOuter + 8.dp.toPx()
                        val innerSize = (size.width - inset * 2f).coerceAtLeast(10f)
                        drawArc(
                            color = trackColor.copy(alpha = 0.7f),
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = Offset(inset, inset),
                            size = Size(innerSize, innerSize),
                            style = Stroke(width = strokeInner, cap = StrokeCap.Round)
                        )
                        drawArc(
                            color = secondaryColor,
                            startAngle = -90f,
                            sweepAngle = (averagePercent / 100f) * 360f,
                            useCenter = false,
                            topLeft = Offset(inset, inset),
                            size = Size(innerSize, innerSize),
                            style = Stroke(width = strokeInner, cap = StrokeCap.Round)
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$completionPercent%",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = primaryColor
                        )
                        Text(
                            text = "concluído",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Resumo Geral",
                        style = MaterialTheme.typography.titleMedium
                    )
                    LegendMetricRow(
                        dotColor = primaryColor,
                        label = "Entregas concluídas",
                        value = "$completedCount de $totalCount"
                    )
                    LegendMetricRow(
                        dotColor = secondaryColor,
                        label = "Aproveitamento notas",
                        value = if (gradedPossiblePoints > 0.0) {
                            "$averagePercent% (${formatNumberClean(earnedPoints)}/${formatNumberClean(gradedPossiblePoints)})"
                        } else "Sem notas"
                    )
                    LegendMetricRow(
                        dotColor = academic.priorityA,
                        label = "Trabalhos pendentes",
                        value = "$pendingCount"
                    )
                }
            }

            if (totalCount > 0) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Distribuição dos Trabalhos",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(9.dp)
                            .clip(CircleShape)
                            .background(trackColor),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        if (gradedCount > 0) {
                            Box(
                                modifier = Modifier
                                    .weight(gradedCount.toFloat())
                                    .height(9.dp)
                                    .background(academic.approved)
                            )
                        }
                        if (submittedCount > 0) {
                            Box(
                                modifier = Modifier
                                    .weight(submittedCount.toFloat())
                                    .height(9.dp)
                                    .background(secondaryColor)
                            )
                        }
                        if (inProgressCount > 0) {
                            Box(
                                modifier = Modifier
                                    .weight(inProgressCount.toFloat())
                                    .height(9.dp)
                                    .background(academic.priorityA)
                            )
                        }
                        if (pendingOnlyCount > 0) {
                            Box(
                                modifier = Modifier
                                    .weight(pendingOnlyCount.toFloat())
                                    .height(9.dp)
                                    .background(academic.priorityB.copy(alpha = 0.78f))
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Avaliados: $gradedCount", style = MaterialTheme.typography.labelSmall, color = academic.approved)
                        Text("Entregues: $submittedCount", style = MaterialTheme.typography.labelSmall, color = secondaryColor)
                        Text("Andamento: $inProgressCount", style = MaterialTheme.typography.labelSmall, color = academic.priorityA)
                        Text("Pendentes: $pendingOnlyCount", style = MaterialTheme.typography.labelSmall, color = academic.priorityB)
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendMetricRow(
    dotColor: Color,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun AttentionCourseCard(
    performance: CoursePerformance,
    onClick: () -> Unit
) {
    val academic = MaterialTheme.academicColors
    Card(
        onClick = onClick,
        shape = OxfordCardShape,
        colors = CardDefaults.cardColors(containerColor = academic.priorityBSoft.copy(alpha = 0.65f)),
        border = BorderStroke(1.dp, academic.priorityB.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Disciplina: ${performance.course.name}",
                    style = MaterialTheme.typography.titleSmall,
                    color = academic.priorityB
                )
                Text(
                    text = performance.course.institutionName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            performance.attentionReasons.forEach { reason ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ErrorOutline,
                        contentDescription = null,
                        tint = academic.priorityB,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = reason,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun AttentionAssignmentCard(
    item: AssignmentAttentionItem,
    courseName: String,
    onClick: () -> Unit
) {
    val academic = MaterialTheme.academicColors
    val isCritical = item.urgencyLevel >= 3
    val bg = if (isCritical) academic.priorityBSoft.copy(alpha = 0.65f) else academic.revisionSoft.copy(alpha = 0.72f)
    val fg = if (isCritical) academic.priorityB else academic.revision

    Card(
        onClick = onClick,
        shape = OxfordCardShape,
        colors = CardDefaults.cardColors(containerColor = bg),
        border = BorderStroke(1.dp, fg.copy(alpha = 0.38f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.assignment.title,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = formatDueDateShort(item.assignment.dueDateMillis),
                    style = MaterialTheme.typography.labelSmall,
                    color = fg
                )
            }
            Text(
                text = "$courseName · ${item.reasons.joinToString(" • ")}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun CourseProgressDetailCard(
    performance: CoursePerformance,
    onClick: () -> Unit
) {
    val academic = MaterialTheme.academicColors
    val accent = parseHexColor(performance.course.colorHex, MaterialTheme.colorScheme.primary)
    val gradeRatio = (performance.averagePercent / 100f).coerceIn(0f, 1f)
    val completionRatio = (performance.completionPercent / 100f).coerceIn(0f, 1f)
    val statusColor = when {
        performance.needsAttention -> academic.priorityB
        performance.gradedPossiblePoints > 0.0 -> academic.approved
        else -> accent
    }

    Card(
        onClick = onClick,
        shape = OxfordCardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            width = 1.dp,
            color = if (performance.needsAttention) academic.priorityB.copy(alpha = 0.45f)
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.58f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("progress_course_card_${performance.course.id}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = performance.course.institutionName,
                        style = MaterialTheme.typography.labelSmall,
                        color = accent
                    )
                    Text(
                        text = performance.course.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (performance.course.professor.isNotBlank() || performance.course.term.isNotBlank()) {
                        Text(
                            text = listOf(performance.course.professor, performance.course.term)
                                .filter { it.isNotBlank() }
                                .joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    color = statusColor.copy(alpha = 0.14f),
                    shape = OxfordChipShape
                ) {
                    Text(
                        text = if (performance.gradedPossiblePoints > 0.0) {
                            "${formatNumberClean(performance.projectedOnScale)} / ${formatNumberClean(performance.course.maxScale)}"
                        } else {
                            "Meta ${formatNumberClean(performance.course.targetGrade)}"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricBox(
                    icon = Icons.Rounded.AssignmentTurnedIn,
                    label = "Concluídos",
                    value = "${performance.completedAssignments}/${performance.totalAssignments}",
                    tint = academic.approved,
                    modifier = Modifier.weight(1f)
                )
                MetricBox(
                    icon = Icons.Rounded.PendingActions,
                    label = "Pendentes",
                    value = "${performance.pendingAssignments}",
                    tint = if (performance.overdueAssignments > 0) academic.priorityB else academic.priorityA,
                    modifier = Modifier.weight(1f)
                )
                MetricBox(
                    icon = Icons.AutoMirrored.Rounded.TrendingUp,
                    label = "Notas",
                    value = if (performance.gradedPossiblePoints > 0.0) {
                        "${formatNumberClean(performance.earnedPoints)}/${formatNumberClean(performance.gradedPossiblePoints)}"
                    } else "—",
                    tint = accent,
                    modifier = Modifier.weight(1f)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Desempenho em notas (meta ${formatNumberClean(performance.course.targetGrade)})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${performance.averagePercent}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor
                    )
                }
                LinearProgressIndicator(
                    progress = { gradeRatio },
                    color = statusColor,
                    trackColor = statusColor.copy(alpha = 0.16f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Progresso de entregas",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${performance.completionPercent}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = accent
                    )
                }
                LinearProgressIndicator(
                    progress = { completionRatio },
                    color = accent,
                    trackColor = accent.copy(alpha = 0.16f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape)
                )
            }

            if (performance.attentionReasons.isNotEmpty()) {
                Surface(
                    color = academic.priorityBSoft.copy(alpha = 0.65f),
                    shape = OxfordChipShape,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Atenção: ${performance.attentionReasons.joinToString(" • ")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = academic.priorityB,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricBox(
    icon: ImageVector,
    label: String,
    value: String,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(14.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
