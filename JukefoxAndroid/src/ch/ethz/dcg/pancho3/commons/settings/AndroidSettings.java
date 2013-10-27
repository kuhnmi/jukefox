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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.ActivityInfo;
import android.preference.PreferenceManager;
import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.pancho3.R;

public class AndroidSettings implements ISettingsEditor, ISettingsReader, OnSharedPreferenceChangeListener {

	private final static String TAG = AndroidSettings.class.getSimpleName();

	public enum FbSendInterval {
		WEEKLY, DAILY
	}

	public enum ScrobblingType {
		NONE(1), SCROBBLE_DROID(2), INTERNAL(3);

		private final int value;

		private ScrobblingType(int value) {
			this.value = value;
		}

		public int getValue() {
			return value;
		}
	}

	private Context ctx;
	private SharedPreferences preferences;
	private SharedPreferences.Editor editor;
	private List<SharedPreferences.OnSharedPreferenceChangeListener> changeListeners;

	public AndroidSettings(Context ctx) {
		this.ctx = ctx;
		preferences = PreferenceManager.getDefaultSharedPreferences(ctx);
		editor = preferences.edit();
		changeListeners = new ArrayList<SharedPreferences.OnSharedPreferenceChangeListener>();
		preferences.registerOnSharedPreferenceChangeListener(this);
		addLogFileEnabledSettingListener();
	}

