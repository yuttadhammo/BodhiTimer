package org.yuttadhammo.BodhiTimer.Models

import android.app.AlarmManager
import android.app.AlarmManager.AlarmClockInfo
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import org.yuttadhammo.BodhiTimer.Const.BroadcastTypes
import org.yuttadhammo.BodhiTimer.Const.SessionTypes
import org.yuttadhammo.BodhiTimer.Service.TimerReceiver
import timber.log.Timber


data class AlarmTask(val context: Context, val offset: Int, val duration: Int) {

    // The Android system alarm manager
    private var mAlarmMgr: AlarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private var mPendingIntent: PendingIntent? = null

    var sessionType: SessionTypes = SessionTypes.INVALID
    var uri: String = ""
    var id: Int = 0

    fun run() {
        if (!ensureNecessaryPermission()) return
        val intent = Intent(context, TimerReceiver::class.java)
        intent.putExtra("offset", offset)
        intent.putExtra("duration", duration)
        intent.putExtra("uri", uri)
        intent.putExtra("id", id)
        intent.action = BroadcastTypes.BROADCAST_END
        val time = duration + offset
        Timber.i(
            "Running new alarm task $id, " +
                    "uri: $uri, type: $sessionType " +
                    "due in ${time / 1000}, duration $duration"
        )

        mPendingIntent = PendingIntent.getBroadcast(
            context,
            id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmInfoIntent = Intent(context, TimerReceiver::class.java)
        val pendingAlarmInfo = PendingIntent.getBroadcast(context,
            id + 1000, alarmInfoIntent, PendingIntent.FLAG_IMMUTABLE)
        val info = AlarmClockInfo(System.currentTimeMillis() + time, pendingAlarmInfo)
        mAlarmMgr.setAlarmClock(info, mPendingIntent)
    }

    private fun ensureNecessaryPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = ContextCompat.getSystemService(context, AlarmManager::class.java)
            if (alarmManager?.canScheduleExactAlarms() == false) {
                Intent().also { intent ->
                    intent.action = Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
                    intent.data = Uri.fromParts("package", context.packageName, null)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TASK
                    context.startActivity(intent)
                }
                return false
            }
        }
        return true
    }

    fun cancel() {
        if (mPendingIntent != null) mAlarmMgr.cancel(mPendingIntent)
    }

}
