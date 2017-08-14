/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package com.android.systemui.qs.tiles;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.UserHandle;

import com.android.systemui.R;
import com.android.systemui.qs.QSTile;
import com.android.systemui.statusbar.StatusBarState;

/** Quick settings tile: Control Diabolo **/
public class TakeScreenShotTile extends QSTile<QSTile.BooleanState> {
    private static final String TAG = "TakeScreenShotTile";

    private static final int TAKE_SCREEN_SHOT_MESSAGE = 10000;
	//private static GlobalScreenshot mScreenshot;
	private boolean clicked = false;
    
    private int mAudioState = R.drawable.itel_ic_qs_screenshot;//talpa zhw ic_qs_screenshot;
    
    final Object mScreenshotLock = new Object();                                                                                                                                                             
	ServiceConnection mScreenshotConnection = null;

	final Runnable mScreenshotTimeout = new Runnable() {
		@Override public void run() {
			synchronized (mScreenshotLock) {
				if (mScreenshotConnection != null) {
					mContext.unbindService(mScreenshotConnection);
					mScreenshotConnection = null;
				}
			}
		}
	};

    public TakeScreenShotTile(Host host) {
        super(host);
    }

    @Override
	public BooleanState newTileState() {
        return new BooleanState();
    }

    @Override
    public void setListening(boolean listening) {
    }

	@Override
	public CharSequence getTileLabel() {
		return mContext.getString(R.string.take_screenshot);
	}

	@Override
    protected void handleClick() {
    	doClick();
    }
    @Override
    protected void handleLongClick() {
		//itel@andy delete @{
    	/*doClick();*/
		//@}
    }

	@Override
	public Intent getLongClickIntent() {
		return null;
	}

	private final Runnable mScreenshotRunnable = new Runnable() {
        @Override
        public void run() {
            takeScreenshot();
        }
    };
    
    private void doClick() {
    	
//    	if(!clicked){
//    		mHost.startSettingsActivity(new Intent());
//            Message msg = mHandler.obtainMessage(TAKE_SCREEN_SHOT_MESSAGE);
//            mHandler.sendMessageDelayed(msg,1000);
//            clicked = true;
//    	}
    	
//    	mHost.startSettingsActivity(new Intent());
    	if(mHost.getBarState() == StatusBarState.SHADE_LOCKED) {
    		mHandler.postDelayed(new Runnable() {
    			@Override public void run() {
//    				mHost.getStatusBarWindow().showKeyguardView();
    				mHost.getStatusBar().takeToKeyguard();
    			}
    		}, 300);
    	} else {
    		mHost.collapsePanels();
    	}
    	
    	mHandler.removeCallbacks(mScreenshotRunnable);
    	mHandler.postDelayed(mScreenshotRunnable, 1500);
    	
    }
    

    @Override
    protected void handleUpdateState(BooleanState state, Object arg) {
        state.label = mContext.getString(R.string.take_screenshot);
//        state.visible = true;
        state.icon = ResourceIcon.get(mAudioState);
    }

//    Handler mHandler = new Handler() {
//        public void handleMessage(Message msg) {
//            switch (msg.what) {
//            case TAKE_SCREEN_SHOT_MESSAGE:
//				//takeScreenshot(); //此方法也可以截屏，摘自PhoneWindowManager.java类中
//				final Messenger callback = msg.replyTo;
//				if (mScreenshot == null) {
//					mScreenshot = new GlobalScreenshot(mContext);
//				}
//				mScreenshot.takeScreenshot(new Runnable() {
//					@Override public void run() {
//						Message reply = Message.obtain(null, 1);
//						try {
//							if(callback != null){
//								callback.send(reply);
//							}
//					    	
//						}catch(RemoteException e){
//						}
//					}
//				}, msg.arg1 > 0, msg.arg2 > 0);
//				clicked = false;
//				break;                    
//            default:
//                break;
//            }
//        }
//    };

    @Override
    public int getMetricsCategory() {
    	// TODO Auto-generated method stub
        return -1;
    }

	private void takeScreenshot() {
		synchronized (mScreenshotLock) {
			if (mScreenshotConnection != null) {
				return;
			}
		
			ComponentName cn = new ComponentName("com.android.systemui",
					"com.android.systemui.screenshot.TakeScreenshotService");
			Intent intent = new Intent();
			intent.setComponent(cn);
			ServiceConnection conn = new ServiceConnection() {
				@Override
				public void onServiceConnected(ComponentName name, IBinder service) {
					synchronized (mScreenshotLock) {
						if (mScreenshotConnection != this) {
							return;
						}
						Messenger messenger = new Messenger(service);
						Message msg = Message.obtain(null, 1);
						final ServiceConnection myConn = this;
						Handler h = new Handler(mHandler.getLooper()) {
							@Override
								public void handleMessage(Message msg) {
									synchronized (mScreenshotLock) {
										if (mScreenshotConnection == myConn) {
											mContext.unbindService(mScreenshotConnection);
											mScreenshotConnection = null;
											mHandler.removeCallbacks(mScreenshotTimeout);
										}
									}
								}
						};
						msg.replyTo = new Messenger(h);
						msg.arg2 = msg.arg1=0;
						
						try {
							messenger.send(msg);
						} catch (RemoteException e) {
						}
					}
				}
				@Override
					public void onServiceDisconnected(ComponentName name) {}
				
			};
			if (mContext.bindServiceAsUser(
						intent, conn, Context.BIND_AUTO_CREATE, UserHandle.CURRENT)) {
				mScreenshotConnection = conn;
				mHandler.postDelayed(mScreenshotTimeout, 10000);
			}
		}
	}
}
