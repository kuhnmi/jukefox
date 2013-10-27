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
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;
import ch.ethz.dcg.jukefox.model.collection.IBaseListItem;
import ch.ethz.dcg.pancho3.R;

public class MoveableTextListAdapter<ListType extends IBaseListItem> extends ArrayAdapter<ListType> {

	private int layoutId = 0;
	private int highlighPosition = -1;

	public MoveableTextListAdapter(Context context, int textViewResourceId, List<ListType> items) {
		super(context, textViewResourceId, items);

		layoutId = textViewResourceId;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View v = convertView;
		TextView line1 = null;
		TextView line2 = null;
		RelativeLayout background = null;
		if (v == null) {
			v = getNewView();
		}

		// Sometimes we got the wrong view, which resulted in a null pointer
		// exception.
		// Therefore, we check if finding items works.
		line1 = (TextView) v.findViewById(R.id.text1);
		line2 = (TextView) v.findViewById(R.id.text2);
		background = (RelativeLayout) v.findViewById(R.id.background);
		if (line1 == null || line2 == null || background == null) {
			v = getNewView();
		}
		IBaseListItem item = super.getItem(position);
		if (item == null) {
			return v;
		}
		line1.setText(item.getTitle());
		line2.setText(item.getSubTitle());
		if (position == highlighPosition) {
			background.setBackgroundColor(Color.argb(64, 255, 255, 255));
		} else {
			background.setBackgroundColor(Color.argb(000, 255, 255, 255));
		}
		return v;
	}

	private View getNewView() {
		View v;
		LayoutInflater vi = (LayoutInflater) super.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		v = vi.inflate(layoutId, null);
		return v;
	}

	public void setHighlightPosition(int position) {
		highlighPosition = position;
		notifyDataSetChanged();
	}

}
