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

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.view.View.OnLongClickListener
import android.view.animation.Animation
import android.view.animation.AnimationSet
import android.view.animation.TranslateAnimation
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Gallery
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import org.yuttadhammo.BodhiTimer.Util.Settings

/**
 * Dialog box with an arbitrary number of number pickers
 */
open class NNumberPicker : Activity(), View.OnClickListener, OnLongClickListener {
    interface OnNNumberPickedListener {
        fun onNumbersPicked(number: IntArray?)
    }

    private var hour: Gallery? = null
    private var min: Gallery? = null
    private var sec: Gallery? = null
    internal var i1: String? = null
    internal var i2: String? = null
    internal var i3: String? = null
    internal var i4: String? = null
    private var prefs: SharedPreferences? = null
    private var context: Context? = null
    internal open val layout: Int = R.layout.n_number_picker_dialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        context = this
        setContentView(layout)
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
        val adapter1 = ArrayAdapter(this, R.layout.gallery_item, numbers)
        hour!!.adapter = adapter1
        min!!.adapter = adapter1
        sec!!.adapter = adapter1
        val times = intent.getIntArrayExtra("times")
        hour!!.setSelection(times!![0])
        min!!.setSelection(times[1])
        sec!!.setSelection(times[2])
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
            R.id.btnCancel -> finish()
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
            Toast.makeText(context, context!!.getString(R.string.longclick), Toast.LENGTH_LONG)
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
        } else Toast.makeText(context, context!!.getString(R.string.longclick), Toast.LENGTH_LONG)
            .show()
    }

    internal fun returnResults(values: IntArray) {
        val i = Intent()
        i.putExtra("times", values)
        setResult(RESULT_OK, i)
        finish()
    }

    internal fun returnAdvanced(): Boolean {
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

    internal fun startAdvancedPicker() {
        val i = Intent(this, AdvNumberPicker::class.java)
        startActivityForResult(i, 0)
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

    internal fun setPreset(v: View, i: Int, s: String) {
        var s: String? = s
        var t = s
        if (s == "00:00:00") {
            s = null
            t = when (i) {
                1 -> context!!.getString(R.string.pre1)
                2 -> context!!.getString(R.string.pre2)
                3 -> context!!.getString(R.string.pre3)
                else -> context!!.getString(R.string.pre4)
            }
        }
        if (s == null && (v as TextView).text == t) {
            Toast.makeText(context, context!!.getString(R.string.notset), Toast.LENGTH_LONG).show()
        } else {
            (v as TextView).text = t
        }
        savePreset(i, s)
    }

    private fun savePreset(i: Int, s: String?) {
//        Settings::class.java.getField("pre$i").set(Settings, s!!)
        val editor = prefs!!.edit()
        editor.putString("pre$i", s)
        editor.apply()
    }

    fun setTimes(_times: IntArray) {
        hour!!.setSelection(_times[0])
        min!!.setSelection(_times[1])
        sec!!.setSelection(_times[2])
    }

    companion object {
        fun slideDown(): Animation {
            val set = AnimationSet(true)
            val animation: Animation = TranslateAnimation(
                Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF,
                0.0f, Animation.RELATIVE_TO_SELF, -1.0f,
                Animation.RELATIVE_TO_SELF, 0.0f
            )
            animation.duration = 200
            animation.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation) {
                    // TODO Auto-generated method stub
                }

                override fun onAnimationRepeat(animation: Animation) {
                    // TODO Auto-generated method stub
                }

                override fun onAnimationEnd(animation: Animation) {
                    // TODO Auto-generated method stub
                }
            })
            set.addAnimation(animation)
            return animation
        }
    }
}