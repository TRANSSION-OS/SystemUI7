package itel.transsion.systemui.View;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

import com.android.systemui.R;

/**
 * Created by yuanchang.liu on 2017/3/13.
 */

public class NotificationLineView extends View{

    private int mWidth;
    private int mHeight;
    // 0xff282828 ,  0xff4c4c4c
    private final ColorDrawable mDrawable = new ColorDrawable(0xff282828);
    private final ColorDrawable mSecondDrawable = new ColorDrawable(0xff4c4c4c);

    public NotificationLineView(Context context) {
        this(context,null);
    }

    public NotificationLineView(Context context, AttributeSet attrs) {
        this(context, attrs,0);
    }

    public NotificationLineView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr,0);
    }

    public NotificationLineView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
       // TypedArray.ta = context.obtainStyledAttributes(attrs, R.styleable.notification_line);

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int widthMode=MeasureSpec.getMode(widthMeasureSpec);
        int widthSize=MeasureSpec.getSize(widthMeasureSpec);
        int heightMode=MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        switch (widthMode){
            case MeasureSpec.AT_MOST:
            case MeasureSpec.EXACTLY:
                mWidth=widthSize;
                break;
            case MeasureSpec.UNSPECIFIED:
                break;
        }
        switch (heightMode){
            case MeasureSpec.AT_MOST:
            case MeasureSpec.EXACTLY:
                mHeight=heightSize;
                break;
        }
        setMeasuredDimension(mWidth,mHeight);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        mDrawable.setBounds(0, 0, mWidth,  mHeight);
        mDrawable.draw(canvas);
        mSecondDrawable.setBounds(0, mHeight/2, mWidth,  mHeight);
        mSecondDrawable.draw(canvas);
    }

}
