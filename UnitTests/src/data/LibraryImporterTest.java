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

import static org.junit.Assert.fail;

import org.junit.Test;

import ch.ethz.dcg.jukefox.manager.DirectoryManager;
import ch.ethz.dcg.jukefox.manager.libraryimport.LibraryChanges;
import ch.ethz.dcg.jukefox.manager.libraryimport.LibraryImportManager;
import ch.ethz.dcg.jukefox.manager.libraryimport.LibraryScanner;
import ch.ethz.dcg.jukefox.model.CollectionModelManager;
import ch.ethz.dcg.jukefox.model.libraryimport.ImportState;

public class LibraryImporterTest {
	
	//-----------------------------------------------------------
	// DO NOT EXECUTE ALL TESTS... ONLY ONE
	//-----------------------------------------------------------

	@Test
	public void testLibraryScanner()
	{
		DirectoryManager directoryManager = new DirectoryManager();
		directoryManager.createAllDirectories();
		CollectionModelManager collectionModelManager = new CollectionModelManager(directoryManager);
		
		//-----------------------------------------------------------
		// set your path once, after it's stored in data/settings.set
		//-----------------------------------------------------------
//		final String libraryPath = "D:\\Musik\\...";
//		HashSet<String> libraryPaths = new HashSet<String>();
//		libraryPaths.add(libraryPath);
//		ModelSettingsManager.setLibraryPaths(libraryPaths);
		
		ImportState importState = new ImportState();
		LibraryScanner libScan = new LibraryScanner(collectionModelManager, importState);
		LibraryChanges libChanges = null;
		try {
			libScan.scan();
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		
		try {
			libChanges = libScan.getLibraryChanges();
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		
		// Test whether lib changes are correct
		// For now testing against null to get rid of the unused warning.
		if (libChanges != null) {
			
		}
	}
	
	@Test
	public void testDoImport()
	{
		DirectoryManager directoryManager = new DirectoryManager();
		directoryManager.createAllDirectories();
		CollectionModelManager collectionModelManager = new CollectionModelManager(directoryManager);

		//-----------------------------------------------------------
		// set your path once, after it's stored in data/settings.set
		//-----------------------------------------------------------
//		final String libraryPath = "D:\\Musik\\...";
//		HashSet<String> libraryPaths = new HashSet<String>();
//		libraryPaths.add(libraryPath);
//		ModelSettingsManager.setLibraryPaths(libraryPaths);

		LibraryImportManager libraryImportManager = collectionModelManager.getLibraryImportManager();

		try {
			libraryImportManager.doImportAsync(false, false);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}
}
