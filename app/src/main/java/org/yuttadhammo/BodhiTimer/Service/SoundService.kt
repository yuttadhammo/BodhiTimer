package org.yuttadhammo.BodhiTimer.Service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import org.yuttadhammo.BodhiTimer.Const.BroadcastTypes
import org.yuttadhammo.BodhiTimer.Const.BroadcastTypes.BROADCAST_PLAY
import org.yuttadhammo.BodhiTimer.R
import org.yuttadhammo.BodhiTimer.TimerActivity
import org.yuttadhammo.BodhiTimer.Util.Notifications
import org.yuttadhammo.BodhiTimer.Util.Sounds

class SoundService : Service() {

    private val TAG: String = "SoundService"

    private var stop: Boolean = false
    private var lastStamp: Long = 0L
    private var active: Int = 0

    private lateinit var soundManager: Sounds



    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {

        val pendingIntent: PendingIntent =
                Intent(this, TimerActivity::class.java).let { notificationIntent ->
                    PendingIntent.getActivity(this, 0, notificationIntent, 0)
                }


        val notification: Notification = NotificationCompat.Builder(this, Notifications.SERVICE_CHANNEL_ID)
                .setContentTitle(getText(R.string.app_name))
                .setContentText(getText(R.string.service_text))
                .setSmallIcon(R.drawable.icon)
                .setContentIntent(pendingIntent)
                .setTicker(getText(R.string.service_text))
                .build()

        startForeground(2, notification)

        soundManager = Sounds(applicationContext)


        val action = intent.action

        if (BROADCAST_PLAY == action) {
            Log.v(TAG, "Received Play Start")
            playIntent(intent)
        }

        val filter = IntentFilter()
        filter.addAction(BroadcastTypes.BROADCAST_END)
        registerReceiver(alarmEndReceiver, filter)

        val filter2 = IntentFilter()
        filter2.addAction(BroadcastTypes.BROADCAST_STOP)
        registerReceiver(stopReceiver, filter2)



        return START_NOT_STICKY
    }

    fun playIntent(intent: Intent) {
        val stamp = intent.getLongExtra("stamp", 0L)
        val volume = intent.getIntExtra("volume", 100)
        val uri = intent.getStringExtra("uri")


        if (uri != null && stamp != lastStamp) {
            lastStamp = stamp
            active++;

            var mediaPlayer = soundManager.play(uri, volume)
            mediaPlayer!!.setOnCompletionListener { mp ->
                Log.v(TAG, "Resetting media player...")
                mp.reset()
                mp.release()
                active--;

                if (stop && active < 1) {
                    Log.v(TAG, "Stopping service")

                    stopSelf()
                }
            }
        } else {
            Log.v(TAG, "Skipping play")
        }
    }


    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }


    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(alarmEndReceiver)
        unregisterReceiver(stopReceiver)
    }

    private val alarmEndReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.v(TAG, "Received Broadcast")
            playIntent(intent)
        }
    }

    private val stopReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.e(TAG, "Received Stop Broadcast")
            stop = true
        }
    }





}