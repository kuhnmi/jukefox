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
package ch.ethz.dcg.pancho3.tablet.view;

import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

public class OnDragTouchListener implements OnTouchListener {

	private float x;
	private final DragManager dragManager;
	private final float touchSlop;
	private boolean triggered = false;

	public OnDragTouchListener(DragManager dragManager) {
		this.dragManager = dragManager;
		this.touchSlop = dragManager.getTouchSlop();
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		/*dragManager.setLatestXY(event.getX(), event.getY());
		switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN: {
				Log.i("TEST", "DOWN");
				x = event.getX();
				triggered = false;
				return true;
			}
			case MotionEvent.ACTION_MOVE: {
				if (!triggered) {
					Log.i("TEST", "MOVE");
					if (Math.abs(event.getX() - x) > touchSlop) {
						triggered = true;
						v.performLongClick();
					}
					return true;
				}
			}
		}*/
		return false;
	}
}