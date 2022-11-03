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

import android.appwidget.AppWidgetProvider
import android.appwidget.AppWidgetManager
import timber.log.Timber
import android.content.IntentFilter
import android.content.Intent
import org.yuttadhammo.BodhiTimer.Widget.BodhiAppWidgetProvider
import android.content.SharedPreferences
import android.widget.RemoteViews
import org.yuttadhammo.BodhiTimer.R
import org.yuttadhammo.BodhiTimer.TimerActivity
import android.app.PendingIntent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.content.ComponentName
import android.content.Context
import androidx.preference.PreferenceManager
import java.util.HashMap

class BodhiAppWidgetProvider : AppWidgetProvider() {
    private var isRegistered = false
    private var widgetIds: IntArray

    //private boolean stopTicking;
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
        doTick()
    }

    private fun doUpdate(context: Context) {
        Timber.i("updating")
        val mSettings = PreferenceManager.getDefaultSharedPreferences(context)
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
        val originalBitmap = BitmapFactory.decodeResource(resources, R.drawable.leaf)
        appWidgetManager = AppWidgetManager.getInstance(context)
        val appWidgets = ComponentName(
            context.packageName,
            "org.yuttadhammo.BodhiTimer.widget.BodhiAppWidgetProvider"
        )
        widgetIds = appWidgetManager.getAppWidgetIds(appWidgets)
        val backgrounds = HashMap<Int, Int>()
        if (widgetIds.size > 0) {
            for (widgetId in widgetIds) {

                // Get the layout for the App Widget and attach an on-click listener
                // to the button
                views!!.setOnClickPendingIntent(R.id.mainImage, pendingIntent)

                // set background
                views!!.setImageViewResource(R.id.backImage, themeId)
                backgrounds[widgetId] = themeId
                appWidgetManager.updateAppWidget(widgetId, views)
            }
        }
    }

    private fun doTick() {

//        if (widgetIds.length == 0)
//            return;
//
//        views = new RemoteViews(mContext.getPackageName(), R.layout.appwidget);
//
//        Bitmap bmp = originalBitmap;
//
//        views.setImageViewBitmap(R.id.mainImage, bmp);
//
//        // Tell the widget manager
//        for (int widgetId : widgetIds) {
//            // set background
//            views.setImageViewResource(R.id.backImage, themeId);
//            appWidgetManager.updateAppWidget(widgetId, views);
//        }
    }

    companion object {
        private var appWidgetManager: AppWidgetManager? = null
        private const val TAG = "BodhiAppWidgetProvider"
        const val ACTION_CLOCK_UPDATE = "org.yuttadhammo.BodhiTimer.ACTION_CLOCK_UPDATE"
        private var views: RemoteViews? = null
        private const val themeId = R.drawable.widget_background_black
    }
}