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

import android.content.Intent;
import android.util.SparseBooleanArray;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.jukefox.controller.Controller;
import ch.ethz.dcg.jukefox.model.collection.BaseAlbum;
import ch.ethz.dcg.jukefox.model.collection.BaseArtist;
import ch.ethz.dcg.jukefox.model.collection.BaseSong;
import ch.ethz.dcg.jukefox.model.collection.PlaylistSong;
import ch.ethz.dcg.jukefox.model.collection.PlaylistSong.SongSource;
import ch.ethz.dcg.jukefox.model.commons.PlaylistPositionOutOfRangeException;
import ch.ethz.dcg.pancho3.R;
import ch.ethz.dcg.pancho3.model.collection.ParcelableSong;
import ch.ethz.dcg.pancho3.view.overlays.AlbumDetails;
import ch.ethz.dcg.pancho3.view.overlays.SongMenu;
import ch.ethz.dcg.pancho3.view.tabs.PlayerActivity;

public class AlbumDetailEventListener extends BaseJukefoxEventListener {

	public static final int NUMBER_COVER_HINT_THRESSHOLD = 1000;

	AlbumDetails albumDetails;

	public AlbumDetailEventListener(Controller controller, AlbumDetails activity) {
		super(controller, activity);

		this.albumDetails = activity;

	}

	public void playButtonClicked() {
		controller.doHapticFeedback();
		controller.getPlayerController().stop();
		controller.getPlayerController().clearPlaylist();
		appendSelectedSongs();
		try {
			controller.getPlayerController().playSongAtPosition(0);
		} catch (PlaylistPositionOutOfRangeException e) {
			Log.w(TAG, e);
		}
		Intent intent = new Intent(activity.getApplicationContext(), PlayerActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		activity.startActivity(intent);
		activity.finish();
	}

	public void playlistAppendButtonClicked() {
		controller.doHapticFeedback();
		appendSelectedSongs();
		controller.showToast(activity.getString(R.string.appended_songs));
	}

	@SuppressWarnings("unchecked")
	public void playlistInsertButtonClicked() {
		controller.doHapticFeedback();
		ListView songList = albumDetails.getSongList();
		SparseBooleanArray checked = songList.getCheckedItemPositions();
		for (int i = songList.getAdapter().getCount() - 1; i >= 0; i--) {
			if (checked.get(i)) {
				PlaylistSong<BaseArtist, BaseAlbum> song = new PlaylistSong<BaseArtist, BaseAlbum>(
						(BaseSong<BaseArtist, BaseAlbum>) songList.getItemAtPosition(i), SongSource.MANUALLY_SELECTED);
				controller.getPlayerController().insertSongAsNext(song);
			}
		}
		controller.showToast(activity.getString(R.string.inserted_songs));
	}

	public void selectAllButtonClicked() {
		controller.doHapticFeedback();
		ListView songList = albumDetails.getSongList();
		for (int i = 0; i < songList.getCount(); i++) {
			songList.setItemChecked(i, true);
		}
	}

	public void selectNoneButtonClicked() {
		controller.doHapticFeedback();
		ListView songList = albumDetails.getSongList();
		for (int i = 0; i < songList.getCount(); i++) {
			songList.setItemChecked(i, false);
		}
	}

	@SuppressWarnings("unchecked")
	private void appendSelectedSongs() {
		ListView songList = albumDetails.getSongList();
		SparseBooleanArray checked = songList.getCheckedItemPositions();
		for (int i = 0; i < songList.getAdapter().getCount(); i++) {
			if (checked.get(i, false)) {
				PlaylistSong<BaseArtist, BaseAlbum> song = new PlaylistSong((BaseSong<BaseArtist, BaseAlbum>) songList
						.getItemAtPosition(i), SongSource.MANUALLY_SELECTED);
				// Log.v(TAG, "Found song to add");
				controller.getPlayerController().appendSongAtEnd(song);
			}
		}
	}

	public void albumArtClicked() {
		activity.findViewById(R.id.songList).setVisibility(View.VISIBLE);
		activity.findViewById(R.id.selectAllButton).setVisibility(View.VISIBLE);
		activity.findViewById(R.id.selectNoneButton).setVisibility(View.VISIBLE);
		TextView clickCover = (TextView) activity.findViewById(R.id.clickCover);
		int numberCoverClicked = activity.getSettings().getCoverHintCountAlbum();
		if (numberCoverClicked < NUMBER_COVER_HINT_THRESSHOLD) {
			controller.getSettingsEditor().setCoverHintCountAlbum(numberCoverClicked + 1);
			activity.findViewById(R.id.clickCover).setVisibility(View.GONE);
		}
		if (clickCover != null) {
			clickCover.setVisibility(View.GONE);
		}
	}

	public void onItemLongClicked(int position) {
		controller.doHapticFeedback();
		ListView songList = albumDetails.getSongList();

		PlaylistSong<BaseArtist, BaseAlbum> song = new PlaylistSong((BaseSong<BaseArtist, BaseAlbum>) songList
				.getItemAtPosition(position), SongSource.MANUALLY_SELECTED);
		Intent intent = new Intent(activity, SongMenu.class);
		intent.putExtra(Controller.INTENT_EXTRA_BASE_SONG, new ParcelableSong(song));
		activity.startActivity(intent);
	}

}
