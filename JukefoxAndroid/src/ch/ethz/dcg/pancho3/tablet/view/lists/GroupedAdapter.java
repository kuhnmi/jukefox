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
package ch.ethz.dcg.pancho3.tablet.view.lists;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import ch.ethz.dcg.pancho3.R;
import ch.ethz.dcg.pancho3.tablet.widget.PinnedHeaderListView;
import ch.ethz.dcg.pancho3.tablet.widget.PinnedHeaderListView.PinnedHeaderAdapter;

public abstract class GroupedAdapter extends BaseAdapter implements PinnedHeaderAdapter
{

	protected Context mCtx;
	private List<Integer> mGroupCounts = new ArrayList<Integer>();
	protected LayoutInflater mInflater;
	protected PinnedHeaderListView mBoundList;

	public GroupedAdapter(Context ctx, PinnedHeaderListView boundList)
	{
		mInflater = (LayoutInflater) ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mBoundList = boundList;
		mCtx = ctx;
	}

	public void initAdapter() {
		mGroupCounts = getGroupCounts();
	}

	public void initUI()
	{
		View view = mInflater.inflate(R.layout.tablet_list_separator, mBoundList, false);
		((TextView) view.findViewById(R.id.text)).setText(" ");
		mBoundList.setPinnedHeaderView(view);
	}

	@Override
	public boolean isEnabled(int position) {
		return getItemViewType(position) != 0;
	}

	@Override
	public int getCount()
	{
		int sum = 0;
		for (int count : mGroupCounts)
		{
			if (count == 0) {
				continue;
			}
			sum += count + 1;
		}
		return sum;
	}

	@Override
	public Object getItem(int position)
	{
		return null;
	}

	@Override
	public long getItemId(int position)
	{
		return position;
	}

	@Override
	public int getViewTypeCount()
	{
		return 2;
	}

	@Override
	public int getItemViewType(int position)
	{
		int current = 0;
		for (int count : mGroupCounts)
		{
			if (position == current) {
				return 0;
			}

			if (count != 0) {
				current += count + 1;
			}
			if (position < current) {
				return 1;
			}
		}

		return 0;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		int current = 0;
		int totalIndex = 0;
		int length = mGroupCounts.size();
		for (int i = 0; i < length; i++)
		{
			if (position == current)
			{
				while (mGroupCounts.get(i) == 0 && i < length) {
					i++;
				}
				return getSeperatorView(i, position, convertView);
			}

			int count = mGroupCounts.get(i);
			if (count != 0) {
				current += count + 1;
			}

			if (position < current)
			{
				View v = getView(i, position - (current - count), convertView,
						totalIndex + position - (current - count));
				v.setVisibility(View.VISIBLE);
				return v;
			}

			totalIndex += count;
		}

		throw new RuntimeException("unreachable code statement detected");
	}

	private final View getSeperatorView(int index, int listViewPosition, View convertView)
	{
		if (convertView != null)
		{
			TextView text = (TextView) ((ViewGroup) convertView).findViewById(R.id.text);
			text.setText(getGroupTitle(index));

			if (listViewPosition == mBoundList.getFirstVisiblePosition()) {
				convertView.setVisibility(View.INVISIBLE);
			} else {
				convertView.setVisibility(View.VISIBLE);
			}
			return convertView;
		}
		else
		{
			View view = mInflater.inflate(R.layout.tablet_list_separator, null);

			TextView text = (TextView) view.findViewById(R.id.text);
			text.setText(getGroupTitle(index));
			return view;
		}
	}

	@Override
	public void configurePinnedHeader(View header, int position, int alpha,
			boolean positionChanged, int lastPosition) {
		if (positionChanged) {
			TextView text = (TextView) ((ViewGroup) header).findViewById(R.id.text);
			int index = 0;
			int current = 0;
			int length = mGroupCounts.size();

			for (int i = 0; i < length; i++) {
				int count = mGroupCounts.get(i);
				if (count != 0) {
					current += count + 1;
				}

				if (position < current) {
					index = i;
					break;
				}
			}

			if (mGroupCounts.size() > 0) {
				text.setText(getGroupTitle(index));

				int firstIndex = position - mBoundList.getFirstVisiblePosition();
				// when top element is header, hide it
				if (position < getCount() && getItemViewType(position) == 0)
				{
					View firstView = mBoundList.getChildAt(firstIndex);
					if (firstView != null) {
						firstView.setVisibility(View.INVISIBLE);
					}
				}
				else if (position + 1 < mBoundList.getCount() && getItemViewType(position + 1) == 0)
				{
					View firstView = mBoundList.getChildAt(firstIndex + 1);
					if (firstView != null) {
						firstView.setVisibility(View.VISIBLE);
					}
				}
			}
		}
	}

	@Override
	public int getPinnedHeaderState(int position)
	{
		int current = 0;
		int length = mGroupCounts.size();
		for (int i = 0; i < length; i++)
		{
			int count = mGroupCounts.get(i);
			if (count != 0) {
				current += count + 1;
			}

			if (position == current - 1)
			{
				return PINNED_HEADER_PUSHED_UP;
			}
		}
		return PINNED_HEADER_VISIBLE;
	}

	protected abstract List<Integer> getGroupCounts();

	protected abstract String getGroupTitle(int groupNumber);

	protected abstract View getView(int groupNumber, int positionWithinGroup, View convertView, int totalDataIndex);
}