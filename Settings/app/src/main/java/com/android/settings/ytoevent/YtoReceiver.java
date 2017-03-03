package com.android.settings.ytoevent;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.provider.Settings;
import android.device.ScanManager;
import android.telephony.TelephonyManager;
import static com.android.internal.telephony.MSimConstants.MAX_PHONE_COUNT_TRI_SIM;

public class YtoReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO Auto-generated method stub
        String action = intent.getAction();
        boolean enable = intent.getBooleanExtra("enable", false);
        //android.util.Log.d("-==========================", action);
        if ("android.intent.action.BOOT_COMPLETED".equals(action) && Build.PROJECT.equals("ID85") && Build.PWV_CUSTOM_CUSTOM.equals("YTO")) {
            context.startService(new Intent(context, TextToSpeechService.class));
            if(!getStatus(context) && hasNewUpdate()) {
                Settings.System.putInt(context.getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, 0);
                Settings.Secure.putString(context.getContentResolver(), Settings.Secure.DEFAULT_INPUT_METHOD, "com.tencent.qqpinyin/.QQPYInputMethodService");
                Settings.Secure.putString(context.getContentResolver(), Settings.Secure.ENABLED_INPUT_METHODS, "com.tencent.qqpinyin/.QQPYInputMethodService");
                ScanManager sm = new ScanManager();
                sm.setOutputParameter(4, 1);
                sm.setOutputParameter(5, 0);
                setStatus(context);
            }
        } else if ("com.yto.action.APP_INSTALL_ENABLE".equals(action)) {
            Settings.System.putInt(context.getContentResolver(), "APP_INSTALL_ENABLE", enable ? 1 : 0);
        } else if ("com.yto.action.SET_ADB".equals(action)) {
            Settings.Global.putInt(context.getContentResolver(), Settings.Global.ADB_ENABLED, enable ? 1 : 0);
        } else if(action.equals("com.yto.action.CHANGE_NETWORK_STATE")) {
            String type = intent.getStringExtra("type");
            //boolean enable = intent.getBooleanExtra("enable", false);
            if(type != null) {
                if (type.equals("mobile")) {
                    ConnectivityManager cm =
                            (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                    boolean mobileState = cm.getMobileDataEnabled();
                    if(!(mobileState == enable)) {
                        cm.setMobileDataEnabled(enable);
                    }
                } else if (type.equals("wifi")) {
                    WifiManager mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                    boolean state = mWifiManager.isWifiEnabled();
                    if(!(state == enable)) {
                        mWifiManager.setWifiEnabled(enable);
                    }
                }
            }
        } else if(action.equals("android.intent.action.CHANGE_MOBILE_NETWORK")){
            ConnectivityManager cm =
                    (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            boolean mobileState = cm.getMobileDataEnabled();
            if(mobileState != enable) {
                TelephonyManager telephony = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
                if(telephony.getSimState() != TelephonyManager.SIM_STATE_READY)
                {
                    Settings.Global.putInt(context.getContentResolver(),
                        Settings.Global.MOBILE_DATA , enable ? 1 : 0);
                }
                cm.setMobileDataEnabled(enable);
                for (int i = 0; i < MAX_PHONE_COUNT_TRI_SIM; i++) {
                    Settings.Global.putInt(context.getContentResolver(),
                            Settings.Global.MOBILE_DATA + i, enable ? 1 : 0);
                }
            }
        }
    }
    
    private static String[] getCurrentVersionInfo() {
        String buildId = Build.DISPLAY;
        if(buildId != null && !buildId.equals(""))
            return buildId.split("_");
        return null;
    }
    private static int covertInt(String info) {
        try{
            return Integer.parseInt(info);
        } catch(NumberFormatException e) {
            e.printStackTrace();
        }
        return 0;
    }
    public static boolean hasNewUpdate() {
        String[] curVersion = getCurrentVersionInfo();
        int curDate = covertInt(curVersion[curVersion.length -2]);
         
        return curDate == 150611;
    }
    
    private static boolean getStatus(Context context) {
        
        SharedPreferences yto = context.getSharedPreferences("ytoupdate",context.MODE_PRIVATE);
        boolean isUpdate = yto.getBoolean("currentupdate", false);
        return isUpdate;
        
    }

    private static void setStatus(Context context) {
        SharedPreferences yto = context.getSharedPreferences("ytoupdate",context.MODE_PRIVATE);
        Editor edit = yto.edit();
        edit.putBoolean("currentupdate", true);
        edit.commit();
        
    }
}
