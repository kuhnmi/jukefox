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

import android.app.ActionBar;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StrictMode;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.SearchView;
import android.widget.Spinner;
import ch.ethz.dcg.jukefox.commons.utils.AndroidUtils;
import ch.ethz.dcg.pancho3.R;
import ch.ethz.dcg.pancho3.controller.eventhandlers.TabletActivityEventListener;
import ch.ethz.dcg.pancho3.model.JukefoxApplication;
import ch.ethz.dcg.pancho3.tablet.TabletFactory;
import ch.ethz.dcg.pancho3.tablet.TabletFactory.TabletFactoryGetter;
import ch.ethz.dcg.pancho3.tablet.ViewServer;
import ch.ethz.dcg.pancho3.tablet.presenter.TabletPresenter;
import ch.ethz.dcg.pancho3.tablet.presenter.TabletPresenter.IMainView;
import ch.ethz.dcg.pancho3.tablet.presenter.TabletPresenter.SelectionView;
import ch.ethz.dcg.pancho3.tablet.view.queue.QueueFragment;
import ch.ethz.dcg.pancho3.view.commons.JukefoxActivity;

/**
 * The main activity of the tablet interface. Its interface has four main
 * components: - The queue: The queue displays the play queue with the current
 * song on top and the next songs below it. The queue supports dragging and
 * swiping gestures and play control - The selection view: The space next to the
 * queue is used for the selection view. Those can be different views for
 * selecting music. Also drag&drop to the queue is supported. - The action bar:
 * The interface supports the action bar from the honeycomb interface. One
 * primary use case of the action bar are the tabs to select different selection
 * views.
 */
public class TabletActivity extends JukefoxActivity implements TabletFactoryGetter, IMainView {

	private boolean DEBUG = true;
	public static final String TAG = TabletActivity.class.getSimpleName();
	private TabletActivityEventListener eventListener;
	private TabletPresenter presenter;
	private TabletFactory tabletFactory;
	private boolean displayMapMenuItem = true;

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (DEBUG) {
			ViewServer.get(this).removeWindow(this);
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		if (DEBUG) {
			ViewServer.get(this).setFocusedWindow(this);
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_ACTION_BAR);
		setContentView(R.layout.tablet_main);
		if (DEBUG) {
			StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
			StrictMode.setThreadPolicy(policy);
			ViewServer.get(this).addWindow(this);
		}
		final JukefoxApplication application = (JukefoxApplication) getApplication();
		new AsyncTask<Void, Void, Void>() {

			@Override
			protected Void doInBackground(Void... params) {
				applicationState.waitForPlaybackFunctionality();
				return null;
			}

			@Override
			protected void onPostExecute(Void result) {
				tabletFactory = new TabletFactory(TabletActivity.this, application);
				presenter = tabletFactory.getTabletPresenter();
				presenter.viewFinishedInit();
				eventListener = tabletFactory.getEventListener();
				if (!AndroidUtils.isSdCardOk()) {
					eventListener.sdCardProblemDetected();
					finish();
					return;
				}
				if (applicationState.isFirstStart()) {
					eventListener.detectedFirstStart();
				}
				((QueueFragment) getFragmentManager().findFragmentById(R.id.queue)).initialize();
			}
		}.execute();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		presenter.exploreAllAlbumsMaybe();
		return true;
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		if (applicationState.isImporting()) {
			showStatusInfo(getString(R.string.jukefox_is_currently_importing));
		}
	}

	@Override
	public void clearLocalUI() {
		FragmentManager fragmentManager = getFragmentManager();
		while (fragmentManager.getBackStackEntryCount() > 0) {
			// We still have an overlay or search fragment. Remove it.
			fragmentManager.popBackStackImmediate();
		}
	}

	@Override
	public void displayFragment(SelectionView selectionView) {
		FragmentManager fragmentManager = getFragmentManager();
		Fragment fragment = fragmentManager.findFragmentByTag(selectionView.getTag());
		FragmentTransaction ft = fragmentManager.beginTransaction();
		boolean needsCommit = true;
		if (fragment == null) {
			try {
				fragment = selectionView.getFragmentClass().newInstance();
				ft.add(R.id.viewmodeholder, fragment, selectionView.getTag());
			} catch (InstantiationException e) {
			} catch (IllegalAccessException e) {
			}
		} else if (fragment.isHidden()) {
			ft.show(fragment);
		} else {
			needsCommit = false;
		}
		if (needsCommit) {
			for (SelectionView otherView : SelectionView.values()) {
				if (!otherView.equals(selectionView)) {
					Fragment otherFragment = fragmentManager.findFragmentByTag(otherView.getTag());
					if (otherFragment != null) {
						ft.hide(otherFragment);
					}
				}
			}
			ft.commit();
		}
	}

	/**
	 * We need a title bar since we have the action bar.
	 */
	@Override
	protected boolean hideTitleBar() {
		return false;
	}

	@Override
	public TabletFactory getTabletFactory() {
		return tabletFactory;
	}

	@Override
	public boolean isTabletFactoryReady() {
		return tabletFactory != null;
	}

	@Override
	public void displayOverlay(Fragment overlayFragment) {
		FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
		fragmentTransaction.add(R.id.viewmodeholder, overlayFragment);
		fragmentTransaction.addToBackStack(getString(R.string.overlay_backstack_name));
		fragmentTransaction.commit();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		final SearchView searchView = new SearchView(this);
		searchView.setOnSearchClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				searchView.setIconified(true);
				presenter.displaySearch();
			}
		});
		menu.add(R.string.search_menu_item_name).setOnMenuItemClickListener(
				new OnMenuItemClickListener() {

					@Override
					public boolean onMenuItemClick(MenuItem item) {
						presenter.displaySearch();
						return true;
					}
				}).setActionView(searchView).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		if (displayMapMenuItem) {
			menu.add("Music Map").setIcon(R.drawable.d169_map).setOnMenuItemClickListener(
					new OnMenuItemClickListener() {

						@Override
						public boolean onMenuItemClick(MenuItem item) {
							presenter.mapMaybe();
							return true;
						}
					}).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		}
		Spinner spinner = new Spinner(this);
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
				this, R.array.autofill_array, android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(adapter);
		menu.add("Autofill").setActionView(spinner).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		spinner.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				// + 1 because we auto fill the "coming up" songs, so the "now playing" song
				// does not count.
				tabletFactory.getMagicPlaylistController().setAutofillNumberOfSongs(
						JukefoxApplication.getPlayerController(), position + 1);
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {

			}
		});
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			if (presenter.handleBackButton()) {
				return true;
			}
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public void updateActionBar(String title, boolean displayHomeAsUpEnabled,
			boolean displayMapMenuItem, int iconResId) {
		ActionBar actionBar = getActionBar();
		actionBar.setTitle(title);
		actionBar.setDisplayHomeAsUpEnabled(displayHomeAsUpEnabled);
		//actionBar.setIcon(iconResId);
		this.displayMapMenuItem = displayMapMenuItem;
		invalidateOptionsMenu();
	}
}
