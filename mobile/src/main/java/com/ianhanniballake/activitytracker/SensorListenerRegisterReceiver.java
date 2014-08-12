package com.ianhanniballake.activitytracker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class SensorListenerRegisterReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(SensorListenerRegisterReceiver.class.getSimpleName(), "onReceive: " + intent.getAction());
        Intent serviceIntent = new Intent(context, SensorListenerRegisterService.class);
        serviceIntent.setAction(intent.getAction());
        context.startService(serviceIntent);
    }
}
