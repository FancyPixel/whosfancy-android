package it.fancypixel.whosfancy;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);

        mRegion = new Region(Globals.WHOS_FANCY_REGION, Globals.PROXIMITY_UUID, Globals.MAJOR, Globals.MINOR);

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
                if(checkBluetooth()) {
                    Intent i = new Intent(this, MyService.class);
                    startService(i);
                    Toast.makeText(this, getString(R.string.whos_fancy_service_started), Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, getString(R.string.bluetooth_not_enabled), Toast.LENGTH_LONG).show();
                if(getActionBar() != null) getActionBar().setSubtitle(getString(R.string.bluetooth_not_enabled));
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.my, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_start) {
            if(checkBluetooth()) {
                Intent i = new Intent(this, MyService.class);
                startService(i);
                Toast.makeText(this, getString(R.string.whos_fancy_service_started), Toast.LENGTH_SHORT).show();
            }
            return true;
        }
        else if (id == R.id.action_stop) {
            Intent i = new Intent(this, MyService.class);
            stopService(i);
            Toast.makeText(this, getString(R.string.whos_fancy_service_stopped), Toast.LENGTH_SHORT).show();
            return true;
        }
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
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
}
