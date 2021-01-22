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
import android.text.TextUtils;
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

import androidx.appcompat.app.AppCompatActivity;

import org.yuttadhammo.BodhiTimer.Animation.TimerAnimation;
import org.yuttadhammo.BodhiTimer.Service.AlarmTaskManager;
import org.yuttadhammo.BodhiTimer.Service.SessionType;
import org.yuttadhammo.BodhiTimer.Service.SoundManager;
import org.yuttadhammo.BodhiTimer.Service.TimerList;
import org.yuttadhammo.BodhiTimer.SimpleNumberPicker.OnNNumberPickedListener;
import org.yuttadhammo.BodhiTimer.Util.Notification;
import org.yuttadhammo.BodhiTimer.Util.Time;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Timer;

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
public class TimerActivity extends AppCompatActivity implements OnClickListener, OnNNumberPickedListener, OnSharedPreferenceChangeListener {

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
    private TextView mAltLabel;

    private Bitmap mPlayBitmap, mPauseBitmap;


    public AlarmTaskManager mAlarmTaskManager;


    private SharedPreferences prefs;

    // for canceling notifications


    private boolean widget;

    private int[] lastTimes;

    private TimerActivity context;

    private int animationIndex;

    private ImageView blackView;


    private boolean invertColors = false;


    private TextToSpeech tts;

    private SoundManager mSoundManager;


