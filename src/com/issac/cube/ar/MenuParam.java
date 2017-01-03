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

import android.app.AlertDialog;
import android.content.Context;
import android.view.MenuItem;
import android.view.View;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.issac.cube.R;
import com.issac.cube.ar.Constants.AnnotationModeEnum;
import com.issac.cube.ar.Constants.ImageProcessModeEnum;

public class MenuParam {

	public static boolean cubeOverlayDisplay = false;
	public static boolean faceOverlayDisplay = true;
	public static boolean pilotCubeDisplay = true;
	public static boolean stereoscopicView = false;
	public static ImageProcessModeEnum imageProcessMode = ImageProcessModeEnum.NORMAL;
	public static AnnotationModeEnum annotationMode = AnnotationModeEnum.NORMAL;

	public static RubikMenuParam gaussianBlurKernelSizeParam = new RubikMenuParam(
			+3.0, +20.0, +7.0);
	public static RubikMenuParam cannyUpperThresholdParam = new RubikMenuParam(
			+50.0, +200.0, +100.0);
	public static RubikMenuParam cannyLowerThresholdParam = new RubikMenuParam(
			+20.0, +100.0, +50.0);
	public static RubikMenuParam dilationKernelSizeParam = new RubikMenuParam(
			+2.0, +20.0, +10.0);
	public static RubikMenuParam minimumContourAreaParam = new RubikMenuParam(
			+10.0, +500.0, +100.0);
	public static RubikMenuParam polygonEpsilonParam = new RubikMenuParam(
			+10.0, +100.0, +30.0);
	public static RubikMenuParam minimumRhombusAreaParam = new RubikMenuParam(
			+0.0, +2000.0, +1000.0);
	public static RubikMenuParam maximumRhombusAreaParam = new RubikMenuParam(
			+0.0, +20000.0, +10000.0);
	public static RubikMenuParam angleOutlierThresholdPaaram = new RubikMenuParam(
			+1.0, +20.0, +10.0);
	public static RubikMenuParam faceLmsThresholdParam = new RubikMenuParam(
			+5.0, +150.0, +35.0);
	public static RubikMenuParam threeDDepthParam = new RubikMenuParam(+0.0,
			+100.0, +10.0);

	public static class RubikMenuParam {
		public RubikMenuParam(double min, double max, double value) {
			this.min = min;
			this.max = max;
			this.value = value;
		}

		public void setLabel(String label) {
			this.label = label;
		}

		public double value;
		private final double min;
		private final double max;
		private String label;
	}

