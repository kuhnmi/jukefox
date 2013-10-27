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
package ch.ethz.dcg.pancho3.data.context;

import java.io.Serializable;

import ch.ethz.dcg.jukefox.playmode.ContextShuffleManager.PermanentSmartShuffleState;

public class MusicContext implements Serializable {

	private static final long serialVersionUID = 1L;

	public static final String TAG = MusicContext.class.getSimpleName();

	private String name;

	// TODO add Sensor data

	/*
	 * FIXME: Need PermanentSmartShuffleState from "ContextShuffleManager" class
	 * file
	 */
	private PermanentSmartShuffleState smartShuffleState;

	public MusicContext(String name, PermanentSmartShuffleState smartShuffleState) {
		super();
		this.name = name;
		this.smartShuffleState = smartShuffleState;
	}

	public String getName() {
		return name;
	}

	public PermanentSmartShuffleState getSmartShuffleState() {
		return smartShuffleState;
	}

}
