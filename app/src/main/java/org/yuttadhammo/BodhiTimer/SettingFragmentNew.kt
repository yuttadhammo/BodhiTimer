package org.yuttadhammo.BodhiTimer

import android.app.NotificationManager
import android.content.*
import android.content.SharedPreferences.Editor
import android.media.MediaPlayer
import android.media.MediaPlayer.OnCompletionListener
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.webkit.WebView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.*
import org.yuttadhammo.BodhiTimer.Util.ThemeProvider
import java.io.IOException

class SettingsFragmentNew : PreferenceFragmentCompat() {

    private var prefs: SharedPreferences? = null
    private var mContext: Context? = null

    private var player: MediaPlayer? = null
    private var play: Preference? = null
    private var prePlay: Preference? = null
    private var preferenceScreen: PreferenceScreen? = null

    private val SELECT_RINGTONE = 0
    private val SELECT_FILE = 1
    private val SELECT_PRE_RINGTONE = 2
    private val SELECT_PRE_FILE = 3
    private val SELECT_PHOTO = 4

    private val TAG = SettingsActivity::class.java.simpleName

    private val themeProvider by lazy { ThemeProvider(requireContext()) }
    private val themePreference by lazy {
        findPreference<ListPreference>(getString(R.string.theme_preferences_key))
    }


    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
        prefs = PreferenceManager.getDefaultSharedPreferences(context)

        preferenceScreen = getPreferenceScreen()
        mContext = context

