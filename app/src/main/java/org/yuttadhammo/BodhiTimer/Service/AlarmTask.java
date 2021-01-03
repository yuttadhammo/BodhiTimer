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

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import org.yuttadhammo.BodhiTimer.BL.SessionType;
import org.yuttadhammo.BodhiTimer.TimerReceiver;

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

    // The android system alarm manager
    private final AlarmManager mAlarmMgr;
    // Your context to retrieve the alarm manager from
    private final Context context;

    // The duration selected for the alarm
    private final MutableLiveData<Integer> mDuration = new MutableLiveData<>();

    // Unused
    private final MutableLiveData<TimerState> mTimerState = new MutableLiveData<>();
    private final MutableLiveData<SessionType> mSessionType = new MutableLiveData<>();
    private final MutableLiveData<String> mLabel = new MutableLiveData<>();
    private final MutableLiveData<String> mUri = new MutableLiveData<>();

    public AlarmTask(Context context, int duration) {
        this.context = context;
        this.mAlarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        this.mDuration.setValue((int) duration);
//        this.mTimerState.setValue(TimerState.INACTIVE);
//        this.mSessionType.setValue(SessionType.WORK);
//        this.mLabel.setValue("FIXME");
//        // FIXME
//        this.mUri.setValue(null);
    }

    @Override
    public void run() {
        int time = getDuration().getValue();
        Intent intent = new Intent(context, TimerReceiver.class);
        intent.putExtra("SetTime", time);

        PendingIntent mPendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mAlarmMgr.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + time, mPendingIntent);
        } else {
            mAlarmMgr.setExact(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + time, mPendingIntent);
        }
    }

    // Accesors
    public LiveData<Integer> getDuration() {
        return mDuration;
    }

    public LiveData<TimerState> getTimerState() {
        return mTimerState;
    }

    public LiveData<SessionType> getSessionType() {
        return mSessionType;
    }

    public LiveData<String> getLabel() {
        return mLabel;
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

    public void setLabel(String label) {
        mLabel.setValue(label);
    }
}