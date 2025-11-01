package com.pranav.synctask.fragments;

import com.pranav.synctask.models.Task;
import java.util.List;
import java.util.stream.Collectors;

public class PartnerTasksFragment extends BaseTaskFragment {

    @Override
    protected List<Task> filterTasks(List<Task> tasks) {
        // "Partner's Tasks" in a Personal link are ones "Assigned" to them
        return tasks.stream()
                .filter(task -> Task.SCOPE_ASSIGNED.equals(task.getOwnershipScope()))
                .collect(Collectors.toList());
    }
}