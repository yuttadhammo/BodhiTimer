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

/* @file TimerActivity.java
 *
 * TeaTimer is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version. More info: http://www.gnu.org/licenses/
 *
 * Copyright 2009 Ralph Gootee <rgootee@gmail.com>
 *
 */

package org.yuttadhammo.BodhiTimer;

import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.media.AudioManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.WindowManager.LayoutParams;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import org.jetbrains.annotations.NotNull;
import org.yuttadhammo.BodhiTimer.Animation.TimerAnimation;
import org.yuttadhammo.BodhiTimer.Service.AlarmTaskManager;
import org.yuttadhammo.BodhiTimer.Service.SessionType;
import org.yuttadhammo.BodhiTimer.Service.TimerList;
import org.yuttadhammo.BodhiTimer.Util.Notification;
import org.yuttadhammo.BodhiTimer.Util.Time;

import java.io.FileNotFoundException;
import java.util.ArrayList;

import static org.yuttadhammo.BodhiTimer.Service.TimerState.PAUSED;
import static org.yuttadhammo.BodhiTimer.Service.TimerState.RUNNING;
import static org.yuttadhammo.BodhiTimer.Service.TimerState.STOPPED;
import static org.yuttadhammo.BodhiTimer.Util.BroadcastTypes.BROADCAST_END;
import static org.yuttadhammo.BodhiTimer.Util.BroadcastTypes.BROADCAST_STOP;
import static org.yuttadhammo.BodhiTimer.Util.BroadcastTypes.BROADCAST_UPDATE;


/**
 * The main activity which shows the timer and allows the user to set the time
 *
 * @author Ralph Gootee (rgootee@gmail.com)
 */
public class TimerActivity extends AppCompatActivity implements OnClickListener, OnSharedPreferenceChangeListener {

    /**
     * Should the logs be shown
     */
    private final static boolean LOG = true;


    /**
     * debug string
     */
    private final String TAG = "TimerActivity";


    /**
     * To save having to traverse the view tree
     */
    private ImageButton mPauseButton, mCancelButton, mSetButton, mPrefButton;

    private TimerAnimation mTimerAnimation;
    private TextView mTimerLabel;
    private TextView mPreviewLabel;

    private Bitmap mPlayBitmap, mPauseBitmap;

    public AlarmTaskManager mAlarmTaskManager;

    private SharedPreferences prefs;

    private boolean widget;

    private TimerActivity context;

    private int animationIndex;

    private ImageView blackView;

    private boolean invertColors = false;

    private TextToSpeech tts;


