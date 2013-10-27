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
package ch.ethz.dcg.pancho3.view.overlays;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.StreamCorruptedException;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.TextView;
import ch.ethz.dcg.pancho3.R;
import ch.ethz.dcg.pancho3.controller.eventhandlers.ContextShuffleConfigEventListener;
import ch.ethz.dcg.pancho3.data.context.MusicContext;

public class ContextShuffleConfig extends JukefoxOverlayActivity implements View.OnClickListener,
		View.OnLongClickListener {

	public static final String TAG = ContextShuffleConfig.class.getSimpleName();

	public static final float TEXT_SIZE = 45f;

	private ContextShuffleConfigEventListener eventListener;

	private List<MusicContext> contexts = new ArrayList<MusicContext>();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.contextshuffleconfig);

		eventListener = controller.createContextShuffleConfigEventListener(this);

		registerButtonListeners();

		getContextNames();

		createTagCloud();

		eventListener.onActivityStarted();

	}

	private void registerButtonListeners() {
		findViewById(R.id.newButton).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				eventListener.onOkButtonClicked();
			}
		});
		findViewById(R.id.likeButton).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				eventListener.onLikeButtonClicked();
			}
		});
		findViewById(R.id.dislikeButton).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				eventListener.onDislikeButtonClicked();
			}
		});
	}

	private void getContextNames() {
		Log.v(TAG, "path: " + getFilesDir().getAbsolutePath());
		File dir = getFilesDir();
		File[] files = dir.listFiles();

		if (files == null) {
			// TODO: No contexts available
		} else {
			int u = 0;
			for (int i = 0; i < files.length; i++) {
				if (!files[i].isDirectory() && files[i].getName().endsWith(".ctx")) {
					try {
						FileInputStream fin = openFileInput(files[i].getName());
						ObjectInputStream oin = new ObjectInputStream(fin);
						MusicContext context = (MusicContext) oin.readObject();
						contexts.add(context);
						Log.v(TAG, "Added context -" + context.getName() + "- to tag cloud");
					} catch (StreamCorruptedException e) {
						Log.w(TAG, e);
					} catch (IOException e) {
						Log.w(TAG, e);
					} catch (ClassNotFoundException e) {
						Log.w(TAG, e);
					} catch (ClassCastException e) {
						Log.w(TAG, e);
					}
					u++;
				}
			}
		}
		return;
	}

	private void createTagCloud() {
		LinearLayout verticalLayout;
		verticalLayout = (LinearLayout) findViewById(R.id.tagCloud);
		verticalLayout.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT,
				LayoutParams.WRAP_CONTENT));
		verticalLayout.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL);
		verticalLayout.setOrientation(LinearLayout.VERTICAL);
		verticalLayout.removeAllViews();
		LinearLayout row = createRow();
		verticalLayout.addView(row);
		int addedTags = 0;
		float addedToHoriz = 0;
		float max = Float.MIN_VALUE;
		float min = Float.MAX_VALUE;
		// TODO: Compute Probability of context
		// for (Pair<? extends BaseTag, Float> tag : tags) {
		// if (tag.second > max) {
		// max = tag.second;
		// }
		// if (tag.second < min) {
		// min = tag.second;
		// }
		// }
		float prob = 1;
		max = 1f;
		min = 1f;
		int width = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getWidth();
		float range = max - min;
		for (MusicContext context : contexts) {
			TextView text = createTagView(addedTags, min, range, context, prob);
			float textwidth = text.getPaint().measureText(context.getName() + " ");
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

	// Creates a view for a tag.
	private TextView createTagView(int addedTags, float min, float range, MusicContext context, float weight) {
		TextView text = new TextView(this);
		text.setPadding(0, 5, 0, 5);
		text.setText(context.getName() + " ");
		text.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
		text.setGravity(Gravity.CENTER_HORIZONTAL);
		text.setMaxLines(1);
		float size = range == 0 ? TEXT_SIZE : (weight - min) / range * TEXT_SIZE + TEXT_SIZE;
		text.setTextSize(size);
		text.setId(context.getName().hashCode());
		text.setTag(context);
		if (addedTags % 2 == 0) {
			text.setTextColor(Color.WHITE);
		} else {
			text.setTextColor(Color.LTGRAY);
		}
		text.setOnClickListener(this);
		text.setOnLongClickListener(this);
		return text;
	}

	// Creates a horizontal linear layout which represents a row.
	private LinearLayout createRow() {
		LinearLayout horizontalLayout = new LinearLayout(this);
		horizontalLayout.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT,
				LayoutParams.WRAP_CONTENT));
		horizontalLayout.setGravity(Gravity.CENTER_HORIZONTAL);
		return horizontalLayout;
	}

	/**
	 * Called when a tag is clicked
	 */
	@Override
	public void onClick(View v) {
		MusicContext context = (MusicContext) v.getTag();
		eventListener.onTagClicked(context);
	}

	/**
	 * Called when a tag is long clicked
	 */
	@Override
	public boolean onLongClick(View v) {
		return false;
	}

}
