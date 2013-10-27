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

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.view.View;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import ch.ethz.dcg.jukefox.commons.utils.Pair;
import ch.ethz.dcg.jukefox.model.collection.CompleteTag;
import ch.ethz.dcg.jukefox.model.collection.MapAlbum;
import ch.ethz.dcg.pancho3.tablet.interfaces.AlbumAdapter;
import ch.ethz.dcg.pancho3.tablet.presenter.exploration.ExplorationPresenter.IExplorationViewArtist;
import ch.ethz.dcg.pancho3.tablet.view.ViewFactory;
import ch.ethz.dcg.pancho3.tablet.widget.PinnedHeaderListView;

public class ArtistGridAdapter extends GroupedAdapter implements IExplorationViewArtist,
		AlbumAdapter {

	private List<? extends MapAlbum> albums;
	private List<? extends MapAlbum> relatedAlbums;
	private ArrayList<Integer> groupCounts = new ArrayList<Integer>();
	private ArrayList<String> groupTitles = new ArrayList<String>();
	private final ViewFactory viewFactory;
	private OnItemClickListener onItemClickListener;
	private OnItemLongClickListener onItemLongClickListener;

	public ArtistGridAdapter(Context context, PinnedHeaderListView boundList,
			ViewFactory viewFactory) {
		super(context, boundList);
		this.viewFactory = viewFactory;
	}

	@Override
	public void displayAlbums(List<? extends MapAlbum> albums) {
		this.albums = albums;
		init();
	}

	@Override
	public void displayRelatedAlbums(List<? extends MapAlbum> relatedAlbum) {
		this.relatedAlbums = relatedAlbum;
		init();
	}

	private void init() {
		if (albums != null && relatedAlbums != null) {
			groupCounts.clear();
			groupTitles.clear();
			// Adding albums.
			groupCounts.add(getNumberOfRows(albums.size()));
			groupTitles.add("Albums");
			// Adding related albums.
			groupCounts.add(getNumberOfRows(relatedAlbums.size()));
			groupTitles.add("Related Albums");
			initAdapter();
			notifyDataSetChanged();
		}
	}

	private int getNumberOfRows(int items) {
		return (int) Math.ceil(items / 3.0);
	}

	@Override
	protected List<Integer> getGroupCounts() {
		return groupCounts;
	}

	@Override
	protected String getGroupTitle(int groupNumber) {
		return groupTitles.get(groupNumber);
	}

	@Override
	protected View getView(int groupNumber, int positionWithinGroup, View convertView,
			int totalDataIndex) {
		List<? extends MapAlbum> selectedList = groupNumber == 0 ? albums : relatedAlbums;
		int positionOffset = groupNumber == 0 ? 0 : albums.size();
		int startIndex = 3 * positionWithinGroup;
		int endIndex = Math.min(startIndex + 3, selectedList.size());
		return viewFactory.getAlbumRow(selectedList, startIndex, endIndex,
				onItemClickListener, onItemLongClickListener, convertView, positionOffset);
	}

	@Override
	public MapAlbum getItem(int position) {
		if (position < albums.size()) {
			return albums.get(position);
		}
		return relatedAlbums.get(position - albums.size());
	}

	public void setOnItemClickListener(OnItemClickListener listener) {
		this.onItemClickListener = listener;
	}

	public void setOnItemLongClickListener(OnItemLongClickListener listener) {
		this.onItemLongClickListener = listener;
	}

	@Override
	public void displayTags(List<Pair<CompleteTag, Float>> tags) {
		// TODO Auto-generated method stub

	}
}
