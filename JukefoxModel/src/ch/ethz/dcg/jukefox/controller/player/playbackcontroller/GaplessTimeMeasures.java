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
package ch.ethz.dcg.jukefox.controller.player.playbackcontroller;

import java.util.LinkedHashMap;
import java.util.Map;

import ch.ethz.dcg.jukefox.commons.utils.Log;

public class GaplessTimeMeasures {

	public static final String TAG = GaplessTimeMeasures.class.getSimpleName();
	public static final int TIME_MEASUREMENT_ITEMS = 3;
	public static final int UNREALISTIC_GAP_THRESHOLD = 1000;

	private Long tempSong1TimeMeasured;
	private Long tempSong1Position;
	private Long tempSong1Duration;
	private LinkedHashMap<Integer, TimeMeasurement> timeMeasurements;
	private int id = 0;
	private int currentGapTime;
	private int autoGapRemoveTime;
	private int manualGapRemoveTime;

	public GaplessTimeMeasures(int autoGapRemoveTime, int manualGapRemoveTime) {
		this.autoGapRemoveTime = autoGapRemoveTime;
		this.manualGapRemoveTime = manualGapRemoveTime;
		timeMeasurements = new LinkedHashMap<Integer, TimeMeasurement>(0, 0.75f, false) {

			private static final long serialVersionUID = 1L;

			@Override
			public boolean removeEldestEntry(Map.Entry<Integer, TimeMeasurement> eldest) {
				if (this.size() > TIME_MEASUREMENT_ITEMS) {
					return true;
				} else {
					return false;
				}
			}
		};
		TimeMeasurement tm = new TimeMeasurement(autoGapRemoveTime);
		timeMeasurements.put(id, tm);
		id++;
		computeGapTime();
	}

	public void setSong1Times(long song1TimeMeasured, long song1Position, long song1Duration) {
		this.tempSong1TimeMeasured = song1TimeMeasured;
		this.tempSong1Position = song1Position;
		this.tempSong1Duration = song1Duration;
	}

	public void setSong2Times(long song2TimeMeasured, long song2Position, int gapOffset) {
		if (tempSong1Duration == null || tempSong1Position == null || tempSong1TimeMeasured == null) {
			Log.w(TAG, "Song2 time set before Song1 time was set! Not using time measurement");
			return;
		}
		TimeMeasurement tm = new TimeMeasurement(tempSong1TimeMeasured, tempSong1Position, tempSong1Duration,
				song2TimeMeasured, song2Position, gapOffset);
		tm.print();
		this.tempSong1TimeMeasured = null;
		this.tempSong1Position = null;
		this.tempSong1Duration = null;
		if (Math.abs(tm.getGap()) > UNREALISTIC_GAP_THRESHOLD || tm.getGap() < 0) {
			Log.w(TAG, "Not using gap timing because it's unrealistic: " + tm.getGap());
			//			System.out.println("Not using gap timing because it's unrealistic: " + tm.getGap());
			return;
		}
		timeMeasurements.put(id, tm);
		int lastGapCorrection = currentGapTime;
		computeGapTime();
		autoGapRemoveTime = currentGapTime;
		Log.v(TAG, "Last gap size: " + tm.getGap() + ", gap correction was: " + lastGapCorrection);
		//		System.out.println("Last gap size: " + tm.getGap() + ", gap correction was: " + lastGapCorrection);
		id++;
	}

	private void computeGapTime() {
		if (timeMeasurements.size() == 0) {
			currentGapTime = 0;
			return;
		}
		int mean = 0;
		for (TimeMeasurement t : timeMeasurements.values()) {
			mean += t.getGap();
		}
		currentGapTime = mean / timeMeasurements.size();
	}

	public int getGapTime() {
		return currentGapTime;
	}

	private class TimeMeasurement {

		private final long song1TimeMeasured;
		private final long song1Duration;
		private final long song1Position;
		private final long song2TimeMeasured;
		private final long song2Position;
		private final int gapOffset;
		private final int gap;

		public TimeMeasurement(long song1TimeMeasured, long song1Position, long song1Duration, long song2TimeMeasured,
				long song2Position, int gapOffset) {
			this.song1Duration = song1Duration;
			this.song1TimeMeasured = song1TimeMeasured;
			this.song1Position = song1Position;
			this.song2TimeMeasured = song2TimeMeasured;
			this.song2Position = song2Position;
			this.gapOffset = gapOffset;
			gap = computeGap();
		}

		public TimeMeasurement(int gap) {
			this.song1Duration = 0;
			this.song1TimeMeasured = 0;
			this.song1Position = 0;
			this.song2TimeMeasured = 0;
			this.song2Position = 0;
			this.gapOffset = 0;
			this.gap = gap;
		}

		public void print() {
			Log.v(TAG, "Song1Position: " + song1Position);
			Log.v(TAG, "Song1Duration: " + song1Duration);
			Log.v(TAG, "Song1Time: " + song1TimeMeasured);
			Log.v(TAG, "TimeDiff: " + (song2TimeMeasured - song1TimeMeasured));
			Log.v(TAG, "song2Pos: " + song2Position);
		}

		public int getGap() {
			return gap;
		}

		private int computeGap() {
			int remaining = (int) (song1Duration - song1Position);
			int songGap = (int) (remaining + song2Position);
			int totalGap = (int) (song2TimeMeasured - song1TimeMeasured) + gapOffset;
			return totalGap - songGap;
		}
	}

	public int getAutoGapRemoveTime() {
		return autoGapRemoveTime;
	}

	public int getManualGapRemoveTime() {
		return manualGapRemoveTime;
	}

}
