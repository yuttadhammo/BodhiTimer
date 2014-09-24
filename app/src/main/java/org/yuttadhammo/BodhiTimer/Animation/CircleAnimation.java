package org.yuttadhammo.BodhiTimer.Animation;
import org.yuttadhammo.BodhiTimer.R;
import org.yuttadhammo.BodhiTimer.TimerUtils;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RadialGradient;
import android.graphics.SweepGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.preference.PreferenceManager;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;

class CircleAnimation implements TimerAnimation.TimerDrawing
{
	private static final float MAX_SIZE = 1000;

	private final int START_ANGLE = 90;
	
	private float mRadius = MAX_SIZE*0.75f,mInnerRadius=MAX_SIZE/3f,mSecondRadius=MAX_SIZE,mMsRadius=mSecondRadius+MAX_SIZE/50f;
	private float scale;
	
	private Paint mCirclePaint,mInnerPaint,mArcPaint,mLeadPaint,mMsPaint,mTickerPaint;

	private Bitmap mEnsoBitmap;
	
	RadialGradient mInnerGradient;
	
	/** Paint for the seconds arc */
	private Paint mSecondPaint, mSecondBgPaint;
	
	int mInnerColor = 0;
	
	private boolean showMs = false;
	boolean mMsFlipper = false;
	private int [] mLastTime;
	
	/** Rects for the arcs */
	private RectF mArcRect,mSecondRect,mMsRect;

	private Context mContext;

	private int mWidth;

	private int mHeight;

	private int eWidth;

	private int eHeight;

	private float mSecondGap;

	private float mMsGap;
	
	private int theme;

	private boolean invertColors;
	
	public CircleAnimation(Context context)
	{
		mContext = context;
	
		// Create the rects
		mSecondRect = new RectF();
		mArcRect = new RectF();
		mMsRect = new RectF();
		
		configure();
	}
	
	public void configure()
	{	
		Resources resources = mContext.getResources();
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
		
		// LOAD The correct theme
		theme = Integer.parseInt(prefs.getString("Theme", "3"));
		invertColors = prefs.getBoolean("invert_colors", false);
		
		int [] colors;
		
		switch(theme){
			case 0:
				colors = new int[] { resources.getColor(R.color.themeA1),
						 resources.getColor(R.color.themeA2),
						 resources.getColor(R.color.themeA3),
						 resources.getColor(R.color.themeA4),
						 resources.getColor(R.color.themeA5)
				};
			break;
			
			case 1:
				colors = new int[] { resources.getColor(R.color.themeB1),
						 resources.getColor(R.color.themeB2),
						 resources.getColor(R.color.themeB3),
						 resources.getColor(R.color.themeB4),
						 resources.getColor(R.color.themeB5)
				};
				break;
				
			case 2:
				colors = new int[] { resources.getColor(R.color.themeC1),
						 resources.getColor(R.color.themeC2),
						 resources.getColor(R.color.themeC3),
						 resources.getColor(R.color.themeC4),
						 resources.getColor(R.color.themeC5)
				};
				break;
			case 3:
			default:
				colors = new int[] { resources.getColor(invertColors?R.color.themeE1:R.color.themeD1),
						 resources.getColor(invertColors?R.color.themeE2:R.color.themeD2),
						 resources.getColor(invertColors?R.color.themeE3:R.color.themeD3),
						 resources.getColor(invertColors?R.color.themeE4:R.color.themeD4),
						 resources.getColor(invertColors?R.color.themeE5:R.color.themeD5)
				};
				break;				
		}

		mEnsoBitmap = BitmapFactory.decodeResource(resources, invertColors?R.drawable.ensow_black:R.drawable.ensow);	
		eHeight = mEnsoBitmap.getHeight();
		eWidth = mEnsoBitmap.getWidth();
		
		mCirclePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		mCirclePaint.setColor(colors[0]);
	
		mInnerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		mInnerPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
		
		// Paint for the seconds line 
		mSecondPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		mSecondPaint.setColor(colors[3]);
		
		mSecondBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		mSecondBgPaint.setColor(colors[1]);
		
		// Paint for the miliseconds
		mMsPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		mMsPaint.setColor(colors[4]);
		
		mInnerColor = colors[2];
	
		mArcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		mArcPaint.setStyle(Paint.Style.FILL);
		
		mLeadPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		mLeadPaint.setStyle(Paint.Style.FILL);
		
		mTickerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		mTickerPaint.setColor(0xFFFFFFFF);
		
		scale = resources.getDisplayMetrics().density;
		
		if(mWidth != 0 && mHeight != 0) sizeChange(mWidth,mHeight);
	}
	
