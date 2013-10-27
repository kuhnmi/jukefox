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

import java.util.HashMap;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import ch.ethz.dcg.pancho3.tablet.view.ViewFactory;

public class CachingImageAlbumListAdapter extends ImageAlbumListAdapter {

	private final ViewFactory viewFactory;
	private final HashMap<Integer, View> viewMap = new HashMap<Integer, View>();

	public CachingImageAlbumListAdapter(Context context, ViewFactory viewFactory) {
		super(context, viewFactory);
		this.viewFactory = viewFactory;
	}

	@Override
	public void clear() {
		viewMap.clear();
		super.clear();
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View view = viewMap.get(position);
		if (view == null) {
			view = viewFactory.getAlbumImageView(getItem(position), null, parent);
			viewMap.put(position, view);
		}
		return view;
	}

}
