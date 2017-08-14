package com.android.systemui.qs.customize;

import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.android.systemui.R;
import com.android.systemui.qs.QSIconView;
import com.android.systemui.qs.QSTile;

/**
 * Created by chunfa.xiang on 2017/4/5.
 * 排序快捷面板中的自定义Tile图片
 */

public class QSCustomIconView extends QSIconView {
    protected ImageView mCustomBgView;

    public ImageView getBgView() {
        return mBgView;
    }

    public QSCustomIconView(Context context) {
        super(context);
    }

    @Override
    protected View createIcon() {
        mIconFrame = new FrameLayout(mContext);

      /*mBgView = new ImageView(mContext);
        mBgView.setScaleType(ImageView.ScaleType.CENTER);
        mBgView.setImageDrawable(mContext.getDrawable(R.drawable.itel_qs_bg));
        mIconFrame.addView(mBgView);

        mBottomView = new ImageView(mContext);
        mBottomView.setScaleType(ImageView.ScaleType.CENTER);
        mIconFrame.addView(mBottomView);
        */
        //自定义Tile背景
        mCustomBgView = new ImageView(mContext);
        mCustomBgView.setImageDrawable(mContext.getDrawable(R.drawable.itel_qs_tile_bg_dark));
        mCustomBgView.setScaleType(ImageView.ScaleType.CENTER);
        mIconFrame.addView(mCustomBgView);

        mTopView = new ImageView(mContext);
        mTopView.setId(android.R.id.icon);
        mTopView.setScaleType(ImageView.ScaleType.CENTER);
        mIconFrame.addView(mTopView);

      /*  mMaskView = new ImageView(mContext);
        mMaskView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        mMaskView.setImageDrawable(mContext.getDrawable(R.drawable.itel_qs_mask));
        mIconFrame.addView(mMaskView);*/
        return mIconFrame;
    }
}
