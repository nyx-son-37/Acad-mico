package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.NoteAlt
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.AssignmentStatus
import com.example.domain.model.PriorityType
import com.example.domain.model.StudyNote
import com.example.ui.components.AssignmentCard
import com.example.ui.components.CoursePerformanceCard
import com.example.ui.components.EmptyAcademicState
import com.example.ui.components.OxfordCardShape
import com.example.ui.components.OxfordFolioRule
import com.example.ui.components.PriorityModeSelector
import com.example.ui.components.formatNumberClean
import com.example.ui.components.oxfordCardBorder
import com.example.ui.theme.academicColors
import com.example.ui.viewmodel.AcademicDashboardState

@Composable
fun DashboardScreen(
    state: AcademicDashboardState,
    userName: String,
    onSelectPriorityMode: (PriorityType) -> Unit,
    onOpenAssignment: (String) -> Unit,
    onCreateAssignment: () -> Unit,
    onSaveQuickNote: (StudyNote) -> Unit,
    onNavigateToStructure: () -> Unit,
    onNavigateToReminders: () -> Unit,
    onNavigateToProgress: () -> Unit,
    onNavigateToNotes: () -> Unit,
    onSelectCourseFilterAndOpenAssignments: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val academic = MaterialTheme.academicColors
    var showQuickNoteDialog by remember { mutableStateOf(false) }

    if (showQuickNoteDialog) {
        QuickNoteEditorDialog(
            initial = null,
            courses = state.courses,
            assignments = state.assignments,
            onDismiss = { showQuickNoteDialog = false },
            onSave = {
                onSaveQuickNote(it)
                showQuickNoteDialog = false
            }
        )
    }

    val pendingCount = state.pendingAssignmentsCount
    val attentionTotal = state.attentionCourses.size + state.attentionAssignments.size
    val prioritizedList = AcademicDashboardState.sortAssignmentsByPriority(
        state.assignments,
        state.activePriorityMode
    ).take(6)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .widthIn(max = 840.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "SALÃO DE LEITURA",
                    style = MaterialTheme.typography.labelSmall,
                    color = academic.brassAccent
                )
                Text(
                    text = "Olá, ${userName.substringBefore(" ").ifBlank { "Estudante" }}",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Visão geral do seu acervo acadêmico e entregas prioritárias.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Tactile Oxford Library Ledger Hero Card (replaces synthetic flat gradient)
        item {
            Card(
                shape = OxfordCardShape,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = oxfordCardBorder(emphasized = true),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Image(
                        painter = painterResource(
                            id = if (academic.isDark) R.drawable.img_oxford_dark_hero else R.drawable.img_oxford_light_hero
                        ),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize()
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFF1A120D).copy(alpha = 0.72f),
                                        Color(0xFF1A120D).copy(alpha = 0.88f)
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "EX LIBRIS · APROVEITAMENTO GERAL",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFE7C88B)
                                )
                                Text(
                                    text = if (state.overallGradedPossiblePoints > 0.0) {
                                        "${state.overallAveragePercent}%"
                                    } else "—",
                                    style = MaterialTheme.typography.displaySmall,
                                    color = Color(0xFFF6EFE2)
                                )
                                Text(
                                    text = if (state.overallGradedPossiblePoints > 0.0) {
                                        "${formatNumberClean(state.overallEarnedPoints)} de ${formatNumberClean(state.overallGradedPossiblePoints)} pts avaliados · ${state.overallCompletionPercent}% concluído"
                                    } else {
                                        "Registre notas nos trabalhos para acompanhar sua média"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFE7DDD0)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MiniKpiPill(
                                label = "Instituições",
                                value = state.institutions.size.toString(),
                                modifier = Modifier.weight(1f)
                            )
                            MiniKpiPill(
                                label = "Disciplinas",
                                value = state.courses.size.toString(),
                                modifier = Modifier.weight(1f)
                            )
                            MiniKpiPill(
                                label = "Pendentes",
                                value = pendingCount.toString(),
                                modifier = Modifier.weight(1f)
                            )
                            MiniKpiPill(
                                label = "Notas",
                                value = state.notes.size.toString(),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // Quick Shortcuts: Painel de Progresso + Anotações Rápidas
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    onClick = onNavigateToProgress,
                    shape = OxfordCardShape,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        1.dp,
                        if (attentionTotal > 0) academic.priorityB.copy(alpha = 0.5f)
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("dashboard_open_progress_card")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = if (attentionTotal > 0) Icons.Rounded.WarningAmber else Icons.Rounded.Insights,
                            contentDescription = null,
                            tint = if (attentionTotal > 0) academic.priorityB else MaterialTheme.colorScheme.primary
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Progresso",
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1
                            )
                            Text(
                                text = if (attentionTotal > 0) "$attentionTotal em atenção" else "${state.overallCompletionPercent}% entregue",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (attentionTotal > 0) academic.priorityB else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Surface(
                    onClick = onNavigateToNotes,
                    shape = OxfordCardShape,
                    color = MaterialTheme.colorScheme.surface,
                    border = oxfordCardBorder(),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("dashboard_open_notes_card")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.NoteAlt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Anotações",
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1
                            )
                            Text(
                                text = "${state.notes.size} fichas salvas",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // Instant "Anotar Agora" quick bar
        item {
            OutlinedButton(
                onClick = { showQuickNoteDialog = true },
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dashboard_quick_note_button")
            ) {
                Icon(Icons.Rounded.NoteAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.padding(4.dp))
                Text("Escrever Anotação Rápida")
            }
        }

        item { OxfordFolioRule() }

        // Priority Mode Switcher (Prioridade A = Nota vs Prioridade B = Data de entrega)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Ordenação & Lembretes",
                        style = MaterialTheme.typography.titleMedium
                    )
                    TextButton(onClick = onNavigateToReminders) {
                        Icon(
                            Icons.Rounded.NotificationsActive,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.padding(2.dp))
                        Text("Lembretes")
                    }
                }
                PriorityModeSelector(
                    activeMode = state.activePriorityMode,
                    onSelectMode = onSelectPriorityMode
                )
                Text(
                    text = state.activePriorityMode.descriptionPt,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Quick Onboarding if no institutions/courses yet
        if (state.institutions.isEmpty() || state.courses.isEmpty()) {
            item {
                Card(
                    shape = OxfordCardShape,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.68f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AccountBalance,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                            Text(
                                text = "Configure sua estrutura acadêmica",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                        Text(
                            text = "Comece cadastrando sua Instituição (ex: USP, PUC, Oxford) e suas Disciplinas para vincular trabalhos, categorias e metas de nota.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Button(
                            onClick = onNavigateToStructure,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("setup_academic_structure_button")
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.MenuBook, contentDescription = null)
                            Spacer(modifier = Modifier.padding(4.dp))
                            Text("Cadastrar Instituições e Disciplinas")
                        }
                    }
                }
            }
        }

        // Course Performance Strip
        if (state.coursePerformances.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Pontuação por Disciplina",
                            style = MaterialTheme.typography.titleMedium
                        )
                        TextButton(onClick = onNavigateToProgress) {
                            Text("Ver Painel")
                        }
                    }
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.coursePerformances, key = { it.course.id }) { perf ->
                            CoursePerformanceCard(
                                performance = perf,
                                onClick = { onSelectCourseFilterAndOpenAssignments(perf.course.id) }
                            )
                        }
                    }
                }
            }
        }

        // Prioritized Assignments Queue
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (state.activePriorityMode == PriorityType.GRADE_A) {
                        "Foco Prioridade A (Maior Pontuação)"
                    } else {
                        "Foco Prioridade B (Prazo mais Próximo)"
                    },
                    style = MaterialTheme.typography.titleMedium
                )
                if (state.courses.isNotEmpty()) {
                    FilledTonalButton(
                        onClick = onCreateAssignment,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.padding(2.dp))
                        Text("Trabalho")
                    }
                }
            }
        }

        if (prioritizedList.isEmpty()) {
            item {
                EmptyAcademicState(
                    icon = Icons.Rounded.AutoAwesome,
                    title = "Nenhum trabalho cadastrado ainda",
                    description = "Crie seu primeiro trabalho, escolha uma categoria personalizada e use o Assistente de IA para exigir e avaliar anexos.",
                    actionLabel = if (state.courses.isNotEmpty()) "Criar primeiro trabalho" else "Ir para Instituições e Disciplinas",
                    onAction = if (state.courses.isNotEmpty()) onCreateAssignment else onNavigateToStructure
                )
            }
        } else {
            items(prioritizedList, key = { it.id }) { assignment ->
                AssignmentCard(
                    assignment = assignment,
                    institution = state.institutionById[assignment.institutionId],
                    course = state.courseById[assignment.courseId],
                    category = state.categoryById[assignment.categoryId],
                    onClick = { onOpenAssignment(assignment.id) }
                )
            }
        }

        item { Spacer(modifier = Modifier.height(72.dp)) }
    }
}

@Composable
private fun MiniKpiPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xFFFAF5EC).copy(alpha = 0.14f),
        border = BorderStroke(1.dp, Color(0xFFE7C88B).copy(alpha = 0.28f)),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFF6EFE2)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFE2D7C6),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
