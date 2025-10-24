package com.pranav.synctask.data;

import android.net.Uri;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.pranav.synctask.models.User;
import com.pranav.synctask.utils.FirebaseHelper;

public class UserRepository {
    private static volatile UserRepository instance;
    private ListenerRegistration userListenerRegistration;
    private User currentUserCache;

    // CHANGED: Added an instance of FirebaseHelper
    private final FirebaseHelper firebaseHelper;

    // CHANGED: Constructor is private and initializes FirebaseHelper
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

    public User getCurrentUserCache() {
        return currentUserCache;
    }

    public LiveData<Result<String>> updateProfilePicture(FirebaseUser firebaseUser, Uri imageUri) {
        MutableLiveData<Result<String>> result = new MutableLiveData<>(new Result.Loading<>());
        StorageReference storageRef = FirebaseStorage.getInstance().getReference();
        StorageReference photoRef = storageRef.child("profile_images/" + firebaseUser.getUid());

        photoRef.putFile(imageUri).addOnSuccessListener(taskSnapshot -> {
            photoRef.getDownloadUrl().addOnSuccessListener(uri -> {
                String photoUrl = uri.toString();
                UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                        .setPhotoUri(uri)
                        .build();

                firebaseUser.updateProfile(profileUpdates).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // CHANGED: Call instance method
                        firebaseHelper.updatePhotoUrl(firebaseUser.getUid(), photoUrl, new FirebaseHelper.UserCallback() {
                            @Override
                            public void onSuccess(User user) {
                                result.setValue(new Result.Success<>(photoUrl));
                            }
                            @Override
                            public void onError(Exception e) {
                                result.setValue(new Result.Error<>(e));
                            }
                        });
                    } else {
                        result.setValue(new Result.Error<>(task.getException()));
                    }
                });
            }).addOnFailureListener(e -> result.setValue(new Result.Error<>(e)));
        }).addOnFailureListener(e -> result.setValue(new Result.Error<>(e)));

        return result;
    }

    public LiveData<Result<User>> createOrUpdateUser(FirebaseUser firebaseUser) {
        MutableLiveData<Result<User>> result = new MutableLiveData<>();
        result.setValue(new Result.Loading<>());
        // CHANGED: Call instance method
        firebaseHelper.createOrUpdateUser(firebaseUser, new FirebaseHelper.UserCallback() {
            @Override
            public void onSuccess(User user) {
                currentUserCache = user;
                result.setValue(new Result.Success<>(user));
            }

            @Override
            public void onError(Exception e) {
                result.setValue(new Result.Error<>(e));
            }
        });
        return result;
    }

    public LiveData<ListenerRegistration> addUserListener(String uid, MutableLiveData<Result<User>> userLiveData) {
        MutableLiveData<ListenerRegistration> listener = new MutableLiveData<>();
        userLiveData.setValue(new Result.Loading<>());
        // CHANGED: Call instance method
        userListenerRegistration = firebaseHelper.addUserListener(uid, new FirebaseHelper.UserCallback() {
            @Override
            public void onSuccess(User user) {
                currentUserCache = user;
                userLiveData.setValue(new Result.Success<>(user));
            }

            @Override
            public void onError(Exception e) {
                userLiveData.setValue(new Result.Error<>(e));
            }
        });
        listener.setValue(userListenerRegistration);
        return listener;
    }

    public void removeUserListener() {
        if (userListenerRegistration != null) {
            userListenerRegistration.remove();
            userListenerRegistration = null;
        }
    }

    public LiveData<Result<Void>> pairUsers(String currentUserUID, String partnerCode) {
        MutableLiveData<Result<Void>> result = new MutableLiveData<>();
        result.setValue(new Result.Loading<>());
        // CHANGED: Call instance method
        firebaseHelper.pairUsers(currentUserUID, partnerCode, new FirebaseHelper.PairingCallback() {
            @Override
            public void onSuccess() {
                result.setValue(new Result.Success<>(null));
            }
            @Override
            public void onError(Exception e) {
                result.setValue(new Result.Error<>(e));
            }
        });
        return result;
    }

    public LiveData<Result<Void>> unpairUsers(String currentUserUID) {
        MutableLiveData<Result<Void>> result = new MutableLiveData<>();
        result.setValue(new Result.Loading<>());
        // CHANGED: Call instance method
        firebaseHelper.unpairUsers(currentUserUID, new FirebaseHelper.PairingCallback() {
            @Override
            public void onSuccess() {
                result.setValue(new Result.Success<>(null));
            }
            @Override
            public void onError(Exception e) {
                result.setValue(new Result.Error<>(e));
            }
        });
        return result;
    }

    public LiveData<Result<User>> updateDisplayName(String uid, String newName) {
        MutableLiveData<Result<User>> result = new MutableLiveData<>();
        result.setValue(new Result.Loading<>());
        // CHANGED: Call instance method
        firebaseHelper.updateDisplayName(uid, newName, new FirebaseHelper.UserCallback() {
            @Override
            public void onSuccess(User user) {
                currentUserCache = user;
                result.setValue(new Result.Success<>(user));
            }

            @Override
            public void onError(Exception e) {
                result.setValue(new Result.Error<>(e));
            }
        });
        return result;
    }

    public void updateFcmToken(String uid, String token) {
        // CHANGED: Call instance method
        firebaseHelper.updateFcmToken(uid, token);
    }

    public LiveData<Result<User>> getUser(String uid) {
        MutableLiveData<Result<User>> result = new MutableLiveData<>();
        result.setValue(new Result.Loading<>());
        // CHANGED: Call instance method
        firebaseHelper.getUser(uid, new FirebaseHelper.UserCallback() {
            @Override
            public void onSuccess(User user) {
                currentUserCache = user;
                result.setValue(new Result.Success<>(user));
            }

            @Override
            public void onError(Exception e) {
                result.setValue(new Result.Error<>(e));
            }
        });
        return result;
    }
}
