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

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.VectorDrawable;
import androidx.preference.PreferenceManager;

import androidx.annotation.DrawableRes;
import androidx.core.content.ContextCompat;

import org.yuttadhammo.BodhiTimer.R;
import org.yuttadhammo.BodhiTimer.Util.Time;

class CircleAnimation implements TimerAnimation.TimerDrawing {
    private static final float MAX_SIZE = 1000;

    private float mRadius = MAX_SIZE * 0.75f, mInnerRadius = MAX_SIZE / 3f, mSecondRadius = MAX_SIZE, mMsRadius = mSecondRadius + MAX_SIZE / 50f;
    private float scale;

    private Paint mCirclePaint;
    private Paint mInnerPaint;
    private Paint mArcPaint;
    private Paint mMsPaint;

    private Bitmap mEnsoBitmap;

    /**
     * Paint for the seconds arc
     */
    private Paint mSecondPaint, mSecondBgPaint;

    int mInnerColor = 0;

    boolean mMsFlipper = false;
    private int[] mLastTime;

    /**
     * Rects for the arcs
     */
    private final RectF mArcRect;
    private final RectF mSecondRect;

    private final Context mContext;

    private int mWidth;

    private int mHeight;

    private int eWidth;

    private int eHeight;

    private float mSecondGap;

    private float mMsGap;

    private int theme;

    public CircleAnimation(Context context) {
        mContext = context;

        // Create the rects
        mSecondRect = new RectF();
        mArcRect = new RectF();

        configure();
    }

    private static Bitmap getBitmapFromVector(VectorDrawable vectorDrawable) {
        Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(),
                vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        vectorDrawable.draw(canvas);
        return bitmap;
    }