        setupTonePicker()
        setupAnimations()
        setupDND()
        setThemePreference()
    }

    private fun setThemePreference() {
        themePreference?.onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { _, newValue ->
                    if (newValue is String) {
                        val theme = themeProvider.getTheme(newValue)
                        AppCompatDelegate.setDefaultNightMode(theme)
                    }
                    true
                }
        themePreference?.summaryProvider = Preference.SummaryProvider<ListPreference> { preference ->
            themeProvider.getThemeDescriptionForPreference(preference.value)
        }
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        val key = preference.key
        when (key) {
            "aboutPref" -> showAboutScreen()
            "showSystemSettings" -> showSystemSettings()
            "doNotDisturb" -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                toggleDoNotDisturb()
            }
        }
        return super.onPreferenceTreeClick(preference)
    }


    private fun showAboutScreen() {
        val li = LayoutInflater.from(mContext)
        val view = li.inflate(R.layout.about, null)
        val wv = view.findViewById<WebView>(R.id.about_text)
        wv.loadData(getString(R.string.about_text), "text/html", "utf-8")
        val p = AlertDialog.Builder(mContext).setView(view)
        val alert = p.create()
        alert.setIcon(R.drawable.icon)
        alert.setTitle(getString(R.string.about_title))
        alert.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.close)
        ) { dialog: DialogInterface?, whichButton: Int -> }
        alert.show()
    }

    private fun showSystemSettings() {
        val intent = Intent()
        intent.action = "android.settings.APP_NOTIFICATION_SETTINGS"
        intent.putExtra("android.provider.extra.APP_PACKAGE", mContext.getPackageName())
        startActivity(intent)
    }


    private fun setupDND() {
        val checkbox = preferenceScreen.findPreference<CheckBoxPreference>("doNotDisturb")

        // Hide on API <23
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            checkbox!!.isVisible = false
        } else {
            val mNotificationManager = requireActivity().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (!mNotificationManager.isNotificationPolicyAccessGranted) {
                checkbox!!.isChecked = false
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private fun toggleDoNotDisturb() {
        val mNotificationManager = requireActivity().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Check if the notification policy access has been granted for the app.
        if (!mNotificationManager.isNotificationPolicyAccessGranted) {
            val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
            startActivity(intent)
        }
    }

    private fun setupAnimations() {
        // Custom image chooser
        val customImage = preferenceScreen.findPreference<Preference>("custom_bmp")
        customImage!!.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { preference: Preference?, checked: Any ->

            // Only open file picker if its being enabled
            if (checked as Boolean) {
                val uri = Uri.parse("content://com.android.externalstorage.documents/document/")
                val photoPickerIntent = Intent(Intent.ACTION_OPEN_DOCUMENT)
                photoPickerIntent.addCategory(Intent.CATEGORY_OPENABLE)
                photoPickerIntent.type = "image/*"
                photoPickerIntent.putExtra("android.provider.extra.INITIAL_URI", uri)
                requireActivity().startActivityForResult(photoPickerIntent, SELECT_PHOTO)
            }
            true
        }


        // Animation Style
        val indexPref = findPreference<Preference>("DrawingIndex")
        val circleTheme = findPreference<Preference>("CircleTheme")
        val dIndex: Int = prefs.getInt("DrawingIndex", 1)
        if (dIndex == 0) {
            indexPref!!.summary = getString(R.string.is_bitmap)
            circleTheme!!.isEnabled = false
        } else {
            indexPref!!.summary = getString(R.string.is_circle)
            circleTheme!!.isEnabled = true
        }
        indexPref.onPreferenceClickListener = Preference.OnPreferenceClickListener { preference: Preference? ->
            var dIndex1: Int = prefs.getInt("DrawingIndex", 1)
            dIndex1++
            dIndex1 %= 2
            if (dIndex1 == 0) {
                indexPref.summary = getString(R.string.is_bitmap)
                circleTheme.isEnabled = false
                customImage.isEnabled = true
            } else {
                indexPref.summary = getString(R.string.is_circle)
                circleTheme.isEnabled = true
                customImage.isEnabled = false
            }
            val mSettingsEdit: Editor = prefs.edit()
            mSettingsEdit.putInt("DrawingIndex", dIndex1)
            mSettingsEdit.apply()
            true
        }
    }

    private fun setupTonePicker() {
        val tone = preferenceScreen.findPreference<ListPreference>("NotificationUri")
        play = findPreference<Preference>("playSound")
        val pretone = preferenceScreen.findPreference<ListPreference>("PreSoundUri")
        prePlay = preferenceScreen.findPreference<Preference>("playPreSound")
        val entries = resources.getStringArray(R.array.sound_names)
        val entryValues = resources.getStringArray(R.array.sound_uris)

        //Default value
        if (tone!!.value == null) tone.value = entryValues[1]
        tone.setDefaultValue(entryValues[1])
        tone.entries = entries
        tone.entryValues = entryValues
        if (pretone!!.value == null) pretone.value = entryValues[0]
        pretone.setDefaultValue(entryValues[0])
        pretone.entries = entries
        pretone.entryValues = entryValues
        player = MediaPlayer()
        tone.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { preference: Preference?, newValue: Any ->
            selectTone(newValue, SELECT_RINGTONE, SELECT_FILE)
            true
        }
        pretone.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { preference: Preference?, newValue: Any ->
            selectTone(newValue, SELECT_PRE_RINGTONE, SELECT_PRE_FILE)
            true
        }
        play.setOnPreferenceClickListener(Preference.OnPreferenceClickListener { preference: Preference ->
            val notificationUri: String? = prefs.getString("NotificationUri", "android.resource://org.yuttadhammo.BodhiTimer/" + R.raw.bell)
            prePlayTone(notificationUri, preference)
            false
        })
        prePlay.setOnPreferenceClickListener(Preference.OnPreferenceClickListener { preference: Preference ->
            val PreSoundUri: String? = prefs.getString("PreSoundUri", "android.resource://org.yuttadhammo.BodhiTimer/" + R.raw.bell)
            prePlayTone(PreSoundUri, preference)
            false
        })
    }

    private fun selectTone(newValue: Any, ringtoneActivity: Int, fileActivity: Int) {
        if (player.isPlaying()) {
            play.setTitle(mContext.getString(R.string.play_sound))
            play.setSummary(mContext.getString(R.string.play_sound_desc))
            prePlay.setTitle(mContext.getString(R.string.play_pre_sound))
            prePlay.setSummary(mContext.getString(R.string.play_pre_sound_desc))
            player.stop()
        }
        if (newValue.toString() == "system") {
            val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALL)
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Tone")
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, null as Uri?)
            try {
                activity!!.startActivityForResult(intent, ringtoneActivity)
            } catch (ignored: ActivityNotFoundException) {
            }
        } else if (newValue.toString() == "file") {
            val uri = Uri.parse("content://com.android.externalstorage.documents/music/")
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = "audio/*"
            intent.putExtra("android.provider.extra.INITIAL_URI", uri)
            try {
                activity!!.startActivityForResult(intent, fileActivity)
            } catch (ex: ActivityNotFoundException) {
                Toast.makeText(activity, "Please install a File Manager.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun prePlayTone(ToneUri: String, preference: Preference) {
        var ToneUri = ToneUri
        val isPre = preference.key == "playPreSound"
        if (player.isPlaying()) {
            player.stop()
            play.setTitle(mContext.getString(R.string.play_sound))
            play.setSummary(mContext.getString(R.string.play_sound_desc))
            prePlay.setTitle(mContext.getString(R.string.play_pre_sound))
            prePlay.setSummary(mContext.getString(R.string.play_pre_sound_desc))
            return
        }
        try {
            if (isPre && ToneUri == "system") ToneUri = prefs.getString("PreSystemUri", "") else if (!isPre && ToneUri == "system") ToneUri = prefs.getString("SystemUri", "") else if (isPre && ToneUri == "file") ToneUri = prefs.getString("PreFileUri", "") else if (!isPre && ToneUri == "file") ToneUri = prefs.getString("FileUri", "")
            if (ToneUri == "") return
            Log.v(SettingsFragment.TAG, "Playing Uri: $ToneUri")
            player.reset()
            val currVolume: Int = prefs.getInt("tone_volume", 0)
            if (currVolume != 0) {
                val log1 = (Math.log((100 - currVolume).toDouble()) / Math.log(100.0)).toFloat()
                player.setVolume(1 - log1, 1 - log1)
            }
            player.setDataSource(mContext, Uri.parse(ToneUri))
            player.prepare()
            player.setLooping(false)
            player.setOnCompletionListener(OnCompletionListener { mp: MediaPlayer ->
                preference.setTitle(mContext.getString(R.string.play_sound))
                preference.setSummary(mContext.getString(R.string.play_sound_desc))
                Log.v(SettingsFragment.TAG, "Resetting media player...")
                mp.reset()
            })
            player.start()
            preference.setTitle(mContext.getString(R.string.playing_sound))
            preference.setSummary(mContext.getString(R.string.playing_sound_desc))
        } catch (e: IOException) {
            Log.e(SettingsFragment.TAG, "Failed to play uri: $ToneUri")
            e.printStackTrace()
        }
    }

}