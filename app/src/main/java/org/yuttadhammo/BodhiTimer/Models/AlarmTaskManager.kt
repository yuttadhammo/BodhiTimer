package org.yuttadhammo.BodhiTimer.Models

import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.text.TextUtils
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.preference.PreferenceManager
import org.yuttadhammo.BodhiTimer.Const.BroadcastTypes
import org.yuttadhammo.BodhiTimer.Const.BroadcastTypes.BROADCAST_RESET
import org.yuttadhammo.BodhiTimer.Const.SessionTypes
import org.yuttadhammo.BodhiTimer.Const.TimerState.PAUSED
import org.yuttadhammo.BodhiTimer.Const.TimerState.RUNNING
import org.yuttadhammo.BodhiTimer.Const.TimerState.STOPPED
import org.yuttadhammo.BodhiTimer.Const.TimerState.getText
import org.yuttadhammo.BodhiTimer.Service.SoundService
import org.yuttadhammo.BodhiTimer.Util.Settings
import org.yuttadhammo.BodhiTimer.Util.Time
import timber.log.Timber
import java.util.Date
import java.util.Stack
import java.util.Timer
import java.util.TimerTask


class AlarmTaskManager(private val mApp: Application) : AndroidViewModel(mApp) {

    private val mTimer = Timer()
    private val alarms: Stack<AlarmTask> = Stack()
    private var lastId = 0

    @JvmField
    var appIsPaused = false

    // Data
    private var timeStamp: Long = 0
    private var sessionTimeStamp: Long = 0
    private var sessionDuration = 0
    private var sessionTimeLeft = 0

    // Live Data
    private val currentTimerLeft = MutableLiveData(-1)
    private val currentTimerDuration = MutableLiveData(-1)
    private val mIndex = MutableLiveData<Int>()
    private val timerText = MutableLiveData<String>()
    private val previewText = MutableLiveData<String>()
    private val mCurrentState = MutableLiveData(-1)
    private var lastTextGenerated = 0

    // Accessors
    val curTimerLeft: LiveData<Int>
        get() = currentTimerLeft
    val curTimerDuration: LiveData<Int>
        get() = currentTimerDuration
    val currentState: LiveData<Int>
        get() = mCurrentState

    val curTimerLeftVal: Int
        get() = currentTimerLeft.value!!
    val curTimerDurationVal: Int
        get() = currentTimerDuration.value!!

    private var previousInterruptionFilter: Int = 0

    private fun setCurrentState(newState: Int) {
        Timber.v("Entering state: " + getText(newState))
        val editor = prefs.edit()
        editor.putInt("State", newState)
        editor.apply()
        mCurrentState.value = newState
    }

    fun getTimerText(): LiveData<String> {
        return timerText
    }

    fun getPreviewText(): LiveData<String> {
        return previewText
    }

    private fun setCurTimerDuration(newDuration: Int) {
        currentTimerDuration.value = Integer.valueOf(newDuration)
    }

    private fun setCurTimerLeft(newElapsed: Int) {
        currentTimerLeft.value = Integer.valueOf(newElapsed)
    }

    private fun setIndex(newIndex: Int) {
        mIndex.value = Integer.valueOf(newIndex)
    }

    private val prefs: SharedPreferences

    override fun onCleared() {
        super.onCleared()
        Timber.e("View model cleared")
        saveState()
    }

    private fun saveState() {
        val editor = prefs.edit()
        editor.putInt("CurrentTimerDuration", curTimerDurationVal)
        editor.putInt("CurrentTimeLeft", curTimerLeftVal)
        editor.putInt("State", mCurrentState.value!!)
        editor.putInt("SessionDuration", sessionDuration)
        editor.putInt("SessionTimeLeft", sessionTimeLeft)
        editor.apply()
    }

    private fun restoreState() {
        setCurTimerDuration(prefs.getInt("CurrentTimerDuration", DEFAULT_DURATION))
        setCurTimerLeft(prefs.getInt("CurrentTimeLeft", DEFAULT_DURATION))
        sessionDuration = prefs.getInt("SessionDuration", DEFAULT_DURATION)
        sessionTimeLeft = prefs.getInt("SessionTimeLeft", DEFAULT_DURATION)
        mCurrentState.value = prefs.getInt("State", 1)
    }

    @JvmOverloads
    fun loadLastTimers(offset: Int = 0) {
        // Populate the AlarmManager with our last used timers
        addAlarms(retrieveTimerList(), offset)
    }

