package com.feliperonderos.meetingapp;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.google.gson.Gson;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

public class ViewMeeting extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_meeting);
        Gson gson = new Gson();
        final MainActivity.Meeting meeting = gson.fromJson(getIntent().getStringExtra("meeting"), MainActivity.Meeting.class);
        TextView title = (TextView) findViewById(R.id.title);
        TextView info = (TextView) findViewById(R.id.place);
        TextView datetime = (TextView) findViewById(R.id.datetime);

        // Populate the data into the template view using the data object
        title.setText(meeting.name);
        info.setText(meeting.location_name);
        DateTime dateTime = new DateTime(meeting.dateTime*1000);
        String p = "";
        if (dateTime.toLocalDate().equals(new LocalDate())){
            p = "today";
        }
        else if (dateTime.toLocalDate().equals(new LocalDate().plusDays(1))){
            p = "tomorrow";
        }
        else {
            p = dateTime.toString(DateTimeFormat.forPattern("EEEE, MMMM dd, yyyy"));
        }
        p = p + " at " + dateTime.toString(DateTimeFormat.forPattern("h:mm a"));
        datetime.setText(p);
        Button seeMap = (Button) findViewById(R.id.seeMap);
        seeMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Uri gmmIntentUri = Uri.parse("geo:0,0?q=" + meeting.latitude + "," + meeting.longitude + "(" + meeting.location_name + ")");
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                mapIntent.setPackage("com.google.android.apps.maps");
                startActivity(mapIntent);
            }
        });
        Button navigate = (Button) findViewById(R.id.navigate);
        navigate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Uri gmmIntentUri = Uri.parse("google.navigation:q=" + meeting.latitude + "," + meeting.longitude);
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                mapIntent.setPackage("com.google.android.apps.maps");
                startActivity(mapIntent);
            }
        });
        Button invite = (Button) findViewById(R.id.invite);
        invite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                String sendString = "I've invited you to a meeting. Click the link below for details:";
                sendString = sendString + "https://modern-bolt-116904.appspot.com/viewmeeting/" + meeting.API_ID;
                sendIntent.putExtra(Intent.EXTRA_TEXT, sendString);
                sendIntent.setType("text/plain");
                startActivity(sendIntent);
            }
        });
        if (new DateTime().isAfter(meeting.dateTime * 1000)){invite.setVisibility(View.GONE);}
        if (new DateTime().minusHours(1).isBefore(meeting.dateTime*1000)&& new DateTime().plusHours(1).isAfter(meeting.dateTime*1000)){
            URL url;
            try {
                url = new URL("https://modern-bolt-116904.appspot.com/getmembers/"+meeting.API_ID.toString());
                String JSONString;
                new UserFromMeetingTask().execute(url);
                /**
                .get();
                int ps = 0;
                JSONObject everything = new JSONObject(JSONString);
    /**            String users = (String) everything.get("users");
                everything = new JSONObject(JSONString);
                Iterator<?> keys = everything.keys();
                while( keys.hasNext() ) {
                    String key = (String)keys.next();
                    if ( everything.get(key) instanceof JSONObject ) {
                        String name  = (String)((JSONObject) everything.get(key)).get("name");
                        long h = 0;
                    }
                }


            }**/}
            catch (MalformedURLException e){
                return;
            }
        }
    }   private class UserFromMeetingTask extends AsyncTask<URL, Void, String> {
        @Override
        protected String doInBackground(URL... urls) {
            try {
                URL url = urls[0];
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                /**urlConnection.setDoOutput(true);
                 BufferedWriter out = new BufferedWriter(new OutputStreamWriter(urlConnection.getOutputStream()));
                 out.write(((JSONObject) params[0]).toString());
                 out.flush();
                 out.close();
                 **/
                BufferedReader input = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                StringBuilder total = new StringBuilder();
                String line;
                while ((line = input.readLine()) != null) {
                    total.append((String)line);
                }

                String response = total.toString();
                int responseCode= urlConnection.getResponseCode();
                response = response.toString();
                return response;
            } catch (MalformedURLException e) {

            } catch (IOException e1) {

            }
            return "Failure";
        }
        @Override
        protected void onPostExecute(String result) {
            if (!result.equals("Failure"))
            addOtherUsers(result);
        }
    }

    private void addOtherUsers(String result) {
        try {
            JSONObject p = new JSONObject(result.substring(result.indexOf("{"), result.lastIndexOf("}") + 1));
            JSONArray users = p.getJSONArray("users");
            ArrayList<MeetingUser> meetingUsers = new ArrayList<>();
            for (int i = 0; i < users.length(); i++){
                JSONObject user = users.getJSONObject(i);
                String name = user.getString("name");
                String location = user.getString("location");
                Long time = user.getLong("time");
                String[] latLong = location.split(",");
                DateTime datetime = new DateTime(time*1000);
                MeetingUser userr = new MeetingUser(name,Double.parseDouble(latLong[0]),Double.parseDouble(latLong[1]),datetime);
                meetingUsers.add(userr);
            }
            ListView listView = (ListView) findViewById(R.id.listView);
            listView.setAdapter(new UserAdapter(this,meetingUsers));

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }
    private class MeetingUser{
        String name;
        double latitude,longitude;
        DateTime time;
        public MeetingUser(String name, double latitude, double longitude, DateTime time){
            this.name = name;
            this.latitude = latitude;
            this.longitude = longitude;
            this.time = time;
        }
    }
    public class UserAdapter extends ArrayAdapter<MeetingUser> {
        public UserAdapter(Context context, ArrayList<MeetingUser> users) {
            super(context, 0, users);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Gson gson = new Gson();
            final MainActivity.Meeting meeting = gson.fromJson(getIntent().getStringExtra("meeting"), MainActivity.Meeting.class);
            // Get the data item for this position
            final MeetingUser user = (MeetingUser)getItem(position);
            // Check if an existing view is being reused, otherwise inflate the view
            if (convertView == null) {
                convertView = LayoutInflater.from(getApplicationContext()).inflate(R.layout.user_list_view_tile, parent, false);
            }
            // Lookup view for data population
            TextView name = (TextView) convertView.findViewById(R.id.name);
            TextView distance = (TextView) convertView.findViewById(R.id.distance);
            TextView last_updated = (TextView) convertView.findViewById(R.id.last_updated);

            // Populate the data into the template view using the data object
            name.setText(user.name);
            float[] distanceArray = new float[1];
            Location.distanceBetween(meeting.latitude, meeting.longitude, user.latitude, user.longitude, distanceArray);
            distance.setText(((Float) distanceArray[0]).intValue() + " meters from meeting");
            Long difference  = ((new DateTime().getMillis() - user.time.getMillis())/1000);
            String minutes = Long.toString(difference / 60);
            if (minutes.equals("1")) last_updated.setText("Last updated 1 minute ago");
            else last_updated.setText("Last updated "+minutes+" minutes ago");
            convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Uri gmmIntentUri = Uri.parse("geo:0,0?q=" + user.latitude + "," + user.longitude);
                    Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                    mapIntent.setPackage("com.google.android.apps.maps");
                    startActivity(mapIntent);
                }
            });
            return convertView;
        }
    }

}
