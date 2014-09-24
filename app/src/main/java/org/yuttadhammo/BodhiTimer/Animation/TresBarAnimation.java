/*package org.yuttadhammo.BodhiTimer.Animation;

import org.yuttadhammo.BodhiTimer.R;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import org.yuttadhammo.BodhiTimer.TimerUtils;

class TresBarAnimation implements TimerAnimation.TimerDrawing
{
	// buffer 
	private final int PADDING = 10;
	private final int BAR_HEIGHT = 5;
	
	private Bitmap mCupBitmap;

	private int mWidth = 250;

	private int mHeight = 200;
	
	private Paint mProgressPaint,mBgPaint;

	private RectF mHourRect,mMinuteRect,mSecondRect;
	private RectF mHourFillRect, mMinuteFillRect,mSecondFillRect;
	private Paint mTextPaint;
	private int mTextHeight = 16;
	private int mTopMargin = mTextHeight + PADDING;
	
	public TresBarAnimation(Resources resources)
	{
		mProgressPaint = new Paint();
		mProgressPaint.setColor(resources.getColor(R.color.tea_fill));
		
		mBgPaint = new Paint();
		mBgPaint.setColor(resources.getColor(R.color.dark_gray));
		
		mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		mTextPaint.setColor(Color.WHITE);
		
		mHourRect = new RectF(PADDING,PADDING,mWidth-PADDING,PADDING+BAR_HEIGHT);	
		mMinuteRect = new RectF(PADDING,mHourRect.bottom +mTopMargin,mWidth-PADDING,mHourRect.bottom +mTopMargin+BAR_HEIGHT);	
		mSecondRect = new RectF(PADDING,mMinuteRect.bottom+mTopMargin,mWidth-PADDING,mMinuteRect.bottom+mTopMargin+BAR_HEIGHT);	
		
		mHourFillRect = new RectF(mHourRect);
		mMinuteFillRect = new RectF(mMinuteRect);
		mSecondFillRect = new RectF(mSecondRect);
	}
	
	/**
	 * Updates the image to be in sync with the current time
	 * @param time in milliseconds
	 * @param max the original time set in milliseconds
	
	public Bitmap updateImage(int time,int max)
	{	
		Bitmap bitmap = Bitmap.createBitmap(mWidth,mHeight,Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap);
		bitmap.eraseColor(Color.TRANSPARENT);
				
		float p = (max == 0) ? 0 : (time/(float)max);
		int [] timeVec = TimerUtils.time2Mhs(time);
		
		float pSecond = (max == 0) ? 1 : (float)((timeVec[2]/60.0)); 
		float pMs = (float)((timeVec[3]/1000.00));
		
		// Hour rectangle
		mHourFillRect.right = mHourRect.left+ p*(mHourRect.right-mHourRect.left);
		canvas.drawRect(mHourRect,mBgPaint);
		canvas.drawRect(mHourFillRect,mProgressPaint);
		canvas.drawText(timeVec[0] + " hours", PADDING , mHourRect.top+mTextHeight+3, mTextPaint);
		
		// Minute rect
		canvas.drawRect(mMinuteRect,mBgPaint);
		canvas.drawText(timeVec[1] + " minutes", PADDING , mMinuteRect.top+mTextHeight+3, mTextPaint);
				
		// Second rect
		canvas.drawRect(mSecondRect,mBgPaint);
		canvas.drawText(timeVec[2] + " seconds", PADDING , mSecondRect.top+mTextHeight+3, mTextPaint);
		
		return bitmap;	
	}
}*/