package com.feliperonderos.meetingapp;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.location.places.Place;

import org.json.JSONArray;
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
import java.util.ArrayList;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getSharedPreferences("prefs",0).getLong("uid",0)==0){
            try{
                URL p = new URL(getResources().getString(R.string.api_url)+"/newuser/");
                UIDTask task = new UIDTask();
                task.execute(p);
            }
            catch (MalformedURLException e){
                int ps = 3;
            }
        }
        if (getSharedPreferences("prefs",0).getString("name","")==""){
            startActivity(new Intent(this,AddName.class));
        }
        setContentView(R.layout.activity_main2);
        ///NotificationReceiver.setupNotificationsAndGeofences(this);
        JSONObject Meeting = new JSONObject();
        Uri data = getIntent().getData();
        if(data != null){
            String path = data.getPath();
            getIntent().setData(null);
            long id = Long.parseLong(path.substring(1));
            JSONObject obj = new JSONObject();
            try {
                obj.put("user",getSharedPreferences("prefs",0).getLong("uid",0));
                obj.put("friend",id);
                new AddFriendToMeetingTask().execute(obj);
            } catch (JSONException e) {

            }

        };

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null){
            String statusText = extras.getString("textStatus");
            if (statusText == null){
                statusText = "";
            }
            intent.removeExtra("textStatus");
            int status = extras.getInt("status");
            intent.removeExtra("status");
            changeStatus(status,statusText);

        }

        /**MyTask p = (MyTask) new MyTask().execute(Meeting);
        try {
            addMeetingByID(p.get());
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }**/
        fetchUsers();
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        Drawable myFabSrc = getResources().getDrawable(android.R.drawable.ic_input_add);
//copy it in a new one
        Drawable willBeWhite = myFabSrc.getConstantState().newDrawable();
//set the color filter, you can use also Mode.SRC_ATOP
        willBeWhite.mutate().setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
