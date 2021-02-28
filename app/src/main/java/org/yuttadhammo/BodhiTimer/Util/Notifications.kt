package org.yuttadhammo.BodhiTimer.Util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.preference.PreferenceManager
import org.yuttadhammo.BodhiTimer.R



class Notifications {

    companion object {

        const val TAG = "NOTIFY"
        const val ALARM_CHANNEL_ID = "ALARMS"
        const val SERVICE_CHANNEL_ID = "SERVICE"


        fun show(context: Context, time: Int) {
            Log.v(TAG, "Showing notification... $time")

            // Get Notification Manager & Prefs
            val mNotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)


            // Cancel any previous notifications
            mNotificationManager.cancelAll()


            // Construct strings
            val setTimeStr = Time.time2humanStr(context, time)
            val text = context.getText(R.string.Notification)
            val textLatest: CharSequence = String.format(context.getString(R.string.timer_for_x), setTimeStr)


            // Create the notification
            val mBuilder = NotificationCompat.Builder(context.applicationContext, ALARM_CHANNEL_ID)
                    .setSmallIcon(R.drawable.notification)
                    .setContentTitle(text)
                    .setContentText(textLatest)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)

            // Handle light and vibrate in older devices
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                legacyHandler(mBuilder, prefs)
            }
            mNotificationManager.notify(0, mBuilder.build())
        }

        private fun legacyHandler(mBuilder: NotificationCompat.Builder, prefs: SharedPreferences) {
            val vibrate = prefs.getBoolean("Vibrate", true)
            val led = prefs.getBoolean("LED", false)

            // Vibrate
            if (vibrate) {
                mBuilder.setDefaults(Notification.DEFAULT_VIBRATE)
            }

            // Have a light
            if (led) {
                mBuilder.setLights(-0xff0100, 300, 1000)
            }
        }

        fun createNotificationChannel(context: Context) {
            // Get Notification Manager & Prefs
            val mNotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)

            // Create the NotificationChannel, but only on API 26+ because
            // the NotificationChannel class is new and not in the support library
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                var name: CharSequence = context.getString(R.string.alarm_channel_name)
                var description = context.getString(R.string.alarm_channel_description)
                var importance = NotificationManager.IMPORTANCE_HIGH

                val alarmChannel = NotificationChannel(ALARM_CHANNEL_ID, name, importance)
                alarmChannel.description = description

                // Customize
                val vibrate = prefs.getBoolean("Vibrate", true)
                val led = prefs.getBoolean("LED", false)

                // Vibrate
                if (vibrate) {
                    val pattern = longArrayOf(0, 400, 200, 400)
                    alarmChannel.vibrationPattern = pattern
                    alarmChannel.enableVibration(true)
                }

                // Have a light
                if (led) {
                    alarmChannel.lightColor = -0xff0100
                    alarmChannel.enableLights(true)
                }

                // We are playing the sound ourselves,
                // because notification channels don't allow changing sounds.
                alarmChannel.setSound(null, null)


                // Register the channel with the system; you can't change the importance
                // or other notification behaviors after this
                mNotificationManager.createNotificationChannel(alarmChannel)

                name = context.getString(R.string.service_channel_name)
                description = context.getString(R.string.service_channel_description)
                importance = NotificationManager.IMPORTANCE_LOW

                val serviceChannel = NotificationChannel(SERVICE_CHANNEL_ID, name, importance)
                serviceChannel.description = description
                mNotificationManager.createNotificationChannel(serviceChannel)
            }
        }
    }



}