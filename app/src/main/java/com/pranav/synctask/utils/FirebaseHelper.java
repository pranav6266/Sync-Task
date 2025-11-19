package com.pranav.synctask.utils;

import android.util.Log;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
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
    private static final String USERS_COLLECTION = "users";
    private static final String TASKS_COLLECTION = "tasks";
    private static final String SPACES_COLLECTION = "spaces";

    public FirebaseHelper() {
        db = FirebaseFirestore.getInstance();
    }

    // Callbacks
    public interface UserCallback { void onSuccess(User user); void onError(Exception e); }
    public interface TasksCallback { void onSuccess(List<Task> tasks); void onError(Exception e); }
    public interface TaskCallback { void onSuccess(Task task); void onError(Exception e); }
    public interface SpaceCallback { void onSuccess(Space space); void onError(Exception e); }
    public interface SpacesCallback { void onSuccess(List<Space> spaces); void onError(Exception e); }

    // --- USER METHODS ---
    public void createOrUpdateUser(FirebaseUser firebaseUser, UserCallback callback) {
        DocumentReference userDocRef = db.collection(USERS_COLLECTION).document(firebaseUser.getUid());
        userDocRef.get().addOnSuccessListener(document -> {
            if (document.exists()) {
                userDocRef.update("displayName", firebaseUser.getDisplayName(),
                                "photoURL", firebaseUser.getPhotoUrl() != null ? firebaseUser.getPhotoUrl().toString() : null)
                        .addOnSuccessListener(aVoid -> getUser(firebaseUser.getUid(), callback))
                        .addOnFailureListener(callback::onError);
            } else {
                User newUser = new User(firebaseUser.getUid(), firebaseUser.getEmail(), firebaseUser.getDisplayName(),
                        firebaseUser.getPhotoUrl() != null ? firebaseUser.getPhotoUrl().toString() : null);
                userDocRef.set(newUser)
                        .addOnSuccessListener(aVoid -> callback.onSuccess(newUser))
                        .addOnFailureListener(callback::onError);
            }
        }).addOnFailureListener(callback::onError);
    }

    public void getUser(String uid, UserCallback callback) {
        db.collection(USERS_COLLECTION).document(uid).get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) callback.onSuccess(document.toObject(User.class));
                    else callback.onError(new Exception("User not found"));
                }).addOnFailureListener(callback::onError);
    }

    public ListenerRegistration addUserListener(String uid, UserCallback callback) {
        return db.collection(USERS_COLLECTION).document(uid)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) { callback.onError(e); return; }
                    if (snapshot != null && snapshot.exists()) callback.onSuccess(snapshot.toObject(User.class));
                });
    }

    public void updateDisplayName(String uid, String newName, UserCallback callback) {
        db.collection(USERS_COLLECTION).document(uid).update("displayName", newName)
                .addOnSuccessListener(aVoid -> getUser(uid, callback)).addOnFailureListener(callback::onError);
    }

    public void updatePhotoUrl(String uid, String newUrl, UserCallback callback) {
        db.collection(USERS_COLLECTION).document(uid).update("photoURL", newUrl)
                .addOnSuccessListener(aVoid -> getUser(uid, callback)).addOnFailureListener(callback::onError);
    }

    public void updateFcmToken(String uid, String token) {
        if (uid != null) db.collection(USERS_COLLECTION).document(uid).update("fcmToken", token);
    }

    // --- SPACE METHODS (FIXED) ---

    // 1. New Query Method: Finds spaces where 'members' array contains the UID
    public ListenerRegistration getSpacesForUser(String uid, SpacesCallback callback) {
        return db.collection(SPACES_COLLECTION)
                .whereArrayContains("members", uid)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        callback.onError(error);
                        return;
                    }
                    if (value != null) {
                        List<Space> spaces = value.toObjects(Space.class);
                        callback.onSuccess(spaces);
                    } else {
                        callback.onSuccess(new ArrayList<>());
                    }
                });
    }

    // 2. Create Space: No longer writes to User document
    public void createSpace(String spaceName, String creatorUID, SpaceCallback callback) {
        DocumentReference spaceDocRef = db.collection(SPACES_COLLECTION).document();
        String spaceId = spaceDocRef.getId();
        String inviteCode = UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        Space newSpace = new Space(spaceId, spaceName, Arrays.asList(creatorUID), inviteCode);
        newSpace.setSpaceType(Space.TYPE_SHARED);

        spaceDocRef.set(newSpace)
                .addOnSuccessListener(aVoid -> callback.onSuccess(newSpace))
                .addOnFailureListener(callback::onError);
    }

    // 3. Create Personal Link: No longer writes to User documents
    public void createPersonalLink(String creatorUID, String partnerUID, String spaceName, SpaceCallback callback) {
        DocumentReference spaceDocRef = db.collection(SPACES_COLLECTION).document();
        String spaceId = spaceDocRef.getId();

        Space newSpace = new Space(spaceId, spaceName, Arrays.asList(creatorUID, partnerUID), null);
        newSpace.setSpaceType(Space.TYPE_PERSONAL);

        spaceDocRef.set(newSpace)
                .addOnSuccessListener(aVoid -> callback.onSuccess(newSpace))
                .addOnFailureListener(callback::onError);
    }

    // 4. Join Space: No longer writes to User document
    public void joinSpace(String inviteCode, String userUID, SpaceCallback callback) {
        db.collection(SPACES_COLLECTION)
                .whereEqualTo("inviteCode", inviteCode.toUpperCase())
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        callback.onError(new Exception("Invalid invite code."));
                        return;
                    }
                    DocumentSnapshot spaceDoc = querySnapshot.getDocuments().get(0);
                    Space space = spaceDoc.toObject(Space.class);

                    if (space.getMembers().contains(userUID)) {
                        callback.onError(new Exception("You are already in this space."));
                        return;
                    }

                    // Only update the Space document
                    spaceDoc.getReference().update("members", FieldValue.arrayUnion(userUID))
                            .addOnSuccessListener(aVoid -> callback.onSuccess(space))
                            .addOnFailureListener(callback::onError);
                })
                .addOnFailureListener(callback::onError);
    }

    public void leaveSpace(String spaceId, String userUID, SpaceCallback callback) {
        // 1. Get the space first to check its type
        db.collection(SPACES_COLLECTION).document(spaceId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        callback.onError(new Exception("Space not found"));
                        return;
                    }

                    Space space = documentSnapshot.toObject(Space.class);
                    if (space == null) return;

                    // 2. LOGIC CHECK
                    if (Space.TYPE_PERSONAL.equals(space.getSpaceType())) {
                        // Case A: Personal Link -> DELETE IT FOR EVERYONE
                        // This ensures it disappears from the partner's phone too.
                        deleteSpace(spaceId, userUID, callback);
                    } else {
                        // Case B: Shared Space -> JUST REMOVE ME
                        db.collection(SPACES_COLLECTION).document(spaceId)
                                .update("members", FieldValue.arrayRemove(userUID))
                                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                                .addOnFailureListener(callback::onError);
                    }
                })
                .addOnFailureListener(callback::onError);
    }
    public void deleteSpace(String spaceId, String userUID, SpaceCallback callback) {
        // 1. First, find all tasks in this space
        db.collection(TASKS_COLLECTION)
                .whereEqualTo("spaceId", spaceId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    // 2. Create a batch to delete them all
                    WriteBatch batch = db.batch();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        batch.delete(doc.getReference());
                    }
                    // 3. Delete the Space document itself in the same batch
                    DocumentReference spaceRef = db.collection(SPACES_COLLECTION).document(spaceId);
                    batch.delete(spaceRef);

                    // 4. Commit the batch
                    batch.commit()
                            .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                            .addOnFailureListener(callback::onError);
                })
                .addOnFailureListener(callback::onError);
    }

    // --- TASK METHODS ---
    public void createTask(Task task, TasksCallback callback) {
        db.collection(TASKS_COLLECTION).add(task.toMap())
                .addOnSuccessListener(ref -> callback.onSuccess(null))
                .addOnFailureListener(callback::onError);
    }

    public void updateTask(String taskId, Map<String, Object> taskMap, TasksCallback callback) {
        db.collection(TASKS_COLLECTION).document(taskId).update(taskMap)
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(callback::onError);
    }

    public ListenerRegistration getTasks(String spaceId, TasksCallback callback) {
        return db.collection(TASKS_COLLECTION)
                .whereEqualTo("spaceId", spaceId)
                .whereEqualTo("status", Task.STATUS_PENDING)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) { callback.onError(error); return; }
                    if (value != null) {
                        List<Task> tasks = value.toObjects(Task.class);
                        for (int i = 0; i < tasks.size(); i++) tasks.get(i).setId(value.getDocuments().get(i).getId());
                        callback.onSuccess(tasks);
                    } else callback.onSuccess(new ArrayList<>());
                });
    }

    public ListenerRegistration getCompletedTasks(String spaceId, TasksCallback callback) {
        return db.collection(TASKS_COLLECTION)
                .whereEqualTo("spaceId", spaceId)
                .whereEqualTo("status", Task.STATUS_COMPLETED)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) { callback.onError(error); return; }
                    if (value != null) {
                        List<Task> tasks = value.toObjects(Task.class);
                        for (int i = 0; i < tasks.size(); i++) tasks.get(i).setId(value.getDocuments().get(i).getId());
                        callback.onSuccess(tasks);
                    } else callback.onSuccess(new ArrayList<>());
                });
    }

    public ListenerRegistration getCompletedTasksForSpaces(List<String> spaceIds, TasksCallback callback) {
        if (spaceIds.isEmpty()) { callback.onSuccess(new ArrayList<>()); return null; }
        return db.collection(TASKS_COLLECTION)
                .whereIn("spaceId", spaceIds)
                .whereEqualTo("status", Task.STATUS_COMPLETED)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (value != null) {
                        List<Task> tasks = value.toObjects(Task.class);
                        for (int i = 0; i < tasks.size(); i++) tasks.get(i).setId(value.getDocuments().get(i).getId());
                        callback.onSuccess(tasks);
                    }
                });
    }

    public ListenerRegistration getAllTasksForSpaces(List<String> spaceIds, TasksCallback callback) {
        if (spaceIds.isEmpty()) { callback.onSuccess(new ArrayList<>()); return null; }
        return db.collection(TASKS_COLLECTION)
                .whereIn("spaceId", spaceIds)
                .addSnapshotListener((value, error) -> {
                    if (value != null) {
                        List<Task> tasks = value.toObjects(Task.class);
                        for (int i = 0; i < tasks.size(); i++) tasks.get(i).setId(value.getDocuments().get(i).getId());
                        callback.onSuccess(tasks);
                    }
                });
    }

    public ListenerRegistration getTaskById(String taskId, TaskCallback callback) {
        return db.collection(TASKS_COLLECTION).document(taskId)
                .addSnapshotListener((snapshot, e) -> {
                    if (snapshot != null && snapshot.exists()) {
                        Task task = snapshot.toObject(Task.class);
                        if(task != null) { task.setId(snapshot.getId()); callback.onSuccess(task); }
                    }
                });
    }

    public void updateTaskStatus(String taskId, String status) {
        db.collection(TASKS_COLLECTION).document(taskId).update("status", status);
    }

    public void deleteTask(String taskId) {
        db.collection(TASKS_COLLECTION).document(taskId).delete();
    }
}