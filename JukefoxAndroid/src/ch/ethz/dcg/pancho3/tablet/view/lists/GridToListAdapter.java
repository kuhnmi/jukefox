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
package ch.ethz.dcg.pancho3.tablet.view.lists;

import android.content.Context;
import android.database.DataSetObserver;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListAdapter;
import ch.ethz.dcg.pancho3.tablet.widget.GridRow;

public class GridToListAdapter implements ListAdapter {

	private final ListAdapter innerAdapter;
	private final Context context;
	private OnItemClickListener onItemClickListener;
	private OnItemLongClickListener onItemLongClickListener;

	public GridToListAdapter(ListAdapter innerAdapter, Context context) {
		this.innerAdapter = innerAdapter;
		this.context = context;
	}

	@Override
	public int getCount() {
		return (int) Math.ceil(innerAdapter.getCount() / 3.0);
	}

	@Override
	public Object getItem(int position) {
		return innerAdapter.getItem(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public int getItemViewType(int position) {
		return 0;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		GridRow row = new GridRow(context);
		int startIndex = position * 3;
		int endIndex = Math.min(startIndex + 3, innerAdapter.getCount());
		Log.i("TEST", startIndex + "   " + endIndex);
		for (int i = startIndex; i < endIndex; i++) {
			View view = innerAdapter.getView(i, null, null);
			if (view.getParent() != null) {
				((ViewGroup) view.getParent()).removeView(view);
			}
			final int index = i;
			view.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					if (onItemClickListener != null) {
						onItemClickListener.onItemClick(null, v, index,
								innerAdapter.getItemId(index));
					}
				}
			});
			view.setOnLongClickListener(new OnLongClickListener() {

				@Override
				public boolean onLongClick(View v) {
					if (onItemLongClickListener != null) {
						onItemLongClickListener.onItemLongClick(null, v, index,
								innerAdapter.getItemId(index));
						return true;
					}
					return false;
				}
			});
			row.addView(view);

		}
		return row;
	}

	@Override
	public int getViewTypeCount() {
		return 1;
	}

	@Override
	public boolean hasStableIds() {
		return true;
	}

	@Override
	public boolean isEmpty() {
		return innerAdapter.isEmpty();
	}

	@Override
	public void registerDataSetObserver(DataSetObserver observer) {
		innerAdapter.registerDataSetObserver(observer);
	}

	@Override
	public void unregisterDataSetObserver(DataSetObserver observer) {
		innerAdapter.unregisterDataSetObserver(observer);
	}

	@Override
	public boolean areAllItemsEnabled() {
		return true;
	}

	@Override
	public boolean isEnabled(int position) {
		return true;
	}

	public void setOnItemClickListener(OnItemClickListener listener) {
		this.onItemClickListener = listener;
	}

	public void setOnItemLongClickListener(OnItemLongClickListener listener) {
		this.onItemLongClickListener = listener;
	}
}
