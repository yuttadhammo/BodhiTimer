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

import org.yuttadhammo.BodhiTimer.TimerActivity;

import java.io.FileNotFoundException;
import java.util.Vector;

import android.R;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

public class TimerAnimation extends ImageView implements OnClickListener, OnSharedPreferenceChangeListener
{		
	Vector<TimerDrawing> mDrawings = new Vector<TimerDrawing>();
	int mIndex = 0;
	int mLastTime =0,mLastMax=0;
	
	Bitmap mBitmap = null;
	
	Context mContext;
	private TimerActivity mActivity;
	private boolean clicked;
	
	public interface TimerDrawing{
		
		/**
		 * Updates the image to be in sync with the current time
		 * @param time in milliseconds
		 * @param max the original time set in milliseconds
		 */
		public void updateImage( Canvas canvas, int time,int max);
		
		public void configure();
	}
	
	public void setActivity(TimerActivity activity) {
		mActivity = activity;
	}

	public TimerAnimation(Context context){
		
		super(context);
		mContext = context;
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
		prefs.registerOnSharedPreferenceChangeListener(this);
		
		setOnClickListener(this);
	}
	
	public TimerAnimation(Context context, AttributeSet attrs) {
		
		super(context, attrs);
		mContext = context;
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
		prefs.registerOnSharedPreferenceChangeListener(this);
		
		setOnClickListener(this);
	}

	public void setIndex(int i) throws FileNotFoundException {

        mDrawings = new Vector<TimerDrawing>();
        mDrawings.add(new BodhiLeaf(mContext));
        mDrawings.add(new CircleAnimation(mContext));

        if(i < 0 || i >= mDrawings.size()) i = 0;
		mIndex = i;
		invalidate();
	}
	
	public int getIndex(){ return mIndex;}
	
	public void updateImage(int time,int max){
		//Log.v(this.getClass().getCanonicalName(),"time: "+time+" "+max);
		mLastTime = time;
		mLastMax = max;

		invalidate();
	}

	@Override
	public void onDraw(Canvas canvas){
		if(mIndex < 0 || mIndex >= mDrawings.size()) mIndex = 0;
		mDrawings.get(mIndex).updateImage(canvas, mLastTime, mLastMax);
	}
	
	public void onClick(View v){
		if(mActivity.mCurrentState == TimerActivity.STOPPED)
			mActivity.mNM.cancelAll();
		else
			clicked = true;
		
	}

	public void thisClicked() {
		startAnimation(AnimationUtils.loadAnimation(mContext, R.anim.fade_out));
		
		mIndex++;
		mIndex %= mDrawings.size();
		
		startAnimation(AnimationUtils.loadAnimation(mContext, R.anim.fade_in));
		
		invalidate();

	}
	
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		
		if(key.equals("Theme")){	
			for(TimerDrawing drawing : mDrawings)
			{
				drawing.configure();
			}
		}	
		invalidate();
	}

	
}