package com.pranav.synctask.fragments;

import com.pranav.synctask.models.Task;
import com.pranav.synctask.utils.DateUtils;
import java.util.List;
import java.util.stream.Collectors;

public class ThisMonthFragment extends BaseTaskFragment {

    @Override
    protected List<Task> filterTasks(List<Task> tasks) {
        return tasks.stream()
                .filter(task -> DateUtils.isThisMonth(task.getDueDate()))
                .collect(Collectors.toList());
    }
}
