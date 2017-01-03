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

import java.util.LinkedList;
import java.util.List;

import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.core.Core;
import org.opencv.core.CvException;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import android.annotation.SuppressLint;

import com.issac.cube.ar.Constants.ColorTileEnum;
import com.issac.cube.ar.Constants.ImageProcessModeEnum;

public class ImageRecognizer implements CvCameraViewListener2 {

	private final StateController stateController;
	private final StateModel stateModel;
	private final Overlay overlay;
	public Mat errorImage = null;

	public ImageRecognizer(StateController stateController,
			StateModel stateModel) {
		this.stateController = stateController;
		this.stateModel = stateModel;
		overlay = new Overlay(this.stateModel);
	}

	@Override
	public void onCameraViewStarted(int width, int height) {
	}

	@Override
	public void onCameraViewStopped() {
	}

	@Override
	public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
		Mat image = onFrame(inputFrame);
		if (MenuParam.stereoscopicView) {
			Size imageSize = image.size();
			Mat newImage = new Mat();
			Imgproc.resize(image, newImage, new Size(0, 0), 0.5, 0.5,
					Imgproc.INTER_LINEAR);
			Mat m = new Mat(imageSize, image.type());
			newImage.copyTo(m.submat(newImage.rows() / 2,
					newImage.rows() * 3 / 2, 0, newImage.cols()));
			newImage.copyTo(m.submat(newImage.rows() / 2,
					newImage.rows() * 3 / 2, newImage.cols(),
					newImage.cols() * 2));
			return m;
		} else
			return image;
	}

	@SuppressLint("DefaultLocale")
	private Mat onFrame(CvCameraViewFrame inputFrame) {
		if (errorImage != null)
			return errorImage;

		Mat image = inputFrame.rgba();
		Size imageSize = image.size();

		try {
			Face face = new Face();

			if (MenuParam.imageProcessMode == ImageProcessModeEnum.DIRECT) {
				stateModel.activeFace = face;
				return overlay.drawOverlay(image);
			}

			Mat greyscale_image = new Mat();
			Imgproc.cvtColor(image, greyscale_image, Imgproc.COLOR_BGR2GRAY);
			if (MenuParam.imageProcessMode == ImageProcessModeEnum.GREYSCALE) {
				stateModel.activeFace = face;
				image.release();
				return overlay.drawOverlay(greyscale_image);
			}

			Mat blur_image = new Mat();
			int kernelSize = (int) MenuParam.gaussianBlurKernelSizeParam.value;
			kernelSize = kernelSize % 2 == 0 ? kernelSize + 1 : kernelSize;
			Imgproc.GaussianBlur(greyscale_image, blur_image, new Size(
					kernelSize, kernelSize), -1, -1);
			greyscale_image.release();
			if (MenuParam.imageProcessMode == ImageProcessModeEnum.GAUSSIAN) {
				stateModel.activeFace = face;
				image.release();
				return overlay.drawOverlay(blur_image);
			}

			Mat canny_image = new Mat();
			Imgproc.Canny(blur_image, canny_image,
					MenuParam.cannyLowerThresholdParam.value,
					MenuParam.cannyUpperThresholdParam.value, 3, false);
			blur_image.release();
			if (MenuParam.imageProcessMode == ImageProcessModeEnum.CANNY) {
				stateModel.activeFace = face;
				image.release();
				return overlay.drawOverlay(canny_image);
			}

			Mat dilate_image = new Mat();
			Imgproc.dilate(canny_image, dilate_image, Imgproc
					.getStructuringElement(Imgproc.MORPH_RECT, new Size(
							MenuParam.dilationKernelSizeParam.value,
							MenuParam.dilationKernelSizeParam.value)));
			canny_image.release();
			if (MenuParam.imageProcessMode == ImageProcessModeEnum.DILATION) {
				stateModel.activeFace = face;
				image.release();
				return overlay.drawOverlay(dilate_image);
			}

			List<MatOfPoint> contours = new LinkedList<MatOfPoint>();
			Mat heirarchy = new Mat();
			Imgproc.findContours(dilate_image, contours, heirarchy,
					Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);
			dilate_image.release();
			heirarchy.release();
			if (MenuParam.imageProcessMode == ImageProcessModeEnum.CONTOUR) {
				stateModel.activeFace = face;
				Mat gray_image = new Mat(imageSize, CvType.CV_8UC4);
				Mat rgba_gray_image = new Mat(imageSize, CvType.CV_8UC4);
				Imgproc.cvtColor(image, gray_image, Imgproc.COLOR_RGB2GRAY);
				Imgproc.cvtColor(gray_image, rgba_gray_image,
						Imgproc.COLOR_GRAY2BGRA, 3);
				Imgproc.drawContours(rgba_gray_image, contours, -1,
						ColorTileEnum.YELLOW.cvColor, 3);
				Core.putText(rgba_gray_image,
						"Num Contours: " + contours.size(), new Point(500, 50),
						Constants.FontFace, 4, ColorTileEnum.RED.cvColor, 4);
				gray_image.release();
				image.release();
				return overlay.drawOverlay(rgba_gray_image);
			}

			List<Rhombus> polygonList = new LinkedList<Rhombus>();
			for (MatOfPoint contour : contours) {
				double contourArea = Imgproc.contourArea(contour, true);
				if (contourArea < 0.0)
					continue;

				if (contourArea < MenuParam.minimumContourAreaParam.value)
					continue;

				MatOfPoint2f contour2f = new MatOfPoint2f();
				MatOfPoint2f polygone2f = new MatOfPoint2f();
				MatOfPoint polygon = new MatOfPoint();

				contour.convertTo(contour2f, CvType.CV_32FC2);
				Imgproc.approxPolyDP(contour2f, polygone2f,
						MenuParam.polygonEpsilonParam.value, true);
				polygone2f.convertTo(polygon, CvType.CV_32S);

				polygonList.add(new Rhombus(polygon));
			}
			if (MenuParam.imageProcessMode == ImageProcessModeEnum.POLYGON) {
				stateModel.activeFace = face;
				Mat gray_image = new Mat(imageSize, CvType.CV_8UC4);
				Mat rgba_gray_image = new Mat(imageSize, CvType.CV_8UC4);
				Imgproc.cvtColor(image, gray_image, Imgproc.COLOR_RGB2GRAY);
				Imgproc.cvtColor(gray_image, rgba_gray_image,
						Imgproc.COLOR_GRAY2BGRA, 4);
				for (Rhombus polygon : polygonList)
					polygon.draw(rgba_gray_image, ColorTileEnum.YELLOW.cvColor);
				Core.putText(rgba_gray_image,
						"Num Polygons: " + polygonList.size(), new Point(500,
								50), Constants.FontFace, 3,
						ColorTileEnum.RED.cvColor, 4);
				gray_image.release();
				image.release();
				return overlay.drawOverlay(rgba_gray_image);
			}

			List<Rhombus> rhombusList = new LinkedList<Rhombus>();
			for (Rhombus rhombus : polygonList)
				if (rhombus.qualify())
					rhombusList.add(rhombus);
			Rhombus.removedOutlierRhombi(rhombusList);
			if (MenuParam.imageProcessMode == ImageProcessModeEnum.RHOMBUS) {
				stateModel.activeFace = face;
				Mat gray_image = new Mat(imageSize, CvType.CV_8UC4);
				Mat rgba_gray_image = new Mat(imageSize, CvType.CV_8UC4);
				Imgproc.cvtColor(image, gray_image, Imgproc.COLOR_RGB2GRAY);
				Imgproc.cvtColor(gray_image, rgba_gray_image,
						Imgproc.COLOR_GRAY2BGRA, 4);
				for (Rhombus rhombus : rhombusList)
					rhombus.draw(rgba_gray_image, ColorTileEnum.YELLOW.cvColor);
				Core.putText(rgba_gray_image,
						"Num Rhombus: " + rhombusList.size(),
						new Point(500, 50), Constants.FontFace, 4,
						ColorTileEnum.RED.cvColor, 4);
				gray_image.release();
				image.release();
				return overlay.drawOverlay(rgba_gray_image);
			}

			face.processRhombuses(rhombusList, image);
			if (MenuParam.imageProcessMode == ImageProcessModeEnum.FACE_DETECT) {
				stateModel.activeFace = face;
				return overlay.drawOverlay(image);
			}

			if (face.solved) {
				CubePoseEstimator cubePoseEstimator = new CubePoseEstimator();
				cubePoseEstimator.estimate(face, image, stateModel);
				stateModel.cubePoseEstimator = cubePoseEstimator;
			} else
				stateModel.cubePoseEstimator = null;

			stateController.onFaceEvent(face);

			stateModel.activeFace = face;
		} catch (CvException e) {
			e.printStackTrace();
			errorImage = new Mat(imageSize, CvType.CV_8UC4);
			Core.putText(errorImage, "CvException: " + e.getMessage(),
					new Point(50, 50), Constants.FontFace, 2,
					ColorTileEnum.WHITE.cvColor, 2);
			int i = 1;
			for (StackTraceElement element : e.getStackTrace())
				Core.putText(errorImage, element.toString(), new Point(50,
						50 + 50 * i++), Constants.FontFace, 2,
						ColorTileEnum.WHITE.cvColor, 2);
		} catch (Exception e) {
			e.printStackTrace();
			errorImage = new Mat(imageSize, CvType.CV_8UC4);
			Core.putText(errorImage, "Exception: " + e.getMessage(), new Point(
					50, 50), Constants.FontFace, 2,
					ColorTileEnum.WHITE.cvColor, 2);
			int i = 1;
			for (StackTraceElement element : e.getStackTrace())
				Core.putText(errorImage, element.toString(), new Point(50,
						50 + 50 * i++), Constants.FontFace, 2,
						ColorTileEnum.WHITE.cvColor, 2);
		} catch (Error e) {
			e.printStackTrace();
			errorImage = new Mat(imageSize, CvType.CV_8UC4);
			Core.putText(errorImage, "Error: " + e.getMessage(), new Point(50,
					50), Constants.FontFace, 2, ColorTileEnum.WHITE.cvColor, 2);
			int i = 1;
			for (StackTraceElement element : e.getStackTrace())
				Core.putText(errorImage, element.toString(), new Point(50,
						50 + 50 * i++), Constants.FontFace, 2,
						ColorTileEnum.WHITE.cvColor, 2);
		}

		return overlay.drawOverlay(image);
	}

}
