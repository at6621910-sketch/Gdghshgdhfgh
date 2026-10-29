package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.JarvisAccentGreen
import com.example.ui.theme.JarvisAccentRed
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisCyanLight
import com.example.ui.theme.JarvisSurface
import com.example.ui.theme.JarvisSurfaceVariant
import com.example.ui.theme.JarvisTextMuted
import com.example.ui.theme.JarvisTextPrimary

@Composable
fun JarvisSettingsDialog(
    speechRate: Float,
    speechLanguage: String,
    autoSpeak: Boolean,
    isGeminiConnected: Boolean,
    customApiKey: String,
    onSpeechRateChange: (Float) -> Unit,
    onLanguageChange: (String) -> Unit,
    onAutoSpeakChange: (Boolean) -> Unit,
    onCustomApiKeyChange: (String) -> Unit,
    onClearChat: () -> Unit,
    onDismiss: () -> Unit
) {
    var rate by remember { mutableFloatStateOf(speechRate) }
    var keyInput by remember { mutableStateOf(customApiKey) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, Color(0x3300EAFF), RoundedCornerShape(16.dp))
            .testTag("settings_dialog"),
        containerColor = JarvisSurface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "⚙️ Jarvis Settings",
                    color = JarvisCyan,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Language Selection
                Text(
                    text = "Voice Language Preference",
                    color = JarvisTextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val langs = listOf(
                        "auto" to "Auto",
                        "hi-IN" to "Hindi",
                        "en-IN" to "English"
                    )
                    langs.forEach { (code, label) ->
                        val isSelected = speechLanguage.equals(code, ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = { onLanguageChange(code) },
                            label = { Text(label, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = JarvisCyan,
                                selectedLabelColor = Color(0xFF00111B),
                                containerColor = JarvisSurfaceVariant,
                                labelColor = JarvisTextPrimary
                            ),
                            modifier = Modifier.testTag("lang_chip_$code")
                        )
                    }
                }

                // Speech Rate Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Speech Rate",
                            color = JarvisTextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = String.format("%.2fx", rate),
                            color = JarvisCyanLight,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Slider(
                        value = rate,
                        onValueChange = {
                            rate = it
                            onSpeechRateChange(it)
                        },
                        valueRange = 0.7f..1.5f,
                        steps = 7,
                        colors = SliderDefaults.colors(
                            thumbColor = JarvisCyan,
                            activeTrackColor = JarvisCyan,
                            inactiveTrackColor = Color(0xFF1B324D)
                        ),
                        modifier = Modifier.testTag("speech_rate_slider")
                    )
                }

                // Auto-speak responses toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Auto-speak Replies",
                            color = JarvisTextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Vocalize Jarvis answers automatically",
                            color = JarvisTextMuted,
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = autoSpeak,
                        onCheckedChange = onAutoSpeakChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF00111B),
                            checkedTrackColor = JarvisCyan,
                            uncheckedThumbColor = JarvisTextMuted,
                            uncheckedTrackColor = JarvisSurfaceVariant
                        ),
                        modifier = Modifier.testTag("auto_speak_switch")
                    )
                }

                // Gemini Intelligence Status
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(JarmonizedStatusBackground(isGeminiConnected))
                        .padding(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isGeminiConnected) Icons.Default.CheckCircle else Icons.Default.Info,
                            contentDescription = null,
                            tint = if (isGeminiConnected) JarvisAccentGreen else JarvisCyanLight,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isGeminiConnected) "Gemini AI: Connected" else "Intelligence: Built-in Brain Active",
                            color = if (isGeminiConnected) JarvisAccentGreen else JarvisCyanLight,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = if (isGeminiConnected)
                            "Online generative intelligence enabled via Gemini API."
                        else
                            "Offline smart brain active. For deep web questions, enter a Gemini API Key or configure it in AI Studio Secrets.",
                        color = JarvisTextMuted,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = keyInput,
                        onValueChange = {
                            keyInput = it
                            onCustomApiKeyChange(it)
                        },
                        placeholder = { Text("Custom Gemini API Key (optional)", fontSize = 11.sp, color = JarvisTextMuted) },
                        leadingIcon = {
                            Icon(Icons.Default.VpnKey, contentDescription = null, tint = JarvisCyan, modifier = Modifier.size(14.dp))
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF07111F),
                            unfocusedContainerColor = Color(0xFF07111F),
                            focusedBorderColor = JarvisCyan,
                            unfocusedBorderColor = Color(0x3300EAFF),
                            focusedTextColor = JarvisTextPrimary,
                            unfocusedTextColor = JarvisTextPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_api_key_field")
                    )
                }

                // Clear Chat button
                OutlinedButton(
                    onClick = {
                        onClearChat()
                        onDismiss()
                    },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = JarvisAccentRed
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("clear_chat_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "Clear Chat",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Clear Conversation History", fontSize = 13.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = JarvisCyan,
                    contentColor = Color(0xFF00111B)
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("close_settings_button")
            ) {
                Text("Done", fontWeight = FontWeight.Bold)
            }
        }
    )
}

private fun JarmonizedStatusBackground(connected: Boolean): Color {
    return if (connected) Color(0xFF0F2B26) else Color(0xFF102238)
}
