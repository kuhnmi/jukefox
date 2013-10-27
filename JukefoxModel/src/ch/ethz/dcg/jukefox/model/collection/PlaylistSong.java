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
package ch.ethz.dcg.jukefox.model.collection;

public class PlaylistSong<ArtistType extends BaseArtist, AlbumType extends BaseAlbum> extends
		BaseSong<ArtistType, AlbumType> {

	public static enum SongSource {
		MANUALLY_SELECTED(0), SMART_SHUFFLE(1), SIMILAR_PLAY_MODE(2), AUTOMATICALLY_SELECTED(3), RANDOM_SONG(4), TAG_BASED(
				5), TIME_BASED(6);

		private final int value;

		SongSource(int value) {
			this.value = value;
		}

		public final int value() {
			return value;
		}

	}

	private SongSource source;

	public PlaylistSong(BaseSong<ArtistType, AlbumType> song, SongSource source) {
		super(song.getId(), song.getName(), song.getArtist(), song.getAlbum(), song.getDuration());
		this.source = source;
	}

	public PlaylistSong(int id, String name, ArtistType artist, AlbumType album, SongSource source, int duration) {
		super(id, name, artist, album, duration);
		this.source = source;
	}

	public SongSource getSongSource() {
		return source;
	}

	@Override
	public boolean equals(Object obj) {
		return super.equals(obj);
	}

	@Override
	public int hashCode() {
		return super.hashCode();
	}

}
