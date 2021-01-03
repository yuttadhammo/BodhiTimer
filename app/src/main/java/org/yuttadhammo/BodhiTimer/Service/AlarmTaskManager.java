package org.yuttadhammo.BodhiTimer.Service;

import android.content.Context;
import android.os.CountDownTimer;
import android.util.Log;

import java.util.Stack;

public class AlarmTaskManager {

    /**
     * All possible timer states
     */
    public static final int RUNNING = 0;
    public static final int STOPPED = 1;
    public static final int PAUSED = 2;

    /**
     * Update rate of the internal timer
     */
    public final static int TIMER_TIC = 100;

    /**
     * The timer's current state
     */
    public int mCurrentState = -1;

    /**
     * The maximum time
     */
    public int mLastTime = 1800000;

    /**
     * The current timer time
     */
    public int mTime = 0;


    private Stack<AlarmTask> alarms;
    // The context to start the service in
    private Context mContext;

    public AlarmTaskManager(Context context) {
        mContext = context;
        alarms = new Stack<AlarmTask>();
    }

    /**
     * Show an alarm for a certain date when the alarm is called it will pop up a notification
     */
    public void addAlarm(int time) {
        // This starts a new thread to set the alarm
        // You want to push off your tasks onto a new thread to free up the UI to carry on responding
        Log.i("ScheduleService", "Creating new alarm task");

//        AlarmTask alarm =  new AlarmTask(mContext, time, "", "");
//        alarms.push(alarm);
//
//        alarm.run();
    }

    /**
     * Show an alarm for a certain date when the alarm is called it will pop up a notification
     */
    public void addAlarmWithUri(int time, String uriType, String notificationUri) {
        // This starts a new thread to set the alarm
        // You want to push off your tasks onto a new thread to free up the UI to carry on responding
        Log.i("ScheduleService", "Creating new alarm task");

//        AlarmTask alarm =  new AlarmTask(mContext, time, uriType, notificationUri);
//        alarms.push(alarm);
//
//        alarm.run();
    }


    public void cancelAlarm() {

    }

    public void pauseAlarms() {

    }

    public void unpauseAlarms() {

    }

    public void cancelAllAlarms() {




    }

//    private class AppCountDownTimer extends CountDownTimer {
//
//        private final String TAG = AlarmTaskManager.AppCountDownTimer.class.getSimpleName();
//        /**
//         * @param millisInFuture    The number of millis in the future from the call
//         *                          to {@link #start()} until the countdown is done and {@link #onFinish()}
//         *                          is called.
//         */
//        private AppCountDownTimer(long millisInFuture) {
//            super(millisInFuture, 1000);
//        }
//
//        /**
//         * This is useful only when the screen is turned on. It seems that onTick is not called for every tick if the
//         * phone is locked and the app runs in the background.
//         * I found this the hard way when using the session duration(which is set here) in saving to statistics.
//         */
//        @Override
//        public void onTick(long millisUntilFinished) {
//            Log.v(TAG, "is Ticking: " + millisUntilFinished + " millis remaining.");
//            mCurrentTimer.setDuration(millisUntilFinished);
//            mRemaining = millisUntilFinished;
//            //EventBus.getDefault().post(new Constants.UpdateTimerProgressEvent());
//        }
//
//        @Override
//        public void onFinish() {
//            Log.v(TAG, "is finished.");
//            mRemaining = 0;
//        }
//    }
}
