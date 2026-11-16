package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.automirrored.rounded.TextSnippet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.NoteAlt
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.domain.model.AiVerdict
import com.example.domain.model.Assignment
import com.example.domain.model.AssignmentAttachment
import com.example.domain.model.AssignmentStatus
import com.example.domain.model.Category
import com.example.domain.model.Course
import com.example.domain.model.Institution
import com.example.domain.model.PriorityType
import com.example.domain.model.StudyNote
import com.example.ui.components.AiVerdictChip
import com.example.ui.components.OxfordCardShape
import com.example.ui.components.OxfordChipShape
import com.example.ui.components.OxfordFolioRule
import com.example.ui.components.OxfordLibraryBackdrop
import com.example.ui.components.formatDueDateShort
import com.example.ui.components.formatNumberClean
import com.example.ui.components.oxfordCardBorder
import com.example.ui.theme.academicColors
import com.example.ui.viewmodel.AiTaskState
import com.example.ui.viewmodel.DataState
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AssignmentDetailScreen(
    assignment: Assignment,
    institution: Institution?,
    course: Course?,
    category: Category?,
    allCourses: List<Course>,
    allAssignments: List<Assignment>,
    assignmentNotes: List<StudyNote>,
    attachmentsState: DataState<List<AssignmentAttachment>>,
    aiTaskState: AiTaskState,
    aiChatResponse: String,
    isAiChatStreaming: Boolean,
    onBack: () -> Unit,
    onEditAssignment: () -> Unit,
    onDeleteAssignment: () -> Unit,
    onUpdateScoreAndStatus: (Double?, String, Boolean) -> Unit,
    onAnalyzeRequirements: () -> Unit,
    onAttachUri: (android.net.Uri, String) -> Unit,
    onAttachTextDraft: (String, String) -> Unit,
    onReEvaluateAttachment: (AssignmentAttachment) -> Unit,
    onDeleteAttachment: (String) -> Unit,
    onAskAiQuestion: (String) -> Unit,
    onDismissAiState: () -> Unit,
    onSaveNote: (StudyNote) -> Unit,
    onTogglePinNote: (StudyNote) -> Unit,
    onDeleteNote: (String) -> Unit,
) {
    val academic = MaterialTheme.academicColors
    val instName = institution?.name ?: "Instituição"
    val courseName = course?.name ?: "Disciplina"
    val categoryName = category?.name ?: "Geral"

    val attachments = (attachmentsState as? DataState.Success)?.data ?: emptyList()
    val hasApprovedAttachment = attachments.any { it.aiStatus == "approved" }

    var showScoreDialog by remember { mutableStateOf(false) }
    var showTextDraftDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showNoteEditor by remember { mutableStateOf(false) }
    var editingNote by remember { mutableStateOf<StudyNote?>(null) }
    var chatQuestion by remember { mutableStateOf("") }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                onAttachUri(uri, "")
            }
        }
    )

    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            if (uri != null) {
                onAttachUri(uri, "")
            }
        }
    )

    if (showScoreDialog) {
        ScoreAndStatusDialog(
            assignment = assignment,
            hasApprovedAttachment = hasApprovedAttachment,
            onDismiss = { showScoreDialog = false },
            onConfirm = { score, status ->
                onUpdateScoreAndStatus(score, status, hasApprovedAttachment)
                showScoreDialog = false
            }
        )
    }

    if (showTextDraftDialog) {
        TextDraftAttachmentDialog(
            onDismiss = { showTextDraftDialog = false },
            onSubmit = { title, content ->
                onAttachTextDraft(title, content)
                showTextDraftDialog = false
            }
        )
    }

    if (showNoteEditor) {
        QuickNoteEditorDialog(
            initial = editingNote,
            courses = allCourses,
            assignments = allAssignments,
            preselectedCourseId = assignment.courseId,
            preselectedAssignmentId = assignment.id,
            onDismiss = { showNoteEditor = false },
            onSave = {
                onSaveNote(it)
                showNoteEditor = false
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Excluir trabalho?") },
            text = { Text("Esta ação removerá '${assignment.title}' e seu histórico de avaliações.") },
            confirmButton = {
                Button(onClick = {
                    showDeleteConfirm = false
                    onDeleteAssignment()
                }) { Text("Excluir") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancelar") }
            }
        )
    }

    OxfordLibraryBackdrop {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                    ),
                    title = { Text(courseName, maxLines = 1) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Voltar")
                        }
                    },
                    actions = {
                        IconButton(onClick = onEditAssignment) {
                            Icon(Icons.Rounded.Edit, contentDescription = "Editar trabalho")
                        }
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Rounded.DeleteOutline, contentDescription = "Excluir trabalho")
                        }
                    }
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.TopCenter
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 840.dp),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header & Metadata Card
                    item {
                        Card(
                            shape = OxfordCardShape,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = oxfordCardBorder(emphasized = true),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "$instName · $courseName · $categoryName",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = assignment.title,
                                    style = MaterialTheme.typography.headlineSmall
                                )
                                if (assignment.description.isNotBlank()) {
                                    OxfordFolioRule(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                                    Text(
                                        text = assignment.description,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        shape = OxfordChipShape
                                    ) {
                                        Text(
                                            text = if (assignment.earnedPoints != null) {
                                                "Nota: ${formatNumberClean(assignment.earnedPoints)} / ${formatNumberClean(assignment.maxPoints)} pts"
                                            } else {
                                                "Vale ${formatNumberClean(assignment.maxPoints)} pts"
                                            },
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        shape = OxfordChipShape
                                    ) {
                                        Text(
                                            text = "Entrega: ${formatDueDateShort(assignment.dueDateMillis)}",
                                            style = MaterialTheme.typography.labelMedium,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                    Surface(
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                        shape = OxfordChipShape
                                    ) {
                                        Text(
                                            text = PriorityType.fromValue(assignment.priorityType).labelPt,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                    AiVerdictChip(
                                        verdict = AiVerdict.fromValue(assignment.aiVerdict),
                                        requiresAttachment = assignment.requiresAttachment
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    FilledTonalButton(
                                        onClick = { showScoreDialog = true },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("update_score_status_button")
                                    ) {
                                        Text(
                                            "Status: ${AssignmentStatus.fromValue(assignment.status).labelPt} · Nota"
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Embedded Quick Notes for this Assignment
                    item {
                        Card(
                            shape = OxfordCardShape,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = oxfordCardBorder(),
                            modifier = Modifier.fillMaxWidth()
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
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.NoteAlt,
                                            contentDescription = null,
                                            tint = academic.brassAccent
                                        )
                                        Text(
                                            text = "Anotações do Trabalho (${assignmentNotes.size})",
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                    }
                                    FilledTonalButton(
                                        onClick = {
                                            editingNote = null
                                            showNoteEditor = true
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        modifier = Modifier.testTag("detail_add_note_button")
                                    ) {
                                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.padding(2.dp))
                                        Text("Anotar")
                                    }
                                }
                                if (assignmentNotes.isEmpty()) {
                                    Text(
                                        text = "Salve referências bibliográficas, rascunhos ou lembretes específicos deste trabalho.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    if (assignmentNotes.isNotEmpty()) {
                        items(assignmentNotes, key = { "detail_note_${it.id}" }) { note ->
                            QuickNoteCard(
                                note = note,
                                course = course,
                                assignment = null,
                                onTogglePin = { onTogglePinNote(note) },
                                onEdit = {
                                    editingNote = note
                                    showNoteEditor = true
                                },
                                onDelete = { onDeleteNote(note.id) },
                                onOpenAssignment = {}
                            )
                        }
                    }

                    // AI Status Banner if active
                    if (aiTaskState !is AiTaskState.Idle) {
                        item {
                            AiStatusBanner(
                                aiTaskState = aiTaskState,
                                onDismiss = onDismissAiState
                            )
                        }
                    }

                    // AI Attachment Requirement & Evaluation Section
                    item {
                        Card(
                            shape = OxfordCardShape,
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.AutoAwesome,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Assistente de IA · Exigência & Avaliação de Anexos",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }

                                if (assignment.requiredAttachmentSpec.isNotBlank()) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.surface,
                                        shape = OxfordCardShape,
                                        border = oxfordCardBorder(),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = if (assignment.requiresAttachment) "Anexo obrigatório exigido:" else "Recomendação de anexo:",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.tertiary
                                            )
                                            Text(
                                                text = assignment.requiredAttachmentSpec,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                    }
                                } else {
                                    Text(
                                        text = "Peça ao Assistente de IA para analisar o enunciado e definir exatamente quais anexos devem ser exigidos neste trabalho.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                OutlinedButton(
                                    onClick = onAnalyzeRequirements,
                                    enabled = aiTaskState !is AiTaskState.Working,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("ai_require_attachment_button")
                                ) {
                                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.padding(4.dp))
                                    Text("IA: Analisar Enunciado e Exigir Anexos")
                                }

                                Text(
                                    text = "Enviar anexo para avaliação imediata da IA:",
                                    style = MaterialTheme.typography.labelLarge
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            photoPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        },
                                        enabled = aiTaskState !is AiTaskState.Working,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("attach_image_button")
                                    ) {
                                        Icon(Icons.Rounded.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.padding(3.dp))
                                        Text("Foto")
                                    }

                                    Button(
                                        onClick = {
                                            documentPickerLauncher.launch(
                                                arrayOf("application/pdf", "text/*", "image/*")
                                            )
                                        },
                                        enabled = aiTaskState !is AiTaskState.Working,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("attach_document_button")
                                    ) {
                                        Icon(Icons.Rounded.AttachFile, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.padding(3.dp))
                                        Text("PDF / Arq.")
                                    }

                                    FilledTonalButton(
                                        onClick = { showTextDraftDialog = true },
                                        enabled = aiTaskState !is AiTaskState.Working,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("attach_text_button")
                                    ) {
                                        Icon(Icons.AutoMirrored.Rounded.TextSnippet, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.padding(3.dp))
                                        Text("Texto")
                                    }
                                }
                            }
                        }
                    }

                    // Submitted Attachments & AI Evaluations List
                    item {
                        Text(
                            text = "Anexos Enviados (${attachments.size})",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    when (attachmentsState) {
                        is DataState.Loading -> {
                            item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
                        }
                        is DataState.Error -> {
                            item {
                                Text(
                                    text = attachmentsState.message,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                        is DataState.Success -> {
                            if (attachments.isEmpty()) {
                                item {
                                    Card(
                                        shape = OxfordCardShape,
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        border = oxfordCardBorder(),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = if (assignment.requiresAttachment) {
                                                "Nenhum anexo enviado ainda. Este trabalho exige pelo menos um anexo validado pela IA."
                                            } else {
                                                "Nenhum anexo enviado para este trabalho."
                                            },
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(16.dp)
                                        )
                                    }
                                }
                            } else {
                                items(attachments, key = { it.id }) { att ->
                                    AttachmentEvaluationCard(
                                        attachment = att,
                                        maxPoints = assignment.maxPoints,
                                        isWorking = aiTaskState is AiTaskState.Working,
                                        onReEvaluate = { onReEvaluateAttachment(att) },
                                        onDelete = { onDeleteAttachment(att.id) }
                                    )
                                }
                            }
                        }
                    }

                    // Contextual AI Tutor Chat
                    item {
                        Card(
                            shape = OxfordCardShape,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = oxfordCardBorder(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "Tirar dúvida com o Assistente de IA",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = chatQuestion,
                                        onValueChange = { chatQuestion = it },
                                        placeholder = { Text("Ex: O que falta no meu anexo para nota 10?") },
                                        modifier = Modifier.weight(1f),
                                        maxLines = 3
                                    )
                                    IconButton(
                                        onClick = {
                                            val q = chatQuestion.trim()
                                            if (q.isNotBlank()) {
                                                chatQuestion = ""
                                                onAskAiQuestion(q)
                                            }
                                        },
                                        enabled = chatQuestion.isNotBlank() && !isAiChatStreaming
                                    ) {
                                        if (isAiChatStreaming) {
                                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                        } else {
                                            Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Enviar pergunta")
                                        }
                                    }
                                }
                                if (aiChatResponse.isNotBlank()) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                                        shape = OxfordCardShape,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = aiChatResponse,
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.padding(12.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(32.dp)) }
                }
            }
        }
    }
}

@Composable
private fun AiStatusBanner(
    aiTaskState: AiTaskState,
    onDismiss: () -> Unit
) {
    val academic = MaterialTheme.academicColors
    when (aiTaskState) {
        AiTaskState.Idle -> Unit
        is AiTaskState.Working -> {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = OxfordCardShape,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp)
                    Text(aiTaskState.label, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        is AiTaskState.RequirementReady -> {
            Surface(
                color = academic.approvedSoft,
                shape = OxfordCardShape,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Exigência de anexo atualizada pela IA!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = academic.approved,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onDismiss) { Text("OK") }
                }
            }
        }
        is AiTaskState.EvaluationCompleted -> {
            Surface(
                color = academic.approvedSoft,
                shape = OxfordCardShape,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = aiTaskState.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = academic.approved,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onDismiss) { Text("OK") }
                }
            }
        }
        is AiTaskState.Error -> {
            Surface(
                color = academic.priorityBSoft,
                shape = OxfordCardShape,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = aiTaskState.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = academic.priorityB,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onDismiss) { Text("Fechar") }
                }
            }
        }
    }
}

@Composable
private fun AttachmentEvaluationCard(
    attachment: AssignmentAttachment,
    maxPoints: Double,
    isWorking: Boolean,
    onReEvaluate: () -> Unit,
    onDelete: () -> Unit
) {
    val academic = MaterialTheme.academicColors
    val (bg, fg, icon, statusLabel) = when (attachment.aiStatus) {
        "approved" -> Tuple4(academic.approvedSoft, academic.approved, Icons.Rounded.CheckCircle, "Aprovado pela IA")
        "rejected" -> Tuple4(academic.priorityBSoft, academic.priorityB, Icons.Rounded.ErrorOutline, "Inadequado")
        "needs_revision" -> Tuple4(academic.revisionSoft, academic.revision, Icons.Rounded.WarningAmber, "Precisa de Ajustes")
        else -> Tuple4(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            Icons.Rounded.Description,
            "Pendente de avaliação"
        )
    }

    Card(
        shape = OxfordCardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = oxfordCardBorder(),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val localFile = remember(attachment.localFilePath) {
                    attachment.localFilePath.takeIf { it.isNotBlank() }?.let { File(it) }
                }
                if (attachment.mimeType.startsWith("image/") && localFile != null && localFile.exists()) {
                    AsyncImage(
                        model = localFile,
                        contentDescription = attachment.fileName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                } else {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.size(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(attachment.fileName, style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = "${attachment.sizeBytes / 1024} KB · ${attachment.mimeType}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "Remover anexo")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(color = bg, shape = OxfordChipShape) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(16.dp))
                        Text(statusLabel, style = MaterialTheme.typography.labelMedium, color = fg)
                    }
                }

                if (attachment.aiStatus != "pending") {
                    val estPoints = (attachment.aiEstimatedPercent / 100.0) * maxPoints
                    Text(
                        text = "Est. IA: ${attachment.aiEstimatedPercent}% (${formatNumberClean(estPoints)} pts)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (attachment.aiFeedback.isNotBlank()) {
                Text(
                    text = attachment.aiFeedback,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (attachment.aiStrengths.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Pontos fortes:", style = MaterialTheme.typography.labelMedium, color = academic.approved)
                    attachment.aiStrengths.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
                }
            }

            if (attachment.aiGaps.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Lacunas identificadas:", style = MaterialTheme.typography.labelMedium, color = academic.priorityB)
                    attachment.aiGaps.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
                }
            }

            if (attachment.aiNextSteps.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Próximos passos:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    attachment.aiNextSteps.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
                }
            }

            OutlinedButton(
                onClick = onReEvaluate,
                enabled = !isWorking,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.padding(3.dp))
                Text("Reavaliar Anexo com IA")
            }
        }
    }
}

private data class Tuple4<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ScoreAndStatusDialog(
    assignment: Assignment,
    hasApprovedAttachment: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Double?, String) -> Unit
) {
    val academic = MaterialTheme.academicColors
    var status by remember { mutableStateOf(assignment.status) }
    var scoreText by remember { mutableStateOf(assignment.earnedPoints?.toString() ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Atualizar Status e Nota") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (assignment.requiresAttachment && !hasApprovedAttachment) {
                    Surface(
                        color = academic.revisionSoft,
                        shape = OxfordCardShape,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Atenção: este trabalho exige um anexo aprovado pela IA antes de marcar como Entregue ou Avaliado.",
                            style = MaterialTheme.typography.bodySmall,
                            color = academic.revision,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
                Text("Status", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AssignmentStatus.entries.forEach { st ->
                        FilterChip(
                            selected = status == st.value,
                            onClick = { status = st.value },
                            label = { Text(st.labelPt) }
                        )
                    }
                }
                OutlinedTextField(
                    value = scoreText,
                    onValueChange = { scoreText = it },
                    label = { Text("Nota obtida (máx: ${formatNumberClean(assignment.maxPoints)})") },
                    placeholder = { Text("Deixe em branco se ainda não corrigido") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val parsed = scoreText.replace(",", ".").toDoubleOrNull()?.coerceIn(0.0, assignment.maxPoints)
                val finalStatus = if (parsed != null && status == AssignmentStatus.PENDING.value) {
                    AssignmentStatus.GRADED.value
                } else status
                onConfirm(parsed, finalStatus)
            }) { Text("Salvar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
private fun TextDraftAttachmentDialog(
    onDismiss: () -> Unit,
    onSubmit: (String, String) -> Unit
) {
    var title by remember { mutableStateOf("Resposta_Trabalho") }
    var content by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Enviar Texto / Resposta para IA") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Nome do anexo") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Conteúdo / Resposta / Resumo") },
                    placeholder = { Text("Cole aqui seu texto, código ou resumo para avaliação da IA...") },
                    minLines = 5,
                    maxLines = 8,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(title.trim(), content.trim()) },
                enabled = content.isNotBlank()
            ) { Text("Enviar e Avaliar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
