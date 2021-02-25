package org.yuttadhammo.BodhiTimer.Service

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.os.PowerManager
import android.os.PowerManager.WakeLock
import android.util.Log
import androidx.preference.PreferenceManager

class SoundManager(private val mContext: Context) {

    private val TAG: String = "SoundManager"
    private val flags: Int = PowerManager.FULL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP

    private var player: MediaPlayer = MediaPlayer()
    private val pm = mContext.getSystemService(Context.POWER_SERVICE) as PowerManager
    private var wL: WakeLock = pm.newWakeLock(flags, "Bodhi:Alarm")

    private fun play(mUri: Uri) {

        try {
            player.reset()
            val currVolume: Int = PreferenceManager.getDefaultSharedPreferences(mContext).getInt("tone_volume", 0)
            if (currVolume != 0) {
                val log1 = (Math.log((100 - currVolume).toDouble()) / Math.log(100.0)).toFloat()
                player.setVolume(1 - log1, 1 - log1)
            }
            player.setDataSource(mContext, mUri)
            player.prepare()
            getWakeLock(player.duration)
            player.isLooping = false

            player.setOnCompletionListener { mp ->
                Log.v(TAG, "Resetting media player...")
                mp.reset()
                mp.release()

                releaseWakeLock()
            }

            player.setOnErrorListener { mp, what, extra ->
                Log.e("Player error", "what:$what extra:$extra")
                true
            }

            player.setOnInfoListener { mp, what, extra ->
                Log.e("Player info", "what:$what extra:$extra")
                true
            }

            player.setWakeMode(mContext, flags)
            player.start()

            Log.v(TAG, "Playing sound")
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    fun play(mUri: String) {
        var uri = mUri

        if (mUri == "sys_def") {
            uri = PreferenceManager.getDefaultSharedPreferences(mContext).getString("NotificationUri", "").toString()
        }

        play(Uri.parse(uri))
    }

    // Make sure we can play the sound until it's finished
    private fun getWakeLock(dur: Int) {
        if (wL != null && !wL.isHeld) {
            Log.v(TAG, "Acquiring Wake Lock for $dur")
            wL.acquire((dur + 1000).toLong())
        }
    }

    private fun releaseWakeLock() {
        if (wL != null && wL.isHeld) {
            Log.v(TAG, "Releasing Wake Lock")
            wL.release()
        }
    }
}