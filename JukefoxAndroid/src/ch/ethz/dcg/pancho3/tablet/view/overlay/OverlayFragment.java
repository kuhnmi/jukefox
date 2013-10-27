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
package ch.ethz.dcg.pancho3.tablet.view.overlay;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.SparseBooleanArray;
import android.view.ActionMode.Callback;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnDragListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import ch.ethz.dcg.jukefox.commons.utils.JoinableThread;
import ch.ethz.dcg.jukefox.model.collection.BaseAlbum;
import ch.ethz.dcg.jukefox.model.collection.BaseArtist;
import ch.ethz.dcg.jukefox.model.collection.PlaylistSong;
import ch.ethz.dcg.pancho3.R;
import ch.ethz.dcg.pancho3.tablet.TabletFactory;
import ch.ethz.dcg.pancho3.tablet.TabletFactory.TabletFactoryGetter;
import ch.ethz.dcg.pancho3.tablet.interfaces.ISongAdapter;
import ch.ethz.dcg.pancho3.tablet.presenter.overlay.AbstractOverlayPresenter;
import ch.ethz.dcg.pancho3.tablet.presenter.overlay.AbstractOverlayPresenter.IOverlayView;
import ch.ethz.dcg.pancho3.tablet.view.DragManager.DragDataContainer;
import ch.ethz.dcg.pancho3.tablet.view.OnDragTouchListener;
import ch.ethz.dcg.pancho3.tablet.widget.CheckedRelativeLayout;

/**
 * Fragment for displaying a popup with a list of songs.
 */
public class OverlayFragment extends Fragment implements IOverlayView {

	// The outermost view.
	private View mainView;
	// An adapter for all the songs in the list.
	private ISongAdapter adapter;
	// Our presenter.
	private AbstractOverlayPresenter presenter;
	// The seekbar where the user can increase/decrease the number of displayed songs.
	private SeekBar songSeekBar;
	// The background of the whole view, we draw some fancy clouds here.
	private ImageView backgroundImage;
	// The background image of the center part: We possibly display an album art here.
	private ImageView albumArt;
	// Draws fancy and colorful clouds in the background for some style.
	private CloudDrawer cloudDrawer;
	// The title of the header item, which represents all the displayed songs.
	private TextView headerItemTitle;
	// Indicates whether this view has already been hidden again (after it has been shown).
	// This is used so we don't pop too much off the back stack.
	private boolean hiddenAgain = false;

