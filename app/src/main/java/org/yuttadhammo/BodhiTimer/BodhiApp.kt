/*
 * BodhiApp.kt
 * Copyright (C) 2014-2022 BodhiTimer developers
 *
 * Distributed under terms of the GNU GPLv3 license.
 */

package org.yuttadhammo.BodhiTimer

import android.app.Application

import android.content.Context
import android.os.Build
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.os.StrictMode.VmPolicy
import timber.log.Timber
import timber.log.Timber.DebugTree

/**
 * The Main class of the Application
 */

class BodhiApp : Application() {

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
