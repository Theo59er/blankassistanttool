package com.example

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.screens.CalendarScreen
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppScreen(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    val calendarPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val readGranted = permissions[Manifest.permission.READ_CALENDAR] == true
        val writeGranted = permissions[Manifest.permission.WRITE_CALENDAR] == true
        viewModel.onCalendarPermissionResult(readGranted && writeGranted)
    }

    LaunchedEffect(Unit) {
        viewModel.refreshDiagnostics()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (selectedTab) {
                            0 -> "OpenClaw Assistant"
                            1 -> "Google Kalender"
                            else -> "OpenClaw Bridge & Setup"
                        },
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("bottom_navigation_bar"),
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.ChatBubble, contentDescription = "Assistent") },
                    label = { Text("Chat") },
                    modifier = Modifier.testTag("nav_item_chat")
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.CalendarToday, contentDescription = "Kalender") },
                    label = { Text("Kalender") },
                    modifier = Modifier.testTag("nav_item_calendar")
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Einstellungen") },
                    label = { Text("Setup") },
                    modifier = Modifier.testTag("nav_item_settings")
                )
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> ChatScreen(
                    uiState = uiState,
                    onSendMessage = { prompt -> viewModel.sendUserMessage(prompt) },
                    onTestConnection = { viewModel.testConnection() },
                    onNavigateToCalendar = { selectedTab = 1 },
                    onDeleteEvent = { eventId -> viewModel.deleteCalendarEvent(eventId) }
                )
                1 -> CalendarScreen(
                    uiState = uiState,
                    onRequestCalendarPermission = {
                        calendarPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.READ_CALENDAR,
                                Manifest.permission.WRITE_CALENDAR
                            )
                        )
                    },
                    onCreateEvent = { title, desc, start, end, loc ->
                        viewModel.createCalendarEvent(title, desc, start, end, loc)
                    },
                    onDeleteEvent = { eventId ->
                        viewModel.deleteCalendarEvent(eventId)
                    },
                    onSyncWithOpenClaw = {
                        viewModel.syncCalendarWithOpenClaw()
                    }
                )
                2 -> SettingsScreen(
                    uiState = uiState,
                    onSaveConfig = { provider, host, port, model, token, prompt ->
                        viewModel.updateServerConfig(provider, host, port, model, token, prompt)
                    },
                    onSelectProvider = { provider ->
                        viewModel.setAiProvider(provider)
                    },
                    onSelectModel = { modelName ->
                        viewModel.selectModel(modelName)
                    },
                    onTestConnection = {
                        viewModel.testConnection()
                    },
                    onRefreshDiagnostics = {
                        viewModel.refreshDiagnostics()
                    },
                    onSendDeviceReport = {
                        viewModel.sendDeviceReportToOpenClaw()
                    },
                    onUpdateEmail = { email ->
                        viewModel.updateGoogleEmail(email)
                    }
                )
            }
        }
    }
}

/**
 * Kept for testing and screenshot verification compatibility
 */
@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}
