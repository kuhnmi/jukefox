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

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.Shader.TileMode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import ch.ethz.dcg.pancho3.R;

/**
 * Class to obtain a cloud-like drawable which can be used for fancy background.
 * The given color is used to influence the color of the cloud.
 */
public class CloudDrawer {

	// The prepared cloud bitmap which is always used.
	// The only modification done is the addition of color.
	private final Bitmap bitmap;

	/**
	 * The constructor creates the cloud bitmap.
	 */
	public CloudDrawer(Resources resources) {
		// Grayscale image of a cloud-like effect. 
		Bitmap cloud = BitmapFactory.decodeResource(resources, R.drawable.d144_cloud);
		int width = cloud.getWidth();
		int height = cloud.getHeight();
		// A mutable bitmap so we can add effects.
		bitmap = Bitmap.createBitmap(width, height, Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap);
		canvas.drawBitmap(cloud, 0, 0, null);
		Paint paint = new Paint();
		paint.setShader(new LinearGradient(0, 0, width / 2, height / 2,
				Color.argb(180, 0, 0, 0), Color.TRANSPARENT, TileMode.MIRROR));
		canvas.drawRect(0, 0, width, height, paint);
	}

	/**
	 * Returns a cloud drawable influeced by the given color.
	 */
	public Drawable getCloudDrawable(int color) {
		float[] hsv = new float[3];
		Color.colorToHSV(color, hsv);
		hsv[1] = Math.min(2 * hsv[1], 1.0f);
		hsv[2] = Math.min(2 * hsv[2], 1.0f);
		color = Color.HSVToColor(hsv);
		final BitmapDrawable drawable = new BitmapDrawable(bitmap);
		drawable.setColorFilter(color, Mode.MULTIPLY);
		drawable.setAlpha(180);
		return drawable;
	}
}
