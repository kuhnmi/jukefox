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
import android.util.AttributeSet;

/**
 * TODO comment and also update in dragger
 */
public class ArrowDragger extends Dragger {

	public ArrowDragger(Context context) {
		super(context);
	}

	public ArrowDragger(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public ArrowDragger(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override
	protected int getNumCirclesHorizontally(int index) {
		if (index == 1) {
			return 2;
		} else {
			return 1;
		}
	}

}
