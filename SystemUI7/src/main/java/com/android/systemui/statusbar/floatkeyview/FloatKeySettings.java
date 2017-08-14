
package com.android.systemui.statusbar.floatkeyview;

import android.app.ActivityManager;
import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;
import android.view.KeyEvent;

import com.android.systemui.R;

/**
 * SPRD: add for assistant touch
 */
public class FloatKeySettings {
    public static final int DEFAULT_SHORTCUT_TOP = KeyEvent.KEYCODE_SEARCH;
    public static final int DEFAULT_SHORTCUT_BOTTOM = KeyEvent.KEYCODE_HOME;
    public static final int DEFAULT_SHORTCUT_LEFT = KeyEvent.KEYCODE_BACK;
    public static final int DEFAULT_SHORTCUT_RIGHT = KeyEvent.KEYCODE_MENU;

    public static String mDefAppTop = null;
    public static String mDefAppBottom = null;
    public static String mDefAppLeft = null;
    public static String mDefAppRight = null;

    private ContentResolver mContentResolver;
    public String[] mAssistent=null;

    public FloatKeySettings(Context context) {
        mAssistent=context.getResources().getStringArray(R.array.touch_assistent_apps);
        mDefAppTop= mAssistent[0];
        mDefAppBottom=mAssistent[1];
        mDefAppLeft=mAssistent[2];
        mDefAppRight=mAssistent[3];
        this.mContentResolver = context.getContentResolver();
    }

    public int getAssistantStatus() {
        return getIntSecure(Settings.Secure.ASSISTANT_ON, 0);
    }

    public int getShortcutTopValue() {
        return getIntSecure(Settings.Secure.ASSISTANT_SHORTCUT_TOP, DEFAULT_SHORTCUT_TOP);
    }

    public int getShortcutBottomValue() {
        return getIntSecure(Settings.Secure.ASSISTANT_SHORTCUT_BOTTOM, DEFAULT_SHORTCUT_BOTTOM);
    }

    public int getShortcutLeftValue() {
        return getIntSecure(Settings.Secure.ASSISTANT_SHORTCUT_LEFT, DEFAULT_SHORTCUT_LEFT);
    }

    public int getShortcutRightValue() {
        return getIntSecure(Settings.Secure.ASSISTANT_SHORTCUT_RIGHT, DEFAULT_SHORTCUT_RIGHT);
    }

    public String getAppTopValue() {
        return getStringSecure(Settings.Secure.ASSISTANT_APP_TOP, mDefAppTop);
    }

    public String getAppBottomValue() {
        return getStringSecure(Settings.Secure.ASSISTANT_APP_BOTTOM,mDefAppBottom);
    }

    public String getAppLeftValue() {
        return getStringSecure(Settings.Secure.ASSISTANT_APP_LEFT, mDefAppLeft);
    }

    public String getAppRightValue() {
        return getStringSecure(Settings.Secure.ASSISTANT_APP_RIGHT,mDefAppRight);
    }

    private int getIntSecure(String name, int def) {
        return Settings.Secure.getIntForUser(mContentResolver, name, def, ActivityManager.getCurrentUser());
    }

    private String getStringSecure(String name, String def) {
        String ret = Settings.Secure.getStringForUser(mContentResolver, name, ActivityManager.getCurrentUser());
        return ret == null ? def : ret;
    }
}
