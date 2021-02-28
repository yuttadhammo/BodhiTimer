package org.yuttadhammo.BodhiTimer.Service

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.os.PowerManager
import android.util.Log
import androidx.preference.PreferenceManager
import kotlin.math.ln

class SoundManager(private val mContext: Context) {

    private val TAG: String = "SoundManager"
    private val flags: Int = PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP

    private lateinit var mediaPlayer: MediaPlayer

    private fun play(mUri: Uri, volume: Int): MediaPlayer? {

        try {
            if (::mediaPlayer.isInitialized) {
                mediaPlayer.reset()
            } else {
                mediaPlayer = MediaPlayer()
            }

            if (volume != 0) {
                val log1 = (ln((100 - volume).toDouble()) / ln(100.0)).toFloat()
                mediaPlayer.setVolume(1 - log1, 1 - log1)
            }
            mediaPlayer.setDataSource(mContext, mUri)
            mediaPlayer.prepare()

            mediaPlayer.isLooping = false

            mediaPlayer.setOnCompletionListener { mp ->
                Log.v(TAG, "Resetting media player...")
                mp.reset()
                mp.release()

            }

            mediaPlayer.setOnErrorListener { mp, what, extra ->
                Log.e("Player error", "what:$what extra:$extra")
                true
            }

            mediaPlayer.setOnInfoListener { mp, what, extra ->
                Log.e("Player info", "what:$what extra:$extra")
                true
            }

            mediaPlayer.setWakeMode(mContext, flags)
            mediaPlayer.start()



            Log.v(TAG, "Playing sound")

            return mediaPlayer
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return null
    }


    fun play(mUri: String, volume: Int): MediaPlayer? {
        var uri = mUri

        if (mUri == "sys_def") {
            uri = PreferenceManager.getDefaultSharedPreferences(mContext).getString("NotificationUri", "").toString()
        }

        if (uri != null) {
            return play(Uri.parse(uri), volume)
        }

        return null
    }


}