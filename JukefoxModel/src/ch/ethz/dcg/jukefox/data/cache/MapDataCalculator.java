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
package ch.ethz.dcg.jukefox.data.cache;

import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import Jama.Matrix;
import ch.ethz.dcg.jukefox.commons.Constants;
import ch.ethz.dcg.jukefox.commons.DataUnavailableException;
import ch.ethz.dcg.jukefox.commons.utils.Log;
import ch.ethz.dcg.jukefox.commons.utils.MathUtils;
import ch.ethz.dcg.jukefox.commons.utils.RandomProvider;
import ch.ethz.dcg.jukefox.commons.utils.kdtree.AdvancedKdTree;
import ch.ethz.dcg.jukefox.commons.utils.kdtree.KdTreePoint;
import ch.ethz.dcg.jukefox.data.db.IDbDataPortal;
import ch.ethz.dcg.jukefox.manager.DirectoryManager;
import ch.ethz.dcg.jukefox.model.collection.CompleteAlbum;
import ch.ethz.dcg.jukefox.model.collection.CompleteTag;
import ch.ethz.dcg.jukefox.model.collection.ListAlbum;
import ch.ethz.dcg.jukefox.model.collection.MapAlbum;
import ch.ethz.dcg.jukefox.model.collection.SongCoords;
import ch.ethz.dcg.jukefox.model.libraryimport.ImportState;
import edu.wlu.cs.levy.CG.KeySizeException;

public class MapDataCalculator {

	private final static String TAG = MapDataCalculator.class.getSimpleName();

	private final static int NUM_RELEVANT_TAGS = 100;
	private final static int NUM_PCA_COORDS = 100;
	private final static float GRID_PLACES_FACTOR = 10;
	private final static float GRID_SIZE = 2f; // the distance between two

	private static final int TAG_GRID_SIZE_X = 20;
	private static final int TAG_GRID_SIZE_Y = 5;
	// adjacent grid points

	private final IDbDataPortal dbDataPortal;

	private List<CompleteTag> relevantTags;
	private HashMap<Integer, float[]> songPcaCoords;
	private List<SongCoords> songCoords;
	private ArrayList<Integer> idsOfSongsWithoutCoords;
	private ImportState importState;
	private int stepsPerformed = 0;
	private int maxSteps = 11;
	// private Collection<MapAlbum> mapAlbums;

	private LinkedList<MapDataCalculatorListener> listeners;

	private DirectoryManager directoryManager;

	private static class PcaTransform {

		private float[] means;
		private float[][] transform;

		public PcaTransform(float[] means, float[][] transform) {
			super();
			this.means = means;
			this.transform = transform;
		}

		public float[] getMeans() {
			return means;
		}

		public float[][] getTransform() {
			return transform;
		}

	}

	private static class TagWithProbability {

		private CompleteTag tag;
		private float probability;

		public TagWithProbability(CompleteTag tag, float probability) {
			this.tag = tag;
			this.probability = probability;
		}

		public CompleteTag getTag() {
			return tag;
		}

		public float getProbability() {
			return probability;
		}

	}

	// private HashMap<Integer, GlTagLabel> glTags;
	// private HashMap<Integer, GlAlbumCover> glAlbums;

	private static class Bounds {

		public float minX;
		public float minY;
		public float maxX;
		public float maxY;
		public float meanX;
		public float meanY;
		public float varX;
		public float varY;
		public float stdX;
		public float stdY;
	}

	public MapDataCalculator(IDbDataPortal dbDataPortal, ImportState importState, DirectoryManager directoryManager) {
		this.dbDataPortal = dbDataPortal;
		this.importState = importState;
		this.directoryManager = directoryManager;
		listeners = new LinkedList<MapDataCalculatorListener>();
	}

	public void addListener(MapDataCalculatorListener listener) {
		listeners.add(listener);
	}

