package com.pranav.synctask.data;

import android.net.Uri;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.pranav.synctask.models.Space;
import com.pranav.synctask.models.User;
import com.pranav.synctask.utils.FirebaseHelper;
import java.util.List;

public class UserRepository {
    private static volatile UserRepository instance;
    private ListenerRegistration userListenerRegistration;
    // REMOVED: private ListenerRegistration spacesListenerRegistration; <--- Caused the bug
    private User currentUserCache;
    private final FirebaseHelper firebaseHelper;

    private UserRepository() {
        firebaseHelper = new FirebaseHelper();
    }

    public static UserRepository getInstance() {
        if (instance == null) {
            synchronized (UserRepository.class) {
                if (instance == null) {
                    instance = new UserRepository();
                }
            }
        }
        return instance;
    }

    // --- UPDATED: Returns the listener so ViewModel can own it ---
    public ListenerRegistration getSpacesForUser(String uid, MutableLiveData<Result<List<Space>>> spacesLiveData) {
        spacesLiveData.setValue(new Result.Loading<>());

        // We simply return the new registration. We do NOT cancel any existing ones here.
        return firebaseHelper.getSpacesForUser(uid, new FirebaseHelper.SpacesCallback() {
            @Override
            public void onSuccess(List<Space> spaces) {
                spacesLiveData.setValue(new Result.Success<>(spaces));
            }
            @Override
            public void onError(Exception e) {
                spacesLiveData.setValue(new Result.Error<>(e));
            }
        });
    }

    // REMOVED: removeSpacesListener() - No longer needed here.

    // ... (Keep all other methods: updateProfilePicture, createOrUpdateUser, etc. exactly as they were) ...