    /**
     * Show an alarm for a certain date when the alarm is called it will pop up a notification
     */
    private fun addAlarmWithUri(
        offset: Int,
        duration: Int,
        uri: String,
        sessionType: SessionTypes
    ): AlarmTask {
        Timber.i("Creating new alarm task, uri " + uri + " type: " + sessionType + " due in " + (duration + offset))
        val alarm = AlarmTask(mApp.applicationContext, offset, duration)
        alarm.uri = uri
        alarm.sessionType = sessionType
        lastId++
        alarm.id = lastId
        alarms.push(alarm)
        updateTimerText()
        updatePreviewText()
        return alarm
    }

    fun addAlarms(list: TimerList, offset: Int) {
        // If adding a complete list, clear all previous alarms
        var offset = offset
        cancelAllAlarms(true)
        var totalDuration = 0
        lastId = 0
        for (timer in list.timers) {
            val duration = timer.duration

            // Don't add elapsed timers, but increase the id,
            // to make sure that on App resume the alarms have the correct id.
            if (duration + offset < 0) {
                lastId++
                continue
            }
            addAlarmWithUri(offset, duration, timer.uri, timer.sessionType)
            offset += duration
            totalDuration += totalDuration
        }
        resetCurrentAlarm()
    }

    private fun startAllAlarms() {
        for (alarm in alarms) {
            alarm.run()
        }
    }

    fun startAll() {
        var sessionDur = 0
        for (alarm in alarms) {
            alarm.run()
            sessionDur += alarm.duration
        }
        val dur = curTimerDurationVal
        sessionDuration = sessionDur
        setSessionTimeStamp(sessionDur)
        startTicker(dur)
        Timber.v("Started ticker & timer, first duration: $dur")
    }

    private fun resetCurrentAlarm() {
        val dur = currentAlarmDuration
        setCurTimerDuration(dur)
        if (mCurrentState.value != PAUSED) {
            setCurTimerLeft(dur)
        }
        updateTimerText()
    }

    private val currentAlarmDuration: Int
        get() = if (!alarms.empty()) {
            val (_, _, duration) = alarms.firstElement()
            duration
        } else {
            0
        }

    val totalDuration: Int
        get() {
            var sessionDur = 0
            for ((_, _, duration) in alarms) {
                sessionDur += duration
            }
            return sessionDur
        }

    private fun cancelAllAlarms(clear: Boolean) {
        for (alarm in alarms) {
            alarm.cancel()
        }
        if (clear) {
            alarms.clear()
        }
        updateTimerText()
        updatePreviewText()
    }

    private val previewTimes: ArrayList<Int>
        get() {
            val previewTimes = ArrayList<Int>()
            for (i in 1 until alarms.size) {
                val (_, _, duration) = alarms.elementAt(i)
                previewTimes.add(duration)
            }
            return previewTimes
        }

    /**
     * Starts the timer at the given time
     *
     * @param time with which to count down
     */
    private fun startTicker(time: Int) {
        Timber.v("Starting the ticker: $time")
        setCurrentState(RUNNING)
        mCurrentState.value = RUNNING
        setCurTimerLeft(time)
        setTimeStamp(time)
        mTimer.schedule(
            object : TimerTask() {
                override fun run() {
                    mHandler.sendEmptyMessage(0)
                }
            },
            TIMER_TIC.toLong(),
            TIMER_TIC.toLong()
        )

        startDND()
        startService()
    }

    /**
     * Stops the timer
     */
    private fun stopTicker() {
        Timber.v("Timer stopped")
        clearTime()
        cancelAllAlarms(false)
        stopDND()
        stopService()

        // Stop our timer service
        setCurrentState(STOPPED)
    }

    fun timerUnPause() {
        // How far have we elapsed?
        val sessionLeft = prefs.getInt("SessionTimeLeft", 0)
        val currentTimerLeft = prefs.getInt("CurrentTimeLeft", 0)
        val timeElapsed = sessionDuration - sessionLeft

        // Setup the alarms
        loadLastTimers(-timeElapsed)
        timerResume(currentTimerLeft)
        startAllAlarms()
    }

    /**
     * Resume the time after being paused
     */
    fun timerResume() {
        Timber.v("Resuming the timer...")
        startTicker(curTimerLeftVal)
        setCurrentState(RUNNING)
    }

    /**
     * Resume the time after being paused
     */
    private fun timerResume(timeLeft: Int) {
        Timber.v("Resuming the timer...")
        startTicker(timeLeft)
        setCurrentState(RUNNING)
    }

