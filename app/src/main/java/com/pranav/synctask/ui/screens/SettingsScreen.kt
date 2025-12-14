package com.pranav.synctask.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pranav.synctask.data.Result
import com.pranav.synctask.models.User
import com.pranav.synctask.ui.viewmodels.SettingsViewModel

@Composable
fun SettingsScreen(
    onLogout: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    // Observe Data
    val userResult by viewModel.getUserLiveData().observeAsState(Result.Loading())
    val generateCodeResult by viewModel.getGenerateCodeResult().observeAsState()
    val linkResult by viewModel.getLinkPartnerResult().observeAsState()

    // Local State
    var partnerCodeInput by remember { mutableStateOf("") }
    var showUnlinkDialog by remember { mutableStateOf(false) }

    // Side Effects for Toasts
    LaunchedEffect(linkResult) {
        when (linkResult) {
            is Result.Success -> {
                Toast.makeText(context, "Partner Linked Successfully!", Toast.LENGTH_SHORT).show()
                partnerCodeInput = "" // Clear input
            }
            is Result.Error -> Toast.makeText(context, "Error: ${(linkResult as Result.Error).exception?.message}", Toast.LENGTH_SHORT).show()
            else -> {}
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        when (val result = userResult) {
            is Result.Loading<*> -> CircularProgressIndicator()
            is Result.Error<*> -> Text("Error loading profile")
            is Result.Success<*> -> {
                val user = result.data as User

                // --- PROFILE SECTION ---
                ProfileCard(user)

                Spacer(modifier = Modifier.height(24.dp))

                // --- PARTNER SECTION ---
                if (user.partnerSpaceId != null) {
                    // LINKED STATE
                    LinkedPartnerCard(
                        onUnlinkClick = { showUnlinkDialog = true }
                    )
                } else {
                    // UNLINKED STATE
                    UnlinkedPartnerCard(
                        generateCodeResult = generateCodeResult,
                        onGenerateClick = { viewModel.generatePartnerCode() },
                        partnerCodeInput = partnerCodeInput,
                        onCodeChange = { partnerCodeInput = it },
                        onLinkClick = { viewModel.linkPartner(partnerCodeInput) },
                        onCopyClick = { code ->
                            clipboardManager.setText(AnnotatedString(code))
                            Toast.makeText(context, "Code copied!", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
            else -> {}
        }

        Spacer(modifier = Modifier.weight(1f))

        // --- LOGOUT ---
        OutlinedButton(
            onClick = {
                viewModel.logout()
                onLogout()
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
        ) {
            Icon(Icons.Default.ExitToApp, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Logout")
        }
    }

    // Unlink Dialog
    if (showUnlinkDialog) {
        AlertDialog(
            onDismissRequest = { showUnlinkDialog = false },
            title = { Text("Unlink Partner?") },
            text = { Text("You will lose access to shared tasks and chats. This cannot be undone easily.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.unlinkPartner()
                        showUnlinkDialog = false
                    }
                ) { Text("Unlink", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showUnlinkDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun ProfileCard(user: User) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = user.displayName?.firstOrNull()?.toString() ?: "U",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = user.displayName ?: "User", style = MaterialTheme.typography.titleMedium)
                Text(text = user.email ?: "", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun LinkedPartnerCard(onUnlinkClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Favorite, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "You are connected!",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Head over to the 'Us' tab to share tasks and chat.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onUnlinkClick,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Unlink Partner")
            }
        }
    }
}

@Composable
fun UnlinkedPartnerCard(
    generateCodeResult: Result<String>?,
    onGenerateClick: () -> Unit,
    partnerCodeInput: String,
    onCodeChange: (String) -> Unit,
    onLinkClick: () -> Unit,
    onCopyClick: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Link with Partner", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(16.dp))

            // Option A: Generate Code
            Text("Option 1: Invite Partner", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            if (generateCodeResult is Result.Success<*>) {
                val code = generateCodeResult.data as String
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = code, style = MaterialTheme.typography.headlineSmall, letterSpacing = 2.sp)
                    IconButton(onClick = { onCopyClick(code) }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                    }
                }
                Text("Share this code with your partner", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            } else {
                Button(onClick = onGenerateClick) {
                    Text("Generate Invite Code")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Divider()
            Spacer(modifier = Modifier.height(16.dp))

            // Option B: Enter Code
            Text("Option 2: Join Partner", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = partnerCodeInput,
                onValueChange = onCodeChange,
                label = { Text("Enter Invite Code") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onLinkClick,
                enabled = partnerCodeInput.length >= 6,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Link")
            }
        }
    }
}