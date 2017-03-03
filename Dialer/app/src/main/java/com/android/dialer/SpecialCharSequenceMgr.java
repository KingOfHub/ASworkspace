/*
 * Copyright (C) 2006 The Android Open Source Project
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

package com.android.dialer;

import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Looper;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.provider.Settings;
import android.telecom.TelecomManager;
import android.telephony.PhoneNumberUtils;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Locale;

import com.android.common.io.MoreCloseables;
import com.android.contacts.common.database.NoNullCursorAsyncQueryHandler;
import com.android.internal.telephony.ITelephony;

import com.qualcomm.qcrilhook.QcRilHook;
import com.qualcomm.qcrilhook.IQcRilHook;
import com.qualcomm.qcrilhook.QcRilHookCallback;
import com.qualcomm.qcnvitems.QcNvItemIds;
import com.android.internal.os.storage.ExternalStorageFormatter;

/**
 * Helper class to listen for some magic character sequences
 * that are handled specially by the dialer.
 *
 * Note the Phone app also handles these sequences too (in a couple of
 * relatively obscure places in the UI), so there's a separate version of
 * this class under apps/Phone.
 *
 * TODO: there's lots of duplicated code between this class and the
 * corresponding class under apps/Phone.  Let's figure out a way to
 * unify these two classes (in the framework? in a common shared library?)
 */
public class SpecialCharSequenceMgr {
    private static final String TAG = "SpecialCharSequenceMgr";

    private static final String SECRET_CODE_ACTION = "android.provider.Telephony.SECRET_CODE";
    private static final String MMI_IMEI_DISPLAY = "*#06#";
    private static final String MMI_REGULATORY_INFO_DISPLAY = "*#07#";
    private static final String MMI_OPEN_DIAG_MENU_DISPLAY = "*76278#";
    private static final String MMI_FACTORY_MODE_DISPLAY = "#38378#";
    private static final String MMI_ENGINEER_MODE_DISPLAY = "*#7548135*#";
    private static final String PRL_VERSION_DISPLAY = "*#0000#";

    private static final String QUAL_DISPLAY_QLOG = "*#999";

    /** GPS Test Tool */
    private static final String GPS_TOOL     = "*#311";

    private static final String Fine_Touch_Cal     = "*#315";
    /** software & hardware version display command code */
    private static final String DEVICEINFO_DISPLAY = "*#316";
    //add for pwv build id
    private static final String PWV_BUILD_ID     = "*#317";
    /** factory test command code */
    private static final String TESTING_NAME     = "*#";
    private static final String TESTING_TOOL     = "*#318";
    private static final String TESTING_TOOL_319     = "*#319";
    private static final String TESTING_TOOL_UROVO     = "*#1262*#";
    // 3250
    private static final String MAX_TOOL = "*#3250"; 
    
    // checktrigger
    private static final String CHECK_TRIGGER = "*#3251"; 
    private static final String TESTING_SCAN_TYPE     = "*#1261*#";
    private static final String SCAN_AGE     = "*#1260*#";
    private static QcRilHook mQcRilOemHook;
    private static String mHWNStr;
    private static String qcnStr;

    private static TextView hwn1, hwn2;
    private static TextView swn1, swn2;
    private static TextView armn1, armn2;
    private static TextView mod1, mod2;
    private static TextView qcn1, qcn2;
    private static TextView baseLine1, baseLine2;

    /**
     * Remembers the previous {@link QueryHandler} and cancel the operation when needed, to
     * prevent possible crash.
     *
     * QueryHandler may call {@link ProgressDialog#dismiss()} when the screen is already gone,
     * which will cause the app crash. This variable enables the class to prevent the crash
     * on {@link #cleanup()}.
     *
     * TODO: Remove this and replace it (and {@link #cleanup()}) with better implementation.
     * One complication is that we have SpecialCharSequenceMgr in Phone package too, which has
     * *slightly* different implementation. Note that Phone package doesn't have this problem,
     * so the class on Phone side doesn't have this functionality.
     * Fundamental fix would be to have one shared implementation and resolve this corner case more
     * gracefully.
     */

    private static QcRilHookCallback mQcrilHookCb = new QcRilHookCallback() {
        public void onQcRilHookReady() {
		Log.e("mQcrilHookCb-mQcrilHookCb", "getDeviceNV");
                mHWNStr = mQcRilOemHook.getDeviceNV(QcNvItemIds.NV_OEM_ITEM_1_I);
                qcnStr = mQcRilOemHook.getDeviceNV(QcNvItemIds.NV_OEM_ITEM_7_I);
        }
    };

