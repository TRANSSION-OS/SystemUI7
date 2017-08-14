package itel.transsion.systemui.View;/*
 * Copyright (C) 2008 The Android Open Source Project
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


import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.android.keyguard.AlphaOptimizedLinearLayout;
import com.android.systemui.R;
import com.android.systemui.statusbar.ExpandableNotificationRow;

import itel.transsion.systemui.View.NotificationNumberView;

public class TopIconMerger extends LinearLayout {
    private static final String TAG = "TopIconMerger";
    private static final boolean DEBUG = false;

    private int mIconSize;
    private int mIconHPadding;
    private int mShowAppCount;

    public TopIconMerger(Context context, AttributeSet attrs) {
        super(context, attrs);
        reloadDimens();
        if (DEBUG) {
            setBackgroundColor(0x800099FF);
        }
    }

    private void reloadDimens() {
        Resources res = mContext.getResources();
        mIconSize = res.getDimensionPixelSize(R.dimen.itel_status_bar_icon_size);
        mIconHPadding = res.getDimensionPixelSize(R.dimen.status_bar_icon_padding);
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        reloadDimens();
    }

    public void setShowAppCount(int showAppCount) {
        mShowAppCount = showAppCount;
    }

    private int getFullIconWidth() {
        return mIconSize + 2 * mIconHPadding;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        // we need to constrain this to an integral multiple of our children
        int width = getMeasuredWidth();
        //setMeasuredDimension(width- (width % getFullIconWidth()), getMeasuredHeight());
        /// set IconMerger space as small as possible
        if (mShowAppCount * getFullIconWidth() < (width - (width % getFullIconWidth()))) {
            setMeasuredDimension(mShowAppCount * getFullIconWidth(), getMeasuredHeight());
        } else {
            setMeasuredDimension(width - (width % getFullIconWidth()), getMeasuredHeight());
        }
    }
}
