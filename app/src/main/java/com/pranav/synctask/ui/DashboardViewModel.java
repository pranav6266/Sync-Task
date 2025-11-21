package com.pranav.synctask.ui;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.ListenerRegistration;
import com.pranav.synctask.data.Result;
import com.pranav.synctask.data.TaskRepository;
import com.pranav.synctask.data.UserRepository;
import com.pranav.synctask.models.Space;
import com.pranav.synctask.models.Task;
import com.pranav.synctask.models.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class DashboardViewModel extends ViewModel {

    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final String currentUid;

    // Listeners
    private ListenerRegistration mySpacesListener;

    // LiveData
    private final MutableLiveData<Result<User>> userLiveData = new MutableLiveData<>();
    private final MutableLiveData<Result<List<Space>>> _sharedSpacesLiveData = new MutableLiveData<>();
    private final MutableLiveData<Map<String, User>> _membersMap = new MutableLiveData<>(new HashMap<>());
    private final MutableLiveData<Result<List<Task>>> _allTasksResult = new MutableLiveData<>();

    // Actions Results
    private final MutableLiveData<Result<Space>> createSpaceResult = new MutableLiveData<>();
    private final MutableLiveData<Result<Space>> joinSpaceResult = new MutableLiveData<>();
    private final MutableLiveData<Result<Void>> leaveSpaceResult = new MutableLiveData<>();
    private final MutableLiveData<Result<Void>> deleteSpaceResult = new MutableLiveData<>();

    public DashboardViewModel() {
        this.userRepository = UserRepository.getInstance();
        this.taskRepository = TaskRepository.getInstance();
        this.currentUid = FirebaseAuth.getInstance().getUid();

        if (currentUid != null) {
            userRepository.addUserListener(currentUid, userLiveData);
            refreshSpaces();
        }

        // Observe task repository globally for progress bars
        taskRepository.getAllTasksResult().observeForever(_allTasksResult::setValue);
    }

    public void refreshSpaces() {
        if (currentUid == null) return;

        // Cancel previous listener if exists
        if (mySpacesListener != null) {
            mySpacesListener.remove();
        }

        // Fetch only spaces where I am a member
        mySpacesListener = userRepository.getSpacesForUser(currentUid, new MutableLiveData<Result<List<Space>>>() {
            @Override
            public void setValue(Result<List<Space>> result) {
                if (result instanceof Result.Success) {
                    List<Space> allSpaces = ((Result.Success<List<Space>>) result).data;

                    // Filter: We only want SHARED spaces now.
                    List<Space> sharedSpaces = allSpaces.stream()
                            .filter(space -> Space.TYPE_SHARED.equals(space.getSpaceType()))
                            .collect(Collectors.toList());

                    _sharedSpacesLiveData.setValue(new Result.Success<>(sharedSpaces));

                    // Fetch names of members in these spaces
                    fetchMemberDetails(sharedSpaces);

                    // Attach listeners for task progress
                    List<String> spaceIds = sharedSpaces.stream().map(Space::getSpaceId).collect(Collectors.toList());
                    if (!spaceIds.isEmpty()) {
                        taskRepository.attachAllTasksListener(spaceIds);
                    }
                } else if (result instanceof Result.Error) {
                    _sharedSpacesLiveData.setValue(new Result.Error<>(((Result.Error<?>) result).exception));
                }
            }
        });
    }

    private void fetchMemberDetails(List<Space> spaces) {
        Set<String> uidsToFetch = new HashSet<>();
        for (Space space : spaces) {
            if (space.getMembers() != null) uidsToFetch.addAll(space.getMembers());
        }
        uidsToFetch.remove(currentUid);

        Map<String, User> currentMap = _membersMap.getValue();
        if (currentMap == null) currentMap = new HashMap<>();
        final Map<String, User> mapRef = currentMap;

        for (String uid : uidsToFetch) {
            if (!mapRef.containsKey(uid)) {
                userRepository.getUser(uid).observeForever(userResult -> {
                    if (userResult instanceof Result.Success) {
                        User user = ((Result.Success<User>) userResult).data;
                        mapRef.put(user.getUid(), user);
                        _membersMap.setValue(mapRef);
                    }
                });
            }
        }
    }

    // --- Actions ---

    public void createSpace(String spaceName) {
        if (currentUid != null) {
            userRepository.createSpace(spaceName, currentUid).observeForever(createSpaceResult::setValue);
        }
    }

    public void joinSpace(String inviteCode) {
        if (currentUid != null) {
            joinSpaceResult.setValue(new Result.Loading<>());
            userRepository.joinSpace(inviteCode, currentUid).observeForever(joinSpaceResult::setValue);
        }
    }

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

    // --- Getters ---

    public LiveData<Result<User>> getUserLiveData() { return userLiveData; }
    public LiveData<Result<List<Space>>> getSharedSpacesLiveData() { return _sharedSpacesLiveData; }
    public LiveData<Map<String, User>> getMembersMap() { return _membersMap; }
    public LiveData<Result<List<Task>>> getAllTasksResult() { return _allTasksResult; }

    public LiveData<Result<Space>> getCreateSpaceResult() { return createSpaceResult; }
    public LiveData<Result<Space>> getJoinSpaceResult() { return joinSpaceResult; }
    public LiveData<Result<Void>> getLeaveSpaceResult() { return leaveSpaceResult; }
    public LiveData<Result<Void>> getDeleteSpaceResult() { return deleteSpaceResult; }

    @Override
    protected void onCleared() {
        super.onCleared();
        userRepository.removeUserListener();
        if (mySpacesListener != null) {
            mySpacesListener.remove();
        }
        TaskRepository.removeAllTasksListener();
    }
}