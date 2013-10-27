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
package ch.ethz.dcg.pancho3.view.dialogs;

import java.util.ArrayList;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.pancho3.R;
import ch.ethz.dcg.pancho3.view.youtube.YoutubePlayer;

public class ContinueDialog extends BaseDialog {

	public static final String TAG = ContinueDialog.class.getSimpleName();
	public static final String VIDEO_URL = "video_url";
	public ArrayList<String> vid_url;
	public Intent intent;
	public int k = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		intent = getIntent();
		Bundle extras = intent.getExtras();
		vid_url = extras.getStringArrayList(VIDEO_URL);

		YoutubePlayer p = new YoutubePlayer(vid_url.get(0));
		String url = p.getVideoUrl();
		Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
		startActivity(intent);

	}

	@Override
	public void onPause() {
		super.onPause();
		k++;
		Log.v(TAG, "on pause changing value to " + k);
	}

	@Override
	public void onRestart() {
		super.onRestart();
		if (k == vid_url.size()) {
			Intent intent = new Intent(ContinueDialog.this, StandardDialog.class);
			intent
					.putExtra(StandardDialog.DIALOG_MSG, ContinueDialog.this
							.getString(R.string.no_more_available_videos));
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(intent);
			ContinueDialog.this.finish();
		}
		Log.v(TAG, "on restart");
		setContentView(R.layout.continue_standarddialog);
		registerButtons();
	}

	private void registerButtons() {

		findViewById(R.id.contButton).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {

				Log.v(TAG, "continue button clicked");
				playVideo();

			}

		});

		findViewById(R.id.playvideo_againButton).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				k--;
				playVideo();

			}
		});

		findViewById(R.id.gobackButton).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Log.v(TAG, "go back button clicked");
				ContinueDialog.this.finish();
			}

		});
	}

	private void playVideo() {
		YoutubePlayer p = new YoutubePlayer(vid_url.get(k));
		String url = p.getVideoUrl();
		Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
		startActivity(intent);
	}

}
