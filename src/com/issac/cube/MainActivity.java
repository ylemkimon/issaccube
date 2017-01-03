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
package com.issac.cube;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.issac.cube.ar.ARActivity;

public class MainActivity extends Activity implements OnClickListener {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		Button[] button = new Button[3];
		button[0] = (Button) findViewById(R.id.button1);
		button[1] = (Button) findViewById(R.id.button2);
		button[2] = (Button) findViewById(R.id.button3);

		int color[] = { 0xFF006AC1, 0xFFF3B200, 0xFFC1004F };

		for (int i = 0; i < 3; i++) {
			button[i].setBackgroundColor(color[i]);
			button[i].setTextColor(Color.WHITE);
			button[i].setOnClickListener(this);
		}
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.button1:
			startActivity(new Intent(this, ARActivity.class));
			break;
		case R.id.button2:
			startActivity(new Intent().setComponent(new ComponentName(
					"com.pluscubed.plustimer",
					"com.pluscubed.plustimer.ui.CurrentSessionActivity")));
			break;
		case R.id.button3:
			break;
		}

	}

}
