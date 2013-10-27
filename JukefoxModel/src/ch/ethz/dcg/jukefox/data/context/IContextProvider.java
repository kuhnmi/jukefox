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

public interface IContextProvider {

	public AbstractContextResult getLatestContextValues();

	/**
	 * Get the mean context values for a specified period in the past
	 * 
	 * @param millisBack
	 *            time back that the mean should be computed for
	 * @return means for the last millisBack milliseconds
	 */
	public AbstractContextResult getMeanContextValues(int millisBack);

	public void reregisterSensors();

	public void onPlayerStateChanged(final PlayerState playerState);

	public void onSongCompleted();

	public void onSongStarted();

	/**
	 * Once this method is called the ContextProvider should register itself at
	 * the playercontroller as a PlayereventListenet
	 * 
	 * @param playerController
	 */
	public void setPlayerController(IReadOnlyPlayerController playerController);

}