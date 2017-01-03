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

import java.util.ArrayList;
import java.util.List;

import org.opencv.calib3d.Calib3d;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfPoint3f;
import org.opencv.core.Point;
import org.opencv.core.Point3;

public class CubePoseEstimator {

	public float x;
	public float y;
	public float z;
	public float[] poseRotationMatrix = new float[16];

	public void estimate(Face face, Mat image, StateModel stateModel) {
		if (face == null || !face.solved || face.lmsResult == null)
			return;

		if (face.rhombusList.size() <= 4)
			return;

		List<Point3> objectPointsList = new ArrayList<Point3>(9);
		List<Point> imagePointsList = new ArrayList<Point>(9);

		for (int n = 0; n < 3; n++)
			for (int m = 0; m < 3; m++) {
				Rhombus rhombus = face.faceRhombusArray[n][m];
				if (rhombus != null) {
					Point imagePoint = new Point(rhombus.center.x,
							rhombus.center.y);
					imagePointsList.add(imagePoint);

					int mm = 2 - n;
					int nn = 2 - m;

					float x = (1 - mm) * 0.66666f;
					float y = -1.0f;
					float z = -1.0f * (1 - nn) * 0.666666f;
					Point3 objectPoint = new Point3(x, y, z);
					objectPointsList.add(objectPoint);
				}
			}

		MatOfPoint2f imagePoints = new MatOfPoint2f();
		imagePoints.fromList(imagePointsList);

		MatOfPoint3f objectPoints = new MatOfPoint3f();
		objectPoints.fromList(objectPointsList);

		Mat cameraMatrix = stateModel.cameraParameters.getCameraMatrix();
		MatOfDouble distCoeffs = new MatOfDouble();
		Mat rvec = new Mat();
		Mat tvec = new Mat();

		Calib3d.solvePnP(objectPoints, imagePoints, cameraMatrix, distCoeffs,
				rvec, tvec);

		x = +1.0f * (float) tvec.get(0, 0)[0];
		y = -1.0f * (float) tvec.get(1, 0)[0];
		z = -1.0f * (float) tvec.get(2, 0)[0];

		rvec.put(1, 0, -1.0f * rvec.get(1, 0)[0]);
		rvec.put(2, 0, -1.0f * rvec.get(2, 0)[0]);

		Mat rMatrix = new Mat(4, 4, CvType.CV_32FC2);
		Calib3d.Rodrigues(rvec, rMatrix);

		for (int i = 0; i < 16; i++)
			poseRotationMatrix[i] = 0.0f;

		poseRotationMatrix[3 * 4 + 3] = 1.0f;

		for (int r = 0; r < 3; r++)
			for (int c = 0; c < 3; c++)
				poseRotationMatrix[r + c * 4] = (float) rMatrix.get(r, c)[0];
	}

}
