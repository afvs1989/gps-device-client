package com.afvsystems.myapplication.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.IBinder;
import android.widget.Toast;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.json.JSONArray;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

public class SendLocationService extends Service {
    MyTask myTask;
    public static String[] arguments;

    @Override
    public void onCreate() {
        super.onCreate();
        Toast.makeText(this, "Servicio creado!", Toast.LENGTH_SHORT).show();
        myTask = new MyTask();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (isConnectingToInternet(getApplicationContext())) {
            try {
                myTask.execute(new String[]{
                        "http://134.209.58.218:3000/api/gps-device-management/coordinates",
                        "afvs-nativo",
                        "afvs-moto"
                });
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return START_STICKY;
    }

    private static boolean isConnectingToInternet(Context _context) {
        ConnectivityManager connectivity = (ConnectivityManager) _context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivity != null) {
            NetworkInfo[] info = connectivity.getAllNetworkInfo();
            if (info != null)
                for (int i = 0; i < info.length; i++)
                    if (info[i].getState() == NetworkInfo.State.CONNECTED) {
                        return true;
                    }

        }
        return false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Toast.makeText(this, "Servicio destruído!", Toast.LENGTH_SHORT).show();
        //myTask.cancel(true);
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private class MyTask extends AsyncTask<String, String, String> {

        private DateFormat dateFormat;
        private String date;
        private boolean cent;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            dateFormat = new SimpleDateFormat("HH:mm:ss");
            cent = true;
        }

        private void sendCoordinates(double longitude, double latitude) {
            HttpClient httpclient;
            HttpPost httppost = new HttpPost(SendLocationService.arguments[0]);
            HttpParams httpParameters = new BasicHttpParams();
            httpclient = new DefaultHttpClient(httpParameters);
            JSONObject obj = new JSONObject();
            try {
                ArrayList<Double> coordinates = new ArrayList<>(Arrays.asList(longitude, latitude));
                JSONArray jsonArray = new JSONArray(coordinates);
                obj.put("userKey", SendLocationService.arguments[1]);
                obj.put("deviceKey", SendLocationService.arguments[2]);
                obj.put("location", jsonArray);
                StringEntity params = new StringEntity(obj.toString());
                httppost.addHeader("content-type", "application/json");
                httppost.setEntity(params);
                httpclient.execute(httppost);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        protected String doInBackground(String... arg0) {
            SendLocationService.arguments = arg0;
            while (cent) {
                try {
                    //create an instance singleton
                    GPSTracker tracker = GPSTracker.getInstance();
                    tracker.canGetLocation();
                    Location location = tracker.getLocation(getApplicationContext());
                    this.sendCoordinates(location.getLongitude(), location.getLatitude());
                    date = dateFormat.format(new Date());
                    publishProgress(date);
                    // Stop 5s
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            Toast.makeText(getApplicationContext(), "Hora actual: " + values[0], Toast.LENGTH_SHORT).show();
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            cent = false;
        }
    }
}
