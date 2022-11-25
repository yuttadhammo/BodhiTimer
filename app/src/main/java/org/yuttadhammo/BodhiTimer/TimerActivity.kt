/*
 * TimerActivity.kt
 * Copyright (C) 2014-2022 BodhiTimer developers
 *
 * Distributed under terms of the GNU GPLv3 license.
 */

package org.yuttadhammo.BodhiTimer

import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import org.yuttadhammo.BodhiTimer.Animation.TimerAnimation
import org.yuttadhammo.BodhiTimer.Const.BroadcastTypes.BROADCAST_END
import org.yuttadhammo.BodhiTimer.Const.BroadcastTypes.BROADCAST_UPDATE
import org.yuttadhammo.BodhiTimer.Const.SessionTypes
import org.yuttadhammo.BodhiTimer.Const.TimerState.PAUSED
import org.yuttadhammo.BodhiTimer.Const.TimerState.RUNNING
import org.yuttadhammo.BodhiTimer.Const.TimerState.STOPPED
import org.yuttadhammo.BodhiTimer.Models.AlarmTaskManager
import org.yuttadhammo.BodhiTimer.Models.TimerList
import org.yuttadhammo.BodhiTimer.Util.Notifications.createNotificationChannel
import org.yuttadhammo.BodhiTimer.Util.Settings
import org.yuttadhammo.BodhiTimer.Util.Themes
import org.yuttadhammo.BodhiTimer.Util.Time
import org.yuttadhammo.BodhiTimer.Util.Time.msFromArray
import org.yuttadhammo.BodhiTimer.Util.Time.str2complexTimeString
import org.yuttadhammo.BodhiTimer.Util.Time.str2timeString
import org.yuttadhammo.BodhiTimer.Util.Time.time2Array
import org.yuttadhammo.BodhiTimer.Util.Time.time2humanStr
import timber.log.Timber
import java.io.FileNotFoundException
import java.util.Locale


/**
 * The main activity which shows the timer and allows the user to set the time
 */
class TimerActivity : AppCompatActivity(), View.OnClickListener, OnSharedPreferenceChangeListener {

    private var lastColor: Int = -4

    /**
     * To save having to traverse the view tree
     */
    private lateinit var mPauseButton: ImageButton
    private lateinit var mStopButton: ImageButton
    private lateinit var mSetButton: ImageButton
    private lateinit var mPrefButton: ImageButton
    private lateinit var mTimerAnimation: TimerAnimation
    private lateinit var mTimerLabel: TextView
    private lateinit var mPreviewLabel: TextView

    private var mPlayBitmap: Bitmap? = null
    private var mPauseBitmap: Bitmap? = null
    var mAlarmTaskManager: AlarmTaskManager? = null

    private var widget = false
    private var context: TimerActivity? = null
    private var animationIndex = 0
    private var tts: TextToSpeech? = null
    private var lastp = 0f

    /**
     * Called when the activity is first created.
     * { @inheritDoc}
     */
    public override fun onCreate(savedInstanceState: Bundle?) {
        Timber.i("CREATE")
        super.onCreate(savedInstanceState)
        context = this
        mAlarmTaskManager = ViewModelProvider(this)[AlarmTaskManager::class.java]

        Themes.applyTheme(this)

        val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(baseContext)
        prefs.registerOnSharedPreferenceChangeListener(this)
        setupObservers()
        prepareUI()
        val filter2 = IntentFilter()
        filter2.addAction(BROADCAST_END)
        registerReceiver(alarmEndReceiver, filter2)
        createNotificationChannel(context!!)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        checkForDeepLink(intent)
    }

