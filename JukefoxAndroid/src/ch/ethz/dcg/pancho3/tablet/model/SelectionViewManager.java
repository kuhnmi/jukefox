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
package ch.ethz.dcg.pancho3.tablet.model;

import android.app.Fragment;
import android.content.Context;
import android.content.res.Resources;
import ch.ethz.dcg.pancho3.R;
import ch.ethz.dcg.pancho3.tablet.view.exploration.ExplorationSelectionFragment;
import ch.ethz.dcg.pancho3.tablet.view.map.MapSelectionFragment;

/**
 * Manages the different selection views. By setting the selection view one can
 * obtain its associated fragment. This manager is persistent and will store the
 * choice of the selection view across sessions.
 */
public class SelectionViewManager {

	/**
	 * This enum contains all the different selection views. Each entry needs a
	 * display string (e.g. for the tab in the action bar) and a fragment class
	 * which corresponds to this selection view.
	 */
	public static enum SelectionView {
		SELECTION_VIEW_EXPLORATION(R.string.explore_mode_name,
				ExplorationSelectionFragment.class),
		SELECTION_VIEW_MAP(R.string.map_mode_name,
				MapSelectionFragment.class);

		// Name for the selection view to display.
		private final int displayStringResource;
		// The class of which the instantiation represents this selection view.
		private Class<? extends Fragment> fragmentClass;

		private SelectionView(int displayStringResource, Class<? extends Fragment> fragmentClass) {
			this.displayStringResource = displayStringResource;
			this.fragmentClass = fragmentClass;
		}

		/**
		 * Returns a string to display as a name of this selection view.
		 */
		public String getDisplayString(Resources resources) {
			return resources.getString(displayStringResource);
		}

		/**
		 * Returns a class of the fragment to instantiate to obtain the actual
		 * view of this selection view.
		 */
		public Class<? extends Fragment> getFragmentClass() {
			return fragmentClass;
		}
	}

	// The index of the selection view which will be shown when the app is used for the first time.
	private static final int SELECTION_VIEW_DEFAULT_INDEX =
			SelectionView.SELECTION_VIEW_EXPLORATION.ordinal();
	// The name of the shared preferences namespace.
	private static final String PREFERENCES = "SelectionView";
	// The name of the preference which stores the index of the current selection view.
	private static final String SELECTION_VIEW_INDEX = "CurrentSelectionViewIndex";

	// We use a fragment cache outside of the enum to avoid a memory leak.
	private final Fragment[] fragmentCache = new Fragment[SelectionView.values().length];

	// The current selectionView.
	private SelectionView currentView;

	/**
	 * The constructor sets the current selection view to the one stores in the
	 * preferences or to the default.
	 */
	public SelectionViewManager(Context context) {
		int storedSelectionViewIndex = 0;
		currentView = SelectionView.values()[storedSelectionViewIndex];
	}

	/**
	 * Sets the current selection view. Returns the fragment (cached or created
	 * if needed) and saves the state in the preferences.
	 */
	public void setSelectionView(SelectionView newView) {
		this.currentView = newView;
	}

	/**
	 * Returns the current selection view.
	 */
	public SelectionView getCurrentSelectionView() {
		return currentView;
	}

	public Fragment getCurrentSelectionViewFragment() {
		int ordinal = currentView.ordinal();
		if (fragmentCache[ordinal] == null) {
			try {
				fragmentCache[ordinal] = currentView.getFragmentClass().newInstance();
			} catch (InstantiationException e) {
			} catch (IllegalAccessException e) {
			}
		}
		return fragmentCache[ordinal];
	}
}
