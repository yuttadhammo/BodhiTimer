package org.yuttadhammo.BodhiTimer.Util

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.os.PowerManager
import androidx.preference.PreferenceManager
import org.yuttadhammo.BodhiTimer.R
import timber.log.Timber
import kotlin.math.ln


class Sounds(private val mContext: Context) {

    private val flags: Int = PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP

    private fun play(mUri: Uri, volume: Int): MediaPlayer? {

        try {
            val mediaPlayer = MediaPlayer()

            mediaPlayer.setDataSource(mContext, mUri)
            mediaPlayer.prepare()

            //Timber.v("Volume: " + volume)
            if (volume != 0) {
                val log1 = (ln((100 - volume).toDouble()) / ln(100.0)).toFloat()
                mediaPlayer.setVolume(1 - log1, 1 - log1)
                //Timber.v("Volume: " + (1 -log1))
            }


            mediaPlayer.isLooping = false

            mediaPlayer.setOnCompletionListener { mp ->
                Timber.v("Resetting media player...")
                mp.reset()
                mp.release()
            }

            mediaPlayer.setOnErrorListener { _, what, extra ->
                Timber.e("what:" + what + " extra:" + extra)
                true
            }

            mediaPlayer.setOnInfoListener { _, what, extra ->
                Timber.e("what:" + what + " extra:" + extra)
                true
            }

            mediaPlayer.setWakeMode(mContext, flags)
            mediaPlayer.start()

            Timber.v("Playing sound")

            return mediaPlayer
        } catch (e: Exception) {
            Timber.w("Problem playing sound, uri: $mUri")
            e.printStackTrace()
            //throw (e)
        }

        return null
    }


    fun play(mUri: String, volume: Int): MediaPlayer? {
        val uri = resolveUri(mUri, mContext)

        if (uri != "") {
            return play(Uri.parse(uri), volume)
        }

        return null
    }

    companion object {
        const val DEFAULT_SOUND = "android.resource://org.yuttadhammo.BodhiTimer/${R.raw.bowl_low}"

        fun resolveUri(mUri: String, mContext: Context): String {
            val prefs = PreferenceManager.getDefaultSharedPreferences(mContext)
            var result = ""

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