    private fun checkForDeepLink(intent: Intent?) {
        val data: Uri = intent?.data ?: return

        val paramNames = data.queryParameterNames

        if (paramNames.contains("times")) {
            val tL = TimerList()

            var notificationUri = Settings.notificationUri

            when (notificationUri) {
                "system" -> notificationUri = Settings.systemUri
                "file" -> notificationUri = Settings.fileUri
            }

            for (timeStr in data.getQueryParameter("times")!!.split(",").toTypedArray()) {
                var time = 0
                try {
                    time = Integer.parseInt(timeStr)
                } catch (e: NumberFormatException) {
                    Timber.e("Invalid number format provided: $timeStr", e)
                }

                if (time > 0) {
                    tL.timers.add(TimerList.Timer(time * 1000, notificationUri, SessionTypes.REAL))
                }
            }

            if (tL.timers.size > 0) {
                mAlarmTaskManager?.startAlarms(tL)
            }
        }
    }

    private fun prepareUI() {
        setContentView(R.layout.main)
        mStopButton = findViewById(R.id.stopButton)
        mStopButton.setOnClickListener(this)
        mSetButton = findViewById(R.id.setButton)
        mSetButton.setOnClickListener(this)
        mSetButton.setOnLongClickListener {
            if (Settings.switchTimeMode) showNumberPicker() else startVoiceRecognitionActivity()
            false
        }
        mPauseButton = findViewById(R.id.pauseButton)
        mPauseButton.setOnClickListener(this)
        mPrefButton = findViewById(R.id.prefButton)
        mPrefButton.setOnClickListener(this)
        mPauseBitmap = BitmapFactory.decodeResource(
            resources, R.drawable.pause
        )
        mPlayBitmap = BitmapFactory.decodeResource(
            resources, R.drawable.play
        )
        mTimerLabel = findViewById(R.id.text_top)
        mPreviewLabel = findViewById(R.id.text_preview)
        mTimerAnimation = findViewById(R.id.mainImage)
        mTimerAnimation.setOnClickListener(this)
        animationIndex = Settings.drawingIndex
    }

    private fun setupObservers() {
        // Create the observer which updates the UI.
        val timerLabelObserver = Observer { newTime: String? -> mTimerLabel.text = newTime }
        val previewLabelObserver = Observer { newText: String? -> mPreviewLabel.text = newText }

        val timeLeftObserver = Observer { timeLeft: Int ->
            updateInterfaceWithTime(
                timeLeft,
                mAlarmTaskManager!!.curTimerDurationVal
            )
        }

        val durationObserver = Observer { duration: Int ->
            updateInterfaceWithTime(
                mAlarmTaskManager!!.curTimerLeftVal, duration
            )
        }

        val stateObserver = Observer { newState: Int -> hasEnteredState(newState) }

        // Observe the LiveData, passing in this activity as the LifecycleOwner and the observer.
        mAlarmTaskManager!!.getTimerText().observe(this, timerLabelObserver)
        mAlarmTaskManager!!.getPreviewText().observe(this, previewLabelObserver)
        mAlarmTaskManager!!.curTimerDuration.observe(this, durationObserver)
        mAlarmTaskManager!!.curTimerLeft.observe(this, timeLeftObserver)
        mAlarmTaskManager!!.currentState.observe(this, stateObserver)
    }

    /**
     * {@inheritDoc}
     */
    public override fun onResume() {
        super.onResume()
        Timber.i("RESUME")
        volumeControlStream = AudioManager.STREAM_MUSIC
        mAlarmTaskManager!!.appIsPaused = false
        if (intent.hasExtra("set")) {
            Timber.d("Create From Widget")
            widget = true
            intent.removeExtra("set")
        }
        animationIndex = Settings.drawingIndex
        setupUI()
        if (mAlarmTaskManager!!.currentState.value == STOPPED) {
            if (widget) {
                if (Settings.switchTimeMode
                ) startVoiceRecognitionActivity() else showNumberPicker()
                return
            }
        }
        widget = false
    }

    /**
     * { @inheritDoc}
     */
    public override fun onPause() {
        super.onPause()
        Timber.i("PAUSE")
        mAlarmTaskManager!!.appIsPaused = true // tell gui timer to stop
        sendBroadcast(Intent(BROADCAST_UPDATE)) // tell widgets to update
    }

    override fun onStop() {
        // When our activity is stopped ensure we also stop the connection to the service
        // this stops us leaking our activity into the system *bad*
        Timber.d("STOPPED")
        super.onStop()
    }

