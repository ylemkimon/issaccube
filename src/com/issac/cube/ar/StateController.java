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

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.opengl.Matrix;
import android.os.Bundle;

import com.issac.cube.ar.Constants.AppStateEnum;
import com.issac.cube.ar.Constants.ColorTileEnum;
import com.issac.cube.ar.Constants.FaceNameEnum;
import com.issac.cube.ar.Constants.GestureRecogniztionStateEnum;

public class StateController {

	private final StateModel stateModel;
	private final ARActivity activity;
	private int gotItCount = 0;
	private Face candidateFace = null;
	private int consecutiveCandidateFaceCount = 0;
	private Face lastNewStableFace = null;
	private boolean allowOneMoreRotation = false;
	private boolean scheduleReset = false;

	public StateController(StateModel stateModel, ARActivity activity) {
		this.stateModel = stateModel;
		this.activity = activity;
	}

	public void onFaceEvent(Face face) {
		final int consecutiveCandidateCountThreashold = 3;

		if (scheduleReset) {
			gotItCount = 0;
			scheduleReset = false;
			candidateFace = null;
			consecutiveCandidateFaceCount = 0;
			lastNewStableFace = null;
			allowOneMoreRotation = false;
			stateModel.reset();
		}

		onFrameEvent();

		switch (stateModel.gestureRecogniztionState) {

		case UNKNOWN:
			if (face.solved) {
				stateModel.gestureRecogniztionState = GestureRecogniztionStateEnum.PENDING;
				candidateFace = face;
				consecutiveCandidateFaceCount = 0;
			}
			break;

		case PENDING:
			if (face.solved) {
				if (face.myHashCode == candidateFace.myHashCode) {
					if (consecutiveCandidateFaceCount > consecutiveCandidateCountThreashold) {
						if (lastNewStableFace == null
								|| face.myHashCode != lastNewStableFace.myHashCode) {
							lastNewStableFace = face;
							stateModel.gestureRecogniztionState = GestureRecogniztionStateEnum.NEW_STABLE;
							onNewStableFaceEvent(face);
							onStableFaceEvent(candidateFace);
						} else {
							stateModel.gestureRecogniztionState = GestureRecogniztionStateEnum.STABLE;
							onStableFaceEvent(candidateFace);
						}
					} else
						consecutiveCandidateFaceCount++;
				} else
					stateModel.gestureRecogniztionState = GestureRecogniztionStateEnum.UNKNOWN;
			} else
				stateModel.gestureRecogniztionState = GestureRecogniztionStateEnum.UNKNOWN;
			break;

		case STABLE:
			if (face.solved) {
				if (face.myHashCode != candidateFace.myHashCode) {
					stateModel.gestureRecogniztionState = GestureRecogniztionStateEnum.PARTIAL;
					consecutiveCandidateFaceCount = 0;
				}
			} else {
				stateModel.gestureRecogniztionState = GestureRecogniztionStateEnum.PARTIAL;
				consecutiveCandidateFaceCount = 0;
			}
			break;

		case PARTIAL:
			if (face.solved) {
				if (face.myHashCode == candidateFace.myHashCode) {
					if (lastNewStableFace != null
							&& face.myHashCode == lastNewStableFace.myHashCode)
						stateModel.gestureRecogniztionState = GestureRecogniztionStateEnum.NEW_STABLE;
					else
						stateModel.gestureRecogniztionState = GestureRecogniztionStateEnum.STABLE;
				} else if (consecutiveCandidateFaceCount > consecutiveCandidateCountThreashold) {
					stateModel.gestureRecogniztionState = GestureRecogniztionStateEnum.UNKNOWN;
					offNewStableFaceEvent();
					offStableFaceEvent();
				} else
					consecutiveCandidateFaceCount++;
			} else if (consecutiveCandidateFaceCount > consecutiveCandidateCountThreashold) {
				stateModel.gestureRecogniztionState = GestureRecogniztionStateEnum.UNKNOWN;
				offNewStableFaceEvent();
				offStableFaceEvent();
			} else
				consecutiveCandidateFaceCount++;
			break;

		case NEW_STABLE:
			if (face.solved) {
				if (face.myHashCode != candidateFace.myHashCode) {
					stateModel.gestureRecogniztionState = GestureRecogniztionStateEnum.PARTIAL;
					consecutiveCandidateFaceCount = 0;
				}
			} else {
				stateModel.gestureRecogniztionState = GestureRecogniztionStateEnum.PARTIAL;
				consecutiveCandidateFaceCount = 0;
			}
			break;
		}
	}

