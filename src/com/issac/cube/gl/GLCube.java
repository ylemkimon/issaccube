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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import android.opengl.GLES20;

import com.issac.cube.ar.Constants;
import com.issac.cube.ar.Constants.FaceNameEnum;
import com.issac.cube.ar.Face;
import com.issac.cube.ar.StateModel;

public class GLCube {
	public enum Transparency {
		OPAQUE, TRANSLUCENT, TRANSPARENT
	};

	private final FloatBuffer vertexBuffer;
	private final StateModel stateModel;
	private static final int NUM_FACES = 6;
	private static final int COORDS_PER_VERTEX = 3;
	private static final int BYTES_PER_FLOAT = 4;
	private static final int VERTEX_STRIDE = COORDS_PER_VERTEX
			* BYTES_PER_FLOAT;
	private static float[] transparentBlack = { 0f, 0f, 0f, 0f };
	private static float[] translusentGrey = { 0.5f, 0.5f, 0.5f, 0.5f };
	private static float[] vertices = { -1.0f, -1.0f, 1.0f, 1.0f, -1.0f, 1.0f,
			-1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, -1.0f, -1.0f, -1.0f,
			-1.0f, -1.0f, 1.0f, 1.0f, -1.0f, -1.0f, 1.0f, -1.0f, -1.0f, -1.0f,
			-1.0f, -1.0f, -1.0f, 1.0f, -1.0f, 1.0f, -1.0f, -1.0f, 1.0f, 1.0f,
			1.0f, -1.0f, 1.0f, 1.0f, -1.0f, -1.0f, 1.0f, 1.0f, 1.0f, 1.0f,
			1.0f, -1.0f, -1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, -1.0f, 1.0f,
			-1.0f, 1.0f, 1.0f, -1.0f, -1.0f, -1.0f, -1.0f, 1.0f, -1.0f, -1.0f,
			-1.0f, -1.0f, 1.0f, 1.0f, -1.0f, 1.0f };

	public GLCube(StateModel stateModel) {
		this.stateModel = stateModel;

		ByteBuffer vbb = ByteBuffer.allocateDirect(vertices.length * 4);
		vbb.order(ByteOrder.nativeOrder());
		vertexBuffer = vbb.asFloatBuffer();
		vertexBuffer.put(vertices);
		vertexBuffer.position(0);
	}

	public void draw(float[] mvpMatrix, Transparency transparencyMode,
			int programID) {
		GLES20.glEnable(GLES20.GL_CULL_FACE);
		GLES20.glCullFace(GLES20.GL_BACK);

		GLES20.glUseProgram(programID);

		int vertexArrayID = GLES20.glGetAttribLocation(programID, "vPosition");

		GLES20.glEnableVertexAttribArray(vertexArrayID);

		GLES20.glVertexAttribPointer(vertexArrayID, COORDS_PER_VERTEX,
				GLES20.GL_FLOAT, false, VERTEX_STRIDE, vertexBuffer);

		int colorID = GLES20.glGetUniformLocation(programID, "vColor");

		int mvpMatrixID = GLES20.glGetUniformLocation(programID, "uMVPMatrix");
		GLUtil.checkGlError("glGetUniformLocation");

		GLES20.glUniformMatrix4fv(mvpMatrixID, 1, false, mvpMatrix, 0);
		GLUtil.checkGlError("glUniformMatrix4fv");

		for (int faceIndex = 0; faceIndex < NUM_FACES; faceIndex++) {
			switch (transparencyMode) {

			case TRANSPARENT:
				GLES20.glUniform4fv(colorID, 1, transparentBlack, 0);
				break;

			case OPAQUE:
				Face face = null;
				switch (faceIndex) {
				case 0:
					face = stateModel.faceMap.get(FaceNameEnum.FRONT);
					break;
				case 1:
					face = stateModel.faceMap.get(FaceNameEnum.BACK);
					break;
				case 2:
					face = stateModel.faceMap.get(FaceNameEnum.LEFT);
					break;
				case 3:
					face = stateModel.faceMap.get(FaceNameEnum.RIGHT);
					break;
				case 4:
					face = stateModel.faceMap.get(FaceNameEnum.UP);
					break;
				case 5:
					face = stateModel.faceMap.get(FaceNameEnum.DOWN);
					break;
				}

				float[] colorGL = face != null
						&& face.transformedTileArray != null
						&& face.transformedTileArray[1][1] != null ? face.transformedTileArray[1][1].glColor
						: Constants.ColorTileEnum.GREY.glColor;

				GLES20.glUniform4fv(colorID, 1, colorGL, 0);
				break;

			case TRANSLUCENT:
				GLES20.glUniform4fv(colorID, 1, translusentGrey, 0);
				break;
			}

			GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, faceIndex
					* BYTES_PER_FLOAT, BYTES_PER_FLOAT);
		}

		GLES20.glDisableVertexAttribArray(vertexArrayID);

		GLES20.glDisable(GLES20.GL_CULL_FACE);
	}
}
