/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package itel.transsion.keyguard;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import com.android.keyguard.R;

import java.util.ArrayList;

/** View that represents the Keyguard settings tile panel. **/
public class KeyguardPanel extends ViewGroup {


	private final Context mContext;
	private final ArrayList<GuardRecord> mRecords = new ArrayList<GuardRecord>();

	private int mCellWidth;
	private int mCellHeight;

	private int mColumns;// number of bg
	//private int colorOne;
	//private int colorTwo;
	//private int colorThree;


	public KeyguardPanel(Context context) {
		this(context, null);
	}

	public KeyguardPanel(Context context, AttributeSet attrs) {
		  this(context, attrs, 0);
	
	}
	public KeyguardPanel(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
		mContext = context;
		//colorOne =#4cffffff;
		//colorTwo= #19ffffff;
		//colorTwo= #7fffffff;
		init();
	}
	

	public void init() {
		//linwujia edit begin
		/*WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
		DisplayMetrics dm = new DisplayMetrics();
		wm.getDefaultDisplay().getMetrics(dm);
		mColumns = 3;
		mCellHeight = mCellWidth = (dm.widthPixels - getResources().getDimensionPixelOffset(R.dimen.keyguard_cell_grid_padding) * 2) / 3;*/
		//linwujia edit end

		//mCellHeight = 118; //px
		//mCellWidth = 117;  //px
		mColumns = 3;
		int r = -1;
		int c = -1;
		for(int i=0;i<9;i++)
		{
			final GuardRecord record = new GuardRecord();
			if (r== -1 || c == (mColumns - 1) ) {
				r++;
				c = 0;
			} else {
				c++;
			}
			record.row = r;
			record.col = c;
		
			record.view = new View(mContext);
			//linwujia add begin
			/*if(i%2 == 0) {
				record.color = 0x26ffffff;//George change %15 0x26ffffff
			}else {
				record.color = 0x19ffffff; // George change 10%
			}
			record.view.setBackgroundColor(record.color);*/
			record.resid = getBackgroundResourceId(i);
			record.view.setBackgroundResource(record.resid);
			//linwujia add end
			mRecords.add(record);
			addView(record.view);
		}

	}
	
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int width = MeasureSpec.getSize(widthMeasureSpec);
		int height = MeasureSpec.getSize(heightMeasureSpec);
		mCellHeight = mCellWidth = width / 3;

		for (GuardRecord record : mRecords) {
			record.view.measure(exactly(mCellWidth), exactly(mCellHeight));
		}

		setMeasuredDimension(width, height);
	}

	private static int exactly(int size) {
		return MeasureSpec.makeMeasureSpec(size, MeasureSpec.EXACTLY);
	}


	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {

		for (GuardRecord record : mRecords) {
            int left = record.col * mCellWidth;
			int top = record.row * mCellHeight;
			int right = left + mCellWidth ;
			int bottom = top +mCellHeight ;
			Log.d("KeyguardPatternView", "layout(" + left + ", " + top + ", " + right + ", " + bottom + ")");
			record.view.layout(left, top, right,bottom);
		}
	}
	private  final class GuardRecord  {
		int row;
		int col;
		//linwujia add begin
		//int color;
		int resid;
		//linwujia add end
		View view;
	}

	/**
	 *
	 * @param index
	 * @return ResourceId
	 */
	public int getBackgroundResourceId(int index) {
		int resid = 0;
		if(index == 0) {
			resid = R.drawable.itel_keyguard_pattern_topleft_key_bg;
		} else if(index == 2) {
			resid = R.drawable.itel_keyguard_pattern_topright_key_bg;
		} else if(index == 6) {
			resid = R.drawable.itel_keyguard_pattern_bottomleft_key_bg;
		} else if(index == 8) {
			resid = R.drawable.itel_keyguard_pattern_bottomright_key_bg;
		} else if(index % 2 == 0) {
			resid = R.drawable.itel_keyguard_pattern_even_key_bg;
		} else {
			resid = R.drawable.itel_keyguard_pattern_odd_key_bg;
		}
		return resid;
	}

}
