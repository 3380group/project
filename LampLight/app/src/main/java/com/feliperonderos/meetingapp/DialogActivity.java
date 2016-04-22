package com.feliperonderos.meetingapp;

import android.app.Activity;
import android.content.Intent;
import android.os.Parcelable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.joda.time.DateTime;

public class DialogActivity extends Activity {
    Button status1,status2,status3;
    EditText statusText;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dialog);
        statusText = (EditText) findViewById(R.id.statusText);
        String p = getSharedPreferences("prefs", 0).getString("statusText","");
        if (p!= null){
        statusText.setText(p);
        }
        status1 = (Button) findViewById(R.id.status1);
        status2 = (Button) findViewById(R.id.status2);
        status3 = (Button) findViewById(R.id.status3);
        status1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent finishIntent = new Intent(getApplicationContext(), MainActivity.class);
                finishIntent.putExtra("status", 1);
                finishIntent.putExtra("textStatus",statusText.getText().toString());
                startActivity(finishIntent);
            }
        });
        status2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent finishIntent = new Intent(getApplicationContext(), MainActivity.class);
                finishIntent.putExtra("status", 2);
                finishIntent.putExtra("textStatus",statusText.getText().toString());
                startActivity(finishIntent);
            }
        });
        status3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent finishIntent = new Intent(getApplicationContext(), MainActivity.class);
                finishIntent.putExtra("status", 3);
                finishIntent.putExtra("textStatus",statusText.getText().toString());
                startActivity(finishIntent);
            }
        });
    }
}
