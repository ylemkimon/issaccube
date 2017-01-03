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
package com.issac.cube.gl;

import static android.opengl.GLES20.GL_COMPILE_STATUS;
import static android.opengl.GLES20.GL_LINK_STATUS;
import static android.opengl.GLES20.glCompileShader;
import static android.opengl.GLES20.glCreateShader;
import static android.opengl.GLES20.glDeleteProgram;
import static android.opengl.GLES20.glDeleteShader;
import static android.opengl.GLES20.glGetProgramiv;
import static android.opengl.GLES20.glGetShaderiv;
import static android.opengl.GLES20.glShaderSource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import org.opencv.core.Scalar;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import com.issac.cube.ar.Constants.AppStateEnum;
import com.issac.cube.ar.Constants.ColorTileEnum;
import com.issac.cube.ar.Constants.FaceNameEnum;
import com.issac.cube.ar.CubePoseEstimator;
import com.issac.cube.ar.MenuParam;
import com.issac.cube.ar.StateModel;
import com.issac.cube.gl.GLArrow.Amount;
import com.issac.cube.gl.GLCube.Transparency;

public class GLRenderer implements GLSurfaceView.Renderer {

	public enum Rotation {
		CLOCKWISE, COUNTER_CLOCKWISE, ONE_HUNDRED_EIGHTY
	};

	public enum Direction {
		POSITIVE, NEGATIVE
	};

	private final StateModel stateModel;
	private final Context context;
	int programID;
	private GLArrow arrowQuarterTurn;
	private GLArrow arrowHalfTurn;
	private GLCube overlayGLCube;
	private GLCube pilotGLCube;
	private int mWidth, mHeight;
	private final float[] mProjectionMatrix = new float[16];
	private boolean rotationActive = false;
	private long timeReference = 0;

	public GLRenderer(StateModel stateModel, Context context) {
		this.stateModel = stateModel;
		this.context = context;
	}

	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		int vertexShaderID = compileShader(GLES20.GL_VERTEX_SHADER,
				"simple_vertex_shader.glsl");
		int fragmentShaderID = compileShader(GLES20.GL_FRAGMENT_SHADER,
				"simple_fragment_shader.glsl");

		programID = GLES20.glCreateProgram();
		GLES20.glAttachShader(programID, vertexShaderID);
		GLES20.glAttachShader(programID, fragmentShaderID);
		GLES20.glLinkProgram(programID);

		final int[] linkStatus = new int[1];
		glGetProgramiv(programID, GL_LINK_STATUS, linkStatus, 0);

		if (linkStatus[0] == 0)
			glDeleteProgram(programID);

		GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

		pilotGLCube = new GLCube(stateModel);

		overlayGLCube = new GLCube(stateModel);