	private void addLogFileEnabledSettingListener() {
		this.addSettingsChangeListener(new OnSharedPreferenceChangeListener() {

			@Override
			public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
				if (!ctx.getString(R.string.KEY_ENABLE_LOG_FILE).equals(key)) {
					return;
				}
				Log.setLogToFile(isLogFileEnabled());
			}
		});
	}

	@Override
	public boolean isFullScreen() {
		return false;
	}

	@Override
	public boolean isGapless() {
		return preferences.getBoolean(ctx.getString(R.string.KEY_GAPLESS), Boolean.parseBoolean(ctx
				.getString(R.string.DEFAULT_GAPLESS)));
	}

	@Override
	public int getGaplessGapRemoveTime() {
		return preferences.getInt(ctx.getString(R.string.KEY_GAPLESS_OFFSET), Integer.parseInt(ctx
				.getString(R.string.DEFAULT_GAPLESS_OFFSET)));
	}

	@Override
	public void setGaplessGapRemoveTime(int time) {
		editor.putInt(ctx.getString(R.string.KEY_GAPLESS_OFFSET), time);
	}

	@Override
	public boolean isFirstStart() {
		return preferences.getBoolean(ctx.getString(R.string.KEY_IS_FIRST_START), true);
	}

	@Override
	public String getLastFmPassword() {
		return preferences.getString(ctx.getString(R.string.KEY_SCROBBLE_PWD), "");
	}

	@Override
	public String getLastFmUserName() {
		return preferences.getString(ctx.getString(R.string.KEY_SCROBBLE_USERNAME), "");
	}

	@Override
	public int getScrobbleInterval() {
		return preferences.getInt(ctx.getString(R.string.KEY_SCROBBLE_INTERVAL), Integer.parseInt(ctx
				.getString(R.string.DEFAULT_SCROBBLE_INTERVAL)));
	}

	@Override
	public boolean isScrobblingPaused() {
		return preferences.getBoolean(ctx.getString(R.string.KEY_SCROBBLE_PAUSED), Boolean.parseBoolean(ctx
				.getString(R.string.DEFAULT_SCROBBLE_PAUSED)));
	}

	@Override
	public boolean isScrobblingEnabled() {
		return preferences.getBoolean(ctx.getString(R.string.KEY_SCROBBLE_ENABLED), Boolean.parseBoolean(ctx
				.getString(R.string.DEFAULT_SCROBBLE_ENABLED)));
	}

	@Override
	public boolean areNotificationsShown() {
		return preferences.getBoolean(ctx.getString(R.string.KEY_SHOW_NOTIFICATIONS), Boolean.parseBoolean(ctx
				.getString(R.string.DEFAULT_SHOW_NOTIFICATIONS)));
	}

	@Override
	public boolean isCurrentAlbumHighlighted() {
		return preferences.getBoolean(ctx.getString(R.string.KEY_HIGHLIGHT_CUR_ALBUM), Boolean.parseBoolean(ctx
				.getString(R.string.DEFAULT_HIGHLIGHT_CUR_ALBUM)));
	}

	@Override
	public void setFirstStart(boolean b) {
		editor.putBoolean(ctx.getString(R.string.KEY_IS_FIRST_START), b);
		editor.commit();
	}

	@Override
	public boolean isAutomaticallyShowCover() {
		return preferences.getBoolean(ctx.getString(R.string.KEY_AUTO_SHOW_COVER), Boolean.parseBoolean(ctx
				.getString(R.string.DEFAULT_AUTO_SHOW_COVER)));
	}

	@Override
	public boolean isAutomaticallyResumeOnHeadsetPlugged() {
		return preferences.getBoolean(ctx.getString(R.string.KEY_RESUME_ON_HEADSET_PLUG), Boolean.parseBoolean(ctx
				.getString(R.string.DEFAULT_RESUME_ON_HEADSET_PLUG)));
	}

	@Override
	public boolean isCommittingServerData() {
		return preferences.getBoolean(ctx.getString(R.string.KEY_IS_COMMITTING_SERVER_DATA), false);
	}

	@Override
	public void setCommittingServerData(boolean b) {
		editor.putBoolean(ctx.getString(R.string.KEY_IS_COMMITTING_SERVER_DATA), b);
		editor.commit();
	}

	@Override
	public void setRandomUserHash(Long randomNr) {
		editor.putLong(ctx.getString(R.string.KEY_RANDOM_USER_HASH), randomNr);
		editor.commit();
	}

	@Override
	public Long getRandomUserHash() {
		if (!preferences.contains(ctx.getString(R.string.KEY_RANDOM_USER_HASH))) {
			return null;
		}
		return preferences.getLong(ctx.getString(R.string.KEY_RANDOM_USER_HASH), 0L);
	}

	@Override
	public int getSimilarArtistAvoidanceNumber() {
		return preferences.getInt(ctx.getString(R.string.KEY_SIMILAR_MODE_ARTIST_AVOIDANCE_NUMBER), 0);
	}

	@Override
	public boolean isHapticFeedback() {
		return preferences.getBoolean(ctx.getString(R.string.KEY_HAPTIC_FEEDBACK), Boolean.parseBoolean(ctx
				.getString(R.string.DEFAULT_HAPTIC_FEEDBACK)));
	}

	@Override
	public void setSimilarArtistAvoidanceNumber(int numberOfSubsequentArtists) {
		editor.putInt(ctx.getString(R.string.KEY_SIMILAR_MODE_ARTIST_AVOIDANCE_NUMBER), numberOfSubsequentArtists);
		editor.commit();
	}

	@Override
	public boolean isUseIconLists() {
		return preferences.getBoolean(ctx.getString(R.string.KEY_ICON_LISTS), Boolean.parseBoolean(ctx
				.getString(R.string.DEFAULT_ICON_LISTS)));
	}

	@Override
	public boolean isUseAlbumArtFiles() {
		return preferences.getBoolean(ctx.getString(R.string.KEY_USE_COVER_FILES), true);
	}

	@Override
	public boolean isKineticMovement() {
		return preferences.getBoolean(ctx.getString(R.string.KEY_KINETIC_MOVEMENT), Boolean.parseBoolean(ctx
				.getString(R.string.DEFAULT_KINETIC_MOVEMENT)));
	}

	@Override
	public boolean isSearchWhileTyping() {
		return preferences.getBoolean(ctx.getString(R.string.KEY_SEARCH_WHILE_TYPING), Boolean.parseBoolean(ctx
				.getString(R.string.DEFAULT_SEARCH_WHILE_TYPING)));
	}

	@Override
	public void setLastPositionInPcaMapX(float posX) {
		editor.putFloat(ctx.getString(R.string.KEY_LASTPOS_IN_PCA_X), posX);
		editor.commit();
	}

	@Override
	public void setLastPositionInPcaMapY(float posY) {
		editor.putFloat(ctx.getString(R.string.KEY_LASTPOS_IN_PCA_Y), posY);
		editor.commit();
	}

	@Override
	public float getLastPositionInPcaMapX() {
		return preferences.getFloat(ctx.getString(R.string.KEY_LASTPOS_IN_PCA_X), -1);
	}

	@Override
	public float getLastPositionInPcaMapY() {
		return preferences.getFloat(ctx.getString(R.string.KEY_LASTPOS_IN_PCA_X), -1);
	}

	public int getSharedPreferencesVersion() {
		return preferences.getInt(ctx.getString(R.string.KEY_SHARED_PREF_VERSION), 0);
	}

	@Override
	public void setDontShowAgain(String key, boolean b) {
		Log.v(TAG, "set don't show again for key '" + key + "' to: " + b);
		editor.putBoolean(key, b);
		editor.commit();
	}

	@Override
	public boolean isDontShowAgain(String key) {
		return preferences.getBoolean(key, false);
	}

	@Override
	public void addSettingsChangeListener(OnSharedPreferenceChangeListener listener) {
		changeListeners.add(listener);
	}

	@Override
	public void removeSettingsChangeListener(OnSharedPreferenceChangeListener listener) {
		changeListeners.remove(listener);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		// Log.v(TAG, "Shared Pref changed");
		for (SharedPreferences.OnSharedPreferenceChangeListener listener : changeListeners) {
			listener.onSharedPreferenceChanged(preferences, key);
		}
	}

	@Override
	public boolean isShowZoomBar() {
		return preferences.getBoolean(ctx.getString(R.string.KEY_SHOW_ZOOM_BAR), Boolean.parseBoolean(ctx
				.getString(R.string.DEFAULT_SHOW_ZOOM_BAR)));
	}

	public long getLastSentPlayLogId() {
		return preferences.getInt(ctx.getString(R.string.KEY_LAST_SENT_PLAY_LOG_ID), -1);
	}

	public void setLastSentPlayLogId(int id) {
		editor.putInt(ctx.getString(R.string.KEY_LAST_SENT_PLAY_LOG_ID), id);
		editor.commit();
	}

	@Override
	public boolean isLockScreenControls() {
		return preferences.getBoolean(ctx.getString(R.string.KEY_LOCK_SCREEN_CONTROLS), Boolean.parseBoolean(ctx
				.getString(R.string.DEFAULT_LOCK_SCREEN_CONTROLS)));
	}

	@Override
	public int getPreferredScreenOrientation() {
		return preferences.getInt(ctx.getString(R.string.KEY_SCREEN_ORIENTATION),
				ActivityInfo.SCREEN_ORIENTATION_SENSOR);
	}

	public int getLastApplicationVersion() {
		return preferences.getInt(ctx.getString(R.string.KEY_LAST_APP_VERSION), -1);
	}

	public void setLastApplicationVersion(int currentAppVersion) {
		editor.putInt(ctx.getString(R.string.KEY_LAST_APP_VERSION), currentAppVersion);
		editor.commit();
	}

	public void setCurrentLogFileNumber(int logFileNumber) {
		editor.putInt(ctx.getString(R.string.KEY_CURRENT_LOG_FILE_NR), logFileNumber);
		editor.commit();
	}

	public int getCurrentLogFileNumber() {
		return preferences.getInt(ctx.getString(R.string.KEY_CURRENT_LOG_FILE_NR), 1);
	}

	public void setFbAccessToken(String token) {
		editor.putString(ctx.getString(R.string.KEY_FB_ACCESS_TOKEN), token);
		editor.commit();
	}

	public String getFbAccessToken() {
		return preferences.getString(ctx.getString(R.string.KEY_FB_ACCESS_TOKEN), "");
	}

	public void setFbExpiryDate(long expiryDate) {
		editor.putLong(ctx.getString(R.string.KEY_FB_EXPIRY_DATE), expiryDate);
		editor.commit();
	}

	public long getFbExpiryDate() {
		return preferences.getLong(ctx.getString(R.string.KEY_FB_EXPIRY_DATE), 0);
	}

	public void removeFbAccessToken() {
		editor.remove(ctx.getString(R.string.KEY_FB_ACCESS_TOKEN));
		editor.commit();
	}

	public void removeFbExpiryDate() {
		editor.remove(ctx.getString(R.string.KEY_FB_EXPIRY_DATE));
		editor.commit();
	}

	public boolean isAutoSendToFacebook() {
		return preferences.getBoolean(ctx.getString(R.string.KEY_FACEBOOK_AUTOSEND_ENABLED), false);
	}

	public FbSendInterval getFacbookAutoSendInterval() {
		String interval = preferences.getString(ctx.getString(R.string.KEY_FACEBOOK_AUTOSEND_INTERVAL), ctx
				.getString(R.string.DEFAULT_FACEBOOK_AUTOSEND_INTERVAL));
		if (interval.equals("weekly")) {
			return FbSendInterval.WEEKLY;
		}
		return FbSendInterval.DAILY;
	}

	public void setFbLastSendTime(long collageSendedTime) {
		editor.putLong(ctx.getString(R.string.KEY_FB_LAST_SEND_TIME), collageSendedTime);
		editor.commit();
	}

	public Long getFbLastSendTime() {
		return preferences.getLong(ctx.getString(R.string.KEY_FB_LAST_SEND_TIME), 0);
	}

	public void setEnableLogFile(boolean enableLogFile) {
		editor.putBoolean(ctx.getString(R.string.KEY_ENABLE_LOG_FILE), enableLogFile);
		editor.commit();
	}

	@Override
	public boolean isLogFileEnabled() {
		return preferences.getBoolean(ctx.getString(R.string.KEY_ENABLE_LOG_FILE), Boolean.parseBoolean(ctx
				.getString(R.string.DEFAULT_ENABLE_LOG_FILE)));
	}

	public boolean isGotoCurrentAlbumEnabled() {
		return preferences.getBoolean(ctx.getString(R.string.KEY_GOTO_CUR_ALBUM), Boolean.parseBoolean(ctx
				.getString(R.string.DEFAULT_GOTO_CUR_ALBUM)));
	}

	@Override
	public void setNumberOfCompletedImports(int numberOfCompletedImports) {
		editor.putInt(ctx.getString(R.string.KEY_NUMBER_OF_COMPLETED_IMPORTS), numberOfCompletedImports);
		editor.commit();
	}

	@Override
	public int getNumberOfCompletedImports() {
		return preferences.getInt(ctx.getString(R.string.KEY_NUMBER_OF_COMPLETED_IMPORTS), 0);
	}

	private HashSet<String> parseSemicolonSeparatedStringValues(String csv) throws Exception {
		HashSet<String> ret = new HashSet<String>();
		if (csv == null) {
			return ret;
		}
		char[] characters = csv.toCharArray();
		StringBuilder token = new StringBuilder();
		for (int i = 0; i < characters.length; i++) {
			if (characters[i] == '\\') {
				i++;
				if (characters[i] == '\\') {
					token.append('\\');
				} else if (characters[i] == ';') {
					token.append(';');
				} else {
					throw new Exception("Error parsing string: " + csv);
				}
			} else if (characters[i] == ';') {
				ret.add(token.toString());
				token = new StringBuilder();
			} else {
				token.append(characters[i]);
			}
		}

		// add remainder if there is no trailing semicolon
		if (token.length() != 0) {
			ret.add(token.toString());
		}
		return ret;
	}

	@Override
	public int getAutoGaplessGapRemoveTime() {
		return preferences.getInt(ctx.getString(R.string.KEY_GAPLESS_AUTO_REMOVE_TIME), Integer.parseInt(ctx
				.getString(R.string.DEFAULT_GAPLESS_AUTO_REMOVE_TIME)));
	}

	@Override
	public void setAutoGaplessGapRemoveTime(int currentGapTime) {
		editor.putInt(ctx.getString(R.string.KEY_GAPLESS_AUTO_REMOVE_TIME), currentGapTime);
		editor.commit();
	}

	@Override
	public void setAlbumListPosition(int listPosition) {
		editor.putInt(ctx.getString(R.string.KEY_ALBUM_LIST_POSITION), listPosition);
		editor.commit();
	}

	@Override
	public int getAlbumListPosition() {
		return preferences.getInt(ctx.getString(R.string.KEY_ALBUM_LIST_POSITION), Integer.parseInt(ctx
				.getString(R.string.DEFAULT_ALBUM_LIST_POSITION)));
	}

	@Override
	public void setArtistListPosition(int listPosition) {
		editor.putInt(ctx.getString(R.string.KEY_ARTIST_LIST_POSITION), listPosition);
		editor.commit();
	}

	@Override
	public int getArtistListPosition() {
		return preferences.getInt(ctx.getString(R.string.KEY_ARTIST_LIST_POSITION), Integer.parseInt(ctx
				.getString(R.string.DEFAULT_ARTIST_LIST_POSITION)));
	}

	public boolean isUseWallpaperBackground() {
		return preferences.getBoolean(ctx.getString(R.string.KEY_USE_WALLPAPER_BACKGROUND), Boolean.parseBoolean(ctx
				.getString(R.string.DEFAULT_USE_WALLPAPER_BACKGROUND)));
	}

	public boolean isUseGalleryBackground() {
		return preferences.getBoolean(ctx.getString(R.string.KEY_PICK_BACKGROUND_FROM_GALLERY), Boolean
				.parseBoolean(ctx.getString(R.string.DEFAULT_PICK_BACKGROUND_FROM_GALLERY)));
	}

	public String getGalleryBackgroundPath() {
		return preferences.getString(ctx.getString(R.string.KEY_GALLERY_BACKGROUND_PATH), "");
	}

	public boolean isShakeSkip() {
		return preferences.getBoolean(ctx.getString(R.string.KEY_SHAKE_SKIP), Boolean.parseBoolean(ctx
				.getString(R.string.DEFAULT_SHAKE_SKIP)));
	}

	@Override
	public void setCoverHintCountPlayer(int number) {
		editor.putInt(ctx.getString(R.string.KEY_COVER_HINT_COUNTER_PLAYER), number);
		editor.commit();
	}

	@Override
	public int getCoverHintCountPlayer() {
		return preferences.getInt(ctx.getString(R.string.KEY_COVER_HINT_COUNTER_PLAYER), 0);
	}

	@Override
	public void setCoverHintCountAlbum(int number) {
		editor.putInt(ctx.getString(R.string.KEY_COVER_HINT_COUNTER_ALBUM), number);
		editor.commit();
	}

	@Override
	public int getCoverHintCountAlbum() {
		return preferences.getInt(ctx.getString(R.string.KEY_COVER_HINT_COUNTER_ALBUM), 0);
	}

	@Override
	public float getShakeSkipThreshhold() {
		return preferences.getInt(ctx.getString(R.string.KEY_SHAKE_SKIP_THRESHHOLD), Integer.parseInt(ctx
				.getString(R.string.DEFAULT_SHAKE_SKIP_THRESHHOLD)));
	}

	@Override
	public boolean isTwitterEnabled() {
		return preferences.getBoolean(ctx.getString(R.string.KEY_TWITTER_NOWPLAYING_ENABLED), Boolean.parseBoolean(ctx
				.getString(R.string.DEFAULT_TWITTER_NOWPLAYING_ENABLED)));
	}

	@Override
	public boolean isScrobbledroidEnabled() {
		String type = preferences.getString(ctx.getString(R.string.KEY_SCROBBLE_TYPE), ctx
				.getString(R.string.DEFAULT_SCROBBLE_TYPE));
		Log.v(TAG, "type: " + type);
		if (Integer.parseInt(type) == 2) {
			return true;
		}
		return false;
	}

	@Override
	public boolean isInternalScrobblingEnabled() {
		String type = preferences.getString(ctx.getString(R.string.KEY_SCROBBLE_TYPE), ctx
				.getString(R.string.DEFAULT_SCROBBLE_TYPE));
		Log.v(TAG, "type: " + type);
		if (Integer.parseInt(type) == 3) {
			return true;
		}
		return false;
	}

	// @Override
	// public void setScrobblingType(ScrobblingType type) {
	// editor.putInt(ctx.getString(R.string.KEY_SCROBBLE_TYPE),
	// type.getValue());
	// editor.commit();
	// }

	@Override
	public void updateScrobblingEnabledPref() {
		editor.putString(ctx.getString(R.string.KEY_SCROBBLE_TYPE), "" + ScrobblingType.INTERNAL.getValue());
		editor.putBoolean(ctx.getString(R.string.KEY_SCROBBLE_ENABLED), false);
		editor.commit();
	}

	@Override
	public boolean isIgnoreLeadingThe() {
		return preferences.getBoolean(ctx.getString(R.string.KEY_IGNORE_LEADING_THE), Boolean.parseBoolean(ctx
				.getString(R.string.DEFAULT_IGNORE_LEADING_THE)));
	}

	@Override
	public boolean isAutomaticImports() {
		return preferences.getBoolean(ctx.getString(R.string.KEY_AUTOMATIC_IMPORTS), Boolean.parseBoolean(ctx
				.getString(R.string.DEFAULT_AUTOMATIC_IMPORTS)));
	}

	@Override
	public boolean isDirectlyShowAlbumSongList() {
		return preferences.getBoolean(ctx.getString(R.string.KEY_DIRECTLY_SHOW_ALBUM_LIST), Boolean.parseBoolean(ctx
				.getString(R.string.DEFAULT_DIRECTLY_SHOW_ALBUM_LIST)));
	}

	@Override
	public boolean isCrossfadingEnabled() {
		return preferences.getBoolean(ctx.getString(R.string.KEY_CROSSFADING), Boolean.parseBoolean(ctx
				.getString(R.string.DEFAULT_CROSSFADING)));
	}

	@Override
	public boolean isBeatMatchingEnabled() {
		return preferences.getBoolean(ctx.getString(R.string.KEY_BEAT_MATCHING), Boolean.parseBoolean(ctx
				.getString(R.string.DEFAULT_BEAT_MATCHING)));
	}

	public String getLogString() {
		StringBuilder sb = new StringBuilder();
		sb.append("isFullScreen: " + isFullScreen() + "\n");
		sb.append("isGapless: " + isGapless() + "\n");
		sb.append("getGaplessGapRemoveTime: " + getGaplessGapRemoveTime() + "\n");
		sb.append("isFirstStart: " + isFirstStart() + "\n");
		sb.append("getScrobbleInterval: " + getScrobbleInterval() + "\n");
		sb.append("isScrobblingPaused: " + isScrobblingPaused() + "\n");
		sb.append("isScrobblingEnabled: " + isScrobblingEnabled() + "\n");
		sb.append("areNotificationsShown: " + areNotificationsShown() + "\n");
		sb.append("isCurrentAlbumHighlighted: " + isCurrentAlbumHighlighted() + "\n");
		sb.append("isAutomaticallyShowCover: " + isAutomaticallyShowCover() + "\n");
		sb.append("isAutomaticallyResumeOnHeadsetPlugged: " + isAutomaticallyResumeOnHeadsetPlugged() + "\n");
		sb.append("isCommittingServerData: " + isCommittingServerData() + "\n");
		sb.append("getRandomUserHash: " + getRandomUserHash() + "\n");
		sb.append("getSimilarArtistAvoidanceNumber: " + getSimilarArtistAvoidanceNumber() + "\n");
		sb.append("isHapticFeedback: " + isHapticFeedback() + "\n");
		sb.append("isUseIconLists: " + isUseIconLists() + "\n");
		sb.append("isUseAlbumArtFiles: " + isUseAlbumArtFiles() + "\n");
		sb.append("isKineticMovement: " + isKineticMovement() + "\n");
		sb.append("isSearchWhileTyping: " + isSearchWhileTyping() + "\n");
		// sb.append("isFamousArtistsInserted: " + isFamousArtistsInserted() +
		// "\n");
		sb.append("getLastPositionInPcaMapX: " + getLastPositionInPcaMapX() + "\n");
		sb.append("getLastPositionInPcaMapY: " + getLastPositionInPcaMapY() + "\n");
		sb.append("getSharedPreferencesVersion: " + getSharedPreferencesVersion() + "\n");
		sb.append("isShowZoomBar: " + isShowZoomBar() + "\n");
		sb.append("getLastSentPlayLogId: " + getLastSentPlayLogId() + "\n");
		// sb.append("isHelpImproveJukefox: " + isHelpImproveJukefox() + "\n");
		sb.append("isLockScreenControls: " + isLockScreenControls() + "\n");
		sb.append("getPreferredScreenOrientation: " + getPreferredScreenOrientation() + "\n");
		sb.append("getLastApplicationVersion: " + getLastApplicationVersion() + "\n");
		sb.append("getCurrentLogFileNumber: " + getCurrentLogFileNumber() + "\n");
		sb.append("isGotoCurrentAlbumEnabled: " + isGotoCurrentAlbumEnabled() + "\n");
		// sb.append("getNumberOfStartedImports: " + getNumberOfStartedImports()
		// + "\n");
		sb.append("getNumberOfCompletedImports: " + getNumberOfCompletedImports() + "\n");
		sb.append("getAutoGaplessGapRemoveTime: " + getAutoGaplessGapRemoveTime() + "\n");
		sb.append("getAlbumListPosition: " + getAlbumListPosition() + "\n");
		sb.append("getArtistListPosition: " + getArtistListPosition() + "\n");
		sb.append("isUseWallpaperBackground: " + isUseWallpaperBackground() + "\n");
		sb.append("isUseGalleryBackground: " + isUseGalleryBackground() + "\n");
		sb.append("isShakeSkip: " + isShakeSkip() + "\n");
		sb.append("getCoverHintCountPlayer: " + getCoverHintCountPlayer() + "\n");
		sb.append("getCoverHintCountAlbum: " + getCoverHintCountAlbum() + "\n");
		sb.append("getShakeSkipThreshhold: " + getShakeSkipThreshhold() + "\n");
		sb.append("isTwitterEnabled: " + isTwitterEnabled() + "\n");
		sb.append("isScrobbledroidEnabled: " + isScrobbledroidEnabled() + "\n");
		sb.append("isInternalScrobblingEnabled: " + isInternalScrobblingEnabled() + "\n");
		sb.append("isIgnoreLeadingThe: " + isIgnoreLeadingThe() + "\n");
		sb.append("isAutomaticImports: " + isAutomaticImports() + "\n");
		sb.append("isDirectlyShowAlbumSongList: " + isDirectlyShowAlbumSongList() + "\n");
		return sb.toString();
	}

}
