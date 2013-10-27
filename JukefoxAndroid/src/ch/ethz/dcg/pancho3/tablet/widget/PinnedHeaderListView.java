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
/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ch.ethz.dcg.pancho3.tablet.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ListAdapter;
import android.widget.ListView;

/**
 * A ListView that maintains a header pinned at the top of the list. The pinned
 * header can be pushed up and dissolved as needed.
 */
public class PinnedHeaderListView extends ListView
{

	/**
	 * Adapter interface. The list adapter must implement this interface.
	 */
	public interface PinnedHeaderAdapter {

		/**
		 * Pinned header state: don't show the header.
		 */
		public static final int PINNED_HEADER_GONE = 0;

		/**
		 * Pinned header state: show the header at the top of the list.
		 */
		public static final int PINNED_HEADER_VISIBLE = 1;

		/**
		 * Pinned header state: show the header. If the header extends beyond
		 * the bottom of the first shown element, push it up and clip.
		 */
		public static final int PINNED_HEADER_PUSHED_UP = 2;

		/**
		 * Computes the desired state of the pinned header for the given
		 * position of the first visible list item. Allowed return values are
		 * {@link #PINNED_HEADER_GONE}, {@link #PINNED_HEADER_VISIBLE} or
		 * {@link #PINNED_HEADER_PUSHED_UP}.
		 */
		int getPinnedHeaderState(int position);

		/**
		 * Configures the pinned header view to match the first visible list
		 * item.
		 * 
		 * @param header
		 *            pinned header view.
		 * @param position
		 *            position of the first visible list item.
		 * @param alpha
		 *            fading of the header view, between 0 and 255.
		 */
		void configurePinnedHeader(View header, int position, int alpha, boolean positionChanged,
				int lastPosition);
	}

	private static final int MAX_ALPHA = 255;

	private PinnedHeaderAdapter mAdapter;
	private View mHeaderView;
	private boolean mHeaderViewVisible;
	private int mHeaderViewLeft;
	private int mHeaderViewWidth;
	private int mHeaderViewHeight;
	private int mLastConfiguredPosition = -1;
	private int mLastHeaderState = -1;
	private OnScrollListener mScrollListener;
	private MyScrollListener mLocalScrollListener = new MyScrollListener();

	public PinnedHeaderListView(Context context) {
		super(context);
		super.setOnScrollListener(mLocalScrollListener);
	}

	public PinnedHeaderListView(Context context, AttributeSet attrs) {
		super(context, attrs);
		super.setOnScrollListener(mLocalScrollListener);
	}

