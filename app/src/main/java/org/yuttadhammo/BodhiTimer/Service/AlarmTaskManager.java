package org.yuttadhammo.BodhiTimer.Service;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.Date;
import java.util.Stack;
import java.util.Timer;
import java.util.TimerTask;

import static org.yuttadhammo.BodhiTimer.Service.TimerState.PAUSED;
import static org.yuttadhammo.BodhiTimer.Service.TimerState.RUNNING;
import static org.yuttadhammo.BodhiTimer.Service.TimerState.STOPPED;
import static org.yuttadhammo.BodhiTimer.Util.BroadcastTypes.BROADCAST_END;
import static org.yuttadhammo.BodhiTimer.Util.BroadcastTypes.BROADCAST_RESET;

public class AlarmTaskManager extends BroadcastReceiver {
    private final String TAG = AlarmTaskManager.class.getSimpleName();

    /**
     * Update rate of the internal timer
     */
    public final static int TIMER_TIC = 100;

    /**
     * The timer's current state
     */
    public int mCurrentState = -1;


    public long timeStamp;
    private long sessionTimeStamp;

    public int sessionDuration;

    public Timer mTimer;

    private final Stack<AlarmTask> alarms;
    private int lastId = 0;

    // The context to start the service in
    private final Context mContext;

    public boolean appIsPaused;

    // Live Data
    private final MutableLiveData<Integer> currentTimerDuration = new MutableLiveData<>();
    // The current elapsed timer time
    private final MutableLiveData<Integer> currentTimerLeft = new MutableLiveData<>();
    // The current elapsed timer time
    private final MutableLiveData<Integer> mIndex = new MutableLiveData<>();
    // The current elapsed timer time
    private final MutableLiveData<Integer> sessionTimeLeft = new MutableLiveData<>();
    // The current elapsed timer time
    //private final MutableLiveData<Integer> sessionTimeLeft = new MutableLiveData<>();

