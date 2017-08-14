package com.android.systemui.recents.talpa;

import android.content.Context;
import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;


/**
 * Created by deping.huang on 2016/12/15.
 */
public class TaskViewHeaderMoveHelper {
    private static final String TAG = TaskViewHeaderMoveHelper.class.getSimpleName();
    private RecyclerView mRecyclerView;
    private Context mContext;

    private float mScale = 0.95f; // 两边视图scale
    private int mTotalMarginPx; // 两张卡片之间的间隔总和

    private int mPageWidth; // 卡片宽度
    private int mOneScrollWidth; // 滑动一页的距离
    private int mHeadViewID;


    private int mCurrentItemPos;
    private int mCurrentItemOffset;
    private int mLastItemPos;

    private CardLinearSnapHelper mLinearSnapHelper = new CardLinearSnapHelper();

    public void attachToRecyclerView(final RecyclerView mRecyclerView, int pageWidth, int pageMargin, int itemHeadViewID) {
        // 开启log会影响滑动体验, 调试时才开启
        mPageWidth = pageWidth;
        mTotalMarginPx = pageMargin * 2;
        mHeadViewID = itemHeadViewID;

        this.mRecyclerView = mRecyclerView;
        mContext = mRecyclerView.getContext();
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    mLinearSnapHelper.mNoNeedToScroll = mCurrentItemOffset == 0
                            || mCurrentItemOffset == getDestItemOffset(mRecyclerView.getAdapter().getItemCount() - 1);
                } else {
                    mLinearSnapHelper.mNoNeedToScroll = false;
                }
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                // dx>0则表示右滑, dx<0表示左滑, dy<0表示上滑, dy>0表示下滑
                mCurrentItemOffset += dx;
                computeCurrentItemPos();
                /*onScrollHeadView(dx);*/
                /*onScrolledChangedCallback();*/
            }
        });

        initWidth();
        mLinearSnapHelper.attachToRecyclerView(mRecyclerView);
    }

    /**
     * 初始化卡片宽度
     */
    private void initWidth() {
        mRecyclerView.post(new Runnable() {
            @Override
            public void run() {
                mOneScrollWidth = mPageWidth + mTotalMarginPx;
                mRecyclerView.smoothScrollToPosition(mCurrentItemPos);
                /*onScrolledChangedCallback();*/
            }
        });
    }

    public void setCurrentItemPos(int currentItemPos) {
        this.mCurrentItemPos = currentItemPos;
    }

    public int getCurrentItemPos() {
        return mCurrentItemPos;
    }

    public void clearParam(int pageWidth, int pageMargin){
        mPageWidth = pageWidth;
        mTotalMarginPx = pageMargin*2;
        mOneScrollWidth = mPageWidth + mTotalMarginPx;
        mCurrentItemPos = 0;
        mCurrentItemOffset = 0;
    }

    private int getDestItemOffset(int destPos) {
        return mOneScrollWidth * destPos;
    }

    /**
     * 计算mCurrentItemOffset
     */
    private void computeCurrentItemPos() {
        if (mOneScrollWidth <= 0) return;
        boolean pageChanged = false;
        // 滑动超过一页说明已翻页
        if (Math.abs(mCurrentItemOffset - mCurrentItemPos * mOneScrollWidth) >= mOneScrollWidth) {
            pageChanged = true;
        }
        if (pageChanged) {
            mLastItemPos = mCurrentItemPos;
            mCurrentItemPos = mCurrentItemOffset / mOneScrollWidth;
        }
    }

    private void onScrollHeadView(int dx){
        int offset = mCurrentItemOffset - mCurrentItemPos * mOneScrollWidth;
//        Log.i(TAG,"----------offset: "+offset + "     mCurrentItemOffset: "+mCurrentItemOffset
//                +"   currentPos:"+mCurrentItemPos + "  mLastItemPos:" + mLastItemPos);
//        if (Math.abs(mCurrentItemPos - mLastItemPos) > 1 && offset != 0) {
//            Log.i(TAG, "滑动过快，忽略表头移动处理");
//
//            return;
//        }

        View leftView = null;
        View currentView;
        View rightView = null;
        if (mCurrentItemPos > 0) {
            leftView = mRecyclerView.getLayoutManager().findViewByPosition(mCurrentItemPos - 1);
        }
        currentView = mRecyclerView.getLayoutManager().findViewByPosition(mCurrentItemPos);
        if (mCurrentItemPos < mRecyclerView.getAdapter().getItemCount() - 1) {
            rightView = mRecyclerView.getLayoutManager().findViewByPosition(mCurrentItemPos + 1);
        }


//        // for test start
//        if (mCurrentItemOffset == 0){
//            View headerView = currentView.findViewById(mHeadViewID);
//            Rect rcGlobal = new Rect();
//            headerView.getGlobalVisibleRect(rcGlobal);
//            Log.i(TAG, "更新前headerView GlobalVisibleRect"+ rcGlobal);
//        }
//        // for test end

        // 绘制头部移动的动画（实际位置不移）
        //if (Math.abs(offset) > 10) {
            if (leftView != null) {
                moveHeaderView(leftView, offset, dx, false);
            }
            if (currentView != null) {
                moveHeaderView(currentView, offset, dx, true);
            }
            if (rightView != null) {
                moveHeaderView(rightView, offset,dx, false);
            }
       // }


        // 修正初始状态headerView的位置
        if (offset == 0) {
            if (leftView != null) {
                setViewAlignToRelativeLayout(leftView, Gravity.RIGHT);
            }
            if (currentView != null) {
                setViewAlignToRelativeLayout(currentView, Gravity.CENTER);
            }
            if (rightView != null){
                setViewAlignToRelativeLayout(rightView, Gravity.LEFT);
            }
        }

//        // for test start
//        if (mCurrentItemOffset == 0){
//            View headerView = currentView.findViewById(mHeadViewID);
//            Rect rcGlobal = new Rect();
//            headerView.getGlobalVisibleRect(rcGlobal);
//            Log.i(TAG, "更新后headerView GlobalVisibleRect"+ rcGlobal);
//        }
//        // for test end

 //       Log.i(TAG, "dx:" + dx);
    }
    private void setViewAlignToRelativeLayout(View itemView, int gravityType){
        View headView = itemView.findViewById(mHeadViewID);
        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams)headView.getLayoutParams();
        layoutParams.gravity = gravityType;
        headView.setLayoutParams(layoutParams);
        headView.scrollTo(0,0);
    }


    private void moveHeaderView(View itemView, int offset,int dx, boolean isPrintLogTest){
        View headView = itemView.findViewById(mHeadViewID);
        if (null != headView) {
            if (mOneScrollWidth != 0) {
                int HEAD_WIDTH = headView.getWidth();
                int HEAD_TOTAL_PATH = (mOneScrollWidth - HEAD_WIDTH)/2;


                Rect rectLocal = new Rect();
                itemView.getLocalVisibleRect(rectLocal);
                //Log.i(TAG, "rectLocal.left :" + rectLocal.left + "  rectLocal.right :" + rectLocal.right);
                if (rectLocal.left < 0 || rectLocal.right < 0||
                        rectLocal.right - rectLocal.left < HEAD_WIDTH){
                    //Log.i(TAG, "区域过小 不移动");
                }
                else {
                    int headScrollDx;
                    float headScrollDxFloat = -(float)dx*HEAD_TOTAL_PATH/ mOneScrollWidth;
                    //Log.i(TAG, "headScrollDxFloat:"+ headScrol    lDxFloat);
                    if (headScrollDxFloat >0.000001&& headScrollDxFloat<1.0){
                        headScrollDx = 1;
                    }
                    else {
                        headScrollDx = (int)headScrollDxFloat;
                    }



                    if (isPrintLogTest) {
                        Log.i(TAG, "headView.getScrollX()："+ headView.getScrollX()
                                + "headView.get"
                                +"      itemView.getRight()" + itemView.getRight());

 /*                       Rect rcGlobalHeadView = new Rect();
                        headView.getGlobalVisibleRect(rcGlobalHeadView);
                        Log.i(TAG, "更新中headerView GlobalVisibleRect"+ rcGlobalHeadView);

                        Rect rcGlobalItemView = new Rect();
                        itemView.getGlobalVisibleRect(rcGlobalItemView);
                        Log.i(TAG, "更新中itemView GlobalVisibleRect"+ rcGlobalItemView);

                        if ((rcGlobalHeadView.left+headScrollDx < rcGlobalItemView.left)
                                || (rcGlobalHeadView.right+headScrollDx > rcGlobalItemView.right)) {
                            Log.i(TAG, "移动之后headView在外面 不移动");
                            //headView.scrollTo(0,0);
                            return;
                        }*/

                    }


                    headView.scrollBy(headScrollDx,0);

                    /* int headScrollOffset = -offset*HEAD_TOTAL_PATH /mOneScrollWidth;
                     headView.scrollTo(headScrollOffset,0);*/
                }
            }
        }
    }

    public void resetPosition(int position){
        mCurrentItemPos = position;
        mCurrentItemOffset =  mCurrentItemPos * mOneScrollWidth;
    }

    public void setScale(float scale) {
        mScale = scale;
    }


    /**
    * RecyclerView位移事件监听, view大小随位移事件变化
     */
    private void onScrolledChangedCallback() {
        int offset = mCurrentItemOffset - mCurrentItemPos * mOneScrollWidth;
        //Log.i("DepingHuang","----------offset: "+offset);

        float percent = (float) Math.max(Math.abs(offset) * 1.0 / mOneScrollWidth, 0.0001);

        View leftView = null;
        View currentView;
        View rightView = null;
        if (mCurrentItemPos > 0) {
            leftView = mRecyclerView.getLayoutManager().findViewByPosition(mCurrentItemPos - 1);
        }
        currentView = mRecyclerView.getLayoutManager().findViewByPosition(mCurrentItemPos);
        if (mCurrentItemPos < mRecyclerView.getAdapter().getItemCount() - 1) {
            rightView = mRecyclerView.getLayoutManager().findViewByPosition(mCurrentItemPos + 1);
        }

        // 放大动画
        if (leftView != null) {
            // y = (1 - mScale)x + mScale
            leftView.setScaleY((1 - mScale) * percent + mScale);
        }
        if (currentView != null) {
            // y = (mScale - 1)x + 1
            currentView.setScaleY((mScale - 1) * percent + 1);
        }
        if (rightView != null) {
            // y = (1 - mScale)x + mScale
            rightView.setScaleY((1 - mScale) * percent + mScale);
        }
    }

}
