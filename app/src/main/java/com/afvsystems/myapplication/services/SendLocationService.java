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
    public static MyTask myTask;
    public static String[] arguments;

    @Override
    public void onCreate() {
        super.onCreate();
        Toast.makeText(this, "Envio de coordenadas iniciado!", Toast.LENGTH_SHORT).show();
        myTask = new MyTask();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (isConnectingToInternet(getApplicationContext())) {
            try {
                SendLocationService.myTask.execute(new String[]{
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
    }

    public static void cancelTask() {
        SendLocationService.myTask.cancel(true);
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private class MyTask extends AsyncTask<String, String, String> {

        private DateFormat _dateFormat;
        private String _date;
        private boolean _cent;
        private GPSTracker _tracker;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            this._dateFormat = new SimpleDateFormat("HH:mm:ss");
            this._cent = true;
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
            //create an instance singleton
            this._tracker = GPSTracker.getInstance();
            while (this._cent) {
                try {
                    this._tracker.canGetLocation();
                    Location location = this._tracker.getLocation(getApplicationContext());
                    this._date = this._dateFormat.format(new Date());
                    double longitude = location.getLongitude();
                    double latitude = location.getLatitude();
                    this.sendCoordinates(longitude, latitude);
                    publishProgress(this._date);
                    // Stop 10s
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    this._tracker.stopUsingGPS();
                    this.onCancelled();
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
            this._cent = false;
        }
    }
}
