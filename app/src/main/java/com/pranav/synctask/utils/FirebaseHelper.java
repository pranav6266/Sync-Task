package com.pranav.synctask.utils;

import android.util.Log;

import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.functions.FirebaseFunctions;
import com.pranav.synctask.models.Space;
import com.pranav.synctask.models.Task;
import com.pranav.synctask.models.User;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class FirebaseHelper {
    private static final String TAG = "FirebaseHelper";
    private final FirebaseFirestore db;
    private final FirebaseFunctions functions;

    private static final String USERS_COLLECTION = "users";
    private static final String TASKS_COLLECTION = "tasks";
    private static final String SPACES_COLLECTION = "spaces"; // ADDED

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

    // ADDED
    public interface SpaceCallback {
        void onSuccess(Space space);
        void onError(Exception e);
    }

    // ADDED
    public interface SpacesCallback {
        void onSuccess(List<Space> spaces);
        void onError(Exception e);
    }

    public interface NotificationCallback {
        void onSuccess();
        void onError(Exception e);
    }

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
                // This is a
                // new user, create the document
                User newUser = new User(
                        firebaseUser.getUid(),
                        firebaseUser.getEmail(),
                        firebaseUser.getDisplayName(),
                        firebaseUser.getPhotoUrl() != null ? firebaseUser.getPhotoUrl().toString() : null
                );
                // newUser.spaceIds is already initialized as new ArrayList<>()
                userDocRef.set(newUser)
                        .addOnSuccessListener(aVoid -> callback.onSuccess(newUser))
                        .addOnFailureListener(callback::onError);
            }
        }).addOnFailureListener(callback::onError);
    }

    public void updateDisplayName(String uid, String newName, UserCallback callback) {
        db.collection(USERS_COLLECTION).document(uid)
                .update("displayName", newName)
                .addOnSuccessListener(aVoid -> getUser(uid, callback))
                .addOnFailureListener(callback::onError);
    }

    public void updatePhotoUrl(String uid, String newUrl, UserCallback callback) {
        db.collection(USERS_COLLECTION).document(uid)
                .update("photoURL", newUrl)
                .addOnSuccessListener(aVoid -> getUser(uid, callback))
                .addOnFailureListener(callback::onError);
    }

    public void updateFcmToken(String uid, String token) {
        if (uid == null || token == null) return;
        Map<String, Object> updates = new HashMap<>();
        updates.put("fcmToken", token);
        db.collection(USERS_COLLECTION).document(uid)
                .update(updates)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "FCM token updated successfully."))
                .addOnFailureListener(e -> Log.e(TAG, "Error updating FCM token", e));
    }

    public void updateTask(String taskId, Map<String, Object> taskMap, TasksCallback callback) {
        db.collection(TASKS_COLLECTION)
                .document(taskId)
                .update(taskMap)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Task updated successfully");
                    // TODO: Re-implement notification logic based on spaceId
                    callback.onSuccess(null);
                })
                .addOnFailureListener(callback::onError);
    }

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

    public ListenerRegistration addUserListener(String uid, UserCallback callback) {
        return db.collection(USERS_COLLECTION)
                .document(uid)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) {
                        Log.w(TAG, "User listener failed.", e);
                        callback.onError(e);
                        return;
                    }
                    if (snapshot != null && snapshot.exists())
                    {
                        User user = snapshot.toObject(User.class);
                        callback.onSuccess(user);
                    } else {
                        Log.d(TAG, "Current data: null");
                    }
                });
    }

    // --- NEW METHODS for SPACES ---

    public void createSpace(String spaceName, String creatorUID, SpaceCallback callback) {
        // Generate a new space document
        DocumentReference spaceDocRef = db.collection(SPACES_COLLECTION).document();
        String spaceId = spaceDocRef.getId();
        String inviteCode = UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        Space newSpace = new Space(
                spaceId,
                spaceName,
                Arrays.asList(creatorUID),
                inviteCode
        );
        // Get the user document
        DocumentReference userDocRef = db.collection(USERS_COLLECTION).document(creatorUID);
        // Run a batch write to create the space AND update the user
        WriteBatch batch = db.batch();
        batch.set(spaceDocRef, newSpace);
        batch.update(userDocRef, "spaceIds", FieldValue.arrayUnion(spaceId));

        batch.commit()
                .addOnSuccessListener(aVoid -> callback.onSuccess(newSpace))
                .addOnFailureListener(callback::onError);
    }

    public void joinSpace(String inviteCode, String userUID, SpaceCallback callback) {
        // Find the space with the invite code
        db.collection(SPACES_COLLECTION)
                .whereEqualTo("inviteCode", inviteCode.toUpperCase())
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        callback.onError(new Exception("Invalid invite code."));
                        return;
                    }

                    DocumentSnapshot
                            spaceDoc = querySnapshot.getDocuments().get(0);
                    String spaceId = spaceDoc.getId();
                    Space space = spaceDoc.toObject(Space.class);

                    if (space.getMembers().contains(userUID)) {
                        callback.onError(new Exception("You are already in this space."));
                        return;
                    }

                    // Add user to the space and add space to the user's list
                    DocumentReference spaceDocRef = spaceDoc.getReference();
                    DocumentReference userDocRef = db.collection(USERS_COLLECTION).document(userUID);

                    WriteBatch batch = db.batch();
                    batch.update(spaceDocRef, "members", FieldValue.arrayUnion(userUID));
                    batch.update(userDocRef, "spaceIds", FieldValue.arrayUnion(spaceId));
                    batch.commit()
                            .addOnSuccessListener(aVoid -> callback.onSuccess(space))
                            .addOnFailureListener(callback::onError);
                })
                .addOnFailureListener(callback::onError);
    }

    public void getSpaces(List<String> spaceIds, SpacesCallback callback) {
        if (spaceIds == null || spaceIds.isEmpty()) {
            callback.onSuccess(new ArrayList<>());
            return;
        }

        List<com.google.android.gms.tasks.Task<DocumentSnapshot>> tasks = new ArrayList<>();
        for (String id : spaceIds) {
            tasks.add(db.collection(SPACES_COLLECTION).document(id).get());
        }

        Tasks.whenAllSuccess(tasks).addOnSuccessListener(list -> {
            List<Space> spaces = new ArrayList<>();
            for (Object doc : list) {
                DocumentSnapshot snapshot = (DocumentSnapshot) doc;
                if (snapshot.exists()) {
                    spaces.add(snapshot.toObject(Space.class));
                }
            }
            callback.onSuccess(spaces);
        }).addOnFailureListener(callback::onError);
    }

    // --- MODIFIED TASK METHODS ---

    public void createTask(Task task, TasksCallback callback) {
        db.collection(TASKS_COLLECTION)
                .add(task.toMap()) // task.toMap() now includes spaceId
                .addOnSuccessListener(documentReference -> {
                    String taskId = documentReference.getId();
                    Log.d(TAG, "Task created with ID: " + taskId);
                    // TODO: Re-implement notification logic for space
                    callback.onSuccess(null);
                })
                .addOnFailureListener(callback::onError);
    }

    public ListenerRegistration getTasks(String spaceId, TasksCallback callback) {
        return db.collection(TASKS_COLLECTION)
                .whereEqualTo("spaceId", spaceId) // CHANGED QUERY
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

    public void updateTaskStatus(String taskId, String status) {
        db.collection(TASKS_COLLECTION)
                .document(taskId)
                .update("status", status)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Task status updated");
                    // TODO: Re-implement notification logic
                })
                .addOnFailureListener(e -> Log.w(TAG, "Error updating task status", e));
    }

    public void deleteTask(String taskId) {
        db.collection(TASKS_COLLECTION)
                .document(taskId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Task deleted");
                    // TODO: Re-implement notification logic
                })
                .addOnFailureListener(e -> Log.w(TAG, "Error deleting task", e));
    }

    public void leaveSpace(String spaceId, String userUID, SpaceCallback callback) {
        DocumentReference spaceDocRef = db.collection(SPACES_COLLECTION).document(spaceId);
        DocumentReference userDocRef = db.collection(USERS_COLLECTION).document(userUID);

        db.runTransaction(transaction -> {
            DocumentSnapshot spaceDoc = transaction.get(spaceDocRef);
            if (!spaceDoc.exists()) {
                throw new FirebaseFirestoreException("Space not found.", FirebaseFirestoreException.Code.NOT_FOUND);
            }

            Space space = spaceDoc.toObject(Space.class);
            List<String> members = space.getMembers();

            // Run batch updates
            transaction.update(userDocRef, "spaceIds", FieldValue.arrayRemove(spaceId));
            transaction.update(spaceDocRef, "members", FieldValue.arrayRemove(userUID));

            // If the user was the last member, delete the space and its tasks
            if (members.size() == 1 && members.contains(userUID)) {
                // Return the space object so we can delete tasks
                return space;
            }

            return null; // User left, but space remains
        }).addOnSuccessListener(result -> {
            if (result != null) {
                //
                // This was the last user. Delete all tasks for this space.
                Space spaceToDelete = (Space) result;
                deleteTasksForSpace(spaceToDelete.getSpaceId(), () -> {
                    // After tasks are deleted, delete the space doc
                    spaceDocRef.delete()
                            .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                            .addOnFailureListener(callback::onError);
                });
            } else {
                // Space was left successfully, not deleted
                callback.onSuccess(null);
            }
        }).addOnFailureListener(callback::onError);
    }

    // --- NEW ---
    public void deleteSpace(String spaceId, String userUID, SpaceCallback callback) {
        DocumentReference spaceDocRef = db.collection(SPACES_COLLECTION).document(spaceId);

        db.runTransaction(transaction -> {
            DocumentSnapshot spaceDoc = transaction.get(spaceDocRef);
            if (!spaceDoc.exists()) {
                throw new FirebaseFirestoreException("Space not found.", FirebaseFirestoreException.Code.NOT_FOUND);
            }

            Space space = spaceDoc.toObject(Space.class);
            if (space == null || space.getMembers().isEmpty() || !space.getMembers().get(0).equals(userUID)) {
                throw new FirebaseFirestoreException("Permission denied. Only the creator can delete a space.", FirebaseFirestoreException.Code.PERMISSION_DENIED);
            }

            // If checks pass, return the list of members to update
            return space.getMembers();
        }).addOnSuccessListener(members -> {
            if (members == null) {
                callback.onError(new Exception("An unknown error occurred."));
                return;
            }

            // 1. Delete all tasks for the space
            deleteTasksForSpace(spaceId, () -> {
                // 2. After tasks are deleted, delete the space and update all users
                WriteBatch batch = db.batch();

                // 2a. Delete the space doc
                batch.delete(spaceDocRef);

                // 2b. Remove the spaceId from all members
                List<String> memberList = (List<String>) members;
                for (String memberId : memberList) {
                    DocumentReference userDocRef = db.collection(USERS_COLLECTION).document(memberId);
                    batch.update(userDocRef, "spaceIds", FieldValue.arrayRemove(spaceId));
                }

                // 2c. Commit the final batch
                batch.commit()
                        .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                        .addOnFailureListener(callback::onError);
            });
        }).addOnFailureListener(callback::onError);
    }
    // --- END NEW ---

    private void deleteTasksForSpace(String spaceId, Runnable onComplete) {
        db.collection(TASKS_COLLECTION).whereEqualTo("spaceId", spaceId).get()
                .addOnSuccessListener(querySnapshot -> {
                    WriteBatch batch = db.batch();
                    for
                    (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        batch.delete(doc.getReference());
                    }
                    batch.commit().addOnCompleteListener(task -> onComplete.run());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to query tasks for deletion", e);
                    onComplete.run(); // Still run onComplete, even if task deletion fails
                });
    }
}