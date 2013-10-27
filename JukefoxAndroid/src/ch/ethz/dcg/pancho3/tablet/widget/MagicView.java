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
import android.database.DataSetObserver;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import ch.ethz.dcg.pancho3.R;
import ch.ethz.dcg.pancho3.tablet.widget.SwipeHelper.Callback;

/**
 * View which consists of a list with a header element on top that doesn't
 * scroll. The view allows the dragging and swipe removing of elements.
 * 
 * The MagicView needs a MagicListAdapter to be fully operational.
 * 
 * @author Yannick Stucki (yannickstucki@gmail.com)
 * 
 */
public class MagicView extends LinearLayout implements Callback {

	private static final int ITEM_HEIGHT = 64;
	// The header wraps one item and is always on top (doesn't scroll).
	private final FrameLayout magicHeader;
	private final View nowPlayingView;
	// The rect will be used for hitting calculations.
	private final Rect rect = new Rect();
	// The list view contains the rest of the items.
	private final MagicListView listView;
	// A magic view needs a magic adapter.
	private MagicListAdapter<?> magicListAdapter;

	// Dragging is the process of reordering items or swiping them to be
	// removed.
	// True while dragging occurs
	private boolean isDragging = false;

	private final FrameLayout draggedView;
	private View draggedViewContent;

	private int draggedYOffset;
	private int draggedViewY;

	private int lastPosition = -1;

	private int firstYCoordinate = 0;

	private OnItemClickListener listener;

	private int lastY;

	private final int itemHeight;

	private SwipeHelper swipeHelper;