    public LiveData<Result<String>> updateProfilePicture(FirebaseUser firebaseUser, Uri imageUri) {
        MutableLiveData<Result<String>> result = new MutableLiveData<>(new Result.Loading<>());
        StorageReference storageRef = FirebaseStorage.getInstance().getReference();
        StorageReference photoRef = storageRef.child("profile_images/" + firebaseUser.getUid());
        photoRef.putFile(imageUri).addOnSuccessListener(taskSnapshot -> {
            photoRef.getDownloadUrl().addOnSuccessListener(uri -> {
                String photoUrl = uri.toString();
                UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder().setPhotoUri(uri).build();
                firebaseUser.updateProfile(profileUpdates).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        firebaseHelper.updatePhotoUrl(firebaseUser.getUid(), photoUrl, new FirebaseHelper.UserCallback() {
                            @Override public void onSuccess(User user) { result.setValue(new Result.Success<>(photoUrl)); }
                            @Override public void onError(Exception e) { result.setValue(new Result.Error<>(e)); }
                        });
                    } else { result.setValue(new Result.Error<>(task.getException())); }
                });
            });
        }).addOnFailureListener(e -> result.setValue(new Result.Error<>(e)));
        return result;
    }

    public LiveData<Result<User>> createOrUpdateUser(FirebaseUser firebaseUser) {
        MutableLiveData<Result<User>> result = new MutableLiveData<>();
        result.setValue(new Result.Loading<>());
        firebaseHelper.createOrUpdateUser(firebaseUser, new FirebaseHelper.UserCallback() {
            @Override public void onSuccess(User user) { currentUserCache = user; result.setValue(new Result.Success<>(user)); }
            @Override public void onError(Exception e) { result.setValue(new Result.Error<>(e)); }
        });
        return result;
    }

    public LiveData<ListenerRegistration> addUserListener(String uid, MutableLiveData<Result<User>> userLiveData) {
        MutableLiveData<ListenerRegistration> listener = new MutableLiveData<>();
        userLiveData.setValue(new Result.Loading<>());
        userListenerRegistration = firebaseHelper.addUserListener(uid, new FirebaseHelper.UserCallback() {
            @Override public void onSuccess(User user) { currentUserCache = user; userLiveData.setValue(new Result.Success<>(user)); }
            @Override public void onError(Exception e) { userLiveData.setValue(new Result.Error<>(e)); }
        });
        listener.setValue(userListenerRegistration);
        return listener;
    }

    public void removeUserListener() {
        if (userListenerRegistration != null) { userListenerRegistration.remove(); userListenerRegistration = null; }
    }

    public LiveData<Result<Space>> createSpace(String spaceName, String creatorUID) {
        MutableLiveData<Result<Space>> result = new MutableLiveData<>();
        result.setValue(new Result.Loading<>());
        firebaseHelper.createSpace(spaceName, creatorUID, new FirebaseHelper.SpaceCallback() {
            @Override public void onSuccess(Space space) { result.setValue(new Result.Success<>(space)); }
            @Override public void onError(Exception e) { result.setValue(new Result.Error<>(e)); }
        });
        return result;
    }

    public LiveData<Result<Space>> createPersonalLink(String creatorUID, String partnerUID, String spaceName) {
        MutableLiveData<Result<Space>> result = new MutableLiveData<>();
        result.setValue(new Result.Loading<>());
        firebaseHelper.createPersonalLink(creatorUID, partnerUID, spaceName, new FirebaseHelper.SpaceCallback() {
            @Override public void onSuccess(Space space) { result.setValue(new Result.Success<>(space)); }
            @Override public void onError(Exception e) { result.setValue(new Result.Error<>(e)); }
        });
        return result;
    }

    public LiveData<Result<Space>> joinSpace(String inviteCode, String userUID) {
        MutableLiveData<Result<Space>> result = new MutableLiveData<>();
        result.setValue(new Result.Loading<>());
        firebaseHelper.joinSpace(inviteCode, userUID, new FirebaseHelper.SpaceCallback() {
            @Override public void onSuccess(Space space) { result.setValue(new Result.Success<>(space)); }
            @Override public void onError(Exception e) { result.setValue(new Result.Error<>(e)); }
        });
        return result;
    }

    public LiveData<Result<User>> updateDisplayName(String uid, String newName) {
        MutableLiveData<Result<User>> result = new MutableLiveData<>();
        result.setValue(new Result.Loading<>());
        firebaseHelper.updateDisplayName(uid, newName, new FirebaseHelper.UserCallback() {
            @Override public void onSuccess(User user) { currentUserCache = user; result.setValue(new Result.Success<>(user)); }
            @Override public void onError(Exception e) { result.setValue(new Result.Error<>(e)); }
        });
        return result;
    }

    public void updateFcmToken(String uid, String token) { firebaseHelper.updateFcmToken(uid, token); }

    public LiveData<Result<User>> getUser(String uid) {
        MutableLiveData<Result<User>> result = new MutableLiveData<>();
        result.setValue(new Result.Loading<>());
        firebaseHelper.getUser(uid, new FirebaseHelper.UserCallback() {
            @Override public void onSuccess(User user) { currentUserCache = user; result.setValue(new Result.Success<>(user)); }
            @Override public void onError(Exception e) { result.setValue(new Result.Error<>(e)); }
        });
        return result;
    }

    public LiveData<Result<Void>> leaveSpace(String spaceId, String userUID) {
        MutableLiveData<Result<Void>> result = new MutableLiveData<>();
        firebaseHelper.leaveSpace(spaceId, userUID, new FirebaseHelper.SpaceCallback() {
            @Override public void onSuccess(Space space) { result.setValue(new Result.Success<>(null)); }
            @Override public void onError(Exception e) { result.setValue(new Result.Error<>(e)); }
        });
        return result;
    }

    public LiveData<Result<Void>> deleteSpace(String spaceId, String userUID) {
        MutableLiveData<Result<Void>> result = new MutableLiveData<>();
        firebaseHelper.deleteSpace(spaceId, userUID, new FirebaseHelper.SpaceCallback() {
            @Override public void onSuccess(Space space) { result.setValue(new Result.Success<>(null)); }
            @Override public void onError(Exception e) { result.setValue(new Result.Error<>(e)); }
        });
        return result;
    }
}