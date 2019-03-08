package com.afvsystems.myapplication;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import com.afvsystems.myapplication.services.SendLocationService;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.buttonOn).setOnClickListener(mClickListener);
        findViewById(R.id.buttonOff).setOnClickListener(mClickListener);
    }

    View.OnClickListener mClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.buttonOn:
                    // Start Service
                    ActivityCompat.requestPermissions(MainActivity.this, new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, 123);
                    startService(new Intent(MainActivity.this, SendLocationService.class));
                    break;
                case R.id.buttonOff:
                    // Stop Service
                    stopService(new Intent(MainActivity.this, SendLocationService.class));
                    break;
            }
        }
    };
}
