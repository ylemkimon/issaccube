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

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;

import com.issac.cube.ar.Constants.ColorTileEnum;
import com.issac.cube.ar.Constants.FaceNameEnum;

public class CubeLayoutEditor extends TableLayout implements OnClickListener {

	private final Button[] button = new Button[54];
	private final StateModel stateModel;
	private final FaceNameEnum[] faces = { FaceNameEnum.UP, FaceNameEnum.LEFT,
			FaceNameEnum.FRONT, FaceNameEnum.RIGHT, FaceNameEnum.BACK,
			FaceNameEnum.DOWN };
	private final ColorTileEnum[] color = new ColorTileEnum[54];

	public CubeLayoutEditor(Context context, StateModel stateModel) {
		super(context);
		this.stateModel = stateModel;

		final TableRow.LayoutParams lp = new TableRow.LayoutParams(100, 60);

		int face = 0;
		for (int row = 0; row < 9; row++) {
			TableRow tr = new TableRow(context);
			for (int col = 0; col < 12; col++) {
				if ((row < 3 || row >= 6) && (col < 3 || col >= 6)) {
					if (col < 3) {
						Button dummy = new Button(context);
						tr.addView(dummy, lp);
						dummy.setVisibility(View.INVISIBLE);
					}
					continue;
				}
				button[face] = new Button(context);
				button[face].setId(face + 1);
				int[] faceArray = faceNumToArray(face);
				color[face] = stateModel.getFaceByName(faces[faceArray[0]]).transformedTileArray[faceArray[1]][faceArray[2]];
				if (color[face] == null)
					color[face] = ColorTileEnum.BLACK;
				button[face].getBackground().setColorFilter(
						Color.argb(255, (int) color[face].cvColor.val[0],
								(int) color[face].cvColor.val[1],
								(int) color[face].cvColor.val[2]),
						PorterDuff.Mode.MULTIPLY);
				button[face].setOnClickListener(this);
				tr.addView(button[face], lp);
				face++;
			}
			addView(tr);
		}
	}

	public int[] faceNumToArray(int face) {
		int[] faceArray = new int[3];
		if (face < 9) {
			faceArray[0] = 0;
			faceArray[2] = face / 3;
		} else if (face < 45) {
			faceArray[0] = (face % 12 / 3 + 1) % 4 + 1;
			faceArray[2] = (face - 9) / 12;
		} else {
			faceArray[0] = 5;
			faceArray[2] = (face - 45) / 3;
		}
		faceArray[1] = face % 3;
		return faceArray;
	}

	@Override
	public void onClick(View v) {
		int face = v.getId() - 1;
		color[face] = ColorTileEnum.values()[(color[face].ordinal() + 1) % 6];
		int[] faceArray = faceNumToArray(face);
		stateModel.getFaceByName(faces[faceArray[0]]).transformedTileArray[faceArray[1]][faceArray[2]] = color[face];
		v.getBackground().setColorFilter(
				Color.argb(255, (int) color[face].cvColor.val[0],
						(int) color[face].cvColor.val[1],
						(int) color[face].cvColor.val[2]),
				PorterDuff.Mode.MULTIPLY);
	}

}
