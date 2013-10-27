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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.SectionIndexer;
import android.widget.TextView;
import ch.ethz.dcg.jukefox.commons.utils.JoinableThread;
import ch.ethz.dcg.jukefox.manager.model.albumart.FastBitmapDrawable;
import ch.ethz.dcg.jukefox.model.AndroidCollectionModelManager;
import ch.ethz.dcg.jukefox.model.collection.ListAlbum;
import ch.ethz.dcg.jukefox.model.commons.NoAlbumArtException;
import ch.ethz.dcg.pancho3.R;

public class IconSectionAdapter<T extends ListAlbum> extends ArrayAdapter<T> implements SectionIndexer {

	@SuppressWarnings("unused")
	private static final String TAG = IconSectionAdapter.class.getSimpleName();
	private static final int VIEW_HOLDER_BUFFER_SIZE = 30;
	private static final int ICON_SIZE = 100;
	private static final int ICON_LOAD_DELAY = 300;

	private List<Integer> positionForSection;
	private List<Integer> sectionForPosition;
	private Object[] sections;
	private List<T> albums;
	Context context;
	JoinableThread thread;
	LinkedList<LoadData> toLoad;
	Handler handler;
	boolean threadRunning = false;
	LinkedList<LoadData> toDisplay;
	HashMap<ImageView, ListAlbum> imageViewToAlbum;
	private FastBitmapDrawable defaultAlbumArt;

	private final List<ViewHolder> vhBuffer;
	private int vhBufferIdx = 0;
	private AndroidCollectionModelManager collectionModel;

	private class CoverLoadTask extends TimerTask {

		ListAlbum album;
		ImageView icon;

		public CoverLoadTask(ImageView icon, ListAlbum album) {
			this.album = album;
			this.icon = icon;
		}

		@Override
		public void run() {
			ListAlbum newAlbum = imageViewToAlbum.get(icon);
			if (newAlbum != null && newAlbum == album) {
				BitmapDrawable albumIcon;
				try {
					albumIcon = collectionModel.getAlbumArtProvider().getListAlbumArt(album);
					setAlbumCover(icon, albumIcon);
				} catch (NoAlbumArtException e) {
				}
			}

		}
	}

	private void setAlbumCover(final ImageView icon, final BitmapDrawable albumIcon) {
		handler.post(new Runnable() {

			@Override
			public void run() {
				icon.setBackgroundDrawable(albumIcon);
			}

		});
	}

	@Override
	protected void finalize() throws Throwable {
		threadRunning = false;
		super.finalize();
	}

	public IconSectionAdapter(Context context, int textViewResourceId, List<T> items,
			AndroidCollectionModelManager collectionModel) {
		super(context, textViewResourceId, items);
		this.context = context;
		this.albums = items;
		this.collectionModel = collectionModel;
		vhBuffer = new ArrayList<ViewHolder>(VIEW_HOLDER_BUFFER_SIZE);
		for (int i = 0; i < VIEW_HOLDER_BUFFER_SIZE; i++) {
			vhBuffer.add(new ViewHolder());
			vhBuffer.get(i).timer = new Timer();
		}

		handler = new Handler();
		toLoad = new LinkedList<LoadData>();
		toDisplay = new LinkedList<LoadData>();
		imageViewToAlbum = new HashMap<ImageView, ListAlbum>();

		positionForSection = new ArrayList<Integer>();
		sectionForPosition = new ArrayList<Integer>();
		ArrayList<Object> tmpSections = new ArrayList<Object>();
		Collections.sort(albums, new Comparator<ListAlbum>() {

			@Override
			public int compare(ListAlbum object1, ListAlbum object2) {
				return object1.getName().toLowerCase().compareTo(object2.getName().toLowerCase());
			}

		});
		Character lastChar = null;
		int pos = 0;
		for (ListAlbum album : albums) {
			char indexChar;
			if (album.getName().length() == 0) {
				indexChar = ' ';
			} else {
				indexChar = album.getName().toLowerCase().charAt(0);
			}
			if (lastChar == null || indexChar != lastChar) {
				if (!bothDigits(lastChar, indexChar)) {
					tmpSections.add(new String("" + indexChar));
					positionForSection.add(pos);
					lastChar = indexChar;
				}
			}
			sectionForPosition.add(tmpSections.size() - 1);
			pos++;
		}
		sections = tmpSections.toArray();

		Resources r = context.getResources();
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inSampleSize = 2;
		Bitmap b = resizeBitmap(BitmapFactory.decodeResource(r, R.drawable.d005_empty_cd, options));
		defaultAlbumArt = new FastBitmapDrawable(b);
		defaultAlbumArt.setFilterBitmap(false);
		defaultAlbumArt.setDither(false);
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
			LayoutInflater vi = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			v = vi.inflate(R.layout.iconlistitem, null);
		}
		ListAlbum album = albums.get(position);

		vhBufferIdx = (vhBufferIdx + 1) % VIEW_HOLDER_BUFFER_SIZE;
		ViewHolder vh = vhBuffer.get(vhBufferIdx);
		vh.line1 = (TextView) v.findViewById(R.id.text1);
		vh.line1.setText(album.getName());
		vh.line2 = (TextView) v.findViewById(R.id.text2);
		vh.line2.setText(album.getFirstArtist().getName());
		vh.icon = (ImageView) v.findViewById(R.id.icon);

		// Log.v("TAG", ""+v + " "+ item.getIconPath());
		ListAlbum albumToLoad = imageViewToAlbum.get(vh.icon);
		if (albumToLoad != null && albumToLoad == album) {
			return v; // the job to load this icon into this view is already
			// scheduled
		}

		Timer t = vh.timer;

		imageViewToAlbum.put(vh.icon, album);

		vh.icon.setBackgroundDrawable(defaultAlbumArt);
		vh.icon.setPadding(0, 0, 1, 0);
		v.setTag(vh);

		CoverLoadTask clt = new CoverLoadTask(vh.icon, album);
		t.schedule(clt, ICON_LOAD_DELAY);

		return v;
	}

	private Bitmap resizeBitmap(Bitmap bm) {
		// scale the image for opengl
		int width = bm.getWidth();
		int height = bm.getHeight();
		float scaleWidth = (float) ICON_SIZE / width;
		float scaleHeight = (float) ICON_SIZE / height;

		// scale matrix
		Matrix matrix = new Matrix();
		// resize the bit map
		matrix.postScale(scaleWidth, scaleHeight);

		// recreate the new Bitmap
		Bitmap resizedBitmap = Bitmap.createBitmap(bm, 0, 0, width, height, matrix, true);
		return resizedBitmap;
	}

	class ViewHolder {

		TextView line1;
		TextView line2;
		ImageView play_indicator;
		ImageView icon;
		Timer timer;
	}

	class LoadData {

		BitmapDrawable bm;
		String iconPath;
		ImageView icon;
	}

}
