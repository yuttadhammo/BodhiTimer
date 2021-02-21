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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import org.yuttadhammo.BodhiTimer.Service.SoundManager;
import org.yuttadhammo.BodhiTimer.Service.SoundService;
import org.yuttadhammo.BodhiTimer.Util.Notification;

import static org.yuttadhammo.BodhiTimer.Service.SoundServiceKt.ACTION_PLAY;
import static org.yuttadhammo.BodhiTimer.Util.BroadcastTypes.BROADCAST_END;


// This class handles the alarm callback

public class TimerReceiver extends BroadcastReceiver {

    private final static String TAG = "TimerReceiver";

    public TimerReceiver() {
        super();
    }

    @Override
    public void onReceive(Context mContext, Intent mIntent) {

        Log.v(TAG, "Received system alarm callback ");

        // Send Broadcast to main activity
        // This will be only received if the app is not stopped (or destroyed)...
        Intent broadcast = new Intent();

        broadcast.putExtra("duration", mIntent.getIntExtra("duration", 0));
        broadcast.putExtra("id", mIntent.getIntExtra("id", 0));
        broadcast.putExtra("uri", mIntent.getStringExtra("uri"));
        broadcast.setAction(BROADCAST_END);
        mContext.sendBroadcast(broadcast);

        // Show notification
        String notificationUri = mIntent.getStringExtra("uri");
        int duration = mIntent.getIntExtra("duration", 0);

        //SoundManager mSoundManager = new SoundManager(mContext);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        boolean alwaysShow = prefs.getBoolean("showAlwaysNotifications", false);

        if (alwaysShow || notificationUri == null) {
            Notification.show(mContext, duration);
        }

        int volume = PreferenceManager.getDefaultSharedPreferences(mContext).getInt("tone_volume", 0);

        if (notificationUri != null) {
          Intent playIntent = new Intent(mContext, SoundService.class);
          playIntent.setAction(ACTION_PLAY);
          playIntent.putExtra("uri", notificationUri);
          playIntent.putExtra("volume", volume);
          mContext.startService(playIntent);
        }
    }

}