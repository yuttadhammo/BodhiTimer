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
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by noah on 9/16/14.
 */
public class ANumberPicker extends Activity {

    private String TAG = "ANumberPicker";
    private Activity context;
    private SharedPreferences prefs;
    private MyAdapter adapter;
    private String advTimeString;
    private EditText hours;
    private EditText mins;
    private EditText secs;
    ListView listView;
    private List<String> advTimeList;

    String customUri = "sys_def";
    String customSound = "";
    private TextView uriText;
    private DialogInterface mDialog;

    private final int SELECT_RINGTONE = 0;
    private final int SELECT_FILE = 1;

    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        this.context = this;

        prefs = PreferenceManager.getDefaultSharedPreferences(context);

        if(prefs.getBoolean("FULLSCREEN", false))
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        else
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);


        customSound = getString(R.string.sys_def);

        advTimeString = prefs.getString("advTimeString","");

        setContentView(R.layout.adv_number_picker);
        Button add = (Button) findViewById(R.id.add);
        Button cancel = (Button) findViewById(R.id.cancel);
        Button clear = (Button) findViewById(R.id.clear);
        Button save = (Button) findViewById(R.id.save);
        hours = (EditText) findViewById(R.id.hours);
        mins = (EditText) findViewById(R.id.mins);
        secs = (EditText) findViewById(R.id.secs);

        uriText = (TextView) findViewById(R.id.uri);

        hours.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
                if(s.length() >= 2) {
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
                if(s.length() >= 2) {
                    secs.requestFocus();
                    getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                }
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });

        uriText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builderSingle = new AlertDialog.Builder(
                        context);
                builderSingle.setIcon(R.drawable.icon);

                //builderSingle.setTitle("Select One Name:-");

                final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(context, android.R.layout.select_dialog_singlechoice);
                arrayAdapter.add(getString(R.string.sys_def));

                String[] sounds = getResources().getStringArray(R.array.sound_names);

                for(String s: sounds) {
                    arrayAdapter.add(s);
                }

                builderSingle.setNegativeButton(getString(R.string.cancel),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });

                builderSingle.setAdapter(arrayAdapter,
                        new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                if(which > 0) {
                                    customUri = getResources().getStringArray(R.array.sound_uris)[which - 1];
                                    customSound = getResources().getStringArray(R.array.sound_names)[which - 1];
                                }

                                if(which == 0) {
                                    customUri = "sys_def";
                                    customSound = getString(R.string.sys_def);
                                }
                                else if(customUri.equals("system")) {
                                    mDialog = dialog;
                                    Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
                                    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALL);
                                    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Tone");
                                    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, (Uri) null);
                                    context.startActivityForResult(intent, SELECT_RINGTONE);
                                }
                                else if(customUri.equals("file")) {
                                    mDialog = dialog;

                                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                                    intent.setType("audio/*");
                                    intent.addCategory(Intent.CATEGORY_OPENABLE);

                                    try {
                                        context.startActivityForResult(Intent.createChooser(intent, "Select Sound File"), SELECT_FILE);
                                    }
                                    catch (ActivityNotFoundException ex) {
                                        Toast.makeText(context, getString(R.string.get_file_man),
                                                Toast.LENGTH_SHORT).show();
                                    }
                                }

                                uriText.setText(arrayAdapter.getItem(which));
                                dialog.dismiss();
                            }
                        });
                builderSingle.show();
            }
        });

        listView = (ListView) findViewById(R.id.timesList);
        TextView emptyText = (TextView)findViewById(android.R.id.empty);
        listView.setEmptyView(emptyText);

        clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hours.setText("");
                mins.setText("");
                secs.setText("");
            }
        });

        add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addTimeToList();
            }
        });

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString("advTimeString",advTimeString);
                editor.apply();
                finish();
            }
        });


        if(advTimeString.equals("")) {
            advTimeList = new ArrayList<String>();
        }
        else {
            String[] advTime = advTimeString.split("\\^");
            advTimeList = Arrays.asList(advTime);
        }
        adapter = new MyAdapter(context, R.layout.adv_list_item, advTimeList);
        listView.setAdapter(adapter);
    }

    private void addTimeToList() {
        String hs = hours.getText().toString();
        String ms = mins.getText().toString();
        String ss = secs.getText().toString();

        int h = hs.length() > 0 ? Integer.parseInt(hs):0;
        int m = ms.length() > 0 ? Integer.parseInt(ms):0;
        int s = ss.length() > 0 ? Integer.parseInt(ss):0;

        int time =  h*60*60*1000 + m*60*1000 + s*1000;

        advTimeString += (advTimeString.length() == 0 ? "":"^")+time+"#"+customUri+"#"+customSound;
        updateDataSet();
        hours.setText("");
        mins.setText("");
        secs.setText("");
    }

    private void updateDataSet() {
        if(advTimeString.equals("")) {
            advTimeList = new ArrayList<String>();
        }
        else {
            String[] advTime = advTimeString.split("\\^");
            advTimeList = Arrays.asList(advTime);
        }
        adapter = new MyAdapter(context, R.layout.adv_list_item, advTimeList);
        listView.setAdapter(adapter);
        Log.d(TAG, "advTimeString: " + advTimeString);
        Log.d(TAG,"adapter items: "+adapter.getCount());

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

                TextView time = (TextView) rowView.findViewById(R.id.time);

                if (time != null) {
                    String ts = TimerUtils.time2humanStr(context, Integer.parseInt(p[0]));
                    time.setText(ts);
                }
            }
            if (p.length > 2 && p[2].length() > 0) {
                TextView sound = (TextView) rowView.findViewById(R.id.sound);

                if (sound != null) {
                    sound.setText(p[2]);
                }
            }
            Button b = (Button) rowView.findViewById(R.id.delete);
            b.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    removeItem(position);
                }
            });

            return rowView;

        }
    }

    private void removeItem(int p) {
        String[] times = advTimeString.split("\\^");
        advTimeString = "";
        for(int i = 0; i < times.length; i++) {
            if(i == p)
                continue;
            advTimeString += (advTimeString.length() == 0?"":"^")+times[i];
        }
        updateDataSet();
    }


    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent intent) {
        if (resultCode == Activity.RESULT_OK) {
            Uri uri = null;

            switch(requestCode) {
                case SELECT_RINGTONE:
                    uri = intent.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
                    if (uri != null) {
                        customUri = uri.toString();
                        customSound = getString(R.string.sys_tone);
                    } else {
                        customUri = "sys_def";
                        customSound = getString(R.string.sys_def);
                    }
                    break;
                case SELECT_FILE:
                    // Get the Uri of the selected file
                    uri = intent.getData();
                    if (uri != null) {
                        customUri = uri.toString();
                        customSound = getString(R.string.sound_file);
                    } else {
                        customUri = "sys_def";
                        customSound = getString(R.string.sys_def);
                    }
                    break;
            }
            mDialog.dismiss();
        }
    }

}
