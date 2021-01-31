package org.yuttadhammo.BodhiTimer.Service

import android.app.AlarmManager
import android.app.AlarmManager.AlarmClockInfo
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.lifecycle.MutableLiveData
import org.yuttadhammo.BodhiTimer.TimerReceiver
import org.yuttadhammo.BodhiTimer.Util.BroadcastTypes

data class AlarmModel(val duration: Int, val offset: Int) {

    private val TAG = AlarmTask::class.java.simpleName

    // The android system alarm manager
    private var mAlarmMgr: AlarmManager? = null

    // Your context to retrieve the alarm manager from
    private val context: Context? = null

    // The duration selected for the alarm

    // The duration selected for the alarm
    private var mPendingIntent: PendingIntent? = null

    // Unused
    private val mSessionType = MutableLiveData<SessionType>()
    private val mUri = MutableLiveData<String>()

    var id = 0
//    var offset = 0
//    var duration = 0

    fun AlarmModel(context: Context, offset: Int, duration: Int) {
        //this.context = context
        mAlarmMgr = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
//        this.duration = duration
//        this.offset = offset
    }

    fun run() {
        val intent = Intent(context, TimerReceiver::class.java)
        intent.putExtra("offset", offset)
        intent.putExtra("duration", duration)
        intent.putExtra("uri", mUri.value)
        intent.putExtra("id", id)
        intent.action = BroadcastTypes.BROADCAST_END
        val time = duration + offset
        Log.i(TAG, "Running new alarm task " + id + ", uri " + mUri.value + " type: " + mSessionType.value + " due in " + time / 1000 + " duration " + duration)
        mPendingIntent = PendingIntent.getBroadcast(context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val alarmInfoIntent = Intent(context, TimerReceiver::class.java)
            val pendingAlarmInfo = PendingIntent.getBroadcast(context, id + 1000, alarmInfoIntent, 0)
            val info = AlarmClockInfo(System.currentTimeMillis() + time, pendingAlarmInfo)
            mAlarmMgr!!.setAlarmClock(info, mPendingIntent)
            //mAlarmMgr.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + time, mPendingIntent);
        } else {
            mAlarmMgr!!.setExact(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + time, mPendingIntent)
        }
    }

    fun cancel() {
        if (mPendingIntent != null) mAlarmMgr!!.cancel(mPendingIntent)
    }
}
