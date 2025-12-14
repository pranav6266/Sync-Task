package com.pranav.synctask.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.pranav.synctask.models.Message
import com.pranav.synctask.utils.DateUtils

@Composable
fun ChatBubble(message: Message) {
    val isMe = message.senderId == FirebaseAuth.getInstance().uid
    val isSystem = message.isSystemMessage

    if (isSystem) {
        // SYSTEM MESSAGE (Centered, small gray text)
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(
                text = message.text,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                modifier = Modifier
                    .background(Color.LightGray.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    } else {
        // USER MESSAGE
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = if (isMe) Alignment.CenterEnd else Alignment.CenterStart
        ) {
            Column(
                horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
            ) {
                // Name (only for partner)
                if (!isMe) {
                    Text(
                        text = message.senderName ?: "Partner",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                // Bubble
                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .background(
                            color = if (isMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(
                                topStart = 16.dp, topEnd = 16.dp,
                                bottomStart = if (isMe) 16.dp else 4.dp,
                                bottomEnd = if (isMe) 4.dp else 16.dp
                            )
                        )
                        .padding(12.dp)
                ) {
                    Text(
                        text = message.text,
                        color = if (isMe) Color.White else MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                // Time
                if (message.timestamp != null) {
                    Text(
                        text = DateUtils.formatTime(com.google.firebase.Timestamp(message.timestamp)),
                        fontSize = 10.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }
        }
    }
}