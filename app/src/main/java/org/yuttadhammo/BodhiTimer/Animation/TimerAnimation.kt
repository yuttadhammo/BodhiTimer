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
package org.yuttadhammo.BodhiTimer.Animation

import android.content.Context
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.graphics.Canvas
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import androidx.preference.PreferenceManager
import java.io.FileNotFoundException
import java.util.Vector

class TimerAnimation : AppCompatImageView, OnSharedPreferenceChangeListener {
    var mDrawings = Vector<TimerDrawing>()
    var mIndex = 1
    var mLastTime = 0
    var mLastMax = 0
    var prefs: SharedPreferences? = null
    val mContext: Context

    interface TimerDrawing {
        /**
         * Updates the image to be in sync with the current time
         *
         * @param time in milliseconds
         * @param max  the original time set in milliseconds
         */
        fun updateImage(canvas: Canvas, time: Int, max: Int)
        fun configure()
    }

    constructor(context: Context) : super(context) {
        mContext = context
        prefs = PreferenceManager.getDefaultSharedPreferences(mContext)
        prefs!!.registerOnSharedPreferenceChangeListener(this)

        //setOnClickListener(this);
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        mContext = context
        val prefs = PreferenceManager.getDefaultSharedPreferences(mContext)
        prefs.registerOnSharedPreferenceChangeListener(this)

        //setOnClickListener(this);
    }

    @set:Throws(FileNotFoundException::class)
    var index: Int
        get() = mIndex
        set(i) {
            var i = i
            mDrawings = Vector()
            mDrawings.add(BodhiLeaf(mContext))
            mDrawings.add(CircleAnimation(mContext))
            if (i < 0 || i >= mDrawings.size) i = 0
            mIndex = i
            invalidate()
        }

    fun updateImage(time: Int, max: Int) {
        mLastTime = time
        mLastMax = max
        invalidate()
    }

    public override fun onDraw(canvas: Canvas) {
        if (mIndex < 0 || mIndex >= mDrawings.size) mIndex = 0
        mDrawings[mIndex].updateImage(canvas, mLastTime, mLastMax)
    }

    public fun configure() {
        for (drawing in mDrawings) {
            drawing.configure()
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        if (key == "CircleTheme") {
            configure()
            invalidate()
        }
    }
}