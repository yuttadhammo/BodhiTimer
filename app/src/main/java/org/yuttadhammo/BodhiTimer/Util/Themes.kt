/*
 * Themes.kt
 * Copyright (C) 2014-2022 BodhiTimer developers
 *
 * Distributed under terms of the GNU GPLv3 license.
 */

package org.yuttadhammo.BodhiTimer.Util

import android.content.Context
import org.yuttadhammo.BodhiTimer.R

object Themes {
    fun applyTheme(context: Context?) {
        if (context == null) return
        val style = getStyleFromSettings(context)
        // First set the theme (light, dark, etc.)
        context.setTheme(style)
        // Then set an overlay controlling the status bar behaviour etc.
        context.setTheme(R.style.BodhiTheme_Base)
    }

    private fun getStyleFromSettings(context: Context): Int {
        return when (Settings.theme.lowercase()) {
            context.getString(R.string.setting_key_theme_dark) -> {
                R.style.BodhiTheme_Dark
            }
            context.getString(R.string.setting_key_theme_light) -> {
                R.style.BodhiTheme_Light
            }
            context.getString(R.string.setting_key_theme_black) -> {
                R.style.BodhiTheme_Black
            }
            else -> {
                R.style.BodhiTheme_DayNight
            }
        }
    }

}