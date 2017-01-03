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

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

public class Rhombus {

	private final MatOfPoint polygonMatrix;
	private final List<Point> polygonPointList;
	private final Point[] polygonePointArray;
	Point center = new Point();
	double area;
	double alphaAngle;
	double betaAngle;
	double alphaLength;
	double betaLength;
	double gammaRatio;

	public Rhombus(MatOfPoint polygon) {
		polygonMatrix = polygon;
		polygonPointList = polygon.toList();
		polygonePointArray = polygon.toArray();
	}

	public boolean qualify() {
		double x = 0;
		double y = 0;
		for (Point point : polygonPointList) {
			x += point.x;
			y += point.y;
		}
		center.x = x / polygonPointList.size();
		center.y = y / polygonPointList.size();

		if (polygonPointList.size() != 4)
			return false;

		if (!Imgproc.isContourConvex(polygonMatrix))
			return false;

		area = areaOfConvexQuadrilateral(polygonePointArray);
		if (area < MenuParam.minimumRhombusAreaParam.value
				|| area > MenuParam.maximumRhombusAreaParam.value)
			return false;

		if (adjustQuadrilaterVertices())
			return false;

		alphaAngle = 180.0
				/ Math.PI
				* Math.atan2(
						polygonePointArray[1].y
								- polygonePointArray[0].y
								+ (polygonePointArray[2].y - polygonePointArray[3].y),
						polygonePointArray[1].x
								- polygonePointArray[0].x
								+ (polygonePointArray[2].x - polygonePointArray[3].x));
		betaAngle = 180.0
				/ Math.PI
				* Math.atan2(
						polygonePointArray[2].y
								- polygonePointArray[1].y
								+ (polygonePointArray[3].y - polygonePointArray[0].y),
						polygonePointArray[2].x
								- polygonePointArray[1].x
								+ (polygonePointArray[3].x - polygonePointArray[0].x));

		alphaLength = (line(polygonePointArray[0], polygonePointArray[1]) + line(
				polygonePointArray[3], polygonePointArray[2])) / 2;
		betaLength = (line(polygonePointArray[0], polygonePointArray[3]) + line(
				polygonePointArray[1], polygonePointArray[2])) / 2;

		gammaRatio = betaLength / alphaLength;

		return true;
	}

	private boolean adjustQuadrilaterVertices() {
		double y_min = Double.MAX_VALUE;
		int index = 0;
		for (int i = 0; i < polygonePointArray.length; i++)
			if (polygonePointArray[i].y < y_min) {
				y_min = polygonePointArray[i].y;
				index = i;
			}

		for (int i = 0; i < index; i++) {
			Point tmp = polygonePointArray[0];
			polygonePointArray[0] = polygonePointArray[1];
			polygonePointArray[1] = polygonePointArray[2];
			polygonePointArray[2] = polygonePointArray[3];
			polygonePointArray[3] = tmp;
		}

		if (polygonePointArray[1].x < polygonePointArray[3].x)
			return true;
		else
			return false;
	}

	private static double areaOfConvexQuadrilateral(
			Point[] quadrilateralPointArray) {
		double area = area(
				line(quadrilateralPointArray[0], quadrilateralPointArray[1]),
				line(quadrilateralPointArray[1], quadrilateralPointArray[2]),
				line(quadrilateralPointArray[2], quadrilateralPointArray[0]))
				+ area(line(quadrilateralPointArray[0],
						quadrilateralPointArray[3]),
						line(quadrilateralPointArray[3],
								quadrilateralPointArray[2]),
						line(quadrilateralPointArray[2],
								quadrilateralPointArray[0]));
		return area;
	}

	private static double area(double a, double b, double c) {
		double area = Math.sqrt((a + b - c) * (a - b + c) * (-a + b + c)
				* (a + b + c)) / 4.0;
		return area;
	}

	private static double line(Point a, Point b) {
		double length = Math.sqrt((a.x - b.x) * (a.x - b.x) + (a.y - b.y)
				* (a.y - b.y));
		return length;
	}

	public void draw(Mat rgba_gray_image, Scalar color) {
		final LinkedList<MatOfPoint> listOfPolygons = new LinkedList<MatOfPoint>();
		listOfPolygons.add(polygonMatrix);
		Core.polylines(rgba_gray_image, listOfPolygons, true, color, 3);
	}

	public static void removedOutlierRhombi(List<Rhombus> rhombusList) {
		final double angleOutlierTolerance = MenuParam.angleOutlierThresholdPaaram.value;

		if (rhombusList.size() < 3)
			return;

		int midIndex = rhombusList.size() / 2;

		Collections.sort(rhombusList, new Comparator<Rhombus>() {
			@Override
			public int compare(Rhombus lhs, Rhombus rhs) {
				return (int) (lhs.alphaAngle - rhs.alphaAngle);
			}
		});
		double medianAlphaAngle = rhombusList.get(midIndex).alphaAngle;

		Collections.sort(rhombusList, new Comparator<Rhombus>() {
			@Override
			public int compare(Rhombus lhs, Rhombus rhs) {
				return (int) (lhs.betaAngle - rhs.betaAngle);
			}
		});
		double medianBetaAngle = rhombusList.get(midIndex).betaAngle;

		Iterator<Rhombus> rhombusItr = rhombusList.iterator();
		while (rhombusItr.hasNext()) {
			Rhombus rhombus = rhombusItr.next();
			if (Math.abs(rhombus.alphaAngle - medianAlphaAngle) > angleOutlierTolerance
					|| Math.abs(rhombus.betaAngle - medianBetaAngle) > angleOutlierTolerance)
				rhombusItr.remove();
		}
	}

}