	public static boolean onOptionsItemSelected(MenuItem item, ARActivity ma) {
		switch (item.getItemId()) {
		case R.id.directImageProcessMenuItem:
			imageProcessMode = ImageProcessModeEnum.DIRECT;
			break;

		case R.id.greyscaleImageProcessMenuItem:
			imageProcessMode = ImageProcessModeEnum.GREYSCALE;
			break;

		case R.id.boxBlurImageProcessMenuItem:
			imageProcessMode = ImageProcessModeEnum.GAUSSIAN;
			break;

		case R.id.cannyImageProcessMenuItem:
			imageProcessMode = ImageProcessModeEnum.CANNY;
			break;

		case R.id.dialateImageProcessMenuItem:
			imageProcessMode = ImageProcessModeEnum.DILATION;
			break;

		case R.id.contourImageProcessMenuItem:
			imageProcessMode = ImageProcessModeEnum.CONTOUR;
			break;

		case R.id.ploygoneProcessMenuItem:
			imageProcessMode = ImageProcessModeEnum.POLYGON;
			break;

		case R.id.rhombusProcessMenuItem:
			imageProcessMode = ImageProcessModeEnum.RHOMBUS;
			break;

		case R.id.faceRecognitionMenuItem:
			imageProcessMode = ImageProcessModeEnum.FACE_DETECT;
			break;

		case R.id.normalProcessMenuItem:
			imageProcessMode = ImageProcessModeEnum.NORMAL;
			break;

		case R.id.normalAnnotationMenuItem:
			annotationMode = AnnotationModeEnum.NORMAL;
			break;

		case R.id.layoutAnnotationMenuItem:
			annotationMode = AnnotationModeEnum.LAYOUT;
			break;

		case R.id.faceMetricsAnnotationMenuItem:
			annotationMode = AnnotationModeEnum.FACE_METRICS;
			break;

		case R.id.faceColorAnnotationMenuItem:
			annotationMode = AnnotationModeEnum.COLOR_FACE;
			break;

		case R.id.cubeColorAnnotationMenuItem:
			annotationMode = AnnotationModeEnum.COLOR_CUBE;
			break;

		case R.id.boxBlurKernelSizeMenuItem:
			seekerDialog(gaussianBlurKernelSizeParam, ma);
			break;

		case R.id.cannyLowerThresholdMenuItem:
			seekerDialog(cannyLowerThresholdParam, ma);
			break;

		case R.id.cannyUpperThresholdMenuItem:
			seekerDialog(cannyUpperThresholdParam, ma);
			break;

		case R.id.dilationKernelMenuItem:
			seekerDialog(dilationKernelSizeParam, ma);
			break;

		case R.id.minimumContourAreaSizelMenuItem:
			seekerDialog(minimumContourAreaParam, ma);
			break;

		case R.id.polygonEpsilonMenuItem:
			seekerDialog(polygonEpsilonParam, ma);
			break;

		case R.id.minimumRhombusAreaSizelMenuItem:
			seekerDialog(minimumRhombusAreaParam, ma);
			break;

		case R.id.maximumRhombusAreaSizelMenuItem:
			seekerDialog(maximumRhombusAreaParam, ma);
			break;

		case R.id.angleOutlierThresholdMenuItem:
			seekerDialog(angleOutlierThresholdPaaram, ma);
			break;

		case R.id.faceLmsThresholdMenuItem:
			seekerDialog(faceLmsThresholdParam, ma);
			break;

		case R.id.threeDDepthMenuItem:
			seekerDialog(threeDDepthParam, ma);
			break;

		case R.id.resetMenuItem:
			ma.stateController.reset();
			break;

		case R.id.exitMenuItem:
			ma.finish();
			System.exit(0);
			break;

		case R.id.toggleStereoscopicViewMenuItem:
			stereoscopicView ^= true;
			break;

		case R.id.toggleFaceOverlayMenuItem:
			faceOverlayDisplay ^= true;
			break;

		case R.id.toggleCubeOverlayMenuItem:
			cubeOverlayDisplay ^= true;
			break;

		case R.id.togglePilotCubeMenuItem:
			pilotCubeDisplay ^= true;
			break;
		}

		return true;
	}

	private static void seekerDialog(final RubikMenuParam rubikMenuParam,
			Context context) {
		View promptsView = View.inflate(context, R.layout.prompts, null);

		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
				context);

		alertDialogBuilder.setView(promptsView);

		AlertDialog alertDialog = alertDialogBuilder.create();

		alertDialog.show();

		double value = rubikMenuParam.value;

		TextView paramTitleTextView = (TextView) promptsView
				.findViewById(R.id.param_title_text_view);
		paramTitleTextView.setText(rubikMenuParam.label);

		final TextView paramValueTextView = (TextView) promptsView
				.findViewById(R.id.param_value_text_view);
		paramValueTextView.setText(String.format("%5.1f", value));

		SeekBar seekBar = (SeekBar) promptsView
				.findViewById(R.id.parameter_seekbar);
		seekBar.setMax(100);

		seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				double newParamValue = (rubikMenuParam.max - rubikMenuParam.min)
						* progress / 100.0 + rubikMenuParam.min;
				paramValueTextView.setText(String
						.format("%5.1f", newParamValue));
				rubikMenuParam.value = newParamValue;
			}
		});
		seekBar.setProgress((int) (100.0 * (value - rubikMenuParam.min) / (rubikMenuParam.max - rubikMenuParam.min)));
	}
}
