package com.example.ui.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.AcademicAiAssistant
import com.example.data.ai.AiRequirementAnalysis
import com.example.data.attachments.AttachmentStorageHelper
import com.example.data.repository.AcademicRepository
import com.example.domain.model.AiVerdict
import com.example.domain.model.Assignment
import com.example.domain.model.AssignmentAttachment
import com.example.domain.model.AssignmentAttentionItem
import com.example.domain.model.AssignmentStatus
import com.example.domain.model.Category
import com.example.domain.model.Course
import com.example.domain.model.CoursePerformance
import com.example.domain.model.Institution
import com.example.domain.model.PriorityType
import com.example.domain.model.StudyNote
import com.example.domain.model.UserProfile
import com.example.notifications.ReminderScheduler
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface DataState<out T> {
    data object Loading : DataState<Nothing>
    data class Success<T>(val data: T) : DataState<T>
    data class Error(val message: String) : DataState<Nothing>
}

data class AcademicDashboardState(
    val profile: UserProfile? = null,
    val institutions: List<Institution> = emptyList(),
    val courses: List<Course> = emptyList(),
    val categories: List<Category> = emptyList(),
    val assignments: List<Assignment> = emptyList(),
    val notes: List<StudyNote> = emptyList(),
    val activePriorityMode: PriorityType = PriorityType.GRADE_A,
    val selectedInstitutionFilterId: String? = null,
    val selectedCourseFilterId: String? = null,
    val selectedCategoryFilterId: String? = null,
) {
    val institutionById: Map<String, Institution> = institutions.associateBy { it.id }
    val courseById: Map<String, Course> = courses.associateBy { it.id }
    val categoryById: Map<String, Category> = categories.associateBy { it.id }
    val assignmentById: Map<String, Assignment> = assignments.associateBy { it.id }

    val coursePerformances: List<CoursePerformance>
        get() {
            val now = System.currentTimeMillis()
            return courses.map { course ->
                val linked = assignments.filter { it.courseId == course.id }
                val graded = linked.filter { it.earnedPoints != null }
                val earned = graded.sumOf { it.earnedPoints ?: 0.0 }
                val gradedPossible = graded.sumOf { it.maxPoints }
                val plannedTotal = linked.sumOf { it.maxPoints }
                val completedCount = linked.count {
                    it.status == AssignmentStatus.SUBMITTED.value || it.status == AssignmentStatus.GRADED.value
                }
                val gradedCount = linked.count { it.status == AssignmentStatus.GRADED.value || it.earnedPoints != null }
                val pendingCount = linked.count {
                    it.status == AssignmentStatus.PENDING.value || it.status == AssignmentStatus.IN_PROGRESS.value
                }
                val overdueCount = linked.count {
                    (it.status == AssignmentStatus.PENDING.value || it.status == AssignmentStatus.IN_PROGRESS.value) &&
                        it.dueDateMillis in 1 until now
                }
                val pendingReqAttachments = linked.count {
                    (it.status == AssignmentStatus.PENDING.value || it.status == AssignmentStatus.IN_PROGRESS.value) &&
                        it.requiresAttachment &&
                        it.aiVerdict != AiVerdict.APPROVED.value
                }
                CoursePerformance(
                    course = course,
                    earnedPoints = earned,
                    gradedPossiblePoints = gradedPossible,
                    totalPlannedPoints = plannedTotal,
                    totalAssignments = linked.size,
                    completedAssignments = completedCount,
                    gradedAssignments = gradedCount,
                    pendingAssignments = pendingCount,
                    overdueAssignments = overdueCount,
                    pendingRequiredAttachments = pendingReqAttachments
                )
            }
        }

    val attentionCourses: List<CoursePerformance>
        get() = coursePerformances.filter { it.needsAttention }

    val attentionAssignments: List<AssignmentAttentionItem>
        get() {
            val now = System.currentTimeMillis()
            val fortyEightHours = 48L * 3_600_000L
            return assignments.mapNotNull { item ->
                val isDone = item.status == AssignmentStatus.SUBMITTED.value || item.status == AssignmentStatus.GRADED.value
                if (isDone) return@mapNotNull null

                val reasons = mutableListOf<String>()
                var level = 0

                if (item.dueDateMillis in 1 until now) {
                    reasons.add("Prazo vencido")
                    level = maxOf(level, 3)
                } else if (item.dueDateMillis in now..(now + fortyEightHours)) {
                    reasons.add("Entrega nas próximas 48h")
                    level = maxOf(level, 2)
                }

                if (item.aiVerdict == AiVerdict.REJECTED.value) {
                    reasons.add("Anexo rejeitado pela IA")
                    level = maxOf(level, 3)
                } else if (item.aiVerdict == AiVerdict.NEEDS_REVISION.value) {
                    reasons.add("Anexo precisa de ajustes (IA)")
                    level = maxOf(level, 2)
                } else if (item.requiresAttachment && item.aiVerdict != AiVerdict.APPROVED.value) {
                    reasons.add("Anexo obrigatório pendente")
                    level = maxOf(level, 2)
                }

                val courseMax = courseById[item.courseId]?.maxScale ?: 10.0
                if (item.priorityType == PriorityType.GRADE_A.value && item.maxPoints >= courseMax * 0.25) {
                    reasons.add("Alto peso na nota (${item.maxPoints} pts)")
                    level = maxOf(level, 1)
                }

                if (reasons.isEmpty()) null else AssignmentAttentionItem(item, reasons, level)
            }.sortedWith(
                compareByDescending<AssignmentAttentionItem> { it.urgencyLevel }
                    .thenBy { it.assignment.dueDateMillis }
            )
        }

    val completedAssignmentsCount: Int
        get() = assignments.count {
            it.status == AssignmentStatus.SUBMITTED.value || it.status == AssignmentStatus.GRADED.value
        }

    val pendingAssignmentsCount: Int
        get() = assignments.count {
            it.status == AssignmentStatus.PENDING.value || it.status == AssignmentStatus.IN_PROGRESS.value
        }

    val overallCompletionPercent: Int
        get() = if (assignments.isNotEmpty()) {
            ((completedAssignmentsCount.toDouble() / assignments.size.toDouble()) * 100.0).toInt().coerceIn(0, 100)
        } else 0

    val filteredAssignments: List<Assignment>
        get() {
            val base = assignments.filter { item ->
                (selectedInstitutionFilterId == null || item.institutionId == selectedInstitutionFilterId) &&
                    (selectedCourseFilterId == null || item.courseId == selectedCourseFilterId) &&
                    (selectedCategoryFilterId == null || item.categoryId == selectedCategoryFilterId)
            }
            return sortAssignmentsByPriority(base, activePriorityMode)
        }

    val reminderQueue: List<Assignment>
        get() {
            val pending = assignments.filter {
                it.reminderEnabled &&
                    it.status != AssignmentStatus.GRADED.value &&
                    it.status != AssignmentStatus.SUBMITTED.value
            }
            return sortAssignmentsByPriority(pending, activePriorityMode)
        }

    val overallEarnedPoints: Double
        get() = assignments.mapNotNull { it.earnedPoints }.sum()

    val overallGradedPossiblePoints: Double
        get() = assignments.filter { it.earnedPoints != null }.sumOf { it.maxPoints }

    val overallAveragePercent: Int
        get() = if (overallGradedPossiblePoints > 0.0) {
            ((overallEarnedPoints / overallGradedPossiblePoints) * 100.0).toInt().coerceIn(0, 100)
        } else 0

    companion object {
        fun sortAssignmentsByPriority(
            list: List<Assignment>,
            mode: PriorityType
        ): List<Assignment> {
            return list.sortedWith(
                compareBy<Assignment> {
                    // Pending & in_progress come before submitted & graded
                    if (it.status == AssignmentStatus.SUBMITTED.value || it.status == AssignmentStatus.GRADED.value) 1 else 0
                }.thenComparator { a, b ->
                    when (mode) {
                        PriorityType.GRADE_A -> {
                            // Priority A: Highest points weight first, then nearest deadline
                            val aMatchesMode = if (a.priorityType == PriorityType.GRADE_A.value) 0 else 1
                            val bMatchesMode = if (b.priorityType == PriorityType.GRADE_A.value) 0 else 1
                            val pointsCmp = b.maxPoints.compareTo(a.maxPoints)
                            if (pointsCmp != 0) pointsCmp
                            else if (aMatchesMode != bMatchesMode) aMatchesMode.compareTo(bMatchesMode)
                            else a.dueDateMillis.compareTo(b.dueDateMillis)
                        }
                        PriorityType.DEADLINE_B -> {
                            // Priority B: Earliest due date first, then highest points
                            val dateCmp = a.dueDateMillis.compareTo(b.dueDateMillis)
                            if (dateCmp != 0) dateCmp
                            else b.maxPoints.compareTo(a.maxPoints)
                        }
                    }
                }
            )
        }
    }
}

