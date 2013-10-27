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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;

import ch.ethz.dcg.jukefox.commons.DataUnavailableException;
import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.jukefox.commons.utils.RandomProvider;
import ch.ethz.dcg.jukefox.commons.utils.Utils;
import ch.ethz.dcg.jukefox.data.db.PcaCoordinatesUnavailableException;
import ch.ethz.dcg.jukefox.model.AbstractCollectionModelManager;
import ch.ethz.dcg.jukefox.model.AbstractPlayerModelManager;
import ch.ethz.dcg.jukefox.model.collection.BaseAlbum;
import ch.ethz.dcg.jukefox.model.collection.BaseArtist;
import ch.ethz.dcg.jukefox.model.collection.PlaylistSong;
import ch.ethz.dcg.jukefox.model.collection.PlaylistSong.SongSource;
import ch.ethz.dcg.jukefox.model.collection.SongCoords;

/**
 * Handles all the smart shuffling logic.
 */
@Deprecated
public class SmartShuffleManager {

	protected final static String TAG = SmartShuffleManager.class.getSimpleName();

	protected final static int INIT_SAMPLE_SIZE = 300;

	protected final AbstractCollectionModelManager collectionModel;
	protected AbstractPlayerModelManager playerModel;
	protected final Random random;

	protected LinkedHashSet<Integer> played = new LinkedHashSet<Integer>();
	protected ArrayList<ProcessedSong> processed = new ArrayList<ProcessedSong>();

	protected double weightThreshold = 0.01;
	protected int resetCnt = -1; // account for first call to reset...
	protected int minPlayableSize = 80;
	protected int sampleSize;
	protected int minSampleSize = 200;

	protected int maxPlayedHistorySize = 50;

	protected int numCandiates = 5;

	public SmartShuffleManager(AbstractCollectionModelManager collectionModel, AbstractPlayerModelManager playerModel) {
		this.collectionModel = collectionModel;
		this.playerModel = playerModel;
		this.random = RandomProvider.getRandom();
	}

	/**
	 * Get a new song chosen by smart shuffle.
	 * 
	 * @param skipped
	 */
	public PlaylistSong<BaseArtist, BaseAlbum> getSong(int currentlyPlaying, boolean skipped)
			throws DataUnavailableException {
		if (currentlyPlaying == -1 && processed.isEmpty()) {
			int randomSongId;
			randomSongId = getRandomNonPlayedId();
			return new PlaylistSong<BaseArtist, BaseAlbum>(collectionModel.getSongProvider().getBaseSong(randomSongId),
					SongSource.RANDOM_SONG);
		}
		long start = System.currentTimeMillis();
		int songId = getNextSongId(currentlyPlaying, skipped);
		Log.i(TAG, "returning song id " + songId);
		long end = System.currentTimeMillis();
		Log.i(TAG, "time for smart shuffle: " + (end - start));
		PlaylistSong<BaseArtist, BaseAlbum> song = new PlaylistSong<BaseArtist, BaseAlbum>(collectionModel
				.getSongProvider().getBaseSong(songId), SongSource.RANDOM_SONG);
		return song;
	}

	// TODO: Do we need this or can we not just every song to played?
	public void addToPlayed(int songId) {
		played.add(songId);
		if (played.size() > maxPlayedHistorySize) {
			// remove oldest
			int id = played.iterator().next();
			played.remove(id);
		}
	}

	public void processSong(int songId, float rating) {
		Log.v(TAG, "addSongToProcessed(): id: " + songId + ", rating: " + rating);
		ProcessedSong ps = createProcessedSong(songId, rating);
		if (ps == null) {
			return;
		}
		processed.add(ps);
		adjustWeights();
	}

	public float getRatingForSignal(boolean skipped) {
		return skipped ? -1 : 1;
	}

	private ProcessedSong createProcessedSong(int songId, float rating) {
		if (songId <= 0) {
			return null;
		}
		float[] pcaCoords = null;
		try {
			pcaCoords = collectionModel.getOtherDataProvider().getSongPcaCoords(songId);
		} catch (DataUnavailableException e) {
			Log.w(TAG, e);
		}
		if (pcaCoords == null) {
			Log.v(TAG, "no pcaCoords available for song => don't add it to processed.");
			return null;
		}
		ProcessedSong ps = new ProcessedSong(songId, Math.abs(rating), rating <= 0, pcaCoords);
		return ps;
	}

	public void reset() {
		resetCnt++;
		this.sampleSize = INIT_SAMPLE_SIZE;
		played.clear();
		processed.clear();
	}

