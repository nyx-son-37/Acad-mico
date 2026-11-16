package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.domain.model.Assignment
import com.example.domain.model.AssignmentStatus
import com.example.domain.model.Category
import com.example.domain.model.Course
import com.example.domain.model.Institution
import com.example.domain.model.PriorityType
import com.example.ui.components.formatDueDateShort
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AssignmentEditorDialog(
    initialAssignment: Assignment?,
    institutions: List<Institution>,
    courses: List<Course>,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onSave: (Assignment, String) -> Unit
) {
    val defaultDue = remember(initialAssignment) {
        initialAssignment?.dueDateMillis?.takeIf { it > 0L }
            ?: (System.currentTimeMillis() + 3L * 24L * 3600_000L)
    }

    var selectedInstitutionId by remember {
        mutableStateOf(
            initialAssignment?.institutionId?.takeIf { it.isNotBlank() }
                ?: courses.firstOrNull()?.institutionId
                ?: institutions.firstOrNull()?.id.orEmpty()
        )
    }

    val filteredCourses = remember(selectedInstitutionId, courses) {
        courses.filter { selectedInstitutionId.isBlank() || it.institutionId == selectedInstitutionId }
            .ifEmpty { courses }
    }

    var selectedCourseId by remember {
        mutableStateOf(
            initialAssignment?.courseId?.takeIf { it.isNotBlank() }
                ?: filteredCourses.firstOrNull()?.id.orEmpty()
        )
    }

    var selectedCategoryId by remember {
        mutableStateOf(
            initialAssignment?.categoryId?.takeIf { it.isNotBlank() }
                ?: categories.firstOrNull()?.id.orEmpty()
        )
    }

    var title by remember { mutableStateOf(initialAssignment?.title.orEmpty()) }
    var description by remember { mutableStateOf(initialAssignment?.description.orEmpty()) }
    var maxPointsText by remember {
        mutableStateOf(initialAssignment?.maxPoints?.toString() ?: "10.0")
    }
    var dueDateMillis by remember { mutableLongStateOf(defaultDue) }
    var priorityType by remember {
        mutableStateOf(initialAssignment?.priorityType ?: PriorityType.GRADE_A.value)
    }
    var reminderEnabled by remember { mutableStateOf(initialAssignment?.reminderEnabled ?: true) }
    var reminderHoursBefore by remember {
        mutableLongStateOf(initialAssignment?.reminderHoursBefore ?: 24L)
    }
    var requiresAttachment by remember {
        val catDefault = categories.firstOrNull { it.id == selectedCategoryId }?.requiresAttachmentDefault ?: true
        mutableStateOf(initialAssignment?.requiresAttachment ?: catDefault)
    }
    var requiredAttachmentSpec by remember {
        mutableStateOf(initialAssignment?.requiredAttachmentSpec.orEmpty())
    }
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = dueDateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val picked = datePickerState.selectedDateMillis
                    if (picked != null) {
                        val oldCal = Calendar.getInstance().apply { timeInMillis = dueDateMillis }
                        val newCal = Calendar.getInstance().apply {
                            timeInMillis = picked
                            set(Calendar.HOUR_OF_DAY, oldCal.get(Calendar.HOUR_OF_DAY))
                            set(Calendar.MINUTE, oldCal.get(Calendar.MINUTE))
                        }
                        dueDateMillis = newCal.timeInMillis
                    }
                    showDatePicker = false
                }) { Text("Confirmar") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (initialAssignment == null) "Novo Trabalho" else "Editar Trabalho",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Fechar")
                    }
                }

                if (courses.isEmpty()) {
                    Text(
                        text = "Cadastre primeiro uma Instituição e uma Disciplina na aba Acadêmico para vincular este trabalho.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    Text("Instituição", style = MaterialTheme.typography.labelLarge)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        institutions.forEach { inst ->
                            FilterChip(
                                selected = inst.id == selectedInstitutionId,
                                onClick = {
                                    selectedInstitutionId = inst.id
                                    val firstCourse = courses.firstOrNull { it.institutionId == inst.id }
                                    if (firstCourse != null) selectedCourseId = firstCourse.id
                                },
                                label = { Text(inst.shortName.ifBlank { inst.name }) }
                            )
                        }
                    }

                    Text("Disciplina", style = MaterialTheme.typography.labelLarge)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        filteredCourses.forEach { course ->
                            FilterChip(
                                selected = course.id == selectedCourseId,
                                onClick = {
                                    selectedCourseId = course.id
                                    selectedInstitutionId = course.institutionId
                                },
                                label = { Text(course.name) }
                            )
                        }
                    }

                    Text("Categoria Personalizada", style = MaterialTheme.typography.labelLarge)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        categories.forEach { cat ->
                            FilterChip(
                                selected = cat.id == selectedCategoryId,
                                onClick = {
                                    selectedCategoryId = cat.id
                                    if (initialAssignment == null) {
                                        requiresAttachment = cat.requiresAttachmentDefault
                                    }
                                },
                                label = { Text(cat.name) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Título do trabalho") },
                        placeholder = { Text("Ex: Artigo sobre Redes Neurais") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("assignment_title_input")
                    )

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Enunciado / Rubrica (usado pela IA)") },
                        placeholder = { Text("Descreva o que o professor pediu e critérios de avaliação") },
                        minLines = 3,
                        maxLines = 5,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = maxPointsText,
                            onValueChange = { maxPointsText = it },
                            label = { Text("Pontuação máxima") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedButton(
                            onClick = { showDatePicker = true },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1.2f)
                                .height(56.dp)
                        ) {
                            Icon(Icons.Rounded.Event, contentDescription = null)
                            Spacer(modifier = Modifier.padding(4.dp))
                            Text(formatDueDateShort(dueDateMillis))
                        }
                    }

                    Text("Prioridade do Lembrete", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = priorityType == PriorityType.GRADE_A.value,
                            onClick = { priorityType = PriorityType.GRADE_A.value },
                            label = { Text("Prioridade A · Nota") }
                        )
                        FilterChip(
                            selected = priorityType == PriorityType.DEADLINE_B.value,
                            onClick = { priorityType = PriorityType.DEADLINE_B.value },
                            label = { Text("Prioridade B · Data de entrega") }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Lembrete por notificação", style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = "Avisar antes do prazo com base na prioridade",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(checked = reminderEnabled, onCheckedChange = { reminderEnabled = it })
                    }

                    if (reminderEnabled) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(2L to "2h antes", 12L to "12h antes", 24L to "24h antes", 48L to "48h antes", 168L to "7 dias").forEach { (hrs, label) ->
                                FilterChip(
                                    selected = reminderHoursBefore == hrs,
                                    onClick = { reminderHoursBefore = hrs },
                                    label = { Text(label) }
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Exigir anexo avaliado por IA", style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = "Impede concluir sem evidência validada",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(checked = requiresAttachment, onCheckedChange = { requiresAttachment = it })
                    }

                    if (requiresAttachment) {
                        OutlinedTextField(
                            value = requiredAttachmentSpec,
                            onValueChange = { requiredAttachmentSpec = it },
                            label = { Text("Especificação do anexo exigido (opcional)") },
                            placeholder = { Text("Ex: PDF com cálculos + foto do gráfico (ou peça à IA depois)") },
                            minLines = 2,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    val selectedCourse = courses.firstOrNull { it.id == selectedCourseId }
                    val canSave = title.isNotBlank() && selectedCourse != null && selectedCategoryId.isNotBlank()

                    Button(
                        onClick = {
                            val course = selectedCourse ?: return@Button
                            val points = maxPointsText.replace(",", ".").toDoubleOrNull()?.coerceAtLeast(0.0) ?: 10.0
                            val updated = (initialAssignment ?: Assignment()).copy(
                                institutionId = course.institutionId,
                                courseId = course.id,
                                categoryId = selectedCategoryId,
                                title = title.trim(),
                                description = description.trim(),
                                maxPoints = points,
                                dueDateMillis = dueDateMillis,
                                priorityType = priorityType,
                                reminderEnabled = reminderEnabled,
                                reminderHoursBefore = reminderHoursBefore,
                                status = initialAssignment?.status ?: AssignmentStatus.PENDING.value,
                                requiresAttachment = requiresAttachment,
                                requiredAttachmentSpec = requiredAttachmentSpec.trim()
                            )
                            onSave(updated, course.name)
                        },
                        enabled = canSave,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("save_assignment_button")
                    ) {
                        Text(if (initialAssignment == null) "Criar Trabalho" else "Salvar Alterações")
                    }
                }
            }
        }
    }
}