    private static QueryHandler sPreviousAdnQueryHandler;

    /** This class is never instantiated. */
    private SpecialCharSequenceMgr() {
    }

    public static boolean handleChars(Context context, String input, EditText textField) {
        return handleChars(context, input, false, textField);
    }

    static boolean handleChars(Context context, String input) {
        return handleChars(context, input, false, null);
    }

    static boolean handleChars(Context context, String input, boolean useSystemWindow,
            EditText textField) {

        //get rid of the separators so that the string gets parsed correctly
        String dialString = PhoneNumberUtils.stripSeparators(input);

        if (context.getResources().getBoolean(R.bool.def_dialer_secretcode_enabled) ||
                context.getResources().getBoolean(R.bool.def_dialer_settings_diagport_enabled)) {
            if (handlePRLVersion(context, dialString)
                    || handleIMEIDisplay(context, dialString, useSystemWindow)
                    || handleRegulatoryInfoDisplay(context, dialString)
                    || handleEngineerModeDisplay(context, dialString)
                    || handlePinEntry(context, dialString)
                    || handleAdnEntry(context, dialString, textField)
                    || handleSecretCode(context, dialString)
                    || handleFactorySetCode(context, dialString)
                    || handleSetDiagPortCode(context, dialString)) {
                return true;
            }
        } else {
            if (handlePRLVersion(context, dialString)
                    || handleIMEIDisplay(context, dialString, useSystemWindow)
                    || handleRegulatoryInfoDisplay(context, dialString)
                    || handleEngineerModeDisplay(context, dialString)
                    || handlePinEntry(context, dialString)
                    || handleAdnEntry(context, dialString, textField)
                    || handleQualcommQLog(context, dialString)
                    || handleGpsTool(context, dialString)
                    || handleTounchCal(context, dialString)
                    || handleScanType(context, dialString)
                    || handleScanAgeTest(context, dialString)
                    || handleTestingEntry(context, dialString)
                    || handleDeviceInfoEntry(context, dialString, useSystemWindow)
                    || handlePWVBUILDIDEntry(context, dialString, useSystemWindow)
                    || handleSecretCode(context, dialString)
	                // add max3250
	                || handleMaxTool(context, dialString)
	                || handleCheckTrigger(context,dialString)
                    || handleShowNetworkSetting(context, dialString)) {
                return true;
            }
        }
        return false;
    }
    
    
    static private boolean handleShowNetworkSetting(Context context, String input) {
        int len = input.length();
        if (len > 4 && input.equals("*#410")) {
        	Intent intent = new Intent();
            intent.setClassName("com.android.phone","com.android.phone.MobileNetworkSettings");
            intent.putExtra("networksetting",true);
            context.startActivity(intent);
            return true;
        }
        return false;
    }
    

