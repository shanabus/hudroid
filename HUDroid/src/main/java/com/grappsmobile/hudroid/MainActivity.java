package com.grappsmobile.hudroid;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.ndeftools.Message;
import org.ndeftools.Record;
import org.ndeftools.externaltype.AndroidApplicationRecord;
import org.ndeftools.wellknown.TextRecord;

import java.util.List;
import java.util.Locale;

import static android.nfc.NfcAdapter.getDefaultAdapter;

public class MainActivity extends Activity implements LocationListener, TextToSpeech.OnInitListener {

    private LocationManager locationManager;
    private String provider;
    private static final String TAG = "HUDroid";// NfcActivity.class.getSimpleName();

    protected NfcAdapter nfcAdapter;
    protected PendingIntent nfcPendingIntent;

    MediaPlayer soundPlayer;
    private TextToSpeech tts;

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
        // initialize NFC
        nfcAdapter = getDefaultAdapter(this);
        nfcPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, this.getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

        // Register Android Beam callback
        //nfcAdapter.setNdefPushMessageCallback(this, this);
        // Register callback to listen for message-sent success
        //nfcAdapter.setOnNdefPushCompleteCallback(this, this);

        tts = new TextToSpeech(this, this);
    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD_MR1)
    public void enableForegroundMode() {
        Log.d(TAG, "enableForegroundMode");

        IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED); // filter for all
        IntentFilter[] writeTagFilters = new IntentFilter[] {tagDetected};
        nfcAdapter.enableForegroundDispatch(this, nfcPendingIntent, writeTagFilters, null);
    }

    public void disableForegroundMode() {
        Log.d(TAG, "disableForegroundMode");

        nfcAdapter.disableForegroundDispatch(this);
    }

    @Override
    public void onNewIntent(Intent intent) {
        Log.d(TAG, "onNewIntent");

        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {
            TextView textView = (TextView) findViewById(R.id.tvNfcMessage);

            textView.setText("Hello NFC!");

            Parcelable[] messages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            if (messages != null) {

                Log.d(TAG, "Found " + messages.length + " NDEF messages"); // is almost always just one

                //vibrate(); // signal found messages :-)
                // parse to records
                for (int i = 0; i < messages.length; i++) {
                    try {
                        List<Record> records = new Message((NdefMessage)messages[i]);

                        Log.d(TAG, "Found " + records.size() + " records in message " + i);

                        for(int k = 0; k < records.size(); k++) {
                            Log.d(TAG, " Record #" + k + " is of class " + records.get(k).getClass().getSimpleName());

                            if (records.get(k).getClass().getSimpleName().equals("UriRecord"))
                            {
                                textView.append(" - a Url");
                            }

                            Record record = records.get(k);

                            if (record instanceof TextRecord){
                                TextRecord textRecord = (TextRecord)record;
                                TextView tvSpeed = (TextView)findViewById(R.id.tvSpeed);

                                String text = textRecord.getText();
                                String[] parts = text.split(",");

                                String vehicle = parts[0]; // who's vehicle?
                                String welcomeMessage = parts[1]; // string to speak
                                String layoutOptions = parts[2]; // 0,1,2 for layout set

                                Log.d(TAG, "Text Record is " + text);

                                textView.setText(vehicle);
                                //playSoundAndSetText(R.raw.kitten);
                                if (layoutOptions.equals("1"))
                                {
                                    tvSpeed.setTextAppearance(getApplicationContext(), R.style.HUD_1);
                                }
                                else if (layoutOptions.equals("2"))
                                {
                                    tvSpeed.setTextAppearance(getApplicationContext(), R.style.HUD_2);
                                }
                                tts.speak(welcomeMessage, TextToSpeech.QUEUE_ADD, null);
                                //Toast.makeText(getApplicationContext(), layoutOptions.toString(), Toast.LENGTH_LONG).show();
                            }

                            if(record instanceof AndroidApplicationRecord) {
                                AndroidApplicationRecord aar = (AndroidApplicationRecord)record;
                                Log.d(TAG, "Package is " + aar.getPackageName());
                            }

                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Problem parsing message", e);
                    }

                }
            }
        } else {
            // ignore
        }
    }

    protected void playSoundAndSetText(Integer soundResourceId) //int position, String[] itemNames, Integer[] soundIds) {
    {
        try
        {
            if (soundPlayer != null)
            {
                soundPlayer.stop();
                soundPlayer.release();
            }

            soundPlayer = MediaPlayer.create(getApplicationContext(), soundResourceId);

            soundPlayer.start();
        }
        catch(Exception e)
        {
            Log.d(TAG, e.getLocalizedMessage());
        }

//        mHandler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                TextView actionLabel = (TextView)findViewById(R.id.actionLabel);
//                actionLabel.setText("");
//                actionLabel.setVisibility(View.GONE);
//            }
//        }, 5000);
    }

    @Override
    public void onInit(int status) {

        if (status == TextToSpeech.SUCCESS) {

            int result = tts.setLanguage(Locale.US);

            if (result == TextToSpeech.LANG_MISSING_DATA
                    || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS", "This Language is not supported");
            } else {
                //btnSpeak.setEnabled(true);
                //speakOut();
            }

        } else {
            Log.e("TTS", "Initilization Failed!");
        }

    }

    /* Request updates at startup */
    @Override
    protected void onResume() {
        super.onResume();
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, this);
        enableForegroundMode();
    }

    /* Remove the locationlistener updates when Activity is paused */
    @Override
    protected void onPause() {
        super.onPause();
        locationManager.removeUpdates(this);
        disableForegroundMode();
    }

    @Override
    public void onDestroy() {
        // Don't forget to shutdown tts!
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
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
