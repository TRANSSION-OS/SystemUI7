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

package com.android.systemui.recents.events.ui;

import com.android.systemui.recents.events.EventBus;

/**
 * This is sent whenever all the task views in a stack have been dismissed.
 */
public class AllTaskViewsDismissedEvent extends EventBus.Event {

    public final int msgResId;
    // Talpa:bo.yang1 modify for allcleartask add flag @{
    private boolean isAllClearTask;

    public AllTaskViewsDismissedEvent(int msgResId) {
        this.msgResId = msgResId;
    }

    // Talpa:bo.yang1 modify for allcleartask add flag @{
    public AllTaskViewsDismissedEvent(int msgResId, boolean isAllClearTask) {
        this.msgResId = msgResId;
        this.isAllClearTask=isAllClearTask;
    }

    public boolean isAllClearTask() {
        return isAllClearTask;
    }
    //@}
}
