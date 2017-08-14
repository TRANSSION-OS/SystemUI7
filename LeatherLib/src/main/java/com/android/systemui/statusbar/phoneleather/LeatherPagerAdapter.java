package com.android.systemui.statusbar.phoneleather;

import android.support.v4.view.PagerAdapter;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import itel.transsion.settingslib.utils.LogUtil;

public class LeatherPagerAdapter extends PagerAdapter {
	private List<View> mListViews;
	private int mChangedView;
	
	public LeatherPagerAdapter(List<View> mListViews) {
		this.mListViews = mListViews;
		mChangedView = -1;
	}
	
	@Override
	public int getCount() {
		return mListViews.size();
	}

	@Override
	public boolean isViewFromObject(View view, Object obj) {
		return view == obj;
	}
	
	@Override
	public void destroyItem(ViewGroup container, int position, Object object) {
		LogUtil.d("destroyItem position:" + position);
		container.removeView((View) object);
	}

	@Override
	public Object instantiateItem(ViewGroup container, int position) {
		View view = mListViews.get(position);
		view.setTag(position);
		container.addView(view);
		return mListViews.get(position);
	}
	
	@Override
	public int getItemPosition(Object object) {
		View view = (View) object;
		if(mChangedView == (Integer)view.getTag()) {
			LogUtil.d("getItemPosition:POSITION_NONE,  mChangedView:" + mChangedView + ",(Integer)view.getTag():" + (Integer)view.getTag());
			return POSITION_NONE;
		} else {
			LogUtil.d("getItemPosition:POSITION_UNCHANGED,  mChangedView:" + mChangedView + ",(Integer)view.getTag():" + (Integer)view.getTag());
			return POSITION_UNCHANGED;
		}
	}
	
	/**
	 * 设置新View的索引值
	 * @param index 索引值
	 */
	public void setChangedViewIndex(int index) {
		mChangedView = index;
	}

}
