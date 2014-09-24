package org.yuttadhammo.BodhiTimer;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
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

import java.util.Arrays;
import java.util.List;

/**
 * Created by noah on 9/16/14.
 */
public class ANumberPicker extends Activity {

    private String TAG = "ANumberPicker";
    private Context context;
    private SharedPreferences prefs;
    private MyAdapter adapter;
    private String advTimeString;
    private EditText hours;
    private EditText mins;
    private EditText secs;
    ListView listView;
    private List<String> advTimeList;

    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        this.context = this;

        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        advTimeString = prefs.getString("advTimeString","");

        setContentView(R.layout.adv_number_picker);
        Button add = (Button) findViewById(R.id.add);
        Button cancel = (Button) findViewById(R.id.cancel);
        Button clear = (Button) findViewById(R.id.clear);
        Button save = (Button) findViewById(R.id.save);
        hours = (EditText) findViewById(R.id.hours);
        mins = (EditText) findViewById(R.id.mins);
        secs = (EditText) findViewById(R.id.secs);

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

        listView = (ListView) findViewById(R.id.timesList);

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


        String[] advTime = advTimeString.split("\\^");
        advTimeList = Arrays.asList(advTime);
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

        advTimeString += (advTimeString.length() == 0 ? "":"^")+time+"#"+prefs.getString("NotificationUri","android.resource://org.yuttadhammo.BodhiTimer/" + R.raw.bell);
        updateDataSet();
        hours.setText("");
        mins.setText("");
        secs.setText("");
    }

    private void updateDataSet() {
        String[] advTime = advTimeString.split("\\^");
        advTimeList = Arrays.asList(advTime);
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
}
