/*
 * BodhiApp.kt
 * Copyright (C) 2014-2022 BodhiTimer developers
 *
 * Distributed under terms of the GNU GPLv3 license.
 */

package org.yuttadhammo.BodhiTimer

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.os.StrictMode.VmPolicy
import org.yuttadhammo.BodhiTimer.Const.BroadcastTypes
import org.yuttadhammo.BodhiTimer.Models.AlarmTaskManager
import timber.log.Timber
import timber.log.Timber.DebugTree

/**
 * The Main class of the Application
 */

class BodhiApp : Application() {

    var alarmTaskManager: AlarmTaskManager? = null
    private var initiated: Boolean = false

    init {
        instance = this
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(ThreadPolicy.Builder().detectAll().penaltyLog().build())
            StrictMode.setVmPolicy(VmPolicy.Builder().detectAllExceptSocket().penaltyLog().build())
        }
    }


    override fun onCreate() {
        initiated = true
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(DebugTree())
        }

        Timber.d("onCreate called")

        alarmTaskManager = AlarmTaskManager(this)

        val filter = IntentFilter()
        filter.addAction(BroadcastTypes.BROADCAST_END)
        registerReceiver(alarmEndReceiver, filter)
    }


    // Should move to Manager....
    // receiver to get restart
    private val alarmEndReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Timber.v("Received app alarm callback in App scope")
            Timber.d("id " + intent.getIntExtra("id", -1))
            alarmTaskManager!!.onAlarmEnd(intent.getIntExtra("id", -1))
        }
    }

    companion object {
        var instance: BodhiApp? = null

        fun applicationContext(): Context {
            return instance!!.applicationContext
        }
    }
}

private fun VmPolicy.Builder.detectAllExceptSocket(): VmPolicy.Builder {
    detectLeakedSqlLiteObjects()
    detectActivityLeaks()
    detectLeakedClosableObjects()
    detectLeakedRegistrationObjects()
    detectFileUriExposure()

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        detectContentUriWithoutPermission()
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        detectCredentialProtectedWhileLocked()
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        detectUnsafeIntentLaunch()
        detectIncorrectContextUse()
    }
    return this
}
