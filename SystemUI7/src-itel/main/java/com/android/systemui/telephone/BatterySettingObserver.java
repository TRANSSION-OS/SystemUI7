package com.android.systemui.telephone;

import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by wujia.lin on 2017/4/6.
 */

public class BatterySettingObserver extends ContentObserver {
    public static final String SHOW_PERCENT_SETTING = "status_bar_show_battery_percent";
    // public static final String SHOW_PERCENT_SETTING = "battery_percentage_enabled";

    private Context mContext;
    private List<BatteryCallBack> mCallBacks;

    private boolean mIsRegister;

    public BatterySettingObserver(Context context) {
        super(new Handler());
        mCallBacks = new ArrayList<>();
        mContext = context;
        registerContentObserver();
    }

    @Override
    public void onChange(boolean selfChange, Uri uri) {
        super.onChange(selfChange, uri);
        updateCallBacks();
    }

    public void addBatteryCallBack(BatteryCallBack callback) {
        if(callback != null) {
            if(!mCallBacks.contains(callback)) {
                mCallBacks.add(callback);
            }
            callback.onChange(getState(mContext));
        }
    }

    public void removeBatteryCallBack(BatteryCallBack callback) {
        if(callback != null) {
            if(mCallBacks.contains(callback)) {
                mCallBacks.remove(callback);
            }
        }
    }

    public void registerContentObserver() {
        if(!mIsRegister) {
            mContext.getContentResolver().registerContentObserver(
                    Settings.System.getUriFor(SHOW_PERCENT_SETTING), false, this);
            mIsRegister = true;
            updateCallBacks();
        }
    }

    public void unregisterContentObserver() {
        if(mIsRegister) {
            mContext.getContentResolver().unregisterContentObserver(this);
            mIsRegister = false;
        }
    }

    private void updateCallBacks() {
        boolean isOpen = getState(mContext);
        for (int i = 0; i < mCallBacks.size(); i++) {
            mCallBacks.get(i).onChange(isOpen);
        }
    }

    public static boolean getState(Context context) {
        //linwujia edit begin
        int state = Settings.System.getInt(context.getContentResolver(),
                SHOW_PERCENT_SETTING, 1);//linwujia edit 与settings同步，将默认值修改为1
        Log.d("BatterySettingObserver", "state:" + state);
        //linwujia edit end
        return state == 1;
    }

    public interface BatteryCallBack {
        void onChange(boolean isOpen);
    }
}
