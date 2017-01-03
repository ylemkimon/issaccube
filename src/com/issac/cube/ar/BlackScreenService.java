/**
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

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;

public class BlackScreenService extends Service {
	
	private WindowManager windowManager;
	private View black;

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@SuppressLint("InlinedApi")
	@Override
	public void onCreate() {
		windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
	    black = new View(this);
	    black.setBackgroundColor(Color.BLACK);
	    final LayoutParams params = new LayoutParams(
	        LayoutParams.MATCH_PARENT,
	        LayoutParams.MATCH_PARENT,
	        LayoutParams.TYPE_SYSTEM_ERROR,
	        LayoutParams.FLAG_NOT_FOCUSABLE
	        /*| LayoutParams.FLAG_FULLSCREEN
	        | LayoutParams.FLAG_DIM_BEHIND
	        | LayoutParams.FLAG_TRANSLUCENT_STATUS*/
	        | LayoutParams.FLAG_LAYOUT_IN_SCREEN,
	        PixelFormat.TRANSLUCENT);
	    windowManager.addView(black, params);
	}
	
	@Override
	public void onDestroy() {
		if (black != null)
			windowManager.removeView(black);
	}

}
