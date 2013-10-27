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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ch.ethz.dcg.jukefox.commons.utils.RandomProvider;
import ch.ethz.dcg.jukefox.data.db.IDbStatisticsHelper.Direction;
import ch.ethz.dcg.jukefox.model.collection.BaseAlbum;
import ch.ethz.dcg.jukefox.model.collection.BaseArtist;
import ch.ethz.dcg.jukefox.model.collection.BaseSong;
import ch.ethz.dcg.jukefox.model.collection.statistics.StatisticsArtist;
import ch.ethz.dcg.jukefox.model.providers.SongProvider;
import ch.ethz.dcg.jukefox.model.providers.StatisticsProvider;
import ch.ethz.dcg.jukefox.model.rating.RatingEntry.RatingSource;
import ch.ethz.dcg.jukefox.playmode.smartshuffle.SongVote;
import ch.ethz.dcg.jukefox.playmode.smartshuffle.agents.AgentManager.AgentType;

/**
 * This agent returns the top songs and assigns their rating returned from the statistics provider.
 */
public class TopArtistAgent extends AbstractRecentAgent {

	/**
	 * How many artists should be considered when searching for song suggestions.
	 */
	private final static int HOW_MANY_ARTISTS_FOR_SUGGESTION = 5;

	private final SongProvider songProvider;
	private final StatisticsProvider statisticsProvider;
	private List<StatisticsArtist> suggestedTopArtists;

	public TopArtistAgent(SongProvider songProvider, StatisticsProvider statisticsProvider, TimeFilter timeFilter) {
		super(timeFilter);

		this.songProvider = songProvider;
		this.statisticsProvider = statisticsProvider;
	}

	/**
	 * Returns songs from the top num artists.
	 */
	@Override
	public List<BaseSong<BaseArtist, BaseAlbum>> suggestSongs(int num) {
		if (getTimeFilter() == TimeFilter.RECENTLY) {
			// Recently should not propose anything (we would end up with too much songs of the same agent get played)
			return new ArrayList<BaseSong<BaseArtist, BaseAlbum>>();
		}

		num = Math.min(num, HOW_MANY_ARTISTS_FOR_SUGGESTION);

		// Find the top artists
		suggestedTopArtists = statisticsProvider.getTopArtists(num, getTimeRange(), getTimeFilter()
				.toDbTimeFilter(), Direction.TOP, false);

		// Calculate the overall rating sum
		float ratingSum = 0.0f;
		for (StatisticsArtist artist : suggestedTopArtists) {
			ratingSum += ((Float) artist.getValue()) + 1; // To only have positive ratings
		}

		// Get the song suggestions from each artist proportional to its rating
		List<BaseSong<BaseArtist, BaseAlbum>> ret = new ArrayList<BaseSong<BaseArtist, BaseAlbum>>(num);
		for (StatisticsArtist artist : suggestedTopArtists) {
			int songsCount = Math.round((((Float) artist.getValue()) + 1) / ratingSum * num);

			ret.addAll(getRandomSongsOfArtist(artist, songsCount));
		}

		return ret;
	}

	/**
	 * Returns random songs of the given artist.
	 * 
	 * @param artist
	 *            The artist
	 * @param songsCount
	 *            The number of songs which should be returned
	 * @return The songs
	 */
	private List<BaseSong<BaseArtist, BaseAlbum>> getRandomSongsOfArtist(StatisticsArtist artist,
			int songsCount) {

		// Get the songs of this artist
		List<BaseSong<BaseArtist, BaseAlbum>> songs = songProvider.getAllBaseSongs(artist);
		songsCount = Math.min(songs.size(), songsCount);

		// Get songsCount random songs
		Set<BaseSong<BaseArtist, BaseAlbum>> ret = new HashSet<BaseSong<BaseArtist, BaseAlbum>>();
		while (ret.size() < songsCount) {
			int idx = RandomProvider.getRandom().nextInt(songs.size());
			ret.add(songs.get(idx));
		}
		return new ArrayList<BaseSong<BaseArtist, BaseAlbum>>(ret);
	}

	/**
	 * Assigns each song the all-time rating of its artist.
	 */
	@Override
	public List<SongVote> vote(List<BaseSong<BaseArtist, BaseAlbum>> songs) {
		// Get the set of artists
		Set<BaseArtist> artists = new HashSet<BaseArtist>();
		for (BaseSong<BaseArtist, BaseAlbum> song : songs) {
			artists.add(song.getArtist());
		}

		// Reuse vote of our suggested
		List<StatisticsArtist> artistRatings;
		if (suggestedTopArtists != null) {
			artistRatings = new ArrayList<StatisticsArtist>(suggestedTopArtists);
			artists.removeAll(suggestedTopArtists);
		} else {
			artistRatings = new ArrayList<StatisticsArtist>();
		}

		// Find the artist ratings
		artistRatings.addAll(statisticsProvider.getArtistRatings(new ArrayList<BaseArtist>(artists),
				getTimeRange(), getTimeFilter().toDbTimeFilter(),
				new RatingSource[] { RatingSource.Playlog, RatingSource.Neighbor }, false));

		// Asign the songs their artists rating
		List<SongVote> ret = new ArrayList<SongVote>(songs.size());
		for (BaseSong<BaseArtist, BaseAlbum> song : songs) {
			float rating;
			int idx = artistRatings.indexOf(song.getArtist());
			if (idx >= 0) {
				rating = (Float) artistRatings.get(idx).getValue();
			} else {
				// Artist has no rating
				rating = 0.0f;
			}

			ret.add(new SongVote(song, rating));
		}

		return ret;
	}

	@Override
	public AgentType getAgentType() {
		return AgentType.Top;
	}

}