	public void calculate(Collection<ListAlbum> allAlbums, List<CompleteTag> tags, PreloadedAlbums preloadedAlbums,
			int recomputeTaskId) {
		List<SongCoords> songsCoordinateInfo = dbDataPortal.getSongCoords(true);
		stepsPerformed++;
		importState.setMapDataProgress(stepsPerformed, maxSteps, "processing song coordinates");
		processSongsCoordinateInfo(songsCoordinateInfo);
		stepsPerformed++;
		importState.setMapDataProgress(stepsPerformed, maxSteps, "updating tags");
		Log.v(TAG, "retrieved all song coords, size: " + songCoords.size());

		// TODO: We could read them directly from the file.
		// List<CompleteTag> tags = ResourceLoader.readTagFile();
		Log.v(TAG, "retrieved all tags, size: " + tags.size());

		updatePlsaProbInfo(tags);
		stepsPerformed++;
		importState.setMapDataProgress(stepsPerformed, maxSteps, "get relevant tags");

		relevantTags = getRelevantTags(tags);
		stepsPerformed++;
		importState.setMapDataProgress(stepsPerformed, maxSteps, "doing 2D computation");
		Log.v(TAG, "Got relevant tags");

		// HashMap<Integer, Float> plsaSpaceVariances =
		// getTagPlsaSpaceVariances();
		// Log.v(TAG, "Got PLSA space variances");

		// ArrayList<CompleteTag> pcaTags = getPcaTags(plsaSpaceVariances);

		ArrayList<float[]> coordSample = getCoordSample();

		PcaTransform pcaTransform;
		if (!hasThreeDistinctCoords(coordSample)) {
			Log.v(TAG, "applying nullTransform (instead of pca transform), as there are less than 3 distinct coords.");
			// we cannot calculate a reasonable pca transform on such a sample
			// => map all pca-coords to [0,0] afterwards...
			// TODO: better solution?
			pcaTransform = nullTransform();
		} else {
			pcaTransform = getPcaTransform(coordSample);
		}

		stepsPerformed++;
		importState.setMapDataProgress(stepsPerformed, maxSteps, "");
		Log.v(TAG, "Computed PCA transform ");

		songPcaCoords = getSongPcaCoords(pcaTransform);
		stepsPerformed++;
		importState.setMapDataProgress(stepsPerformed, maxSteps, "mapping tags to 2D");
		Log.v(TAG, "Applied PCA to songs");
		informListenersSongPcaCoordsCalculated();

		updateTagPcaInfo(pcaTransform);
		Log.v(TAG, "Updated tag PCA info");
		stepsPerformed++;
		importState.setMapDataProgress(stepsPerformed, maxSteps, "mapping albums to 2D");

		// They are filled expect for the pca coords at this stage...
		// create clone, such that currently cached list remains consistent.
		// this.mapAlbums = getAlbumCollectionClone(allAlbums);
		// Log.v(TAG, "Cloned Map albums");

		HashMap<Integer, float[]> albumPcaCoords = getAlbumPcaCoords(allAlbums);
		Log.v(TAG, "got Album PCA coords");
		stepsPerformed++;
		importState.setMapDataProgress(stepsPerformed, maxSteps, "placing albums on grid");
		// this.mapAlbums = getMapAlbums(allAlbums, albumPcaCoords);

		// currently also changes the values in albumPcaCoords (normalization)
		Bounds pcaBounds = getBounds(albumPcaCoords);
		Collection<MapAlbum> mapAlbums = updateAlbumGridCoords(allAlbums, albumPcaCoords, pcaBounds);
		Log.v(TAG, "updated album grid coords");
		stepsPerformed++;
		importState.setMapDataProgress(stepsPerformed, maxSteps, "creating data structures");
		informListenersMapAlbumsCalculated(mapAlbums);
		AdvancedKdTree<MapAlbum> albumQuadTree = new AdvancedKdTree<MapAlbum>(2);
		updateAlbumQuadTree(mapAlbums, albumQuadTree);
		Log.v(TAG, "updated album quad tree");
		stepsPerformed++;
		importState.setMapDataProgress(stepsPerformed, maxSteps, "placing tags on grid");
		Log.v(TAG, "applying grid placement to tags...");
		applyGridPlacementToTags(relevantTags, pcaBounds, albumPcaCoords.size(), albumQuadTree);
		Log.v(TAG, "Applied grid placement to tags");
		stepsPerformed++;
		importState.setMapDataProgress(stepsPerformed, maxSteps, "constructed map");
		Log.v(TAG, "set Map data progress");
		informListenersTagsCalculated();
		Log.v(TAG, "end of mapdatacalculator.calculate()");

		// informListenersCompleted(preloadedAlbums, recomputeTaskId);
	}

