package org.yuttadhammo.BodhiTimer.Service;

import java.util.Calendar;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import org.yuttadhammo.BodhiTimer.TimerReceiver;

/**
 * Set an alarm for the date passed into the constructor
 * When the alarm is raised it will start the NotifyService
 *
 * This uses the android build in alarm manager *NOTE* if the phone is turned off this alarm will be cancelled
 *
 * This will run on it's own thread.
 *
 * @author paul.blundell
 */
public class AlarmTask implements Runnable{
    // The date selected for the alarm
    private final int time;
    // The android system alarm manager
    private final AlarmManager mAlarmMgr;
    // Your context to retrieve the alarm manager from
    private final Context context;

    public AlarmTask(Context context, int time) {
        this.context = context;
        this.mAlarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        this.time = time;
    }

    @Override
    public void run() {
        Intent intent = new Intent(context, TimerReceiver.class);
        intent.putExtra("SetTime", time);

        PendingIntent mPendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        if (Build.VERSION.SDK_INT >= 19) {
            mAlarmMgr.setExact(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + time, mPendingIntent);
        }
        else {
            mAlarmMgr.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + time, mPendingIntent);
        }
    }
}