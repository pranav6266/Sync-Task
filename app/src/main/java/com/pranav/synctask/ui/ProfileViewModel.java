package com.pranav.synctask.ui.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.pranav.synctask.data.Result;
import com.pranav.synctask.data.UserRepository;
import com.pranav.synctask.models.User;

public class ProfileViewModel extends ViewModel {
    private final UserRepository userRepository;
    private final MutableLiveData<Result<User>> userLiveData = new MutableLiveData<>();

    public ProfileViewModel() {
        this.userRepository = UserRepository.getInstance();
    }

    public LiveData<Result<User>> getUserLiveData() {
        return userLiveData;
    }

    public void attachUserListener(String uid) {
        userRepository.addUserListener(uid, userLiveData);
    }

    public LiveData<Result<Void>> unpair(String uid) {
        return userRepository.unpairUsers(uid);
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