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

import junit.framework.Assert;

import org.junit.Test;

import ch.ethz.dcg.jukefox.data.context.AbstractContextResult;
import ch.ethz.dcg.jukefox.data.context.ContextResult;

public class TestContextResult {

	private ContextResult createTestContextResult(Float testFloatValue, int testIntValue, String expectedStringValue){
		ContextResult testContextResult = new ContextResult();
		testContextResult.setAccelerationEnergy(testFloatValue);
		testContextResult.setLight(testFloatValue);
		testContextResult.setNetworkState(expectedStringValue);
		testContextResult.setOrientation(testIntValue);
		testContextResult.setOrientationChange(testFloatValue);
		testContextResult.setProximity(testFloatValue);
		
		return testContextResult;
	}
	
	@Test
	public void testCreateDbString() {
		Float testFloatValue = 123.123f;
		int testIntValue = 77;
		String expectedStringValue = "This is it!";
		
		ContextResult testContextResult = createTestContextResult(testFloatValue, testIntValue, expectedStringValue);
		
		String actualDbString = testContextResult.createDbString();
		
		String expectedDbString =
				AbstractContextResult.PLATFORM + "=" + AbstractContextResult.PC + ";" +
				AbstractContextResult.NETWORK_STATE + "=" + expectedStringValue;
		
		Assert.assertEquals(expectedDbString, actualDbString);
	}
	
	@Test
	public void testParseDbString() {
		Float testFloatValue = 123.123f;
		int testIntValue = 77;
		String expectedStringValue = "This is it!";
		
		ContextResult expectedContextResult = createTestContextResult(testFloatValue, testIntValue, expectedStringValue);
		
		String dbString = expectedContextResult.createDbString();
		
		ContextResult actualContextResult = new ContextResult();
		actualContextResult.parseDbString(dbString);
		
		Assert.assertNull(actualContextResult.getAccelerationEnergy());
		Assert.assertNull(actualContextResult.getLight());
		Assert.assertNull(actualContextResult.getOrientation());
		Assert.assertNull(actualContextResult.getOrientationChange());
		Assert.assertNull(actualContextResult.getProximity());
		Assert.assertNotNull(actualContextResult.getNetworkState());
		
		Assert.assertEquals(expectedStringValue, actualContextResult.getNetworkState());
	}

}
