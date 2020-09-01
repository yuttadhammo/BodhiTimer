/*
    This file is part of Bodhi Timer.

    Bodhi Timer is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Bodhi Timer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Bodhi Timer.  If not, see <http://www.gnu.org/licenses/>.
*/

package org.yuttadhammo.BodhiTimer;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager.LayoutParams;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class TimerPrefActivity extends PreferenceActivity {
    private static final String TAG = TimerPrefActivity.class.getSimpleName();
    private SharedPreferences prefs;
    private Context context;
    private static Activity activity;
    private MediaPlayer player;
    private Preference play;
    private Preference preplay;

    private final int SELECT_RINGTONE = 0;
    private final int SELECT_FILE = 1;
    private final int SELECT_PRE_RINGTONE = 2;
    private final int SELECT_PRE_FILE = 3;
    private final int SELECT_PHOTO = 4;

    private String lastToneType;
    private String lastPreToneType;

    private TextToSpeech tts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = this;
        activity = this;

        tts = new TextToSpeech(this, null);

        prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        if (prefs.getBoolean("FULLSCREEN", false))
            getWindow().setFlags(LayoutParams.FLAG_FULLSCREEN, LayoutParams.FLAG_FULLSCREEN);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);

        // Load the sounds
        ListPreference tone = (ListPreference) findPreference("NotificationUri");
        play = (Preference) findPreference("playSound");

        ListPreference pretone = (ListPreference) findPreference("PreSoundUri");
        preplay = (Preference) findPreference("playPreSound");

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

        lastToneType = prefs.getString("NotificationUri", (String) entryValues[1]);
        lastPreToneType = prefs.getString("NotificationUri", (String) entryValues[0]);

        tone.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
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
                    activity.startActivityForResult(intent, SELECT_RINGTONE);
                } else if (newValue.toString().equals("file")) {

                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.setType("audio/*");
                    intent.addCategory(Intent.CATEGORY_OPENABLE);

                    try {
                        activity.startActivityForResult(Intent.createChooser(intent, "Select Sound File"), SELECT_FILE);
                    } catch (ActivityNotFoundException ex) {
                        Toast.makeText(activity, "Please install a File Manager.",
                                Toast.LENGTH_SHORT).show();
                    }
                } else if (newValue.toString().equals("tts")) {
                    final EditText input = new EditText(context);
                    input.setText(prefs.getString("tts_string", ""));
                    new AlertDialog.Builder(context)
                            .setTitle(getString(R.string.input_text))
                            .setMessage(getString(R.string.input_text_desc))
                            .setView(input)
                            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    if (input.getText().toString().equals(""))
                                        return;

                                    Editor editor = prefs.edit();
                                    editor.putString("tts_string", input.getText().toString());
                                    editor.apply();
                                }
                            }).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            // Do nothing.
                        }
                    }).show();
                } else
                    lastToneType = (String) newValue;

                return true;
            }

        });

        play.setOnPreferenceClickListener(new OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(final Preference preference) {
                if (player.isPlaying()) {
                    player.stop();
                    play.setTitle(context.getString(R.string.play_sound));
                    play.setSummary(context.getString(R.string.play_sound_desc));
                    preplay.setTitle(context.getString(R.string.play_pre_sound));
                    preplay.setSummary(context.getString(R.string.play_pre_sound_desc));
                    return false;
                }

                try {
                    String notificationUri = prefs.getString("NotificationUri", "android.resource://org.yuttadhammo.BodhiTimer/" + R.raw.bell);
                    if (notificationUri.equals("system"))
                        notificationUri = prefs.getString("SystemUri", "");
                    else if (notificationUri.equals("file"))
                        notificationUri = prefs.getString("FileUri", "");
                    else if (notificationUri.equals("tts")) {
                        notificationUri = "";
                        final String ttsString = prefs.getString("tts_string", context.getString(R.string.timer_done));
                        tts.speak(ttsString, TextToSpeech.QUEUE_ADD, null);
                    }
                    if (notificationUri.equals(""))
                        return false;
                    Log.v(TAG, "Playing Uri: " + notificationUri);
                    player.reset();
                    int currVolume = prefs.getInt("tone_volume", 0);
                    if (currVolume != 0) {
                        float log1 = (float) (Math.log(100 - currVolume) / Math.log(100));
                        player.setVolume(1 - log1, 1 - log1);
                    }
                    player.setDataSource(context, Uri.parse(notificationUri));
                    player.prepare();
                    player.setLooping(false);
                    player.setOnCompletionListener(new OnCompletionListener() {
                        @Override
                        public void onCompletion(MediaPlayer mp) {
                            preference.setTitle(context.getString(R.string.play_sound));
                            preference.setSummary(context.getString(R.string.play_sound_desc));
                        }
                    });
                    player.start();
                    preference.setTitle(context.getString(R.string.playing_sound));
                    preference.setSummary(context.getString(R.string.playing_sound_desc));
                } catch (IOException e) {
                    // TODO Auto-generated catch block

                    e.printStackTrace();
                }

                return false;
            }

        });

        // pre play tone


        pretone.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
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
                    activity.startActivityForResult(intent, SELECT_PRE_RINGTONE);
                } else if (newValue.toString().equals("file")) {

                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.setType("audio/*");
                    intent.addCategory(Intent.CATEGORY_OPENABLE);

                    try {
                        activity.startActivityForResult(Intent.createChooser(intent, "Select Sound File"), SELECT_PRE_FILE);
                    } catch (ActivityNotFoundException ex) {
                        Toast.makeText(activity, "Please install a File Manager.",
                                Toast.LENGTH_SHORT).show();
                    }
                } else if (newValue.toString().equals("tts")) {
                    final EditText input = new EditText(context);
                    input.setText(prefs.getString("tts_string_pre", ""));

                    new AlertDialog.Builder(context)
                            .setTitle(getString(R.string.input_text))
                            .setMessage(getString(R.string.input_text_desc))
                            .setView(input)
                            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    if (input.getText().toString().equals(""))
                                        return;

                                    Editor editor = prefs.edit();
                                    editor.putString("tts_string_pre", input.getText().toString());
                                    editor.apply();
                                }
                            }).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            // Do nothing.
                        }
                    }).show();
                } else
                    lastPreToneType = (String) newValue;

                return true;
            }

        });

        preplay.setOnPreferenceClickListener(new OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(final Preference preference) {
                if (player.isPlaying()) {
                    player.stop();
                    play.setTitle(context.getString(R.string.play_sound));
                    play.setSummary(context.getString(R.string.play_sound_desc));
                    preplay.setTitle(context.getString(R.string.play_pre_sound));
                    preplay.setSummary(context.getString(R.string.play_pre_sound_desc));
                    return false;
                }

                try {
                    String preSoundUri = prefs.getString("PreSoundUri", "android.resource://org.yuttadhammo.BodhiTimer/" + R.raw.bell);
                    if (preSoundUri.equals("system"))
                        preSoundUri = prefs.getString("SystemUri", "");
                    else if (preSoundUri.equals("file"))
                        preSoundUri = prefs.getString("FileUri", "");
                    else if (preSoundUri.equals("tts")) {
                        preSoundUri = "";
                        final String ttsString = prefs.getString("tts_string_pre", context.getString(R.string.timer_done));
                        tts.speak(ttsString, TextToSpeech.QUEUE_ADD, null);
                    }

                    if (preSoundUri.equals(""))
                        return false;
                    Log.v(TAG, "Playing Uri: " + preSoundUri);
                    player.reset();
                    int currVolume = prefs.getInt("tone_volume", 0);
                    if (currVolume != 0) {
                        float log1 = (float) (Math.log(100 - currVolume) / Math.log(100));
                        player.setVolume(1 - log1, 1 - log1);
                    }
                    player.setDataSource(context, Uri.parse(preSoundUri));
                    player.prepare();
                    player.setLooping(false);
                    player.setOnCompletionListener(new OnCompletionListener() {
                        @Override
                        public void onCompletion(MediaPlayer mp) {
                            preference.setTitle(context.getString(R.string.play_sound));
                            preference.setSummary(context.getString(R.string.play_sound_desc));
                        }
                    });
                    player.start();
                    preference.setTitle(context.getString(R.string.playing_sound));
                    preference.setSummary(context.getString(R.string.playing_sound_desc));
                } catch (IOException e) {
                    // TODO Auto-generated catch block

                    e.printStackTrace();
                }

                return false;
            }

        });

        Preference about = (Preference) findPreference("aboutPref");

        about.setOnPreferenceClickListener(new OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(Preference preference) {
                LayoutInflater li = LayoutInflater.from(context);
                View view = li.inflate(R.layout.about, null);
                WebView wv = (WebView) view.findViewById(R.id.about_text);
                wv.loadData(getString(R.string.about_text), "text/html", "utf-8");

                Builder p = new AlertDialog.Builder(context).setView(view);
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
                return true;

            }

        });

        final Preference bmpUrl = (Preference) findPreference("bmp_url");
        CheckBoxPreference customBmp = (CheckBoxPreference) findPreference("custom_bmp");
        if (!customBmp.isChecked())
            bmpUrl.setEnabled(false);

        bmpUrl.setOnPreferenceClickListener(new OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                photoPickerIntent.setType("image/*");
                startActivityForResult(photoPickerIntent, SELECT_PHOTO);
                return true;

            }

        });


        customBmp.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {

                if (newValue.toString().equals("true"))
                    bmpUrl.setEnabled(true);
                else
                    bmpUrl.setEnabled(false);
                return true;
            }

        });

        CheckBoxPreference full = (CheckBoxPreference) findPreference("FULLSCREEN");

        full.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {

                if (newValue.toString().equals("true"))
                    getWindow().setFlags(LayoutParams.FLAG_FULLSCREEN, LayoutParams.FLAG_FULLSCREEN);
                else
                    getWindow().clearFlags(LayoutParams.FLAG_FULLSCREEN);
                return true;
            }

        });

        final Preference indexPref = (Preference) findPreference("DrawingIndex");
        int dIndex = prefs.getInt("DrawingIndex", 0);
        if (dIndex == 0)
            indexPref.setSummary(getString(R.string.is_bitmap));
        else
            indexPref.setSummary(getString(R.string.is_circle));

        indexPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(Preference preference) {
                int dIndex = prefs.getInt("DrawingIndex", 0);
                dIndex++;
                dIndex %= 2;

                if (dIndex == 0) {
                    indexPref.setSummary(getString(R.string.is_bitmap));
                } else {
                    indexPref.setSummary(getString(R.string.is_circle));
                }
                Editor mSettingsEdit = prefs.edit();
                mSettingsEdit.putInt("DrawingIndex", dIndex);
                mSettingsEdit.commit();
                return true;

            }

        });

        Preference export = (Preference) findPreference("exportPref");

        export.setOnPreferenceClickListener(new OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(Preference preference) {
                Log.i(TAG, "clicked export");

                if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                    Toast.makeText(activity, "SD Card not mounted.",
                            Toast.LENGTH_SHORT).show();
                    return true;
                }

                ProgressDialog downloadProgressDialog = new ProgressDialog(activity);
                downloadProgressDialog.setCancelable(false);
                downloadProgressDialog.setMessage(getString(R.string.exporting));
                downloadProgressDialog.setIndeterminate(true);
                downloadProgressDialog.show();
                try {
                    Log.i(TAG, "exporting");
                    File path = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "music");
                    if (!path.exists())
                        path.mkdir();

                    // Check a second time, if not the most likely cause is the volume doesn't exist
                    if (!path.exists()) {
                        Toast.makeText(activity, "Problem accessing SD Card.",
                                Toast.LENGTH_SHORT).show();
                        return true;
                    }
                    path = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "music" + File.separator + "notifications");

                    if (!path.exists())
                        path.mkdir();

                    InputStream in = null;
                    OutputStream out = null;
                    int[] sounds = {R.raw.bell, R.raw.bell1, R.raw.bowl, R.raw.gong, R.raw.bowl_low};
                    String[] soundNames = {"bell", "bell1", "bowl", "gong", "bowl_low"};
                    downloadProgressDialog.setMax(sounds.length);
                    for (int idx = 0; idx < sounds.length; idx++) {
                        Log.i(TAG, "exporting " + soundNames[idx]);

                        in = activity.getResources().openRawResource(sounds[idx]);
                        File outFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "music" + File.separator + "notifications" + File.separator + soundNames[idx] + ".ogg");
                        if (outFile.exists())
                            continue;
                        outFile.createNewFile();

                        out = new FileOutputStream(outFile);
                        byte[] buffer = new byte[1024];
                        int read;
                        while ((read = in.read(buffer)) != -1) {
                            out.write(buffer, 0, read);
                        }
                        in.close();
                        in = null;
                        out.flush();
                        out.close();
                        out = null;
                        downloadProgressDialog.setProgress(idx + 1);
                    }
                    Toast.makeText(activity, "Sounds copied to" + Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "music" + File.separator + "notifications",
                            Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                downloadProgressDialog.dismiss();
                return true;

            }

        });

    }

    @Override
    public void onPause() {
        if (player.isPlaying()) {
            player.stop();
            play.setTitle(context.getString(R.string.play_sound));
            play.setSummary(context.getString(R.string.play_sound_desc));
        }
        super.onPause();
    }

    @Override
    public void onDestroy() {


        //Close the Text to Speech Library
        if (tts != null) {

            tts.stop();
            tts.shutdown();
            Log.d(TAG, "TTSService Destroyed");
        }

        super.onDestroy();
    }

    @Override
    public void onResume() {

        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        if (prefs.getBoolean("FULLSCREEN", false))
            getWindow().setFlags(LayoutParams.FLAG_FULLSCREEN, LayoutParams.FLAG_FULLSCREEN);
        else
            getWindow().clearFlags(LayoutParams.FLAG_FULLSCREEN);

        super.onResume();
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent intent) {
        if (resultCode == Activity.RESULT_OK) {
            Uri uri = null;
            Editor mSettingsEdit = prefs.edit();
            switch (requestCode) {
                case SELECT_RINGTONE:
                    uri = intent.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
                    if (uri != null) {
                        Log.i("Timer", "Got ringtone " + uri.toString());
                        mSettingsEdit.putString("SystemUri", uri.toString());
                        lastToneType = "system";
                    } else {
                        mSettingsEdit.putString("SystemUri", "");
                        mSettingsEdit.putString("NotificationUri", lastToneType);
                    }
                    break;
                case SELECT_FILE:
                    // Get the Uri of the selected file
                    uri = intent.getData();
                    if (uri != null) {
                        Log.i(TAG, "File Path: " + uri);
                        mSettingsEdit.putString("FileUri", uri.toString());
                        lastToneType = "file";
                    } else {
                        mSettingsEdit.putString("FileUri", "");
                        mSettingsEdit.putString("NotificationUri", lastToneType);
                    }
                    break;
                case SELECT_PRE_RINGTONE:
                    uri = intent.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
                    if (uri != null) {
                        Log.i("Timer", "Got ringtone " + uri.toString());
                        mSettingsEdit.putString("PreSystemUri", uri.toString());
                        lastPreToneType = "system";
                    } else {
                        mSettingsEdit.putString("PreSystemUri", "");
                        mSettingsEdit.putString("PreSoundUri", lastPreToneType);
                    }
                    break;
                case SELECT_PRE_FILE:
                    // Get the Uri of the selected file
                    uri = intent.getData();
                    if (uri != null) {
                        Log.i(TAG, "File Path: " + uri);
                        mSettingsEdit.putString("PreFileUri", uri.toString());
                        lastPreToneType = "file";
                    } else {
                        mSettingsEdit.putString("PreFileUri", "");
                        mSettingsEdit.putString("PreSoundUri", lastPreToneType);
                    }
                    break;
                case SELECT_PHOTO:
                    uri = intent.getData();
                    if (uri != null)
                        mSettingsEdit.putString("bmp_url", uri.toString());
                    else
                        mSettingsEdit.putString("bmp_url", "");
                    break;
            }
            mSettingsEdit.commit();
        }
    }

}