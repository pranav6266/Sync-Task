package com.pranav.synctask.ui;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseAuth;
import com.pranav.synctask.data.Result;
import com.pranav.synctask.data.UserRepository;
import com.pranav.synctask.models.Space;
import com.pranav.synctask.models.User;

import java.util.List;

public class DashboardViewModel extends ViewModel {
    private final UserRepository userRepository;
    private final MutableLiveData<Result<User>> userLiveData = new MutableLiveData<>();
    private final MutableLiveData<Result<List<Space>>> spacesLiveData = new MutableLiveData<>();
    private final MutableLiveData<Result<Space>> createSpaceResult = new MutableLiveData<>();

    public DashboardViewModel() {
        this.userRepository = UserRepository.getInstance();
    }

    public LiveData<Result<User>> getUserLiveData() {
        return userLiveData;
    }

    public LiveData<Result<List<Space>>> getSpacesLiveData() {
        return spacesLiveData;
    }

    public LiveData<Result<Space>> getCreateSpaceResult() {
        return createSpaceResult;
    }

    public void attachUserListener(String uid) {
        userRepository.addUserListener(uid, userLiveData);
    }

    public void loadSpaces(List<String> spaceIds) {
        userRepository.getSpaces(spaceIds).observeForever(spacesLiveData::setValue);
    }

    public void createSpace(String spaceName) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid != null) {
            userRepository.createSpace(spaceName, uid).observeForever(createSpaceResult::setValue);
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        userRepository.removeUserListener();
    }
}