	private void onStableFaceEvent(Face face) {
		switch (stateModel.appState) {

		case WAITING_MOVE:
			stateModel.appState = AppStateEnum.ROTATE_FACE;
			String moveNumonic = stateModel.solutionResultsArray[stateModel.solutionResultIndex];

			int rotation = 0;
			FaceNameEnum rotationTile[] = null;
			if (moveNumonic.length() == 1)
				rotation = 1;
			else if (moveNumonic.charAt(1) == '2')
				rotation = 2;
			else if (moveNumonic.charAt(1) == '\'')
				rotation = 3;
			switch (moveNumonic.charAt(0)) {
			case 'r':
			case 'x':
				rotation = 4 - rotation;
			case 'l':
			case 'M':
				rotationTile = new FaceNameEnum[] { FaceNameEnum.FRONT,
						FaceNameEnum.UP, FaceNameEnum.BACK, FaceNameEnum.DOWN };
				break;

			case 'u':
			case 'y':
				rotation = 4 - rotation;
			case 'd':
			case 'E':
				rotationTile = new FaceNameEnum[] { FaceNameEnum.FRONT,
						FaceNameEnum.LEFT, FaceNameEnum.BACK,
						FaceNameEnum.RIGHT };
				break;

			case 'b':
				rotation = 4 - rotation;
			case 'f':
			case 'z':
			case 'S':
				rotationTile = new FaceNameEnum[] { FaceNameEnum.LEFT,
						FaceNameEnum.DOWN, FaceNameEnum.RIGHT, FaceNameEnum.UP };
				break;
			}
			if (rotationTile != null)
				for (; rotation > 0; rotation--) {
					ColorTileEnum temp = stateModel
							.getFaceByName(rotationTile[0]).transformedTileArray[1][1];
					for (int i = 0; i < 3; i++)
						stateModel.getFaceByName(rotationTile[i]).transformedTileArray[1][1] = stateModel
								.getFaceByName(rotationTile[i + 1]).transformedTileArray[1][1];
					stateModel.getFaceByName(rotationTile[3]).transformedTileArray[1][1] = temp;
				}

			do {
				if (!moveNumonic.trim().isEmpty()
						&& moveNumonic.charAt(0) == '/')
					if (stateModel.solutionResultsStage < 0)
						stateModel.solutionResultsStage = -stateModel.solutionResultsStage;
					else
						stateModel.solutionResultsStage--;
				moveNumonic = stateModel.solutionResultsArray[++stateModel.solutionResultIndex];
			} while (moveNumonic.trim().isEmpty()
					|| moveNumonic.charAt(0) == '/');

			activity.mediaPlayer.stop();
			activity.mediaPlayer.start();

			if (stateModel.solutionResultIndex == stateModel.solutionResultsArray.length)
				stateModel.appState = AppStateEnum.DONE;
			else {
				StringBuilder solutionResults = new StringBuilder();
				for (int i = stateModel.solutionResultIndex - 1; i < stateModel.solutionResultsArray.length; i++) {
					if (i < 0)
						continue;
					String move = stateModel.solutionResultsArray[i];
					if (!move.trim().isEmpty()) {
						solutionResults.append(move);
						solutionResults.append(' ');
					}
				}
				stateModel.solutionResults = solutionResults.toString();
			}
			break;

		default:
			break;
		}
	}

	public void offStableFaceEvent() {
		switch (stateModel.appState) {

		case ROTATE_FACE:
			stateModel.appState = AppStateEnum.WAITING_MOVE;
			break;

		default:
			break;
		}
	}

