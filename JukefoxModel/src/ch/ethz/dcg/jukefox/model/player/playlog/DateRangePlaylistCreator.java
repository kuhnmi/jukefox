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
package ch.ethz.dcg.jukefox.model.player.playlog;

import java.util.ArrayList;
import java.util.List;

import ch.ethz.dcg.jukefox.commons.DataUnavailableException;
import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.jukefox.model.collection.BaseAlbum;
import ch.ethz.dcg.jukefox.model.collection.BaseArtist;
import ch.ethz.dcg.jukefox.model.collection.PlaylistSong;

public class DateRangePlaylistCreator {

	public static final String TAG = DateRangePlaylistCreator.class.getSimpleName();
	private static final float TOLERANCE_RANGE = 0.1f;
	private static final float TOLERANCE_GLOBAL = 0.1f;
	private long fromTimestamp;
	private long toTimestamp;
	private PlayLog playLog;

	public DateRangePlaylistCreator(long fromTimestamp, long toTimestamp, PlayLog playLog) {
		this.playLog = playLog;
		this.fromTimestamp = fromTimestamp;
		this.toTimestamp = toTimestamp;
	}

	// public BaseSong<BaseArtist, BaseAlbum> getFirstSong() throws
	// DataUnavailableException {
	// Date fromDate = new Date(fromTimestamp);
	// Date toDate = new Date(toTimestamp);
	// Log.v(TAG, "try to get songs for date range " + fromDate.getDate() + " "
	// + fromDate.getTime() + " to " + toDate.getDate() + " " +
	// toDate.getTime());
	// BaseSong<BaseArtist, BaseAlbum> song =
	// model.getPlayLogger().getArbitrarySongForDateRange(fromTimestamp,
	// toTimestamp);
	// if (song == null) {
	// Log.v(TAG, "no song found => try to get song outside of range");
	// song = model.getPlayLogger().getSongCloseToDateRange(fromTimestamp,
	// toTimestamp, TOLERANCE_RANGE, TOLERANCE_GLOBAL);
	// }
	// if (song == null) {
	// song = model.getPlayLogger().getSongByDateTag(fromTimestamp,
	// toTimestamp);
	// }
	// if (song != null) {
	// Log.v(TAG, "Got song " + song.getName());
	// } else {
	// Log.v(TAG, "Song is null ");
	// }
	// if (song == null) {
	// throw new DataUnavailableException("No logged song available");
	// }
	// return song;
	//
	// }

	public List<PlaylistSong<BaseArtist, BaseAlbum>> getAtMostSongs(int number) throws DataUnavailableException {
		// Date fromDate = new Date(fromTimestamp);
		// Date toDate = new Date(toTimestamp);
		// Log.v(TAG,
		// "try to get songs for date range " + fromDate.getDate() + " " +
		// fromDate.getTime() + " to "
		// + toDate.getDate() + " " + toDate.getTime());
		List<PlaylistSong<BaseArtist, BaseAlbum>> songs = null;
		try {
			songs = playLog.getSongsForDateRange(fromTimestamp, toTimestamp, number);
			if (songs != null && songs.size() == 0) {
				return songs;
			}
			Log.v(TAG, "try to get song outside of range");
			PlaylistSong<BaseArtist, BaseAlbum> tmp = playLog.getSongCloseToDateRange(fromTimestamp, toTimestamp,
					TOLERANCE_RANGE, TOLERANCE_GLOBAL);
			if (tmp != null) {
				songs = new ArrayList<PlaylistSong<BaseArtist, BaseAlbum>>();
				songs.add(tmp);
				Log.v(TAG, "returning song close to date range");
				// Log.v(TAG, "song.name: " + songs.get(0).getName());
				// Log.v(TAG, "song.id: " + songs.get(0).getId());
				return songs;
			}
		} catch (Exception e) {
			Log.w(TAG, e);
		}
		Log.v(TAG, "no songs found for data range => trying to get songs for corresponding tag...");
		return playLog.getSongByDateTag(fromTimestamp, toTimestamp);
	}
}
