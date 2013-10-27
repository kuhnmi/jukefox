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
import java.util.Date;
import java.util.List;

import ch.ethz.dcg.jukefox.model.collection.BaseAlbum;
import ch.ethz.dcg.jukefox.model.collection.BaseArtist;
import ch.ethz.dcg.jukefox.model.collection.BaseSong;
import ch.ethz.dcg.jukefox.playmode.smartshuffle.SongVote;
import ch.ethz.dcg.jukefox.playmode.smartshuffle.agents.AgentManager.AgentType;

/**
 * This agent looks for repetitions. It does not suggest songs but votes against very recently played ones.
 */
public abstract class AbstractRepetitionAgent extends AbstractAgent {

	/**
	 * Does not return any suggestions, just votes against songs recently played.
	 */
	@Override
	public List<BaseSong<BaseArtist, BaseAlbum>> suggestSongs(int num) {
		// TODO: propose long not rated songs/artists?
		return new ArrayList<BaseSong<BaseArtist, BaseAlbum>>();
	}

	/**
	 * If a song or a container around it was just played it receives a rating of -1 if it was played before (now -
	 * {@link #getMinTimeForReplayAcceptable()}), then it gets a vote of 0. In between the vote changes linearly:
	 * 
	 * <pre>
	 * v(x) = 1/{@link #getMinTimeForReplayAcceptable()} * x - 1.
	 * </pre>
	 * 
	 * @return The votes &in; [-1, 0]
	 */
	@Override
	public List<SongVote> vote(List<BaseSong<BaseArtist, BaseAlbum>> songs) {
		List<SongVote> ret = new ArrayList<SongVote>(songs.size());
		for (BaseSong<BaseArtist, BaseAlbum> song : songs) {
			Date lastPlayed = getLastPlayedTime(song);

			long diff = (new Date()).getTime() - lastPlayed.getTime();
			diff /= 1000 * 60; // to min

			float rating = Math.min(1 / (float) getMinTimeForReplayAcceptable() * diff - 1, 0);
			ret.add(new SongVote(song, rating));
		}
		return ret;
	}

	/**
	 * Returns the time which must go by after last playing a song or its container until we vote neutral for it again.
	 * 
	 * @return The time [min]
	 */
	protected abstract int getMinTimeForReplayAcceptable();

	/**
	 * Returns the to be considered last played date for this song.
	 * 
	 * @param song
	 *            The song
	 * @return The last played date
	 */
	protected abstract Date getLastPlayedTime(BaseSong<BaseArtist, BaseAlbum> song);

	/**
	 * Returns the string identifier of what repetition we are looking for.
	 * 
	 * @return The repetition type
	 */
	public abstract String getRepetitionType();

	@Override
	public final AgentType getAgentType() {
		return AgentType.Repetition;
	}

	@Override
	public final String getIdentifier() {
		return super.getIdentifier() + getRepetitionType();
	}

}