    /**
     * Resume the time after being paused
     */
    private fun timerResume(timeLeft: Int, curDuration: Int) {
        Timber.v("Resuming the timer...")
        currentTimerDuration.value = curDuration
        startTicker(timeLeft)
        setCurrentState(RUNNING)
    }

    /**
     * Pause the timer and stop the timer service
     */
    fun timerPause() {
        Timber.v("Pausing the timer...")
        saveState()
        cancelAllAlarms(false)
        setCurrentState(PAUSED)
    }

    /**
     * Clears the time, sets the image and label to default
     */
    private fun clearTime() {
        setCurTimerLeft(0)
        setIndex(0)
    }

    /**
     * Cancels the alarm portion of the timer
     */
    fun stopAlarmsAndTicker() {
        Timber.v("Stopping Alarms and Ticker ...")
        cancelAllAlarms(false)
        stopTicker()
        clearTime()
    }

    /**
     * HELPER FUNCTIONS
     */
    fun startAlarms(list: TimerList) {
        addAlarms(list, 0)
        startAll()
    }

    fun saveTimerList(tL: TimerList) {
        val editor = prefs.edit()
        val ret = tL.string
        Timber.v("Saved timer string: $ret")
        editor.putString("timeString", tL.string)
        editor.apply()
    }

    private val timeString: String
        get() {
            var prefString = Settings.timeString
            if (prefString == "") prefString = Settings.advTimeString
            return prefString
        }

    fun retrieveTimerList(): TimerList {
        val prefString = timeString
        Timber.v("Got timer string: $prefString from Settings")
        return TimerList(prefString)
    }

    /**
     * TICKING
     */
    fun doTick() {
        if (mCurrentState.value != RUNNING || appIsPaused) return
        val now = Date()
        val nextAlarm = Date(timeStamp)
        val sessionEnd = Date(sessionTimeStamp)
        val timeLeft = nextAlarm.time - now.time

        sessionTimeLeft = (sessionEnd.time - now.time).toInt()

        if (timeLeft < 0) {
            if (alarms.size > 0) {
                Timber.e("Tick cycled ended")
            } else {
                Timber.e("Error: Time up. This probably means that the Broadcast was not received")
                stopTicker()
            }
        } else {
            currentTimerLeft.value = timeLeft.toInt()
            updateTimerText(timeLeft.toInt())

            // Internal thread to properly update the GUI
            mTimer.schedule(object : TimerTask() {
                override fun run() {
                    mHandler.sendEmptyMessage(0)
                }
            }, TIMER_TIC.toLong())
        }
    }

    private fun updateTimerText(timeLeft: Int = curTimerLeftVal) {
        // Calculate text only if time has changed
        val rounded = timeLeft / 1000 * 1000
        if (lastTextGenerated != rounded) {
            lastTextGenerated = rounded
            timerText.value = Time.time2hms(rounded)
        }
    }

    private fun updatePreviewText() {
        val arr = makePreviewArray()
        val oldText = previewText.value
        val newText = TextUtils.join("\n", arr)
        if (newText != oldText) {
            previewText.value = newText
        }
    }

    private fun makePreviewArray(): ArrayList<String?> {
        val arr = ArrayList<String?>()
        val previewTimes = previewTimes
        for (i in previewTimes.indices) {
            if (i >= 2) {
                arr.add("...")
                break
            }
            arr.add(Time.time2hms(previewTimes[i]))
        }
        return arr
    }

