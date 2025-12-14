package com.pranav.synctask.data;

import android.net.Uri;
import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.pranav.synctask.models.Space;
import com.pranav.synctask.models.User;
import com.pranav.synctask.utils.FirebaseHelper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserRepository {
    private static volatile UserRepository instance;
    private ListenerRegistration userListenerRegistration;
    private User currentUserCache;
    private final FirebaseHelper firebaseHelper;
    private final FirebaseFirestore db;

    private UserRepository() {
        firebaseHelper = new FirebaseHelper();
        db = FirebaseFirestore.getInstance();
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

    // --- USER PROFILE METHODS ---

    public LiveData<ListenerRegistration> addUserListener(String uid, MutableLiveData<Result<User>> userLiveData) {
        userLiveData.setValue(new Result.Loading<>());
        if (userListenerRegistration != null) userListenerRegistration.remove();

        userListenerRegistration = firebaseHelper.addUserListener(uid, new FirebaseHelper.UserCallback() {
            @Override public void onSuccess(User user) {
                currentUserCache = user;
                userLiveData.setValue(new Result.Success<>(user));
            }
            @Override public void onError(Exception e) {
                userLiveData.setValue(new Result.Error<>(e));
            }
        });
        return new MutableLiveData<>(userListenerRegistration);
    }

    public LiveData<Result<User>> getUser(String uid) {
        MutableLiveData<Result<User>> result = new MutableLiveData<>();
        result.setValue(new Result.Loading<>());
        firebaseHelper.getUser(uid, new FirebaseHelper.UserCallback() {
            @Override public void onSuccess(User user) {
                currentUserCache = user;
                result.setValue(new Result.Success<>(user));
            }
            @Override public void onError(Exception e) {
                result.setValue(new Result.Error<>(e));
            }
        });
        return result;
    }

    public LiveData<Result<User>> createOrUpdateUser(FirebaseUser firebaseUser) {
        MutableLiveData<Result<User>> result = new MutableLiveData<>();
        result.setValue(new Result.Loading<>());
        firebaseHelper.createOrUpdateUser(firebaseUser, new FirebaseHelper.UserCallback() {
            @Override public void onSuccess(User user) {
                currentUserCache = user;
                result.setValue(new Result.Success<>(user));
            }
            @Override public void onError(Exception e) {
                result.setValue(new Result.Error<>(e));
            }
        });
        return result;
    }

    // --- PARTNER LINKING METHODS (NEW FOR PHASE 4) ---

    // 1. Generate a Code (Create a hidden "Partner" space)
    public void generatePartnerCode(String uid, MutableLiveData<Result<String>> resultData) {
        resultData.setValue(new Result.Loading<>());

        // Create a space named "Partner Space"
        firebaseHelper.createSpace("Partner Space", uid, Space.TYPE_SHARED,new FirebaseHelper.SpaceCallback() {
            @Override
            public void onSuccess(Space space) {
                // Update the user's partnerSpaceId immediately
                db.collection("users").document(uid)
                        .update("partnerSpaceId", space.getSpaceId())
                        .addOnSuccessListener(aVoid -> resultData.setValue(new Result.Success<>(space.getInviteCode())))
                        .addOnFailureListener(e -> resultData.setValue(new Result.Error<>(e)));
            }
            @Override
            public void onError(Exception e) {
                resultData.setValue(new Result.Error<>(e));
            }
        });
    }

    // 2. Join a Partner (Enter Code)
    public void linkPartner(String myUid, String inviteCode, MutableLiveData<Result<Void>> resultData) {
        resultData.setValue(new Result.Loading<>());

        firebaseHelper.joinSpace(inviteCode, myUid, new FirebaseHelper.SpaceCallback() {
            @Override
            public void onSuccess(Space space) {
                // The helper adds us to the space members.
                // Now we must update our User doc AND the Partner's User doc to set the partnerSpaceId.

                // 1. Update My User Doc
                db.collection("users").document(myUid)
                        .update("partnerSpaceId", space.getSpaceId())
                        .addOnSuccessListener(aVoid -> resultData.setValue(new Result.Success<>(null)))
                        .addOnFailureListener(e -> resultData.setValue(new Result.Error<>(e)));
            }
            @Override
            public void onError(Exception e) {
                resultData.setValue(new Result.Error<>(e));
            }
        });
    }

    public void unlinkPartner(String myUid, String partnerSpaceId, MutableLiveData<Result<Void>> resultData) {
        resultData.setValue(new Result.Loading<>());

        // In a real app, you might delete the space or just remove the reference.
        // Here, we just set the ID to null for the current user.
        db.collection("users").document(myUid)
                .update("partnerSpaceId", null)
                .addOnSuccessListener(aVoid -> resultData.setValue(new Result.Success<>(null)))
                .addOnFailureListener(e -> resultData.setValue(new Result.Error<>(e)));
    }

    // --- UTILS ---

    public void removeUserListener() {
        if (userListenerRegistration != null) {
            userListenerRegistration.remove();
            userListenerRegistration = null;
        }
    }

    // Legacy support methods (Space creation, etc.) required by other VMs
    public ListenerRegistration getSpacesForUser(String uid, MutableLiveData<Result<List<Space>>> spacesLiveData) {
        return firebaseHelper.getSpacesForUser(uid, new FirebaseHelper.SpacesCallback() {
            @Override public void onSuccess(List<Space> spaces) { spacesLiveData.setValue(new Result.Success<>(spaces)); }
            @Override public void onError(Exception e) { spacesLiveData.setValue(new Result.Error<>(e)); }
        });
    }

    public LiveData<Result<Space>> createSpace(String spaceName, String creatorUID) {
        MutableLiveData<Result<Space>> result = new MutableLiveData<>();
        firebaseHelper.createSpace(spaceName, creatorUID, Space.TYPE_SHARED,new FirebaseHelper.SpaceCallback() {
            @Override public void onSuccess(Space space) { result.setValue(new Result.Success<>(space)); }
            @Override public void onError(Exception e) { result.setValue(new Result.Error<>(e)); }
        });
        return result;
    }

    public LiveData<Result<Space>> getSpace(String spaceId) {
        MutableLiveData<Result<Space>> result = new MutableLiveData<>();
        firebaseHelper.getSpace(spaceId, new FirebaseHelper.SpaceCallback() {
            @Override public void onSuccess(Space space) { result.setValue(new Result.Success<>(space)); }
            @Override public void onError(Exception e) { result.setValue(new Result.Error<>(e)); }
        });
        return result;
    }

    public LiveData<Result<List<User>>> getUsers(List<String> uids) {
        MutableLiveData<Result<List<User>>> result = new MutableLiveData<>();
        firebaseHelper.getUsers(uids, new FirebaseHelper.UsersCallback() {
            @Override public void onSuccess(List<User> users) { result.setValue(new Result.Success<>(users)); }
            @Override public void onError(Exception e) { result.setValue(new Result.Error<>(e)); }
        });
        return result;
    }

    // Placeholder methods for legacy delete/leave space if referenced
    public LiveData<Result<Void>> leaveSpace(String spaceId, String userUID) { return new MutableLiveData<>(); }
    public LiveData<Result<Void>> deleteSpace(String spaceId, String userUID) { return new MutableLiveData<>(); }
    public LiveData<Result<Space>> joinSpace(String inviteCode, String userUID) { return new MutableLiveData<>(); }
    public void updateFcmToken(String uid, String token) { firebaseHelper.updateFcmToken(uid, token); }
    public LiveData<Result<User>> updateDisplayName(String uid, String newName) { return new MutableLiveData<>(); }
    public LiveData<Result<String>> updateProfilePicture(FirebaseUser firebaseUser, Uri imageUri) { return new MutableLiveData<>(); }
}