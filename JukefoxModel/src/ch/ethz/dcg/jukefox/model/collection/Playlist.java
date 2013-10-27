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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ch.ethz.dcg.jukefox.model.commons.PlaylistPositionOutOfRangeException;

/**
 * Representation of a mutable playlist
 */
public class Playlist implements IPlaylist {

	private String name;
	private List<PlaylistSong<BaseArtist, BaseAlbum>> songList;

	private int positionInList = 0;
	private int positionInSong = 0;
	private int playMode = 0;
	private boolean hasExtras = false;

	public Playlist() {
		// TODO: skip lists could be more efficient.
		songList = new ArrayList<PlaylistSong<BaseArtist, BaseAlbum>>();
		setPlaylistName("");
	}

	public Playlist(List<PlaylistSong<BaseArtist, BaseAlbum>> songList, String name) {
		this.songList = songList;
		this.name = name;
	}

	@Override
	public List<PlaylistSong<BaseArtist, BaseAlbum>> getSongList() {
		return Collections.unmodifiableList(songList);
	}

	@Override
	public void setPlaylistName(String name) {
		this.name = name;
	}

	@Override
	public String getPlaylistName() {
		return name;
	}

	@Override
	public int getPlaylistSize() {
		return songList.size();
	}

	@Override
	public boolean isPlaylistEmpty() {
		return songList.isEmpty();
	}

	@Override
	public PlaylistSong<BaseArtist, BaseAlbum> getSongAtPosition(int position)
			throws PlaylistPositionOutOfRangeException {
		if (position >= 0 && position < songList.size()) {
			return songList.get(position);
		} else {
			throw new PlaylistPositionOutOfRangeException();
		}
	}

	/**
	 * Inserts the specified song into the playlist at the specified location.
	 * The song is inserted before the current element at the specified
	 * location. If the location is equal to the size of the playlist, the song
	 * is added at the end. If the location is smaller than the size of the
	 * playlist, then all elements beyond the specified location are moved by
	 * one position towards the end of the playlist.
	 * 
	 * @param song
	 *            song to insert
	 * @param position
	 *            position before which song is inserted
	 * @throws PlaylistPositionOutOfRangeException
	 */
	@Override
	public void insertSongAtPosition(PlaylistSong<BaseArtist, BaseAlbum> song, int position)
			throws PlaylistPositionOutOfRangeException {
		if (position < 0 || position > getPlaylistSize()) {
			throw new PlaylistPositionOutOfRangeException();
		}
		songList.add(position, song);
	}

	@Override
	public void appendSongAtEnd(PlaylistSong<BaseArtist, BaseAlbum> song) {
		songList.add(song);
	}

	/**
	 * Inserts the specified songs into the playlist at the specified location.
	 * The songs are inserted before the current element at the specified
	 * location. If the location is equal to the size of the playlist, the song
	 * is added at the end. If the location is smaller than the size of the
	 * playlist, then all elements beyond the specified location are moved by
	 * one position towards the end of the playlist.
	 * 
	 * @param song
	 *            song to insert
	 * @param position
	 *            position before which song is inserted
	 * @throws PlaylistPositionOutOfRangeException
	 */
	@Override
	public void insertSongsAtPosition(List<PlaylistSong<BaseArtist, BaseAlbum>> songs, int position)
			throws PlaylistPositionOutOfRangeException {
		if (position < 0 || position > getPlaylistSize()) {
			throw new PlaylistPositionOutOfRangeException();
		}
		songList.addAll(position, songs);
	}

	@Override
	public void appendSongsAtEnd(List<PlaylistSong<BaseArtist, BaseAlbum>> songs) {
		songList.addAll(songs);
	}

	/**
	 * Moves the song at oldPosition to newPosition
	 * 
	 * @param oldPosition
	 *            position of the song to move
	 * @param newPosition
	 *            the position of the song after the operation
	 * @throws PlaylistPositionOutOfRangeException
	 */
	@Override
	public void moveSong(int oldPosition, int newPosition) throws PlaylistPositionOutOfRangeException {

		if (oldPosition == newPosition) {
			return;
		}

		// Check plausability
		if (oldPosition < 0 || newPosition < 0 || oldPosition >= getPlaylistSize() || newPosition >= getPlaylistSize()
				|| oldPosition == newPosition) {
			throw new PlaylistPositionOutOfRangeException();
		}
		PlaylistSong<BaseArtist, BaseAlbum> song = songList.remove(oldPosition);
		insertSongAtPosition(song, newPosition);
	}

	@Override
	public void removeSong(int position) throws PlaylistPositionOutOfRangeException {
		if (position >= 0 && position < songList.size()) {
			songList.remove(position);
		} else {
			throw new PlaylistPositionOutOfRangeException();
		}
	}

	@Override
	public void shuffle(int startPosition) {
		Collections.shuffle(songList.subList(startPosition, songList.size()));
	}

	@Override
	public boolean hasExtras() {
		return hasExtras;
	}

	@Override
	public void setHasExtras(boolean hasExtras) {
		this.hasExtras = hasExtras;
	}

	public String getName() {
		return name;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public int getPositionInList() {
		return positionInList;
	}

	@Override
	public void setPositionInList(int positionInList) {
		this.positionInList = positionInList;
	}

	@Override
	public int getPositionInSong() {
		return positionInSong;
	}

	@Override
	public void setPositionInSong(int positionInSong) {
		this.positionInSong = positionInSong;
	}

	@Override
	public int getPlayMode() {
		return playMode;
	}

	@Override
	public void setPlayMode(int playMode) {
		this.playMode = playMode;
	}

	@Override
	public int getSize() {
		return songList.size();
	}
}