    // Accessors
    public LiveData<Integer> getCurTimerDuration() {
        return currentTimerDuration;
    }
    public LiveData<Integer> getCurTimerLeft() {
        return currentTimerLeft;
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
    public NotificationManager mNM;
    private final SharedPreferences prefs;



    public AlarmTaskManager(Context context) {
        mContext = context;
        alarms = new Stack<>();

        mNM = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        prefs = PreferenceManager.getDefaultSharedPreferences(context);

        setCurTimerLeft(0);
        setCurTimerDuration(0);

        // Constructor where listener events are ignored
        this.listener = null;
    }

    private AlarmTaskListener listener;

    public void saveState() {
        SharedPreferences.Editor editor = prefs.edit();

        editor.putInt("CurrentTimerDuration", getCurTimerDurationVal());
        editor.putInt("CurrentTimeLeft", getCurTimerLeftVal());
        editor.putInt("State", mCurrentState);
        editor.putInt("SessionDuration", sessionDuration);
        editor.putInt("SessionTimeLeft", sessionTimeLeft.getValue());

        editor.apply();
    }



    public interface AlarmTaskListener {
        void onEnterState(int state);

        // These methods are the different events and
        // need to pass relevant arguments related to the event triggered
        void onObjectReady(String title);

        // or when data has been loaded
        void onDataLoaded(String data);

        void onUpdateTime(int elapsed, int duration);
    }

    // Assign the listener implementing events interface that will receive the events
    public void setListener(AlarmTaskListener listener) {
        this.listener = listener;
    }


    private void onEnterState(int state) {
        if (listener != null)
            listener.onEnterState(state);
    }

    private void onUpdateTime() {
        if (listener != null)
            listener.onUpdateTime(currentTimerLeft.getValue(), currentTimerDuration.getValue());
    }


    /**
     * Show an alarm for a certain date when the alarm is called it will pop up a notification
     */
    public AlarmTask addAlarmWithUri(int offset, int duration, String uri, SessionType sessionType) {

        Log.i(TAG, "Creating new alarm task, uri " + uri + " type: " + sessionType + " due in " + (duration + offset)/1000 );
        AlarmTask alarm = new AlarmTask(mContext, offset, duration);
        alarm.setUri(uri);
        alarm.setSessionType(sessionType);

        lastId++;
        alarm.id = lastId;

        alarms.push(alarm);

        return alarm;
    }

    public int addAlarms(TimerList list, int offset) {
        // If adding a complete list, clear all previous alarms
        cancelAllAlarms();

        int totalDuration = 0;
        lastId = 0;

        for (TimerList.Timer timer: list.timers) {
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

        return totalDuration;

    }

    public void startAllAlarms(){
        for (AlarmTask alarm : alarms) {
            alarm.run();
        }
    }

    public void startAll() {
        int sessionDur = 0;

        for (AlarmTask alarm : alarms) {
            alarm.run();
            sessionDur += alarm.duration;
        }

        resetCurrentAlarm();
        int dur = getCurTimerDurationVal();
        sessionDuration = sessionDur;
        setSessionTimeStamp(sessionDur);

        startTicker(dur);
        Log.v(TAG, "Started ticker & timer, first duration: " + dur);
    }

    private void resetCurrentAlarm() {
        int dur = getCurrentAlarmDuration();
        setCurTimerDuration(dur);
        setCurTimerLeft(0);
    }

    public int getCurrentAlarmDuration() {
        if (!alarms.empty()) {
            AlarmTask firstAlarm = alarms.firstElement();
            return firstAlarm.duration;
        } else {
            return 0;
        }
    }

    public int getTotalDuration() {
        int sessionDur = 0;

        for (AlarmTask alarm : alarms) {
            sessionDur += alarm.duration;
        }

        return sessionDur;

    }

    public int getAlarmCount() {
        return alarms.size();
    }



    public void cancelAllAlarms() {
        for (AlarmTask alarm : alarms) {
            alarm.cancel();
        }
        alarms.clear();
    }

    public ArrayList<Integer> getPreviewTimes() {
        ArrayList<Integer> previewTimes = new ArrayList<>();
        for (int i = 1; i < alarms.size(); i++) {
            AlarmTask alarm = alarms.elementAt(i);
            previewTimes.add(alarm.duration);
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
        onEnterState(RUNNING);

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
                TIMER_TIC
        );
    }


    /**
     * Stops the timer
     */
    public void stopTicker() {
        Log.v(TAG, "Timer stopped");

        clearTime();
        cancelAllAlarms();


        // Stop our timer service
        onEnterState(STOPPED);

    }

    /**
     * Resume the time after being paused
     */
    public void timerResume() {
        Log.v(TAG, "Resuming the timer...");

        startTicker(currentTimerLeft.getValue());
        onEnterState(RUNNING);
    }

    /**
     * Resume the time after being paused
     */
    public void timerResume(int timeLeft) {
        Log.v(TAG, "Resuming the timer...");

        startTicker(timeLeft);
        onEnterState(RUNNING);
    }

    /**
     * Resume the time after being paused
     */
    public void timerResume(int timeLeft, int curDuration) {
        Log.v(TAG, "Resuming the timer...");

        currentTimerDuration.setValue(curDuration);
        startTicker(timeLeft);
        onEnterState(RUNNING);
    }



    /**
     * Pause the timer and stop the timer service
     */
    public void timerPause() {
        Log.v(TAG, "Pausing the timer...");

        saveState();

        cancelAllAlarms();
        //stopTicker();

        onEnterState(PAUSED);
    }

    /**
     * Clears the time, sets the image and label to default
     */
    public void clearTime() {
        setCurTimerLeft(0);
        setIndex(0);
        onUpdateTime();
    }


    /**
     * Cancels the alarm portion of the timer
     */
    public void stopAlarmsAndTicker() {
        Log.v(TAG, "Stopping Alarms and Ticker ...");

        cancelAllAlarms();
        stopTicker();
        clearTime();
        //mNM.cancelAll();
    }

    public void doTick() {
        //Log.w(TAG,"ticking");

        if (mCurrentState != RUNNING || appIsPaused)
            return;

        Date now = new Date();
        Date nextAlarm = new Date(timeStamp);
        Date sessionEnd = new Date(sessionTimeStamp);

        currentTimerLeft.setValue((int) (nextAlarm.getTime() - now.getTime()));
        sessionTimeLeft.setValue((int) (sessionEnd.getTime() - now.getTime()));

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

    /**
     * Handler for the message from the timer service
     */
    private final Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            onUpdateTime();
            doTick();
        }
    };

    public AlarmTask getAlarmById(int id) {
        AlarmTask found = null;
        for (AlarmTask alarm : alarms) {
            if (alarm.id == id) found = alarm;
        }

        return found;
    }

    public void onAlarmEnd(int id) {
        int left = alarms.size() - 1;
        Log.v(TAG, "Alarm has ended. There are " +  left + " alarms left");

        AlarmTask alarm = getAlarmById(id);

        if (alarm == null) return;



        // Remove alarm
        alarms.remove(alarm);

        // Update labels
        if (alarms.empty()) {
            //cleanup?
        } else {
            switchToTimer(alarms.firstElement());
            //increaseIndex();
        }
    }

    private void switchToTimer(AlarmTask alarm) {
        int duration = alarm.duration;
        setCurTimerDuration(duration);
        setTimeStamp(duration);
        setCurTimerLeft(0);
        doTick();

        Intent broadcast = new Intent();

        broadcast.putExtra("time", duration);
        broadcast.setAction(BROADCAST_RESET);
        mContext.sendBroadcast(broadcast);
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

    @Override
    public void onReceive(Context context, Intent mIntent) {

        Log.v(TAG, "MANGER Received alarm callback ");

        Intent broadcast = new Intent();
        broadcast.putExtra("time", 0);
        broadcast.putExtra("id", mIntent.getIntExtra("id", 0));
        broadcast.setAction(BROADCAST_END);
        context.sendBroadcast(broadcast);


    }
}
