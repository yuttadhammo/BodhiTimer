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

import android.app.AlarmManager;
import android.app.PendingIntent;
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
import android.os.SystemClock;
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
import org.yuttadhammo.BodhiTimer.Service.TimerList;
import org.yuttadhammo.BodhiTimer.SimpleNumberPicker.OnNNumberPickedListener;
import org.yuttadhammo.BodhiTimer.Util.Time;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Timer;

import static org.yuttadhammo.BodhiTimer.Service.TimerState.PAUSED;
import static org.yuttadhammo.BodhiTimer.Service.TimerState.RUNNING;
import static org.yuttadhammo.BodhiTimer.Service.TimerState.STOPPED;
import static org.yuttadhammo.BodhiTimer.Util.BroadcastTypes.BROADCAST_END;
import static org.yuttadhammo.BodhiTimer.Util.BroadcastTypes.BROADCAST_RESET;
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
     * Macros for our dialogs
     */
    private final static int ALERT_DIALOG = 1;

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
    private static PendingIntent mPendingIntent;

    private AudioManager mAudioMgr;

    private SharedPreferences prefs;

    // for canceling notifications


    private boolean widget;

    private int[] lastTimes;

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
        super.onCreate(savedInstanceState);

        // Setup a new AlarmTaskManager
        mAlarmTaskManager = new AlarmTaskManager(this);

        setupListener();

        tts = new TextToSpeech(this, null);

        //Intent intent = new Intent(this, TimerReceiver.class);
        //mPendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        setContentView(R.layout.main);
        //RelativeLayout main = (RelativeLayout)findViewById(R.id.mainLayout);

        context = this;

        mCancelButton = (ImageButton) findViewById(R.id.cancelButton);
        mCancelButton.setOnClickListener(this);

        mSetButton = (ImageButton) findViewById(R.id.setButton);
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

        mPauseButton = (ImageButton) findViewById(R.id.pauseButton);
        mPauseButton.setOnClickListener(this);

        mPrefButton = (ImageButton) findViewById(R.id.prefButton);
        mPrefButton.setOnClickListener(this);

        mPauseBitmap = BitmapFactory.decodeResource(
                getResources(), R.drawable.pause);

        mPlayBitmap = BitmapFactory.decodeResource(
                getResources(), R.drawable.play);

        mTimerLabel = (TextView) findViewById(R.id.text_top);
        mAltLabel = (TextView) findViewById(R.id.text_alt);

        mTimerAnimation = (TimerAnimation) findViewById(R.id.mainImage);
        mTimerAnimation.setOnClickListener(this);

        blackView = (ImageView) findViewById(R.id.black);

        // Store some useful values
        prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        mAudioMgr = (AudioManager) getSystemService(Context.AUDIO_SERVICE);


        // get last times

        lastTimes = new int[3];

        prefs.registerOnSharedPreferenceChangeListener(this);

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
     * { @inheritDoc}
     */
    @Override
    public void onPause() {
        super.onPause();
        mAlarmTaskManager.isPaused = true; // tell gui timer to stop
        sendBroadcast(new Intent(BROADCAST_UPDATE)); // tell widgets to update

        //unregisterReceiver(resetReceiver);
        unregisterReceiver(alarmEndReceiver);

        BitmapDrawable drawable = (BitmapDrawable) mTimerAnimation.getDrawable();
        if (drawable != null) {
            Bitmap bitmap = drawable.getBitmap();
            bitmap.recycle();
        }

        // Save our settings
        SharedPreferences.Editor editor = prefs.edit();
        mAlarmTaskManager.saveState();
        mTimerAnimation.saveState(prefs);

        switch (mAlarmTaskManager.mCurrentState) {

            case RUNNING:
                Log.i(TAG, "pause while running: " + new Date().getTime() + mAlarmTaskManager.getCurElapsedVal());
                break;
            case STOPPED:
                cancelNotification();
            case PAUSED:
                editor.putLong("TimeStamp", 1);
                break;
            default:
                break;
        }

        editor.apply();

    }

    @Override
    public void onDestroy() {
        //Close the Text to Speech Library
        if (tts != null) {

            tts.stop();
            tts.shutdown();
            Log.d(TAG, "TTSService Destroyed");
        }
        super.onDestroy();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onResume() {
        super.onResume();

        Log.i(TAG, "RESUME");

        setVolumeControlStream(AudioManager.STREAM_MUSIC);

//        IntentFilter filter = new IntentFilter();
//        filter.addAction(BROADCAST_RESET);
//        registerReceiver(resetReceiver, filter);

        IntentFilter filter2 = new IntentFilter();
        filter2.addAction(BROADCAST_END);
        registerReceiver(alarmEndReceiver, filter2);

        mAlarmTaskManager.isPaused = false;
        sendBroadcast(new Intent(BROADCAST_STOP)); // tell widgets to stop updating
        mAlarmTaskManager.mTimer = new Timer();

        lastTimes = Time.time2Array(prefs.getInt("LastTime", 0));

        if (getIntent().hasExtra("set")) {
            Log.d(TAG, "Create From Widget");
            widget = true;
            getIntent().removeExtra("set");
        }

        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        animationIndex = prefs.getInt("DrawingIndex", 1);

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

        // check the timestamp from the last update and start the timer.
        // assumes the data has already been loaded?
        int dur = prefs.getInt("LastTime", 1800000);
        mAlarmTaskManager.setDuration(dur);

        Log.d(TAG, "Last Time: " + dur);
        int state = prefs.getInt("State", STOPPED);
        if (state == STOPPED)
            cancelNotification();


        switch (state) {
            case RUNNING:
                Log.i(TAG, "Resume while running: " + prefs.getLong("TimeStamp", -1));
                mAlarmTaskManager.timeStamp = prefs.getLong("TimeStamp", -1);

                Date now = new Date();
                Date then = new Date(mAlarmTaskManager.timeStamp);

                // We still have a timer running!
                if (then.after(now)) {
                    if (LOG) Log.i(TAG, "Still have a timer");
                    mAlarmTaskManager.setCurElapsed((int) (then.getTime() - now.getTime()));

                    enterState(RUNNING);

                    mAlarmTaskManager.doTick();

                    // All finished
                } else {
                    cancelNotification();
                    mAlarmTaskManager.tickerStop();
                }
                break;

            case STOPPED:
                mAlarmTaskManager.mNM.cancelAll();
                mAlarmTaskManager.tickerStop();
                if (widget) {
                    if (prefs.getBoolean("SwitchTimeMode", false))
                        startVoiceRecognitionActivity();
                    else
                        showNumberPicker();
                    return;
                }
                break;

            case PAUSED:
                int curTime = prefs.getInt("CurrentTime", 0);
                mAlarmTaskManager.setCurElapsed(curTime);
                // FIXME
                updateInterfaceWithTime(curTime, 0);
                enterState(PAUSED);
                break;
        }
        widget = false;
    }

    @Override
    protected void onStop() {
        // When our activity is stopped ensure we also stop the connection to the service
        // this stops us leaking our activity into the system *bad*
        Log.d(TAG, "service stopped");

        super.onStop();
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
                        mAlarmTaskManager.timerResume();
                        break;
                    case STOPPED:
                        // We are stopped and want to restore the last used timers.
                        startAdvancedAlarm(retrieveTimerList());
                        break;
                }
                break;

            case R.id.cancelButton:
                mAlarmTaskManager.stopAlarmsAndTicker();
                enterState(STOPPED);
                break;
        }
    }

    @Override
    public boolean onKeyDown(int keycode, KeyEvent e) {
        mAlarmTaskManager.mNM.cancelAll();
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
        startActivityForResult(i, NUMBERPICK_REQUEST_CODE);
    }


    /**
     * Updates the time
     * @param elapsed
     * @param duration
     */
    public void updateInterfaceWithTime(int elapsed, int duration) {
        updateLabel(elapsed);

        if (animationIndex != 0) {
            blackView.setVisibility(View.GONE);
            mTimerAnimation.updateImage(elapsed, duration);
        } else {
            float p = (duration != 0) ? (elapsed / (float) duration) : 0;
            int alpha = Math.round(255 * p);
            alpha = Math.min(alpha, 255);

            String alphas = Integer.toHexString(alpha);
            alphas = alphas.length() == 1 ? "0" + alphas : alphas;

            String colors = "#" + alphas + (invertColors ? "FFFFFF" : "000000");

            int color = Color.parseColor(colors);
            blackView.setBackgroundColor(color);
            blackView.setVisibility(View.VISIBLE);
        }
    }


    /**
     * Updates the text label with the given time
     *
     * @param time in milliseconds
     */
    public void updateLabel(int time) {
        if (time == 0) {
            time = mAlarmTaskManager.getCurDurationVal();
        }

        int rtime = (int) (Math.ceil(((float) time) / 1000) * 1000);  // round to seconds

        mTimerLabel.setText(Time.time2hms(rtime));

    }


    public void startSimpleAlarm(int[] numbers) {

        int prepTime = prefs.getInt("preparationTime", 0) * 1000;
        String preUriString = prefs.getString("PreSoundUri", "");

        TimerList tL = new TimerList();

        // Add a preparatory timer
        if (prepTime > 0 && !preUriString.equals("")) {

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
        int offset = 0;

        for (TimerList.Timer timer: list.timers) {
            int duration = timer.duration;

            mAlarmTaskManager.addAlarmWithUri(offset, duration, timer.uri, timer.sessionType);
            offset += duration;
        }

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
            editor.putInt("LastSimpleTime", Time.msFromArray(numbers));
            startSimpleAlarm(numbers);
        }

        editor.commit();
        updatePreviewLabel();

    }

    private void updatePreviewLabel() {

        // FIXME; If stopped: show last used....
        ArrayList<String> arr = makePreviewArray();
        if (LOG) Log.v(TAG, "Update preview label");

        String advTimeStringLeft = TextUtils.join("\n", arr);
        mAltLabel.setText(advTimeStringLeft);
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
                mAlarmTaskManager.mNM.cancelAll();
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
        mPauseButton.setAlpha(i);
        mCancelButton.setAlpha(i);
        mPrefButton.setAlpha(i);
    }


    // Shortens the array to max three
    private ArrayList<String> makePreviewArray() {
        ArrayList<String> arr = new ArrayList<String>();
        ArrayList<Integer> previewTimes = mAlarmTaskManager.getPreviewTimes();

        for (int i = 0; i < previewTimes.size(); i++) {
            if (i >= 2) {
                arr.add("...");
                break;
            }
            arr.add(Time.time2hms(previewTimes.get(i) ));
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

    private void cancelNotification() {
        // Create intent for cancelling the notification
        Intent intent = new Intent(this, TimerReceiver.class);
        intent.setAction(TimerReceiver.CANCEL_NOTIFICATION);

        // Cancel the pending cancellation and create a new one
        PendingIntent pendingCancelIntent =
                PendingIntent.getBroadcast(this, 0, intent,
                        PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager alarmMgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alarmMgr.set(AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime(),
                pendingCancelIntent);

    }


    private int VOICE_RECOGNITION_REQUEST_CODE = 1234;

    private int NUMBERPICK_REQUEST_CODE = 5678;

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

        if (requestCode == NUMBERPICK_REQUEST_CODE && resultCode == RESULT_OK) {
            int[] values = data.getIntArrayExtra("times");

            onNumbersPicked(values);
            if (widget) {
                finish();
            }
        } else if (requestCode == VOICE_RECOGNITION_REQUEST_CODE && resultCode == RESULT_OK) {
            // Fill the list view with the strings the recognizer thought it could have heard
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
                                        Log.e("error", "Initilization Failed!");
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

        widget = false;

        super.onActivityResult(requestCode, resultCode, data);
    }

    // receiver to get restart
    private BroadcastReceiver alarmEndReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //mAlarmTaskManager
            Log.d(TAG, "id " + intent.getIntExtra("id", 1212));
            mAlarmTaskManager.onAlarmEnd(intent.getIntExtra("id", 1212));
            onSingleAlarmEnd();
        }
    };

}
