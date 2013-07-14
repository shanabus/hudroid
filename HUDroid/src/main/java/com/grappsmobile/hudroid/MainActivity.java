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

        Button btnStart = (Button) findViewById(R.id.btnStart);
        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(getApplicationContext(), "Attempting to bind Location Listener", Toast.LENGTH_LONG).show();

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

    @Override
    public void onLocationChanged(Location location) {
        TextView tvSpeed = (TextView)findViewById(R.id.tvSpeed);
        TextView tvLog = (TextView)findViewById(R.id.tvLog);
        TextView tvLogMax = (TextView)findViewById(R.id.tvLogMax);
        TextView tvTimesUpdated = (TextView)findViewById(R.id.tvTimesUpdated);

        float speed;

        timesUpdated = new Integer(timesUpdated + 1);

        try{
            speed = location.getSpeed();
            float mph = (float) (speed * 2.23694);
            int m = (int) mph;
            tvSpeed.setText(Integer.toString(m));

            tvLog.setText(Float.toString(speed));
            tvTimesUpdated.setText(Integer.toString(timesUpdated));

            if (speed > maxSpeed){
                maxSpeed = speed;
                tvLogMax.setText(Float.toString(maxSpeed));
            }
        }
        catch (Exception e) {
            Toast.makeText(getApplicationContext(), "Could not get speed", Toast.LENGTH_SHORT).show();
        }

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
