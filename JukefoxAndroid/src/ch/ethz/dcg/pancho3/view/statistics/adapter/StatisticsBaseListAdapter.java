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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import ch.ethz.dcg.jukefox.model.collection.statistics.IStatisticsData;
import ch.ethz.dcg.pancho3.R;

public abstract class StatisticsBaseListAdapter<T extends IStatisticsData> extends ArrayAdapter<T> {

	private final int textViewRessourceId;

	public StatisticsBaseListAdapter(Context context, int textViewRessourceId, List<T> data) {
		super(context, textViewRessourceId, data);

		this.textViewRessourceId = textViewRessourceId;
	}

	@Override
	public final View getView(int position, View convertView, ViewGroup parent) {
		View v = convertView;
		if (v == null) {
			LayoutInflater vi = (LayoutInflater) super.getContext().getSystemService(
					Context.LAYOUT_INFLATER_SERVICE);
			v = vi.inflate(textViewRessourceId, null);
		}

		TextView titleText = (TextView) v.findViewById(R.id.txt_title);
		titleText.setText(super.getItem(position).getTitle());

		TextView subtitleText = (TextView) v.findViewById(R.id.txt_subtitle);
		subtitleText.setText(super.getItem(position).getSubTitle());

		// Hide the icon
		ImageView icon = (ImageView) v.findViewById(R.id.icon);
		icon.setVisibility(ImageView.GONE);

		fillSpecialFields(v, getItem(position));

		// Set a padding to the last row
		if (position == super.getCount() - 1) {
			// To be able to scroll all data above the settings button.
			v.setPadding(0, 0, 0, 130);
		} else {
			v.setPadding(0, 0, 0, 0);
		}

		return v;
	}

	/**
	 * In this method all the special fields (like rating, date and icon) can be set by a child class.
	 * 
	 * @param v
	 *            The view
	 * @param item
	 *            The data item of this row
	 */
	protected abstract void fillSpecialFields(View v, T item);

}