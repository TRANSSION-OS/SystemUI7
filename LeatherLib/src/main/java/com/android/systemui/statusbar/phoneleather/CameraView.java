package com.android.systemui.statusbar.phoneleather;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.Size;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;

import com.android.systemui.statusbar.phoneleather.util.LeatherUtil;

import java.util.ArrayList;
import java.util.List;

import itel.transsion.settingslib.utils.LogUtil;

public class CameraView {
    private ViewGroup mView;
    private Context mActivity;

    private ImageButton mBtnTakePhoto;
    private SurfaceView mSurfaceView;
    private View mPreview;
    private View mTakeBgView;

    private Camera mCamera;
    private Parameters mParameters;
    private CameraInfo mCameraInfo;

    private int mScreenWidth;
    private int mScreenHeight;

    private Handler mHandler = new Handler();

    private Runnable mAutoFocusRunnable = new Runnable() {
        public void run() {
            if(mCamera != null) {
                safeAutoFocus();
            }
        }
    };

    private Runnable mShowRunnable = new Runnable() {
        @Override
        public void run() {
            if(mSurfaceView.getVisibility() != View.VISIBLE) {
                mBtnTakePhoto.setVisibility(View.VISIBLE);
                mPreview.setVisibility(View.VISIBLE);
                mSurfaceView.setVisibility(View.VISIBLE);
            }
        }
    };

    private Runnable mHideRunnable =  new Runnable() {
        @Override
        public void run() {
            if(mSurfaceView.getVisibility() != View.GONE) {
                mSurfaceView.setVisibility(View.GONE);
                mBtnTakePhoto.setVisibility(View.GONE);
            }
        }
    };
    
    private AutoFocusCallback mAutoFocusCallback =  new AutoFocusCallback() {
        @Override
        public void onAutoFocus(boolean success, Camera camera) {
            if (success) { 
                mCamera.cancelAutoFocus(); 
            }
        }
    };
    
    public CameraView(Context mActivity, ViewGroup mView) {
        this.mActivity = mActivity;
        this.mView = mView;
        initCameraView();
        initCameraEvent();
    }

    private void initCameraView() {
        mBtnTakePhoto = (ImageButton) mView.findViewById(R.id.leather_camera_takepic);
        mSurfaceView = (SurfaceView) mView.findViewById(R.id.cameraSurfaceView);
        mPreview = mView.findViewById(R.id.leather_camera_preview);
        mTakeBgView = mView.findViewById(R.id.leather_camera_take_bg);
        DisplayMetrics dm = mActivity.getResources().getDisplayMetrics();
        initScreenSize(dm.widthPixels, dm.heightPixels);
    }

