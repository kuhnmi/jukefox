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
package ch.ethz.dcg.pancho3.controller.eventhandlers;

import java.util.List;

import android.content.Intent;
import ch.ethz.dcg.jukefox.commons.utils.JoinableThread;
import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.jukefox.controller.Controller;
import ch.ethz.dcg.jukefox.model.collection.BaseAlbum;
import ch.ethz.dcg.jukefox.model.collection.BaseArtist;
import ch.ethz.dcg.jukefox.model.collection.BaseSong;
import ch.ethz.dcg.jukefox.model.collection.Genre;
import ch.ethz.dcg.jukefox.model.collection.PlaylistSong;
import ch.ethz.dcg.jukefox.model.collection.PlaylistSong.SongSource;
import ch.ethz.dcg.jukefox.model.commons.PlaylistPositionOutOfRangeException;
import ch.ethz.dcg.pancho3.model.collection.ParcelableGenre;
import ch.ethz.dcg.pancho3.view.commons.JukefoxActivity;
import ch.ethz.dcg.pancho3.view.tabs.PlayerActivity;
import ch.ethz.dcg.pancho3.view.tabs.lists.AlbumList;
import ch.ethz.dcg.pancho3.view.tabs.lists.ArtistList;
import ch.ethz.dcg.pancho3.view.tabs.lists.SongList;

public class GenreListMenuEventListener extends BaseJukefoxEventListener {

	public GenreListMenuEventListener(Controller controller, JukefoxActivity activity) {
		super(controller, activity);
	}

	public void onPlayGenreButtonClicked(final Genre genre) {
		JoinableThread t = new JoinableThread(new Runnable() {

			@Override
			public void run() {
				controller.doHapticFeedback();
				controller.getPlayerController().stop();
				controller.getPlayerController().clearPlaylist();
				List<BaseSong<BaseArtist, BaseAlbum>> songs = activity.getCollectionModel().getSongProvider()
						.getAllBaseSongs(
								genre);
				for (BaseSong<BaseArtist, BaseAlbum> song : songs) {
					controller.getPlayerController().appendSongAtEnd(
							new PlaylistSong<BaseArtist, BaseAlbum>(song, SongSource.MANUALLY_SELECTED));
				}
				try {
					controller.getPlayerController().playSongAtPosition(0);
				} catch (PlaylistPositionOutOfRangeException e) {
					Log.w(TAG, e);
				}
			}
		});
		t.start();
		Intent intent = new Intent(activity.getApplicationContext(), PlayerActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		activity.startActivity(intent);
		activity.finish();
	}

	public void onShowGenreArtistsClicked(Genre genre) {
		controller.doHapticFeedback();
		Intent intent = new Intent(activity, ArtistList.class);
		intent.putExtra(Controller.INTENT_EXTRA_BASE_GENRE, new ParcelableGenre(genre));
		activity.startActivity(intent);
		activity.finish();
	}

	public void onShowGenreAlbumsClicked(Genre genre) {
		controller.doHapticFeedback();
		Intent intent = new Intent(activity, AlbumList.class);
		intent.putExtra(Controller.INTENT_EXTRA_BASE_GENRE, new ParcelableGenre(genre));
		activity.startActivity(intent);
		activity.finish();
	}

	public void onShowGenreSongsClicked(Genre genre) {
		controller.doHapticFeedback();
		Intent intent = new Intent(activity, SongList.class);
		intent.putExtra(Controller.INTENT_EXTRA_BASE_GENRE, new ParcelableGenre(genre));
		activity.startActivity(intent);
		activity.finish();
	}

}
