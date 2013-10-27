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

import android.content.SharedPreferences;

public interface ISettingsReader {

	public boolean isFullScreen();

	public boolean isGapless();

	public int getGaplessGapRemoveTime();

	public String getLastFmUserName();

	public String getLastFmPassword();

	public int getScrobbleInterval();

	public boolean isScrobblingPaused();

	public boolean isScrobblingEnabled();

	public boolean areNotificationsShown();

	public boolean isCurrentAlbumHighlighted();

	public boolean isAutomaticallyShowCover();

	public boolean isAutomaticallyResumeOnHeadsetPlugged();

	public boolean isCommittingServerData();

	public Long getRandomUserHash();

	public int getSimilarArtistAvoidanceNumber();

	public boolean isHapticFeedback();

	public boolean isUseIconLists();

	public boolean isUseAlbumArtFiles();

	public boolean isKineticMovement();

	public boolean isSearchWhileTyping();

	public float getLastPositionInPcaMapX();

	public float getLastPositionInPcaMapY();

	public void addSettingsChangeListener(SharedPreferences.OnSharedPreferenceChangeListener listener);

	public void removeSettingsChangeListener(SharedPreferences.OnSharedPreferenceChangeListener listener);

	public boolean isShowZoomBar();

	public boolean isLockScreenControls();

	public int getPreferredScreenOrientation();

	public int getNumberOfCompletedImports();

	public boolean isDontShowAgain(String dontShowSharedPrefKey);

	public boolean isGotoCurrentAlbumEnabled();

	public int getAutoGaplessGapRemoveTime();

	public int getAlbumListPosition();

	public int getArtistListPosition();

	public boolean isUseWallpaperBackground();

	public boolean isUseGalleryBackground();

	public String getGalleryBackgroundPath();

	public boolean isShakeSkip();

	public int getCoverHintCountPlayer();

	public int getCoverHintCountAlbum();

	public float getShakeSkipThreshhold();

	public boolean isTwitterEnabled();

	public boolean isScrobbledroidEnabled();

	public boolean isInternalScrobblingEnabled();

	public boolean isIgnoreLeadingThe();

	public boolean isAutomaticImports();

	public boolean isDirectlyShowAlbumSongList();

	public boolean isLogFileEnabled();

	boolean isFirstStart();

	public boolean isCrossfadingEnabled();

	public boolean isBeatMatchingEnabled();

}
