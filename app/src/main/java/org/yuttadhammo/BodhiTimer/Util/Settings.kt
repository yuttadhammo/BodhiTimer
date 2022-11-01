/*
 * Settings.kt
 * Copyright (C) 2009-2022 Ultrasonic developers
 *
 * Distributed under terms of the GNU GPLv3 license.
 */

package org.yuttadhammo.BodhiTimer.Util

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import org.yuttadhammo.BodhiTimer.BodhiApp
import org.yuttadhammo.BodhiTimer.IntSetting
import org.yuttadhammo.BodhiTimer.R
import org.yuttadhammo.BodhiTimer.StringIntSetting
import org.yuttadhammo.BodhiTimer.StringSetting

/**
 * Contains convenience functions for reading and writing preferences
 */
object Settings {

    @JvmStatic
    val preferences: SharedPreferences
        get() = PreferenceManager.getDefaultSharedPreferences(appContext)


    @JvmStatic
    var theme by StringSetting(
        getKey(R.string.setting_key_theme),
        getKey(R.string.setting_key_theme_day_night)
    )

    var circleTheme by StringIntSetting(
        "CircleTheme",
        3
    )

    var drawingIndex by IntSetting(
        "DrawingIndex",
        1
    )


    fun hasKey(key: String): Boolean {
        return preferences.contains(key)
    }

    private fun getKey(key: Int): String {
        return appContext.getString(key)
    }

    fun getAllKeys(): List<String> {
        val prefs = PreferenceManager.getDefaultSharedPreferences(BodhiApp.applicationContext())
        return prefs.all.keys.toList()
    }

    private val appContext: Context
        get() = BodhiApp.applicationContext()

}
