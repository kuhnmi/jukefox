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
import ch.ethz.dcg.jukefox.model.collection.MapAlbum;
import ch.ethz.dcg.pancho3.tablet.interfaces.AlbumAdapter;
import ch.ethz.dcg.pancho3.tablet.presenter.exploration.ExplorationPresenter.IExplorationViewAllAlbums;
import ch.ethz.dcg.pancho3.tablet.view.ViewFactory;
import ch.ethz.dcg.pancho3.tablet.widget.PinnedHeaderListView;

public class AlbumGridAdapter extends GroupedAdapter implements IExplorationViewAllAlbums,
		AlbumAdapter {

	private List<? extends MapAlbum> albums;
	private SectionManager sectionManager = new SectionManager();
	private ArrayList<Integer> groupCounts = new ArrayList<Integer>();
	private ArrayList<Integer> groupStartPosition = new ArrayList<Integer>();
	private final ViewFactory viewFactory;
	private OnItemClickListener onItemClickListener;
	private OnItemLongClickListener onItemLongClickListener;

	public AlbumGridAdapter(Context context, PinnedHeaderListView boundList,
			ViewFactory viewFactory) {
		super(context, boundList);
		this.viewFactory = viewFactory;
	}

	@Override
	public void displayAlbums(List<? extends MapAlbum> albums) {
		this.albums = albums;
		sectionManager.createSections(albums);
		groupCounts.clear();
		int total = 0;
		for (int groupCount : sectionManager.getGroupCounts()) {
			int newGroupCount = (int) Math.ceil(groupCount / 3.0);
			groupCounts.add(newGroupCount);
			groupStartPosition.add(total);
			total += groupCount;
		}
		initAdapter();
		notifyDataSetChanged();
	}

	@Override
	protected List<Integer> getGroupCounts() {
		return groupCounts;
	}

	@Override
	protected String getGroupTitle(int groupNumber) {
		return sectionManager.getGroupTitle(groupNumber);
	}

	@Override
	protected View getView(int groupNumber, int positionWithinGroup, View convertView,
			int totalDataIndex) {
		int startIndex = groupStartPosition.get(groupNumber) + 3 * positionWithinGroup;
		int endIndex = Math.min(startIndex + 3, albums.size());
		if (groupNumber < groupStartPosition.size() - 1) {
			endIndex = Math.min(endIndex, groupStartPosition.get(groupNumber + 1));
		}

		return viewFactory.getAlbumRow(albums, startIndex, endIndex,
				onItemClickListener, onItemLongClickListener, convertView, 0);
	}

	@Override
	public MapAlbum getItem(int position) {
		return albums.get(position);
	}

	public void setOnItemClickListener(OnItemClickListener listener) {
		this.onItemClickListener = listener;
	}

	public void setOnItemLongClickListener(OnItemLongClickListener listener) {
		this.onItemLongClickListener = listener;
	}
}