		arrowQuarterTurn = new GLArrow(Amount.QUARTER_TURN);
		arrowHalfTurn = new GLArrow(Amount.HALF_TURN);
	}

	@Override
	public void onSurfaceChanged(GL10 gl, int width, int height) {
		if (height == 0)
			height = 1;

		mWidth = width;
		mHeight = height;

		if (MenuParam.stereoscopicView)
			GLES20.glViewport(0, height / 4, width / 2, height / 2);
		else
			GLES20.glViewport(0, 0, width, height);

		float ratio = (float) width / height;

		Matrix.frustumM(mProjectionMatrix, 0, -ratio, ratio, -1, 1, 2, 100);
	}

	@Override
	public void onDrawFrame(GL10 unused) {
		GLES20.glEnable(GLES20.GL_DEPTH_TEST);

		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

		if (MenuParam.stereoscopicView) { // TODO 3D depth
			GLES20.glViewport((int) MenuParam.threeDDepthParam.value,
					mHeight / 4, mWidth / 2, mHeight / 2);
			drawFrame();
			GLES20.glViewport(mWidth / 2
					- (int) MenuParam.threeDDepthParam.value, mHeight / 4,
					mWidth / 2, mHeight / 2);
			drawFrame();
		} else
			drawFrame();
	}

	private void drawFrame() {
		final float[] viewMatrix = new float[16];
		final float[] pvMatrix = new float[16];
		final float[] mvpMatrix = new float[16];

		CubePoseEstimator myCubeReconstructor = stateModel.cubePoseEstimator;

		if (myCubeReconstructor == null)
			return;

		Matrix.setLookAtM(viewMatrix, 0, 0, 0, 0, 0f, 0f, -1f, 0f, 1.0f, 0.0f);

		Matrix.multiplyMM(pvMatrix, 0, mProjectionMatrix, 0, viewMatrix, 0);

		if (MenuParam.cubeOverlayDisplay
				|| stateModel.appState == AppStateEnum.ROTATE_CUBE
				|| stateModel.appState == AppStateEnum.ROTATE_FACE) {

			System.arraycopy(pvMatrix, 0, mvpMatrix, 0, pvMatrix.length);

			Matrix.translateM(mvpMatrix, 0, myCubeReconstructor.x,
					myCubeReconstructor.y, myCubeReconstructor.z);

			rotateMatrix(mvpMatrix, myCubeReconstructor.poseRotationMatrix);

			if (MenuParam.cubeOverlayDisplay)
				overlayGLCube.draw(mvpMatrix, Transparency.TRANSLUCENT,
						programID);
			else
				overlayGLCube.draw(mvpMatrix, Transparency.TRANSPARENT,
						programID);

			switch (stateModel.appState) {
			case ROTATE_CUBE:
				renderCubeFullRotationArrow(mvpMatrix, getRotationInDegrees());
				break;

			case ROTATE_FACE:
				renderCubeEdgeRotationArrow(mvpMatrix, getRotationInDegrees());
				break;

			default:
				break;
			}
		}

		if (MenuParam.pilotCubeDisplay && stateModel.renderPilotCube) {
			System.arraycopy(pvMatrix, 0, mvpMatrix, 0, pvMatrix.length);

			Matrix.translateM(mvpMatrix, 0, -6.0f, 0.0f, -10.0f);

			rotateMatrix(mvpMatrix, myCubeReconstructor.poseRotationMatrix);

			rotateMatrix(mvpMatrix, stateModel.additionalGLCubeRotation);

			pilotGLCube.draw(mvpMatrix, Transparency.OPAQUE, programID);
		}
	}

	public int compileShader(int type, String fileName) {
		final int shaderObjectId = glCreateShader(type);

		if (shaderObjectId == 0)
			return 0;

		try {
			InputStream stream = context.getAssets().open(fileName);
			InputStreamReader inputStreamReader = new InputStreamReader(stream);
			BufferedReader reader = new BufferedReader(inputStreamReader);

			StringBuilder buffer = new StringBuilder();
			String str;

			while ((str = reader.readLine()) != null) {
				buffer.append(str);
				buffer.append("\n");
			}

			reader.close();
			String shaderCode = buffer.toString();

			glShaderSource(shaderObjectId, shaderCode);

			glCompileShader(shaderObjectId);

			final int[] compileStatus = new int[1];
			glGetShaderiv(shaderObjectId, GL_COMPILE_STATUS, compileStatus, 0);

			if (compileStatus[0] == 0) {
				glDeleteShader(shaderObjectId);
				return 0;
			}

			return shaderObjectId;
		} catch (IOException e) {
			throw new RuntimeException("Could not open asset: " + fileName, e);
		}
	}

	public void rotateMatrix(float[] x, float[] y) {
		float[] z = new float[16];
		Matrix.multiplyMM(z, 0, x, 0, y, 0);
		System.arraycopy(z, 0, x, 0, x.length);
	}

	private int getRotationInDegrees() {
		long time = System.currentTimeMillis();

		if (stateModel.appState == AppStateEnum.ROTATE_CUBE
				|| stateModel.appState == AppStateEnum.ROTATE_FACE) {
			if (!rotationActive) {
				timeReference = time;
				rotationActive = true;
			}
		} else
			rotationActive = false;

		int arrowRotationInDegrees = (int) ((time - timeReference) / 10 % 90);
		return arrowRotationInDegrees;
	}

	private void renderCubeEdgeRotationArrow(final float[] mvpMatrix,
			int arrowRotationInDegrees) {
		String moveNumonic = stateModel.solutionResultsArray[stateModel.solutionResultIndex];
		if (moveNumonic.trim().isEmpty() || moveNumonic.charAt(0) == '/')
			return;

		Rotation rotation = null;
		boolean wide = Character.isLowerCase(moveNumonic.charAt(0)), full = false;
		if (moveNumonic.length() == 1)
			rotation = Rotation.CLOCKWISE;
		else if (moveNumonic.charAt(1) == '2')
			rotation = Rotation.ONE_HUNDRED_EIGHTY;
		else if (moveNumonic.charAt(1) == '\'')
			rotation = Rotation.COUNTER_CLOCKWISE;

		Scalar color = null;
		Direction direction = null;

		switch (Character.toUpperCase(moveNumonic.charAt(0))) {
		case 'U':
			color = stateModel.getFaceByName(FaceNameEnum.UP).transformedTileArray[1][1].cvColor;
			Matrix.translateM(mvpMatrix, 0, 0.0f, wide ? +2.0f : +3.0f, 0.0f);
			Matrix.rotateM(mvpMatrix, 0, 90f, 1.0f, 0.0f, 0.0f);
			direction = rotation == Rotation.CLOCKWISE ? Direction.NEGATIVE
					: Direction.POSITIVE;
			break;
		case 'D':
			color = stateModel.getFaceByName(FaceNameEnum.DOWN).transformedTileArray[1][1].cvColor;
			Matrix.translateM(mvpMatrix, 0, 0.0f, wide ? -2.0f : -3.0f, 0.0f);
			Matrix.rotateM(mvpMatrix, 0, 90f, 1.0f, 0.0f, 0.0f);
			direction = rotation == Rotation.COUNTER_CLOCKWISE ? Direction.NEGATIVE
					: Direction.POSITIVE;
			break;
		case 'L':
			color = stateModel.getFaceByName(FaceNameEnum.LEFT).transformedTileArray[1][1].cvColor;
			Matrix.translateM(mvpMatrix, 0, wide ? -2.0f : -3.0f, 0.0f, 0.0f);
			Matrix.rotateM(mvpMatrix, 0, 90f, 0.0f, 1.0f, 0.0f);
			direction = rotation == Rotation.CLOCKWISE ? Direction.NEGATIVE
					: Direction.POSITIVE;
			Matrix.rotateM(mvpMatrix, 0, 30f, 0.0f, 0.0f, 1.0f);
			break;
		case 'R':
			color = stateModel.getFaceByName(FaceNameEnum.RIGHT).transformedTileArray[1][1].cvColor;
			Matrix.translateM(mvpMatrix, 0, wide ? +2.0f : +3.0f, 0.0f, 0.0f);
			Matrix.rotateM(mvpMatrix, 0, 90f, 0.0f, 1.0f, 0.0f);
			direction = rotation == Rotation.COUNTER_CLOCKWISE ? Direction.NEGATIVE
					: Direction.POSITIVE;
			Matrix.rotateM(mvpMatrix, 0, 30f, 0.0f, 0.0f, 1.0f);
			break;
		case 'F':
			color = stateModel.getFaceByName(FaceNameEnum.FRONT).transformedTileArray[1][1].cvColor;
			Matrix.translateM(mvpMatrix, 0, 0.0f, 0.0f, wide ? +2.0f : +3.0f);
			direction = rotation == Rotation.COUNTER_CLOCKWISE ? Direction.NEGATIVE
					: Direction.POSITIVE;
			Matrix.rotateM(mvpMatrix, 0, 30f, 0.0f, 0.0f, 1.0f);
			break;
		case 'B':
			color = stateModel.getFaceByName(FaceNameEnum.BACK).transformedTileArray[1][1].cvColor;
			Matrix.translateM(mvpMatrix, 0, 0.0f, 0.0f, wide ? -2.0f : -3.0f);
			direction = rotation == Rotation.CLOCKWISE ? Direction.NEGATIVE
					: Direction.POSITIVE;
			Matrix.rotateM(mvpMatrix, 0, 30f, 0.0f, 0.0f, 1.0f);
			break;
		case 'M':
			color = stateModel.getFaceByName(FaceNameEnum.LEFT).transformedTileArray[1][1].cvColor;
			Matrix.rotateM(mvpMatrix, 0, 90f, 0.0f, 1.0f, 0.0f);
			direction = rotation == Rotation.CLOCKWISE ? Direction.NEGATIVE
					: Direction.POSITIVE;
			Matrix.rotateM(mvpMatrix, 0, 30f, 0.0f, 0.0f, 1.0f);
			break;
		case 'E':
			color = stateModel.getFaceByName(FaceNameEnum.DOWN).transformedTileArray[1][1].cvColor;
			Matrix.rotateM(mvpMatrix, 0, 90f, 1.0f, 0.0f, 0.0f);
			direction = rotation == Rotation.COUNTER_CLOCKWISE ? Direction.NEGATIVE
					: Direction.POSITIVE;
			break;
		case 'S':
			color = stateModel.getFaceByName(FaceNameEnum.FRONT).transformedTileArray[1][1].cvColor;
			direction = rotation == Rotation.COUNTER_CLOCKWISE ? Direction.NEGATIVE
					: Direction.POSITIVE;
			Matrix.rotateM(mvpMatrix, 0, 30f, 0.0f, 0.0f, 1.0f);
			break;
		case 'X':
			color = stateModel.getFaceByName(FaceNameEnum.RIGHT).transformedTileArray[1][1].cvColor;
			Matrix.rotateM(mvpMatrix, 0, 90f, 0.0f, 1.0f, 0.0f);
			direction = rotation == Rotation.COUNTER_CLOCKWISE ? Direction.NEGATIVE
					: Direction.POSITIVE;
			Matrix.rotateM(mvpMatrix, 0, 30f, 0.0f, 0.0f, 1.0f);
			full = true;
			break;
		case 'Y':
			color = stateModel.getFaceByName(FaceNameEnum.UP).transformedTileArray[1][1].cvColor;
			Matrix.rotateM(mvpMatrix, 0, 90f, 1.0f, 0.0f, 0.0f);
			direction = rotation == Rotation.CLOCKWISE ? Direction.NEGATIVE
					: Direction.POSITIVE;
			full = true;
			break;
		case 'Z':
			color = stateModel.getFaceByName(FaceNameEnum.FRONT).transformedTileArray[1][1].cvColor;
			direction = rotation == Rotation.COUNTER_CLOCKWISE ? Direction.NEGATIVE
					: Direction.POSITIVE;
			Matrix.rotateM(mvpMatrix, 0, 30f, 0.0f, 0.0f, 1.0f);
			full = true;
			break;
		default:
			return;
		}

		if (direction == Direction.NEGATIVE) {
			Matrix.rotateM(mvpMatrix, 0, arrowRotationInDegrees, 0.0f, 0.0f,
					1.0f);
			Matrix.rotateM(mvpMatrix, 0, -90f, 0.0f, 0.0f, 1.0f);
			Matrix.rotateM(mvpMatrix, 0, +180f, 0.0f, 1.0f, 0.0f);
		} else
			Matrix.rotateM(mvpMatrix, 0, -1 * arrowRotationInDegrees, 0.0f,
					0.0f, 1.0f);

		if (full)
			Matrix.scaleM(mvpMatrix, 0, 3.0f, 3.0f, 3.0f);
		else if (wide)
			Matrix.scaleM(mvpMatrix, 0, 2.0f, 2.0f, 2.0f);
		else
			Matrix.scaleM(mvpMatrix, 0, 1.5f, 1.5f, 1.5f);

		if (rotation == Rotation.CLOCKWISE
				|| rotation == Rotation.COUNTER_CLOCKWISE)
			arrowQuarterTurn.draw(mvpMatrix, color, programID);
		else
			arrowHalfTurn.draw(mvpMatrix, color, programID);
	}

	private void renderCubeFullRotationArrow(final float[] mvpMatrix,
			int arrowRotationInDegrees) {
		if (stateModel.getNumObservedFaces() % 2 == 0) {
			Matrix.translateM(mvpMatrix, 0, 0.0f, +1.5f, +1.5f);
			Matrix.rotateM(mvpMatrix, 0, -90f, 0.0f, 1.0f, 0.0f);
		} else
			Matrix.translateM(mvpMatrix, 0, +1.5f, +1.5f, 0.0f);

		Matrix.rotateM(mvpMatrix, 0, arrowRotationInDegrees - 60, 0.0f, 0.0f,
				1.0f);
		Matrix.rotateM(mvpMatrix, 0, -90f, 0.0f, 0.0f, 1.0f);
		Matrix.rotateM(mvpMatrix, 0, +180f, 0.0f, 1.0f, 0.0f);

		Matrix.scaleM(mvpMatrix, 0, 1.0f, 1.0f, 3.0f);

		arrowQuarterTurn
				.draw(mvpMatrix, ColorTileEnum.WHITE.cvColor, programID);
	}

}
