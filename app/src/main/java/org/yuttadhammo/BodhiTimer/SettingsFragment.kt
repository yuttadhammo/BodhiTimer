/*
 * SettingsFragment.kt
 * Copyright (C) 2014-2022 BodhiTimer developers
 *
 * Distributed under terms of the GNU GPLv3 license.
 */

package org.yuttadhammo.BodhiTimer

import android.app.NotificationManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS
import android.view.LayoutInflater
import android.webkit.WebView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import org.yuttadhammo.BodhiTimer.Util.Settings
import org.yuttadhammo.BodhiTimer.Util.Sounds
import timber.log.Timber
import java.io.IOException
import kotlin.math.ln

class SettingsFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {
    private var prefs: SharedPreferences? = null
    private var mContext: Context? = null
    private var player: MediaPlayer? = null
    private var play: Preference? = null
    private var prePlay: Preference? = null
    private val SELECT_RINGTONE = 0
    private val SELECT_FILE = 1
    private val SELECT_PRE_RINGTONE = 2
    private val SELECT_PRE_FILE = 3
    private val SELECT_PHOTO = 4

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
        prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        mContext = context
        setupTonePicker()
        setupAnimations()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get all setting keys and populate the summaries
        Settings.getAllKeys().forEach {
            updatePreferenceSummaries(it)
        }
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        when (preference.key) {
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
        val p = AlertDialog.Builder(
            mContext!!
        ).setView(view)
        val alert = p.create()
        alert.setIcon(R.mipmap.ic_launcher)
        alert.setTitle(getString(R.string.about_title))
        alert.setButton(
            AlertDialog.BUTTON_NEGATIVE, getString(R.string.close)
        ) { _: DialogInterface?, _: Int -> }
        alert.show()
    }

    private fun showSystemSettings() {
        val intent = Intent()
        intent.action = "android.settings.APP_NOTIFICATION_SETTINGS"
        intent.putExtra("android.provider.extra.APP_PACKAGE", mContext!!.packageName)
        startActivity(intent)
    }

    private fun toggleDoNotDisturb() {
        val mNotificationManager =
            requireActivity().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Check if the notification policy access has been granted for the app.
        if (!mNotificationManager.isNotificationPolicyAccessGranted) {
            val intent = Intent(ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
            startActivity(intent)
        }
    }

    private fun setupAnimations() {
        // Custom image chooser
        val customImage = preferenceScreen!!.findPreference<Preference>("custom_bmp")
        customImage!!.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _: Preference?, checked: Any ->

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
        val dIndex = Settings.drawingIndex
        if (dIndex == 0) {
            indexPref!!.summary = getString(R.string.is_bitmap)
        } else {
            indexPref!!.summary = getString(R.string.is_circle)
        }
        indexPref.onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
                var dIndex1 = Settings.drawingIndex
                dIndex1++
                dIndex1 %= 2
                if (dIndex1 == 0) {
                    indexPref.summary = getString(R.string.is_bitmap)
                    customImage.isEnabled = true
                } else {
                    indexPref.summary = getString(R.string.is_circle)
                    customImage.isEnabled = false
                }
                Settings.drawingIndex = dIndex1
                true
            }
    }

