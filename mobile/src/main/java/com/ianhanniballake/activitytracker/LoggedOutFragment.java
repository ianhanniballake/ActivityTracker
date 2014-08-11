package com.ianhanniballake.activitytracker;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.common.SignInButton;

public class LoggedOutFragment extends Fragment implements View.OnClickListener {
    private GooglePlayServicesActivity mGooglePlayServicesActivity;

    @Override
    public void onAttach(final Activity activity) {
        super.onAttach(activity);
        try {
            mGooglePlayServicesActivity = (GooglePlayServicesActivity) activity;
        } catch (ClassCastException e) {
            throw new IllegalStateException(activity.getClass().getSimpleName() +
                    " must be a subclass of " + GooglePlayServicesActivity.class.getSimpleName(), e);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_logged_out, container, false);
        SignInButton signInButton = (SignInButton) view.findViewById(R.id.sign_in_button);
        signInButton.setSize(SignInButton.SIZE_WIDE);
        signInButton.setOnClickListener(this);
        return view;
    }

    @Override
    public void onClick(final View view) {
        mGooglePlayServicesActivity.beginLogin();
    }
}
