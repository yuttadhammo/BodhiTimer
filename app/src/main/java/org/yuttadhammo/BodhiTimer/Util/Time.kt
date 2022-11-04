/*
 * Time.kt
 * Copyright (C) 2014-2022 BodhiTimer developers
 *
 * Distributed under terms of the GNU GPLv3 license.
 */

package org.yuttadhammo.BodhiTimer.Util

import android.content.Context
import android.text.TextUtils
import androidx.appcompat.app.AppCompatActivity
import org.yuttadhammo.BodhiTimer.R
import timber.log.Timber
import java.util.regex.Pattern

object Time {
    const val TIME_SEPARATOR = " again "

    private fun msFromNumbers(hour: Int, minutes: Int, seconds: Int): Int {
        return hour * 60 * 60 * 1000 + minutes * 60 * 1000 + seconds * 1000
    }

    @JvmStatic
    fun msFromArray(numbers: IntArray): Int {
        return msFromNumbers(numbers[0], numbers[1], numbers[2])
    }

    private fun padWithZeroes(number: Int): String {
        return if (number > 9) {
            number.toString()
        } else {
            "0$number"
        }
    }

    /**
     * Converts a millisecond time to a string time
     * Not meant to be pretty, but fast..
     *
     * @param ms the time in milliseconds
     * @return the formatted string
     */
    private fun ms2Str(ms: Int): String {
        val time = time2Array(ms)
        return if (time[0] == 0 && time[1] == 0) {
            time[2].toString()
        } else if (time[0] == 0) {
            time[1].toString() + ":" + padWithZeroes(
                time[2]
            )
        } else {
            time[0].toString() + ":" + padWithZeroes(
                time[1]
            ) + ":" + padWithZeroes(time[2])
        }
    }

    /**
     * Creates a time vector
     *
     * @param time the time in milliseconds
     * @return [hour, minutes, seconds, ms]
     */
    @JvmStatic
    fun time2Array(time: Int): IntArray {
        val ms = time % 1000
        var seconds = time / 1000 // 3550000 / 1000 = 3550
        var minutes = seconds / 60 // 59.16666
        var hours = minutes / 60 // 0.9
        if (hours > 60) hours = 60
        minutes %= 60
        seconds %= 60
        val timeVec = IntArray(4)
        timeVec[0] = hours
        timeVec[1] = minutes
        timeVec[2] = seconds
        timeVec[3] = ms
        return timeVec
    }

    @JvmStatic
    fun time2humanStr(context: Context, time: Int): String {
        val timeVec = time2Array(time)
        val hour = timeVec[0]
        val minutes = timeVec[1]
        val seconds = timeVec[2]
        val strList = ArrayList<String?>()
        val res = context.resources

        // string formatting
        if (hour != 0) {
            strList.add(res.getQuantityString(R.plurals.x_hours, hour, hour))
        }
        if (minutes != 0) {
            strList.add(res.getQuantityString(R.plurals.x_mins, minutes, minutes))
        }
        if (seconds != 0 || seconds >= 0 && minutes == 0 && hour == 0) {
            strList.add(res.getQuantityString(R.plurals.x_secs, seconds, seconds))
        }
        return TextUtils.join(", ", strList)
    }

    fun time2hms(time: Int): String {
        return ms2Str(time)
    }

    @JvmStatic
    fun str2complexTimeString(activity: AppCompatActivity, numberString: String): String {
        val out: String
        val stringArray = ArrayList<String?>()
        val strings = numberString.split(TIME_SEPARATOR).toTypedArray()
        for (string in strings) {
            val atime = str2timeString(activity, string)
            if (atime > 0) stringArray.add(atime.toString() + "#sys_def#" + activity.getString(R.string.sys_def))
        }
        out = TextUtils.join("^", stringArray)
        return out
    }

    @JvmStatic
    fun str2timeString(activity: AppCompatActivity, numberString: String): Int {
        val res = activity.resources
        val numbers = res.getStringArray(R.array.numbers)
        var newString = numberString

        for ((position, number) in numbers.withIndex()) {
            val num = 60 - position
            newString = newString.replace(number.toRegex(), num.toString())
        }

        val HOUR = Pattern.compile("([0-9]+) " + activity.getString(R.string.hour))
        val MINUTE = Pattern.compile("([0-9]+) " + activity.getString(R.string.minute))
        val SECOND = Pattern.compile("([0-9]+) " + activity.getString(R.string.second))

        var hours = 0
        var minutes = 0
        var seconds = 0
        var m = HOUR.matcher(newString)
        while (m.find()) {
            val match = m.group(1)
            hours += match?.toInt() ?: 0
        }
        m = MINUTE.matcher(newString)
        while (m.find()) {
            val match = m.group(1)
            minutes += match?.toInt() ?: 0
        }
        m = SECOND.matcher(newString)
        while (m.find()) {
            val match = m.group(1)
            seconds += match?.toInt() ?: 0
        }
        Timber.d("Got numbers: $hours hours, $minutes minutes, $seconds seconds")
        var total = hours * 60 * 60 * 1000 + minutes * 60 * 1000 + seconds * 1000
        if (total > 60 * 60 * 60 * 1000 + 59 * 60 * 1000 + 59 * 1000) total =
            60 * 60 * 60 * 1000 + 59 * 60 * 1000 + 59 * 1000
        return total
    }
}