	private boolean hasThreeDistinctCoords(ArrayList<float[]> coordSample) {
		ArrayList<float[]> distinct = new ArrayList<float[]>();
		for (float[] c : coordSample) {
			if (distinct.size() == 0) {
				distinct.add(c);
				continue;
			}
			boolean inSet = false;
			for (float[] c2 : distinct) {
				if (MathUtils.distance(c2, c) == 0) {
					inSet = true;
					break;
				}
			}
			if (!inSet) {
				distinct.add(c);
			}
			if (distinct.size() >= 3) {
				return true;
			}
		}
		return false;
	}

	private PcaTransform nullTransform() {
		return new PcaTransform(new float[Constants.DIM], new float[2][Constants.DIM]);
	}

	private ArrayList<float[]> getCoordSample() {
		ArrayList<float[]> sample = new ArrayList<float[]>(NUM_PCA_COORDS);
		Collections.shuffle(songCoords, RandomProvider.getRandom());
		int max = Math.min(NUM_PCA_COORDS, songCoords.size());
		for (int i = 0; i < max; i++) {
			sample.add(songCoords.get(i).getCoords());
		}
		if (sample.size() == 0) {
			Log.v(TAG, "Sample song coords size is 0");
			sample.add(new float[Constants.DIM]);
			sample.add(new float[Constants.DIM]);
		}
		return sample;
	}

	private void processSongsCoordinateInfo(List<SongCoords> songsCoordinateInfo) {
		songCoords = new ArrayList<SongCoords>();
		idsOfSongsWithoutCoords = new ArrayList<Integer>();
		for (SongCoords sc : songsCoordinateInfo) {
			if (sc.getCoords() == null) {
				idsOfSongsWithoutCoords.add(sc.getId());
				continue;
			}
			songCoords.add(sc);
		}
	}

	private void applyGridPlacementToTags(List<CompleteTag> tags, Bounds pcaBounds, int numAlbums,
			AdvancedKdTree<MapAlbum> albumQuadTree) {

		HashMap<Integer, Float> maxTagProbs = new HashMap<Integer, Float>();

		int size = (int) Math.floor(Math.sqrt(numAlbums * GRID_PLACES_FACTOR));

		resetMapInformation(tags);

		// float gridMax = size * GRID_SIZE;

		// float gridCellLength = gridMax / (size / 2);

		boolean shifted = false;
		for (int i = TAG_GRID_SIZE_X / 2; i < size; i += TAG_GRID_SIZE_X) {
			Log.v(TAG, "applying grid placement to tags: i: " + i);

			for (int j = TAG_GRID_SIZE_Y / 2; j < size; j += TAG_GRID_SIZE_Y) {
				float x;
				if (shifted) {
					x = (i - TAG_GRID_SIZE_X / 2) * GRID_SIZE;
				} else {
					x = i * GRID_SIZE;
				}
				shifted = !shifted;
				float y = j * GRID_SIZE;

				// Log.v(TAG, "x: " + x + ", y: " + y);

				// Log.v(TAG, "getting nearest album...");
				MapAlbum closestAlbum = getNearestAlbum(x, y, albumQuadTree);
				// Log.v(TAG, "nearest album found.");

				// find most probable tag for album
				// Log.v(TAG, "getting album plsa coords...");
				SongCoords songCoords = getAlbumPlsaCoords(closestAlbum);
				// Log.v(TAG, "album plsa coords retrieved.");
				if (songCoords == null) { // no position found for album
					Log.v(TAG, "song coords == null");
					continue;
				}
				// Log.v(TAG, "finding best tag for position...");
				TagWithProbability bestTag = findBestTag(tags, maxTagProbs, x, y, songCoords);
				// Log.v(TAG, "best tag found. null: " + (bestTag == null));
				if (bestTag != null) {
					bestTag.getTag().setMeanPcaSpaceX(x);
					bestTag.getTag().setMeanPcaSpaceY(y);
					bestTag.getTag().setMapTag(true);
					maxTagProbs.put(bestTag.getTag().getId(), bestTag.getProbability());
				}
			}
		}

	}

