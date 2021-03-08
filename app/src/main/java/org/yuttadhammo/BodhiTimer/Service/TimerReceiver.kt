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
package org.yuttadhammo.BodhiTimer.Service

import android.content.*
import android.os.IBinder
import android.util.Log
import androidx.preference.PreferenceManager
import org.yuttadhammo.BodhiTimer.Const.BroadcastTypes.BROADCAST_END
import org.yuttadhammo.BodhiTimer.Const.BroadcastTypes.BROADCAST_PLAY
import org.yuttadhammo.BodhiTimer.Util.Notifications
import org.yuttadhammo.BodhiTimer.Util.Notifications.Companion.show
import org.yuttadhammo.BodhiTimer.Util.Sounds


// This class handles the alarm callback
class TimerReceiver : BroadcastReceiver() {
    private var notificationUri: String? = null
    private var stamp: Long = 0
    private var volume: Int = 100
    lateinit var mContext: Context


    override fun onReceive(context: Context, mIntent: Intent) {
        Log.v(TAG, "Received system alarm callback ")

        stamp = System.currentTimeMillis()
        mContext = context


        // Send Broadcast to main activity
        // This will be only received if the app is not stopped (or destroyed)...
        val broadcast = Intent()
        broadcast.putExtra("duration", mIntent.getIntExtra("duration", 0))
        broadcast.putExtra("id", mIntent.getIntExtra("id", 0))
        broadcast.putExtra("uri", mIntent.getStringExtra("uri"))
        broadcast.putExtra("stamp", stamp)
        broadcast.action = BROADCAST_END
        mContext.sendBroadcast(broadcast)

        // Show notification
        notificationUri = mIntent.getStringExtra("uri")
        val duration = mIntent.getIntExtra("duration", 0)
        val prefs = PreferenceManager.getDefaultSharedPreferences(mContext)
        val alwaysShow = prefs.getBoolean("showAlwaysNotifications", false)

        if (alwaysShow || notificationUri == null) {
            show(mContext, duration)
        }

        volume = prefs.getInt("tone_volume", 0)

        if (notificationUri == null) return

        if (!prefs.getBoolean("useOldNotification", false)) {
            var playIntent = getServiceIntent(mContext)

            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    mContext.startForegroundService(playIntent)
                } else {
                    mContext.startService(playIntent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Could not start service")
                Sounds(mContext).play(notificationUri!!, volume)
            }
        } else {
            Sounds(mContext).play(notificationUri!!, volume)
        }
    }


    // Create the service connection.
    private var connection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            // The binder of the service that returns the instance that is created.
            val binder: SoundService.LocalBinder = service as SoundService.LocalBinder

            // The getter method to acquire the service.
            val myService: SoundService? = binder.getService()

            // getServiceIntent(context) returns the relative service intent
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                mContext.startForegroundService(getServiceIntent(mContext))
                // This is the key: Without waiting Android Framework to call this method
                // inside Service.onCreate(), immediately call here to post the notification.
                myService!!.startForeground(1312, Notifications.getServiceNotification(mContext))
            } else {
                mContext.startService(getServiceIntent(mContext))
            }


            // Release the connection to prevent leaks.
            mContext.unbindService(this)
        }

        override fun onBindingDied(name: ComponentName) {
            Log.w(TAG, "Binding has dead.")
        }


        override fun onServiceDisconnected(name: ComponentName) {
            Log.w(TAG, "Service is disconnected..")
        }
    }

    private fun getServiceIntent(mContext: Context): Intent {
        val playIntent = Intent(mContext, SoundService::class.java)
        playIntent.action = BROADCAST_PLAY
        playIntent.putExtra("uri", notificationUri)
        playIntent.putExtra("volume", volume)
        playIntent.putExtra("stamp", stamp)
        return playIntent
    }

    companion object {
        private const val TAG = "TimerReceiver"
    }
}