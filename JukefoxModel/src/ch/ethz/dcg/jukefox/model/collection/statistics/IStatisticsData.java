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
package ch.ethz.dcg.jukefox.model.collection.statistics;

import java.util.Date;

public interface IStatisticsData {

	/**
	 * Returns the id for this data item.
	 * 
	 * @return The id
	 */
	public int getId();

	/**
	 * Returns the title for this data item.
	 * 
	 * @return The title
	 */
	public String getTitle();

	/**
	 * Returns the subtitle for this data item.
	 * 
	 * @return The subtitle
	 */
	public String getSubTitle();

	/**
	 * Returns the value of this data item.<br/>
	 * * Date values are of type {@link Date}.<br/>
	 * * Raings are of type {@link Float}
	 * 
	 * @return The value
	 */
	public Object getValue();

	/**
	 * Sets the value of this data item.
	 * 
	 * @param value
	 *            The value
	 */
	public void setValue(Object value);

}
