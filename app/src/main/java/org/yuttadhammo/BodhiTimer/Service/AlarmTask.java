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

package org.yuttadhammo.BodhiTimer.Service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import org.yuttadhammo.BodhiTimer.TimerReceiver;

import static org.yuttadhammo.BodhiTimer.Util.BroadcastTypes.BROADCAST_END;

/**
 * Set an alarm for the date passed into the constructor
 * When the alarm is raised it will start the NotifyService
 * <p>
 * This uses the android build in alarm manager *NOTE* if the phone is turned off this alarm will be cancelled
 * <p>
 * This will run on it's own thread.
 *
 * @author paul.blundell
 */
public class AlarmTask implements Runnable {

    private final String TAG = AlarmTask.class.getSimpleName();

    // The android system alarm manager
    private final AlarmManager mAlarmMgr;
    // Your context to retrieve the alarm manager from
    private final Context context;

    // The duration selected for the alarm
    private final MutableLiveData<Integer> mDuration = new MutableLiveData<>();

    private PendingIntent mPendingIntent;

    // Unused
    private final MutableLiveData<TimerState> mTimerState = new MutableLiveData<>();
    private final MutableLiveData<SessionType> mSessionType = new MutableLiveData<>();
    private final MutableLiveData<String> mLabel = new MutableLiveData<>();
    private final MutableLiveData<String> mUri = new MutableLiveData<>();
    private final MutableLiveData<String> mUriType = new MutableLiveData<>();

    public int id = 0;
    public int offset = 0;
    public int duration = 0;

    public AlarmTask(Context context, int offset, int duration) {
        this.context = context;
        this.mAlarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        this.mDuration.setValue(duration);
        this.duration = duration;
        this.offset = offset;
//        this.mTimerState.setValue(TimerState.INACTIVE);
//        this.mSessionType.setValue(SessionType.WORK);
//        this.mLabel.setValue("FIXME");
//        // FIXME
//        this.mUri.setValue(null);
    }

    @Override
    public void run() {

        Intent intent = new Intent(context, TimerReceiver.class);
        //Intent intent = new Intent();

        //intent.putExtra("SetTime", time);
        intent.putExtra("offset", offset);
        intent.putExtra("duration", duration);
        intent.putExtra("uri", mUri.getValue());
        intent.putExtra("id", id);
        intent.setAction(BROADCAST_END);

        int time = duration + offset;

        Log.i(TAG, "Running new alarm task " + id + ", uri " + mUri.getValue() + " type: " + mSessionType.getValue() + " due in " + (time) / 1000);

        mPendingIntent = PendingIntent.getBroadcast(context, id, intent, 0);
        //mPendingIntent.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent alarmInfoIntent = new Intent(context, TimerReceiver.class);
            PendingIntent pendingAlarmInfo = PendingIntent.getBroadcast(context, id + 1000, alarmInfoIntent, 0);
            AlarmManager.AlarmClockInfo info = new AlarmManager.AlarmClockInfo(System.currentTimeMillis() + time, pendingAlarmInfo);

            mAlarmMgr.setAlarmClock(info, mPendingIntent);
            //mAlarmMgr.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + time, mPendingIntent);
        } else {
            mAlarmMgr.setExact(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + time, mPendingIntent);
        }
    }

    public void cancel() {
        if (mPendingIntent != null)
            mAlarmMgr.cancel(mPendingIntent);
    }

    // Accesors

    public LiveData<TimerState> getTimerState() {
        return mTimerState;
    }

    public LiveData<SessionType> getSessionType() {
        return mSessionType;
    }

    public void setDuration(int newDuration) {
        mDuration.setValue(newDuration);
    }

    public void setTimerState(TimerState state) {
        mTimerState.setValue(state);
    }

    public void setSessionType(SessionType type) {
        mSessionType.setValue(type);
    }

    public LiveData<String> getUri() {
        return mUri;
    }

    public void setUri(String label) {
        mUri.setValue(label);
    }

    public LiveData<String> getUriType() {
        return mUriType;
    }

    public void setUriType(String val) {
        mUriType.setValue(val);
    }


}