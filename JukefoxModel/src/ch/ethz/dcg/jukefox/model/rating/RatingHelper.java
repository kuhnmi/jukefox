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

import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Vector;

import ch.ethz.dcg.jukefox.commons.DataUnavailableException;
import ch.ethz.dcg.jukefox.commons.DataWriteException;
import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.jukefox.commons.utils.Utils;
import ch.ethz.dcg.jukefox.commons.utils.kdtree.KdTreePoint;
import ch.ethz.dcg.jukefox.data.db.IDbDataPortal;
import ch.ethz.dcg.jukefox.data.log.LogManager;
import ch.ethz.dcg.jukefox.data.log.RatingLogEntry;
import ch.ethz.dcg.jukefox.model.collection.BaseAlbum;
import ch.ethz.dcg.jukefox.model.collection.BaseArtist;
import ch.ethz.dcg.jukefox.model.collection.BaseSong;
import ch.ethz.dcg.jukefox.model.collection.SongCoords;
import ch.ethz.dcg.jukefox.model.collection.statistics.CollectionProperties;
import ch.ethz.dcg.jukefox.model.providers.OtherDataProvider;
import ch.ethz.dcg.jukefox.model.providers.SongProvider;
import ch.ethz.dcg.jukefox.model.rating.RatingEntry.RatingSource;

/**
 * Helper class for writing rating entries into the database.
 */
public class RatingHelper {

	/**
	 * How many songs the neighborhood should contain in average.
	 */
	private final static int AVG_NEIGHBORHOOD_SIZE = 10;

	/**
	 * How many songs the neighborhood should contain at most.
	 */
	private final static int MAX_NEIGHBORHOOD_SIZE = 25;

	/**
	 * Minimum weight so that we consider it as interresting. Weights below this value can be dropped since they most
	 * likely get outvoted anyway.
	 */
	public static final double MIN_WEIGHT = 0.01d;

	private final static String TAG = RatingHelper.class.getName();

	private final IDbDataPortal dbDataPortal;
	private final SongProvider songProvider;
	private final LogManager logManager;
	private final int profileId;

	private OtherDataProvider otherDataProvider;

	public RatingHelper(int profileId, IDbDataPortal dbDataPortal, SongProvider songProvider,
			OtherDataProvider otherDataProvider, LogManager logManager) {
		this.profileId = profileId;
		this.dbDataPortal = dbDataPortal;
		this.songProvider = songProvider;
		this.otherDataProvider = otherDataProvider;
		this.logManager = logManager;
	}

	/**
	 * Adds rating entries for the played song and its neighbors out of the fraction which was played. This is done
	 * synchroneous.
	 * 
	 * @param song
	 *            The song
	 * @param playlogTimestamp
	 *            The timestamp of the according playlog entry
	 * @param fractionPlayed
	 *            How much was played (must be in [0, 1])
	 * 
	 * @see #getRatingFromFractionPlayed(double)
	 */
	public void addRatingFromPlayLog(final BaseSong<BaseArtist, BaseAlbum> song, final Date playlogTimestamp,
			final double fractionPlayed) {
		// Calculate the rating
		double rating = getRatingFromFractionPlayed(fractionPlayed);

		// Write the rating for the main song
		try {
			dbDataPortal.getStatisticsHelper().writeRatingEntry(profileId, song.getId(), playlogTimestamp,
					rating, 1.0d, RatingSource.Playlog);
		} catch (DataWriteException e) {
			Log.w(TAG, e);
		}

		try {
			int neighborhoodSize = 0;

			// Get song coordinates
			final SongCoords mainCoords = getCoordinatesForSong(song);
			if ((mainCoords != null) && (mainCoords.getCoords() != null)) {
				// Only rate neighborhood for songs with coordinates

				// Calculate the maximum distance
				double maxDistance = getMaxDistance();

				// Find neighbors
				Vector<KdTreePoint<Integer>> neighbors = songProvider.getSongsAroundPositionEuclidian(
						mainCoords.getCoords(), (float) maxDistance);
				neighborhoodSize = neighbors.size();

				// Sort the neighbors, so that the nearest ones come first
				Collections.sort(neighbors, new Comparator<KdTreePoint<Integer>>() {

					@Override
					public int compare(KdTreePoint<Integer> p1, KdTreePoint<Integer> p2) {
						float distP1 = Utils.distance(mainCoords.getCoords(), p1.getPosition());
						float distP2 = Utils.distance(mainCoords.getCoords(), p2.getPosition());

						return Float.compare(distP1, distP2);
					}
				});

				// Write the weighted rating for the neighbors
				int i = 0;
				for (KdTreePoint<Integer> node : neighbors) {
					try {
						// Find the songId
						int songId = node.getID();

						if (songId == song.getId()) {
							// Don't reprocess it
							continue;
						}

						// Limit the neighborhood by a fixed size 
						if (i >= MAX_NEIGHBORHOOD_SIZE) {
							break;
						}
						i++;

						// Find the coordinates for this song
						float[] coords = node.getPosition();

						// Calculate the weight for this distance
						double weight = getWeightForNeighbor(mainCoords.getCoords(), coords, maxDistance);

						// Limit the neighborhood by a minimum weight
						if (weight < MIN_WEIGHT) {
							break;
						}

						// Write the rating for this neighbor
						dbDataPortal.getStatisticsHelper().writeRatingEntry(profileId, songId,
								playlogTimestamp,
								rating, weight, RatingSource.Neighbor);
					} catch (DataWriteException e2) {
						// Just ignore it
						Log.w(TAG, e2);
					}
				}
			}

			// Write log for this rating round (only if not in transaction, since otherwise we are most probably in the fake round of NextSongCalculationThread)
			if (!dbDataPortal.inTransaction()) {
				// Get the meSongId of the played song
				Integer meSongId = null;
				try {
					meSongId = otherDataProvider.getMusicExplorerSongId(song);
				} catch (DataUnavailableException e) {
					Log.w(TAG, e);
				}

				RatingLogEntry.Builder log = RatingLogEntry.createInstance()
						.setPlayedSong((meSongId != null) ? meSongId : 0)
						.setRating((float) rating)
						.setNeighborhoodSize(neighborhoodSize);
				logManager.addLogEntry(log.build());
			}
		} catch (DataUnavailableException e) {
			// Some data is not available -> abort
			Log.w(TAG, e);
			return;
		}
	}

