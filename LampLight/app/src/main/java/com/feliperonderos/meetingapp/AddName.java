package com.feliperonderos.meetingapp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

/**
 * Created by Felipe on 1/11/2016.
 */
public class AddName extends Activity{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_name);
        EditText editText = (EditText) findViewById(R.id.name);
        editText.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                        (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    String p = ((EditText) v).getText().toString();
                    if (!((EditText) v).getText().toString().equals("")) {
                        getSharedPreferences("prefs", 0).edit().putString("name", p).commit();
                        startActivity(new Intent(getApplicationContext(),MainActivity.class));
                        return true;
                    } else {
                        Toast.makeText(getApplicationContext(), "Please add a name", Toast.LENGTH_SHORT).show();
                    }

                }
                return false;
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if ("".equals(getSharedPreferences("prefs", 0).getString("name",""))){
            Intent intent = new Intent(getApplicationContext(),AddName.class);
            startActivity(intent);
        }
    }
}