	private void resetMapInformation(List<CompleteTag> tags) {
		for (CompleteTag tag : tags) {
			tag.setMeanPcaSpaceX(null);
			tag.setMeanPcaSpaceY(null);
			tag.setMapTag(false);
		}
	}

	private MapAlbum getNearestAlbum(float x, float y, AdvancedKdTree<MapAlbum> albumQuadTree) {
		try {
			Vector<KdTreePoint<MapAlbum>> album = albumQuadTree.getNearestPoints(new float[] { x, y }, 1);
			return album.get(0).getID();
		} catch (Exception e) {
			Log.wtf(TAG, e);
			return null;
		}
	}

	private TagWithProbability findBestTag(List<CompleteTag> tags, HashMap<Integer, Float> maxTagProbs, float x,
			float y, SongCoords songCoords) {
		float maxProb = -1;
		CompleteTag maxTag = null;
		// int debugCnt = 0;
		// Log.v(TAG, "tags.size: " + tags.size());
		for (CompleteTag tag : tags) {
			// Log.v(TAG, "debugCnt: " + debugCnt++);
			float prob = MathUtils.dotProduct(tag.getPlsaCoords(), songCoords.getCoords());
			if (prob > maxProb) {
				maxProb = prob;
				maxTag = tag;
			}
		}
		// Log.v(TAG, "for loop finished.");

		// check if tag is already used
		Float previousTagProb = maxTagProbs.get(maxTag.getId());

		// if it's already used, but with lower probability, then use new pos
		// instead.
		if (previousTagProb == null || previousTagProb < maxProb) {
			// Log.v(TAG, "tag is free => return it.");
			return new TagWithProbability(maxTag, maxProb);
		}
		// Log.v(TAG, "tag already used. returning null");
		return null;
	}

	private SongCoords getAlbumPlsaCoords(MapAlbum closestAlbum) {
		SongCoords songCoords = null;
		try {
			CompleteAlbum compAlbum = dbDataPortal.getCompleteAlbum(closestAlbum);
			songCoords = dbDataPortal.getSongCoordsById(compAlbum.getSongs().get(0).getId());
		} catch (DataUnavailableException e) {
			Log.w(TAG, e);
			return null;
		}
		return songCoords;
	}

	private void informListenersMapAlbumsCalculated(Collection<MapAlbum> mapAlbums) {
		Log.v(TAG, "informing listeners map albums calculated...");
		for (MapDataCalculatorListener l : listeners) {
			l.onMapAlbumsCalculated(mapAlbums);
		}
		Log.v(TAG, "listeners informed map albums calculated.");
	}

	private void informListenersTagsCalculated() {
		for (MapDataCalculatorListener l : listeners) {
			l.onRelevantTagsCalculated(relevantTags);
		}
	}

	private void informListenersSongPcaCoordsCalculated() {
		for (MapDataCalculatorListener l : listeners) {
			l.onSongPcaCoordsCalculated(songPcaCoords);
		}
	}

	public List<CompleteTag> getRelevantTags() {
		return relevantTags;
	}

	public HashMap<Integer, float[]> getSongPcaCoords() {
		return songPcaCoords;
	}

	public List<SongCoords> getSongCoords() {
		return songCoords;
	}

	private void updateAlbumQuadTree(Collection<MapAlbum> mapAlbums, AdvancedKdTree<MapAlbum> albumQuadTree) {
		for (MapAlbum album : mapAlbums) {
			try {
				albumQuadTree.insert(album.getGridCoords(), album);
			} catch (KeySizeException e) {
				Log.w(TAG, e);
			}
		}
	}

