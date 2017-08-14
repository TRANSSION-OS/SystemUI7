package com.android.systemui.recents.views;

import android.animation.AnimatorSet;
import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;

import com.android.systemui.R;
import com.android.systemui.recents.events.EventBus;
import com.android.systemui.recents.events.ui.AllTaskViewsDismissedEvent;
import com.android.systemui.recents.model.Task;
import com.android.systemui.recents.model.TaskStack;
import com.android.systemui.recents.talpa.SwipeRecyclerView;

import itel.transsion.settingslib.utils.LogUtil;

/**
 * 横向显示Task list
 * Created by deping.Huang  on 2016/12/29
 */
public class TaskTalpaHorizontalListView extends SwipeRecyclerView implements TaskStack.TaskStackCallbacks {
    private static final int ANIMATION_DELAY_MS = 50;
    private static final int MSG_START_RECENT_ROW_FOCUS_ANIMATION = 100;
    private TaskStack mStack;
    private Task mFocusedTask;
    private AnimatorSet mRecentsRowFocusAnimation;


    public TaskTalpaHorizontalListView(Context context) {
        super(context);
    }

    public TaskTalpaHorizontalListView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public TaskTalpaHorizontalListView(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    /**
     * Initializes the grid view.
     * @param stack
     */
    public void init(TaskStack stack) {
        // Set new stack
        mStack = stack;
        if (mStack != null) {
            mStack.setCallbacks(this);
        }
    }

    /**
     * @return Returns the task stack.
     */
    public TaskStack getStack() {
        return mStack;
    }

    /**
     * @return - The focused task.
     */
    public Task getFocusedTask() {
      /*  if (findFocus() != null) {
            mFocusedTask = ((TaskTalpaView)findFocus()).getTask();
        }*/

        TaskTalpaView itemView = getFirstVisibleItemView();
        if (itemView!= null)
            mFocusedTask = itemView.getTask();
        else
            mFocusedTask = null;
        return mFocusedTask;
    }

    /**
     * @param task
     * @return Child view for given task
     */
    public TaskTalpaView getChildViewForTask(Task task) {
        for (int i = 0; i < getChildCount(); i++) {
            TaskTalpaView tv = (TaskTalpaView) getChildAt(i);
            if (tv.getTask() == task) {
                return tv;
            }
        }
        return null;
    }


    /**
     * Starts the Recents row's focus gain animation.
     */
    public void startFocusGainAnimation() {
        for (int i = 0; i < getChildCount(); i++) {
//            TaskCardView v = (TaskCardView) getChildAt(i);
//            if (v.hasFocus()) {
//                v.getViewFocusAnimator().changeSize(true);
//            }
//            v.getRecentsRowFocusAnimationHolder().startFocusGainAnimation();
        }
    }

    /**
     * Starts the Recents row's focus loss animation.
     */
    public void startFocusLossAnimation() {
        for (int i = 0; i < getChildCount(); i++) {
//            TaskCardView v = (TaskCardView) getChildAt(i);
//            if (v.hasFocus()) {
//                v.getViewFocusAnimator().changeSize(false);
//            }
//            v.getRecentsRowFocusAnimationHolder().startFocusLossAnimation();
        }
    }

    // taskStack中有任务添加会回调这里
    @Override
    public void onStackTaskAdded(TaskStack stack, Task newTask) {
        LogUtil.i("onStackTaskAdded..");
        TaskTalpaHorizontalListAdapter adapter = (TaskTalpaHorizontalListAdapter) getAdapter();
        if (null != adapter) {
            adapter.addTaskAt(newTask, stack.indexOfStackTask(newTask));
        }

    }

    @Override
    public void onStackTaskRemoved(TaskStack stack, Task removedTask, Task newFrontMostTask,
                                   AnimationProps animation, boolean fromDockGesture) {
        LogUtil.i( "onStackTaskRemoved..");

        ((TaskTalpaHorizontalListAdapter) getAdapter()).removeTask(removedTask);
        if (mFocusedTask == removedTask) {
            mFocusedTask = null;
        }
        // If there are no remaining tasks, then just close recents
        if (mStack.getStackTaskCount() == 0) {
            boolean shouldFinishActivity = (mStack.getStackTaskCount() == 0);
            if (shouldFinishActivity) {
                    EventBus.getDefault().send(new AllTaskViewsDismissedEvent(fromDockGesture
                            ? R.string.recents_empty_message
                            : R.string.recents_empty_message_dismissed_all));
            }
        }
    }

    @Override
    public void onStackTasksRemoved(TaskStack stack) {
        LogUtil.i( "onStackTasksRemoved..");

        // If there are no remaining tasks, then just close recents
        // Talpa:bo.yang1 modify for allcleartask add true flag @{
        EventBus.getDefault().send(new AllTaskViewsDismissedEvent(
                R.string.recents_empty_message_dismissed_all, true));
    }

    @Override
    public void onStackTasksUpdated(TaskStack stack) {
        LogUtil.i( "onStackTasksUpdated..");
        // Do nothing

    }


    /**
     * 获取列表中第一个可视项
     * @return
     */
    public TaskTalpaView getFirstVisibleItemView(){
        RecyclerView.LayoutManager layoutManager = getLayoutManager();
        int firstVisiblePosition = 0;
        if (layoutManager instanceof LinearLayoutManager) {
            firstVisiblePosition = ((LinearLayoutManager)layoutManager).findFirstVisibleItemPosition();
            //int lastVisiblePosition = ((LinearLayoutManager)layoutManager).findLastVisibleItemPosition();
            //focusPosition = firstVisiblePosition;
        }
        // （当position不可以见时，获取到的为null）
        TaskTalpaView itemView = (TaskTalpaView) layoutManager.findViewByPosition(firstVisiblePosition);
        return itemView;
    }


/*    public void setSelectedPosition(int position) {
        GridLayoutManager layoutManager = (GridLayoutManager) getLayoutManager();
        layoutManager.setSelection(position, 0);
    }*/


}
