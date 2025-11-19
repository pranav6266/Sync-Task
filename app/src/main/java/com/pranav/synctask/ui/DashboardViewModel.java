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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class DashboardViewModel extends ViewModel {
    private static final String TAG = "DashboardViewModel";
    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final String currentUid;

    private final MutableLiveData<Result<User>> userLiveData = new MutableLiveData<>();
    private final MutableLiveData<Result<List<Space>>> _allSpaces = new MutableLiveData<>();
    private final MutableLiveData<Result<List<Space>>> _sharedSpacesLiveData = new MutableLiveData<>();
    private final MutableLiveData<Result<List<Space>>> _personalLinksLiveData = new MutableLiveData<>();
    private final MutableLiveData<Result<List<User>>> _partnerDetails = new MutableLiveData<>(new Result.Success<>(new ArrayList<>()));
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

        // 1. Load User
        userRepository.addUserListener(currentUid, userLiveData);

        // 2. Load Spaces via Query (NEW: Independent of User object)
        if (currentUid != null) {
            userRepository.getSpacesForUser(currentUid, _allSpaces);
        }

        // 3. Filter Spaces
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

                // Load Tasks for these spaces (for progress bars)
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

        // 4. Setup Dialog Mediator
        _allDialogItems.addSource(_personalLinksLiveData, result -> combineDialogItems());
        _allDialogItems.addSource(_sharedSpacesLiveData, result -> combineDialogItems());
        _allDialogItems.addSource(_partnerDetails, result -> combineDialogItems());

        // 5. Observe Tasks
        taskRepository.getAllTasksResult().observeForever(_allTasksResult::setValue);
    }

    // --- Helper Methods (Restored) ---

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
                userRepository.getUser(partnerUid).observeForever(userResult -> {
                    if (userResult instanceof Result.Success) {
                        partnerList.add(((Result.Success<User>) userResult).data);
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
        } else {
            // One of the sources is loading or error
            _allDialogItems.setValue(new Result.Loading<>());
        }
    }

    // --- Actions ---

    public void attachUserListener(String uid) {
        // Already handled in constructor, kept for compatibility
    }

    public void createSpace(String spaceName) {
        if (currentUid != null) {
            userRepository.createSpace(spaceName, currentUid).observeForever(createSpaceResult::setValue);
        }
    }

    public void createPersonalLink(String partnerUid) {
        if (currentUid == null || partnerUid.isEmpty() || currentUid.equals(partnerUid)) {
            createLinkResult.setValue(new Result.Error<>(new Exception("Invalid Partner UID.")));
            return;
        }
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

    public void leaveSpace(String spaceId) {
        if (currentUid != null) userRepository.leaveSpace(spaceId, currentUid).observeForever(leaveSpaceResult::setValue);
    }

    public void deleteSpace(String spaceId) {
        if (currentUid != null) userRepository.deleteSpace(spaceId, currentUid).observeForever(deleteSpaceResult::setValue);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        userRepository.removeUserListener();
        userRepository.removeSpacesListener();
        TaskRepository.removeAllTasksListener();
    }

    // --- Getters ---
    public LiveData<Result<List<Space>>> getSharedSpacesLiveData() { return _sharedSpacesLiveData; }
    public LiveData<Result<List<Space>>> getPersonalLinksLiveData() { return _personalLinksLiveData; }
    public LiveData<Result<List<User>>> getPartnerDetails() { return _partnerDetails; }
    public LiveData<Result<List<DialogItem>>> getAllDialogItems() { return _allDialogItems; }
    public LiveData<Result<List<Task>>> getAllTasksResult() { return _allTasksResult; }
    public LiveData<Result<Space>> getCreateSpaceResult() { return createSpaceResult; }
    public LiveData<Result<Space>> getCreateLinkResult() { return createLinkResult; }
    public LiveData<Result<Void>> getLeaveSpaceResult() { return leaveSpaceResult; }
    public LiveData<Result<Void>> getDeleteSpaceResult() { return deleteSpaceResult; }
}