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
package ch.ethz.dcg.jukefox.playmode.smartshuffle;

import ch.ethz.dcg.jukefox.model.collection.BaseAlbum;
import ch.ethz.dcg.jukefox.model.collection.BaseArtist;
import ch.ethz.dcg.jukefox.model.collection.BaseSong;

/**
 * Represents an agent vote for a song.
 */
public final class SongVote {

	private final BaseSong<BaseArtist, BaseAlbum> song;
	private final float vote;

	public SongVote(BaseSong<BaseArtist, BaseAlbum> song, float vote) {
		this.song = song;
		this.vote = vote;
	}

	/**
	 * Returns the voted song.
	 * 
	 * @return
	 */
	public BaseSong<BaseArtist, BaseAlbum> getSong() {
		return song;
	}

	/**
	 * Returns the vote for the song.
	 * 
	 * @return &in; [-1, 1]
	 */
	public float getVote() {
		return vote;
	}

}
