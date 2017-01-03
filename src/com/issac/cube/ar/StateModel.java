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

import java.util.HashMap;

import android.opengl.Matrix;

import com.issac.cube.ar.Constants.AppStateEnum;
import com.issac.cube.ar.Constants.ColorTileEnum;
import com.issac.cube.ar.Constants.FaceNameEnum;
import com.issac.cube.ar.Constants.GestureRecogniztionStateEnum;

public class StateModel {

	public Face activeFace;
	public HashMap<FaceNameEnum, Face> faceMap = new HashMap<Constants.FaceNameEnum, Face>(
			6);
	public AppStateEnum appState = AppStateEnum.START;
	public GestureRecogniztionStateEnum gestureRecogniztionState = GestureRecogniztionStateEnum.UNKNOWN;
	public String solutionResults;
	public int solutionResultsStage;
	public String[] solutionResultsArray;
	public int solutionResultIndex;
	public int adoptFaceCount = 0;
	public float[] additionalGLCubeRotation = new float[16];
	public boolean renderPilotCube = true;
	public CubePoseEstimator cubePoseEstimator;
	public CameraParameters cameraParameters;
	public boolean manualColor = false;

	public StateModel() {
		reset();
	}

	public void adopt(Face face) {
		switch (adoptFaceCount) {
		case 0:
			face.faceNameEnum = FaceNameEnum.UP;
			face.transformedTileArray = face.observedTileArray.clone();
			break;
		case 1:
			face.faceNameEnum = FaceNameEnum.RIGHT;
			face.transformedTileArray = Util
					.rotateArrayClockwise(face.observedTileArray);
			break;
		case 2:
			face.faceNameEnum = FaceNameEnum.FRONT;
			face.transformedTileArray = Util
					.rotateArrayClockwise(face.observedTileArray);
			break;
		case 3:
			face.faceNameEnum = FaceNameEnum.DOWN;
			face.transformedTileArray = Util
					.rotateArrayClockwise(face.observedTileArray);
			break;
		case 4:
			face.faceNameEnum = FaceNameEnum.LEFT;
			face.transformedTileArray = Util
					.rotateArray180(face.observedTileArray);
			break;
		case 5:
			face.faceNameEnum = FaceNameEnum.BACK;
			face.transformedTileArray = Util
					.rotateArray180(face.observedTileArray);
			break;
		}

		if (adoptFaceCount < 6)
			faceMap.put(face.faceNameEnum, face);

		adoptFaceCount++;
	}

	public Face getFaceByName(FaceNameEnum faceNameEnum) {
		return faceMap.get(faceNameEnum);
	}

	public int getNumObservedFaces() {
		return faceMap.size();
	}

	public String getStringRepresentationOfCube() {
		FaceNameEnum[] faces = { FaceNameEnum.LEFT, FaceNameEnum.UP,
				FaceNameEnum.FRONT, FaceNameEnum.DOWN, FaceNameEnum.RIGHT,
				FaceNameEnum.BACK };

		HashMap<ColorTileEnum, FaceNameEnum> colorTileToNameMap = new HashMap<ColorTileEnum, FaceNameEnum>(
				6);
		for (FaceNameEnum face : faces)
			colorTileToNameMap.put(
					getFaceByName(face).transformedTileArray[1][1], face);

		StringBuffer sb = new StringBuffer();
		for (FaceNameEnum face : faces)
			sb.append(getStringRepresentationOfFace(colorTileToNameMap,
					getFaceByName(face)));
		return sb.toString();
	}

	private StringBuffer getStringRepresentationOfFace(
			HashMap<ColorTileEnum, FaceNameEnum> colorTileToNameMap, Face face) {

		StringBuffer sb = new StringBuffer();
		ColorTileEnum[][] virtualLogicalTileArray = face.transformedTileArray;
		for (int m = 0; m < 3; m++)
			for (int n = 0; n < 3; n++)
				sb.append(getCharacterRepresentingColor(colorTileToNameMap,
						virtualLogicalTileArray[n][m]));
		return sb;
	}

	private char getCharacterRepresentingColor(
			HashMap<ColorTileEnum, FaceNameEnum> colorTileToNameMap,
			ColorTileEnum colorEnum) {
		switch (colorTileToNameMap.get(colorEnum)) {
		case FRONT:
			return 'F';
		case BACK:
			return 'B';
		case DOWN:
			return 'D';
		case LEFT:
			return 'L';
		case RIGHT:
			return 'R';
		case UP:
			return 'U';
		}
		return 0;
	}

	public void reset() {
		activeFace = null;
		faceMap = new HashMap<Constants.FaceNameEnum, Face>(6);
		appState = AppStateEnum.START;
		gestureRecogniztionState = GestureRecogniztionStateEnum.UNKNOWN;
		solutionResults = null;
		solutionResultsArray = null;
		solutionResultIndex = 0;
		adoptFaceCount = 0;
		renderPilotCube = true;
		cubePoseEstimator = null;
		manualColor = false;
		Matrix.setIdentityM(additionalGLCubeRotation, 0);
	}
}
