package com.jinhaihan.qqnotfandshare;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.Objects;

/**
 * 系统启动完成广播接收器
 * #<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
 * @author Lone_Wolf
 */
public class BootCompletedReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Objects.equals(intent.getAction(), Intent.ACTION_BOOT_COMPLETED)) {
            //example:启动程序
            Intent start = new Intent(context, NotificationMonitorService.class);
            context.startService(start);
        }
    }
}