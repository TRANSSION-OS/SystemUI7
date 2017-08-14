/*
 * Copyright (c) 2015 uis
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package itel.transsion.settingslib.utils;

import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.media.ThumbnailUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Drawable工具类
 * @author andy
 * @version 1.0.0
 * @date 2017/04/5
 * @time 17:27
 */
public final class BitmapUtils {
    private static BitmapUtils Instance = new BitmapUtils();
    public static BitmapUtils getInstance(){
        return Instance;
    }
    //缩放,width,height缩放后的宽和高
    public Bitmap zoomBitmap(Bitmap bitmap, int width, int height){
        Bitmap newBitmap = Bitmap.createScaledBitmap(bitmap,width,height,true);
        bitmap = null;
        return newBitmap;
    }

    //缩放,width,height缩放后的宽和高
    public Drawable zoomDrawable(Drawable drawable, int width, int height, Resources res){
        Bitmap newBitmap = Bitmap.createScaledBitmap(drawable2Bitmap(drawable),width,height,true);
        drawable = null;
        return bitmap2Drawable(newBitmap, res); 
    }

    /** 获取bitmap大小 */
    public int getBitmapByte(Bitmap image){
        int size = 0;
        if(null != image) {
            size = image.getHeight() * image.getRowBytes();
        }
        image = null;
        return size;
    }

    /** 获取bitmap大小 */
    public int getBitmapDrawableByte(Drawable drawable){
        int size = 0;
        if(drawable instanceof BitmapDrawable){
            BitmapDrawable bitmapDrawable = (BitmapDrawable)drawable;
            size = getBitmapByte(bitmapDrawable.getBitmap());
            bitmapDrawable = null;
        }else{
            int w = drawable.getIntrinsicWidth();
            int h = drawable.getIntrinsicHeight();
            int per = drawable.getOpacity() != PixelFormat.OPAQUE ? 4 :2;
            size = w*h*per;
        }
        drawable = null;
        return size;
    }

    //获取倒角图片，isSquare=true:获取正方形倒角图片
    public Bitmap getFilletBitmap(Bitmap bitmap, int angle, boolean isSquare) {
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        int color = 0xff424242;
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(color);
        int left = 0;
        int top = 0;
        int right = bitmap.getWidth();
        int bottom = bitmap.getHeight();
        //如果是正方形，图片直接倒角，不进行裁剪
        if(right==bottom){
            isSquare=false;
        }
        if(isSquare) {
            int min=right>bottom?bottom:right;
            left=(bitmap.getWidth()-min)/2;
            top=(bitmap.getHeight()-min)/2;
            right=left+min;
            bottom=top+min;
        }
        Rect rect = new Rect(left,top,right,bottom);
        canvas.drawARGB(0, 0, 0, 0);
        canvas.drawRoundRect(new RectF(rect), angle, angle, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);
        bitmap = null;
        if(isSquare) {
            return clipBitmap(output, left, top, right - left, bottom - top);
        }
        return output;
    }
    public Bitmap getCircleBitmap(Bitmap bitmap){
        return getCircleBitmap(bitmap, 0);
    }
    //获取圆形图片，radus图片半径(0取横宽中小的)
    public Bitmap getCircleBitmap(Bitmap bitmap, int radus){
        Bitmap output = Bitmap.createBitmap( bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        int color = 0xff424242;
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(color);
        Rect rect=new Rect(0, 0,bitmap.getWidth(), bitmap.getHeight());
        float cx=bitmap.getWidth()/2;
        float cy = bitmap.getHeight()/2;
        if(radus>cx || radus>cy){
            radus=0;
        }
        if(radus==0) {
            radus = cx > cy ? (int) cy : (int) cx;
        }
        canvas.drawCircle(cx, cy, radus, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);
        int left=(int)cx-radus;
        int top=(int)cy-radus;
        bitmap = null;
        return clipBitmap(output,left,top,2*radus,2*radus);
    }
    //不同图片裁剪，ratio(true按照输入的width,height裁剪)
    public Bitmap clipBitmap(Bitmap resBitmap, int image_width, int image_height){
        int width=resBitmap.getWidth();
        int height=resBitmap.getHeight();
        int x=0;
        int y=0;
        if(image_width<=width){
            x=(width-image_width)/2;
        }else{
            image_width=width;
        }
        if(image_height<=height){
            y=(height-image_height)/2;
        }else{
            image_height=height;
        }
        return clipBitmap(resBitmap, x, y, image_width, image_height);
    }
    //bitmap 剪切,以x,y为裁剪的起始坐标
    public Bitmap clipBitmap(Bitmap resBitmap, int x, int y, int image_width, int image_height){
        if(x<0 || y<0){return resBitmap;}
        Bitmap bitmap= Bitmap.createBitmap(resBitmap, x, y, image_width, image_height);
        resBitmap = null;
        return bitmap;
    }
    private  int readPictureDegree(String path) {
        int degree  = 0;
        try {
            ExifInterface exifInterface = new ExifInterface(path);
            int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    degree = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    degree = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    degree = 270;
                    break;
            }
        }catch (IOException e) {
            e.printStackTrace();
        }
        return degree;
    }
    public Bitmap rotateBitmap(Bitmap bitmap, int angle){
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        Bitmap resizedBitmap = Bitmap.createBitmap(bitmap, 0, 0,bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        bitmap = null;
        return resizedBitmap;
    }

    //文件流转数组
    private  byte[] toByteArray(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int n = 0;
        while (-1 != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
        }
        byte[] data = output.toByteArray();
        output.flush();
        output.close();
        return data;
    }

    public Bitmap drawable2Bitmap(Drawable drawable){
        Bitmap bitmap = null;
        if(drawable instanceof BitmapDrawable){
            bitmap=((BitmapDrawable)drawable).getBitmap();
        }else {
            int w = drawable.getIntrinsicWidth();
            int h = drawable.getIntrinsicHeight();
            Bitmap.Config config = drawable.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565;
            bitmap = Bitmap.createBitmap(w, h, config);
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, w, h);
            drawable.draw(canvas);
        }
        return bitmap;
    }
    public Drawable bitmap2Drawable(Bitmap bitmap, Resources res){
        return new BitmapDrawable(res,bitmap);
    }
    /**
     * 视频文件缩略图
     * @param path video full path
     * @param width
     * @param height
     * @param kind could be MINI_KIND or MICRO_KIND
     * @return Bitmap
     */
    public Bitmap getVideoThumbnail(String path, int width, int height, int kind) {
        Bitmap bm = null;
        bm = ThumbnailUtils.createVideoThumbnail(path, kind);
        bm = ThumbnailUtils.extractThumbnail(bm, width, height, 2);
        return bm;
    }

    /** bitmap保存到文件 */
    public File saveBitmap2File(Bitmap bitmap, String path, String fileName){
        File saveFile=new File(path,fileName);
        return saveBitmap2File(bitmap, saveFile);
    }

    public File saveBitmap2File(Bitmap bitmap, String fullName){
        File saveFile=new File(fullName);
        return saveBitmap2File(bitmap, saveFile);
    }

    public File saveBitmap2File(Bitmap bitmap, File saveFile){
        try {
            OutputStream os = new FileOutputStream(saveFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os);
            os.flush();
            os.close();
        }catch(IOException io){
            LogUtil.e(io.getMessage());
        }finally {
            return saveFile;
        }
    }
    public File saveBitmap2File(Drawable drawable, File saveFile){
        return saveBitmap2File(drawable2Bitmap(drawable),saveFile);
    }
}