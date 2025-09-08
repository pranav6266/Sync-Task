package com.pranav.synctask.data;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.ListenerRegistration;
import com.pranav.synctask.models.User;
import com.pranav.synctask.utils.FirebaseHelper;

public class UserRepository {
    private static volatile UserRepository instance;
    private ListenerRegistration userListenerRegistration;

    // OFFLINE SUPPORT: Cache the last known user object to check pairing status synchronously.
    private User currentUserCache;

    private UserRepository() {}

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

    public LiveData<Result<User>> createOrUpdateUser(FirebaseUser firebaseUser) {
        MutableLiveData<Result<User>> result = new MutableLiveData<>();
        result.setValue(new Result.Loading<>());
        FirebaseHelper.createOrUpdateUser(firebaseUser, new FirebaseHelper.UserCallback() {
            @Override
            public void onSuccess(User user) {
                currentUserCache = user; // Update cache
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
        userListenerRegistration = FirebaseHelper.addUserListener(uid, new FirebaseHelper.UserCallback() {
            @Override
            public void onSuccess(User user) {
                currentUserCache = user; // Update cache
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
        FirebaseHelper.pairUsers(currentUserUID, partnerCode, new FirebaseHelper.PairingCallback() {
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
        FirebaseHelper.unpairUsers(currentUserUID, new FirebaseHelper.PairingCallback() {
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
        FirebaseHelper.updateDisplayName(uid, newName, new FirebaseHelper.UserCallback() {
            @Override
            public void onSuccess(User user) {
                currentUserCache = user; // Update cache
                result.setValue(new Result.Success<>(user));
            }

            @Override
            public void onError(Exception e) {
                result.setValue(new Result.Error<>(e));
            }
        });
        return result;
    }

    public LiveData<Result<User>> getUser(String uid) {
        MutableLiveData<Result<User>> result = new MutableLiveData<>();
        result.setValue(new Result.Loading<>());
        FirebaseHelper.getUser(uid, new FirebaseHelper.UserCallback() {
            @Override
            public void onSuccess(User user) {
                currentUserCache = user; // Update cache
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