package com.jinhaihan.qqnotfandshare;

import android.accessibilityservice.AccessibilityService;
import android.app.Application;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

public class AccessibilityMonitorService extends AccessibilityService {


    @Override
    protected void onServiceConnected() {
        //辅助服务被打开后 执行此方法
        super.onServiceConnected();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        int eventType = event.getEventType();
        switch (eventType) {
            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
            {
                if(event.getPackageName() == null || event.getClassName() == null)
                    return;
                int tag = NotificationMonitorService.getTagfromPackageName(event.getPackageName().toString());
                String className = event.getClassName().toString();
                Log.v("class", className);
                if("com.tencent.mobileqq.activity.SplashActivity".equals(event.getClassName()) ||
                        "com.dataline.activities.LiteActivity".equals(event.getClassName())){
                    startService(new Intent(this, NotificationMonitorService.class).putExtra("tag", tag)); //((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).cancel(tag);
                }else if(className.startsWith("cooperation.qzone.")){
                    ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).cancel(tag + 1);
                    startService(new Intent(this, NotificationMonitorService.class).putExtra("tag", NotificationMonitorService.id_qzone));//startService(new Intent(this, NotificationMonitorService.class).putExtra("resetCount", tag));
                }

            }
            break;
        }
    }

    @Override
    public void onInterrupt() {
        //辅助服务被关闭 执行此方法

    }
}
