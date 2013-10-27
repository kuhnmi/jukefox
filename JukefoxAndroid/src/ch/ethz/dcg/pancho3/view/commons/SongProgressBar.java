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

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ProgressBar;

public class SongProgressBar extends ProgressBar {

	private OnProgressChangeListener listener;
	private static int padding = 2;

	private boolean reactOnDownEvents = true;
	private boolean reactOnMoveEvents = false;

	public interface OnProgressChangeListener {

		void onProgressChanged(View v, int progress);
	}

	public SongProgressBar(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public SongProgressBar(Context context, AttributeSet attrs) {
		super(context, attrs, android.R.attr.progressBarStyleHorizontal);
	}

	public SongProgressBar(Context context) {
		super(context);
	}

	public void setOnProgressChangeListener(OnProgressChangeListener l) {
		listener = l;
	}

	public void setReactOnDownEvents(boolean b) {
		reactOnDownEvents = b;
	}

	public void setReactOnMoveEvents(boolean b) {
		reactOnMoveEvents = b;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		int action = event.getAction();
		boolean b = reactOnDownEvents && action == MotionEvent.ACTION_DOWN;
		b = b || reactOnMoveEvents && action == MotionEvent.ACTION_MOVE;
		if (b) {
			float x_mouse = event.getX() - padding;
			float width = getWidth() - 2 * padding;
			int progress = Math.round(getMax() * x_mouse / width);
			if (progress < 0) {
				progress = 0;
			}
			if (progress > getMax()) {
				progress = getMax();
			}
			this.setProgress(progress);
			if (listener != null) {
				listener.onProgressChanged(this, progress);
			}
		}
		return true;
	}

}
