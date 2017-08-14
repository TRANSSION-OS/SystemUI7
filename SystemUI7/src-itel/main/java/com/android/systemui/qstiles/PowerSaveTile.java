package com.android.systemui.qstiles;

import android.content.ComponentName;
import android.content.Intent;

import com.android.systemui.R;
import com.android.systemui.qs.QSTile;
import com.android.systemui.statusbar.StatusBarState;

import itel.transsion.settingslib.utils.TalpaUtils;

/**
 * Created by zhengjun.chen on 2017/2/16.
 */

public class PowerSaveTile extends QSTile<QSTile.BooleanState> {

    public PowerSaveTile(Host host) {
        super(host);
    }

    @Override
    public BooleanState newTileState() {
        return new BooleanState();
    }

    @Override
    protected void handleClick() {

        enterPowerSave();

        if(mHost.getBarState() == StatusBarState.SHADE_LOCKED) {
            mHandler.postDelayed(new Runnable() {
                @Override public void run() {
                    mHost.getStatusBarWindow().showKeyguardView();
                }
            }, 260);
        } else {
            //    		mHost.collapseAllPanelsNoAnimate();
            mHost.collapsePanels();
        }
    }

    private void enterPowerSave() {
        Intent intentPowerSave = new Intent();

        if (TalpaUtils.isMTKPlatform()) {
            mContext.sendOrderedBroadcast(new Intent("android.intent.action.itel.SUPER_SAVE_OPEN"), "SuperSave");
            ComponentName componentName = new ComponentName("com.transsion.powersaver",
                    "com.transsion.powersaver.service.OpenSuperSaverService");
            intentPowerSave.setComponent(componentName);
        }else{
            intentPowerSave.setAction("com.android.battery.superpower.dialogservice");
            intentPowerSave.setPackage("com.android.battery");
        }

        mContext.startService(intentPowerSave);
    }

    @Override
    protected void handleUpdateState(BooleanState state, Object arg) {
        state.label = mContext.getString(R.string.qs_power_save);

        //		if(isPowerSave()) {
        //			state.icon = ResourceIcon.get(R.drawable.power_save_on);
        //		} else {
        state.icon = ResourceIcon.get(R.drawable.power_save_off);
        //		}
    }

    @Override
    public int getMetricsCategory() {
        return -1;
    }

    @Override
    public Intent getLongClickIntent() {
        return null;
    }

    @Override
    protected void setListening(boolean listening) {

    }

    @Override
    public CharSequence getTileLabel() {
        return mContext.getString(R.string.qs_power_save);
    }

}
