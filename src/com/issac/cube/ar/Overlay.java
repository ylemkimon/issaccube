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

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;

import com.issac.cube.ar.Constants.AnnotationModeEnum;
import com.issac.cube.ar.Constants.AppStateEnum;
import com.issac.cube.ar.Constants.ColorTileEnum;
import com.issac.cube.ar.Constants.FaceNameEnum;
import com.issac.cube.ar.Constants.GestureRecogniztionStateEnum;

public class Overlay {

	private final StateModel stateModel;
	Mat overlay;

	public Overlay(StateModel stateModel) {
		this.stateModel = stateModel;
	}

	public Mat drawOverlay(Mat image) {
		Face face = stateModel.activeFace;

		if (MenuParam.faceOverlayDisplay && face != null)
			drawFaceOverlayAnnotation(image, face);

		if (MenuParam.annotationMode != AnnotationModeEnum.NORMAL) {
			stateModel.renderPilotCube = false;
			Core.rectangle(image, new Point(0, 0), new Point(450, 720),
					ColorTileEnum.BLACK.cvColor, -1);
		} else
			stateModel.renderPilotCube = true;

		switch (MenuParam.annotationMode) {

		case LAYOUT:
			drawFlatCubeLayoutRepresentations(image);
			break;

		case FACE_METRICS:
			if (face != null)
				drawFaceMetrics(image, face);
			break;

		case COLOR_FACE:
			if (face != null && face.solved) {
				drawColorGrid(image, true);
				drawTileColorMetrics(image, face, true);
			}
			break;

		case COLOR_CUBE:
			drawColorGrid(image, false);
			for (Face cubeFace : stateModel.faceMap.values())
				drawTileColorMetrics(image, cubeFace, false);
			break;

		case NORMAL:
			break;
		}

		return image;
	}

	private void drawFaceOverlayAnnotation(Mat img, Face face) {
		Scalar color1 = ColorTileEnum.RED.cvColor;
		if (face.solved)
			if (stateModel.gestureRecogniztionState == GestureRecogniztionStateEnum.STABLE
					|| stateModel.gestureRecogniztionState == GestureRecogniztionStateEnum.NEW_STABLE)
				color1 = ColorTileEnum.GREEN.cvColor;
			else
				color1 = ColorTileEnum.YELLOW.cvColor;
		else if (face.rhombusList.size() >= 3 && face.lmsResult.valid)
			color1 = ColorTileEnum.ORANGE.cvColor;

		Scalar color2 = ColorTileEnum.BLACK.cvColor;
		switch (stateModel.appState) {

		case START:
		case ROTATE_CUBE:
		case ROTATE_FACE:
			color2 = ColorTileEnum.RED.cvColor;
			break;

		case SEARCHING:
		case WAITING_MOVE:
			color2 = ColorTileEnum.YELLOW.cvColor;
			break;

		case GOT_IT:
		case COMPLETE:
		case DONE:
		case WAIT_SOLVING:
			color2 = ColorTileEnum.GREEN.cvColor;
			break;
		default:
			break;
		}

		Point center = face.getTileCenterInPixels(1, 1);
		int radius = (int) Math.sqrt(face.alphaLatticLength
				* face.alphaLatticLength + face.betaLatticLength
				* face.betaLatticLength) * 3 / 2;
		Core.circle(img, center, radius + 10, color1, 10);
		Core.circle(img, center, radius + 20, color2, 10);

		if (face.solved && stateModel.solutionResults == null)
			for (int n = 0; n < 3; n++)
				for (int m = 0; m < 3; m++)
					Core.circle(img, face.getTileCenterInPixels(n, m), 20,
							face.observedTileArray[n][m].cvColor, -1);
		else if (stateModel.appState == AppStateEnum.ROTATE_FACE
				|| stateModel.appState == AppStateEnum.WAITING_MOVE) {
			center.x -= 150.0;
			center.y += 100.0;
			Core.putText(
					img,
					stateModel.solutionResultsArray[stateModel.solutionResultIndex],
					center, Constants.FontFace, 18,
					ColorTileEnum.WHITE.cvColor, 30);
		}
	}

	private void drawFlatCubeLayoutRepresentations(Mat image) {
		final int tSize = 35;
		drawFlatFaceRepresentation(image,
				stateModel.getFaceByName(FaceNameEnum.UP), 3 * tSize + 15,
				0 * tSize + 225, tSize, false);
		drawFlatFaceRepresentation(image,
				stateModel.getFaceByName(FaceNameEnum.LEFT), 0 * tSize + 15,
				3 * tSize + 225, tSize, false);
		drawFlatFaceRepresentation(image,
				stateModel.getFaceByName(FaceNameEnum.FRONT), 3 * tSize + 15,
				3 * tSize + 225, tSize, false);
		drawFlatFaceRepresentation(image,
				stateModel.getFaceByName(FaceNameEnum.RIGHT), 6 * tSize + 15,
				3 * tSize + 225, tSize, false);
		drawFlatFaceRepresentation(image,
				stateModel.getFaceByName(FaceNameEnum.BACK), 9 * tSize + 15,
				3 * tSize + 225, tSize, false);
		drawFlatFaceRepresentation(image,
				stateModel.getFaceByName(FaceNameEnum.DOWN), 3 * tSize + 15,
				6 * tSize + 225, tSize, false);
	}

