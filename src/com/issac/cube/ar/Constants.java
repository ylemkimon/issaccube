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
import org.opencv.core.Scalar;

public class Constants {

	public enum AppStateEnum {
		START, GOT_IT, ROTATE_CUBE, SEARCHING, COMPLETE, BAD_COLORS, WAIT_SOLVING, ERROR, ROTATE_FACE, WAITING_MOVE, DONE
	};

	public enum GestureRecogniztionStateEnum {
		UNKNOWN, PENDING, STABLE, NEW_STABLE, PARTIAL
	};

	public enum ImageProcessModeEnum {
		DIRECT, GREYSCALE, GAUSSIAN, CANNY, DILATION, CONTOUR, POLYGON, RHOMBUS, FACE_DETECT, NORMAL
	}

	public enum AnnotationModeEnum {
		LAYOUT, FACE_METRICS, COLOR_FACE, COLOR_CUBE, NORMAL
	}

	public enum FaceNameEnum {
		UP, DOWN, LEFT, RIGHT, FRONT, BACK
	};

	public enum ColorTileEnum {
		RED(true, 'R', new Scalar(220.0, 20.0, 30.0), new float[] { 1.0f, 0.0f, 0.0f, 1.0f }),
		ORANGE(true, 'O', new Scalar(240.0, 80.0, 0.0), new float[] { 0.9f, 0.4f, 0.0f, 1.0f }),
		YELLOW(true, 'Y', new Scalar(230.0, 230.0, 20.0), new float[] { 0.9f, 0.9f, 0.2f, 1.0f }),
		GREEN(true, 'G', new Scalar(0.0, 140.0, 60.0), new float[] { 0.0f, 1.0f, 0.0f, 1.0f }),
		BLUE(true, 'B', new Scalar(0.0, 60.0, 220.0), new float[] { 0.2f, 0.2f, 1.0f, 1.0f }),
		WHITE(true, 'W', new Scalar(225.0, 225.0, 225.0), new float[] { 1.0f, 1.0f, 1.0f, 1.0f }),

		BLACK(false, 'K', new Scalar(0.0, 0.0, 0.0)),
		GREY(false, 'E', new Scalar(50.0, 50.0, 50.0));

		public final boolean isTileColor;
		public final Scalar tileColor;
		public final Scalar cvColor;
		public final float[] glColor;
		public final char symbol;

		private ColorTileEnum(boolean isTileColor, char symbol, Scalar color) {
			this.isTileColor = isTileColor;
			cvColor = color;
			tileColor = color;
			glColor = new float[] { (float) color.val[0] / 255f,
					(float) color.val[1] / 255f,
					(float) color.val[2] / 255f, 1.0f };
			this.symbol = symbol;
		}

		private ColorTileEnum(boolean isTileColor, char symbol, Scalar color,
				float[] renderColor) {
			this.isTileColor = isTileColor;
			cvColor = color;
			tileColor = color;
			glColor = renderColor;
			this.symbol = symbol;
		}

	}

	public final static int FontFace = Core.FONT_HERSHEY_PLAIN;
}
