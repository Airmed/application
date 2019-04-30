package com.ti.smartconfig;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.IBinder;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import cz.msebera.android.httpclient.Header;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.loopj.android.http.*;
import java.util.ArrayList;



public class NotificationService extends Service{
    private static final String TAG_BOOT_EXECUTE_SERVICE = "BOOT_BROADCAST_SERVICE";
    public static final String NOTIFICATION_CHANNEL_ID = "10001";
    String URL="https://seniordesigndb.herokuapp.com/";
    String timeStamp = "";
    String eventMessage = "";
    ArrayList<NotRecord>notifications = new ArrayList<>();
    int clearcnt = 0;



    public NotificationService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG_BOOT_EXECUTE_SERVICE, "NotificationService onCreate() method.");

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.d(TAG_BOOT_EXECUTE_SERVICE, "NotificationService onStartCommand() method. Pulling Notifications from DB");

        AsyncHttpClient client = new AsyncHttpClient();
        RequestParams params = new RequestParams();
        params.put("query", "select * from notifications;");
        Log.d("FETCH_NOTIFICATION_DB", "Entered attempted query: select * from notifications;");

        client.post(URL, params, new JsonHttpResponseHandler() {
            @Override
            public void onStart() {
                super.onStart();
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject res) {
                super.onSuccess(statusCode, headers, res);

                try {
                    Log.d("FETCH_NOTIFICATION_DB", "Entered try");
                    JSONArray jsonArray = res.getJSONArray("data");
                    for (int i = 0; i < jsonArray.length(); i++) {
                        NotRecord n = new NotRecord();
                        n.timestamp = jsonArray.getJSONObject(i).getString("cur_time");
                        n.event = jsonArray.getJSONObject(i).getString("error");
                        notifications.add(n);
                        Log.d("FETCH_NOTIFICATION_DB", n.timestamp +":     " + n.event);
                                            }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                Log.d("FETCH_NOTIFICATION_DB", "Successful notification fetch from DB");
            }
            @Override
            public void onFailure(int status, Header[] headers, String answer, Throwable throwable) {
                Log.e("FETCH_NOTIFICATION_DB", "FAILURE");
            }
        });

        for(int i = 0; i < notifications.size(); i++)
        {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this);

            builder.setAutoCancel(true)
                    .setDefaults(Notification.DEFAULT_ALL)
                    .setWhen(System.currentTimeMillis())
                    .setSmallIcon(R.drawable.app_icon)
                    .setContentTitle("Apotech Alert")
                    .setContentText(notifications.get(i).timestamp + ":    " + notifications.get(i).event)
                    .setDefaults(Notification.DEFAULT_LIGHTS | Notification.DEFAULT_SOUND)
                    .setContentInfo("Info");

            int mNotificationId = (int) System.currentTimeMillis();
            NotificationManager mNotifyMgr =
                    (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O)
            {
                int importance = NotificationManager.IMPORTANCE_HIGH;
                NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, "NOTIFICATION_CHANNEL_NAME", importance);

                builder.setChannelId(NOTIFICATION_CHANNEL_ID);
                mNotifyMgr.createNotificationChannel(notificationChannel);
            }
            mNotifyMgr.notify(mNotificationId, builder.build());
            Log.e("FETCH_NOTIFICATION_DB", "Sent notification.");
        }
        notifications.clear();
        Log.e("FETCH_NOTIFICATION_DB", "Finished Service Routine.");

        if(clearcnt > 0) {
            //clearNotificationDB();
        }
        clearcnt++;
        //return super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public void clearNotificationDB()
    {
        AsyncHttpClient client = new AsyncHttpClient();
        RequestParams params = new RequestParams();
        params.put("query", "truncate notifications;");
        Log.d("FETCH_NOTIFICATION_DB", "Entered attempted query: truncate notifications;");

        client.post(URL, params, new JsonHttpResponseHandler() {
            @Override
            public void onStart() {
                super.onStart();
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject res) {
                super.onSuccess(statusCode, headers, res);
                Log.d("FETCH_NOTIFICATION_DB", "Successful clearing of notification table");
            }
            @Override
            public void onFailure(int status, Header[] headers, String answer, Throwable throwable) {
                Log.e("FETCH_NOTIFICATION_DB", "FAILURE to clear table");
            }
        });
    }

}
