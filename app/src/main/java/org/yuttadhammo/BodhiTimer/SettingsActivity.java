package org.yuttadhammo.BodhiTimer;

import android.app.Activity;
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
import android.view.View;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;


public class SettingsActivity extends AppCompatActivity {

    private static final String TAG = SettingsActivity.class.getSimpleName();
    private SharedPreferences prefs;
    private static Context context;
    private AppCompatActivity activity;
    private MediaPlayer player;
    private Preference play;
    private Preference preplay;

    private final int SELECT_RINGTONE = 0;
    private final int SELECT_FILE = 1;
    private final int SELECT_PRE_RINGTONE = 2;
    private final int SELECT_PRE_FILE = 3;
    private final int SELECT_PHOTO = 4;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);


        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(getString(R.string.preferences));
            toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onBackPressed();
                }
            });
        }

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings, new SettingsFragment())
                .commit();


        prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        player = new MediaPlayer();

    }



    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (resultCode == Activity.RESULT_OK) {
            Uri uri;
            SharedPreferences.Editor mSettingsEdit = prefs.edit();
            switch (requestCode) {
                case SELECT_RINGTONE:
                    uri = intent.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
                    if (uri != null) {
                        Log.i("Timer", "Got ringtone " + uri.toString());
                        mSettingsEdit.putString("SystemUri", uri.toString());
                    }
                    break;
                case SELECT_FILE:
                    // Get the Uri of the selected file
                    uri = intent.getData();
                    if (uri != null) {
                        Log.i(TAG, "File Path: " + uri);
                        mSettingsEdit.putString("FileUri", uri.toString());
                    }
                    break;
                case SELECT_PRE_RINGTONE:
                    uri = intent.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
                    if (uri != null) {
                        Log.i("Timer", "Got ringtone " + uri.toString());
                        mSettingsEdit.putString("PreSystemUri", uri.toString());
                    }
                    break;
                case SELECT_PRE_FILE:
                    // Get the Uri of the selected file
                    uri = intent.getData();
                    if (uri != null) {
                        Log.i(TAG, "File Path: " + uri);
                        mSettingsEdit.putString("PreFileUri", uri.toString());
                    }
                    break;
                case SELECT_PHOTO:
                    uri = intent.getData();

                    if (uri != null)
                        getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                        mSettingsEdit.putString("bmp_url", uri.toString());

                    break;
            }
            mSettingsEdit.apply();
        }
    }

}


