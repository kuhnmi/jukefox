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

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.HashSet;

import org.junit.Assert;
import org.junit.Test;

import ch.ethz.dcg.jukefox.data.settings.ModelSettings;
import ch.ethz.dcg.jukefox.manager.DirectoryManager;

public class SettingsTest {
	
	private final DirectoryManager directoryManager = new DirectoryManager();
	private final File serTestFile1 = directoryManager.getSettingsFile();
	
	private HashSet<String> createDefaultAlbumNamesToGroup(){
		HashSet<String> result = new HashSet<String>();
		result.add("Alpha");
		result.add("Beta");
		result.add("Gamma");
		return result;
	}
	
	private void compareDefaultSettings(ModelSettings settings)
	{
		compareSettings(settings, true, 42, 77, createDefaultAlbumNamesToGroup());
	}
	
	private void compareSettings(ModelSettings settings, boolean famousArtistsInserted,
			int numberOfStartedImports, int recomputeTaskId, HashSet<String> albumNamesToGroup)
	{
		Assert.assertNotNull(settings);
		Assert.assertEquals(famousArtistsInserted, settings.isFamousArtistsInserted());
		Assert.assertEquals(numberOfStartedImports, settings.getNumberOfStartedImports());
		Assert.assertEquals(recomputeTaskId, settings.getRecomputeTaskId());
		Assert.assertNotNull(albumNamesToGroup);
		Assert.assertNotNull(settings.getAlbumNamesToGroup());
		Assert.assertEquals(albumNamesToGroup.size(), settings.getAlbumNamesToGroup().size());
		for (int i = 0; i < albumNamesToGroup.size(); i++)
		{
			Assert.assertEquals(albumNamesToGroup.toArray()[i], settings.getAlbumNamesToGroup().toArray()[i]);
		}
	}
	
	private ModelSettings createDefaultTestSettingsFile(File serTestFile)
	{
		return createTestSettingsFile(serTestFile, true, 42, 77, createDefaultAlbumNamesToGroup());
	}
	
	private ModelSettings createTestSettingsFile(File serTestFile, boolean famousArtistsInserted,
			int numberOfStartedImports, int recomputeTaskId, HashSet<String> albumNamesToGroup)
	{
		if (serTestFile.exists())
		{
			serTestFile.delete();
		}
		
		ModelSettings testSettings = null;
		
		try {
			testSettings = new ModelSettings(serTestFile);
			Assert.assertTrue(serTestFile.exists());
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}

		testSettings.setFamousArtistsInserted(famousArtistsInserted);
		testSettings.setNumberOfStartedImports(numberOfStartedImports);
		testSettings.setRecomputeTaskId(recomputeTaskId);
		testSettings.setAlbumNamesToGroup(albumNamesToGroup);
		
		doubleCheckSettings(serTestFile, famousArtistsInserted, numberOfStartedImports, recomputeTaskId, albumNamesToGroup);
		
		return testSettings;
	}
	
	private void doubleCheckSettings(File serTestFile, boolean famousArtistsInserted,
			int numberOfStartedImports, int recomputeTaskId, HashSet<String> albumNamesToGroup)
	{
		Assert.assertTrue(serTestFile.exists());
		
		ObjectInputStream ois = null;
		FileInputStream fileIn = null;
		ModelSettings settings = null;
		
		try {
			fileIn = new FileInputStream(serTestFile);
			ois = new ObjectInputStream(fileIn);
			
			settings = (ModelSettings) ois.readObject();
			
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			if (ois != null) try { ois.close(); } catch (Exception e) {}
			if (fileIn != null) try { fileIn.close(); } catch (Exception e) {}
		}
		
		compareSettings(settings, famousArtistsInserted, numberOfStartedImports, recomputeTaskId, albumNamesToGroup);
	}
	
