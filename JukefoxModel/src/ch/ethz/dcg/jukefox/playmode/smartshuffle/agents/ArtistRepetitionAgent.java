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

import java.util.Date;

import ch.ethz.dcg.jukefox.commons.DataUnavailableException;
import ch.ethz.dcg.jukefox.model.collection.BaseAlbum;
import ch.ethz.dcg.jukefox.model.collection.BaseArtist;
import ch.ethz.dcg.jukefox.model.collection.BaseSong;
import ch.ethz.dcg.jukefox.model.providers.StatisticsProvider;

/**
 * This agent looks for artist repetitions. It does not suggest songs but votes against songs of agents which have very
 * recently played songs.
 */
public class ArtistRepetitionAgent extends AbstractRepetitionAgent {

	public final static String REPETITION_TYPE = "artist";

	/**
	 * In [min]
	 * 
	 * @see #getLastPlayedTime(BaseSong)
	 */
	private final static int MIN_TIME_FOR_REPLAY_ACCEPTABLE = 60; // 60min

	private final StatisticsProvider statisticsProvider;

	public ArtistRepetitionAgent(StatisticsProvider statisticsProvider) {
		super();

		this.statisticsProvider = statisticsProvider;
	}

	@Override
	protected int getMinTimeForReplayAcceptable() {
		return MIN_TIME_FOR_REPLAY_ACCEPTABLE;
	}

	@Override
	protected Date getLastPlayedTime(BaseSong<BaseArtist, BaseAlbum> song) {
		Date lastPlayed;
		try {
			lastPlayed = statisticsProvider.getLastPlayedTime(song.getArtist());
		} catch (DataUnavailableException e) {
			lastPlayed = new Date(0); // Not played yet
		}
		return lastPlayed;
	}

	@Override
	public String getRepetitionType() {
		return REPETITION_TYPE;
	}

}
