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
package data;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.DataInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.List;

import junit.framework.Assert;

import org.junit.Test;

import ch.ethz.dcg.jukefox.commons.Constants;
import ch.ethz.dcg.jukefox.commons.DataUnavailableException;
import ch.ethz.dcg.jukefox.commons.DataWriteException;
import ch.ethz.dcg.jukefox.data.ResourceLoader;
import ch.ethz.dcg.jukefox.manager.DirectoryManager;
import ch.ethz.dcg.jukefox.model.collection.CompleteTag;

public class ResourceLoaderTest {

	private final DirectoryManager directoryManager = new DirectoryManager();
	private final ResourceLoader resourceLoader = new ResourceLoader(directoryManager);

	@Test
	public void testOpenInputStream() {
		InputStream fis = null;

		try {
			fis = directoryManager.getArtistDataResourceInputStream();
			DataInputStream dis = new DataInputStream(fis);
			assertNotNull(fis);
			assertNotNull(dis);
		} catch (Exception e) {
			fail(e.getMessage());
		} finally {
			if (fis != null)
				try {
					fis.close();
				} catch (Exception e) {
				}
		}
	}

	@Test
	public void testWriteTagsToSdCard() {
		final File tagFile = directoryManager.getTagFile();

		// delete file if it already exists.
		if (tagFile.exists()) {
			assertTrue(tagFile.delete());
		}

		try {
			resourceLoader.writeTagsToSdCard();
		} catch (DataWriteException dataWriteException) {
			fail(dataWriteException.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		} finally {
			if (tagFile.exists()) {
				assertTrue(tagFile.delete());
			}
		}
	}

	@Test
	public void testReadTags() {

		final File tagFile = directoryManager.getTagFile();

		// delete file if it already exists.
		if (tagFile.exists()) {
			assertTrue(tagFile.delete());
		}

		try {
			resourceLoader.writeTagsToSdCard();
		} catch (DataWriteException dataWriteException) {
			fail(dataWriteException.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		List<CompleteTag> completeTags = null;

		try {
			completeTags = resourceLoader.readTags();
		} catch (DataUnavailableException e) {
			fail(e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		Assert.assertNotNull(completeTags);
		Assert.assertEquals(Constants.NUM_TAGS, completeTags.size());
		CompleteTag completeTag = null;
		for (int i = 0; i < completeTags.size(); i++) {
			completeTag = completeTags.get(i);
			Assert.assertNotNull(completeTag);
			Assert.assertNotNull(completeTag.getName());
			Assert.assertNotNull(completeTag.getId());
			Assert.assertNotNull(completeTag.getPlsaCoords());
			Assert.assertEquals(Constants.DIM, completeTag.getPlsaCoords().length);
		}
	}

}