	/**
	 * Returns the rating for the given play fraction. <br/>
	 * <p>
	 * Rating function: (x is percent of song which was played)
	 * <table>
	 * <tr>
	 * <td></td>
	 * <td>( -1</td>
	 * <td>, if x in [0, 0.25)</td>
	 * </tr>
	 * <tr>
	 * <td>f(x) =</td>
	 * <td>{ 4x - 2</td>
	 * <td>, if x in [0.25, 0.75) // Linear growth between 1/4 & 3/4</td>
	 * </tr>
	 * <tr>
	 * <td></td>
	 * <td>( 1</td>
	 * <td>, if x in [0.75, 1]</td>
	 * </tr>
	 * </table>
	 * </p>
	 * 
	 * @param fractionPlayed
	 *            How much was played (must be in [0, 1])
	 * @return The rating
	 */
	public static double getRatingFromFractionPlayed(double fractionPlayed) {
		return Math.min(Math.max(4 * fractionPlayed - 2, -1), 1);
	}

	/**
	 * Returns the coordinates of the given song or null if it has none.
	 * 
	 * @param song
	 * @return The coordinates or null
	 */
	private SongCoords getCoordinatesForSong(BaseSong<BaseArtist, BaseAlbum> song) {
		try {
			return dbDataPortal.getSongCoordsById(song.getId());
		} catch (DataUnavailableException e) {
			return null;
		}
	}

	/**
	 * Returns the radius of the neighborhood.<br/>
	 * This is calculated so that in average {@link #AVG_NEIGHBORHOOD_SIZE} ({@value #AVG_NEIGHBORHOOD_SIZE}) songs are
	 * rated.<br/>
	 * We use the standard deviation to find the maximal distance to ensure the above property. We have:
	 * <p>
	 * proportion_around_mean = 1 - 2*NEIGHBORHOOD_SIZE_FRACTION = error_function(z / sqrt(2))
	 * </p>
	 * and therefore
	 * <p>
	 * z = error_function<sup>-1</sup>(1 - 2*NEIGHBORHOOD_SIZE_FRACTION) * sqrt(2)
	 * </p>
	 * The distance is then calculated using
	 * <p>
	 * max_distance = mean - std_deviation * z
	 * </p>
	 * 
	 * @return The maximum distance
	 * @throws DataUnavailableException
	 *             If the std deviation is not available
	 */
	private double getMaxDistance() throws DataUnavailableException {
		CollectionProperties cp = otherDataProvider.getCollectionProperties();

		double stdDeviation = cp.getSongDistanceStdDeviation();
		double proportion = 1 - 2 * (AVG_NEIGHBORHOOD_SIZE / (double) otherDataProvider
				.getNumberOfSongsWithCoordinates());
		double z = invErrorFunction(proportion) * Math.sqrt(2);

		double mean = cp.getAverageSongDistance();

		return Math.max(mean - z * stdDeviation, 0); // Ensure that we get a positive distance
	}

	/**
	 * Returns an approximation of the inverse error-function for the given x.
	 * 
	 * @param x
	 * @return erf<sup>-1</sup>(x)
	 * @see {@linkplain http://en.wikipedia.org/wiki/Error_function#Approximation_with_elementary_functions}
	 */
	private double invErrorFunction(double x) {
		double a = 0.147;

		double f1 = Math.pow(2 / (Math.PI * a) + Math.log(1 - x * x) / 2, 2);
		double f2 = Math.log(1 - x * x) / a;
		double f3 = 2 / (Math.PI * a) + Math.log(1 - x * x) / 2;

		return Math.signum(x) * Math.sqrt(Math.sqrt(f1 - f2) - f3);
	}

	/**
	 * Returns the weight according to the distance between the given songs. The weight function <i>w</i> is linear and
	 * w(maxDistance) = 0, w(0) = 1.
	 * 
	 * @param coords1
	 * @param coords2
	 * @return The calculated weight
	 */
	private double getWeightForNeighbor(float[] coords1, float[] coords2, double maxDistance) {
		// Calculate the distance
		double distance = Utils.distance(coords1, coords2);

		// Return the weight
		return -1 / maxDistance * distance + 1;
	}

}
