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

import org.opencv.core.CvType;
import org.opencv.core.Mat;

import android.hardware.Camera;
import android.hardware.Camera.Parameters;

public class CameraParameters {

	private final Parameters parameters;
	private final int widthPixels;
	private final int heightPixels;
	private final float fovX;
	private final float fovY;

	public CameraParameters() {
		Camera camera = Camera.open();
		parameters = camera.getParameters();
		camera.release();

		widthPixels = 1280;
		heightPixels = 720;

		fovY = parameters.getVerticalViewAngle() * (float) (Math.PI / 180.0);
		fovX = parameters.getHorizontalViewAngle() * (float) (Math.PI / 180.0);
	}

	public Mat getCameraMatrix() {
		double focalLengthXPixels = widthPixels / (2.0 * Math.tan(0.5 * fovX));
		double focalLengthYPixels = heightPixels / (2.0 * Math.tan(0.5 * fovY));

		Mat cameraMatrix = new Mat(3, 3, CvType.CV_64FC1);
		cameraMatrix.put(0, 0, focalLengthXPixels);
		cameraMatrix.put(0, 1, 0.0);
		cameraMatrix.put(0, 2, widthPixels / 2.0);
		cameraMatrix.put(1, 0, 0.0);
		cameraMatrix.put(1, 1, focalLengthYPixels);
		cameraMatrix.put(1, 2, heightPixels / 2.0);
		cameraMatrix.put(2, 0, 0.0);
		cameraMatrix.put(2, 1, 0.0);
		cameraMatrix.put(2, 2, 1.0);

		return cameraMatrix;
	}

}
