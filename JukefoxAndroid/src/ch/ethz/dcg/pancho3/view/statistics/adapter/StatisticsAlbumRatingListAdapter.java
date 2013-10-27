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

import java.util.List;

import android.content.Context;
import android.view.View;
import ch.ethz.dcg.jukefox.model.AndroidCollectionModelManager;
import ch.ethz.dcg.jukefox.model.collection.statistics.StatisticsAlbum;

/**
 * Same as {@link StatisticsRatingListAdapter} but with an icon.
 * 
 * @see {@link StatisticsRatingListAdapter}
 */
public class StatisticsAlbumRatingListAdapter<T extends StatisticsAlbum> extends StatisticsRatingListAdapter<T> {

	private final StatisticsAlbumIconHelper<T> albumIconHelper;

	public StatisticsAlbumRatingListAdapter(Context context, List<T> data,
			AndroidCollectionModelManager collectionModelManager) {

		super(context, data);

		albumIconHelper = new StatisticsAlbumIconHelper<T>(context, collectionModelManager);
	}

	@Override
	protected void fillSpecialFields(View v, T item) {
		super.fillSpecialFields(v, item);

		// Fill the album icon
		albumIconHelper.fillAlbumIcon(v, item);
	}
}
