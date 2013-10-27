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
import android.view.View;
import android.view.ViewGroup;

public class GridRow extends ViewGroup {

	private int maxItems = 3;
	private final int outerSpacing;
	private int itemHeight;
	private int itemWidth;

	public GridRow(Context context) {
		super(context);
		outerSpacing = (int) (30 * context.getResources().getDisplayMetrics().density);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		if (getChildCount() > 0) {
			measureChildren(widthMeasureSpec, heightMeasureSpec);
			itemHeight = MeasureSpec.getSize(getChildAt(0).getMeasuredHeight());
			setMeasuredDimension(widthMeasureSpec,
					MeasureSpec.makeMeasureSpec(itemHeight + 2 * outerSpacing,
							MeasureSpec.EXACTLY));
			itemWidth = MeasureSpec.getSize(getChildAt(0).getMeasuredWidth());
		} else {
			super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		}
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		int innerSpacing = Math.max(0, (r - l - 2 * outerSpacing - maxItems * itemWidth) / 2);
		for (int i = 0; i < getChildCount(); i++) {
			View view = getChildAt(i);
			int left = outerSpacing + i * (itemWidth + innerSpacing);
			view.layout(left, outerSpacing, left + itemWidth, outerSpacing + itemHeight);
		}
	}
}
