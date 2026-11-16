package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.domain.model.Category
import com.example.domain.model.Course
import com.example.domain.model.Institution
import com.example.domain.model.InstitutionType
import com.example.ui.components.EmptyAcademicState
import com.example.ui.components.OxfordCardShape
import com.example.ui.components.categoryIconForKey
import com.example.ui.components.formatNumberClean
import com.example.ui.components.oxfordCardBorder
import com.example.ui.theme.AcademicPaletteHexes
import com.example.ui.theme.parseHexColor
import com.example.ui.viewmodel.AcademicDashboardState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AcademicStructureScreen(
    state: AcademicDashboardState,
    onSaveInstitution: (Institution) -> Unit,
    onDeleteInstitution: (String) -> Unit,
    onSaveCourse: (Course) -> Unit,
    onDeleteCourse: (String) -> Unit,
    onSaveCategory: (Category) -> Unit,
    onDeleteCategory: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var editingInstitution by remember { mutableStateOf<Institution?>(null) }
    var showInstitutionDialog by remember { mutableStateOf(false) }

    var editingCourse by remember { mutableStateOf<Course?>(null) }
    var showCourseDialog by remember { mutableStateOf(false) }

    var editingCategory by remember { mutableStateOf<Category?>(null) }
    var showCategoryDialog by remember { mutableStateOf(false) }

    if (showInstitutionDialog) {
        InstitutionEditorDialog(
            initial = editingInstitution,
            onDismiss = { showInstitutionDialog = false },
            onSave = {
                onSaveInstitution(it)
                showInstitutionDialog = false
            }
        )
    }

    if (showCourseDialog) {
        CourseEditorDialog(
            initial = editingCourse,
            institutions = state.institutions,
            onDismiss = { showCourseDialog = false },
            onSave = {
                onSaveCourse(it)
                showCourseDialog = false
            }
        )
    }

    if (showCategoryDialog) {
        CategoryEditorDialog(
            initial = editingCategory,
            onDismiss = { showCategoryDialog = false },
            onSave = {
                onSaveCategory(it)
                showCategoryDialog = false
            }
        )
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
            Text(
                text = "Estrutura Acadêmica",
                style = MaterialTheme.typography.headlineMedium
            )
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Instituições (${state.institutions.size})") },
                    modifier = Modifier.testTag("tab_institutions")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Disciplinas (${state.courses.size})") },
                    modifier = Modifier.testTag("tab_courses")
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Categorias (${state.categories.size})") },
                    modifier = Modifier.testTag("tab_categories")
                )
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            when (selectedTab) {
                0 -> {
                    item {
                        Button(
                            onClick = {
                                editingInstitution = null
                                showInstitutionDialog = true
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("add_institution_button")
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null)
                            Spacer(modifier = Modifier.padding(4.dp))
                            Text("Nova Instituição")
                        }
                    }

                    if (state.institutions.isEmpty()) {
                        item {
                            EmptyAcademicState(
                                icon = Icons.Rounded.AccountBalance,
                                title = "Sem instituições cadastradas",
                                description = "Adicione universidades, escolas ou plataformas de cursos para associar suas disciplinas."
                            )
                        }
                    } else {
                        items(state.institutions, key = { it.id }) { inst ->
                            val color = parseHexColor(inst.colorHex, MaterialTheme.colorScheme.primary)
                            val courseCount = state.courses.count { it.institutionId == inst.id }
                            Card(
                                shape = OxfordCardShape,
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = oxfordCardBorder(),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Surface(
                                        color = color.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.size(46.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Rounded.AccountBalance, contentDescription = null, tint = color)
                                        }
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(inst.name, style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            text = "${InstitutionType.fromValue(inst.type).labelPt} · $courseCount disciplinas",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (inst.notes.isNotBlank()) {
                                            Text(
                                                text = inst.notes,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    IconButton(onClick = {
                                        editingInstitution = inst
                                        showInstitutionDialog = true
                                    }) {
                                        Icon(Icons.Rounded.Edit, contentDescription = "Editar instituição")
                                    }
                                    IconButton(onClick = { onDeleteInstitution(inst.id) }) {
                                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "Excluir instituição")
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    item {
                        Button(
                            onClick = {
                                editingCourse = null
                                showCourseDialog = true
                            },
                            enabled = state.institutions.isNotEmpty(),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("add_course_button")
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null)
                            Spacer(modifier = Modifier.padding(4.dp))
                            Text(
                                if (state.institutions.isEmpty()) "Cadastre uma instituição primeiro"
                                else "Nova Disciplina"
                            )
                        }
                    }

                    if (state.courses.isEmpty()) {
                        item {
                            EmptyAcademicState(
                                icon = Icons.AutoMirrored.Rounded.MenuBook,
                                title = "Nenhuma disciplina ainda",
                                description = "Associe disciplinas às suas instituições com meta de nota, professor e semestre."
                            )
                        }
                    } else {
                        items(state.coursePerformances, key = { it.course.id }) { perf ->
                            val course = perf.course
                            val color = parseHexColor(course.colorHex, MaterialTheme.colorScheme.primary)
                            Card(
                                shape = OxfordCardShape,
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = oxfordCardBorder(),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp, 48.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(color)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(course.name, style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            text = buildString {
                                                append(course.institutionName)
                                                if (course.code.isNotBlank()) append(" · ${course.code}")
                                                if (course.term.isNotBlank()) append(" · ${course.term}")
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "Meta: ${formatNumberClean(course.targetGrade)}/${formatNumberClean(course.maxScale)} · ${perf.totalAssignments} trabalhos (${perf.averagePercent}%)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = color
                                        )
                                    }
                                    IconButton(onClick = {
                                        editingCourse = course
                                        showCourseDialog = true
                                    }) {
                                        Icon(Icons.Rounded.Edit, contentDescription = "Editar disciplina")
                                    }
                                    IconButton(onClick = { onDeleteCourse(course.id) }) {
                                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "Excluir disciplina")
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    item {
                        Button(
                            onClick = {
                                editingCategory = null
                                showCategoryDialog = true
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("add_category_button")
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null)
                            Spacer(modifier = Modifier.padding(4.dp))
                            Text("Nova Categoria Personalizada")
                        }
                    }

                    if (state.categories.isEmpty()) {
                        item {
                            EmptyAcademicState(
                                icon = Icons.Rounded.Category,
                                title = "Sem categorias personalizadas",
                                description = "Crie categorias como Artigo, Seminário, Prova Prática ou TCC para organizar seus trabalhos."
                            )
                        }
                    } else {
                        items(state.categories, key = { it.id }) { cat ->
                            val color = parseHexColor(cat.colorHex, MaterialTheme.colorScheme.primary)
                            Card(
                                shape = OxfordCardShape,
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = oxfordCardBorder(),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Surface(
                                        color = color.copy(alpha = 0.14f),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.size(42.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = categoryIconForKey(cat.iconKey),
                                                contentDescription = null,
                                                tint = color
                                            )
                                        }
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(cat.name, style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            text = if (cat.requiresAttachmentDefault) "Exige anexo por padrão" else "Anexo opcional por padrão",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(onClick = {
                                        editingCategory = cat
                                        showCategoryDialog = true
                                    }) {
                                        Icon(Icons.Rounded.Edit, contentDescription = "Editar categoria")
                                    }
                                    IconButton(onClick = { onDeleteCategory(cat.id) }) {
                                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "Excluir categoria")
                                    }
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InstitutionEditorDialog(
    initial: Institution?,
    onDismiss: () -> Unit,
    onSave: (Institution) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var shortName by remember { mutableStateOf(initial?.shortName.orEmpty()) }
    var type by remember { mutableStateOf(initial?.type ?: InstitutionType.UNIVERSITY.value) }
    var colorHex by remember { mutableStateOf(initial?.colorHex ?: AcademicPaletteHexes[0]) }
    var notes by remember { mutableStateOf(initial?.notes.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Nova Instituição" else "Editar Instituição") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome completo") },
                    placeholder = { Text("Ex: Universidade de São Paulo") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = shortName,
                    onValueChange = { shortName = it },
                    label = { Text("Sigla (opcional)") },
                    placeholder = { Text("Ex: USP") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Tipo", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    InstitutionType.entries.forEach { t ->
                        FilterChip(
                            selected = type == t.value,
                            onClick = { type = t.value },
                            label = { Text(t.labelPt) }
                        )
                    }
                }
                ColorPickerRow(selectedHex = colorHex, onSelectHex = { colorHex = it })
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Observações / Campus") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        (initial ?: Institution()).copy(
                            name = name.trim(),
                            shortName = shortName.trim().ifBlank { name.trim().take(10) },
                            type = type,
                            colorHex = colorHex,
                            notes = notes.trim()
                        )
                    )
                },
                enabled = name.isNotBlank()
            ) { Text("Salvar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CourseEditorDialog(
    initial: Course?,
    institutions: List<Institution>,
    onDismiss: () -> Unit,
    onSave: (Course) -> Unit
) {
    var institutionId by remember {
        mutableStateOf(initial?.institutionId ?: institutions.firstOrNull()?.id.orEmpty())
    }
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var code by remember { mutableStateOf(initial?.code.orEmpty()) }
    var professor by remember { mutableStateOf(initial?.professor.orEmpty()) }
    var term by remember { mutableStateOf(initial?.term ?: "2026.2") }
    var targetGrade by remember { mutableStateOf(initial?.targetGrade?.toString() ?: "7.0") }
    var maxScale by remember { mutableStateOf(initial?.maxScale?.toString() ?: "10.0") }
    var colorHex by remember { mutableStateOf(initial?.colorHex ?: AcademicPaletteHexes[1]) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Nova Disciplina" else "Editar Disciplina") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Instituição", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    institutions.forEach { inst ->
                        FilterChip(
                            selected = inst.id == institutionId,
                            onClick = { institutionId = inst.id },
                            label = { Text(inst.shortName.ifBlank { inst.name }) }
                        )
                    }
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome da disciplina") },
                    placeholder = { Text("Ex: Engenharia de Software") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it },
                        label = { Text("Código") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = term,
                        onValueChange = { term = it },
                        label = { Text("Período") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = professor,
                    onValueChange = { professor = it },
                    label = { Text("Professor(a)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = targetGrade,
                        onValueChange = { targetGrade = it },
                        label = { Text("Meta de nota") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = maxScale,
                        onValueChange = { maxScale = it },
                        label = { Text("Escala máx.") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                ColorPickerRow(selectedHex = colorHex, onSelectHex = { colorHex = it })
            }
        },
        confirmButton = {
            val selectedInst = institutions.firstOrNull { it.id == institutionId }
            Button(
                onClick = {
                    val inst = selectedInst ?: return@Button
                    onSave(
                        (initial ?: Course()).copy(
                            institutionId = inst.id,
                            institutionName = inst.shortName.ifBlank { inst.name },
                            name = name.trim(),
                            code = code.trim(),
                            professor = professor.trim(),
                            term = term.trim(),
                            targetGrade = targetGrade.replace(",", ".").toDoubleOrNull() ?: 7.0,
                            maxScale = maxScale.replace(",", ".").toDoubleOrNull()?.coerceAtLeast(1.0) ?: 10.0,
                            colorHex = colorHex
                        )
                    )
                },
                enabled = name.isNotBlank() && selectedInst != null
            ) { Text("Salvar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryEditorDialog(
    initial: Category?,
    onDismiss: () -> Unit,
    onSave: (Category) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var iconKey by remember { mutableStateOf(initial?.iconKey ?: "article") }
    var colorHex by remember { mutableStateOf(initial?.colorHex ?: AcademicPaletteHexes[4]) }
    var requiresAttachmentDefault by remember {
        mutableStateOf(initial?.requiresAttachmentDefault ?: true)
    }

    val iconOptions = listOf(
        "article" to "Artigo",
        "quiz" to "Prova",
        "code" to "Projeto",
        "co_present" to "Seminário",
        "science" to "Lab",
        "checklist" to "Lista",
        "folder" to "Portfólio"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Nova Categoria" else "Editar Categoria") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome da categoria") },
                    placeholder = { Text("Ex: TCC, Fichamento, Laboratório") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Ícone", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    iconOptions.forEach { (key, label) ->
                        FilterChip(
                            selected = iconKey == key,
                            onClick = { iconKey = key },
                            leadingIcon = {
                                Icon(categoryIconForKey(key), contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            label = { Text(label) }
                        )
                    }
                }
                ColorPickerRow(selectedHex = colorHex, onSelectHex = { colorHex = it })
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Exigir anexo por padrão", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = requiresAttachmentDefault,
                        onCheckedChange = { requiresAttachmentDefault = it }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        (initial ?: Category()).copy(
                            name = name.trim(),
                            iconKey = iconKey,
                            colorHex = colorHex,
                            requiresAttachmentDefault = requiresAttachmentDefault
                        )
                    )
                },
                enabled = name.isNotBlank()
            ) { Text("Salvar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
private fun ColorPickerRow(
    selectedHex: String,
    onSelectHex: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AcademicPaletteHexes.forEach { hex ->
            val color = parseHexColor(hex)
            val isSelected = hex.equals(selectedHex, ignoreCase = true)
            Surface(
                color = color,
                shape = CircleShape,
                border = if (isSelected) BorderStroke(3.dp, MaterialTheme.colorScheme.onSurface) else null,
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .clickable { onSelectHex(hex) }
            ) {}
        }
    }
}
