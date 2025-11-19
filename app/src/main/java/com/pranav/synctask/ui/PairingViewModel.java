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

    // To report results of Personal Link creation
    private final MutableLiveData<Result<Space>> linkPartnerResult = new MutableLiveData<>();

    public PairingViewModel() {
        this.userRepository = UserRepository.getInstance();
    }

    // 1. Join Shared Space (Existing)
    public LiveData<Result<Space>> joinSpace(String inviteCode, String userUID) {
        return userRepository.joinSpace(inviteCode, userUID);
    }

    // 2. Create Personal Link (New for Requirement 3)
    public LiveData<Result<Space>> getLinkPartnerResult() {
        return linkPartnerResult;
    }

    public void linkPartner(String partnerUid, String currentUid) {
        if (currentUid == null || partnerUid == null || partnerUid.isEmpty() || currentUid.equals(partnerUid)) {
            linkPartnerResult.setValue(new Result.Error<>(new Exception("Invalid User ID.")));
            return;
        }

        linkPartnerResult.setValue(new Result.Loading<>());

        // First, fetch the partner's name to name the space nicely
        userRepository.getUser(partnerUid).observeForever(userResult -> {
            if (userResult instanceof Result.Success) {
                User partner = ((Result.Success<User>) userResult).data;
                String spaceName = "Tasks with " + partner.getDisplayName();

                // Now create the link
                userRepository.createPersonalLink(currentUid, partnerUid, spaceName)
                        .observeForever(linkPartnerResult::setValue);
            } else if (userResult instanceof Result.Error) {
                linkPartnerResult.setValue(new Result.Error<>(new Exception("User not found. Check the ID.")));
            }
        });
    }
}