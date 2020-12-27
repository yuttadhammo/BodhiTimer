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
import android.app.NotificationManager;
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
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
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
import org.yuttadhammo.BodhiTimer.Service.ScheduleClient;
import org.yuttadhammo.BodhiTimer.SimpleNumberPicker.OnNNumberPickedListener;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

// import android.net.Uri;

/**
 * The main activity which shows the timer and allows the user to set the time
 *
 * @author Ralph Gootee (rgootee@gmail.com)
 */
public class TimerActivity extends AppCompatActivity implements OnClickListener, OnNNumberPickedListener, OnSharedPreferenceChangeListener {
    /**
     * All possible timer states
     */
    public final static int RUNNING = 0;
    public static final int STOPPED = 1;
    public static final int PAUSED = 2;

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
     * Update rate of the internal timer
     */
    public final static int TIMER_TIC = 100;

    /**
     * The timer's current state
     */
    public static int mCurrentState = -1;

    /**
     * The maximum time
     */
    private int mLastTime = 1800000;

    /**
     * The current timer time
     */
    private int mTime = 0;

    /**
     * To save having to traverse the view tree
     */
    private ImageButton mPauseButton, mCancelButton, mSetButton, mPrefButton;

    private TimerAnimation mTimerAnimation;
    private TextView mTimerLabel;
    private TextView mAltLabel;

    private Bitmap mPlayBitmap, mPauseBitmap;

    private AlarmManager mAlarmMgr;

    private static PendingIntent mPendingIntent;

    private AudioManager mAudioMgr;

    private SharedPreferences prefs;

    // for canceling notifications

    public NotificationManager mNM;

    private boolean widget;
    private boolean isPaused;

    private int[] lastTimes;

    private TimerActivity context;

    private int animationIndex;

    private ImageView blackView;

    private MediaPlayer prePlayer;

    private long timeStamp;

    public static final String BROADCAST_UPDATE = "org.yuttadhammo.BodhiTimer.ACTION_CLOCK_UPDATE";
    public static final String BROADCAST_STOP = "org.yuttadhammo.BodhiTimer.ACTION_CLOCK_CANCEL";

    private boolean invertColors = false;
    private String advTimeString = "";
    private String advTimeStringLeft = "";
    private boolean useAdvTime = false;
    private int advTimeIndex;

    private ScheduleClient scheduleClient;

    private TextToSpeech tts;

