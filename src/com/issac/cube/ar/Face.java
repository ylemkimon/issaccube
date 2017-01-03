/**
 * Original: Augmented Reality Rubik Cube Wizard (https://github.com/AndroidSteve/Rubik-Cube-Wizard)
 * Original Author: Steven P. Punte (aka Android Steve : android.steve@cl-sw.com)
 * Year: 2015
 * 
 * License:
 * 
 *  GPL
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.issac.cube.ar;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;

import com.issac.cube.ar.Constants.ColorTileEnum;
import com.issac.cube.ar.Constants.FaceNameEnum;

public class Face {
	public boolean solved = false;
	public List<Rhombus> rhombusList = new LinkedList<Rhombus>();
	public Rhombus[][] faceRhombusArray = new Rhombus[3][3];
	public ColorTileEnum[][] observedTileArray = new ColorTileEnum[3][3];
	public ColorTileEnum[][] transformedTileArray = new ColorTileEnum[3][3];
	public double[][][] measuredColorArray = new double[3][3][4];
	public double alphaAngle = 0.0;
	public double betaAngle = 0.0;
	public double alphaLatticLength = 0.0;
	public double betaLatticLength = 0.0;
	public double gammaRatio = 0.0;
	public LeastMeanSquare lmsResult = new LeastMeanSquare(640, 225, 100, 314,
			true);
	public int numRhombusMoves = 0;
	public int myHashCode = 0;
	public FaceNameEnum faceNameEnum;

	public Face() {
		alphaAngle = 45.0 * Math.PI / 180.0;
		betaAngle = 135.0 * Math.PI / 180.0;
		alphaLatticLength = 100.0;
		betaLatticLength = 100.0;
	}

	public void processRhombuses(List<Rhombus> rhombusList, Mat image) {
		this.rhombusList = rhombusList;

		if (rhombusList.size() < 3) {
			solved = false;
			return;
		}

		calculateMetrics();

		if (!doInitialLayout()) {
			solved = false;
			return;
		}

		lmsResult = findOptimumFaceFit();
		if (!lmsResult.valid) {
			solved = false;
			return;
		}

		alphaLatticLength = lmsResult.alphaLattice;
		betaLatticLength = gammaRatio * lmsResult.alphaLattice;
		double lastSigma = lmsResult.sigma;

		while (lmsResult.sigma > MenuParam.faceLmsThresholdParam.value) {
			if (numRhombusMoves > 5) {
				solved = false;
				return;
			}

			if (!findAndMoveRhombus()) {
				solved = false;
				return;
			}
			numRhombusMoves++;

			lmsResult = findOptimumFaceFit();
			if (!lmsResult.valid) {
				solved = false;
				return;
			}
			alphaLatticLength = lmsResult.alphaLattice;
			betaLatticLength = gammaRatio * lmsResult.alphaLattice;

			if (lmsResult.sigma > lastSigma)
				solved = false;
		}

		new ColorRecognizer.FaceRecognizer(this).recognize(image);

		myHashCode = 0;
		for (int n = 0; n < 3; n++)
			for (int m = 0; m < 3; m++)
				myHashCode = observedTileArray[n][m].hashCode()
						^ Integer.rotateRight(myHashCode, 1);

		solved = true;
	}

	public boolean doInitialLayout() {
		List<Collection<Rhombus>> alphaListOfSets = createOptimizedList(new Comparator<Rhombus>() {
			@Override
			public int compare(Rhombus rhombus0, Rhombus rhombus1) {
				return getAlpha(rhombus0) - getAlpha(rhombus1);
			}

			private int getAlpha(Rhombus rhombus) {
				return (int) (rhombus.center.x * Math.cos(alphaAngle) + rhombus.center.y
						* Math.sin(alphaAngle));
			}
		});

		List<Collection<Rhombus>> betaListOfSets = createOptimizedList(new Comparator<Rhombus>() {
			@Override
			public int compare(Rhombus rhombus0, Rhombus rhombus1) {
				return getBeta(rhombus0) - getBeta(rhombus1);
			}

			private int getBeta(Rhombus rhombus) {
				return (int) (rhombus.center.x * Math.cos(betaAngle) + rhombus.center.y
						* Math.sin(betaAngle));
			}
		});

		for (int n = 0; n < 3; n++)
			for (int m = 0; m < 3; m++) {
				Collection<Rhombus> alphaSet = alphaListOfSets.get(n);
				Collection<Rhombus> betaSet = betaListOfSets.get(m);

				List<Rhombus> commonElements = new LinkedList<Rhombus>(alphaSet);
				commonElements.retainAll(betaSet);

				if (commonElements.size() == 0)
					faceRhombusArray[n][m] = null;

				else if (commonElements.size() == 1)
					faceRhombusArray[n][m] = commonElements.get(0);
				else
					faceRhombusArray[n][m] = commonElements.get(0);
			}

		for (int n = 0; n < 3; n++) {
			boolean leastRow = true, leastCol = true;
			for (int m = 0; m < 3; m++) {
				leastRow &= faceRhombusArray[n][m] == null;
				leastCol &= faceRhombusArray[m][n] == null;
			}
			if (leastRow || leastCol)
				return false;
		}

		return true;
	}

	private List<Collection<Rhombus>> createOptimizedList(
			Comparator<Rhombus> comparator) {
		int best_error = Integer.MAX_VALUE;
		int best_p = 0;
		int best_q = 0;

		int n = rhombusList.size();

		ArrayList<Rhombus> sortedRhombusList = new ArrayList<Rhombus>(
				rhombusList);
		Collections.sort(sortedRhombusList, comparator);

		for (int p = 1; p < n - 1; p++)
			for (int q = p + 1; q < n; q++) {
				int error = calculateError(sortedRhombusList.subList(0, p),
						comparator)
						+ calculateError(sortedRhombusList.subList(p, q),
								comparator)
						+ calculateError(sortedRhombusList.subList(q, n),
								comparator);

				if (error < best_error) {
					best_error = error;
					best_p = p;
					best_q = q;
				}
			}

		LinkedList<Collection<Rhombus>> result = new LinkedList<Collection<Rhombus>>();
		result.add(sortedRhombusList.subList(0, best_p));
		result.add(sortedRhombusList.subList(best_p, best_q));
		result.add(sortedRhombusList.subList(best_q, n));
		return result;
	}

	private int calculateError(List<Rhombus> subList,
			Comparator<Rhombus> comparator) {
		int n = subList.size();
		int sumSquared = 0;

		for (int i = 0; i < n - 1; i++)
			for (int j = i + 1; j < n; j++) {
				int cmp = comparator.compare(subList.get(i), subList.get(j));
				sumSquared += cmp * cmp;
			}

		return sumSquared;
	}

	private void calculateMetrics() {
		int numElements = rhombusList.size();
		for (Rhombus rhombus : rhombusList) {
			alphaAngle += rhombus.alphaAngle;
			betaAngle += rhombus.betaAngle;
			gammaRatio += rhombus.gammaRatio;
		}

		alphaAngle = alphaAngle / numElements * Math.PI / 180.0;
		betaAngle = betaAngle / numElements * Math.PI / 180.0;
		gammaRatio = gammaRatio / numElements;
	}

	private LeastMeanSquare findOptimumFaceFit() {
		int k = 0;
		for (int n = 0; n < 3; n++)
			for (int m = 0; m < 3; m++)
				if (faceRhombusArray[n][m] != null)
					k++;

		Mat bigAmatrix = new Mat(2 * k, 3, CvType.CV_64FC1);
		Mat bigYmatrix = new Mat(2 * k, 1, CvType.CV_64FC1);
		Mat bigXmatrix = new Mat(3, 1, CvType.CV_64FC1);

		int index = 0;
		for (int n = 0; n < 3; n++)
			for (int m = 0; m < 3; m++) {
				Rhombus rhombus = faceRhombusArray[n][m];
				if (rhombus != null) {
					{
						double bigY = rhombus.center.x;
						double bigA = n * Math.cos(alphaAngle) + gammaRatio * m
								* Math.cos(betaAngle);

						bigYmatrix.put(index, 0, new double[] { bigY });
						bigAmatrix.put(index, 0, new double[] { 1.0 });
						bigAmatrix.put(index, 1, new double[] { 0.0 });
						bigAmatrix.put(index, 2, new double[] { bigA });

						index++;
					}

					{
						double bigY = rhombus.center.y;
						double bigA = n * Math.sin(alphaAngle) + gammaRatio * m
								* Math.sin(betaAngle);

						bigYmatrix.put(index, 0, new double[] { bigY });
						bigAmatrix.put(index, 0, new double[] { 0.0 });
						bigAmatrix.put(index, 1, new double[] { 1.0 });
						bigAmatrix.put(index, 2, new double[] { bigA });

						index++;
					}
				}
			}

		Core.solve(bigAmatrix, bigYmatrix, bigXmatrix, Core.DECOMP_NORMAL);

		Mat bigEmatrix = new Mat(2 * k, 1, CvType.CV_64FC1);
		for (int r = 0; r < 2 * k; r++) {
			double y = bigYmatrix.get(r, 0)[0];
			double error = y;
			for (int c = 0; c < 3; c++) {
				double a = bigAmatrix.get(r, c)[0];
				double x = bigXmatrix.get(c, 0)[0];
				error -= a * x;
			}
			bigEmatrix.put(r, 0, error);
		}

		double sigma = 0;
		for (int r = 0; r < 2 * k; r++) {
			double error = bigEmatrix.get(r, 0)[0];
			sigma += error * error;
		}
		sigma = Math.sqrt(sigma);

		double x = bigXmatrix.get(0, 0)[0];
		double y = bigXmatrix.get(1, 0)[0];
		double alphaLatice = bigXmatrix.get(2, 0)[0];
		boolean valid = !Double.isNaN(x) && !Double.isNaN(y)
				&& !Double.isNaN(alphaLatice) && !Double.isNaN(sigma);

		return new LeastMeanSquare(x, y, alphaLatice, sigma, valid);
	}

	private boolean findAndMoveRhombus() {
		Rhombus largestErrorRhombus = null;
		double largetError = Double.NEGATIVE_INFINITY;
		int tile_n = 0;
		int tile_m = 0;
		for (int n = 0; n < 3; n++)
			for (int m = 0; m < 3; m++) {
				Rhombus rhombus = faceRhombusArray[n][m];
				if (rhombus != null) {
					double tile_x = lmsResult.origin.x + n * alphaLatticLength
							* Math.cos(alphaAngle) + m * betaLatticLength
							* Math.cos(betaAngle);
					double tile_y = lmsResult.origin.y + n * alphaLatticLength
							* Math.sin(alphaAngle) + m * betaLatticLength
							* Math.sin(betaAngle);

					double error = Math.sqrt((rhombus.center.x - tile_x)
							* (rhombus.center.x - tile_x)
							+ (rhombus.center.y - tile_y)
							* (rhombus.center.y - tile_y));

					if (error > largetError) {
						largestErrorRhombus = rhombus;
						tile_n = n;
						tile_m = m;
						largetError = error;
					}
				}
			}

		double error_x = largestErrorRhombus.center.x
				- (lmsResult.origin.x + tile_n * alphaLatticLength
						* Math.cos(alphaAngle) + tile_m * betaLatticLength
						* Math.cos(betaAngle));
		double error_y = largestErrorRhombus.center.y
				- (lmsResult.origin.y + tile_n * alphaLatticLength
						* Math.sin(alphaAngle) + tile_m * betaLatticLength
						* Math.sin(betaAngle));

		double alphaError = error_x * Math.cos(alphaAngle) + error_y
				* Math.sin(alphaAngle);
		double betaError = error_x * Math.cos(betaAngle) + error_y
				* Math.sin(betaAngle);

		int delta_n = (int) Math.round(alphaError / alphaLatticLength);
		int delta_m = (int) Math.round(betaError / betaLatticLength);

		int new_n = tile_n + delta_n;
		int new_m = tile_m + delta_m;

		if (new_n < 0)
			new_n = 0;
		if (new_n > 2)
			new_n = 2;
		if (new_m < 0)
			new_m = 0;
		if (new_m > 2)
			new_m = 2;

		if (new_n == tile_n && new_m == tile_m)
			return false;
		else {
			Rhombus tmp = faceRhombusArray[new_n][new_m];
			faceRhombusArray[new_n][new_m] = faceRhombusArray[tile_n][tile_m];
			faceRhombusArray[tile_n][tile_m] = tmp;
			return true;
		}

	}

	public Point getTileCenterInPixels(int n, int m) {
		return new Point(lmsResult.origin.x + n * alphaLatticLength
				* Math.cos(alphaAngle) + m * betaLatticLength
				* Math.cos(betaAngle), lmsResult.origin.y + n
				* alphaLatticLength * Math.sin(alphaAngle) + m
				* betaLatticLength * Math.sin(betaAngle));
	}

	public class LeastMeanSquare {
		public Point origin;
		public double alphaLattice;
		public double sigma;
		public boolean valid;

		public LeastMeanSquare(double x, double y, double alphaLatice,
				double sigma, boolean valid) {
			origin = new Point(x, y);
			alphaLattice = alphaLatice;
			this.sigma = sigma;
			this.valid = valid;
		}
	}
}
