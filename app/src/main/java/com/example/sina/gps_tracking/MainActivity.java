package com.example.sina.gps_tracking;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import org.json.JSONObject;
import ua.naiksoftware.stomp.Stomp;
import ua.naiksoftware.stomp.client.StompClient;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int PERMISSION_REQUEST = 100;

    private GoogleMap gMap;

    private Location currentLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Verif que le tracking GPS est activé
        LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (lm == null || !lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            finish();
        }

        // Verif de la permission
        int permission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);

        // Si permission accordée, démarrage du TrackerService
        if (permission == PackageManager.PERMISSION_GRANTED) {
            currentLocation = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            new LocationOperation().execute("");
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSION_REQUEST);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST && grantResults.length == 1
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            new LocationOperation().execute("");
        } else {
            Toast.makeText(this, "GPS tracking enabled", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        gMap = googleMap;
        LatLng here;
        here = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
        gMap.addMarker(new MarkerOptions().position(here).title("I am here"));
        gMap.moveCamera(CameraUpdateFactory.newLatLng(here));
        new LocationOperation().execute("");
    }

    private class LocationOperation extends AsyncTask<String, Void, String> {

        private final String TAG = "LocationOperation";

        private StompClient mStompClient;

        @Override
        protected String doInBackground(String... params) {

            mStompClient = Stomp.over(Stomp.ConnectionProvider.OKHTTP, "ws://gps-locator-esipe.herokuapp.com/locations/websocket");

            mStompClient.topic("/topic/greetings")
                    .subscribe(topicMessage -> {
                        Log.d(TAG, topicMessage.getPayload());
                        JSONObject reader = new JSONObject(topicMessage.getPayload());
                        String id = reader.getString("id");
                        double latitude = Double.parseDouble(reader.getString("latitude"));
                        double longitude = Double.parseDouble(reader.getString("longitude"));
                        LatLng location = new LatLng(latitude, longitude);
                        gMap.addMarker(new MarkerOptions().position(location).title(id));
                        gMap.moveCamera(CameraUpdateFactory.newLatLng(location));
                    });

            mStompClient.send("/app/locations", "Get locations")
                    .subscribe();

            mStompClient.lifecycle()
                    .subscribe(lifecycleEvent -> {
                switch (lifecycleEvent.getType()) {
                    case OPENED:
                        Log.d(TAG, "Stomp connection opened");
                        break;
                    case ERROR:
                        Log.e(TAG, "Error", lifecycleEvent.getException());
                        break;
                    case CLOSED:
                        Log.d(TAG, "Stomp connection closed");
                        break;
                }
            });

            mStompClient.connect();

            return "Executed";
        }

        @Override
        protected void onPostExecute(String result) {
        }

    }

}
