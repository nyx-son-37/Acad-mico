package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Assignment
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.NoteAlt
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.core.configureAppCheck
import com.example.data.ai.AcademicAiAssistant
import com.example.data.repository.AcademicRepository
import com.example.domain.model.Assignment
import com.example.notifications.ReminderReceiver
import com.example.notifications.ReminderScheduler
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.authStateFlow
import com.example.ui.auth.signOutUser
import com.example.ui.components.EmptyAcademicState
import com.example.ui.components.OxfordLibraryBackdrop
import com.example.ui.screens.AcademicStructureScreen
import com.example.ui.screens.AssignmentDetailScreen
import com.example.ui.screens.AssignmentEditorDialog
import com.example.ui.screens.AssignmentsAndRemindersScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ProgressScreen
import com.example.ui.screens.QuickNotesScreen
import com.example.ui.theme.AcademicoTheme
import com.example.ui.theme.OxfordThemeSettings
import com.example.ui.theme.ThemePreferencesRepository
import com.example.ui.theme.ThemeSettingsDialog
import com.example.ui.viewmodel.AcademicViewModel
import com.example.ui.viewmodel.DataState
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

object Routes {
    const val DASHBOARD = "dashboard"
    const val PROGRESS = "progress"
    const val ASSIGNMENTS = "assignments"
    const val REMINDERS = "reminders"
    const val NOTES = "notes"
    const val STRUCTURE = "structure"
    const val DETAIL = "assignment_detail/{assignmentId}"

    fun detail(assignmentId: String) = "assignment_detail/$assignmentId"
}

private enum class TopNavTab(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    DASHBOARD(Routes.DASHBOARD, "Painel", Icons.Rounded.Home),
    PROGRESS(Routes.PROGRESS, "Progresso", Icons.Rounded.Insights),
    ASSIGNMENTS(Routes.ASSIGNMENTS, "Trabalhos", Icons.AutoMirrored.Rounded.Assignment),
    NOTES(Routes.NOTES, "Anotações", Icons.Rounded.NoteAlt),
    STRUCTURE(Routes.STRUCTURE, "Acadêmico", Icons.Rounded.AccountBalance)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Step 1: Configure Firebase App Check with launch intent before UI or Firebase calls
        configureAppCheck(this, intent)
        ReminderScheduler.ensureChannel(this)
        enableEdgeToEdge()

        val initialAssignmentId = intent?.getStringExtra(ReminderReceiver.EXTRA_ASSIGNMENT_ID)

        setContent {
            val appContext = applicationContext
            val themeRepo = remember(appContext) { ThemePreferencesRepository(appContext) }
            val themeSettings by themeRepo.settingsFlow.collectAsStateWithLifecycle(
                initialValue = OxfordThemeSettings()
            )
            val themeScope = rememberCoroutineScope()
            var showThemeSettings by remember { mutableStateOf(false) }

            AcademicoTheme(settings = themeSettings) {
                if (showThemeSettings) {
                    ThemeSettingsDialog(
                        settings = themeSettings,
                        onSelectAcademiaMode = { mode ->
                            themeScope.launch { themeRepo.updateAcademiaMode(mode) }
                        },
                        onSelectPalette = { palette ->
                            themeScope.launch { themeRepo.updatePalette(palette) }
                        },
                        onSelectTypographyMood = { mood ->
                            themeScope.launch { themeRepo.updateTypographyMood(mood) }
                        },
                        onTogglePaperTexture = { enabled ->
                            themeScope.launch { themeRepo.updatePaperTexture(enabled) }
                        },
                        onDismiss = { showThemeSettings = false }
                    )
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(
                        initialAssignmentId = initialAssignmentId,
                        onOpenThemeSettings = { showThemeSettings = true }
                    )
                }
            }
        }
    }
}

