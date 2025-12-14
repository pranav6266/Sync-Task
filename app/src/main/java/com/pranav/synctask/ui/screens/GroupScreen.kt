package com.pranav.synctask.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pranav.synctask.data.Result
import com.pranav.synctask.models.Space
import com.pranav.synctask.ui.DashboardViewModel
import com.pranav.synctask.ui.components.GroupItem

@Composable
fun GroupsScreen(
    onGroupClick: (String) -> Unit, // Pass spaceId
    viewModel: DashboardViewModel = viewModel()
) {
    val spacesResult by viewModel.sharedSpacesLiveData.observeAsState(Result.Loading())
    var showCreateDialog by remember { mutableStateOf(false) }

    // Refresh data when screen loads
    LaunchedEffect(Unit) {
        viewModel.refreshSpaces()
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Create Group")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Text(
                text = "My Groups",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(16.dp)
            )

            when (val result = spacesResult) {
                is Result.Loading<*> -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is Result.Error<*> -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Error loading groups", color = MaterialTheme.colorScheme.error)
                    }
                }
                is Result.Success<*> -> {
                    @Suppress("UNCHECKED_CAST")
                    val spaces = result.data as? List<Space> ?: emptyList()

                    if (spaces.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No groups found. Create one!", color = Color.Gray)
                        }
                    } else {
                        LazyColumn {
                            items(spaces) { space ->
                                GroupItem(space = space, onClick = { onGroupClick(space.spaceId) })
                            }
                        }
                    }
                }
                else -> {}
            }
        }
    }

    // Simple Create Dialog
    if (showCreateDialog) {
        var groupName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Create New Group") },
            text = {
                OutlinedTextField(
                    value = groupName,
                    onValueChange = { groupName = it },
                    label = { Text("Group Name") }
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (groupName.isNotBlank()) {
                        viewModel.createSpace(groupName)
                        showCreateDialog = false
                    }
                }) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text("Cancel") }
            }
        )
    }
}