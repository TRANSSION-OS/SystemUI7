package com.android.systemui.recents.talpa;

import android.view.View;
import android.view.ViewGroup;

import com.android.systemui.R;
import com.android.systemui.recents.util.ScreenUtil;


/**
 * adapter中调用onCreateViewHolder, onBindViewHolder
 *  Created by deping.huang on 2016/12/15.
 */
public class CardAdapterHelper {
    private int mPageMargin;
    private int mPageWith;
    private int mPageHeight;
    private int mContainerWith;

    public void onCreateViewHolder(int containerWidth, int pageWith, int pageHeight,int pageMargin, View itemView) {
        mContainerWith = containerWidth;
        mPageWith = pageWith;
        mPageHeight = pageHeight;
        mPageMargin = pageMargin;

        // 总布局宽度固定
        View layoutRoot =  itemView.findViewById(R.id.layout_root);
        if (layoutRoot != null) {
            ViewGroup.LayoutParams lp = layoutRoot.getLayoutParams();
            lp.width = pageWith;
            layoutRoot.setLayoutParams(lp);
        }

        View viewThumbnailContainer =  itemView.findViewById(R.id.view_thumbnail_container);
        if (viewThumbnailContainer != null) {
            ViewGroup.LayoutParams lp = viewThumbnailContainer.getLayoutParams();
            lp.height = pageHeight;
            viewThumbnailContainer.setLayoutParams(lp);
        }

    }

    public void onBindViewHolder(View itemView, final int position, int itemCount) {

        // 左边显示页的宽度+空隙 =（屏幕宽度-页宽度）/2,
        // mPageWith必须为偶数，否则会出现两边对不齐的情况，精确计算页面滑动时需注意
        int showLeftWidth = (mContainerWith - mPageWith) / 2;

        int leftMargin = (position == 0) ? showLeftWidth : mPageMargin;
        int rightMargin = (position == itemCount - 1) ? showLeftWidth : mPageMargin;
        setViewMargin(itemView, leftMargin, 0, rightMargin, 0);
    }

    private void setViewMargin(View view, int left, int top, int right, int bottom) {
        ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) view.getLayoutParams();

        //if (lp.leftMargin != left || lp.topMargin != top || lp.rightMargin != right || lp.bottomMargin != bottom) {
            //bo.yang modify
            lp.setMarginsRelative(left, top, right, bottom);
            //lp.setMargins(left, top, right, bottom);
            view.setLayoutParams(lp);
       // }
    }

}