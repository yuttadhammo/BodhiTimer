package org.yuttadhammo.BodhiTimer.Util;

import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import org.yuttadhammo.BodhiTimer.R;
import org.yuttadhammo.BodhiTimer.Service.SessionType;

import java.io.IOException;

public class Notification {
    private static final String TAG = "NOTIFY";
    private static final String DEFAULTURI = "android.resource://org.yuttadhammo.BodhiTimer/" + R.raw.bell1;

    public static void show(Context context, String notificationUri, int time) {

        MediaPlayer player;
        SharedPreferences prefs;



        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);


        // ...or display a new one
        Log.v(TAG, "Showing notification...");


        player = new MediaPlayer();

        int setTime = time;
        String setTimeStr = Time.time2humanStr(context, setTime);
        Log.v(TAG, "Time: " + setTime);

        CharSequence text = context.getText(R.string.Notification);
        CharSequence textLatest = String.format(context.getString(R.string.timer_for_x), setTimeStr);

        // Load the settings
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean led = prefs.getBoolean("LED", true);
        boolean vibrate = prefs.getBoolean("Vibrate", true);

        if (notificationUri == null)
            notificationUri = DEFAULTURI;

        if (notificationUri.equals("sys_def"))
            notificationUri = prefs.getString("NotificationUri", DEFAULTURI);

//        boolean useAdvTime = prefs.getBoolean("useAdvTime", false);
//        String advTimeString = prefs.getString("advTimeString", "");
//        advTime = null;
//        advTimeIndex = 0;
//
//        if (useAdvTime && advTimeString.length() > 0) {
//            advTime = advTimeString.split("\\^");
//            advTimeIndex = prefs.getInt("advTimeIndex", 0);
//            String[] thisAdvTime = advTime[advTimeIndex].split("#"); // will be of format timeInMs#pathToSound
//
//            if (thisAdvTime.length == 3)
//                notificationUri = thisAdvTime[1];
//            if (notificationUri.equals("sys_def"))
//                notificationUri = prefs.getString("NotificationUri", "android.resource://org.yuttadhammo.BodhiTimer/" + R.raw.bell);
//        } else
//            notificationUri = prefs.getString("NotificationUri", "android.resource://org.yuttadhammo.BodhiTimer/" + R.raw.bell);
//
//        Log.v(TAG, "notification uri: " + notificationUri);
//
//        switch (notificationUri) {
//            case "system":
//                notificationUri = prefs.getString("SystemUri", "");
//                break;
//            case "file":
//                notificationUri = prefs.getString("FileUri", "");
//                break;
//        }

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(context.getApplicationContext())
                        .setSmallIcon(R.drawable.notification)
                        .setContentTitle(text)
                        .setContentText(textLatest);

        Uri uri = null;

        // Play a sound!
        if (!notificationUri.equals(""))
            uri = Uri.parse(notificationUri);

        // Vibrate
        if (vibrate && uri == null) {
            mBuilder.setDefaults(android.app.Notification.DEFAULT_VIBRATE);
        }

        // Have a light
        if (led) {
            mBuilder.setLights(0xff00ff00, 300, 1000);
        }

        mBuilder.setAutoCancel(true);

        // Creates an explicit intent for an Activity in your app
//        Intent resultIntent = new Intent(context, TimerActivity.class);
//
//        // The stack builder object will contain an artificial back stack for the
//        // started Activity.
//        // This ensures that navigating backward from the Activity leads out of
//        // your application to the Home screen.
//        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
//        // Adds the back stack for the Intent (but not the Intent itself)
//        stackBuilder.addParentStack(TimerActivity.class);
//        // Adds the Intent that starts the Activity to the top of the stack
//        stackBuilder.addNextIntent(resultIntent);
//        PendingIntent resultPendingIntent =
//                stackBuilder.getPendingIntent(
//                        0,
//                        PendingIntent.FLAG_UPDATE_CURRENT
//                );
//        mBuilder.setContentIntent(resultPendingIntent);
//        NotificationManager mNotificationManager =
//                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
//
//        // Create intent for cancelling the notification
//        Context appContext = context.getApplicationContext();
//        Intent intent = new Intent(appContext, TimerReceiver.class);
//        intent.setAction(CANCEL_NOTIFICATION);
//
//        // Cancel the pending cancellation and create a new one
//        PendingIntent pendingCancelIntent =
//                PendingIntent.getBroadcast(appContext, 0, intent,
//                        PendingIntent.FLAG_CANCEL_CURRENT);


        if (uri != null) {

            // We play the sound manually, so that it works even if muted.
            //remove notification sound
            mBuilder.setSound(null);

            try {
                if (player != null && player.isPlaying()) {
                    player.release();
                    player = new MediaPlayer();
                }

                int currVolume = prefs.getInt("tone_volume", 0);
                if (currVolume != 0) {
                    float log1 = (float) (Math.log(100 - currVolume) / Math.log(100));
                    player.setVolume(1 - log1, 1 - log1);
                }
                player.setDataSource(context, uri);
                player.prepare();
                player.setLooping(false);
                player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        Log.v(TAG, "Releasing media player...");
                        mp.reset();
                        mp.release();
                    }

                });
                player.start();
                if (vibrate) {
                    Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
                    vibrator.vibrate(1000);
                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        mNotificationManager.notify(0, mBuilder.build());


//        if (prefs.getBoolean("AutoClear", false)) {
//            // Determine duration of notification sound
//            int duration = 5000;
//            if (uri != null) {
//                MediaPlayer cancelPlayer = new MediaPlayer();
//                try {
//                    cancelPlayer.setDataSource(context, uri);
//                    cancelPlayer.prepare();
//                    duration = Math.max(duration, cancelPlayer.getDuration() + 2000);
//                } catch (IOException ex) {
//                    Log.e(TAG, "Cannot get sound duration: " + ex);
//                    duration = 30000; // on error, default to 30 seconds
//                } finally {
//                    cancelPlayer.release();
//                }
//                cancelPlayer.release();
//            }
//            Log.v(TAG, "Notification duration: " + duration + " ms");
//            // Schedule cancellation
//            AlarmManager alarmMgr = (AlarmManager) context
//                    .getSystemService(Context.ALARM_SERVICE);
//            alarmMgr.set(AlarmManager.ELAPSED_REALTIME,
//                    SystemClock.elapsedRealtime() + duration,
//                    pendingCancelIntent);
//        }


    }

}
