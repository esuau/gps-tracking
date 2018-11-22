package com.example.sina.gps_tracking;

import android.app.*;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.location.Location;
import android.net.http.AndroidHttpClient;
import android.os.AsyncTask;
import android.os.IBinder;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

public class TrackingService extends Service {
    public TrackingService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        buildNotification();
    }

    private void buildNotification() {
        String STOP = "stop";

        registerReceiver(stopReceiver, new IntentFilter(STOP));

        PendingIntent broadcastIntent = PendingIntent.getBroadcast(
                this, 0, new Intent(STOP), PendingIntent.FLAG_UPDATE_CURRENT
        );

        Notification.Builder builder;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            String NOTIFICATION_CHANNEL_ID = "com.example.sina.gps_tracking";
            String channelName = "My Background Service";
            NotificationChannel chan = null;
            chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_NONE);
            chan.setLightColor(Color.BLUE);
            chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            assert manager != null;
            manager.createNotificationChannel(chan);

            builder = new Notification.Builder(this, NOTIFICATION_CHANNEL_ID)
                    .setContentTitle(getString(R.string.app_name))
                    .setContentText(getString(R.string.tracking_enabled_notif))
                    .setOngoing(true)
                    .setContentIntent(broadcastIntent)
                    .setSmallIcon(R.drawable.tracking_enabled);

        } else {
            builder = new Notification.Builder(this)
                    .setContentTitle(getString(R.string.app_name))
                    .setContentText(getString(R.string.tracking_enabled_notif))
                    .setOngoing(true)
                    .setContentIntent(broadcastIntent)
                    .setSmallIcon(R.drawable.tracking_enabled);
        }

        startForeground(1, builder.build());
    }

    protected BroadcastReceiver stopReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            unregisterReceiver(stopReceiver);
            stopSelf();
        }
    };

    private class HttpRequestTask extends AsyncTask<Location, String, HttpResponse> {

        @Override
        protected HttpResponse doInBackground(Location... locations) {
            HttpPost postRequest = new HttpPost("https://gps-locator-esipe.herokuapp.com/location/");
            StringEntity entity = null;
            try {
                entity = new StringEntity(locationToJson(locations[0]));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            postRequest.setEntity(entity);
            postRequest.setHeader("Content-Type", "application/json");
            AndroidHttpClient client = AndroidHttpClient.newInstance("Android");
            try {
                return client.execute(postRequest);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        private String locationToJson(Location location) {
            StringBuilder sb = new StringBuilder();
            sb.append("{");

            sb.append("\"altitude\":" + String.valueOf(location.getAltitude()) + ",");
            sb.append("\"longitude\":" + String.valueOf(location.getLongitude()) + ",");
            sb.append("\"latitude\":" + String.valueOf(location.getLatitude()));

            sb.append("}");
            return sb.toString();
        }
    }
}
