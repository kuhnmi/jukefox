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
package ch.ethz.dcg.pancho3.commons.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.preference.PreferenceManager;
import ch.ethz.dcg.jukefox.commons.AndroidConstants;
import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.pancho3.R;

public class SharedPreferencesKeys {

	private final static String TAG = SharedPreferencesKeys.class.getSimpleName();

	// TODO: A prefs file is also defined in PanchoConsts
	// public final static String PANCHO_SHARED_PREFS = "PanchoSharedPrefs";
	private static final String OLD_PREFS_FILE = "panchoprefs";

	public final static String OLD_KEY_IS_UPDATING = "IsUpdating";
	public final static String OLD_KEY_IS_DATA_DISTORTED = "IsDataDistorted";
	public final static String OLD_KEY_UPDATE_SUCCESSFUL = "successful update";

	public final static String OLD_KEY_COORD_VERSION = "coords version";
	public final static String OLD_KEY_PLAY_LOG_DB_VERSION = "play log db version";
	public final static String OLD_KEY_LAST_SENT_PLAY_LOG_ID = "last sent play log id";
	public final static String OLD_KEY_RANDOM_USER_HASH = "random user hash";

	public final static String OLD_KEY_GAPLESS_OFFSET = "gapless offset";
	public final static String OLD_KEY_APP_VERSION = "version";
	public final static String OLD_KEY_FULL_SCREEN = "fullscreen";
	public final static String OLD_KEY_FILE_LOCATIONS = "file locations";
	public final static String OLD_KEY_TRACKS_SORTED = "sorted tracks";
	public final static String OLD_KEY_STARTED_UPDATES_CNT = "started updates";
	public final static String OLD_KEY_SUCCESSFUL_UPDATES_CNT = "successful updates";

	public final static String OLD_KEY_USE_COVER_FILES = "use cover files";
	public final static String OLD_KEY_LOGGING = "logging";
	public final static String OLD_KEY_LOG_START_TIME = "log start";
	public final static String OLD_KEY_PLAYLIST_POS = "playlist position";
	public final static String OLD_KEY_POS_IN_SONG = "pos in song";
	public final static String OLD_KEY_LAST_PLAY_MODE = "last play mode";
	public final static String OLD_KEY_AVOID_ARTISTS = "avoid artists";

	public final static String OLD_KEY_SHOW_ZOOM_BAR = "show zoom bar";
	public final static String OLD_KEY_RESUME_ON_HEADSET = "resume on headset";
	public final static String OLD_KEY_HAPTIC_FEEDBACK = "haptic feedback";
	public final static String OLD_KEY_GOTO_CUR_ALBUM = "goto cur album";
	public final static String OLD_KEY_HIGHLIGHT_CUR_ALBUM = "highlight current album";

	public final static String OLD_KEY_AUTO_SHOW_COVER = "auto show cover";
	public final static String OLD_KEY_DRAW_FLOOR = "draw floor";
	public final static String OLD_KEY_SHOW_TIPS = "show tipps";
	public final static String OLD_KEY_ICON_LISTS = "icon lists";
	public final static String OLD_KEY_LOCK_ORIENTATION = "lock orientation";

	public final static String OLD_KEY_ALWAYS_NOTIFY = "always notify"; // RENAMED
																		// TO
																		// SHOW_NOTIFICATIONS
	public final static String OLD_KEY_GAPLESS = "gapless";
	public final static String OLD_KEY_SCROBBLE = "scrobble";
	public final static String OLD_KEY_SCROBBLE_PAUSED = "scrobble paused";
	public final static String OLD_KEY_SCROBBLE_NUM_TRACKS = "scrobble numTracks"; // RENAMED
																					// TO
																					// SCROBBLE_INTERVAL
	public final static String OLD_KEY_SCROBBLE_USERNAME = "scrobble username";
	public final static String OLD_KEY_SCROBBLE_PWD = "scrobble password";
	public final static String OLD_KEY_LOG_LAST_ASK_TIME = "last log ask";
	public final static String OLD_KEY_LOG_CNT = "num times log";

