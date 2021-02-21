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

package org.yuttadhammo.BodhiTimer.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.RemoteViews;

import org.yuttadhammo.BodhiTimer.R;
import org.yuttadhammo.BodhiTimer.TimerActivity;

import java.util.HashMap;

public class BodhiAppWidgetProvider extends AppWidgetProvider {

    private static AppWidgetManager appWidgetManager;

    private final static String TAG = "BodhiAppWidgetProvider";
    private boolean isRegistered = false;
    private int[] widgetIds;

    private Bitmap originalBitmap;
    private Context mContext;

    public static final String ACTION_CLOCK_UPDATE = "org.yuttadhammo.BodhiTimer.ACTION_CLOCK_UPDATE";
    private static RemoteViews views;
    private static final int themeId =  R.drawable.widget_background_black;
    //private boolean stopTicking;

    public void onUpdate(Context context, final AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.i(TAG, "onUpdate");

        if (!isRegistered) {
            context.getApplicationContext().registerReceiver(this, new IntentFilter(Intent.ACTION_SCREEN_ON));
            context.getApplicationContext().registerReceiver(this, new IntentFilter(Intent.ACTION_SCREEN_OFF));
            isRegistered = true;
        }
        context.sendBroadcast(new Intent(ACTION_CLOCK_UPDATE));
    }

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        Log.i(TAG, "onEnabled");
        if (!isRegistered) {
            context.getApplicationContext().registerReceiver(this, new IntentFilter(Intent.ACTION_SCREEN_ON));
            context.getApplicationContext().registerReceiver(this, new IntentFilter(Intent.ACTION_SCREEN_OFF));
            isRegistered = true;
        }
        context.sendBroadcast(new Intent(ACTION_CLOCK_UPDATE));
    }

    @Override
    public void onDisabled(Context context) {
        Log.i(TAG, "onDisabled");
        super.onDisabled(context);
    }

    public void onDeleted(Context context, int[] appWidgetIds) {
        Log.d(TAG, "onDeleted");

    }

    @Override
    public void onReceive(Context context, Intent i) {
        super.onReceive(context, i);

        final String action = i.getAction();

        //stopTicking = action.equals(BROADCAST_STOP) || action.equals(Intent.ACTION_SCREEN_OFF);

        doUpdate(context);
        doTick();
    }

    private void doUpdate(Context context) {
        Log.i(TAG, "updating");

        SharedPreferences mSettings = PreferenceManager.getDefaultSharedPreferences(context);
        mContext = context;
        if (views == null)
            views = new RemoteViews(context.getPackageName(), R.layout.appwidget);

        Intent intent = new Intent(context, TimerActivity.class);
        intent.putExtra("set", "true");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

       Resources resources = context.getResources();
       originalBitmap = BitmapFactory.decodeResource(resources, R.drawable.leaf);


        appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName appWidgets = new ComponentName(context.getPackageName(), "org.yuttadhammo.BodhiTimer.widget.BodhiAppWidgetProvider");
        widgetIds = appWidgetManager.getAppWidgetIds(appWidgets);

        HashMap<Integer, Integer> backgrounds = new HashMap<>();
        if (widgetIds.length > 0) {
            for (int widgetId : widgetIds) {

                // Get the layout for the App Widget and attach an on-click listener
                // to the button
                views.setOnClickPendingIntent(R.id.mainImage, pendingIntent);

                // set background
                 views.setImageViewResource(R.id.backImage, themeId);
                 backgrounds.put(widgetId, themeId);
                appWidgetManager.updateAppWidget(widgetId, views);
            }
        }
    }


    private void doTick() {

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





}