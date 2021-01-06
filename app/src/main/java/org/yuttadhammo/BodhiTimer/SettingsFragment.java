package org.yuttadhammo.BodhiTimer;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.WebView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.CheckBoxPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

import java.io.IOException;


public class SettingsFragment extends PreferenceFragmentCompat {
    private SharedPreferences prefs;
    private Context context;

    private MediaPlayer player;
    private Preference play;
    private Preference preplay;
    private PreferenceScreen preferenceScreen;

    private final int SELECT_RINGTONE = 0;
    private final int SELECT_FILE = 1;
    private final int SELECT_PRE_RINGTONE = 2;
    private final int SELECT_PRE_FILE = 3;
    private final int SELECT_PHOTO = 4;

    private static final String TAG = SettingsActivity.class.getSimpleName();


    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);
        prefs = PreferenceManager.getDefaultSharedPreferences(getContext());

        preferenceScreen = getPreferenceScreen();
        context = getContext();

        setupTonePicker();
        setupAnimations();
        //setupBatteryAction();

    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        String key = preference.getKey();

        switch (key) {
            case "aboutPref":
                showAboutScreen();
                break;
        }

        return super.onPreferenceTreeClick(preference);
    }

//    private boolean isIgnoringBatteryOptimizations() {
//        PowerManager pwrm = (PowerManager) requireActivity().getSystemService(Context.POWER_SERVICE);
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            boolean status = pwrm.isIgnoringBatteryOptimizations(requireActivity().getPackageName());
//            Log.v(TAG, "isIgnoringBatteryOptimizations = "  + status);
//            return status;
//        }
//        return true;
//    }
//
//    private void setupBatteryAction() {
//        Preference batteryButton = (Preference) preferenceScreen.findPreference("disable_battery_optim");
//        PreferenceCategory batteryCategory = preferenceScreen.findPreference("disable_battery_header");
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !isIgnoringBatteryOptimizations()) {
//            batteryButton.setVisible(true);
//            batteryCategory.setVisible(true);
//        } else {
//            batteryButton.setVisible(false);
//            batteryCategory.setVisible(false);
//        }
//
//    }


    private void showAboutScreen() {
        LayoutInflater li = LayoutInflater.from(context);
        View view = li.inflate(R.layout.about, null);
        WebView wv = (WebView) view.findViewById(R.id.about_text);
        wv.loadData(getString(R.string.about_text), "text/html", "utf-8");

        AlertDialog.Builder p = new AlertDialog.Builder(context).setView(view);
        final AlertDialog alrt = p.create();
        alrt.setIcon(R.drawable.icon);
        alrt.setTitle(getString(R.string.about_title));
        alrt.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.close),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,
                                        int whichButton) {
                    }
                });
        alrt.show();

    }

    private void setupAnimations() {
        // Custom image chooser
        final Preference customImage = (CheckBoxPreference) preferenceScreen.findPreference("custom_bmp");

        customImage.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference preference, Object checked) {

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
            }
        });


        // Animation Style
        final Preference indexPref = (Preference) findPreference("DrawingIndex");
        final Preference circleTheme = (Preference) findPreference("CircleTheme");

        int dIndex = prefs.getInt("DrawingIndex", 1);
        if (dIndex == 0) {
            indexPref.setSummary(getString(R.string.is_bitmap));
            circleTheme.setEnabled(false);
        } else {
            indexPref.setSummary(getString(R.string.is_circle));
            circleTheme.setEnabled(true);
        }

        indexPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(Preference preference) {
                int dIndex = prefs.getInt("DrawingIndex", 1);
                dIndex++;
                dIndex %= 2;

                if (dIndex == 0) {
                    indexPref.setSummary(getString(R.string.is_bitmap));
                    circleTheme.setEnabled(false);
                    customImage.setEnabled(true);
                } else {
                    indexPref.setSummary(getString(R.string.is_circle));
                    circleTheme.setEnabled(true);
                    customImage.setEnabled(false);
                }

                SharedPreferences.Editor mSettingsEdit = prefs.edit();
                mSettingsEdit.putInt("DrawingIndex", dIndex);
                mSettingsEdit.apply();
                return true;

            }

        });
    }

    private void setupTonePicker() {
        ListPreference tone = (ListPreference) preferenceScreen.findPreference("NotificationUri");
        play = (Preference) findPreference("playSound");

        ListPreference pretone = (ListPreference) preferenceScreen.findPreference("PreSoundUri");
        preplay = (Preference) preferenceScreen.findPreference("playPreSound");

        String[] entries = getResources().getStringArray(R.array.sound_names);
        final String[] entryValues = getResources().getStringArray(R.array.sound_uris);

        //Default value
        if (tone.getValue() == null) tone.setValue((String) entryValues[1]);
        tone.setDefaultValue((String) entryValues[1]);

        tone.setEntries(entries);
        tone.setEntryValues(entryValues);

        if (pretone.getValue() == null) pretone.setValue((String) entryValues[0]);
        pretone.setDefaultValue((String) entryValues[0]);

        pretone.setEntries(entries);
        pretone.setEntryValues(entryValues);

        player = new MediaPlayer();


        tone.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                selectTone(newValue, SELECT_RINGTONE, SELECT_FILE);
                return true;
            }

        });

        pretone.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                selectTone(newValue, SELECT_PRE_RINGTONE, SELECT_PRE_FILE);
                return true;
            }

        });

        play.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(final Preference preference) {
                String notificationUri = prefs.getString("NotificationUri", "android.resource://org.yuttadhammo.BodhiTimer/" + R.raw.bell);
                prePlayTone(notificationUri, preference);

                return false;
            }

        });

        preplay.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(final Preference preference) {
                String PreSoundUri = prefs.getString("PreSoundUri", "android.resource://org.yuttadhammo.BodhiTimer/" + R.raw.bell);
                prePlayTone(PreSoundUri, preference);

                return false;
            }

        });

    }

    private boolean selectTone(Object newValue, int ringtoneActivity, int fileActivity) {
        if (player.isPlaying()) {
            play.setTitle(context.getString(R.string.play_sound));
            play.setSummary(context.getString(R.string.play_sound_desc));
            preplay.setTitle(context.getString(R.string.play_pre_sound));
            preplay.setSummary(context.getString(R.string.play_pre_sound_desc));

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

            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("audio/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);

            try {
                getActivity().startActivityForResult(Intent.createChooser(intent, "Select Sound File"), fileActivity);
            } catch (ActivityNotFoundException ex) {
                Toast.makeText(getActivity(), "Please install a File Manager.", Toast.LENGTH_SHORT).show();
            }
        }

        return true;
    }

    private void prePlayTone(String ToneUri, final Preference preference) {

        boolean isPre = (preference.getKey().equals("playPreSound"));

        if (player.isPlaying()) {
            player.stop();
            play.setTitle(context.getString(R.string.play_sound));
            play.setSummary(context.getString(R.string.play_sound_desc));
            preplay.setTitle(context.getString(R.string.play_pre_sound));
            preplay.setSummary(context.getString(R.string.play_pre_sound_desc));
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
            player.setDataSource(context, Uri.parse(ToneUri));
            player.prepare();
            player.setLooping(false);
            player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    preference.setTitle(context.getString(R.string.play_sound));
                    preference.setSummary(context.getString(R.string.play_sound_desc));
                    Log.v(TAG, "Resetting media player...");
                    mp.reset();
                }
            });
            player.start();
            preference.setTitle(context.getString(R.string.playing_sound));
            preference.setSummary(context.getString(R.string.playing_sound_desc));
        } catch (IOException e) {
            Log.e(TAG, "Failed to play uri: " + ToneUri);
            e.printStackTrace();
        }

    }
}