	private View dropArea;
	private ViewGroup dropAreaPlayNow;
	private ViewGroup dropAreaPlayNext;
	private ViewGroup dropAreaEnqueue;
	private View songSeekBarDescription;
	private ListView listView;
	private CheckedRelativeLayout header;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		mainView = inflater.inflate(R.layout.tablet_overlay, null);
		mainView.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				hide(); // Clicking outside of the popup hides the popup.
			}
		});
		backgroundImage = (ImageView) mainView.findViewById(R.id.imagebackground);

		new AsyncTask<Void, Void, TabletFactory>() {

			@Override
			protected TabletFactory doInBackground(Void... params) {
				TabletFactoryGetter tabletFactoryGetter = (TabletFactoryGetter) getActivity();
				while (!tabletFactoryGetter.isTabletFactoryReady()) {
					try {
						JoinableThread.sleep(200);
					} catch (InterruptedException e) {
					}
				}
				return tabletFactoryGetter.getTabletFactory();
			}

			@Override
			protected void onPostExecute(TabletFactory tabletFactory) {
				View popup = mainView.findViewById(R.id.popup);
				popup.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						// NOP. Just to make sure the click event gets killed here.
						// Otherwise it would go on to the outerview, which would hide the
						// the popup on click.
					}
				});
				presenter = tabletFactory.getCurrentOverlayPresenter();
				if (presenter != null) {
					adapter = presenter.getSongAdapter();

					listView = (ListView) popup.findViewById(R.id.list);
					listView.setAdapter(adapter);
					listView.setOnItemClickListener(new OnItemClickListener() {

						@Override
						public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
							presenter.onCheckedIndicesChange(listView.getCheckedItemCount(),
									listView.getCheckedItemPositions(), position);
						}
					});
					tabletFactory.getDragManager().registerSongListViewDragging(listView, adapter);

					albumArt = (ImageView) popup.findViewById(R.id.image);

					songSeekBar = (SeekBar) mainView.findViewById(R.id.songbar);
					songSeekBar.setOnSeekBarChangeListener(presenter);
					songSeekBarDescription = mainView.findViewById(R.id.songbar_description);

					cloudDrawer = tabletFactory.getCloudDrawer();

					header = (CheckedRelativeLayout) popup.findViewById(R.id.headeritem);
					header.setOnLongClickListener(new OnLongClickListener() {

						@Override
						public boolean onLongClick(View v) {
							header.performClick();
							return presenter.onHeaderItemLongClick(headerItemTitle);
						}
					});
					header.setOnClickListener(new OnClickListener() {

						@Override
						public void onClick(View v) {
							presenter.onHeaderItemClick();
						}
					});
					headerItemTitle = (TextView) header.findViewById(R.id.text1);
					header.setOnTouchListener(new OnDragTouchListener(
							tabletFactory.getDragManager()));
					initDropArea();
					presenter.viewFinishedInit();
				} else {
					hide();
				}
			}
		}.execute();

		return mainView;
	}

	public void initDropArea() {
		Resources resources = getActivity().getResources();
		final int textColor = resources.getColor(R.color.text_color);
		final int highlightColor = resources.getColor(R.color.highlight_dark);
		dropArea = mainView.findViewById(R.id.droparea);
		dropAreaPlayNow = (ViewGroup) dropArea.findViewById(R.id.droparea_playnow);
		final TextView dropAreaPlayNowText =
				(TextView) dropAreaPlayNow.findViewById(R.id.text);
		dropAreaPlayNowText.setText(R.string.command_play_now);
		dropAreaPlayNow.setOnDragListener(new OnDragListener() {

			@Override
			public boolean onDrag(View v, DragEvent event) {
				switch (event.getAction()) {
					case DragEvent.ACTION_DRAG_LOCATION:
						// So that we don't reset the text color all the time.
						return true;
					case DragEvent.ACTION_DRAG_ENTERED:
						dropAreaPlayNowText.setTextColor(highlightColor);
						return true;
					case DragEvent.ACTION_DROP:
						DragDataContainer<PlaylistSong<BaseArtist, BaseAlbum>> data =
								(DragDataContainer<PlaylistSong<BaseArtist, BaseAlbum>>)
								event.getLocalState();
						presenter.playNow(data.getData());
						break;
				}
				dropAreaPlayNowText.setTextColor(textColor);
				return true;
			}
		});
		dropAreaPlayNow.findViewById(R.id.text).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				presenter.playNowClicked();
			}
		});

		dropAreaPlayNext = (ViewGroup) dropArea.findViewById(R.id.droparea_playnext);
		LayoutParams params = (LayoutParams) dropAreaPlayNext.getLayoutParams();
		int margin = Math.round(
				getActivity().getResources().getDisplayMetrics().density * 8);
		params.topMargin = margin;
		params.bottomMargin = margin;
		dropAreaPlayNext.setLayoutParams(params);
		final TextView dropAreaPlayNextText =
				(TextView) dropAreaPlayNext.findViewById(R.id.text);
		dropAreaPlayNextText.setText(R.string.command_play_next);
		dropAreaPlayNext.setOnDragListener(new OnDragListener() {

			@Override
			public boolean onDrag(View v, DragEvent event) {
				switch (event.getAction()) {
					case DragEvent.ACTION_DRAG_LOCATION:
						return true;
					case DragEvent.ACTION_DRAG_ENTERED:
						dropAreaPlayNextText.setTextColor(highlightColor);
						return true;
					case DragEvent.ACTION_DROP:
						DragDataContainer<PlaylistSong<BaseArtist, BaseAlbum>> data =
								(DragDataContainer<PlaylistSong<BaseArtist, BaseAlbum>>)
								event.getLocalState();
						presenter.playNext(data.getData());
						break;
				}
				dropAreaPlayNextText.setTextColor(textColor);
				return true;
			}
		});
		dropAreaPlayNext.findViewById(R.id.text).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				presenter.playNextClicked();
			}
		});

		dropAreaEnqueue = (ViewGroup) dropArea.findViewById(R.id.droparea_enqueue);
		final TextView dropAreaEnqueueText =
				(TextView) dropAreaEnqueue.findViewById(R.id.text);
		dropAreaEnqueueText.setText(R.string.command_enqueue);
		dropAreaEnqueue.setOnDragListener(new OnDragListener() {

			@Override
			public boolean onDrag(View v, DragEvent event) {
				switch (event.getAction()) {
					case DragEvent.ACTION_DRAG_LOCATION:
						return true;
					case DragEvent.ACTION_DRAG_ENTERED:
						dropAreaEnqueueText.setTextColor(highlightColor);
						return true;
					case DragEvent.ACTION_DROP:
						DragDataContainer<PlaylistSong<BaseArtist, BaseAlbum>> data =
								(DragDataContainer<PlaylistSong<BaseArtist, BaseAlbum>>)
								event.getLocalState();
						presenter.enqueue(data.getData());
						break;
				}
				dropAreaEnqueueText.setTextColor(textColor);
				return true;
			}
		});
		dropAreaEnqueue.findViewById(R.id.text).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				presenter.enqueueClicked();
			}
		});
	}

	@Override
	public void uncheckAllSongs() {
		SparseBooleanArray indices = listView.getCheckedItemPositions();
		int count = indices.size();
		for (int i = 0; i < count; i++) {
			int key = indices.keyAt(i);
			if (indices.get(key)) {
				listView.setItemChecked(key, false);
			}
		}
		header.setChecked(false);
	}

	@Override
	public void setAlbumArt(Bitmap bitmap) {
		albumArt.setImageBitmap(bitmap);
	}

	@Override
	public void hide() {
		if (!hiddenAgain) {
			hiddenAgain = true;
			if (presenter != null) {
				presenter.onHideOverlay();
			}
			FragmentManager fragmentManager = getFragmentManager();
			if (fragmentManager != null) {
				fragmentManager.popBackStack();
			}
			Activity activity = getActivity();
			if (activity != null) {
				InputMethodManager imm = (InputMethodManager) activity.
						getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.hideSoftInputFromWindow(mainView.getWindowToken(), 0);
			}
		}
	}

	@Override
	public void show(Callback callback) {
		getActivity().startActionMode(callback);
		mainView.setVisibility(View.VISIBLE);
	}

	@Override
	public void initializeSongSeekbar(int numberOfSongs) {
		songSeekBar.setMax(numberOfSongs);
		songSeekBar.setProgress(numberOfSongs);
		songSeekBar.setThumbOffset(0);
	}

	@Override
	public void setHeaderItemTitle(String title) {
		headerItemTitle.setText(title);
	}

	@Override
	public void setBackgroundColor(int color) {
		Drawable drawable = cloudDrawer.getCloudDrawable(color);
		backgroundImage.setScaleType(ScaleType.CENTER_CROP);
		backgroundImage.setImageDrawable(drawable);
	}

	@Override
	public void highlight() {
		dropAreaPlayNow.setBackgroundResource(R.drawable.d171_box_background_highlight);
		dropAreaPlayNext.setBackgroundResource(R.drawable.d171_box_background_highlight);
		dropAreaEnqueue.setBackgroundResource(R.drawable.d171_box_background_highlight);
	}

	@Override
	public void unhighlight() {
		dropAreaPlayNow.setBackgroundResource(R.drawable.d168_box_background);
		dropAreaPlayNext.setBackgroundResource(R.drawable.d168_box_background);
		dropAreaEnqueue.setBackgroundResource(R.drawable.d168_box_background);
	}

	@Override
	public void showDropArea() {
		dropArea.setVisibility(View.VISIBLE);
		InputMethodManager imm = (InputMethodManager) getActivity().
				getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(mainView.getWindowToken(), 0);
	}

	@Override
	public void hideDropArea() {
		dropArea.setVisibility(View.GONE);
	}

	@Override
	public void hideSongbarDescription() {
		songSeekBarDescription.setVisibility(View.GONE);
	}

	@Override
	public void setHeaderChecked(boolean checked, boolean updateListItems) {
		header.setChecked(checked);
		if (updateListItems || checked) {
			for (int i = 0; i < listView.getCount(); i++) {
				listView.setItemChecked(i, checked);
			}
		}
	}

	@Override
	public void setAdapter(ISongAdapter adapter) {
		this.adapter = adapter;
		listView.setAdapter(adapter);
	}

	@Override
	public ImageView getAlbumArt() {
		return albumArt;
	}
}