    /**
     * Called when the activity is first created.
     * { @inheritDoc}
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "CREATE");
        super.onCreate(savedInstanceState);

        // Setup a new AlarmTaskManager
        mAlarmTaskManager = new AlarmTaskManager(this);
        mSoundManager = new SoundManager(this);

        setupListener();

        tts = new TextToSpeech(this, null);


        setContentView(R.layout.main);

        context = this;

        mCancelButton = findViewById(R.id.cancelButton);
        mCancelButton.setOnClickListener(this);

        mSetButton = findViewById(R.id.setButton);
        mSetButton.setOnClickListener(this);
        mSetButton.setOnLongClickListener(new OnLongClickListener() {

            @Override
            public boolean onLongClick(View v) {
                if (prefs.getBoolean("SwitchTimeMode", false))
                    showNumberPicker();
                else
                    startVoiceRecognitionActivity();
                return false;
            }

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
        mAltLabel = findViewById(R.id.text_alt);

        mTimerAnimation = findViewById(R.id.mainImage);
        mTimerAnimation.setOnClickListener(this);

        blackView = findViewById(R.id.black);

        // Store some useful values
        prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());


        // Setup Last Times
        lastTimes = new int[3];

        prefs.registerOnSharedPreferenceChangeListener(this);


        IntentFilter filter2 = new IntentFilter();
        filter2.addAction(BROADCAST_END);
        registerReceiver(alarmEndReceiver, filter2);

        Notification.createNotificationChannel(context);

    }

    private void setupListener() {

        mAlarmTaskManager.setListener(new AlarmTaskManager.AlarmTaskListener() {
            @Override
            public void onEnterState(int state) {
                enterState(state);
            }

            @Override
            public void onObjectReady(String title) {
                //AlarmTaskManager
            }

            @Override
            public void onDataLoaded(String data) {
                // Code to handle data loaded from network
                // Use the data here!
            }

            @Override
            public void onUpdateTime(int elapsed, int duration) {
                updateInterfaceWithTime(elapsed, duration);
            }
        });
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void onResume() {
        super.onResume();


        setVolumeControlStream(AudioManager.STREAM_MUSIC);


        mAlarmTaskManager.appIsPaused = false;
        sendBroadcast(new Intent(BROADCAST_STOP)); // tell widgets to stop updating
        mAlarmTaskManager.mTimer = new Timer();


        if (getIntent().hasExtra("set")) {
            Log.d(TAG, "Create From Widget");
            widget = true;
            getIntent().removeExtra("set");
        }

        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        animationIndex = prefs.getInt("DrawingIndex", 1);

        setupUI();

        int state = prefs.getInt("State", STOPPED);

        switch (state) {
            case RUNNING:
                Log.i(TAG, "RESUME, state RUNNING");
                // We are resuming the app while timers are (presumably) still active
                // We might not have access to any objects, since the app might have been killed in the meantime.

                long sessionTimeStamp = prefs.getLong("SessionTimeStamp", -1);
                long curTimerStamp = prefs.getLong("TimeStamp", -1);

                Log.i(TAG, "Resume while running: " + prefs.getLong("TimeStamp", -1));

                Date now = new Date();
                Date sessionEnd = new Date(sessionTimeStamp);
                Date curTimerEnd = new Date(curTimerStamp);

                // We still have timers running!
                if (sessionEnd.after(now)) {
                    // TODO:
                    Log.i(TAG, "Still have timers");

                    int sessionTimeLeft = (int) (sessionEnd.getTime() - now.getTime());
                    int curTimerLeft = (int) (curTimerEnd.getTime() - now.getTime());
                    int sessionDuration = prefs.getInt("SessionDuration", -1);

                    Log.i(TAG, "Session Time Left " + sessionTimeLeft);
                    //Log.i(TAG, "Cur Time Left " + curTimerLeft);
                    Log.i(TAG, "SessionDuration " + sessionDuration);

                    int timeElapsed = sessionDuration - sessionTimeLeft;

                    // RECREATE ALARMS if empty, but we are running.
                    // THEY WILL HAVE WRONG IDS.....
                    if (mAlarmTaskManager.getAlarmCount() == 0) {
                        Log.i(TAG, "Trying to recreate alarms");
                        mAlarmTaskManager.addAlarms(retrieveTimerList(), -timeElapsed);

                        // Resume ticker at correct position
                        // Get duration of current alarm
                        int curTimerDuration = mAlarmTaskManager.getCurrentAlarmDuration();
                        curTimerLeft = mAlarmTaskManager.getTotalDuration() - sessionTimeLeft;

                        Log.i(TAG, "Setting timer: " + curTimerLeft + " of " + curTimerDuration);
                        mAlarmTaskManager.timerResume(curTimerLeft, curTimerDuration);

                    } else {
                        mAlarmTaskManager.timerResume(curTimerLeft);

                    }


                    enterState(RUNNING);

                } else {
                    Log.i(TAG, "Resumed to RUNNING, but all timers are over");
                    mAlarmTaskManager.stopTicker();
                    loadLastTimers();
                    updateMainLabel(0);
                }

                break;

            case STOPPED:
                Log.i(TAG, "RESUME, state STOPPED");

                loadLastTimers();

                enterState(STOPPED);

                if (widget) {
                    if (prefs.getBoolean("SwitchTimeMode", false))
                        startVoiceRecognitionActivity();
                    else
                        showNumberPicker();
                    return;
                }


                updateMainLabel(0);

                break;

            case PAUSED:
                Log.i(TAG, "RESUME, state PAUSED");
                loadLastTimers();
                mAlarmTaskManager.restoreState();

                updateInterfaceWithTime(mAlarmTaskManager.getCurTimerLeftVal(), mAlarmTaskManager.getCurTimerDurationVal());
                updateMainLabel(mAlarmTaskManager.getCurTimerLeftVal());

                enterState(PAUSED);
                break;
        }
        widget = false;

    }

    private void loadLastTimers() {
        // Populate the AlarmManager with our last used timers
        mAlarmTaskManager.addAlarms(retrieveTimerList(), 0);
        updatePreviewLabel();
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

        // Save our settings
        SharedPreferences.Editor editor = prefs.edit();
        mAlarmTaskManager.saveState();
        mTimerAnimation.saveState(prefs);

        if (mAlarmTaskManager.mCurrentState == RUNNING) {
            Log.i(TAG, "Pause while running: " + new Date().getTime() + mAlarmTaskManager.getCurTimerLeftVal());
        }

        editor.apply();

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

        //unregisterReceiver(resetReceiver);
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
                switch (mAlarmTaskManager.mCurrentState) {
                    case RUNNING:
                        mAlarmTaskManager.timerPause();
                        break;
                    case PAUSED:
                        resumeTimer();
                        break;
                    case STOPPED:
                        // We are stopped and want to restore the last used timers.
                        startAdvancedAlarm(retrieveTimerList());
                        break;
                }
                break;

            case R.id.cancelButton:
                mAlarmTaskManager.stopAlarmsAndTicker();
                loadLastTimers();
                enterState(STOPPED);
                break;
        }
    }

    public void resumeTimer() {
        // How far have we elapsed?
        int sessionLeft = prefs.getInt("SessionTimeLeft", 0);
        int currentTimerLeft = prefs.getInt("CurrentTimeLeft", 0);
        int sessionDuration = mAlarmTaskManager.sessionDuration;

        int timeElapsed = sessionDuration - sessionLeft;

        // Setup the alarms
        mAlarmTaskManager.addAlarms(retrieveTimerList(), -timeElapsed);
        mAlarmTaskManager.timerResume(currentTimerLeft);
        mAlarmTaskManager.startAllAlarms();
    }

    @Override
    public boolean onKeyDown(int keycode, KeyEvent e) {
        //mAlarmTaskManager.mNM.cancelAll();
        if (keycode == KeyEvent.KEYCODE_MENU) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        return super.onKeyDown(keycode, e);
    }

    public void onSingleAlarmEnd() {
        updatePreviewLabel();
    }

    private void setLowProfile() {

        View rootView = getWindow().getDecorView();
        rootView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);

    }

    private void showNumberPicker() {
        Intent i = new Intent(this, SimpleNumberPicker.class);
        i.putExtra("times", lastTimes);
        startActivityForResult(i, NUMBER_PICK_REQUEST_CODE);
    }


    /**
     * Updates the time
     *
     * @param elapsed  the elapsed time of the current timer
     * @param duration the duration of the current timer
     */
    public void updateInterfaceWithTime(int elapsed, int duration) {
        updateMainLabel(elapsed);

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


    /**
     * Updates the text label with the given time
     *
     * @param time in milliseconds
     */
    public void updateMainLabel(int time) {
        if (time == 0) {
            time = mAlarmTaskManager.getCurTimerDurationVal();
        }

        int remainingTime = (int) (Math.ceil(((float) time) / 1000) * 1000);  // round to seconds
        mTimerLabel.setText(Time.time2hms(remainingTime));

    }

    private void updatePreviewLabel() {
        ArrayList<String> arr = makePreviewArray();
        Log.v(TAG, "Update preview label");

        String advTimeStringLeft = TextUtils.join("\n", arr);
        mAltLabel.setText(advTimeStringLeft);
    }

    private void updatePreviewLabelFromSettings() {

        ArrayList<String> arr = makePreviewArray();
        Log.v(TAG, "Update preview label");

        String advTimeStringLeft = TextUtils.join("\n", arr);
        mAltLabel.setText(advTimeStringLeft);
    }


    public void startSimpleAlarm(int[] numbers) {

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
        int mainTime = Time.msFromArray(numbers);
        tL.timers.add(new TimerList.Timer(mainTime, notificationUri, SessionType.REAL));

        saveTimerList(tL);
        startAdvancedAlarm(tL);

    }

    private void saveTimerList(TimerList tL) {
        Editor editor = prefs.edit();
        String ret = tL.getString();
        Log.v(TAG, "Saved timer string: " + ret);
        editor.putString("advTimeString", tL.getString());
        editor.apply();
    }

    private TimerList retrieveTimerList() {
        String prefString = prefs.getString("advTimeString", "120000#sys_def");
        TimerList tL = new TimerList(prefString);
        Log.v(TAG, "Got timer string: " + prefString + " from Settings");
        return tL;
    }

    public void startAdvancedAlarm(String advTimeString) {
        TimerList list = new TimerList(advTimeString);

        Log.v(TAG, "advString: " + advTimeString);
        Log.v(TAG, "advString2: " + list.getString());

        startAdvancedAlarm(list);
    }

    public void startAdvancedAlarm(TimerList list) {
        mAlarmTaskManager.addAlarms(list, 0);
        mAlarmTaskManager.startAll();
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
            String advTimeString = prefs.getString("advTimeString", "");

            if (advTimeString == null || advTimeString.length() == 0) {
                widget = false;
                return;
            }

            startAdvancedAlarm(advTimeString);
            updatePreviewLabel();

        } else {
            lastTimes = numbers;
            Log.v(TAG, "Saving simple time: " + Time.msFromArray(numbers));
            editor.putInt("LastSimpleTime", Time.msFromArray(numbers));
            startSimpleAlarm(numbers);
        }

        editor.commit();
        updatePreviewLabel();

    }


