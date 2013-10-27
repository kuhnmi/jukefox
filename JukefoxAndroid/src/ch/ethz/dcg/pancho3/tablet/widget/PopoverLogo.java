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
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.util.AttributeSet;
import android.view.View;

/**
 * TODO: mainly remove a ton of magic numbers
 */
public class PopoverLogo extends View {

	private static final int WIDTH = 36;
	private static final int HEIGHT = 36;
	private static final int COLOR = Color.WHITE;
	private static final int ALPHA = 45;
	private final float scale;

	private final Paint paint = new Paint();
	private final Paint thinPaint = new Paint();
	// We will measure width and height once we know how big this view will be.
	private int width = 0;
	private int height = 0;

	public PopoverLogo(Context context) {
		super(context);
		scale = context.getResources().getDisplayMetrics().density;
		init();
	}

	public PopoverLogo(Context context, AttributeSet attrs) {
		super(context, attrs);
		scale = context.getResources().getDisplayMetrics().density;
		init();
	}

	public PopoverLogo(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		scale = context.getResources().getDisplayMetrics().density;
		init();
	}

	private void init() {
		paint.setColor(COLOR);
		paint.setAlpha(ALPHA);
		paint.setStyle(Style.STROKE);
		paint.setStrokeWidth(4);
		thinPaint.setColor(COLOR);
		thinPaint.setAlpha(ALPHA);
		thinPaint.setStyle(Style.STROKE);
		thinPaint.setStrokeWidth(2);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		canvas.drawRect(1, 2, width - 2, height - 2, paint);
		for (int i = 0; i < 3; i++) {
			int y = (height - 10) / 3 * i + 10;
			canvas.drawLine(8, y, width - 8, y, thinPaint);
		}
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		width = (int) (WIDTH * scale);
		height = (int) (HEIGHT * scale);
		setMeasuredDimension(MeasureSpec.makeMeasureSpec(width,
				MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
	}

}
