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
package org.yuttadhammo.BodhiTimer.Widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.widget.RemoteViews
import org.yuttadhammo.BodhiTimer.R
import org.yuttadhammo.BodhiTimer.TimerActivity
import timber.log.Timber


class BodhiAppWidgetProvider : AppWidgetProvider() {
    private var isRegistered = false

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        Timber.i("onUpdate")
        if (!isRegistered) {
            context.applicationContext.registerReceiver(this, IntentFilter(Intent.ACTION_SCREEN_ON))
            context.applicationContext.registerReceiver(
                this,
                IntentFilter(Intent.ACTION_SCREEN_OFF)
            )
            isRegistered = true
        }
        context.sendBroadcast(Intent(ACTION_CLOCK_UPDATE))
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        Timber.i("onEnabled")
        if (!isRegistered) {
            context.applicationContext.registerReceiver(this, IntentFilter(Intent.ACTION_SCREEN_ON))
            context.applicationContext.registerReceiver(
                this,
                IntentFilter(Intent.ACTION_SCREEN_OFF)
            )
            isRegistered = true
        }
        context.sendBroadcast(Intent(ACTION_CLOCK_UPDATE))
    }

    override fun onDisabled(context: Context) {
        Timber.i("onDisabled")
        super.onDisabled(context)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        Timber.d("onDeleted")
    }

    override fun onReceive(context: Context, i: Intent) {
        super.onReceive(context, i)
        val action = i.action

        //stopTicking = action.equals(BROADCAST_STOP) || action.equals(Intent.ACTION_SCREEN_OFF);
        doUpdate(context)
    }

    private fun doUpdate(context: Context) {
        Timber.i("updating")
        if (views == null) views = RemoteViews(context.packageName, R.layout.appwidget)
        val intent = Intent(context, TimerActivity::class.java)
        intent.putExtra("set", "true")
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val resources = context.resources
        appWidgetManager = AppWidgetManager.getInstance(context)
        val appWidgets = ComponentName(
            context.packageName,
            "org.yuttadhammo.BodhiTimer.widget.BodhiAppWidgetProvider"
        )
        val widgetIds = appWidgetManager!!.getAppWidgetIds(appWidgets)
        val backgrounds = HashMap<Int, Int>()
        if (widgetIds.isNotEmpty()) {
            for (widgetId in widgetIds) {


                // Get the layout for the App Widget and attach an on-click listener
                // to the button
                views!!.setOnClickPendingIntent(R.id.mainImage, pendingIntent)
                views!!.setImageViewResource(R.id.mainImage, R.drawable.leaf);

                appWidgetManager?.updateAppWidget(widgetId, views)
            }
        }
    }

    companion object {
        private var appWidgetManager: AppWidgetManager? = null
        const val ACTION_CLOCK_UPDATE = "org.yuttadhammo.BodhiTimer.ACTION_CLOCK_UPDATE"
        private var views: RemoteViews? = null
    }
}