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

package com.android.systemui.qs.customize;

import android.content.ComponentName;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Vibrator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.GridLayoutManager.SpanSizeLookup;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.android.systemui.R;
import com.android.systemui.qs.QSTileView;
import com.android.systemui.qs.customize.TileAdapter.Holder;
import com.android.systemui.qs.customize.TileQueryHelper.TileInfo;
import com.android.systemui.qs.customize.TileQueryHelper.TileStateListener;
import com.android.systemui.qs.external.CustomTile;
import com.android.systemui.statusbar.phone.QSTileHost;

import java.util.ArrayList;
import java.util.List;

import itel.transsion.settingslib.utils.LogUtil;

public class TileAdapter extends RecyclerView.Adapter<Holder> implements TileStateListener {
    private static final String TAG = "TileAdapter";
    private static final long DRAG_LENGTH = 100;
    private static final float DRAG_SCALE = 1.1f;//1.2f
    public static final long MOVE_DURATION = 150;

    private static final int TYPE_SYSTEM = 0;
    private static final int TYPE_DIVIDER = 1;
    private static final int TYPE_CUSTOM = 2;

    private final Context mContext;

    private final List<TileInfo> mTiles = new ArrayList<>();
    private final ItemTouchHelper mItemTouchHelper;
    private List<String> mCurrentSpecs;
    private List<TileInfo> mAllTiles;

    //added by chenzhengjun start
    private Vibrator mVibrator;
    private int mQSItemCount;
    private int mChildCount;
    //added by chenzhengjun end

    private Holder mCurrentDrag;
    private QSTileHost mHost;
    private int mPosition;
    private List<TileInfo> mOtherTiles;

    public TileAdapter(Context context) {
        mContext = context;
        mItemTouchHelper = new ItemTouchHelper(mCallbacks);
        //added by chenzhengjun start
        mVibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        mQSItemCount = mContext.getResources().getInteger(R.integer.qspanel_qs_count);
    }
  // talpa@andy 2017/4/20 18:05 delete:tfs#15632 横屏模式点击编辑通知栏按钮，出现Keyguard停止运行 @{
   /* @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
//        Log.i(TAG, "onAttachedToRecyclerView");
        RecyclerView.LayoutManager lm = recyclerView.getLayoutManager();
        if(lm instanceof GridLayoutManager){
            final GridLayoutManager gridLayoutManager = (GridLayoutManager) lm;
            gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                @Override
                public int getSpanSize(int position) {
//                    Log.i(TAG, "getSpanSize>"+position );
                    if (getItemViewType(position) == TYPE_DIVIDER ){
                        return gridLayoutManager.getSpanCount();
                    }
                    return 1;
                }
            });
        }
    }*/
    // @}

    public int getChildCount(){
        return mChildCount;
    }

    public void setHost(QSTileHost host) {
        mHost = host;
    }

    public ItemTouchHelper getItemTouchHelper() {
        return mItemTouchHelper;
    }
    public void saveSpecs(QSTileHost host) {
        List<String> newSpecs = new ArrayList<>();
        for (int i = 0; i < mQSItemCount; i++) {
            newSpecs.add(mTiles.get(i).spec);
        }
        host.changeTiles(mCurrentSpecs, newSpecs);
        mCurrentSpecs = newSpecs;
    }

    public void setTileSpecs(List<String> currentSpecs) {
        if (currentSpecs.equals(mCurrentSpecs)) {
            return;
        }
        mCurrentSpecs = currentSpecs;
        recalcSpecs();
    }

    /**
     * @param tiles 默认配置的tiles
     */
    @Override
    public void onTilesChanged(List<TileInfo> tiles) {
        mAllTiles = tiles;
        recalcSpecs();
    }

    private void recalcSpecs() {
        if (mCurrentSpecs == null || mAllTiles == null) {
            return;
        }
        mOtherTiles = new ArrayList<TileInfo>(mAllTiles);
        mTiles.clear();
        for (int i = 0; i < mCurrentSpecs.size(); i++) {
            final TileInfo tile = getAndRemoveOther(mCurrentSpecs.get(i));
            if (tile != null) {
                mTiles.add(tile);
            }
        }
//        mTiles.add(null);
        for (int i = 0; i < mOtherTiles.size(); i++) {
            final TileInfo tile = mOtherTiles.get(i);
            if (tile.isSystem) {
                mOtherTiles.remove(i--);
                mTiles.add(tile);
            }
        }
//        mTiles.add(null);
        mTiles.addAll(mOtherTiles);
        if(mAllTiles.size() == 14) {
            mTiles.add(12, null);
        }
        notifyDataSetChanged();
    }

