package org.yuttadhammo.BodhiTimer.Service;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import org.yuttadhammo.BodhiTimer.Util.Time;
import org.yuttadhammo.BodhiTimer.Util.Notification;

import java.util.ArrayList;
import java.util.Date;
import java.util.Stack;
import java.util.Timer;
import java.util.TimerTask;

import static org.yuttadhammo.BodhiTimer.Service.TimerState.PAUSED;
import static org.yuttadhammo.BodhiTimer.Service.TimerState.RUNNING;
import static org.yuttadhammo.BodhiTimer.Service.TimerState.STOPPED;
import static org.yuttadhammo.BodhiTimer.Util.BroadcastTypes.BROADCAST_RESET;

public class AlarmTaskManager {
    private final String TAG = AlarmTaskManager.class.getSimpleName();

    /**
     * Update rate of the internal timer
     */
    public final static int TIMER_TIC = 100;

    /**
     * The timer's current state
     */
    public int mCurrentState = -1;

    /**
     * The current duration
     */
    private int mLastDuration = 1800000;


    public long timeStamp;

    public Timer mTimer;

    private AlarmManager mAlarmMgr;

    private Stack<AlarmTask> alarms;
    private int lastId = 0;
    private AlarmTask lastAlarm;

    // The context to start the service in
    private Context mContext;

    public boolean isPaused;

    // Live Data
    private final MutableLiveData<Integer> mCurDuration = new MutableLiveData<>();
    // The current elapsed timer time
    private final MutableLiveData<Integer> mCurElapsed = new MutableLiveData<>();


    // Accesors
    public LiveData<Integer> getCurDuration() {
        return mCurDuration;
    }
    public LiveData<Integer> getCurElapsed() {
        return mCurElapsed;
    }
    public Integer getCurDurationVal() {
        return mCurDuration.getValue();
    }
    public Integer getCurElapsedVal() {
        return mCurElapsed.getValue();
    }
    public Integer getProgress() {
        return mCurElapsed.getValue() / mCurDuration.getValue();
    }

    public void setDuration(int newDuration) {
        mLastDuration = newDuration;
        mCurDuration.setValue(new Integer(newDuration));
    }
    public void setCurElapsed(int newElapsed) {
        mCurElapsed.setValue(new Integer(newElapsed));
    }


    // TODO: These need to be handled outside
    public NotificationManager mNM;
    private SharedPreferences prefs;

    // TODO: These should not be needed in future
    public String advTimeString = "";

    public boolean useAdvTime = false;
    public int advTimeIndex;
    private String[] advTime;


    public AlarmTaskManager(Context context) {
        mContext = context;
        alarms = new Stack<AlarmTask>();

        mAlarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        mNM = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        prefs = PreferenceManager.getDefaultSharedPreferences(context);

        setCurElapsed(0);
        setDuration(1800000);

        // Constructor where listener events are ignored
        this.listener = null;
    }

    private AlarmTaskListener listener;

