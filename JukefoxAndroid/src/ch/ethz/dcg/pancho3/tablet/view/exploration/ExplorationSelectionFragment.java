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
package ch.ethz.dcg.pancho3.tablet.view.exploration;

import android.app.Fragment;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import ch.ethz.dcg.jukefox.commons.utils.JoinableThread;
import ch.ethz.dcg.jukefox.model.collection.BaseArtist;
import ch.ethz.dcg.jukefox.model.collection.BaseTag;
import ch.ethz.dcg.pancho3.R;
import ch.ethz.dcg.pancho3.tablet.TabletFactory;
import ch.ethz.dcg.pancho3.tablet.TabletFactory.TabletFactoryGetter;
import ch.ethz.dcg.pancho3.tablet.presenter.exploration.ExplorationPresenter;
import ch.ethz.dcg.pancho3.tablet.presenter.exploration.ExplorationPresenter.IExplorationView;
import ch.ethz.dcg.pancho3.tablet.presenter.exploration.ExplorationPresenter.IExplorationViewAllAlbums;
import ch.ethz.dcg.pancho3.tablet.presenter.exploration.ExplorationPresenter.IExplorationViewArtist;
import ch.ethz.dcg.pancho3.tablet.presenter.exploration.ExplorationPresenter.IExplorationViewTag;
import ch.ethz.dcg.pancho3.tablet.view.lists.AlbumGridAdapter;
import ch.ethz.dcg.pancho3.tablet.view.lists.ArtistGridAdapter;
import ch.ethz.dcg.pancho3.tablet.widget.PinnedHeaderListView;

/**
 * Selection fragment for the exploration mode. This mode allows to explore
 * music by getting related albums, tags etc.
 */
public class ExplorationSelectionFragment extends Fragment implements IExplorationView {

	private View mainView;

	// The loading of the information and other logic is done here.
	private ExplorationPresenter presenter;
	private View selectArtistArea;
	private TabletFactory tabletFactory;

	private PinnedHeaderListView listView;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		if (mainView == null) {
			mainView = inflater.inflate(R.layout.tablet_exploration, null);
			selectArtistArea = mainView.findViewById(R.id.select_artist);
			selectArtistArea.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					presenter.displayArtistChooser();
				}
			});
			listView = (PinnedHeaderListView) mainView.findViewById(R.id.list);
			listView.setDivider(null);
		}

		new AsyncTask<Void, Void, Void>() {

			@Override
			protected Void doInBackground(Void... params) {
				TabletFactoryGetter tabletFactoryGetter = (TabletFactoryGetter) getActivity();
				// TODO: got null pointer below here
				while (!tabletFactoryGetter.isTabletFactoryReady()) {
					try {
						JoinableThread.sleep(200);
					} catch (InterruptedException e) {
					}
				}
				tabletFactory = tabletFactoryGetter.getTabletFactory();
				return null;
			}

			@Override
			protected void onPostExecute(Void result) {
				presenter = tabletFactory.getExplorationPresenter(ExplorationSelectionFragment.this);
				presenter.viewFinishedInit();
			}
		}.execute();
		return mainView;
	}

	@Override
	public void onResume() {
		super.onResume();
		InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(mainView.getWindowToken(), 0);
	}

	@Override
	public IExplorationViewArtist exploreArtist(final BaseArtist artist) {
		final ArtistGridAdapter adapter = new ArtistGridAdapter(getActivity(), listView,
				tabletFactory.getViewFactory());
		listView.setAdapter(adapter);
		listView.setPinnedHeaderAdapter(adapter);
		adapter.initUI();
		adapter.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				presenter.onAlbumClick(adapter.getItem(position), false);
			}
		});
		tabletFactory.getDragManager().registerAlbumViewDragging(adapter);
		return adapter;
	}

	@Override
	public IExplorationViewTag exploreTag(BaseTag tag) {
		return null;
	}

	@Override
	public IExplorationViewAllAlbums exploreAllAlbums() {
		final AlbumGridAdapter adapter = new AlbumGridAdapter(getActivity(), listView,
				tabletFactory.getViewFactory());
		listView.setAdapter(adapter);
		listView.setPinnedHeaderAdapter(adapter);
		adapter.initUI();
		adapter.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				presenter.onAlbumClick(adapter.getItem(position), false);
			}
		});
		tabletFactory.getDragManager().registerAlbumViewDragging(adapter);
		return adapter;
	}
}
