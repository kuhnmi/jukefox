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
import android.graphics.Color;
import android.util.AttributeSet;
import android.widget.Checkable;
import android.widget.RelativeLayout;
import ch.ethz.dcg.pancho3.R;

/**
 * A relative layout which supports the checkable interface. If the layout is
 * checked it gets a colored background. Otherwise its background is
 * transparent.
 */
public class CheckedRelativeLayout extends RelativeLayout implements Checkable {

	// Boolean remembering the checked state.
	private boolean checked = false;

	public CheckedRelativeLayout(Context context) {
		super(context);
	}

	public CheckedRelativeLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public CheckedRelativeLayout(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override
	public boolean isChecked() {
		return checked;
	}

	@Override
	public void setChecked(boolean checked) {
		if (this.checked != checked) {
			this.checked = checked;
			checkedChanged();
		}
	}

	@Override
	public void toggle() {
		checked = !checked;
		checkedChanged();
	}

	// If checked we color the background gray instead of transparent.
	private void checkedChanged() {
		if (checked) {
			this.setBackgroundColor(getContext().getResources().getColor(R.color.trans_highlight));
		} else {
			this.setBackgroundColor(Color.TRANSPARENT);
		}
	}
}
