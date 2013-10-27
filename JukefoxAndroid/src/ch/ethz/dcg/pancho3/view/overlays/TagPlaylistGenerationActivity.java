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

import java.util.List;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import ch.ethz.dcg.jukefox.commons.DataUnavailableException;
import ch.ethz.dcg.jukefox.commons.utils.JoinableThread;
import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.jukefox.controller.Controller;
import ch.ethz.dcg.jukefox.model.TagPlaylistGenerator;
import ch.ethz.dcg.jukefox.model.collection.BaseAlbum;
import ch.ethz.dcg.jukefox.model.collection.BaseArtist;
import ch.ethz.dcg.jukefox.model.collection.BaseTag;
import ch.ethz.dcg.jukefox.model.collection.CompleteTag;
import ch.ethz.dcg.jukefox.model.collection.PlaylistSong;
import ch.ethz.dcg.pancho3.R;
import ch.ethz.dcg.pancho3.controller.eventhandlers.TagPlaylistGenerationEventListener;
import ch.ethz.dcg.pancho3.model.collection.ParcelableTag;
import ch.ethz.dcg.pancho3.view.tabs.lists.TextListAdapter;

public class TagPlaylistGenerationActivity extends JukefoxOverlayActivity implements OnItemClickListener {

	public static final String TAG = TagPlaylistGenerationActivity.class.getSimpleName();

	private TagPlaylistGenerationEventListener eventListener;
	private EditText playlistSize;
	private ListView playlistList;
	private TextView title;
	private List<PlaylistSong<BaseArtist, BaseAlbum>> currentPlaylist;
	private CompleteTag tag;
	private Handler handler;
	private JoinableThread playlistLoadingThread;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.tagplaylistgeneration);

		playlistSize = (EditText) findViewById(R.id.playlistSize);
		playlistList = (ListView) findViewById(R.id.playlist);
		title = (TextView) findViewById(R.id.tagPlaylistTitle);
		eventListener = controller.createTagPlaylistGenerationEventListener(this);

		this.handler = new Handler();

		getTagFromIntent();

		if (tag == null) {
			finish();
		}

		generatePlaylist();

		registerButtonListeners();
	}

	@Override
	protected void onPause() {
		if (playlistLoadingThread != null) {

		}
		super.onPause();
	}

	private void getTagFromIntent() {
		Intent intent = getIntent();
		Bundle extras = intent.getExtras();
		if (extras == null) {
			return;
		}
		ParcelableTag pTag = extras.getParcelable(Controller.INTENT_EXTRA_BASE_TAG);
		BaseTag baseTag = pTag.getBaseTag();
		if (baseTag == null) {
			return;
		}
		try {
			tag = tagProvider.getCompleteTag(baseTag.getId());
		} catch (DataUnavailableException e) {
			Log.w(TAG, e);
		}
		title.setText(tag.getName() + " - " + getString(R.string.playlist));
	}

	public void generatePlaylist() {
		eventListener.startLoadingPlaylist();
		playlistLoadingThread = new JoinableThread(new Runnable() {

			@Override
			public void run() {
				int size = getPlaylistSize();
				try {
					currentPlaylist = collectionModel.getTagPlaylistGenerator().generatePlaylist(tag, size,
							TagPlaylistGenerator.DEFAULT_SAMPLE_FACTOR);
				} catch (DataUnavailableException e) {
					Log.w(TAG, e);
					return;
				}
				handler.post(new Runnable() {

					@Override
					public void run() {
						createAndSetAdapter();
					}

				});
			}

		});
		playlistLoadingThread.start();
	}

	private int getPlaylistSize() {
		int size = TagPlaylistGenerator.DEFAULT_PLAYLIST_SIZE;
		try {
			size = Integer.parseInt(playlistSize.getText().toString());
		} catch (NumberFormatException e) {
			Log.w(TAG, e);
		}
		return size;
	}

	private void registerButtonListeners() {
		findViewById(R.id.playButton).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				eventListener.onPlayButtonClicked();
			}
		});
		findViewById(R.id.regenerateButton).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				eventListener.onRegenerateButtonClicked();
			}
		});
	}

	public List<PlaylistSong<BaseArtist, BaseAlbum>> getCurrentPlaylist() {
		return currentPlaylist;
	}

	@Override
	public void onItemClick(AdapterView<?> listAdapter, View arg1, int position, long arg3) {
		eventListener.onListItemClicked(position);
	}

	private void createAndSetAdapter() {
		TextListAdapter<PlaylistSong<BaseArtist, BaseAlbum>> adapter;
		adapter = new TextListAdapter<PlaylistSong<BaseArtist, BaseAlbum>>(TagPlaylistGenerationActivity.this,
				R.layout.textlistitem, currentPlaylist);
		playlistList.setAdapter(adapter);
		eventListener.endLoadingPlaylist();
		playlistList.setOnItemClickListener(TagPlaylistGenerationActivity.this);
	}

}
