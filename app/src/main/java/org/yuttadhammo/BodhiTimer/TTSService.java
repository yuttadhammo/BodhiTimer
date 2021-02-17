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

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import java.util.HashMap;

/**
 * Created by noah on 10/6/14.
 */
public class TTSService extends Service implements TextToSpeech.OnInitListener {
    private TextToSpeech mTts;
    private String spokenText;

    @Override
    public void onCreate() {
        mTts = new TextToSpeech(this, this);
    }

    @Override
    public void onInit(int status) {
        Log.e("TTSService", "initializing TTSService");
        if (status == TextToSpeech.SUCCESS) {
            HashMap<String, String> hashAudio = new HashMap<>();
            hashAudio.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "english");


            Log.e("TTSService", "speaking: " + spokenText);

            mTts.setOnUtteranceCompletedListener(s -> {
                Log.d("TTSService", "utterance completed");
                stopSelf();
            });

            mTts.speak(spokenText, TextToSpeech.QUEUE_FLUSH, hashAudio);
        } else
            Log.e("TTSService", "error initializing TTSService");
    }

    @Override
    public void onDestroy() {
        if (mTts != null) {
            mTts.stop();
            mTts.shutdown();
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        spokenText = intent.getStringExtra("spoken_text");
        Log.d("TTSService", spokenText);

        return START_STICKY;
    }
}