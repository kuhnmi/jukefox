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
package ch.ethz.dcg.pancho3.view.commons;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuffXfermode;
import android.graphics.Bitmap.Config;
import android.graphics.PorterDuff.Mode;
import android.graphics.Shader.TileMode;
import android.widget.ImageView;
import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.pancho3.model.JukefoxApplication;

public class BitmapReflection {

	private static final String TAG = BitmapReflection.class.getSimpleName();

	/**
	 * 
	 * @param image
	 * @return original image with reflection if it can successfully be created
	 *         or the original image else
	 */
	public static Bitmap getReflection(Bitmap image) {
		if (image == null) {
			return null;
		}
		// The gap we want between the reflection and the original image
		final int reflectionGap = 2;

		// Get you bit map from drawable folder
		Bitmap originalImage = image;

		int width = originalImage.getWidth();
		int height = originalImage.getHeight();
		if (width < 4 || height < 4) {
			// If the size is smaller than 4, height / 4 = 0 and it will not be
			// able to create a bitmap
			return null;
		}

		// This will not scale but will flip on the Y axis
		Matrix matrix = new Matrix();
		matrix.preScale(1, -1);

		// Create a Bitmap with the flip matrix applied to it.
		// We only want the bottom half of the image
		Bitmap reflectionImage = createReflectionImage(originalImage, width, height, matrix);

		// Create a new bitmap with same width but taller to fit reflection
		Bitmap bitmapWithReflection = createReflectionBitmap(width, height);

		// Check if reflection could not be created
		if (reflectionImage == null || bitmapWithReflection == null) {
			return originalImage;
		}

		// Create a new Canvas with the bitmap that's big enough for
		// the image plus gap plus reflection
		Canvas canvas = new Canvas(bitmapWithReflection);
		// Draw in the original image
		canvas.drawBitmap(originalImage, 0, 0, null);
		// Draw in the gap
		Paint defaultPaint = new Paint();
		canvas.drawRect(0, height, width, height + reflectionGap, defaultPaint);
		// Draw in the reflection
		canvas.drawBitmap(reflectionImage, 0, height + reflectionGap, null);

		// Create a shader that is a linear gradient that covers the reflection
		Paint paint = new Paint();
		LinearGradient shader = new LinearGradient(0, originalImage.getHeight(), 0, bitmapWithReflection.getHeight()
				+ reflectionGap, 0xa0ffffff, 0x00ffffff, TileMode.CLAMP);
		// Set the paint to use this shader (linear gradient)
		paint.setShader(shader);
		// Set the Transfer mode to be porter duff and destination in
		paint.setXfermode(new PorterDuffXfermode(Mode.DST_IN));
		// Draw a rectangle using the paint with our linear gradient
		canvas.drawRect(0, height, width, bitmapWithReflection.getHeight() + reflectionGap, paint);
		return bitmapWithReflection;
	}

	private static Bitmap createReflectionBitmap(int width, int height) {
		Bitmap bitmap = null;
		try {
			try {

				bitmap = Bitmap.createBitmap(width, (height + height / 4), Config.ARGB_8888);
			} catch (Error e) {

				// Avoid that heap has to be grown for the BitmapFactory,
				// as this would lead to an out of memory error
				int[] dummyArray = new int[width * (height + height / 4)];
				// Avoid being eliminated by optimization of compiler
				if (dummyArray != null) {
					dummyArray = null;
					System.gc();
				}
				Log.w("BitmapFactory", e);
			}
			if (bitmap == null) {
				try {
					bitmap = Bitmap.createBitmap(width, (height + height / 4), Config.ARGB_8888);
				} catch (Error e) {
					System.gc();
					Log.w("BitmapFactory", e);
				}
			}
		} catch (Throwable e) {
			Log.w(TAG, e);
		}
		return bitmap;
	}

	private static Bitmap createReflectionImage(Bitmap originalImage, int width, int height, Matrix matrix) {
		Bitmap bitmap = null;
		try {
			try {

				bitmap = Bitmap.createBitmap(originalImage, 0, height / 4 * 3, width, height / 4, matrix, false);
			} catch (Error e) {

				// Avoid that heap has to be grown for the BitmapFactory,
				// as this would lead to an out of memory error
				int[] dummyArray = new int[width * height / 4];
				// Avoid being eliminated by optimization of compiler
				if (dummyArray != null) {
					dummyArray = null;
					System.gc();
				}
				Log.w("BitmapFactory", e);
			}
			if (bitmap == null) {
				try {
					bitmap = Bitmap.createBitmap(originalImage, 0, height / 4 * 3, width, height / 4, matrix, false);
				} catch (Error e) {
					System.gc();
					Log.w("BitmapFactory", e);
				}
			}
		} catch (Exception e) {
			Log.w("Bitmap", e.toString());

		}
		return bitmap;
	}

	public static void setReflectionToImageViewAsync(final Bitmap bitmap, final ImageView imageView) {

		Thread t = new Thread(new Runnable() {

			@Override
			public void run() {

				final Bitmap bitmapWithReflection = getReflection(bitmap);

				if (bitmapWithReflection != null) {

					JukefoxApplication.getHandler().post(new Runnable() {

						@Override
						public void run() {
							imageView.setImageBitmap(bitmapWithReflection);
						}

					});
				}
			}

		});
		t.start();
	}
}
