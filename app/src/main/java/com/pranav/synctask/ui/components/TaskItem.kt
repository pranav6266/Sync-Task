package com.pranav.synctask.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.pranav.synctask.models.Task
import com.pranav.synctask.utils.DateUtils
import com.google.firebase.Timestamp

@Composable
fun TaskItem(
    task: Task,
    onCheckedChange: (Boolean) -> Unit,
    onTaskClick: () -> Unit
) {
    val isCompleted = task.status == Task.STATUS_COMPLETED

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { onTaskClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Priority Indicator
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(40.dp)
                    .background(
                        color = when (task.priority) {
                            "High" -> Color.Red
                            "Low" -> Color.Green
                            else -> Color.Blue // Normal
                        },
                        shape = RoundedCornerShape(2.dp)
                    )
            )

            Spacer(modifier = Modifier.width(12.dp))

            Checkbox(
                checked = isCompleted,
                onCheckedChange = onCheckedChange
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = task.title ?: "Untitled",
                    style = MaterialTheme.typography.bodyLarge,
                    textDecoration = if (isCompleted) TextDecoration.LineThrough else null,
                    color = if (isCompleted) Color.Gray else MaterialTheme.colorScheme.onSurface
                )

                if (task.dueDate != null) {
                    Text(
                        text = DateUtils.formatDate(task.dueDate),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (DateUtils.isOverdue(task.dueDate) && !isCompleted) Color.Red else Color.Gray
                    )
                }
            }
        }
    }
}