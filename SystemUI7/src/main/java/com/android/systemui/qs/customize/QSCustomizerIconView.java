package com.android.systemui.qs.customize;

import android.content.Context;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.android.systemui.R;
import com.android.systemui.qs.QSIconView;
import com.android.systemui.qs.QSTile;

import java.util.Objects;

/**
 * Created by chunfa.xiang on 2017/3/8.
 * customizer icon view
 */

public class QSCustomizerIconView extends QSIconView {


    public QSCustomizerIconView(Context context) {
        super(context);
    }

    @Override
    protected View createIcon() {
        mIconFrame = new FrameLayout(mContext);
        mTopView = new ImageView(mContext);
        mTopView.setId(android.R.id.icon);
        mTopView.setScaleType(ImageView.ScaleType.CENTER);
        mIconFrame.addView(mTopView);
        return mIconFrame;
    }
}