	protected int getNextSongId(int currentlyPlaying, boolean skipped) throws DataUnavailableException {

		// ArrayList<Integer> songIds =
		// preloadedDataManager.getData().getIdsOfSongsWithCoords();

		setSampleSize(sampleSize); // make sure the bounds are still valid...
		// ArrayList<Integer> sampleIndices =
		// Utils.getRandomNumbers(songIds.size(), sampleSize, random);

		List<SongCoords> candidateSongs = collectionModel.getSongCoordinatesProvider().getRandomSongsWithCoords(
				sampleSize);

		Log.i(TAG, "sampleSize(): " + sampleSize);
		Log.i(TAG, "candidateSongs.size(): " + candidateSongs.size());

		if (onlySkippedSongs() && (currentlyPlaying == -1 || skipped)) {
			Log.v(TAG, "returning far to bad song");
			return getFarToBadSong(currentlyPlaying, skipped, candidateSongs);
		} else {
			Log.v(TAG, "returning close to good song");
			return getCloseToGoodSong(currentlyPlaying, skipped, candidateSongs);
		}

	}

	protected int getFarToBadSong(int currentSongId, boolean skipped, List<SongCoords> candidateSongs)
			throws DataUnavailableException {
		ArrayList<Float> distribution = new ArrayList<Float>();
		ArrayList<Integer> playable = new ArrayList<Integer>();
		float sum = 0;
		for (SongCoords sc : candidateSongs) {
			int id = sc.getId();
			sum = processIdForCloseToBadSong(id, currentSongId, skipped, sum, distribution, playable);
			// if (played.contains(id)) {
			// continue;
			// }
			// float[] pos;
			// try {
			// pos = model.getSongPcaCoords(id);
			// } catch (DataUnavailableException e) {
			// continue;
			// }
			// ProcessedSong ps = getClosestProcessedSong(pos);
			//
			// // songs that have the closest processed song far away get high
			// // probability to be chosen.
			// float weightedDist = Utils.distance(pos, ps.pos) * ps.weight;
			// sum += weightedDist;
			// distribution.add(sum);
			// playable.add(id);
		}

		if (playable.size() == 0) {
			return getRandomNonPlayedId();
		}

		if (playable.size() < minPlayableSize) {
			setSampleSize(sampleSize * 2);
		} else {
			setSampleSize((int) Math.round(sampleSize * 0.9));
		}

		Log.i(TAG, "new sample size: " + sampleSize + ", playableCnt: " + playable.size());

		return getCandidate(distribution, playable, sum);
	}

	protected Float processIdForCloseToBadSong(int id, int currentSongId, boolean skipped, float sum,
			ArrayList<Float> distribution, ArrayList<Integer> playable) {
		if (played.contains(id) || id == currentSongId) {
			return sum;
		}
		float[] pos;
		try {
			pos = collectionModel.getOtherDataProvider().getSongPcaCoords(id);
		} catch (PcaCoordinatesUnavailableException e) {
			// TODO Controller should be noticed that something is probably
			// strange (coords without pca coords).
			return sum;
		} catch (DataUnavailableException e) {
			return sum;
		}
		ProcessedSong ps = getClosestProcessedSong(pos, currentSongId, skipped);

		// songs that have the closest processed song far away get high
		// probability to be chosen.
		float weightedDist = Utils.distance(pos, ps.pos) * ps.weight;
		sum += weightedDist;
		distribution.add(sum);
		playable.add(id);
		return sum;
	}

	protected int getCloseToGoodSong(int currentSongId, boolean skipped, List<SongCoords> candidateSongs)
			throws DataUnavailableException {
		ArrayList<Float> distribution = new ArrayList<Float>();
		ArrayList<Integer> playable = new ArrayList<Integer>();
		float sum = 0;
		for (SongCoords sc : candidateSongs) {
			int id = sc.getId();
			sum = processIdForCloseToGoodSong(id, currentSongId, skipped, sum, distribution, playable);
			// if (played.contains(id)) {
			// continue;
			// }
			// float[] pos;
			// try {
			// pos = model.getSongPcaCoords(id);
			// } catch (DataUnavailableException e) {
			// continue;
			// }
			// ProcessedSong ps = getClosestProcessedSong(pos);
			// if (!ps.skipped) {
			// double p = ps.weight;
			// sum += p;
			// distribution.add(sum);
			// playable.add(id);
			// }
		}
		if (playable.size() == 0) {
			return getRandomNonPlayedId();
		}

		if (playable.size() < minPlayableSize) {
			setSampleSize(sampleSize * 2);
		} else {
			setSampleSize((int) Math.round(sampleSize * 0.9));
		}

		Log.i(TAG, "new sample size: " + sampleSize + ", playableCnt: " + playable.size());

		return getBestCandidate(currentSongId, skipped, distribution, playable, sum);
	}

