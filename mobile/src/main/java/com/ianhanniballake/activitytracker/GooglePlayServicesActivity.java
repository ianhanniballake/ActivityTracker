package com.ianhanniballake.activitytracker;

import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessScopes;

public abstract class GooglePlayServicesActivity extends ActionBarActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {
    /**
     * Request code for auto Google Play Services error resolution.
     */
    protected static final int REQUEST_CODE_RESOLUTION = 1;
    private static final String TAG = GooglePlayServicesActivity.class.getSimpleName();
    private static final String KEY_IN_RESOLUTION = "is_in_resolution";
    /**
     * Google API client.
     */
    private GoogleApiClient mGoogleApiClient;
    private ConnectionResult mConnectionResult;
    /**
     * Determines if the client is in a resolution state, and
     * waiting for resolution intent to return.
     */
    private boolean mIsInResolution;
    /**
     * Track whether the sign-in button has been clicked so that we know to resolve
     * all issues preventing sign-in without waiting.
     */
    private boolean mSignInClicked;
    private ConnectionListener mConnectionListener;

    /**
     * Called when the activity is starting. Restores the activity state.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_google_play_services);
        if (savedInstanceState != null) {
            mIsInResolution = savedInstanceState.getBoolean(KEY_IN_RESOLUTION, false);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(final Menu menu) {
        boolean connected = mGoogleApiClient != null && mGoogleApiClient.isConnected();
        menu.findItem(R.id.menu_sign_out).setVisible(connected);
        return connected;
    }

    /**
     * Called when the Activity is made visible.
     * A connection to Play Services need to be initiated as
     * soon as the activity is visible. Registers {@code ConnectionCallbacks}
     * and {@code OnConnectionFailedListener} on the
     * activities itself.
     */
    @Override
    protected void onStart() {
        super.onStart();
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(Fitness.API)
                    .addScope(FitnessScopes.SCOPE_ACTIVITY_READ_WRITE)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
        }
        mGoogleApiClient.connect();
    }

    /**
     * Called when activity gets invisible. Connection to Play Services needs to
     * be disconnected as soon as an activity is invisible.
     */
    @Override
    protected void onStop() {
        if (mGoogleApiClient != null) {
            if (mConnectionListener != null) {
                mConnectionListener.onDisconnecting(mGoogleApiClient);
                mConnectionListener = null;
            }
            mGoogleApiClient.disconnect();
        }
        super.onStop();
    }

    /**
     * Saves the resolution state.
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_IN_RESOLUTION, mIsInResolution);
    }

    /**
     * Handles Google Play Services resolution callbacks.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_CODE_RESOLUTION:
                if (resultCode == RESULT_OK) {
                    retryConnecting();
                } else {
                    mIsInResolution = false;
                    mSignInClicked = false;
                }
                break;
        }
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_sign_out:
                mIsInResolution = false;
                mSignInClicked = false;
                if (mConnectionListener != null) {
                    mConnectionListener.onDisconnecting(mGoogleApiClient);
                }
                Fitness.ConfigApi.disableFit(mGoogleApiClient)
                        .setResultCallback(new ResultCallback<Status>() {
                            @Override
                            public void onResult(final Status status) {
                                mGoogleApiClient.disconnect();
                                retryConnecting();
                            }
                        });
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void retryConnecting() {
        mIsInResolution = false;
        if (!mGoogleApiClient.isConnecting()) {
            mGoogleApiClient.connect();
        }
    }

    public void setConnectionListener(ConnectionListener connectionListener) {
        mConnectionListener = connectionListener;
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            mConnectionListener.onConnected(mGoogleApiClient);
        }
    }

    /**
     * Called when {@code mGoogleApiClient} is connected.
     */
    @Override
    public void onConnected(Bundle connectionHint) {
        Log.i(TAG, "GoogleApiClient connected");
        supportInvalidateOptionsMenu();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.content_frame, getLoggedInFragment())
                .setTransitionStyle(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .commit();
    }

    protected abstract Fragment getLoggedInFragment();

    /**
     * Called when {@code mGoogleApiClient} connection is suspended.
     */
    @Override
    public void onConnectionSuspended(int cause) {
        Log.i(TAG, "GoogleApiClient connection suspended " + cause);
        supportInvalidateOptionsMenu();
        if (mConnectionListener != null) {
            mConnectionListener.onDisconnected();
            mConnectionListener = null;
        }
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.content_frame, new LoggedOutFragment())
                .setTransitionStyle(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .commit();
        retryConnecting();
    }

    /**
     * Called when {@code mGoogleApiClient} is trying to connect but failed.
     * Handle {@code result.getResolution()} if there is a resolution
     * available.
     */
    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.i(TAG, "GoogleApiClient connection failed: " + result.toString());
        supportInvalidateOptionsMenu();
        // If there is an existing resolution error being displayed or a resolution
        // activity has started before, do nothing and wait for resolution
        // progress to be completed.
        if (mIsInResolution) {
            return;
        }
        // Store the ConnectionResult so that we can use it later when the user clicks
        // 'sign-in'.
        mConnectionResult = result;
        if (mSignInClicked) {
            // The user has already clicked 'sign-in' so we attempt to resolve all
            // errors until the user is signed in, or they cancel.
            resolveSignInErrors();
        } else {
            if (mConnectionListener != null) {
                mConnectionListener.onDisconnected();
                mConnectionListener = null;
            }
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.content_frame, new LoggedOutFragment())
                    .setTransitionStyle(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                    .commit();
        }
    }

    public void beginLogin() {
        if (!mGoogleApiClient.isConnecting()) {
            mSignInClicked = true;
            resolveSignInErrors();
        }
    }

    private void resolveSignInErrors() {
        if (mConnectionResult == null) {
            retryConnecting();
            return;
        }
        if (!mConnectionResult.hasResolution()) {
            // Show a localized error dialog.
            GooglePlayServicesUtil.getErrorDialog(
                    mConnectionResult.getErrorCode(), this, 0, new OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            retryConnecting();
                        }
                    }).show();
            return;
        }
        try {
            mConnectionResult.startResolutionForResult(this, REQUEST_CODE_RESOLUTION);
        } catch (SendIntentException e) {
            Log.e(TAG, "Exception while starting resolution activity", e);
            retryConnecting();
        }
    }

    public interface ConnectionListener {
        public void onConnected(GoogleApiClient googleApiClient);

        public void onDisconnecting(GoogleApiClient googleApiClient);

        public void onDisconnected();
    }
}
