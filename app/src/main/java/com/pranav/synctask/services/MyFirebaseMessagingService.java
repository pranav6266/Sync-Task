package com.pranav.synctask.services;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.pranav.synctask.R;
import com.pranav.synctask.activities.TaskDetailActivity;
import com.pranav.synctask.data.TaskRepository;
import com.pranav.synctask.data.UserRepository;
import java.util.Map;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "MyFirebaseMsgService";
    private static final String CHANNEL_ID = "SYNC_TASK_CHANNEL";
    private static final String TASK_ACTIONS_CHANNEL = "TASK_ACTIONS_CHANNEL";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannels();
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        // Refresh data immediately so app is ready when opened
        TaskRepository.getInstance().refreshTasks();

        if (!remoteMessage.getData().isEmpty()) {
            handleDataMessage(remoteMessage.getData());
        }
    }

    private void handleDataMessage(Map<String, String> data) {
        String action = data.get("action");
        String taskId = data.get("taskId");
        String taskTitle = data.get("taskTitle");
        String creatorName = data.get("creatorName");

        // Basic validation
        if (action == null || taskTitle == null) return;

        // Don't notify if I triggered the action myself (optional check)
        // FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        // if (currentUser != null && creatorName != null && creatorName.equals(currentUser.getDisplayName())) return;

        String notificationTitle;
        String notificationBody;
        int iconRes = R.drawable.ic_sync_task_logo; // Default fallback

        switch (action) {
            case "new_task":
                notificationTitle = "New Task: " + taskTitle;
                notificationBody = creatorName + " added a new task.";
                iconRes = R.drawable.ic_add;
                break;
            case "task_updated":
                notificationTitle = "Task Updated: " + taskTitle;
                notificationBody = creatorName + " made changes.";
                iconRes = R.drawable.ic_edit;
                break;
            case "task_deleted":
                notificationTitle = "Task Deleted";
                notificationBody = creatorName + " deleted '" + taskTitle + "'";
                iconRes = R.drawable.ic_delete;
                break;
            case "status_changed":
                String newStatus = data.get("newStatus");
                boolean completed = "completed".equals(newStatus);
                notificationTitle = completed ? "Task Completed! 🎉" : "Task Reopened";
                notificationBody = creatorName + (completed ? " completed " : " reopened ") + taskTitle;
                iconRes = completed ? R.drawable.ic_check_white : R.drawable.ic_task_type_update;
                break;
            default:
                return;
        }

        // If deleted, we can't open the task, so just show the notification
        if ("task_deleted".equals(action)) {
            sendGenericNotification(notificationTitle, notificationBody, iconRes);
        } else {
            sendDeepLinkNotification(notificationTitle, notificationBody, taskId, iconRes);
        }
    }

    private void sendDeepLinkNotification(String title, String body, String taskId, int iconRes) {
        // Intent to open the specific Task Detail
        Intent intent = new Intent(this, TaskDetailActivity.class);
        intent.putExtra(TaskDetailActivity.EXTRA_TASK_ID, taskId);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                taskId != null ? taskId.hashCode() : 0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        int color = ContextCompat.getColor(this, R.color.md_theme_light_primary);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, TASK_ACTIONS_CHANNEL)
                .setSmallIcon(R.drawable.ic_notification) // Status bar icon (must be white/transparent)
                .setContentTitle(title)
                .setContentText(body)
                .setColor(color)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body));

        // Optional: Add a "Mark Complete" action button here in the future

        showNotification(builder, taskId);
    }

    private void sendGenericNotification(String title, String body, int iconRes) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        showNotification(builder, null);
    }

    private void showNotification(NotificationCompat.Builder builder, String taskId) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        int id = taskId != null ? taskId.hashCode() : (int) System.currentTimeMillis();
        NotificationManagerCompat.from(this).notify(id, builder.build());
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            UserRepository.getInstance().updateFcmToken(currentUser.getUid(), token);
        }
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = getSystemService(NotificationManager.class);

            // High Priority Channel for Actions (New Tasks, Updates)
            NotificationChannel actionChannel = new NotificationChannel(
                    TASK_ACTIONS_CHANNEL,
                    "Task Updates",
                    NotificationManager.IMPORTANCE_HIGH
            );
            actionChannel.setDescription("Notifications for new tasks and status changes");
            actionChannel.enableVibration(true);

            // Default Channel
            NotificationChannel defaultChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "General",
                    NotificationManager.IMPORTANCE_DEFAULT
            );

            manager.createNotificationChannel(actionChannel);
            manager.createNotificationChannel(defaultChannel);
        }
    }
}