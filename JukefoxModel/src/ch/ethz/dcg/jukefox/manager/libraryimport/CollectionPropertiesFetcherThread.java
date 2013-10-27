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
package ch.ethz.dcg.jukefox.manager.libraryimport;

import java.util.ArrayList;
import java.util.List;

import ch.ethz.dcg.jukefox.commons.DataWriteException;
import ch.ethz.dcg.jukefox.commons.utils.JoinableThread;
import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.jukefox.commons.utils.MathUtils;
import ch.ethz.dcg.jukefox.commons.utils.Utils;
import ch.ethz.dcg.jukefox.data.cache.ImportStateListener;
import ch.ethz.dcg.jukefox.model.collection.SongCoords;
import ch.ethz.dcg.jukefox.model.collection.statistics.CollectionProperties;
import ch.ethz.dcg.jukefox.model.libraryimport.ImportState;
import ch.ethz.dcg.jukefox.model.providers.OtherDataProvider;
import ch.ethz.dcg.jukefox.model.providers.SongCoordinatesProvider;

public class CollectionPropertiesFetcherThread extends JoinableThread {

	private final static String TAG = CollectionPropertiesFetcherThread.class.getName();
	private final static int NUM_SONGS_FOR_CALCULATION = 1000;

	private final SongCoordinatesProvider songCoordinatesProvider;
	private final OtherDataProvider otherDataProvider;

	private ImportState importState;

	public CollectionPropertiesFetcherThread(OtherDataProvider otherDataProvider,
			SongCoordinatesProvider songCoordinatesProvider, ImportState importState) {
		this.otherDataProvider = otherDataProvider;
		this.songCoordinatesProvider = songCoordinatesProvider;
		this.importState = importState;
	}

	@Override
	public void run() {
		if (!importState.isCoordinatesFetched()) {
			// Wait for the coordinates to be fetched
			importState.addListener(new ImportStateListener() {

				private void doNotify() {
					synchronized (CollectionPropertiesFetcherThread.this) {
						CollectionPropertiesFetcherThread.this.notify();
					}
				}

				@Override
				public void onImportStarted() {
					doNotify();
				}

				@Override
				public void onImportProblem(Throwable e) {
					doNotify();
				}

				@Override
				public void onImportCompleted(boolean hadChanges) {
					doNotify();
				}

				@Override
				public void onImportAborted(boolean hadChanges) {
					doNotify();
				}

				@Override
				public void onCoordinatesFetched() {
					doNotify();
				}

				@Override
				public void onBaseDataCommitted() {
				}

				@Override
				public void onAlbumCoversFetched() {
				}
			});
			try {
				synchronized (this) {
					// Wait for the coordinates to be fetched
					while (!importState.isCoordinatesFetched() && importState.isImporting()) { // Ensure that we finish waiting eventually
						wait(5000);
					}
				}
			} catch (InterruptedException e) {
				Log.w(TAG, e);
			}

			if (!importState.isCoordinatesFetched()) {
				return; // Probably the import got aborted
			}
		}

		// Start the computation
		try {
			List<SongCoords> songCoords = songCoordinatesProvider.getSongCoords(false); // Read them from the db, since the preloaded data may not be available yet
			songCoords = MathUtils.getRandomElements(songCoords, NUM_SONGS_FOR_CALCULATION);

			List<Float> distances = new ArrayList<Float>(songCoords.size() * (songCoords.size() - 1) / 2);
			float mean = 0;
			int progress = -1;
			for (int i = 0; i < songCoords.size(); ++i) {
				// Update the progress message
				int newProgress = (int) (i / (float) songCoords.size() * 3); // 0 - 3
				if (newProgress != progress) {
					progress = newProgress;
					importState.setCollectionPropertiesProgress(progress, 5, "calculating collection properties");
				}

				// Calculate the distances
				SongCoords sc1 = songCoords.get(i);

				for (int j = i + 1; j < songCoords.size(); ++j) {
					SongCoords sc2 = songCoords.get(j);

					float distance = Utils.distance(sc1.getCoords(), sc2.getCoords());
					distances.add(distance);
					mean += distance;
				}
			}
			mean /= distances.size();

			importState.setCollectionPropertiesProgress(4, 5, "calculating collection properties"); // 4 - 5
			float sum = 0;
			for (float distance : distances) {
				float tmp = distance - mean;
				sum += tmp * tmp; // Probably faster than Math.pow()
			}
			float stdDeviation = (float) Math.sqrt(sum / (distances.size() - 1)); // = \sqrt{1/(N-1) * \sum_{i=1}^{N} (x_i - mean)^2} = \sqrt{1/(N-1) * sum}

			CollectionProperties cp = new CollectionProperties();
			cp.setAverageSongDistance(mean);
			cp.setSongDistanceStdDeviation(stdDeviation);
			otherDataProvider.setCollectionProperties(cp);

			importState.setCollectionPropertiesProgress(5, 5, "calculating collection properties");
			importState.setCollectionPropertiesCalculated(true);
		} catch (DataWriteException e) {
			Log.w(TAG, e);
		}
	}
}
