package com.android.systemui.statusbar.phoneleather;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.Region;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceView;

public class CameraSurfaceView extends SurfaceView {

	public CameraSurfaceView(Context context, AttributeSet attrs, int defStyleAttr,
			int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
	}

	public CameraSurfaceView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public CameraSurfaceView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public CameraSurfaceView(Context context) {
		super(context);
	}

	@Override
	public void draw(Canvas canvas) {
		//canvas.drawColor(Color.TRANSPARENT, android.graphics.PorterDuff.Mode.CLEAR);
	    Path path = new Path();  
	    //设置裁剪的圆心，半径  
	    float radius = getResources().getDimension(R.dimen.leather_menu_radius) / 2;
	    float top = getResources().getDimension(R.dimen.leather_margin_top);
	    float x = getWidth() / 2f;
	    float y = top + radius;
	    path.addCircle(x, y, radius, Path.Direction.CCW);  
	    //裁剪画布，并设置其填充方式  
	    canvas.clipPath(path, Region.Op.REPLACE);
	    super.draw(canvas);  
	}
	
}
