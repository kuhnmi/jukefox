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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import ch.ethz.dcg.pancho3.R;

/**
 * TODO: mainly remove a ton of magic numbers
 */
public class SelectRegionButton extends View {

	private static final int WIDTH = 120;
	private static final int HEIGHT = 120;
	private static final int COLOR = Color.WHITE;
	private static final int ALPHA = 35;
	private float scale;

	private Bitmap polygonBitmap;

	private final Paint paint = new Paint();
	private final Paint polygonPaint = new Paint();
	// We will measure width and height once we know how big this view will be.
	private int width = 0;
	private int height = 0;

	private boolean touched = false;

	public SelectRegionButton(Context context) {
		super(context);
		init(context);
	}

	public SelectRegionButton(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	public SelectRegionButton(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context);
	}

	private void init(Context context) {
		scale = context.getResources().getDisplayMetrics().density;
		polygonBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.d156_select_region);
		paint.setColor(COLOR);
		paint.setAlpha(ALPHA);
		paint.setAntiAlias(true);
		polygonPaint.setColor(COLOR);
		polygonPaint.setAntiAlias(true);
		polygonPaint.setAlpha(150);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		canvas.drawCircle(0, height, width, touched ? polygonPaint : paint);
		int y = 2 * height / 3 - polygonBitmap.getHeight() / 2 - 7;
		int x = width / 3 - polygonBitmap.getWidth() / 2 + 5;
		canvas.drawBitmap(polygonBitmap, Math.max(x, 0), Math.max(y, 0), polygonPaint);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				touched = true;
				invalidate();
				break;
			case MotionEvent.ACTION_UP: // Fall through.
			case MotionEvent.ACTION_CANCEL:
				touched = false;
				invalidate();
		}
		return super.onTouchEvent(event);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		width = (int) (WIDTH * scale);
		height = (int) (HEIGHT * scale);
		setMeasuredDimension(MeasureSpec.makeMeasureSpec(width,
				MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
	}
}
