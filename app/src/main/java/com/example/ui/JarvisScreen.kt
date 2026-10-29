package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.model.AssistantState
import com.example.ui.components.ArcReactorView
import com.example.ui.components.ChatList
import com.example.ui.components.ControlPanel
import com.example.ui.components.JarvisSettingsDialog
import com.example.ui.theme.JarvisAccentGreen
import com.example.ui.theme.JarvisAccentRed
import com.example.ui.theme.JarvisBackground
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisCyanLight
import com.example.ui.theme.JarvisSurface
import com.example.ui.theme.JarvisTextPrimary
import com.example.viewmodel.JarvisViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JarvisScreen(
    viewModel: JarvisViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showSettingsDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Audio permission request launcher
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startVoiceInput()
        } else {
            Toast.makeText(
                context,
                "Microphone permission is required for voice input.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    fun handleStartVoice() {
        if (uiState.assistantState == AssistantState.LISTENING) {
            viewModel.stopVoice()
            return
        }

        val hasAudioPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasAudioPermission) {
            viewModel.startVoiceInput()
        } else {
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = JarvisBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "🤖 JARVIS AI",
                            color = JarvisCyan,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            modifier = Modifier.testTag("app_header_title")
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showSettingsDialog = true },
                        modifier = Modifier.testTag("open_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Jarvis Settings",
                            tint = JarvisCyanLight
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = JarvisSurface
                ),
                modifier = Modifier.statusBarsPadding()
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Arc Reactor Centerpiece
            ArcReactorView(
                state = uiState.assistantState,
                audioLevel = uiState.audioLevel,
                isSpeaking = uiState.isTtsSpeaking,
                onClick = { handleStartVoice() },
                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
            )

            // Subtitle
            Text(
                text = "Your Personal AI Assistant",
                color = JarvisTextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )

            // Status Indicator (Matching <p id="status"> from prototype)
            StatusIndicatorBadge(state = uiState.assistantState)

            Spacer(modifier = Modifier.height(6.dp))

            // Chat Messages History
            ChatList(
                messages = uiState.messages,
                onSpeakMessage = { viewModel.speakMessage(it) },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .widthIn(max = 600.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Control & Input Panel
            ControlPanel(
                inputText = uiState.inputText,
                onInputTextChanged = { viewModel.onInputTextChanged(it) },
                onSend = { viewModel.sendMessage() },
                onStartVoice = { handleStartVoice() },
                onStopVoice = { viewModel.stopVoice() },
                assistantState = uiState.assistantState,
                isSpeaking = uiState.isTtsSpeaking,
                onSuggestionSelected = { suggestion ->
                    viewModel.sendMessage(suggestion)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp)
            )
        }
    }

    if (showSettingsDialog) {
        JarvisSettingsDialog(
            speechRate = uiState.speechRate,
            speechLanguage = uiState.speechLanguage,
            autoSpeak = uiState.autoSpeak,
            isGeminiConnected = uiState.isGeminiConnected,
            customApiKey = uiState.customApiKey,
            onSpeechRateChange = { viewModel.setSpeechRate(it) },
            onLanguageChange = { viewModel.setSpeechLanguage(it) },
            onAutoSpeakChange = { viewModel.setAutoSpeak(it) },
            onCustomApiKeyChange = { viewModel.setCustomApiKey(it) },
            onClearChat = { viewModel.clearChat() },
            onDismiss = { showSettingsDialog = false }
        )
    }
}

@Composable
private fun StatusIndicatorBadge(state: AssistantState) {
    val (statusColor, statusText) = when (state) {
        AssistantState.READY -> JarvisCyan to "Assistant Ready"
        AssistantState.LISTENING -> JarvisAccentGreen to "Sun raha hoon..."
        AssistantState.THINKING -> JarvisCyanLight to "AI soch raha hai..."
        AssistantState.SPEAKING -> JarvisCyan to "Bol raha hoon..."
        AssistantState.STOPPED -> Color(0xFF6C87A3) to "Voice Stopped"
        AssistantState.ERROR -> JarvisAccentRed to "Connection Required"
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0C1B2E))
            .border(1.dp, statusColor.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .testTag("assistant_status_badge")
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(statusColor)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = statusText,
            color = statusColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
