package com.pranav.synctask.services;

import android.util.Log;
import androidx.annotation.NonNull;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.pranav.synctask.data.TaskRepository;
import com.pranav.synctask.data.UserRepository;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "MyFirebaseMsgService";

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "Refreshed token: " + token);

        // Keep this! It ensures your user can receive messages later.
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            UserRepository.getInstance().updateFcmToken(currentUser.getUid(), token);
        }
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        // 1. Refresh Data
        // This is the most important part: tell the app to pull new data
        // even if the user doesn't tap a notification.
        Log.d(TAG, "Message received, refreshing tasks...");
        TaskRepository.getInstance().refreshTasks();

        // 2. Log Data (For debugging only right now)
        if (!remoteMessage.getData().isEmpty()) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());
        }

        // TODO: Later, when you rebuild the UI, we will add the
        // Notification logic back here using your new Compose Activity.
    }
}