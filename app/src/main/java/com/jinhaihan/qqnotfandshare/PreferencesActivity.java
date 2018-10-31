package com.jinhaihan.qqnotfandshare;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.text.TextUtils;

import com.jinhaihan.qqnotfandshare.utils.FileUtils;
import com.jinhaihan.qqnotfandshare.utils.PreferencesUtils;

import java.io.File;
import java.net.URISyntaxException;


public class PreferencesActivity extends Activity {
    private static final int REQUEST_STORAGE_CODE = 1;

    public static class PreferencesFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
        public SharedPreferences sp;

        @Override
        public void onCreate(Bundle saveInstanceState) {
            super.onCreate(saveInstanceState);
            // 加载xml资源文件
            addPreferencesFromResource(R.xml.preferences);
            sp = getPreferenceManager().getSharedPreferences();
            refreshSummary();
        }

        @Override
        public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference){
            //Log.d("onPreferenceTreeClick",preference.getKey());
            if("notf_permit".equals(preference.getKey()))
                openNotificationListenSettings();
            if("aces_permit".equals(preference.getKey()))
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
            if(Build.VERSION.SDK_INT >= 23 && "save_permit".equals(preference.getKey()) && !isStorageEnable())
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_STORAGE_CODE);
            if("version_code".equals(preference.getKey()))
                ((PreferencesActivity)getActivity()).showInfo();
            if("icon_path".equals(preference.getKey()))
                ((PreferencesActivity)getActivity()).getIcon();
            if("ringtone".equals(preference.getKey()))
                ((PreferencesActivity)getActivity()).getRingtone();
            return false;
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if("hide_share".equals(key)){
                PackageManager pkg=getActivity().getPackageManager();
                if(sharedPreferences.getBoolean(key, false)){
                    pkg.setComponentEnabledSetting(new ComponentName(getActivity(), ShareActivity.class),
                            PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
                }else{
                    pkg.setComponentEnabledSetting(new ComponentName(getActivity(), ShareActivity.class),
                            PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
                }
            }
//            if("hide_launcher".equals(key)){
//                PackageManager pkg=getActivity().getPackageManager();
//                if(sharedPreferences.getBoolean(key, false)){
//                    pkg.setComponentEnabledSetting(new ComponentName(getActivity(), SplashActivity.class),
//                            PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
//                }else{
//                    pkg.setComponentEnabledSetting(new ComponentName(getActivity(), SplashActivity.class),
//                            PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
//                }
//            }
            refreshSummary();
        }

        @Override
        public void onResume() {
            super.onResume();

            refreshSummary();
            getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onPause() {
            getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
            super.onPause();
        }
/*
        public boolean isNotificationListenerEnabled(Context context) {
            Set<String> packageNames = NotificationManager.getEnabledListenerPackages(context);
            if (packageNames.contains(context.getPackageName())) {
                return true;
            }
            return false;
        }
      */
        public static boolean isNotificationListenerEnabled(Context context){
            String s = android.provider.Settings.Secure.getString(context.getContentResolver(), "enabled_notification_listeners");
            if(s!= null && s.contains(context.getPackageName()))
                return true;
            return false;
        }

        private boolean isAccessibilitySettingsOn(Context context) {
            int accessibilityEnabled = 0;
            final String service = context.getPackageName() + "/" + AccessibilityMonitorService.class.getCanonicalName();
            try {
                accessibilityEnabled = Settings.Secure.getInt(context.getApplicationContext().getContentResolver(),
                        android.provider.Settings.Secure.ACCESSIBILITY_ENABLED);
            } catch (Settings.SettingNotFoundException e) {
                e.printStackTrace();
            }
            TextUtils.SimpleStringSplitter mStringColonSplitter = new TextUtils.SimpleStringSplitter(':');

            if (accessibilityEnabled == 1) {
                String settingValue = Settings.Secure.getString(context.getApplicationContext().getContentResolver(),
                        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
                if (settingValue != null) {
                    mStringColonSplitter.setString(settingValue);
                    while (mStringColonSplitter.hasNext()) {
                        String accessibilityService = mStringColonSplitter.next();
                        if (accessibilityService.equalsIgnoreCase(service)) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }

        public boolean isStorageEnable() {
            if(Build.VERSION.SDK_INT >= 23 && !(getActivity().checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED))
                return false;
            return true;
        }

        public void refreshSummary(){
            ListPreference listPref = (ListPreference) findPreference("icon_mode");
            listPref.setSummary(listPref.getEntry());

            listPref = (ListPreference) findPreference("priority");
            listPref.setSummary(listPref.getEntry());

            listPref = (ListPreference) findPreference("group_priority");
            listPref.setSummary(listPref.getEntry());

            Preference dirPref = (Preference) findPreference("icon_path");
            dirPref.setEnabled(Integer.parseInt(listPref.getValue())==2);
            dirPref.setSummary(PreferencesUtils.getIconPath(getActivity()));

            Preference ringPref = (Preference) findPreference("ringtone");
            Uri uri = PreferencesUtils.getRingtone(getActivity());
            String sum = uri == null? "无" : RingtoneManager.getRingtone(getActivity(), uri).getTitle(getActivity());
            ringPref.setSummary(sum);

            Preference notfPref = (Preference) findPreference("notf_permit");
            notfPref.setSummary(getString(isNotificationListenerEnabled(getActivity())? R.string.pref_enable_permit : R.string.pref_disable_permit));

            Preference acesPref = (Preference) findPreference("aces_permit");
            acesPref.setSummary(getString(isAccessibilitySettingsOn(getActivity())? R.string.pref_enable_permit : R.string.pref_disable_permit));

            //Preference savePref = (Preference) findPreference("save_permit");
            //savePref.setSummary(getString(isStorageEnable()? R.string.pref_enable_permit : R.string.pref_disable_permit));

            EditTextPreference numberPref = (EditTextPreference) findPreference("max_single_msg");
            numberPref.setSummary(numberPref.getText());

            Preference aboutPref = (Preference) findPreference("version_code");
            aboutPref.setSummary(PreferencesUtils.getVersion(getActivity()));
        }

        public void openNotificationListenSettings() {
            try {
                Intent intent;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1) {
                    intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
                } else {
                    intent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
                }
                startActivity(intent);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static final int PHOTO_REQUEST_GALLERY = 2;
    public void getIcon(){
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, PHOTO_REQUEST_GALLERY);
    }

    private static final int RINGTONE_REQUEST = 3;
    public void getRingtone(){
        Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, PreferencesUtils.getRingtone(this));
        startActivityForResult(intent, RINGTONE_REQUEST);
    }

    public void showInfo(){
        AlertDialog.Builder builder=new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.about_dialog_title));
        builder.setMessage(getString(R.string.about_dialog_message));
        builder.setNeutralButton(R.string.about_dialog_github, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_VIEW);
                Uri content_url = Uri.parse("https://github.com/Jinhaihan/QQNotfAndShare");
                intent.setData(content_url);
                startActivity(Intent.createChooser(intent, null));
            }
        });
        builder.setNegativeButton(R.string.about_dialog_support, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String intentFullUrl = "intent://platformapi/startapp?saId=10000007&" +
                        "clientVersion=3.7.0.0718&qrcode=https%3A%2F%2Fqr.alipay.com%2Ffkx075954pd27u8cqiwvg68%3F_s" +
                        "%3Dweb-other&_t=1472443966571#Intent;" +
                        "scheme=alipayqr;package=com.eg.android.AlipayGphone;end";
                try {
                    Intent intent = Intent.parseUri(intentFullUrl, Intent.URI_INTENT_SCHEME );
                    startActivity(intent);
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }
            }
        });
        builder.setPositiveButton(R.string.about_dialog_button, null);
        builder.show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.preference_layout);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PHOTO_REQUEST_GALLERY) {
            // 从相册返回的数据
            if (data != null) {
                // 得到图片的全路径
                Uri uri = data.getData();
                File file = FileUtils.saveUriToCache(this, uri, "icon", true);
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
                editor.putString("icon_path", file.getAbsolutePath()).apply();
            }
        }
        if(requestCode == RINGTONE_REQUEST && resultCode == Activity.RESULT_OK){
            Uri pickedUri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
            editor.putString("ringtone", pickedUri == null ? "" : pickedUri.toString()).apply();
        }
    }
/*
    public String getRealPathFromURI(Context context, Uri contentUri) {
        Cursor cursor = null;
        try {
            String[] proj = { MediaStore.Images.Media.DATA };
            cursor = context.getContentResolver().query(contentUri,  proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }
    */
}