    /**
     * Handler for the message from the timer service
     */
    private val mHandler: Handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            doTick()
        }
    }

    private fun getAlarmById(id: Int): AlarmTask? {
        var found: AlarmTask? = null
        for (alarm in alarms) {
            if (alarm.id == id) found = alarm
        }
        return found
    }

    fun onAlarmEnd(id: Int) {
        val left = alarms.size - 1
        Timber.v("Alarm has ended. There are $left alarms left")
        val alarm = getAlarmById(id) ?: return

        // Remove alarm
        alarms.remove(alarm)

        // Update labels
        if (alarms.empty()) {
            stopAlarmsAndTicker()
            loadLastTimers()
            // Send message to activity,
            // in case AutoRepeat is on.
            handleAutoRepeat()
        } else {
            switchToTimer(alarms.firstElement())
        }
        updateTimerText()
        updatePreviewText()
    }

    private fun handleAutoRepeat() {
        if (prefs.getBoolean("AutoRestart", false)) {
            Timber.i("AUTO RESTART")
            startAlarms(retrieveTimerList())
            setCurrentState(RUNNING)
        }
    }

    private fun switchToTimer(alarm: AlarmTask) {
        val duration = alarm.duration
        setCurTimerDuration(duration)
        setTimeStamp(duration)
        setCurTimerLeft(0)
        doTick()
        val broadcast = Intent()
        broadcast.putExtra("time", duration)
        broadcast.action = BROADCAST_RESET
        mApp.sendBroadcast(broadcast)
    }

    private fun setTimeStamp(duration: Int) {
        Timber.v("Next alarm will finish in: $duration")
        val editor = prefs.edit()
        timeStamp = Date().time + duration

        // Save new time
        editor.putLong("TimeStamp", timeStamp)
        editor.apply()
    }

    private fun setSessionTimeStamp(duration: Int) {
        Timber.v("Session will finish in: $duration")
        val editor = prefs.edit()
        sessionTimeStamp = Date().time + duration

        // Save new time
        editor.putLong("SessionTimeStamp", sessionTimeStamp)
        editor.apply()
    }

    private fun startDND() {
        if (Settings.doNotDisturb) {
            try {
                val mNotificationManager =
                    mApp.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                previousInterruptionFilter = mNotificationManager.currentInterruptionFilter
                mNotificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }

    private fun stopDND() {
        if (Settings.doNotDisturb) {
            try {
                val newFilter = if (previousInterruptionFilter != 0)
                    previousInterruptionFilter else NotificationManager.INTERRUPTION_FILTER_ALL
                val mNotificationManager =
                    mApp.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                mNotificationManager.setInterruptionFilter(newFilter)
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }

    private fun startService() {
        if (!prefs.getBoolean("useOldNotification", false)) {
            val startIntent = Intent(mApp.applicationContext, SoundService::class.java)
            mApp.applicationContext.startService(startIntent)
        }

    }

    private fun stopService() {
        if (!prefs.getBoolean("useOldNotification", false)) {
            mApp.sendBroadcast(Intent(BroadcastTypes.BROADCAST_STOP))
        }
    }


    init {
        updatePreviewText()

        prefs = PreferenceManager.getDefaultSharedPreferences(mApp.applicationContext)
        restoreState()

        when (mCurrentState.value) {
            RUNNING -> {
                Timber.i("CREATE, state RUNNING")

                // We are resuming the app while timers are (presumably) still active
                val sessionTimeStamp = prefs.getLong("SessionTimeStamp", -1)
                val curTimerStamp = prefs.getLong("TimeStamp", -1)
                val now = Date()
                val sessionEnd = Date(sessionTimeStamp)
                val curTimerEnd = Date(curTimerStamp)

                // We still have timers running!
                if (sessionEnd.after(now)) {
                    Timber.i("Still have timers")
                    val sessionTimeLeft = (sessionEnd.time - now.time).toInt()
                    val curTimerLeft = (curTimerEnd.time - now.time).toInt()
                    val sessionDuration = prefs.getInt("SessionDuration", -1)
                    Timber.i("Session Time Left $sessionTimeLeft")
                    //Timber.i("Cur Time Left " + curTimerLeft);
                    Timber.i("SessionDuration $sessionDuration")
                    val timeElapsed = sessionDuration - sessionTimeLeft

                    // RECREATE ALARMS if empty, but we are running.
                    // THEY WILL HAVE WRONG IDS.....
                    Timber.i("Trying to recreate alarms")
                    loadLastTimers(-timeElapsed)

                    // Resume ticker at correct position
                    // Get duration of current alarm
                    val curTimerDuration = currentAlarmDuration
                    Timber.i("Setting timer: $curTimerLeft of $curTimerDuration")
                    timerResume(curTimerLeft, curTimerDuration)
                } else {
                    Timber.i("Resumed to RUNNING, but all timers are over")
                    loadLastTimers()
                }
                loadLastTimers()
            }

            STOPPED -> {
                Timber.i("CREATE, state STOPPED")
                loadLastTimers()
            }

            PAUSED -> {
                Timber.i("CREATE, state PAUSED")
                val sessionLeft = prefs.getInt("SessionTimeLeft", 0)
                val timeElapsed = sessionDuration - sessionLeft

                // Setup the alarms
                loadLastTimers(-timeElapsed)
            }
        }
    }

    companion object {
        // Update rate of the internal timer
        const val TIMER_TIC = 100

        const val DEFAULT_DURATION = 120000
    }
}