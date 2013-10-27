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
package ch.ethz.dcg.pancho3.view.overlays;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.Preference.OnPreferenceClickListener;
import android.provider.MediaStore;
import android.widget.Toast;
import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.jukefox.controller.IViewController;
import ch.ethz.dcg.jukefox.manager.AndroidSettingsManager;
import ch.ethz.dcg.jukefox.model.AndroidCollectionModelManager;
import ch.ethz.dcg.pancho3.R;
import ch.ethz.dcg.pancho3.commons.settings.ISettingsReader;
import ch.ethz.dcg.pancho3.model.AndroidApplicationState;
import ch.ethz.dcg.pancho3.model.JukefoxApplication;
import ch.ethz.dcg.pancho3.view.dialogs.DirectorySelectionDialog;
import ch.ethz.dcg.pancho3.view.dialogs.GaplessSettingsDialog;
import ch.ethz.dcg.pancho3.view.dialogs.ScrobbleIntervalDialog;

public class JukefoxPreferenceActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener {

	public static final String TAG = JukefoxPreferenceActivity.class.getSimpleName();

	private static final int ACTIVITY_SELECT_IMAGE = 0;

	protected AndroidApplicationState applicationState;
	protected AndroidCollectionModelManager data;
	// protected PlayerController playerController;
	protected ISettingsReader settings;
	protected JukefoxApplication application;
	protected IViewController controller;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		application = (JukefoxApplication) getApplication();
		applicationState = JukefoxApplication.getCollectionModel().getApplicationStateManager()
				.getApplicationStateController();
		data = JukefoxApplication.getCollectionModel();
		// playManager = application.getModel().getPlayManager();
		settings = AndroidSettingsManager.getAndroidSettingsReader();
		controller = application.getController();

		setRequestedOrientation(settings.getPreferredScreenOrientation());

		this.addPreferencesFromResource(R.xml.preferences);

		registerListeners();

		registerDirectorySelectionListener();

		registerNotificationListener();