	public PinnedHeaderListView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		super.setOnScrollListener(mLocalScrollListener);
	}

	public void setPinnedHeaderView(View view) {
		mLastConfiguredPosition = -1;
		mLastConfiguredPosition = -1;
		mHeaderView = view;
		//mHeaderView.setBackgroundColor(Color.BLACK);
		if (mHeaderView != null) {
			setFadingEdgeLength(0);
		}
		requestLayout();
	}

	@Override
	public void invalidate() {
		super.invalidate();
		configureHeaderView(getFirstVisiblePosition());
	}

	@Override
	public void setOnScrollListener(OnScrollListener l) {
		mScrollListener = l;
	}

	public void setPinnedHeaderAdapter(PinnedHeaderAdapter adapter) {
		mAdapter = adapter;
	}

	@Override
	public void setAdapter(ListAdapter adapter) {
		super.setAdapter(adapter);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		if (mHeaderView != null) {
			measureChild(mHeaderView, widthMeasureSpec, heightMeasureSpec);
			mHeaderViewLeft = getPaddingLeft();
			mHeaderViewWidth = mHeaderView.getMeasuredWidth() + getPaddingLeft();
			mHeaderViewHeight = mHeaderView.getMeasuredHeight();
		}
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		super.onLayout(changed, left, top, right, bottom);
		if (mHeaderView != null) {
			mHeaderView.layout(mHeaderViewLeft, 0, mHeaderViewWidth, mHeaderViewHeight);
			configureHeaderView(getFirstVisiblePosition());
		}
	}

	public void configureHeaderView(int position) {
		if (mHeaderView == null || mAdapter == null) {
			return;
		}

		int state;
		if (mLastConfiguredPosition != position) {
			state = mAdapter.getPinnedHeaderState(position);
		} else {
			state = mLastHeaderState;
		}

		if (state == PinnedHeaderAdapter.PINNED_HEADER_PUSHED_UP) {
			View firstView = getChildAt(0);
			int bottom = firstView.getBottom();
			int headerHeight = mHeaderView.getHeight();
			if (headerHeight != 0) {
				int y;
				if (bottom < headerHeight) {
					y = bottom - headerHeight;
					if (y < -headerHeight) {
						state = PinnedHeaderAdapter.PINNED_HEADER_VISIBLE;
						position = position + 1;
					}
				}
			}
		}

		switch (state) {
			case PinnedHeaderAdapter.PINNED_HEADER_GONE: {
				mHeaderViewVisible = false;
				break;
			}

			case PinnedHeaderAdapter.PINNED_HEADER_VISIBLE: {
				mAdapter.configurePinnedHeader(mHeaderView, position, MAX_ALPHA, mLastConfiguredPosition != position,
						mLastConfiguredPosition);
				if (mHeaderView.getTop() != 0 || mLastConfiguredPosition != position) {
					mHeaderView.layout(mHeaderViewLeft, 0, mHeaderViewWidth, mHeaderViewHeight);
					mHeaderView.invalidate();
				}
				mHeaderViewVisible = true;
				break;
			}

			case PinnedHeaderAdapter.PINNED_HEADER_PUSHED_UP: {
				View firstView = getChildAt(0);
				int bottom = firstView.getBottom();
				int headerHeight = mHeaderView.getHeight();
				if (headerHeight == 0) {
					break;
				}
				int y;
				int alpha;
				if (bottom < headerHeight) {
					y = bottom - headerHeight;
					alpha = MAX_ALPHA * (headerHeight + y) / headerHeight;
				} else {
					y = 0;
					alpha = MAX_ALPHA;
				}

				mAdapter.configurePinnedHeader(mHeaderView, position,
						alpha, mLastConfiguredPosition != position, mLastConfiguredPosition);
				if (mHeaderView.getTop() != y) {
					mHeaderView.layout(mHeaderViewLeft, y, mHeaderViewWidth, mHeaderViewHeight + y);
					mHeaderView.invalidate();
				}
				mHeaderViewVisible = true;
				break;
			}
		}

		mLastConfiguredPosition = position;
		mLastHeaderState = state;
	}

	@Override
	protected void dispatchDraw(Canvas canvas) {
		super.dispatchDraw(canvas);
		if (mHeaderViewVisible) {
			drawChild(canvas, mHeaderView, getDrawingTime());
		}
	}

	private class MyScrollListener implements OnScrollListener {

		@Override
		public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
				int totalItemCount) {
			if (getChildCount() > 0) {
				View topView = getChildAt(0);
				// TODO: refine this formula.
				float alpha = 1 + (topView.getY() - 60) / 300;
				alpha = Math.max(0.0f, Math.min(alpha, 1.0f));
				topView.setAlpha(alpha);
				for (int i = 1; i < getChildCount(); i++) {
					getChildAt(i).setAlpha(1.0f);
				}
			}
			if (mScrollListener != null) {
				mScrollListener.onScroll(view, firstVisibleItem, visibleItemCount, totalItemCount);
			}
		}

		@Override
		public void onScrollStateChanged(AbsListView view, int scrollState) {
			if (mScrollListener != null) {
				mScrollListener.onScrollStateChanged(view, scrollState);
			}
		}
	}
}