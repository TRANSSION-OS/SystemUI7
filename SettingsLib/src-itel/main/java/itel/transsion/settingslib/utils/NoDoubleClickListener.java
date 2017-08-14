package itel.transsion.settingslib.utils;

import android.view.View;

import java.util.Calendar;

/**
 * 防止过快点击造成多次事件
 * @author andy
 * @version 1.0.0
 * @date 2017/03/22
 * @time 20:40
 */
public abstract class NoDoubleClickListener implements View.OnClickListener {
    // talpa@andy 2017/4/15 11:03 add:取消延时处理 @{
    public static final int MIN_CLICK_DELAY_TIME = 0;
    // @}
    private long lastClickTime = 0;

    @Override
    public void onClick(View v) {
        long currentTime = Calendar.getInstance().getTimeInMillis();
        if (currentTime - lastClickTime > MIN_CLICK_DELAY_TIME) {
            lastClickTime = currentTime;
            onNoDoubleClick(v);
        }
    }

    protected abstract void onNoDoubleClick(View v);

}
