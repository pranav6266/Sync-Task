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
import com.pranav.synctask.models.Task;
import com.pranav.synctask.models.User;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirebaseHelper {
    private static final String TAG = "FirebaseHelper";
    private static final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final String USERS_COLLECTION = "users";
    private static final String TASKS_COLLECTION = "tasks";

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

    public static void createOrUpdateUser(FirebaseUser firebaseUser, UserCallback callback) {
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

    public static void updateDisplayName(String uid, String newName, UserCallback callback) {
        db.collection(USERS_COLLECTION).document(uid)
                .update("displayName", newName)
                .addOnSuccessListener(aVoid -> getUser(uid, callback))
                .addOnFailureListener(callback::onError);
    }

    // PHASE 2: Method to update photo URL in Firestore
    public static void updatePhotoUrl(String uid, String newUrl, UserCallback callback) {
        db.collection(USERS_COLLECTION).document(uid)
                .update("photoURL", newUrl)
                .addOnSuccessListener(aVoid -> getUser(uid, callback))
                .addOnFailureListener(callback::onError);
    }

    // PHASE 3: Method to update a user's FCM token in Firestore
    public static void updateFcmToken(String uid, String token) {
        if (uid == null || token == null) return;
        Map<String, Object> updates = new HashMap<>();
        updates.put("fcmToken", token);
        db.collection(USERS_COLLECTION).document(uid)
                .update(updates)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "FCM token updated successfully."))
                .addOnFailureListener(e -> Log.e(TAG, "Error updating FCM token", e));
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

    public static ListenerRegistration addUserListener(String uid, UserCallback callback) {
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

    public static void pairUsers(String currentUserUID, String partnerCode, PairingCallback callback) {
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

    public static void unpairUsers(String currentUserUID, PairingCallback callback) {
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

    public static void createTask(Task task, TasksCallback callback) {
        db.collection(TASKS_COLLECTION)
                .add(task.toMap())
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Task created with ID: " + documentReference.getId());
                    callback.onSuccess(null); // Listener will pick up the change
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