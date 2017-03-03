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
package com.suntek.mway.rcs.client.api.blacklist.impl;

import java.util.List;
import java.util.Locale;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;

import com.suntek.mway.rcs.client.api.ClientApi;
import com.suntek.mway.rcs.client.aidl.blacklist.IBlackListApi;
import com.suntek.mway.rcs.client.api.util.ServiceDisconnectedException;
import com.suntek.mway.rcs.client.api.util.VerificationUtil;
import com.suntek.mway.rcs.client.api.util.log.LogHelper;

/**
 * <p>Title: BlackListApi class</p>
 * <p>
 * Description: The class <code>BlackListApi</code> offers the functions of
 * operating the blacklist information.
 * In order to use BlackListApi, one must to initialize the API
 * in the method onCreate() in UI(Activity for example).<p></p>
 * Here is the pseudo code example:<p></p>
 * blacklistApi = new BlackListApi();<br/>
 * blacklistApi.init(this);
 * </p>
 * <p>
 * Copyright: Copyright (c) 2014
 * </p>
 * <p>
 * Company: pci-suntek
 * </p>
 * @author YE JIE MING
 * @version 1.0
 *
 */
public class BlackListApi extends ClientApi {

    /** The service name. */
    private static String serviceName = "com.suntek.mway.rcs.app.service.api.impl.blacklist.BlacklistApiService";

    /** The api. */
    private static IBlackListApi myApi;

    /** The service connection. */
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            myApi = IBlackListApi.Stub.asInterface(service);
            notifyServiceConnected();
            LogHelper.d("IBlackListApi have success connect, api="+myApi);
        }

        public void onServiceDisconnected(ComponentName className) {
            if(isNormallyClosed || reconnectionTimes > MAX_RECONECTION_TIMES) {
                LogHelper.d("IBlackListApi api disconnect service");
                myApi = null;
                notifyServiceDisconnected();
            } else {
                LogHelper.d("illegal call IBlackListApi api disconnect service :"
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

    /**
     * Instantiates a new black list api.
     */
    public BlackListApi() {
        super(serviceName);
        super.initServiceConnect(mConnection);
    }

    /**
     * Adds the number to blacklist.
     *
     * @param number the number
     */
    public boolean add(String number) throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper.i(String.format( Locale.getDefault(),"enter method add. [number]=%s", number));
        if (!VerificationUtil.isNumber(number)) {
            LogHelper.i("number field value error");
            return false;
        }
        try {
            return myApi.add(VerificationUtil.formatNumber(number));
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
        return false;
    }

    /**
     * Removes the number from blacklist.
     *
     * @param number the number
     */
    public boolean remove(String number) throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper.i(String.format( Locale.getDefault(),"enter method remove. [number]=%s", number));
        if (!VerificationUtil.isNumber(number)) {
            LogHelper.i("number field value error");
            return false;
        }
        try {
            return myApi.remove(VerificationUtil.formatNumber(number));
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
        return false;
    }

    /**
     * Clear the blacklist.
     */
    public void clear() throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper.i(String.format( Locale.getDefault(),"enter method clear. "));
        try {
            myApi.clear();
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
    }

    /**
     * Check whether the number is in blacklist or not.
     *
     * @param number the number
     * @return true, if successful
     */
    public boolean checkIsBlack(String number) throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper.i(String.format( Locale.getDefault(),"enter method checkIsBlack. [number]=%s", number));
        if (!VerificationUtil.isNumber(number)) {
            LogHelper.i("number field value error");
            return false;
        }
        try {
            return myApi.checkIsBlack(VerificationUtil.formatNumber(number));
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
        return false;//default value is non-black user
    }

    public List<String> getList() throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        try {
            return myApi.getList();
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
        return null;
    }
}
