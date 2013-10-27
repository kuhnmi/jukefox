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
import java.util.List;

import android.R;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.TextView;
import ch.ethz.dcg.jukefox.commons.utils.Pair;
import ch.ethz.dcg.jukefox.model.collection.BaseTag;
import ch.ethz.dcg.jukefox.model.collection.CompleteTag;

/**
 * A class which creates a view that displays a tag cloud. Each instance of
 * TagCloudCreator creates exactly one view which can be obtained using
 * {@link #getTagCloudContainer()}.
 */
public class TagCloudCreator {

	private final Context context;
	private final LayoutInflater inflater;
	private final int textSize;
	private OnClickListener onClickListener;
	private OnLongClickListener onLongClickListener;
	private LinearLayout verticalLayout;

	/**
	 * The constructor needs to know the text size for the tags.
	 */
	public TagCloudCreator(Context context, int textSize) {
		this.context = context;
		this.inflater = LayoutInflater.from(context);
		this.textSize = textSize;
		verticalLayout = new LinearLayout(context);
		verticalLayout.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT,
				LayoutParams.WRAP_CONTENT));
		verticalLayout.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL);
		verticalLayout.setOrientation(LinearLayout.VERTICAL);
	}

	/**
	 * If set it will register a click listener for each tag. The view element
	 * supplied with each click contains the ID of the tag (getId()) and the
	 * complete tag itself ((CompleteTag) getTag()).
	 */
	public void setOnClickListener(OnClickListener onClickListener) {
		this.onClickListener = onClickListener;
	}

	/**
	 * If set it will register a long click listener for each tag. The view
	 * element supplied with each click contains the ID of the tag (getId()) and
	 * the complete tag itself ((CompleteTag) getTag()).
	 */
	public void setOnLongClickListener(OnLongClickListener onLongClickListener) {
		this.onLongClickListener = onLongClickListener;
	}

	/**
	 * Returns the container of the tag cloud. The container is always the same
	 * for each instance of this class and its content will be updated by
	 * {@link #createTagCloud(List, int)}.
	 */
	public View getTagCloudContainer() {
		return verticalLayout;
	}

	/**
	 * Fills the container obtainable by {@link #getTagCloudContainer()} with a
	 * tag cloud.
	 * 
	 * As the weights the overall weights of the whole music collection are
	 * taken.
	 * 
	 * @param tags
	 *            a list of all the tags to be displayed.
	 * @param width
	 *            the width the container will have once embedded.
	 */
	public void createTagCloud(List<CompleteTag> tags, int width) {
		List<Pair<CompleteTag, Float>> tagsWithWeights = new ArrayList<Pair<CompleteTag, Float>>();
		for (CompleteTag tag : tags) {
			tagsWithWeights.add(new Pair<CompleteTag, Float>(tag, tag.getMeanPlsaProb()));
		}
		createTagCloudWithWeights(tagsWithWeights, width);
	}

	/**
	 * Fills the container obtainable by {@link #getTagCloudContainer()} with a
	 * tag cloud.
	 * 
	 * @param tags
	 *            a list of all the tags to be displayed.
	 * @param width
	 *            the width the container will have once embedded.
	 */
	public void createTagCloudWithWeights(List<? extends Pair<? extends BaseTag, Float>> tags, int width) {
		verticalLayout.removeAllViews();
		LinearLayout row = createRow();
		verticalLayout.addView(row);
		int addedTags = 0;
		float addedToHoriz = 0;
		float max = Float.MIN_VALUE;
		float min = Float.MAX_VALUE;
		for (Pair<? extends BaseTag, Float> tag : tags) {
			if (tag.second > max) {
				max = tag.second;
			}
			if (tag.second < min) {
				min = tag.second;
			}
		}
		float range = max - min;
		for (Pair<? extends BaseTag, Float> tag : tags) {
			TextView text = createTagView(addedTags, min, range, tag.first, tag.second);
			float textwidth = text.getPaint().measureText(tag.first.getName() + " ");
			if (addedToHoriz + textwidth > width) {
				addedToHoriz = 0;
				row = createRow();
				verticalLayout.addView(row);
			}
			addedToHoriz += textwidth;
			row.addView(text);
			addedTags++;
		}
	}

	// Creates a horizontal linear layout which represents a row.
	private LinearLayout createRow() {
		LinearLayout horizontalLayout = new LinearLayout(context);
		horizontalLayout.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT,
				LayoutParams.WRAP_CONTENT));
		horizontalLayout.setGravity(Gravity.CENTER_HORIZONTAL);
		return horizontalLayout;
	}

	// Creates a view for a tag.
	private TextView createTagView(int addedTags, float min, float range, BaseTag tag, float weight) {
		//		TextView text = (TextView) inflater.inflate(R.layout.tablet_tag, null);
		//		// TODO: text not visible anymore:S

		TextView text = new TextView(context);

		text.setBackgroundDrawable(Resources.getSystem().getDrawable(R.drawable.list_selector_background));

		text.setPadding(0, 5, 0, 5);
		text.setText(tag.getName() + " ");
		text.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
		text.setGravity(Gravity.CENTER_HORIZONTAL);
		text.setMaxLines(1);

		float size = (weight - min) / range * textSize + textSize;
		text.setTextSize(size);
		text.setId(tag.getId());
		text.setTag(tag);
		if (addedTags % 2 == 0) {
			text.setTextColor(Color.WHITE);
		} else {
			text.setTextColor(Color.LTGRAY);
		}
		if (onClickListener != null) {
			text.setOnClickListener(onClickListener);
		}
		if (onLongClickListener != null) {
			text.setOnLongClickListener(onLongClickListener);
		}
		return text;
	}
}
