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

import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.opencv.core.Core;
import org.opencv.core.CvException;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;

import com.issac.cube.ar.Constants.ColorTileEnum;
import com.issac.cube.ar.Constants.FaceNameEnum;

public class ColorRecognizer {

	public static class CubeRecognizer {
		private static class TileLocation {
			final Constants.FaceNameEnum faceNameEnum;
			final int n;
			final int m;

			public TileLocation(Constants.FaceNameEnum faceNameEnum, int n,
					int m) {
				this.faceNameEnum = faceNameEnum;
				this.n = n;
				this.m = m;
			}
		}

		private final StateModel stateModel;
		private static Map<ColorTileEnum, TreeMap<Double, TileLocation>> observedColorGroupMap;
		private static Map<ColorTileEnum, TreeMap<Double, TileLocation>> bestColorGroupMap;
		private static Map<FaceNameEnum, ColorTileEnum[][]> bestAssignmentState;
		private static double bestAssignmentCost;

		public CubeRecognizer(StateModel stateModel) {
			this.stateModel = stateModel;
		}

		public void recognize() {
			for (Constants.FaceNameEnum faceNameEnum : Constants.FaceNameEnum
					.values())
				for (int n = 0; n < 3; n++)
					for (int m = 0; m < 3; m++)
						stateModel.faceMap.get(faceNameEnum).observedTileArray[n][m] = null;

			observedColorGroupMap = new TreeMap<ColorTileEnum, TreeMap<Double, TileLocation>>();
			for (ColorTileEnum colorTile : ColorTileEnum.values())
				if (colorTile.isTileColor)
					observedColorGroupMap.put(colorTile,
							new TreeMap<Double, TileLocation>());

			bestColorGroupMap = new TreeMap<ColorTileEnum, TreeMap<Double, TileLocation>>();
			for (ColorTileEnum colorTile : ColorTileEnum.values())
				if (colorTile.isTileColor)
					bestColorGroupMap.put(colorTile,
							new TreeMap<Double, TileLocation>());

			for (Constants.FaceNameEnum faceNameEnum : Constants.FaceNameEnum
					.values())
				for (int n = 0; n < 3; n++)
					for (int m = 0; m < 3; m++) {
						bestAssignmentState = new TreeMap<Constants.FaceNameEnum, Constants.ColorTileEnum[][]>();
						for (Constants.FaceNameEnum faceNameEnum2 : Constants.FaceNameEnum
								.values()) {
							ColorTileEnum[][] tileArrayClone = new ColorTileEnum[3][3];
							for (int n2 = 0; n2 < 3; n2++)
								for (int m2 = 0; m2 < 3; m2++)
									tileArrayClone[n2][m2] = stateModel.faceMap
											.get(faceNameEnum2).observedTileArray[n2][m2];
							bestAssignmentState.put(faceNameEnum2,
									tileArrayClone);
						}

						for (ColorTileEnum colorTile2 : ColorTileEnum.values())
							if (colorTile2.isTileColor) {
								TreeMap<Double, TileLocation> colorGroupClone = new TreeMap<Double, TileLocation>(
										observedColorGroupMap.get(colorTile2));
								bestColorGroupMap.put(colorTile2,
										colorGroupClone);
							}

						bestAssignmentCost = Double.MAX_VALUE;

						TileLocation tileLocation = new TileLocation(
								faceNameEnum, n, m);
						evaluate(tileLocation, new HashSet<ColorTileEnum>(9));

						for (Constants.FaceNameEnum faceNameEnum3 : Constants.FaceNameEnum
								.values())
							stateModel.faceMap.get(faceNameEnum3).observedTileArray = bestAssignmentState
									.get(faceNameEnum3);

						for (ColorTileEnum colorTile2 : ColorTileEnum.values())
							if (colorTile2.isTileColor)
								observedColorGroupMap.put(
										colorTile2,
										new TreeMap<Double, TileLocation>(
												bestColorGroupMap
														.get(colorTile2)));
					}

			stateModel.faceMap.get(FaceNameEnum.UP).transformedTileArray = stateModel.faceMap
					.get(FaceNameEnum.UP).observedTileArray.clone();
			stateModel.faceMap.get(FaceNameEnum.RIGHT).transformedTileArray = Util
					.rotateArrayClockwise(stateModel.faceMap
							.get(FaceNameEnum.RIGHT).observedTileArray);
			stateModel.faceMap.get(FaceNameEnum.FRONT).transformedTileArray = Util
					.rotateArrayClockwise(stateModel.faceMap
							.get(FaceNameEnum.FRONT).observedTileArray);
			stateModel.faceMap.get(FaceNameEnum.DOWN).transformedTileArray = Util
					.rotateArrayClockwise(stateModel.faceMap
							.get(FaceNameEnum.DOWN).observedTileArray);
			stateModel.faceMap.get(FaceNameEnum.LEFT).transformedTileArray = Util
					.rotateArray180(stateModel.faceMap.get(FaceNameEnum.LEFT).observedTileArray);
			stateModel.faceMap.get(FaceNameEnum.BACK).transformedTileArray = Util
					.rotateArray180(stateModel.faceMap.get(FaceNameEnum.BACK).observedTileArray);
		}

