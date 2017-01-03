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

import android.opengl.GLES20;

public class GLUtil {

	public static void checkGlError(String glOperation) {
		int error;

		while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR)
			throw new RuntimeException(glOperation + ": glError " + error);
	}

}
