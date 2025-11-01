package com.pranav.synctask.ui;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseAuth;
import com.pranav.synctask.data.Result;
import com.pranav.synctask.data.TaskRepository;
import com.pranav.synctask.data.UserRepository;
import com.pranav.synctask.models.DialogItem;
import com.pranav.synctask.models.Space;
import com.pranav.synctask.models.Task;
import com.pranav.synctask.models.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class DashboardViewModel extends ViewModel {
    private static final String TAG = "DashboardViewModel";
    private final UserRepository userRepository;
    private final TaskRepository taskRepository; // ADDED IN PHASE 3D
    private final String currentUid;
    private final MutableLiveData<Result<User>> userLiveData = new MutableLiveData<>();

    private final MutableLiveData<Result<List<Space>>> _allSpaces = new MutableLiveData<>();
    private final MutableLiveData<Result<List<Space>>> _sharedSpacesLiveData = new MutableLiveData<>();
    private final MutableLiveData<Result<List<Space>>> _personalLinksLiveData = new MutableLiveData<>();
    private final MutableLiveData<Result<List<User>>> _partnerDetails = new MutableLiveData<>(new Result.Success<>(new ArrayList<>()));
    private final MediatorLiveData<Result<List<DialogItem>>> _allDialogItems = new MediatorLiveData<>();

    // --- MODIFIED: REMOVED Progress Mediator, PersonalProgress, SharedProgress ---
    private final MutableLiveData<Result<List<Task>>> _allTasksResult = new MutableLiveData<>();

    private final MutableLiveData<Result<Space>> createSpaceResult = new MutableLiveData<>();
    private final MutableLiveData<Result<Void>> leaveSpaceResult = new MutableLiveData<>();
    private final MutableLiveData<Result<Void>> deleteSpaceResult = new MutableLiveData<>();

    // --- ADDED IN PHASE 3E ---
    private final MutableLiveData<Result<Space>> createLinkResult = new MutableLiveData<>();
    // --- END ADDED ---

    public DashboardViewModel() {
        this.userRepository = UserRepository.getInstance();
        this.taskRepository = TaskRepository.getInstance(); // ADDED IN PHASE 3D
        this.currentUid = FirebaseAuth.getInstance().getUid();
        // Observe the user to trigger space loading
        userLiveData.observeForever(userResult -> {
            if (userResult instanceof Result.Success) {
                User user = ((Result.Success<User>) userResult).data;
                if (user.getSpaceIds() != null && !user.getSpaceIds().isEmpty()) {
                    loadSpaces(user.getSpaceIds());

                    // ADDED IN PHASE 3D: Also load ALL tasks
                    taskRepository.attachAllTasksListener(user.getSpaceIds());
                } else {
                    _allSpaces.setValue(new Result.Success<>(new ArrayList<>()));

                    _sharedSpacesLiveData.setValue(new Result.Success<>(new ArrayList<>()));
                    _personalLinksLiveData.setValue(new Result.Success<>(new ArrayList<>()));
                    _allTasksResult.setValue(new Result.Success<>(new ArrayList<>())); // Clear tasks
                }
            } else if (userResult instanceof Result.Error) {

                Exception e = ((Result.Error<User>) userResult).exception;
                _sharedSpacesLiveData.setValue(new Result.Error<>(e));
                _personalLinksLiveData.setValue(new Result.Error<>(e));
                _allTasksResult.setValue(new Result.Error<>(e)); // Propagate error
            }
        });
        // Observe allSpaces to filter into two lists
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

                fetchPartnerDetails(personalLinks);

            } else if (spacesResult instanceof Result.Error) {
                Exception e = ((Result.Error<List<Space>>) spacesResult).exception;
                _sharedSpacesLiveData.setValue(new Result.Error<>(e));
                _personalLinksLiveData.setValue(new Result.Error<>(e));
            } else if (spacesResult instanceof Result.Loading) {
                _sharedSpacesLiveData.setValue(new Result.Loading<>());
                _personalLinksLiveData.setValue(new Result.Loading<>());
            }
        });
        // --- Mediator for the "Add Task" dialog list ---
        _allDialogItems.addSource(_personalLinksLiveData, result -> combineDialogItems());
        _allDialogItems.addSource(_sharedSpacesLiveData, result -> combineDialogItems());
        _allDialogItems.addSource(_partnerDetails, result -> combineDialogItems());

        // --- REMOVED Progress Calculation Mediator ---
        // This observer now just caches the task list
        taskRepository.getAllTasksResult().observeForever(result -> {
            if (result instanceof Result.Success) {
                _allTasksResult.setValue(result);
            } else if (result instanceof Result.Error) {
                Log.e(TAG, "Error loading all tasks");
                _allTasksResult.setValue(new Result.Error<>(((Result.Error<List<Task>>) result).exception));
            } else {
                _allTasksResult.setValue(new Result.Loading<>());
            }
        });
    }

    // --- REMOVED calculateProgress() method ---

    private void fetchPartnerDetails(List<Space> personalLinks) {
        if (personalLinks.isEmpty()) {
            _partnerDetails.setValue(new Result.Success<>(new ArrayList<>()));
            return;
        }

        _partnerDetails.setValue(new Result.Loading<>());
        List<User> partnerList = new ArrayList<>();
        AtomicInteger counter = new AtomicInteger(personalLinks.size());

        for (Space link : personalLinks) {
            String partnerUid = link.getMembers().stream().filter(memberId -> !memberId.equals(currentUid)).findFirst().orElse(null);
            if (partnerUid != null) {
                // Use a one-time fetch for this
                userRepository.getUser(partnerUid).observeForever(userResult -> {
                    if (userResult instanceof Result.Success) {
                        partnerList.add(((Result.Success<User>) userResult).data);

                    } else if (userResult instanceof Result.Error) {
                        Log.e(TAG, "Failed to fetch partner details for " + partnerUid, ((Result.Error<User>) userResult).exception);
                    }

                    if (counter.decrementAndGet() == 0) {

                        _partnerDetails.setValue(new Result.Success<>(partnerList));
                    }
                });
            } else {
                if (counter.decrementAndGet() == 0) {
                    _partnerDetails.setValue(new Result.Success<>(partnerList));
                }
            }
        }
    }

    private void combineDialogItems() {
        Result<List<Space>> personalResult = _personalLinksLiveData.getValue();
        Result<List<Space>> sharedResult = _sharedSpacesLiveData.getValue();
        Result<List<User>> partnersResult = _partnerDetails.getValue();

        if (personalResult instanceof Result.Success &&
                sharedResult instanceof Result.Success &&
                partnersResult instanceof Result.Success) {

            List<Space> personalLinks = ((Result.Success<List<Space>>) personalResult).data;
            List<Space> sharedSpaces = ((Result.Success<List<Space>>) sharedResult).data;
            List<User> partners = ((Result.Success<List<User>>) partnersResult).data;
            List<DialogItem> dialogItems = new ArrayList<>();
            for (Space link : personalLinks) {
                String partnerUid = null;
                for (String memberId : link.getMembers()) {
                    if (!memberId.equals(currentUid)) {
                        partnerUid = memberId;
                        break;
                    }
                }

                String partnerName = "Partner";
                if (partnerUid != null) {
                    for (User partner : partners) {
                        if (partner.getUid().equals(partnerUid)) {
                            partnerName = partner.getDisplayName();
                            break;
                        }
                    }
                }
                dialogItems.add(new DialogItem("Tasks with " + partnerName, link.getSpaceId(), Space.TYPE_PERSONAL));
            }

            for (Space space : sharedSpaces) {
                dialogItems.add(new DialogItem(space.getSpaceName(), space.getSpaceId(), Space.TYPE_SHARED));
            }

            _allDialogItems.setValue(new Result.Success<>(dialogItems));
        } else if (personalResult instanceof Result.Error || sharedResult instanceof Result.Error || partnersResult instanceof Result.Error) {
            _allDialogItems.setValue(new Result.Error<>(new Exception("Failed to load spaces")));
        } else {
            _allDialogItems.setValue(new Result.Loading<>());
        }
    }


    public LiveData<Result<User>> getUserLiveData() {
        return userLiveData;
    }

    public LiveData<Result<List<Space>>> getSharedSpacesLiveData() {
        return _sharedSpacesLiveData;
    }

    public LiveData<Result<List<Space>>> getPersonalLinksLiveData() {
        return _personalLinksLiveData;
    }

    public LiveData<Result<List<User>>> getPartnerDetails() {
        return _partnerDetails;
    }

    public LiveData<Result<List<DialogItem>>> getAllDialogItems() {
        return _allDialogItems;
    }

    // --- ADDED: Getter for all tasks ---
    public LiveData<Result<List<Task>>> getAllTasksResult() {
        return _allTasksResult;
    }
    // --- END ADDED ---

    // --- REMOVED getPersonalProgress() and getSharedProgress() ---

    public LiveData<Result<Space>> getCreateSpaceResult() {
        return createSpaceResult;
    }

    public LiveData<Result<Void>> getLeaveSpaceResult() {
        return leaveSpaceResult;
    }

    public LiveData<Result<Void>> getDeleteSpaceResult() {
        return deleteSpaceResult;
    }

    // --- ADDED IN PHASE 3E ---
    public LiveData<Result<Space>> getCreateLinkResult() {
        return createLinkResult;
    }
    // --- END ADDED ---

    public void attachUserListener(String uid) {
        userRepository.addUserListener(uid, userLiveData);
    }

    public void loadSpaces(List<String> spaceIds) {
        userRepository.getSpaces(spaceIds).observeForever(_allSpaces::setValue);
    }

    public void createSpace(String spaceName) {
        if (currentUid != null) {
            userRepository.createSpace(spaceName, currentUid).observeForever(createSpaceResult::setValue);
        }
    }

    // --- ADDED IN PHASE 3E ---
    public void createPersonalLink(String partnerUid) {
        if (currentUid == null || partnerUid.isEmpty() || currentUid.equals(partnerUid)) {
            createLinkResult.setValue(new Result.Error<>(new Exception("Invalid Partner UID.")));
            return;
        }

        // We need the partner's display name to create the default space name
        userRepository.getUser(partnerUid).observeForever(userResult -> {
            if (userResult instanceof Result.Success) {
                User partner = ((Result.Success<User>) userResult).data;
                String spaceName = "Tasks with " + partner.getDisplayName();

                userRepository.createPersonalLink(currentUid, partnerUid, spaceName)
                        .observeForever(createLinkResult::setValue);
            } else if (userResult instanceof Result.Error) {
                createLinkResult.setValue(new Result.Error<>(new Exception("Partner user not found.")));
            }
        });
    }
    // --- END ADDED ---

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

    @Override
    protected void onCleared() {
        super.onCleared();
        userRepository.removeUserListener();
        TaskRepository.removeAllTasksListener();
    }

    // --- REMOVED ProgressCalculationHelper inner class ---
}