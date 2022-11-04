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

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeech.OnInitListener
import timber.log.Timber

/**
 * Created by noah on 10/6/14.
 */
class TTSService : Service(), OnInitListener {
    private var mTts: TextToSpeech? = null
    private var spokenText: String? = null
    override fun onCreate() {
        mTts = TextToSpeech(this, this)
    }

    override fun onInit(status: Int) {
        Timber.i("initializing TTSService")
        if (status == TextToSpeech.SUCCESS) {
            val hashAudio = HashMap<String, String>()
            hashAudio[TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID] = "english"
            Timber.i("speaking: $spokenText")
            mTts!!.setOnUtteranceCompletedListener { s: String? ->
                Timber.d("utterance completed")
                stopSelf()
            }
            mTts!!.speak(spokenText, TextToSpeech.QUEUE_FLUSH, hashAudio)
        } else Timber.e("error initializing TTSService")
    }

    override fun onDestroy() {
        if (mTts != null) {
            mTts!!.stop()
            mTts!!.shutdown()
        }
        super.onDestroy()
    }

    override fun onBind(arg0: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        spokenText = intent.getStringExtra("spoken_text")
        Timber.d(spokenText!!)
        return START_STICKY
    }
}