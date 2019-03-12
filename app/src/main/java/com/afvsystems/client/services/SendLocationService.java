package com.afvsystems.client.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import com.afvsystems.client.MainActivity;
import com.afvsystems.client.R;

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
    public static String userKey;
    public static String deviceKey;
    public static int frequencyTimeKey;
    private static String _url;

    static {
        SendLocationService.userKey = "default";
        SendLocationService.deviceKey = "default";
        SendLocationService.frequencyTimeKey = 30000;
        SendLocationService._url = "http://134.209.58.218:3000/api/gps-device-management/coordinates";
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Toast.makeText(this, "Envio de coordenadas iniciado!", Toast.LENGTH_SHORT).show();
        myTask = new MyTask();
    }

    private Notification updateNotification() {
        String info = "1.0V";

        Context context = getApplicationContext();

        PendingIntent action = PendingIntent.getActivity(context,
                0, new Intent(context, MainActivity.class),
                PendingIntent.FLAG_CANCEL_CURRENT); // Flag indicating that if the described PendingIntent already exists, the current one should be canceled before generating a new one.

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder builder;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {

            String CHANNEL_ID = "gps_channel";

            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "GpsChannel",
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("AFVS Cliente Gps");
            manager.createNotificationChannel(channel);

            builder = new NotificationCompat.Builder(this, CHANNEL_ID);
        } else {
            builder = new NotificationCompat.Builder(context);
        }

        return builder.setContentIntent(action)
                .setContentTitle(info)
                .setTicker(info)
                .setContentText(info)
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setContentIntent(action)
                .setOngoing(true).build();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent.getAction().contains("start")) {
            startForeground(101, updateNotification());
        } else {
            stopForeground(true);
            stopSelf();
        }
        if (isConnectingToInternet(getApplicationContext())) {
            try {
                SendLocationService.myTask.execute(new String[]{
                        SendLocationService._url,
                        SendLocationService.userKey,
                        SendLocationService.deviceKey,
                        String.valueOf(SendLocationService.frequencyTimeKey)
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
                    int stopSeconds = Integer.parseInt(SendLocationService.arguments[3]) * 1000;
                    Thread.sleep(stopSeconds);
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
