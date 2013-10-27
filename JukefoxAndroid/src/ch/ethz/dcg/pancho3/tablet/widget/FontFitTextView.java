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
package ch.ethz.dcg.pancho3.tablet.widget;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.TextView;

public class FontFitTextView extends TextView {

	// Attributes
	private Paint mTestPaint;

	public FontFitTextView(Context context) {
		super(context);
		setSingleLine(true);
		initialise();
	}

	public FontFitTextView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initialise();
	}

	private void initialise() {
		mTestPaint = new Paint();
		mTestPaint.set(this.getPaint());
		// max size defaults to the initially specified text size unless it is
		// too small
	}

	/*
	 * Re size the font so the specified text fits in the text box assuming the
	 * text box is the specified width.
	 */
	private void refitText(String text, int textWidth, int textHeight) {
		if (textWidth <= 0) {
			return;
		}
		int targetWidth = textWidth - this.getPaddingLeft()
				- this.getPaddingRight();
		int targetHeight = textHeight - this.getPaddingTop()
				- this.getPaddingBottom();
		float hi = 100;
		float lo = 2;
		final float threshold = 0.5f; // How close we have to be

		mTestPaint.set(this.getPaint());

		while (hi - lo > threshold) {
			float size = (hi + lo) / 2;
			mTestPaint.setTextSize(size);
			Rect rect = new Rect();
			mTestPaint.getTextBounds(text, 0, text.length(), rect);
			if (Math.abs(rect.width()) >= targetWidth
					|| Math.abs(rect.height()) >= targetHeight) {
				hi = size; // too big
			} else {
				lo = size; // too small
			}
		}
		// Use lo so that we undershoot rather than overshoot
		this.setTextSize(TypedValue.COMPLEX_UNIT_PX, (float) Math.floor(lo));
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		int parentHeight = MeasureSpec.getSize(heightMeasureSpec);
		int parentWidth = MeasureSpec.getSize(widthMeasureSpec);
		refitText(this.getText().toString(), parentWidth, parentHeight);
		setMeasuredDimension(getMeasuredWidth(), getMeasuredHeight());
	}

	@Override
	protected void onTextChanged(final CharSequence text, final int start,
			final int before, final int after) {
		refitText(text.toString(), this.getWidth(), this.getHeight());
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		if (w != oldw || h != oldh) {
			refitText(this.getText().toString(), w, h);
		}
	}
}