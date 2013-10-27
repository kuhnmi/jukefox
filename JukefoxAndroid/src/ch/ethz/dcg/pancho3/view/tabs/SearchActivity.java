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
package ch.ethz.dcg.pancho3.view.tabs;

import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnKeyListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import ch.ethz.dcg.pancho3.R;
import ch.ethz.dcg.pancho3.controller.eventhandlers.SearchEventListener;
import ch.ethz.dcg.pancho3.view.tabs.lists.TextSectionAdapter;

public class SearchActivity extends JukefoxTabActivity {
	
	private final static String TAG = SearchActivity.class.getSimpleName();

	public static final int NUM_PROPOSITIONS = 3;
	public static final int SEARCH_THRESH_MILLIS = 1000;

	private SearchEventListener eventListener;
	private EditText searchTerm;
	
	private Spinner searchInSelectionSpinner;

//	private RadioButton searchInArtists;
//	private RadioButton searchInAlbums;
//	private RadioButton searchInTitles;
//	private RadioButton searchInFamousArtists;

//	private ImageButton searchButton;
	private TextView resultListTitle;
	private ArrayAdapter<String> textAdapter;
	private String[] propositions = new String[0];
	private ListView resultList;
	private long lastAutomatedSearch;
	private Timer searchTimer;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.search);

		setCurrentTab(Tab.SEARCH);

		eventListener = controller.createSearchEventListener(this);

		registerEventListeners();

//		registerRadioButtons();
		int selectedPos = 2; // TODO: initialize this from intent
		registerSpinner(selectedPos);
	}

//	private void registerRadioButtons() {
//		searchInArtists = (RadioButton) findViewById(R.id.searchInArtists);
//		searchInAlbums = (RadioButton) findViewById(R.id.searchInAlbums);
//		searchInFamousArtists = (RadioButton) findViewById(R.id.searchInSimilarArtists);
//		searchInTitles = (RadioButton) findViewById(R.id.searchInTitles);
//		RadioGroup radioGroup = (RadioGroup) findViewById(R.id.searchCriterion);
//		radioGroup.setOnCheckedChangeListener(new OnCheckedChangeListener() {
//
//			@Override
//			public void onCheckedChanged(RadioGroup group, int checkedId) {
//				search();
//			}
//		});
//	}
	
	private void registerSpinner(int selectedPos) {
		searchInSelectionSpinner = (Spinner)findViewById(R.id.searchInSelectionSpinner);
		searchInSelectionSpinner.setSelection(selectedPos); // title
		searchInSelectionSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int pos, long id) {
				search();
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				// TODO: nothing to do (?)
			}
		});
