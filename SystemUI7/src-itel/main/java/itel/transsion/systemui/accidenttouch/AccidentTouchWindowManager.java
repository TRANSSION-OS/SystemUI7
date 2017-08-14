package itel.transsion.systemui.accidenttouch;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.database.ContentObserver;
import android.graphics.PixelFormat;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;

import com.android.systemui.R;

import java.lang.reflect.Field;
import java.util.Timer;
import java.util.TimerTask;

import itel.transsion.settingslib.utils.LogUtil;

public class AccidentTouchWindowManager implements SensorEventListener, VolumeKeyEventListener{

	private static final int LIGHT_SENSOR = 5;
    private static final int PROXIMITY_SENSOR = 8;
    private static final float SENSOR_MIN_VALUE = (float) 0.0;
	public static final int ACCIDENT_TOUCH_OPEN = 0;
	public static final int ACCIDENT_TOUCH_CLOSE = 1;
	private WindowManager mWindowManager;
	private View mAccidentTouchWindowView;
    private SensorManager mSensorManager;
    private Sensor mLightSensor, mProximitySensor;
    private WindowManager.LayoutParams mAccidentParams;
    //private boolean isScreenTurnOn;
    private boolean isAccidentWindowViewShow;
   // private boolean isShowAccidentWindow;
    private boolean isSensorShow;
    private AccidentTouchObserver observer;
    
    private boolean isProximity;
	private Context mContext;
	private int mVolumeCount;
	private boolean isStartCount;
	private Timer mTimer;
	private int mMeasuringTimes;
	private CountTimerTask task;
	private boolean isConstantly;
	private int mCompareValue;
	private boolean isRegisterSensor;
	//private boolean isScreenOff;
	public AccidentTouchWindowManager(Context context){
		this.mContext = context;
		isRegisterSensor = false;
		mTimer = new Timer();
		if(mContext != null){
			mWindowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
			mSensorManager = (SensorManager) mContext.getSystemService(mContext.SENSOR_SERVICE);
			mLightSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
	        mProximitySensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
	        observer = new AccidentTouchObserver();
		}
	}

	public boolean isOpen(){

		int result = ACCIDENT_TOUCH_OPEN;
		return result == ACCIDENT_TOUCH_OPEN;
	}
	public void registerTouchSensor(){
		inflateAccidentLayout();
		observer.startObserving();
	}

