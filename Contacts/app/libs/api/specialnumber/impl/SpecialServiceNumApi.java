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
package com.suntek.mway.rcs.client.api.specialnumber.impl;

import java.util.List;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;

import com.suntek.mway.rcs.client.api.ClientApi;
import com.suntek.mway.rcs.client.aidl.specialnumber.ISpecialServiceNumApi;
import com.suntek.mway.rcs.client.api.util.ServiceDisconnectedException;
import com.suntek.mway.rcs.client.api.util.VerificationUtil;
import com.suntek.mway.rcs.client.api.util.log.LogHelper;

public class SpecialServiceNumApi extends ClientApi{

    private static String serviceName = "com.suntek.mway.rcs.app.service.api.impl.specialnumber.SpecialServiceNumApiService";

    ISpecialServiceNumApi myApi;

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            LogHelper.d("ISpecialServiceNumApi api connect service");
            myApi = ISpecialServiceNumApi.Stub.asInterface(service);
            notifyServiceConnected();
        }

        public void onServiceDisconnected(ComponentName className) {
            if(isNormallyClosed || reconnectionTimes > MAX_RECONECTION_TIMES) {
                LogHelper.d("ISpecialServiceNumApi api disconnect service");
                myApi = null;
                notifyServiceDisconnected();
            } else {
                LogHelper.d("illegal call ISpecialServiceNumApi api disconnect service :"
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

    public SpecialServiceNumApi() {
        super(serviceName);
        super.initServiceConnect(mConnection);
    }

    /**
     * Add special number.
     *
     * @param number the number
     */
    public void add(String number)throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        try {
            myApi.add(number);
        } catch (Exception e) {
            LogHelper.e(e.getMessage(),e);
        }
    }

    /**
     * Close function, Set status to close.
     */
    public void closeFunction()throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        try {
            myApi.closeFunction();
        } catch (Exception e) {
            LogHelper.e(e.getMessage(),e);
        }
    }

    /**
     * Delete special number prefix.
     *
     * @param number the number
     * @return the string
     */
    public String delSpecialPreNum(String telephone)throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        try {
            return myApi.delSpecialPreNum(telephone);
        } catch (Exception e) {
            LogHelper.e(e.getMessage(),e);
            return "";
        }
    }

    /**
     * Gets the list of special numbers.
     *
     * @return the list
     */
    public List<String> getList()throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        try {
            return myApi.getList();
        } catch (Exception e) {
            LogHelper.e(e.getMessage(),e);
            return null;
        }
    }

    /**
     * Open function, Set status to open.
     */
    public void openFunction()throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        try {
            myApi.openFunction();
        } catch (Exception e) {
            LogHelper.e(e.getMessage(),e);
        }
    }

    /**
     * Removes a special number.
     *
     * @param number the number
     */
    public void remove(String number)throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        try {
            myApi.remove(number);
        } catch (Exception e) {
            LogHelper.e(e.getMessage(),e);
        }
    }

    /**
     * Removes all special number.
     */
    public void removeAll()throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        try {
            myApi.removeAll();
        } catch (Exception e) {
            LogHelper.e(e.getMessage(),e);
        }
    }
}
