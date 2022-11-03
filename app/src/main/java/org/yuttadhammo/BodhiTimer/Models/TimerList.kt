package org.yuttadhammo.BodhiTimer.Models

import org.yuttadhammo.BodhiTimer.Const.SessionTypes
import org.yuttadhammo.BodhiTimer.Models.TimerList
import android.text.TextUtils
import java.lang.Exception
import java.util.ArrayList

class TimerList {
    class Timer {
        val duration: Int
        val uri: String
        val sessionType: SessionTypes

        constructor(mDuration: Int, mUri: String) : super() {
            duration = mDuration
            uri = mUri
            sessionType = SessionTypes.REAL
        }

        constructor(mDuration: Int, mUri: String, mSessionType: SessionTypes) : super() {
            duration = mDuration
            uri = mUri
            sessionType = mSessionType
        }
    }

    val timers: ArrayList<Timer>

    constructor(advTimeString: String) {
        timers = timeStringToList(advTimeString)
    }

    constructor() {
        timers = ArrayList()
    }

    val string: String
        get() = listToTimeString(timers)

    companion object {
        fun timeStringToList(advTimeString: String): ArrayList<Timer> {
            val list = ArrayList<Timer>()
            val advTime = advTimeString.split("\\^").toTypedArray()
            for (s in advTime) {
                //  advTime[n] will be of format timeInMs#pathToSound
                val thisAdvTime = s.split("#").toTypedArray()
                var duration = 0
                try {
                    duration = thisAdvTime[0].toInt()
                    val timer = Timer(duration, thisAdvTime[1])
                    list.add(timer)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            return list
        }

        fun listToTimeString(list: ArrayList<Timer>): String {
            val stringArray = ArrayList<String?>()
            for (timer in list) {
                stringArray.add(timer.duration.toString() + "#" + timer.uri + "#" + timer.sessionType)
            }
            return TextUtils.join("^", stringArray)
        }
    }
}