/*
    This file is part of Bodhi Timer.

    Bodhi Timer is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Bodhi Timer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Bodhi Timer.  If not, see <http://www.gnu.org/licenses/>.
*/

package org.yuttadhammo.BodhiTimer;

import java.io.IOException;
import java.util.Date;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.net.Uri;
import android.os.SystemClock;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import androidx.core.app.NotificationCompat;
import androidx.core.app.TaskStackBuilder;
import android.util.Log;

public class TimerReceiver extends BroadcastReceiver {
	private final static String TAG = "TimerReceiver";
    final static String CANCEL_NOTIFICATION = "CANCEL_NOTIFICATION";
	public static MediaPlayer player;

    public static String BROADCAST_RESET = "org.yuttadhammo.BodhiTimer.RESTART";

    private TextToSpeech tts;

    @Override
	public void onReceive(Context context, Intent pintent) 
    {
        Log.v(TAG,"ALARM: received alarm");

		NotificationManager mNM = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);

        if(player != null) {
            Log.v(TAG,"Releasing media player...");
            try{
                player.release();
                player = null;
            }
            catch(Exception e) {
                e.printStackTrace();
                player = null;
            }
            finally {
                // do nothing
            }
        }

        // Cancel notification and return...
        if (CANCEL_NOTIFICATION.equals(pintent.getAction())) {
            Log.v(TAG,"Cancelling notification...");
            
            mNM.cancelAll();
            return;
        }

        // ...or display a new one

		Log.v(TAG,"Showing notification...");

        player = new MediaPlayer();

        int setTime = pintent.getIntExtra("SetTime",0);
		String setTimeStr = TimerUtils.time2humanStr(context, setTime);
		Log.v(TAG,"Time: "+setTime);
		
		CharSequence text = context.getText(R.string.Notification);
		CharSequence textLatest = String.format(context.getString(R.string.timer_for_x),setTimeStr);

		// Load the settings 
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean led = prefs.getBoolean("LED",true);
        boolean vibrate = prefs.getBoolean("Vibrate",true);
        String notificationUri = "";

        boolean useAdvTime = prefs.getBoolean("useAdvTime",false);
        String advTimeString = prefs.getString("advTimeString","");
        String[] advTime = null;
        int advTimeIndex = 1;

        if(useAdvTime && advTimeString.length() > 0) {
            advTime = advTimeString.split("\\^");
            advTimeIndex = prefs.getInt("advTimeIndex", 1);
            String[] thisAdvTime = advTime[advTimeIndex-1].split("#"); // will be of format timeInMs#pathToSound

            if(thisAdvTime.length == 3)
                notificationUri = thisAdvTime[1];
            if(notificationUri.equals("sys_def"))
                notificationUri = prefs.getString("NotificationUri", "android.resource://org.yuttadhammo.BodhiTimer/" + R.raw.bell);
        }
        else
            notificationUri = prefs.getString("NotificationUri", "android.resource://org.yuttadhammo.BodhiTimer/" + R.raw.bell);

        Log.v(TAG,"notification uri: "+notificationUri);

        if(notificationUri.equals("system"))
			notificationUri = prefs.getString("SystemUri", "");
		else if(notificationUri.equals("file"))
			notificationUri = prefs.getString("FileUri", "");
        else if (notificationUri.equals("tts")) {
            notificationUri = "";
            final String ttsString = prefs.getString("tts_string",context.getString(R.string.timer_done));
            Intent ttsIntent = new Intent(context,TTSService.class);
            ttsIntent.putExtra("spoken_text", ttsString);
            context.startService(ttsIntent);
        }

		NotificationCompat.Builder mBuilder =
	        new NotificationCompat.Builder(context.getApplicationContext())
	        .setSmallIcon(R.drawable.notification)
	        .setContentTitle(text)
	        .setContentText(textLatest);

		Uri uri = null;

		// Play a sound!
        if(!notificationUri.equals(""))
			uri = Uri.parse(notificationUri);
		
        // Vibrate
        if(vibrate && uri == null){
        	mBuilder.setDefaults(Notification.DEFAULT_VIBRATE);
        }

        // Have a light
        if(led){
        	mBuilder.setLights(0xff00ff00, 300, 1000);
        }
        
        mBuilder.setAutoCancel(true);
        
		// Creates an explicit intent for an Activity in your app
      	Intent resultIntent = new Intent(context,TimerActivity.class);

		// The stack builder object will contain an artificial back stack for the
		// started Activity.
		// This ensures that navigating backward from the Activity leads out of
		// your application to the Home screen.
		TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
		// Adds the back stack for the Intent (but not the Intent itself)
		stackBuilder.addParentStack(TimerActivity.class);
		// Adds the Intent that starts the Activity to the top of the stack
		stackBuilder.addNextIntent(resultIntent);
		PendingIntent resultPendingIntent =
		        stackBuilder.getPendingIntent(
		            0,
		            PendingIntent.FLAG_UPDATE_CURRENT
		        );
		mBuilder.setContentIntent(resultPendingIntent);
		NotificationManager mNotificationManager =
		    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

