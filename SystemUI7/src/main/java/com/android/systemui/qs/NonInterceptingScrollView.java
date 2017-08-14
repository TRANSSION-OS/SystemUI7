/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.android.systemui.qs;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.ScrollView;

import itel.transsion.settingslib.utils.LogUtil;

/**
 * ScrollView that disallows intercepting for touches that can cause scrolling.
 */
public class NonInterceptingScrollView extends ScrollView {

    public NonInterceptingScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        int action = ev.getActionMasked();
//        LogUtil.i("action="+ev.getActionMasked()+"|x="+ev.getX()+"|y="+ev.getY());
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                // talpa@andy 2017/5/5 12:11 modify @{
                if (canScrollVertically()) {
                // @}
                    requestDisallowInterceptTouchEvent(true);
                }
                break;
        }
        return super.onTouchEvent(ev);
    }
    // talpa@andy 2017/5/5 12:10 add @{
    /**
     * 判断是否可以滚动
     * @return
     */
    public boolean canScrollVertically() {
        int range = this.computeVerticalScrollRange() - this.computeVerticalScrollExtent();
//        LogUtil.i("range="+range+"|computeVerticalScrollRange="+computeVerticalScrollRange()+"" +
//                "|computeVerticalScrollExtent="+computeVerticalScrollExtent());
        return range > 0? true : false;
    }
    // @}
}