@Composable
fun AppNavigation(
    initialAssignmentId: String?,
    onOpenThemeSettings: () -> Unit
) {
    val auth = Firebase.auth
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)

    val user = currentUser
    if (user == null) {
        AuthScreen(
            onOpenThemeSettings = onOpenThemeSettings,
            onAuthSuccess = {}
        )
    } else {
        val uid = user.uid
        val displayName = user.displayName.orEmpty().ifBlank { user.email.orEmpty() }
        key(uid) {
            MainFeatureScreen(
                currentUserId = uid,
                userDisplayName = displayName,
                initialAssignmentId = initialAssignmentId,
                onOpenThemeSettings = onOpenThemeSettings
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainFeatureScreen(
    currentUserId: String,
    userDisplayName: String,
    initialAssignmentId: String?,
    onOpenThemeSettings: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: AcademicViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                AcademicViewModel(
                    repository = AcademicRepository(db),
                    aiAssistant = AcademicAiAssistant(),
                    currentUserId = currentUserId,
                    initialDisplayName = userDisplayName
                )
            }
        }
    )

    val dashboardState by viewModel.dashboardState.collectAsStateWithLifecycle()
    val attachmentsState by viewModel.selectedAttachments.collectAsStateWithLifecycle()
    val aiTaskState by viewModel.aiTaskState.collectAsStateWithLifecycle()
    val aiChatResponse by viewModel.aiChatResponse.collectAsStateWithLifecycle()
    val isAiChatStreaming by viewModel.isAiChatStreaming.collectAsStateWithLifecycle()
    val actionError by viewModel.actionError.collectAsStateWithLifecycle()
    val actionSuccess by viewModel.actionSuccessMessage.collectAsStateWithLifecycle()

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val snackbarHostState = remember { SnackbarHostState() }
    var editingAssignment by remember { mutableStateOf<Assignment?>(null) }
    var showAssignmentEditor by remember { mutableStateOf(false) }

    LaunchedEffect(initialAssignmentId) {
        if (!initialAssignmentId.isNullOrBlank()) {
            viewModel.selectAssignment(initialAssignmentId)
            navController.navigate(Routes.detail(initialAssignmentId))
        }
    }

    LaunchedEffect(actionError) {
        val msg = actionError
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(actionSuccess) {
        val msg = actionSuccess
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessages()
        }
    }

    val successState = (dashboardState as? DataState.Success)?.data
    if (showAssignmentEditor && successState != null) {
        AssignmentEditorDialog(
            initialAssignment = editingAssignment,
            institutions = successState.institutions,
            courses = successState.courses,
            categories = successState.categories,
            onDismiss = { showAssignmentEditor = false },
            onSave = { assignment, courseName ->
                viewModel.saveAssignment(context, assignment, courseName) {
                    showAssignmentEditor = false
                }
            }
        )
    }

    val isDetailRoute = currentRoute?.startsWith("assignment_detail") == true
    val navigateTab: (String) -> Unit = { targetRoute ->
        navController.navigate(targetRoute) {
            popUpTo(Routes.DASHBOARD) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    OxfordLibraryBackdrop {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useRail = maxWidth >= 600.dp

            Scaffold(
                containerColor = Color.Transparent,
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                topBar = {
                    if (!isDetailRoute) {
                        TopAppBar(
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                            ),
                            title = {
                                Text(
                                    text = stringResource(R.string.app_name),
                                    style = MaterialTheme.typography.headlineSmall
                                )
                            },
                            actions = {
                                IconButton(
                                    onClick = onOpenThemeSettings,
                                    modifier = Modifier.testTag("theme_settings_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Palette,
                                        contentDescription = "Configurações de tema Oxford Library"
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        signOutUser(credentialManager, scope)
                                    },
                                    modifier = Modifier.testTag("sign_out_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.Logout,
                                        contentDescription = "Sair da conta"
                                    )
                                }
                            }
                        )
                    }
                },
                bottomBar = {
                    if (!isDetailRoute && !useRail) {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
                        ) {
                            TopNavTab.entries.forEach { tab ->
                                val isSelected = currentRoute == tab.route ||
                                    (tab == TopNavTab.ASSIGNMENTS && currentRoute == Routes.REMINDERS)
                                NavigationBarItem(
                                    selected = isSelected,
                                    onClick = { navigateTab(tab.route) },
                                    icon = { Icon(tab.icon, contentDescription = tab.label) },
                                    label = {
                                        Text(
                                            text = tab.label,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    modifier = Modifier.testTag("nav_${tab.route}")
                                )
                            }
                        }
                    }
                },
                floatingActionButton = {
                    if (!isDetailRoute && currentRoute != Routes.NOTES && successState != null && successState.courses.isNotEmpty()) {
                        FloatingActionButton(
                            onClick = {
                                editingAssignment = null
                                showAssignmentEditor = true
                            },
                            shape = RoundedCornerShape(12.dp),
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.testTag("fab_new_assignment")
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = "Novo trabalho")
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (!isDetailRoute && useRail) {
                        NavigationRail(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                        ) {
                            TopNavTab.entries.forEach { tab ->
                                val isSelected = currentRoute == tab.route ||
                                    (tab == TopNavTab.ASSIGNMENTS && currentRoute == Routes.REMINDERS)
                                NavigationRailItem(
                                    selected = isSelected,
                                    onClick = { navigateTab(tab.route) },
                                    icon = { Icon(tab.icon, contentDescription = tab.label) },
                                    label = { Text(tab.label) }
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        when (val state = dashboardState) {
                            is DataState.Loading -> {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator()
                                }
                            }
                            is DataState.Error -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    EmptyAcademicState(
                                        title = "Erro ao carregar dados",
                                        description = state.message
                                    )
                                }
                            }
                            is DataState.Success -> {
                                val data = state.data
                                NavHost(
                                    navController = navController,
                                    startDestination = Routes.DASHBOARD
                                ) {
                                    composable(Routes.DASHBOARD) {
                                        DashboardScreen(
                                            state = data,
                                            userName = userDisplayName,
                                            onSelectPriorityMode = viewModel::setPriorityMode,
                                            onOpenAssignment = { id ->
                                                viewModel.selectAssignment(id)
                                                navController.navigate(Routes.detail(id))
                                            },
                                            onCreateAssignment = {
                                                editingAssignment = null
                                                showAssignmentEditor = true
                                            },
                                            onSaveQuickNote = { viewModel.saveNote(it) },
                                            onNavigateToStructure = { navigateTab(Routes.STRUCTURE) },
                                            onNavigateToReminders = { navigateTab(Routes.REMINDERS) },
                                            onNavigateToProgress = { navigateTab(Routes.PROGRESS) },
                                            onNavigateToNotes = { navigateTab(Routes.NOTES) },
                                            onSelectCourseFilterAndOpenAssignments = { courseId ->
                                                viewModel.setCourseFilter(courseId)
                                                navigateTab(Routes.ASSIGNMENTS)
                                            }
                                        )
                                    }

                                    composable(Routes.PROGRESS) {
                                        ProgressScreen(
                                            state = data,
                                            onOpenAssignment = { id ->
                                                viewModel.selectAssignment(id)
                                                navController.navigate(Routes.detail(id))
                                            },
                                            onOpenCourseAssignments = { courseId ->
                                                viewModel.setCourseFilter(courseId)
                                                navigateTab(Routes.ASSIGNMENTS)
                                            },
                                            onNavigateToStructure = { navigateTab(Routes.STRUCTURE) }
                                        )
                                    }

                                    composable(Routes.ASSIGNMENTS) {
                                        AssignmentsAndRemindersScreen(
                                            state = data,
                                            initialSubTab = 0,
                                            onSelectPriorityMode = viewModel::setPriorityMode,
                                            onSelectInstitutionFilter = viewModel::setInstitutionFilter,
                                            onSelectCourseFilter = viewModel::setCourseFilter,
                                            onSelectCategoryFilter = viewModel::setCategoryFilter,
                                            onOpenAssignment = { id ->
                                                viewModel.selectAssignment(id)
                                                navController.navigate(Routes.detail(id))
                                            },
                                            onCreateAssignment = {
                                                editingAssignment = null
                                                showAssignmentEditor = true
                                            }
                                        )
                                    }

                                    composable(Routes.REMINDERS) {
                                        AssignmentsAndRemindersScreen(
                                            state = data,
                                            initialSubTab = 1,
                                            onSelectPriorityMode = viewModel::setPriorityMode,
                                            onSelectInstitutionFilter = viewModel::setInstitutionFilter,
                                            onSelectCourseFilter = viewModel::setCourseFilter,
                                            onSelectCategoryFilter = viewModel::setCategoryFilter,
                                            onOpenAssignment = { id ->
                                                viewModel.selectAssignment(id)
                                                navController.navigate(Routes.detail(id))
                                            },
                                            onCreateAssignment = {
                                                editingAssignment = null
                                                showAssignmentEditor = true
                                            }
                                        )
                                    }

                                    composable(Routes.NOTES) {
                                        QuickNotesScreen(
                                            state = data,
                                            onSaveNote = { viewModel.saveNote(it) },
                                            onTogglePin = viewModel::toggleNotePin,
                                            onDeleteNote = viewModel::deleteNote,
                                            onOpenAssignment = { id ->
                                                viewModel.selectAssignment(id)
                                                navController.navigate(Routes.detail(id))
                                            }
                                        )
                                    }

                                    composable(Routes.STRUCTURE) {
                                        AcademicStructureScreen(
                                            state = data,
                                            onSaveInstitution = { viewModel.saveInstitution(it) },
                                            onDeleteInstitution = viewModel::deleteInstitution,
                                            onSaveCourse = { viewModel.saveCourse(it) },
                                            onDeleteCourse = viewModel::deleteCourse,
                                            onSaveCategory = { viewModel.saveCategory(it) },
                                            onDeleteCategory = viewModel::deleteCategory
                                        )
                                    }

                                    composable(
                                        route = Routes.DETAIL,
                                        arguments = listOf(navArgument("assignmentId") { type = NavType.StringType })
                                    ) { entry ->
                                        val assignmentId = entry.arguments?.getString("assignmentId").orEmpty()
                                        val assignment = data.assignments.firstOrNull { it.id == assignmentId }

                                        LaunchedEffect(assignmentId) {
                                            viewModel.selectAssignment(assignmentId)
                                        }

                                        if (assignment == null) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(24.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                EmptyAcademicState(
                                                    title = "Trabalho não encontrado",
                                                    description = "Este trabalho pode ter sido removido.",
                                                    actionLabel = "Voltar",
                                                    onAction = { navController.popBackStack() }
                                                )
                                            }
                                        } else {
                                            val institution = data.institutionById[assignment.institutionId]
                                            val course = data.courseById[assignment.courseId]
                                            val category = data.categoryById[assignment.categoryId]
                                            val instName = institution?.name ?: "Instituição"
                                            val courseName = course?.name ?: "Disciplina"
                                            val catName = category?.name ?: "Geral"
                                            val linkedNotes = remember(data.notes, assignment.id) {
                                                data.notes.filter { it.assignmentId == assignment.id }
                                            }

                                            AssignmentDetailScreen(
                                                assignment = assignment,
                                                institution = institution,
                                                course = course,
                                                category = category,
                                                allCourses = data.courses,
                                                allAssignments = data.assignments,
                                                assignmentNotes = linkedNotes,
                                                attachmentsState = attachmentsState,
                                                aiTaskState = aiTaskState,
                                                aiChatResponse = aiChatResponse,
                                                isAiChatStreaming = isAiChatStreaming,
                                                onBack = { navController.popBackStack() },
                                                onEditAssignment = {
                                                    editingAssignment = assignment
                                                    showAssignmentEditor = true
                                                },
                                                onDeleteAssignment = {
                                                    viewModel.deleteAssignment(context, assignment.id) {
                                                        navController.popBackStack()
                                                    }
                                                },
                                                onUpdateScoreAndStatus = { score, status, hasApproved ->
                                                    viewModel.updateAssignmentScoreAndStatus(
                                                        assignment = assignment,
                                                        earnedPoints = score,
                                                        newStatus = status,
                                                        hasApprovedAttachment = hasApproved
                                                    )
                                                },
                                                onAnalyzeRequirements = {
                                                    viewModel.analyzeAssignmentRequirements(
                                                        assignment = assignment,
                                                        institutionName = instName,
                                                        courseName = courseName,
                                                        categoryName = catName
                                                    )
                                                },
                                                onAttachUri = { uri, note ->
                                                    viewModel.attachFileAndEvaluateWithAi(
                                                        context = context,
                                                        uri = uri,
                                                        textNote = note,
                                                        assignment = assignment,
                                                        institutionName = instName,
                                                        courseName = courseName,
                                                        categoryName = catName
                                                    )
                                                },
                                                onAttachTextDraft = { title, content ->
                                                    viewModel.attachTextNoteAndEvaluate(
                                                        title = title,
                                                        contentText = content,
                                                        assignment = assignment,
                                                        institutionName = instName,
                                                        courseName = courseName,
                                                        categoryName = catName
                                                    )
                                                },
                                                onReEvaluateAttachment = { att ->
                                                    viewModel.reEvaluateAttachment(
                                                        assignment = assignment,
                                                        attachment = att,
                                                        institutionName = instName,
                                                        courseName = courseName,
                                                        categoryName = catName
                                                    )
                                                },
                                                onDeleteAttachment = { attId ->
                                                    viewModel.deleteAttachment(assignment.id, attId)
                                                },
                                                onAskAiQuestion = { q ->
                                                    viewModel.askAiQuestion(courseName, assignment, q)
                                                },
                                                onDismissAiState = viewModel::clearAiState,
                                                onSaveNote = { viewModel.saveNote(it) },
                                                onTogglePinNote = viewModel::toggleNotePin,
                                                onDeleteNote = viewModel::deleteNote
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