	public final static String OLD_KEY_IMPROVE_Jukefox = "improve museek";

	// public final static String KEY_IS_UPDATING = "IsUpdating";
	// public final static String KEY_IS_DATA_DISTORTED = "IsDataDistorted";
	// public final static String KEY_UPDATE_SUCCESSFUL = "successfulUpdate";
	//
	// public final static String KEY_COORD_VERSION = "coordsVersion";
	// public final static String KEY_PLAY_LOG_DB_VERSION = "playLogDbVersion";
	// public final static String KEY_LAST_SENT_PLAY_LOG_ID =
	// "lastSentPlayLogId";
	// public final static String KEY_RANDOM_USER_HASH = "randomUserHash";
	//	
	// public final static String KEY_GAPLESS_OFFSET = "gaplessOffset";
	// public final static String KEY_SIMILAR_MODE_ARTIST_AVOIDANCE_NUMBER =
	// "similarModeArtistAvoidanceNumber";
	// public final static String KEY_APP_VERSION = "version";
	// public final static String KEY_FULL_SCREEN = "fullscreen";
	// public final static String KEY_FILE_LOCATIONS = "fileLocations";
	// public final static String KEY_TRACKS_SORTED = "sortedTracks";
	// public final static String KEY_STARTED_UPDATES_CNT = "startedUpdates";
	// public final static String KEY_SUCCESSFUL_UPDATES_CNT =
	// "successfulUpdates";
	//	
	// public final static String KEY_USE_COVER_FILES = "useCoverFiles";
	// public final static String KEY_LOGGING = "logging";
	// public final static String KEY_LOG_START_TIME = "logStartTime";
	// public final static String KEY_PLAYLIST_POS = "playlistPosition";
	// public final static String KEY_POS_IN_SONG = "posInSong";
	// public final static String KEY_LAST_PLAY_MODE = "lastPlayMode";
	//	
	// public final static String KEY_SHOW_ZOOM_BAR = "showZoomBar";
	// public final static String KEY_RESUME_ON_HEADSET_PLUG =
	// "resumeOnHeadsetPlug";
	// public final static String KEY_KINETIC_MOVEMENT = "kineticMovement";
	// public final static String KEY_HAPTIC_FEEDBACK = "vibrationFeedback";
	// public final static String KEY_GOTO_CUR_ALBUM = "goToCurrentSongInMap";
	// public final static String KEY_HIGHLIGHT_CUR_ALBUM =
	// "highlightCurrentSongInMap";
	//	
	// public final static String KEY_AUTO_SHOW_COVER = "autoShowAlbumArt";
	// public final static String KEY_DRAW_FLOOR = "drawFloor";
	// public final static String KEY_SHOW_TIPS = "showTips";
	// public final static String KEY_ICON_LISTS = "showIconLists";
	// public final static String KEY_LOCK_ORIENTATION =
	// "lockScreenOrientation";
	//	
	// public final static String KEY_SHOW_NOTIFICATIONS = "showNotifications";
	// public final static String KEY_GAPLESS = "gaplessPlaybackEnabled";
	// public final static String KEY_SCROBBLE_ENABLED = "scrobbleEnabled";
	// public final static String KEY_SCROBBLE_PAUSED = "scrobblePaused";
	// public final static String KEY_SCROBBLE_INTERVAL = "scrobbleInterval";
	// public final static String KEY_SCROBBLE_USERNAME = "lastFmUserName";
	// public final static String KEY_SCROBBLE_PWD = "lastFmPassword";
	// public final static String KEY_LOG_LAST_ASK_TIME = "lastLogAsk";
	// public final static String KEY_LOG_CNT = "numTimesLog";
	//	
	// public final static String KEY_IMPROVE_MUSEEK = "improveMuseek";
	//	
	// public final static String KEY_IS_FIRST_START = "isFirstStart";
	// public final static String KEY_IS_COMMITTING_SERVER_DATA =
	// "isCommittingServerData";
	// public final static String KEY_NEEDS_RECOMPUTE = "needsRecompute";
	//
	// public static final String KEY_SEARCH_WHILE_TYPING = "searchWhileTyping";
	//
	// public static final String KEY_FAMOUS_ARTISTS_INSERTED =
	// "famousArtistsInserted";
	//
	// public static final String KEY_LASTPOS_IN_PCA_X = "lastPositionInPcaX";
	// public static final String KEY_LASTPOS_IN_PCA_Y = "lastPositionInPcaY";
	//
	// public static final String KEY_SHARED_PREF_VERSION = "sharedPrefVersion";

