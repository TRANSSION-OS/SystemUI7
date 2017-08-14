package com.android.systemui.recents.talpa;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Scroller;

import com.android.systemui.R;
import com.android.systemui.recents.views.TaskTalpaView;


/**
 *  1. 控制fling速度的RecyclerView
 *  2. 滑动手势相关监测处理
 * add by Deping Huang
 */
public class SwipeRecyclerView extends RecyclerView {
	private static final float FLING_SCALE_DOWN_FACTOR = 0.5f; 	// 减速因子
	private static final int FLING_MAX_VELOCITY = 5000; 		// 最大瞬时滑动速度
	private static final int REMOVE_ANIM_DURATION = 200;		// 删除效果的时长
	private static final int DELETE_MOVE_ITEM_DURATION = 100;	// 删除后移动效果的时长
	private static final int DRAG_BACK_DURATION = 200;			// 拖拽回弹的效果时长

	//private Orientation orientation = Orientation.HORIZONTAL;
	/**
	 * 当前滑动的ListView　position
	 */
	private int mSlidePosition;
	/**
	 * 手指按下X的坐标
	 */
	private float mDownX;
	/**
	 * 手指按下Y的坐标
	 */
	private float mDownY;
	/**
	 * 屏幕宽度
	 */
	private int mScreenHeight;
	/**
	 * RecyclerView的item
	 */
	private TaskTalpaView mItemView;
	private View mControlLayout;
	private View mHeaderLayout;
	private View mOperatorLayout;

	// Talpa:bo.yang1 add del last down itemview @{
	private View mMemoryShowTV;
	private View mClearButton;
	private boolean mIsMidPos;
	private boolean mIsRestore;
	private boolean mIsLRSlop;
	private TaskTalpaView mPreItemView;
	private View mPreControlLayout;
	private View mPreHeaderLayout;
	private View mPreOperatorLayout;
	//@}
	/**
	 * 滑动类
	 */
	private Scroller mScroller;
	private static final int SNAP_VELOCITY = 600;
	/**
	 * 速度追踪对象
	 */
	private VelocityTracker mVelocityTracker;
	/**
	 * 是否响应滑动，默认为不响应
	 */
	private boolean mIsSlide = false;
	/**
	 * 认为是用户滑动的最小距离
	 */
	private int mTouchSlop;
	/**
	 * 移除item后的回调接口
	 */
	private RemoveListener mRemoveListener;
	/**
	 * 用来指示item滑出屏幕的方向,向左或者向右,用一个枚举值来标记
	 */
	private RemoveDirection removeDirection;

	// 滑动删除方向的枚举值
	public enum RemoveDirection {
		UP, DOWN;
	}

	/**
	 * 是否处于控件操作模式
	 */
	private volatile boolean mIsItemOperateMode;


	private boolean mIsInMultiWindowMode = false;

	/**
	 * 移动距离初始定义
	 */
	private int mOperatorSwipeH;
	private int mOperatorBtnMoveH;
	private Resources mRes;

	public SwipeRecyclerView(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
		init(context);
	}

	public SwipeRecyclerView(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
		init(context);
	}

