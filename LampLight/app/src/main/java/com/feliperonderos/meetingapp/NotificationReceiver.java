package com.feliperonderos.meetingapp;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.v4.content.WakefulBroadcastReceiver;

import com.google.gson.Gson;

import org.joda.time.DateTime;
import org.joda.time.Instant;

import java.util.ArrayList;

/**
 * Created by Felipe on 1/9/2016.
 */
public class NotificationReceiver extends WakefulBroadcastReceiver {
    public static void setupNotificationsAndGeofences(Context context) {
        //getfromDB and set alarmIntentsForEach
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        ArrayList<MainActivity.Meeting> Meetings = new ArrayList<>();//MainActivity.fetchMeetings(context);
        DateTime now = new DateTime();
        for (MainActivity.Meeting meeting: Meetings){
            if (now.isBefore(new Instant(meeting.dateTime*1000))){
                PendingIntent alarmIntent = getStartPendingIntent(context, meeting,"START_ON_TIME");
                PendingIntent alarmIntent2 = getStartPendingIntent(context, meeting,"START_HOUR_BEFORE");
                PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, new Intent(context, NotificationIntentService.class).setAction("BROADCAST_LOCATION").putExtra("meeting", new Gson().toJson(meeting)), PendingIntent.FLAG_UPDATE_CURRENT);
                alarmManager.setRepeating(0,meeting.dateTime*1000 - 3600000,120000, pendingIntent);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP,meeting.dateTime*1000,alarmIntent);
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP,meeting.dateTime*1000 - 3600000,alarmIntent2);
                }
                else{
                    alarmManager.set(AlarmManager.RTC_WAKEUP, meeting.dateTime*1000, alarmIntent);
                    alarmManager.set(AlarmManager.RTC_WAKEUP, meeting.dateTime * 1000 - 3600000, alarmIntent2);
                }
            }
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Intent serviceIntent = NotificationIntentService.createIntentStartNotificationService(context);
        serviceIntent.putExtra("meeting",intent.getStringExtra("meeting"));
        serviceIntent.setAction(action);
        if (serviceIntent != null) {
            startWakefulService(context, serviceIntent);
        }
    }
    private static PendingIntent getStartPendingIntent(Context context, MainActivity.Meeting meeting, String action) {
        Intent intent = new Intent(context, NotificationReceiver.class);
        intent.setAction(action);
        Gson gson = new Gson();
        intent.putExtra("meeting", gson.toJson(meeting));
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }
}