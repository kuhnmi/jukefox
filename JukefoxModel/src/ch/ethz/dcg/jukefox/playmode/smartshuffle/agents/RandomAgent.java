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
package ch.ethz.dcg.jukefox.playmode.smartshuffle.agents;

import java.util.ArrayList;
import java.util.List;

import ch.ethz.dcg.jukefox.commons.DataUnavailableException;
import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.jukefox.model.collection.BaseAlbum;
import ch.ethz.dcg.jukefox.model.collection.BaseArtist;
import ch.ethz.dcg.jukefox.model.collection.BaseSong;
import ch.ethz.dcg.jukefox.model.collection.PlaylistSong;
import ch.ethz.dcg.jukefox.model.providers.SongProvider;
import ch.ethz.dcg.jukefox.playmode.smartshuffle.SongVote;
import ch.ethz.dcg.jukefox.playmode.smartshuffle.agents.AgentManager.AgentType;

/**
 * This agent simply votes for a random set of songs and sets their rating to 0.
 */
public class RandomAgent extends AbstractAgent {

	private final static String TAG = RandomAgent.class.getName();

	private SongProvider songProvider;
	private List<BaseSong<BaseArtist, BaseAlbum>> suggestedSongs;

	public RandomAgent(SongProvider songProvider) {
		this.songProvider = songProvider;
	}

	/**
	 * Gets num random songs and assigns 0 as their rating.
	 */
	@Override
	public List<BaseSong<BaseArtist, BaseAlbum>> suggestSongs(int num) {
		try {
			// Load the random songs
			List<PlaylistSong<BaseArtist, BaseAlbum>> songs = songProvider.getRandomSongs(num);
			suggestedSongs = new ArrayList<BaseSong<BaseArtist, BaseAlbum>>(songs);
		} catch (DataUnavailableException e) {
			Log.w(TAG, e);
			suggestedSongs = new ArrayList<BaseSong<BaseArtist, BaseAlbum>>();
		}

		return suggestedSongs;
	}

	/**
	 * Votes for our suggested songs with 1 and for the others with 0.
	 */
	@Override
	public List<SongVote> vote(List<BaseSong<BaseArtist, BaseAlbum>> songs) {
		List<SongVote> ret = new ArrayList<SongVote>(songs.size());
		for (BaseSong<BaseArtist, BaseAlbum> song : songs) {
			float vote = suggestedSongs.contains(song) ? 1.0f : 0.0f;
			ret.add(new SongVote(song, vote));
		}
		return ret;
	}

	@Override
	public AgentType getAgentType() {
		return AgentType.Random;
	}

}
