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
package org.yuttadhammo.BodhiTimer

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TimePicker
import android.widget.Toast
import org.yuttadhammo.BodhiTimer.Util.Settings

/**
 * Dialog box with an arbitrary number of number pickers
 *
 * THIS IS AN UNUSED EXPERIMENT!
 */
class SimpleNumberPicker : NNumberPicker() {
    interface OnNNumberPickedListener {
        fun onNumbersPicked(number: IntArray?)
    }

    private var timePicker: TimePicker? = null
    private var i1: String? = null
    private var i2: String? = null
    private var i3: String? = null
    private var i4: String? = null
    private var context: Context? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        context = this
        setContentView(R.layout.n_number_picker_dialog)
        val scrollView = findViewById<LinearLayout>(R.id.container)
        scrollView.visibility = View.VISIBLE
        setupTimePicker()
        val cancel = findViewById<Button>(R.id.btnCancel)
        val ok = findViewById<Button>(R.id.btnOk)
        cancel.setOnClickListener(this)
        ok.setOnClickListener(this)
        val pre1 = findViewById<Button>(R.id.btn1)
        val pre2 = findViewById<Button>(R.id.btn2)
        val pre3 = findViewById<Button>(R.id.btn3)
        val pre4 = findViewById<Button>(R.id.btn4)
        val adv = findViewById<Button>(R.id.btnadv)
        i1 = Settings.preset1
        i2 = Settings.preset2
        i3 = Settings.preset3
        i4 = Settings.preset4
        if (i1 != null) pre1.text = i1
        if (i2 != null) pre2.text = i2
        if (i3 != null) pre3.text = i3
        if (i4 != null) pre4.text = i4
        pre1.setOnClickListener(this)
        pre2.setOnClickListener(this)
        pre3.setOnClickListener(this)
        pre4.setOnClickListener(this)
        pre1.setOnLongClickListener(this)
        pre2.setOnLongClickListener(this)
        pre3.setOnLongClickListener(this)
        pre4.setOnLongClickListener(this)
        adv.setOnClickListener(this)
        adv.setOnLongClickListener(this)
    }

    private fun setupTimePicker() {
        // Established times
        val times = intent.getIntArrayExtra("times")

        // Time Picker
        timePicker = findViewById(R.id.timePicker)
        timePicker!!.setIs24HourView(true)
        setHour(times)
        setMinute(times)
    }

    /**
     * {@inheritDoc}
     */
    override fun onClick(v: View) {
        when (v.id) {
            R.id.btnOk -> {
                val hsel = hour
                val msel = minute
                val ssel = 0
                val values = intArrayOf(hsel, msel, ssel)
                returnResults(values)
            }
            R.id.btnCancel -> finish()
            R.id.btn1 -> setFromPre(i1)
            R.id.btn2 -> setFromPre(i2)
            R.id.btn3 -> setFromPre(i3)
            R.id.btn4 -> setFromPre(i4)
            R.id.btnadv -> setFromAdv()
        }
    }


    private fun setFromAdv() {
        val success = returnAdvanced()
        if (!success) {
            startAdvancedPicker()
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return
     */
    override fun onLongClick(v: View): Boolean {
        var h = hour.toString() + ""
        var m = minute.toString() + ""
        if (h.length == 1) h = "0$h"
        if (m.length == 1) m = "0$m"

        val str = "$h:$m"

        return when (v.id) {
            R.id.btn1 -> {
                i1 = str
                setPreset(v, 1, str)
                true
            }
            R.id.btn2 -> {
                i2 = str
                setPreset(v, 2, str)
                true
            }
            R.id.btn3 -> {
                i3 = str
                setPreset(v, 3, str)
                true
            }
            R.id.btn4 -> {
                i4 = str
                setPreset(v, 4, str)
                true
            }
            R.id.btnadv -> {
                startAdvancedPicker()
                true
            }
            else -> false
        }
    }



    // This is called when we come back from the advanced time picker
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode < 0) {
            returnAdvanced()
        }
    }

    // Helper functions
    private fun setMinute(times: IntArray?) {
        timePicker!!.minute = times!![1]
    }

    private fun setHour(times: IntArray?) {
        timePicker!!.hour = times!![0]
    }

    private val minute: Int
        get() =  timePicker!!.minute

    private val hour: Int
        get() =  timePicker!!.hour
}