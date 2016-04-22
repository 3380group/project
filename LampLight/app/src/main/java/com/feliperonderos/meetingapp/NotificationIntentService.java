package com.feliperonderos.meetingapp;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.support.v7.app.NotificationCompat;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderApi;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.gson.Gson;

import org.joda.time.DateTime;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by Felipe on 1/9/2016.
 */
public class NotificationIntentService extends IntentService implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {
    private static final int NOTIFICATION_ID = 1;
    private static final String ACTION_START = "ACTION_START";
    private static final String ACTION_DELETE = "ACTION_DELETE";
    GoogleApiClient mGoogleApiClient;

    public NotificationIntentService() {
        super(NotificationIntentService.class.getSimpleName());
    }

    public static Intent createIntentStartNotificationService(Context context) {
        Intent intent = new Intent(context, NotificationIntentService.class);
        return intent;
    }

    public static Intent createIntentDeleteNotification(Context context) {
        Intent intent = new Intent(context, NotificationIntentService.class);
        intent.setAction(ACTION_DELETE);
        return intent;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        try {
            if (LocationAvailability.hasLocationAvailability(intent) && LocationResult.hasResult(intent)) {
                Location location = LocationResult.extractResult(intent).getLocations().get(0);
                updateLocation(location);
            }
            if (intent.hasExtra(FusedLocationProviderApi.KEY_LOCATION_CHANGED)) {

                Location location = intent.getParcelableExtra(FusedLocationProviderApi.KEY_LOCATION_CHANGED);
                updateLocation(location);

                //Log.d("locationtesting", "accuracy: " + location.getAccuracy() + " lat: " + location.getLatitude() + " lon: " + location.getLongitude());
            }
            String action = intent.getAction();
            if ("START_ON_TIME".equals(action)) {
                Gson gson = new Gson();
                final MainActivity.Meeting meeting = gson.fromJson(intent.getStringExtra("meeting"), MainActivity.Meeting.class);
                processStartNowNotification(meeting);
            }
            if (ACTION_DELETE.equals(action)) {
                processDeleteNotification(intent);
            }
            if ("START_HOUR_BEFORE".equals(action)) {
                Gson gson = new Gson();
                final MainActivity.Meeting meeting = gson.fromJson(intent.getStringExtra("meeting"), MainActivity.Meeting.class);
                processStartHourNotification(meeting);
            }
            if ("BROADCAST_LOCATION".equals(action)) {
                Gson gson = new Gson();
                final MainActivity.Meeting meeting = gson.fromJson(intent.getStringExtra("meeting"), MainActivity.Meeting.class);
                if (new DateTime().isAfter(meeting.dateTime * 1000 + 3600000)) {
                    //cancel pendingintent
                } else {
                    broadcastLocation();
                }

            }
        } finally {
            WakefulBroadcastReceiver.completeWakefulIntent(intent);
        }
    }