	public void sizeChange(int w, int h){
			
		mWidth = w;
		mHeight = h;
		
		mMsRadius = Math.min(Math.min(w/2.0f,h/2.0f),MAX_SIZE*scale);
		mMsGap = mMsRadius * .95f;
		mSecondRadius = mMsRadius * .97f;
		mSecondGap = mMsRadius *.93f;
		mRadius = mMsRadius * .93f;
		mInnerRadius=mMsRadius*0.4f;
		
		// gradient
		
		if(theme != 3) {
		
			int offset = 75;
			
			int r = Color.red(mInnerColor) - offset;
			int g = Color.green(mInnerColor) - offset;
			int b = Color.blue(mInnerColor) - offset;
			
			int start = Color.rgb(r, g, b);
			
			Shader shader = new RadialGradient(0, 0, mRadius, start, mInnerColor, Shader.TileMode.CLAMP);
			mArcPaint.setShader(shader);
		}
		else {
			Shader shader = new SweepGradient (0, 0, mInnerColor, mInnerColor);
			mArcPaint.setShader(shader);
		}
		
	}
	
	/**
	 * Updates the image to be in sync with the current time
	 * @param time in milliseconds
	 * @param max the original time set in milliseconds
	 */
	public void updateImage(Canvas canvas, int time, int max) {
	
		canvas.save();

		float p = (max == 0) ? 0 : (time/(float)max);
		int [] timeVec = TimerUtils.time2Array(time);
		if(mLastTime == null) mLastTime = timeVec;
		if(mLastTime[2] != timeVec[2]) mMsFlipper = !mMsFlipper;
		
		float pSecond = (max == 0) ? 1 : (timeVec[2] + timeVec[3]/1000.0f )/60.0f; 		
		float thetaSecond = pSecond*360;
		
		
		if(mWidth != canvas.getClipBounds().width() || mHeight != canvas.getClipBounds().height())
			sizeChange(canvas.getClipBounds().width(),canvas.getClipBounds().height());
	
		canvas.translate(mWidth/2.0f, mHeight/2.0f);
		
		mSecondRect.set(-mSecondRadius, -mSecondRadius, mSecondRadius, mSecondRadius);
		mArcRect.set(-mRadius, -mRadius, mRadius, mRadius);
			


		mLastTime = timeVec;
		
		// enso

		if(theme == 3) {
			
			// enso

			int w = canvas.getClipBounds().width();
			int h = canvas.getClipBounds().height();
			
			Rect rs = new Rect(0, 0, eWidth, eHeight);
			Rect rd;
			
			if(w < h) {
				rd = new Rect(0,0,w,w);
				canvas.translate(w/-2,h/-2+(h-w)/2);
			}
			else {
				rd = new Rect(0,0,h,h);
				canvas.translate(w/-2+(w-h)/2,h/-2);
			}
			canvas.drawBitmap(mEnsoBitmap, rs, rd, null);
			
			canvas.restore();
			canvas.translate(mWidth/2.0f, mHeight/2.0f);

			// uncover arc
			
			float timeAngle = 360*(1-p);
			
			float ucAngle = START_ANGLE+timeAngle;
			
			if(ucAngle > 360)
				ucAngle = ucAngle - 360;

			canvas.drawArc(mArcRect,ucAngle,360-360*(1-p),true,mArcPaint);
			
			//canvas.drawCircle(0,0,mInnerRadius,mInnerPaint);			
		}
		else {

			// Ms Arc
			if(showMs){
				float pMs = (float)((timeVec[3]/1000.00));
				float thetaMs = pMs*360;

				mMsRect.set(-mMsRadius, -mMsRadius, mMsRadius, mMsRadius);		
				canvas.drawCircle(0,0,mMsRadius, (mMsFlipper) ? mCirclePaint : mMsPaint );
				canvas.drawArc(mMsRect, START_ANGLE, thetaMs, true, (mMsFlipper) ? mMsPaint: mCirclePaint);
			}
			// We want to draw a very thin border
			else{
				canvas.drawCircle(0,0,mMsRadius, mMsPaint );
			}
		
			// Gap between the ms and seconds
			canvas.drawCircle(0,0,mMsGap,mInnerPaint);
					
			//Second arc
			canvas.drawCircle(0,0,mSecondRadius,mSecondBgPaint);
			canvas.drawArc(mSecondRect, START_ANGLE, thetaSecond, true, mSecondPaint);
			
			// Gap between the seconds and the inner radius
			canvas.drawCircle(0,0,mSecondGap,mInnerPaint);
			
			
			// Background fill
			canvas.drawCircle(0,0,mRadius,mCirclePaint);
			
			// Main arc
			canvas.drawArc(mArcRect,START_ANGLE,360*(1-p),true,mArcPaint);
			// Inner paint
			canvas.drawCircle(0,0,mInnerRadius,mInnerPaint);			

		}
		
		canvas.restore();
	}
}