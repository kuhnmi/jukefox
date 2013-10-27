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

import java.util.List;

import android.content.Context;
import android.view.View;
import ch.ethz.dcg.jukefox.model.collection.BaseArtist;
import ch.ethz.dcg.pancho3.tablet.view.ViewFactory;
import ch.ethz.dcg.pancho3.tablet.widget.PinnedHeaderListView;

/**
 * An adapter to display a list of all artists. It also provides a header view
 * which represents all artists.
 */
public class ArtistListAdapter extends GroupedAdapter {

	// Used to create all the views.
	private final ViewFactory viewFactory;
	private final SectionManager sectionManager = new SectionManager();
	private List<BaseArtist> artists;

	public ArtistListAdapter(Context context, ViewFactory viewFactory, PinnedHeaderListView list) {
		// We pass 0 as a resource ID: it doesn't matter since we create all the views by
		// using the view factory.
		super(context, list);
		this.viewFactory = viewFactory;
	}

	public void setArtists(List<BaseArtist> artists) {
		this.artists = artists;
		sectionManager.createSections(artists);
	}

	@Override
	public BaseArtist getItem(int position) {
		return artists.get(position);
	}

	@Override
	protected List<Integer> getGroupCounts() {
		return sectionManager.getGroupCounts();
	}

	@Override
	protected String getGroupTitle(int groupNumber) {
		return sectionManager.getGroupTitle(groupNumber);
	}

	@Override
	protected View getView(int groupNumber, int positionWithinGroup, View convertView,
			int totalDataIndex) {
		return viewFactory.getArtistView(artists.get(totalDataIndex), convertView, null);
	}
}
