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
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

/**
 * A view container with layout behavior like that of the Swing FlowLayout.
 * Originally from
 * http://nishantvnair.wordpress.com/2010/09/28/flowlayout-in-android/
 * 
 * @author Melinda Green
 * @author Sämy Zehnder
 */
public class FlowLayout extends ViewGroup {

	public static class LayoutParams extends ViewGroup.LayoutParams {

		public final int horizontalSpacing;
		public final int verticalSpacing;

		public final boolean center;

		public LayoutParams() {
			this(false, 1, 1); // default of 1px spacing
		}

		/**
		 * @param horizontalSpacing
		 *            Pixels between items, horizontally
		 * @param vertical_spacing
		 *            Pixels between items, vertically
		 */
		public LayoutParams(boolean center, int horizontalSpacing, int verticalSpacing) {
			super(MATCH_PARENT, WRAP_CONTENT);
			this.horizontalSpacing = horizontalSpacing;
			this.verticalSpacing = verticalSpacing;
			this.center = center;
		}
	}

	public FlowLayout(Context context) {
		super(context);
	}

	public FlowLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		assert MeasureSpec.getMode(widthMeasureSpec) != MeasureSpec.UNSPECIFIED;

		final int width = MeasureSpec.getSize(widthMeasureSpec) - getPaddingLeft() - getPaddingRight();
		int height = MeasureSpec.getSize(heightMeasureSpec) - getPaddingTop() - getPaddingBottom();

		int childHeightMeasureSpec;
		if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.AT_MOST) {
			childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.AT_MOST);
		} else {
			childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
		}

		final LayoutParams lp = (LayoutParams) getChildAt(0).getLayoutParams();

		int nextChild = 0;
		Rect lastRowRect = new Rect(0, 0, 0, 0);
		while (nextChild < getChildCount()) {
			// Get the dimensions of this line
			nextChild += getRowRect(lastRowRect, nextChild, lastRowRect.bottom + lp.verticalSpacing, getPaddingLeft(),
					width, true, childHeightMeasureSpec);
		}

		if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.UNSPECIFIED) {
			height = lastRowRect.bottom;
		} else if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.AT_MOST) {
			if (lastRowRect.bottom < height) {
				height = lastRowRect.bottom;
			}
		}
		height += 5; // Fudge to avoid clipping bottom of last row.
		setMeasuredDimension(width, height);
	}// end onMeasure()

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		final int width = r - l;
		int xpos = getPaddingLeft();
		int ypos = getPaddingTop();

		if (getChildCount() == 0) {
			return;
		}

		final LayoutParams lp = (LayoutParams) getChildAt(0).getLayoutParams();

		int nextChild = 0;
		while (nextChild < getChildCount()) {

			// First get the dimensions of this line
			Rect rowRect = new Rect();
			int processedChildren = getRowRect(rowRect, nextChild, ypos, getPaddingLeft(), width, false, 0);

			if (lp.center) {
				xpos = (width - rowRect.width()) / 2; // Add padding so that the row is centered
			}

			// Calculate the child positions
			for (int i = 0; i < processedChildren; ++i) {
				final View child = getChildAt(nextChild);
				++nextChild;

				if (child.getVisibility() != GONE) {
					final int childw = child.getMeasuredWidth();
					final int childh = child.getMeasuredHeight();

					int childY = ypos;
					if (lp.center) {
						childY = ypos + (rowRect.height() - childh) / 2;
					}
					child.layout(xpos, childY, xpos + childw, childY + childh);

					xpos += childw + lp.horizontalSpacing;
				}
			}

			// Prepare for next line
			xpos = getPaddingLeft();
			ypos = rowRect.bottom + lp.verticalSpacing;
		}
	} // end onLayout()

	/**
	 * Computes the bounding rect of the row started with the given child
	 * element. It is shifted to top/left. No padding or anything is added
	 * around it. The child count for this row is returned.
	 * 
	 * @param nextChild
	 * @param top
	 * @param left
	 * @param width
	 * @param doMeasure
	 * @param childHeightMeasureSpec
	 * @return
	 */
	private int getRowRect(Rect rect, final int nextChild, final int top, final int left, final int width,
			final boolean doMeasure, final int childHeightMeasureSpec) {

		rect.set(left, top, left, top);

		boolean first = true;
		int numProcessed = 0;
		for (int i = nextChild; i < getChildCount(); ++i) {
			final View child = getChildAt(i);

			if (child.getVisibility() != GONE) {
				final LayoutParams lp = (LayoutParams) child.getLayoutParams();

				final int childw;
				final int childh;
				if (doMeasure) {
					child.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.AT_MOST), childHeightMeasureSpec);
				}
				childw = child.getMeasuredWidth();
				childh = child.getMeasuredHeight();

				boolean deliedBreak = false;
				if (rect.right + childw > width) {
					if (first) {
						// At least one element has to be processed
						deliedBreak = true;
					} else {
						break;
					}
				}

				if (first) {
					first = false;
					rect.right -= lp.horizontalSpacing;
				}
				rect.right += lp.horizontalSpacing + childw;
				rect.bottom = Math.max(rect.bottom, top + childh);

				if (deliedBreak) {
					++numProcessed;
					break;
				}
			}

			++numProcessed;
		}

		return numProcessed;
	}

	@Override
	protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
		return new LayoutParams();
	}

	@Override
	protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
		if (p instanceof LayoutParams) {
			return true;
		}
		return false;
	}
}