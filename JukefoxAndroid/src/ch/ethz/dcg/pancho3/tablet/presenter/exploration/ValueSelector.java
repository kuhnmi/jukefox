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
package ch.ethz.dcg.pancho3.tablet.presenter.exploration;

import java.util.List;

public class ValueSelector {

	public int getNumberOfValuesToSelect(List<Float> values, int minNumberOfValues,
			int maxNumberOfValues) {
		int count = 0;
		if (!values.isEmpty()) {
			float bestValue = values.get(0);
			if (bestValue < 0) {
				for (float value : values) {
					if (value > bestValue * 2) {
						count++;
					} else {
						break;
					}
				}
			} else {
				for (float value : values) {
					if (value / bestValue >= 0.9) {
						count++;
					} else {
						break;
					}
				}
			}
		}
		return Math.min(values.size(), Math.max(minNumberOfValues, Math.min(maxNumberOfValues,
				count)));
	}

}
