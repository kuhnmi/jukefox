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
package ch.ethz.dcg.pancho3.tablet.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import ch.ethz.dcg.jukefox.model.collection.BaseAlbum;
import ch.ethz.dcg.jukefox.model.collection.MapAlbum;
import ch.ethz.dcg.pancho3.R;

public class AlbumImageView extends FrameLayout implements OnClickListener, OnLongClickListener {

	private ImageView image;
	private View shade;
	private TextView albumText;
	private TextView artistText;

	private int index;
	private int id;

	private OnItemClickListener onItemClickListener;
	private OnItemLongClickListener onItemLongClickListener;

	private long timestamp = 0;

	public AlbumImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();
		image = (ImageView) findViewById(R.id.image);
		shade = findViewById(R.id.shade);
		albumText = (TextView) findViewById(R.id.albumText);
		artistText = (TextView) findViewById(R.id.artistText);
		setOnClickListener(this);
		setOnLongClickListener(this);
	}

	public void bind(MapAlbum album, int index) {
		id = album.getId();
		albumText.setText(album.getTitle());
		artistText.setText(album.getFirstArtist().getTitle());
		this.index = index;
		shade.setBackgroundColor(getDarkerColor(album.getColor()));
		image.setBackgroundColor(album.getColor());
		image.setImageDrawable(null);

		if (timestamp != 0) {
			Log.i("TEST", "Lived for ms   " + (System.currentTimeMillis() - timestamp));
		}
		timestamp = System.currentTimeMillis();
	}

	public void setListeners(OnItemClickListener onItemClickListener,
			OnItemLongClickListener onItemLongClickListener) {
		this.onItemClickListener = onItemClickListener;
		this.onItemLongClickListener = onItemLongClickListener;
	}

	@Override
	public boolean onLongClick(View v) {
		if (onItemLongClickListener != null) {
			onItemLongClickListener.onItemLongClick(null, v, index, id);
			return true;
		}
		return false;
	}

	@Override
	public void onClick(View v) {
		Log.i("TEST", "CLICK");
		if (onItemClickListener != null) {
			onItemClickListener.onItemClick(null, v, index, id);
		}
	}

	private int getDarkerColor(int color) {
		float[] hsv = new float[3];
		Color.colorToHSV(color, hsv);
		if (hsv[1] < 0.4f) {
			color = Color.parseColor("#C8515151");
		} else {
			color = Color.HSVToColor(200, hsv);
		}
		return color;
	}

	public void setBitmap(Bitmap bitmap, BaseAlbum album) {
		if (album.getId() == id) {
			image.setImageBitmap(bitmap);
		}
	}

	public boolean matchAlbum(BaseAlbum album) {
		return album.getId() == id;
	}
}
