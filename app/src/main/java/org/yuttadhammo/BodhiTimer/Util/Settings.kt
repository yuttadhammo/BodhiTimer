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
import org.yuttadhammo.BodhiTimer.R

/**
 * Contains convenience functions for reading and writing preferences
 */
const val DEFAULT_DURATION = 120000

object Settings {

    val customBmp by BooleanSetting(
        "custom_bmp",
        false
    )
    var bmpUri by StringSetting(
        "bmp_url"
    )
    val doNotDisturb by BooleanSetting(
        "doNotDisturb",
        false
    )
    val hideTime by BooleanSetting(
        "hideTime",
        false
    )
    val switchTimeMode by BooleanSetting(
        "SwitchTimeMode",
        false
    )
    var preFileUri by StringSetting(
        "PreFileUri",
        ""
    )
    var preSystemUri by StringSetting(
        "PreSystemUri",
        ""
    )
    val preparationTime by IntSetting(
        "preparationTime",
        0
    )

    val preSoundUri by StringSetting(
        "PreSoundUri", ""
    )
    val notificationUri by StringSetting(
        "NotificationUri", Sounds.DEFAULT_SOUND
    )
    var systemUri by StringSetting(
        "SystemUri", Sounds.DEFAULT_SOUND
    )
    var fileUri by StringSetting(
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

    val isDarkTheme: Boolean
        get() = (theme == getKey(R.string.setting_key_theme_dark) ||
                theme == getKey(R.string.setting_key_theme_black))

    var drawingIndex by IntSetting(
        "DrawingIndex",
        1
    )

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
        ""
    )

    var timeString by StringSetting(
        "timeString",
        ""
    )

    var lastWasSimple by BooleanSetting(
        "LastWasSimple",
        true
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
