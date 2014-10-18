package com.ianhanniballake.activitytracker;

import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.Fields;

public class SensorListenerIntentService extends IntentService {
    public static final int NOTIFICATION_ID = 0;
    private static final String TAG = SensorListenerIntentService.class.getSimpleName();

    public SensorListenerIntentService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        DataPoint dataPoint = DataPoint.extract(intent);
        if (dataPoint == null) {
            Log.w(TAG, "Null DataPoint");
            return;
        }
        int activity = dataPoint.getValue(Fields.ACTIVITY).asInt();
        float confidence = dataPoint.getValue(Fields.CONFIDENCE).asFloat();
        Log.d(TAG, "Got activity " + activity + " with confidence " + confidence);
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        Intent launchIntent = getPackageManager().getLaunchIntentForPackage(getPackageName());
        PendingIntent pendingIntent = TaskStackBuilder.create(this).addNextIntentWithParentStack(launchIntent)
                .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        String text = getString(R.string.currently, getResources().getStringArray(R.array
                .detected_activity)[activity]);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_notification)
                .setColor(getResources().getColor(R.color.primary))
                .setContentTitle(getText(R.string.app_name))
                .setContentText(text)
                .setContentInfo(getString(R.string.confidence, confidence))
                .setContentIntent(pendingIntent)
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setOngoing(true);
        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }
}
