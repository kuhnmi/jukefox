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

import java.util.List;

import android.content.Context;
import android.database.DataSetObserver;
import android.view.DragEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.TextView;
import ch.ethz.dcg.pancho3.R;
import ch.ethz.dcg.pancho3.tablet.view.DragManager;
import ch.ethz.dcg.pancho3.tablet.view.DragManager.DragDataContainer;

/**
 * A list adapter for the MagicView.
 * 
 * @author Yannick Stucki (yannickstucki@gmail.com)
 */
public class MagicListAdapter<T> implements ListAdapter {

	// Listener needed for a callback to add a new item to this adapter.
	public static interface NewItemListener {

		void onRequestNewItem();
	}

	// Interface for the inner adapter.
	public static interface MagicListInnerAdapter<T> extends ListAdapter {

		/**
		 * Appends an item at the end.
		 */
		void appendItem(T object);

		/**
		 * Removes an item at the specified position.
		 */
		void removeItem(int position);

		/**
		 * Called when the underlying data changed.
		 */
		void notifyDataSetChanged();

		/**
		 * Moves an item from the startPosition to the endPosition.
		 */
		void moveItem(int startPosition, int endPosition);

		/**
		 * Moves an item from the startPosition to the endPosition.
		 */
		void insertItemsAndRemoveLast(List<T> items, int insertPosition);

		/**
		 * Returns the data of the item displayed at the specified position.
		 */
		T getItem(int position);

		/**
		 * Returns the view which should be displayed at the specified position,
		 * but for an element which is currently being dragged.
		 */
		View getDraggingView(int position, View convertView, ViewGroup parent);
	}

	private static final int ITEM_HEIGHT = 64;

	// The inner adapter holds the data for all the items.
	// The item at position 0 represents the item in the header, while
	// the following items represent the items in the list.

	// This class (MagicListAdapter)
	// reroutes the calls from the list to only give it access to item
	// 1 to n.
	private final MagicListInnerAdapter<T> innerAdapter;

	// Dragging is the process of reordering items or swiping them to be
	// removed.
	// While we're dragging an item, this is true.
	private boolean isDragging = false;

	private boolean isRemoving = false;

	private int removePosition;

	// The position when the drag started if isDragging.
	private int dragStartPosition;

	// The position where the drag currently is if isDragging.
	private int dragCurrentPosition;

	private final TextView emptyViewForHeader;
	private final TextView emptyViewForList;

	// A listener which reacts to the new item request
	private final NewItemListener newItemListener;

	// TODO: can this be calculated in a more elegant way?
	private final int itemHeight;

	private final DragManager dragManager;

	/**
	 * The constructor needs a MagicListInnerAdapter.
	 */
	public MagicListAdapter(Context context,
			MagicListInnerAdapter<T> innerAdapter,
			NewItemListener newItemListener,
			DragManager dragManager) {
		this.innerAdapter = innerAdapter;
		this.newItemListener = newItemListener;
		this.dragManager = dragManager;
		emptyViewForHeader = new TextView(context);
		emptyViewForHeader.setBackgroundResource(R.drawable.d170_item_highlight);
		emptyViewForList = new TextView(context);
		emptyViewForList.setBackgroundResource(R.drawable.d170_item_highlight);
		itemHeight = Math.round(context.getResources().getDisplayMetrics().density * ITEM_HEIGHT);
	}

	/**
	 * Adds the specified item to the end of the list.
	 */
	public void add(T item) {
		innerAdapter.appendItem(item);
		innerAdapter.notifyDataSetChanged();
	}

	/**
	 * Called when the dragging starts at the specified position.
	 */
	public void startDragging(int position) {
		dragStartPosition = position;
		dragCurrentPosition = position;
		isDragging = true;
		innerAdapter.notifyDataSetChanged();
	}

	public void startRemoving(int position) {
		removePosition = position;
		isRemoving = true;
		innerAdapter.notifyDataSetChanged();
	}

	public void stopRemoving() {
		isRemoving = false;
		innerAdapter.notifyDataSetChanged();
	}

	/**
	 * Called when the touch events come in and the view is in drag mode.
	 * 
	 * @param position
	 *            the current position where the dragged element is.
	 */
	public void continueDragging(int position) {
		if (dragCurrentPosition != position) {
			// Only update if the current position changed. TODO: maybe check
			// this in the MagicView?
			dragCurrentPosition = position;
			innerAdapter.notifyDataSetChanged();
		}
	}

	/**
	 * Called when dragging stops.
	 */
	public void stopDragging() {
		// TODO: maybe we need some write lock here? But probably not needed.
		isDragging = false;
		innerAdapter.moveItem(dragStartPosition, dragCurrentPosition);
		innerAdapter.notifyDataSetChanged();
	}

	public void stopDraggingAfterInsert() {
		isDragging = false;
		innerAdapter.insertItemsAndRemoveLast(lastDragDataContainer.getData(), dragCurrentPosition);
		innerAdapter.notifyDataSetChanged();
	}

	// Remove the element at the specified position.
	// (dragging mode not included and will be turned off).
	public void remove(int position) {
		isDragging = false;
		innerAdapter.removeItem(position);
		innerAdapter.notifyDataSetChanged();
	}

	public void removeQueueItem(QueueItem item) {
		item.dismiss();
		remove(item.position);
	}

