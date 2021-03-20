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
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import androidx.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import org.yuttadhammo.BodhiTimer.Const.SessionTypes;
import org.yuttadhammo.BodhiTimer.Util.Time;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by noah on 9/16/14.
 */
public class AdvNumberPicker extends AppCompatActivity {

    private AppCompatActivity context;
    private SharedPreferences prefs;
    private String advTimeString;
    private EditText hours;
    private EditText mins;
    private EditText secs;


    ListView listView;

    String customUri = "sys_def";
    String[] customUris;
    String[] customSounds;
    private TextView uriText;
    private DialogInterface mDialog;

    private final int SELECT_RINGTONE = 0;
    private final int SELECT_FILE = 1;

    @SuppressWarnings("FieldCanBeLocal")
    private final String TAG = "AdvNumberPicker";

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.context = this;

        prefs = PreferenceManager.getDefaultSharedPreferences(context);

        if (prefs.getBoolean("FULLSCREEN", false))
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        else
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        customUris = getResources().getStringArray(R.array.sound_uris);
        customSounds = getResources().getStringArray(R.array.sound_names);

        advTimeString = prefs.getString("advTimeString", "");

        setContentView(R.layout.adv_number_picker);
        Button add = findViewById(R.id.add);
        Button cancel = findViewById(R.id.cancel);
        Button clear = findViewById(R.id.clear);
        Button save = findViewById(R.id.save);

        hours = findViewById(R.id.hours);
        mins = findViewById(R.id.mins);
        secs = findViewById(R.id.secs);

        uriText = findViewById(R.id.uri);

        hours.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
                if (s.length() >= 2) {
                    mins.requestFocus();
                    getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                }
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });

        mins.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
                if (s.length() >= 2) {
                    secs.requestFocus();
                    getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                }
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });

        uriText.setOnClickListener(view -> {
            AlertDialog.Builder builderSingle = new AlertDialog.Builder(
                    context);
            builderSingle.setIcon(R.mipmap.ic_launcher);


            final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(context, android.R.layout.select_dialog_singlechoice);
            arrayAdapter.add(getString(R.string.sys_def));


            for (String s : customSounds) {
                arrayAdapter.add(s);
            }

            builderSingle.setNegativeButton(getString(R.string.cancel),
                    (dialog, which) -> dialog.dismiss());

            builderSingle.setAdapter(arrayAdapter,
                    (dialog, which) -> {

                        if (which > 0) {
                            customUri = customUris[which - 1];
                        }

                        if (which == 0) {
                            customUri = "sys_def";
                        } else if (customUri.equals("system")) {
                            mDialog = dialog;
                            Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
                            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALL);
                            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Tone");
                            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, (Uri) null);
                            context.startActivityForResult(intent, SELECT_RINGTONE);
                        } else if (customUri.equals("file")) {
                            mDialog = dialog;

                            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                            intent.setType("audio/*");
                            intent.addCategory(Intent.CATEGORY_OPENABLE);

                            try {
                                context.startActivityForResult(Intent.createChooser(intent, "Select Sound File"), SELECT_FILE);
                            } catch (ActivityNotFoundException ex) {
                                Toast.makeText(context, getString(R.string.get_file_man),
                                        Toast.LENGTH_SHORT).show();
                            }
                        }

                        uriText.setText(arrayAdapter.getItem(which));
                        dialog.dismiss();
                    });
            builderSingle.show();
        });

        listView = findViewById(R.id.timesList);
        TextView emptyText = findViewById(android.R.id.empty);
        listView.setEmptyView(emptyText);

        clear.setOnClickListener(v -> {
            hours.setText("");
            mins.setText("");
            secs.setText("");
        });

        add.setOnClickListener(v -> addTimeToList());

        cancel.setOnClickListener(v -> finish());

        save.setOnClickListener(v -> {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("advTimeString", advTimeString);
            editor.apply();
            Intent i = new Intent();
            setResult(AppCompatActivity.RESULT_OK, i);
            finish();
        });

        updateDataSet();

    }

    private void addTimeToList() {
        String hs = hours.getText().toString();
        String ms = mins.getText().toString();
        String ss = secs.getText().toString();

        int h = hs.length() > 0 ? Integer.parseInt(hs) : 0;
        int m = ms.length() > 0 ? Integer.parseInt(ms) : 0;
        int s = ss.length() > 0 ? Integer.parseInt(ss) : 0;

        int time = h * 60 * 60 * 1000 + m * 60 * 1000 + s * 1000;

        advTimeString += (advTimeString.length() == 0 ? "" : "^") + time + "#" + customUri + "#" + SessionTypes.REAL;
        updateDataSet();
        hours.setText("");
        mins.setText("");
        secs.setText("");
    }

    private void updateDataSet() {
        List<String> advTimeList;
        if (advTimeString.equals("")) {
            advTimeList = new ArrayList<>();
        } else {
            String[] advTime = advTimeString.split("\\^");
            advTimeList = Arrays.asList(advTime);
        }
        MyAdapter adapter = new MyAdapter(context, R.layout.adv_list_item, advTimeList);
        listView.setAdapter(adapter);

        Log.d(TAG, "advTimeString: " + advTimeString);
        Log.d(TAG, "adapter items: " + adapter.getCount());

    }

    public class MyAdapter extends ArrayAdapter<String> {


        private final List<String> values;

        public MyAdapter(Context context, int resource, List<String> items) {
            super(context, resource, items);
            this.values = items;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {

            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View rowView = inflater.inflate(R.layout.adv_list_item, parent, false);

            String[] p = values.get(position).split("#");

            if (p[0].length() > 0) {

                TextView timeView = rowView.findViewById(R.id.time);

                if (timeView != null) {
                    String ts = Time.time2humanStr(context, Integer.parseInt(p[0]));
                    timeView.setText(ts);
                }
            }
            if (p.length > 2 && p[2].length() > 0) {
                TextView soundView = rowView.findViewById(R.id.sound);

                if (soundView != null) {
                    soundView.setText(descriptionFromUri(p[1]));
                }
            }
            Button b = rowView.findViewById(R.id.delete);
            b.setOnClickListener(v -> removeItem(position));

            return rowView;

        }
    }

    private String descriptionFromUri(String uri) {
        if ("sys_def".equals(uri)) {
            return getString(R.string.sys_def);
        }// Is it part of our tones?
        int index = Arrays.asList(customUris).indexOf(uri);

        if (index != -1) {
            return customSounds[index];
        }

        return getString(R.string.custom_sound);
    }

    private void removeItem(int p) {
        String[] times = advTimeString.split("\\^");
        advTimeString = "";
        for (int i = 0; i < times.length; i++) {
            if (i == p)
                continue;
            advTimeString = advTimeString.concat((advTimeString.length() == 0 ? "" : "^") + times[i]);
        }
        updateDataSet();
    }


    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (resultCode == Activity.RESULT_OK) {
            Uri uri;

            switch (requestCode) {
                case SELECT_RINGTONE:
                    uri = intent.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
                    if (uri != null) {
                        customUri = uri.toString();
                    } else {
                        customUri = "sys_def";
                    }
                    break;
                case SELECT_FILE:
                    // Get the Uri of the selected file
                    uri = intent.getData();
                    if (uri != null) {
                        customUri = uri.toString();
                    } else {
                        customUri = "sys_def";
                    }
                    break;
            }
            mDialog.dismiss();
        }
    }

}
