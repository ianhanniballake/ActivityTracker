package com.ianhanniballake.activitytracker;

import android.support.v4.app.Fragment;

public class MainActivity extends GooglePlayServicesActivity {
    @Override
    protected Fragment getLoggedInFragment() {
        return new MainFragment();
    }
}
