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
import timber.log.Timber
import java.io.FileNotFoundException
import java.util.Vector

class TimerAnimation : AppCompatImageView, OnSharedPreferenceChangeListener {
    var mDrawings = Vector<TimerDrawing>()
    var mIndex = 1
    var mLastTime = 0
    var mLastMax = 0
    var prefs: SharedPreferences? = null
    private val mContext: Context


    interface TimerDrawing {
        /**
         * Updates the image to be in sync with the current time
         *
         * @param time in milliseconds
         * @param max  the original time set in milliseconds
         */
        fun updateImage(canvas: Canvas, time: Int, max: Int)
        fun configure(isEditMode: Boolean)
    }

    constructor(context: Context) : super(context) {
        mContext = context
        prefs = PreferenceManager.getDefaultSharedPreferences(mContext)
        prefs!!.registerOnSharedPreferenceChangeListener(this)
        createDrawings(mContext)
        //setOnClickListener(this);
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        mContext = context
        val prefs = PreferenceManager.getDefaultSharedPreferences(mContext)
        prefs.registerOnSharedPreferenceChangeListener(this)
        createDrawings(mContext)
        //setOnClickListener(this);
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        mContext = context
        val prefs = PreferenceManager.getDefaultSharedPreferences(mContext)
        prefs.registerOnSharedPreferenceChangeListener(this)
        createDrawings(mContext)
    }

    private fun createDrawings(mContext: Context) {
        mDrawings = Vector()
        mDrawings.add(BodhiLeaf(mContext))
        mDrawings.add(CircleAnimation(mContext))
    }

    @set:Throws(FileNotFoundException::class)
    var index: Int
        get() = mIndex
        set(i) {
            Timber.e("Calling Set $i")
            mIndex = i.coerceAtLeast(0).coerceAtMost(mDrawings.size)
            invalidate()
        }

    fun updateImage(time: Int, max: Int) {
        mLastTime = time
        mLastMax = max
        invalidate()
    }

    public override fun onDraw(canvas: Canvas) {
        if (mIndex < 0 || mIndex >= mDrawings.size) mIndex = 0
        if (isInEditMode) {
            createDrawings(context)
            configure()
        }
        val drawing = mDrawings[mIndex]
        drawing.updateImage(canvas, mLastTime, mLastMax)
    }

    fun configure() {
        for (drawing in mDrawings) {
            drawing.configure(isInEditMode)
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
    }
}