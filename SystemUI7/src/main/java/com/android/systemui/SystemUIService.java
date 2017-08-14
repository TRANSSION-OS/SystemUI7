/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.systemui;

import android.app.ActivityManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.os.SystemProperties;
import android.provider.Settings;

import java.io.FileDescriptor;
import java.io.PrintWriter;

import com.android.systemui.statusbar.floatkeyview.FloatKeyView;

import android.os.UserHandle;

import itel.transsion.settingslib.utils.TalpaUtils;

public class SystemUIService extends Service {

    //SPRD: add for assistant touch
    FloatKeyView mFloatKeyView;

    @Override
    public void onCreate() {
        super.onCreate();
        ((SystemUIApplication) getApplication()).startServicesIfNeeded();
        if (TalpaUtils.isSPRDPlatform()) {
            // SPRD: Bug 598664  display only for owner
            if (SystemProperties.get("ro.product.assistanttouch").equals("") && UserHandle.myUserId() == UserHandle.USER_OWNER) {
                //SPRD: add for assistant touch @{
                mFloatKeyView = new FloatKeyView(this);
                IntentFilter filter = new IntentFilter();
                filter.addAction("com.android.systemui.FLOATKEY_ACTION_STOP");
                filter.addAction("com.android.systemui.FLOATKEY_ACTION_START");
                filter.addAction("com.android.systemui.FLOATKEY_ACTION_RESTART");
                filter.addAction(Intent.ACTION_USER_SWITCHED);
                this.registerReceiver(floatKeyReceiver, filter);
                if (Settings.Secure.getInt(this.getContentResolver()
                        , Settings.Secure.ASSISTANT_ON, 0) != 0) {
                    mFloatKeyView.addToWindow();
                }
                // @}
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        SystemUI[] services = ((SystemUIApplication) getApplication()).getServices();
        if (args == null || args.length == 0) {
            for (SystemUI ui: services) {
                pw.println("dumping service: " + ui.getClass().getName());
                ui.dump(fd, pw, args);
            }
        } else {
            String svc = args[0];
            for (SystemUI ui: services) {
                String name = ui.getClass().getName();
                if (name.endsWith(svc)) {
                    ui.dump(fd, pw, args);
                }
            }
        }
    }

    /**
     * SPRD: add for assistant touch
     */
    private final BroadcastReceiver floatKeyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mFloatKeyView == null)
                return;
            if (intent.getAction().equals(
            "com.android.systemui.FLOATKEY_ACTION_STOP")) {
                mFloatKeyView.removeFromWindow();
            } else if (intent.getAction().equals(
            "com.android.systemui.FLOATKEY_ACTION_START")) {
                mFloatKeyView.addToWindow();
            } else if (intent.getAction().equals(
            "com.android.systemui.FLOATKEY_ACTION_RESTART")) {
                mFloatKeyView.removeFromWindow();
                mFloatKeyView.addToWindow();
            } else if (intent.getAction().equals((Intent.ACTION_USER_SWITCHED))) {
                if (Settings.Secure.getIntForUser(getContentResolver()
                                           , Settings.Secure.ASSISTANT_ON, 0, ActivityManager.getCurrentUser()) != 0) {
                    mFloatKeyView.addToWindow();
                } else {
                    mFloatKeyView.removeFromWindow();
                }
            }
        }
    };
}

