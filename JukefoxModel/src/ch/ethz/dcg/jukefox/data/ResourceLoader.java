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
package ch.ethz.dcg.jukefox.data;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import ch.ethz.dcg.jukefox.commons.Constants;
import ch.ethz.dcg.jukefox.commons.DataUnavailableException;
import ch.ethz.dcg.jukefox.commons.DataWriteException;
import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.jukefox.data.db.IDbDataPortal;
import ch.ethz.dcg.jukefox.manager.DirectoryManager;
import ch.ethz.dcg.jukefox.model.collection.CompleteTag;

public class ResourceLoader {

	private final static String TAG = ResourceLoader.class.getSimpleName();

	private final DirectoryManager directoryManager;

	public ResourceLoader(DirectoryManager directoryManager) {
		this.directoryManager = directoryManager;
	}

	/**
	 * Used for MuseekSlideshow
	 */
	public void writeTagsToSdCard() throws DataWriteException {
		InputStream fis = null;
		DataInputStream dis = null;

		try {
			fis = directoryManager.getTagDataResourceInputStream();
			dis = new DataInputStream(fis);
			List<CompleteTag> list = new ArrayList<CompleteTag>();

			for (int i = 0; i < Constants.NUM_TAGS; i++) {
				int meId = dis.readInt();
				String name = dis.readUTF();
				float[] coords = new float[Constants.DIM];
				coords = readCoords(dis, coords);
				CompleteTag tag = new CompleteTag(-1, name, meId, null, null, null, null, null, coords, false);
				list.add(tag);
			}
			writeTagsToSdCard(list); // used for MuseekSlideshow

		} catch (IOException e) {
			throw new DataWriteException(e);
		} finally {
			if (dis != null) {
				try {
					dis.close();
				} catch (Exception e) {
				}
			}
			if (fis != null) {
				try {
					fis.close();
				} catch (Exception e) {
				}
			}
		}
	}

	public List<CompleteTag> readTags() throws DataUnavailableException {
		InputStream fis = null;
		DataInputStream dis = null;
		List<CompleteTag> list = new ArrayList<CompleteTag>();

		try {
			fis = directoryManager.getTagDataResourceInputStream();
			dis = new DataInputStream(fis);
			for (int i = 0; i < Constants.NUM_TAGS; i++) {
				int meId = dis.readInt();
				String name = dis.readUTF();
				float[] coords = new float[Constants.DIM];
				coords = readCoords(dis, coords);
				CompleteTag tag = new CompleteTag(-1, name, meId, null, null, null, null, null, coords, false);
				list.add(tag);
			}
		} catch (Exception e) {
			throw new DataUnavailableException(e);
		} finally {
			if (dis != null) {
				try {
					dis.close();
				} catch (Exception e) {
				}
			}
			if (fis != null) {
				try {
					fis.close();
				} catch (Exception e) {
				}
			}
		}

		return list;
	}

	private void writeTagsToSdCard(List<CompleteTag> list) {
		FileWriter writer = null;

		try {
			writer = new FileWriter(directoryManager.getTagFile());
			for (CompleteTag tag : list) {
				writer.write(tag.getName());
				float[] coords = tag.getPlsaCoords();
				for (int i = 0; i < Constants.DIM; i++) {
					writer.write(";" + coords[i]);
				}
				writer.write("\n");
			}
		} catch (Exception e) {
			Log.w(TAG, e);
		} finally {
			if (writer != null) {
				try {
					writer.close();
				} catch (Exception e2) {
				}
			}
		}
	}

	public void loadFamousArtists(IDbDataPortal dbDataPortal) throws DataUnavailableException {
		InputStream fis = null;
		DataInputStream dis = null;

		final int ARTISTS_PER_BATCH = 100;

		try {
			fis = directoryManager.getArtistDataResourceInputStream();
			dis = new DataInputStream(fis);

			Log.v(TAG, "inserting famous artists");
			for (int lowerBound = 0; lowerBound < Constants.NUM_ARTISTS; lowerBound += ARTISTS_PER_BATCH) {
				int upperBound = lowerBound + ARTISTS_PER_BATCH;
				if (upperBound > Constants.NUM_ARTISTS) {
					upperBound = Constants.NUM_ARTISTS;
				}

				int[] meIds = new int[ARTISTS_PER_BATCH];
				String[] names = new String[ARTISTS_PER_BATCH];
				float[][] allcoords = new float[ARTISTS_PER_BATCH][];
				int j = 0;

				dbDataPortal.beginTransaction();
				try {
					for (int i = lowerBound; i < upperBound; i++) {
						float[] coords = new float[Constants.DIM];
						int meId = dis.readInt();
						String name = dis.readUTF();
						readCoords(dis, coords);

						meIds[j] = meId;
						names[j] = name;
						allcoords[j] = coords;
						j++;
					}
					dbDataPortal.batchInsertFamousArtists(meIds, names, allcoords);
					dbDataPortal.setTransactionSuccessful();
				} catch (DataWriteException e) {
					throw new DataUnavailableException(e);
				} finally {
					dbDataPortal.endTransaction();
				}
			}

		} catch (IOException e) {
			throw new DataUnavailableException(e);
		} finally {
			if (dis != null) {
				try {
					dis.close();
				} catch (Exception e) {
				}
			}
			if (fis != null) {
				try {
					fis.close();
				} catch (Exception e) {
				}
			}
		}
	}

	private float[] readCoords(DataInputStream dis, float[] coords) throws IOException {
		for (int i = 0; i < Constants.DIM; i++) {
			coords[i] = dis.readFloat();
		}
		return coords;
	}

	public void writeDataToFile() {
		InputStream fis = null;
		DataInputStream dis = null;
		OutputStream fos = null;
		DataOutputStream dos = null;

		try {

			fos = new FileOutputStream(new File("famousArtists.dat"));
			fis = directoryManager.getArtistDataResourceInputStream();
			dis = new DataInputStream(fis);
			dos = new DataOutputStream(dos);

			float[] coords = new float[Constants.DIM];
			Log.v(TAG, "inserting famous artists");
			for (int lowerBound = 0; lowerBound < Constants.NUM_ARTISTS; lowerBound += 50) {
				int upperBound = lowerBound + 50;
				if (upperBound > Constants.NUM_ARTISTS) {
					upperBound = Constants.NUM_ARTISTS;
				}
				for (int i = lowerBound; i < upperBound; i++) {
					int meId = dis.readInt();
					String name = dis.readUTF();
					readCoords(dis, coords);
					Log.v(TAG, "inserting artist (meId: " + meId +
							", name: " + name + ")");
					dos.writeInt(meId);
					dos.writeUTF(name);
					writeCoords(dos, coords);

				}
			}

		} catch (IOException e) {

		} finally {
			if (dis != null) {
				try {
					dis.close();
				} catch (Exception e) {
				}
			}
			if (fis != null) {
				try {
					fis.close();
				} catch (Exception e) {
				}
			}
			if (dos != null) {
				try {
					dos.close();
				} catch (Exception e) {
				}
			}
			if (fos != null) {
				try {
					fos.close();
				} catch (Exception e) {
				}
			}
		}
	}

	private void writeCoords(DataOutputStream dos, float[] coords) {
		//		// TODO Auto-generated method stub
		//		if (Constants.THROW_METHOD_STUB_EXCEPTIONS) {
		//			throw new MethodNotImplementedException();
		//		}

	}

}
