package org.yuttadhammo.BodhiTimer;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import java.util.HashMap;
import java.util.Locale;

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
        Log.e("TTSService","initializing TTSService");
        if (status == TextToSpeech.SUCCESS) {
            HashMap<String,String> hashAudio = new HashMap<String, String>();
            hashAudio.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "english");


            Log.e("TTSService","speaking: "+spokenText);

            mTts.setOnUtteranceCompletedListener(new TextToSpeech.OnUtteranceCompletedListener() {
                @Override
                public void onUtteranceCompleted(String s) {
                    Log.d("TTSService","utterance completed");
                    stopSelf();
                }
            });

            mTts.speak(spokenText, TextToSpeech.QUEUE_FLUSH, hashAudio);
        }
        else
            Log.e("TTSService","error initializing TTSService");
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
    public int onStartCommand (Intent intent, int flags, int startId){

        spokenText = intent.getStringExtra("spoken_text");
        Log.d("TTSService",spokenText );

        return START_STICKY;
    }
}