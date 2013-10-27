/* 
 * Copyright 2008-2013, ETH Zürich, Samuel Welten, Michael Kuhn, Tobias Langner,
 * Sandro Affentranger, Lukas Bossard, Michael Grob, Rahul Jain, 
 * Dominic Langenegger, Sonia Mayor Alonso, Roger Odermatt, Tobias Schlueter,
 * Yannick Stucki, Sebastian Wendland, Samuel Zehnder, Samuel Zihlmann,       
 * Samuel Zweifel
 *
 * This file is part of Jukefox.
 *
 * Jukefox is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software 
 * Foundation, either version 3 of the License, or any later version. Jukefox is
 * distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; 
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * Jukefox. If not, see <http://www.gnu.org/licenses/>.
 */
package ch.ethz.dcg.pancho3.view.commons;

import java.util.LinkedList;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

public class TriggeringSlider extends View implements OnTouchListener {
	
	private final static String TAG = TriggeringSlider.class.getSimpleName();
	
	public interface OnTriggerListener {
		public void onTrigger();
	}
	
	private int lockWidth = 50;
	private int lockHeight = 50;
	private Bitmap bitmap;
	
	private Paint paint;
	
	private int width;
	private int triggerPos;
	private Float downPos;
	private float currentPos;
	private LinkedList<OnTriggerListener> listeners;
	

	public TriggeringSlider(Context context) {
		super(context); 
		init();
	}
	
	public TriggeringSlider(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}



	public TriggeringSlider(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}
	
	public void init() {
		listeners = new LinkedList<OnTriggerListener>();
		setOnTouchListener(this);
		paint = new Paint();
		paint.setColor(Color.GRAY);
	}

	public void addOnTriggerListener(OnTriggerListener l) {
		listeners.add(l);
	}
	
	public void setColor(int color) {
		paint = new Paint();
		paint.setColor(color);
	}
	
	public int getLockWidth() {
		return lockWidth;
	}

	
	public void setLockWidth(int lockWidth) {
		this.lockWidth = lockWidth;
		triggerPos = width - lockWidth;
	}

	
	public int getLockHeight() {
		return lockHeight;
	}

	
	public void setLockHeight(int lockHeight) {
		this.lockHeight = lockHeight;
	}

	public Bitmap getBitmap() {
		return bitmap;
	}

	public void setBitmap(Bitmap bitmap) {
		this.bitmap = bitmap;
	}


	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		Rect r = new Rect();
		r.top = 0;
		r.left = 0;
		r.bottom = lockHeight;
		r.right = lockWidth;
		if (downPos != null) {
			moveRect(r);
		}
		Log.v(TAG, "left: " + r.left);
		if (bitmap != null) {
			canvas.drawBitmap(bitmap, null, r, paint);
		} else {
			canvas.drawRect(r, paint);
		}
	}
	




	private void moveRect(Rect r) {
		float diff = currentPos - downPos;
		r.left += diff;
		r.right += diff; 
	}



	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
	    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		width = MeasureSpec.getSize(widthMeasureSpec);
		Log.v(TAG, "width: " + width);
		setMeasuredDimension(width, lockHeight);
	}

	


	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		triggerPos = w - lockWidth; 
		width = w;
		Log.v(TAG, "triggerPos: " + triggerPos);
		super.onSizeChanged(w, h, oldw, oldh);
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
//		Log.v(TAG, "onTouch");
		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			Log.v(TAG, "downPos: " + event.getX());
			if (event.getX() > lockWidth) {
				downPos = null;
				return true;
			}
			downPos = event.getX();
			invalidate();
			return true;
		}
		
		if (event.getAction() == MotionEvent.ACTION_MOVE) {
			Log.v(TAG, "currentPos: " + event.getX());
			currentPos = event.getX();
			invalidate();
			if (downPos != null && currentPos-downPos > triggerPos) {
				informOnTriggerListeners();
			}
			return true;
		}
		
		if (event.getAction() == MotionEvent.ACTION_UP) {
			downPos = null;
			invalidate();
			return true;
		} 
		
		return false;
	}

	private void informOnTriggerListeners() {
		for (OnTriggerListener l: listeners) {
			l.onTrigger();
		}
	}
	
	


	
	

}
