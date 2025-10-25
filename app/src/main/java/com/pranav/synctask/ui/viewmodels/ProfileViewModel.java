package com.pranav.synctask.ui.viewmodels;

import android.net.Uri;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.google.firebase.auth.FirebaseUser;
import com.pranav.synctask.data.Result;
// REMOVED: TaskRepository import
import com.pranav.synctask.data.UserRepository;
import com.pranav.synctask.models.User;
// REMOVED: Map import

public class ProfileViewModel extends ViewModel {
    private final UserRepository userRepository;
    // REMOVED: private final TaskRepository taskRepository;
    private final MutableLiveData<Result<User>> userLiveData = new MutableLiveData<>();
    // REMOVED: private final MutableLiveData<Map<String, Integer>> taskStats = new MutableLiveData<>();
    private final MutableLiveData<Result<String>> photoUpdateResult = new MutableLiveData<>();

    public ProfileViewModel() {
        this.userRepository = UserRepository.getInstance();
        // REMOVED: this.taskRepository = TaskRepository.getInstance();
    }

    public LiveData<Result<User>> getUserLiveData() {
        return userLiveData;
    }

    // REMOVED: public LiveData<Map<String, Integer>> getTaskStats() { ... }

    public LiveData<Result<String>> getPhotoUpdateResult() {
        return photoUpdateResult;
    }

    public void attachUserListener(String uid) {
        userRepository.addUserListener(uid, userLiveData);
    }

    // REMOVED: public void loadTaskStats() { ... }

    public void uploadProfilePicture(FirebaseUser firebaseUser, Uri imageUri) {
        userRepository.updateProfilePicture(firebaseUser, imageUri).observeForever(photoUpdateResult::setValue);
    }

    public LiveData<Result<User>> saveProfileChanges(String uid, String newName) {
        return userRepository.updateDisplayName(uid, newName);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        userRepository.removeUserListener();
    }
}