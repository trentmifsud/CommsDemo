package com.honeywell.commsdemo;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Date;


public class CommsDemoActivity extends Activity implements AsyncResponse {

    // onscreen controls

    protected TextView textViewDeviceState;
    protected EditText editTextRadioInfo;
    protected PowerManager powerManager;
    protected TextView textViewHttpResult;
    protected TextView textView3gState;
    protected RequestTask asyncTask;
    protected ConnectivityManager manager;
    private ToggleButton gpsToggleButton;
    protected TelephonyManager telephonyManager;
    private LocationManager locationManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_commsdemo);
        textViewDeviceState = (TextView) findViewById(R.id.textViewDeviceState);
        textView3gState = (TextView) findViewById(R.id.textView3gState);
        gpsToggleButton = (ToggleButton) findViewById(R.id.gpsToggleButton);
        powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        textViewHttpResult = (TextView) findViewById(R.id.textViewHttpResult);
        manager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);

        locationManager = (LocationManager) this.getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        //telephonyManager.listen();

        // match GPS enable to toggle button
        gpsToggleButton.setChecked(isGpsEnabled());


        PhoneStateListener callStateListener = new PhoneStateListener() {
            public void onDataConnectionStateChanged(int state) {
                switch (state) {
                    case TelephonyManager.DATA_DISCONNECTED:
                        Log.i("State: ", "Offline");
                        // String stateString = "Offline";
                        // Toast.makeText(getApplicationContext(),
                        // stateString, Toast.LENGTH_LONG).show();
                        textView3gState.setText(new Date().toString() + " Offline");
                        break;
                    case TelephonyManager.DATA_SUSPENDED:
                        Log.i("State: ", "IDLE");
                        // stateString = "Idle";
                        // Toast.makeText(getApplicationContext(),
                        // stateString, Toast.LENGTH_LONG).show();
                        textView3gState.setText(new Date().toString() + " IDLE");
                        break;
                    case TelephonyManager.DATA_CONNECTED:
                        textView3gState.setText(new Date().toString() + " CONNECTED");
                        break;
                    case TelephonyManager.DATA_CONNECTING:
                        textView3gState.setText(new Date().toString() + " CONNECTING");
                        break;
                }
            }
        };

        telephonyManager.listen(callStateListener, PhoneStateListener.LISTEN_DATA_CONNECTION_STATE);


        // If flight mode gets turned on - turn off GPS. If Flight mode goes back off - GPS goes back on

        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                Log.d(TAG, "Airplane mode state is : " + isAirplaneModeOn());

                if (isAirplaneModeOn()) {
                    // turn off GPS
                    turnGPSOff();

                    //update toggle button
                    gpsToggleButton.setChecked(false);
                } else {
                    turnGPSOn();
                    gpsToggleButton.setChecked(true);
                }
            }
        };

        this.getApplicationContext().registerReceiver(receiver, intentFilter);
        findViewById(R.id.buttonCredits).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CommsDemoActivity.this.startActivity(new Intent(CommsDemoActivity.this, CreditsActivity.class));
            }
        });
    }


    private boolean isAirplaneModeOn() {
       return Settings.Global.getInt(this.getApplicationContext().getContentResolver(),
                    Settings.Global.AIRPLANE_MODE_ON, 0) != 0;

    }

    //region GPS methods
    private boolean isGpsEnabled()
    {
        return locationManager.isProviderEnabled( LocationManager.GPS_PROVIDER );
    }

    private void turnGPSOn()
    {
        Intent intent = new Intent("android.location.GPS_ENABLED_CHANGE");
        intent.putExtra("enabled", true);

        this.getApplicationContext().sendBroadcast(intent);

        String provider = Settings.Secure.getString(this.getApplicationContext().getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
        if(!provider.contains("gps"))
        {
            //if gps is disabled
            final Intent poke = new Intent();
            poke.setClassName("com.android.settings", "com.android.settings.widget.SettingsAppWidgetProvider");
            poke.addCategory(Intent.CATEGORY_ALTERNATIVE);
            poke.setData(Uri.parse("3"));
            this.getApplicationContext().sendBroadcast(poke);
        }
    }

    private void turnGPSOff()
    {
        String provider = Settings.Secure.getString(this.getApplicationContext().getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
        if(provider.contains("gps")){ //if gps is enabled
            final Intent poke = new Intent();
            poke.setClassName("com.android.settings", "com.android.settings.widget.SettingsAppWidgetProvider");
            poke.addCategory(Intent.CATEGORY_ALTERNATIVE);
            poke.setData(Uri.parse("3"));
            this.getApplicationContext().sendBroadcast(poke);
        }
    }
//endregion

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.comms_demo, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    private boolean Is3gOn() {
        return manager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE)
                    .isConnectedOrConnecting();
    }


    public void PromptUserForRadio(View view)
    {
        Intent intent=new Intent(Settings.ACTION_DATA_ROAMING_SETTINGS);
        ComponentName cn = new ComponentName("com.android.phone","com.android.phone.MobileNetworkSettings");
        intent.setComponent(cn);
        startActivity(intent);
    }

    private static final String TAG = "CommsDemoActivity";
    private Boolean VERBOSE = true;


    //region Activity level methods
    @Override
    public void onStart() {
        super.onStart();
        if (VERBOSE) Log.v(TAG, "++ ON START ++");

        SetTextViewDeviceState("Application Start");
    }

    @Override
    public void onResume() {
        super.onResume();
        if (VERBOSE) Log.v(TAG, "+ ON RESUME +");

        SetTextViewDeviceState("Application Resumed");
    }

    @Override
    public void onPause() {
        super.onPause();
        if (VERBOSE) Log.v(TAG, "- ON PAUSE -");
        SetTextViewDeviceState("Application Paused");
    }

    @Override
    public void onStop() {
        super.onStop();
        if (VERBOSE) Log.v(TAG, "-- ON STOP --");

        SetTextViewDeviceState("Application Stopped");

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (VERBOSE) Log.v(TAG, "- ON DESTROY -");

        SetTextViewDeviceState("Application Destroyed");
    }
    //endregion

    private void SetTextViewDeviceState(String text)
    {
        textViewDeviceState.setText(text + " " +  new Date().toString());
    }


    protected Boolean runningTask = false;
    public void DoSimpleHttpRequest(View view)
    {
        boolean is3g = Is3gOn();
        if(is3g) {
            textViewHttpResult.setText("Starting Request");
            if (!runningTask) {
                asyncTask = new RequestTask();
                asyncTask.delegate = this;
                runningTask = true;
                asyncTask.execute("http://www.google.com.au");
            }
            else
            {
                textViewHttpResult.setText("Other task is still running");
            }
        }
        else
        {
            textViewHttpResult.setText("3g is currently not available. Will try and turn on radio");
            try {
                setMobileDataEnabled(getApplicationContext(), true);
                textViewHttpResult.setText("3g Radio should be on");
            }
            catch(Exception ex)
            {
                String stateString = ex.getMessage();
                Toast.makeText(getApplicationContext(),
                        stateString, Toast.LENGTH_LONG).show();
                textViewHttpResult.setText("Error turning on radio");
                Log.e(TAG, ex.toString());
            }
        }
    }

    public void processFinish(String output){
        runningTask = false;
        textViewHttpResult.setText(new Date().toString() + " " + output.substring(0,30));
    }

    private void setMobileDataEnabled(Context context, boolean enabled) throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        final ConnectivityManager conman = (ConnectivityManager)  context.getSystemService(Context.CONNECTIVITY_SERVICE);
        final Class conmanClass = Class.forName(conman.getClass().getName());
        final Field connectivityManagerField = conmanClass.getDeclaredField("mService");
        connectivityManagerField.setAccessible(true);
        final Object connectivityManager = connectivityManagerField.get(conman);
        final Class connectivityManagerClass =  Class.forName(connectivityManager.getClass().getName());
        final Method setMobileDataEnabledMethod = connectivityManagerClass.getDeclaredMethod("setMobileDataEnabled", Boolean.TYPE);
        setMobileDataEnabledMethod.setAccessible(true);

        setMobileDataEnabledMethod.invoke(connectivityManager, enabled);
    }

    public void GPSToggleButtonClick(View view)
    {
        if(this.gpsToggleButton.isChecked())
        {
            Log.i(TAG, "Turning on GPS" );
            turnGPSOn();
        }
        else
        {
            Log.i(TAG, "Turning off GPS" );
            turnGPSOff();
        }

        Log.i(TAG, "GPS State is " + isGpsEnabled());

    }
}
