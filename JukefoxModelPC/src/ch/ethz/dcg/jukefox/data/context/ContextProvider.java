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
package ch.ethz.dcg.jukefox.data.context;

import ch.ethz.dcg.jukefox.controller.player.IReadOnlyPlayerController;
import ch.ethz.dcg.jukefox.model.player.PlayerState;

public class ContextProvider implements IContextProvider {

	public ContextProvider() {

	}

	@Override
	public ContextResult getLatestContextValues() {
		return new ContextResult();
	}

	@Override
	public ContextResult getMeanContextValues(int millisBack) {
		// TODO do we need here a difference
		return new ContextResult();
	}

	@Override
	public void reregisterSensors() {
	}

	@Override
	public void onSongCompleted() {
	}

	@Override
	public void onSongStarted() {
	}

	@Override
	public void onPlayerStateChanged(PlayerState playerState) {
	}

	@Override
	public void setPlayerController(IReadOnlyPlayerController playerController) {
		// no need for this on PC
	}

}
