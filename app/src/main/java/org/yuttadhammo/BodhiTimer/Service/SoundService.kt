package org.yuttadhammo.BodhiTimer.Service

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Binder
import android.os.IBinder
import androidx.preference.PreferenceManager
import org.yuttadhammo.BodhiTimer.Const.BroadcastTypes
import org.yuttadhammo.BodhiTimer.Const.BroadcastTypes.BROADCAST_PLAY
import org.yuttadhammo.BodhiTimer.Util.Notifications.getServiceNotification
import org.yuttadhammo.BodhiTimer.Util.Sounds
import timber.log.Timber
import java.lang.ref.WeakReference

class SoundService : Service() {


    private var stop: Boolean = false
    private var lastStamp: Long = 0L
    private var active: Int = 0

    private lateinit var soundManager: Sounds


    // Create the instance on the service.
    private val binder = LocalBinder()

    override fun onCreate() {
        super.onCreate()
        startForeground(1312, getServiceNotification(this))
        Timber.v("here")
    }


    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Timber.v("there")
        startForeground(1312, getServiceNotification(this))

        soundManager = Sounds(applicationContext)

        val action = intent.action

        if (BROADCAST_PLAY == action) {
            Timber.v("Received Play Start")
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
        val prefs = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        val stamp = intent.getLongExtra("stamp", 0L)
        val volume = prefs.getInt("tone_volume", 90)
        val uri = intent.getStringExtra("uri")


        if (uri != null && stamp != lastStamp) {
            lastStamp = stamp
            active++

            var mediaPlayer = soundManager.play(uri, volume)

            mediaPlayer?.setOnCompletionListener { mp ->
                Timber.v("Resetting media player...")
                mp.reset()
                mp.release()
                active--

                if (stop && active < 1) {
                    Timber.v("Stopping service")
                    stopSelf()
                }
            }

        } else {
            Timber.v("Skipping play")
        }
    }


    override fun onBind(intent: Intent): IBinder {
        binder.onBind(this)
        return binder
    }


    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(alarmEndReceiver)
        unregisterReceiver(stopReceiver)
    }

    private val alarmEndReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Timber.v("Received Broadcast")
            playIntent(intent)
        }
    }

    private val stopReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Timber.e("Received Stop Broadcast, active = $active")

            if (active == 0) {
                Timber.v("Stopping service")
                stopSelf()
            }
            stop = true
        }
    }

    class LocalBinder : Binder() {
        private var weakService: WeakReference<SoundService>? = null

        // Inject service instance to weak reference.
        fun onBind(service: SoundService) {
            weakService = WeakReference(service)
        }

        fun getService(): SoundService? {
            return weakService?.get()
        }
    }

    companion object {
        private const val TAG: String = "SoundService"
    }

}