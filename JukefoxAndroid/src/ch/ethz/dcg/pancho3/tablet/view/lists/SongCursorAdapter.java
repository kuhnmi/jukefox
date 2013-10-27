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
import java.util.Arrays;
import java.util.List;

import android.content.Context;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;
import ch.ethz.dcg.jukefox.model.collection.BaseAlbum;
import ch.ethz.dcg.jukefox.model.collection.BaseArtist;
import ch.ethz.dcg.jukefox.model.collection.BaseSong;
import ch.ethz.dcg.jukefox.model.collection.ListAlbum;
import ch.ethz.dcg.pancho3.R;
import ch.ethz.dcg.pancho3.tablet.interfaces.ISongAdapter;
import ch.ethz.dcg.pancho3.tablet.view.ViewFactory;

public class SongCursorAdapter extends CursorAdapter implements ISongAdapter {

	private final ViewFactory viewFactory;
	private Cursor cursor;

	public SongCursorAdapter(Context context, ViewFactory viewFactory) {
		super(context, null, 0);
		this.viewFactory = viewFactory;
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		((TextView) view.findViewById(R.id.text1)).setText(cursor.getString(1));
		TextView albumButton = (TextView) view.findViewById(R.id.text2);
		albumButton.setText(cursor.getString(3));
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		return viewFactory.createSearchSongView(parent);
	}

	@Override
	public void changeCursor(Cursor cursor) {
		/** 
		 * Cursors columns (@see SqlAndroidDbDataPort):
		 * 		TblSongs.SONG_ID, TblSongs.NAME, TblSongs.ARTIST_ID, TblArtists.NAME, 
		 * 		TblSongs.ALBUM_ID, TblAlbums.ALBUM_NAME, TblSongs.DURATION
		 */
		if (cursor == null) {
			this.cursor = null;
		} else {
			this.cursor = new SongCursorWrapper(cursor);
		}
		super.changeCursor(this.cursor);
	}

	public List<BaseSong<BaseArtist, BaseAlbum>> getSongList() {
		List<BaseSong<BaseArtist, BaseAlbum>> songList =
				new ArrayList<BaseSong<BaseArtist, BaseAlbum>>();
		cursor.moveToPosition(-1);
		while (cursor.moveToNext()) {
			songList.add(new BaseSong<BaseArtist, BaseAlbum>(cursor.getInt(0), cursor.getString(1),
					getArtist(), getAlbum(), cursor.getInt(6)));
		}
		return songList;
	}

	@Override
	public BaseSong<BaseArtist, BaseAlbum> getSong(int position) {
		cursor.moveToPosition(position);
		return new BaseSong<BaseArtist, BaseAlbum>(cursor.getInt(0), cursor.getString(1),
				getArtist(), getAlbum(), cursor.getInt(6));
	}

	private ListAlbum getAlbum() {
		return new ListAlbum(cursor.getInt(4), cursor.getString(5), Arrays
				.asList(getArtist()));
	}

	private BaseArtist getArtist() {
		return new BaseArtist(cursor.getInt(2), cursor.getString(3));
	}

	private static class SongCursorWrapper extends CursorWrapper {

		public SongCursorWrapper(Cursor cursor) {
			super(cursor);
		}

		@Override
		public int getColumnIndexOrThrow(String columnName) throws IllegalArgumentException {
			if (columnName.equals("_id")) {
				return 0;
			} else {
				return super.getColumnIndexOrThrow(columnName);
			}
		}

		@Override
		public int getColumnIndex(String columnName) {
			if (columnName.equals("_id")) {
				return 0;
			} else {
				return super.getColumnIndex(columnName);
			}
		}
	}
}
