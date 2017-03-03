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
package com.suntek.mway.rcs.client.api;

import java.util.List;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.RemoteException;
import android.util.Log;

import com.suntek.mway.rcs.client.aidl.constant.APIConstant;
import com.suntek.mway.rcs.client.api.util.log.LogHelper;

public abstract class ClientApi implements ClientApiInterface {

    /** check whether the service is binded or not */
    private boolean isBinded = false;

    private String serviceName = null;
    private ServiceConnection mConnection = null;
    protected RCSServiceListener rcsListener = null;
    protected Context context = null;

    protected boolean isNormallyClosed = false;
    protected int reconnectionTimes = 1;
    protected final int MAX_RECONECTION_TIMES = 10;

    public ClientApi(String theService) {
        // TODO Auto-generated constructor stub
        serviceName = theService;
    }

    /**
     * judge the bind state about remote service.
     * @return true if binded, return false if unbinded.
     */
    public boolean isBinded() {
        return isBinded;
    }

    public void initServiceConnect(ServiceConnection theServiceConnection) {
        mConnection = theServiceConnection;
    }

    public void init(Context context, RCSServiceListener listener) {
        this.rcsListener = listener;
        this.context = context;
        if (context.getPackageName() != null
                && !"".equals(context.getPackageName())) {
            LogHelper.MYLOG_PATH_SDCARD_DIR = LogHelper.MYLOG_PATH_SDCARD_DIR
                    .replace("com.suntek.mway.rcs.service.api",
                            context.getPackageName());
        }
        // TODO Auto-generated constructor stub
        Intent intent = new Intent();
//        intent.setAction(serviceName);
        intent.setClassName("com.suntek.mway.rcs.app.service", serviceName);
        isBinded = context.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        LogHelper.d("bind " + serviceName + "--> result:" + isBinded);
    }

    public void destory(Context context) {
        try {
            if (isBinded) {
                LogHelper.d("destory()--> to destroy service : " + serviceName);
                isNormallyClosed = true;
                context.unbindService(mConnection);
            }
            else {
                LogHelper.i("destory()--> service("+serviceName+") already unbinded, do not need to destroy.");
            }
        } catch (Exception e) {
            LogHelper.e("unbind " + serviceName + "--> result:" + e.getMessage(),e);
        }finally {
            isBinded = false;
        }
    }


    /**
     * check the service is start or not.
     * @param ctx android.content.Context
     * @return true if the service is started , false otherwise.
     */
    public static boolean isMainServiceStarted(Context ctx) {
        ActivityManager activityManager = (ActivityManager)ctx.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> serviceList = activityManager.getRunningServices(Integer.MAX_VALUE);
         for(int i = 0; i < serviceList.size(); i++) {
               ActivityManager.RunningServiceInfo serviceInfo = serviceList.get(i);
               ComponentName serviceName = serviceInfo.service;
               if (serviceName.getClassName().equals(APIConstant.MAIN_SERVICE_NAME)) {
                     if (serviceInfo.pid != 0) {
                          return true;
                     } else {
                          return false;
                     }
               }
         }
         return false;
    }

    protected void notifyServiceConnected() {
        if (rcsListener != null) {
            try {
                rcsListener.onServiceConnected();
            } catch (RemoteException e) {
                LogHelper.e(e.getMessage(), e);
            }
        }
    }

    protected void notifyServiceDisconnected() {
        if (rcsListener != null) {
            try {
                rcsListener.onServiceDisconnected();
            } catch (RemoteException e) {
                LogHelper.e(e.getMessage(), e);
            }
        }
    }
}
