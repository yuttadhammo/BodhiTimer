package org.yuttadhammo.BodhiTimer;

import android.app.NotificationManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.WebView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

import java.io.IOException;


public class SettingsFragment extends PreferenceFragmentCompat {
    private SharedPreferences prefs;
    private Context mContext;

    private MediaPlayer player;
    private Preference play;
    private Preference prePlay;
    private PreferenceScreen preferenceScreen;

    private final int SELECT_RINGTONE = 0;
    private final int SELECT_FILE = 1;
    private final int SELECT_PRE_RINGTONE = 2;
    private final int SELECT_PRE_FILE = 3;
    private final int SELECT_PHOTO = 4;

    private static final String TAG = SettingsActivity.class.getSimpleName();
    //private SoundManager mSoundManager;


    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);
        prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        //mSoundManager = new SoundManager(getContext());

        preferenceScreen = getPreferenceScreen();
        mContext = getContext();

        setupTonePicker();
        setupAnimations();
        setupDND();

    }

    private void setupDND() {
        // Hide on API <23
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            preferenceScreen.findPreference("doNotDisturb").setVisible(false);
        }
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        String key = preference.getKey();

        switch (key) {
            case "aboutPref":
                showAboutScreen();
                break;
            case "showSystemSettings":
                showSystemSettings();
                break;
            case "doNotDisturb":
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    toggleDoNotDisturb();
                }
                break;
        }

        return super.onPreferenceTreeClick(preference);
    }


    private void showAboutScreen() {
        LayoutInflater li = LayoutInflater.from(mContext);
        View view = li.inflate(R.layout.about, null);
        WebView wv = view.findViewById(R.id.about_text);
        wv.loadData(getString(R.string.about_text), "text/html", "utf-8");

        AlertDialog.Builder p = new AlertDialog.Builder(mContext).setView(view);
        final AlertDialog alert = p.create();
        alert.setIcon(R.drawable.icon);
        alert.setTitle(getString(R.string.about_title));
        alert.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.close),
                (dialog, whichButton) -> {
                });
        alert.show();

    }

    private void showSystemSettings() {
        Intent intent = new Intent();
        intent.setAction("android.settings.APP_NOTIFICATION_SETTINGS");

        intent.putExtra("android.provider.extra.APP_PACKAGE", mContext.getPackageName());

        startActivity(intent);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void toggleDoNotDisturb() {
        NotificationManager mNotificationManager = (NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE);

        // Check if the notification policy access has been granted for the app.
        if (!mNotificationManager.isNotificationPolicyAccessGranted()) {
            Intent intent = new Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
            startActivity(intent);
        }
    }

    private void setupAnimations() {
        // Custom image chooser
        final Preference customImage = preferenceScreen.findPreference("custom_bmp");

        customImage.setOnPreferenceChangeListener((preference, checked) -> {

            // Only open file picker if its being enabled
            if ((Boolean) checked) {
                Uri uri = Uri.parse("content://com.android.externalstorage.documents/document/");

                Intent photoPickerIntent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                photoPickerIntent.addCategory(Intent.CATEGORY_OPENABLE);
                photoPickerIntent.setType("image/*");
                photoPickerIntent.putExtra("android.provider.extra.INITIAL_URI", uri);

                getActivity().startActivityForResult(photoPickerIntent, SELECT_PHOTO);
            }
            return true;
        });


        // Animation Style
        final Preference indexPref = findPreference("DrawingIndex");
        final Preference circleTheme = findPreference("CircleTheme");

        int dIndex = prefs.getInt("DrawingIndex", 1);
        if (dIndex == 0) {
            indexPref.setSummary(getString(R.string.is_bitmap));
            circleTheme.setEnabled(false);
        } else {
            indexPref.setSummary(getString(R.string.is_circle));
            circleTheme.setEnabled(true);
        }

        indexPref.setOnPreferenceClickListener(preference -> {
            int dIndex1 = prefs.getInt("DrawingIndex", 1);
            dIndex1++;
            dIndex1 %= 2;

            if (dIndex1 == 0) {
                indexPref.setSummary(getString(R.string.is_bitmap));
                circleTheme.setEnabled(false);
                customImage.setEnabled(true);
            } else {
                indexPref.setSummary(getString(R.string.is_circle));
                circleTheme.setEnabled(true);
                customImage.setEnabled(false);
            }

            SharedPreferences.Editor mSettingsEdit = prefs.edit();
            mSettingsEdit.putInt("DrawingIndex", dIndex1);
            mSettingsEdit.apply();
            return true;

        });
    }

    private void setupTonePicker() {
        ListPreference tone = preferenceScreen.findPreference("NotificationUri");
        play = findPreference("playSound");

        ListPreference pretone = preferenceScreen.findPreference("PreSoundUri");
        prePlay = preferenceScreen.findPreference("playPreSound");

        String[] entries = getResources().getStringArray(R.array.sound_names);
        final String[] entryValues = getResources().getStringArray(R.array.sound_uris);

        //Default value
        if (tone.getValue() == null) tone.setValue(entryValues[1]);
        tone.setDefaultValue(entryValues[1]);

        tone.setEntries(entries);
        tone.setEntryValues(entryValues);

        if (pretone.getValue() == null) pretone.setValue(entryValues[0]);
        pretone.setDefaultValue(entryValues[0]);

        pretone.setEntries(entries);
        pretone.setEntryValues(entryValues);

        player = new MediaPlayer();


        tone.setOnPreferenceChangeListener((preference, newValue) -> {
            selectTone(newValue, SELECT_RINGTONE, SELECT_FILE);
            return true;
        });

        pretone.setOnPreferenceChangeListener((preference, newValue) -> {
            selectTone(newValue, SELECT_PRE_RINGTONE, SELECT_PRE_FILE);
            return true;
        });

        play.setOnPreferenceClickListener(preference -> {
            String notificationUri = prefs.getString("NotificationUri", "android.resource://org.yuttadhammo.BodhiTimer/" + R.raw.bell);
            prePlayTone(notificationUri, preference);

            return false;
        });

        prePlay.setOnPreferenceClickListener(preference -> {
            String PreSoundUri = prefs.getString("PreSoundUri", "android.resource://org.yuttadhammo.BodhiTimer/" + R.raw.bell);
            prePlayTone(PreSoundUri, preference);

            return false;
        });

    }

    private void selectTone(Object newValue, int ringtoneActivity, int fileActivity) {
        if (player.isPlaying()) {
            play.setTitle(mContext.getString(R.string.play_sound));
            play.setSummary(mContext.getString(R.string.play_sound_desc));
            prePlay.setTitle(mContext.getString(R.string.play_pre_sound));
            prePlay.setSummary(mContext.getString(R.string.play_pre_sound_desc));

            player.stop();
        }
        if (newValue.toString().equals("system")) {
            Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALL);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Tone");
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, (Uri) null);
            try {
                getActivity().startActivityForResult(intent, ringtoneActivity);
            } catch (ActivityNotFoundException ignored) {
            }

        } else if (newValue.toString().equals("file")) {

            Uri uri = Uri.parse("content://com.android.externalstorage.documents/music/");

            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("audio/*");
            intent.putExtra("android.provider.extra.INITIAL_URI", uri);

            try {
                getActivity().startActivityForResult(intent, fileActivity);
            } catch (ActivityNotFoundException ex) {
                Toast.makeText(getActivity(), "Please install a File Manager.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void prePlayTone(String ToneUri, final Preference preference) {

        boolean isPre = (preference.getKey().equals("playPreSound"));

        if (player.isPlaying()) {
            player.stop();
            play.setTitle(mContext.getString(R.string.play_sound));
            play.setSummary(mContext.getString(R.string.play_sound_desc));
            prePlay.setTitle(mContext.getString(R.string.play_pre_sound));
            prePlay.setSummary(mContext.getString(R.string.play_pre_sound_desc));
            return;
        }

        try {
            if (isPre && ToneUri.equals("system"))
                ToneUri = prefs.getString("PreSystemUri", "");
            else if (!isPre && ToneUri.equals("system"))
                ToneUri = prefs.getString("SystemUri", "");
            else if (isPre && ToneUri.equals("file"))
                ToneUri = prefs.getString("PreFileUri", "");
            else if (!isPre && ToneUri.equals("file"))
                ToneUri = prefs.getString("FileUri", "");

            if (ToneUri.equals(""))
                return;
            Log.v(TAG, "Playing Uri: " + ToneUri);
            player.reset();
            int currVolume = prefs.getInt("tone_volume", 0);
            if (currVolume != 0) {
                float log1 = (float) (Math.log(100 - currVolume) / Math.log(100));
                player.setVolume(1 - log1, 1 - log1);
            }
            player.setDataSource(mContext, Uri.parse(ToneUri));
            player.prepare();
            player.setLooping(false);
            player.setOnCompletionListener(mp -> {
                preference.setTitle(mContext.getString(R.string.play_sound));
                preference.setSummary(mContext.getString(R.string.play_sound_desc));
                Log.v(TAG, "Resetting media player...");
                mp.reset();
            });
            player.start();
            preference.setTitle(mContext.getString(R.string.playing_sound));
            preference.setSummary(mContext.getString(R.string.playing_sound_desc));
        } catch (IOException e) {
            Log.e(TAG, "Failed to play uri: " + ToneUri);
            e.printStackTrace();
        }

    }
}

