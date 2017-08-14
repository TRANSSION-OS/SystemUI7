package itel.transsion.systemui.View;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;

import com.android.systemui.R;

/**
 * Created by yuanchang.liu on 2017/3/14.
 */

public class NotificationClearAllViewButton extends ImageView{
    private boolean nowVisibled;
    private int mClearAllHeight;
    private int mClearAllDuration;
    private TranslateAnimation showClearAllButtonAnimation;
    private TranslateAnimation hideClearAllButtonAnimation;

    private void init(){
        mClearAllHeight = getResources().getDimensionPixelSize(R.dimen.notification_clear_all_button_height);
        mClearAllDuration = getResources().getInteger(R.integer.notification_clear_all_button_duration);
        showClearAllButtonAnimation = new TranslateAnimation(0,0,mClearAllHeight,0);
        showClearAllButtonAnimation.setDuration(mClearAllDuration);
        hideClearAllButtonAnimation = new TranslateAnimation(0,0,0,mClearAllHeight);
        hideClearAllButtonAnimation.setDuration(mClearAllDuration);
    }
    public NotificationClearAllViewButton(Context context) {
        super(context);
        init();
    }

    public NotificationClearAllViewButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public NotificationClearAllViewButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public NotificationClearAllViewButton(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        animation(visibility==VISIBLE);
    }
    public boolean isButtonVisibled(){
        return nowVisibled;
    }
    private void animation(boolean visible){
        //Log.i("lych","ClearButton animation visible="+visible);
        if(visible){
            if(!nowVisibled)startAnimation(showClearAllButtonAnimation);
            nowVisibled=true;
        }else{
            if(nowVisibled)startAnimation(hideClearAllButtonAnimation);
            nowVisibled=false;
        }
    }
}