	private Collection<MapAlbum> updateAlbumGridCoords(Collection<ListAlbum> allAlbums,
			HashMap<Integer, float[]> albumPcaCoords, Bounds pcaBounds) {

		int size = (int) Math.floor(Math.sqrt(albumPcaCoords.size() * GRID_PLACES_FACTOR));

		float gridMax = size * GRID_SIZE;

		float offsetX = pcaBounds.meanX - 2 * pcaBounds.stdX;
		float offsetY = pcaBounds.meanY - 2 * pcaBounds.stdY;

		scaleToGrid(albumPcaCoords.values(), pcaBounds, gridMax, offsetX, offsetY);

		boolean[][] grid = new boolean[size][size];

		ArrayList<MapAlbum> mapAlbums = new ArrayList<MapAlbum>();

		for (ListAlbum listAlbum : allAlbums) {
			float[] pcaCoords = albumPcaCoords.get(listAlbum.getId());
			if (pcaCoords == null) {
				continue;
			}
			MapAlbum album = new MapAlbum(listAlbum);
			mapAlbums.add(album);
			float minDist = Float.MAX_VALUE;
			int[] bestGridIndices = new int[2];
			float[] gridCoords = new float[2];
			float[] bestGridCoords = new float[2];
			// greedy search for best unoccupied grid position
			// TODO: search in the region of the optimal position (=> faster)
			for (int x = 0; x < size; x++) {
				for (int y = 0; y < size; y++) {
					if (grid[x][y]) {
						continue;
					}

					// it's not really a grid what we have, but every second
					// line is shifted by half a unit
					gridCoords[0] = (x + y % 2 * 0.5f) * GRID_SIZE;
					gridCoords[1] = y * GRID_SIZE;
					float dist = MathUtils.distance(pcaCoords, gridCoords);
					if (dist < minDist) {
						minDist = dist;
						bestGridIndices[0] = x;
						bestGridIndices[1] = y;
						bestGridCoords[0] = gridCoords[0];
						bestGridCoords[1] = gridCoords[1];
					}
				}
			}
			// assign grid coordinates
			album.setGridCoords(bestGridCoords);
			;
			grid[bestGridIndices[0]][bestGridIndices[1]] = true;
		}
		return mapAlbums;
	}

	private void scaleToGrid(Collection<float[]> coordsToScale, Bounds pcaBounds, float gridMax, float offsetX,
			float offsetY) {
		// normalize pca-coords (TODO: write to new hash map?)
		for (float[] coords : coordsToScale) {
			coords[0] = (coords[0] - offsetX) / (4 * pcaBounds.stdX) * gridMax;
			coords[1] = (coords[1] - offsetY) / (4 * pcaBounds.stdY) * gridMax;
		}
	}

	private Bounds getBounds(HashMap<Integer, float[]> albumPcaCoords) {
		Bounds bounds = new Bounds();
		bounds.maxX = -Float.MAX_VALUE;
		bounds.minX = Float.MAX_VALUE;
		bounds.maxY = -Float.MAX_VALUE;
		bounds.minY = Float.MAX_VALUE;
		for (float[] coords : albumPcaCoords.values()) {
			bounds.meanX += coords[0];
			bounds.meanY += coords[1];
			bounds.varX += coords[0] * coords[0];
			bounds.varY += coords[1] * coords[1];
			if (coords[0] < bounds.minX) {
				bounds.minX = coords[0];
			}
			if (coords[1] < bounds.minY) {
				bounds.minY = coords[1];
			}
			if (coords[0] > bounds.maxX) {
				bounds.maxX = coords[0];
			}
			if (coords[1] > bounds.maxY) {
				bounds.maxY = coords[1];
			}
		}
		bounds.meanX /= albumPcaCoords.size();
		bounds.meanY /= albumPcaCoords.size();
		bounds.varX /= albumPcaCoords.size();
		bounds.varX -= bounds.meanX * bounds.meanX;
		bounds.varY /= albumPcaCoords.size();
		bounds.varY -= bounds.meanY * bounds.meanY;
		bounds.stdX = (float) Math.sqrt(bounds.varX);
		bounds.stdY = (float) Math.sqrt(bounds.varY);
		return bounds;
	}

