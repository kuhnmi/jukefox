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
package ch.ethz.dcg.pancho3.view.tabs.lists;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import ch.ethz.dcg.jukefox.model.collection.IBaseListItem;
import ch.ethz.dcg.pancho3.R;

public class VideoListAdapter<ListType extends IBaseListItem> extends ArrayAdapter<ListType> {

	private int layoutId = 0;
	private IVideoButtonCallback callback;

	public VideoListAdapter(Context context, int textViewResourceId, List<ListType> items, IVideoButtonCallback callback) {
		super(context, textViewResourceId, items);

		layoutId = textViewResourceId;
		this.callback = callback;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View v = convertView;
		if (v == null) {
			LayoutInflater vi = (LayoutInflater) super.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			v = vi.inflate(layoutId, null);
		}
		TextView line1 = (TextView) v.findViewById(R.id.text1);
		line1.setText(super.getItem(position).getTitle());
		TextView line2 = (TextView) v.findViewById(R.id.text2);
		line2.setText(super.getItem(position).getSubTitle());
		final int songId = super.getItem(position).getId();

		ImageView videoButton = (ImageView) v.findViewById(R.id.videoButton);
		videoButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				callback.onVideoButtonClicked(songId);
			}
		});
		return v;
	}

}
