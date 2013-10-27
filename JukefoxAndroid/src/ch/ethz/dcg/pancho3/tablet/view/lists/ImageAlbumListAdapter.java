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
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import ch.ethz.dcg.jukefox.model.collection.MapAlbum;
import ch.ethz.dcg.pancho3.tablet.view.ViewFactory;

public class ImageAlbumListAdapter extends ArrayAdapter<MapAlbum> {

	private View headerItemCache = null;
	private final ViewFactory viewFactory;

	public ImageAlbumListAdapter(Context context, ViewFactory viewFactory) {
		super(context, 0);
		this.viewFactory = viewFactory;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if (position == 0) {
			if (headerItemCache == null) {
				headerItemCache = viewFactory.getAlbumImageView(getItem(0), null, parent);
			}
			return headerItemCache;
		}
		return viewFactory.getAlbumImageView(getItem(position), convertView, parent);
	}

	@Override
	public void clear() {
		headerItemCache = null;
		super.clear();
	}

	@Override
	public long getItemId(int position) {
		return getItem(position).getId();
	}

	@Override
	public boolean hasStableIds() {
		return true;
	}

	@Override
	public MapAlbum getItem(int position) {
		return super.getItem(position);
	}

	@Override
	public int getItemViewType(int position) {
		if (position == 0) {
			return IGNORE_ITEM_VIEW_TYPE;
		}
		return 0;
	}

	@Override
	public int getViewTypeCount() {
		return 1;
	}
}