    public override fun onDestroy() {
        Timber.d("DESTROY")
        //Close the Text to Speech Library
        if (tts != null) {
            tts!!.stop()
            tts!!.shutdown()
            Timber.d("TTSService Destroyed")
        }
        mAlarmTaskManager!!.stopAlarmsAndTicker()
        unregisterReceiver(alarmEndReceiver)
        super.onDestroy()
    }

    private fun setupUI() {
        try {
            mTimerAnimation.index = animationIndex
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }
        if (Settings.hideTime) mTimerLabel.visibility =
            View.INVISIBLE else mTimerLabel.visibility = View.VISIBLE


        Timber.i("Configuring animation")
        mTimerAnimation.configure()


        updateFontColor(0F)

        setLowProfile()
        if (Settings.wakeLock) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        if (Settings.fullscreen) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun onClick(v: View) {
        setLowProfile()
        when (v.id) {
            R.id.setButton -> {
                Timber.i("Timer", "set button clicked")
                if (Settings.switchTimeMode
                ) startVoiceRecognitionActivity() else showNumberPicker()
            }
            R.id.prefButton -> {
                Timber.i("Timer", "pref button clicked")
                widget = false
                startActivity(Intent(this, SettingsActivity::class.java))
            }
            R.id.pauseButton -> when (mAlarmTaskManager!!.currentState.value) {
                RUNNING -> mAlarmTaskManager!!.timerPause()
                PAUSED -> mAlarmTaskManager!!.timerUnPause()
                STOPPED ->                         // We are stopped and want to restore the last used timers.
                    mAlarmTaskManager!!.startAlarms(mAlarmTaskManager!!.retrieveTimerList())
            }
            R.id.stopButton -> {
                mAlarmTaskManager!!.stopAlarmsAndTicker()
                mAlarmTaskManager!!.loadLastTimers()
            }
        }
    }

    override fun onKeyDown(keycode: Int, e: KeyEvent): Boolean {
        if (keycode == KeyEvent.KEYCODE_MENU) {
            startActivity(Intent(this, SettingsActivity::class.java))
            return true
        }
        return super.onKeyDown(keycode, e)
    }

    override fun onBackPressed(){
        // NOTE: Let back key act as home key, so auto-restarting
        // feature can work well when user press back key.
        Toast.makeText(this, getString(R.string.run_app_in_background),
                       Toast.LENGTH_SHORT).show()
        val intent = Intent(Intent.ACTION_MAIN)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        intent.addCategory(Intent.CATEGORY_HOME)
        startActivity(intent)
    }

    private fun setLowProfile() {
        val rootView = window.decorView
        rootView.systemUiVisibility = View.SYSTEM_UI_FLAG_LOW_PROFILE
    }

    private fun showNumberPicker() {

        val view = SlidingPickerDialog(context!!)
        val lastTimePicked = Settings.lastSimpleTime
        view.mTimes = time2Array(lastTimePicked)
        view.setOnConfirm {
            onNumbersPicked(it)
        }
        view.show()
    }

    /**
     * Updates the time
     *
     * @param elapsed  the elapsed time of the current timer
     * @param duration the duration of the current timer
     */
    private fun updateInterfaceWithTime(elapsed: Int, duration: Int) {

        // TODO: This is a hack, to show the full circle after stopped or finished..
        var elapsed = elapsed
        if (elapsed == duration) {
            elapsed = 0
        }
        val p: Float = if (duration != 0) elapsed / duration.toFloat() else 0f
        if (lastp - p < 0.001 && lastp - p > 0) {
            // The change in progress was minimal
            return
        }
        lastp = p

        mTimerAnimation.updateImage(elapsed, duration)
        updateFontColor(p)
    }

    /**
     * Manually handle the font color when using the Fade-In animation
     */
    private fun updateFontColor(p: Float) {
        if (Settings.drawingIndex == 0) {
            val dayNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            val isLightTheme = (dayNightMode == Configuration.UI_MODE_NIGHT_NO)
            val textColor = when {
                isLightTheme && p < 0.5 -> {
                    Color.WHITE
                }
                isLightTheme && p >= 0.5 -> {
                    Color.BLACK
                }
                else -> Color.WHITE
            }
            if (lastColor == textColor) return
            findViewById<TextView>(R.id.text_top).setTextColor(textColor)
            findViewById<TextView>(R.id.text_preview).setTextColor(textColor)
            lastColor = textColor
        }
    }

    private fun startSimpleAlarm(numbers: IntArray?, startAll: Boolean) {
        startSimpleAlarm(msFromArray(numbers!!), startAll)
    }

    private fun startSimpleAlarm(time: Int, startAll: Boolean) {
        val tL = createSimpleTimerList(time)
        mAlarmTaskManager!!.saveTimerList(tL)
        mAlarmTaskManager!!.addAlarms(tL, 0)
        if (startAll) {
            mAlarmTaskManager!!.startAll()
        }
    }

    private fun createSimpleTimerList(time: Int): TimerList {
        val prepTime = Settings.preparationTime * 1000
        var preUriString = Settings.preSoundUri
        val tL = TimerList()

        // Add a preparatory timer if the user picked a tone
        if (preUriString != "") {
            when (preUriString) {
                "system" -> preUriString = Settings.preSystemUri
                "file" -> preUriString = Settings.preFileUri
            }
            tL.timers.add(TimerList.Timer(prepTime, preUriString, SessionTypes.PREPARATION))
        }

        var notificationUri = Settings.notificationUri

        when (notificationUri) {
            "system" -> notificationUri = Settings.systemUri
            "file" -> notificationUri = Settings.fileUri
        }

        // Add main timer
        tL.timers.add(TimerList.Timer(time, notificationUri, SessionTypes.REAL))

        return tL
    }

    private fun startAdvancedAlarm(advTimeString: String) {
        val list = TimerList(advTimeString)
        Timber.v("advString: $advTimeString")
        Timber.v("advString2: " + list.string)
        mAlarmTaskManager!!.startAlarms(list)
    }

    /**
     * Callback for the number picker dialog
     */
    private fun onNumbersPicked(numbers: IntArray?) {
        if (numbers == null) {
            widget = false
            return
        }

        mAlarmTaskManager!!.stopAlarmsAndTicker()

        // advanced timer - 0 will be -1
        if (numbers[0] == -1) {
            val advTimeString = Settings.advTimeString

            // Overwrite the current timeString
            Settings.timeString = advTimeString
            if (advTimeString.isEmpty()) {
                widget = false
                return
            }
            Settings.lastWasSimple = true
            startAdvancedAlarm(advTimeString)
        } else {
            Timber.v("Saving simple time: " + msFromArray(numbers))
            Settings.lastSimpleTime = msFromArray(numbers)
            Settings.lastWasSimple = true
            startSimpleAlarm(numbers, true)
        }
    }

    private fun hasEnteredState(newState: Int) {
        when (newState) {
            RUNNING -> {
                mSetButton.visibility = View.GONE
                mStopButton.visibility = View.VISIBLE
                mPauseButton.visibility = View.VISIBLE
                mPauseButton.setImageBitmap(mPauseBitmap)
                setButtonAlpha(127)
            }
            STOPPED -> {
                mPauseButton.setImageBitmap(mPlayBitmap)
                mStopButton.visibility = View.GONE
                mSetButton.visibility = View.VISIBLE
                setButtonAlpha(255)
            }
            PAUSED -> {
                mSetButton.visibility = View.GONE
                mPauseButton.visibility = View.VISIBLE
                mStopButton.visibility = View.VISIBLE
                mPauseButton.setImageBitmap(mPlayBitmap)
                setButtonAlpha(255)
            }
        }
    }

    private fun setButtonAlpha(i: Int) {
        mPauseButton.imageAlpha = i
        mStopButton.imageAlpha = i
        mPrefButton.imageAlpha = i
    }

    /**
     * Update visual components if preferences have changed
     */
    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        // Case PreSystemUri
        when (key) {
            "WakeLock" -> {
                if (Settings.wakeLock)
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) else window.clearFlags(
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                )
                val lastTime = Settings.lastSimpleTime
                if (mAlarmTaskManager!!.currentState.value == STOPPED && Settings.lastWasSimple
                ) {
                    startSimpleAlarm(lastTime, false)
                }
            }
            "PreSoundUri", "PreSystemUri", "SoundUri", "NotificationUri", "preparationTime" -> {
                val lastTime = Settings.lastSimpleTime
                if (mAlarmTaskManager!!.currentState.value == STOPPED && Settings.lastWasSimple
                ) {
                    startSimpleAlarm(lastTime, false)
                }
            }
        }
    }

    private val VOICE_RECOGNITION_REQUEST_CODE = 1234
    private val NUMBER_PICK_REQUEST_CODE = 5678

    /**
     * Fire an intent to start the speech recognition activity.
     */
    private fun startVoiceRecognitionActivity() {
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)

            // Display an hint to the user about what he should say.
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.speech_description))

            // Give a hint to the recognizer about what the user is going to say
            intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            intent.putExtra(
                RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS,
                1000
            )

            // Specify how many results you want to receive. The results will be sorted
            // where the first result is the one with higher confidence.
            intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            startActivityForResult(intent, VOICE_RECOGNITION_REQUEST_CODE)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(
                this,
                getString(R.string.NoVoiceRecognitionInstalled),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * Handle the all activities.
     */
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Timber.v("Got result")
        if (requestCode == NUMBER_PICK_REQUEST_CODE && resultCode == RESULT_OK) {
            val values = data!!.getIntArrayExtra("times")
            onNumbersPicked(values)
            if (widget) {
                finish()
            }
        } else if (requestCode == VOICE_RECOGNITION_REQUEST_CODE && resultCode == RESULT_OK) {
            // Fill the list view with the strings the recognizer thought it could have heard
            onVoiceSpoken(data)
        }
        widget = false
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun onVoiceSpoken(data: Intent?) {
        val matches = data!!.getStringArrayListExtra(
            RecognizerIntent.EXTRA_RESULTS
        )
        for (match in matches!!) {
            val match = match.lowercase(Locale.getDefault())
            Timber.d("Got speech: $match")
            if (match.contains(Time.TIME_SEPARATOR)) {
                val complexTime = str2complexTimeString(this, match)
                if (complexTime.isNotEmpty()) {
                    Settings.timeString = complexTime
                    if (Settings.speakTime) {
                        tts = TextToSpeech(this) { status: Int ->
                            if (status == TextToSpeech.SUCCESS) {
                                tts!!.speak(
                                    getString(R.string.adv_speech_recognized),
                                    TextToSpeech.QUEUE_ADD,
                                    null
                                )
                            } else Timber.tag("error").e("Initialization failed!")
                        }
                    }
                    Toast.makeText(
                        this,
                        getString(R.string.adv_speech_recognized),
                        Toast.LENGTH_SHORT
                    ).show()
                    val values = intArrayOf(-1, -1, -1)
                    onNumbersPicked(values)
                    break
                }
            } else {
                val speechTime = str2timeString(this, match)
                if (speechTime != 0) {
                    val values = time2Array(speechTime)
                    Toast.makeText(
                        this,
                        String.format(
                            getString(R.string.speech_recognized),
                            time2humanStr(this, speechTime)
                        ),
                        Toast.LENGTH_SHORT
                    ).show()
                    if (Settings.speakTime) {
                        Timber.d("Speaking time")
                        tts!!.speak(
                            String.format(
                                getString(R.string.speech_recognized), time2humanStr(
                                    context!!, speechTime
                                )
                            ), TextToSpeech.QUEUE_ADD, null
                        )
                    }
                    onNumbersPicked(values)
                    break
                } else Toast.makeText(
                    this,
                    getString(R.string.speech_not_recognized),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // Should move to Manager....
    // receiver to get restart
    private val alarmEndReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Timber.v("Received app alarm callback")
            Timber.d("id " + intent.getIntExtra("id", -1))
            mAlarmTaskManager!!.onAlarmEnd(intent.getIntExtra("id", -1))
        }
    }
}