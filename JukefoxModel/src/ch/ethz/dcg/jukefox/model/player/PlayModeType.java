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
package ch.ethz.dcg.jukefox.model.player;

/**
 * Different play modes are implemented by using different
 * PlaylistControllerCores. Playmodes can basically alter any method declared in
 * IPlaylistControllerCore.
 */
public enum PlayModeType {
	PLAY_ONCE(0), REPEAT(1), REPEAT_SONG(2), SHUFFLE_PLAYLIST(3), SIMILAR(4), SMART_SHUFFLE(5), RANDOM_SHUFFLE(6), MAGIC(
			7), CONTEXT_SHUFFLE(8);

	private final int value;

	PlayModeType(int value) {
		this.value = value;
	}

	public final int value() {
		return value;
	}

	public static final PlayModeType byValue(int value) {
		switch (value) {
			case 0:
				return PlayModeType.PLAY_ONCE;
			case 1:
				return PlayModeType.REPEAT;
			case 2:
				return PlayModeType.REPEAT_SONG;
			case 3:
				return PlayModeType.SHUFFLE_PLAYLIST;
			case 4:
				return PlayModeType.SIMILAR;
			case 5:
				return PlayModeType.SMART_SHUFFLE;
			case 6:
				return PlayModeType.RANDOM_SHUFFLE;
			case 7:
				return PlayModeType.MAGIC;
			case 8:
				return PlayModeType.CONTEXT_SHUFFLE;
			default:
				return PlayModeType.REPEAT;
		}
	}
};
