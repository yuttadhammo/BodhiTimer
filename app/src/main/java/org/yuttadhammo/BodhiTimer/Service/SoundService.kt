package org.yuttadhammo.BodhiTimer.Service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.media.session.PlaybackState
import android.net.Uri
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.preference.PreferenceManager
import kotlin.math.ln

const val ACTION_PLAY: String = "org.yuttadhammo.BodhiTimer.Service.PLAY"
private const val TAG: String = "SoundManager"

class SoundService : Service() {



    private val flags: Int = PowerManager.PARTIAL_WAKE_LOCK

    private var mediaPlayer: MediaPlayer = MediaPlayer()

//    private val pm = applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager
//    private var wL: PowerManager.WakeLock = pm.newWakeLock(flags, "Bodhi:Alarm")


    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val action = intent.action
        if (ACTION_PLAY == action) {

            val volume = intent.getIntExtra("volume", 100)
            val uri = intent.getStringExtra("uri")

            if (uri != null) {
                play(uri, volume)
            }

        }

        return START_NOT_STICKY
    }

    private fun play(mUri: Uri, volume: Int) {

        try {
            mediaPlayer.reset()

            if (volume != 0) {
                val log1 = (ln((100 - volume).toDouble()) / ln(100.0)).toFloat()
                mediaPlayer.setVolume(1 - log1, 1 - log1)
            }
            mediaPlayer.setDataSource(applicationContext, mUri)
            mediaPlayer.prepare()
            //getWakeLock(mediaPlayer.duration)
            mediaPlayer.isLooping = false

            mediaPlayer.setOnCompletionListener { mp ->
                Log.v(TAG, "Resetting media player...")
                mp.reset()
                mp.release()

                //releaseWakeLock()
            }

            mediaPlayer.setOnErrorListener { mp, what, extra ->
                Log.e("Player error", "what:$what extra:$extra")
                true
            }

            mediaPlayer.setOnInfoListener { mp, what, extra ->
                Log.e("Player info", "what:$what extra:$extra")
                true
            }

            mediaPlayer.setWakeMode(applicationContext, flags)
            mediaPlayer.start()

            Log.v(TAG, "Playing sound")
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    private fun play(mUri: String, volume: Int) {
        var uri = mUri

        if (mUri == "sys_def") {
            uri = PreferenceManager.getDefaultSharedPreferences(applicationContext).getString("NotificationUri", "").toString()
        }

        play(Uri.parse(uri), volume)
    }

    // Make sure we can play the sound until it's finished
//    private fun getWakeLock(dur: Int) {
//        if (wL != null && wL.isHeld) {
//            Log.v(TAG, "Acquiring Wake Lock for $dur")
//            wL.acquire((dur + 1000).toLong())
//        }
//    }
//
//    private fun releaseWakeLock() {
//        if (wL != null && wL.isHeld) {
//            Log.v(TAG, "Releasing Wake Lock")
//            wL.release()
//        }
//    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.release()
    }



}