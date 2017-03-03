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
package com.suntek.mway.rcs.client.api.mcontact;

import java.util.Locale;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;

import com.suntek.mway.rcs.client.api.ClientApi;
import com.suntek.mway.rcs.client.aidl.plugin.callback.IMContactSyncListener;
import com.suntek.mway.rcs.client.aidl.plugin.entity.mcontact.IntervalAction;
import com.suntek.mway.rcs.client.aidl.plugin.entity.mcontact.SyncAction;
import com.suntek.mway.rcs.client.api.util.ServiceDisconnectedException;
import com.suntek.mway.rcs.client.api.util.VerificationUtil;
import com.suntek.mway.rcs.client.api.util.log.LogHelper;
import com.suntek.mway.rcs.client.aidl.mcontact.IMcontactApi;

public class McontactApi extends ClientApi {
    private static String serviceName = "com.suntek.mway.rcs.app.service.api.impl.mcontact.McontactApiService";
    IMcontactApi myApi;

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            LogHelper.d("client api connect service");
            myApi = IMcontactApi.Stub.asInterface(service);
            notifyServiceConnected();
        }

        public void onServiceDisconnected(ComponentName className) {
            if (isNormallyClosed || reconnectionTimes > MAX_RECONECTION_TIMES) {
                LogHelper.d("client api disconnect service");
                myApi = null;
                notifyServiceDisconnected();
            } else {
                LogHelper.d("illegal call client api disconnect service :"
                        + reconnectionTimes);
                init(context, rcsListener);
                if (!isBinded()) {
                    // app is uninstalled
                    myApi = null;
                    notifyServiceDisconnected();
                }
                reconnectionTimes++;
            }
        }
    };

    public McontactApi() {
        super(serviceName);
        super.initServiceConnect(mConnection);
    }

    public void cancelIntervalSync() throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper.i(String.format(Locale.getDefault(),
                "enter method:downloadFileFromUrl. "));
        try {
            myApi.cancelIntervalSync();
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
    }

    public boolean getEnableAutoSync() throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper.i(String.format(Locale.getDefault(),
                "enter method:getEnableAutoSync. "));
        try {
            return myApi.getEnableAutoSync();
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
        return false;
    }

    public int getLocalContactCounts() throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper.i(String.format(Locale.getDefault(),
                "enter method:getLocalContactCounts. "));
        try {
            return myApi.getLocalContactCounts();
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
        return 0;
    }

    public boolean getOnlySyncEnableViaWifi()
            throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper.i(String.format(Locale.getDefault(),
                "enter method:getOnlySyncEnableViaWifi. "));
        try {
            return myApi.getOnlySyncEnableViaWifi();
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
        return false;
    }

    public int getRemoteContactCounts() throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper.i(String.format(Locale.getDefault(),
                "enter method:getRemoteContactCounts. "));
        try {
            return myApi.getRemoteContactCounts();
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
        return 0;
    }

    public void setOnlySyncEnableViaWifi(boolean status)
            throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper.i(String.format(Locale.getDefault(),
                "enter method:setOnlySyncEnableViaWifi. [status]=%b", status));
        try {
            myApi.setOnlySyncEnableViaWifi(status);
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
    }

    public void doSync(SyncAction action, IMContactSyncListener listener)
            throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper.i(String.format(Locale.getDefault(),
                "enter method:doSync. [action]=%d", action.ordinal()));
        try {
            myApi.doSync(action.ordinal(), listener);
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
    }

    public SyncAction getAutoSync() throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper.i(String.format(Locale.getDefault(),
                "enter method:getAutoSync. "));
        try {
            return SyncAction.valueOf(myApi.getAutoSync());
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
        return null;
    }

    public void setEnableAutoSync(boolean status, SyncAction syncAction)
            throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper.i(String.format(Locale.getDefault(),
                "enter method:setEnableAutoSync. [status, syncAction]=%b,%d",
                status, syncAction.ordinal()));
        try {
            myApi.setEnableAutoSync(status, syncAction.ordinal());
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
    }

    public void startIntervalSync(SyncAction syncAction,
            IntervalAction intervalAction, long time)
            throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper
                .i(String.format(
                        Locale.getDefault(),
                        "enter method:startIntervalSync. [syncAction, intervalAction, time]=%d,%d,%d",
                        syncAction.ordinal(), intervalAction.ordinal(), time));
        try {
            myApi.startIntervalSync(syncAction.ordinal(),
                    intervalAction.ordinal(), time);
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
    }

    public IntervalAction getIntervalSyncMode() throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper.i(String.format(Locale.getDefault(),
                "enter method:getIntervalSyncMode. "));
        try {
            return IntervalAction.valueOf(myApi.getIntervalSyncMode());
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
        return null;
    }

}
