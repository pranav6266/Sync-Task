package com.pranav.synctask.services;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.pranav.synctask.R;
import com.pranav.synctask.activities.CompletedTasksActivity;
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

        Log.d(TAG, "From: " + remoteMessage.getFrom());

        // Handle data payload
        if (!remoteMessage.getData().isEmpty()) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());
            handleDataMessage(remoteMessage.getData());
        }

        // Handle notification payload
        if (remoteMessage.getNotification() != null) {
            String title = remoteMessage.getNotification().getTitle();
            String body = remoteMessage.getNotification().getBody();
            Log.d(TAG, "Message Notification Body: " + body);
            sendNotification(title, body, remoteMessage.getData());
        }
    }

    private void handleDataMessage(Map<String, String> data) {
        String action = data.get("action");
        String taskId = data.get("taskId");
        String taskTitle = data.get("taskTitle");
        String creatorName = data.get("creatorName");

        if (action == null || taskTitle == null) return;

        String notificationTitle;
        String notificationBody;

        switch (action) {
            case "new_task":
                notificationTitle = "New Task Added";
                notificationBody = creatorName + " added: " + taskTitle;
                break;
            case "task_updated":
                notificationTitle = "Task Updated";
                notificationBody = creatorName + " updated: " + taskTitle;
                break;
            case "task_deleted":
                notificationTitle = "Task Deleted";
                notificationBody = creatorName + " deleted: " + taskTitle;
                break;
            case "status_changed":
                String newStatus = data.get("newStatus");
                notificationTitle = "Task Status Changed";
                String statusText = "completed".equals(newStatus) ? "completed" : "reopened";
                notificationBody = creatorName + " " + statusText + ": " + taskTitle;
                break;
            default:
                return;
        }

        sendTaskActionNotification(notificationTitle, notificationBody, taskId, action);

        // Trigger data refresh in the app
        refreshTaskData();
    }

    private void refreshTaskData() {
        // Force refresh the task repository
        TaskRepository.getInstance().refreshTasks();
    }

    private void sendTaskActionNotification(String title, String body, String taskId, String action) {
        Intent intent = new Intent(this, CompletedTasksActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        // Add task-specific data to intent
        if (taskId != null) {
            intent.putExtra("taskId", taskId);
            intent.putExtra("action", action);
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                taskId != null ? taskId.hashCode() : 0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, TASK_ACTIONS_CHANNEL)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setContentIntent(pendingIntent)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body));

        // Add action buttons for certain notification types
        if ("new_task".equals(action) && taskId != null) {
            // Add "View Task" action
            Intent viewIntent = new Intent(this, CompletedTasksActivity.class);
            viewIntent.putExtra("taskId", taskId);
            viewIntent.putExtra("action", "view");
            PendingIntent viewPendingIntent = PendingIntent.getActivity(this,
                    (taskId + "_view").hashCode(),
                    viewIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            notificationBuilder.addAction(R.drawable.ic_task_type_task, "View Task", viewPendingIntent);
        }

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Notification permission not granted.");
            return;
        }

        int notificationId = taskId != null ? taskId.hashCode() : (int) System.currentTimeMillis();
        notificationManager.notify(notificationId, notificationBuilder.build());
    }

    private void sendNotification(String title, String body, Map<String, String> data) {
        Intent intent = new Intent(this, CompletedTasksActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        // Add any extra data from the notification
        for (Map.Entry<String, String> entry : data.entrySet()) {
            intent.putExtra(entry.getKey(), entry.getValue());
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Notification permission not granted.");
            return;
        }
        notificationManager.notify(0, notificationBuilder.build());
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "Refreshed token: " + token);
        sendRegistrationToServer(token);
    }

    private void sendRegistrationToServer(String token) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            UserRepository.getInstance().updateFcmToken(currentUser.getUid(), token);
        }
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // General notifications channel
            NotificationChannel generalChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Sync Task Notifications",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            generalChannel.setDescription("General app notifications");

            // Task action notifications channel (high priority)
            NotificationChannel taskActionsChannel = new NotificationChannel(
                    TASK_ACTIONS_CHANNEL,
                    "Task Updates",
                    NotificationManager.IMPORTANCE_HIGH
            );
            taskActionsChannel.setDescription("Notifications for task creation, updates, and status changes");
            taskActionsChannel.enableVibration(true);
            taskActionsChannel.setVibrationPattern(new long[]{100, 200, 100, 200});

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(generalChannel);
            notificationManager.createNotificationChannel(taskActionsChannel);
        }
    }
}
