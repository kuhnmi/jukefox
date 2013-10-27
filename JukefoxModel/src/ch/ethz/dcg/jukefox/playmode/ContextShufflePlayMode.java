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

import ch.ethz.dcg.jukefox.controller.player.IReadOnlyPlayerController;
import ch.ethz.dcg.jukefox.model.AbstractCollectionModelManager;
import ch.ethz.dcg.jukefox.model.AbstractPlayerModelManager;
import ch.ethz.dcg.jukefox.model.collection.BaseAlbum;
import ch.ethz.dcg.jukefox.model.collection.BaseArtist;
import ch.ethz.dcg.jukefox.model.collection.PlaylistSong;
import ch.ethz.dcg.jukefox.model.player.PlayModeType;
import ch.ethz.dcg.jukefox.playmode.ContextShuffleManager.PermanentSmartShuffleState;

/**
 * Chooses a smart song once the playlist is at an end.
 */
public class ContextShufflePlayMode extends SmartShufflePlayMode {

	@SuppressWarnings("unused")
	private final static String TAG = ContextShufflePlayMode.class.getSimpleName();
	public final static int SMART_SHUFFLING_MIN_SONGS = 20;

	private final ContextShuffleManager contextShuffleManager;

	public ContextShufflePlayMode(AbstractCollectionModelManager collectionModel,
			AbstractPlayerModelManager playerModel, ContextShuffleManager contextShuffleManager,
			IReadOnlyPlayerController playerController) {
		super(collectionModel, playerModel, contextShuffleManager, playerController);
		this.contextShuffleManager = contextShuffleManager;
	}

	/**
	 * Call this to stop that the playmode adapts the regions of interest based
	 * on the listened or skipped songs
	 */
	public void lockRegion() {
		contextShuffleManager.lockRegion();
	}

	/**
	 * Call this to ensure that the playmode adapts the regions of interest
	 * based on the listened or skipped songs
	 */
	public void unlockRegion() {
		contextShuffleManager.unlockRegion();
	}

	public boolean isRegionLocked() {
		return contextShuffleManager.isRegionLocked();
	}

	public void loadPermanentState(PermanentSmartShuffleState state) {
		contextShuffleManager.loadPermanentState(state);
	}

	public PermanentSmartShuffleState getPermanentSmartShuffleState() {
		return contextShuffleManager.getPermanentSmartShuffleState();
	}

	@Override
	public PlayModeType getPlayModeType() {
		return PlayModeType.CONTEXT_SHUFFLE;
	}

	public void likedSong(PlaylistSong<BaseArtist, BaseAlbum> song) {

	}

	public void dislikedSong(PlaylistSong<BaseArtist, BaseAlbum> song) {

	}

}