    public void saveState() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("LastTime", getCurDurationVal());
        editor.putInt("CurrentTime", getCurElapsedVal());
        editor.putInt("State", mCurrentState);
    }

    public interface AlarmTaskListener {
        public void onEnterState(int state);

        // These methods are the different events and
        // need to pass relevant arguments related to the event triggered
        public void onObjectReady(String title);

        // or when data has been loaded
        public void onDataLoaded(String data);

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
        int a;
        int b;
        if (listener != null)
            a = getCurElapsedVal();
            b = getCurDurationVal();
            listener.onUpdateTime(getCurElapsedVal(), getCurDurationVal());
    }



    /**
     * Show an alarm for a certain date when the alarm is called it will pop up a notification
     */
    @Deprecated
    public void setAlarmForNotification(int time) {
        // This starts a new thread to set the alarm
        // You want to push off your tasks onto a new thread to free up the UI to carry on responding
        Log.i(TAG, "Creating new alarm task");
        AlarmTask alarm = new AlarmTask(mContext, 0, time);
        alarms.push(alarm);
        lastAlarm = alarm;
        alarm.run();
    }


    /**
     * Show an alarm for a certain date when the alarm is called it will pop up a notification
     */
    public AlarmTask addAlarmWithUri(int offset, int duration, String uri, SessionType sessionType) {

        Log.i(TAG, "Creating new alarm task, uri " + uri + " type: " + sessionType );
        AlarmTask alarm = new AlarmTask(mContext, offset, duration);
        alarm.setUri(uri);
        alarm.setSessionType(sessionType);

        lastId++;
        alarm.id = lastId;

        alarms.push(alarm);
        lastAlarm = alarm;

        return alarm;
    }

    @Deprecated
    public void startAlarmWithUri(int offset, int duration, String uri, SessionType sessionType) {
        AlarmTask alarm = addAlarmWithUri(offset, duration, uri, sessionType);
        alarm.run();
    }

    public void startAll() {
        for (AlarmTask alarm : alarms) {
            alarm.run();
        }

        AlarmTask firstAlarm = alarms.firstElement();

        int dur = firstAlarm.duration;
        setDuration(dur);
        setCurElapsed(0);

        Log.v(TAG, "Started timer, first duration: " + mLastDuration);


    }

    public int getAlarmCount() {
        return alarms.size();
    }

    public void cancelAlarm() {

    }

    public void pauseAlarms() {

    }

    public void unpauseAlarms() {

    }

    public void cancelAllAlarms() {
        for (AlarmTask alarm : alarms) {
            alarm.cancel();
        }
        alarms.clear();
    }

    public ArrayList<Integer> getPreviewTimes() {
        ArrayList<Integer> previewTimes = new ArrayList<Integer>();
        for (int i = advTimeIndex + 1; i < alarms.size(); i++) {
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

        setCurElapsed(time);
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
     * Starts the timer at the given time
     *
     * @param time with which to count down
     */
    public void timerStart(int time) {
        Log.v(TAG, "Starting the timer: " + time);
        Log.v(TAG, "ALARM: Starting the timer service: " + Time.time2humanStr(mContext, getCurElapsedVal()));

        onEnterState(RUNNING);

        setCurElapsed(time);
        setTimeStamp(time);



        setAlarmForNotification(getCurElapsedVal());

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
    public void tickerStop() {
        Log.v(TAG, "Timer stopped");

        clearTime();
        // FIXME Need to differantiate between a forced stop and a soft stop
        //cancelAllAlarms();

        // Stop our timer service
        onEnterState(STOPPED);

    }

    /**
     * Resume the time after being paused
     */
    public void timerResume() {
        Log.v(TAG, "Resuming the timer...");

        timerStart(getCurElapsedVal());
        onEnterState(RUNNING);
    }

    /**
     * Pause the timer and stop the timer service
     */
    public void timerPause() {
        Log.v(TAG, "Pausing the timer...");

        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("CurrentTime", getCurElapsedVal());
        editor.apply();

        stopAlarmTimer();

        onEnterState(PAUSED);
    }

    /**
     * Clears the time, sets the image and label to default
     */
    public void clearTime() {
        setCurElapsed(0);
        onUpdateTime();
    }


    /**
     * Cancels the alarm portion of the timer
     */
    public void stopAlarmTimer() {
        Log.v(TAG, "Stopping the alarm timer ...");
        //mAlarmMgr.cancel(mPendingIntent);
        mNM.cancelAll();
    }

    public void doTick() {
        //Log.w(TAG,"ticking");

        if (mCurrentState != RUNNING || isPaused)
            return;

        Date now = new Date();
        Date then = new Date(timeStamp);

        setCurElapsed((int) (then.getTime() - now.getTime()));

        if (getCurElapsedVal() <= 0) {

            if (alarms.size() > 0) {
                Log.v(TAG, "Tick cycled ended");
            } else {
                Log.e(TAG, "Error: Time up. This probably means that the Broadcast was not received");
                tickerStop();
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
    private Handler mHandler = new Handler() {

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
        // TODO: Find correct alarm (by ID)
        AlarmTask alarm = getAlarmById(id);

        if (alarm == null) return;

        // Send notification
        Notification.show(mContext, alarm.getUri().getValue(), alarm.duration, alarm.getSessionType().getValue());

        // Remove alarm
        alarms.remove(alarm);

        // Update labels
        switchToTimer(alarms.firstElement());
    }

    private void switchToTimer(AlarmTask alarm) {
        lastAlarm = alarm;
        int duration = alarm.duration;
        setDuration(duration);
        setTimeStamp(duration);
        setCurElapsed(0);

        Intent broadcast = new Intent();
        SharedPreferences.Editor editor = prefs.edit();
        broadcast.putExtra("time", duration);
    }



    private void continueAdvTimer() {
        Intent broadcast = new Intent();
        SharedPreferences.Editor editor = prefs.edit();

        boolean useAdvTime = prefs.getBoolean("useAdvTime", false);
        String advTimeString = prefs.getString("advTimeString", "");
        advTime = null;
        advTimeIndex = 0;

        if (useAdvTime && advTimeString.length() > 0) {
            advTime = advTimeString.split("\\^");
            advTimeIndex = prefs.getInt("advTimeIndex", 0);

            if (advTimeIndex < advTime.length - 1) {
                advTimeIndex++;
                editor.putInt("advTimeIndex", advTimeIndex);

                String[] thisAdvTime = advTime[advTimeIndex].split("#"); // will be of format timeInMs#pathToSound

                int duration = Integer.parseInt(thisAdvTime[0]);

                broadcast.putExtra("time", duration);
                setTimeStamp(duration);

                editor.putInt("LastTime", duration);

                // editor.putString("NotificationUri", thisAdvTime[1]);

                mNM.cancelAll();
                Log.v(TAG, "Starting next iteration of the timer service ...");

            } else {
                broadcast.putExtra("stop", true);
                editor.putInt("advTimeIndex", 0);

            }
            broadcast.setAction(BROADCAST_RESET);
            mContext.sendBroadcast(broadcast);

            editor.commit();
        }


    }

    private void setTimeStamp(int duration) {
        SharedPreferences.Editor editor = prefs.edit();

        timeStamp = new Date().getTime() + duration;

        // Save new time
        editor.putLong("TimeStamp", new Date().getTime() + duration);
        editor.apply();
    }
}