	/**
	 * Returns a view representing the header item. Possibly reuses convertView.
	 */
	public View getHeaderView(View convertView) {
		if (isDragging && dragCurrentPosition == 0 ||
				isRemoving && removePosition == 0) {
			emptyViewForHeader.setHeight(itemHeight);
			return emptyViewForHeader;
		}
		if (convertView == emptyViewForHeader) {
			convertView = null;
		}
		final int position;
		if (isDragging && dragStartPosition == 0) {
			// The element 0 has been dragged away, and element 1 is now on top.
			position = 1;
		} else {
			// The header view usually holds the object at position 0.
			position = 0;
		}
		View item = innerAdapter.getView(position, convertView, null);
		return item;
	}

	public View getDraggedView(View convertView) {
		if (isRemoving) {
			return innerAdapter.getDraggingView(removePosition, convertView, null);
		}
		return innerAdapter.getDraggingView(dragStartPosition, convertView, null);
	}

	/**
	 * The underlying data changed and the view needs to be redrawn.
	 */
	public void notifyDataSetChanged() {
		innerAdapter.notifyDataSetChanged();
	}

	public void requestNewItem() {
		newItemListener.onRequestNewItem();
	}

	/**
	 * Returns the item associated with the header of this view.
	 */
	public T getHeaderItem() {
		return innerAdapter.getItem(0);
	}

	// Below here are the methods which override the ListAdapter interface.
	// This methods don't include the head item, but only the ones represented
	// in the list. This methods are meant to be used by the ListView to
	// get the proper information about all the items that are actually in the
	// list. Note that positions are always mapped to other positions (see
	// #mapPosition).

	@Override
	public boolean areAllItemsEnabled() {
		return innerAdapter.areAllItemsEnabled();
	}

	@Override
	public boolean isEnabled(int position) {
		return innerAdapter.isEnabled(mapPosition(position));
	}

	/**
	 * The count is one less than in the innerAdapter, since the list doesn't
	 * see the top item.
	 */
	@Override
	public int getCount() {
		int count = innerAdapter.getCount() - 1;
		if (count < 0) {
			return 0;
		}
		return count;
	}

	@Override
	public T getItem(int position) {
		return innerAdapter.getItem(mapPosition(position));
	}

	@Override
	public long getItemId(int position) {
		return innerAdapter.getItemId(mapPosition(position));
	}

	@Override
	public int getItemViewType(int position) {
		if (isDragging && mapPosition(position) == dragStartPosition ||
				isRemoving && position + 1 == removePosition) {
			return ListAdapter.IGNORE_ITEM_VIEW_TYPE;
		}
		return innerAdapter.getItemViewType(mapPosition(position));
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if (isDragging && dragCurrentPosition == position + 1 ||
				isRemoving && removePosition == position + 1) {
			emptyViewForList.setHeight(itemHeight);
			return emptyViewForList;
		}
		return innerAdapter.getView(mapPosition(position), convertView, parent);
	}

	@Override
	public int getViewTypeCount() {
		return innerAdapter.getViewTypeCount();
	}

	@Override
	public boolean hasStableIds() {
		return innerAdapter.hasStableIds();
	}

	/**
	 * One item already means that the list is empty since this item will be the
	 * head.
	 */
	@Override
	public boolean isEmpty() {
		return innerAdapter.getCount() <= 1;
	}

	public boolean hasHeader() {
		return innerAdapter.getCount() > 0;
	}

	@Override
	public void registerDataSetObserver(DataSetObserver observer) {
		innerAdapter.registerDataSetObserver(observer);
	}

	@Override
	public void unregisterDataSetObserver(DataSetObserver observer) {
		innerAdapter.unregisterDataSetObserver(observer);
	}

	// Below here are private helper methods.

	// This method list positions to positions to use the inner adapter. This
	// method doesn't work for the head view, but only for the list
	// items.
	private int mapPosition(int position) {
		position++; // The +1 on everything is since the list starts at 1
					// because of the head.
		if (!isDragging/* && !innerAdapter.isWaitingForActionToFinish()*/) {
			return position; // When we're not in drag mode, there is no other
								// mapping going on.
		}
		if (position < dragCurrentPosition && position < dragStartPosition
				|| position > dragCurrentPosition
				&& position > dragStartPosition) {
			// If we're below or above all the dragging action, there's also no
			// further mapping going on.
			return position;
		}
		// If we're at the position where the dragged item currently is, it is
		// the item which was
		// at the position where the dragging started.
		if (position == dragCurrentPosition) {
			return dragStartPosition;
		}
		// Otherwise the item is shifted by one position, depending on whether
		// the dragging.
		// is going up or down.
		if (dragCurrentPosition < dragStartPosition) {
			return position - 1;
		} else {
			return position + 1;
		}
	}

	private DragDataContainer<T> lastDragDataContainer;

	// Experimental TODO: Clean this stuff up.
	public boolean onDragEvent(DragEvent event) {
		if (!dragManager.onDragEvent(event)) {
			switch (event.getAction()) {
				case DragEvent.ACTION_DRAG_ENTERED:
					DragDataContainer<T> data = (DragDataContainer<T>) event.getLocalState();
					if (data.isReady()) {
						lastDragDataContainer = data;
						add(data.getData().get(0));
						startDragging(innerAdapter.getCount() - 1);
					}
					break;
				case DragEvent.ACTION_DRAG_LOCATION:
					break;
				case DragEvent.ACTION_DRAG_EXITED:
					if (lastDragDataContainer != null) {
						remove(innerAdapter.getCount() - 1);
						stopDragging();
					}
					break;
				case DragEvent.ACTION_DROP:
					if (lastDragDataContainer != null) {
						stopDraggingAfterInsert();
					}
					break;
			}
		}
		return true;
	}

	public boolean onDragEventLocation(DragEvent event, int position) {
		if (position >= 0) {
			continueDragging(position);
		}
		return true;
	}
}