   static private boolean handleQualcommQLog(Context context, String input) {
        int len = input.length();
        if (len > 4 && input.equals(QUAL_DISPLAY_QLOG)) {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName("com.qualcomm.qlogcat", "com.qualcomm.qlogcat.QLogcatActivity"));
            context.startActivity(intent);
            return true;
        }
        return false;
    }

    // device info display command Added by daiqian 20140814 --start
    static boolean handleDeviceInfoEntry(Context context, String input, boolean useSystemWindow) {
        //Secret codes are in the form *#*#<code>#*#*

        int len = input.length();
	    if(input.equals(TESTING_NAME)){
		    mQcRilOemHook = new QcRilHook(context, mQcrilHookCb);
	    }
        if (len > 4 && input.equals(DEVICEINFO_DISPLAY)) {
            showDeviceInfoPanel(context, useSystemWindow);
            return true;
        }
        return false;
    }
    static void showDeviceInfoPanel(Context context, boolean useSystemWindow) {
        mQcRilOemHook = new QcRilHook(context, mQcrilHookCb);
        String aRMStr = "";
        try {
            aRMStr = SystemProperties.get("gsm.version.baseband","");
        } catch (RuntimeException e) {
            // No recovery
        }
        if(aRMStr != null){
            int len = aRMStr.length();
            if(len > 32){
               aRMStr = aRMStr.substring(0, 31);
            }
        }
        //String modStr = getModemStr();
        LinearLayout view = new LinearLayout(context);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        view.setOrientation(LinearLayout.VERTICAL);
/*
        final TextView hwn1, hwn2;
        final TextView swn1, swn2;
        final TextView armn1, armn2;
        final TextView mod1, mod2;
        final TextView qcn1, qcn2;
*/
        hwn1 = new TextView(context);
        hwn2 = new TextView(context);
        swn1 = new TextView(context);
        swn2 = new TextView(context);
        armn1 = new TextView(context);
        armn2 = new TextView(context);
        //mod1 = new TextView(context);
        //mod2 = new TextView(context);
        qcn1 = new TextView(context);
        qcn2 = new TextView(context);
        baseLine1 = new TextView(context);
        baseLine2 = new TextView(context);
        view.addView(hwn1, layoutParams);
        view.addView(hwn2, layoutParams);
        view.addView(swn1, layoutParams);
        view.addView(swn2, layoutParams);
        view.addView(armn1, layoutParams);
        view.addView(armn2, layoutParams);
        //view.addView(mod1, layoutParams);
        //view.addView(mod2, layoutParams);
        view.addView(qcn1, layoutParams);
        view.addView(qcn2, layoutParams);
        view.addView(baseLine1, layoutParams);
        view.addView(baseLine2, layoutParams);
        hwn1.setText(R.string.hardware_version);
        hwn2.setText(mHWNStr);
        swn1.setText(R.string.software_version);
        swn2.setText(Build.DISPLAY);
        armn1.setText(R.string.arm_version);
        armn2.setText(aRMStr);
        //mod1.setText(R.string.modem_version);
        //mod2.setText(modStr);
        qcn1.setText(R.string.qcn_version);
        qcn2.setText(qcnStr);
        baseLine1.setText(R.string.baseline);
        baseLine2.setText("2154");

        AlertDialog alert = new AlertDialog.Builder(context)
                .setTitle(R.string.device_version)
                .setView(view)
                .setPositiveButton(android.R.string.ok, null)
                .setCancelable(false).show();
    }
    static String getModemStr() {
        String str1 = "/sys/devices/soc0/build_id/";
        String str2 = null;

        try {
            FileReader localFileReader = new FileReader(str1);
            BufferedReader localBufferedReader = new BufferedReader(localFileReader, 8192);
            str2 = localBufferedReader.readLine();

        } catch (IOException e) {
        }
        return str2;
    }

    /**
     * Display PWV BUILD ID when input *#317
     *
     * @param context the context to use
     * @param input the text to check for a secret code in
     * @return true if a secret code was encountered
     */
    static boolean handlePWVBUILDIDEntry(Context context, String input, boolean useSystemWindow) {
        //Secret codes are in the form *#*#<code>#*#*
        int len = input.length();
        if (len > 4 && input.equals(PWV_BUILD_ID)) {
            showPWVBUILDID(context, useSystemWindow);
            return true;
        }
        return false;
    }

    static void showPWVBUILDID(Context context, boolean useSystemWindow) {
        String PWV_ID;
        PWV_ID = Build.INNER_VERSION;

        AlertDialog alert = new AlertDialog.Builder(context)
                .setTitle(R.string.pwv_build_id)
                .setMessage(PWV_ID)
                .setPositiveButton(android.R.string.ok, null)
                .setCancelable(false).show();
        //alert.getWindow().setType(WindowManager.LayoutParams.TYPE_PRIORITY_PHONE);
    }
    //modified by daiqian at 20111010 end

    static boolean handleGpsTool(Context context, String input) {
        //Secret codes are in the form *#*#<code>#*#*
        int len = input.length();
        if (len > 4 && input.equals(GPS_TOOL)) {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName("com.chartcross.gpstest", "com.chartcross.gpstest.GPSTest"));
            context.startActivity(intent);
            return true;
        }
        return false;
    }
    static boolean handleScanType(Context context, String input) {
        int len = input.length();
        if(len > 4 && (input.equals(TESTING_SCAN_TYPE))) {
            Intent intent = new Intent("android.intent.action.SCANNER_TYPE_SETTINGS");
            try {
                context.startActivity(intent);
            } catch (ActivityNotFoundException e) {
                e.printStackTrace();
            }
            return true;
        }
        return false;
    }
    static boolean handleScanAgeTest(Context context ,String input){
        int len = input.length();
        if(len > 4 && (input.equals(SCAN_AGE))) {
            Intent intent = new Intent("android.intent.action.AGEING_DEVICE");
            try {
                context.startActivity(intent);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return true;
        }
        return false;  
    }
    static boolean handleTounchCal(Context context, String input) {
        //Secret codes are in the form *#*#<code>#*#*
        int len = input.length();
        if (len > 4 && input.equals(Fine_Touch_Cal)) {
		    if(Build.TOUCHCAL.equals("true")){
            		Intent intent = new Intent();
            		intent.setComponent(new ComponentName("com.dongbu.finetouchm", "com.dongbu.finetouchm.MainActivity"));
            		context.startActivity(intent);
                	return true;
		    }
        }
        return false;
    }

    /**
     * Handle factory test tool open request
     *
     * @param context the context to use
     * @param input   the input code from the dialpad
     * @return true if a secret code was encounted
     */
    static boolean handleTestingEntry(Context context, String input) {
        int len = input.length();
        if(len > 4 && (input.equals(TESTING_TOOL) || input.equals(TESTING_TOOL_319) || input.equals(TESTING_TOOL_UROVO))) {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName("com.qualcomm.factory", "com.qualcomm.factory.Framework.Framework"));
            if(input.equals(TESTING_TOOL_319)) {
                intent.putExtra("msg",false);
            } else {
                intent.putExtra("msg",true);
            }
            try {
                context.startActivity(intent);
            } catch (ActivityNotFoundException e) {
                AlertDialog alert = new AlertDialog.Builder(context)
                                           .setTitle(R.string.alert_title_error)
                                           .setMessage(R.string.factory_test_noexist_error)
                                           .setPositiveButton(android.R.string.ok, null)
                                           .setCancelable(false)
                                           .show();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return true;
        }
        return false;
    }

    static private boolean handlePRLVersion(Context context, String input) {
        if (input.equals(PRL_VERSION_DISPLAY)) {
            try {
                Intent intent = new Intent("android.intent.action.ENGINEER_MODE_DEVICEINFO");
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                return true;
            } catch (ActivityNotFoundException e) {
                Log.d(TAG, "no activity to handle showing device info");
            }
        }
        return false;
    }

    static private boolean handleSetDiagPortCode(Context context, String input) {
        int len = input.length();
        if (input.equals(MMI_OPEN_DIAG_MENU_DISPLAY)) {
            Intent intent = new Intent(SECRET_CODE_ACTION,
                    Uri.parse("android_secret_code://" + input.substring(1, len - 1)));
            context.sendBroadcast(intent);
            return true;
        }
        return false;
    }
	
	//add max3250
    static boolean handleMaxTool(Context context, String input) {
        int len = input.length();
        if(len > 4 && input.equals(MAX_TOOL)) {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName("com.ped.maxq3250", "com.ped.maxq3250.Maxq3250"));
            try {
                context.startActivity(intent);
            } catch (ActivityNotFoundException e) {
                AlertDialog alert = new AlertDialog.Builder(context)
                                           .setTitle(R.string.alert_title_error)
                                           .setMessage(R.string.max_tool_noexist_error)
                                           .setPositiveButton(android.R.string.ok, null)
                                           .setCancelable(false)
                                           .show();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return true;
        }
        return false;
    }
    
  //add checktrigger
    static boolean handleCheckTrigger(Context context, String input) {
        int len = input.length();
        if(len > 4 && input.equals(CHECK_TRIGGER)) {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName("com.example.checktrigger", "com.example.checktrigger.MainActivity"));
            try {
                context.startActivity(intent);
            } catch (ActivityNotFoundException e) {
                AlertDialog alert = new AlertDialog.Builder(context)
                                           .setTitle(R.string.alert_title_error)
                                           .setMessage(R.string.check_trigger_noexist_error)
                                           .setPositiveButton(android.R.string.ok, null)
                                           .setCancelable(false)
                                           .show();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return true;
        }
        return false;
    }


    /**
     * Cleanup everything around this class. Must be run inside the main thread.
     *
     * This should be called when the screen becomes background.
     */
    public static void cleanup() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            Log.wtf(TAG, "cleanup() is called outside the main thread");
            return;
        }

        if (sPreviousAdnQueryHandler != null) {
            sPreviousAdnQueryHandler.cancel();
            sPreviousAdnQueryHandler = null;
        }
    }

    /**
     * Handles secret codes to launch arbitrary activities in the form of *#*#<code>#*#*.
     * If a secret code is encountered an Intent is started with the android_secret_code://<code>
     * URI.
     *
     * @param context the context to use
     * @param input the text to check for a secret code in
     * @return true if a secret code was encountered
     */
    static boolean handleSecretCode(Context context, String input) {
        // Secret codes are in the form *#*#<code>#*#*
        int len = input.length();
        if (len > 8 && input.startsWith("*#*#") && input.endsWith("#*#*")) {
            if ("4636".equals(input.substring(4, len - 4))) {
                //return false;
            }
            final Intent intent = new Intent(SECRET_CODE_ACTION,
                    Uri.parse("android_secret_code://" + input.substring(4, len - 4)));
            context.sendBroadcast(intent);
            return true;
        }

        return false;
    }

    static boolean handleFactorySetCode(Context context, String input) {
        int len = input.length();
        if (input.equals(MMI_FACTORY_MODE_DISPLAY)) {
            Intent intent = new Intent(SECRET_CODE_ACTION,
                    Uri.parse("android_secret_code://" + input.substring(1, len - 1)));
            context.sendBroadcast(intent);
            return true;
        }
        return false;
    }

    /**
     * Handle ADN requests by filling in the SIM contact number into the requested
     * EditText.
     *
     * This code works alongside the Asynchronous query handler {@link QueryHandler}
     * and query cancel handler implemented in {@link SimContactQueryCookie}.
     */
    static boolean handleAdnEntry(Context context, String input, EditText textField) {
        /* ADN entries are of the form "N(N)(N)#" */

        TelephonyManager telephonyManager =
                (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (telephonyManager == null
                || telephonyManager.getPhoneType() != TelephonyManager.PHONE_TYPE_GSM) {
            return false;
        }

        // if the phone is keyguard-restricted, then just ignore this
        // input.  We want to make sure that sim card contacts are NOT
        // exposed unless the phone is unlocked, and this code can be
        // accessed from the emergency dialer.
        KeyguardManager keyguardManager =
                (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        if (keyguardManager.inKeyguardRestrictedInputMode()) {
            return false;
        }

        int len = input.length();
        if ((len > 1) && (len < 5) && (input.endsWith("#"))) {
            try {
                // get the ordinal number of the sim contact
                int index = Integer.parseInt(input.substring(0, len-1));

                // The original code that navigated to a SIM Contacts list view did not
                // highlight the requested contact correctly, a requirement for PTCRB
                // certification.  This behaviour is consistent with the UI paradigm
                // for touch-enabled lists, so it does not make sense to try to work
                // around it.  Instead we fill in the the requested phone number into
                // the dialer text field.

                // create the async query handler
                QueryHandler handler = new QueryHandler (context.getContentResolver());

                // create the cookie object
                SimContactQueryCookie sc = new SimContactQueryCookie(index - 1, handler,
                        ADN_QUERY_TOKEN);

                // setup the cookie fields
                sc.contactNum = index - 1;
                sc.setTextField(textField);

                // create the progress dialog
                sc.progressDialog = new ProgressDialog(context);
                sc.progressDialog.setTitle(R.string.simContacts_title);
                sc.progressDialog.setMessage(context.getText(R.string.simContacts_emptyLoading));
                sc.progressDialog.setIndeterminate(true);
                sc.progressDialog.setCancelable(true);
                sc.progressDialog.setOnCancelListener(sc);
                sc.progressDialog.getWindow().addFlags(
                        WindowManager.LayoutParams.FLAG_BLUR_BEHIND);

                // display the progress dialog
                sc.progressDialog.show();

                // run the query.
                int subId = SubscriptionManager.from(context).getDefaultVoiceSubId();
                Uri uri = Uri.parse("content://icc/adn/subId/" + subId);
                handler.startQuery(ADN_QUERY_TOKEN, sc, uri,
                        new String[]{ADN_PHONE_NUMBER_COLUMN_NAME}, null, null, null);

                if (sPreviousAdnQueryHandler != null) {
                    // It is harmless to call cancel() even after the handler's gone.
                    sPreviousAdnQueryHandler.cancel();
                }
                sPreviousAdnQueryHandler = handler;
                return true;
            } catch (NumberFormatException ex) {
                // Ignore
            }
        }
        return false;
    }

    static boolean handlePinEntry(Context context, String input) {
        if ((input.startsWith("**04") || input.startsWith("**05")) && input.endsWith("#")) {
            int subId = SubscriptionManager.getDefaultVoiceSubId();
            try {
                return ITelephony.Stub.asInterface(ServiceManager.getService(
                        Context.TELEPHONY_SERVICE)).handlePinMmiForSubscriber(subId, input);
            } catch(RemoteException ex) {
                Log.e(TAG, "Remote Exception "+ex);
                return false;
            }
        }
        return false;
    }

    static boolean handleIMEIDisplay(Context context, String input, boolean useSystemWindow) {
        TelephonyManager telephonyManager =
                (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (telephonyManager != null && input.equals(MMI_IMEI_DISPLAY)) {
            int phoneType;
            int subId = SubscriptionManager.getDefaultVoiceSubId();
            phoneType = telephonyManager.getCurrentPhoneType(subId);
            if (telephonyManager.isMultiSimEnabled()) {
                return handleMSimIMEIDisplay(context, telephonyManager);
            }

            if (phoneType == TelephonyManager.PHONE_TYPE_GSM) {
                showIMEIPanel(context, useSystemWindow, telephonyManager);
                return true;
            } else if (phoneType == TelephonyManager.PHONE_TYPE_CDMA) {
                showMEIDPanel(context, useSystemWindow, telephonyManager);
                return true;
            }
        }

        return false;
    }

    private static boolean handleMSimIMEIDisplay(Context context,
            TelephonyManager telephonyManager) {
        StringBuffer deviceIds = new StringBuffer();
        int titleId = R.string.device_id;
        int count = telephonyManager.getPhoneCount();

        for (int i = 0; i < count; i++) {
            if (i != 0) {
                deviceIds.append("\n");
            }
            int[] subIds = SubscriptionManager.getSubId(i);
            int phoneType = telephonyManager.getCurrentPhoneType(subIds[0]);
            if (phoneType != TelephonyManager.PHONE_TYPE_GSM
                    && phoneType != TelephonyManager.PHONE_TYPE_CDMA) {
                return false;
            }
            deviceIds.append(context.getString(TelephonyManager.PHONE_TYPE_CDMA == phoneType
                    ? R.string.meid : R.string.imei) + " ");
            deviceIds.append(telephonyManager.getDeviceId(i));
        }

        AlertDialog alert = new AlertDialog.Builder(context)
                .setTitle(titleId)
                .setMessage(deviceIds.toString())
                .setPositiveButton(android.R.string.ok, null)
                .setCancelable(false)
                .show();
        return true;
    }

    private static boolean handleRegulatoryInfoDisplay(Context context, String input) {
        if (input.equals(MMI_REGULATORY_INFO_DISPLAY)) {
            Log.d(TAG, "handleRegulatoryInfoDisplay() sending intent to settings app");
            Intent showRegInfoIntent = new Intent(Settings.ACTION_SHOW_REGULATORY_INFO);
            try {
                context.startActivity(showRegInfoIntent);
            } catch (ActivityNotFoundException e) {
                Log.e(TAG, "startActivity() failed: " + e);
            }
            return true;
        }
        return false;
    }

    // TODO: Combine showIMEIPanel() and showMEIDPanel() into a single
    // generic "showDeviceIdPanel()" method, like in the apps/Phone
    // version of SpecialCharSequenceMgr.java.  (This will require moving
    // the phone app's TelephonyCapabilities.getDeviceIdLabel() method
    // into the telephony framework, though.)

    private static void showIMEIPanel(Context context, boolean useSystemWindow,
            TelephonyManager telephonyManager) {
        String imeiStr = null;
        int subId = SubscriptionManager.getDefaultVoiceSubId();
        int slotId = SubscriptionManager.getSlotId(subId);
        imeiStr = telephonyManager.getDeviceId(slotId);

        AlertDialog alert = new AlertDialog.Builder(context)
                .setTitle(R.string.imei)
                .setMessage(imeiStr)
                .setPositiveButton(android.R.string.ok, null)
                .setCancelable(false)
                .show();
    }

    private static void showMEIDPanel(Context context, boolean useSystemWindow,
            TelephonyManager telephonyManager) {
        String meidStr = null;
        int subId = SubscriptionManager.getDefaultVoiceSubId();
        int slotId = SubscriptionManager.getSlotId(subId);
        meidStr = telephonyManager.getDeviceId(slotId);

        AlertDialog alert = new AlertDialog.Builder(context)
                .setTitle(R.string.meid)
                .setMessage(meidStr)
                .setPositiveButton(android.R.string.ok, null)
                .setCancelable(false)
                .show();
    }

    static boolean handleEngineerModeDisplay(Context context, String input) {
        if (input.equals(MMI_ENGINEER_MODE_DISPLAY)) {
            Intent intent = new Intent(SECRET_CODE_ACTION,
                    Uri.parse("android_secret_code://3878"));
            context.sendBroadcast(intent);
            return true;
        }
        return false;
    }

    /*******
     * This code is used to handle SIM Contact queries
     *******/
    private static final String ADN_PHONE_NUMBER_COLUMN_NAME = "number";
    private static final String ADN_NAME_COLUMN_NAME = "name";
    private static final int ADN_QUERY_TOKEN = -1;

    /**
     * Cookie object that contains everything we need to communicate to the
     * handler's onQuery Complete, as well as what we need in order to cancel
     * the query (if requested).
     *
     * Note, access to the textField field is going to be synchronized, because
     * the user can request a cancel at any time through the UI.
     */
    private static class SimContactQueryCookie implements DialogInterface.OnCancelListener{
        public ProgressDialog progressDialog;
        public int contactNum;

        // Used to identify the query request.
        private int mToken;
        private QueryHandler mHandler;

        // The text field we're going to update
        private EditText textField;

        public SimContactQueryCookie(int number, QueryHandler handler, int token) {
            contactNum = number;
            mHandler = handler;
            mToken = token;
        }

        /**
         * Synchronized getter for the EditText.
         */
        public synchronized EditText getTextField() {
            return textField;
        }

        /**
         * Synchronized setter for the EditText.
         */
        public synchronized void setTextField(EditText text) {
            textField = text;
        }

        /**
         * Cancel the ADN query by stopping the operation and signaling
         * the cookie that a cancel request is made.
         */
        public synchronized void onCancel(DialogInterface dialog) {
            // close the progress dialog
            if (progressDialog != null) {
                progressDialog.dismiss();
            }

            // setting the textfield to null ensures that the UI does NOT get
            // updated.
            textField = null;

            // Cancel the operation if possible.
            mHandler.cancelOperation(mToken);
        }
    }

    /**
     * Asynchronous query handler that services requests to look up ADNs
     *
     * Queries originate from {@link #handleAdnEntry}.
     */
    private static class QueryHandler extends NoNullCursorAsyncQueryHandler {

        private boolean mCanceled;

        public QueryHandler(ContentResolver cr) {
            super(cr);
        }

        /**
         * Override basic onQueryComplete to fill in the textfield when
         * we're handed the ADN cursor.
         */
        @Override
        protected void onNotNullableQueryComplete(int token, Object cookie, Cursor c) {
            try {
                sPreviousAdnQueryHandler = null;
                if (mCanceled) {
                    return;
                }

                SimContactQueryCookie sc = (SimContactQueryCookie) cookie;

                // close the progress dialog.
                sc.progressDialog.dismiss();

                // get the EditText to update or see if the request was cancelled.
                EditText text = sc.getTextField();

                // if the textview is valid, and the cursor is valid and postionable
                // on the Nth number, then we update the text field and display a
                // toast indicating the caller name.
                if ((c != null) && (text != null) && (c.moveToPosition(sc.contactNum))) {
                    String name = c.getString(c.getColumnIndexOrThrow(ADN_NAME_COLUMN_NAME));
                    String number = c.getString(c.getColumnIndexOrThrow(ADN_PHONE_NUMBER_COLUMN_NAME));

                    // fill the text in.
                    text.getText().replace(0, 0, number);

                    // display the name as a toast
                    Context context = sc.progressDialog.getContext();
                    name = context.getString(R.string.menu_callNumber, name);
                    Toast.makeText(context, name, Toast.LENGTH_SHORT)
                        .show();
                }
            } finally {
                MoreCloseables.closeQuietly(c);
            }
        }

        public void cancel() {
            mCanceled = true;
            // Ask AsyncQueryHandler to cancel the whole request. This will fails when the
            // query already started.
            cancelOperation(ADN_QUERY_TOKEN);
        }
    }
}
