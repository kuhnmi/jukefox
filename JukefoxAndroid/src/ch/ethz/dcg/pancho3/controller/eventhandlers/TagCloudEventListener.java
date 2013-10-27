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
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.TextView;
import ch.ethz.dcg.jukefox.commons.Constants;
import ch.ethz.dcg.jukefox.commons.DataUnavailableException;
import ch.ethz.dcg.jukefox.commons.utils.AndroidUtils;
import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.jukefox.commons.utils.RandomProvider;
import ch.ethz.dcg.jukefox.controller.Controller;
import ch.ethz.dcg.jukefox.manager.AndroidSettingsManager;
import ch.ethz.dcg.jukefox.model.collection.BaseAlbum;
import ch.ethz.dcg.jukefox.model.collection.BaseArtist;
import ch.ethz.dcg.jukefox.model.collection.BaseSong;
import ch.ethz.dcg.jukefox.model.collection.BaseTag;
import ch.ethz.dcg.jukefox.model.collection.CompleteTag;
import ch.ethz.dcg.jukefox.model.collection.PlaylistSong;
import ch.ethz.dcg.jukefox.model.collection.PlaylistSong.SongSource;
import ch.ethz.dcg.jukefox.model.player.PlayModeType;
import ch.ethz.dcg.pancho3.model.collection.ParcelableTag;
import ch.ethz.dcg.pancho3.view.commons.JukefoxActivity;
import ch.ethz.dcg.pancho3.view.overlays.TagPlaylistGenerationActivity;
import ch.ethz.dcg.pancho3.view.tabs.PlayerActivity;

public class TagCloudEventListener extends BaseJukefoxEventListener implements OnClickListener, OnLongClickListener {

	public static final int NUM_SONGS = 5;

	public TagCloudEventListener(Controller controller, JukefoxActivity activity) {
		super(controller, activity);
	}

	@Override
	public boolean onLongClick(View v) {
		controller.doHapticFeedback();
		CompleteTag tag;
		try {
			tag = activity.getCollectionModel().getTagProvider().getCompleteTag(v.getId());
			List<BaseSong<BaseArtist, BaseAlbum>> songs = activity.getCollectionModel().getSongProvider()
					.getClosestBaseSongsToPosition(AndroidUtils.normalizeCoordsSum(tag.getPlsaCoords(), 1), NUM_SONGS);
			BaseSong<BaseArtist, BaseAlbum> song = songs.get(RandomProvider.getRandom().nextInt(NUM_SONGS));
			controller.getPlayerController().stop();
			controller.getPlayerController().clearPlaylist();
			controller.getPlayerController().appendSongAtEnd(
					new PlaylistSong<BaseArtist, BaseAlbum>(song, SongSource.TAG_BASED));
			int artistAvoidance = AndroidSettingsManager.getAndroidSettingsReader().getSimilarArtistAvoidanceNumber();
			controller.getPlayerController().setPlayMode(PlayModeType.SIMILAR, artistAvoidance,
					Constants.SAME_SONG_AVOIDANCE_NUM);
			try {
				controller.getPlayerController().play();
			} catch (Exception e) {
				Log.w(TAG, e);
			}
			controller.startActivity(activity, PlayerActivity.class);
			activity.finish();

		} catch (DataUnavailableException e) {
			// TODO Show dialog?
			Log.w(TAG, e);
		}
		return true;
	}

	@Override
	public void onClick(View v) {
		controller.doHapticFeedback();
		Intent intent = new Intent(activity, TagPlaylistGenerationActivity.class);
		int id = v.getId();
		String name = ((TextView) v).getText().toString();
		BaseTag tag = new BaseTag(id, name);
		intent.putExtra(Controller.INTENT_EXTRA_BASE_TAG, new ParcelableTag(tag));
		activity.startActivity(intent);
		// activity.finish();
	}

}
