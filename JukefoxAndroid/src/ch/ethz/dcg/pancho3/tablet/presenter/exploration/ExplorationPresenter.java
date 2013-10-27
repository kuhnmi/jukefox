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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import ch.ethz.dcg.jukefox.commons.utils.Pair;
import ch.ethz.dcg.jukefox.model.collection.AllAlbumsRepresentative;
import ch.ethz.dcg.jukefox.model.collection.AllRelatedAlbumsRepresentative;
import ch.ethz.dcg.jukefox.model.collection.BaseArtist;
import ch.ethz.dcg.jukefox.model.collection.BaseTag;
import ch.ethz.dcg.jukefox.model.collection.CompleteTag;
import ch.ethz.dcg.jukefox.model.collection.ListAlbum;
import ch.ethz.dcg.jukefox.model.collection.MapAlbum;
import ch.ethz.dcg.pancho3.tablet.model.DataFetcher;
import ch.ethz.dcg.pancho3.tablet.model.DataFetcher.OnDataFetchedListener;
import ch.ethz.dcg.pancho3.tablet.presenter.TabletPresenter;

/**
 * Presenter for the exploration selection view.
 */
public class ExplorationPresenter {

	// Reference to the main presenter to display overlays.
	private final TabletPresenter tabletPresenter;
	// From here we get our data.
	private final DataFetcher dataFetcher;
	// Decides how many values we select from a list of weighted values.
	private final ValueSelector valueSelector;

	// We notify this listener once we've loaded data.
	private IExplorationView view;

	private PendingCommand pendingCommand;

	/**
	 * An interface to the exploration view.
	 */
	public static interface IExplorationView {

		/**
		 * Tells the view to display info about a new artist.
		 */
		IExplorationViewArtist exploreArtist(BaseArtist artist);

		IExplorationViewTag exploreTag(BaseTag tag);

		IExplorationViewAllAlbums exploreAllAlbums();
	}

	public static interface IExplorationViewArtist {

		/**
		 * Tells the view to display new tags.
		 */
		void displayTags(List<Pair<CompleteTag, Float>> tags);

		/**
		 * Tells the view to display new related albums. TODO comment
		 */
		void displayRelatedAlbums(List<? extends MapAlbum> albums);

		void displayAlbums(List<? extends MapAlbum> albums);
	}

	public static interface IExplorationViewTag {

		/**
		 * Tells the view to display new tags.
		 */
		void displayTags(List<Pair<CompleteTag, Float>> tags);

		/**
		 * Tells the view to display new related albums. TODO comment
		 */
		void displayRelatedAlbums(List<? extends MapAlbum> albums);
	}

	public static interface IExplorationViewAllAlbums {

		void displayAlbums(List<? extends MapAlbum> albums);
	}

	private static interface PendingCommand {

		void execute();
	}

	private class PendingExploreAllAlbums implements PendingCommand {

		@Override
		public void execute() {
			exploreAllAlbums();
		}
	}

	private class PendingExploreArtist implements PendingCommand {

		private final BaseArtist artist;

		public PendingExploreArtist(BaseArtist artist) {
			this.artist = artist;
		}

		@Override
		public void execute() {
			exploreArtist(artist);
		}
	}

	private class PendingExploreTag implements PendingCommand {

		private final BaseTag tag;

		public PendingExploreTag(BaseTag tag) {
			this.tag = tag;
		}

		@Override
		public void execute() {
			exploreTag(tag);
		}
	}

	/**
	 * The constructor with the needed references.
	 */
	public ExplorationPresenter(TabletPresenter tabletPresenter, DataFetcher dataFetcher,
			ValueSelector valueSelector) {
		this.tabletPresenter = tabletPresenter;
		this.dataFetcher = dataFetcher;
		this.valueSelector = valueSelector;
	}

	/**
	 * The presenter needs this reference to be functional. If the presenter
	 * gets a command to explore an artist before this reference is set it will
	 * execute the command once the view is set and initialized.
	 */
	public void setExplorationView(IExplorationView view) {
		this.view = view;
	}

	/**
	 * Called when the user wants to explore an artist.
	 */
	public void exploreArtistMaybe(BaseArtist artist) {
		tabletPresenter.exploreArtistMaybe(artist);
	}

	/**
	 * Called when the user wants to explore a tag.
	 */
	public void exploreTagMaybe(BaseTag tag) {
		tabletPresenter.exploreTagMaybe(tag);
	}

