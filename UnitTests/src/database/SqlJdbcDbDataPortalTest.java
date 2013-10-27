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
package database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import ch.ethz.dcg.jukefox.commons.DataUnavailableException;
import ch.ethz.dcg.jukefox.commons.DataWriteException;
import ch.ethz.dcg.jukefox.commons.utils.Pair;
import ch.ethz.dcg.jukefox.data.cache.PreloadedSongInfo;
import ch.ethz.dcg.jukefox.data.db.SqlJdbcDbDataPortal;
import ch.ethz.dcg.jukefox.manager.DirectoryManager;
import ch.ethz.dcg.jukefox.model.collection.BaseAlbum;
import ch.ethz.dcg.jukefox.model.collection.BaseArtist;
import ch.ethz.dcg.jukefox.model.collection.BaseSong;
import ch.ethz.dcg.jukefox.model.collection.Genre;
import ch.ethz.dcg.jukefox.model.libraryimport.ImportAlbum;
import ch.ethz.dcg.jukefox.model.libraryimport.ImportSong;
import ch.ethz.dcg.jukefox.model.player.playlog.PlayLogSendEntity;

public class SqlJdbcDbDataPortalTest {

	private static final String DB2_FILE = "res/museekDb02.copy";
	private static final String DB1_FILE = "res/museekDb01.copy";
	private final DirectoryManager directoryManager = new DirectoryManager();
	
	@Before
	public void before() throws IOException {
		copy("res/museekDb01", DB1_FILE);
		copy("res/museekDb02", DB2_FILE);
	}

	private void copy(String fromPath, String toPath) throws IOException {
		File f1 = new File(fromPath);
		File f2 = new File(toPath);
		f2.delete();
		f2.getParentFile().mkdirs();
		InputStream in = new FileInputStream(f1);

		// For Append the file.
		// OutputStream out = new FileOutputStream(f2,true);

		// For Overwrite the file.
		OutputStream out = new FileOutputStream(f2);

		byte[] buf = new byte[1024];
		int len;
		while ((len = in.read(buf)) > 0) {
			out.write(buf, 0, len);
		}
		in.close();
		out.close();
		System.out.println("File copied.");
	}
	
	@Test
	public void testClose() {
		File f = new File(DB1_FILE);
		SqlJdbcDbDataPortal db = new SqlJdbcDbDataPortal(directoryManager, "jdbc:sqlite:" + f.getPath());

		assertTrue(db.isOpen());

		db.close();

		assertTrue(!db.isOpen());
	}

	@Ignore
	@Test
	public void testExecSelect() {

		File f = new File(DB1_FILE);
		SqlJdbcDbDataPortal db = new SqlJdbcDbDataPortal(directoryManager, "jdbc:sqlite:" + f.getPath());
		BaseSong<BaseArtist, BaseAlbum> expected = createBaseSong_Feuerkind();
		BaseSong<BaseArtist, BaseAlbum> actual = null;

		try {
			actual = db.getBaseSongById(expected.getId());
		} catch (DataUnavailableException e) {
			fail(e.getMessage());
		}

		compareBaseSong(actual, expected);

	}

	@Ignore
	@Test
	public void testExecSelect2() {

		File f = new File(DB1_FILE);
		SqlJdbcDbDataPortal db = new SqlJdbcDbDataPortal(directoryManager, "jdbc:sqlite:" + f.getPath());
		BaseSong<BaseArtist, BaseAlbum> expected = createBaseSong_Feuerkind();

		List<Pair<Genre, Integer>> actual = null;
		try {
			actual = db.getGenresForArtist(expected.getArtist());
		} catch (DataUnavailableException e) {
			fail(e.getMessage());
		}

		// Test whether actual genres are the same as expected once
		// For now testing against null to get rid of the unused warning.
		if (actual != null) {

		}

	}

	@Ignore
	@Test
	public void testExecSelect3() {

		File f = new File(DB1_FILE);
		SqlJdbcDbDataPortal db = new SqlJdbcDbDataPortal(directoryManager, "jdbc:sqlite:" + f.getPath());
		PlayLogSendEntity entity = null;

		try {
			entity = db.getPlayLogString(0, 0, 0, 0);
		} catch (DataUnavailableException e) {
			fail(e.getMessage());
		}

		assertNotNull(entity);
	}