//set it to your fab button initialized before
        fab.setImageDrawable(willBeWhite);
        fab.refreshDrawableState();
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(), DialogActivity.class));
                //pickPlace();
            }
        });
        Button friendButton = (Button) findViewById(R.id.friend_button);
        friendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                inviteFriends();
            }
        });
    }
    private void changeStatus(int status,String statusText){
        Long uid = getSharedPreferences("prefs", 0).getLong("uid", 0);
        String name = getSharedPreferences("prefs", 0).getString("name", "Unknown");
        getSharedPreferences("prefs", 0).edit().putString("statusText", statusText).commit();
        getSharedPreferences("prefs", 0).edit().putInt("status", status).commit();
        JSONObject p = new JSONObject();
        try {
            p.put("uid",uid);
            p.put("statusText",statusText);
            p.put("status",status);
            p.put("name",name);
            new UpdateStatusTask().execute(p);
        } catch (JSONException e) {
            //e.printStackTrace();
        }

    }
    private void inviteFriends(){
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        String sendString = "I've invited you to be my friend on LampLight. Click the link to add me: ";
        sendString = sendString + getResources().getString(R.string.api_url) + "/addfriend/" +getSharedPreferences("prefs",0).getLong("uid",0);
        sendIntent.putExtra(Intent.EXTRA_TEXT, sendString);
        sendIntent.setType("text/plain");
        startActivity(sendIntent);
    }
    @Override
    protected void onResume() {
        super.onResume();
        //NotificationReceiver.setupNotificationsAndGeofences(this);
    }
    private void fetchUsers(){
        long uid = getSharedPreferences("prefs",0).getLong("uid",0);
        URL url = null;
        try{
            url =  new URL( getResources().getString(R.string.api_url) + "/getfriends/"+uid);
        }
        catch (MalformedURLException e){
            return;
        }
        new GetFriendsTask().execute(url);
    }
    private class UpdateStatusTask extends AsyncTask<JSONObject, Void, String> {
        @Override
        protected String doInBackground(JSONObject... params) {
            try {
                URL url = new URL( getResources().getString(R.string.api_url)+"/updatestatus/");
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
            //fetchUsers();
        }
    }

    private class AddFriendToMeetingTask extends AsyncTask<JSONObject, Void, String> {
        @Override
        protected String doInBackground(JSONObject... params) {
            try {
                URL url = new URL( getResources().getString(R.string.api_url)+"/addusertofriends/");
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
            fetchUsers();
        }
    }
    private class GetFriendsTask extends AsyncTask<URL, Void, String> {
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
                    total.append(line);
                }
                String response = total.toString();
                int responseCode= urlConnection.getResponseCode();
                return response;
            } catch (MalformedURLException e) {

            } catch (IOException e1) {

            }
            return "Failure";
        }

        @Override
        protected void onPostExecute(String s) {
            try {
                displayFriends(new JSONObject(s));
            } catch (JSONException e) {
                return;
            }
        }

    }

    private void displayFriends(JSONObject jsonObject) {
        ArrayList<User> users = new ArrayList<>();
        try {
            JSONArray JSONusers = jsonObject.getJSONArray("users");
            JSONObject JSONuser;
            User newUser;
            int len = JSONusers.length();
            for (int i = 0; i <len; i++){
                JSONuser = JSONusers.getJSONObject(i);
                newUser = new User(
                        JSONuser.getLong("api_id"),
                        JSONuser.getString("name"),
                        JSONuser.getLong("datetime"),
                        JSONuser.getDouble("latitude"),
                        JSONuser.getDouble("longitude"),
                        JSONuser.getInt("status"),
                        JSONuser.getString("statusText")
                                   );
                users.add(newUser);
            }
        } catch (JSONException e) {
            return;
        }
        ListView listView = (ListView) findViewById(R.id.listView);
        listView.setAdapter(new UserAdapter(this, users));
    }

    private class UIDTask extends AsyncTask<URL, Void, String> {
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
                    total.append(line);
                }
                String response = total.toString();
                int responseCode= urlConnection.getResponseCode();
                return response;
            } catch (MalformedURLException e) {

            } catch (IOException e1) {

            }
            return "Failure";
        }
        @Override
        protected void onPostExecute(String result) {
            try{
                getSharedPreferences("prefs",0).edit().putLong("uid",Long.parseLong(result)).commit();
            }
            catch (Exception e){
            }
        }
    }


    public static class User{
        Double latitude;
        Double longitude;
        Long dateTime, API_ID;
        String name;
        String statusText;
        Integer status;

        //List<Member> Members;
        public User(Long API_ID, String name,Long dateTime,Double latitude, Double longitude, int status,String statusText){
            //List<Member> Members;){
            this.latitude = latitude;
            this.longitude = longitude;
            this.dateTime = dateTime;
            this.name = name;
            this.status = status;
            this.statusText = statusText;
            this.API_ID = API_ID;
        }

    }
    public static class Meeting{
        Double latitude;
        Double longitude;
        Long dateTime, API_ID;
        String name;
        String location_name;

        //List<Member> Members;
        public Meeting(Long API_ID, String name,Long dateTime,Double latitude, Double longitude, String location_name){
            //List<Member> Members;){
            this.latitude = latitude;
            this.longitude = longitude;
            this.dateTime = dateTime;
            this.name = name;
            this.location_name = location_name;
            this.API_ID = API_ID;
        }

    }
    public class UserAdapter extends ArrayAdapter<User> {
        public UserAdapter(Context context, ArrayList<User> users) {
            super(context, 0, users);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // Get the data item for this position
            final User user = (User)getItem(position);
            // Check if an existing view is being reused, otherwise inflate the view
            if (convertView == null) {
                convertView = LayoutInflater.from(getApplicationContext()).inflate(R.layout.list_view_tile, parent, false);
            }
            // Lookup view for data population
            TextView title = (TextView) convertView.findViewById(R.id.title);
            TextView status = (TextView) convertView.findViewById(R.id.place);
            TextView textStatus = (TextView) convertView.findViewById(R.id.textStatus);
            TextView datetime = (TextView) convertView.findViewById(R.id.datetime);
            String availability = user.status.toString();
            switch (availability){
                case "1":
                    availability = "Unavailable";
                    break;
                case "2":
                    availability = "Available";
                    break;
                case "3":
                    availability = "Come Over";
                    break;
            }
            // Populate the data into the template view using the data object
            title.setText(user.name);
            status.setText(availability);
            textStatus.setText(user.statusText.toString());
            DateTime dateTime = new DateTime(user.dateTime*1000);
            String p = dateTime.toString(DateTimeFormat.forPattern("EEEE, MMMM dd, yyyy"));
            p ="Updated "+ p + " at " + dateTime.toString(DateTimeFormat.forPattern("h:mm a"));
            datetime.setText(p);
            /**convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(getApplicationContext(), ViewMeeting.class);
                    Gson gson = new Gson();
                    intent.putExtra("meeting", gson.toJson(meeting));
                    startActivity(intent);
                }
            });**/
            return convertView;
        }
    }
}