    /**
     * Called when the activity is first created.
     * { @inheritDoc}
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "CREATE");
        super.onCreate(savedInstanceState);

        context = this;
        mAlarmTaskManager = new ViewModelProvider(this).get(AlarmTaskManager.class);

        prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        prefs.registerOnSharedPreferenceChangeListener(this);

        setupObservers();
        prepareUI();


        IntentFilter filter2 = new IntentFilter();
        filter2.addAction(BROADCAST_END);
        registerReceiver(alarmEndReceiver, filter2);

        Notification.createNotificationChannel(context);

    }

    private void prepareUI() {
        setContentView(R.layout.main);


        mCancelButton = findViewById(R.id.cancelButton);
        mCancelButton.setOnClickListener(this);

        mSetButton = findViewById(R.id.setButton);
        mSetButton.setOnClickListener(this);
        mSetButton.setOnLongClickListener(v -> {
            if (prefs.getBoolean("SwitchTimeMode", false))
                showNumberPicker();
            else
                startVoiceRecognitionActivity();
            return false;
        });

        mPauseButton = findViewById(R.id.pauseButton);
        mPauseButton.setOnClickListener(this);

        mPrefButton = findViewById(R.id.prefButton);
        mPrefButton.setOnClickListener(this);

        mPauseBitmap = BitmapFactory.decodeResource(
                getResources(), R.drawable.pause);

        mPlayBitmap = BitmapFactory.decodeResource(
                getResources(), R.drawable.play);

        mTimerLabel = findViewById(R.id.text_top);
        mPreviewLabel = findViewById(R.id.text_preview);

        mTimerAnimation = findViewById(R.id.mainImage);
        mTimerAnimation.setOnClickListener(this);

        blackView = findViewById(R.id.black);

        animationIndex = prefs.getInt("DrawingIndex", 1);
    }

    private void setupObservers() {
        // Create the observer which updates the UI.
        final Observer<String> timerLabelObserver = newTime -> mTimerLabel.setText(newTime);

        final Observer<String> previewLabelObserver = newText -> mPreviewLabel.setText(newText);

        final Observer<Integer> timeLeftObserver = timeLeft -> updateInterfaceWithTime(timeLeft, mAlarmTaskManager.getCurTimerDurationVal());

        final Observer<Integer> durationObserver = duration -> updateInterfaceWithTime(mAlarmTaskManager.getCurTimerLeftVal(), duration);

        final Observer<Integer> stateObserver = this::hasEnteredState;

        // Observe the LiveData, passing in this activity as the LifecycleOwner and the observer.
        mAlarmTaskManager.getTimerText().observe(this, timerLabelObserver);
        mAlarmTaskManager.getPreviewText().observe(this, previewLabelObserver);
        mAlarmTaskManager.getCurTimerDuration().observe(this, durationObserver);
        mAlarmTaskManager.getCurTimerLeft().observe(this, timeLeftObserver);
        mAlarmTaskManager.getCurrentState().observe(this, stateObserver);

    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void onResume() {
        super.onResume();
        Log.i(TAG, "RESUME");
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        mAlarmTaskManager.appIsPaused = false;
        sendBroadcast(new Intent(BROADCAST_STOP)); // tell widgets to stop updating

        if (getIntent().hasExtra("set")) {
            Log.d(TAG, "Create From Widget");
            widget = true;
            getIntent().removeExtra("set");
        }

        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        animationIndex = prefs.getInt("DrawingIndex", 1);

        setupUI();


        if (mAlarmTaskManager.getCurrentState().getValue() == STOPPED) {

            if (widget) {
                if (prefs.getBoolean("SwitchTimeMode", false))
                    startVoiceRecognitionActivity();
                else
                    showNumberPicker();
                return;
            }

        }

        widget = false;
    }


    /**
     * { @inheritDoc}
     */
    @Override
    public void onPause() {
        super.onPause();
        Log.i(TAG, "PAUSE");
        mAlarmTaskManager.appIsPaused = true; // tell gui timer to stop
        sendBroadcast(new Intent(BROADCAST_UPDATE)); // tell widgets to update

        BitmapDrawable drawable = (BitmapDrawable) mTimerAnimation.getDrawable();
        if (drawable != null) {
            Bitmap bitmap = drawable.getBitmap();
            bitmap.recycle();
        }

    }

    @Override
    protected void onStop() {
        // When our activity is stopped ensure we also stop the connection to the service
        // this stops us leaking our activity into the system *bad*
        Log.d(TAG, "STOPPED");

        super.onStop();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "DESTROY");
        //Close the Text to Speech Library
        if (tts != null) {

            tts.stop();
            tts.shutdown();
            Log.d(TAG, "TTSService Destroyed");
        }

        unregisterReceiver(alarmEndReceiver);


