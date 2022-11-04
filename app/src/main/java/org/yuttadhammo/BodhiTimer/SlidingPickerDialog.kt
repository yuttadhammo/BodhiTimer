/*
 * SlidingPickerView.kt
 * Copyright (C) 2014-2022 BodhiTimer developers
 *
 * Distributed under terms of the GNU GPLv3 license.
 */

package org.yuttadhammo.BodhiTimer

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Gallery
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat.startActivity
import org.yuttadhammo.BodhiTimer.Util.Settings


open class SlidingPickerDialog(context: Context) : Dialog(context), View.OnClickListener,
    View.OnLongClickListener {

    private var onConfirm: ((IntArray) -> Unit)? = null
    private var hour: Gallery? = null
    private var min: Gallery? = null
    private var sec: Gallery? = null
    internal var i1: String? = null
    internal var i2: String? = null
    internal var i3: String? = null
    internal var i4: String? = null

    var mTimes: IntArray = IntArray(0)

    internal open val layout: Int = R.layout.sliding_picker_dialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(layout)
        setCancelable(true)
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

    internal open fun setupTimePicker() {
        val numbers = arrayOfNulls<String>(61)
        for (i in 0..60) {
            numbers[i] = i.toString()
        }
        hour = findViewById(R.id.gallery_hour)
        min = findViewById(R.id.gallery_min)
        sec = findViewById(R.id.gallery_sec)
        val adapter1 = ArrayAdapter(context, R.layout.gallery_item, numbers)
        hour!!.adapter = adapter1
        min!!.adapter = adapter1
        sec!!.adapter = adapter1

        hour!!.setSelection(mTimes[0])
        min!!.setSelection(mTimes[1])
        sec!!.setSelection(mTimes[2])
        val htext = findViewById<TextView>(R.id.text_hour)
        val mtext = findViewById<TextView>(R.id.text_min)
        val stext = findViewById<TextView>(R.id.text_sec)
        htext.setOnClickListener(this)
        mtext.setOnClickListener(this)
        stext.setOnClickListener(this)
    }

    /**
     * {@inheritDoc}
     */
    override fun onClick(v: View) {
        when (v.id) {
            R.id.btnOk -> {
                val hsel = hour!!.selectedItemPosition
                val msel = min!!.selectedItemPosition
                val ssel = sec!!.selectedItemPosition
                val values = intArrayOf(hsel, msel, ssel)
                returnResults(values)
            }
            R.id.btnCancel -> cancel()
            R.id.btn1 -> setFromPreset(i1)
            R.id.btn2 -> setFromPreset(i2)
            R.id.btn3 -> setFromPreset(i3)
            R.id.btn4 -> setFromPreset(i4)
            R.id.btnadv -> setFromAdv()
            R.id.text_hour -> hour!!.setSelection(0)
            R.id.text_min -> min!!.setSelection(0)
            R.id.text_sec -> sec!!.setSelection(0)
        }
    }

    internal fun setFromPreset(ts: String?) {
        if (ts == null) {
            Toast.makeText(context, context.getString(R.string.longclick), Toast.LENGTH_LONG)
                .show()
            return
        }
        val h = ts.substring(0, 2).toInt()
        val m = ts.substring(3, 5).toInt()
        var s = 0
        if (ts.length > 5) s = ts.substring(6, 8).toInt()
        if (h != 0 || m != 0 || s != 0) {
            val values = intArrayOf(h, m, s)
            returnResults(values)
        } else Toast.makeText(context, context.getString(R.string.longclick), Toast.LENGTH_LONG)
            .show()
    }

    internal fun returnResults(values: IntArray) {
        onConfirm?.invoke(values)
        dismiss()
    }

    private fun returnAdvanced(): Boolean {
        return if (Settings.advTimeString.isNotEmpty()) {
            val values = intArrayOf(-1, -1, -1)
            returnResults(values)
            true
        } else {
            false
        }
    }

    internal fun setFromAdv() {
        val success = returnAdvanced()
        if (!success) {
            startAdvancedPicker()
        }
    }

    private fun startAdvancedPicker() {
        val i = Intent(context, AdvNumberPicker::class.java)
        startActivity(context, i, null)
    }


    /**
     * {@inheritDoc}
     *
     * @return
     */
    override fun onLongClick(v: View): Boolean {
        val str = getStringFromUI()

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

    open fun getStringFromUI(): String {
        var h = hour!!.selectedItemPosition.toString() + ""
        if (h.length == 1) h = "0$h"
        var m = min!!.selectedItemPosition.toString() + ""
        if (m.length == 1) m = "0$m"
        var s = sec!!.selectedItemPosition.toString() + ""
        if (s.length == 1) s = "0$s"
        return "$h:$m:$s"
    }

    private fun setPreset(v: View, i: Int, s: String) {
        var s: String? = s
        var t = s
        if (s == "00:00:00") {
            s = null
            t = when (i) {
                1 -> context.getString(R.string.pre1)
                2 -> context.getString(R.string.pre2)
                3 -> context.getString(R.string.pre3)
                else -> context.getString(R.string.pre4)
            }
        }
        if (s == null && (v as TextView).text == t) {
            Toast.makeText(context, context.getString(R.string.notset), Toast.LENGTH_LONG).show()
        } else {
            (v as TextView).text = t
        }
        savePreset(i, s)
    }

    private fun savePreset(i: Int, s: String?) {
        if (s == null) return
        when (i) {
            1 -> Settings.preset1 = s
            2 -> Settings.preset2 = s
            3 -> Settings.preset3 = s
            4 -> Settings.preset4 = s
        }
    }

    fun setTimes(_times: IntArray) {
        mTimes = _times
        hour!!.setSelection(_times[0])
        min!!.setSelection(_times[1])
        sec!!.setSelection(_times[2])
    }

    fun setOnConfirm(function: (IntArray) -> Unit) {
        onConfirm = function
    }

}