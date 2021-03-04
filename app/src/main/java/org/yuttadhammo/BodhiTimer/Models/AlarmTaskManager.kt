package org.yuttadhammo.BodhiTimer.Models

import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.text.TextUtils
import android.util.Log
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
import org.yuttadhammo.BodhiTimer.Service.SoundService
import org.yuttadhammo.BodhiTimer.Util.Time
import java.util.*



class AlarmTaskManager(val mApp: Application) : AndroidViewModel(mApp) {

    private val mTimer = Timer()
    private val alarms: Stack<AlarmTask> = Stack()
    private var lastId = 0

    @JvmField
    var appIsPaused = false

    // Data
    var timeStamp: Long = 0
    private var sessionTimeStamp: Long = 0
    var sessionDuration = 0
    private var sessionTimeLeft = 0

    // Live Data
    private val currentTimerLeft = MutableLiveData<Int>()
    private val currentTimerDuration = MutableLiveData<Int>()
    private val timerText = MutableLiveData<String>()
    private val previewText = MutableLiveData<String>()
    private val mIndex = MutableLiveData<Int>()
    private val mCurrentState = MutableLiveData<Int>()
    private var lastTextGenerated = 0

    // Accessors
    val curTimerLeft: LiveData<Int>
        get() = currentTimerLeft
    val curTimerDuration: LiveData<Int>
        get() = currentTimerDuration
    val currentState: LiveData<Int>
        get() = mCurrentState

    private fun setCurrentState(newState: Int) {
        Log.v(TAG, "Entering state: $newState")
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

    val curTimerDurationVal: Int
        get() = currentTimerDuration.value!!
    val curTimerLeftVal: Int?
        get() = currentTimerLeft.value
    val indexVal: Int?
        get() = mIndex.value

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
        Log.i(TAG, "View model cleared")
        saveState()
    }

    fun saveState() {
        val editor = prefs.edit()
        editor.putInt("CurrentTimerDuration", curTimerDurationVal)
        editor.putInt("CurrentTimeLeft", curTimerLeftVal!!)
        editor.putInt("State", mCurrentState.value!!)
        editor.putInt("SessionDuration", sessionDuration)
        editor.putInt("SessionTimeLeft", sessionTimeLeft)
        editor.apply()
    }

    fun restoreState() {
        setCurTimerDuration(prefs.getInt("CurrentTimerDuration", 0))
        setCurTimerLeft(prefs.getInt("CurrentTimeLeft", 0))
        sessionDuration = prefs.getInt("SessionDuration", 0)
        sessionTimeLeft = prefs.getInt("SessionTimeLeft", 0)
        mCurrentState.value = prefs.getInt("State", 0)
    }

    @JvmOverloads
    fun loadLastTimers(offset: Int = 0) {
        // Populate the AlarmManager with our last used timers
        addAlarms(retrieveTimerList(), offset)
    }

    /**
     * Show an alarm for a certain date when the alarm is called it will pop up a notification
     */
    private fun addAlarmWithUri(offset: Int, duration: Int, uri: String, sessionType: SessionTypes): AlarmTask {
        Log.i(TAG, "Creating new alarm task, uri " + uri + " type: " + sessionType + " due in " + (duration + offset))
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
        Log.v(TAG, "Started ticker & timer, first duration: $dur")
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

    val alarmCount: Int
        get() = alarms.size

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
        Log.v(TAG, "Starting the ticker: $time")
        setCurrentState(RUNNING)
        mCurrentState.setValue(RUNNING)
        setCurTimerLeft(time)
        setTimeStamp(time)
        mTimer.schedule(
                object : TimerTask() {
                    override fun run() {
                        mHandler?.sendEmptyMessage(0)
                    }
                },
                TIMER_TIC.toLong(),
                TIMER_TIC.toLong())

        startDND()
        startService()
    }

    /**
     * Stops the timer
     */
    private fun stopTicker() {
        Log.v(TAG, "Timer stopped")
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
        Log.v(TAG, "Resuming the timer...")
        startTicker(currentTimerLeft.value!!)
        setCurrentState(RUNNING)
    }

    /**
     * Resume the time after being paused
     */
    private fun timerResume(timeLeft: Int) {
        Log.v(TAG, "Resuming the timer...")
        startTicker(timeLeft)
        setCurrentState(RUNNING)
    }

    /**
     * Resume the time after being paused
     */
    private fun timerResume(timeLeft: Int, curDuration: Int) {
        Log.v(TAG, "Resuming the timer...")
        currentTimerDuration.value = curDuration
        startTicker(timeLeft)
        setCurrentState(RUNNING)
    }

    /**
     * Pause the timer and stop the timer service
     */
    fun timerPause() {
        Log.v(TAG, "Pausing the timer...")
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
        Log.v(TAG, "Stopping Alarms and Ticker ...")
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
        Log.v(TAG, "Saved timer string: $ret")
        editor.putString("timeString", tL.string)
        editor.apply()
    }

