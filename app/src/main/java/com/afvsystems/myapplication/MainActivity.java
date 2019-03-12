package com.afvsystems.myapplication;

import android.Manifest;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.afvsystems.myapplication.services.SendLocationService;

public class MainActivity extends AppCompatActivity {
    private static SharedPreferences _sharedPreferences;
    private static SharedPreferences.Editor _editor;
    private static String _userKey;
    private static String _deviceKey;
    private static String _frequencyTime;
    private static EditText _textUserKey;
    private static EditText _textDeviceKey;
    private static EditText _textFrequencyTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.initData();
    }

    private void initData() {
        setContentView(R.layout.activity_main);
        findViewById(R.id.buttonOn).setOnClickListener(mClickListener);
        findViewById(R.id.buttonOff).setOnClickListener(mClickListener);
        MainActivity._sharedPreferences = getApplicationContext().getSharedPreferences("MyPref", 0); // 0 - for private mode
        MainActivity._editor = this._sharedPreferences.edit();
        MainActivity._textUserKey = findViewById(R.id.textUserKey);
        MainActivity._textDeviceKey = findViewById(R.id.textDeviceKey);
        MainActivity._textFrequencyTime = findViewById(R.id.textFrequencyTime);
        String userKey = MainActivity._sharedPreferences.getString("userKey", null);
        String deviceKey = MainActivity._sharedPreferences.getString("deviceKey", null);
        String frequencyTimeKey = MainActivity._sharedPreferences.getString("frequencyTimeKey", null);
        if (userKey != null && !userKey.equals("")) {
            MainActivity._textUserKey.setText(userKey);
        }
        if (deviceKey != null && !deviceKey.equals("")) {
            MainActivity._textDeviceKey.setText(deviceKey);
        }
        if (frequencyTimeKey != null && !frequencyTimeKey.equals("")) {
            MainActivity._textFrequencyTime.setText(frequencyTimeKey);
        } else {
            MainActivity._textFrequencyTime.setText("30");
        }
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    View.OnClickListener mClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.buttonOn:
                    MainActivity._userKey = MainActivity._textUserKey.getText().toString();
                    MainActivity._deviceKey = MainActivity._textDeviceKey.getText().toString();
                    MainActivity._frequencyTime = MainActivity._textFrequencyTime.getText().toString();
                    MainActivity._editor.putString("userKey", MainActivity._userKey);
                    MainActivity._editor.putString("deviceKey", MainActivity._deviceKey);
                    MainActivity._editor.putString("frequencyTimeKey", MainActivity._frequencyTime);
                    MainActivity._editor.commit();
                    // Start Service
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 123);
                    SendLocationService.userKey = MainActivity._userKey;
                    SendLocationService.deviceKey = MainActivity._deviceKey;
                    SendLocationService.frequencyTimeKey = Integer.parseInt(MainActivity._frequencyTime);
                    //startService(new Intent(MainActivity.this, SendLocationService.class));
                    if (isMyServiceRunning(SendLocationService.class)) return;
                    Intent startIntent = new Intent(MainActivity.this, SendLocationService.class);
                    startIntent.setAction("start");
                    startService(startIntent);
                    break;
                case R.id.buttonOff:
                    // Stop Service
                    stopService(new Intent(MainActivity.this, SendLocationService.class));
                    SendLocationService.cancelTask();
                    Toast.makeText(getApplicationContext(), "Envio de Coordenadas Detenido!",
                            Toast.LENGTH_LONG).show();
                    break;
            }
        }
    };
}
