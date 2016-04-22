package com.feliperonderos.meetingapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by Felipe on 1/9/2016.
 */
public final class ServiceStarter extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        NotificationReceiver.setupNotificationsAndGeofences(context);
    }
}
