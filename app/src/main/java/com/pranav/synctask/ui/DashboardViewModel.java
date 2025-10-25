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
    private final String currentUid;
    private final MutableLiveData<Result<User>> userLiveData = new MutableLiveData<>();
    private final MutableLiveData<Result<List<Space>>> spacesLiveData = new MutableLiveData<>();
    private final MutableLiveData<Result<Space>> createSpaceResult = new MutableLiveData<>();

    // --- NEW ---
    private final MutableLiveData<Result<Void>> leaveSpaceResult = new MutableLiveData<>();
    private final MutableLiveData<Result<Void>> deleteSpaceResult = new MutableLiveData<>();
    // --- END NEW ---

    public DashboardViewModel() {
        this.userRepository = UserRepository.getInstance();
        this.currentUid = FirebaseAuth.getInstance().getUid();
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

    // --- NEW ---
    public LiveData<Result<Void>> getLeaveSpaceResult() {
        return leaveSpaceResult;
    }

    public LiveData<Result<Void>> getDeleteSpaceResult() {
        return deleteSpaceResult;
    }
    // --- END NEW ---

    public void attachUserListener(String uid) {
        userRepository.addUserListener(uid, userLiveData);
    }

    public void loadSpaces(List<String> spaceIds) {
        userRepository.getSpaces(spaceIds).observeForever(spacesLiveData::setValue);
    }

    public void createSpace(String spaceName) {
        if (currentUid != null) {
            userRepository.createSpace(spaceName, currentUid).observeForever(createSpaceResult::setValue);
        }
    }

    // --- NEW ---
    public void leaveSpace(String spaceId) {
        if (currentUid != null) {
            userRepository.leaveSpace(spaceId, currentUid).observeForever(leaveSpaceResult::setValue);
        }
    }

    public void deleteSpace(String spaceId) {
        if (currentUid != null) {
            userRepository.deleteSpace(spaceId, currentUid).observeForever(deleteSpaceResult::setValue);
        }
    }
    // --- END NEW ---

    @Override
    protected void onCleared() {
        super.onCleared();
        userRepository.removeUserListener();
    }
}