        super.onDestroy();
    }

    private void setupUI() {
        try {
            mTimerAnimation.setIndex(animationIndex);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        if (prefs.getBoolean("hideTime", false))
            mTimerLabel.setVisibility(View.INVISIBLE);
        else
            mTimerLabel.setVisibility(View.VISIBLE);


        boolean newInvertColors = prefs.getBoolean("invert_colors", false);

        if (newInvertColors != invertColors) {

            if (newInvertColors) {
                mPauseBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.pause_black);

                mPlayBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.play_black);
                mSetButton.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.set_black));

                if (prefs.getInt("State", STOPPED) == RUNNING)
                    mPauseButton.setImageBitmap(mPauseBitmap);
                else
                    mPauseButton.setImageBitmap(mPlayBitmap);

                mPrefButton.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.preferences_black));
                mCancelButton.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.stop_black));
                findViewById(R.id.mainLayout).setBackgroundColor(0xFFFFFFFF);
                mTimerLabel.setTextColor(0xFF000000);
            } else {
                mPauseBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.pause);

                mPlayBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.play);
                mSetButton.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.set));

                if (prefs.getInt("State", STOPPED) == RUNNING)
                    mPauseButton.setImageBitmap(mPauseBitmap);
                else
                    mPauseButton.setImageBitmap(mPlayBitmap);

                mPrefButton.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.preferences));
                mCancelButton.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.stop));
                findViewById(R.id.mainLayout).setBackgroundColor(0xFF000000);
                mTimerLabel.setTextColor(0xFFFFFFFF);
            }

            invertColors = newInvertColors;
        }

        setLowProfile();
        if (prefs.getBoolean("WakeLock", false))
            getWindow().addFlags(LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (prefs.getBoolean("FULLSCREEN", false))
            getWindow().setFlags(LayoutParams.FLAG_FULLSCREEN, LayoutParams.FLAG_FULLSCREEN);
        else
            getWindow().clearFlags(LayoutParams.FLAG_FULLSCREEN);
    }


    /**
     * {@inheritDoc}
     */
    public void onClick(View v) {

        setLowProfile();


        switch (v.getId()) {
            case R.id.setButton:
                Log.i("Timer", "set button clicked");
                if (prefs.getBoolean("SwitchTimeMode", false))
                    startVoiceRecognitionActivity();
                else
                    showNumberPicker();
                break;

            case R.id.prefButton:
                Log.i("Timer", "pref button clicked");
                widget = false;
                startActivity(new Intent(this, SettingsActivity.class));
                break;


            case R.id.pauseButton:
                switch (mAlarmTaskManager.getCurrentState().getValue()) {
                    case RUNNING:
                        mAlarmTaskManager.timerPause();
                        break;
                    case PAUSED:
                        mAlarmTaskManager.timerUnPause();
                        break;
                    case STOPPED:
                        // We are stopped and want to restore the last used timers.
                        mAlarmTaskManager.startAlarms(mAlarmTaskManager.retrieveTimerList());
                        break;
                }
                break;

            case R.id.cancelButton:
                mAlarmTaskManager.stopAlarmsAndTicker();
                mAlarmTaskManager.loadLastTimers();
                break;
        }
    }


    @Override
    public boolean onKeyDown(int keycode, KeyEvent e) {
        if (keycode == KeyEvent.KEYCODE_MENU) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        return super.onKeyDown(keycode, e);
    }


    private void setLowProfile() {

        View rootView = getWindow().getDecorView();
        rootView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);

    }

    private void showNumberPicker() {
        Intent i = new Intent(this, NNumberPicker.class);


        int lastTimePicked = prefs.getInt("LastSimpleTime", 0);

        i.putExtra("times", Time.time2Array(lastTimePicked));

        startActivityForResult(i, NUMBER_PICK_REQUEST_CODE);
    }


    /**
     * Updates the time
     *
     * @param elapsed  the elapsed time of the current timer
     * @param duration the duration of the current timer
     */
    public void updateInterfaceWithTime(int elapsed, int duration) {

        // TODO: This is a hack, to show the full circle after stopped or finished..
        if (elapsed == duration) {
            elapsed = 0;
        }

        if (animationIndex != 0) {
            blackView.setVisibility(View.GONE);
            mTimerAnimation.updateImage(elapsed, duration);
        } else {
            float p = (duration != 0) ? (elapsed / (float) duration) : 0;
            int alpha = Math.round(255 * p);
            alpha = Math.min(Math.max(0, alpha), 255);

            int val = (invertColors ? 255 : 0);

            int color = Color.argb(alpha, val, val, val);
            blackView.setBackgroundColor(color);
            blackView.setVisibility(View.VISIBLE);
        }
    }

    public void startSimpleAlarm(int[] numbers, boolean startAll) {
        startSimpleAlarm(Time.msFromArray(numbers), startAll);
    }

    public void startSimpleAlarm(int time, boolean startAll) {

        TimerList tL = createSimpleTimerList(time);

        mAlarmTaskManager.saveTimerList(tL);
        mAlarmTaskManager.addAlarms(tL, 0);

        if (startAll) {
            mAlarmTaskManager.startAll();
        }


    }

    @NotNull
    private TimerList createSimpleTimerList(int time) {
        int prepTime = prefs.getInt("preparationTime", 0) * 1000;
        String preUriString = prefs.getString("PreSoundUri", "");

        TimerList tL = new TimerList();

        // Add a preparatory timer
        if (!preUriString.equals("")) {

            switch (preUriString) {
                case "system":
                    preUriString = prefs.getString("PreSystemUri", "");
                    break;
                case "file":
                    preUriString = prefs.getString("PreFileUri", "");
                    break;
            }

            tL.timers.add(new TimerList.Timer(prepTime, preUriString, SessionType.PREPARATION));
        }


        String notificationUri = prefs.getString("NotificationUri", "android.resource://org.yuttadhammo.BodhiTimer/" + R.raw.bell);

        switch (notificationUri) {
            case "system":
                notificationUri = prefs.getString("SystemUri", "");
                break;
            case "file":
                notificationUri = prefs.getString("FileUri", "");
                break;
        }

        // Add main timer

        tL.timers.add(new TimerList.Timer(time, notificationUri, SessionType.REAL));
        return tL;
    }


    public void startAdvancedAlarm(String advTimeString) {
        TimerList list = new TimerList(advTimeString);

        Log.v(TAG, "advString: " + advTimeString);
        Log.v(TAG, "advString2: " + list.getString());

        mAlarmTaskManager.startAlarms(list);
    }


    /**
     * Callback for the number picker dialog
     */
    public void onNumbersPicked(int[] numbers) {
        if (numbers == null) {
            widget = false;
            return;
        }

        mAlarmTaskManager.stopAlarmsAndTicker();

        Editor editor = prefs.edit();

        // advanced timer - 0 will be -1
        if (numbers[0] == -1) {
            String advTimeString = prefs.getString("advTimeString", "120000#sys_def");

            // Overwrite the current timeString
            editor.putString("timeString", advTimeString);

            if (advTimeString == null || advTimeString.length() == 0) {
                widget = false;
                return;
            }
            editor.putBoolean("LastWasSimple", false);
            startAdvancedAlarm(advTimeString);

        } else {
            Log.v(TAG, "Saving simple time: " + Time.msFromArray(numbers));
            editor.putInt("LastSimpleTime", Time.msFromArray(numbers));
            editor.putBoolean("LastWasSimple", true);
            startSimpleAlarm(numbers, true);
        }

        editor.commit();

    }


    private void hasEnteredState(int newState) {
        switch (newState) {
            case RUNNING:
                mSetButton.setVisibility(View.GONE);
                mCancelButton.setVisibility(View.VISIBLE);
                mPauseButton.setVisibility(View.VISIBLE);
                mPauseButton.setImageBitmap(mPauseBitmap);
                setButtonAlpha(127);
                break;

            case STOPPED:
                mPauseButton.setImageBitmap(mPlayBitmap);
                mCancelButton.setVisibility(View.GONE);
                mSetButton.setVisibility(View.VISIBLE);
                setButtonAlpha(255);
                break;

            case PAUSED:
                mSetButton.setVisibility(View.GONE);
                mPauseButton.setVisibility(View.VISIBLE);
                mCancelButton.setVisibility(View.VISIBLE);
                mPauseButton.setImageBitmap(mPlayBitmap);
                setButtonAlpha(255);
                break;
        }
    }


    private void setButtonAlpha(int i) {
        mPauseButton.setImageAlpha(i);
        mCancelButton.setImageAlpha(i);
        mPrefButton.setImageAlpha(i);
    }


    /**
     * Update visual components if preferences have changed
     */
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals("WakeLock")) {
            if (prefs.getBoolean("WakeLock", false))
                getWindow().addFlags(LayoutParams.FLAG_KEEP_SCREEN_ON);
            else
                getWindow().clearFlags(LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else if ((key.equals("PreSoundUri") || key.equals("preparationTime")) && prefs.getBoolean("LastWasSimple", false)) {
            int lastTime = prefs.getInt("LastSimpleTime", 1200);

            if  (mAlarmTaskManager.getCurrentState().getValue() == STOPPED) {
                startSimpleAlarm(lastTime, false);
            }
        }
    }


    private final int VOICE_RECOGNITION_REQUEST_CODE = 1234;
    private final int NUMBER_PICK_REQUEST_CODE = 5678;

    /**
     * Fire an intent to start the speech recognition activity.
     */
    private void startVoiceRecognitionActivity() {
        try {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);

            // Display an hint to the user about what he should say.
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.speech_description));

            // Give a hint to the recognizer about what the user is going to say
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);

            intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1000);

            // Specify how many results you want to receive. The results will be sorted
            // where the first result is the one with higher confidence.
            intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5);

            startActivityForResult(intent, VOICE_RECOGNITION_REQUEST_CODE);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, getString(R.string.NoVoiceRecognitionInstalled), Toast.LENGTH_SHORT).show();

        }
    }


    /**
     * Handle the all activities.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (LOG) Log.v(TAG, "Got result");

        if (requestCode == NUMBER_PICK_REQUEST_CODE && resultCode == RESULT_OK) {
            int[] values = data.getIntArrayExtra("times");

            onNumbersPicked(values);
            if (widget) {
                finish();
            }
        } else if (requestCode == VOICE_RECOGNITION_REQUEST_CODE && resultCode == RESULT_OK) {
            // Fill the list view with the strings the recognizer thought it could have heard
            onVoiceSpoken(data);
        }

        widget = false;

        super.onActivityResult(requestCode, resultCode, data);
    }

    private void onVoiceSpoken(Intent data) {
        ArrayList<String> matches = data.getStringArrayListExtra(
                RecognizerIntent.EXTRA_RESULTS);
        for (String match : matches) {

            match = match.toLowerCase();
            Log.d(TAG, "Got speech: " + match);

            if (match.contains(Time.TIME_SEPARATOR)) {
                String complexTime = Time.str2complexTimeString(this, match);
                if (complexTime.length() > 0) {
                    Editor editor = prefs.edit();
                    editor.putString("timeString", complexTime);
                    editor.apply();
                    if (prefs.getBoolean("SpeakTime", false)) {
                        tts = new TextToSpeech(this, status -> {
                            if (status == TextToSpeech.SUCCESS) {
                                tts.speak(getString(R.string.adv_speech_recognized), TextToSpeech.QUEUE_ADD, null);
                            } else
                                Log.e("error", "Initialization failed!");
                        });
                    }
                    Toast.makeText(this, getString(R.string.adv_speech_recognized), Toast.LENGTH_SHORT).show();
                    int[] values = {-1, -1, -1};
                    onNumbersPicked(values);
                    break;
                }
            } else {
                final int speechTime = Time.str2timeString(this, match);
                if (speechTime != 0) {
                    int[] values = Time.time2Array(speechTime);
                    Toast.makeText(this, String.format(getString(R.string.speech_recognized), Time.time2humanStr(this, speechTime)), Toast.LENGTH_SHORT).show();
                    if (prefs.getBoolean("SpeakTime", false)) {
                        Log.d(TAG, "Speaking time");
                        tts.speak(String.format(getString(R.string.speech_recognized), Time.time2humanStr(context, speechTime)), TextToSpeech.QUEUE_ADD, null);
                    }

                    onNumbersPicked(values);
                    break;
                } else
                    Toast.makeText(this, getString(R.string.speech_not_recognized), Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Should move to Manager....
    // receiver to get restart
    private final BroadcastReceiver alarmEndReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.v(TAG, "Received app alarm callback");
            Log.d(TAG, "id " + intent.getIntExtra("id", -1));

            mAlarmTaskManager.onAlarmEnd(intent.getIntExtra("id", -1));
        }
    };


}
