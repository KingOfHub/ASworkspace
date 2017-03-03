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
package com.suntek.mway.rcs.client.api.autoconfig;

import java.util.Locale;

import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;

import com.suntek.mway.rcs.client.api.ClientApi;
import com.suntek.mway.rcs.client.api.autoconfig.callback.AccountEventListener;
import com.suntek.mway.rcs.client.aidl.setting.IRcsSettingApi;
import com.suntek.mway.rcs.client.aidl.setting.LoginUser;
import com.suntek.mway.rcs.client.aidl.setting.RcsUserProfileInfo;
import com.suntek.mway.rcs.client.aidl.setting.SystemInfo;
import com.suntek.mway.rcs.client.api.util.ServiceDisconnectedException;
import com.suntek.mway.rcs.client.api.util.VerificationUtil;
import com.suntek.mway.rcs.client.api.util.log.LogHelper;

public class RcsAccountApi extends ClientApi {
    /** The service name. */
    private static String serviceName = "com.suntek.mway.rcs.app.service.api.impl.setting.RcsSettingApiService";

    /** The api. */
    private IRcsSettingApi myApi;

    /** The service connection. */
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            myApi = IRcsSettingApi.Stub.asInterface(service);
            notifyServiceConnected();
            LogHelper.d("IRcsSettingApi have success connect, api="+myApi);
        }

        public void onServiceDisconnected(ComponentName className) {
            if(isNormallyClosed || reconnectionTimes > MAX_RECONECTION_TIMES) {
                LogHelper.d("IRcsSettingApi api disconnect service");
                myApi = null;
                notifyServiceDisconnected();
            } else {
                LogHelper.d("illegal call IRcsSettingApi api disconnect service :"
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

    public RcsAccountApi() {
        super(serviceName);
        super.initServiceConnect(mConnection);
    }


    public RcsUserProfileInfo getRcsUserProfileInfo() throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        try {
            return myApi.getRcsUserProfileInfo();
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
        return null;
    }

    public SystemInfo getNetworkInfo() throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        try {
            return myApi.getNetworkInfo();
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
        return null;
    }

    public void acceptAutoReg() throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        try {
            myApi.acceptAutoReg();
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
    }

    public boolean isOnline() throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        try {
            return myApi.isOnline();
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
        return false;
    }

    private void login(LoginUser loginUser,AccountEventListener listener) throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
    }

    private void logout(LoginUser loginUser,AccountEventListener listener) throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
    }

    private void startService(Context context, AccountEventListener listener) throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
    }

    private void stopService(Context context, AccountEventListener listener) throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
    }

    public void checkOtp(String otp) throws RemoteException {
        LogHelper.i(String.format(Locale.getDefault(),
                "enter method:checkOtp. [otp]=%s", otp));
        try {
            myApi.checkOtp(otp);
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
            throw new RemoteException();
        }
    }

    public void start() throws ServiceDisconnectedException {
        LogHelper.i(String.format( Locale.getDefault(),"enter method:start. "));
        VerificationUtil.ApiIsNull(myApi);
        try {
            myApi.start();
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
    }
}
