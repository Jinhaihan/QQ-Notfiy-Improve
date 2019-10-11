package com.jinhaihan.qqnotfandshare;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.preference.PreferenceManager;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import com.jinhaihan.qqnotfandshare.utils.FileUtils;
import com.jinhaihan.qqnotfandshare.utils.PreferencesUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NotificationMonitorService extends NotificationListenerService {
    String ClassTag = "NotificationListenerService";
    public static final int id_qq = 1;
    public static final int id_qqlite = 2;
    public static final int id_tim = 3;
    public static final int id_qzone = 4;
    public static final int id_group0 = 5;

    private static final int maxCount = 20;

    private static final int PUSH_NOTIFICATION_ID = (0x001);
    private static final String PUSH_CHANNEL_ID = "QQ_";
    private static final String PUSH_CHANNEL_NAME = "QQ";
    private static final String PUSH_CHANNEL_Group_ID = "QQ_Group_";
    private static final String PUSH_CHANNEL_Group_NAME = "QQ Group";

    // 在收到消息时触发
    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        Log.d("ClassTag","有通知弹出消息");
        int tag = getTagfromPackageName(sbn.getPackageName());
        if (tag != 0)
            notif(sbn, tag);
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            if (intent.hasExtra("tag")) {
                int tag = intent.getIntExtra("tag", 0);
                if (tag > 0) {
                    NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                    assert nm != null;
                    nm.cancel(tag);
                    if (tag != id_qzone) {
                        StatusBarNotification[] sbns = getActiveNotifications();
                        if (sbns != null && sbns.length > 0) {
                            for (StatusBarNotification sbn : sbns) {
                                if (!getPackageName().equals(sbn.getPackageName()) || sbn.getId() < id_group0)
                                    continue;
                                nm.cancel(sbn.getId());
                            }
                        }
                        notifs.clear();
                    }
                }
            } else if (intent.hasExtra("notfClick")) {
                if (lastIntent != null) {
                    notifs.clear();
                    NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                    StatusBarNotification[] sbns = getActiveNotifications();
                    if (sbns != null && sbns.length > 0) {
                        for (StatusBarNotification sbn : sbns) {
                            if (!getPackageName().equals(sbn.getPackageName()) || sbn.getId() < id_group0)
                                continue;
                            assert nm != null;
                            nm.cancel(sbn.getId());
                        }
                    }
                    try {
                        lastIntent.send();
                    } catch (PendingIntent.CanceledException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        return START_STICKY;
    }

    private void notif(StatusBarNotification sbn, int tag) {
        Notification notification = sbn.getNotification();
        if (notification == null)
            return;
        //标题/内容
        String notf_title = notification.extras.getString(Notification.EXTRA_TITLE);
        String notf_text = notification.extras.getString(Notification.EXTRA_TEXT);
        if (notf_text != null && !notf_text.isEmpty())
            notf_text = notf_text.replaceAll("\n", " ");
        else
            notf_text = "";
        String notf_ticker = "";
        if (notification.tickerText != null) {
            notf_ticker = notification.tickerText.toString();
            notf_ticker = notf_ticker.replaceAll("\n", " ");
        }
        //多人消息
        boolean mul = !notf_text.contains(":") && !notf_ticker.endsWith(notf_text);
        String title = mul ? notf_text : notf_title;
        if (title == null || notf_ticker.isEmpty() || title.equals(notf_ticker))
            return;

        //单独处理QQ空间
        boolean isQzone = false;
        int count = 1;
        Matcher matcher = Pattern.compile("QQ空间动态\\(共(\\d+)条未读\\)$").matcher(title);
        if (notf_ticker.equals(notf_text)) {
            if (matcher.find()) {
                isQzone = true;
                count = Integer.parseInt(Objects.requireNonNull(matcher.group(1)));
            } else if ("QQ空间动态".equals(title)) {
                isQzone = true;
                count = maxCount;
            }
        }
        //消息数量
        if (!isQzone) {
            matcher = Pattern.compile("(\\d+)\\S{1,3}新消息\\)?$").matcher(title);
            if (matcher.find())
                count = Integer.parseInt(Objects.requireNonNull(matcher.group(1)));
        }
        int maxMsgLength = getMaxMsgLength();
        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("use_group", false) ?
                notif_group(count, tag, title, notf_ticker, notf_text, mul, isQzone, notification, maxMsgLength) :
                notif_list(count, tag, title, notf_ticker, notf_text, mul, isQzone, notification, maxMsgLength)) {
        }
        if (Build.VERSION.SDK_INT >= 23)
            setNotificationsShown(new String[]{sbn.getKey()});
        cancelNotification(sbn.getKey());
    }

    private class NotifInfo {
        String name;
        List<String> msgs = new ArrayList<>();
    }

    final ArrayList<NotifInfo> notifs = new ArrayList<>();

    private boolean notif_group(int count, int tag, String title, String notf_ticker, String notf_text, boolean mul, boolean isQzone, Notification notification, int maxMsgLength) {
        if (isQzone)
            return notif_list(count, tag, title, notf_ticker, notf_text, mul, isQzone, notification, maxMsgLength);
        String name = null, text = null;
        boolean isGroupMsg = false;
        Matcher matcher = Pattern.compile("(.*?)\\((.+?)\\):(.+)").matcher(notf_ticker);
        if (matcher.find()) {
            name = matcher.group(2);
            text = matcher.group(1) + ":" + matcher.group(3);
            isGroupMsg = true;
        } else {

            matcher = Pattern.compile("([^:]+):(.+)").matcher(notf_ticker);
            if (matcher.find()) {
                name = matcher.group(1);
                text = matcher.group(2);
            }

        }
        if (name == null || text == null)
            return notif_list(count, tag, title, notf_ticker, notf_text, mul, isQzone, notification, maxMsgLength);

        NotifInfo newInfo = new NotifInfo();
        newInfo.name = name;
        newInfo.msgs.add(text);
        int id = -1;
        for (int i = 0; i < notifs.size(); i++) {
            NotifInfo notif = notifs.get(i);
            if (name.equals(notif.name)) {
                id = i;
                newInfo.msgs.addAll(notif.msgs);
                break;
            }
        }
        if (id < 0)
            id = notifs.size();
        if (id >= notifs.size())
            notifs.add(newInfo);
        else
            notifs.set(id, newInfo);

        NotificationCompat.Style style;
        String str = "";
        NotificationCompat.BigTextStyle bstyle = new NotificationCompat.BigTextStyle();
        if (maxMsgLength > 0) {

            bstyle.setBigContentTitle(name);
            for (int m = 1; m < newInfo.msgs.size() && m < maxCount; m++) {
                String msg = newInfo.msgs.get(m);
                if (msg.length() > maxMsgLength)
                    msg = msg.substring(0, maxMsgLength);
                text = text + "\n" + msg;
            }
            bstyle.bigText(text);
            style = bstyle;
        } else {
            NotificationCompat.InboxStyle istyle = new NotificationCompat.InboxStyle();
            istyle.setBigContentTitle(name);
            for (int m = 0; m < newInfo.msgs.size() && m < maxCount; m++) {
                String msg = newInfo.msgs.get(m);
                istyle.addLine(msg);
                str += msg + "\n";
            }
            try{
                str = str.substring(0, str.length()-1);
            }
            catch (Exception e){}
            bstyle.bigText(str);
            style = bstyle;
        }

        buildNotification(name, text, isQzone, mul, tag, style, notification, false, true, id + 1 + id_group0, isGroupMsg);

        if (Build.VERSION.SDK_INT >= 24 && PreferenceManager.getDefaultSharedPreferences(this).getBoolean("use_notif_group", false))//Nougat
            buildNotification(name, text, isQzone, mul, tag, null, notification, true, true, id_group0, isGroupMsg);
        return true;
    }

    private boolean notif_list(int count, int tag, String title, String notf_ticker, String notf_text, boolean mul, boolean isQzone, Notification notification, int maxMsgLength) {
        String msg = notf_ticker + "\n" + (mul ? notf_ticker : notf_text);
        ArrayList<String> msgs = isQzone ? msgQzone : getMsgList(tag);
        msgs.add(0, msg);
        //删除多余消息
        for (int i = count; i < msgs.size(); ) {
            msgs.remove(i);
        }
        String first = "";
        NotificationCompat.InboxStyle istyle = new NotificationCompat.InboxStyle();
        NotificationCompat.BigTextStyle bstyle = new NotificationCompat.BigTextStyle();
        istyle.setBigContentTitle(title);
        String str = "";
        for (String s : msgs) {
            count--;
            String[] ss = s.split("\n");
            String m = ss.length > 1 ? ss[mul ? 0 : 1] : s;

            if (maxMsgLength > 0) {
                if (m.length() > maxMsgLength)
                    m = m.substring(0, maxMsgLength) + "...";
                if (first.isEmpty())
                    first = m;
                else
                    first = m + "\n" + first;
            } else {
                istyle.addLine(m);
                str = m + "\n" + str;
                if (first.isEmpty())
                    first = m;
            }
            if (count == 0)
                break;
        }
        str = str.substring(0, str.length() - 1);
        bstyle.bigText(str);
        buildNotification(title, first, isQzone, mul, tag, /*maxMsgLength > 0 ? bstyle : istyle*/bstyle, notification, false, false, isQzone ? id_qzone : tag, false);
        return true;
    }

    PendingIntent lastIntent;

    private void buildNotification(String title, String text, boolean isQzone, boolean mul, int tag, NotificationCompat.Style style, Notification notification, boolean setGroupSummary, boolean group, int id, boolean isGroupMsg) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        if (isGroupMsg && sp.getBoolean("ignore_group_msg", false))
            return;

        //boolean noFloat = sp.getBoolean("disable_float", false);
        //noFloat |= isGroupMsg && sp.getBoolean("ignore_group_float", false);

        int priority = Integer.parseInt(sp.getString("priority", "0"));
        String channel = "";
        if (isGroupMsg) {
            priority = Math.min(priority, Integer.parseInt(sp.getString("group_priority", "0")));
            channel = PUSH_CHANNEL_Group_ID;
        } else channel = PUSH_CHANNEL_ID;
        priority = Math.min(notification.priority, priority);


        int channelNum = Integer.parseInt(PreferencesUtils.getChannelNum(getBaseContext()));
        Log.e("Channel", channel + channelNum);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channel + channelNum)
                //.setSubText(getString(getStringId(tag)))
                .setContentTitle(title)
                .setContentText(text)
                .setColor(getResources().getColor(isQzone ? R.color.colorQzone : R.color.colorPrimary))
                //.setSmallIcon(isQzone ? R.drawable.ic_qzone : getIcon(tag))
                //.setLargeIcon((Bitmap) notification.extras.get(Notification.EXTRA_LARGE_ICON))
                .setStyle(style)
                .setAutoCancel(true)
                .setContentIntent(notification.contentIntent)
                .setDeleteIntent(notification.deleteIntent)
                .setTicker(title)
                //.setPriority(setGroupSummary ? Notification.PRIORITY_HIGH : priority)
                .setPriority(Notification.PRIORITY_HIGH)
                .setLights(notification.ledARGB, notification.ledOnMS, notification.ledOffMS)
                //.setVibrate(notification.vibrate)
                .setDefaults(Notification.DEFAULT_VIBRATE | Notification.FLAG_AUTO_CANCEL | Notification.DEFAULT_ALL)
                .setShowWhen(true)
                .setGroupSummary(setGroupSummary)
                .setChannelId(channel + channelNum);

        setIcon(builder, tag, isQzone);

        Bitmap bmp = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            bmp = FileUtils.drawableToBitmap(notification.getLargeIcon().loadDrawable(this));
        }
        else {
            bmp = (Bitmap) Objects.requireNonNull(notification.extras.get(Notification.EXTRA_LARGE_ICON));
        }

        if (!isQzone && group) {
            lastIntent = notification.contentIntent;
            Intent notificationIntent = new Intent(this, NotificationMonitorService.class);
            notificationIntent.putExtra("notfClick", true);
            PendingIntent pendingIntent = PendingIntent.getService(this.getApplicationContext(), 0, notificationIntent, 0);
            builder.setContentIntent(pendingIntent);
            if (mul) {
                Bitmap cache = FileUtils.getBitmapFromCache(this, title, "profile");
                if (cache != null)
                    bmp = cache;
            } else if (bmp != null) {
                FileUtils.saveBitmapToCache(this, bmp, title, "profile", false);
            }
        }
        builder.setLargeIcon(bmp);

        if (group)
            builder.setGroup("GROUP");
        boolean sound = !isGroupMsg || !sp.getBoolean("ignore_group_sound", false);

        //SDK<26
        if (!setGroupSummary && sound)
            builder.setSound(PreferencesUtils.getRingtone(this));

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(id, builder.build());
    }


    final ArrayList<String> msgQQ = new ArrayList<String>();
    final ArrayList<String> msgQQLite = new ArrayList<String>();
    final ArrayList<String> msgTim = new ArrayList<String>();
    final ArrayList<String> msgQzone = new ArrayList<String>();

    private ArrayList<String> getMsgList(int tag) {
        switch (tag) {
            case id_qq://R.string.qq:
                return msgQQ;
            case id_tim://R.string.tim:
                return msgTim;
            case id_qqlite://R.string.qqlite:
                return msgQQLite;
        }
        return msgQQ;
    }

    private int getStringId(int tag) {
        switch (tag) {
            case id_qq://R.string.qq:
                return R.string.qq;
            case id_tim://R.string.tim:
                return R.string.tim;
            case id_qqlite://R.string.qqlite:
                return R.string.qqlite;
        }
        return R.string.qq;
    }

    private String path = "";
    private Icon icon;

    private void setIcon(NotificationCompat.Builder builder, int tag, boolean isQzone) {
        if (isQzone) {
            builder.setSmallIcon(R.drawable.ic_qzone);
            return;
        }
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        int mode = Integer.parseInt(sp.getString("icon_mode", "0"));
        if (mode == 5 && Build.VERSION.SDK_INT >= 23) {
            String s = sp.getString("icon_path", "");
            if (icon == null || !s.equals(path)) {
                path = s;
                Bitmap bmp = BitmapFactory.decodeFile(path);
                if (bmp != null)
                    icon = Icon.createWithBitmap(bmp);
            }
            if (icon != null) {
                builder.setSmallIcon(R.drawable.ic_qq);
                return;
            }
        }
        int iconRes = R.drawable.ic_qq;
//        switch (tag){
//            case id_qq://R.string.qq:
//            case id_qqlite://R.string.qqlite:
//                iconRes =  mode == 1? R.drawable.ic_qq_full : R.drawable.ic_qq;
//                break;
//            case id_tim://R.string.tim:
//                iconRes =  R.drawable.ic_tim;
//                break;
//        }
        switch (mode) {
            case 0:
                switch (tag) {
                    case id_qq://R.string.qq:
                    case id_qqlite://R.string.qqlite:
                        iconRes = R.drawable.ic_qq;
                        break;
                    case id_tim://R.string.tim:
                        iconRes = R.drawable.ic_tim;
                        break;
                }
                break;
            case 1:
                iconRes = R.drawable.ic_tim;
                break;
            case 2:
                iconRes = R.drawable.chat2;
                break;
            case 3:
                iconRes = R.drawable.chat;
                break;
            default:
                iconRes = R.drawable.ic_qq;
                break;


        }
        builder.setSmallIcon(iconRes);
    }

    public static int getTagfromPackageName(String packageName) {
        switch (packageName) {
            case "com.tencent.mobileqq":
                return id_qq;//R.string.qq;
            case "com.tencent.tim":
                return id_tim;//R.string.tim;
            case "com.tencent.qqlite":
                return id_qqlite;//R.string.qqlite;
        }
        return 0;
    }

    private int getMaxMsgLength() {
        if (!PreferenceManager.getDefaultSharedPreferences(this).
                getBoolean("use_multi_line", false))
            return 0;
        return Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(this).
                getString("max_single_msg", "0"));
    }
}
