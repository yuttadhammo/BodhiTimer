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
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.net.Uri
import android.os.ParcelFileDescriptor
import org.yuttadhammo.BodhiTimer.Animation.TimerAnimation.TimerDrawing
import org.yuttadhammo.BodhiTimer.R
import org.yuttadhammo.BodhiTimer.Util.Settings
import java.io.IOException

internal class BodhiLeaf(context: Context) : TimerDrawing {
    private var mBitmap: Bitmap? = null
    private val mWidth: Int
    private val mHeight: Int
    private val mProgressPaint: Paint = Paint()
    private var isCustom: Boolean = false

    init {
        mProgressPaint.color = Color.BLACK
        mProgressPaint.alpha = 255
        mProgressPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)

        // Get custom bitmap
        mBitmap = if (!Settings.customBmp  || Settings.bmpUri.isEmpty()) {
            isCustom = false
            BitmapFactory.decodeResource(context.resources, R.drawable.leaf)
        } else {
            isCustom = true
            val bmpUrl = Settings.bmpUri
            val selectedImage = Uri.parse(bmpUrl)
            val resolver = context.contentResolver
            val readOnlyMode = "r"
            var file: ParcelFileDescriptor? = null
            try {
                file = resolver.openFileDescriptor(selectedImage, readOnlyMode)
                BitmapFactory.decodeFileDescriptor(file?.fileDescriptor)
            } catch (e: IOException) {
                e.printStackTrace()
                BitmapFactory.decodeResource(context.resources, R.drawable.leaf)
            } finally {
                file?.close()
            }
        }
        mHeight = mBitmap!!.height
        mWidth = mBitmap!!.width
    }

    /**
     * Updates the image to be in sync with the current time
     *
     * @param time in milliseconds
     * @param max  the original time set in milliseconds
     */
    override fun updateImage(canvas: Canvas, time: Int, max: Int) {
        canvas.save()
        val w = canvas.clipBounds.width()
        val h = canvas.clipBounds.height()
        val rs = Rect(0, 0, mWidth, mHeight)
        val rd: Rect
        if (mHeight / mWidth > h / w) { // image skinnier than canvas
            val nWidth = (mWidth * (h.toFloat() / mHeight.toFloat())).toInt()
            val shift = (w - nWidth) / 2
            rd = Rect(shift, 0, nWidth + shift, h)
        } else { // image fatter than or equal to canvas
            val nHeight = (mHeight * (w.toFloat() / mWidth.toFloat())).toInt()
            var shift = (h - nHeight) / 2
            // Special tweak to visually center the leaf image:
            if (!isCustom) shift -= 90
            rd = Rect(0, shift, w, nHeight + shift)
        }
        canvas.drawBitmap(mBitmap!!, rs, rd, null)
        val p: Float = if (max != 0) time / max.toFloat() else 0F
        mProgressPaint.alpha = (255 - 255 * p).toInt()
        canvas.restore()
    }

    override fun configure(isEditMode: Boolean) {
        // Void
    }
}