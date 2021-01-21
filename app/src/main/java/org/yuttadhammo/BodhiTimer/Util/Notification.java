package org.yuttadhammo.BodhiTimer.Util;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import org.yuttadhammo.BodhiTimer.R;

public class Notification {
    private static final String TAG = "NOTIFY";
    private static final String CHANNEL_ID = "ALARMS";


    public static void show(Context context, int time){
        Log.v(TAG, "Showing notification... " + time);

        // Get Notification Manager & Prefs
        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);


        // Cancel any previous notifications
        mNotificationManager.cancelAll();


        // Construct strings
        String setTimeStr = Time.time2humanStr(context, time);

        CharSequence text = context.getText(R.string.Notification);
        CharSequence textLatest = String.format(context.getString(R.string.timer_for_x), setTimeStr);



        // Create the notification
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context.getApplicationContext(), CHANNEL_ID)
                .setSmallIcon(R.drawable.notification)
                .setContentTitle(text)
                .setContentText(textLatest)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        // Handle light and vibrate in older devices
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            legacyHandler(mBuilder, prefs);
        }


        mNotificationManager.notify(0, mBuilder.build());
    }


    public static void createNotificationChannel(Context context) {
        // Get Notification Manager & Prefs
        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = context.getString(R.string.channel_name);
            String description = context.getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            // Customize
            boolean vibrate = prefs.getBoolean("Vibrate", true);
            boolean led = prefs.getBoolean("LED", false);

            // Vibrate
            if (vibrate) {
                long[] pattern = new long[]{0, 400, 200, 400};
                channel.setVibrationPattern(pattern);
                channel.enableVibration(true);
            }

            // Have a light
            if (led) {
                channel.setLightColor(0xff00ff00);
                channel.enableLights(true);
            }

            // TODO: For now we are playing the sound ourselves. But this can lead to cut of alarms

            // But notification channels don't allow changing sounds. So we would need to create a new channel each time?
            channel.setSound(null, null);


            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            mNotificationManager.createNotificationChannel(channel);
        }
    }




    private static void legacyHandler(NotificationCompat.Builder mBuilder, SharedPreferences prefs) {
        boolean vibrate = prefs.getBoolean("Vibrate", true);
        boolean led = prefs.getBoolean("LED", false);

        // Vibrate
        if (vibrate) {
            mBuilder.setDefaults(android.app.Notification.DEFAULT_VIBRATE);
        }

        // Have a light
        if (led) {
            mBuilder.setLights(0xff00ff00, 300, 1000);
        }

    }


}
