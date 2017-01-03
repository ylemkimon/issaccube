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

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.media.MediaPlayer;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.issac.cube.R;
import com.issac.cube.ar.Constants.AppStateEnum;
import com.issac.cube.gl.GLRenderer;

public class ARActivity extends Activity {

	public static final int SCRIPT_EXEC_PY = 40001;

	private CameraBridgeViewBase mOpenCvCameraView;
	private GLSurfaceView gLSurfaceView;
	public ImageRecognizer imageRecognizer;
	public StateController stateController;
	public StateModel stateModel;
	public MediaPlayer mediaPlayer;

	private final BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(
			this) {
		@Override
		public void onManagerConnected(int status) {
			switch (status) {
			case LoaderCallbackInterface.SUCCESS:
				mOpenCvCameraView.enableView();
				break;
			default:
				super.onManagerConnected(status);
				break;
			}
		}
	};

	public ARActivity() {
		stateModel = new StateModel();
		stateController = new StateController(stateModel, this);
		imageRecognizer = new ImageRecognizer(stateController, stateModel);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		stateModel.cameraParameters = new CameraParameters();

		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		setContentView(R.layout.surface_view);

		FrameLayout frameLayout = (FrameLayout) findViewById(R.id.activity_frame_layout);

		mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.activity_surface_view);
		mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
		mOpenCvCameraView.setCvCameraViewListener(imageRecognizer);

		gLSurfaceView = new GLSurfaceView(this);
		gLSurfaceView.setEGLContextClientVersion(2);
		gLSurfaceView.getHolder().setFormat(PixelFormat.TRANSPARENT);
		gLSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
		gLSurfaceView.setZOrderOnTop(true);
		gLSurfaceView.setLayoutParams(new FrameLayout.LayoutParams(
				FrameLayout.LayoutParams.MATCH_PARENT,
				FrameLayout.LayoutParams.MATCH_PARENT));
		frameLayout.addView(gLSurfaceView);
		GLRenderer gLRenderer = new GLRenderer(stateModel, this);
		gLSurfaceView.setRenderer(gLRenderer);

		mediaPlayer = MediaPlayer.create(this, R.raw.dingdong);

		MenuParam.gaussianBlurKernelSizeParam
				.setLabel(getString(R.string.gaussian_blur_kernel_size));
		MenuParam.cannyUpperThresholdParam
				.setLabel(getString(R.string.canny_upper_threshold));
		MenuParam.cannyLowerThresholdParam
				.setLabel(getString(R.string.canny_lower_threshold));
		MenuParam.dilationKernelSizeParam
				.setLabel(getString(R.string.dilation_kernel_size));
		MenuParam.minimumContourAreaParam
				.setLabel(getString(R.string.minimum_contour_area_size));
		MenuParam.polygonEpsilonParam
				.setLabel(getString(R.string.polygon_epsilon_threshold));
		MenuParam.minimumRhombusAreaParam
				.setLabel(getString(R.string.minimum_parallelogram_area_size));
		MenuParam.maximumRhombusAreaParam
				.setLabel(getString(R.string.maximum_parallelogram_area_size));
		MenuParam.angleOutlierThresholdPaaram
				.setLabel(getString(R.string.angle_outlier_threshold));
		MenuParam.faceLmsThresholdParam
				.setLabel(getString(R.string.face_lms_threshold));
		MenuParam.threeDDepthParam.setLabel(getString(R.string.three_d_depth));
	}

	@TargetApi(Build.VERSION_CODES.KITKAT)
	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		if (hasFocus && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
			getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
					| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
					| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
					| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
					| View.SYSTEM_UI_FLAG_FULLSCREEN
					| View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
	}
	
	@Override
	public void startActivityForResult(Intent intent, int requestCode) {
		startService(new Intent(this, BlackScreenService.class));
		super.startActivityForResult(intent, requestCode);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == SCRIPT_EXEC_PY)
			if (data != null) {
				String result = data.getExtras().getString("result");
				if (result.contains("Invalid Cube"))
					stateModel.appState = AppStateEnum.BAD_COLORS;
				else if (result.contains("Error"))
					stateModel.appState = AppStateEnum.ERROR;
				else {
					stateModel.solutionResults = result.split("\\*")[1];
					stateModel.solutionResultsStage = stateModel.solutionResults
							.replace("/", "").length()
							- stateModel.solutionResults.length();
					stateModel.solutionResultsArray = stateModel.solutionResults
							.split(" ");
					stateModel.solutionResultIndex = 0;
					stateModel.appState = AppStateEnum.ROTATE_FACE;
				}
			}
		stopService(new Intent(this, BlackScreenService.class));
	}

	@Override
	public void onPause() {
		super.onPause();
		if (mOpenCvCameraView != null)
			mOpenCvCameraView.disableView();
		if (gLSurfaceView != null)
			gLSurfaceView.onPause();
	}

	@Override
	public void onResume() {
		super.onResume();
		if (gLSurfaceView != null)
			gLSurfaceView.onResume();
		OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_11, this,
				mLoaderCallback);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (mOpenCvCameraView != null)
			mOpenCvCameraView.disableView();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		return MenuParam.onOptionsItemSelected(item, this);
	}
}
