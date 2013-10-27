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

import java.util.List;

import ch.ethz.dcg.jukefox.model.commons.PlaylistPositionOutOfRangeException;

public interface IPlaylist extends IReadOnlyPlaylist {

	void setPlaylistName(String name);

	void insertSongAtPosition(PlaylistSong<BaseArtist, BaseAlbum> song, int position)
			throws PlaylistPositionOutOfRangeException;

	void insertSongsAtPosition(List<PlaylistSong<BaseArtist, BaseAlbum>> songs, int position)
			throws PlaylistPositionOutOfRangeException;

	void appendSongAtEnd(PlaylistSong<BaseArtist, BaseAlbum> song);

	void appendSongsAtEnd(List<PlaylistSong<BaseArtist, BaseAlbum>> songs);

	void moveSong(int oldPosition, int newPosition) throws PlaylistPositionOutOfRangeException;

	void removeSong(int position) throws PlaylistPositionOutOfRangeException;

	void shuffle(int startPosition);

	public void setHasExtras(boolean hasExtras);

	public void setName(String name);

	public void setPositionInList(int positionInList);

	public void setPositionInSong(int positionInSong);

	public void setPlayMode(int playMode);
}
