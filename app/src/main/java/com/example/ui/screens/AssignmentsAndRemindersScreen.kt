package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Assignment
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.FilterListOff
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.domain.model.PriorityType
import com.example.notifications.ReminderScheduler
import com.example.ui.components.AssignmentCard
import com.example.ui.components.EmptyAcademicState
import com.example.ui.components.OxfordCardShape
import com.example.ui.components.OxfordChipShape
import com.example.ui.components.PriorityModeSelector
import com.example.ui.components.formatCountdownLabel
import com.example.ui.components.formatDueDateShort
import com.example.ui.components.formatNumberClean
import com.example.ui.components.oxfordCardBorder
import com.example.ui.theme.academicColors
import com.example.ui.viewmodel.AcademicDashboardState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignmentsAndRemindersScreen(
    state: AcademicDashboardState,
    initialSubTab: Int = 0,
    onSelectPriorityMode: (PriorityType) -> Unit,
    onSelectInstitutionFilter: (String?) -> Unit,
    onSelectCourseFilter: (String?) -> Unit,
    onSelectCategoryFilter: (String?) -> Unit,
    onOpenAssignment: (String) -> Unit,
    onCreateAssignment: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val academic = MaterialTheme.academicColors
    var subTab by remember(initialSubTab) { mutableIntStateOf(initialSubTab) }

    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasNotificationPermission = granted }
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .widthIn(max = 840.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Trabalhos & Lembretes", style = MaterialTheme.typography.headlineMedium)
                if (state.courses.isNotEmpty()) {
                    Button(
                        onClick = onCreateAssignment,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.padding(2.dp))
                        Text("Novo")
                    }
                }
            }

            PrimaryTabRow(
                selectedTabIndex = subTab,
                containerColor = Color.Transparent
            ) {
                Tab(
                    selected = subTab == 0,
                    onClick = { subTab = 0 },
                    text = { Text("Trabalhos (${state.filteredAssignments.size})") }
                )
                Tab(
                    selected = subTab == 1,
                    onClick = { subTab = 1 },
                    text = { Text("Lembretes (${state.reminderQueue.size})") }
                )
            }

            PriorityModeSelector(
                activeMode = state.activePriorityMode,
                onSelectMode = onSelectPriorityMode
            )
        }

        if (subTab == 0) {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (state.selectedInstitutionFilterId != null ||
                                state.selectedCourseFilterId != null ||
                                state.selectedCategoryFilterId != null
                            ) {
                                TextButton(onClick = {
                                    onSelectInstitutionFilter(null)
                                    onSelectCourseFilter(null)
                                    onSelectCategoryFilter(null)
                                }) {
                                    Icon(Icons.Rounded.FilterListOff, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.padding(2.dp))
                                    Text("Limpar")
                                }
                            }
                            state.institutions.forEach { inst ->
                                FilterChip(
                                    selected = state.selectedInstitutionFilterId == inst.id,
                                    onClick = {
                                        onSelectInstitutionFilter(
                                            if (state.selectedInstitutionFilterId == inst.id) null else inst.id
                                        )
                                    },
                                    label = { Text(inst.shortName.ifBlank { inst.name }) }
                                )
                            }
                            state.courses.forEach { course ->
                                FilterChip(
                                    selected = state.selectedCourseFilterId == course.id,
                                    onClick = {
                                        onSelectCourseFilter(
                                            if (state.selectedCourseFilterId == course.id) null else course.id
                                        )
                                    },
                                    label = { Text(course.name) }
                                )
                            }
                        }

                        if (state.categories.isNotEmpty()) {
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                state.categories.forEach { cat ->
                                    FilterChip(
                                        selected = state.selectedCategoryFilterId == cat.id,
                                        onClick = {
                                            onSelectCategoryFilter(
                                                if (state.selectedCategoryFilterId == cat.id) null else cat.id
                                            )
                                        },
                                        label = { Text(cat.name) }
                                    )
                                }
                            }
                        }
                    }
                }

                if (state.filteredAssignments.isEmpty()) {
                    item {
                        EmptyAcademicState(
                            icon = Icons.AutoMirrored.Rounded.Assignment,
                            title = "Nenhum trabalho encontrado",
                            description = "Ajuste os filtros acima ou crie um novo trabalho vinculado à sua disciplina e categoria.",
                            actionLabel = if (state.courses.isNotEmpty()) "Criar Trabalho" else null,
                            onAction = if (state.courses.isNotEmpty()) onCreateAssignment else null
                        )
                    }
                } else {
                    items(state.filteredAssignments, key = { it.id }) { assignment ->
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
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    item {
                        Card(
                            shape = OxfordCardShape,
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer
                            ),
                            border = oxfordCardBorder(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Ative as notificações do Android",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = "Permita notificações para receber alertas de Prioridade A (Nota) e Prioridade B (Entrega) no horário programado.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Button(
                                    onClick = {
                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("grant_notifications_button")
                                ) {
                                    Text("Permitir Notificações")
                                }
                            }
                        }
                    }
                }

                if (state.reminderQueue.isEmpty()) {
                    item {
                        EmptyAcademicState(
                            icon = Icons.Rounded.NotificationsNone,
                            title = "Nenhum lembrete pendente",
                            description = "Ative lembretes nos seus trabalhos para receber avisos priorizados por nota ou data de entrega."
                        )
                    }
                } else {
                    items(state.reminderQueue, key = { it.id }) { assignment ->
                        val course = state.courseById[assignment.courseId]
                        val isGradeA = assignment.priorityType == PriorityType.GRADE_A.value
                        val (countdown, _) = formatCountdownLabel(assignment.dueDateMillis, assignment.status)

                        Card(
                            onClick = { onOpenAssignment(assignment.id) },
                            shape = OxfordCardShape,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = oxfordCardBorder(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        color = if (isGradeA) academic.priorityASoft else academic.priorityBSoft,
                                        shape = OxfordChipShape
                                    ) {
                                        Text(
                                            text = if (isGradeA) "Prioridade A · ${formatNumberClean(assignment.maxPoints)} pts" else "Prioridade B · $countdown",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isGradeA) academic.priorityA else academic.priorityB,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                    Text(
                                        text = "Alerta ${assignment.reminderHoursBefore}h antes",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }

                                Text(assignment.title, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = "${course?.name ?: "Disciplina"} · Entrega: ${formatDueDateShort(assignment.dueDateMillis)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                            } else {
                                                ReminderScheduler.triggerImmediatePreview(
                                                    context = context,
                                                    assignment = assignment,
                                                    courseName = course?.name ?: "Disciplina"
                                                )
                                            }
                                        },
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Rounded.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.padding(3.dp))
                                        Text("Testar alerta")
                                    }
                                }
                            }
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(72.dp)) }
            }
        }
    }
}