    /**
     * Called when the activity is first created.
     * { @inheritDoc}
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Create a new service client and bind our activity to this service
        scheduleClient = new ScheduleClient(this);
        scheduleClient.doBindService();

        tts = new TextToSpeech(this, null);

        Intent intent = new Intent(this, TimerReceiver.class);
        mPendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

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
        mAlarmMgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        mAudioMgr = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mNM = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);

        // get last times

        lastTimes = new int[3];

        prefs.registerOnSharedPreferenceChangeListener(this);

    }

    /**
     * { @inheritDoc}
     */
    @Override
    public void onPause() {
        super.onPause();
        isPaused = true; // tell gui timer to stop
        sendBroadcast(new Intent(BROADCAST_UPDATE)); // tell widgets to update

        unregisterReceiver(receiver);

        BitmapDrawable drawable = (BitmapDrawable) mTimerAnimation.getDrawable();
        if (drawable != null) {
            Bitmap bitmap = drawable.getBitmap();
            bitmap.recycle();
        }

        // Save our settings
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("LastTime", mLastTime);
        editor.putInt("CurrentTime", mTime);
        editor.putInt("DrawingIndex", mTimerAnimation.getIndex());
        editor.putInt("State", mCurrentState);

        editor.putInt("last_hour", lastTimes[0]);
        editor.putInt("last_min", lastTimes[1]);
        editor.putInt("last_sec", lastTimes[2]);

        switch (mCurrentState) {

            case RUNNING:
                Log.i(TAG, "pause while running: " + new Date().getTime() + mTime);
                break;
            case STOPPED:
                cancelNotification();
            case PAUSED:
                editor.putLong("TimeStamp", 1);
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

        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        IntentFilter filter = new IntentFilter();
        filter.addAction(TimerReceiver.BROADCAST_RESET);
        registerReceiver(receiver, filter);

        isPaused = false;
        sendBroadcast(new Intent(BROADCAST_STOP)); // tell widgets to stop updating
        mTimer = new Timer();

        lastTimes[0] = prefs.getInt("last_hour", 0);
        lastTimes[1] = prefs.getInt("last_min", 0);
        lastTimes[2] = prefs.getInt("last_sec", 0);

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
        mLastTime = prefs.getInt("LastTime", 1800000);

        Log.d(TAG, "Last Time: " + mLastTime);
        int state = prefs.getInt("State", STOPPED);
        if (state == STOPPED)
            cancelNotification();

        checkWhetherAdvTime(false);

        switch (state) {
            case RUNNING:
                Log.i(TAG, "Resume while running: " + prefs.getLong("TimeStamp", -1));
                timeStamp = prefs.getLong("TimeStamp", -1);

                Date now = new Date();
                Date then = new Date(timeStamp);

                // We still have a timer running!
                if (then.after(now)) {
                    if (LOG) Log.i(TAG, "Still have a timer");
                    mTime = (int) (then.getTime() - now.getTime());

                    enterState(RUNNING);

                    doTick();

                    // All finished
                } else {
                    cancelNotification();
                    timerStop();
                }
                break;

            case STOPPED:
                mNM.cancelAll();
                timerStop();
                if (widget) {
                    if (prefs.getBoolean("SwitchTimeMode", false))
                        startVoiceRecognitionActivity();
                    else
                        showNumberPicker();
                    return;
                }
                break;

            case PAUSED:
                mTime = prefs.getInt("CurrentTime", 0);
                onUpdateTime();
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

        if (scheduleClient != null)
            scheduleClient.doUnbindService();
        super.onStop();
    }

    /**
     * {@inheritDoc}
     */
    public void onClick(View v) {

        setLowProfile();

        if (mCurrentState == STOPPED) {
            if (prePlayer != null) {
                prePlayer.release();
            }

            //cancelNotification();
        }

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
                switch (mCurrentState) {
                    case RUNNING:
                        timerPause();
                        break;
                    case PAUSED:
                        timerResume();
                        break;
                    case STOPPED:
                        playPreSound();
                        checkWhetherAdvTime(true);
                        timerStart(mLastTime, true);
                        break;
                }
                break;

            case R.id.cancelButton:

                stopAlarmTimer();

                // We need to be careful to not cancel timers
                // that are not running (e.g. if we're paused)
                switch (mCurrentState) {
                    case RUNNING:
                        if (prePlayer != null) {
                            prePlayer.release();
                        }
                        cancelNotification();
                        timerStop();
                        break;
                    case PAUSED:
                        clearTime();
                        enterState(STOPPED);
                        break;
                }
                checkWhetherAdvTime(true);

                break;
        }
    }

    @Override
    public boolean onKeyDown(int keycode, KeyEvent e) {
        mNM.cancelAll();
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
        Intent i = new Intent(this, SimpleNumberPicker.class);
        i.putExtra("times", lastTimes);
        startActivityForResult(i, NUMBERPICK_REQUEST_CODE);
    }


    /**
     * Updates the time
     */
    public void onUpdateTime() {
        if (mCurrentState == STOPPED)
            mTime = 0;
        updateLabel(mTime);
        if (animationIndex != 0) {
            blackView.setVisibility(View.GONE);
            mTimerAnimation.updateImage(mTime, mLastTime);
        } else {
            float p = (mLastTime != 0) ? (mTime / (float) mLastTime) : 0;
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
            time = mLastTime;
        }

        int rtime = (int) (Math.ceil(((float) time) / 1000) * 1000);  // round to seconds

        //Log.v(TAG,"rounding time: "+time+" "+rtime);

        mTimerLabel.setText(TimerUtils.time2hms(rtime));

        //mTimerLabel2.setText(str[1]);
    }


    /**
     * Callback for the number picker dialog
     */
    public void onNumbersPicked(int[] number) {
        if (number == null) {
            widget = false;
            return;
        }

        Editor editor = prefs.edit();

        // advanced timer - 0 will be -1

        if (number[0] == -1) {
            advTimeString = prefs.getString("advTimeString", "");
            if (advTimeString == null || advTimeString.length() == 0) {
                widget = false;
                return;
            }

            useAdvTime = true;

            String[] advTime = advTimeString.split("\\^");

            String[] thisAdvTime = advTime[0].split("#"); // will be of format timeInMs#pathToSound
            number = TimerUtils.time2Array(Integer.parseInt(thisAdvTime[0]));

            //editor.putString("NotificationUri",thisAdvTime[1]);

            // switch timer to use advanced time

            editor.putBoolean("useAdvTime", true);

            // set index to 1, because we're doing the first one already

            editor.putInt("advTimeIndex", 1);


            advTimeStringLeft = "";

            ArrayList<String> arr = makeAdvLeftArray(advTime);

            advTimeStringLeft = TextUtils.join("\n", arr);
            mAltLabel.setText(advTimeStringLeft);

        } else {
            if (prefs.getBoolean("useAdvTime", false)) {
                editor.putBoolean("useAdvTime", false);
            }
        }

        int hour = number[0];
        int min = number[1];
        int sec = number[2];

        mLastTime = hour * 60 * 60 * 1000 + min * 60 * 1000 + sec * 1000;

        mTime = mLastTime;
        Log.v(TAG, "Picked time: " + mLastTime);

        onUpdateTime();

        lastTimes = new int[3];

        lastTimes[0] = hour;
        lastTimes[1] = min;
        lastTimes[2] = sec;

        // Save last set times in preferences
        editor.putInt("LastTime", mLastTime);
        editor.putInt("last_hour", lastTimes[0]);
        editor.putInt("last_min", lastTimes[1]);
        editor.putInt("last_sec", lastTimes[2]);
        editor.apply();

        // Check to make sure the phone isn't set to silent
        boolean silent = (mAudioMgr.getRingerMode() == AudioManager.RINGER_MODE_SILENT);
        boolean vibrate = prefs.getBoolean("Vibrate", false);
        String noise = prefs.getString("NotificationUri", "");
        boolean nag = prefs.getBoolean("NagSilent", true);

        // If the conditions are _just_ right show a nag screen
        if (nag && silent && (noise.length() > 0 || vibrate)) {
            showDialog(ALERT_DIALOG);
        }

        playPreSound();
        timerStart(mLastTime, true);

        if (widget) {
            sendBroadcast(new Intent(BROADCAST_UPDATE)); // tell widgets to update
            finish();
        }
    }


    /**
     * This only refers to the visual state of the application, used to manage
     * the view coming back into focus.
     *
     * @param state the visual state that is being entered
     */
    private void enterState(int state) {
        if (mCurrentState != state) {

            // update preference for widget, notification

            if (LOG) Log.v(TAG, "From/to states: " + mCurrentState + " " + state);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt("State", state);
            editor.apply();
            mCurrentState = state;

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
                mNM.cancelAll();
                mPauseButton.setImageBitmap(mPlayBitmap);
                mCancelButton.setVisibility(View.GONE);
                mSetButton.setVisibility(View.VISIBLE);
                clearTime();
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

    /**
     * Starts the timer at the given time
     *
     * @param time    with which to count down
     * @param service whether or not to start the service as well
     */
    private void timerStart(int time, boolean service) {
        if (LOG) Log.v(TAG, "Starting the timer: " + time);

        enterState(RUNNING);

        mTime = time;

        timeStamp = new Date().getTime() + mTime;

        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong("TimeStamp", timeStamp);
        editor.apply();

        if (useAdvTime) {
            String[] advTime = advTimeString.split("\\^");

            ArrayList<String> arr = new ArrayList<String>();

            advTimeStringLeft = "";

            advTimeIndex = prefs.getInt("advTimeIndex", 1);

            Log.d(TAG, "time index: " + advTimeIndex);

            if (advTimeIndex < advTime.length) {

                arr = makeAdvLeftArray(advTime);
            }
            advTimeStringLeft = TextUtils.join("\n", arr);
        }

        // Start external service
        if (service) {
            if (LOG)
                Log.v(TAG, "ALARM: Starting the timer service: " + TimerUtils.time2humanStr(context, mTime));

            scheduleClient.setAlarmForNotification(mTime);

        }

        mTimer.schedule(
                new TimerTask() {
                    public void run() {
                        if (mHandler != null) {
                            mHandler.sendEmptyMessage(0);
                        }
                    }
                },
                TIMER_TIC
        );


    }

    /**
     * Stops the timer
     */
    private void timerStop() {
        if (LOG) Log.v(TAG, "Timer stopped");

        clearTime();
        stopAlarmTimer();

        // Stop our timer service
        enterState(STOPPED);

    }

    /**
     * Resume the time after being paused
     */
    private void timerResume() {
        if (LOG) Log.v(TAG, "Resuming the timer...");

        timerStart(mTime, true);
        enterState(RUNNING);
    }

    /**
     * Pause the timer and stop the timer service
     */
    private void timerPause() {
        if (LOG) Log.v(TAG, "Pausing the timer...");

        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("CurrentTime", mTime);
        editor.apply();

        stopAlarmTimer();

        enterState(PAUSED);
    }

    /**
     * Clears the time, sets the image and label to default
     */
    private void clearTime() {
        mTime = 0;

        onUpdateTime();
    }


    /**
     * Cancels the alarm portion of the timer
     */
    private void stopAlarmTimer() {
        if (LOG) Log.v(TAG, "Stopping the alarm timer ...");
        mAlarmMgr.cancel(mPendingIntent);
        mNM.cancelAll();
    }


    /**
     * plays a sound before timer starts
     */
    private void playPreSound() {
        String uriString = prefs.getString("PreSoundUri", "");

        switch (uriString) {
            case "system":
                uriString = prefs.getString("PreSystemUri", "");
                break;
            case "file":
                uriString = prefs.getString("PreFileUri", "");
                break;
            case "tts":
                uriString = "";
                final String ttsString = prefs.getString("tts_string_pre", context.getString(R.string.timer_done));
                tts.speak(ttsString, TextToSpeech.QUEUE_ADD, null);
                break;
        }

        if (uriString.equals(""))
            return;

        Log.v(TAG, "preplay uri: " + uriString);

        try {
            prePlayer = new MediaPlayer();
            Uri uri = Uri.parse(uriString);

            int currVolume = prefs.getInt("tone_volume", 0);
            if (currVolume != 0) {
                float log1 = (float) (Math.log(100 - currVolume) / Math.log(100));
                prePlayer.setVolume(1 - log1, 1 - log1);
            }
            prePlayer.setDataSource(context, uri);
            prePlayer.prepare();
            prePlayer.setLooping(false);
            prePlayer.setOnCompletionListener(new OnCompletionListener() {

                @Override
                public void onCompletion(MediaPlayer mp) {
                    // TODO Auto-generated method stub
                    mp.release();
                }

            });
            prePlayer.start();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void checkWhetherAdvTime(boolean reset) {
        if (!prefs.getBoolean("useAdvTime", false)) {
            mAltLabel.setText("");
            return;
        }

        advTimeString = prefs.getString("advTimeString", "");
        if (advTimeString == null || advTimeString.length() == 0)
            return;

        String[] advTime = advTimeString.split("\\^");

        String[] thisAdvTime = advTime[0].split("#"); // will be of format timeInMs#pathToSound
        int[] number = TimerUtils.time2Array(Integer.parseInt(thisAdvTime[0]));

        Editor editor = prefs.edit();

        // set index to 1, because we're doing the first one already

        if (reset) {
            editor.putInt("advTimeIndex", 1);
            advTimeIndex = 1;
        } else
            advTimeIndex = prefs.getInt("advTimeIndex", 1);

        advTimeStringLeft = "";

        ArrayList<String> arr = makeAdvLeftArray(advTime);

        advTimeStringLeft = TextUtils.join("\n", arr);

        mAltLabel.setText(advTimeStringLeft);

        int hour = number[0];
        int min = number[1];
        int sec = number[2];

        mLastTime = hour * 60 * 60 * 1000 + min * 60 * 1000 + sec * 1000;

        mTime = mLastTime;
        Log.v(TAG, "Picked time: " + mLastTime);

        onUpdateTime();

        lastTimes = new int[3];

        lastTimes[0] = hour;
        lastTimes[1] = min;
        lastTimes[2] = sec;

        // put last set time to prefs

        editor.putInt("LastTime", mLastTime);
        editor.putInt("last_hour", lastTimes[0]);
        editor.putInt("last_min", lastTimes[1]);
        editor.putInt("last_sec", lastTimes[2]);
        editor.apply();
    }

    private ArrayList<String> makeAdvLeftArray(String[] advTime) {
        ArrayList<String> arr = new ArrayList<String>();
        for (int i = advTimeIndex; i < advTime.length; i++) {
            if (arr.size() >= 2 && advTime.length - i > 1) {
                arr.add("...");
                break;
            }
            arr.add(TimerUtils.time2hms(Integer.parseInt(advTime[i].split("#")[0])));
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

    private Timer mTimer;

    private void doTick() {
        //Log.w(TAG,"ticking");

        if (mCurrentState != RUNNING || isPaused)
            return;

        Date now = new Date();
        Date then = new Date(timeStamp);

        mTime = (int) (then.getTime() - now.getTime());

        if (mTime <= 0) {

            Log.e(TAG, "Time up");

            timerStop();

            // Update the time
        } else {
            // Internal thread to properly update the GUI
            mTimer.schedule(new TimerTask() {
                                public void run() {
                                    if (mHandler != null) {
                                        mHandler.sendEmptyMessage(0);
                                    }
                                }
                            },
                    TIMER_TIC
            );
        }
    }

    /**
     * Handler for the message from the timer service
     */
    private Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            onUpdateTime();
            doTick();
        }
    };

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
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://market.android.com/details?id=com.google.android.voicesearch"));
            startActivity(browserIntent);

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
        }
        else  if (requestCode == VOICE_RECOGNITION_REQUEST_CODE && resultCode == RESULT_OK) {
            // Fill the list view with the strings the recognizer thought it could have heard
            ArrayList<String> matches = data.getStringArrayListExtra(
                    RecognizerIntent.EXTRA_RESULTS);
            for (String match : matches) {

                match = match.toLowerCase();
                Log.d(TAG, "Got speech: " + match);

                if (match.contains(TimerUtils.TIME_SEPARATOR)) {
                    String complexTime = TimerUtils.str2complexTimeString(this, match);
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
                    final int speechTime = TimerUtils.str2timeString(this, match);
                    if (speechTime != 0) {
                        int[] values = TimerUtils.time2Array(speechTime);
                        Toast.makeText(this, String.format(getString(R.string.speech_recognized), TimerUtils.time2humanStr(this, speechTime)), Toast.LENGTH_SHORT).show();
                        if (prefs.getBoolean("SpeakTime", false)) {
                            Log.d(TAG, "Speaking time");
                            tts.speak(String.format(getString(R.string.speech_recognized), TimerUtils.time2humanStr(context, speechTime)), TextToSpeech.QUEUE_ADD, null);
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

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            timerStop();
            checkWhetherAdvTime(false);
            Log.d(TAG, "received: " + intent.getIntExtra("time", 999));
            if (intent.getBooleanExtra("stop", false))
                return;

            mLastTime = intent.getIntExtra("time", mLastTime);
            mTime = mLastTime;
            timerStart(mTime, true);
        }
    };

}
