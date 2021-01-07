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

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;


import org.yuttadhammo.BodhiTimer.Util.Notification;

import static org.yuttadhammo.BodhiTimer.Util.BroadcastTypes.*;


// This class handles the alarm callback

public class TimerReceiver extends BroadcastReceiver {

    private final static String TAG = "TimerReceiver";
    private Context context;

    public TimerReceiver() {
        super();
    }

    @Override
    public void onReceive(Context contextPassed, Intent pintent) {
        context = contextPassed;


        Log.v(TAG, "Received system alarm callback ");

        // Send notification
        // This will be only received if the app is not stopped (or destroyed)...
        // TODO: Use wakeful receiver?
        Intent broadcast = new Intent();

        broadcast.putExtra("duration",  pintent.getIntExtra("duration", 0));
        broadcast.putExtra("id", pintent.getIntExtra("id", 0));
        broadcast.putExtra("uri", pintent.getStringExtra("uri"));
        broadcast.setAction(BROADCAST_END);
        context.sendBroadcast(broadcast);

    }

}