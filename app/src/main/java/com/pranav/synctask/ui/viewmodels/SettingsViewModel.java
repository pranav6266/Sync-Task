package com.pranav.synctask.ui.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.pranav.synctask.data.Result;
import com.pranav.synctask.data.UserRepository;
import com.pranav.synctask.models.User;

public class SettingsViewModel extends ViewModel {
    private final UserRepository userRepository;
    private final MutableLiveData<Result<User>> userLiveData = new MutableLiveData<>();

    // Results for UI actions
    private final MutableLiveData<Result<String>> generateCodeResult = new MutableLiveData<>();
    private final MutableLiveData<Result<Void>> linkPartnerResult = new MutableLiveData<>();
    private final MutableLiveData<Result<Void>> unlinkPartnerResult = new MutableLiveData<>();

    public SettingsViewModel() {
        this.userRepository = UserRepository.getInstance();
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid != null) {
            userRepository.addUserListener(uid, userLiveData);
        }
    }

    public LiveData<Result<User>> getUserLiveData() { return userLiveData; }
    public LiveData<Result<String>> getGenerateCodeResult() { return generateCodeResult; }
    public LiveData<Result<Void>> getLinkPartnerResult() { return linkPartnerResult; }

    public void generatePartnerCode() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid != null) {
            userRepository.generatePartnerCode(uid, generateCodeResult);
        }
    }

    public void linkPartner(String inviteCode) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid != null) {
            userRepository.linkPartner(uid, inviteCode, linkPartnerResult);
        }
    }

    public void unlinkPartner() {
        String uid = FirebaseAuth.getInstance().getUid();
        // We need the current user's partnerSpaceId to remove it
        Result<User> currentVal = userLiveData.getValue();
        if (uid != null && currentVal instanceof Result.Success) {
            String partnerSpaceId = ((Result.Success<User>) currentVal).data.getPartnerSpaceId();
            if (partnerSpaceId != null) {
                userRepository.unlinkPartner(uid, partnerSpaceId, unlinkPartnerResult);
            }
        }
    }

    public void logout() {
        FirebaseAuth.getInstance().signOut();
        // UI should observe auth state and navigate to login
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        userRepository.removeUserListener();
    }
}