    private fun setupTonePicker() {
        val tone = preferenceScreen!!.findPreference<ListPreference>("NotificationUri")
        play = findPreference("playSound")

        val pretone = preferenceScreen!!.findPreference<ListPreference>("PreSoundUri")
        prePlay = preferenceScreen!!.findPreference("playPreSound")

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
        tone.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _: Preference?, newValue: Any ->
                selectTone(newValue, SELECT_RINGTONE, SELECT_FILE)
                true
            }
        pretone.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _: Preference?, newValue: Any ->
                selectTone(newValue, SELECT_PRE_RINGTONE, SELECT_PRE_FILE)
                true
            }
        play!!.onPreferenceClickListener =
            Preference.OnPreferenceClickListener { preference: Preference ->
                val notificationUri = prefs!!.getString("NotificationUri", Sounds.DEFAULT_SOUND)
                prePlayTone(notificationUri, preference)
                false
            }
        prePlay!!.onPreferenceClickListener =
            Preference.OnPreferenceClickListener { preference: Preference ->
                val PreSoundUri = prefs!!.getString("PreSoundUri", Sounds.DEFAULT_SOUND)
                prePlayTone(PreSoundUri, preference)
                false
            }
    }

    private fun selectTone(newValue: Any, ringtoneActivity: Int, fileActivity: Int) {
        if (player!!.isPlaying) {
            play!!.title = mContext!!.getString(R.string.play_sound)
            play!!.summary = mContext!!.getString(R.string.play_sound_desc)
            prePlay!!.title = mContext!!.getString(R.string.play_pre_sound)
            prePlay!!.summary = mContext!!.getString(R.string.play_pre_sound_desc)
            player!!.stop()
        }
        if (newValue.toString() == "system") {
            val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALL)
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Tone")
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, null as Uri?)
            try {
                requireActivity().startActivityForResult(intent, ringtoneActivity)
            } catch (ignored: ActivityNotFoundException) {
            }
        } else if (newValue.toString() == "file") {
            val uri = Uri.parse("content://com.android.externalstorage.documents/music/")
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = "audio/*"
            intent.putExtra("android.provider.extra.INITIAL_URI", uri)
            try {
                requireActivity().startActivityForResult(intent, fileActivity)
            } catch (ex: ActivityNotFoundException) {
                Toast.makeText(activity, "Please install a File Manager.", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private fun prePlayTone(ToneUri: String?, preference: Preference) {
        var toneUri = ToneUri
        val isPre = preference.key == "playPreSound"
        if (player!!.isPlaying) {
            player!!.stop()
            play!!.title = mContext!!.getString(R.string.play_sound)
            play!!.summary = mContext!!.getString(R.string.play_sound_desc)
            prePlay!!.title = mContext!!.getString(R.string.play_pre_sound)
            prePlay!!.summary = mContext!!.getString(R.string.play_pre_sound_desc)
            return
        }
        try {
            if (isPre && toneUri == "system") toneUri = prefs!!.getString(
                "PreSystemUri",
                ""
            ) else if (!isPre && toneUri == "system") toneUri =
                prefs!!.getString("SystemUri", "") else if (isPre && toneUri == "file") toneUri =
                prefs!!.getString("PreFileUri", "") else if (!isPre && toneUri == "file") toneUri =
                prefs!!.getString("FileUri", "")
            if (toneUri == "") return
            Timber.v("Playing Uri: $toneUri")
            player!!.reset()
            val currVolume = prefs!!.getInt("tone_volume", 0)
            if (currVolume != 0) {
                val log1 = (ln((100 - currVolume).toDouble()) / ln(100.0)).toFloat()
                player!!.setVolume(1 - log1, 1 - log1)
            }
            player!!.setDataSource(mContext!!, Uri.parse(toneUri))
            player!!.prepare()
            player!!.isLooping = false
            player!!.setOnCompletionListener { mp: MediaPlayer ->
                preference.title = mContext!!.getString(R.string.play_sound)
                preference.summary = mContext!!.getString(R.string.play_sound_desc)
                Timber.v("Resetting media player...")
                mp.reset()
            }
            player!!.start()
            preference.title = mContext!!.getString(R.string.playing_sound)
            preference.summary = mContext!!.getString(R.string.playing_sound_desc)
        } catch (e: IOException) {
            Timber.e("Failed to play uri: $toneUri")
            e.printStackTrace()
        }
    }

    override fun onResume() {
        super.onResume()
        prefs!!.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        prefs!!.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        updatePreferenceSummaries(key)
    }

    /**
     * Update preference summaries to reflect the current select item (or entered text) in the UI
     *
     * @param key: The key of the preference to update
     */
    private fun updatePreferenceSummaries(key: String) {
        try {
            when (val pref: Preference? = findPreference(key)) {
                is ListPreference -> {
                    pref.summary = pref.entry
                }
                is EditTextPreference -> {
                    pref.summary = pref.text
                }
            }
        } catch (ignored: Exception) {
            // If we have updated a ListPreferences possible values, and the user has now an
            // impossible value, getEntry() will throw an Exception.
        }
    }
}