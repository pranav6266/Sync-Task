package com.pranav.synctask.ui.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.pranav.synctask.data.Result;
import com.pranav.synctask.data.UserRepository;
import com.pranav.synctask.models.User;

public class PairingViewModel extends ViewModel {
    private final UserRepository userRepository;

    public PairingViewModel() {
        this.userRepository = UserRepository.getInstance();
    }

    public LiveData<Result<User>> getUser(String uid) {
        return userRepository.getUser(uid);
    }

    public LiveData<Result<Void>> pairWithPartner(String currentUserUID, String partnerCode) {
        return userRepository.pairUsers(currentUserUID, partnerCode);
    }

    public LiveData<Result<User>> getUserPairingStatus() {
        MutableLiveData<Result<User>> userPairingStatus = new MutableLiveData<>();
        return userPairingStatus;
    }

    public void attachUserListener(String uid, MutableLiveData<Result<User>> userLiveData) {
        userRepository.addUserListener(uid, userLiveData);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        userRepository.removeUserListener();
    }
}