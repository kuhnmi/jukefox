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
package ch.ethz.dcg.jukefox.model.player.playlog;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import ch.ethz.dcg.jukefox.commons.Constants;
import ch.ethz.dcg.jukefox.commons.DataUnavailableException;
import ch.ethz.dcg.jukefox.commons.DataWriteException;
import ch.ethz.dcg.jukefox.commons.utils.JoinableThread;
import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.jukefox.controller.player.IOnPlayerStateChangeListener;
import ch.ethz.dcg.jukefox.controller.player.IReadOnlyPlayerController;
import ch.ethz.dcg.jukefox.data.HttpHelper;
import ch.ethz.dcg.jukefox.data.context.AbstractContextResult;
import ch.ethz.dcg.jukefox.data.context.IContextProvider;
import ch.ethz.dcg.jukefox.data.db.IDbDataPortal;
import ch.ethz.dcg.jukefox.manager.ModelSettingsManager;
import ch.ethz.dcg.jukefox.model.TagPlaylistGenerator;
import ch.ethz.dcg.jukefox.model.collection.BaseAlbum;
import ch.ethz.dcg.jukefox.model.collection.BaseArtist;
import ch.ethz.dcg.jukefox.model.collection.BaseSong;
import ch.ethz.dcg.jukefox.model.collection.CompleteTag;
import ch.ethz.dcg.jukefox.model.collection.DateTag;
import ch.ethz.dcg.jukefox.model.collection.PlaylistSong;
import ch.ethz.dcg.jukefox.model.collection.PlaylistSong.SongSource;
import ch.ethz.dcg.jukefox.model.player.PlayerState;
import ch.ethz.dcg.jukefox.model.providers.SongProvider;
import ch.ethz.dcg.jukefox.model.providers.TagProvider;
import ch.ethz.dcg.jukefox.model.rating.RatingHelper;

public class PlayLog implements IOnPlayerStateChangeListener {

	private final static String TAG = PlayLog.class.getSimpleName();

	private final IContextProvider contextProvider;
	private final int playerModelId;

	private final SongProvider songProvider;
	private final TagProvider tagProvider;
	private final TagPlaylistGenerator tagPlaylistGenerator;
	private final IDbDataPortal dbDataPortal;
	private final ModelSettingsManager modelSettingsManager;
	private final RatingHelper ratingHelper;

	private int lastReturnedLogId;

	private IReadOnlyPlayerController playerController;

