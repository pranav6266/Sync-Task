package com.pranav.synctask.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pranav.synctask.data.Result
import com.pranav.synctask.models.Message
import com.pranav.synctask.models.Task
import com.pranav.synctask.ui.components.ChatBubble
import com.pranav.synctask.ui.components.TaskItem
import com.pranav.synctask.ui.viewmodels.PartnerViewModel

@Composable
fun PartnerScreen(
    onTaskClick: (String) -> Unit,
    viewModel: PartnerViewModel = viewModel()
) {
    val partnerSpaceId by viewModel.getPartnerSpaceId().observeAsState()
    val tasksResult by viewModel.getPartnerTasks().observeAsState(Result.Loading())
    val messagesResult by viewModel.getMessages().observeAsState(Result.Loading())

    // UI State
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Chat, 1 = Tasks
    var chatInput by remember { mutableStateOf("") }

    // Scroll state for chat (auto-scroll to bottom)
    val listState = rememberLazyListState()

    // Auto-scroll effect
    LaunchedEffect(messagesResult) {
        if (messagesResult is Result.Success<*>) {
            // FIX: Cast the generic 'data' to a List so Kotlin knows it has a .size property
            val listData = (messagesResult as Result.Success<*>).data as? List<*>
            val count = listData?.size ?: 0

            if (count > 0) {
                listState.scrollToItem(count - 1)
            }
        }
    }

    if (partnerSpaceId == null) {
        // --- EMPTY STATE (Not Linked) ---
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("No Partner Linked", style = MaterialTheme.typography.headlineMedium)
                Text("Go to Settings to connect.", color = Color.Gray)
            }
        }
    } else {
        // --- MAIN CONTENT ---
        Column(modifier = Modifier.fillMaxSize()) {

            // 1. Tab Row (Chat | Tasks)
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Chat") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Shared Tasks") })
            }

            // 2. Content Area
            Box(modifier = Modifier.weight(1f)) {
                if (selectedTab == 0) {
                    // --- CHAT VIEW ---
                    when (val result = messagesResult) {
                        is Result.Success<*> -> {
                            @Suppress("UNCHECKED_CAST")
                            val messages = result.data as? List<Message> ?: emptyList()

                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(messages) { msg -> ChatBubble(msg) }
                            }
                        }
                        else -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                } else {
                    // --- TASK LIST VIEW ---
                    when (val result = tasksResult) {
                        is Result.Success<*> -> {
                            @Suppress("UNCHECKED_CAST")
                            val tasks = result.data as? List<Task> ?: emptyList()

                            if (tasks.isEmpty()) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("No shared tasks yet.", color = Color.Gray)
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(bottom = 80.dp) // Space for FAB
                                ) {
                                    items(tasks) { task ->
                                        TaskItem(
                                            task = task,
                                            onCheckedChange = { isChecked ->
                                                viewModel.updateTaskStatus(task, isChecked)
                                            },
                                            onTaskClick = { onTaskClick(task.id) }
                                        )
                                    }
                                }
                            }
                        }
                        else -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                }
            }

            // 3. Input Bar (Only visible in Chat Mode)
            if (selectedTab == 0) {
                Divider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = chatInput,
                        onValueChange = { chatInput = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Type a message...") },
                        maxLines = 3
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (chatInput.isNotBlank()) {
                                viewModel.sendMessage(chatInput)
                                chatInput = ""
                            }
                        },
                        colors = IconButtonDefaults.filledIconButtonColors()
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Send")
                    }
                }
            }
        }
    }
}