		// Create intent for cancelling the notification
        Context appContext = context.getApplicationContext();
        Intent intent = new Intent(appContext, TimerReceiver.class);
        intent.setAction(CANCEL_NOTIFICATION);

        // Cancel the pending cancellation and create a new one
        PendingIntent pendingCancelIntent =
            PendingIntent.getBroadcast(appContext, 0, intent,
                                       PendingIntent.FLAG_CANCEL_CURRENT);

        


      	if(uri != null) {
      		
      		//remove notification sound
      		mBuilder.setSound(null);
			
	        try {
                if(player != null && player.isPlaying()) {
                    player.release();
                    player = new MediaPlayer();
                }

	        	int currVolume = prefs.getInt("tone_volume", 0);
	        	if(currVolume != 0) {
		        	float log1=(float)(Math.log(100-currVolume)/Math.log(100));
		            player.setVolume(1-log1,1-log1);
	        	}
	            player.setDataSource(context, uri);
		        player.prepare();
		        player.setLooping(false);
		        player.setOnCompletionListener(new OnCompletionListener(){

					@Override
					public void onCompletion(MediaPlayer mp) {
						// TODO Auto-generated method stub
						mp.release();
					}

		                });
		        player.start();
                if(vibrate) {
                    Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
                    vibrator.vibrate(1000);
                }
	        } catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
      	}
      	
        if (prefs.getBoolean("AutoClear", false)) {
            // Determine duration of notification sound
            int duration = 5000;
            if (uri != null) {
                MediaPlayer cancelPlayer = new MediaPlayer();
                try {
                	cancelPlayer.setDataSource(context, uri);
                	cancelPlayer.prepare();
                    duration = Math.max(duration, cancelPlayer.getDuration() + 2000);
                }
                catch (java.io.IOException ex) {
                    Log.e(TAG, "Cannot get sound duration: " + ex);
                    duration = 30000; // on error, default to 30 seconds
                }
                finally {
                	cancelPlayer.release();
                }
            	cancelPlayer.release();
            }
            Log.v(TAG, "Notification duration: " + duration + " ms");
            // Schedule cancellation
            AlarmManager alarmMgr = (AlarmManager)context
                .getSystemService(Context.ALARM_SERVICE);
            alarmMgr.set(AlarmManager.ELAPSED_REALTIME,
                         SystemClock.elapsedRealtime() + duration,
                         pendingCancelIntent);
        }

        if(useAdvTime && advTimeString.length() > 0) {
            Intent broadcast = new Intent();

            SharedPreferences.Editor editor = prefs.edit();
            if(advTimeIndex < advTime.length) {
                editor.putInt("advTimeIndex", advTimeIndex + 1);

                String[] thisAdvTime = advTime[advTimeIndex].split("#"); // will be of format timeInMs#pathToSound

                int time = Integer.parseInt(thisAdvTime[0]);

                broadcast.putExtra("time", time);

                // Save new time
                editor.putLong("TimeStamp", new Date().getTime() + time);
                editor.putInt("LastTime", time);

                // editor.putString("NotificationUri", thisAdvTime[1]);

                mNM.cancelAll();
                Log.v(TAG, "Starting next iteration of the timer service ...");
                Intent rintent = new Intent(context, TimerReceiver.class);
                rintent.putExtra("SetTime", time);
                PendingIntent mPendingIntent = PendingIntent.getBroadcast(context, 0, rintent, PendingIntent.FLAG_UPDATE_CURRENT);
                AlarmManager mAlarmMgr = (AlarmManager) context
                        .getSystemService(Context.ALARM_SERVICE);
                mAlarmMgr.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + time, mPendingIntent);
            }
            else {
                broadcast.putExtra("stop",true);
                editor.putInt("advTimeIndex", 1);

            }
            broadcast.setAction(BROADCAST_RESET);
            context.sendBroadcast(broadcast);

            editor.apply();

        }
        else if (prefs.getBoolean("AutoRestart", false)) {
        	int time = pintent.getIntExtra("SetTime", 0);
        	if (time != 0) {

                mNM.cancel(0);
    		    Log.v(TAG,"Restarting the timer service ...");
    		    Intent rintent = new Intent( context, TimerReceiver.class);
    		    rintent.putExtra("SetTime", time);
    		    PendingIntent mPendingIntent = PendingIntent.getBroadcast( context, 0, rintent, PendingIntent.FLAG_UPDATE_CURRENT);
                AlarmManager mAlarmMgr = (AlarmManager)context
                        .getSystemService(Context.ALARM_SERVICE);
                mAlarmMgr.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + time, mPendingIntent);

                // Save new time
                SharedPreferences.Editor editor = prefs.edit();
                editor.putLong("TimeStamp", new Date().getTime() + time);
                editor.putInt("LastTime", time);
                editor.apply();

                Intent broadcast = new Intent();
                broadcast.putExtra("time",time);
                broadcast.setAction(BROADCAST_RESET);
                context.sendBroadcast(broadcast);

            }
        }

        mNotificationManager.notify(0, mBuilder.build());

        Log.d(TAG,"ALARM: alarm finished");

	}
}