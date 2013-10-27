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
package ch.ethz.dcg.pancho3.tablet.presenter.exploration;

import java.util.List;

import android.view.ActionMode;
import android.view.ActionMode.Callback;
import android.view.Menu;
import android.view.MenuItem;
import ch.ethz.dcg.jukefox.model.collection.BaseArtist;
import ch.ethz.dcg.pancho3.R;
import ch.ethz.dcg.pancho3.tablet.model.DataFetcher;
import ch.ethz.dcg.pancho3.tablet.model.DataFetcher.OnDataFetchedListener;

/**
 * Presenter for the artist chooser view, which is list of artists together with
 * an action mode. Clicking an artist will hide the view and the action mode.
 */
public class ArtistChooserPresenter implements Callback {

	// The view displaying the list of the artist.
	private final IArtistChooserView view;
	// We tell this presenter which artist was chosen to be explored.
	private final ExplorationPresenter explorationPresenter;
	private final DataFetcher dataFetcher;
	// The action mode reference is kept so the action mode can be finished.
	private ActionMode actionMode;

	/**
	 * The constructor sets the references.
	 */
	public ArtistChooserPresenter(IArtistChooserView view,
			ExplorationPresenter explorationPresenter, DataFetcher dataFetcher) {
		this.view = view;
		this.explorationPresenter = explorationPresenter;
		this.dataFetcher = dataFetcher;
	}

	/**
	 * Interface to a view displaying the artists.
	 */
	public static interface IArtistChooserView {

		/**
		 * Tells the view to hide itself.
		 */
		void hide();

		void displayArtists(List<BaseArtist> artists);

		void startAnimation();
	}

	/**
	 * The action mode simply explains what the user should do.
	 */
	@Override
	public boolean onCreateActionMode(ActionMode mode, Menu menu) {
		this.actionMode = mode;
		mode.setTitle(R.string.actionbar_artist_selection_title);
		mode.setSubtitle("");
		return true;
	}

	/**
	 * If the action mode gets destroyed we also need to hide the view.
	 */
	@Override
	public void onDestroyActionMode(ActionMode mode) {
		view.hide();
	}

	/**
	 * If the view is hidden, we also need to destroy the action mode.
	 */
	public void onHideOverlay() {
		if (actionMode != null) {
			actionMode.finish();
		}
	}

	/**
	 * Forward the call to the exploration presenter and hide the view.
	 */
	public void onArtistSelected(BaseArtist artist) {
		explorationPresenter.onArtistSelected(artist);
		view.hide();
	}

	public void onViewFinishedInit() {
		view.startAnimation();
		dataFetcher.fetchAllArtists(new OnDataFetchedListener<List<BaseArtist>>() {

			@Override
			public void onDataFetched(List<BaseArtist> artists) {
				view.displayArtists(artists);
			}
		});
	}

	@Override
	public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
		return true;
	}

	@Override
	public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
		return true;
	}
}
