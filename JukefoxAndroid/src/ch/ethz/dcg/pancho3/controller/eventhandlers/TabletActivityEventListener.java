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
package ch.ethz.dcg.pancho3.controller.eventhandlers;

import ch.ethz.dcg.jukefox.controller.Controller;
import ch.ethz.dcg.jukefox.model.collection.BaseAlbum;
import ch.ethz.dcg.jukefox.model.collection.BaseArtist;
import ch.ethz.dcg.jukefox.model.collection.BaseSong;
import ch.ethz.dcg.jukefox.model.player.PlayerState;
import ch.ethz.dcg.pancho3.tablet.presenter.TabletPresenter;
import ch.ethz.dcg.pancho3.tablet.view.TabletActivity;
import ch.ethz.dcg.pancho3.view.tabs.JukefoxTabActivity.Tab;

public class TabletActivityEventListener extends MainTabButtonEventListener {

	@SuppressWarnings("unused")
	private static final String TAG = TabletActivityEventListener.class.getSimpleName();

	private final TabletPresenter tabletPresenter;

	// TODO: this should be on songselectedlistener but then we have a clash...
	// resolve.
	public static interface OnSongChosenListener {

		void onSongChosen(BaseSong<BaseArtist, BaseAlbum> song);
	}

	public TabletActivityEventListener(Controller controller, TabletActivity activity, TabletPresenter tabletPresenter) {
		super(controller, activity, Tab.PLAYER);
		this.tabletPresenter = tabletPresenter;
	}

	@Override
	public void onPlayPauseButtonClicked() {
		controller.doHapticFeedback();
		controller.playPauseButtonPressed();
	}

	@Override
	public void onPreviousButtonClicked() {
		controller.doHapticFeedback();
		controller.previousButtonPressed();
	}

	@Override
	public void onNextButtonClicked() {
		controller.doHapticFeedback();
		controller.nextButtonPressed();
	}

	public void onSongSelected(BaseSong<BaseArtist, BaseAlbum> song) {
		tabletPresenter.onSongChosen(song);
	}

	public void setProgress(int progress) {
		PlayerState state = controller.getPlayerController().getPlayerState();
		if (state == PlayerState.PLAY || state == PlayerState.PAUSE) {
			controller.getPlayerController().seekTo(progress);
			// activity.updateProgress(); TODO we need this functionality
		}
	}

	public void detectedFirstStart() {
		controller.showFirstStartDialog();
	}

	public void sdCardProblemDetected() {
		controller.showSdCardProblemDialog();
	}

	public Controller getController() {
		return controller;
	}
}