	private void drawFlatFaceRepresentation(Mat image, Face face, int x, int y,
			int tSize, boolean observed) {
		if (face == null || !face.solved)
			Core.rectangle(image, new Point(x, y), new Point(x + 3 * tSize, y
					+ 3 * tSize), ColorTileEnum.GREY.cvColor, -1);
		else
			for (int n = 0; n < 3; n++)
				for (int m = 0; m < 3; m++) {
					ColorTileEnum colorTile = observed ? face.observedTileArray[n][m]
							: face.transformedTileArray[n][m];

					if (colorTile != null)
						Core.rectangle(image, new Point(x + tSize * n, y
								+ tSize * m), new Point(x + tSize * (n + 1), y
								+ tSize * (m + 1)), colorTile.cvColor, -1);
					else
						Core.rectangle(image, new Point(x + tSize * n, y
								+ tSize * m), new Point(x + tSize * (n + 1), y
								+ tSize * (m + 1)), ColorTileEnum.GREY.cvColor,
								-1);
				}
	}

	private void drawFaceMetrics(Mat image, Face face) {
		drawFlatFaceRepresentation(image, face, 50, 50, 50, true);

		Core.putText(image, "Status = " + face.solved, new Point(50, 300),
				Constants.FontFace, 2, ColorTileEnum.WHITE.cvColor, 2);
		Core.putText(
				image,
				String.format("AlphaA = %4.1f", face.alphaAngle * 180.0
						/ Math.PI), new Point(50, 350), Constants.FontFace, 2,
				ColorTileEnum.WHITE.cvColor, 2);
		Core.putText(
				image,
				String.format("BetaA  = %4.1f", face.betaAngle * 180.0
						/ Math.PI), new Point(50, 400), Constants.FontFace, 2,
				ColorTileEnum.WHITE.cvColor, 2);
		Core.putText(image,
				String.format("AlphaL = %4.0f", face.alphaLatticLength),
				new Point(50, 450), Constants.FontFace, 2,
				ColorTileEnum.WHITE.cvColor, 2);
		Core.putText(image,
				String.format("BetaL  = %4.0f", face.betaLatticLength),
				new Point(50, 500), Constants.FontFace, 2,
				ColorTileEnum.WHITE.cvColor, 2);
		Core.putText(image, String.format("Gamma  = %4.2f", face.gammaRatio),
				new Point(50, 550), Constants.FontFace, 2,
				ColorTileEnum.WHITE.cvColor, 2);
		Core.putText(image,
				String.format("Sigma  = %5.0f", face.lmsResult.sigma),
				new Point(50, 600), Constants.FontFace, 2,
				ColorTileEnum.WHITE.cvColor, 2);
	}

	private void drawColorGrid(Mat image, boolean single) {
		Core.rectangle(image, new Point(0, 0), new Point(570, 720),
				ColorTileEnum.BLACK.cvColor, -1);
		Core.rectangle(image, new Point(-256 + 256, -256 + 400), new Point(
				256 + 256, 256 + 400), ColorTileEnum.WHITE.cvColor);
		Core.line(image, new Point(0 + 256, -256 + 400), new Point(0 + 256,
				256 + 400), ColorTileEnum.WHITE.cvColor);
		Core.line(image, new Point(-256 + 256, 0 + 400), new Point(256 + 256,
				0 + 400), ColorTileEnum.WHITE.cvColor);

		for (ColorTileEnum colorTile : ColorTileEnum.values()) {
			if (!colorTile.isTileColor)
				continue;

			double[] targetColorYUV = Util
					.getYUVfromRGB(colorTile.tileColor.val);
			double x = 2 * targetColorYUV[1] + 256;
			double y = 2 * targetColorYUV[2] + 400;

			Core.circle(image, new Point(x, y), single ? 10 : 15,
					colorTile.cvColor, single ? -1 : 3);
			if (single)
				Core.line(image, new Point(502, -256 + 2 * targetColorYUV[0]
						+ 400), new Point(522, -256 + 2 * targetColorYUV[0]
						+ 400), colorTile.cvColor, 3);
			else
				Core.circle(image, new Point(512, -256 + 2 * targetColorYUV[0]
						+ 400), 15, colorTile.cvColor, 3);
		}
	}

	private void drawTileColorMetrics(Mat image, Face face, boolean single) {
		for (int n = 0; n < 3; n++)
			for (int m = 0; m < 3; m++) {
				double[] measuredTileColor = face.measuredColorArray[n][m];
				double[] measuredTileColorYUV = Util
						.getYUVfromRGB(measuredTileColor);
				double luminousScaled = measuredTileColorYUV[0] * 2 - 256;
				double uChromananceScaled = measuredTileColorYUV[1] * 2;
				double vChromananceScaled = measuredTileColorYUV[2] * 2;

				if (single) {
					String text = Character
							.toString(face.observedTileArray[n][m].symbol);
					Core.putText(image, text,
							new Point(uChromananceScaled + 256,
									vChromananceScaled + 400),
							Constants.FontFace, 3,
							face.observedTileArray[n][m].cvColor, 3);
					Core.putText(image, text, new Point(512 + 20,
							luminousScaled + 400), Constants.FontFace, 3,
							face.observedTileArray[n][m].cvColor, 3);
				} else {
					Core.circle(image, new Point(uChromananceScaled + 256,
							vChromananceScaled + 400), 10, new Scalar(
							face.observedTileArray[n][m].cvColor.val), -1);
					Core.line(image, new Point(522 + 20, luminousScaled + 400),
							new Point(542 + 20, luminousScaled + 400),
							face.observedTileArray[n][m].cvColor, 3);
				}
			}
	}

}
