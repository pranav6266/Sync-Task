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
        switch (position) {
            case 0:
                // Return the fragment for the first tab
                return new ThisMonthFragment();
            case 1:
                // Return the fragment for the second tab
                return new TodayFragment();
            case 2:
                // Return the fragment for the third tab
                return new AllTasksFragment();
            default:
                // This should never happen, but it's good practice
                return new UpdatesFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 4; // We have 4 tabs
    }
}
