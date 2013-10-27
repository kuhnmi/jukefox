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

import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import ch.ethz.dcg.jukefox.model.AndroidCollectionModelManager;
import ch.ethz.dcg.jukefox.model.collection.statistics.IStatisticsData;
import ch.ethz.dcg.jukefox.model.collection.statistics.StatisticsAlbum;
import ch.ethz.dcg.pancho3.view.statistics.adapter.StatisticsAlbumDateListAdapter;
import ch.ethz.dcg.pancho3.view.statistics.adapter.StatisticsAlbumRatingListAdapter;
import ch.ethz.dcg.pancho3.view.statistics.adapter.StatisticsDateListAdapter;
import ch.ethz.dcg.pancho3.view.statistics.adapter.StatisticsRatingListAdapter;

public class StatisticsListDisplay implements IStatisticsDisplay {

	public enum DataType {
		Rating,
		Date
	};

	private final StatisticsActivity statisticsActivity;
	private final DataType dataType;
	private final AndroidCollectionModelManager collectionModelManager;

	public StatisticsListDisplay(StatisticsActivity statisticsActivity, DataType dataType,
			AndroidCollectionModelManager collectionModelManager) {
		super();

		this.statisticsActivity = statisticsActivity;
		this.dataType = dataType;
		this.collectionModelManager = collectionModelManager;
	}

	/**
	 * Shows a list of the data. How the list should be displayed is determined by the
	 * {@link StatisticsListDisplay#dataType}.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public <T extends IStatisticsData> void inflate(final List<T> data, final OnClickListener onClickListener,
			final OnLongClickListener onLongClickListener, int parentId) {
		LinearLayout l = (LinearLayout) statisticsActivity.findViewById(parentId);
		l.removeAllViews(); // Remove old elements

		// Create a scroll container
		ListView lv = new ListView(statisticsActivity);
		lv.setPadding(0, 0, 0, 20);

		ArrayAdapter<? extends IStatisticsData> adapter;
		if ((data.size() > 0) && (data.get(0) instanceof StatisticsAlbum)) {
			switch (dataType) {
				case Date:
					adapter = new StatisticsAlbumDateListAdapter<StatisticsAlbum>(statisticsActivity,
							(List<StatisticsAlbum>) data, collectionModelManager);
					break;

				case Rating:
				default:
					adapter = new StatisticsAlbumRatingListAdapter<StatisticsAlbum>(statisticsActivity,
							(List<StatisticsAlbum>) data, collectionModelManager);
					break;
			}
		} else {
			switch (dataType) {
				case Date:
					adapter = new StatisticsDateListAdapter<T>(statisticsActivity, data);
					break;

				case Rating:
				default:
					adapter = new StatisticsRatingListAdapter<T>(statisticsActivity, data);
					break;
			}
		}
		lv.setFastScrollEnabled(false);
		lv.setAdapter(adapter);
		lv.setFastScrollEnabled(true);

		// Set the listeners
		if (onClickListener != null) {
			lv.setOnItemClickListener(new OnItemClickListener() {

				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					onClickListener.onClick(data.get(position));
				}
			});
		}
		if (onLongClickListener != null) {
			lv.setOnItemLongClickListener(new OnItemLongClickListener() {

				@Override
				public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
					return onLongClickListener.onLongClick(data.get(position));
				}
			});
		}

		// Add the list view to the layout
		l.addView(lv);
	}

}