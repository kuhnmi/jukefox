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
package ch.ethz.dcg.pancho3.view.dialogs;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.view.View;
import android.view.WindowManager.LayoutParams;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;
import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.pancho3.R;
import ch.ethz.dcg.pancho3.controller.player.PlayerService;
import ch.ethz.dcg.pancho3.model.JukefoxApplication;

public class DirectorySelectionDialog extends Dialog {

	private final static String TAG = "DirectorySelectionDialog";
	public static final int MESSAGE_SET_TEXT = 1;
	private boolean[] selected;
	private ListView fileList;
	private String[] dirNames;
	private Context context;

	public interface CancelListener {

		public void onCancel();
	}

	public Handler mHandler;

	public DirectorySelectionDialog(Context context) {
		super(context);
		this.context = context;
		setContentView(R.layout.directoryselectiondialog);
		getWindow().setLayout(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);

		setTitle(context.getString(R.string.select_directories));

		readSdCardDirectory();

		initializeList(context);

		loadDirBlackList();

		fileList.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				selected[arg2] = !selected[arg2];

			}

		});
		fileList.bringToFront();

		this.setCancelable(true);

		initializeOkButton();

		initializeSelectionButtons();

	}

	private void readSdCardDirectory() {
		File topDirectory = JukefoxApplication.getDirectoryManager().getSdCardDirectory();
		File[] directories = topDirectory.listFiles();
		int numRelevantDirectories = 0;
		for (int i = 0; i < directories.length; i++) {
			if (isRelevant(directories[i])) {
				numRelevantDirectories++;
			}
		}
		dirNames = new String[numRelevantDirectories];
		int pos = 0;
		for (int i = 0; i < directories.length; i++) {
			if (isRelevant(directories[i])) {
				if (directories[i].isDirectory()) {
					dirNames[pos] = directories[i].getName() + "/";
				} else {
					dirNames[pos] = directories[i].getName();
				}
				pos++;
			}
		}
	}

	private void initializeSelectionButtons() {
		Button selectAllButton = (Button) findViewById(R.id.selectAllButton);
		selectAllButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				selectAll();
			}
		});

		Button deselectAllButton = (Button) findViewById(R.id.deselectAllButton);
		deselectAllButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				deselectAll();
			}
		});
	}

	private void initializeOkButton() {
		Button okButton = (Button) findViewById(R.id.dirOkButton);

		okButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				int numSelected = 0;
				for (int i = 0; i < dirNames.length; i++) {
					if (selected[i]) {
						numSelected++;
					}
				}
				String[] selectedDirNames = new String[numSelected];
				String[] deselectedDirNames = new String[dirNames.length - numSelected];
				int pos = 0;
				int posDel = 0;
				for (int i = 0; i < dirNames.length; i++) {
					if (selected[i]) {
						selectedDirNames[pos] = dirNames[i];
						// Log.v("DirSel", "Selected: " + selectedDirNames[pos]
						// + " " + selectedDirNames.length);
						pos++;
					} else {
						deselectedDirNames[posDel] = dirNames[i];
						posDel++;
					}
				}
				saveDirList(deselectedDirNames);

				Intent intent = new Intent(DirectorySelectionDialog.this.context, PlayerService.class);
				intent.setAction(PlayerService.ACTION_DO_IMPORT);
				DirectorySelectionDialog.this.context.startService(intent);
				dismiss();
			}
		});
	}

	private void initializeList(Context context) {
		fileList = (ListView) findViewById(R.id.directorylist);

		ArrayAdapter<String> adapter = new ArrayAdapter<String>(context, R.layout.playlistitem, dirNames) {
		};
		fileList.setAdapter(adapter);
		fileList.setItemsCanFocus(true);
		fileList.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
		// Mark a song as selected
		selected = new boolean[dirNames.length];

		for (int i = 0; i < dirNames.length; i++) {
			if (!dirNames[i].equals("albumthumbs")) {
				fileList.setItemChecked(i, true);
				selected[i] = true;
			}
		}
	}

	private boolean isRelevant(File file) {
		if (file.getName().startsWith(".")) {
			return false;
		}
		if (file.isDirectory()) {
			return true;
		}
		if (file.getName().endsWith(".mp3")) {
			return true;
		} else if (file.getName().endsWith(".ogg")) {
			return true;
		} else if (file.getName().endsWith(".3gp")) {
			return true;
		} else if (file.getName().endsWith(".mp4")) {
			return true;
		} else if (file.getName().endsWith(".m4a")) {
			return true;
		} else if (file.getName().endsWith(".aac")) {
			return true;
		} else if (file.getName().endsWith(".wav")) {
			return true;
		} else if (file.getName().endsWith(".wma")) {
			return true;
		}

		return false;
	}

	private void deselectAll() {
		for (int i = 0; i < dirNames.length; i++) {
			fileList.setItemChecked(i, false);
			selected[i] = false;
		}
	}

	private void selectAll() {
		for (int i = 0; i < dirNames.length; i++) {
			fileList.setItemChecked(i, true);
			selected[i] = true;
		}
	}

	private void loadDirBlackList() {

		File dirFile = JukefoxApplication.getDirectoryManager().getMusicDirectoriesBlacklistFile();

		if (!dirFile.exists()) {
			return;
		}

		selectAll();

		FileInputStream fileInput = null;
		DataInputStream dirStream = null;

		try {
			fileInput = new FileInputStream(dirFile);
			dirStream = new DataInputStream(fileInput);

			String line = null;

			while ((line = dirStream.readLine()) != null) {
				for (int i = 0; i < dirNames.length; i++) {
					if (line.equals(dirNames[i])) {
						fileList.setItemChecked(i, false);
						selected[i] = false;
					}
				}
			}

		} catch (Exception e) {
			Log.w(TAG, e);
		} finally {
			try {
				dirStream.close();
			} catch (Exception e) {
			}
		}
	}

	private void saveDirList(String[] dirNames) {

		File dirFile = JukefoxApplication.getDirectoryManager().getMusicDirectoriesBlacklistFile();
		FileOutputStream fileOutput = null;
		DataOutputStream dirStream = null;
		try {
			dirFile.delete();

			fileOutput = new FileOutputStream(dirFile);
			dirStream = new DataOutputStream(fileOutput);

			for (int i = 0; i < dirNames.length; i++) {
				// Log.v("Writing Sel", dirNames[i] + " " + dirNames.length);
				dirStream.writeBytes(dirNames[i] + "\n");
			}
		} catch (Exception e) {
			Log.w(TAG, e);
		} finally {
			try {
				dirStream.close();
			} catch (Exception e) {
				Log.w(TAG, e);
			}
		}
	}

	public static void appendFileBlacklistPath(String path) {
		File dirFile = JukefoxApplication.getDirectoryManager().getMusicFilesBlacklistFile();
		FileOutputStream fileOutput = null;
		DataOutputStream dirStream = null;
		try {

			fileOutput = new FileOutputStream(dirFile, true);
			dirStream = new DataOutputStream(fileOutput);
			dirStream.writeBytes(path + "\n");

		} catch (Exception e) {
			Log.w(TAG, e);
		} finally {
			try {
				dirStream.close();
			} catch (Exception e) {
				Log.w(TAG, e);
			}
		}
	}

}