    private void updateLocation(Location location) {
        JSONObject object = new JSONObject();
        try {
            object.put("latitude", location.getLatitude());
            object.put("longitude", location.getLongitude());
            object.put("uid", getSharedPreferences("prefs", 0).getLong("uid", 0));
            object.put("name", getSharedPreferences("prefs", 0).getString("name", "unknown"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if (getSharedPreferences("prefs", 0).getLong("uid", 0) != 0) {
            new SendLocationTask().doInBackground(object);
        }
    }

    private void processDeleteNotification(Intent intent) {
        // Log something?
    }

    private void broadcastLocation() {

    }

    private void processStartNowNotification(MainActivity.Meeting meeting) {
        final NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        Intent intent = new Intent(getApplicationContext(), ViewMeeting.class);
        Gson gson = new Gson();
        intent.putExtra("meeting", gson.toJson(meeting));
        builder.setContentTitle(meeting.name)
                .setAutoCancel(true)
                .setColor(getResources().getColor(R.color.colorAccent))
                .setContentText(meeting.location_name)
                .setVibrate(new long[]{5000, 1000, 500})
                .setLights(Color.WHITE, 500, 3000)
                .setVisibility(1)
                .setSmallIcon(R.drawable.cast_ic_notification_1);


        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                NOTIFICATION_ID,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(pendingIntent);
        final NotificationManager manager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(NOTIFICATION_ID, builder.build());
    }

    private void processStartHourNotification(MainActivity.Meeting meeting) {
        String notificationString;
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
        mGoogleApiClient.connect();
        addUIDToMeeting(meeting.API_ID);
        DateTime now = new DateTime();
        if (now.plusMinutes(58).isBefore(meeting.dateTime * 1000)) {
            notificationString = "Meeting in an hour";
        } else {
            return;
        }
        final NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        Intent intent = new Intent(getApplicationContext(), ViewMeeting.class);
        Gson gson = new Gson();
        intent.putExtra("meeting", gson.toJson(meeting));
        builder.setContentTitle(notificationString)
                .setAutoCancel(true)
                .setColor(getResources().getColor(R.color.colorAccent))
                .setContentText(meeting.name + ", " + meeting.location_name)
                .setVibrate(new long[]{500, 1000, 500})
                .setLights(Color.WHITE, 500, 3000)
                .setVisibility(1)
                .setSmallIcon(R.drawable.cast_ic_notification_1);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                NOTIFICATION_ID,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(pendingIntent);
        final NotificationManager manager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(NOTIFICATION_ID, builder.build());
    }

    private void addUIDToMeeting(Long api_id) {
        JSONObject object = new JSONObject();
        try {
            object.put("meeting", api_id);
            if (getSharedPreferences("prefs", 0).getLong("uid", 0) != 0) {
                object.put("user",getSharedPreferences("prefs", 0).getLong("uid", 0));
                new AddUserToMeetingTask().doInBackground(object);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onConnected(Bundle bundle) {
        /**LocationRequest mLocationRequest = new LocationRequest();
         mLocationRequest.setInterval(60);//000);
         mLocationRequest.setFastestInterval(60);//000);
         mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
         PendingIntent pendingIntent=PendingIntent.getBroadcast(getApplicationContext(), 0, new Intent(this, NotificationIntentService.class), PendingIntent.FLAG_UPDATE_CURRENT);
         try {
         LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient,mLocationRequest,pendingIntent);
         Toast.makeText(getApplicationContext(), "updating location", Toast.LENGTH_SHORT).show();
         }
         catch (SecurityException e){

         }
         **/
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(60000);
        mLocationRequest.setFastestInterval(60000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setExpirationDuration(7200000);
        //mLocationRequest.setSmallestDisplacement(0.1F);

        // create the Intent to use WebViewActivity to handle results
        Intent mRequestLocationUpdatesIntent = new Intent(this, NotificationIntentService.class);

        // create a PendingIntent
        PendingIntent mRequestLocationUpdatesPendingIntent = PendingIntent.getService(getApplicationContext(), 0,
                mRequestLocationUpdatesIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        // request location updates
        try{
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient,
                mLocationRequest,
                mRequestLocationUpdatesPendingIntent);}
        catch (SecurityException e){

        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }
    private class SendLocationTask extends AsyncTask<JSONObject, Void, String> {
        @Override
        protected String doInBackground(JSONObject... params) {
            try {
                URL url = new URL("https://modern-bolt-116904.appspot.com/updatelocation/");
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setDoOutput(true);
                BufferedWriter out = new BufferedWriter(new OutputStreamWriter(urlConnection.getOutputStream()));
                out.write(((JSONObject) params[0]).toString());
                out.flush();
                out.close();
                BufferedReader input = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                StringBuilder total = new StringBuilder();
                String line;
                while ((line = input.readLine()) != null) {
                    total.append(line);
                }
                String response = total.toString();
                int responseCode=urlConnection.getResponseCode();
                return response;
            } catch (MalformedURLException e) {

            } catch (IOException e1) {

            }
            return "failure";
        }
        @Override
        protected void onPostExecute(String result) {
            //text.setText(result);
        }
    }
    private class AddUserToMeetingTask extends AsyncTask<JSONObject, Void, String> {
        @Override
        protected String doInBackground(JSONObject... params) {
            try {
                URL url = new URL("https://modern-bolt-116904.appspot.com/addusertomeeting/");
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setDoOutput(true);
                BufferedWriter out = new BufferedWriter(new OutputStreamWriter(urlConnection.getOutputStream()));
                out.write(((JSONObject) params[0]).toString());
                out.flush();
                out.close();
                BufferedReader input = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                StringBuilder total = new StringBuilder();
                String line;
                while ((line = input.readLine()) != null) {
                    total.append(line);
                }
                String response = total.toString();
                int responseCode=urlConnection.getResponseCode();
                return response;
            } catch (MalformedURLException e) {

            } catch (IOException e1) {

            }
            return "failure";
        }
        @Override
        protected void onPostExecute(String result) {
            //text.setText(result);
        }
    }
}
