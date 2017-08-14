package com.android.systemui.recents.views;

import android.app.ActivityManager;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.ImageButton;

import com.android.systemui.R;
import com.android.systemui.recents.events.EventBus;
import com.android.systemui.recents.events.activity.ToggleRecentsEvent;

/**
 * allclear view for add Progress and animation
 * Created by bo.yang1 on 2017/5/26.
 */

public class CircleProgressTalpaBar extends ImageButton  {

    private Paint paint;
    private int circleColor;
    private int circleProgressColor;
    private int textColor;
    private float textSize;
    private float roundWidth;
    private int max;
    private int progress;
    private boolean textIsDisplayable;
    private int offsetRadius=0;
    private boolean inAnimation  = false;

    public CircleProgressTalpaBar(Context context) {
        this(context, null);
    }

    public CircleProgressTalpaBar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CircleProgressTalpaBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        paint = new Paint();
        TypedArray mTypedArray = context.obtainStyledAttributes(attrs,
                R.styleable.CircleProgressTalpaBar);

        //获取自定义属性和默认值
        circleColor = mTypedArray.getColor(R.styleable.CircleProgressTalpaBar_circleColor, 0xff47d8af);
        circleProgressColor = mTypedArray.getColor(R.styleable.CircleProgressTalpaBar_circleProgressColor, Color.WHITE);
        textColor = mTypedArray.getColor(R.styleable.CircleProgressTalpaBar_textColor, Color.BLUE);
        textSize = mTypedArray.getDimension(R.styleable.CircleProgressTalpaBar_textSize, 40);
        roundWidth = mTypedArray.getDimension(R.styleable.CircleProgressTalpaBar_circleWidth, 3);
        textIsDisplayable = mTypedArray.getBoolean(R.styleable.CircleProgressTalpaBar_textIsDisplayable, true);
        // Talpa bo.yang1 modify for max @{
        ActivityManager am = (ActivityManager) context.getSystemService(
                Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(mi);
        float maxf=(((float)mi.totalMem/1048576L)/(float) 1024);
        max=1024*(int)(Math.ceil(maxf));
        //@}

        mTypedArray.recycle();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        /**
         * 画最外层的大圆环
         */
        int centre = getWidth()/2; //获取圆心的x坐标
        int radius = (int) (centre - roundWidth/2)-offsetRadius; //圆环的半径
        paint.setColor(circleColor); //设置圆环的颜色
        paint.setStyle(Paint.Style.STROKE); //设置空心
        paint.setStrokeWidth(roundWidth); //设置圆环的宽度
        paint.setAntiAlias(true);  //消除锯齿
        canvas.setDrawFilter(new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG));
        canvas.drawCircle(centre, centre, radius, paint); //画出圆环


        /**
         * 画进度百分比
         */
        /*paint.setStrokeWidth(0);
        paint.setColor(textColor);
        paint.setTextSize(textSize);
        paint.setTypeface(Typeface.DEFAULT_BOLD); //设置字体

        int percent = (int)(((float)progress / (float)max) * 100);  //中间的进度百分比，先转换成float在进行除法运算，不然都为0
        float textWidth = paint.measureText(*//*percent + "%"*//*"X");   //测量字体宽度，我们需要根据字体的宽度设置在圆环中间

       /if(textIsDisplayable && percent != 0 && style == STROKE){
            canvas.drawText("X", centre - textWidth / 2, centre + textSize/2, paint); //画出进度百分比
        }*/


        /**
         * 画圆弧 ，画圆环的进度
         */

        //设置进度是实心还是空心
        radius = (int) (centre - roundWidth/2)-offsetRadius;
        paint.setStrokeWidth(roundWidth); //设置圆环的宽度
        paint.setColor(circleProgressColor);  //设置进度的颜色
        RectF oval = new RectF(centre - radius, centre - radius, centre
                + radius, centre + radius);  //用于定义的圆弧的形状和大小的界限


        paint.setStyle(Paint.Style.STROKE);
        canvas.drawArc(oval, -90, 360 * progress / max, false, paint);  //根据进度画圆弧


    }


    public synchronized int getMax() {
        return max;
    }

    /**
     * 设置进度的最大值
     * @param max
     */
    public synchronized void setMax(int max) {
        if(max < 0){
            throw new IllegalArgumentException("max not less than 0");
        }
        this.max = max;
    }

    /**
     * 获取进度.需要同步
     * @return
     */
    public synchronized int getProgress() {
        return progress;
    }

    /**
     * 设置进度，此为线程安全控件，由于考虑多线的问题，需要同步
     * 刷新界面调用postInvalidate()能在非UI线程刷新
     * @param progress
     */
    public synchronized void setProgress(int progress) {
        if(progress <= max){
            this.progress = progress;
            postInvalidate();
        }

    }

    public void setProgress(int progress,boolean withAnim){
        if(progress < 0){
            throw new IllegalArgumentException("progress not less than 0");
        }
        if(withAnim){
            if(inAnimation){
                clearAnimation();
            }
            ProgressAnimation animation = new ProgressAnimation(max-progress);
            this.startAnimation(animation);
            animation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    //EventBus.getDefault().send(new AllTaskViewsDismissedEvent(R.string.recents_empty_message));
                    EventBus.getDefault().send(new ToggleRecentsEvent());
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
            inAnimation = true;
        }else if (progress >= 0) {
            if(progress > max){
                progress = max;
            }
            if(progress <= max){
                this.progress = max-progress;
                postInvalidate();
            }
        }
    }


    public int getCricleColor() {
        return circleColor;
    }

    public void setCricleColor(int cricleColor) {
        this.circleColor = cricleColor;
    }

    public int getCricleProgressColor() {
        return circleProgressColor;
    }

    public void setCricleProgressColor(int cricleProgressColor) {
        this.circleProgressColor = cricleProgressColor;
    }

    public int getTextColor() {
        return textColor;
    }

    public void setTextColor(int textColor) {
        this.textColor = textColor;
    }

    public float getTextSize() {
        return textSize;
    }

    public void setTextSize(float textSize) {
        this.textSize = textSize;
    }

    public float getRoundWidth() {
        return roundWidth;
    }

    public void setRoundWidth(float roundWidth) {
        this.roundWidth = roundWidth;
    }

    public class ProgressAnimation extends Animation {

        float start,end;

        public ProgressAnimation(float end) {
            start = 0;//getProgress();
            this.end = end;
            //时间间隔有点距离才能跑动画．
            //这样可以保证多次动画是匀速的.
            // 动画时间是个二次函数,这样效果比较均匀.
            final float interval = Math.abs(end - start);
            if(interval >= 1)
                setDuration((long) (30*Math.sqrt(interval)));
        }

        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            super.applyTransformation(interpolatedTime, t);
            if(interpolatedTime>0.5)
                setProgress((int)(start + (end - start) * (interpolatedTime-0.5)*2));
            else
                setProgress((int)(end - (end - start) * (interpolatedTime)*2));
            if(interpolatedTime==1){
                inAnimation = false;
            }
        }

        @Override
        public void start() {
            super.start();
            inAnimation = true;
        }

        @Override
        public void cancel() {
            super.cancel();
            inAnimation = false;
        }

        @Override
        public boolean willChangeBounds() {
            return false;
        }
    }


}
