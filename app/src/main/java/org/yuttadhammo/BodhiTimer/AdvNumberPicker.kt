/*
 * AdvNumberPicker.java
 * Copyright (C) 2014-2022 BodhiTimer developers
 *
 * Distributed under terms of the GNU GPLv3 license.
 */
package org.yuttadhammo.BodhiTimer

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import org.yuttadhammo.BodhiTimer.Const.SessionTypes
import org.yuttadhammo.BodhiTimer.Util.Settings
import org.yuttadhammo.BodhiTimer.Util.Time.time2humanStr
import timber.log.Timber

class AdvNumberPicker : AppCompatActivity() {
    private var context: AppCompatActivity? = null
    private var advTimeString: String? = null
    private var hours: EditText? = null
    private var mins: EditText? = null
    private var secs: EditText? = null
    var listView: ListView? = null
    var customUri = "sys_def"
    var customUris: Array<String> = arrayOf()
    var customSounds: Array<String> = arrayOf()
    private var uriText: TextView? = null
    private var mDialog: DialogInterface? = null
    private val SELECT_RINGTONE = 0
    private val SELECT_FILE = 1

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        context = this
        if (Settings.fullscreen) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        }
        customUris = resources.getStringArray(R.array.sound_uris)
        customSounds = resources.getStringArray(R.array.sound_names)
        advTimeString = Settings.advTimeString
        setContentView(R.layout.adv_number_picker)
        val add = findViewById<Button>(R.id.add)
        val cancel = findViewById<Button>(R.id.cancel)
        val clear = findViewById<Button>(R.id.clear)
        val save = findViewById<Button>(R.id.save)
        hours = findViewById(R.id.hours)
        mins = findViewById(R.id.mins)
        secs = findViewById(R.id.secs)
        uriText = findViewById(R.id.uri)

        hours!!.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {
                if (s.length >= 2) {
                    mins!!.requestFocus()
                    window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
                }
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
        })

        mins!!.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {
                if (s.length >= 2) {
                    secs!!.requestFocus()
                    window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
                }
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
        })

        uriText!!.setOnClickListener {
            val builderSingle = AlertDialog.Builder(
                context!!
            )
            builderSingle.setIcon(R.mipmap.ic_launcher)
            val arrayAdapter =
                ArrayAdapter<String>(context!!, android.R.layout.select_dialog_singlechoice)
            arrayAdapter.add(getString(R.string.sys_def))
            for (s in customSounds) {
                arrayAdapter.add(s)
            }
            builderSingle.setNegativeButton(
                getString(R.string.cancel)
            ) { dialog: DialogInterface, _: Int -> dialog.dismiss() }
            builderSingle.setAdapter(
                arrayAdapter
            ) { dialog: DialogInterface, which: Int ->
                if (which > 0) {
                    customUri = customUris[which - 1]
                }
                if (which == 0) {
                    customUri = "sys_def"
                } else if (customUri == "system") {
                    mDialog = dialog
                    val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
                    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALL)
                    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Tone")
                    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, null as Uri?)
                    context!!.startActivityForResult(intent, SELECT_RINGTONE)
                } else if (customUri == "file") {
                    mDialog = dialog
                    val intent = Intent(Intent.ACTION_GET_CONTENT)
                    intent.type = "audio/*"
                    intent.addCategory(Intent.CATEGORY_OPENABLE)
                    try {
                        context!!.startActivityForResult(
                            Intent.createChooser(
                                intent,
                                "Select Sound File"
                            ), SELECT_FILE
                        )
                    } catch (ex: ActivityNotFoundException) {
                        Toast.makeText(
                            context, getString(R.string.get_file_man),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                uriText!!.text = arrayAdapter.getItem(which)
                dialog.dismiss()
            }
            builderSingle.show()
        }
        listView = findViewById(R.id.timesList)
        val emptyText = findViewById<TextView>(android.R.id.empty)
        listView!!.emptyView = emptyText
        clear.setOnClickListener {
            hours!!.setText("")
            mins!!.setText("")
            secs!!.setText("")
        }
        add.setOnClickListener { addTimeToList() }
        cancel.setOnClickListener { finish() }
        save.setOnClickListener {
            Settings.advTimeString = advTimeString!!
            val i = Intent()
            setResult(RESULT_OK, i)
            finish()
        }
        updateDataSet()
    }

    private fun addTimeToList() {
        val hs = hours!!.text.toString()
        val ms = mins!!.text.toString()
        val ss = secs!!.text.toString()
        val h = if (hs.isNotEmpty()) hs.toInt() else 0
        val m = if (ms.isNotEmpty()) ms.toInt() else 0
        val s = if (ss.isNotEmpty()) ss.toInt() else 0
        val time = h * 60 * 60 * 1000 + m * 60 * 1000 + s * 1000
        advTimeString += (if (advTimeString!!.isEmpty()) "" else "^") + time + "#" + customUri + "#" + SessionTypes.REAL
        updateDataSet()
        hours!!.setText("")
        mins!!.setText("")
        secs!!.setText("")
    }

    private fun updateDataSet() {
        val advTimeList: List<String> = if (advTimeString == "") {
            ArrayList()
        } else {
            val advTime = advTimeString!!.split("\\^").toTypedArray()
            listOf(*advTime)
        }
        val adapter = MyAdapter(context, R.layout.adv_list_item, advTimeList)
        listView!!.adapter = adapter
        Timber.d("advTimeString: %s", advTimeString)
        Timber.d("adapter items: %s", adapter.count)
    }

    inner class MyAdapter(context: Context?, resource: Int, private val values: List<String>) :
        ArrayAdapter<String?>(
            context!!, resource, values
        ) {
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val inflater = context
                .getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
            val rowView = inflater.inflate(R.layout.adv_list_item, parent, false)
            val p = values[position].split("#").toTypedArray()
            if (p[0].isNotEmpty()) {
                val timeView = rowView.findViewById<TextView>(R.id.time)
                if (timeView != null) {
                    val ts = time2humanStr(context, p[0].toInt())
                    timeView.text = ts
                }
            }
            if (p.size > 2 && p[2].isNotEmpty()) {
                val soundView = rowView.findViewById<TextView>(R.id.sound)
                if (soundView != null) {
                    soundView.text = descriptionFromUri(p[1])
                }
            }
            val b = rowView.findViewById<Button>(R.id.delete)
            b.setOnClickListener { removeItem(position) }
            return rowView
        }
    }

    private fun descriptionFromUri(uri: String): String {
        if ("sys_def" == uri) {
            return getString(R.string.sys_def)
        } // Is it part of our tones?
        val index = listOf(*customUris).indexOf(uri)
        return if (index != -1) {
            customSounds[index]
        } else getString(R.string.custom_sound)
    }

    private fun removeItem(p: Int) {
        val times = advTimeString!!.split("\\^").toTypedArray()
        advTimeString = ""
        for (i in times.indices) {
            if (i == p) continue
            advTimeString =
                advTimeString + (if (advTimeString!!.isEmpty()) "" else "^") + times[i]
        }
        updateDataSet()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        if (resultCode == RESULT_OK) {
            val uri: Uri?
            when (requestCode) {
                SELECT_RINGTONE -> {
                    uri = intent!!.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
                    customUri = uri?.toString() ?: "sys_def"
                }
                SELECT_FILE -> {
                    // Get the Uri of the selected file
                    uri = intent!!.data
                    customUri = uri?.toString() ?: "sys_def"
                }
            }
            mDialog!!.dismiss()
        }
    }
}