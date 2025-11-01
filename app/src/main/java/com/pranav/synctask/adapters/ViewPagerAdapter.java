package com.pranav.synctask.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import com.pranav.synctask.fragments.AllTasksFragment;
import com.pranav.synctask.fragments.AssignedFragment;
import com.pranav.synctask.fragments.MyTasksFragment;
import com.pranav.synctask.fragments.PartnerTasksFragment;
import com.pranav.synctask.fragments.SharedFragment;
import com.pranav.synctask.models.Space;

public class ViewPagerAdapter extends FragmentStateAdapter {

    private final String contextType;

    public ViewPagerAdapter(@NonNull FragmentActivity fragmentActivity, String contextType) {
        super(fragmentActivity);
        this.contextType = (contextType != null) ? contextType : Space.TYPE_SHARED; // Default to shared
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if (Space.TYPE_PERSONAL.equals(contextType)) {
            // Personal Context: [All, My Tasks, Partner's Tasks]
            switch (position) {
                case 0:
                    return new AllTasksFragment();
                case 1:
                    return new MyTasksFragment();
                case 2:
                    return new PartnerTasksFragment();
                default:
                    return new AllTasksFragment();
            }
        } else {
            // Shared Context: [All, Shared, Assigned] - (Individual removed)
            switch (position) {
                case 1:
                    return new SharedFragment();  // Was case 2
                case 2:
                    return new AssignedFragment();  // Was case 3
                default:
                    return new AllTasksFragment(); 
            }
        }
    }

    @Override
    public int getItemCount() {
        return 3;
    }
}