	/**
	 * Creates a new instance of {@link PlayLog}
	 */
	public PlayLog(IContextProvider contextProvider, int playerModelId, SongProvider songProvider,
			TagProvider tagProvider, TagPlaylistGenerator tagPlaylistGenerator, IDbDataPortal dbDataPortal,
			ModelSettingsManager modelSettingsManager, RatingHelper ratingHelper) {
		this.contextProvider = contextProvider;
		this.playerModelId = playerModelId;
		this.songProvider = songProvider;
		this.tagProvider = tagProvider;
		this.modelSettingsManager = modelSettingsManager;
		this.tagPlaylistGenerator = tagPlaylistGenerator;
		this.dbDataPortal = dbDataPortal;
		this.ratingHelper = ratingHelper;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * ch.ethz.dcg.jukefox.model.player.playlog.asdfasdf#writeToPlayLogAsync
	 * (int, java.util.Date, ch.ethz.dcg.jukefox.model.collection.PlaylistSong,
	 * boolean, int)
	 */
	public void writeToPlayLogAsync(final Date time, final PlaylistSong<BaseArtist, BaseAlbum> song,
			final boolean skip, final int playbackPosition) {

		new Thread(new Runnable() {

			@Override
			public void run() {
				writeToPlayLog(time, song, skip, playbackPosition);
			}
		}).start();
	}

	public void writeToPlayLog(final Date time, final PlaylistSong<BaseArtist, BaseAlbum> song,
			final boolean skip, final int playbackPosition) {
		int day_of_week = 0, hour_of_day = 0;
		long utcTime = time.getTime();

		Calendar cal = Calendar.getInstance();

		// in minutes
		int timeZoneOffset = (cal.get(Calendar.ZONE_OFFSET) + cal.get(Calendar.DST_OFFSET)) / 60000;
		// in hours
		timeZoneOffset /= 60;

		Calendar logcalendar = Calendar.getInstance();
		logcalendar.setTimeInMillis(time.getTime());
		day_of_week = logcalendar.get(Calendar.DAY_OF_WEEK);
		hour_of_day = logcalendar.get(Calendar.HOUR_OF_DAY);
		Log.v(TAG, "day of week: " + day_of_week);
		Log.v(TAG, "hour of day: " + hour_of_day);
		Log.v(TAG, "utcTime: " + utcTime);

		AbstractContextResult contextData = contextProvider.getMeanContextValues(30 * 1000);

		try {
			// Write playlog entry
			dbDataPortal.writePlayLogEntry(playerModelId, song, utcTime, timeZoneOffset, day_of_week, hour_of_day,
					skip, playerController.getPlayMode().getPlayModeType().value(), contextData, playbackPosition);

			// Write a rating entry as well
			double fractionPlayed = playbackPosition / (double) song.getDuration();
			ratingHelper.addRatingFromPlayLog(song, time, fractionPlayed);
		} catch (DataWriteException e) {
			Log.w(TAG, e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * ch.ethz.dcg.jukefox.model.player.playlog.asdfasdf#setSentSuccessful()
	 */
	public void setSentSuccessful() {
		Log.v(TAG, "Sending log succeded: Saving last Sent Id");
		modelSettingsManager.setLastSentPlayLogId(lastReturnedLogId);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ch.ethz.dcg.jukefox.model.player.playlog.asdfasdf#sendPlayLog()
	 */
	public void sendPlayLog() {
		if (!modelSettingsManager.isHelpImproveJukefox()) {
			return;
		}
		JoinableThread t = new JoinableThread(new Runnable() {

			@Override
			public void run() {

				// Log.v(TAG, "SendPlaylog");

				int coordinateVersion = modelSettingsManager.getCoordinateVersion();
				long lastSentId = modelSettingsManager.getLastSentPlayLogId();
				PlayLogSendEntity logEntity = null;
				try {
					logEntity = dbDataPortal.getPlayLogString(playerModelId, Constants.PLAY_LOG_VERSION,
							coordinateVersion, lastSentId);
				} catch (DataUnavailableException e) {
					Log.w(TAG, e);
				}

				if (logEntity == null) {
					return;
				}

				String log = logEntity.logString;
				lastReturnedLogId = logEntity.lastId;
				// Log.v(TAG, log);

				if (log == null || log.equals("")) {
					return;
				}
				String answer = null;

				try {

					// Create a new HttpClient and Post Header
					HttpClient httpClient = HttpHelper.createHttpClientWithDefaultSettings();
					HttpPost httpPost = new HttpPost(Constants.FORMAT_PLAY_LOG_URL);
					httpPost.setHeader(HTTP.CONTENT_TYPE, "application/x-www-form-urlencoded");

					httpPost.setEntity(new StringEntity("log=" + log));

					// Execute HTTP Post Request
					HttpResponse response = httpClient.execute(httpPost);
					answer = EntityUtils.toString(response.getEntity());
					// Log.v(TAG, "playLog sent, answer: " + answer);
				} catch (Exception e) {
					Log.w(TAG, e);
					answer = null;
				}

				if (answer != null) {
					Log.v(TAG, "Play Log Server answer: " + answer);
					if (answer.equals("1")) {
						setSentSuccessful();
					}
				}
			}

		});
		t.start();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ch.ethz.dcg.jukefox.model.player.playlog.asdfasdf#
	 * getArbitrarySongForDateRange(long, long)
	 */
	public PlaylistSong<BaseArtist, BaseAlbum> getArbitrarySongForDateRange(long fromTimestamp, long toTimestamp)
			throws DataUnavailableException {
		return new PlaylistSong<BaseArtist, BaseAlbum>(songProvider.getArbitraryBaseSongInTimeRange(playerModelId,
				fromTimestamp, toTimestamp), SongSource.TIME_BASED);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * ch.ethz.dcg.jukefox.model.player.playlog.asdfasdf#getSongCloseToDateRange
	 * (long, long, float, float)
	 */
	public PlaylistSong<BaseArtist, BaseAlbum> getSongCloseToDateRange(long fromTimestamp, long toTimestamp,
			float toleranceRange, float toleranceGlobal) throws DataUnavailableException {
		return new PlaylistSong<BaseArtist, BaseAlbum>(songProvider.getBaseSongCloseToTimeRange(playerModelId,
				fromTimestamp, toTimestamp, toleranceRange, toleranceGlobal), SongSource.TIME_BASED);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * ch.ethz.dcg.jukefox.model.player.playlog.asdfasdf#getSongsForDateRange
	 * (long, long, int)
	 */
	public List<PlaylistSong<BaseArtist, BaseAlbum>> getSongsForDateRange(long fromTimestamp, long toTimestamp,
			int number) {
		return songProvider.getPlaylistSongsForTimeRange(playerModelId, fromTimestamp, toTimestamp, number);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * ch.ethz.dcg.jukefox.model.player.playlog.asdfasdf#getSongByDateTag(long,
	 * long)
	 */
	public List<PlaylistSong<BaseArtist, BaseAlbum>> getSongByDateTag(long fromTimestamp, long toTimestamp)
			throws DataUnavailableException {
		ArrayList<DateTag> sortedDateTags = tagProvider.getSortedDateTags();
		long avgTime = (fromTimestamp + toTimestamp) / 2;
		DateTag bestTag = null;
		for (DateTag t : sortedDateTags) {
			if (toTimestamp < t.getFrom()) {
				continue;
			}
			if (fromTimestamp > t.getTo()) {
				continue;
			}
			// we have at least some overlap...
			if (bestTag == null) {
				bestTag = t;
				continue;
			}
			long bestDiff = Math.abs(bestTag.getTime() - avgTime);
			long curDiff = Math.abs(t.getTime() - avgTime);
			if (curDiff < bestDiff) {
				bestTag = t;
				continue;
			}
			if (curDiff == bestDiff && t.getRange() < bestTag.getRange()) {
				bestTag = t;
			}
		}
		if (bestTag != null) {
			return returnSongsForDateTag(bestTag);
		}

		// no tag overlaps with our time range... just take the one with best
		// mean-time fit...
		DateTag relevantDate = new DateTag();
		relevantDate.setFrom(fromTimestamp);
		relevantDate.setTo(toTimestamp);
		int idx = Collections.binarySearch(sortedDateTags, relevantDate, new Comparator<DateTag>() {

			@Override
			public int compare(DateTag t1, DateTag t2) {
				if (t1.getTime() < t2.getTime()) {
					return -1;
				}
				if (t1.getTime() > t2.getTime()) {
					return 1;
				}
				return 0;
			}
		});
		if (idx > 0) {
			DateTag tag = sortedDateTags.get(idx);
			return returnSongsForDateTag(tag);
		}
		idx = Math.min(-idx - 1, sortedDateTags.size() - 1);
		DateTag tag = sortedDateTags.get(idx);
		return returnSongsForDateTag(tag);
	}

	private List<PlaylistSong<BaseArtist, BaseAlbum>> returnSongsForDateTag(DateTag bestTag)
			throws DataUnavailableException {
		CompleteTag tag = tagProvider.getCompleteTag(bestTag.getId());
		Log.v(TAG, "getting songs for tag: " + tag.getName());
		return tagPlaylistGenerator.generatePlaylist(tag, TagPlaylistGenerator.DEFAULT_PLAYLIST_SIZE,
				TagPlaylistGenerator.DEFAULT_SAMPLE_FACTOR);
	}

	/**
	 * Is the given {@link BaseSong} in the recent history?
	 * 
	 * @param candidate
	 *            The {@link BaseSong}
	 * @param equalSongAvoidanceNumber
	 * @return
	 */
	public boolean isSongInRecentHistory(BaseSong<BaseArtist, BaseAlbum> candidate, int equalSongAvoidanceNumber) {
		boolean result = false;
		try {
			result = dbDataPortal.isSongInRecentHistory(playerModelId, candidate, equalSongAvoidanceNumber);
		} catch (DataUnavailableException e) {
			Log.w(TAG, e);
		}
		return result;
	}

	/**
	 * 
	 * @param artist
	 * @param similarArtistAvoidanceNumber
	 * @return
	 */
	public boolean isArtistInRecentHistory(BaseArtist baseArtist, int similarArtistAvoidanceNumber) {
		boolean result = false;
		try {
			result = dbDataPortal.isArtistInRecentHistory(playerModelId, baseArtist, similarArtistAvoidanceNumber);
		} catch (DataUnavailableException e) {
			Log.w(TAG, e);
		}
		return result;
	}

	public void setPlayerController(IReadOnlyPlayerController playerController) {
		this.playerController = playerController;
		playerController.addOnPlayerStateChangeListener(this);
	}

	@Override
	public void onPlayerStateChanged(PlayerState playerState) {
	}

	@Override
	public void onSongCompleted(PlaylistSong<BaseArtist, BaseAlbum> song) {
		writeToPlayLogAsync(new Date(), song, false, song.getDuration());
	}

	@Override
	public void onSongSkipped(PlaylistSong<BaseArtist, BaseAlbum> song, int position) {
		writeToPlayLogAsync(new Date(), song, true, position);
	}

	@Override
	public void onSongStarted(PlaylistSong<BaseArtist, BaseAlbum> song) {
	}

}