	private void onNewStableFaceEvent(Face candidateFace) {
		switch (stateModel.appState) {

		case START:
			stateModel.adopt(candidateFace);
			stateModel.appState = AppStateEnum.GOT_IT;
			gotItCount = 0;
			break;

		case SEARCHING:
			activity.mediaPlayer.stop();
			activity.mediaPlayer.start();

			stateModel.adopt(candidateFace);

			if (stateModel.getNumObservedFaces() < 6) {
				stateModel.appState = AppStateEnum.GOT_IT;
				allowOneMoreRotation = true;
				gotItCount = 0;
			} else if (allowOneMoreRotation) {
				stateModel.appState = AppStateEnum.GOT_IT;
				allowOneMoreRotation = false;
				gotItCount = 0;
			} else
				stateModel.appState = AppStateEnum.COMPLETE;
			break;

		default:
			break;
		}
	}

	private void offNewStableFaceEvent() {
		switch (stateModel.appState) {

		case ROTATE_CUBE:
			stateModel.appState = AppStateEnum.SEARCHING;
			break;

		default:
			break;
		}

		if (stateModel.adoptFaceCount <= 6) {
			float[] rotationMatrix = new float[16];
			Matrix.setIdentityM(rotationMatrix, 0);
			if (stateModel.adoptFaceCount % 2 == 0)
				Matrix.rotateM(rotationMatrix, 0, -90f, 1.0f, 0.0f, 0.0f);
			else
				Matrix.rotateM(rotationMatrix, 0, +90f, 0.0f, 0.0f, 1.0f);
			float[] z = new float[16];
			Matrix.multiplyMM(z, 0, rotationMatrix, 0,
					stateModel.additionalGLCubeRotation, 0);
			System.arraycopy(z, 0, stateModel.additionalGLCubeRotation, 0,
					stateModel.additionalGLCubeRotation.length);
		}
	}

	private void onFrameEvent() {
		switch (stateModel.appState) {

		case GOT_IT:
			if (gotItCount < 3)
				gotItCount++;
			else {
				stateModel.appState = AppStateEnum.ROTATE_CUBE;
				gotItCount = 0;
			}
			break;

		case COMPLETE:
		case BAD_COLORS:
			if (stateModel.appState == AppStateEnum.COMPLETE
					&& !stateModel.manualColor)
				new ColorRecognizer.CubeRecognizer(stateModel).recognize();

			String cubeString = null;
			try {
				cubeString = stateModel.getStringRepresentationOfCube();
			} catch (NullPointerException e) {
				stateModel.appState = AppStateEnum.BAD_COLORS;
			}

			if (stateModel.appState == AppStateEnum.BAD_COLORS) {
				stateModel.appState = AppStateEnum.WAIT_SOLVING;
				activity.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						AlertDialog.Builder alert = new AlertDialog.Builder(
								activity);
						CubeLayoutEditor cl = new CubeLayoutEditor(activity,
								stateModel);
						alert.setView(cl).setPositiveButton(
								android.R.string.ok,
								new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog,
											int id) {
										stateModel.manualColor = true;
										stateModel.appState = AppStateEnum.COMPLETE;
									}
								});
						AlertDialog alertDialog = alert.create();
						alertDialog.show();
						alertDialog.getWindow().setLayout(1270, 710);
					}
				});
				break;
			}

			stateModel.appState = AppStateEnum.WAIT_SOLVING;
			Intent intent = new Intent();
			intent.setClassName("com.hipipal.qpyplus",
					"com.hipipal.qpyplus.MPyApi");
			intent.setAction("com.hipipal.qpyplus.action.MPyApi");

			Bundle mBundle = new Bundle();
			mBundle.putString("app", "rubik");
			mBundle.putString("act", "onPyApi");
			mBundle.putString("flag", "");
			mBundle.putString("param", "");
			mBundle.putString(
					"pycode",
					"import pycuber as a, pycuber.solver as b\nprint b.CFOPSolver(a.Cube(a.helpers.array_to_cubies(\""
							+ cubeString + "\"))).solve()");

			intent.putExtras(mBundle);
			activity.startActivityForResult(intent, ARActivity.SCRIPT_EXEC_PY);
			break;

		default:
			break;
		}
	}

	public void reset() {
		scheduleReset = true;
	}
}