	private void inflateAccidentLayout(){
		if(mContext == null) return;
        mAccidentTouchWindowView = (LinearLayout) LayoutInflater.from(mContext).inflate(R.layout.keyguard_accident_touch, null);

        mAccidentParams = new WindowManager.LayoutParams();
        mAccidentParams.gravity = Gravity.TOP;
        mAccidentParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
        mAccidentParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ERROR;
        mAccidentParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
        mAccidentParams.flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
				| WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
				| WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
				| WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER ;
       // mAccidentParams.alpha = 0.8f;
        mAccidentParams.screenOrientation = ActivityInfo.SCREEN_ORIENTATION_NOSENSOR;
        mAccidentParams.format = PixelFormat.RGB_565;
        mWindowManager.addView(mAccidentTouchWindowView, mAccidentParams);
        mAccidentTouchWindowView.setVisibility(View.GONE);

	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {

	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		float acc = event.accuracy;  
        //获取强度  
        float lux = event.values[0];  
        
        switch (event.sensor.getType()) {
		case LIGHT_SENSOR:
			Log.d("sensor", "LIGHT_SENSOR lux : " + lux);
			if(lux == SENSOR_MIN_VALUE){
				isSensorShow = true;
			}else{
				isSensorShow = false;
			}
			Log.d("sensor", "isSensorShow : " + isSensorShow);
			break;
		case PROXIMITY_SENSOR:
			Log.d("sensor", "LIGHT_SENSOR lux : " + lux);
			if(lux == SENSOR_MIN_VALUE){
				isProximity = true;
			}else{
				isProximity = false;
			}
			Log.d("sensor", "isProximity : " + isProximity);
			break;
		}
        if(mAccidentTouchWindowView == null) return;
        if(lux != 0.0){
        	Log.d("sensor", "isAccidentWindowViewShow : " + isAccidentWindowViewShow);
        	if(isAccidentWindowViewShow && isRegisterSensor){
				isRegisterSensor = false;
        		mSensorManager.unregisterListener(this);
            	accidentWindowDismiss();
        	}
        }
	}
	
	private void accidentWindowShow(){
		isAccidentWindowViewShow = true;
 		mAccidentTouchWindowView.setVisibility(View.VISIBLE);
	}
	
    public void accidentWindowDismiss(){
    	isAccidentWindowViewShow = false;
		mAccidentTouchWindowView.setVisibility(View.GONE);
		if(isStartCount){
			isStartCount = false;
			task.cancel();
		}
	}
    
    public boolean isShowWindow(){
    	
    	return isAccidentWindowViewShow;
    }
	
	 public void onScreenTurnedOff() {
		 //isScreenOff = true;
		 Log.d("sensor", "onScreenTurnedOff observer.value : " + observer.value);
		 if(observer.value == AccidentTouchObserver.TOUCH_OPEN && !isRegisterSensor){
			 Log.d("sensor", "onScreenTurnedOff sensor register success");
			 isRegisterSensor = true;
			 mSensorManager.registerListener(this, mLightSensor, SensorManager.SENSOR_DELAY_NORMAL);
			 mSensorManager.registerListener(this, mProximitySensor, SensorManager.SENSOR_DELAY_NORMAL);
		}
		accidentWindowDismiss();
	 }

	 public void onScreenTurnedOn() {
		 Log.d("sensor", "onScreenTurnedOn isSensorShow : " + isSensorShow);
		 Log.d("sensor", "onScreenTurnedOn isProximity : " + isProximity);
		 if(/*isScreenOff && */isSensorShow && isProximity){
	        	//isScreenOff = false;
	     		accidentWindowShow();
	     }else{
			 if(isRegisterSensor){
				 isRegisterSensor = false;
				 mSensorManager.unregisterListener(this);
			 }
	     }
	 }

	@Override
	public boolean volumeKeyUp() {
		
		return isAccidentWindowViewShow;
	}
	
	@Override
	public boolean volumeKeyDown() {
		
		if(!isAccidentWindowViewShow){
			return false;
		}
		mVolumeCount++;
		if(!isStartCount){
			isStartCount = true;
			isConstantly = false;
			task = new CountTimerTask();
			mTimer.scheduleAtFixedRate(task, 200, 200);
		}
		if(!isConstantly){
			return true;
		}
		accidentWindowDismiss();
		if(isRegisterSensor){
			isRegisterSensor = false;
			mSensorManager.unregisterListener(this);
		}

		return false;
	}
	
	class CountTimerTask extends TimerTask{
		
		@Override
		public void run() {
			Log.d("sensor", "volumeKeyUp : " + mVolumeCount);
			mMeasuringTimes++;
			if(mVolumeCount <= mCompareValue){
				mMeasuringTimes = 0;
				mCompareValue = 0;
				mVolumeCount = 0;
				isConstantly = false;
			}
			if(mMeasuringTimes == 10){
				mMeasuringTimes = 0;
				mCompareValue = 0;
				mVolumeCount = 0;
				isConstantly = true;
			}
			mCompareValue = mVolumeCount;
		}
	}
	
	 private class AccidentTouchObserver extends ContentObserver {

		 //private Uri ITEL_MISTAKEN_TOUCHING_PROOF = Settings.Global.getUriFor("string"/*Settings.Global.ITEL_MISTAKEN_TOUCHING_PROOF*/);
		 public static final int TOUCH_OPEN = 1;
		 public static final int TOUCH_CLOSE = 0;
		 private Uri touchUri;
		 private String touchProof;
		 public int value = TOUCH_CLOSE;
		 public AccidentTouchObserver() {
			super(new Handler());
			try {
				Class clazz = Settings.class;
				Class[] innerClazz = clazz.getClasses();
				for (Class cls : innerClazz) {
					String name = "android.provider.Settings$Global";
					if(name.equals(cls.getName())){
						Field field =  cls.getField("ITEL_MISTAKEN_TOUCHING_PROOF");
						touchProof = (String) field.get(cls);
						touchUri = Settings.Global.getUriFor(touchProof);
						value = Settings.Global.getInt(mContext.getContentResolver(), touchProof, 0);
						break;
					}
				}

			} catch (Throwable e) {
				LogUtil.d("android.provider.Settings$Global Field fail", e);
			}
			// = Settings.Global.getUriFor("string"/*Settings.Global.ITEL_MISTAKEN_TOUCHING_PROOF*/);

	        }

	        @Override
	        public void onChange(boolean selfChange) {
	            onChange(selfChange, null);
	        }

	        @Override
	        public void onChange(boolean selfChange, Uri uri) {
	           if(touchUri.equals(uri)){

	        	   value = Settings.Global.getInt(mContext.getContentResolver(), touchProof, TOUCH_CLOSE);
	        	   Log.d("sensor", value + "");
	        	   if(value == TOUCH_CLOSE){
	        		   isSensorShow = false;
	        		   isProximity = false;
	        	   }
//	        	   if(value == TOUCH_OPEN){
//	        		   mSensorManager.registerListener(AccidentTouchWindowManager.this, mLightSensor, SensorManager.SENSOR_DELAY_NORMAL);
//	        	       mSensorManager.registerListener(AccidentTouchWindowManager.this, mProximitySensor, SensorManager.SENSOR_DELAY_NORMAL);
//	        	   }else{
//	        		   mSensorManager.unregisterListener(AccidentTouchWindowManager.this);
//	        	   }
	           }
	        }

	        public void startObserving() {
				if(touchUri == null) return;
	            final ContentResolver cr = mContext.getContentResolver();
	            cr.unregisterContentObserver(this);
	            cr.registerContentObserver(touchUri, false, this, UserHandle.USER_ALL);
	        }

	        public void stopObserving() {
	            final ContentResolver cr = mContext.getContentResolver();
	            cr.unregisterContentObserver(this);
	        }

	    }

}
