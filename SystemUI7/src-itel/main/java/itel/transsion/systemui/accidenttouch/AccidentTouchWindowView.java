package itel.transsion.systemui.accidenttouch;

import android.content.Context;
import android.content.res.Configuration;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.systemui.R;

public class AccidentTouchWindowView extends LinearLayout{

	private TextView mTextModel;
	private TextView mTextContent;
	private Context mContext;
	public AccidentTouchWindowView(Context context) {
		this(context, null);
	}
	public AccidentTouchWindowView(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext = context;
		
	}

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		mTextModel = (TextView) findViewById(R.id.accident_touch_model);
		mTextContent = (TextView) findViewById(R.id.accident_touch_content);
		
	}
	
	@Override
	protected void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		mTextModel.setText(getResources().getString(R.string.accident_touch_model));
		mTextContent.setText(getResources().getString(R.string.accident_touch_content));
	}
	
}
