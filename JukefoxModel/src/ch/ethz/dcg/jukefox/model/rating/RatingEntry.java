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
package ch.ethz.dcg.jukefox.model.rating;

import java.util.Date;

public class RatingEntry {

	public static enum RatingSource {
		Playlog(0),
		Neighbor(1);

		//Manual(2);

		private final int value;

		RatingSource(int value) {
			this.value = value;
		}

		public final int value() {
			return value;
		}
	};

	private int id;
	private int profileId;
	private Date timestamp;
	private int songId;
	private double rating;
	private double weight;
	private RatingSource ratingSource;

	/**
	 * Constructor for a {@link RatingEntry}.
	 * 
	 * @param id
	 *            The id of this {@link RatingEntry}.
	 * @param profileId
	 *            The player model id
	 * @param timestamp
	 *            The timestamp of the rating
	 * @param songId
	 *            The id of the song
	 * @param rating
	 *            The rating (in [-1, 1])
	 * @param weight
	 *            The weight of the rating. Should be 1 except for neighbor ratings. The further away the original song
	 *            is the more the weight gets to 0.
	 * @param ratingSource
	 *            Where does this rating come from?
	 */
	public RatingEntry(int id, int profileId, Date timestamp, int songId, double rating, double weight,
			RatingSource ratingSource) {
		this.id = id;
		this.profileId = profileId;
		this.timestamp = timestamp;
		this.songId = songId;
		this.rating = rating;
		this.weight = weight;
		this.ratingSource = ratingSource;
	}

	// *** Getters *** //

	public int getId() {
		return id;
	}

	public int getProfileId() {
		return profileId;
	}

	public Date getTimestamp() {
		return timestamp;
	}

	public int getSongId() {
		return songId;
	}

	public double getRating() {
		return rating;
	}

	public double getWeight() {
		return weight;
	}

	public RatingSource getRatingSource() {
		return ratingSource;
	}

}
