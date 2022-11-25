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
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.SweepGradient
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.VectorDrawable
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import com.google.android.material.color.MaterialColors
import org.yuttadhammo.BodhiTimer.Animation.TimerAnimation.TimerDrawing
import org.yuttadhammo.BodhiTimer.R
import org.yuttadhammo.BodhiTimer.Util.Settings
import org.yuttadhammo.BodhiTimer.Util.Time.time2Array
import kotlin.math.min

internal class CircleAnimation(private val mContext: Context) : TimerDrawing {
    private var mRadius = MAX_SIZE * 0.75f
    private var mInnerRadius = MAX_SIZE / 3f
    private var mSecondRadius = MAX_SIZE
    private var mMsRadius = mSecondRadius + MAX_SIZE / 50f
    private var scale = 0f
    private var mCirclePaint: Paint? = null
    private var mTransparentPaint: Paint? = null
    private var mArcPaint: Paint? = null
    private var mMsPaint: Paint? = null
    private var mEnsoBitmap: Bitmap? = null

    /**
     * Paint for the seconds arc
     */
    private var mSecondPaint: Paint? = null
    private var mSecondBgPaint: Paint? = null
    var mInnerColor = 0
    var mMsFlipper = false
    private var mLastTime: IntArray? = null

    /**
     * Rects for the arcs
     */
    private val mArcRect: RectF
    private val mSecondRect: RectF
    private var mWidth = 0
    private var mHeight = 0
    private var eWidth = 0
    private var eHeight = 0
    private var mSecondGap = 0f
    private var mMsGap = 0f
    private var theme = 0

    init {
        // Create the rects
        mSecondRect = RectF()
        mArcRect = RectF()
        //configure()
    }

    fun invert(src: Bitmap?): Bitmap {
        val height = src!!.height
        val width = src.width
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint()
        val matrixGrayscale = ColorMatrix()
        matrixGrayscale.setSaturation(0f)
        val matrixInvert = ColorMatrix()
        matrixInvert.set(
            floatArrayOf(
                -1.0f, 0.0f, 0.0f, 0.0f, 255.0f,
                0.0f, -1.0f, 0.0f, 0.0f, 255.0f,
                0.0f, 0.0f, -1.0f, 0.0f, 255.0f,
                0.0f, 0.0f, 0.0f, 1.0f, 0.0f
            )
        )
        matrixInvert.preConcat(matrixGrayscale)
        val filter = ColorMatrixColorFilter(matrixInvert)
        paint.colorFilter = filter
        canvas.drawBitmap(src, 0f, 0f, paint)
        return bitmap
    }

    override fun configure(isEditMode: Boolean) {
        val resources = mContext.resources
        var lightTheme = true

        val dayNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        lightTheme = (dayNightMode == Configuration.UI_MODE_NIGHT_NO)

        // Manually set light theme to false if we are during day but in forced dark mode
        if (Settings.isDarkTheme) {
            lightTheme = false
        }

        val colors: IntArray

        colors = intArrayOf(
            MaterialColors.getColor(mContext, R.attr.colorSurface, 0),
            MaterialColors.getColor(mContext, R.attr.colorOnSurface, 1),
            MaterialColors.getColor(mContext, R.attr.colorSurface, 0),
            MaterialColors.getColor(mContext, R.attr.colorSurfaceVariant, 0),
            MaterialColors.getColor(mContext, R.attr.colorOnSurface, 1))

        mEnsoBitmap = getBitmapFromVector(mContext, R.drawable.enso)
        eHeight = mEnsoBitmap!!.height
        eWidth = mEnsoBitmap!!.width
        if (lightTheme) mEnsoBitmap = invert(mEnsoBitmap)

        mCirclePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        mCirclePaint!!.color = colors[0]

        // This should be transparent but it doesnt work :(
        mTransparentPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        mTransparentPaint!!.color = if (lightTheme) Color.WHITE else Color.BLACK
        //mTransparentPaint!!.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)

        // Paint for the second (reverse) line
        mSecondPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        mSecondPaint!!.color = colors[3]
        mSecondBgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        mSecondBgPaint!!.color = colors[1]

        // Paint for the milliseconds
        mMsPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        mMsPaint!!.color = colors[4]
        mInnerColor = colors[2]

        mArcPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        mArcPaint!!.color = if (lightTheme) Color.RED else Color.BLUE

        val mLeadPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        mLeadPaint.style = Paint.Style.FILL
        val mTickerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        mTickerPaint.color = -0x1
        scale = resources.displayMetrics.density
        if (mWidth != 0 && mHeight != 0) sizeChange(mWidth, mHeight)
    }

