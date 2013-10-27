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
package ch.ethz.dcg.jukefox.model;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import ch.ethz.dcg.jukefox.commons.DataUnavailableException;
import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.jukefox.commons.utils.MathUtils;
import ch.ethz.dcg.jukefox.model.collection.BaseAlbum;
import ch.ethz.dcg.jukefox.model.collection.BaseArtist;
import ch.ethz.dcg.jukefox.model.collection.BaseSong;
import ch.ethz.dcg.jukefox.model.collection.CompleteTag;
import ch.ethz.dcg.jukefox.model.collection.PlaylistSong;
import ch.ethz.dcg.jukefox.model.collection.PlaylistSong.SongSource;
import ch.ethz.dcg.jukefox.model.collection.SongCoords;
import ch.ethz.dcg.jukefox.model.providers.SongCoordinatesProvider;
import ch.ethz.dcg.jukefox.model.providers.SongProvider;

public class TagPlaylistGenerator {

	public static final String TAG = TagPlaylistGenerator.class.getSimpleName();

	public static final int DEFAULT_SAMPLE_FACTOR = 15;
	public static final int DEFAULT_PLAYLIST_SIZE = 10;

	private final SongCoordinatesProvider songCoordinatesProvider;
	private final SongProvider songProvider;

	/**
	 * Creates a new instance of {@link TagPlaylistGenerator}
	 */
	public TagPlaylistGenerator(SongCoordinatesProvider songCoordinatesProvider, SongProvider songProvider) {
		this.songCoordinatesProvider = songCoordinatesProvider;
		this.songProvider = songProvider;
	}

	public ArrayList<PlaylistSong<BaseArtist, BaseAlbum>> generatePlaylist(CompleteTag tag, int size, float sampleFactor)
			throws DataUnavailableException {
		int count = 0;

		final float[] tagCoords = tag.getPlsaCoords();
		final float std = (float) Math.sqrt(tag.getVariancePlsaProb());
		final float mean = tag.getMeanPlsaProb();

		LinkedHashSet<Integer> targetSongIds = new LinkedHashSet<Integer>();
		while (count < 30 && targetSongIds.size() < 2 * size) {
			List<SongCoords> songs = songCoordinatesProvider.getRandomSongsWithCoords(size);
			for (SongCoords coords : songs) {
				if (coords.getCoords() != null) {
					if (MathUtils.dotProduct(coords.getCoords(), tagCoords) - mean > std) {
						targetSongIds.add(coords.getId());
					}
				}
			}
			count++;
			// Log.v(TAG, "end of loop " + count + ", targetSongs.size(): "
			// + targetSongIds.size());
		}
		Log.v(TAG, "loop count: " + count);

		return getPlaylist(targetSongIds, size);
	}

	private ArrayList<PlaylistSong<BaseArtist, BaseAlbum>> getPlaylist(LinkedHashSet<Integer> songIds, int size) {
		ArrayList<PlaylistSong<BaseArtist, BaseAlbum>> playlist = new ArrayList<PlaylistSong<BaseArtist, BaseAlbum>>(
				size);
		int i = 0;
		for (Integer songId : songIds) {
			BaseSong<BaseArtist, BaseAlbum> baseSong = null;
			try {
				baseSong = songProvider.getBaseSong(songId);
			} catch (DataUnavailableException e) {
				Log.w(TAG, e);
			}
			if (baseSong == null) {
				continue;
			}
			playlist.add(new PlaylistSong<BaseArtist, BaseAlbum>(baseSong, SongSource.TAG_BASED));
			i++;
			if (i == size) {
				break;
			}
		}
		return playlist;
	}

}
