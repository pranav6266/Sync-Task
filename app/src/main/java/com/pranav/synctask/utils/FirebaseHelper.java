package com.pranav.synctask.utils;

import android.util.Log;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.pranav.synctask.models.Task;
import com.pranav.synctask.models.User;

import java.util.List;

public class FirebaseHelper {
    private static final String TAG = "FirebaseHelper";
    private static final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final String USERS_COLLECTION = "users";
    private static final String TASKS_COLLECTION = "tasks";

    // User operations
    public interface UserCallback {
        void onSuccess(User user);
        void onError(Exception e);
    }

    public interface TasksCallback {
        void onSuccess(List<Task> tasks);
        void onError(Exception e);
    }

    public interface PairingCallback {
        void onSuccess();
        void onError(Exception e);
    }

    public static void createOrUpdateUser(User user, UserCallback callback) {
        db.collection(USERS_COLLECTION)
                .document(user.getUid())
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        // User exists, update only basic info
                        User existingUser = document.toObject(User.class);
                        existingUser.setEmail(user.getEmail());
                        existingUser.setDisplayName(user.getDisplayName());
                        existingUser.setPhotoURL(user.getPhotoURL());

                        db.collection(USERS_COLLECTION)
                                .document(user.getUid())
                                .set(existingUser.toMap())
                                .addOnSuccessListener(aVoid -> callback.onSuccess(existingUser))
                                .addOnFailureListener(callback::onError);
                    } else {
                        // Create new user
                        db.collection(USERS_COLLECTION)
                                .document(user.getUid())
                                .set(user.toMap())
                                .addOnSuccessListener(aVoid -> callback.onSuccess(user))
                                .addOnFailureListener(callback::onError);
                    }
                })
                .addOnFailureListener(callback::onError);
    }

    public static void getUser(String uid, UserCallback callback) {
        db.collection(USERS_COLLECTION)
                .document(uid)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        User user = document.toObject(User.class);
                        callback.onSuccess(user);
                    } else {
                        callback.onError(new Exception("User not found"));
                    }
                })
                .addOnFailureListener(callback::onError);
    }

    public static void pairUsers(String currentUserUID, String partnerCode, PairingCallback callback) {
        // Find user with the partner code
        db.collection(USERS_COLLECTION)
                .whereEqualTo("partnerCode", partnerCode)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        callback.onError(new Exception("Invalid partner code"));
                        return;
                    }

                    DocumentSnapshot partnerDoc = querySnapshot.getDocuments().get(0);
                    User partner = partnerDoc.toObject(User.class);

                    if (partner.getPairedWithUID() != null && !partner.getPairedWithUID().isEmpty()) {
                        callback.onError(new Exception("Partner is already paired with someone else"));
                        return;
                    }

                    // Update both users
                    db.runTransaction(transaction -> {
                                // Update current user
                                transaction.update(db.collection(USERS_COLLECTION).document(currentUserUID),
                                        "pairedWithUID", partner.getUid());

                                // Update partner
                                transaction.update(db.collection(USERS_COLLECTION).document(partner.getUid()),
                                        "pairedWithUID", currentUserUID);

                                return null;
                            }).addOnSuccessListener(aVoid -> callback.onSuccess())
                            .addOnFailureListener(callback::onError);
                })
                .addOnFailureListener(callback::onError);
    }

    // Task operations
    public static void createTask(Task task, TasksCallback callback) {
        db.collection(TASKS_COLLECTION)
                .add(task.toMap())
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Task created with ID: " + documentReference.getId());
                    // Note: callback could be modified to return the created task with ID
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error creating task", e);
                    callback.onError(e);
                });
    }

    public static ListenerRegistration getTasks(String userUID, TasksCallback callback) {
        return db.collection(TASKS_COLLECTION)
                .whereArrayContains("sharedWith", userUID)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.w(TAG, "Listen failed.", error);
                        callback.onError(error);
                        return;
                    }

                    if (value != null) {
                        List<Task> tasks = value.toObjects(Task.class);
                        for (int i = 0; i < tasks.size(); i++) {
                            tasks.get(i).setId(value.getDocuments().get(i).getId());
                        }
                        callback.onSuccess(tasks);
                    }
                });
    }

    public static void updateTaskStatus(String taskId, String status) {
        db.collection(TASKS_COLLECTION)
                .document(taskId)
                .update("status", status)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Task status updated"))
                .addOnFailureListener(e -> Log.w(TAG, "Error updating task status", e));
    }

    public static void deleteTask(String taskId) {
        db.collection(TASKS_COLLECTION)
                .document(taskId)
                .delete()
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Task deleted"))
                .addOnFailureListener(e -> Log.w(TAG, "Error deleting task", e));
    }
}