	private HashMap<Integer, float[]> getAlbumPcaCoords(Collection<ListAlbum> allAlbums) {
		HashMap<Integer, float[]> albumPcaCoords = new HashMap<Integer, float[]>();
		for (ListAlbum album : allAlbums) {
			float[] pcaCoords = getAlbumPcaCoords(album);
			if (pcaCoords == null) {
				continue;
			}
			albumPcaCoords.put(album.getId(), pcaCoords);
		}
		return albumPcaCoords;
	}

	private float[] getAlbumPcaCoords(ListAlbum album) {
		List<Integer> songIds = dbDataPortal.getSongIdsForAlbum(album.getId());
		float[] pcaCoords = getMeanPcaCoords(songIds);
		return pcaCoords;
	}

	private float[] getMeanPcaCoords(List<Integer> songIds) {
		float[] mean = new float[2];
		int cnt = 0;
		for (Integer songId : songIds) {
			float[] songPcaPos = songPcaCoords.get(songId);
			if (songPcaPos == null) {
				continue;
			}
			mean[0] += songPcaPos[0];
			mean[1] += songPcaPos[1];
			cnt++;
		}
		if (cnt == 0) {
			return null;
		}
		mean[0] /= cnt;
		mean[1] /= cnt;
		return mean;
	}

	private HashMap<Integer, float[]> getSongPcaCoords(PcaTransform pcaTransform) {

		// to reduce garbage collection
		float[] tmpCoords = new float[Constants.DIM];

		HashMap<Integer, float[]> songPcaCoords = new HashMap<Integer, float[]>();
		for (SongCoords sc : songCoords) {
			float[] pcaCoords = applyPcaTransform(sc.getCoords(), tmpCoords, pcaTransform);
			songPcaCoords.put(sc.getId(), pcaCoords);
		}
		return songPcaCoords;
	}

	private void updateTagPcaInfo(PcaTransform pcaTransform) {
		updateTagPcaCoords(pcaTransform);
		updateTagPcaVariances();
	}

	private void updateTagPcaVariances() {
		for (CompleteTag t : relevantTags) {
			updateTagPcaVariance(t);
		}
	}

	private void updateTagPcaVariance(CompleteTag t) {

		float probSum = 0;
		float var = 0;
		for (SongCoords sc : songCoords) {
			float prob = MathUtils.scalarProduct(t.getPlsaCoords(), sc.getCoords());
			float[] tagPcaPos = t.getPcaCoords();
			float[] songPcaPos = songPcaCoords.get(sc.getId());
			float squareDist = MathUtils.squareDistance(tagPcaPos, songPcaPos);
			var += prob * squareDist;
			probSum += prob;
		}
		t.setVariancePcaSpace(var / probSum);
	}

	private void updateTagPcaCoords(PcaTransform pcaTransform) {

		// to reduce garbage collection
		float[] tmpCoords = new float[Constants.DIM];

		for (CompleteTag t : relevantTags) {
			// float[] pcaCoords = applyPcaTransform(t.getPlsaCoords(),
			// tmpCoords,
			// pcaTransform);
			float[] pcaCoords = applyPcaTransform(t.getPlsaMeanSongCoords(), tmpCoords, pcaTransform);
			t.setMeanPcaSpaceX(pcaCoords[0]);
			t.setMeanPcaSpaceY(pcaCoords[1]);
		}
	}

	private float[] applyPcaTransform(float[] coords, float[] tmpCoords, PcaTransform pcaTransform) {
		float[] pcaCoords = new float[2];
		float[] means = pcaTransform.getMeans();
		for (int i = 0; i < Constants.DIM; i++) {
			tmpCoords[i] = coords[i] - means[i];
		}
		pcaCoords[0] = MathUtils.scalarProduct(tmpCoords, pcaTransform.getTransform()[0]);
		pcaCoords[1] = MathUtils.scalarProduct(tmpCoords, pcaTransform.getTransform()[1]);
		return pcaCoords;
	}