	/**
	 * Creates test settings without an existing file
	 */
	@Test
	public void testCreateSettingsWithoutFile()
	{
		if (serTestFile1.exists())
		{
			serTestFile1.delete();
		}
		
		ModelSettings testSettings = null;
		
		try {
			testSettings = new ModelSettings(serTestFile1);
			Assert.assertTrue(serTestFile1.exists());
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
		
		compareSettings(testSettings, false, 0, 0, new HashSet<String>());
		doubleCheckSettings(serTestFile1, false, 0, 0, new HashSet<String>());
		
		serTestFile1.delete();
	}
	
	/**
	 * Creates test settings with an already existing file
	 */
	@Test
	public void testCreateSettingsWithFile()
	{
		createDefaultTestSettingsFile(serTestFile1);
		
		ModelSettings testSettings = null;
		
		try {
			testSettings = new ModelSettings(serTestFile1);
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
		
		Assert.assertTrue(serTestFile1.exists());
		compareDefaultSettings(testSettings);
		
		serTestFile1.delete();
	}

	@Test
	public void testSetFamousArtistsInserted()
	{
		ModelSettings testSettings = createDefaultTestSettingsFile(serTestFile1);
		
		testSettings.setFamousArtistsInserted(false);
		
		compareSettings(testSettings, false, 42, 77, createDefaultAlbumNamesToGroup());
		doubleCheckSettings(serTestFile1, false, 42, 77, createDefaultAlbumNamesToGroup());
		
		serTestFile1.delete();
	}
	
	@Test
	public void testSetNumberOfStartedImports()
	{
		ModelSettings testSettings = createDefaultTestSettingsFile(serTestFile1);
		
		testSettings.setNumberOfStartedImports(88);
		
		compareSettings(testSettings, true, 88, 77, createDefaultAlbumNamesToGroup());
		doubleCheckSettings(serTestFile1, true, 88, 77, createDefaultAlbumNamesToGroup());
		
		serTestFile1.delete();
	}

	@Test
	public void testSetRecomputeTaskId()
	{
		ModelSettings testSettings = createDefaultTestSettingsFile(serTestFile1);
		
		testSettings.setRecomputeTaskId(11);
		
		compareSettings(testSettings, true, 42, 11, createDefaultAlbumNamesToGroup());
		doubleCheckSettings(serTestFile1, true, 42, 11, createDefaultAlbumNamesToGroup());
		
		serTestFile1.delete();
	}
	
	@Test
	public void testIncRecomputeTaskId()
	{
		ModelSettings testSettings = createDefaultTestSettingsFile(serTestFile1);
		
		testSettings.incRecomputeTaskId();
		
		compareSettings(testSettings, true, 42, 78, createDefaultAlbumNamesToGroup());
		doubleCheckSettings(serTestFile1, true, 42, 78, createDefaultAlbumNamesToGroup());
		
		serTestFile1.delete();
	}

	@Test
	public void testResetRecomputeTaskId()
	{
		ModelSettings testSettings = createDefaultTestSettingsFile(serTestFile1);
		
		testSettings.resetRecomputeTaskId();
		
		compareSettings(testSettings, true, 42, 0, createDefaultAlbumNamesToGroup());
		doubleCheckSettings(serTestFile1, true, 42, 0, createDefaultAlbumNamesToGroup());
		
		serTestFile1.delete();
	}
	
	@Test
	public void testAddAlbumNameToGroup()
	{
		ModelSettings testSettings = createDefaultTestSettingsFile(serTestFile1);
		String albumNameToGroup = "Omega";
		
		testSettings.addAlbumNameToGroup(albumNameToGroup);
		
		HashSet<String> defaultAlbumNamesToGroup = createDefaultAlbumNamesToGroup();
		HashSet<String> expectedAlbumNamesToGroup = new HashSet<String>();
		for (String string : defaultAlbumNamesToGroup) {
			expectedAlbumNamesToGroup.add(string);
		}
		expectedAlbumNamesToGroup.add(albumNameToGroup);
		
		compareSettings(testSettings, true, 42, 77, expectedAlbumNamesToGroup);
		doubleCheckSettings(serTestFile1, true, 42, 77, expectedAlbumNamesToGroup);
		
		serTestFile1.delete();
	}
}
