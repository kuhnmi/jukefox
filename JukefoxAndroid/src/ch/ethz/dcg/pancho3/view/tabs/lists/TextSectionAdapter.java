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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.SectionIndexer;
import android.widget.TextView;
import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.jukefox.model.collection.IBaseListItem;
import ch.ethz.dcg.pancho3.R;

public class TextSectionAdapter<ListType extends IBaseListItem> extends ArrayAdapter<ListType> implements
		SectionIndexer {

	private static final String TAG = TextSectionAdapter.class.getSimpleName();
	private List<Integer> positionForSection;
	private List<Integer> sectionForPosition;
	private Object[] sections;
	private int layoutId;

	public TextSectionAdapter(Context context, int textViewResourceId, List<ListType> items,
			final boolean ignoreLeadingThe) {
		super(context, textViewResourceId, items);
		layoutId = textViewResourceId;
		positionForSection = new ArrayList<Integer>();
		sectionForPosition = new ArrayList<Integer>();
		ArrayList<Object> tmpSections = new ArrayList<Object>();
		Collections.sort(items, new Comparator<IBaseListItem>() {

			@Override
			public int compare(IBaseListItem object1, IBaseListItem object2) {
				return object1.getSortString(ignoreLeadingThe).toLowerCase().compareTo(
						object2.getSortString(ignoreLeadingThe).toLowerCase());
			}

		});
		Character lastChar = null;
		int pos = 0;
		for (IBaseListItem item : items) {
			char indexChar;
			if (item.getSortString(ignoreLeadingThe).length() == 0) {
				indexChar = ' ';
			} else {
				indexChar = item.getSortString(ignoreLeadingThe).toLowerCase().charAt(0);
			}
			if (lastChar == null || indexChar != lastChar) {
				if (!bothDigits(lastChar, indexChar)) {
					tmpSections.add("" + indexChar);
					Log.v(TAG, "" + indexChar);
					positionForSection.add(pos);
					lastChar = indexChar;
				}
			}
			sectionForPosition.add(tmpSections.size() - 1);
			pos++;
		}
		sections = tmpSections.toArray();
	}

	@Override
	public int getPositionForSection(int section) {
		return positionForSection.get(section);
	}

	@Override
	public int getSectionForPosition(int position) {
		return sectionForPosition.get(position);
	}

	@Override
	public Object[] getSections() {
		return sections;
	}

	private boolean bothDigits(Character c1, char c2) {
		if (c1 == null) {
			return false;
		}
		return Character.isDigit(c1) && Character.isDigit(c2);
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
		return v;
	}

}
