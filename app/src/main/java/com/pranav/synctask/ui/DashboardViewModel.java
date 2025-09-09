package com.pranav.synctask.ui;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.pranav.synctask.data.Result;
import com.pranav.synctask.data.UserRepository;
import com.pranav.synctask.models.User;

public class DashboardViewModel extends ViewModel {
    private final UserRepository userRepository;
    private final MutableLiveData<Result<User>> userPairingStatus = new MutableLiveData<>();

    public DashboardViewModel() {
        this.userRepository = UserRepository.getInstance();
    }

    public LiveData<Result<User>> getUserPairingStatus() {
        return userPairingStatus;
    }

    public void attachUserListener(String uid) {
        userRepository.addUserListener(uid, userPairingStatus);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        userRepository.removeUserListener();
    }
}