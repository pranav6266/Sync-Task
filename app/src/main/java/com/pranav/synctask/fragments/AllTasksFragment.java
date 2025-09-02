package com.pranav.synctask.fragments;

import com.pranav.synctask.models.Task;
import java.util.List;

public class AllTasksFragment extends BaseTaskFragment {

    @Override
    protected List<Task> filterTasks(List<Task> tasks) {
        // No filtering needed for all tasks
        return tasks;
    }
}