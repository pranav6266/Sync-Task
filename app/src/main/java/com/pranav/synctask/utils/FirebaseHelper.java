package com.pranav.synctask.utils;

import android.util.Log;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.functions.FirebaseFunctions;
import com.pranav.synctask.models.Task;
import com.pranav.synctask.models.User;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// CHANGED: Class is no longer static. This fixes the memory leak warning.
public class FirebaseHelper {
    private static final String TAG = "FirebaseHelper";

    // CHANGED: Fields are no longer static and are final
    private final FirebaseFirestore db;
    private final FirebaseFunctions functions;

    private static final String USERS_COLLECTION = "users";
    private static final String TASKS_COLLECTION = "tasks";

    // CHANGED: Constructor initializes non-static fields
    public FirebaseHelper() {
        db = FirebaseFirestore.getInstance();
        functions = FirebaseFunctions.getInstance();
    }

    // Callbacks
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

    public interface NotificationCallback {
        void onSuccess();
        void onError(Exception e);
    }

    // CHANGED: Method is no longer static
    public void createOrUpdateUser(FirebaseUser firebaseUser, UserCallback callback) {
        DocumentReference userDocRef = db.collection(USERS_COLLECTION).document(firebaseUser.getUid());
        userDocRef.get().addOnSuccessListener(document -> {
            if (document.exists()) {
                // User exists, just update their profile info
                userDocRef.update(
                        "displayName", firebaseUser.getDisplayName(),
                        "photoURL", firebaseUser.getPhotoUrl() != null ? firebaseUser.getPhotoUrl().toString() : null
                ).addOnSuccessListener(aVoid -> {
                    getUser(firebaseUser.getUid(), callback);
                }).addOnFailureListener(callback::onError);
            } else {
                // This is a new user, create the document
                User newUser = new User(
                        firebaseUser.getUid(),
                        firebaseUser.getEmail(),
                        firebaseUser.getDisplayName(),
                        firebaseUser.getPhotoUrl() != null ? firebaseUser.getPhotoUrl().toString() : null
                );
                userDocRef.set(newUser)
                        .addOnSuccessListener(aVoid -> callback.onSuccess(newUser))
                        .addOnFailureListener(callback::onError);
            }
        }).addOnFailureListener(callback::onError);
    }

    // CHANGED: Method is no longer static
    public void updateDisplayName(String uid, String newName, UserCallback callback) {
        db.collection(USERS_COLLECTION).document(uid)
                .update("displayName", newName)
                .addOnSuccessListener(aVoid -> getUser(uid, callback))
                .addOnFailureListener(callback::onError);
    }

    // CHANGED: Method is no longer static
    public void updatePhotoUrl(String uid, String newUrl, UserCallback callback) {
        db.collection(USERS_COLLECTION).document(uid)
                .update("photoURL", newUrl)
                .addOnSuccessListener(aVoid -> getUser(uid, callback))
                .addOnFailureListener(callback::onError);
    }