    private TileInfo getAndRemoveOther(String s) {
        for (int i = 0; i < mOtherTiles.size(); i++) {
            if (mOtherTiles.get(i).spec.equals(s)) {
                return mOtherTiles.remove(i);
            }
        }
        return null;
    }

    @Override
    public int getItemViewType(int position) {
        mPosition = position;
       TileInfo tileInfo = mTiles.get(position);
        if (position == 12/*mAccessibilityMoving && position == mEditIndex - 1*/) {
            return TYPE_DIVIDER;
        } else if (tileInfo.isSystem == true){
            return TYPE_SYSTEM;
        } else {
            return TYPE_CUSTOM;
        }
    }
    @Override
    public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
        final Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        if (viewType == TYPE_DIVIDER) {
            View view =  inflater.inflate(R.layout.qs_customize_divider, parent, false);
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) view.
                    findViewById(R.id.double_split_line).getLayoutParams();
            int left = mContext.getResources().getDimensionPixelSize(
                    R.dimen.itel_qs_customizer_divider_left);
            int right = mContext.getResources().getDimensionPixelSize(
                    R.dimen.itel_qs_customizer_divider_right);
            lp.setMargins(left, 0, right, 0);
            return new Holder(view);
        } else if(viewType == TYPE_SYSTEM){
            FrameLayout frame = (FrameLayout) inflater.inflate(R.layout.qs_customize_tile_frame,
                    parent, false);
            frame.addView(new QSTileView(context, new QSCustomizerIconView(context)));
            return new Holder(frame);
        } else {
            FrameLayout frame = (FrameLayout) inflater.inflate(R.layout.qs_customize_tile_frame,
                    parent, false);
            frame.addView(new QSTileView(context, new QSCustomIconView(context)));
            return new Holder(frame);
        }
    }

    @Override
    public int getItemCount() {
        return mTiles.size();
    }

    @Override
    public boolean onFailedToRecycleView(Holder holder) {
        holder.clearDrag();
        return true;
    }

    @Override
    public void onBindViewHolder(final Holder holder, int position) {
        if (holder.getItemViewType() != TYPE_DIVIDER) {
            holder.mTileView.setClickable(true);
            holder.mTileView.setFocusable(true);
            holder.mTileView.setFocusableInTouchMode(true);
            holder.mTileView.setVisibility(View.VISIBLE);
            holder.mTileView.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
            holder.mTileView.setContentDescription(mContext.getString(
                    R.string.accessibility_qs_edit_position_label, position + 1));
            TileInfo info = mTiles.get(position);
            // talpa@andy 2017/4/12 15:10 modify:解决recycleView刷新闪屏问题@{
//          holder.mTileView.onStateChanged(info.state);
            holder.mTileView.handleStateChanged(info.state);
            // @}
            return;
        }
    }

    public SpanSizeLookup getSizeLookup() {
        return mSizeLookup;
    }


    private static String strip(TileInfo tileInfo) {
        String spec = tileInfo.spec;
        if (spec.startsWith(CustomTile.PREFIX)) {
            ComponentName component = CustomTile.getComponentFromSpec(spec);
            return component.getPackageName();
        }
        return spec;
    }

    private boolean move(int from, int to, List<TileInfo> list) {
//        LogUtil.i("onMove>from="+from+"|to="+to);
        if (to == from) {
            return true;
        }
        if (from > 12 && to <12 ) {
            list.add(to,list.remove(from));
            list.add(12,list.remove(13));
            notifyItemMoved(from, to);
            notifyItemMoved(12, 13);
        } else if (from < 12 && to ==12) {//移除tile时，拖拽到分割线时移除tile
            list.add(to + 1, list.remove(from));
            list.add(11, list.remove(12));
            notifyItemMoved(from, to + 1);
            notifyItemMoved(11, 12);
        } else if (from > 12 && to == 12) {//增加tile时，拖拽到分割线不处理
            return true;
        } else if (from < 12 && to > 12) {
            list.add(to, list.remove(from));
            list.add(11, list.remove(12));
            notifyItemMoved(from, to);
            notifyItemMoved(11, 12);
        } else {
            list.add(to,list.remove(from));
            notifyItemRangeChanged(to, 1);
            notifyItemMoved(from, to);
        }
        saveSpecs(mHost);
        return true;
    }

    public class Holder extends ViewHolder {
        private QSTileView mTileView;

        public Holder(View itemView) {
            super(itemView);
            if (itemView instanceof FrameLayout) {
                mTileView = (QSTileView) ((FrameLayout) itemView).getChildAt(0);
                mTileView.setBackground(null);
                mTileView.getIcon().disableAnimation();
            }
        }

        public void clearDrag() {
            itemView.clearAnimation();
            mTileView.findViewById(R.id.tile_label).clearAnimation();
            mTileView.findViewById(R.id.tile_label).setAlpha(1);
            mTileView.getLabel().clearAnimation();
            mTileView.getLabel().setAlpha(.6f);
        }

        public void startDrag() {
            //added by chenzhengjun start 在开始拖拽的时候，震动50ms。
            mVibrator.vibrate(50);
            //added by chenzhengjun end
            itemView.animate()
                    .setDuration(DRAG_LENGTH)
                    .scaleX(DRAG_SCALE)
                    .scaleY(DRAG_SCALE);
            mTileView.findViewById(R.id.tile_label).animate()
                    .setDuration(DRAG_LENGTH)
                    .alpha(0);
            mTileView.getLabel().animate()
                    .setDuration(DRAG_LENGTH)
                    .alpha(0);
        }

        public void stopDrag() {
            itemView.animate()
                    .setDuration(DRAG_LENGTH)
                    .scaleX(1)
                    .scaleY(1);
            mTileView.findViewById(R.id.tile_label).animate()
                    .setDuration(DRAG_LENGTH)
                    .alpha(1);
            mTileView.getLabel().animate()
                    .setDuration(DRAG_LENGTH)
                    .alpha(.6f);
        }
    }

    private final SpanSizeLookup mSizeLookup = new SpanSizeLookup() {
        @Override
        public int getSpanSize(int position) {
            final int type = getItemViewType(position);
            //talpa zhw return type == TYPE_EDIT || type == TYPE_DIVIDER ? 3 : 1;
            int count = 0;
            if (mContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                count = 4;
            } else {
                count = 6;
            }
//            LogUtil.i("TileAdapter>getSpanSize>type = " + (type == TYPE_DIVIDER ? count : 1)+ "| position = " + position);
            return type == TYPE_DIVIDER ? count : 1;//talpa zhw add
        }
    };


    private final ItemTouchHelper.Callback mCallbacks = new ItemTouchHelper.Callback() {

        @Override
        public boolean isLongPressDragEnabled() {
            return true;
        }

        @Override
        public boolean isItemViewSwipeEnabled() {
            return false;
        }

        @Override
        public void onSelectedChanged(ViewHolder viewHolder, int actionState) {
            super.onSelectedChanged(viewHolder, actionState);
            if (actionState != ItemTouchHelper.ACTION_STATE_DRAG) {
                viewHolder = null;
            }
            if (viewHolder == mCurrentDrag) return;
            if (mCurrentDrag != null) {
                int position = mCurrentDrag.getAdapterPosition();
                /* SPRD: Bug 603390  deal with exception @{ */
                if(position == -1 ){
                    return;
                }
                /* @} */
                mCurrentDrag.stopDrag();
                mCurrentDrag = null;
            }
            if (viewHolder != null) {
                mCurrentDrag = (Holder) viewHolder;
                mCurrentDrag.startDrag();
            }
        }

        @Override
        public boolean canDropOver(RecyclerView recyclerView, ViewHolder current,
                ViewHolder target) {
            return true; //返回true的话，当拖拽控件时，控件都可以移动
        }

        @Override
        public int getMovementFlags(RecyclerView recyclerView, ViewHolder viewHolder) {
            int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN | ItemTouchHelper.RIGHT
                    | ItemTouchHelper.LEFT;
            return makeMovementFlags(dragFlags, 0);
        }

        @Override
        public boolean onMove(RecyclerView recyclerView, ViewHolder viewHolder, ViewHolder target) {
            int from = viewHolder.getAdapterPosition();
            int to = target.getAdapterPosition();
            return move(from, to, mTiles);
        }

        @Override
        public void onSwiped(ViewHolder viewHolder, int direction) {
        }
    };
}