//		searchInArtists = (RadioButton) findViewById(R.id.searchInArtists);
//		searchInAlbums = (RadioButton) findViewById(R.id.searchInAlbums);
//		searchInFamousArtists = (RadioButton) findViewById(R.id.searchInSimilarArtists);
//		searchInTitles = (RadioButton) findViewById(R.id.searchInTitles);
//		RadioGroup radioGroup = (RadioGroup) findViewById(R.id.searchCriterion);
//		radioGroup.setOnCheckedChangeListener(new OnCheckedChangeListener() {
//
//			@Override
//			public void onCheckedChanged(RadioGroup group, int checkedId) {
//				search();
//			}
//		});
	}


	private void search() {
		String searchTerm = getSearchTerm();
		if (searchTerm == null || searchTerm.trim().length() == 0) {
			return;
		}
//		eventListener.search(getSearchTerm(), searchInArtists
//				.isChecked(), searchInTitles.isChecked(), searchInFamousArtists
//				.isChecked(), searchInAlbums.isChecked());
		int selectedPos = searchInSelectionSpinner.getSelectedItemPosition();
		boolean searchInArtists = (selectedPos == 0);
		boolean searchInTitles = (selectedPos == 2);
		boolean searchInFamousArtists = (selectedPos == 3);
		boolean searchInAlbums = (selectedPos == 1);
		eventListener.search(getSearchTerm(), searchInArtists, searchInTitles,
				searchInFamousArtists, searchInAlbums);
	}

	private void registerEventListeners() {
		searchTerm = (EditText) findViewById(R.id.searchTerm);
		textAdapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_dropdown_item_1line, propositions);
		searchTerm.setOnKeyListener(new OnKeyListener() {

			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if (event.getAction() == KeyEvent.ACTION_DOWN
						&& keyCode == KeyEvent.KEYCODE_ENTER) {
					search();
					hideKeyboard();
					return true;
				} else if (event.getAction() == KeyEvent.ACTION_DOWN
						&& keyCode == KeyEvent.KEYCODE_BACK) {
					return false;
				}
				return false;
			}

		});
		setTextWatcher();
		resultList = (ListView) findViewById(R.id.resultList);
		resultList.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> list, View arg1,
					int position, long arg3) {
				switch (searchInSelectionSpinner.getSelectedItemPosition()) {
				case 0: eventListener.onArtistItemClicked(list, position); break;
				case 1: eventListener.onAlbumItemClicked(list, position); break;
				case 2: eventListener.onTitleItemClicked(list, position); break;
				case 3: eventListener.onFamousArtistItemClicked(list, position); break;
				}
//				eventListener.onItemClicked(list, position, searchInArtists
//						.isChecked(), searchInTitles.isChecked(),
//						searchInFamousArtists.isChecked(), searchInAlbums.isChecked());
			}
		});
		resultList.setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> list, View arg1,
					int position, long arg3) {
				switch (searchInSelectionSpinner.getSelectedItemPosition()) {
				case 0: return eventListener.onArtistItemLongClicked(list, position);
				case 1: return eventListener.onAlbumItemLongClicked(list, position);
				case 2: return eventListener.onTitleItemLongClicked(list, position);
				}
				return false;
//				return eventListener.onItemLongClicked(list, position, searchInArtists
//						.isChecked(), searchInTitles.isChecked(),
//						searchInFamousArtists.isChecked(), searchInAlbums.isChecked());
			}
		});
		resultListTitle = (TextView) findViewById(R.id.resultListTitle);
//		searchButton = (ImageButton) findViewById(R.id.searchButton);
//		searchButton.setOnClickListener(new OnClickListener() {
//
//			@Override
//			public void onClick(View v) {
//				eventListener.search(getSearchTerm(),
//						searchInArtists.isChecked(),
//						searchInTitles.isChecked(), searchInFamousArtists
//								.isChecked());
//				hideKeyboard();
//			}
//		});
	}
	
	private void hideKeyboard() {
		InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		inputManager.hideSoftInputFromWindow(searchTerm
				.getWindowToken(),
				InputMethodManager.HIDE_NOT_ALWAYS);
	}

	private String getSearchTerm() {
		return searchTerm.getText().toString();
	}

	private void setTextWatcher() {
		searchTerm.addTextChangedListener(new TextWatcher() {

			@Override
			public void afterTextChanged(Editable s) {
				if (settings.isSearchWhileTyping()) {
					resetSearchTimer();
				}
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
			}

		});
	}

	public void setResultList(String title, TextSectionAdapter adapter) {
		resultList.setFastScrollEnabled(false);
		resultList.setAdapter(adapter);
		resultList.setFastScrollEnabled(true);
	}

	private void resetSearchTimer() {
		cancelTimer();
		searchTimer = new Timer();
		searchTimer.schedule(new TimerTask() {

			@Override
			public void run() {
				search();
			}
		}, SEARCH_THRESH_MILLIS);
	}

	private void cancelTimer() {
		if (searchTimer != null) {
			searchTimer.cancel();
		}
	}

	public TextView getResultListTitle() {
		return resultListTitle;
	}

	public void showFamousArtistsNotReadyInfo() {
		showStatusInfo(getString(R.string.famous_artists_not_ready));
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (eventListener.onKey(keyCode, event)) {
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	protected void onPause() {
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
		super.onPause();
	}

	@Override
	protected void onResume() {
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
		super.onResume();
	}
	
	

}
