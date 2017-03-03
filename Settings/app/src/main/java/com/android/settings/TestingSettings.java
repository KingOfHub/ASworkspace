/*
 * Copyright (C) 2008 The Android Open Source Project
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
 * limitations under the License.
 */

package com.android.settings;

/*import com.qualcomm.qcnvitems.QcNvItemIds;
import com.qualcomm.qcrilhook.QcRilHook;
import com.qualcomm.qcrilhook.QcRilHookCallback;*/

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemProperties;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class TestingSettings extends PreferenceActivity {
    Preference root;

    private static final String PRL_VERSION_DISPLAY = "*#0000#";

    private static final String PRL_MODEM_TEST_DISPLAY = "*#1234#";

    /** GPS Test Tool */
    private static final String GPS_TOOL = "*#311";

    private static final String Fine_Touch_Cal = "*#315";

    /** software & hardware version display command code */
    private static final String DEVICEINFO_DISPLAY = "*#316";

    /** factory test command code */
    private static final String TESTING_NAME = "*#";

    // private static final String TESTING_TOOL = "*#318";
    private static final String TESTING_TOOL_319 = "*#319";

    // 3250
    private static final String MAX_TOOL = "*#3250";

    // checktrigger
    private static final String CHECK_TRIGGER = "*#3251";

    // 4442
    private static final String ISO4442 = "*#3252";

    // urovo
    private static final String TESTING_TOOL = "*#1262*#";

    private static final String SCAN_SELECT = "*#1261*#";

    private static final String SCAN_AGE = "*#1260*#";

    private static final String SCAN_SN = "*#1258*#";

    private static final String USN_DISPLAY = "*#918";

    // add for pwv build id
    private static final String PWV_BUILD_ID = "*#317";

    private static final String GPS_ID = "GPS";
    private PreferenceScreen PRL_VERSION_DISPLAY_KEY;

    private PreferenceScreen GPS_TOOL_KEY;

    private PreferenceScreen Fine_Touch_Cal_KEY;

    private PreferenceScreen DEVICEINFO_DISPLAY_KEY;

    private PreferenceScreen TESTING_TOOL_KEY;

    private PreferenceScreen SCAN_SELECT_KEY;

    private PreferenceScreen SCAN_AGE_KEY;

    private PreferenceScreen MAX_TOOL_KEY;

    private PreferenceScreen CHECK_TRIGGER_KEY;

    private PreferenceScreen ISO4442_KEY;

    private PreferenceScreen PWV_BUILD_ID_KEY;
    
    private PreferenceScreen APPS_SELECT_KEY;
    private PreferenceScreen SCAN_SET_KEY;
    private PreferenceScreen GPS_KEY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getIntent().getAction().equals("android.intent.action.INTERNAL_TEST")) {
            addPreferencesFromResource(R.xml.testing_settings_list);
            root = this.getPreferenceScreen();
            PRL_VERSION_DISPLAY_KEY = (PreferenceScreen) getPreferenceScreen().findPreference(
                    "prl_version_display");
            PRL_VERSION_DISPLAY_KEY.setTitle(PRL_VERSION_DISPLAY);
            GPS_TOOL_KEY = (PreferenceScreen) getPreferenceScreen().findPreference("gps_tool");
            GPS_TOOL_KEY.setTitle(GPS_TOOL);
            Fine_Touch_Cal_KEY = (PreferenceScreen) getPreferenceScreen()
                    .findPreference("fine_touch_cal");
            Fine_Touch_Cal_KEY.setTitle(Fine_Touch_Cal);
            DEVICEINFO_DISPLAY_KEY = (PreferenceScreen) getPreferenceScreen().findPreference(
                    "deviceinfo_display");
            DEVICEINFO_DISPLAY_KEY.setTitle(DEVICEINFO_DISPLAY);
            TESTING_TOOL_KEY = (PreferenceScreen) getPreferenceScreen().findPreference(
                    "testing_tool");
            TESTING_TOOL_KEY.setTitle(TESTING_TOOL);
            
            SCAN_SELECT_KEY = (PreferenceScreen) getPreferenceScreen()
                    .findPreference("scan_select");
            SCAN_SELECT_KEY.setTitle(SCAN_SELECT);
            
            SCAN_AGE_KEY = (PreferenceScreen) getPreferenceScreen().findPreference("scan_age");
            SCAN_AGE_KEY.setTitle(SCAN_AGE);

            MAX_TOOL_KEY = (PreferenceScreen) getPreferenceScreen().findPreference("max_tool");
            CHECK_TRIGGER_KEY = (PreferenceScreen) getPreferenceScreen().findPreference(
                    "check_trigger");
            ISO4442_KEY = (PreferenceScreen) getPreferenceScreen().findPreference("iso_ictest");
            if (Build.PROJECT.equals("SQ26") || Build.PROJECT.equals("SQ26B")
                    || Build.PROJECT.equals("SQ27") || Build.PROJECT.equals("SQ27C")) {
                MAX_TOOL_KEY.setTitle(MAX_TOOL);
                CHECK_TRIGGER_KEY.setTitle(CHECK_TRIGGER);
                ISO4442_KEY.setTitle(ISO4442);
            } else {
                if (MAX_TOOL_KEY != null)
                    getPreferenceScreen().removePreference(MAX_TOOL_KEY);
                if (CHECK_TRIGGER_KEY != null)
                    getPreferenceScreen().removePreference(CHECK_TRIGGER_KEY);
                if (ISO4442_KEY != null)
                    getPreferenceScreen().removePreference(ISO4442_KEY);
            }

        } if (getIntent().getAction().equals("android.intent.action.INTERNAL_WTM_ITEM")) {
            addPreferencesFromResource(R.xml.wtm_item_settings);
            root = this.getPreferenceScreen();
            SCAN_SET_KEY = (PreferenceScreen) getPreferenceScreen()
                    .findPreference("scan_settings");
            SCAN_SET_KEY.setTitle(R.string.set_scanner_manager);
            
            APPS_SELECT_KEY = (PreferenceScreen) getPreferenceScreen()
                    .findPreference("applications");
            APPS_SELECT_KEY.setTitle(R.string.quick_launch_display_mode_applications);
            GPS_KEY=(PreferenceScreen) getPreferenceScreen().findPreference("gps_settings");
            GPS_KEY.setTitle(GPS_ID);
        }else {
            addPreferencesFromResource(R.xml.testing_settings);
        }
    }

    @Override
    @Deprecated
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        // TODO Auto-generated method stub
        try {

            if (preference == PRL_VERSION_DISPLAY_KEY) {
                Intent intent = new Intent("android.intent.action.ENGINEER_MODE_DEVICEINFO");
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            } else if (preference == GPS_TOOL_KEY) {
                Intent intent = new Intent();
                intent.setComponent(new ComponentName("com.chartcross.gpstest",
                        "com.chartcross.gpstest.GPSTest"));
                startActivity(intent);
            } else if (preference == Fine_Touch_Cal_KEY) {
                if (Build.TOUCHCAL.equals("true")) {
                    Intent intent = new Intent();
                    intent.setComponent(new ComponentName("com.dongbu.finetouchm",
                            "com.dongbu.finetouchm.MainActivity"));
                    startActivity(intent);
                }
            } else if (preference == TESTING_TOOL_KEY) {
                Intent intent = new Intent();
                intent.setComponent(new ComponentName("com.qualcomm.factory",
                        "com.qualcomm.factory.Framework.Framework"));
                intent.putExtra("msg", true);
                startActivity(intent);

            } else if (preference == MAX_TOOL_KEY) {
                Intent intent = new Intent();
                intent.setComponent(new ComponentName("com.ped.maxq3250",
                        "com.ped.maxq3250.Maxq3250"));
                startActivity(intent);
            } else if (preference == CHECK_TRIGGER_KEY) {
                Intent intent = new Intent();
                intent.setComponent(new ComponentName("com.example.checktrigger",
                        "com.example.checktrigger.MainActivity"));
                startActivity(intent);
            } else if (preference == SCAN_SELECT_KEY) {
                Intent intent = new Intent();
                intent.setComponent(new ComponentName("com.android.settings",
                        "com.android.settings.UrovoSettings"));
                startActivity(intent);
            } else if (preference == SCAN_AGE_KEY) {
                Intent intent = new Intent();
                intent.setComponent(new ComponentName("com.urovo.agetest",
                        "com.urovo.agetest.agingtest"));
                startActivity(intent);
            } else if (preference == ISO4442_KEY) {
                Intent intent = new Intent();
                intent.setComponent(new ComponentName("com.urovo.iso4442",
                        "com.urovo.iso4442.MainActivity"));
                startActivity(intent);
            } else if (preference == DEVICEINFO_DISPLAY_KEY) {
                showDeviceInfoPanel(this, true);
            } else if(preference == SCAN_SET_KEY) {
                Intent intent = new Intent("android.intent.action.SCANNER_SETTINGS");
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            } else if(preference == APPS_SELECT_KEY) {
                Intent intent = new Intent("android.settings.APPLICATION_SETTINGS");
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }else if(preference == GPS_KEY) {
                Intent intent = new Intent("android.settings.LOCATION_SOURCE_SETTINGS");
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        } catch (ActivityNotFoundException e) {
            e.printStackTrace();
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    //private static QcRilHook mQcRilOemHook;
    private static String mHWNStr;
    private static String qcnStr;

    private static TextView hwn1, hwn2;
    private static TextView swn1, swn2;
    private static TextView armn1, armn2;
    private static TextView mod1, mod2;
    private static TextView qcn1, qcn2;
    /*private static QcRilHookCallback mQcrilHookCb = new QcRilHookCallback() {
        public void onQcRilHookReady() {
                mHWNStr = mQcRilOemHook.getDeviceNV(QcNvItemIds.NV_OEM_ITEM_1_I);
                qcnStr = mQcRilOemHook.getDeviceNV(QcNvItemIds.NV_OEM_ITEM_7_I);
        }
    };*/
    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
        //mQcRilOemHook = new QcRilHook(this, mQcrilHookCb);
    }

    @Override
    protected void onPause() {
        // TODO Auto-generated method stub
        super.onPause();
    }
    static String getModemStr() {
        String str1 = "/sys/devices/system/soc/soc0/build_id";
        String str2 = null;

        try {
            FileReader localFileReader = new FileReader(str1);
            BufferedReader localBufferedReader = new BufferedReader(localFileReader, 8192);
            str2 = localBufferedReader.readLine();

        } catch (IOException e) {
        }
        return str2;
    }
    static void showDeviceInfoPanel(Context context, boolean useSystemWindow) {
        /*if(mQcRilOemHook == null)
        mQcRilOemHook = new QcRilHook(context, mQcrilHookCb); */
        String aRMStr = "";
        try {
            aRMStr = SystemProperties.get("gsm.version.baseband","");
        } catch (RuntimeException e) {
            // No recovery
        }
        if(aRMStr != null){
            int len = aRMStr.length();
            if(len > 30){
               aRMStr = aRMStr.substring(0, 29);
            }
        }
        String modStr = getModemStr();
        LinearLayout view = new LinearLayout(context);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        view.setOrientation(LinearLayout.VERTICAL);
        hwn1 = new TextView(context);
        hwn2 = new TextView(context);
        swn1 = new TextView(context);
        swn2 = new TextView(context);
        armn1 = new TextView(context);
        armn2 = new TextView(context);
        mod1 = new TextView(context);
        mod2 = new TextView(context);
        qcn1 = new TextView(context);
        qcn2 = new TextView(context);
        view.addView(hwn1, layoutParams);
        view.addView(hwn2, layoutParams);
        view.addView(swn1, layoutParams);
        view.addView(swn2, layoutParams);
        view.addView(armn1, layoutParams);
        view.addView(armn2, layoutParams);
        view.addView(mod1, layoutParams);
        view.addView(mod2, layoutParams);
        view.addView(qcn1, layoutParams);
        view.addView(qcn2, layoutParams);
        hwn1.setText("hardware version:");
        hwn2.setText(mHWNStr);
        swn1.setText("software version:");
        swn2.setText(Build.DISPLAY);
        armn1.setText(" arm version:");
        armn2.setText(aRMStr);
        mod1.setText("modem version:");
        mod2.setText(modStr);
        qcn1.setText("Qcn version:");
        qcn2.setText(qcnStr);

        AlertDialog alert = new AlertDialog.Builder(context)
                .setTitle("Device Version Info")
                .setView(view)
                .setPositiveButton(android.R.string.ok, null)
                .setCancelable(false).show();
    }

}