	protected float processIdForCloseToGoodSong(int id, int currentSongId, boolean skipped, float sum,
			ArrayList<Float> distribution, ArrayList<Integer> playable) {
		if (played.contains(id) || id == currentSongId) {
			return sum;
		}
		float[] pos;
		try {
			pos = collectionModel.getOtherDataProvider().getSongPcaCoords(id);
		} catch (PcaCoordinatesUnavailableException e) {
			// TODO Controller should be notified that something is probably
			// strange (coords without pca coords).
			return sum;
		} catch (DataUnavailableException e) {
			return sum;
		}
		ProcessedSong ps = getClosestProcessedSong(pos, currentSongId, skipped);
		if (ps != null && !ps.skipped) {
			double p = ps.weight;
			sum += p;
			distribution.add(sum);
			playable.add(id);
		}
		return sum;
	}

	protected int getBestCandidate(int currentSongId, boolean skipped, ArrayList<Float> distribution,
			ArrayList<Integer> ids, float max) throws DataUnavailableException {
		float minDist = Float.MAX_VALUE;
		int minId = -1;
		for (int i = 0; i < numCandiates; i++) {
			int songId = getCandidate(distribution, ids, max);
			float[] pos;
			try {
				pos = collectionModel.getOtherDataProvider().getSongPcaCoords(songId);
			} catch (DataUnavailableException e) {
				continue;
			}
			ProcessedSong ps = getClosestProcessedSong(pos, currentSongId, skipped);
			float weightedDist = Utils.distance(pos, ps.pos) / ps.weight;

			if (weightedDist < minDist) {
				minId = songId;
				minDist = weightedDist;
			}
		}
		if (minId == -1) {
			String msg = "getBestCandidate: Should never happen! " + "=> returning random song";
			Log.i(TAG, msg);
			return getRandomNonPlayedId();
		}
		return minId;
	}

	protected int getCandidate(ArrayList<Float> distribution, ArrayList<Integer> ids, float max) {
		float f = random.nextFloat() * max;
		int idx = Collections.binarySearch(distribution, f);
		if (idx < 0) {
			idx = -idx - 1;
		}
		return ids.get(idx);
	}

	protected int getRandomId() throws DataUnavailableException {
		try {
			List<PlaylistSong<BaseArtist, BaseAlbum>> songs = collectionModel.getSongProvider()
					.getRandomSongWithCoordinates(1);
			if (songs != null && songs.size() > 0) {
				return songs.get(0).getId();
			}
		} catch (DataUnavailableException e) {
			Log.w(TAG, e);
		}
		Log.v(TAG, "no songs with coordinates available => selecting random song from db.");
		return collectionModel.getOtherDataProvider().getRandomSongId();
	}

	protected int getRandomNonPlayedId() throws DataUnavailableException {
		int id = getRandomId();
		int cnt = 0;
		while (played.contains(id) && cnt < 8) {
			id = getRandomId();
			cnt++;
		}
		return id;
	}

	protected ProcessedSong getClosestProcessedSong(float[] pos, int currentSongId, boolean skipped) {
		float minDist = Float.MAX_VALUE;
		ProcessedSong closest = null;
		for (ProcessedSong ps : processed) {
			float dist = Utils.distance(pos, ps.pos);
			if (dist < minDist) {
				minDist = dist;
				closest = ps;
			}
		}
		ProcessedSong currentSong = createProcessedSong(currentSongId, getRatingForSignal(skipped));
		if (currentSong != null) {
			float dist = Utils.distance(pos, currentSong.pos);
			if (dist < minDist) {
				minDist = dist;
				closest = currentSong;
			}
		}
		return closest;
	}

	protected boolean onlySkippedSongs() {
		for (ProcessedSong ps : processed) {
			if (!ps.skipped) {
				return false;
			}
		}
		return true;
	}

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

	protected void setSampleSize(int size) {
		// keep this order (if collectionSize < minSampleSize it still works)
		size = Math.max(minSampleSize, size);
		int collectionSize = 0;
		try {
			collectionSize = collectionModel.getOtherDataProvider().getNumberOfSongsWithCoordinates();
		} catch (DataUnavailableException e) {
			Log.w(TAG, e);
		}
		size = Math.min(collectionSize, size);
		sampleSize = size;
	}
}
