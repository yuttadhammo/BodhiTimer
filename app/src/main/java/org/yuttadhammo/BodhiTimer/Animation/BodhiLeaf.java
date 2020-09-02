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

package org.yuttadhammo.BodhiTimer.Animation;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.preference.PreferenceManager;

import org.yuttadhammo.BodhiTimer.R;

import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

class BodhiLeaf implements TimerAnimation.TimerDrawing {
    private Bitmap mCupBitmap;

    private int mWidth = 0;
    private int mHeight = 0;

    private Paint mProgressPaint = null;

    private SharedPreferences prefs;

    public BodhiLeaf(Context context) throws FileNotFoundException {
        mProgressPaint = new Paint();
        mProgressPaint.setColor(Color.BLACK);
        mProgressPaint.setAlpha(255);
        mProgressPaint.setXfermode(new PorterDuffXfermode(Mode.DST_IN));

        // get custom bitmap
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (!prefs.getBoolean("custom_bmp", false) || prefs.getString("bmp_url", "").length() == 0) {
            mCupBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.leaf);
        } else {
            String bmpUrl = prefs.getString("bmp_url", "");
            Uri selectedImage = Uri.parse(bmpUrl);
            ContentResolver resolver = context.getContentResolver();
            String readOnlyMode = "r";

            try {
                ParcelFileDescriptor pfile = resolver.openFileDescriptor(selectedImage, readOnlyMode);
                FileDescriptor file = pfile.getFileDescriptor();
                //InputStream imageStream = resolver.openInputStream(selectedImage);
                mCupBitmap = BitmapFactory.decodeFileDescriptor(file);
            } catch (IOException e) {
                e.printStackTrace();
                mCupBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.leaf);
            }

        }
        mHeight = mCupBitmap.getHeight();
        mWidth = mCupBitmap.getWidth();

    }

    /**
     * Updates the image to be in sync with the current time
     *
     * @param time in milliseconds
     * @param max  the original time set in milliseconds
     */
    public void updateImage(Canvas canvas, int time, int max) {

        canvas.save();
        int w = canvas.getClipBounds().width();
        int h = canvas.getClipBounds().height();

        Rect rs = new Rect(0, 0, mWidth, mHeight);
        Rect rd;

        int nWidth = mWidth;
        int nHeight = mHeight;

        if (mHeight / mWidth > h / w) { // image skinnier than canvas
            nWidth = (int) (mWidth * ((float) h / (float) mHeight));
            int shift = (w - nWidth) / 2;
            rd = new Rect(shift, 0, nWidth + shift, h);
        } else { // image fatter than or equal to canvas
            nHeight = (int) (mHeight * ((float) w / (float) mWidth));
            int shift = (h - nHeight) / 2;
            rd = new Rect(0, shift, w, nHeight + shift);
        }

        //Log.i("Timer",nWidth+" "+nHeight+" "+w+" "+h);

        canvas.drawBitmap(mCupBitmap, rs, rd, null);

        float p = (max != 0) ? (time / (float) max) : 0;

        //if(p == 0) p = 1;

        RectF fill = new RectF(0, 0, canvas.getWidth(), canvas.getHeight());
        mProgressPaint.setAlpha((int) (255 - (255 * p)));
        //canvas.drawRect(fill,mProgressPaint);


        canvas.restore();
    }

    public void configure() {
        // TODO Auto-generated method stub

    }
}