	public static void updateSharedPrefs(Context ctx) {

		SharedPreferences newPrefs = PreferenceManager.getDefaultSharedPreferences(ctx);

		SharedPreferences oldPrefs = ctx.getSharedPreferences(OLD_PREFS_FILE, Context.MODE_PRIVATE);
		// SharedPreferences oldPrefs = PreferenceManager
		// .getDefaultSharedPreferences(ctx);

		SharedPreferences.Editor editor = newPrefs.edit();

		// updateKey(OLD_KEY_IS_UPDATING, KEY_IS_UPDATING, oldPrefs, editor);
		// updateKey(OLD_KEY_IS_DATA_DISTORTED, KEY_IS_DATA_DISTORTED, oldPrefs,
		// editor);
		// updateKey(OLD_KEY_UPDATE_SUCCESSFUL, KEY_UPDATE_SUCCESSFUL, oldPrefs,
		// editor);

		updateKeyInt(OLD_KEY_COORD_VERSION, ctx.getString(R.string.KEY_COORD_VERSION), oldPrefs, editor);

		// TODO: not required as we do not copy the playlog table...:
		// updateKeyInt(OLD_KEY_PLAY_LOG_DB_VERSION,
		// ctx.getString(R.string.KEY_PLAY_LOG_DB_VERSION), oldPrefs, editor);
		// updateKeyLong(OLD_KEY_LAST_SENT_PLAY_LOG_ID,
		// ctx.getString(R.string.KEY_LAST_SENT_PLAY_LOG_ID), oldPrefs, editor);

		updateKeyLong(OLD_KEY_RANDOM_USER_HASH, ctx.getString(R.string.KEY_RANDOM_USER_HASH), oldPrefs, editor);

		updateKeyInt(OLD_KEY_GAPLESS_OFFSET, ctx.getString(R.string.KEY_GAPLESS_OFFSET), oldPrefs, editor);
		updateKeyString(OLD_KEY_APP_VERSION, ctx.getString(R.string.KEY_APP_VERSION), oldPrefs, editor);
		// updateKey(OLD_KEY_FULL_SCREEN, KEY_FULL_SCREEN, oldPrefs, editor);
		// updateKey(OLD_KEY_FILE_LOCATIONS, KEY_FILE_LOCATIONS, oldPrefs,
		// editor);
		// updateKey(OLD_KEY_TRACKS_SORTED, KEY_TRACKS_SORTED, oldPrefs,
		// editor);
		updateKeyInt(OLD_KEY_STARTED_UPDATES_CNT, ctx.getString(R.string.KEY_NUMBER_OF_STARTED_IMPORTS), oldPrefs,
				editor);
		updateKeyInt(OLD_KEY_SUCCESSFUL_UPDATES_CNT, ctx.getString(R.string.KEY_NUMBER_OF_COMPLETED_IMPORTS), oldPrefs,
				editor);

		updateKeyBoolean(OLD_KEY_USE_COVER_FILES, ctx.getString(R.string.KEY_USE_COVER_FILES), oldPrefs, editor);
		updateKeyInt(OLD_KEY_LOGGING, ctx.getString(R.string.KEY_LOGGING), oldPrefs, editor);
		updateKeyLong(OLD_KEY_LOG_START_TIME, ctx.getString(R.string.KEY_LOG_START_TIME), oldPrefs, editor);
		// updateKey(OLD_KEY_PLAYLIST_POS, KEY_PLAYLIST_POS, oldPrefs, editor);
		// updateKey(OLD_KEY_POS_IN_SONG, KEY_POS_IN_SONG, oldPrefs, editor);
		// updateKey(OLD_KEY_LAST_PLAY_MODE, KEY_LAST_PLAY_MODE, oldPrefs,
		// editor);
		updateKeyInt(OLD_KEY_AVOID_ARTISTS, ctx.getString(R.string.KEY_SIMILAR_MODE_ARTIST_AVOIDANCE_NUMBER), oldPrefs,
				editor);

		updateKeyBoolean(OLD_KEY_SHOW_ZOOM_BAR, ctx.getString(R.string.KEY_SHOW_ZOOM_BAR), oldPrefs, editor);
		updateKeyBoolean(OLD_KEY_RESUME_ON_HEADSET, ctx.getString(R.string.KEY_RESUME_ON_HEADSET_PLUG), oldPrefs,
				editor);
		updateKeyBoolean(OLD_KEY_HAPTIC_FEEDBACK, ctx.getString(R.string.KEY_HAPTIC_FEEDBACK), oldPrefs, editor);
		updateKeyBoolean(OLD_KEY_GOTO_CUR_ALBUM, ctx.getString(R.string.KEY_GOTO_CUR_ALBUM), oldPrefs, editor);
		updateKeyBoolean(OLD_KEY_HIGHLIGHT_CUR_ALBUM, ctx.getString(R.string.KEY_HIGHLIGHT_CUR_ALBUM), oldPrefs, editor);

		updateKeyBoolean(OLD_KEY_AUTO_SHOW_COVER, ctx.getString(R.string.KEY_AUTO_SHOW_COVER), oldPrefs, editor);
		// updateKey(OLD_KEY_DRAW_FLOOR, KEY_DRAW_FLOOR, oldPrefs, editor);
		updateKeyBoolean(OLD_KEY_SHOW_TIPS, ctx.getString(R.string.KEY_SHOW_TIPS), oldPrefs, editor);
		updateKeyBoolean(OLD_KEY_ICON_LISTS, ctx.getString(R.string.KEY_ICON_LISTS), oldPrefs, editor);
		// updateKeyInt(OLD_KEY_LOCK_ORIENTATION, KEY_LOCK_ORIENTATION,
		// oldPrefs, editor);

		updateKeyBoolean(OLD_KEY_ALWAYS_NOTIFY, ctx.getString(R.string.KEY_SHOW_NOTIFICATIONS), oldPrefs, editor); // RENAMED
																													// TO
																													// SHOW_NOTIFICATIONS
		updateKeyBoolean(OLD_KEY_GAPLESS, ctx.getString(R.string.KEY_GAPLESS), oldPrefs, editor);
		updateKeyBoolean(OLD_KEY_SCROBBLE, ctx.getString(R.string.KEY_SCROBBLE_ENABLED), oldPrefs, editor);
		updateKeyBoolean(OLD_KEY_SCROBBLE_PAUSED, ctx.getString(R.string.KEY_SCROBBLE_PAUSED), oldPrefs, editor);
		updateKeyInt(OLD_KEY_SCROBBLE_NUM_TRACKS, ctx.getString(R.string.KEY_SCROBBLE_INTERVAL), oldPrefs, editor); // RENAMED
																													// TO
																													// SCROBBLE_INTERVAL
		updateKeyString(OLD_KEY_SCROBBLE_USERNAME, ctx.getString(R.string.KEY_SCROBBLE_USERNAME), oldPrefs, editor);
		updateKeyString(OLD_KEY_SCROBBLE_PWD, ctx.getString(R.string.KEY_SCROBBLE_PWD), oldPrefs, editor);
		// updateKey(OLD_KEY_LOG_LAST_ASK_TIME, KEY_LOG_LAST_ASK_TIME, oldPrefs,
		// editor);
		// updateKey(OLD_KEY_LOG_CNT, KEY_LOG_CNT, oldPrefs, editor);

		updateKeyBoolean(OLD_KEY_IMPROVE_Jukefox, ctx.getString(R.string.KEY_GATHER_USAGE_DATA), oldPrefs, editor);

		if (oldPrefs.getInt(OLD_KEY_SUCCESSFUL_UPDATES_CNT, 0) > 0) {
			editor.putBoolean(ctx.getString(R.string.KEY_HAS_SENT_DOWNLOAD_STATS), true);
		}

		String orientation = oldPrefs.getString(OLD_KEY_LOCK_ORIENTATION, null);
		if (orientation != null) {
			if (orientation.equals("no")) {
				editor.putInt(ctx.getString(R.string.KEY_SCREEN_ORIENTATION),
						ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
			} else if (orientation.equals("portrait")) {
				editor.putInt(ctx.getString(R.string.KEY_SCREEN_ORIENTATION), ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
			} else if (orientation.equals("landscape")) {
				editor
						.putInt(ctx.getString(R.string.KEY_SCREEN_ORIENTATION),
								ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
			}
		}

		editor.putInt(ctx.getString(R.string.KEY_SHARED_PREF_VERSION), AndroidConstants.SHARED_PREF_VERSION);

		try {
			Editor oldEdit = oldPrefs.edit();
			oldEdit.clear();
			boolean b = oldEdit.commit();

			if (!b) {
				Log.v(TAG, "could not clear old prefs");
			} else {
				Log.v(TAG, "successfully cleared old prefs");
			}
		} catch (Exception e) {
			Log.w(TAG, e);
		}

	}

	private static void updateKeyInt(String oldKey, String newKey, SharedPreferences oldPrefs, Editor editor) {
		if (!oldPrefs.contains(oldKey)) {
			return;
		}
		int i = oldPrefs.getInt(oldKey, -1); // should never be set to the
												// default value
		editor.putInt(newKey, i);
	}

	private static void updateKeyBoolean(String oldKey, String newKey, SharedPreferences oldPrefs, Editor editor) {
		if (!oldPrefs.contains(oldKey)) {
			return;
		}
		boolean b = oldPrefs.getBoolean(oldKey, false); // should never be set
														// to the default value
		editor.putBoolean(newKey, b);
	}

	private static void updateKeyString(String oldKey, String newKey, SharedPreferences oldPrefs, Editor editor) {
		if (!oldPrefs.contains(oldKey)) {
			return;
		}
		String s = oldPrefs.getString(oldKey, ""); // should never be set to the
													// default value
		editor.putString(newKey, s);
	}

	private static void updateKeyLong(String oldKey, String newKey, SharedPreferences oldPrefs, Editor editor) {
		if (!oldPrefs.contains(oldKey)) {
			return;
		}
		Long l = oldPrefs.getLong(oldKey, 0); // should never be set to the
												// default value
		editor.putLong(newKey, l);
	}

	public static boolean hasOldVersionKeys(Context ctx) {
		SharedPreferences oldPrefs = ctx.getSharedPreferences(OLD_PREFS_FILE, Context.MODE_PRIVATE);

		if (oldPrefs.contains(SharedPreferencesKeys.OLD_KEY_APP_VERSION)) {
			Log.v(TAG, "contains OLD_KEY_APP_VERSION");
			return true;
		}
		if (oldPrefs.contains(SharedPreferencesKeys.OLD_KEY_STARTED_UPDATES_CNT)) {
			Log.v(TAG, "contains OLD_KEY_STARTED_UPDATES_CNT");
			return true;
		}
		Log.v(TAG, "no old pref keys found.");
		return false;
	}

}
