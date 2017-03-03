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
package com.suntek.mway.rcs.client.api.publicaccount.impl;

import java.util.List;
import java.util.Locale;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;

import com.suntek.mway.rcs.client.api.ClientApi;
import com.suntek.mway.rcs.client.aidl.plugin.IPublicAccountAPI;
import com.suntek.mway.rcs.client.aidl.plugin.entity.pubacct.PublicAccounts;
import com.suntek.mway.rcs.client.aidl.plugin.entity.pubacct.PublicAccountsDetail;
import com.suntek.mway.rcs.client.api.publicaccount.callback.PublicAccountCallback;
import com.suntek.mway.rcs.client.api.util.ServiceDisconnectedException;
import com.suntek.mway.rcs.client.api.util.VerificationUtil;
import com.suntek.mway.rcs.client.api.util.log.LogHelper;

public class PublicAccountApi extends ClientApi {

    private static String  serviceName = "com.suntek.mway.rcs.app.service.pubacct.plugin.service.PublicAccountPluginService";

    IPublicAccountAPI myApi;
    PublicAccountCallback theCallback;

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            LogHelper.d("client api connect service");
            myApi = IPublicAccountAPI.Stub.asInterface(service);
            notifyServiceConnected();
        }

        public void onServiceDisconnected(ComponentName className) {
            if(isNormallyClosed || reconnectionTimes > MAX_RECONECTION_TIMES) {
                LogHelper.d("client api disconnect service...");
                try {
                    if (myApi != null && theCallback != null) myApi.unregisterCallback(theCallback);
                } catch (Exception ex) {
                    // TODO Auto-generated catch block
                    LogHelper.e(ex.getMessage(), ex);
                }
                theCallback = null;
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

    public PublicAccountApi() {
        super(serviceName);
        super.initServiceConnect(mConnection);
    }


    public void unregisterCallback(PublicAccountCallback callback)throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        try {
            myApi.unregisterCallback(callback);
        } catch (RemoteException ex) {
            // TODO Auto-generated catch block
            LogHelper.e(ex.getMessage(),ex);
        }
    }

    public void getUserSubscribePublicList(PublicAccountCallback callback) throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper.i(String.format( Locale.getDefault(),"enter method getUserSubscribePublicList. "));
        /*if (order != 0 && order != 1) {
            LogHelper.i("order field value must be 0 or 1");
            return;
        }*/
        try {
            myApi.registerCallback(callback);
            myApi.getUserSubscribePublicList();
        } catch (Exception ex) {
            // TODO Auto-generated catch block
            LogHelper.e(ex.getMessage(),ex);
        }
    }

    public void getPublicMenuInfo(String uuid,PublicAccountCallback callback) throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper.i(String.format( Locale.getDefault(),"enter method getPublicMenuInfo. [uuid]=%s", uuid));
        try {
            myApi.registerCallback(callback);
            myApi.getPublicMenuInfo(uuid);
        } catch (Exception ex) {
            // TODO Auto-generated catch block
            LogHelper.e(ex.getMessage(),ex);
        }
    }

    public void getPublicDetail(String uuid,PublicAccountCallback callback) throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper.i(String.format( Locale.getDefault(),"enter method getPublicDetail. [uuid]=%s", uuid));
        try {
            myApi.registerCallback(callback);
            myApi.getPublicDetail(uuid);
        } catch (Exception ex) {
            // TODO Auto-generated catch block
            LogHelper.e(ex.getMessage(),ex);
        }
    }

    public void addSubscribe(String uuid,PublicAccountCallback callback) throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper.i(String.format( Locale.getDefault(),"enter method addSubscribe. [uuid]=%s", uuid));
        try {
            myApi.registerCallback(callback);
            myApi.addSubscribe(uuid);
        } catch (Exception ex) {
            // TODO Auto-generated catch block
            LogHelper.e(ex.getMessage(),ex);
        }
    }


    public void cancelSubscribe(String uuid,PublicAccountCallback callback) throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper.i(String.format( Locale.getDefault(),"enter method cancelSubscribe. [uuid]=%s", uuid));
        try {
            myApi.registerCallback(callback);
            myApi.cancelSubscribe(uuid);
        } catch (Exception ex) {
            // TODO Auto-generated catch block
            LogHelper.e(ex.getMessage(),ex);
        }
    }

    public boolean complainPublic(String uuid,String reason, String description, int type, String data ,PublicAccountCallback callback) throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper.i(String.format( Locale.getDefault(),"enter method complainPublic. [uuid,reason,description,type,data]=%s,%s,%s,%d,%s", uuid,reason,description,type,data));
        boolean flag = false;
        try {
            myApi.registerCallback(callback);
            flag = myApi.complainPublic(uuid, reason, description, type, data);
        } catch (Exception ex) {
            // TODO Auto-generated catch block
            LogHelper.e(ex.getMessage(),ex);
        }
        return flag;
    }

    public boolean getPreMessage(String uuid, String timestamp, int order, int pageSize, int pageNum,
            PublicAccountCallback callback) throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        boolean flag = false;
        try {
            myApi.registerCallback(callback);
            flag = myApi.getPreMessage(uuid, timestamp, order, pageSize, pageNum);
        } catch (Exception ex) {
            // TODO Auto-generated catch block
            LogHelper.e(ex.getMessage(),ex);
        }
        return flag;
    }

    public boolean getPublicList(String keywords, int pageSize, int pageNum, int order,
            PublicAccountCallback callback) throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper.i(String.format( Locale.getDefault(),"enter method getPublicList. [keywords,pageSize,pageNum,order]=%s,%d,%d,%d", keywords,pageSize,pageNum,order));
        boolean flag = false;
        try {
            myApi.registerCallback(callback);
            flag = myApi.getPublicList(keywords, pageSize, pageNum, order);
        } catch (Exception ex) {
            // TODO Auto-generated catch block
            LogHelper.e(ex.getMessage(),ex);
        }
        return flag;
    }

    public PublicAccountsDetail getPublicDetailCache(String uuid) throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        try {
            return myApi.getPublicDetailCache(uuid);
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(),ex);
            return null;
        }
    }

    public List<PublicAccounts> getUserSubscribePublicListCache(int order, int pageSize, int pageNum) throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        try {
            return myApi.getUserSubscribePublicListCache(order, pageSize, pageNum);
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(),ex);
            return null;
        }
    }

    public boolean getRecommendPublic(int type, int pageSize, int pageNum, PublicAccountCallback callback)
            throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        if (pageSize <= 0 || pageNum <= 0) {
            LogHelper.i("params is not valid");
            return false;
        }

        boolean flag = false;
        try {
            myApi.registerCallback(callback);
            flag = myApi.getRecommendPublic(type, pageSize, pageNum);
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(),ex);
        }
        return flag;
    }

    public boolean setAcceptStatus(String uuid, int acceptStatus, PublicAccountCallback callback)
            throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        boolean flag = false;
        try {
            myApi.registerCallback(callback);
            flag = myApi.setAcceptStatus(uuid, acceptStatus);
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
        return flag;
    }
}
