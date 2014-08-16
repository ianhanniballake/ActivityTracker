package com.ianhanniballake.activitytracker;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.fitness.DataTypes;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessScopes;
import com.google.android.gms.fitness.SensorRequest;

public class SensorListenerRegisterService extends Service implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {
    private static final String TAG = SensorListenerRegisterService.class.getSimpleName();
    private GoogleApiClient mGoogleApiClient;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Fitness.API)
                .addScope(FitnessScopes.SCOPE_ACTIVITY_READ_WRITE)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    @Override
    public int onStartCommand(final Intent intent, final int flags, final int startId) {
        super.onStartCommand(intent, flags, startId);
        Log.d(TAG, "onStartCommand: " + intent.getAction());
        if (!mGoogleApiClient.isConnecting() && !mGoogleApiClient.isConnected()) {
            Log.d(TAG, "Starting to connect");
            mGoogleApiClient.connect();
        }
        return START_STICKY;
    }

    @Override
    public void onConnected(final Bundle connectionHint) {
        Log.d(TAG, "onConnected");
        SensorRequest sensorRequest = new SensorRequest.Builder()
                .setDataType(DataTypes.ACTIVITY_SAMPLE)
                .build();
        Intent intent = new Intent(this, SensorListenerIntentService.class);
        PendingIntent pendingIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        PendingResult<Status> serviceRegResult = Fitness.SensorsApi.register(mGoogleApiClient, sensorRequest,
                pendingIntent);
        serviceRegResult.setResultCallback(new ResultCallback<Status>() {
            @Override
            public void onResult(final Status status) {
                if (status.isSuccess()) {
                    Log.d(TAG, "Service Sensor listener registered successfully");
                } else {
                    Log.e(TAG, "Service Sensor listener failed to register: " + status.getStatusMessage());
                }
                stopSelf();
            }
        });
    }

    @Override
    public void onConnectionSuspended(final int cause) {
        Log.d(TAG, "onConnectionSuspended: " + cause);
        stopSelf();
    }

    @Override
    public void onConnectionFailed(final ConnectionResult connectionResult) {
        Log.d(TAG, "onConnectionFailed: " + connectionResult.getErrorCode());
        stopSelf();
    }

    @Override
    public void onDestroy() {
        if (mGoogleApiClient != null) {
            mGoogleApiClient.disconnect();
        }
        super.onDestroy();
    }
}