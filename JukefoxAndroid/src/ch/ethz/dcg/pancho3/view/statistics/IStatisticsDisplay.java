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
package ch.ethz.dcg.pancho3.view.statistics;

import java.util.List;

import ch.ethz.dcg.jukefox.model.collection.statistics.IStatisticsData;

public interface IStatisticsDisplay {

	/**
	 * Inflates its display into the given parent element.
	 * 
	 * @param data
	 *            The to be displayed data
	 * @param onClickListener
	 *            The listener for clicks on a data item (Can be null)
	 * @param onLongClickListener
	 *            The listener for long clicks on a data item (Can be null)
	 * @param parentId
	 *            The parent element for the display
	 */
	public <T extends IStatisticsData> void inflate(List<T> data, OnClickListener onClickListener,
			OnLongClickListener onLongClickListener, int parentId);

	/**
	 * Listener for clicks on a data item in the display.
	 */
	public interface OnClickListener {

		public void onClick(IStatisticsData item);
	}

	/**
	 * Listener for long clicks on a data item in the display.
	 */
	public interface OnLongClickListener {

		public boolean onLongClick(IStatisticsData item);
	}
}