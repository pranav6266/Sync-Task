package com.pranav.synctask.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import com.pranav.synctask.fragments.AllTasksFragment;
import com.pranav.synctask.fragments.ThisMonthFragment;
import com.pranav.synctask.fragments.TodayFragment;
import com.pranav.synctask.fragments.UpdatesFragment;

public class ViewPagerAdapter extends FragmentStateAdapter {

    public ViewPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        // CHANGED: Swapped positions 0 and 1 to match the tab labels in MainActivity
        switch (position) {
            case 0:
                // "Today" tab
                return new TodayFragment();
            case 1:
                // "This Month" tab
                return new ThisMonthFragment();
            case 2:
                // "All" tab
                return new AllTasksFragment();
            default:
                // "Updates" tab
                return new UpdatesFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 4; // We have 4 tabs
    }
}
