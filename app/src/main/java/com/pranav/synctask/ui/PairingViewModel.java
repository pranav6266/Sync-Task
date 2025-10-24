package com.pranav.synctask.ui;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.pranav.synctask.data.Result;
import com.pranav.synctask.data.UserRepository;
import com.pranav.synctask.models.Space;
import com.pranav.synctask.models.User;

public class PairingViewModel extends ViewModel {
    private final UserRepository userRepository;

    public PairingViewModel() {
        this.userRepository = UserRepository.getInstance();
    }

    public LiveData<Result<Space>> joinSpace(String inviteCode, String userUID) {
        return userRepository.joinSpace(inviteCode, userUID);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
    }
}