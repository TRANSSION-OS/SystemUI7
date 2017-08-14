/*
 * Copyright (C) 2016 The Android Open Source Project
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
package com.android.systemui.recents.views;

import android.animation.Animator;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewDebug;
import android.view.ViewGroup;

import com.android.systemui.R;
import com.android.systemui.recents.Recents;
import com.android.systemui.recents.events.EventBus;
import com.android.systemui.recents.events.activity.LaunchTalpaTaskEvent;
import com.android.systemui.recents.events.ui.DeleteTaskDataEvent;
import com.android.systemui.recents.model.Task;
import com.android.systemui.recents.model.TaskStack;
import com.android.systemui.recents.talpa.CardAdapterHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static android.app.ActivityManager.StackId.INVALID_STACK_ID;

public class TaskTalpaHorizontalListAdapter extends
        RecyclerView.Adapter<TaskTalpaHorizontalListAdapter.ViewHolder> {
    @ViewDebug.ExportedProperty(category="recents")
    private int mDisplayOrientation = Configuration.ORIENTATION_PORTRAIT;
    // The current display bounds
    @ViewDebug.ExportedProperty(category="recents")
    private Rect mDisplayRect = new Rect();

    //Full class name is 30 characters
    private static final String TAG = "TaskStackViewAdapter";

    // mTaskStack 更新的时候 mTaskList也要对应更新
    private TaskStack mTaskStack;
    private List<Task> mTaskList;
    private int mItemWidth;
    private int mItemThumbHeight;
    private int mContainerWidth;
    private int mItemMargin;

    private CardAdapterHelper mCardAdapterHelper = new CardAdapterHelper();



    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        private TaskTalpaView mTaskTalpaView;
        private Task mTask;
        public ViewHolder(View v) {
            super(v);
            mTaskTalpaView = (TaskTalpaView) v;
        }

        public void bindTaskView(TaskTalpaView tv, Task task) {
            mTask = task;

            // Rebind the task and request that this task's data be filled into the TaskView
            // 第二个参数意义待研究 comment by DepingHuang
            tv.onTaskBound(task, true, mDisplayOrientation, mDisplayRect);

            // Talpa:peterHuang comment  for task view handle click@{
            //tv.setOnClickListener(this);
            //@}

        }
        public void unbindTaskView(TaskTalpaView tv){
            tv.onTaskDataUnloaded();
        }


        // 处理Item的单击事件
        @Override
        public void onClick(View v) {
            try {
                    EventBus.getDefault().send(new LaunchTalpaTaskEvent(mTaskTalpaView, mTask,
                            null, INVALID_STACK_ID, false));
            } catch (Exception e) {
                Log.e(TAG, v.getContext()
                        .getString(R.string.recents_launch_error_message, mTask.title), e);
            }

        }

        private Animator.AnimatorListener getRemoveAtListener(final int position,
                                                              final Task task) {
            return new Animator.AnimatorListener() {

                @Override
                public void onAnimationStart(Animator animation) { }

                @Override
                public void onAnimationEnd(Animator animation) {
                    removeTask(task);
                    EventBus.getDefault().send(new DeleteTaskDataEvent(task));
                }

                @Override
                public void onAnimationCancel(Animator animation) { }

                @Override
                public void onAnimationRepeat(Animator animation) { }
            };

        }
    }

    public TaskTalpaHorizontalListAdapter(TaskStack taskStack, int containerWidth,
                                          int itemWidth, int itemThumbHeight, int itemMargin) {
        mTaskStack = taskStack;
        mItemWidth = itemWidth;
        mItemThumbHeight = itemThumbHeight;
        mContainerWidth = containerWidth;
        mItemMargin = itemMargin;

        List<Task> tasks = taskStack.getStackTasks();
        // 反转，最后加入的最新显示
        Collections.reverse(tasks);
        mTaskList = new ArrayList<Task>(tasks);
    }

    public void setDisplayRect(Rect displayRect){
        mDisplayRect = displayRect;
    }

    public void setItemWidth(int itemWidth){
        mItemWidth = itemWidth;
    }
    public int getItemWidth(){
        return mItemWidth;
    }
    public void setItemThumbHeight(int thumbHeight){
        mItemThumbHeight = thumbHeight;
    }
    public void setContainerWidth(int containerWidth){
        mContainerWidth = containerWidth;
    }
    public void setItemMargin(int itemMargin){
        mItemMargin = itemMargin;
    }

    public void setNewStackTasks(TaskStack taskStack) {
        mTaskStack = taskStack;
        List<Task> tasks = taskStack.getStackTasks();
        // 反转，最后加入的最新显示
        Collections.reverse(tasks);

        mTaskList.clear();
        mTaskList.addAll(tasks);

        notifyDataSetChanged();
    }

    @Override
    public TaskTalpaHorizontalListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                                        int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        TaskTalpaView taskView = (TaskTalpaView) inflater.inflate(R.layout.recents_talpa_task_view, parent,  false);

        mCardAdapterHelper.onCreateViewHolder(mContainerWidth, mItemWidth, mItemThumbHeight, mItemMargin, taskView);
        return new ViewHolder(taskView);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Task task = mTaskList.get(position);
        // Retrives from caches, loading only if necessary
        holder.bindTaskView((TaskTalpaView) holder.itemView,task);

        // loadTaskData应该在bindTask之后，loadTaskData中会通知绑定的UI更新。这里完成一个异步更新的机制
        // 对应tv中的loadTaskData是在init之前，原因未知。
        Recents.getTaskLoader().loadTaskData(task);

        mCardAdapterHelper.onBindViewHolder(holder.itemView, position, getItemCount());
    }

    @Override
    public void onViewRecycled(ViewHolder holder) {
        super.onViewRecycled(holder);
        holder.unbindTaskView((TaskTalpaView) holder.itemView);
    }

    @Override
    public int getItemCount() {
        return mTaskList.size();
    }

    public void removeTask(Task task) {
        int position = mTaskList.indexOf(task);
        if (position >= 0) {
            mTaskList.remove(position);

            notifyItemRemoved(position);
            if(position != mTaskList.size()){ // 如果移除的是最后一个，忽略
                notifyItemRangeChanged(position, mTaskList.size() - position);
                // Talpa:bo.yang1 如果移除的是最后一个则刷新最后一个 @{
            }else{
                notifyItemChanged(mTaskList.size()-1);
                //@}
            }

            mTaskStack.removeTask(task, AnimationProps.IMMEDIATE,
                        false);


            //notifyDataSetChanged();
        }
    }

    public int getPositionOfTask(Task task) {
        int position = mTaskList.indexOf(task);
        return (position >= 0) ? position : 0;
    }



    public void addTaskAt(Task task, int position) {
        mTaskList.add(position, task);
        notifyItemInserted(position);
    }
}
