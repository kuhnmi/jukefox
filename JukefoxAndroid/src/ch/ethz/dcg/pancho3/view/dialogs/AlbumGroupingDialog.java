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
//package ch.ethz.dcg.pancho3.view.dialogs;
//import android.os.Bundle;
//import android.view.View;
//import android.view.View.OnClickListener;
//import android.widget.TextView;
//import ch.ethz.dcg.pancho3.R;
//import ch.ethz.dcg.pancho3.commons.data.music.BaseAlbum;
//import ch.ethz.dcg.pancho3.controller.Controller;
//
//
//public class AlbumGroupingDialog extends StandardDialog {
//	
//	private BaseAlbum album;
//	@Override
//	protected void onCreate(Bundle savedInstanceState) {
//		super.onCreate(savedInstanceState);
//	}
//
//	@Override
//	protected void processExtras(Bundle extras) {
//		if (extras == null) {
//			finish();
//			return;
//		}
//		setContentView();
//        setAlbum(extras);
//		TextView dialogText = (TextView)findViewById(R.id.dialogText);
//		String msg = getString(R.string.msg_really_group_album);
//		msg += album.getName() + "'?";
//		dialogText.setText(msg);
//	}
//	
//	private void setAlbum(Bundle extras) {
//		album = (BaseAlbum) extras.get(Controller.INTENT_EXTRA_BASE_ALBUM);
//		if (album == null) {
//			finish();
//			return;
//		}
//	}
//
//
//	protected void registerOkButton() {
//		findViewById(R.id.okButton).setOnClickListener(new OnClickListener() {
//			
//			@Override
//			public void onClick(View v) {
//				controller.groupAlbum(album.getName());
//				finish();
//			}
//
//		});
//	}
//	
//	
//}
