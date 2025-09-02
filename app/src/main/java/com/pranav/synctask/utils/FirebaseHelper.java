package com.pranav.synctask.utils;

import android.util.Log;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
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

    /**
     * Creates a new user document in Firestore if one doesn't exist,
     * or updates the profile information if the user already exists.
     * This ensures that the partnerCode and pairedWithUID are not overwritten on subsequent logins.
     */
    public static void createOrUpdateUser(FirebaseUser firebaseUser, UserCallback callback) {
        DocumentReference userDocRef = db.collection(USERS_COLLECTION).document(firebaseUser.getUid());

        userDocRef.get().addOnSuccessListener(document -> {
            if (document.exists()) {
                // User exists, just update their profile info
                userDocRef.update(
                        "displayName", firebaseUser.getDisplayName(),
                        "photoURL", firebaseUser.getPhotoUrl() != null ? firebaseUser.getPhotoUrl().toString() : null
                ).addOnSuccessListener(aVoid -> {
                    // Return the full user object after update
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


    /**
     * Fetches a user's data from Firestore.
     */
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

    /**
     * Pairs the current user with a partner using their code.
     * This operation is performed in a transaction to ensure atomicity.
     * It checks if either user is already paired before proceeding.
     */
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

                                // All checks passed, perform the pairing
                                transaction.update(currentUserRef, "pairedWithUID", partnerUID);
                                transaction.update(partnerUserRef, "pairedWithUID", currentUserUID);

                                return null; // Transaction success
                            }).addOnSuccessListener(aVoid -> callback.onSuccess())
                            .addOnFailureListener(e -> callback.onError(new Exception(e.getMessage())));
                })
                .addOnFailureListener(callback::onError);
    }

    // Task operations
    public static void createTask(Task task, TasksCallback callback) {
        db.collection(TASKS_COLLECTION)
                .add(task.toMap())
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Task created with ID: " + documentReference.getId());
                    // The listener will pick up the new task, so no need to call onSuccess here
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