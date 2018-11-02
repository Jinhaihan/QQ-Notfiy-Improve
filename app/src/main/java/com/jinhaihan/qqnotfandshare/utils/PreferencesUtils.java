package com.jinhaihan.qqnotfandshare.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.preference.PreferenceManager;

/**
 * Created by acaoa on 2017/8/24.
 */

public class PreferencesUtils {

    public static String getIconPath(Context context){
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getString("icon_path", "");
    }

    public static Uri getRingtone(Context context){
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        String suri = sp.getString("ringtone", "");
        return suri.isEmpty()? null : Uri.parse(suri);
    }

    public static String getChannelNum(Context context){
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        String num = sp.getString("channel_Num", "");
        return num.isEmpty()? "1" : num;
    }

    public static String getVersion(Context context){
        String versionName="";
        int versionCode=0;
        boolean isApkInDebug = false;
        try {
            PackageInfo pi=context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            versionName = pi.versionName;
            versionCode = pi.versionCode;
            ApplicationInfo info = context.getApplicationInfo();
            isApkInDebug = (info.flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return versionName + "-" + (isApkInDebug? "debug":"release") + "(" + versionCode +")";
    }
}
