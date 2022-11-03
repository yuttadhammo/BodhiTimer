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
import org.yuttadhammo.BodhiTimer.BooleanSetting
import org.yuttadhammo.BodhiTimer.IntSetting
import org.yuttadhammo.BodhiTimer.R
import org.yuttadhammo.BodhiTimer.StringIntSetting
import org.yuttadhammo.BodhiTimer.StringSetting

/**
 * Contains convenience functions for reading and writing preferences
 */
const val DEFAULT_DURATION = 120000
const val DEFAULT_TIME_STRING = "$DEFAULT_DURATION#sys_def"

object Settings {

    val hideTime by BooleanSetting("hideTime", false)
    val switchTimeMode by BooleanSetting(
    "SwitchTimeMode",
    false
    )
    val preFileUri by StringSetting("PreFileUri", "")
    val preSystemUri by StringSetting("PreSystemUri", "")
    val preparationTime by IntSetting("preparationTime", 0)
    val preSoundUri by StringSetting(
    "PreSoundUri", ""
    )
    val notificationUri by StringSetting(
    "NotificationUri", Sounds.DEFAULT_SOUND
    )
    val systemUri by StringSetting(
        "SystemUri", Sounds.DEFAULT_SOUND
    )
    val fileUri by StringSetting(
        "FileUri", Sounds.DEFAULT_SOUND
    )
    val speakTime by BooleanSetting(
   "SpeakTime", false
    )
    val wakeLock by BooleanSetting(
            "WakeLock",
            false
    )

    var lastSimpleTime by IntSetting(
        "LastSimpleTime",
        DEFAULT_DURATION
    )

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

    var preset1 by StringSetting(
    "pre1"
    )
    var preset2 by StringSetting(
        "pre2"
    )
    var preset3 by StringSetting(
        "pre3"
    )
    var preset4 by StringSetting(
        "pre4"
    )

    var fullscreen by BooleanSetting(
        "FULLSCREEN",
        false
    )

    var advTimeString by StringSetting(
        "advTimeString",
        DEFAULT_TIME_STRING
    )

    var timeString by StringSetting(
        "timeString",
        ""
    )

    var lastWasSimple by BooleanSetting(
        "LastWasSimple",
        true
    )

    private val appContext: Context
        get() = BodhiApp.applicationContext()

}
