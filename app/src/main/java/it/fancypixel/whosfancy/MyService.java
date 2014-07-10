package it.fancypixel.whosfancy;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.util.Base64;
import android.util.Log;

import com.estimote.sdk.Beacon;
import com.estimote.sdk.BeaconManager;
import com.estimote.sdk.Region;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MyService extends Service {
    private static final String TAG = MyService.class.getSimpleName();

    private static final Integer WHOS_FANCY_SERVICE_NOTIFICATION_ID = 100;
    private static final int WHOS_FANCY_NOTIFICATION_ID = WHOS_FANCY_SERVICE_NOTIFICATION_ID + 1;

    private BeaconManager mBeaconManager;
    private NotificationManager mNotificationManager;
    private Region mRegion;
    private SharedPreferences mPreferences;

    public MyService() {
    }

    @Override
    public void onCreate() {
        // Configure verbose debug logging, enable this to debugging
        // L.enableDebugLogging(true);

        mRegion = new Region(Globals.WHOS_FANCY_REGION, Globals.PROXIMITY_UUID, Globals.MAJOR, Globals.MINOR);

        mPreferences = getApplicationContext().getSharedPreferences("preferences", Activity.MODE_PRIVATE);

        // User this to receive notification from all iBeacons
        //mRegion = new Region(Globals.WHOS_FANCY_REGION, PROXIMITY_UUID, null, null);

        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mBeaconManager = new BeaconManager(this);

        // Default values are 5s of scanning and 25s of waiting time to save CPU cycles.
        // In order for this demo to be more responsive and immediate we lower down those values.
        mBeaconManager.setBackgroundScanPeriod(TimeUnit.SECONDS.toMillis(1), 0);

        mBeaconManager.setMonitoringListener(new BeaconManager.MonitoringListener() {
            @Override
            public void onEnteredRegion(final Region region, List<Beacon> beacons) {
                postNotification(getString(R.string.status_entered_region));
                new Thread(new Runnable() {
                    public void run() {
                        sendData(Globals.URL_PREFIX_CHECKIN);
                    }
                }).start();
            }

            @Override
            public void onExitedRegion(final Region region) {
                postNotification(getString(R.string.status_exited_region));
                new Thread(new Runnable() {
                    public void run() {
                        sendData(Globals.URL_PREFIX_CHECKOUT);
                    }
                }).start();
            }
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        setNotification();

        mNotificationManager.cancel(WHOS_FANCY_NOTIFICATION_ID);
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

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mNotificationManager.cancel(WHOS_FANCY_NOTIFICATION_ID);
        mBeaconManager.disconnect();
        SharedPreferences.Editor editor = mPreferences.edit();
        editor.putBoolean(Globals.PREFERENCE_SERVICE_STARTED, false);
        editor.commit();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void postNotification(String msg) {
        Intent notifyIntent = new Intent(MyService.this, MyActivity.class);
        notifyIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        notifyIntent.putExtra(MyActivity.EXTRA_STATUS_TEXT, msg);

        PendingIntent pendingIntent = PendingIntent.getActivities(
                MyService.this,
                0,
                new Intent[]{notifyIntent},
                PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder notification = new NotificationCompat.Builder(MyService.this)
                .setSmallIcon(R.drawable.beacon_gray)
                .setContentTitle(getString(R.string.last_post_notification))
                .setContentText(msg)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setDefaults(Notification.DEFAULT_ALL);

        mNotificationManager.notify(WHOS_FANCY_NOTIFICATION_ID, notification.build());
    }

    private void setNotification() {
        Intent notificationIntent = new Intent(this, MyActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        NotificationCompat.Builder notification = new NotificationCompat.Builder(MyService.this)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.waiting_notification))
                .setContentIntent(pendingIntent);

        startForeground(WHOS_FANCY_SERVICE_NOTIFICATION_ID, notification.build());
    }

    private HttpClient createHttpclient() {
        HttpParams httpParameters = new BasicHttpParams();

        HttpConnectionParams.setConnectionTimeout(httpParameters, 3000);

        HttpConnectionParams.setSoTimeout(httpParameters, 5000);

        return new DefaultHttpClient(httpParameters);
    }

    private void sendData(String url) {
        String result;

        HttpClient client = createHttpclient();
        HttpContext localContext = new BasicHttpContext();
        HttpPost httpPost = new HttpPost(Uri.parse(url).buildUpon().toString());

        String credentials = mPreferences.getString(Globals.PREFERENCE_EMAIL, "") + ":" +
                             mPreferences.getString(Globals.PREFERENCE_PASSWORD, "");
        String base64EncodedCredentials = Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);
        httpPost.addHeader("Authorization", "Basic " + base64EncodedCredentials);

        try {

            result = parseResponse(client.execute(httpPost, localContext));

            Log.d(TAG, "response: " + result);
        }
        catch (Exception e) {
            String msg = DateFormat.getDateTimeInstance().format(new Date()) + ": " + e.getMessage();
            Log.d(TAG, msg);
            e.printStackTrace();
        }
    }

    private String parseResponse(HttpResponse response) throws Exception {

        String res = "";

        StringBuilder builder = new StringBuilder();
        StatusLine statusLine = response.getStatusLine();
        int statusCode = statusLine.getStatusCode();

        HttpEntity entity = response.getEntity();
        if(entity != null) {
            InputStream content = entity.getContent();
            BufferedReader reader = new BufferedReader(new InputStreamReader(content));
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
        }

        if(statusCode == 401) {
            // User unauthorized, show Welcome screen

            res = "[401 unauthorized] Response string: " + builder.toString();
        }
        else if (statusCode >= 200 && statusCode < 300) {
            res = "[" + statusCode + "] Response string: " + builder.toString();
        } else {
            // There was an error, notify user
            res = "Failed to make request with status code: " + statusCode + " " + "Response string: " + builder.toString();
        }

        return res;
    }
}