sealed interface AiTaskState {
    data object Idle : AiTaskState
    data class Working(val label: String) : AiTaskState
    data class RequirementReady(val analysis: AiRequirementAnalysis) : AiTaskState
    data class EvaluationCompleted(val message: String) : AiTaskState
    data class Error(val message: String) : AiTaskState
}

@OptIn(ExperimentalCoroutinesApi::class)
class AcademicViewModel(
    private val repository: AcademicRepository,
    private val aiAssistant: AcademicAiAssistant,
    private val currentUserId: String,
    initialDisplayName: String
) : ViewModel() {

    private val _priorityOverride = MutableStateFlow<PriorityType?>(null)
    private val _institutionFilter = MutableStateFlow<String?>(null)
    private val _courseFilter = MutableStateFlow<String?>(null)
    private val _categoryFilter = MutableStateFlow<String?>(null)

    private val _selectedAssignmentId = MutableStateFlow<String?>(null)
    val selectedAssignmentId: StateFlow<String?> = _selectedAssignmentId.asStateFlow()

    private val _actionError = MutableStateFlow<String?>(null)
    val actionError: StateFlow<String?> = _actionError.asStateFlow()

    private val _actionSuccessMessage = MutableStateFlow<String?>(null)
    val actionSuccessMessage: StateFlow<String?> = _actionSuccessMessage.asStateFlow()

    private val _aiTaskState = MutableStateFlow<AiTaskState>(AiTaskState.Idle)
    val aiTaskState: StateFlow<AiTaskState> = _aiTaskState.asStateFlow()

    private val _aiChatResponse = MutableStateFlow("")
    val aiChatResponse: StateFlow<String> = _aiChatResponse.asStateFlow()

    private val _isAiChatStreaming = MutableStateFlow(false)
    val isAiChatStreaming: StateFlow<Boolean> = _isAiChatStreaming.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                repository.ensureUserProfileAndDefaults(currentUserId, initialDisplayName)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Profile initialization warning", e)
                _actionError.value = e.localizedMessage ?: "Não foi possível inicializar seu perfil acadêmico."
            }
        }
    }

    private fun <T> Flow<T>.toSafeUiFlow(label: String): Flow<Result<T>> =
        this.map { Result.success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val root = firestoreError ?: error
                Log.w(TAG, "Error observing $label", root)
                emit(Result.failure(root))
            }

    val dashboardState: StateFlow<DataState<AcademicDashboardState>> = combine(
        repository.observeUserProfile(currentUserId).toSafeUiFlow("profile"),
        repository.observeInstitutions(currentUserId).toSafeUiFlow("institutions"),
        repository.observeCourses(currentUserId).toSafeUiFlow("courses"),
        repository.observeCategories(currentUserId).toSafeUiFlow("categories"),
        repository.observeAssignments(currentUserId).toSafeUiFlow("assignments"),
        repository.observeNotes(currentUserId).toSafeUiFlow("notes"),
        combine(_priorityOverride, _institutionFilter, _courseFilter, _categoryFilter) { p, i, c, cat ->
            FilterParams(p, i, c, cat)
        }
    ) { arr ->
        @Suppress("UNCHECKED_CAST")
        val profileRes = arr[0] as Result<UserProfile?>
        @Suppress("UNCHECKED_CAST")
        val instRes = arr[1] as Result<List<Institution>>
        @Suppress("UNCHECKED_CAST")
        val coursesRes = arr[2] as Result<List<Course>>
        @Suppress("UNCHECKED_CAST")
        val categoriesRes = arr[3] as Result<List<Category>>
        @Suppress("UNCHECKED_CAST")
        val assignmentsRes = arr[4] as Result<List<Assignment>>
        @Suppress("UNCHECKED_CAST")
        val notesRes = arr[5] as Result<List<StudyNote>>
        val filters = arr[6] as FilterParams

        val firstFailure = listOf(profileRes, instRes, coursesRes, categoriesRes, assignmentsRes, notesRes)
            .firstOrNull { it.isFailure }
            ?.exceptionOrNull()

        if (firstFailure != null) {
            DataState.Error(firstFailure.localizedMessage ?: "Erro ao sincronizar dados acadêmicos.")
        } else {
            val profile = profileRes.getOrNull()
            val resolvedPriority = filters.priorityOverride
                ?: profile?.defaultPriorityMode?.let { PriorityType.fromValue(it) }
                ?: PriorityType.GRADE_A

            DataState.Success(
                AcademicDashboardState(
                    profile = profile,
                    institutions = instRes.getOrDefault(emptyList()),
                    courses = coursesRes.getOrDefault(emptyList()),
                    categories = categoriesRes.getOrDefault(emptyList()),
                    assignments = assignmentsRes.getOrDefault(emptyList()),
                    notes = notesRes.getOrDefault(emptyList()),
                    activePriorityMode = resolvedPriority,
                    selectedInstitutionFilterId = filters.institutionId,
                    selectedCourseFilterId = filters.courseId,
                    selectedCategoryFilterId = filters.categoryId,
                )
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = DataState.Loading
    )

    val selectedAttachments: StateFlow<DataState<List<AssignmentAttachment>>> =
        _selectedAssignmentId.flatMapLatest { assignmentId ->
            if (assignmentId.isNullOrBlank()) {
                flowOf(DataState.Success(emptyList()))
            } else {
                repository.observeAttachments(currentUserId, assignmentId)
                    .map<List<AssignmentAttachment>, DataState<List<AssignmentAttachment>>> { DataState.Success(it) }
                    .catch { error ->
                        val firestoreError = generateSequence(error) { it.cause }
                            .filterIsInstance<FirebaseFirestoreException>()
                            .firstOrNull()
                        if (firestoreError == null && error is CancellationException) throw error
                        val root = firestoreError ?: error
                        Log.w(TAG, "Error observing attachments", root)
                        emit(DataState.Error(root.localizedMessage ?: "Falha ao carregar anexos."))
                    }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = DataState.Loading
        )

    fun selectAssignment(assignmentId: String?) {
        _selectedAssignmentId.value = assignmentId
        _aiTaskState.value = AiTaskState.Idle
        _aiChatResponse.value = ""
    }

    fun setPriorityMode(mode: PriorityType) {
        _priorityOverride.value = mode
        viewModelScope.launch {
            runCatching {
                repository.updateDefaultPriorityMode(currentUserId, mode.value)
            }
        }
    }

    fun setInstitutionFilter(id: String?) {
        _institutionFilter.value = id
        _courseFilter.value = null
    }

    fun setCourseFilter(id: String?) {
        _courseFilter.value = id
    }

    fun setCategoryFilter(id: String?) {
        _categoryFilter.value = id
    }

    fun clearMessages() {
        _actionError.value = null
        _actionSuccessMessage.value = null
    }

    fun clearAiState() {
        _aiTaskState.value = AiTaskState.Idle
    }

    // --- CRUD Operations ---
    fun saveInstitution(institution: Institution, onSaved: (String) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val id = repository.saveInstitution(currentUserId, institution)
                _actionSuccessMessage.value = "Instituição salva."
                onSaved(id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _actionError.value = e.localizedMessage ?: "Erro ao salvar instituição."
            }
        }
    }

    fun deleteInstitution(institutionId: String) {
        viewModelScope.launch {
            try {
                repository.deleteInstitution(currentUserId, institutionId)
                _actionSuccessMessage.value = "Instituição removida."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _actionError.value = e.localizedMessage ?: "Erro ao remover instituição."
            }
        }
    }

    fun saveCourse(course: Course, onSaved: (String) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val id = repository.saveCourse(currentUserId, course)
                _actionSuccessMessage.value = "Disciplina salva."
                onSaved(id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _actionError.value = e.localizedMessage ?: "Erro ao salvar disciplina."
            }
        }
    }

    fun deleteCourse(courseId: String) {
        viewModelScope.launch {
            try {
                repository.deleteCourse(currentUserId, courseId)
                _actionSuccessMessage.value = "Disciplina removida."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _actionError.value = e.localizedMessage ?: "Erro ao remover disciplina."
            }
        }
    }

    fun saveCategory(category: Category, onSaved: (String) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val id = repository.saveCategory(currentUserId, category)
                _actionSuccessMessage.value = "Categoria salva."
                onSaved(id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _actionError.value = e.localizedMessage ?: "Erro ao salvar categoria."
            }
        }
    }

    fun deleteCategory(categoryId: String) {
        viewModelScope.launch {
            try {
                repository.deleteCategory(currentUserId, categoryId)
                _actionSuccessMessage.value = "Categoria removida."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _actionError.value = e.localizedMessage ?: "Erro ao remover categoria."
            }
        }
    }

    fun saveAssignment(
        context: Context,
        assignment: Assignment,
        courseName: String,
        onSaved: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val id = repository.saveAssignment(currentUserId, assignment)
                val persisted = assignment.copy(id = id)
                if (persisted.reminderEnabled) {
                    ReminderScheduler.scheduleReminder(context, persisted, courseName)
                } else {
                    ReminderScheduler.cancelReminder(context, id)
                }
                _actionSuccessMessage.value = "Trabalho salvo com sucesso."
                onSaved(id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _actionError.value = e.localizedMessage ?: "Erro ao salvar trabalho."
            }
        }
    }

    fun updateAssignmentScoreAndStatus(
        assignment: Assignment,
        earnedPoints: Double?,
        newStatus: String,
        hasApprovedAttachment: Boolean
    ) {
        if ((newStatus == AssignmentStatus.SUBMITTED.value || newStatus == AssignmentStatus.GRADED.value) &&
            assignment.requiresAttachment &&
            !hasApprovedAttachment
        ) {
            _actionError.value =
                "Este trabalho exige um anexo aprovado pelo Assistente de IA antes de ser marcado como concluído."
            return
        }

        viewModelScope.launch {
            try {
                repository.updateAssignmentScoreAndStatus(
                    userId = currentUserId,
                    assignmentId = assignment.id,
                    earnedPoints = earnedPoints,
                    status = newStatus
                )
                _actionSuccessMessage.value = "Status e pontuação atualizados."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _actionError.value = e.localizedMessage ?: "Erro ao atualizar pontuação."
            }
        }
    }

    fun deleteAssignment(context: Context, assignmentId: String, onDeleted: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                ReminderScheduler.cancelReminder(context, assignmentId)
                repository.deleteAssignment(currentUserId, assignmentId)
                _actionSuccessMessage.value = "Trabalho removido."
                onDeleted()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _actionError.value = e.localizedMessage ?: "Erro ao remover trabalho."
            }
        }
    }

    // --- AI Requirement & Attachment Evaluation ---
    fun analyzeAssignmentRequirements(
        assignment: Assignment,
        institutionName: String,
        courseName: String,
        categoryName: String
    ) {
        viewModelScope.launch {
            _aiTaskState.value = AiTaskState.Working("Analisando enunciado para definir anexos exigidos...")
            try {
                val analysis = aiAssistant.analyzeAttachmentRequirements(
                    institutionName = institutionName,
                    courseName = courseName,
                    categoryName = categoryName,
                    title = assignment.title,
                    description = assignment.description,
                    maxPoints = assignment.maxPoints
                )
                val formattedSpec = buildString {
                    append(analysis.requirementSummary)
                    if (analysis.requiredChecklist.isNotEmpty()) {
                        append("\nChecklist exigido:\n")
                        analysis.requiredChecklist.forEach { item -> append("• $item\n") }
                    }
                }.trim()

                repository.updateAssignmentAiEvaluation(
                    userId = currentUserId,
                    assignmentId = assignment.id,
                    requiresAttachment = analysis.requiresAttachment,
                    requiredAttachmentSpec = formattedSpec,
                    aiVerdict = if (analysis.requiresAttachment && assignment.aiVerdict == AiVerdict.NONE.value) {
                        AiVerdict.MISSING_REQUIREMENTS.value
                    } else assignment.aiVerdict,
                    aiSummary = if (assignment.aiSummary.isBlank()) analysis.rationale else assignment.aiSummary,
                    aiEstimatedPercent = assignment.aiEstimatedPercent
                )
                _aiTaskState.value = AiTaskState.RequirementReady(analysis)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _aiTaskState.value = AiTaskState.Error(e.localizedMessage ?: "Erro na análise de requisitos da IA.")
            }
        }
    }

    fun attachFileAndEvaluateWithAi(
        context: Context,
        uri: Uri,
        textNote: String,
        assignment: Assignment,
        institutionName: String,
        courseName: String,
        categoryName: String,
        autoEvaluate: Boolean = true
    ) {
        viewModelScope.launch {
            _aiTaskState.value = AiTaskState.Working("Salvando anexo e preparando análise...")
            try {
                val saved = AttachmentStorageHelper.copyUriToPrivateStorage(context, uri)
                val attachment = AssignmentAttachment(
                    userId = currentUserId,
                    assignmentId = assignment.id,
                    fileName = saved.fileName,
                    mimeType = saved.mimeType,
                    sizeBytes = saved.sizeBytes,
                    localFilePath = saved.localFilePath,
                    textNote = textNote,
                    aiStatus = "pending"
                )
                val attachmentId = repository.saveAttachment(currentUserId, assignment.id, attachment)
                val persistedAttachment = attachment.copy(id = attachmentId)

                if (autoEvaluate) {
                    evaluateAttachmentInternal(
                        assignment = assignment,
                        attachment = persistedAttachment,
                        institutionName = institutionName,
                        courseName = courseName,
                        categoryName = categoryName
                    )
                } else {
                    _aiTaskState.value = AiTaskState.EvaluationCompleted("Anexo salvo com sucesso.")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _aiTaskState.value = AiTaskState.Error(e.localizedMessage ?: "Não foi possível anexar o arquivo.")
            }
        }
    }

    fun attachTextNoteAndEvaluate(
        title: String,
        contentText: String,
        assignment: Assignment,
        institutionName: String,
        courseName: String,
        categoryName: String
    ) {
        viewModelScope.launch {
            _aiTaskState.value = AiTaskState.Working("Avaliando resposta em texto com a IA...")
            try {
                val attachment = AssignmentAttachment(
                    userId = currentUserId,
                    assignmentId = assignment.id,
                    fileName = "${title.ifBlank { "Entrega_Texto" }}.txt",
                    mimeType = "text/plain",
                    sizeBytes = contentText.toByteArray().size.toLong(),
                    localFilePath = "",
                    textNote = contentText,
                    aiStatus = "pending"
                )
                val attachmentId = repository.saveAttachment(currentUserId, assignment.id, attachment)
                evaluateAttachmentInternal(
                    assignment = assignment,
                    attachment = attachment.copy(id = attachmentId),
                    institutionName = institutionName,
                    courseName = courseName,
                    categoryName = categoryName
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _aiTaskState.value = AiTaskState.Error(e.localizedMessage ?: "Erro ao enviar resposta para avaliação.")
            }
        }
    }

    fun reEvaluateAttachment(
        assignment: Assignment,
        attachment: AssignmentAttachment,
        institutionName: String,
        courseName: String,
        categoryName: String
    ) {
        viewModelScope.launch {
            _aiTaskState.value = AiTaskState.Working("Reavaliando anexo '${attachment.fileName}' com Gemini...")
            try {
                evaluateAttachmentInternal(
                    assignment = assignment,
                    attachment = attachment,
                    institutionName = institutionName,
                    courseName = courseName,
                    categoryName = categoryName
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _aiTaskState.value = AiTaskState.Error(e.localizedMessage ?: "Falha na reavaliação da IA.")
            }
        }
    }

    private suspend fun evaluateAttachmentInternal(
        assignment: Assignment,
        attachment: AssignmentAttachment,
        institutionName: String,
        courseName: String,
        categoryName: String
    ) {
        _aiTaskState.value = AiTaskState.Working("Gemini analisando '${attachment.fileName}' contra o enunciado...")
        val eval = aiAssistant.evaluateAttachment(
            institutionName = institutionName,
            courseName = courseName,
            categoryName = categoryName,
            assignment = assignment,
            attachment = attachment
        )

        val attachmentStatus = when (eval.verdict) {
            "approved" -> "approved"
            "rejected" -> "rejected"
            else -> "needs_revision"
        }

        repository.updateAttachmentAiEvaluation(
            userId = currentUserId,
            assignmentId = assignment.id,
            attachmentId = attachment.id,
            aiStatus = attachmentStatus,
            aiFeedback = eval.feedbackSummary,
            aiEstimatedPercent = eval.estimatedScorePercent.toLong(),
            strengths = eval.strengths,
            gaps = eval.gaps,
            nextSteps = eval.nextSteps
        )

        repository.updateAssignmentAiEvaluation(
            userId = currentUserId,
            assignmentId = assignment.id,
            requiresAttachment = assignment.requiresAttachment,
            requiredAttachmentSpec = assignment.requiredAttachmentSpec,
            aiVerdict = eval.verdict,
            aiSummary = eval.feedbackSummary,
            aiEstimatedPercent = eval.estimatedScorePercent.toLong()
        )

        _aiTaskState.value = AiTaskState.EvaluationCompleted(
            "Avaliação concluída: ${AiVerdict.fromValue(eval.verdict).labelPt} (${eval.estimatedScorePercent}%)"
        )
    }

    fun deleteAttachment(assignmentId: String, attachmentId: String) {
        viewModelScope.launch {
            try {
                repository.deleteAttachment(currentUserId, assignmentId, attachmentId)
                _actionSuccessMessage.value = "Anexo removido."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _actionError.value = e.localizedMessage ?: "Erro ao remover anexo."
            }
        }
    }

    fun askAiQuestion(courseName: String, assignment: Assignment, question: String) {
        if (question.isBlank()) return
        viewModelScope.launch {
            _isAiChatStreaming.value = true
            _aiChatResponse.value = ""
            try {
                aiAssistant.askAboutAssignmentStream(courseName, assignment, question)
                    .collect { partial ->
                        _aiChatResponse.value = partial
                    }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _aiTaskState.value = AiTaskState.Error(e.localizedMessage ?: "Erro no chat com o assistente.")
            } finally {
                _isAiChatStreaming.value = false
            }
        }
    }

    // --- Quick Study Notes ---
    fun saveNote(note: StudyNote, onSaved: (String) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val id = repository.saveNote(currentUserId, note)
                _actionSuccessMessage.value = "Anotação salva."
                onSaved(id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _actionError.value = e.localizedMessage ?: "Erro ao salvar anotação."
            }
        }
    }

    fun toggleNotePin(note: StudyNote) {
        viewModelScope.launch {
            try {
                repository.toggleNotePin(currentUserId, note.id, !note.pinned)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _actionError.value = e.localizedMessage ?: "Erro ao fixar anotação."
            }
        }
    }

    fun deleteNote(noteId: String) {
        viewModelScope.launch {
            try {
                repository.deleteNote(currentUserId, noteId)
                _actionSuccessMessage.value = "Anotação removida."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _actionError.value = e.localizedMessage ?: "Erro ao remover anotação."
            }
        }
    }

    private data class FilterParams(
        val priorityOverride: PriorityType?,
        val institutionId: String?,
        val courseId: String?,
        val categoryId: String?
    )

    private companion object {
        const val TAG = "AcademicVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
