package org.yuttadhammo.BodhiTimer.Service;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import org.yuttadhammo.BodhiTimer.TimerUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.Stack;
import java.util.Timer;
import java.util.TimerTask;

import static org.yuttadhammo.BodhiTimer.Service.TimerState.RUNNING;
import static org.yuttadhammo.BodhiTimer.Service.TimerState.STOPPED;
import static org.yuttadhammo.BodhiTimer.Service.TimerState.PAUSED;

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
     * The maximum time
     */
    public int mLastTime = 1800000;

    /**
     * The current timer time
     */
    public int mTime = 0;

    public long timeStamp;

    public Timer mTimer;

    private AlarmManager mAlarmMgr;

    private Stack<AlarmTask> alarms;
    // The context to start the service in
    private Context mContext;

    public ScheduleClient scheduleClient;

    public boolean isPaused;


    // TODO: These need to be handled outside
    public NotificationManager mNM;
    private SharedPreferences prefs;



    public AlarmTaskManager(Context context) {
        mContext = context;
        alarms = new Stack<AlarmTask>();

        mAlarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        mNM = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        prefs = PreferenceManager.getDefaultSharedPreferences(context);

        // Create a new service client and bind our activity to this service
        scheduleClient = new ScheduleClient(context);
        scheduleClient.doBindService();

        // Constructor where listener events are ignored
        this.listener = null;


    }

    private AlarmTaskListener listener;

    public interface AlarmTaskListener {
        public void onEnterState(int state);

        // These methods are the different events and
        // need to pass relevant arguments related to the event triggered
        public void onObjectReady(String title);

        // or when data has been loaded
        public void onDataLoaded(String data);

        public void onUpdateTime();
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
            listener.onUpdateTime();
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


    /**
     * Starts the timer at the given time
     *
     * @param time    with which to count down
     * @param service whether or not to start the service as well
     */
    public void timerStart(int time, boolean service) {
        Log.v(TAG, "Starting the timer: " + time);

        onEnterState(RUNNING);

        mTime = time;

        timeStamp = new Date().getTime() + mTime;

        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong("TimeStamp", timeStamp);
        editor.apply();


        // Start external service
        if (service) {
            Log.v(TAG, "ALARM: Starting the timer service: " + TimerUtils.time2humanStr(mContext, mTime));

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
    public void timerStop() {
        Log.v(TAG, "Timer stopped");

        clearTime();
        stopAlarmTimer();

        // Stop our timer service
        onEnterState(STOPPED);

    }

    /**
     * Resume the time after being paused
     */
    public void timerResume() {
        Log.v(TAG, "Resuming the timer...");

        timerStart(mTime, true);
        onEnterState(RUNNING);
    }

    /**
     * Pause the timer and stop the timer service
     */
    public void timerPause() {
        Log.v(TAG, "Pausing the timer...");

        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("CurrentTime", mTime);
        editor.apply();

        stopAlarmTimer();

        onEnterState(PAUSED);
    }

    /**
     * Clears the time, sets the image and label to default
     */
    public void clearTime() {
        mTime = 0;

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

        mTime = (int) (then.getTime() - now.getTime());

        if (mTime <= 0) {

            Log.e(TAG, "Error: Time up. This probably means that the Broadcast was not received");
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



//
//    /**
//     * Class for clients to access.  Because we know this service always
//     * runs in the same process as its clients, we don't need to deal with
//     * IPC.
//     */
//    public class LocalBinder extends Binder {
//        AlarmTaskManager getService() {
//            return AlarmTaskManager.this;
//        }
//    }
//
//    @Override
//    public int onStartCommand(Intent intent, int flags, int startId) {
//        Log.i("LocalService", "Received start id " + startId + ": " + intent);
//        return START_NOT_STICKY;
//    }
//
//    @Nullable
//    @Override
//    public IBinder onBind(Intent intent) {
//        return mBinder;
//    }
//
//    // This is the object that receives interactions from clients.  See
//    // RemoteService for a more complete example.
//    private final IBinder mBinder = new LocalBinder();


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
