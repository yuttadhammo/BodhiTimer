package org.yuttadhammo.BodhiTimer.Service;

import android.app.Application;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.preference.PreferenceManager;

import org.yuttadhammo.BodhiTimer.Const.BroadcastTypes;
import org.yuttadhammo.BodhiTimer.Const.SessionTypes;
import org.yuttadhammo.BodhiTimer.Util.Time;

import java.util.ArrayList;
import java.util.Date;
import java.util.Stack;
import java.util.Timer;
import java.util.TimerTask;

import static org.yuttadhammo.BodhiTimer.Const.TimerState.PAUSED;
import static org.yuttadhammo.BodhiTimer.Const.TimerState.RUNNING;
import static org.yuttadhammo.BodhiTimer.Const.TimerState.STOPPED;
import static org.yuttadhammo.BodhiTimer.Const.BroadcastTypes.BROADCAST_RESET;

@SuppressWarnings("UnnecessaryBoxing")
public class AlarmTaskManager extends AndroidViewModel {
    private final String TAG = AlarmTaskManager.class.getSimpleName();

    /**
     * Update rate of the internal timer
     */
    public final static int TIMER_TIC = 100;

    public final Timer mTimer = new Timer();

    private final Stack<AlarmTask> alarms;
    private int lastId = 0;

    public boolean appIsPaused;
    
    // Data
    public long timeStamp;
    private long sessionTimeStamp;
    public int sessionDuration;
    private int sessionTimeLeft;

    final Application mApp;

    // Live Data
    private final MutableLiveData<Integer> currentTimerLeft = new MutableLiveData<>();
    private final MutableLiveData<Integer> currentTimerDuration = new MutableLiveData<>();
    private final MutableLiveData<String> timerText = new MutableLiveData<>();
    private final MutableLiveData<String> previewText = new MutableLiveData<>();
    private final MutableLiveData<Integer> mIndex = new MutableLiveData<>();
    private final MutableLiveData<Integer> mCurrentState = new MutableLiveData<>();

    private int lastTextGenerated;


    // Accessors
    public LiveData<Integer> getCurTimerLeft() {
        return currentTimerLeft;
    }

    public LiveData<Integer> getCurTimerDuration() {
        return currentTimerDuration;
    }

    public LiveData<Integer> getCurrentState() {
        return mCurrentState;
    }

    public void setCurrentState(Integer newState) {

            Log.v(TAG, "Entering state: " + newState);

            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt("State", newState);
            editor.apply();

            mCurrentState.setValue(newState);


    }

    public LiveData<String> getTimerText() {
        return (LiveData<String>) timerText;
    }

    public LiveData<String> getPreviewText() {
        return (LiveData<String>) previewText;
    }


    public Integer getCurTimerDurationVal() {
        return currentTimerDuration.getValue();
    }

    public Integer getCurTimerLeftVal() {
        return currentTimerLeft.getValue();
    }

    public Integer getIndexVal() {
        return mIndex.getValue();
    }

    public void setCurTimerDuration(int newDuration) {
        currentTimerDuration.setValue(Integer.valueOf(newDuration));
    }

    public void setCurTimerLeft(int newElapsed) {
        currentTimerLeft.setValue(Integer.valueOf(newElapsed));
    }

    public void setIndex(int newIndex) {
        mIndex.setValue(Integer.valueOf(newIndex));
    }


    // TODO: These need to be handled outside
    public final NotificationManager mNM;
    private final SharedPreferences prefs;


    public AlarmTaskManager(Application app) {
        super(app);

        mApp = app;

        alarms = new Stack<>();
        updatePreviewText();

        mNM = (NotificationManager) app.getSystemService(Context.NOTIFICATION_SERVICE);
        prefs = PreferenceManager.getDefaultSharedPreferences(app.getApplicationContext());

        restoreState();

        switch (mCurrentState.getValue()) {

            case RUNNING:
                Log.i(TAG, "CREATE, state RUNNING");

                // We are resuming the app while timers are (presumably) still active
                long sessionTimeStamp = prefs.getLong("SessionTimeStamp", -1);
                long curTimerStamp = prefs.getLong("TimeStamp", -1);

                Date now = new Date();
                Date sessionEnd = new Date(sessionTimeStamp);
                Date curTimerEnd = new Date(curTimerStamp);

                // We still have timers running!
                if (sessionEnd.after(now)) {
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
                    Log.i(TAG, "Trying to recreate alarms");
                    loadLastTimers(-timeElapsed);

                    // Resume ticker at correct position
                    // Get duration of current alarm
                    int curTimerDuration = getCurrentAlarmDuration();

                    Log.i(TAG, "Setting timer: " + curTimerLeft + " of " + curTimerDuration);
                    timerResume(curTimerLeft, curTimerDuration);

                } else {
                    Log.i(TAG, "Resumed to RUNNING, but all timers are over");
                    loadLastTimers();
                }

            case STOPPED:
                Log.i(TAG, "CREATE, state STOPPED");
                loadLastTimers();

                break;


            case PAUSED:
                Log.i(TAG, "CREATE, state PAUSED");

                int sessionLeft = prefs.getInt("SessionTimeLeft", 0);
                int timeElapsed = sessionDuration - sessionLeft;

                // Setup the alarms
                loadLastTimers(-timeElapsed);

                break;
        }

    }

