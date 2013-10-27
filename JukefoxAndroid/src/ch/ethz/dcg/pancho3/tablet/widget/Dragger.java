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
import android.util.AttributeSet;
import android.view.View;

/**
 * A view representing a draggable area. It draws a grid of circles; 2
 * horizontally and as many vertically as fit in 2/3 of the height.
 */
public class Dragger extends View {

	public static final int GRAB_WIDTH = 26;
	private static final int WIDTH = 22;
	public static final int GRAB_HEIGHT = 80;
	private static final int HEIGHT = 64;
	private static final int NUM_CIRCLES_HORIZONTALLY = 2;
	private static final int NUM_CIRCLES_VERTICALLY = 3;
	private static final float SIZE_OF_RADIUS_RELATIVE_TO_BOX_SIZE = 0.4f;
	private static final int COLOR = Color.WHITE;
	private static final int ALPHA = 77;
	private final float scale;

	private final Paint paint = new Paint();
	// We will measure width and height once we know how big this view will be.
	private int width = 0;
	private int height = 0;

	public Dragger(Context context) {
		super(context);
		scale = context.getResources().getDisplayMetrics().density;
		init();
	}

	public Dragger(Context context, AttributeSet attrs) {
		super(context, attrs);
		scale = context.getResources().getDisplayMetrics().density;
		init();
	}

	public Dragger(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		scale = context.getResources().getDisplayMetrics().density;
		init();
	}

	private void init() {
		paint.setColor(COLOR);
		paint.setAntiAlias(true);
		paint.setAlpha(ALPHA);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		// We draw a grid of circles with two circles horizontally
		// and as many circles vertically as we can fit.
		int circleBoxLength = width / NUM_CIRCLES_HORIZONTALLY;
		int numCirclesVertically = NUM_CIRCLES_VERTICALLY;
		float extraPaddingTop = (height - numCirclesVertically * circleBoxLength) / 2;
		float radius = circleBoxLength * SIZE_OF_RADIUS_RELATIVE_TO_BOX_SIZE;
		for (int i = 0; i < numCirclesVertically; i++) {
			for (int j = 0; j < getNumCirclesHorizontally(i); j++) {
				float x = circleBoxLength * (j + 0.5f);
				float y = circleBoxLength * (i + 0.5f) + extraPaddingTop;
				canvas.drawCircle(x, y, radius, paint);
			}
		}
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		width = (int) (WIDTH * scale);
		height = (int) (HEIGHT * scale);
		setMeasuredDimension(MeasureSpec.makeMeasureSpec(width,
				MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
	}

	protected int getNumCirclesHorizontally(int index) {
		return NUM_CIRCLES_HORIZONTALLY;
	}

}
