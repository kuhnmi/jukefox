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
package ch.ethz.dcg.pancho3.tablet.view;

import android.graphics.Canvas;
import android.graphics.Point;
import android.view.View;
import android.view.View.DragShadowBuilder;
import android.view.View.MeasureSpec;

public class TabletDragShadowBuilder extends DragShadowBuilder {

	private final View view;
	private final int width;
	private final int height;

	public TabletDragShadowBuilder(View view, int width, int height) {
		this.view = view;
		this.width = width;
		this.height = height;
	}

	@Override
	public void onDrawShadow(Canvas canvas) {
		view.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
				MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
		view.layout(0, 0, width, height);
		view.draw(canvas);
	}

	@Override
	public void onProvideShadowMetrics(Point shadowSize, Point shadowTouchPoint) {
		shadowSize.set(width, height);
		shadowTouchPoint.set(width, height);
	}
}