    private void initCameraEvent() {

        mSurfaceView.getHolder().setKeepScreenOn(true);
        mSurfaceView.getHolder().addCallback(new SurfaceCallback());
        mSurfaceView.getHolder().setFormat(PixelFormat.TRANSPARENT);

        mBtnTakePhoto.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
            	if (mCamera != null) {
                    mBtnTakePhoto.setEnabled(false);
                    mCamera.takePicture(new LeatherShutterCallback(), null, new LeatherPictureCallback());
                }
            }
        });
    }

    private final class SurfaceCallback implements SurfaceHolder.Callback {

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            LogUtil.d("surfaceCreated");
            int nNum = Camera.getNumberOfCameras();
            if (nNum == 0) {
                return;
            }

            for (int i = 0; i < nNum; i++) {
                CameraInfo info = new CameraInfo(); 
                Camera.getCameraInfo(i, info);
                if (info.facing == CameraInfo.CAMERA_FACING_BACK) {
                    startPreview(info, i, holder); 
                    return;
                }
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width,
                int height) {
            LogUtil.d("surfaceChanged");
            if (mCamera == null) {
                return;
            }
            safeAutoFocus();
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            LogUtil.d("surfaceDestroyed Camera release");
            mPreview.setVisibility(View.VISIBLE);
            closeCamera();
        }

    }

    private Size getOptimalSize(int nDisplayWidth, int nDisplayHeight,
            List<Size> sizes, double targetRatio) {
        final double ASPECT_TOLERANCE = 0.001;
        if (sizes == null)
            return null;

        Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = Math.min(nDisplayWidth, nDisplayHeight);
        for (Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE)
                continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }

    public void show() {
        removeCallbacks();
        mHandler.postDelayed(mShowRunnable, 500);
    }

    public void hide() {
        removeCallbacks();
        mHandler.postDelayed(mHideRunnable, 1000);
    }

    public void postHide() {
        removeCallbacks();
        mHandler.post(mHideRunnable);
    }

    private void removeCallbacks() {
        mHandler.removeCallbacks(mShowRunnable);
        mHandler.removeCallbacks(mHideRunnable);
    }

    public final ViewGroup getViewGroup() {
        return mView;
    }

    public void initScreenSize(int nWidth, int nHeight) { 
        ViewGroup.LayoutParams lp = mSurfaceView.getLayoutParams();
        lp.width = nWidth;
        lp.height = nHeight;
        mSurfaceView.setLayoutParams(lp);

        mScreenWidth = nWidth;
        mScreenHeight = nHeight;
    }

    public void startPreview(CameraInfo info, int cameraId, SurfaceHolder holder) {
        try {

            mCameraInfo = info;

            mCamera = Camera.open(cameraId);

            mCamera.setPreviewDisplay(holder);
            mCamera.setPreviewCallback(new Camera.PreviewCallback() {
                @Override
                public void onPreviewFrame(byte[] bytes, Camera camera) {
                    if(mPreview.getVisibility() == View.VISIBLE) {
                        mPreview.animate().alpha(0f).setDuration(100).withEndAction(new Runnable() {
                            @Override
                            public void run() {
                                mPreview.setVisibility(View.GONE);
                                mPreview.setAlpha(1f);
                            }
                        }).start();
                    }
                }
            });
            mCamera.setDisplayOrientation(90); 

            {
                mParameters = mCamera.getParameters();
                
                List<Size> listPictureSizes = mParameters
                        .getSupportedPictureSizes();

                Size sizeOptimalPicture = getOptimalSize(mScreenWidth,
                        mScreenHeight, listPictureSizes, (double) mScreenWidth
                                / mScreenHeight);
                mParameters.setPictureSize(sizeOptimalPicture.width,
                        sizeOptimalPicture.height);

                // PreviewSize
                List<Size> ListPreviewSizes = mParameters
                        .getSupportedPreviewSizes();

                Size sizeOptimalPreview = getOptimalSize(
                        sizeOptimalPicture.width, sizeOptimalPicture.height,
                        ListPreviewSizes, (double) sizeOptimalPicture.width
                                / sizeOptimalPicture.height);
                mParameters.setPreviewSize(sizeOptimalPreview.width,
                        sizeOptimalPreview.height);

                List<String> lstFocusModels = mParameters
                        .getSupportedFocusModes();
                for (String str : lstFocusModels) {
                    if (str.equals(Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                        mParameters
                                .setFocusMode(Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                        break;
                    }
                }

                mCamera.setParameters(mParameters);
            }

            mCamera.startPreview();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void safeAutoFocus() {
        try {
            mCamera.autoFocus(mAutoFocusCallback);
        } catch (RuntimeException e) {
            scheduleAutoFocus(); // wait 1 sec and then do check again
        }
    }

    private void scheduleAutoFocus() {
        mHandler.postDelayed(mAutoFocusRunnable, 1000);
    }

    private final  class LeatherShutterCallback implements Camera.ShutterCallback {

        @Override
        public void onShutter() {
            mTakeBgView.setVisibility(View.VISIBLE);
            mTakeBgView.animate().alpha(0.6f).setDuration(300).withEndAction(new Runnable() {
                @Override
                public void run() {
                    mTakeBgView.animate().alpha(0f).setDuration(300).withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            mBtnTakePhoto.setEnabled(true);
                            mCamera.startPreview();
                            mTakeBgView.setVisibility(View.GONE);
                        }
                    }).start();
                }
            }).start();
        }
    }

    private final class LeatherPictureCallback implements PictureCallback {

        @Override
        public void onPictureTaken(final byte[] data, Camera camera) {

            try {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Bitmap bitmapRotate = BitmapFactory.decodeByteArray(data, 0, data.length);

                        Bitmap mBmp = bitmapRotate;
                        int nDegree = getPictureDegree(mActivity, mCameraInfo);
                        if (nDegree != 0) {
                            Matrix matrix = new Matrix();
                            matrix.preRotate(nDegree);
                            mBmp = Bitmap.createBitmap(bitmapRotate, 0, 0, bitmapRotate.getWidth(), bitmapRotate.getHeight(), matrix, true);
                        }
                        int x = (mBmp.getWidth() - mActivity.getResources().getDimensionPixelSize(R.dimen.leather_menu_radius)) / 2;
                        int y = mActivity.getResources().getDimensionPixelSize(R.dimen.leather_margin_top);
                        int radius = mActivity.getResources().getDimensionPixelSize(R.dimen.leather_menu_radius);
                        mBmp = Bitmap.createBitmap(mBmp, x, y, radius, radius, null, true);
                        LeatherUtil.saveImageToGallery(mActivity, mBmp);
                    }
                }).start();



            } catch (Exception e) {
                e.printStackTrace();
            }
        }

		private int getPictureDegree(Context mActivity, CameraInfo mCameraInfo) {
			return 90;
		}
    }
    
    public void closeCamera() {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.setPreviewCallback(null);
            mCamera.release(); 
            mCamera = null;
            mCameraInfo = null;
        }
    }

    public void pointFocus(int x, int y) {
           if (mParameters.getMaxNumMeteringAreas() > 0) {
                   List<Camera.Area> areas = new ArrayList<Camera.Area>();
                   Rect area1 = new Rect(x - 100, y - 100, x + 100, y + 100);
                   areas.add(new Camera.Area(area1, 600));
                   Rect area2 = new Rect(0, mScreenWidth,0,mScreenHeight); 
                   areas.add(new Camera.Area(area2, 400));
                   mParameters.setMeteringAreas(areas);
           }
           mCamera.cancelAutoFocus();
           mParameters.setFocusMode(Parameters.FOCUS_MODE_AUTO);
           mCamera.setParameters(mParameters);
           mCamera.autoFocus(mAutoFocusCallback);
       }
}