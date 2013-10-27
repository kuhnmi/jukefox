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
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import ch.ethz.dcg.jukefox.commons.DataUnavailableException;
import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.jukefox.model.collection.CompleteTag;
import ch.ethz.dcg.pancho3.R;
import ch.ethz.dcg.pancho3.controller.eventhandlers.TagCloudEventListener;
import ch.ethz.dcg.pancho3.view.tabs.JukefoxTabActivity;

public class TagCloud extends JukefoxTabActivity {

	public static final String TAG = TagCloud.class.getSimpleName();
	public static final int NUM_TAGS = 100;
	public static final int TEXT_SIZE = 15;
	private TagCloudCreator tagCloudCreator;
	private LinearLayout topLayout;
	private List<CompleteTag> tags;
	private TagCloudEventListener eventListener;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.tagcloud);
		setCurrentTab(Tab.LISTS);
		eventListener = controller.createTagCloudEventListener(this);
		checkAppStatus();
		tagCloudCreator = new TagCloudCreator(this, TEXT_SIZE);
		tagCloudCreator.setOnClickListener(eventListener);
		tagCloudCreator.setOnLongClickListener(eventListener);
		loadTagCloud();
	}

	private void checkAppStatus() {
		if (applicationState.isImporting() && !applicationState.isMapDataCommitted()) {
			showStatusInfo(getString(R.string.tags_not_yet_loaded));
		}
	}

	private void loadTagCloud() {
		generateLayouts();
		try {
			tags = getAndSortTags();
			// TODO: BUG. In landscape we also have the tabs, and therefore
			// display.getWidth()
			// is too wide.
			Display display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
			tagCloudCreator.createTagCloud(tags, display.getWidth());
		} catch (DataUnavailableException e) {
			//			Log.w(TAG, e);
			// TODO: Maybe show dialog that tags are not available yet
		} catch (Exception e) {
			Log.w(TAG, e);
		}
	}

	private void generateLayouts() {
		topLayout = (LinearLayout) findViewById(R.id.tagCloud);

		// Add LinearLayout for Buttons
		LinearLayout tophoriz = new LinearLayout(this);
		tophoriz.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
		tophoriz.setGravity(Gravity.CENTER_HORIZONTAL);

		// Add Buttons
		addPlayButton(tophoriz);
		addGotoButton(tophoriz);

		topLayout.addView(tophoriz);
		ScrollView scroll = new ScrollView(this);
		scroll.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
		scroll.addView(tagCloudCreator.getTagCloudContainer());
		topLayout.addView(scroll);
	}

	private void addGotoButton(LinearLayout tophoriz) {
		Button gotoButton = new Button(this);
		gotoButton.setText(" " + getString(R.string.go_to) + " ");
		gotoButton.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
		gotoButton.setTextColor(Color.WHITE);
		gotoButton.setBackgroundResource(R.drawable.d046_bg_button);
		gotoButton.setVisibility(View.GONE);
		tophoriz.addView(gotoButton);
	}

	private void addPlayButton(LinearLayout tophoriz) {
		Button playButton = new Button(this);
		playButton.setText(" " + getString(R.string.play) + " ");
		playButton.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
		playButton.setTextColor(Color.WHITE);
		playButton.setBackgroundResource(R.drawable.d046_bg_button);
		playButton.setVisibility(View.GONE);
		tophoriz.addView(playButton);
	}

	private List<CompleteTag> getAndSortTags() throws DataUnavailableException {
		Collection<CompleteTag> compTags = tagProvider.getAllCompleteTags(NUM_TAGS);
		List<CompleteTag> tags = new ArrayList<CompleteTag>(compTags);
		Collections.sort(tags);
		return tags;
	}
}
