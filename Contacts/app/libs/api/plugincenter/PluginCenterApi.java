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
package com.suntek.mway.rcs.client.api.plugincenter;

import java.util.Locale;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;

import com.suntek.mway.rcs.client.api.ClientApi;
import com.suntek.mway.rcs.client.api.support.RcsSupportApi;
import com.suntek.mway.rcs.client.api.util.ServiceDisconnectedException;
import com.suntek.mway.rcs.client.api.util.VerificationUtil;
import com.suntek.mway.rcs.client.api.util.log.LogHelper;
import com.suntek.mway.rcs.client.aidl.plugincenter.IPluginCenterApi;

public class PluginCenterApi extends ClientApi {
    private static String serviceName = "com.suntek.mway.rcs.app.service.api.impl.plugincenter.PluginCenterApiService";
    IPluginCenterApi myApi;

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            LogHelper.d("client api connect service");
            myApi = IPluginCenterApi.Stub.asInterface(service);
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

    public PluginCenterApi() {
        super(serviceName);
        super.initServiceConnect(mConnection);
    }

    public void intentApk() throws ServiceDisconnectedException {
        if (RcsSupportApi.isRcsPluginCenterInstalled(context)) {
            VerificationUtil.ApiIsNull(myApi);
            LogHelper.i(String.format(Locale.getDefault(),
                    "enter method:intentApk. "));
            try {
                myApi.intentApk();
            } catch (Exception ex) {
                LogHelper.e(ex.getMessage(), ex);
            }
        }
    }
}
