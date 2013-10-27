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
package ch.ethz.dcg.pancho3.view.statistics.adapter;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import android.content.Context;
import android.view.View;
import android.widget.TextView;
import ch.ethz.dcg.jukefox.model.collection.statistics.IStatisticsData;
import ch.ethz.dcg.pancho3.R;

/**
 * Shows a list of the data indicating how far the values of the datas are in the past.<br/>
 * <br/>
 * The values of the data items have to be an instance of {@link Date}.
 */
public class StatisticsDateListAdapter<T extends IStatisticsData> extends StatisticsBaseListAdapter<T> {

	/**
	 * List adapter for statistics data. Assumes, that data is sorted.
	 * 
	 * @param context
	 *            The context
	 * @param data
	 *            The to be displayed data
	 */
	public StatisticsDateListAdapter(Context context, List<T> data) {
		super(context, R.layout.statistics_datelistitem, data);
	}

	@Override
	protected void fillSpecialFields(View v, T item) {
		TextView dateText = (TextView) v.findViewById(R.id.txt_date);
		Date itemDate = (Date) item.getValue();
		dateText.setText(getDateText(itemDate));
	}

	private String getDateText(Date date) {
		Date now = new Date();
		long dateDiff = now.getTime() - date.getTime();

		long minutes = TimeUnit.MILLISECONDS.toMinutes(dateDiff);
		long hours = TimeUnit.MILLISECONDS.toHours(dateDiff);
		long days = TimeUnit.MILLISECONDS.toDays(dateDiff);
		if (days > 0) {
			return String.format(getString(R.string.date_range_days), days);
		} else if (hours > 0) {
			return String.format(getString(R.string.date_range_hours), hours);
		} else if (minutes > 0) {
			return String.format(getString(R.string.date_range_minutes), minutes);
		} else {
			return getString(R.string.date_less_than_one_minute);
		}
	}

	private String getString(int resId) {
		return getContext().getString(resId);
	}
}