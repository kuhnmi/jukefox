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
package ch.ethz.dcg.jukefox.data.log;

/**
 * Log entry for ratings. The song, its rating and the number of neighbors are stored.
 */
public final class RatingLogEntry extends AbstractLogEntry {

	private final static int VERSION = 1;
	private final static String TYPE_IDENT = "rtg";

	private Integer musicexplorerId = null;
	private Float rating = null;
	private Integer neighborhoodSize = null;

	private RatingLogEntry() {
	}

	public static Builder createInstance() {
		return new Builder();
	}

	public Integer getMeSongId() {
		return musicexplorerId;
	}

	public float getRating() {
		return rating;
	}

	public int getNeighborhoodSize() {
		return neighborhoodSize;
	}

	@Override
	public int getTypeVersion() {
		return VERSION;
	}

	@Override
	protected String getTypeIdent() {
		return TYPE_IDENT;
	}

	@Override
	protected String packYourStuff() {
		return String.format("%d|%.2f|%d", // meSongId|rating|neighborhoodSize
				musicexplorerId, rating, neighborhoodSize);
	}

	public final static class Builder extends AbstractLogEntry.Builder<RatingLogEntry, Builder> {

		private Builder() {
			super();
			init(new RatingLogEntry(), this);
		}

		public Builder setPlayedSong(int meSongId) {
			getInstance().musicexplorerId = meSongId;
			return this;
		}

		public Builder setRating(float rating) {
			getInstance().rating = rating;
			return this;
		}

		public Builder setNeighborhoodSize(int size) {
			getInstance().neighborhoodSize = size;
			return this;
		}

	}

}
