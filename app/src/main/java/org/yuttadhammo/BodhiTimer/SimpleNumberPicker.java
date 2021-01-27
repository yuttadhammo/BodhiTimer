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

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Dialog box with an arbitrary number of number pickers
 */
public class SimpleNumberPicker extends AppCompatActivity implements OnClickListener, OnLongClickListener {

    public interface OnNNumberPickedListener {
        void onNumbersPicked(int[] number);
    }

    private TimePicker timePicker;

    private String i1;
    private String i2;
    private String i3;
    private String i4;

    private SharedPreferences prefs;

    private Context context;

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.context = this;

        setContentView(R.layout.n_number_picker_dialog);

        LinearLayout scrollView = findViewById(R.id.container);

        scrollView.setVisibility(View.VISIBLE);

        setupTimePicker();


        Button cancel = findViewById(R.id.btnCancel);
        Button ok = findViewById(R.id.btnOk);
        cancel.setOnClickListener(this);
        ok.setOnClickListener(this);

        Button pre1 = findViewById(R.id.btn1);
        Button pre2 = findViewById(R.id.btn2);
        Button pre3 = findViewById(R.id.btn3);
        Button pre4 = findViewById(R.id.btn4);
        Button adv = findViewById(R.id.btnadv);

        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        i1 = prefs.getString("pre1", null);
        i2 = prefs.getString("pre2", null);
        i3 = prefs.getString("pre3", null);
        i4 = prefs.getString("pre4", null);

        if (i1 != null)
            pre1.setText(i1);
        if (i2 != null)
            pre2.setText(i2);
        if (i3 != null)
            pre3.setText(i3);
        if (i4 != null)
            pre4.setText(i4);

        pre1.setOnClickListener(this);
        pre2.setOnClickListener(this);
        pre3.setOnClickListener(this);
        pre4.setOnClickListener(this);
        pre1.setOnLongClickListener(this);
        pre2.setOnLongClickListener(this);
        pre3.setOnLongClickListener(this);
        pre4.setOnLongClickListener(this);

        adv.setOnClickListener(this);
        adv.setOnLongClickListener(this);

    }



    private void setupTimePicker() {
        // Established times
        int[] times = getIntent().getIntArrayExtra("times");

        // Time Picker
        timePicker = findViewById(R.id.timePicker);
        timePicker.setIs24HourView(true);

        setHour(times);
        setMinute(times);
    }



    /**
     * {@inheritDoc}
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.btnOk:

                int hsel = getHour();
                int msel = getMinute();
                int ssel = 0;

                int[] values = {hsel, msel, ssel};
                returnResults(values);
                break;
            case R.id.btnCancel:
                finish();
                break;
            case R.id.btn1:
                setFromPre(i1);
                break;
            case R.id.btn2:
                setFromPre(i2);
                break;
            case R.id.btn3:
                setFromPre(i3);
                break;
            case R.id.btn4:
                setFromPre(i4);
                break;
            case R.id.btnadv:
                setFromAdv();
                break;

        }

    }

    private void setFromPre(String ts) {
        if (ts == null) {
            Toast.makeText(context, context.getString(R.string.longclick), Toast.LENGTH_LONG).show();
            return;
        }

        int h = Integer.parseInt(ts.substring(0, 2));
        int m = Integer.parseInt(ts.substring(3, 5));
        int s = 0;

        if (ts.length() > 5)
            s = Integer.parseInt(ts.substring(6, 8));

        if (h != 0 || m != 0 || s != 0) {
            int[] values = {h, m, s};
            returnResults(values);
        } else
            Toast.makeText(context, context.getString(R.string.longclick), Toast.LENGTH_LONG).show();
    }

    private void setFromAdv() {
        boolean success = returnAdvanced();

        if (!success) {
            startAdvancedPicker();
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    public boolean onLongClick(View v) {
        String h = getHour() + "";
        String m = getMinute() + "";

        if (h.length() == 1)
            h = "0" + h;
        if (m.length() == 1)
            m = "0" + m;

        String vals = h + ":" + m;
        switch (v.getId()) {
            case R.id.btn1:
                i1 = vals;
                setPreset(v, 1, vals);
                return true;
            case R.id.btn2:
                i2 = vals;
                setPreset(v, 2, vals);
                return true;
            case R.id.btn3:
                i3 = vals;
                setPreset(v, 3, vals);
                return true;
            case R.id.btn4:
                i4 = vals;
                setPreset(v, 4, vals);
                return true;
            case R.id.btnadv:
                startAdvancedPicker();
                return true;
            default:
                return false;
        }
    }

    private void setPreset(View v, int i, String s) {
        String t = s;
        if (s.equals("00:00:00")) {
            s = null;
            t = context.getString(R.string.pre1);
            switch (i) {
                case 2:
                    t = context.getString(R.string.pre2);
                case 3:
                    t = context.getString(R.string.pre3);
                case 4:
                    t = context.getString(R.string.pre4);
            }
        }

        if (s == null && ((TextView) v).getText().equals(t)) {
            Toast.makeText(context, context.getString(R.string.notset), Toast.LENGTH_LONG).show();
        } else
            ((TextView) v).setText(t);

        savePreset(i, s);
    }

    private void savePreset(int i, String s) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("pre" + i, s);
        editor.apply();
    }

    private boolean returnAdvanced() {
        if (prefs.getString("advTimeString", "").length() > 0) {
            int[] values = {-1, -1, -1};
            returnResults(values);
            return true;
        } else {
            return false;
        }
    }

    private void returnResults(int[] values) {
        Intent i = new Intent();
        i.putExtra("times", values);
        setResult(AppCompatActivity.RESULT_OK, i);
        finish();
    }

    private void startAdvancedPicker() {
        Intent i = new Intent(this, AdvNumberPicker.class);
        startActivityForResult(i, 0);
    }

    @Override
    // This is called when we come back from the advanced time picker
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode < 0) {
            returnAdvanced();
        }
    }


    // Helper functions
    private void setMinute(int[] times) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            timePicker.setMinute(times[1]);
        } else {
            timePicker.setCurrentMinute(times[1]);
        }
    }

    private void setHour(int[] times) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            timePicker.setHour(times[0]);
        } else {
            timePicker.setCurrentHour(times[0]);
        }
    }

    private int getMinute() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return timePicker.getMinute();
        } else {
            return timePicker.getCurrentMinute();
        }
    }

    private int getHour() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return timePicker.getHour();
        } else {
            return timePicker.getCurrentHour();
        }
    }
}
