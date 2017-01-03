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

import org.opencv.core.Scalar;

import android.opengl.GLES20;

public class GLArrow {

	public enum Amount {
		QUARTER_TURN, HALF_TURN
	};

	private final FloatBuffer vertexBufferArrow;
	private final FloatBuffer vertexBufferOutline;
	private static final int COORDS_PER_VERTEX = 3;
	private static final int BYTES_PER_FLOAT = 4;
	private static final int VERTEX_STRIDE = COORDS_PER_VERTEX
			* BYTES_PER_FLOAT;
	private static final int VERTICES_PER_ARCH = 90 + 1;
	private static final float[] opaqueBlack = { 0.0f, 0.0f, 0.0f, 1.0f };

	public GLArrow(Amount amount) {
		double angleScale = amount == Amount.QUARTER_TURN ? 1.0 : 3.0;

		float[] verticesArrow = new float[VERTICES_PER_ARCH * 6];
		float[] verticesOutline = new float[VERTICES_PER_ARCH * 6];

		for (int i = 0; i < VERTICES_PER_ARCH; i++) {
			double angleRads = i * angleScale * Math.PI / 180.0;
			float x = (float) Math.cos(angleRads);
			float y = (float) Math.sin(angleRads);

			verticesArrow[i * 6 + 0] = x;
			verticesArrow[i * 6 + 1] = y;
			verticesArrow[i * 6 + 2] = -1.0f * calculateWidth(angleRads);

			verticesArrow[i * 6 + 3] = x;
			verticesArrow[i * 6 + 4] = y;
			verticesArrow[i * 6 + 5] = +1.0f * calculateWidth(angleRads);

			verticesOutline[i * 3 + 0] = x;
			verticesOutline[i * 3 + 1] = y;
			verticesOutline[i * 3 + 2] = -1.0f * calculateWidth(angleRads);

			verticesOutline[(180 - i) * 3 + 3] = x;
			verticesOutline[(180 - i) * 3 + 4] = y;
			verticesOutline[(180 - i) * 3 + 5] = calculateWidth(angleRads);
		}

		ByteBuffer arrowVbb = ByteBuffer
				.allocateDirect(verticesArrow.length * 4);
		arrowVbb.order(ByteOrder.nativeOrder());
		vertexBufferArrow = arrowVbb.asFloatBuffer();
		vertexBufferArrow.put(verticesArrow);
		vertexBufferArrow.position(0);

		ByteBuffer outlineVbb = ByteBuffer
				.allocateDirect(verticesOutline.length * 4);
		outlineVbb.order(ByteOrder.nativeOrder());
		vertexBufferOutline = outlineVbb.asFloatBuffer();
		vertexBufferOutline.put(verticesOutline);
		vertexBufferOutline.position(0);
	}

	private float calculateWidth(double angleRads) {
		double angleDegrees = angleRads * 180.0 / Math.PI;

		if (angleDegrees > 20.0)
			return 0.3f;
		else
			return (float) (angleDegrees / 20.0 * 0.6);
	}

	public void draw(float[] mvpMatrix, Scalar color, int programID) {
		GLES20.glUseProgram(programID);

		int vertexArrayID = GLES20.glGetAttribLocation(programID, "vPosition");

		GLES20.glEnableVertexAttribArray(vertexArrayID);

		int colorID = GLES20.glGetUniformLocation(programID, "vColor");

		int mvpMatrixID = GLES20.glGetUniformLocation(programID, "uMVPMatrix");
		GLUtil.checkGlError("glGetUniformLocation");

		GLES20.glUniformMatrix4fv(mvpMatrixID, 1, false, mvpMatrix, 0);
		GLUtil.checkGlError("glUniformMatrix4fv");

		drawArrowOuterSurface(color, colorID, vertexArrayID);

		drawArrowInnerSurface(color, colorID, vertexArrayID);

		drawArrowOutline(colorID, vertexArrayID);

		GLES20.glDisableVertexAttribArray(vertexArrayID);

		GLES20.glDisable(GLES20.GL_CULL_FACE);
	}

	private void drawArrowOutline(int colorID, int vertexArrayID) {
		GLES20.glDisable(GLES20.GL_CULL_FACE);

		GLES20.glVertexAttribPointer(vertexArrayID, COORDS_PER_VERTEX,
				GLES20.GL_FLOAT, false, VERTEX_STRIDE, vertexBufferOutline);

		GLES20.glUniform4fv(colorID, 1, opaqueBlack, 0);

		GLES20.glLineWidth(10.0f);

		GLES20.glDrawArrays(GLES20.GL_LINE_STRIP, 0, VERTICES_PER_ARCH * 2);
	}

	private void drawArrowOuterSurface(Scalar color, int colorID,
			int vertexArrayID) {
		GLES20.glEnable(GLES20.GL_CULL_FACE);

		GLES20.glVertexAttribPointer(vertexArrayID, COORDS_PER_VERTEX,
				GLES20.GL_FLOAT, false, VERTEX_STRIDE, vertexBufferArrow);

		float[] glFrontSideColor = { (float) color.val[0] / 256.0f,
				(float) color.val[1] / 256.0f, (float) color.val[2] / 256.0f,
				1.0f };
		GLES20.glUniform4fv(colorID, 1, glFrontSideColor, 0);

		GLES20.glCullFace(GLES20.GL_FRONT);

		GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, VERTICES_PER_ARCH * 2);
	}

	private void drawArrowInnerSurface(Scalar color, int colorID,
			int vertexArrayID) {

		GLES20.glEnable(GLES20.GL_CULL_FACE);

		GLES20.glVertexAttribPointer(vertexArrayID, COORDS_PER_VERTEX,
				GLES20.GL_FLOAT, false, VERTEX_STRIDE, vertexBufferArrow);

		float[] glBackSideColor = { (float) color.val[0] / (256.0f + 256.0f),
				(float) color.val[1] / (256.0f + 256.0f),
				(float) color.val[2] / (256.0f + 256.0f), 1.0f };
		GLES20.glUniform4fv(colorID, 1, glBackSideColor, 0);

		GLES20.glCullFace(GLES20.GL_BACK);

		GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, VERTICES_PER_ARCH * 2);
	}
}