	private PcaTransform getPcaTransform(ArrayList<float[]> coords) {

		double[][] tagMatrix = new double[Constants.DIM][coords.size()];

		// for centering...
		float[] means = new float[Constants.DIM];

		for (int i = 0; i < coords.size(); i++) {
			float[] c = coords.get(i);
			for (int j = 0; j < Constants.DIM; j++) {
				tagMatrix[j][i] = c[j];
				means[j] += c[j];
			}
		}

		// TODO: Hmm... should we center the tagMatrix first...?

		for (int j = 0; j < Constants.DIM; j++) {
			means[j] /= coords.size();
			// Log.v(TAG, "means[" + j + "]: " + means[j]);
		}

		for (int i = 0; i < coords.size(); i++) {
			for (int j = 0; j < Constants.DIM; j++) {
				tagMatrix[j][i] -= means[j];
			}
		}

		Matrix m = new Matrix(tagMatrix);
		Log.v(TAG, "m: cols: " + m.getColumnDimension() + ", rows: " + m.getRowDimension());

		try {
			FileWriter writer = new FileWriter(directoryManager.getMatrixFile());
			for (int i = 0; i < tagMatrix.length; i++) {
				writer.write("[");
				for (int j = 0; j < tagMatrix[i].length; j++) {
					writer.write(tagMatrix[i][j] + " ");
				}
				writer.write(";]\n");
			}
			writer.close();
		} catch (Exception e) {
			Log.w(TAG, e);
		}

		Jama.SingularValueDecomposition svd;
		svd = new Jama.SingularValueDecomposition(m);

		Matrix u = svd.getU();
		// Log.v(TAG, "u: cols: " + u.getColumnDimension() + ", rows: "
		// + u.getRowDimension());

		// Matrix v = svd.getV();
		// Log.v(TAG, "v: cols: " + v.getColumnDimension() + ", rows: "
		// + v.getRowDimension());

		float[][] transform = new float[2][Constants.DIM];
		for (int i = 0; i < 2; i++) {
			for (int j = 0; j < Constants.DIM; j++) {
				transform[i][j] = (float) u.get(j, i);
			}
		}

		normalizeTransform(transform);

		return new PcaTransform(means, transform);
	}

	private void normalizeTransform(float[][] transform) {
		for (int i = 0; i < 2; i++) {
			if (transform[i][0] < 0) {
				for (int j = 0; j < Constants.DIM; j++) {
					transform[i][j] = -transform[i][j];
				}
			}
		}
	}

	private void updatePlsaProbInfo(List<CompleteTag> tags) {
		for (CompleteTag tag : tags) {
			updatePlsaProbInfo(tag);
		}
	}

	private void updatePlsaProbInfo(CompleteTag tag) {
		float sum = 0;
		float squareSum = 0;
		float[] tagCoords = new float[Constants.DIM];
		for (SongCoords sc : songCoords) {
			// if (sc.getCoords() == null) {
			// Log.v(TAG, "updatePlsaProbInfo: scCoords == null");
			// }
			// if (tag.getPlsaCoords() == null) {
			// Log.v(TAG, "updatePlsaProbInfo: tagCoords == null");
			// }
			float prob = MathUtils.scalarProduct(sc.getCoords(), tag.getPlsaCoords());
			MathUtils.addWeightedVector(tagCoords, sc.getCoords(), prob);
			sum += prob;
			squareSum += prob * prob;
		}
		MathUtils.divideVector(tagCoords, sum);
		tag.setPlsaMeanSongCoords(tagCoords);
		float mean = sum / songCoords.size();
		float var = squareSum / songCoords.size() - mean * mean;
		tag.setMeanPlsaProb(mean);
		tag.setVariancePlsaProb(var);
	}

	private List<CompleteTag> getRelevantTags(List<CompleteTag> tags) {
		ArrayList<CompleteTag> relevantTags = new ArrayList<CompleteTag>(NUM_RELEVANT_TAGS);
		Collections.sort(tags, new Comparator<CompleteTag>() {

			@Override
			public int compare(CompleteTag t1, CompleteTag t2) {
				return t2.getVariancePlsaProb().compareTo(t1.getVariancePlsaProb());
			}
		});

		// add the first NUM_RELEVANT_TAGS into a new list
		for (int i = 0; i < NUM_RELEVANT_TAGS; i++) {
			relevantTags.add(tags.get(i));
		}
		return relevantTags;
	}

	public ArrayList<Integer> getIdsOfSongsWithoutCoords() {
		return idsOfSongsWithoutCoords;
	}

}