	@Ignore
	@Test
	public void testInsertOrThrow() {
		File f = new File(DB2_FILE);
		if (f.exists()) {
			f.delete();
			f = new File(DB2_FILE);
		}
		SqlJdbcDbDataPortal db = new SqlJdbcDbDataPortal(directoryManager, "jdbc:sqlite:" + f.getPath());
		BaseSong<BaseArtist, BaseAlbum> expected = createBaseSongForImport_Feuerkind();
		BaseSong<BaseArtist, BaseAlbum> actual = null;

		try {
			ImportSong s = createImportSong_Feuerkind();
			db.insertSong(s);
		} catch (DataWriteException e) {
			fail(e.getMessage());
		}

		try {
			actual = db.getBaseSongById(expected.getId());
		} catch (DataUnavailableException e) {
			fail(e.getMessage());
		}

		compareBaseSong(actual, expected);

	}

	@Ignore
	@Test
	public void testUpdate() {
		File f = new File(DB1_FILE);
		SqlJdbcDbDataPortal db = new SqlJdbcDbDataPortal(directoryManager, "jdbc:sqlite:" + f.getPath());
		File f2 = new File(DB2_FILE);
		SqlJdbcDbDataPortal db2 = new SqlJdbcDbDataPortal(directoryManager, "jdbc:sqlite:" + f2.getPath());
		BaseSong<BaseArtist, BaseAlbum> expected = createBaseSongForImport_Feuerkind();
		BaseSong<BaseArtist, BaseAlbum> source = createBaseSong_Feuerkind();
		float[] coords = null;
		try {
			PreloadedSongInfo test = null;
			List<PreloadedSongInfo> preload = db.getPreloadedSongInfo();
			for (int i = 0; i < preload.size(); i++) {
				test = preload.get(i);
				if (test.getSongId() == source.getId()) {
					break;
				}
			}
			coords = test.getSongPcaCoords();
			HashMap<Integer, float[]> songPcaCoords = new HashMap<Integer, float[]>();
			songPcaCoords.put(expected.getId(), coords);
			db2.updateSongsPcaCoords(songPcaCoords);
		} catch (DataWriteException e) {
			fail(e.getMessage());
		}

	}

	@Ignore
	@Test
	public void testDelete() {
		File f = new File(DB2_FILE);
		DirectoryManager directoryManager = new DirectoryManager();
		SqlJdbcDbDataPortal db = new SqlJdbcDbDataPortal(directoryManager, "jdbc:sqlite:" + f.getPath());

		try {
			db.deleteGenreSongMapping(1, 1);
		} catch (DataWriteException e) {
			fail(e.getMessage());
		}

	}

	private BaseSong<BaseArtist, BaseAlbum> createBaseSong_Feuerkind() {
		BaseArtist artist = new BaseArtist(1, "Subway To Sally");
		BaseAlbum album = new BaseAlbum(5, "Nord Nord Ost");
		BaseSong<BaseArtist, BaseAlbum> result = new BaseSong<BaseArtist, BaseAlbum>(29, "Feuerkind", artist, album,
				300);

		return result;
	}

	private BaseSong<BaseArtist, BaseAlbum> createBaseSongForImport_Feuerkind() {
		BaseArtist artist = new BaseArtist(1, "Subway To Sally");
		BaseAlbum album = new BaseAlbum(1, "Nord Nord Ost");
		BaseSong<BaseArtist, BaseAlbum> result = new BaseSong<BaseArtist, BaseAlbum>(1, "Feuerkind", artist, album, 300);

		return result;
	}

	private ImportSong createImportSong_Feuerkind() {
		String name = "Feuerkind";
		String artist = "Subway To Sally";
		ImportAlbum album = new ImportAlbum("Nord Nord Ost", artist);
		int duration = 366341;
		int track = 6;
		String path = "/mnt/sdcard/LOST.DIR/Subway to Sally/2005 - Nord Nord Ost/06 - Feuerkind.mp3";
		ImportSong song = new ImportSong(name, album, artist, path, duration, track, null, null, new Date());

		return song;
	}

	private void compareBaseSong(BaseSong<BaseArtist, BaseAlbum> baseSong1, BaseSong<BaseArtist, BaseAlbum> baseSong2) {
		assertEquals(baseSong1.getName(), baseSong2.getName());
		assertEquals(baseSong1.getId(), baseSong2.getId());
		compareBaseArtist(baseSong1.getArtist(), baseSong2.getArtist());
		compareBaseAlbum(baseSong1.getAlbum(), baseSong2.getAlbum());
	}

	private void compareBaseArtist(BaseArtist baseArtist1, BaseArtist baseArtist2) {
		assertEquals(baseArtist1.getName(), baseArtist2.getName());
		assertEquals(baseArtist1.getId(), baseArtist2.getId());
	}

	private void compareBaseAlbum(BaseAlbum baseAlbum1, BaseAlbum baseAlbum2) {
		assertEquals(baseAlbum1.getName(), baseAlbum2.getName());
		assertEquals(baseAlbum1.getId(), baseAlbum2.getId());
	}

}