		updateInternalScrobbleSettingsState();
		settings.addSettingsChangeListener(this);

	}

	private void registerDirectorySelectionListener() {
		Preference directorySelectionPref = findPreference(getString(R.string.KEY_DIRECTORY_SELECTION));
		directorySelectionPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {

			public boolean onPreferenceClick(Preference preference) {
				DirectorySelectionDialog dsd = new DirectorySelectionDialog(JukefoxPreferenceActivity.this);
				dsd.show();
				return true;
			}

		});
		Preference scrobbleIntervalPref = findPreference(getString(R.string.KEY_SCROBBLE_INTERVAL));
		scrobbleIntervalPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {

			public boolean onPreferenceClick(Preference preference) {
				Intent intent = new Intent(JukefoxPreferenceActivity.this, ScrobbleIntervalDialog.class);
				startActivity(intent);
				return true;
			}

		});
	}

	private void registerNotificationListener() {
		// TODO Auto-generated method stub

	}

	private void registerListeners() {
		CheckBoxPreference gaplessPref = (CheckBoxPreference) findPreference(getString(R.string.KEY_GAPLESS));
		gaplessPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {

			public boolean onPreferenceClick(Preference preference) {
				CheckBoxPreference checkBoxPreference = (CheckBoxPreference) preference;
				if (checkBoxPreference.isChecked()) {
					// GaplessSettingsDialog gpsd = new
					// GaplessSettingsDialog(JukefoxPreferenceActivity.this);
					// gpsd.show();
					Intent intent = new Intent(JukefoxPreferenceActivity.this, GaplessSettingsDialog.class);
					startActivity(intent);
				}
				return true;
			}

		});
		CheckBoxPreference backgroundPref = (CheckBoxPreference) findPreference(getString(R.string.KEY_PICK_BACKGROUND_FROM_GALLERY));
		backgroundPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {

			public boolean onPreferenceClick(Preference preference) {
				CheckBoxPreference checkBoxPreference = (CheckBoxPreference) preference;
				if (checkBoxPreference.isChecked()) {
					doBackgroundSelection();
				}
				return true;
			}

		});
		CheckBoxPreference screenOrientationPref = (CheckBoxPreference) findPreference(getString(R.string.KEY_FIX_SCREEN_ORIENTATION));
		screenOrientationPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {

			public boolean onPreferenceClick(Preference preference) {
				CheckBoxPreference checkBoxPreference = (CheckBoxPreference) preference;
				SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(application);
				SharedPreferences.Editor editor = preferences.edit();
				if (checkBoxPreference.isChecked()) {
					int orientation = getResources().getConfiguration().orientation;
					if (orientation == Configuration.ORIENTATION_PORTRAIT) {
						editor.putInt(application.getString(R.string.KEY_SCREEN_ORIENTATION),
								ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
						Log.v(TAG, "Settings screen  orientation to portrait");
						Toast toast = Toast.makeText(application, application
								.getString(R.string.fixed_orientation_to_portrait), Toast.LENGTH_LONG);
						toast.show();
					} else {
						editor.putInt(application.getString(R.string.KEY_SCREEN_ORIENTATION),
								ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
						Log.v(TAG, "Settings screen  orientation to landscape");
						Toast toast = Toast.makeText(application, application
								.getString(R.string.fixed_orientation_to_landscape), Toast.LENGTH_LONG);
						toast.show();
					}
				} else {
					editor.putInt(application.getString(R.string.KEY_SCREEN_ORIENTATION),
							ActivityInfo.SCREEN_ORIENTATION_SENSOR);
					Log.v(TAG, "Settings screen  orientation to sensor");
					Toast toast = Toast.makeText(application, application
							.getString(R.string.screen_orientation_not_fixed), Toast.LENGTH_LONG);
					toast.show();
				}
				editor.commit();
				setRequestedOrientation(settings.getPreferredScreenOrientation());
				return true;
			}

		});
		CheckBoxPreference mediaButtonPref = (CheckBoxPreference) findPreference(getString(R.string.KEY_IGNORE_MEDIA_BUTTONS));
		mediaButtonPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {

			public boolean onPreferenceClick(Preference preference) {
				CheckBoxPreference checkBoxPreference = (CheckBoxPreference) preference;
				File f = JukefoxApplication.getDirectoryManager().getIgnoreMediaButtonsFile();
				if (checkBoxPreference.isChecked()) {
					try {
						FileOutputStream fin = new FileOutputStream(f, true);
						fin.close();
					} catch (FileNotFoundException e) {
						Log.w(TAG, e);
					} catch (IOException e) {
						Log.w(TAG, e);
					}
				} else {
					f.delete();
				}
				return true;
			}

		});
	}

	private void doBackgroundSelection() {
		Intent i = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI);
		startActivityForResult(i, ACTIVITY_SELECT_IMAGE);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
		super.onActivityResult(requestCode, resultCode, imageReturnedIntent);

		switch (requestCode) {
			case ACTIVITY_SELECT_IMAGE:
				if (resultCode == RESULT_OK) {
					Uri selectedImage = imageReturnedIntent.getData();
					String[] filePathColumn = { MediaStore.Images.Media.DATA };

					Cursor cursor = getContentResolver().query(selectedImage, filePathColumn, null, null, null);
					if (cursor == null) {
						Toast.makeText(this, R.string.image_not_found, Toast.LENGTH_LONG).show();
						return;
					}
					cursor.moveToFirst();

					int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
					String filePath = cursor.getString(columnIndex);
					cursor.close();

					Log.v(TAG, "Selected file: " + filePath);

					// Bitmap yourSelectedImage =
					// BitmapFactory.decodeFile(filePath);

					SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(application);
					SharedPreferences.Editor editor = preferences.edit();
					editor.putString(application.getString(R.string.KEY_GALLERY_BACKGROUND_PATH), filePath);
					editor.commit();
				}
		}
	}

	public IViewController getController() {
		return controller;
	}

	public ISettingsReader getSettingsReader() {
		return settings;
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		Log.v(TAG, "onSharedPreferencesChanged: " + key);
		if (getString(R.string.KEY_SCROBBLE_TYPE).equals(key)) {
			updateInternalScrobbleSettingsState();
		}
	}

	private void updateInternalScrobbleSettingsState() {
		if (settings.isInternalScrobblingEnabled()) {
			setInternalScrobbleSettingsEnabled(true);
		} else {
			setInternalScrobbleSettingsEnabled(false);
		}
	}

	private void setInternalScrobbleSettingsEnabled(boolean enabled) {
		Preference scrobbleUser = findPreference(getString(R.string.KEY_SCROBBLE_USERNAME));
		scrobbleUser.setEnabled(enabled);
		Preference scrobblePwd = findPreference(getString(R.string.KEY_SCROBBLE_PWD));
		scrobblePwd.setEnabled(enabled);
		Preference scrobbleInterval = findPreference(getString(R.string.KEY_SCROBBLE_INTERVAL));
		scrobbleInterval.setEnabled(enabled);
		Preference scrobblePaused = findPreference(getString(R.string.KEY_SCROBBLE_PAUSED));
		scrobblePaused.setEnabled(enabled);
		Log.v(TAG, "scrobble credentials are now enabled: " + enabled);
	}

}