	public void exploreArtist(final BaseArtist artist) {
		if (view != null) {
			final IExplorationViewArtist artistView = view.exploreArtist(artist);
			dataFetcher.fetchAlbumsOfArtist(artist, new OnDataFetchedListener<List<MapAlbum>>() {

				@Override
				public void onDataFetched(List<MapAlbum> albums) {
					if (albums.size() > 1) {
						ArrayList<MapAlbum> albumsWithHeader = new ArrayList<MapAlbum>();
						albumsWithHeader.add(new AllAlbumsRepresentative(artist));
						albumsWithHeader.addAll(albums);
						artistView.displayAlbums(albumsWithHeader);
					} else {
						artistView.displayAlbums(albums);
					}
				}
			});
			dataFetcher.fetchRelatedAlbums2(artist,
					new OnDataFetchedListener<List<MapAlbum>>() {

						@Override
						public void onDataFetched(List<MapAlbum> albums) {
							ArrayList<MapAlbum> albumsWithHeader = new ArrayList<MapAlbum>();
							// TODO: we should maybe also add the albums of the artist to the header.
							albumsWithHeader.add(new AllRelatedAlbumsRepresentative(albums));
							albumsWithHeader.addAll(albums);
							artistView.displayRelatedAlbums(albumsWithHeader);
						}
					});
			dataFetcher.fetchTagsForArtist(artist,
					new OnDataFetchedListener<List<Pair<CompleteTag, Float>>>() {

						@Override
						public void onDataFetched(final List<Pair<CompleteTag, Float>> data) {
							List<Float> values = new ArrayList<Float>();
							for (Pair<CompleteTag, Float> pair : data) {
								values.add(pair.second);
							}
							int numValues = valueSelector.getNumberOfValuesToSelect(values, 0, 20);

							List<Pair<CompleteTag, Float>> goodTags =
									new ArrayList<Pair<CompleteTag, Float>>();
							for (int i = 0; i < numValues; i++) {
								goodTags.add(data.get(i));
							}
							Collections.sort(goodTags, new Comparator<Pair<CompleteTag, Float>>() {

								@Override
								public int compare(Pair<CompleteTag, Float> pair1,
										Pair<CompleteTag, Float> pair2) {
									return pair1.first.getName().compareToIgnoreCase(
											pair2.first.getName());
								}
							});
							artistView.displayTags(goodTags);
						}
					});
		} else {
			pendingCommand = new PendingExploreArtist(artist);
		}
	}

	public void exploreTag(BaseTag tag) {
		if (view != null) {
			final IExplorationViewTag tagView = view.exploreTag(tag);
			dataFetcher.fetchAlbumsForTag(tag, new OnDataFetchedListener<List<MapAlbum>>() {

				@Override
				public void onDataFetched(List<MapAlbum> albums) {
					ArrayList<MapAlbum> albumsWithHeader = new ArrayList<MapAlbum>();
					albumsWithHeader.add(new AllRelatedAlbumsRepresentative(albums));
					albumsWithHeader.addAll(albums);
					tagView.displayRelatedAlbums(albumsWithHeader);
				}
			});
			dataFetcher.fetchRelatedTagsForTag(tag,
					new OnDataFetchedListener<List<Pair<CompleteTag, Float>>>() {

						@Override
						public void onDataFetched(List<Pair<CompleteTag, Float>> data) {
							tagView.displayTags(data);
						}
					});
		} else {
			pendingCommand = new PendingExploreTag(tag);
		}
	}

	public void exploreAllAlbums() {
		if (view != null) {
			final IExplorationViewAllAlbums allAlbumsView = view.exploreAllAlbums();
			dataFetcher.fetchAllAlbums(new OnDataFetchedListener<List<MapAlbum>>() {

				@Override
				public void onDataFetched(List<MapAlbum> albums) {
					ArrayList<MapAlbum> albumsWithHeader = new ArrayList<MapAlbum>();
					//albumsWithHeader.add(new AllSongsRepresentative());
					//albumsWithHeader.add(new RecentSongsRepresentative());
					albumsWithHeader.addAll(albums);
					allAlbumsView.displayAlbums(albumsWithHeader);
				}
			});
		} else {
			pendingCommand = new PendingExploreAllAlbums();
		}
	}

	/**
	 * Called when an album in the view has been clicked to display an overlay.
	 */
	public void onAlbumClick(ListAlbum album, boolean albumOfCurrentArtist) {
		tabletPresenter.displayOverlay(album, true, !albumOfCurrentArtist);
	}

	/**
	 * Signals that the view has finished initialization.
	 */
	public void viewFinishedInit() {
		if (pendingCommand != null) {
			pendingCommand.execute();
			pendingCommand = null;
		}
	}

	public void onArtistSelected(BaseArtist artist) {
		exploreArtistMaybe(artist);
	}

	public void displayArtistChooser() {
		tabletPresenter.displayArtistChooser();
	}
}
