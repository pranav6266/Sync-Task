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
    private static final String SPACES_COLLECTION = "spaces";

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

    public interface TaskCallback {
        void onSuccess(Task task);
        void onError(Exception e);
    }

    public interface SpaceCallback {
        void onSuccess(Space space);
        void onError(Exception e);
    }

    public interface SpacesCallback {
        void onSuccess(List<Space> spaces);
        void onError(Exception e);
    }

    public interface NotificationCallback {
        void onSuccess();
        void onError(Exception e);
    }

    // ... (User and Space methods remain unchanged) ...
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


    // --- TASK METHODS (Updated for Progress) ---

    public void createTask(Task task, TasksCallback callback) {
        db.collection(TASKS_COLLECTION)
                .add(task.toMap()) // task.toMap() now includes spaceId AND progressPercentage
                .addOnSuccessListener(documentReference -> {
                    String taskId = documentReference.getId();
                    Log.d(TAG, "Task created with ID: " + taskId);
                    // TODO: Re-implement notification logic for space
                    callback.onSuccess(null); // Listener handles update
                })
                .addOnFailureListener(callback::onError);
    }

    public void updateTask(String taskId, Map<String, Object> taskMap, TasksCallback callback) {
        // Ensure progressPercentage is included if provided
        db.collection(TASKS_COLLECTION)
                .document(taskId)
                .update(taskMap) // taskMap should come from task.toMap() which includes progress
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Task updated successfully");
                    // TODO: Re-implement notification logic based on spaceId
                    callback.onSuccess(null); // Listener handles update
                })
                .addOnFailureListener(callback::onError);
    }


    public ListenerRegistration getTasks(String spaceId, TasksCallback callback) {
        return db.collection(TASKS_COLLECTION)
                .whereEqualTo("spaceId", spaceId)
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
                            // Assign the document ID to the task object
                            tasks.get(i).setId(value.getDocuments().get(i).getId());
                        }
                        callback.onSuccess(tasks);
                    } else {
                        Log.d(TAG, "Current task list data: null");
                        callback.onSuccess(new ArrayList<>()); // Return empty list if null
                    }
                });
    }

    public ListenerRegistration getTaskById(String taskId, TaskCallback callback) {
        return db.collection(TASKS_COLLECTION).document(taskId)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) {
                        Log.w(TAG, "Task listener failed.", e);
                        callback.onError(e);
                        return;
                    }
                    if (snapshot != null && snapshot.exists()) {
                        Task task = snapshot.toObject(Task.class);
                        if (task != null) {
                            task.setId(snapshot.getId()); // Set the ID from the snapshot
                            callback.onSuccess(task);
                        } else {
                            callback.onError(new Exception("Failed to parse task."));
                        }
                    } else {
                        // Task might be deleted or ID is invalid
                        Log.d(TAG, "Task snapshot null or doesn't exist for ID: " + taskId);
                        callback.onError(new Exception("Task not found. It may have been deleted."));
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

    // NEW METHOD for Phase 3 - Updates both progress and status atomically
    public void updateTaskProgressAndStatus(String taskId, int progress, String status) { //
        Map<String, Object> updates = new HashMap<>(); //
        updates.put("progressPercentage", progress); //
        updates.put("status", status); // Update status based on progress

        db.collection(TASKS_COLLECTION) //
                .document(taskId) //
                .update(updates) //
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Task progress and status updated")) //
                .addOnFailureListener(e -> Log.w(TAG, "Error updating task progress/status", e)); //
    } //

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


    // --- Space Management Methods ---
    public void leaveSpace(String spaceId, String userUID, SpaceCallback callback) {
        DocumentReference spaceDocRef = db.collection(SPACES_COLLECTION).document(spaceId);
        DocumentReference userDocRef = db.collection(USERS_COLLECTION).document(userUID);

        db.runTransaction(transaction -> {
            DocumentSnapshot spaceDoc = transaction.get(spaceDocRef);
            if (!spaceDoc.exists()) {
                throw new FirebaseFirestoreException("Space not found.", FirebaseFirestoreException.Code.NOT_FOUND);
            }

            Space space = spaceDoc.toObject(Space.class);
            if (space == null || space.getMembers() == null) {
                throw new FirebaseFirestoreException("Space data invalid.", FirebaseFirestoreException.Code.DATA_LOSS);
            }
            List<String> members = space.getMembers();

            // Run batch updates
            transaction.update(userDocRef, "spaceIds", FieldValue.arrayRemove(spaceId));
            transaction.update(spaceDocRef, "members", FieldValue.arrayRemove(userUID));

            // If the user was the last member, delete the space and its tasks
            if (members.size() == 1 && members.contains(userUID)) {
                // Return the space object so we know to delete tasks
                return space;
            }

            return null; // User left, but space remains
        }).addOnSuccessListener(result -> {
            if (result != null) {
                // This was the last user. Delete all tasks for this space.
                Space spaceToDelete = (Space) result;
                deleteTasksForSpace(spaceToDelete.getSpaceId(), () -> {
                    // After tasks are deleted, delete the space doc
                    spaceDocRef.delete()
                            .addOnSuccessListener(aVoid -> callback.onSuccess(null)) // Notify success (space deleted)
                            .addOnFailureListener(callback::onError);
                });
            } else {
                // Space was left successfully, not deleted
                callback.onSuccess(null); // Notify success (space left)
            }
        }).addOnFailureListener(callback::onError);
    }

    public void deleteSpace(String spaceId, String userUID, SpaceCallback callback) {
        DocumentReference spaceDocRef = db.collection(SPACES_COLLECTION).document(spaceId);

        db.runTransaction(transaction -> {
            DocumentSnapshot spaceDoc = transaction.get(spaceDocRef);
            if (!spaceDoc.exists()) {
                throw new FirebaseFirestoreException("Space not found.", FirebaseFirestoreException.Code.NOT_FOUND);
            }

            Space space = spaceDoc.toObject(Space.class);
            // Check if user is the creator (assuming creator is the first member)
            if (space == null || space.getMembers() == null || space.getMembers().isEmpty() || !space.getMembers().get(0).equals(userUID)) {
                throw new FirebaseFirestoreException("Permission denied. Only the creator can delete a space.", FirebaseFirestoreException.Code.PERMISSION_DENIED);
            }

            // If checks pass, return the list of members whose user docs need updating
            return space.getMembers();
        }).addOnSuccessListener(membersObject -> {
            if (membersObject == null) {
                // Should not happen if transaction succeeded, but handle defensively
                callback.onError(new Exception("Failed to retrieve members during delete transaction."));
                return;
            }
            @SuppressWarnings("unchecked") // Suppress warning for casting Object to List<String>
            List<String> members = (List<String>) membersObject;


            // 1. Delete all tasks associated with the space
            deleteTasksForSpace(spaceId, () -> {
                // 2. After tasks are deleted, delete the space document and update all members' user documents
                WriteBatch batch = db.batch();

                // 2a. Schedule deletion of the space document
                batch.delete(spaceDocRef);

                // 2b. Schedule removal of the spaceId from each member's user document
                for (String memberId : members) {
                    DocumentReference userDocRef = db.collection(USERS_COLLECTION).document(memberId);
                    batch.update(userDocRef, "spaceIds", FieldValue.arrayRemove(spaceId));
                }

                // 2c. Commit the batch operation
                batch.commit()
                        .addOnSuccessListener(aVoid -> callback.onSuccess(null)) // Notify success (space deleted)
                        .addOnFailureListener(callback::onError); // Notify failure
            });
        }).addOnFailureListener(callback::onError); // Handle transaction failure
    }

    // Helper to delete all tasks within a given space
    private void deleteTasksForSpace(String spaceId, Runnable onComplete) {
        db.collection(TASKS_COLLECTION).whereEqualTo("spaceId", spaceId).get()
                .addOnSuccessListener(querySnapshot -> {
                    WriteBatch batch = db.batch();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        batch.delete(doc.getReference()); // Add each task deletion to the batch
                    }
                    // Commit the batch of task deletions
                    batch.commit().addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Successfully deleted tasks for space: " + spaceId);
                        } else {
                            Log.e(TAG, "Failed to delete tasks for space: " + spaceId, task.getException());
                        }
                        onComplete.run(); // Proceed whether task deletion succeeded or failed
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to query tasks for deletion for space: " + spaceId, e);
                    onComplete.run(); // Proceed even if querying tasks failed
                });
    }
}