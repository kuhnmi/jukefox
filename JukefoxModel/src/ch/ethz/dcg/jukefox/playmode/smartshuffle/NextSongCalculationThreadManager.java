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
package ch.ethz.dcg.jukefox.playmode.smartshuffle;

import ch.ethz.dcg.jukefox.commons.utils.Utils;
import ch.ethz.dcg.jukefox.model.collection.BaseAlbum;
import ch.ethz.dcg.jukefox.model.collection.BaseArtist;
import ch.ethz.dcg.jukefox.model.collection.BaseSong;
import ch.ethz.dcg.jukefox.playmode.smartshuffle.NextSongCalculationThread.Case;

public class NextSongCalculationThreadManager extends Thread {

	private final Helper helper;
	private NextSongCalculationThread runningCalculation = null;

	private BaseSong<BaseArtist, BaseAlbum> currentlyPlayingSong = null;

	private NextSongCalculationThread lastFinishedNegative = null;
	private NextSongCalculationThread lastFinishedPositive = null;

	private NextSongCalculationThread nextMostUpToDate = null;
	private NextSongCalculationThread mostUpToDate = null;

	public NextSongCalculationThreadManager(Helper helper) {
		super();
		setDaemon(true);
		setName(NextSongCalculationThreadManager.class.getSimpleName());

		this.helper = helper;
	}

	@Override
	public void run() {
		super.run();

		for (;;) {
			boolean runningChanged = false;

			synchronized (this) {
				// Evaluate what calculation should be calculated next
				Case nextCase = getNextCase();
				if (nextCase == Case.Positive) {
					runningCalculation = helper.createPositiveNextSongCalculationThread(currentlyPlayingSong);
					runningChanged = true;
				} else if (nextCase == Case.Negative) {
					runningCalculation = helper.createNegativeNextSongCalculationThread(currentlyPlayingSong);
					runningChanged = true;
				}

				if (!runningChanged) {
					try {
						// Nothing to do -> wait until we need to work again
						wait();
					} catch (InterruptedException e) {
						// Just ignore the exception. This does not affect us in any way.
					}
				}
			}

			if (runningChanged) {
				// Start the calculation
				runningCalculation.start();

				// Wait for the calculation thread to finish
				while (runningCalculation.isAlive()) {
					try {
						runningCalculation.realJoin();
					} catch (InterruptedException e) {
						// Just ignore the exception.  
					}
				}

				if (!runningCalculation.isAborted()) {
					synchronized (this) {
						if (runningCalculation.getCalculationCase() == Case.Positive) {
							lastFinishedPositive = runningCalculation;
						} else {
							lastFinishedNegative = runningCalculation;
						}

						if (runningCalculation.equals(nextMostUpToDate) || (mostUpToDate == null)) {
							mostUpToDate = runningCalculation;
						}
					}
				}
			}
		}
	}

	/**
	 * Returns which case the next calculation should cover to be most likely right. If we do not need to do anything in
	 * the moment, <code>null</code> is returned.
	 * 
	 * @return The next case
	 */
	private Case getNextCase() {
		// Check if song changed
		boolean differentSong = true;
		if (runningCalculation != null) {
			BaseSong<BaseArtist, BaseAlbum> calculationSong = runningCalculation.getCurrentSong();
			differentSong = !Utils.nullEquals(currentlyPlayingSong, calculationSong);
		}

		// Evaluate the situation
		double rating = helper.getTemporaryRatingForSong(currentlyPlayingSong);
		Case nextCase;
		if (!differentSong) {
			// Still the same song as when we started the last calculation thread

			if (runningCalculation.getCalculationCase() == Case.Negative) {
				// Last was negative -> start positive calculation
				nextCase = Case.Positive;
			} else {
				// no work is needed at the moment
				nextCase = null;
			}
		} else {
			// New situation -> check how much of the current song is already played

			if (rating >= 0) {
				// Start positive lookahead calculation
				nextCase = Case.Positive;
			} else {
				// Start negative lookahead calculation
				nextCase = Case.Negative;
			}
		}

		return nextCase;
	}

	/**
	 * Adjusts the most up-to-date next song prediction based on the rating of the just finished song.
	 * 
	 * @param song
	 *            The song that just finished
	 * @param rating
	 *            The rating of the song that just finished
	 */
	public synchronized void songFinished(BaseSong<BaseArtist, BaseAlbum> song, double rating) {
		// Find the calculations which considered this song
		NextSongCalculationThread finishedNegative = null;
		NextSongCalculationThread finishedPositive = null;
		NextSongCalculationThread running = null;

		if ((lastFinishedNegative != null) && Utils.nullEquals(lastFinishedNegative.getCurrentSong(), song)) {
			finishedNegative = lastFinishedNegative;
		}
		if ((lastFinishedPositive != null) && Utils.nullEquals(lastFinishedPositive.getCurrentSong(), song)) {
			finishedPositive = lastFinishedPositive;
		}
		if ((runningCalculation != null) && Utils.nullEquals(runningCalculation.getCurrentSong(), song) && runningCalculation
				.isAlive()) {
			running = runningCalculation;
		}

		boolean upToDateChanged = false;
		if (rating >= 0) {
			// Positive rating
			if (finishedPositive != null) {
				mostUpToDate = finishedPositive;
				upToDateChanged = true;
			} else if (finishedNegative != null) {
				mostUpToDate = finishedNegative;
				upToDateChanged = true;
			}
		} else {
			// Negative rating
			if (finishedNegative != null) {
				mostUpToDate = finishedNegative;
				upToDateChanged = true;
			} else if (finishedPositive != null) {
				mostUpToDate = finishedPositive;
				upToDateChanged = true;
			}
		}

		if (running != null) {
			if (upToDateChanged) {
				// We found a correct predicted calculation -> dont waste time on this outdated calculation
				running.abortCalculation();
			} else {
				nextMostUpToDate = running; // Will become the most up-to-date calculation once it finishes			
			}
		}
	}

	/**
	 * Adjusts the calculation behavior
	 * 
	 * @param currentSong
	 */
	public synchronized void currentSongChanged(BaseSong<BaseArtist, BaseAlbum> currentSong) {
		currentlyPlayingSong = currentSong;
		notifyAll(); // Inform, that new work arrived

	}

	/**
	 * Returns the most up-to-date {@link NextSongCalculationThread}. Please make sure, that you called
	 * {@link #songFinished(BaseSong, double)} before this to get the best result.
	 * 
	 * @return The most up-to-date {@link NextSongCalculationThread}
	 */
	public synchronized NextSongCalculationThread getNextSongCalculationThread() {
		return mostUpToDate;
	}

	/**
	 * Helper for the {@link NextSongCalculationThreadManager}. This ensures the information hiding principle.
	 */
	public interface Helper {

		public PositiveNextSongCalculationThread createPositiveNextSongCalculationThread(
				BaseSong<BaseArtist, BaseAlbum> currentSong);

		public NegativeNextSongCalculationThread createNegativeNextSongCalculationThread(
				BaseSong<BaseArtist, BaseAlbum> currentSong);

		/**
		 * Returns the rating out of the actual playback position of the given song. If the given song is not played at
		 * the moment, <code>-1</code> is returned.
		 * 
		 * @param song
		 *            The song
		 * @return The temporary rating
		 */
		public double getTemporaryRatingForSong(BaseSong<BaseArtist, BaseAlbum> song);

	}
}