	/**
	 * The constructor.
	 */
	public MagicView(Context context, AttributeSet attrs) {
		super(context, attrs);
		nowPlayingView = LayoutInflater.from(context).inflate(R.layout.tablet_nowplaying, null);
		magicHeader = (FrameLayout) nowPlayingView.findViewById(R.id.headercontainer);
		this.addView(nowPlayingView);

		listView = new MagicListView(context);
		listView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,
				LayoutParams.MATCH_PARENT));
		this.addView(listView);
		draggedView = new FrameLayout(context);
		this.addView(draggedView);
		draggedView.setBackgroundColor(context.getResources().getColor(R.color.trans_dark));
		itemHeight = Math.round(context.getResources().getDisplayMetrics().density * ITEM_HEIGHT);

		float densityScale = getResources().getDisplayMetrics().density;
		float pagingTouchSlop = ViewConfiguration.get(context).getScaledPagingTouchSlop();
		swipeHelper = new SwipeHelper(SwipeHelper.X, this, densityScale, pagingTouchSlop);

		listView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				if (listener != null) {
					listener.onItemClick(parent, view, position + 1, id);
				}
			}
		});

		magicHeader.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				listener.onItemClick(listView, magicHeader, 0, 0);
			}
		});
	}

	public void setOnItemClickListener(OnItemClickListener listener) {
		this.listener = listener;
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		super.onLayout(changed, l, t, r, b);
		if (isDragging) {
			layoutDraggedView();
		} else {
			draggedView.layout(0, 0, 0, 0);
		}
	}

	/**
	 * Sets the adapter that handles all the magic that happens in this view.
	 * Without this adapter, this view is not fully operational.
	 */
	public void setAdapter(MagicListAdapter<?> magicListAdapter) {
		listView.setAdapter(magicListAdapter);
		this.magicListAdapter = magicListAdapter;
		magicListAdapter.registerDataSetObserver(new MagicDataSetObserver());
		magicListAdapter.notifyDataSetChanged();
	}

	@Override
	public boolean onDragEvent(DragEvent event) {
		if (event.getAction() == DragEvent.ACTION_DRAG_LOCATION) {
			return magicListAdapter.onDragEventLocation(event, getPosition((int) event.getY(), true));
		}
		return magicListAdapter.onDragEvent(event);
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent event) {
		if (!listView.scrolling && !isDragging && swipeHelper.onInterceptTouchEvent(event)) {
			return true;
		}
		if (isDragEvent(event)) {
			final int y = (int) event.getY();
			int position = getPosition(y, true);
			if (position < 0) {
				// If we get an invalid position we just use the last position.
				position = lastPosition;
			}
			if (position >= 0) { // -1 would be an invalid value.
				lastPosition = position;
				switch (event.getAction()) {
					case MotionEvent.ACTION_DOWN:
						startDragging(y, position);
						return true;
				}
			}
		}
		return false;
	}

	/**
	 * This implementation overrides the default behavior if the dragging
	 * handles are touched and initiates dragging and removing by swiping.
	 */
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (!listView.scrolling && !isDragging && swipeHelper.onTouchEvent(event)) {
			return true;
		}
		// We intercept this touch event and handle the dragging if it is a drag
		// event.
		if (isDragEvent(event)) {
			final int y = (int) event.getY();
			int position = getPosition(y, true);
			if (position < 0) {
				// If we get an invalid position we just use the last position.
				position = lastPosition;
			}
			if (position >= 0) { // -1 would be an invalid value.
				lastPosition = position;
				switch (event.getAction()) {
					case MotionEvent.ACTION_DOWN:
						startDragging(y, position);
						break;
					case MotionEvent.ACTION_MOVE:
						draggedViewY = y - draggedYOffset;
						layoutDraggedView();
						// We're in the process of dragging
						magicListAdapter.continueDragging(position);
						// If we're towards the top or the bottom of the screen
						// while dragging, we need to scroll.
						// TODO: Improve this, why does it work with only 0
						// remove magic numbers.
						if (y < 150 && y < firstYCoordinate - 50) {
							listView.awakenScrollBars();
							listView.setSelectionFromTop(0, listView
									.getChildAt(0).getTop() + 10);
						} else if (y > this.getHeight() - 100
								&& y > firstYCoordinate + 50) {
							listView.awakenScrollBars();
							listView.setSelectionFromTop(0, listView
									.getChildAt(0).getTop() - 10);
						}
						break;
					default: // CANCEL	
						// Dragging is over.
						magicListAdapter.stopDragging();
						isDragging = false;
						break;
				}
			}
		}
		// We always return true as we need to receive all the touch events.
		return true;
	}

	private void startDragging(int y, int position) {
		// We need an item in the queue so dragging makes sense
		// "> 0" because the header is not counted in this getCount
		if (!isDragging && magicListAdapter.getCount() > 0) {
			draggedYOffset = getYOffset(y, position);
			isDragging = true;
			magicListAdapter.startDragging(position);
			updateDraggedView();
			firstYCoordinate = y;
		}
	}

	// TODO: magic number.
	private boolean isDragEvent(MotionEvent event) {
		return magicListAdapter != null && event.getX() > getWidth() - itemHeight && event.getAction() ==
				MotionEvent.ACTION_DOWN || isDragging;
	}

	private int getYOffset(int y, int position) {
		if (position == 0) {
			return y;
		}
		// TODO: explain this and verify that it always works
		return y - listView.getChildAt(position - 1 - listView.getFirstVisiblePosition())
				.getTop() - listView.getTop();
	}

	private void updateDraggedView() {
		draggedViewContent = magicListAdapter
				.getDraggedView(draggedViewContent);
		draggedView.removeAllViews();
		// TODO: we had once a nullpointer here
		draggedView.addView(draggedViewContent);
	}

	private void layoutDraggedView() {
		draggedView.layout(0, draggedViewY, this.getMeasuredWidth(), draggedViewY + itemHeight);
		draggedViewContent.layout(0, 0, this.getMeasuredWidth(), itemHeight);
	}

	// Returns the position of the element which is y pixels from the top.
	// 0 is the head element, from 1 on the elements are in the list.
	int getPosition(int y, boolean deduceHeader) {
		int position = 0;
		nowPlayingView.getHitRect(rect);
		// If the point is the rect, we leave the position at 0.
		if (!rect.contains(0, y) || !deduceHeader) {
			int realY = y;
			if (deduceHeader) {
				realY -= nowPlayingView.getBottom();
			}
			position = listView.pointToPosition(0, realY);
			if (position >= 0) {
				// If it's a valid position, we increase it by one, since the
				// list starts at 1.
				position++;
			}
		}
		return position;
	}

	// We need our own DataSetObserver so we can update the header.
	private class MagicDataSetObserver extends DataSetObserver {

		@Override
		public void onChanged() {
			if (magicListAdapter != null) {
				View view = null;
				if (magicHeader.getChildCount() > 0) {
					view = magicHeader.getChildAt(0);
				}
				if (magicListAdapter.hasHeader()) {
					View newView = magicListAdapter.getHeaderView(view);
					magicHeader.removeAllViews();
					magicHeader.addView(newView);
				} else {
					magicHeader.removeAllViews();
				}
			}
		}
	}

	@Override
	public View getChildAtPosition(MotionEvent event) {
		int position;
		final int y = (int) event.getY();
		final int headerHeight = nowPlayingView.getBottom();
		if (y < headerHeight) {
			if (y < itemHeight) {
				return magicHeader.getChildAt(0);
			}
		} else {
			position = listView.pointToPosition((int) event.getX(), y - headerHeight);
			for (int i = 0; i < listView.getChildCount(); i++) {
				View view = listView.getChildAt(i);
				if (view instanceof QueueItem) {
					QueueItem item = (QueueItem) view;
					if (item.position == position + 1) {
						return view;
					}
				}
			}
		}
		return null;
	}

	@Override
	public View getChildContentView(View v) {
		return v;
	}

	@Override
	public boolean canChildBeDismissed(View v) {
		return true;
	}

	@Override
	public void onBeginDrag(View v) {
		// We do this so the underlying ScrollView knows that it won't get
		// the chance to intercept events anymore
		requestDisallowInterceptTouchEvent(true);
		v.setActivated(true);
	}

	@Override
	public void onChildDismissed(View v) {
		v.setTag(Boolean.TRUE);
		magicListAdapter.removeQueueItem((QueueItem) v);
	}

	@Override
	public void onDragCancelled(View v) {
		v.setActivated(false);
	}

	// The list view which displays elements 1 to n.
	private class MagicListView extends ListView {

		protected boolean scrolling = false;

		public MagicListView(Context context) {
			super(context);

			setOnScrollListener(new OnScrollListener() {

				@Override
				public void onScrollStateChanged(AbsListView view, int scrollState) {
					if (scrollState == OnScrollListener.SCROLL_STATE_IDLE) {
						scrolling = false;
					} else {
						scrolling = true;
					}
				}

				@Override
				public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
						int totalItemCount) {
				}
			});
		}

		/**
		 * This implementation doesn't handle the touches where the MagicView
		 * needs to do the dragging.
		 */
		@Override
		public boolean onTouchEvent(MotionEvent event) {
			final int y = (int) event.getY();
			// We don't handle drag events.
			if (isDragEvent(event)) {
				event.setLocation(event.getX(), event.getY() + nowPlayingView.getBottom());
				MagicView.this.onTouchEvent(event);
				return true;
			}
			lastY = y;
			return super.onTouchEvent(event);
		}

		/**
		 * Made this public (from protected) so it can be used in the MagicView.
		 */
		@Override
		public boolean awakenScrollBars() {
			return super.awakenScrollBars();
		}
	}
}