	public SwipeRecyclerView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		// TODO Auto-generated constructor stub
		init(context);
	}

	public void init(Context context) {
		mScreenHeight = ((WindowManager) context
				.getSystemService(Context.WINDOW_SERVICE))
				.getDefaultDisplay().getHeight();
		mScroller = new Scroller(context);
		mTouchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();

		// 修改默认Item动效的时长
		// mTaskListView.getItemAnimator().setRemoveDuration(0);
		//mTaskListView.getItemAnimator().setChangeDuration(0);
		this.getItemAnimator().setMoveDuration(DELETE_MOVE_ITEM_DURATION);

		mIsItemOperateMode = false;
		mRes = context.getResources();
		mOperatorSwipeH = mRes.getDimensionPixelSize(R.dimen.recents_talpa_task_view_operate_swipe_h);
		mOperatorBtnMoveH = mRes.getDimensionPixelOffset(R.dimen.recents_talpa_task_view_operate_btn_move_h);
	}

	/**
	 * 设置滑动删除的回调接口
	 *
	 * @param removeListener
	 */
	public void setRemoveListener(RemoveListener removeListener) {
		this.mRemoveListener = removeListener;
	}

	/**
	 * 分发事件，主要做的是判断点击的是那个item, 以及通过postDelayed来设置响应左右滑动事件
	 */
	@Override
	public boolean dispatchTouchEvent(MotionEvent event) {
		switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN: {
				addVelocityTracker(event);

				// 假如scroller滚动还没有结束，我们直接返回
				if (!mScroller.isFinished()) {
					return super.dispatchTouchEvent(event);
				}

				mDownX = event.getX();
				mDownY = event.getY();

				mItemView = (TaskTalpaView)findChildViewUnder(mDownX, mDownY);
				if (mItemView == null) {
					return super.dispatchTouchEvent(event);
				}

				// Talpa:bo.yang1 只相应中间的itemview @{
				mIsMidPos=false;
				mIsLRSlop=false;
				Rect rt = new Rect();
				mItemView.getLocalVisibleRect(rt);
				if(rt.right-rt.left>=mItemView.getWidth()){
					mIsMidPos=true;
				}
				findMemoryTvAndClearBtn(mItemView);
				//@}
				// Talpa bo.yang1 add for mIsItemOperateMode init @{
				mIsItemOperateMode = false;
				//@}
				mControlLayout = mItemView.getThumbContainerLayout();
				mHeaderLayout = mItemView.getHeaderLayout();
				mOperatorLayout = mItemView.getOperatorLayout();


				//mSlidePosition = getChildPosition(mItemView);
				mSlidePosition = getChildAdapterPosition(mItemView);
				// 无效的position, 不做任何处理
				if (mSlidePosition == AdapterView.INVALID_POSITION) {
					return super.dispatchTouchEvent(event);
				}

				break;
			}
			case MotionEvent.ACTION_MOVE: {
				// 这里判断是上滑删除还是下滑弹出功能键
				// 如果 上下滑动的距离大于最大的滑动距离，并且没有左右滑动，就代表上滑或者下滑
				float touchSlopY=Math.abs(event.getY() - mDownY);
				float touchSlopX=Math.abs(event.getX() - mDownX);
				if (Math.abs(getScrollVelocity()) > SNAP_VELOCITY
						|| (touchSlopY > mTouchSlop && touchSlopX < mTouchSlop)) {
					mIsSlide = true;
					// Talpa:bo.yang1 add for restore itemview @{
				}else if(!mIsRestore && mPreControlLayout!=null && mPreControlLayout.getTranslationY() != 0
						&& ((mPreOperatorLayout!=mOperatorLayout )||(touchSlopY < mTouchSlop && touchSlopX > mTouchSlop
						&& mPreOperatorLayout==mOperatorLayout))){
					mIsLRSlop=true;
						//reStoreItemView();

					//@}
				}

				break;
			}
			case MotionEvent.ACTION_UP:
				recycleVelocityTracker();
				break;
		}

		return super.dispatchTouchEvent(event);
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent e) {
		int action = e.getAction();
		if (action == MotionEvent.ACTION_DOWN
				|| action == MotionEvent.ACTION_MOVE
				|| action == MotionEvent.ACTION_UP) {
			if (mIsSlide) { // 滑动task屏蔽下发事件，以免和click事件冲突
				return true;
				// Talpa:bo.yang1 add for restore itemview @{
			}/*else if(!mIsRestore && mPreControlLayout!=null && mPreControlLayout.getTranslationY() != 0
					&& ((mPreOperatorLayout!=mOperatorLayout )
					*//*|| (touchSlopY < mTouchSlop && touchSlopX > mTouchSlop
					&& mPreOperatorLayout==mOperatorLayout)*//*)){
				reStoreItemView();

				//@}
			}*/else if(mIsLRSlop){
				mIsLRSlop=false;
				reStoreItemView();
				//@}
			}
		}
		return super.onInterceptTouchEvent(e);
	}

	/**
	 * 向上滑动
	 */
	private void scrollUp() {
		removeDirection = RemoveDirection.UP;
		int deltaY = mScreenHeight - mItemView.getControlOffsetY();
		// 调用startScroll方法来设置一些滚动的参数，在computeScroll()方法中调用scrollTo来滚动item
		mScroller.startScroll(0, mItemView.getControlOffsetY(), 0, deltaY,
				REMOVE_ANIM_DURATION/*Math.abs(delta)*/);
		mControlLayout.setAlpha(1.0f);
		postInvalidate(); // 刷新itemView
	}

	/**
	 * 根据手指滚动itemView的距离来判断是滚动到开始位置还是向左或者向右滚动
	 */
	private void scrollByDistanceY() {
		int offsetY = mItemView.getControlOffsetY();
		// 如果向上滚动的距离大于ViewGroup的四分之一，就让其删除
		if (-offsetY >= mControlLayout.getHeight() / 4) {
			scrollUp();
		}
		else{
			// 滚回到原始位置
			if (offsetY > mOperatorSwipeH/2){ // 滑开
				ObjectAnimator.ofFloat(mControlLayout,"translationY", mOperatorSwipeH)
						.setDuration(DRAG_BACK_DURATION).start();
				ObjectAnimator.ofFloat(mHeaderLayout,"Alpha", 0)
						.setDuration(DRAG_BACK_DURATION).start();

				// Talpa:bo.yang1 add view action @{
				ObjectAnimator.ofFloat(mMemoryShowTV,"Alpha", 0)
						.setDuration(DRAG_BACK_DURATION).start();
				ObjectAnimator.ofFloat(mClearButton,"Alpha", 0)
						.setDuration(DRAG_BACK_DURATION).start();
				//@}
			}
			else{ // 恢复

				if (mHeaderLayout.getTranslationY() != 0) {
					ObjectAnimator.ofFloat(mHeaderLayout,"translationY", 0)
							.setDuration(DRAG_BACK_DURATION).start();
				}

				if (mHeaderLayout.getAlpha() != 1.0f){
					ObjectAnimator.ofFloat(mHeaderLayout,"Alpha", 1.0f)
							.setDuration(DRAG_BACK_DURATION).start();
				}

				// Talpa:bo.yang1 add view action @{
				if (mClearButton.getAlpha() != 1.0f){
					ObjectAnimator.ofFloat(mClearButton,"Alpha", 1.0f)
							.setDuration(DRAG_BACK_DURATION).start();
				}
				if (mMemoryShowTV.getAlpha() != 1.0f){
					ObjectAnimator.ofFloat(mMemoryShowTV,"Alpha", 1.0f)
							.setDuration(DRAG_BACK_DURATION).start();
				}
				//@}

				if (mControlLayout.getTranslationY() != 0) {
					ObjectAnimator.ofFloat(mControlLayout,"translationY", 0)
							.setDuration(DRAG_BACK_DURATION).start();
				}

				mItemView.setControlOffsetY(0);
			}
			mOperatorLayout.setVisibility(View.VISIBLE);
		}
	}

	/**
	 * 处理我们拖动ListView item的逻辑
	 */
	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		if (mIsSlide && mSlidePosition != AdapterView.INVALID_POSITION
				&& mItemView != null) {
			requestDisallowInterceptTouchEvent(true);
			addVelocityTracker(ev);
			final int action = ev.getAction();
			switch (action) {
				case MotionEvent.ACTION_DOWN:
					break;
				case MotionEvent.ACTION_MOVE:
					float y = ev.getY();

					MotionEvent cancelEvent = MotionEvent.obtain(ev);
					cancelEvent
							.setAction(MotionEvent.ACTION_CANCEL
									| (ev.getActionIndex() << MotionEvent.ACTION_POINTER_INDEX_SHIFT));
					onTouchEvent(cancelEvent);

					if (!mIsInMultiWindowMode) {
						float deltaY = y - mDownY;

						deltaY = scrollControlOffset(deltaY);

						//mItemView.setControlOffsetY(mItemView.getControlOffsetY() + (int)deltaY); //bo.yang1 转移到scrollControlOffset处理
					}

//					if (!isDragDownEdge(scrollY, deltaY)){
//
//						// 向上滚动加透明效果（最好是滚动越大越透明，待改进）
////					if (scrollY > 0)
////						mItemView.setAlpha(0.7f);
//					}


					mDownY = y;

					return true; // 拖动的时候ListView不滚动
				case MotionEvent.ACTION_UP:
					int velocityX = getScrollVelocity();
					// 先检测手势速率，再检测是否在删除的距离范围
					if (velocityX < -SNAP_VELOCITY) {
						scrollUp();
					} else {
						scrollByDistanceY();
					}

					recycleVelocityTracker();
					// 手指离开的时候就不响应左右滚动
					mIsSlide = false;

					// Talpa:bo.yang1 add save last down itemview @{
					mIsMidPos=false;
					mIsRestore=false;
					mPreItemView=mItemView;
					mPreHeaderLayout=mHeaderLayout;
					mPreControlLayout=mControlLayout;
					mPreOperatorLayout=mOperatorLayout;
					//@}
					break;
			}
		}

		// 否则直接交给ListView来处理onTouchEvent事件
		return super.onTouchEvent(ev);
	}


    @Override
    public void computeScroll() {
        // 调用startScroll的时候scroller.computeScrollOffset()返回true，
        if (mScroller.computeScrollOffset()) {
            // 让ListView item根据当前的滚动偏移量进行滚动
			mItemView.scrollTo(mScroller.getCurrX(), mScroller.getCurrY());

            postInvalidate();

            // 滚动动画结束的时候调用回调接口
            if (mScroller.isFinished()) {
                if (mRemoveListener == null) {
                    throw new NullPointerException(
                            "RemoveListener is null, we should called setRemoveListener()");
                }

				/**
				 * reset value status
				 */
				ObjectAnimator.ofFloat(mHeaderLayout,"translationY", 0)
						.setDuration(0).start();
				ObjectAnimator.ofFloat(mControlLayout,"translationY", 0)
						.setDuration(0).start();
				mItemView.setControlOffsetY(0);
				mItemView.scrollTo(0, 0);
				mItemView.setAlpha(0f);

                mRemoveListener.removeItem(mItemView, mSlidePosition);
            }
        }
    }


    /**
     * 判断是否为拖动的下边缘,返回实际能滚动的距离
     * @return
     */
	private float scrollControlOffset(float deltaY){
		float actualDeltaY = deltaY;

		float offset = mItemView.getControlOffsetY() + deltaY;
		if (offset < 0) {  // 上滑删除状态
			// Talpa bo.yang1 add for downed view is uping return store view @{
			if(mIsItemOperateMode || mItemView.getControlOffsetY() > 0){
				if(mItemView.getControlOffsetY() > 0){
					mIsItemOperateMode=true;
				}
				ObjectAnimator.ofFloat(mControlLayout, "translationY", 0)
						.setDuration(0).start();
				mItemView.setControlOffsetY(0);

			}else {
				//@}
				ObjectAnimator.ofFloat(mHeaderLayout, "translationY", offset)
						.setDuration(0).start();

				mIsItemOperateMode = false;
				mOperatorLayout.setVisibility(View.INVISIBLE);

				// Talpa:bo.yang1 add 单独处理方便两端itemview @{
				ObjectAnimator.ofFloat(mControlLayout, "translationY", mItemView.getControlOffsetY() + actualDeltaY)
						.setDuration(0).start();
				mItemView.setControlOffsetY(mItemView.getControlOffsetY() + (int) deltaY);
				//@}
			}
		} else {

			if(mIsMidPos || (!mIsMidPos && offset < mOperatorSwipeH/2)) {
				if (!mIsItemOperateMode) { // 前一个状态为上滑状态
					// 恢复 mHeaderLayout位置
					ObjectAnimator.ofFloat(mHeaderLayout, "translationY", 0)
							.setDuration(0).start();
					mHeaderLayout.setAlpha(1.0f);

					// Talpa:bo.yang1 add view action @{
					mMemoryShowTV.setAlpha(1.0f);
					mClearButton.setAlpha(1.0f);
					//@}
					mOperatorLayout.setVisibility(View.VISIBLE);
				}

				if (offset > mOperatorSwipeH) {
					actualDeltaY = mOperatorSwipeH - mItemView.getControlOffsetY();
				}

				float alphay = 1 - (mItemView.getControlOffsetY() + actualDeltaY) / mOperatorSwipeH;
				mHeaderLayout.setAlpha(alphay/*1 - (mItemView.getControlOffsetY() + actualDeltaY) / mOperatorSwipeH*/);
				// Talpa:bo.yang1 add view action @{
				if(mIsMidPos) {
					mMemoryShowTV.setAlpha(alphay);
					mClearButton.setAlpha(alphay);
				}
				//@}
				mIsItemOperateMode = true;

				// Talpa:bo.yang1 add 单独处理方便两端itemview @{
				ObjectAnimator.ofFloat(mControlLayout,"translationY", mItemView.getControlOffsetY() + actualDeltaY)
						.setDuration(0).start();
				// Talpa:bo.yang1 modify for Decline in insensitive in downed view @{
				mItemView.setControlOffsetY(mItemView.getControlOffsetY() + (int) /*deltaY*/actualDeltaY);
				//@}
			}
		}

		//ObjectAnimator.ofFloat(mControlLayout,"translationY", mItemView.getControlOffsetY() + actualDeltaY)
		//		.setDuration(0).start();
		return actualDeltaY;
	}

	/**
	 * 添加用户的速度跟踪器
	 *
	 * @param event
	 */
	private void addVelocityTracker(MotionEvent event) {
		if (mVelocityTracker == null) {
			mVelocityTracker = VelocityTracker.obtain();
		}

		mVelocityTracker.addMovement(event);
	}

	/**
	 * 移除用户速度跟踪器
	 */
	private void recycleVelocityTracker() {
		if (mVelocityTracker != null) {
			mVelocityTracker.recycle();
			mVelocityTracker = null;
		}
	}

	/**
	 * 获取X方向的滑动速度,大于0向右滑动，反之向左
	 *
	 * @return
	 */
	private int getScrollVelocity() {
		mVelocityTracker.computeCurrentVelocity(1000);
		int velocity = (int) mVelocityTracker.getYVelocity();
		return velocity;
	}

	/**
	 *
	 * 当ListView item滑出屏幕，回调这个接口 我们需要在回调方法removeItem()中移除该Item,然后刷新ListView
	 *
	 *
	 */
	public interface RemoveListener {
		void removeItem(View itemView, int position);
	}

	public static enum Orientation {
		HORIZONTAL(0), VERTICAL(1);

		private int value;

		private Orientation(int i) {
			value = i;
		}

		public int value() {
			return value;
		}

		public static Orientation valueOf(int i) {
			switch (i) {
				case 0:
					return HORIZONTAL;
				case 1:
					return VERTICAL;
				default:
					throw new RuntimeException("[0->HORIZONTAL, 1->VERTICAL]");
			}
		}
	}


	@Override
	public boolean fling(int velocityX, int velocityY) {
		velocityX = solveVelocity(velocityX);
		velocityY = solveVelocity(velocityY);
		return super.fling(velocityX, velocityY);
	}

	private int solveVelocity(int velocity) {
		if (velocity > 0) {
			return Math.min(velocity, FLING_MAX_VELOCITY);
		} else {
			return Math.max(velocity, -FLING_MAX_VELOCITY);
		}
	}

	public void setMultiWindowMode(boolean isInMultiWindowMode){
		mIsInMultiWindowMode = isInMultiWindowMode;
	}

	// Talpa:bo.yang1 add @{
	/**
	 *
	 * 找出清理和显示内存控件
	 *
	 *
	 */
	private void findMemoryTvAndClearBtn(View mItemView){
		ViewGroup parentView=(ViewGroup) mItemView.getParent();

		while (parentView.getId()!=R.id.recents_view){
			parentView=(ViewGroup)parentView.getParent();
		}
		parentView=(ViewGroup)parentView.getParent();
		for(int i=0; i < parentView.getChildCount(); i++){
			View child = parentView.getChildAt(i);
			if(child.getId()==R.id.memory_show){
				mMemoryShowTV=child;
			}else if(child.getId()==R.id.clear_button){
				mClearButton=child;
			}
		}
	}

	private void reStoreItemView(){
		if (mPreHeaderLayout.getTranslationY() != 0) {
			ObjectAnimator.ofFloat(mPreHeaderLayout, "translationY", 0)
					.setDuration(DRAG_BACK_DURATION).start();

			ObjectAnimator.ofFloat(mMemoryShowTV, "translationY", 0)
					.setDuration(DRAG_BACK_DURATION).start();
			ObjectAnimator.ofFloat(mClearButton, "translationY", 0)
					.setDuration(DRAG_BACK_DURATION).start();
		}

		if (mPreHeaderLayout.getAlpha() != 1.0f) {
			ObjectAnimator.ofFloat(mPreHeaderLayout, "Alpha", 1.0f)
					.setDuration(DRAG_BACK_DURATION).start();

			ObjectAnimator.ofFloat(mMemoryShowTV, "Alpha", 1.0f)
					.setDuration(DRAG_BACK_DURATION).start();
			ObjectAnimator.ofFloat(mClearButton, "Alpha", 1.0f)
					.setDuration(DRAG_BACK_DURATION).start();
		}


		ObjectAnimator.ofFloat(mPreControlLayout, "translationY", 0)
				.setDuration(DRAG_BACK_DURATION).start();


		mPreItemView.setControlOffsetY(0);
		mIsRestore=true;
	}

	//@}
}
