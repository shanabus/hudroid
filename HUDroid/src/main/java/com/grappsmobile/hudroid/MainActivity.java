package com.grappsmobile.hudroid;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.app.Activity;
import android.provider.Settings;
import android.view.Menu;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements LocationListener {

    private LocationManager locationManager;
    private String provider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        boolean enabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

        // Check if enabled and if not send user to the GSP settings
        // Better solution would be to display a dialog and suggesting to
        // go to the settings
        if (!enabled) {
            Toast.makeText(getApplicationContext(), "Your GPS is not on", Toast.LENGTH_LONG).show();
//            AlertDialog.Builder builder = new AlertDialog.Builder(getApplicationContext());
//            builder.setTitle(R.string.gps_settings_title)
//                    .setMessage(R.string.gps_settings_message)
//                    .setCancelable(false)
//                    .setPositiveButton(R.string.gps_settings_confirm, new DialogInterface.OnClickListener() {
//                        public void onClick(DialogInterface dialog, int id) {
//                            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
//                            startActivity(intent);
//                        }
//                    })
//                    .setNegativeButton(R.string.gps_settings_cancel, new DialogInterface.OnClickListener() {
//                        public void onClick(DialogInterface dialog, int id) {
//                            dialog.cancel();
//                        }
//                    });
//            AlertDialog alert = builder.create();
//            alert.show();
        }

        final LocationListener thisLL = this;
        final TextView tvSpeed = (TextView)findViewById(R.id.tvSpeed);
        final TextView tvMaxSpeed = (TextView)findViewById(R.id.tvMaxSpeed);
        final TextView tvDistanceTravelled = (TextView)findViewById(R.id.tvDistanceTravelled);

        Button btnStart = (Button) findViewById(R.id.btnStart);
        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(getApplicationContext(), "Attempting to bind Location Listener", Toast.LENGTH_SHORT).show();

                tvSpeed.setText("0.0");
                tvMaxSpeed.setText("--");
                tvDistanceTravelled.setText("--");

                Criteria criteria = new Criteria();
                provider = locationManager.getBestProvider(criteria, false);
                Location location = locationManager.getLastKnownLocation(provider);

                // Initialize the location fields
                if (location != null) {
                    System.out.println("Provider " + provider + " has been selected.");
                    onLocationChanged(location);
                } else {
                    TextView tvSpeed = (TextView)findViewById(R.id.tvSpeed);
                    tvSpeed.setText((int) location.getSpeed());
                }

                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, thisLL);
            }
        });

        Button btnStop = (Button) findViewById(R.id.btnStop);
        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(getApplicationContext(), "Attempting to stop Location Listener", Toast.LENGTH_LONG).show();
                locationManager.removeUpdates(thisLL);
            }
        });
    }

    /* Request updates at startup */
    @Override
    protected void onResume() {
        super.onResume();
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, this);
    }

    /* Remove the locationlistener updates when Activity is paused */
    @Override
    protected void onPause() {
        super.onPause();
        locationManager.removeUpdates(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    float maxSpeed = 0;
    int timesUpdated = 0;
    Location lastLoc;
    float speed;
    float ttf;
    double totalDistance;

    @Override
    public void onLocationChanged(Location location) {
        TextView tvSpeed = (TextView)findViewById(R.id.tvSpeed);
        TextView tvMaxSpeed = (TextView)findViewById(R.id.tvMaxSpeed);
        TextView tvDistanceTravelled = (TextView)findViewById(R.id.tvDistanceTravelled);

        timesUpdated = new Integer(timesUpdated + 1);

        // attempts to update speed/max speed views
        try{
            speed = location.getSpeed();
            float mph = (float) (speed * 2.23694);
            int m = (int) mph;
            tvSpeed.setText(Integer.toString(m));

            if (speed > maxSpeed){
                maxSpeed = speed;
                tvMaxSpeed.setText(Integer.toString(m));
            }
        }
        catch (Exception e) {
            Toast.makeText(getApplicationContext(), "Could not get speed", Toast.LENGTH_SHORT).show();
        }

        // attempts to update distance travelled
        // http://stackoverflow.com/questions/8132198/how-to-calculate-distance-travelled
        if(lastLoc != null)
        {
            ttf = (location.getTime() - lastLoc.getTime()) / 1000;
            int R = 6371;
            double lat1 = Math.PI / 180.0 *lastLoc.getLatitude();
            double lon1 = Math.PI / 180.0 *lastLoc.getLongitude();
            double lat2 = Math.PI / 180.0 *location.getLatitude();
            double lon2 = Math.PI / 180.0 *location.getLongitude();
            //  double dLon = Math.PI / 180.0 * (location.getLongitude() - lastLoc.getLongitude());
            double dLat = (lat2-lat1);
            double dLon = (lon2-lon1);
            double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                    Math.cos(lat1) * Math.cos(lat2) *
                            Math.sin(dLon/2) * Math.sin(dLon/2);
            double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
            double d = R * c;
            totalDistance = d;

            tvDistanceTravelled.setText(Double.toString(totalDistance));
        }

        lastLoc = location;

    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {
        Toast.makeText(this, "Disabled provider " + provider, Toast.LENGTH_SHORT).show();
    }
}
