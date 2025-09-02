package com.pranav.synctask.fragments;

import com.pranav.synctask.models.Task;
import java.util.List;
import java.util.stream.Collectors;

public class UpdatesFragment extends BaseTaskFragment {

    @Override
    protected List<Task> filterTasks(List<Task> tasks) {
        return tasks.stream()
                .filter(task -> Task.TYPE_UPDATE.equals(task.getTaskType()))
                .collect(Collectors.toList());
    }
}