    @Override
    protected void onCleared() {
        super.onCleared();
        Log.i(TAG, "View model cleared");
        saveState();
    }

    public void saveState() {
        SharedPreferences.Editor editor = prefs.edit();

        editor.putInt("CurrentTimerDuration", getCurTimerDurationVal());
        editor.putInt("CurrentTimeLeft", getCurTimerLeftVal());
        editor.putInt("State", mCurrentState.getValue());
        editor.putInt("SessionDuration", sessionDuration);
        editor.putInt("SessionTimeLeft", sessionTimeLeft);

        editor.apply();
    }

    public void restoreState() {

        setCurTimerDuration(prefs.getInt("CurrentTimerDuration", 0));
        setCurTimerLeft(prefs.getInt("CurrentTimeLeft", 0));
        sessionDuration = prefs.getInt("SessionDuration", 0);
        sessionTimeLeft = prefs.getInt("SessionTimeLeft", 0);

        mCurrentState.setValue(prefs.getInt("State", 0));

    }


    public void loadLastTimers() {
        loadLastTimers(0);
    }

    public void loadLastTimers(int offset) {
        // Populate the AlarmManager with our last used timers
        addAlarms(retrieveTimerList(), offset);
    }

    /**
     * Show an alarm for a certain date when the alarm is called it will pop up a notification
     */
    public AlarmTask addAlarmWithUri(int offset, int duration, String uri, SessionTypes sessionType) {

        Log.i(TAG, "Creating new alarm task, uri " + uri + " type: " + sessionType + " due in " + (duration + offset));
        AlarmTask alarm = new AlarmTask(mApp.getApplicationContext(), offset, duration);
        alarm.setUri(uri);
        alarm.setSessionType(sessionType);

        lastId++;

        alarm.setId(lastId);

        alarms.push(alarm);

        updateTimerText();
        updatePreviewText();

        return alarm;
    }

    public void addAlarms(TimerList list, int offset) {
        // If adding a complete list, clear all previous alarms
        cancelAllAlarms(true);

        int totalDuration = 0;
        lastId = 0;

        for (TimerList.Timer timer : list.timers) {
            int duration = timer.duration;

            // Don't add elapsed timers, but increase the id,
            // to make sure that on App resume the alarms have the correct id.
            if ((duration + offset) < 0) {
                lastId++;
                continue;
            }

            addAlarmWithUri(offset, duration, timer.uri, timer.sessionType);
            offset += duration;
            totalDuration += totalDuration;
        }

        resetCurrentAlarm();

    }

    public void startAllAlarms() {
        for (AlarmTask alarm : alarms) {
            alarm.run();
        }
    }

    public void startAll() {
        int sessionDur = 0;

        for (AlarmTask alarm : alarms) {
            alarm.run();
            sessionDur += alarm.getDuration();
        }

        int dur = getCurTimerDurationVal();
        sessionDuration = sessionDur;
        setSessionTimeStamp(sessionDur);

        startTicker(dur);
        Log.v(TAG, "Started ticker & timer, first duration: " + dur);
    }


    private void resetCurrentAlarm() {
        int dur = getCurrentAlarmDuration();
        setCurTimerDuration(dur);

        if (mCurrentState.getValue() != PAUSED) {
            setCurTimerLeft(dur);
        }

        updateTimerText();
    }

    public int getCurrentAlarmDuration() {
        if (!alarms.empty()) {
            AlarmTask firstAlarm = alarms.firstElement();
            return firstAlarm.getDuration();
        } else {
            return 0;
        }
    }

    public int getTotalDuration() {
        int sessionDur = 0;

        for (AlarmTask alarm : alarms) {
            sessionDur += alarm.getDuration();
        }

        return sessionDur;

    }

    public int getAlarmCount() {
        return alarms.size();
    }


