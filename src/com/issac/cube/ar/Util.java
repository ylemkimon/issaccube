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

import com.issac.cube.ar.Constants.ColorTileEnum;

public class Util {

	public static double[] getYUVfromRGB(double[] rgb) {
		if (rgb == null)
			return new double[] { 0, 0, 0, 0 };
		double[] yuv = new double[4];
		yuv[0] = 0.229 * rgb[0] + 0.587 * rgb[1] + 0.114 * rgb[2];
		yuv[1] = -0.147 * rgb[0] + -0.289 * rgb[1] + 0.436 * rgb[2];
		yuv[2] = 0.615 * rgb[0] + -0.515 * rgb[1] + -0.100 * rgb[2];
		return yuv;
	}

	public static ColorTileEnum[][] rotateArrayClockwise(ColorTileEnum[][] arg) {
		ColorTileEnum[][] result = new ColorTileEnum[3][3];
		result[1][1] = arg[1][1];
		result[2][0] = arg[0][0];
		result[2][1] = arg[1][0];
		result[2][2] = arg[2][0];
		result[1][2] = arg[2][1];
		result[0][2] = arg[2][2];
		result[0][1] = arg[1][2];
		result[0][0] = arg[0][2];
		result[1][0] = arg[0][1];
		return result;
	}

	public static ColorTileEnum[][] rotateArrayCounterclockwise(
			ColorTileEnum[][] arg) {
		return rotateArrayClockwise(rotateArrayClockwise(rotateArrayClockwise(arg)));
	}

	public static ColorTileEnum[][] rotateArray180(ColorTileEnum[][] arg) {
		return rotateArrayClockwise(rotateArrayClockwise(arg));
	}

}
