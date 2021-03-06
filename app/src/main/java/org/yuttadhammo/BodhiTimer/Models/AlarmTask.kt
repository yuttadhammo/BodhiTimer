package org.yuttadhammo.BodhiTimer.Models

import android.app.AlarmManager
import android.app.AlarmManager.AlarmClockInfo
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import org.yuttadhammo.BodhiTimer.Const.BroadcastTypes
import org.yuttadhammo.BodhiTimer.Const.SessionTypes
import org.yuttadhammo.BodhiTimer.Service.TimerReceiver



data class AlarmTask(val context: Context, val offset: Int, val duration: Int) {


    // The android system alarm manager
    private var mAlarmMgr: AlarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private var mPendingIntent: PendingIntent? = null

    var sessionType: SessionTypes = SessionTypes.INVALID
    var uri: String = ""
    var id: Int = 0


    fun run() {
        val intent = Intent(context, TimerReceiver::class.java)
        intent.putExtra("offset", offset)
        intent.putExtra("duration", duration)
        intent.putExtra("uri", uri)
        intent.putExtra("id", id)
        intent.action = BroadcastTypes.BROADCAST_END
        val time = duration + offset
        Log.i(TAG, "Running new alarm task " + id + ", uri " + uri + " type: " + sessionType + " due in " + time / 1000 + " duration " + duration)

        mPendingIntent = PendingIntent.getBroadcast(context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val alarmInfoIntent = Intent(context, TimerReceiver::class.java)
            val pendingAlarmInfo = PendingIntent.getBroadcast(context, id + 1000, alarmInfoIntent, 0)
            val info = AlarmClockInfo(System.currentTimeMillis() + time, pendingAlarmInfo)
            mAlarmMgr.setAlarmClock(info, mPendingIntent)
        } else {
            mAlarmMgr.setExact(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + time, mPendingIntent)
        }
    }

    fun cancel() {
        if (mPendingIntent != null) mAlarmMgr.cancel(mPendingIntent)
    }

    companion object {
        const val TAG = "AlarmTask"
    }
}
