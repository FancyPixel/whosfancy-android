package it.fancypixel.whosfancy;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.estimote.sdk.Beacon;
import com.estimote.sdk.BeaconManager;
import com.estimote.sdk.Region;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class MyActivity extends Activity {

    private static final String TAG = MyActivity.class.getSimpleName();

    public static final String EXTRA_STATUS_TEXT = "EXTRA_STATUS_TEXT";

    private static final int REQUEST_ENABLE_BT = 1234;

    private BeaconManager mBeaconManager;
    private String mStatusText;
    private Region mRegion;

    private SharedPreferences mPreferences;

    private EditText mEmail;
    private EditText mPassword;
    private Switch mSwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);

        mPreferences = getApplicationContext().getSharedPreferences("preferences", Activity.MODE_PRIVATE);

        mRegion = new Region(Globals.WHOS_FANCY_REGION+"_", Globals.PROXIMITY_UUID, Globals.MAJOR, Globals.MINOR);

        // Configure verbose debug logging, enable this to debugging
        // L.enableDebugLogging(true);

        // Configure mBeaconManager.
        mBeaconManager = new BeaconManager(this);

        // Default values are 5s of scanning and 25s of waiting time to save CPU cycles.
        // In order for this demo to be more responsive and immediate we lower down those values.
        mBeaconManager.setBackgroundScanPeriod(TimeUnit.SECONDS.toMillis(1), 0);

        mBeaconManager.setMonitoringListener(new BeaconManager.MonitoringListener() {
            @Override
            public void onEnteredRegion(final Region region, List<Beacon> beacons) {
                mStatusText = getString(R.string.status_entered_region);
                ((TextView)findViewById(R.id.status)).setText(R.string.status_entered_region);
            }

            @Override
            public void onExitedRegion(final Region region) {
                mStatusText = getString(R.string.status_entered_region);
                ((TextView)findViewById(R.id.status)).setText(R.string.status_exited_region);
            }
        });

        if(getIntent().hasExtra(EXTRA_STATUS_TEXT) && getIntent().getStringExtra(EXTRA_STATUS_TEXT) != null && getIntent().getStringExtra(EXTRA_STATUS_TEXT).length() > 0) {
            mStatusText = getIntent().getStringExtra(EXTRA_STATUS_TEXT);
            ((TextView)findViewById(R.id.status)).setText(mStatusText);
        }

        mEmail = ((EditText)findViewById(R.id.email));
        mPassword = ((EditText)findViewById(R.id.password));
        mSwitch = ((Switch)findViewById(R.id.service_switch));

        mEmail.setText(mPreferences.getString(Globals.PREFERENCE_EMAIL, ""));
        mPassword.setText(mPreferences.getString(Globals.PREFERENCE_PASSWORD, ""));

        mSwitch.setChecked(mPreferences.getBoolean(Globals.PREFERENCE_SERVICE_STARTED, false));
        mSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) startBeaconService();
                else stopBeaconService();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Check if device supports Bluetooth Low Energy.
        if (!mBeaconManager.hasBluetooth()) {
            Toast.makeText(this, getString(R.string.device_no_ble), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        mBeaconManager.connect(new BeaconManager.ServiceReadyCallback() {
            @Override
            public void onServiceReady() {
                try {
                    mBeaconManager.startMonitoring(mRegion);
                } catch (RemoteException e) {
                    Log.d(TAG, "Error while starting monitoring");
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        mBeaconManager.disconnect();
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT) {
            if(resultCode == Activity.RESULT_OK) {
                startBeaconService();
                mSwitch.setChecked(true);
            } else {
                Toast.makeText(this, getString(R.string.bluetooth_not_enabled), Toast.LENGTH_LONG).show();
                if(getActionBar() != null) getActionBar().setSubtitle(getString(R.string.bluetooth_not_enabled));
                mSwitch.setChecked(false);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private boolean checkBluetooth() {
        // Check if device supports Bluetooth Low Energy.
        if (!mBeaconManager.hasBluetooth()) {
            Toast.makeText(this, getString(R.string.device_no_ble), Toast.LENGTH_LONG).show();
            return false;
        }

        // If Bluetooth is not enabled, let user enable it.
        if (!mBeaconManager.isBluetoothEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            return true;
        }
        return false;
    }

    private boolean checkCredentials() {
        boolean res = true;
        if(mEmail.getText().length() == 0) {
            mEmail.setError(getString(R.string.error_email));
            res = false;
        }
        if(mPassword.getText().length() == 0) {
            mPassword.setError(getString(R.string.error_password));
            res = false;
        }

        return res;
    }

    private void startBeaconService() {
        if (checkCredentials() && checkBluetooth()) {
            startService(new Intent(MyActivity.this, MyService.class));
            SharedPreferences.Editor editor = mPreferences.edit();
            editor.putBoolean(Globals.PREFERENCE_SERVICE_STARTED, true);
            editor.putString(Globals.PREFERENCE_EMAIL, mEmail.getText().toString());
            editor.putString(Globals.PREFERENCE_PASSWORD, mPassword.getText().toString());
            editor.commit();
            Toast.makeText(MyActivity.this, getString(R.string.whos_fancy_service_started), Toast.LENGTH_SHORT).show();
        }
        else mSwitch.setChecked(false);
    }

    private void stopBeaconService() {
        stopService(new Intent(MyActivity.this, MyService.class));
        SharedPreferences.Editor editor = mPreferences.edit();
        editor.putBoolean(Globals.PREFERENCE_SERVICE_STARTED, false);
        editor.commit();
        Toast.makeText(MyActivity.this, getString(R.string.whos_fancy_service_stopped), Toast.LENGTH_SHORT).show();
    }
}
