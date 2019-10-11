package com.jinhaihan.qqnotfandshare.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Environment;
import android.text.TextUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by acaoa on 2017/8/31.
 */

public class FileUtils {
    public static File saveUriToCache(Context context, Uri uri, String uniqueName, boolean delete){
        File file = new File(uri.getPath());
        if(file.exists())
            return file;
        try {
            InputStream input = context.getContentResolver().openInputStream(uri);
            //获取自己数组
            byte[] buffer = new byte[input.available()];
            input.read(buffer);
            String path = getDiskCacheDir(context, uniqueName).getAbsolutePath();
            File fileFolder = new File(path);
            if(fileFolder.exists() && delete)
                fileFolder.delete();
            if (!fileFolder.exists())
                fileFolder.mkdirs();

            file = new File(path, md5(uri.getLastPathSegment()));
            if (!file.exists())
                file.createNewFile();
            OutputStream outStream = new FileOutputStream(file);
            outStream.write(buffer);
            outStream.flush();
            outStream.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return file;
    }

    public static String md5(String string) {
        if (TextUtils.isEmpty(string)) {
            return "";
        }
        MessageDigest md5 = null;
        try {
            md5 = MessageDigest.getInstance("MD5");
            byte[] bytes = md5.digest(string.getBytes());
            String result = "";
            for (byte b : bytes) {
                String temp = Integer.toHexString(b & 0xff);
                if (temp.length() == 1) {
                    temp = "0" + temp;
                }
                result += temp;
            }
            return result;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return string;
    }

    public static File saveBitmapToCache(Context context, Bitmap bmp, String fileName, String uniqueName, boolean delete){
        File file = null;
        try {
            String path = getDiskCacheDir(context, uniqueName).getAbsolutePath();
            File fileFolder = new File(path);
            if(fileFolder.exists() && delete)
                fileFolder.delete();
            if (!fileFolder.exists())
                fileFolder.mkdirs();

            file = new File(path, md5(fileName));
            if (!file.exists())
                file.createNewFile();
            OutputStream outStream = new FileOutputStream(file);
            bmp.compress(Bitmap.CompressFormat.PNG, 100, outStream);
            outStream.flush();
            outStream.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return file;
    }

    public static Bitmap getBitmapFromCache(Context context, String fileName, String uniqueName){
        try{
            File file = new File(getDiskCacheDir(context, uniqueName).getAbsolutePath(), md5(fileName));
            if(file.exists())
                return BitmapFactory.decodeFile(file.getAbsolutePath());
        } catch( Exception e){
            e.printStackTrace();
        }
        return null;
    }

    public static File getDiskCacheDir(Context context, String uniqueName) {
        String cachePath;

        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())
                || !Environment.isExternalStorageRemovable()) {
            cachePath = context.getExternalCacheDir().getPath();
        } else {
            cachePath = context.getCacheDir().getPath();
        }
        return new File(cachePath + File.separator + uniqueName);
    }

    public static Bitmap drawableToBitmap (Drawable drawable) {
        Bitmap bitmap = null;

        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            if(bitmapDrawable.getBitmap() != null) {
                return bitmapDrawable.getBitmap();
            }
        }

        if(drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888); // Single color bitmap will be created of 1x1 pixel
        } else {
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        }

        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }
}
