package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Assignment
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.NoteAlt
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.model.Assignment
import com.example.domain.model.Course
import com.example.domain.model.StudyNote
import com.example.ui.components.EmptyAcademicState
import com.example.ui.components.OxfordCardShape
import com.example.ui.components.OxfordChipShape
import com.example.ui.components.OxfordFolioRule
import com.example.ui.theme.academicColors
import com.example.ui.theme.parseHexColor
import com.example.ui.viewmodel.AcademicDashboardState
import java.text.SimpleDateFormat
import java.util.Locale

private val ptBrLocale: Locale = Locale.forLanguageTag("pt-BR")

@Composable
fun QuickNotesScreen(
    state: AcademicDashboardState,
    onSaveNote: (StudyNote) -> Unit,
    onTogglePin: (StudyNote) -> Unit,
    onDeleteNote: (String) -> Unit,
    onOpenAssignment: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val academic = MaterialTheme.academicColors
    var searchQuery by remember { mutableStateOf("") }
    var courseFilterId by remember { mutableStateOf<String?>(null) }
    var onlyPinned by remember { mutableStateOf(false) }

    var showEditor by remember { mutableStateOf(false) }
    var editingNote by remember { mutableStateOf<StudyNote?>(null) }

    if (showEditor) {
        QuickNoteEditorDialog(
            initial = editingNote,
            courses = state.courses,
            assignments = state.assignments,
            onDismiss = { showEditor = false },
            onSave = {
                onSaveNote(it)
                showEditor = false
            }
        )
    }

    val filteredNotes = remember(state.notes, searchQuery, courseFilterId, onlyPinned, state.courses, state.assignments) {
        val q = searchQuery.trim().lowercase()
        state.notes.filter { note ->
            val matchesPin = !onlyPinned || note.pinned
            val matchesCourse = courseFilterId == null || note.courseId == courseFilterId
            val courseName = state.courseById[note.courseId]?.name.orEmpty().lowercase()
            val assignmentTitle = state.assignmentById[note.assignmentId]?.title.orEmpty().lowercase()
            val matchesQuery = q.isBlank() ||
                note.title.lowercase().contains(q) ||
                note.content.lowercase().contains(q) ||
                courseName.contains(q) ||
                assignmentTitle.contains(q)
            matchesPin && matchesCourse && matchesQuery
        }
    }

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
                Column {
                    Text(
                        text = "FICHAS & MARGINÁLIA",
                        style = MaterialTheme.typography.labelSmall,
                        color = academic.brassAccent
                    )
                    Text("Anotações Rápidas", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        text = "${state.notes.size} anotações sincronizadas",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = {
                        editingNote = null
                        showEditor = true
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("add_note_button")
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.padding(3.dp))
                    Text("Nova Nota")
                }
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Pesquisar por texto, disciplina ou trabalho...") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = "Pesquisar") },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Limpar pesquisa")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("notes_search_input")
            )

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = onlyPinned,
                    onClick = { onlyPinned = !onlyPinned },
                    leadingIcon = {
                        Icon(Icons.Rounded.PushPin, contentDescription = null, modifier = Modifier.size(15.dp))
                    },
                    label = { Text("Fixadas") }
                )
                state.courses.forEach { course ->
                    FilterChip(
                        selected = courseFilterId == course.id,
                        onClick = {
                            courseFilterId = if (courseFilterId == course.id) null else course.id
                        },
                        label = { Text(course.name) }
                    )
                }
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            if (filteredNotes.isEmpty()) {
                item {
                    EmptyAcademicState(
                        icon = Icons.Rounded.NoteAlt,
                        title = if (state.notes.isEmpty()) "Nenhuma anotação ainda" else "Nenhuma anotação encontrada",
                        description = if (state.notes.isEmpty()) {
                            "Registre ideias de aula, fórmulas, citações ou lembretes vinculados às suas disciplinas e trabalhos."
                        } else {
                            "Tente buscar outro termo ou limpar os filtros acima."
                        },
                        actionLabel = "Criar Anotação Rápida",
                        onAction = {
                            editingNote = null
                            showEditor = true
                        }
                    )
                }
            } else {
                items(filteredNotes, key = { it.id }) { note ->
                    val course = state.courseById[note.courseId]
                    val assignment = state.assignmentById[note.assignmentId]
                    QuickNoteCard(
                        note = note,
                        course = course,
                        assignment = assignment,
                        onTogglePin = { onTogglePin(note) },
                        onEdit = {
                            editingNote = note
                            showEditor = true
                        },
                        onDelete = { onDeleteNote(note.id) },
                        onOpenAssignment = {
                            if (assignment != null) onOpenAssignment(assignment.id)
                        }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(72.dp)) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickNoteCard(
    note: StudyNote,
    course: Course?,
    assignment: Assignment?,
    onTogglePin: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onOpenAssignment: () -> Unit,
    modifier: Modifier = Modifier
) {
    val academic = MaterialTheme.academicColors
    val dateLabel = remember(note.updatedAt, note.createdAt) {
        val ts = note.updatedAt ?: note.createdAt
        ts?.toDate()?.let {
            SimpleDateFormat("dd/MM/yyyy · HH:mm", ptBrLocale).format(it)
        } ?: "Agora"
    }
    val courseColor = course?.let { parseHexColor(it.colorHex, MaterialTheme.colorScheme.primary) }
        ?: MaterialTheme.colorScheme.primary

    Card(
        onClick = onEdit,
        shape = OxfordCardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            width = 1.dp,
            color = if (note.pinned) academic.priorityA.copy(alpha = 0.62f)
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.58f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("study_note_card_${note.id}")
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
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (note.pinned) {
                        Box(
                            modifier = Modifier
                                .size(width = 3.dp, height = 18.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(academic.priorityA)
                        )
                    }
                    Text(
                        text = note.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onTogglePin, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Rounded.PushPin,
                            contentDescription = if (note.pinned) "Desafixar" else "Fixar",
                            tint = if (note.pinned) academic.priorityA else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Rounded.Edit, contentDescription = "Editar anotação", modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "Excluir anotação", modifier = Modifier.size(18.dp))
                    }
                }
            }

            OxfordFolioRule(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))

            Text(
                text = note.content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 6,
                overflow = TextOverflow.Ellipsis
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (course != null) {
                    Surface(
                        color = courseColor.copy(alpha = 0.14f),
                        shape = OxfordChipShape
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.MenuBook, contentDescription = null, tint = courseColor, modifier = Modifier.size(13.dp))
                            Text(course.name, style = MaterialTheme.typography.labelSmall, color = courseColor)
                        }
                    }
                }
                if (assignment != null) {
                    Surface(
                        onClick = onOpenAssignment,
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                        shape = OxfordChipShape
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.Assignment, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(13.dp))
                            Text(
                                text = assignment.title,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                Text(
                    text = dateLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickNoteEditorDialog(
    initial: StudyNote?,
    courses: List<Course>,
    assignments: List<Assignment>,
    preselectedCourseId: String = "",
    preselectedAssignmentId: String = "",
    onDismiss: () -> Unit,
    onSave: (StudyNote) -> Unit
) {
    var title by remember { mutableStateOf(initial?.title.orEmpty()) }
    var content by remember { mutableStateOf(initial?.content.orEmpty()) }
    var selectedCourseId by remember {
        mutableStateOf(initial?.courseId ?: preselectedCourseId)
    }
    var selectedAssignmentId by remember {
        mutableStateOf(initial?.assignmentId ?: preselectedAssignmentId)
    }
    var pinned by remember { mutableStateOf(initial?.pinned ?: false) }

    val availableAssignments = remember(selectedCourseId, assignments) {
        if (selectedCourseId.isBlank()) assignments
        else assignments.filter { it.courseId == selectedCourseId }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Nova Anotação Rápida" else "Editar Anotação") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Título (opcional)") },
                    placeholder = { Text("Ex: Fórmulas da P2") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("note_title_input")
                )
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Texto da anotação") },
                    placeholder = { Text("Escreva sua anotação de aula, lembrete ou resumo...") },
                    minLines = 4,
                    maxLines = 7,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("note_content_input")
                )

                if (courses.isNotEmpty()) {
                    Text("Associar à Disciplina (opcional)", style = MaterialTheme.typography.labelMedium)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        FilterChip(
                            selected = selectedCourseId.isBlank(),
                            onClick = {
                                selectedCourseId = ""
                                selectedAssignmentId = ""
                            },
                            label = { Text("Geral") }
                        )
                        courses.forEach { course ->
                            FilterChip(
                                selected = selectedCourseId == course.id,
                                onClick = {
                                    selectedCourseId = course.id
                                    if (assignments.firstOrNull { it.id == selectedAssignmentId }?.courseId != course.id) {
                                        selectedAssignmentId = ""
                                    }
                                },
                                label = { Text(course.name) }
                            )
                        }
                    }
                }

                if (availableAssignments.isNotEmpty()) {
                    Text("Associar ao Trabalho (opcional)", style = MaterialTheme.typography.labelMedium)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        FilterChip(
                            selected = selectedAssignmentId.isBlank(),
                            onClick = { selectedAssignmentId = "" },
                            label = { Text("Nenhum") }
                        )
                        availableAssignments.take(6).forEach { assign ->
                            FilterChip(
                                selected = selectedAssignmentId == assign.id,
                                onClick = {
                                    selectedAssignmentId = assign.id
                                    selectedCourseId = assign.courseId
                                },
                                label = { Text(assign.title, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Fixar no topo", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = pinned, onCheckedChange = { pinned = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        (initial ?: StudyNote()).copy(
                            title = title.trim(),
                            content = content.trim(),
                            courseId = selectedCourseId,
                            assignmentId = selectedAssignmentId,
                            pinned = pinned
                        )
                    )
                },
                enabled = content.isNotBlank(),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("save_note_button")
            ) { Text("Salvar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
