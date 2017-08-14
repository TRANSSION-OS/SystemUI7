/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.systemui.qs;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Space;

import com.android.systemui.R;
import com.android.systemui.qs.QSTile.SignalState;
import com.android.systemui.qs.QSTile.State;
import com.android.systemui.qs.customize.QSCustomizer;
import com.android.systemui.qs.tiles.AirplaneModeTile;
import com.android.systemui.qs.tiles.BluetoothTile;
import com.android.systemui.qs.tiles.HotspotTile;
import com.android.systemui.recents.talpa.SwipeRecyclerView;
import com.android.systemui.statusbar.phone.QSTileHost;
import com.android.systemui.tuner.TunerService;
import com.android.systemui.tuner.TunerService.Tunable;

import java.util.ArrayList;
import java.util.Collection;

import itel.transsion.settingslib.utils.LogUtil;

/**
 * Version of QSPanel that only shows N Quick Tiles in the QS Header.
 */
public class QuickQSPanel extends QSPanel {

    public static final String NUM_QUICK_TILES = "sysui_qqs_count";

    private int mMaxTiles;
    private QSPanel mFullPanel;
    private View mHeader;

    public QuickQSPanel(Context context, AttributeSet attrs) {
        super(context, attrs);
        if (mTileLayout != null) {
            for (int i = 0; i < mRecords.size(); i++) {
                mTileLayout.removeTile(mRecords.get(i));
            }
            removeView((View) mTileLayout);
        }
        //talpa zhw add
//        for(int i = 0; i < getChildCount();i++)
//        {
//            View v = getChildAt(i);
//            v.setVisibility(View.GONE);
//        }
        //talpz zhw add end
       // talpa@andy: delete @{
        mTileLayout = new HeaderTileLayout(mContext);
        // @}
        mTileLayout.setListening(true);
        addView((View) mTileLayout, 1 /* Between brightness and footer */);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        TunerService.get(mContext).addTunable(mNumTiles, NUM_QUICK_TILES);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        TunerService.get(mContext).removeTunable(mNumTiles);
    }

    public void setQSPanelAndHeader(QSPanel fullPanel, View header) {
        mFullPanel = fullPanel;
        mHeader = header;
    }

    @Override
    protected boolean shouldShowDetail() {
        return !mExpanded;
    }

    @Override
    protected void drawTile(TileRecord r, State state) {
        if (state instanceof SignalState) {
            State copy = r.tile.newTileState();
            state.copyTo(copy);
            // No activity shown in the quick panel.
            ((SignalState) copy).activityIn = false;
            ((SignalState) copy).activityOut = false;
            state = copy;
        }
        super.drawTile(r, state);
    }

    @Override
    protected QSTileBaseView createTileView(QSTile<?> tile, boolean collapsedView) {
        //talpa zhw add
        if(true)
        {
            QSIconView iconview = tile.createTileView(mContext);
            iconview.setQSPanel(QuickQSPanel.this);
            return new QSTileBaseView(mContext, iconview, collapsedView);
        }
        //talpa zhw add end
        return new QSTileBaseView(mContext, tile.createTileView(mContext), collapsedView);
    }

    // talpa@andy 2017/5/25 9:53 delete @{
    /*@Override
    public void setHost(QSTileHost host, QSCustomizer customizer) {
        super.setHost(host, customizer);
        setTiles(mHost.getTiles());
    }*/
    // @}

    public void setMaxTiles(int maxTiles) {
        mMaxTiles = maxTiles;
        if (mHost != null) {
            setTiles(mHost.getTiles());
        }
    }

    @Override
    protected void onTileClick(QSTile<?> tile, QSTileBaseView tileBaseView) {
        tile.secondaryClick();
    }

    @Override
    public void onTuningChanged(String key, String newValue) {
        // No tunings for you.
        if (key.equals(QS_SHOW_BRIGHTNESS)) {
            // No Brightness for you.
            super.onTuningChanged(key, "0");
        }
    }

    @Override
    public void setTiles(Collection<QSTile<?>> tiles) {
        ArrayList<QSTile<?>> quickTiles = new ArrayList<>();
        for (QSTile<?> tile : tiles) {
            quickTiles.add(tile);
            if (quickTiles.size() == mMaxTiles) {
                break;
            }
        }
        super.setTiles(quickTiles, true);
    }

    private final Tunable mNumTiles = new Tunable() {
        @Override
        public void onTuningChanged(String key, String newValue) {
            setMaxTiles(getNumQuickTiles(mContext));
        }
    };