    /**
     * This only refers to the visual state of the application, used to manage
     * the view coming back into focus.
     *
     * @param state the visual state that is being entered
     */
    private void enterState(int state) {
        if (mAlarmTaskManager.mCurrentState != state) {

            // update preference for widget, notification

            if (LOG) Log.v(TAG, "From/to states: " + mAlarmTaskManager.mCurrentState + " " + state);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt("State", state);
            editor.apply();
            mAlarmTaskManager.mCurrentState = state;

        }

        switch (state) {
            case RUNNING:
                mSetButton.setVisibility(View.GONE);
                mCancelButton.setVisibility(View.VISIBLE);
                mPauseButton.setVisibility(View.VISIBLE);
                mPauseButton.setImageBitmap(mPauseBitmap);
                setButtonAlpha(127);
                break;
            case STOPPED:
                loadLastTimers();
                mPauseButton.setImageBitmap(mPlayBitmap);
                mCancelButton.setVisibility(View.GONE);
                mSetButton.setVisibility(View.VISIBLE);
                mAlarmTaskManager.clearTime();
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


    // Shortens the array to max three
    private ArrayList<String> makePreviewArray() {
        ArrayList<String> arr = new ArrayList<>();
        ArrayList<Integer> previewTimes = mAlarmTaskManager.getPreviewTimes();

        for (int i = 0; i < previewTimes.size(); i++) {
            if (i >= 2) {
                arr.add("...");
                break;
            }
            arr.add(Time.time2hms(previewTimes.get(i)));
        }
        return arr;
    }


    /**
     * Mostly used for the wakelock currently -- should be used for the visual components eventually
     */
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

        // We need to check if the
        if (key.equals("WakeLock")) {
            if (prefs.getBoolean("WakeLock", false))
                getWindow().addFlags(LayoutParams.FLAG_KEEP_SCREEN_ON);
            else
                getWindow().clearFlags(LayoutParams.FLAG_KEEP_SCREEN_ON);
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
                    editor.putString("advTimeString", complexTime);
                    editor.apply();
                    if (prefs.getBoolean("SpeakTime", false)) {
                        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {

                            @Override
                            public void onInit(int status) {
                                if (status == TextToSpeech.SUCCESS) {
                                    tts.speak(getString(R.string.adv_speech_recognized), TextToSpeech.QUEUE_ADD, null);
                                } else
                                    Log.e("error", "Initialization failed!");
                            }
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

    // receiver to get restart
    private final BroadcastReceiver alarmEndReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.v(TAG, "Received app alarm callback");
            Log.d(TAG, "id " + intent.getIntExtra("id", -1));

            mAlarmTaskManager.onAlarmEnd(intent.getIntExtra("id", -1));
            onSingleAlarmEnd();
        }
    };


}