    fun sizeChange(w: Int, h: Int) {
        mWidth = w
        mHeight = h
        mMsRadius = min(w / 2.0f, h / 2.0f).coerceAtMost(MAX_SIZE * scale)
        mMsGap = mMsRadius * .95f
        mSecondRadius = mMsRadius * .97f
        mSecondGap = mMsRadius * .93f
        mRadius = mMsRadius * .93f
        mInnerRadius = mMsRadius * 0.4f

        val shader: Shader = SweepGradient(0F, 0F, mInnerColor, mInnerColor)
        mArcPaint!!.shader = shader
    }

    /**
     * Updates the image to be in sync with the current time
     *
     * @param time in milliseconds
     * @param max  the original time set in milliseconds
     */
    override fun updateImage(canvas: Canvas, time: Int, max: Int) {
        canvas.save()
        val progress: Float = if (max == 0) 0F else time / max.toFloat()
        val timeVec = time2Array(time)
        if (mLastTime == null) mLastTime = timeVec
        if (mLastTime!![2] != timeVec[2]) mMsFlipper = !mMsFlipper
        val pSecond: Float = if (max == 0) 1F else (timeVec[2] + timeVec[3] / 1000.0f) / 60.0f
        val thetaSecond = pSecond * 360
        if (mWidth != canvas.clipBounds.width() || mHeight != canvas.clipBounds.height()) sizeChange(
            canvas.clipBounds.width(),
            canvas.clipBounds.height()
        )
        canvas.translate(mWidth / 2.0f, mHeight / 2.0f)
        mSecondRect[-mSecondRadius, -mSecondRadius, mSecondRadius] = mSecondRadius
        mArcRect[-mRadius, -mRadius, mRadius] = mRadius
        mLastTime = timeVec
        drawEnso(canvas, progress)
    }

    /**
     * Draws the Enso image based on the current time
     *
     * @param canvas   The canvas to draw on
     * @param progress the original time set in milliseconds
     */
    private fun drawEnso(canvas: Canvas, progress: Float) {
        val START_ANGLE = 117
        val w = canvas.clipBounds.width()
        val h = canvas.clipBounds.height()
        val rs = Rect(0, 0, eWidth, eHeight)
        val rd: Rect
        if (w < h) {
            rd = Rect(0, 0, w, w)
            canvas.translate(w / -2f, h / -2f + (h - w) / 2f)
        } else {
            rd = Rect(0, 0, h, h)
            canvas.translate(w / -2f + (w - h) / 2f, h / -2f)
        }
        canvas.drawBitmap(mEnsoBitmap!!, rs, rd, null)
        canvas.restore()
        canvas.translate(mWidth / 2.0f, mHeight / 2.0f)

        // Uncover arc
        val timeAngle = 360 * (1 - progress)
        var ucAngle = START_ANGLE + timeAngle
//        val mArcPaint = Paint(Paint.ANTI_ALIAS_FLAG)
//        mArcPaint!!.color = MaterialColors.getColor(mContext, R.attr.colorPrimary, 0)
        if (ucAngle > 360) ucAngle -= 360
        canvas.drawArc(mArcRect, ucAngle, 360 - 360 * (1 - progress), true, mArcPaint!!)
    }

    companion object {
        private const val MAX_SIZE = 1000f
        private fun getBitmapFromVector(vectorDrawable: VectorDrawable): Bitmap {
            val bitmap = Bitmap.createBitmap(
                vectorDrawable.intrinsicWidth,
                vectorDrawable.intrinsicHeight, Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            vectorDrawable.setBounds(0, 0, canvas.width, canvas.height)
            vectorDrawable.draw(canvas)
            return bitmap
        }

        fun getBitmapFromVector(context: Context?, @DrawableRes drawableResId: Int): Bitmap {
            return when (val drawable = ContextCompat.getDrawable(context!!, drawableResId)) {
                is BitmapDrawable -> {
                    drawable.bitmap
                }
                is VectorDrawable -> {
                    getBitmapFromVector(drawable)
                }
                else -> {
                    throw IllegalArgumentException("Unsupported drawable type")
                }
            }
        }
    }
}