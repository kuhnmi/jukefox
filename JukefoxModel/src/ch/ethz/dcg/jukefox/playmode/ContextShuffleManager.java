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
package ch.ethz.dcg.jukefox.playmode;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashSet;

import ch.ethz.dcg.jukefox.commons.DataUnavailableException;
import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.jukefox.model.AbstractCollectionModelManager;
import ch.ethz.dcg.jukefox.model.AbstractPlayerModelManager;

/**
 * Handles all the smart shuffling logic.
 */
public class ContextShuffleManager extends SmartShuffleManager {

	protected final static String TAG = ContextShuffleManager.class.getSimpleName();

	private boolean lockRegion = true;

	// private HashMap<Integer, ProcessedSong> recommendedBy;

	public ContextShuffleManager(AbstractCollectionModelManager collectionModel, AbstractPlayerModelManager playerModel) {
		super(collectionModel, playerModel);

		// recommendedBy = new HashMap<Integer, ProcessedSong>();
	}

	public void loadPermanentState(PermanentSmartShuffleState state) {

		this.played = state.getPlayed();
		this.processed = state.getProcessed();
		this.weightThreshold = state.getWeightThreshold();
		this.resetCnt = state.getResetCnt();
		this.minPlayableSize = state.getMinPlayableSize();
		this.sampleSize = state.getSampleSize();
		this.minSampleSize = state.getMinSampleSize();
		this.maxPlayedHistorySize = state.getMaxPlayedHistorySize();
		this.numCandiates = state.getNumCandiates();
	}

	public PermanentSmartShuffleState getPermanentSmartShuffleState() {
		return new PermanentSmartShuffleState(played, processed, weightThreshold, resetCnt, minPlayableSize,
				sampleSize, minSampleSize, maxPlayedHistorySize, numCandiates);
	}

	@Override
	public void processSong(int songId, float rating) {
		if (lockRegion) {
			Log.v(TAG, "Not processing song with id " + songId + ", as the region is locked.");
			return;
		}
		Log.v(TAG, "addSongToProcessed(): id: " + songId + ", rating: " + rating);
		float[] pcaCoords = null;
		try {
			pcaCoords = collectionModel.getOtherDataProvider().getSongPcaCoords(songId);
		} catch (DataUnavailableException e) {
			Log.w(TAG, e);
		}
		if (pcaCoords == null) {
			Log.v(TAG, "no pcaCoords available for song => don't add it to processed.");
			return;
		}
		ProcessedSong ps = new ProcessedSong(songId, Math.abs(rating), rating <= 0, pcaCoords);
		processed.add(ps);
		adjustWeights();
	}

	@Override
	protected void adjustWeights() {
		// quickly remove existing centroids if the user stops liking some area
		// consecutiveSkipCnt = skipped ? consecutiveSkipCnt + 1 : 0;
		int n = processed.size();
		for (int i = n - 1; i >= 0; i--) {
			ProcessedSong ps = processed.get(i);
			// ps.weight -= 0.1 * (consecutiveSkipCnt + 1);
			// don't specially treat consecutive skips at the moment
			ps.weight -= 0.1;
			if (ps.weight <= weightThreshold) {
				processed.remove(i);
			}
		}
	}

	public void lockRegion() {
		lockRegion = true;
	}

	public void unlockRegion() {
		lockRegion = false;
	}

	public boolean isRegionLocked() {
		return lockRegion;
	}

	public static class PermanentSmartShuffleState implements Serializable {

		private static final long serialVersionUID = 1L;

		public PermanentSmartShuffleState(LinkedHashSet<Integer> played, ArrayList<ProcessedSong> processed,
				double weightThreshold, int resetCnt, int minPlayableSize, int sampleSize, int minSampleSize,
				int maxPlayedHistorySize, int numCandiates) {
			super();
			this.played = played;
			this.processed = processed;
			this.weightThreshold = weightThreshold;
			this.resetCnt = resetCnt;
			this.minPlayableSize = minPlayableSize;
			this.sampleSize = sampleSize;
			this.minSampleSize = minSampleSize;
			this.maxPlayedHistorySize = maxPlayedHistorySize;
			this.numCandiates = numCandiates;

		}

		public static long getSerialversionuid() {
			return serialVersionUID;
		}

		public LinkedHashSet<Integer> getPlayed() {
			return played;
		}

		public ArrayList<ProcessedSong> getProcessed() {
			return processed;
		}

		public double getWeightThreshold() {
			return weightThreshold;
		}

		public int getResetCnt() {
			return resetCnt;
		}

		public int getMinPlayableSize() {
			return minPlayableSize;
		}

		public int getSampleSize() {
			return sampleSize;
		}

		public int getMinSampleSize() {
			return minSampleSize;
		}

		public int getMaxPlayedHistorySize() {
			return maxPlayedHistorySize;
		}

		public int getNumCandiates() {
			return numCandiates;
		}

		protected LinkedHashSet<Integer> played = new LinkedHashSet<Integer>();
		protected ArrayList<ProcessedSong> processed = new ArrayList<ProcessedSong>();

		protected double weightThreshold = 0.01;
		protected int resetCnt = -1; // account for first call to reset...
		protected int minPlayableSize = 80;
		protected int sampleSize;
		protected int minSampleSize = 200;

		protected int maxPlayedHistorySize = 50;

		protected int numCandiates = 5;

	}
}