    private val timeString: String?
        private get() {
            var prefString = prefs.getString("timeString", "")
            if (prefString == "") prefString = prefs.getString("advTimeString", "120000#sys_def")
            return prefString
        }

    fun retrieveTimerList(): TimerList {
        val prefString = timeString
        val tL = TimerList(prefString)
        Log.v(TAG, "Got timer string: $prefString from Settings")
        return tL
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
                Log.e(TAG, "Tick cycled ended")
            } else {
                Log.e(TAG, "Error: Time up. This probably means that the Broadcast was not received")
                stopTicker()
            }
        } else {
            currentTimerLeft.value = timeLeft.toInt()
            updateTimerText(timeLeft.toInt())

            // Internal thread to properly update the GUI
            mTimer.schedule(object : TimerTask() {
                override fun run() {
                    mHandler?.sendEmptyMessage(0)
                }
            }, TIMER_TIC.toLong())
        }
    }

    private fun updateTimerText(timeLeft: Int = currentTimerLeft.value!!) {
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
    private val mHandler: Handler? = object : Handler(Looper.getMainLooper()) {
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
        Log.v(TAG, "Alarm has ended. There are $left alarms left")
        val alarm = getAlarmById(id) ?: return

        // Remove alarm
        alarms.remove(alarm)

        // Update labels
        if (alarms.empty()) {

            // Send message to activity,
            // in case AutoRepeat is on.
            handleAutoRepeat()
            stopDND()
            stopService()
            stopAlarmsAndTicker()
            loadLastTimers()
        } else {
            switchToTimer(alarms.firstElement())
        }
        updateTimerText()
        updatePreviewText()
    }

    private fun handleAutoRepeat() {
        if (prefs.getBoolean("AutoRestart", false)) {
            Log.i(TAG, "AUTO RESTART")
            stopAlarmsAndTicker()
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
        Log.v(TAG, "Next alarm will finish in: $duration")
        val editor = prefs.edit()
        timeStamp = Date().time + duration

        // Save new time
        editor.putLong("TimeStamp", timeStamp)
        editor.apply()
    }

    private fun setSessionTimeStamp(duration: Int) {
        Log.v(TAG, "Session will finish in: $duration")
        val editor = prefs.edit()
        sessionTimeStamp = Date().time + duration

        // Save new time
        editor.putLong("SessionTimeStamp", sessionTimeStamp)
        editor.apply()
    }

    private fun startDND() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && prefs.getBoolean("doNotDisturb", false)) {
            try {
                val mNotificationManager = mApp.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                mNotificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
            } catch (e: Exception) {
                Log.e(TAG, e.toString())
            }
        }
    }

    private fun stopDND() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && prefs.getBoolean("doNotDisturb", false)) {
            try {
                val mNotificationManager = mApp.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                mNotificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
            } catch (e: Exception) {
                Log.e(TAG, e.toString())
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
                Log.i(TAG, "CREATE, state RUNNING")

                // We are resuming the app while timers are (presumably) still active
                val sessionTimeStamp = prefs.getLong("SessionTimeStamp", -1)
                val curTimerStamp = prefs.getLong("TimeStamp", -1)
                val now = Date()
                val sessionEnd = Date(sessionTimeStamp)
                val curTimerEnd = Date(curTimerStamp)

                // We still have timers running!
                if (sessionEnd.after(now)) {
                    Log.i(TAG, "Still have timers")
                    val sessionTimeLeft = (sessionEnd.time - now.time).toInt()
                    val curTimerLeft = (curTimerEnd.time - now.time).toInt()
                    val sessionDuration = prefs.getInt("SessionDuration", -1)
                    Log.i(TAG, "Session Time Left $sessionTimeLeft")
                    //Log.i(TAG, "Cur Time Left " + curTimerLeft);
                    Log.i(TAG, "SessionDuration $sessionDuration")
                    val timeElapsed = sessionDuration - sessionTimeLeft

                    // RECREATE ALARMS if empty, but we are running.
                    // THEY WILL HAVE WRONG IDS.....
                    Log.i(TAG, "Trying to recreate alarms")
                    loadLastTimers(-timeElapsed)

                    // Resume ticker at correct position
                    // Get duration of current alarm
                    val curTimerDuration = currentAlarmDuration
                    Log.i(TAG, "Setting timer: $curTimerLeft of $curTimerDuration")
                    timerResume(curTimerLeft, curTimerDuration)
                } else {
                    Log.i(TAG, "Resumed to RUNNING, but all timers are over")
                    loadLastTimers()
                }
                loadLastTimers()
            }

            STOPPED -> {
                Log.i(TAG, "CREATE, state STOPPED")
                loadLastTimers()
            }

            PAUSED -> {
                Log.i(TAG, "CREATE, state PAUSED")
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
        private const val TAG = "AlarmTaskManager"
    }
}