    public void cancelAllAlarms(boolean clear) {
        for (AlarmTask alarm : alarms) {
            alarm.cancel();
        }

        if (clear) {
            alarms.clear();
        }

        updateTimerText();
        updatePreviewText();
    }



    public ArrayList<Integer> getPreviewTimes() {
        ArrayList<Integer> previewTimes = new ArrayList<>();
        for (int i = 1; i < alarms.size(); i++) {
            AlarmTask alarm = alarms.elementAt(i);
            previewTimes.add(alarm.getDuration());
        }
        return previewTimes;
    }


    /**
     * Starts the timer at the given time
     *
     * @param time with which to count down
     */
    public void startTicker(int time) {
        Log.v(TAG, "Starting the ticker: " + time);

        setCurrentState(RUNNING);
        mCurrentState.setValue(RUNNING);

        setCurTimerLeft(time);
        setTimeStamp(time);

        mTimer.schedule(
                new TimerTask() {
                    public void run() {
                        if (mHandler != null) {
                            mHandler.sendEmptyMessage(0);
                        }
                    }
                },
                TIMER_TIC,
                TIMER_TIC
        );

        startDND();
        startService();
    }


    /**
     * Stops the timer
     */
    public void stopTicker() {
        Log.v(TAG, "Timer stopped");

        clearTime();
        cancelAllAlarms(false);

        stopDND();
        stopService();

        // Stop our timer service
        setCurrentState(STOPPED);

    }



    public void timerUnPause() {
        // How far have we elapsed?
        int sessionLeft = prefs.getInt("SessionTimeLeft", 0);
        int currentTimerLeft = prefs.getInt("CurrentTimeLeft", 0);

        int timeElapsed = sessionDuration - sessionLeft;

        // Setup the alarms
        loadLastTimers(-timeElapsed);
        timerResume(currentTimerLeft);
        startAllAlarms();
    }

    /**
     * Resume the time after being paused
     */
    public void timerResume() {
        Log.v(TAG, "Resuming the timer...");

        startTicker(currentTimerLeft.getValue());
        setCurrentState(RUNNING);
    }

    /**
     * Resume the time after being paused
     */
    public void timerResume(int timeLeft) {
        Log.v(TAG, "Resuming the timer...");

        startTicker(timeLeft);
        setCurrentState(RUNNING);
    }

    /**
     * Resume the time after being paused
     */
    public void timerResume(int timeLeft, int curDuration) {
        Log.v(TAG, "Resuming the timer...");

        currentTimerDuration.setValue(curDuration);
        startTicker(timeLeft);
        setCurrentState(RUNNING);
    }


    /**
     * Pause the timer and stop the timer service
     */
    public void timerPause() {
        Log.v(TAG, "Pausing the timer...");

        saveState();

        cancelAllAlarms(false);

        setCurrentState(PAUSED);
    }

    /**
     * Clears the time, sets the image and label to default
     */
    public void clearTime() {
        setCurTimerLeft(0);
        setIndex(0);
    }


    /**
     * Cancels the alarm portion of the timer
     */
    public void stopAlarmsAndTicker() {
        Log.v(TAG, "Stopping Alarms and Ticker ...");

        cancelAllAlarms(false);
        stopTicker();
        clearTime();
    }


    /**
     * HELPER FUNCTIONS
     */

    public void startAlarms(TimerList list) {
        addAlarms(list, 0);
        startAll();
    }

    public void saveTimerList(TimerList tL) {
        SharedPreferences.Editor editor = prefs.edit();
        String ret = tL.getString();
        Log.v(TAG, "Saved timer string: " + ret);
        editor.putString("timeString", tL.getString());
        editor.apply();
    }

    private String getTimeString() {
        String prefString = prefs.getString("timeString", "");

        if (prefString.equals(""))
            prefString = prefs.getString("advTimeString", "120000#sys_def");

        return prefString;
    }

    public TimerList retrieveTimerList() {
        String prefString = getTimeString();
        TimerList tL = new TimerList(prefString);
        Log.v(TAG, "Got timer string: " + prefString + " from Settings");
        return tL;
    }


    /**
     * TICKING
     */

