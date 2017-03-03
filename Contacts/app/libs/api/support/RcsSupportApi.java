/*
 * Copyright (c) 2014 pci-suntektech Technologies, Inc.  All Rights Reserved.
 * pci-suntektech Technologies Proprietary and Confidential.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.suntek.mway.rcs.client.api.support;

import com.suntek.mway.rcs.client.aidl.constant.BroadcastConstants;
import com.suntek.mway.rcs.client.aidl.setting.RcsUserProfileInfo;
import com.suntek.mway.rcs.client.api.RCSServiceListener;
import com.suntek.mway.rcs.client.api.autoconfig.RcsAccountApi;
import com.suntek.mway.rcs.client.api.util.ServiceDisconnectedException;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.RemoteException;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Dynamically detect the installation status of RCS components during runtime.
 *
 * @author lrb
 *
 */
public class RcsSupportApi {
    /**
     * The package name of RcsService module.
     */
    private static final String RCS_SERVICE_PACKAGE_NAME = "com.suntek.mway.rcs.app.service";

    /**
     * The package name of RcsPlugin module.
     */
    private static final String RCS_PLUGIN_PACKAGE_NAME = "com.suntek.mway.rcs.app.plugin";

    private static final String RCS_PLUGIN_CENTER_PACKAGE_NAME = "com.cmri.rcs.plugincenter";

    /**
     * The Profile plug-in that provided by CMCC.
     */
    public static final int PLUGIN_PROFILE = 0;

    /**
     * The Public Account plug-in that provided by CMCC.
     */
    public static final int PLUGIN_PUBLIC_ACCOUNT = 1;

    /**
     * The QR-Code plug-in that provided by CMCC.
     */
    public static final int PLUGIN_QR_CODE = 2;

    /**
     * The Enhance Call Screen plug-in that provided by CMCC.
     */
    public static final int PLUGIN_ENHANCE_CALL_SCREEN = 3;

    /**
     * The Cloud File Sharing plug-in that provided by CMCC.
     */
    public static final int PLUGIN_CLOUD_FILE_SHARING = 4;

    /**
     * The Emotions Store plug-in that provided by CMCC.
     */
    public static final int PLUGIN_EMOTIONS_STORE = 5;

    /**
     * The Plug-in Center plug-in that provided by CMCC.
     */
    public static final int PLUGIN_PLUGIN_CENTER = 6;

    private static final String TAG = "RCS_UI";
    private static final int DMS_VERSION_UNKNOWN = -999;

    /**
     * Check that the RcsService module is been installed.
     *
     * @param context
     * @return true if RcsService module is installed.
     */
    public static boolean isRcsServiceInstalled(Context context) {
        return isPackageInstalled(context, RCS_SERVICE_PACKAGE_NAME);
    }

    /**
     * Check that the RcsService module is been installed.
     *
     * @param context
     * @return true if RcsService module is installed.
     */
    public static boolean isRcsPluginInstalled(Context context) {
        return isPackageInstalled(context, RCS_PLUGIN_PACKAGE_NAME);
    }

    public static boolean isRcsPluginCenterInstalled(Context context) {
        return isPackageInstalled(context, RCS_PLUGIN_CENTER_PACKAGE_NAME);
    }

    private static boolean isPackageInstalled(Context context, String packageName) {
        PackageManager pm = context.getPackageManager();
        List<ApplicationInfo> installedApps = pm
                .getInstalledApplications(PackageManager.GET_UNINSTALLED_PACKAGES);

        for (ApplicationInfo info : installedApps) {
            if (packageName.equals(info.packageName)) {
                return true;
            }
        }

        return false;
    }

    private RcsAccountApi mAccountApi;
    private Context mContext;

    private boolean mIsRcsServiceInstalled;
    private boolean mIsSimAvailableForRcs;

    /**
     * Dynamically detect the supported plug-in.
     *
     * @param context
     * @return The plug-in list that is supported.
     */
    public static List<Integer> getSupportedPlugins(Context context) {
        List<Integer> supportedPluginIds = new ArrayList<Integer>();

        if (isRcsPluginInstalled(context)) {
            // TODO 增加检测插件安装的机制，动态返回可用的插件
            supportedPluginIds.add(PLUGIN_PROFILE);
            supportedPluginIds.add(PLUGIN_PUBLIC_ACCOUNT);
            supportedPluginIds.add(PLUGIN_QR_CODE);
            supportedPluginIds.add(PLUGIN_ENHANCE_CALL_SCREEN);
            supportedPluginIds.add(PLUGIN_CLOUD_FILE_SHARING);
            supportedPluginIds.add(PLUGIN_EMOTIONS_STORE);
            supportedPluginIds.add(PLUGIN_PLUGIN_CENTER);
        }

        return supportedPluginIds;
    }

    public void init(Context context) {
        mContext = context;
        mIsRcsServiceInstalled = isRcsServiceInstalled(context);

        mAccountApi = new RcsAccountApi();
        if (mIsRcsServiceInstalled) {
            init();
        }
    }

    private void init() {
        new Thread() {
            @Override
            public void run() {
                long t0, t1;
                t0 = System.currentTimeMillis();
                initRcsAccountApi();
                t1 = System.currentTimeMillis();
                Log.d(TAG, "initRcsAccountApi cost " + (t1 - t0) + " ms");
            }
        }.start();
    }

    private BroadcastReceiver userStatusChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            mIsSimAvailableForRcs = isSimAvailableForRcs();
        }
    };

    private void initRcsAccountApi() {
        mAccountApi.init(mContext, new RCSServiceListener() {
            @Override
            public void onServiceDisconnected() throws RemoteException {
                Log.d(TAG, "RcsAccountApi disconnected");
            }

            @Override
            public void onServiceConnected() throws RemoteException {
                Log.d(TAG, "RcsAccountApi connected");
                mIsSimAvailableForRcs = isSimAvailableForRcs();

                IntentFilter filter = new IntentFilter(BroadcastConstants.ACTION_DMS_USER_STATUS_CHANGED);
                mContext.registerReceiver(userStatusChangedReceiver, filter);
            }
        });
    }

    /**
     * Return whether the RCS is support. In general, use this method to define whether the RCS
     * related UI entrance should display. The result might not accurate when it is still
     * initializing. This is a compromise for performance.
     * @param context
     * @return Whether RCS is supported.
     */
    public boolean isRcsSupported() {
        return mIsRcsServiceInstalled && mIsSimAvailableForRcs;
//        return true;
    }

    /**
     * Return whether the RCS is online. The result might not accurate when it is still
     * initializing. This is a compromise for performance.
     * @param context
     * @return Whether RCS is supported.
     */
    public boolean isOnline() {
        try {
            return mAccountApi.isOnline();
        } catch (ServiceDisconnectedException e) {
            Log.w(TAG, "Failed invoking isOnline(). " + e.getMessage());
            return false;
        }
    }

    private boolean isSimAvailableForRcs() {
        int dmVersion = getDmVersion();
        Log.d(TAG, "DM version is " + dmVersion);

        // If the version is larger than 0, the SIM is available for RCS.
        return dmVersion > 0;
    }

    private int getDmVersion() {
        try {
            RcsUserProfileInfo userProfile = mAccountApi.getRcsUserProfileInfo();
            return Integer.valueOf(userProfile.getVersion());
        } catch (Exception e) {
            Log.w(TAG, "Failed getting DM version. " + e.getMessage());
            return DMS_VERSION_UNKNOWN;
        }
    }
}