		private void evaluate(TileLocation tileLocation,
				Set<ColorTileEnum> blackList) {
			for (ColorTileEnum colorTile : ColorTileEnum.values()) {
				if (!colorTile.isTileColor)
					continue;

				if (blackList.contains(colorTile))
					continue;

				assignTile(tileLocation, colorTile);

				TreeMap<Double, TileLocation> colorGroup = observedColorGroupMap
						.get(colorTile);
				if (colorGroup.size() <= 9) {
					double cost = calculateTotalError();

					if (cost < bestAssignmentCost) {
						bestAssignmentCost = cost;
						bestAssignmentState = new TreeMap<Constants.FaceNameEnum, Constants.ColorTileEnum[][]>();
						for (Constants.FaceNameEnum faceNameEnum : Constants.FaceNameEnum
								.values()) {
							ColorTileEnum[][] tileArrayClone = new ColorTileEnum[3][3];
							for (int n = 0; n < 3; n++)
								for (int m = 0; m < 3; m++)
									tileArrayClone[n][m] = stateModel.faceMap
											.get(faceNameEnum).observedTileArray[n][m];
							bestAssignmentState.put(faceNameEnum,
									tileArrayClone);
						}

						for (ColorTileEnum colorTile2 : ColorTileEnum.values())
							if (colorTile2.isTileColor) {
								TreeMap<Double, TileLocation> colorGroupClone = new TreeMap<Double, TileLocation>(
										observedColorGroupMap.get(colorTile2));
								bestColorGroupMap.put(colorTile2,
										colorGroupClone);
							}
					}
				} else {
					TileLocation moveTileLoc = colorGroup.lastEntry()
							.getValue();
					unassignTile(moveTileLoc, colorTile);
					blackList.add(colorTile);
					evaluate(moveTileLoc, blackList);
					blackList.remove(colorTile);
					assignTile(moveTileLoc, colorTile);
				}

				unassignTile(tileLocation, colorTile);
			}
		}

		private void assignTile(TileLocation tileLocation,
				ColorTileEnum colorTile) {
			Face face = stateModel.faceMap.get(tileLocation.faceNameEnum);
			face.observedTileArray[tileLocation.n][tileLocation.m] = colorTile;

			TreeMap<Double, TileLocation> colorGroup = observedColorGroupMap
					.get(colorTile);
			colorGroup
					.put(calculateError(
							new Scalar(
									face.measuredColorArray[tileLocation.n][tileLocation.m]),
							colorTile.cvColor), tileLocation);
		}

		private void unassignTile(TileLocation tileLocation,
				ColorTileEnum colorlTile) {
			Face face = stateModel.faceMap.get(tileLocation.faceNameEnum);
			face.observedTileArray[tileLocation.n][tileLocation.m] = null;

			TreeMap<Double, TileLocation> colorGroup = observedColorGroupMap
					.get(colorlTile);
			Double key = null;
			for (Entry<Double, TileLocation> entry : colorGroup.entrySet())
				if (entry.getValue() == tileLocation)
					key = entry.getKey();
			colorGroup.remove(key);
		}

		private double calculateTotalError() {
			double cost = 0.0;

			for (Constants.FaceNameEnum faceNameEnum : Constants.FaceNameEnum
					.values())
				for (int n = 0; n < 3; n++)
					for (int m = 0; m < 3; m++) {
						Face face = stateModel.faceMap.get(faceNameEnum);
						if (face.observedTileArray[n][m] != null)
							cost += calculateError(new Scalar(
									face.measuredColorArray[n][m]),
									face.observedTileArray[n][m].cvColor);
					}

			Set<ColorTileEnum> centerTileColorSet = new HashSet<ColorTileEnum>(
					16);
			for (Constants.FaceNameEnum faceNameEnum : Constants.FaceNameEnum
					.values()) {
				Face face = stateModel.faceMap.get(faceNameEnum);
				ColorTileEnum centerColorTile = face.observedTileArray[1][1];

				if (centerColorTile == null)
					continue;

				if (centerTileColorSet.contains(centerColorTile))
					return Double.MAX_VALUE;
				else
					centerTileColorSet.add(centerColorTile);
			}

			return cost;
		}

		private static double calculateError(Scalar color1, Scalar color2) {
			double distance = (color1.val[0] - color2.val[0])
					* (color1.val[0] - color2.val[0])
					+ (color1.val[1] - color2.val[1])
					* (color1.val[1] - color2.val[1])
					+ (color1.val[2] - color2.val[2])
					* (color1.val[2] - color2.val[2]);
			return Math.sqrt(distance);
		}
	}

	public static class FaceRecognizer {

		private final Face face;
		public double colorErrorBeforeCorrection;
		public double colorErrorAfterCorrection;
		public double luminousOffset = 0.0;

		public FaceRecognizer(Face face) {
			this.face = face;
		}