    public void doTick() {


        if (mCurrentState.getValue() != RUNNING || appIsPaused)
            return;

        Date now = new Date();
        Date nextAlarm = new Date(timeStamp);
        Date sessionEnd = new Date(sessionTimeStamp);

        long timeLeft = nextAlarm.getTime() - now.getTime();
        currentTimerLeft.setValue((int) timeLeft);

        updateTimerText((int) timeLeft);

        sessionTimeLeft = (int) (sessionEnd.getTime() - now.getTime());

        if (currentTimerLeft.getValue() <= 0) {

            if (alarms.size() > 0) {
                Log.v(TAG, "Tick cycled ended");
            } else {
                Log.e(TAG, "Error: Time up. This probably means that the Broadcast was not received");
                stopTicker();
            }


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

    private void updateTimerText() {
        updateTimerText(currentTimerLeft.getValue());
    }

    private void updateTimerText(int timeLeft) {
        // Calculate text only if time has changed
        int rounded = (timeLeft / 1000) * 1000;

        if (lastTextGenerated != rounded) {
            lastTextGenerated = rounded;
            timerText.setValue(Time.time2hms(rounded));
        }
    }

    private void updatePreviewText() {
        ArrayList<String> arr = makePreviewArray();
        String oldText = previewText.getValue();
        String newText = TextUtils.join("\n", arr);

        if (!newText.equals(oldText)) {
            previewText.setValue(newText);
        }
    }

    private ArrayList<String> makePreviewArray() {
        ArrayList<String> arr = new ArrayList<>();
        ArrayList<Integer> previewTimes = getPreviewTimes();

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
     * Handler for the message from the timer service
     */
    private final Handler mHandler = new Handler(Looper.getMainLooper()) {

        @Override
        public void handleMessage(Message msg) {
            doTick();
        }
    };


    public AlarmTask getAlarmById(int id) {
        AlarmTask found = null;
        for (AlarmTask alarm : alarms) {
            if (alarm.getId() == id) found = alarm;
        }

        return found;
    }

    public void onAlarmEnd(int id) {
        int left = alarms.size() - 1;
        Log.v(TAG, "Alarm has ended. There are " + left + " alarms left");

        AlarmTask alarm = getAlarmById(id);

        if (alarm == null) return;

        // Remove alarm
        alarms.remove(alarm);

        // Update labels
        if (alarms.empty()) {

            // Send message to activity,
            // in case AutoRepeat is on.
            handleAutoRepeat();
            stopDND();
            stopService();
            stopAlarmsAndTicker();
            loadLastTimers();


        } else {
            switchToTimer(alarms.firstElement());
        }
        updateTimerText();
        updatePreviewText();
    }

    private void handleAutoRepeat() {
        if (prefs.getBoolean("AutoRestart", false)) {
            Log.i(TAG, "AUTO RESTART");
            stopAlarmsAndTicker();
            startAlarms(retrieveTimerList());
            setCurrentState(RUNNING);
        }
    }

    private void switchToTimer(AlarmTask alarm) {
        int duration = alarm.getDuration();
        setCurTimerDuration(duration);
        setTimeStamp(duration);
        setCurTimerLeft(0);
        doTick();

        Intent broadcast = new Intent();

        broadcast.putExtra("time", duration);
        broadcast.setAction(BROADCAST_RESET);
        mApp.sendBroadcast(broadcast);
    }


    private void setTimeStamp(int duration) {
        Log.v(TAG, "Next alarm will finish in: " + duration);
        SharedPreferences.Editor editor = prefs.edit();

        timeStamp = new Date().getTime() + duration;

        // Save new time
        editor.putLong("TimeStamp", timeStamp);
        editor.apply();
    }

    private void setSessionTimeStamp(int duration) {
        Log.v(TAG, "Session will finish in: " + duration);
        SharedPreferences.Editor editor = prefs.edit();

        sessionTimeStamp = new Date().getTime() + duration;

        // Save new time
        editor.putLong("SessionTimeStamp", sessionTimeStamp);
        editor.apply();
    }

    private void startDND() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && prefs.getBoolean("doNotDisturb", false)) {
            try {
                NotificationManager mNotificationManager = (NotificationManager) mApp.getSystemService(Context.NOTIFICATION_SERVICE);
                mNotificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY);
            } catch (Exception e) {
                Log.e(TAG, e.toString());
            }
        }
    }

    private void stopDND() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && prefs.getBoolean("doNotDisturb", false)) {
            try {
                NotificationManager mNotificationManager = (NotificationManager) mApp.getSystemService(Context.NOTIFICATION_SERVICE);
                mNotificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL);
            } catch (Exception e) {
                Log.e(TAG, e.toString());
            }
        }
    }

    private void startService() {
        Intent startIntent = new Intent(mApp.getApplicationContext(), SoundService.class);
        mApp.getApplicationContext().startService(startIntent);
    }

    private void stopService() {
        mApp.sendBroadcast(new Intent(BroadcastTypes.BROADCAST_STOP));
    }






}
