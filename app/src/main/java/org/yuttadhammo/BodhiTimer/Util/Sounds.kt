package org.yuttadhammo.BodhiTimer.Util

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.os.PowerManager
import android.util.Log
import androidx.preference.PreferenceManager
import org.yuttadhammo.BodhiTimer.R
import kotlin.math.ln


class Sounds(private val mContext: Context) {
    private val TAG: String = "Sound Util"

    private val flags: Int = PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP

    private fun play(mUri: Uri, volume: Int): MediaPlayer? {

        try {
            var mediaPlayer = MediaPlayer()


            mediaPlayer.setDataSource(mContext, mUri)
            mediaPlayer.prepare()

            //Log.v(TAG, "Volume: " + volume)
            if (volume != 0) {
                val log1 = (ln((100 - volume).toDouble()) / ln(100.0)).toFloat()
                mediaPlayer.setVolume(1 - log1, 1 - log1)
                //Log.v(TAG, "Volume: " + (1 -log1))
            }


            mediaPlayer.isLooping = false

            mediaPlayer.setOnCompletionListener { mp ->
                Log.v(TAG, "Resetting media player...")
                mp.reset()
                mp.release()
            }

            mediaPlayer.setOnErrorListener { _, what, extra ->
                Log.e("Player error", "what:$what extra:$extra")
                true
            }

            mediaPlayer.setOnInfoListener { _, what, extra ->
                Log.e("Player info", "what:$what extra:$extra")
                true
            }

            mediaPlayer.setWakeMode(mContext, flags)
            mediaPlayer.start()

            Log.v(TAG, "Playing sound")

            return mediaPlayer
        } catch (e: Exception) {
            Log.w(TAG, "Problem playing sound, uri: $mUri")
            e.printStackTrace()
            //throw (e)
        }

        return null
    }


    fun play(mUri: String, volume: Int): MediaPlayer? {
        var uri = resolveUri(mUri, mContext)

        if (uri != "") {
            return play(Uri.parse(uri), volume)
        }

        return null
    }

    companion object {
        const val DEFAULT_SOUND = "android.resource://org.yuttadhammo.BodhiTimer/${R.raw.bowl_low}"

        fun resolveUri(mUri: String, mContext: Context): String {
            val prefs = PreferenceManager.getDefaultSharedPreferences(mContext)
            var result = "";

            result = mUri

            if (result == "sys_def") {
                result = prefs.getString("NotificationUri", "").toString()
            }

            when (result) {
                "system" -> result = prefs.getString("SystemUri", "")!!
                "file" -> result = prefs.getString("FileUri", "")!!
            }

            return result

        }
    }


}