    public int getNumQuickTiles(Context context) {
        //talpa zhw add
        if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
        {
            return TunerService.get(context).getValue(NUM_QUICK_TILES, /*talpa zhw 5 */ 6);
        }
        //talpa zhw add end
        return TunerService.get(context).getValue(NUM_QUICK_TILES, /*talpa zhw 5 */ 4);
    }
    private static class HeaderTileLayout extends LinearLayout implements QSTileLayout {

        private final Space mEndSpacer;
        protected final ArrayList<TileRecord> mRecords = new ArrayList<>();
        private boolean mListening;

        public HeaderTileLayout(Context context) {
            super(context);
            setClipChildren(true);
            setClipToPadding(true);
            //talpa zhw modify setGravity(Gravity.CENTER_VERTICAL);
            setGravity(Gravity.TOP);
            //talpa zhw modify end
            setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
            mEndSpacer = new Space(context);
            //talpa zhw add
            mEndSpacer.setVisibility(View.GONE);//talpa zhw
            LayoutParams lp = generateLayoutParams();
            //lp.weight = -1;
            mEndSpacer.setLayoutParams(lp);
            //talpa zhw add end
            //talpa zhw mEndSpacer.setLayoutParams(generateLayoutParams());
            updateDownArrowMargin();
            addView(mEndSpacer);
            setOrientation(LinearLayout.HORIZONTAL);
        }

        @Override
        protected void onConfigurationChanged(Configuration newConfig) {
            super.onConfigurationChanged(newConfig);
            updateDownArrowMargin();
        }

        private void updateDownArrowMargin() {
            LayoutParams params = (LayoutParams) mEndSpacer.getLayoutParams();
            params.setMarginStart(mContext.getResources().getDimensionPixelSize(
                    R.dimen.qs_expand_margin));
            mEndSpacer.setLayoutParams(params);
        }

        @Override
        public void setListening(boolean listening) {
            if (mListening == listening) return;
            mListening = listening;
            for (TileRecord record : mRecords) {
                record.tile.setListening(this, mListening);
            }
        }

        boolean first = true;
        @Override
        public void addTile(TileRecord tile) {
            //talpa zhw add
            View v = tile.tileView;
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), 0);
            //talpa zhw add end
            addView(tile.tileView, getChildCount() - 1 /* Leave icon at end */,
                    generateLayoutParams());
            // Add a spacer.
            //talpa zhw add addView(new Space(mContext), getChildCount() - 1 /* Leave icon at end */,
            //        generateSpaceParams());
            mRecords.add(tile);
            tile.tile.setListening(this, mListening);
        }

        private LayoutParams generateSpaceParams() {
            int size = mContext.getResources().getDimensionPixelSize(R.dimen.qs_quick_tile_size);
            LayoutParams lp = new LayoutParams(0, size);
            lp.weight = 1;
            lp.gravity = Gravity.CENTER;
            return lp;
        }

        private LayoutParams generateLayoutParams() {
            int size = mContext.getResources().getDimensionPixelSize(R.dimen.qs_quick_tile_size);
            //talpa zhw modify
            //LayoutParams lp = new LayoutParams(/*talpa zhw size */ 0, size);
            //
            LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
            //talpa zhw modify end
            lp.weight = 1;//talpa zhw add
            lp.gravity = Gravity.CENTER;
            return lp;
        }

        @Override
        public void removeTile(TileRecord tile) {
            int childIndex = getChildIndex(tile.tileView);
            // Remove the tile.
            removeViewAt(childIndex);
            // Remove its spacer as well.
            //talpa zhw removeViewAt(childIndex);
            mRecords.remove(tile);
            tile.tile.setListening(this, false);
        }

        private int getChildIndex(QSTileBaseView tileView) {
            final int N = getChildCount();
            for (int i = 0; i < N; i++) {
                if (getChildAt(i) == tileView) {
                    return i;
                }
            }
            return -1;
        }

        @Override
        public int getOffsetTop(TileRecord tile) {
            return 0;
        }

        @Override
        public boolean updateResources() {
            // No resources here.
            return false;
        }

        @Override
        public boolean hasOverlappingRendering() {
            return false;
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            if (mRecords != null && mRecords.size() > 0) {
                View previousView = this;
                for (TileRecord record : mRecords) {
                    if (record.tileView.getVisibility() == GONE) continue;
                    previousView = record.tileView.updateAccessibilityOrder(previousView);
                }
                // talpa@andy 2017/3/27 14:32 add @{
               /* mRecords.get(0).tileView.setAccessibilityTraversalAfter(
                        R.id.alarm_status_collapsed);*/
    /*            mRecords.get(mRecords.size() - 1).tileView.setAccessibilityTraversalBefore(
                        R.id.expand_indicator);*/
                // @}
            }
        }
    }
}
