package com.pranav.synctask.fragments;

import com.pranav.synctask.models.Task;
import java.util.List;
import java.util.stream.Collectors;

public class IndividualFragment extends BaseTaskFragment {

    @Override
    protected List<Task> filterTasks(List<Task> tasks) {
        return tasks.stream()
                .filter(task -> Task.SCOPE_INDIVIDUAL.equals(task.getOwnershipScope()))
                .collect(Collectors.toList());
    }
}