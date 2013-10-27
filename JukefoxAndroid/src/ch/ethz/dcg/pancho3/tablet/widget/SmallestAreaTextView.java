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
import android.util.AttributeSet;
import android.view.ViewGroup.LayoutParams;
import android.widget.TextView;

public class SmallestAreaTextView extends TextView {

	// Attributes
	private String text;
	private Paint mTestPaint;
	private TextView testTextView;

	public SmallestAreaTextView(Context context) {
		super(context);
		initialise();
		setSingleLine(false);
	}

	public SmallestAreaTextView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initialise();
	}

	private void initialise() {
		mTestPaint = new Paint();
		mTestPaint.set(this.getPaint());

		testTextView = new TextView(getContext());
		testTextView.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
		testTextView.setSingleLine(false);
	}

	/*
	 * Rearrange the text, so that the area of the text box is minimal.
	 */
	private void refitText(String text) {
		if ((text == null) || (mTestPaint == null)) {
			return;
		}
		testTextView.setTextSize(getTextSize());

		String[] parts = text.split(" ");
		mTestPaint.set(this.getPaint());
		mTestPaint.setTextSize(getTextSize());

		// Find the best line-breaking to get the smallest area
		float minArea = Float.MAX_VALUE;
		for (int i = 0; i < (1 << (parts.length - 1)); ++i) { // 2^{parts.length - 1}
			StringBuffer sb = new StringBuffer(text.length());

			sb.append(parts[0]);
			for (int j = 1; j < parts.length; ++j) {
				if ((i & (1 << (j - 1))) > 0) { // is this bit set?
					sb.append('\n');
				} else {
					sb.append(' ');
				}
				sb.append(parts[j]);
			}
			String testText = sb.toString();

			// Calculate the area of the text
			testTextView.setText(testText);
			testTextView.measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
					MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
			int ww = testTextView.getMeasuredWidth();
			int hh = testTextView.getMeasuredHeight();

			/*String[] lines = testText.split("\n");
			final int LINE_PADDING = 2;
			int w = 0;
			int h = -LINE_PADDING; // First line needs no padding
			for (String line : lines) {
				Rect rect = new Rect();
				mTestPaint.getTextBounds(line, 0, line.length(), rect);

				w = Math.max(w, rect.width());
				h += LINE_PADDING + rect.height();
			}

			float textSize = getTextSize();
			assert textSize > 0;*/

			if (minArea > ww * hh) {
				minArea = ww * hh;
				this.text = testText;
			}
		}

		setText(this.text);
	}

	@Override
	protected void onTextChanged(final CharSequence text, final int start,
			final int before, final int after) {
		String txt = text.toString();
		if (!txt.equals(this.text)) {
			refitText(txt);
		}
	}
}