    public static Bitmap getBitmapFromVector(Context context, @DrawableRes int drawableResId) {
        Drawable drawable = ContextCompat.getDrawable(context, drawableResId);
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        } else if (drawable instanceof VectorDrawable) {
            return getBitmapFromVector((VectorDrawable) drawable);
        } else {
            throw new IllegalArgumentException("Unsupported drawable type");
        }
    }


    public Bitmap invert(Bitmap src) {
        int height = src.getHeight();
        int width = src.getWidth();

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();

        ColorMatrix matrixGrayscale = new ColorMatrix();
        matrixGrayscale.setSaturation(0);

        ColorMatrix matrixInvert = new ColorMatrix();
        matrixInvert.set(new float[]
                {
                        -1.0f, 0.0f, 0.0f, 0.0f, 255.0f,
                        0.0f, -1.0f, 0.0f, 0.0f, 255.0f,
                        0.0f, 0.0f, -1.0f, 0.0f, 255.0f,
                        0.0f, 0.0f, 0.0f, 1.0f, 0.0f
                });
        matrixInvert.preConcat(matrixGrayscale);

        ColorMatrixColorFilter filter = new ColorMatrixColorFilter(matrixInvert);
        paint.setColorFilter(filter);

        canvas.drawBitmap(src, 0, 0, paint);
        return bitmap;
    }

    public void configure() {
        Resources resources = mContext.getResources();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);

        // Load the correct theme
        theme = Integer.parseInt(prefs.getString("CircleTheme", "3"));
        boolean invertColors = prefs.getBoolean("invert_colors", false);

        int[] colors;

        switch (theme) {
            case 0:
                colors = new int[]{resources.getColor(R.color.themeA1),
                        resources.getColor(R.color.themeA2),
                        resources.getColor(R.color.themeA3),
                        resources.getColor(R.color.themeA4),
                        resources.getColor(R.color.themeA5)
                };
                break;

            case 1:
                colors = new int[]{resources.getColor(R.color.themeB1),
                        resources.getColor(R.color.themeB2),
                        resources.getColor(R.color.themeB3),
                        resources.getColor(R.color.themeB4),
                        resources.getColor(R.color.themeB5)
                };
                break;

            case 2:
                colors = new int[]{resources.getColor(R.color.themeC1),
                        resources.getColor(R.color.themeC2),
                        resources.getColor(R.color.themeC3),
                        resources.getColor(R.color.themeC4),
                        resources.getColor(R.color.themeC5)
                };
                break;
            case 3:
            default:
                colors = new int[]{resources.getColor(invertColors ? R.color.themeE1 : R.color.themeD1),
                        resources.getColor(invertColors ? R.color.themeE2 : R.color.themeD2),
                        resources.getColor(invertColors ? R.color.themeE3 : R.color.themeD3),
                        resources.getColor(invertColors ? R.color.themeE4 : R.color.themeD4),
                        resources.getColor(invertColors ? R.color.themeE5 : R.color.themeD5)
                };
                break;
        }

        mEnsoBitmap = getBitmapFromVector(mContext, R.drawable.enso);
        eHeight = mEnsoBitmap.getHeight();
        eWidth = mEnsoBitmap.getWidth();

        if (invertColors)
            mEnsoBitmap = invert(mEnsoBitmap);


        mCirclePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mCirclePaint.setColor(colors[0]);

        mInnerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mInnerPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));

        // Paint for the seconds line
        mSecondPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mSecondPaint.setColor(colors[3]);

        mSecondBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mSecondBgPaint.setColor(colors[1]);

        // Paint for the milliseconds
        mMsPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mMsPaint.setColor(colors[4]);

        mInnerColor = colors[2];

        mArcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mArcPaint.setStyle(Paint.Style.FILL);

        Paint mLeadPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mLeadPaint.setStyle(Paint.Style.FILL);

        Paint mTickerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTickerPaint.setColor(0xFFFFFFFF);

        scale = resources.getDisplayMetrics().density;

        if (mWidth != 0 && mHeight != 0) sizeChange(mWidth, mHeight);
    }

    public void sizeChange(int w, int h) {

        mWidth = w;
        mHeight = h;

        mMsRadius = Math.min(Math.min(w / 2.0f, h / 2.0f), MAX_SIZE * scale);
        mMsGap = mMsRadius * .95f;
        mSecondRadius = mMsRadius * .97f;
        mSecondGap = mMsRadius * .93f;
        mRadius = mMsRadius * .93f;
        mInnerRadius = mMsRadius * 0.4f;

        // gradient

        if (theme != 3) {

            int offset = 75;

            int r = Color.red(mInnerColor) - offset;
            int g = Color.green(mInnerColor) - offset;
            int b = Color.blue(mInnerColor) - offset;

            int start = Color.rgb(r, g, b);

            Shader shader = new RadialGradient(0, 0, mRadius, start, mInnerColor, Shader.TileMode.CLAMP);
            mArcPaint.setShader(shader);
        } else {
            Shader shader = new SweepGradient(0, 0, mInnerColor, mInnerColor);
            mArcPaint.setShader(shader);
        }

    }

    /**
     * Updates the image to be in sync with the current time
     *
     * @param time in milliseconds
     * @param max  the original time set in milliseconds
     */
    public void updateImage(Canvas canvas, int time, int max) {

        canvas.save();

        float progress = (max == 0) ? 0 : (time / (float) max);
        int[] timeVec = Time.time2Array(time);
        if (mLastTime == null) mLastTime = timeVec;
        if (mLastTime[2] != timeVec[2]) mMsFlipper = !mMsFlipper;

        float pSecond = (max == 0) ? 1 : (timeVec[2] + timeVec[3] / 1000.0f) / 60.0f;
        float thetaSecond = pSecond * 360;

        if (mWidth != canvas.getClipBounds().width() || mHeight != canvas.getClipBounds().height())
            sizeChange(canvas.getClipBounds().width(), canvas.getClipBounds().height());

        canvas.translate(mWidth / 2.0f, mHeight / 2.0f);

        mSecondRect.set(-mSecondRadius, -mSecondRadius, mSecondRadius, mSecondRadius);
        mArcRect.set(-mRadius, -mRadius, mRadius, mRadius);

        mLastTime = timeVec;

        if (theme == 3) {
            drawEnso(canvas, progress);
        } else {
            drawGenericCircle(canvas, progress, thetaSecond);
        }

    }

    /**
     * Draws the Enso image based on the current time
     *
     * @param canvas   The canvas to draw on
     * @param progress the original time set in milliseconds
     */
    private void drawEnso(Canvas canvas, float progress) {

        int START_ANGLE = 117;

        int w = canvas.getClipBounds().width();
        int h = canvas.getClipBounds().height();

        Rect rs = new Rect(0, 0, eWidth, eHeight);
        Rect rd;

        if (w < h) {
            rd = new Rect(0, 0, w, w);
            canvas.translate(w / -2f, h / -2f + (h - w) / 2f);
        } else {
            rd = new Rect(0, 0, h, h);
            canvas.translate(w / -2f + (w - h) / 2f, h / -2f);
        }
        canvas.drawBitmap(mEnsoBitmap, rs, rd, null);

        canvas.restore();
        canvas.translate(mWidth / 2.0f, mHeight / 2.0f);

        // Uncover arc
        float timeAngle = 360 * (1 - progress);

        float ucAngle = START_ANGLE + timeAngle;

        if (ucAngle > 360)
            ucAngle = ucAngle - 360;

        canvas.drawArc(mArcRect, ucAngle, 360 - 360 * (1 - progress), true, mArcPaint);
    }


    /**
     * Draws a circle based on the current time
     *
     * @param canvas   The canvas to draw on
     * @param progress the original time set in milliseconds
     */
    private void drawGenericCircle(Canvas canvas, float progress, float thetaSecond) {
        int START_ANGLE = 90;

        // We want to draw a very thin border
        canvas.drawCircle(0, 0, mMsRadius, mMsPaint);

        // Gap between the ms and seconds
        canvas.drawCircle(0, 0, mMsGap, mInnerPaint);

        // Second arc
        canvas.drawCircle(0, 0, mSecondRadius, mSecondBgPaint);
        canvas.drawArc(mSecondRect, START_ANGLE, thetaSecond, true, mSecondPaint);

        // Gap between the seconds and the inner radius
        canvas.drawCircle(0, 0, mSecondGap, mInnerPaint);


        // Background fill
        canvas.drawCircle(0, 0, mRadius, mCirclePaint);

        // Main arc
        canvas.drawArc(mArcRect, START_ANGLE, 360 * (1 - progress), true, mArcPaint);
        // Inner paint
        canvas.drawCircle(0, 0, mInnerRadius, mInnerPaint);
        canvas.restore();
    }
}