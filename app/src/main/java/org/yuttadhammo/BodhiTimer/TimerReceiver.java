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

import android.app.Application;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.speech.tts.TextToSpeech;
import android.util.Log;


import org.yuttadhammo.BodhiTimer.Service.AlarmTaskManager;

import java.util.Date;

import static org.yuttadhammo.BodhiTimer.Util.BroadcastTypes.*;


// This class handles the alarm callback

public class TimerReceiver extends BroadcastReceiver {

    private final static String TAG = "TimerReceiver";
    final static String CANCEL_NOTIFICATION = "CANCEL_NOTIFICATION";
    public static MediaPlayer player;
    private Context context;

    public TimerReceiver() {
        super();
    }

    @Override
    public void onReceive(Context contextPassed, Intent pintent) {
        context = contextPassed;


        NotificationManager mNM = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Cancel notification and return...
        if (CANCEL_NOTIFICATION.equals(pintent.getAction())) {
            Log.v(TAG, "Cancelling notification...");

            mNM.cancelAll();
            return;
        }

        if (player != null) {
            Log.v(TAG, "Releasing media player...");
            try {
                player.reset();
                player.release();
                player = null;
            } catch (Exception e) {
                e.printStackTrace();
                player = null;
            }
        }

        Log.v(TAG, "Received alarm callback ");

        Intent broadcast = new Intent();
        broadcast.putExtra("time", 000);
        broadcast.putExtra("id", pintent.getIntExtra("id", 0));
        broadcast.setAction(BROADCAST_END);
        context.sendBroadcast(broadcast);


    }

}