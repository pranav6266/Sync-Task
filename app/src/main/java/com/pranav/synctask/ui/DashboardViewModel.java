package com.pranav.synctask.ui;

import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.ListenerRegistration; // ADDED
import com.pranav.synctask.data.Result;
import com.pranav.synctask.data.TaskRepository;
import com.pranav.synctask.data.UserRepository;
import com.pranav.synctask.models.DialogItem;
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
    private static final String TAG = "DashboardViewModel";
    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final String currentUid;

    // Hold the listener locally in the ViewModel
    private ListenerRegistration mySpacesListener;

    private final MutableLiveData<Result<User>> userLiveData = new MutableLiveData<>();
    private final MutableLiveData<Result<List<Space>>> _allSpaces = new MutableLiveData<>();
    private final MutableLiveData<Result<List<Space>>> _sharedSpacesLiveData = new MutableLiveData<>();
    private final MutableLiveData<Result<List<Space>>> _personalLinksLiveData = new MutableLiveData<>();

    private final MutableLiveData<Map<String, User>> _membersMap = new MutableLiveData<>(new HashMap<>());

    private final MediatorLiveData<Result<List<DialogItem>>> _allDialogItems = new MediatorLiveData<>();
    private final MutableLiveData<Result<List<Task>>> _allTasksResult = new MutableLiveData<>();
    private final MutableLiveData<Result<Space>> createSpaceResult = new MutableLiveData<>();
    private final MutableLiveData<Result<Void>> leaveSpaceResult = new MutableLiveData<>();
    private final MutableLiveData<Result<Void>> deleteSpaceResult = new MutableLiveData<>();
    private final MutableLiveData<Result<Space>> createLinkResult = new MutableLiveData<>();

    public DashboardViewModel() {
        this.userRepository = UserRepository.getInstance();
        this.taskRepository = TaskRepository.getInstance();
        this.currentUid = FirebaseAuth.getInstance().getUid();

        userRepository.addUserListener(currentUid, userLiveData);

        if (currentUid != null) {
            // Capture the listener registration
            mySpacesListener = userRepository.getSpacesForUser(currentUid, _allSpaces);
        }

        _allSpaces.observeForever(spacesResult -> {
            if (spacesResult instanceof Result.Success) {
                List<Space> allSpaces = ((Result.Success<List<Space>>) spacesResult).data;

                List<Space> sharedSpaces = allSpaces.stream()
                        .filter(space -> Space.TYPE_SHARED.equals(space.getSpaceType()))
                        .collect(Collectors.toList());
                _sharedSpacesLiveData.setValue(new Result.Success<>(sharedSpaces));

                List<Space> personalLinks = allSpaces.stream()
                        .filter(space -> Space.TYPE_PERSONAL.equals(space.getSpaceType()))
                        .collect(Collectors.toList());
                _personalLinksLiveData.setValue(new Result.Success<>(personalLinks));

                fetchMemberDetails(allSpaces);

                List<String> spaceIds = allSpaces.stream().map(Space::getSpaceId).collect(Collectors.toList());
                if (!spaceIds.isEmpty()) {
                    taskRepository.attachAllTasksListener(spaceIds);
                }
            } else if (spacesResult instanceof Result.Error) {
                Exception e = ((Result.Error<List<Space>>) spacesResult).exception;
                _sharedSpacesLiveData.setValue(new Result.Error<>(e));
                _personalLinksLiveData.setValue(new Result.Error<>(e));
            }
        });

        _allDialogItems.addSource(_personalLinksLiveData, result -> combineDialogItems());
        _allDialogItems.addSource(_sharedSpacesLiveData, result -> combineDialogItems());
        _allDialogItems.addSource(_membersMap, result -> combineDialogItems());

        taskRepository.getAllTasksResult().observeForever(_allTasksResult::setValue);
    }

    // ... (Keep fetchMemberDetails, combineDialogItems, attachUserListener, createSpace, etc. exactly as they were) ...
    // Copy the helper methods from the previous full DashboardViewModel code.

    private void fetchMemberDetails(List<Space> allSpaces) {
        Set<String> uidsToFetch = new HashSet<>();
        for (Space space : allSpaces) {
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

    private void combineDialogItems() {
        Result<List<Space>> personalResult = _personalLinksLiveData.getValue();
        Result<List<Space>> sharedResult = _sharedSpacesLiveData.getValue();
        Map<String, User> members = _membersMap.getValue();

        if (personalResult instanceof Result.Success && sharedResult instanceof Result.Success) {
            List<Space> personalLinks = ((Result.Success<List<Space>>) personalResult).data;
            List<Space> sharedSpaces = ((Result.Success<List<Space>>) sharedResult).data;
            List<DialogItem> dialogItems = new ArrayList<>();

            for (Space link : personalLinks) {
                String partnerUid = link.getMembers().stream().filter(id -> !id.equals(currentUid)).findFirst().orElse(null);
                String partnerName = "Partner";
                if (partnerUid != null && members != null && members.containsKey(partnerUid)) {
                    partnerName = members.get(partnerUid).getDisplayName();
                }
                dialogItems.add(new DialogItem("Task with " + partnerName, link.getSpaceId(), Space.TYPE_PERSONAL));
            }

            for (Space space : sharedSpaces) {
                dialogItems.add(new DialogItem(space.getSpaceName(), space.getSpaceId(), Space.TYPE_SHARED));
            }
            _allDialogItems.setValue(new Result.Success<>(dialogItems));
        }
    }

    public void attachUserListener(String uid) { }
    public void createSpace(String spaceName) {
        if (currentUid != null) userRepository.createSpace(spaceName, currentUid).observeForever(createSpaceResult::setValue);
    }
    public void createPersonalLink(String partnerUid) {
        if (currentUid == null) return;
        userRepository.getUser(partnerUid).observeForever(userResult -> {
            if (userResult instanceof Result.Success) {
                User partner = ((Result.Success<User>) userResult).data;
                String spaceName = "Tasks with " + partner.getDisplayName();
                userRepository.createPersonalLink(currentUid, partnerUid, spaceName).observeForever(createLinkResult::setValue);
            } else {
                createLinkResult.setValue(new Result.Error<>(new Exception("User not found")));
            }
        });
    }
    public void leaveSpace(String spaceId) { if (currentUid != null) userRepository.leaveSpace(spaceId, currentUid).observeForever(leaveSpaceResult::setValue); }
    public void deleteSpace(String spaceId) { if (currentUid != null) userRepository.deleteSpace(spaceId, currentUid).observeForever(deleteSpaceResult::setValue); }

    @Override
    protected void onCleared() {
        super.onCleared();
        userRepository.removeUserListener();

        // UPDATED: Clean up the local listener
        if (mySpacesListener != null) {
            mySpacesListener.remove();
            mySpacesListener = null;
        }

        TaskRepository.removeAllTasksListener();
    }

    // Getters
    public LiveData<Map<String, User>> getMembersMap() { return _membersMap; }
    public LiveData<Result<User>> getUserLiveData() { return userLiveData; }
    public LiveData<Result<List<Space>>> getSharedSpacesLiveData() { return _sharedSpacesLiveData; }
    public LiveData<Result<List<Space>>> getPersonalLinksLiveData() { return _personalLinksLiveData; }
    public LiveData<Result<List<DialogItem>>> getAllDialogItems() { return _allDialogItems; }
    public LiveData<Result<List<Task>>> getAllTasksResult() { return _allTasksResult; }
    public LiveData<Result<Space>> getCreateSpaceResult() { return createSpaceResult; }
    public LiveData<Result<Space>> getCreateLinkResult() { return createLinkResult; }
    public LiveData<Result<Void>> getLeaveSpaceResult() { return leaveSpaceResult; }
    public LiveData<Result<Void>> getDeleteSpaceResult() { return deleteSpaceResult; }
    public void refreshSpaces() {
        if (currentUid != null) {
            // Just re-calling this will restart the listener in the repository
            // because we removed the 'spacesListenerRegistration' check in Repo.
            userRepository.getSpacesForUser(currentUid, _allSpaces);
        }
    }
}