		public void recognize(Mat image) {
			double[][] colorError = new double[3][3];

			for (int n = 0; n < 3; n++)
				for (int m = 0; m < 3; m++) {

					Point tileCenter = face.getTileCenterInPixels(n, m);
					Size size = image.size();
					double width = size.width;
					double height = size.height;

					if (tileCenter.x < 10 || tileCenter.x > width - 10
							|| tileCenter.y < 10 || tileCenter.y > height - 10)
						face.measuredColorArray[n][m] = new double[4];
					else
						try {
							Mat mat = image.submat((int) (tileCenter.y - 10),
									(int) (tileCenter.y + 10),
									(int) (tileCenter.x - 10),
									(int) (tileCenter.x + 10));
							face.measuredColorArray[n][m] = Core.mean(mat).val;
						} catch (CvException cvException) {
							face.measuredColorArray[n][m] = new double[4];
						}
				}

			for (int n = 0; n < 3; n++)
				for (int m = 0; m < 3; m++) {
					double[] measuredColor = face.measuredColorArray[n][m];
					double[] measuredColorYUV = Util
							.getYUVfromRGB(measuredColor);

					double smallestError = Double.MAX_VALUE;
					ColorTileEnum bestCandidate = null;

					for (ColorTileEnum candidateColorTile : Constants.ColorTileEnum
							.values())
						if (candidateColorTile.isTileColor) {
							double[] candidateColorYUV = Util
									.getYUVfromRGB(candidateColorTile.tileColor.val);

							double error = (candidateColorYUV[1] - measuredColorYUV[1])
									* (candidateColorYUV[1] - measuredColorYUV[1])
									+ (candidateColorYUV[2] - measuredColorYUV[2])
									* (candidateColorYUV[2] - measuredColorYUV[2]);

							colorError[n][m] = Math.sqrt(error);

							if (error < smallestError) {
								bestCandidate = candidateColorTile;
								smallestError = error;
							}
						}

					face.observedTileArray[n][m] = bestCandidate;
				}

			for (int n = 0; n < 3; n++)
				for (int m = 0; m < 3; m++) {
					double[] selectedColor = face.observedTileArray[n][m].tileColor.val;
					double[] measuredColor = face.measuredColorArray[n][m];
					colorErrorBeforeCorrection += calculateError(selectedColor,
							measuredColor, true, 0.0);
				}

			luminousOffset = 0.0;
			int count = 0;
			for (int n = 0; n < 3; n++)
				for (int m = 0; m < 3; m++) {
					ColorTileEnum colorTile = face.observedTileArray[n][m];
					if (colorTile == ColorTileEnum.RED
							|| colorTile == ColorTileEnum.ORANGE
							|| colorTile == ColorTileEnum.YELLOW)
						continue;
					double measuredLuminousity = Util
							.getYUVfromRGB(face.measuredColorArray[n][m])[0];
					double expectedLuminousity = Util
							.getYUVfromRGB(colorTile.tileColor.val)[0];
					luminousOffset += expectedLuminousity - measuredLuminousity;
					count++;
				}
			luminousOffset = count == 0 ? 0.0 : luminousOffset / count;

			for (int n = 0; n < 3; n++)
				for (int m = 0; m < 3; m++) {
					double[] measuredColor = face.measuredColorArray[n][m];
					double[] measuredColorYUV = Util
							.getYUVfromRGB(measuredColor);

					double smallestError = Double.MAX_VALUE;
					ColorTileEnum bestCandidate = null;

					for (ColorTileEnum candidateColorTile : ColorTileEnum
							.values())
						if (candidateColorTile.isTileColor) {
							double[] candidateColorYUV = Util
									.getYUVfromRGB(candidateColorTile.tileColor.val);

							double error = (candidateColorYUV[0] - (measuredColorYUV[0] + luminousOffset))
									* (candidateColorYUV[0] - (measuredColorYUV[0] + luminousOffset))
									+ (candidateColorYUV[1] - measuredColorYUV[1])
									* (candidateColorYUV[1] - measuredColorYUV[1])
									+ (candidateColorYUV[2] - measuredColorYUV[2])
									* (candidateColorYUV[2] - measuredColorYUV[2]);

							colorError[n][m] = Math.sqrt(error);

							if (error < smallestError) {
								bestCandidate = candidateColorTile;
								smallestError = error;
							}
						}

					if (bestCandidate != face.observedTileArray[n][m])
						face.observedTileArray[n][m] = bestCandidate;
				}

			for (int n = 0; n < 3; n++)
				for (int m = 0; m < 3; m++) {
					double[] selectedColor = face.observedTileArray[n][m].tileColor.val;
					double[] measuredColor = face.measuredColorArray[n][m];
					colorErrorAfterCorrection += calculateError(selectedColor,
							measuredColor, true, luminousOffset);
				}
		}

		private static double calculateError(double[] selected,
				double[] measured, boolean useLuminous, double _luminousOffset) {
			double error = (selected[0] - (measured[0] + _luminousOffset))
					* (selected[0] - (measured[0] + _luminousOffset))
					+ (selected[1] - measured[1]) * (selected[1] - measured[1])
					+ (selected[2] - measured[2]) * (selected[2] - measured[2]);
			return Math.sqrt(error);
		}

	}

}
