package com.android.systemui.usb;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.storage.VolumeInfo;
import android.util.Log;

import com.android.systemui.SystemUIApplication;
import com.android.systemui.statusbar.phone.PhoneStatusBar;

public class BrowseReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent targetIntent =intent.getParcelableExtra("intent");
        VolumeInfo info =intent.getParcelableExtra("volumeinfo");
        if (info.isMountedReadable()) {
            SystemUIApplication app = (SystemUIApplication) context.getApplicationContext();
            PhoneStatusBar statusBar = app.getComponent(PhoneStatusBar.class);
            statusBar.animateCollapsePanels();
            context.startActivity(targetIntent);
        }
    }
}