    // CHANGED: Method is no longer static
    public void updateFcmToken(String uid, String token) {
        if (uid == null || token == null) return;
        Map<String, Object> updates = new HashMap<>();
        updates.put("fcmToken", token);
        db.collection(USERS_COLLECTION).document(uid)
                .update(updates)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "FCM token updated successfully."))
                .addOnFailureListener(e -> Log.e(TAG, "Error updating FCM token", e));
    }

    // CHANGED: Method is no longer static
    public void updateTask(String taskId, Map<String, Object> taskMap, TasksCallback callback) {
        db.collection(TASKS_COLLECTION)
                .document(taskId)
                .update(taskMap)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Task updated successfully");
                    // Send notification about task update
                    sendTaskNotification(taskId, null, "task_updated", null);
                    callback.onSuccess(null);
                })
                .addOnFailureListener(callback::onError);
    }

    // CHANGED: Method is no longer static
    public void getUser(String uid, UserCallback callback) {
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

    // CHANGED: Method is no longer static
    public ListenerRegistration addUserListener(String uid, UserCallback callback) {
        return db.collection(USERS_COLLECTION)
                .document(uid)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) {
                        Log.w(TAG, "User listener failed.", e);
                        callback.onError(e);
                        return;
                    }
                    if (snapshot != null && snapshot.exists()) {
                        User user = snapshot.toObject(User.class);
                        callback.onSuccess(user);
                    } else {
                        Log.d(TAG, "Current data: null");
                    }
                });
    }

    // CHANGED: Method is no longer static
    public void pairUsers(String currentUserUID, String partnerCode, PairingCallback callback) {
        db.collection(USERS_COLLECTION)
                .whereEqualTo("partnerCode", partnerCode.toUpperCase())
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty() || querySnapshot.getDocuments().get(0).getId().equals(currentUserUID)) {
                        callback.onError(new Exception("Invalid partner code. Please check and try again."));
                        return;
                    }

                    DocumentSnapshot partnerDocSnapshot = querySnapshot.getDocuments().get(0);
                    String partnerUID = partnerDocSnapshot.getId();

                    db.runTransaction(transaction -> {
                                DocumentReference currentUserRef = db.collection(USERS_COLLECTION).document(currentUserUID);
                                DocumentReference partnerUserRef = db.collection(USERS_COLLECTION).document(partnerUID);

                                DocumentSnapshot currentUserDoc = transaction.get(currentUserRef);
                                DocumentSnapshot partnerDoc = transaction.get(partnerUserRef);

                                if (!currentUserDoc.exists() || !partnerDoc.exists()) {
                                    throw new FirebaseFirestoreException("User document not found.", FirebaseFirestoreException.Code.ABORTED);
                                }

                                User currentUser = currentUserDoc.toObject(User.class);
                                User partnerUser = partnerDoc.toObject(User.class);

                                if (currentUser != null && currentUser.getPairedWithUID() != null && !currentUser.getPairedWithUID().isEmpty()) {
                                    throw new FirebaseFirestoreException("You are already paired with someone.", FirebaseFirestoreException.Code.ABORTED);
                                }

                                if (partnerUser != null && partnerUser.getPairedWithUID() != null && !partnerUser.getPairedWithUID().isEmpty()) {
                                    throw new FirebaseFirestoreException("Your partner is already paired with someone else.", FirebaseFirestoreException.Code.ABORTED);
                                }

                                transaction.update(currentUserRef, "pairedWithUID", partnerUID);
                                transaction.update(partnerUserRef, "pairedWithUID", currentUserUID);

                                return null;
                            }).addOnSuccessListener(aVoid -> callback.onSuccess())
                            .addOnFailureListener(e -> callback.onError(new Exception(e.getMessage())));
                })
                .addOnFailureListener(callback::onError);
    }

    // CHANGED: Method is no longer static
    public void unpairUsers(String currentUserUID, PairingCallback callback) {
        getUser(currentUserUID, new UserCallback() {
            @Override
            public void onSuccess(User user) {
                String partnerUID = user.getPairedWithUID();
                if (partnerUID == null || partnerUID.isEmpty()) {
                    callback.onError(new Exception("You are not paired with anyone."));
                    return;
                }

                db.collection(TASKS_COLLECTION)
                        .whereArrayContains("sharedWith", currentUserUID)
                        .get()
                        .addOnSuccessListener(queryDocumentSnapshots -> {
                            WriteBatch batch = db.batch();

                            queryDocumentSnapshots.getDocuments().forEach(doc -> {
                                Task task = doc.toObject(Task.class);
                                if (task != null && task.getSharedWith().contains(partnerUID)) {
                                    batch.delete(doc.getReference());
                                }
                            });

                            DocumentReference currentUserRef = db.collection(USERS_COLLECTION).document(currentUserUID);
                            DocumentReference partnerUserRef = db.collection(USERS_COLLECTION).document(partnerUID);
                            batch.update(currentUserRef, "pairedWithUID", null);
                            batch.update(partnerUserRef, "pairedWithUID", null);

                            batch.commit()
                                    .addOnSuccessListener(aVoid -> callback.onSuccess())
                                    .addOnFailureListener(callback::onError);
                        })
                        .addOnFailureListener(callback::onError);
            }

            @Override
            public void onError(Exception e) {
                callback.onError(e);
            }
        });
    }

    // CHANGED: Method is no longer static
    public void createTask(Task task, TasksCallback callback) {
        db.collection(TASKS_COLLECTION)
                .add(task.toMap())
                .addOnSuccessListener(documentReference -> {
                    String taskId = documentReference.getId();
                    Log.d(TAG, "Task created with ID: " + taskId);

                    // Send notification to partner
                    sendTaskNotification(taskId, task, "new_task", null);
                    callback.onSuccess(null);
                })
                .addOnFailureListener(callback::onError);
    }

    // CHANGED: Method is no longer static
    public ListenerRegistration getTasks(String userUID, TasksCallback callback) {
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

    // CHANGED: Method is no longer static
    public void updateTaskStatus(String taskId, String status) {
        db.collection(TASKS_COLLECTION)
                .document(taskId)
                .update("status", status)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Task status updated");
                    // Send notification about status change
                    sendTaskStatusNotification(taskId, status);
                })
                .addOnFailureListener(e -> Log.w(TAG, "Error updating task status", e));
    }

    // CHANGED: Method is no longer static
    public void deleteTask(String taskId) {
        // First get the task to send notification
        db.collection(TASKS_COLLECTION)
                .document(taskId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        Task task = document.toObject(Task.class);
                        // Delete the task
                        db.collection(TASKS_COLLECTION)
                                .document(taskId)
                                .delete()
                                .addOnSuccessListener(aVoid -> {
                                    Log.d(TAG, "Task deleted");
                                    // Send notification about task deletion
                                    sendTaskNotification(taskId, task, "task_deleted", null);
                                })
                                .addOnFailureListener(e -> Log.w(TAG, "Error deleting task", e));
                    }
                })
                .addOnFailureListener(e -> Log.w(TAG, "Error getting task for deletion", e));
    }

    // CHANGED: Method is no longer static
    private void sendTaskNotification(String taskId, Task task, String action, NotificationCallback callback) {
        if (task == null) {
            // If task is null, fetch it first
            db.collection(TASKS_COLLECTION)
                    .document(taskId)
                    .get()
                    .addOnSuccessListener(document -> {
                        if (document.exists()) {
                            Task fetchedTask = document.toObject(Task.class);
                            sendTaskNotificationInternal(taskId, fetchedTask, action, callback);
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (callback != null) callback.onError(e);
                    });
        } else {
            sendTaskNotificationInternal(taskId, task, action, callback);
        }
    }

    // CHANGED: Method is no longer static
    private void sendTaskNotificationInternal(String taskId, Task task, String action, NotificationCallback callback) {
        if (task == null || task.getSharedWith() == null || task.getSharedWith().size() < 2) {
            if (callback != null) callback.onError(new Exception("Task data is incomplete for notification."));
            return;
        }

        // Get the partner's UID (the one who didn't create the task)
        String partnerUID = null;
        for (String uid : task.getSharedWith()) {
            if (!uid.equals(task.getCreatorUID())) {
                partnerUID = uid;
                break;
            }
        }

        if (partnerUID == null) {
            if (callback != null) callback.onError(new Exception("Partner UID not found for notification."));
            return;
        }

        // Get partner's FCM token
        db.collection(USERS_COLLECTION)
                .document(partnerUID)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        User partner = document.toObject(User.class);
                        if (partner != null && partner.getFcmToken() != null) {
                            // Prepare notification data
                            Map<String, Object> data = new HashMap<>();
                            data.put("taskId", taskId);
                            data.put("action", action);
                            data.put("taskTitle", task.getTitle());
                            data.put("creatorName", task.getCreatorDisplayName());
                            data.put("targetToken", partner.getFcmToken());

                            // Call cloud function to send notification
                            // This assumes you have a Cloud Function named "sendTaskNotification"
                            functions.getHttpsCallable("sendTaskNotification")
                                    .call(data)
                                    .addOnSuccessListener(result -> {
                                        Log.d(TAG, "Notification sent successfully");
                                        if (callback != null) callback.onSuccess();
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.w(TAG, "Failed to send notification", e);
                                        if (callback != null) callback.onError(e);
                                    });
                        } else {
                            if (callback != null) callback.onError(new Exception("Partner FCM token is missing."));
                        }
                    } else {
                        if (callback != null) callback.onError(new Exception("Partner document not found."));
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error getting partner info", e);
                    if (callback != null) callback.onError(e);
                });
    }

    // CHANGED: Method is no longer static
    private void sendTaskStatusNotification(String taskId, String newStatus) {
        db.collection(TASKS_COLLECTION)
                .document(taskId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        Task task = document.toObject(Task.class);
                        if (task != null && task.getSharedWith() != null && task.getSharedWith().size() >= 2) {
                            // Find partner UID
                            String partnerUID = null;
                            for (String uid : task.getSharedWith()) {
                                if (!uid.equals(task.getCreatorUID())) {
                                    partnerUID = uid;
                                    break;
                                }
                            }

                            if (partnerUID != null) {
                                // Get partner's FCM token and send notification
                                db.collection(USERS_COLLECTION)
                                        .document(partnerUID)
                                        .get()
                                        .addOnSuccessListener(partnerDoc -> {
                                            if (partnerDoc.exists()) {
                                                User partner = partnerDoc.toObject(User.class);
                                                if (partner != null && partner.getFcmToken() != null) {
                                                    Map<String, Object> data = new HashMap<>();
                                                    data.put("taskId", taskId);
                                                    data.put("action", "status_changed");
                                                    data.put("taskTitle", task.getTitle());
                                                    data.put("newStatus", newStatus);
                                                    data.put("creatorName", task.getCreatorDisplayName());
                                                    data.put("targetToken", partner.getFcmToken());

                                                    functions.getHttpsCallable("sendTaskNotification")
                                                            .call(data)
                                                            .addOnSuccessListener(result -> Log.d(TAG, "Status change notification sent"))
                                                            .addOnFailureListener(e -> Log.w(TAG, "Failed to send status notification", e));
                                                }
                                            }
                                        });
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> Log.w(TAG, "Error getting task for status notification", e));
    }
}
