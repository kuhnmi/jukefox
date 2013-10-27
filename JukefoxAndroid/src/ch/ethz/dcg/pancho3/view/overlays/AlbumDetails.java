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
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemLongClickListener;
import ch.ethz.dcg.jukefox.commons.DataUnavailableException;
import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.jukefox.controller.Controller;
import ch.ethz.dcg.jukefox.model.collection.BaseAlbum;
import ch.ethz.dcg.jukefox.model.collection.BaseArtist;
import ch.ethz.dcg.jukefox.model.collection.BaseSong;
import ch.ethz.dcg.jukefox.model.collection.CompleteAlbum;
import ch.ethz.dcg.jukefox.model.commons.NoAlbumArtException;
import ch.ethz.dcg.pancho3.R;
import ch.ethz.dcg.pancho3.controller.eventhandlers.AlbumDetailEventListener;
import ch.ethz.dcg.pancho3.model.collection.ParcelableAlbum;
import ch.ethz.dcg.pancho3.view.commons.BitmapReflection;

public class AlbumDetails extends JukefoxOverlayActivity implements OnItemLongClickListener {

	private static final String TAG = AlbumDetails.class.getSimpleName();
	private CompleteAlbum currentAlbum;
	private AlbumDetailEventListener eventListener;
	private ListView songList;
	private ImageView albumArt;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.albumdetails);

		songList = (ListView) findViewById(R.id.songList);
		albumArt = (ImageView) findViewById(R.id.albumArt);

		eventListener = controller.createAlbumDetailEventListener(this);

		if (!loadAlbum()) {
			finish();
		}

		registerEventListeners();

		checkAppStatus();

		setCoverClickHint();

	}

	private void setCoverClickHint() {
		int numberCoverClicked = getSettings().getCoverHintCountAlbum();
		if (numberCoverClicked < AlbumDetailEventListener.NUMBER_COVER_HINT_THRESSHOLD) {
			findViewById(R.id.clickCover).setVisibility(View.VISIBLE);
		}
		if (settings.isDirectlyShowAlbumSongList()) {
			eventListener.albumArtClicked();
		}
	}

	private void checkAppStatus() {
		if (applicationState.isImporting()
				&& (!applicationState.isBaseDataCommitted() || !applicationState.isCoversFetched())) {
			showStatusInfo(getString(R.string.list_not_yet_loaded));
		}
	}

	private void registerEventListeners() {
		ImageView playButton = (ImageView) findViewById(R.id.playButton);
		playButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				eventListener.playButtonClicked();
			}
		});
		ImageView playlistAppendButton = (ImageView) findViewById(R.id.addButton);
		playlistAppendButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				eventListener.playlistAppendButtonClicked();
			}
		});
		ImageView playlistInsertButton = (ImageView) findViewById(R.id.insertButton);
		playlistInsertButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				eventListener.playlistInsertButtonClicked();
			}
		});
		ImageView selectAllButton = (ImageView) findViewById(R.id.selectAllButton);
		selectAllButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				eventListener.selectAllButtonClicked();
			}
		});
		ImageView selectNoneButton = (ImageView) findViewById(R.id.selectNoneButton);
		selectNoneButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				eventListener.selectNoneButtonClicked();
			}
		});
		albumArt.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				eventListener.albumArtClicked();
			}
		});
	}

	public boolean loadAlbum() {
		Intent intent = getIntent();
		Bundle extras = intent.getExtras();
		if (extras == null) {
			return false;
		}
		ParcelableAlbum pAlbum = extras.getParcelable(Controller.INTENT_EXTRA_BASE_ALBUM);
		BaseAlbum baseAlbum = pAlbum.getBaseAlbum();
		try {
			currentAlbum = albumProvider.getCompleteAlbum(baseAlbum);
		} catch (DataUnavailableException e) {
			Log.w(TAG, e);
			finish();
		}

		if (currentAlbum == null) {
			return false;
		}

		setAlbumArt();
		setArtistAndTitle();
		setSongList();

		return true;
	}

	private void setSongList() {

		ArrayAdapter<BaseSong<BaseArtist, BaseAlbum>> adapter = new ArrayAdapter<BaseSong<BaseArtist, BaseAlbum>>(this,
				R.layout.playlistitem, currentAlbum.getSongs()) {
		};
		songList.setAdapter(adapter);
		songList.setItemsCanFocus(true);
		songList.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
		for (int i = 0; i < currentAlbum.getSongs().size(); i++) {
			songList.setItemChecked(i, true);
		}
		songList.setOnItemLongClickListener(this);
	}

	private void setArtistAndTitle() {
		TextView artistText = (TextView) findViewById(R.id.albumDetailsArtistName);
		List<BaseArtist> artists = currentAlbum.getArtists();
		String artistsString = "";
		for (BaseArtist artist : artists) {
			artistsString += artist.getName() + "\n";
		}
		artistText.setText(artistsString);
		TextView albumText = (TextView) findViewById(R.id.albumDetailsAlbumName);
		albumText.setText(currentAlbum.getName());
	}

	private void setAlbumArt() {
		try {
			Bitmap bitmap = albumArtProvider.getAlbumArt(currentAlbum, false);
			// albumArt.setImageBitmap(bitmap);
			BitmapReflection.setReflectionToImageViewAsync(bitmap, albumArt);
		} catch (NoAlbumArtException e) {
			Log.w(TAG, e);
			eventListener.albumArtClicked();
			// StatusInfo.showInfo(this,
			// getString(R.string.covers_not_yet_fetched));
		}

	}

	public ListView getSongList() {
		return songList;
	}

	@Override
	public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
		eventListener.onItemLongClicked(position);
		return true;
	}

}
