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
package com.suntek.mway.rcs.client.api.mcloud;

import com.suntek.mway.rcs.client.aidl.mcloud.IMcloudFileApi;
import com.suntek.mway.rcs.client.aidl.plugin.callback.IMcloudOperationCtrl;
import com.suntek.mway.rcs.client.aidl.plugin.entity.mcloudfile.FileNode;
import com.suntek.mway.rcs.client.aidl.plugin.entity.mcloudfile.TransNode;
import com.suntek.mway.rcs.client.api.ClientApi;
import com.suntek.mway.rcs.client.api.util.FileSuffixException;
import com.suntek.mway.rcs.client.api.util.ServiceDisconnectedException;
import com.suntek.mway.rcs.client.api.util.VerificationUtil;
import com.suntek.mway.rcs.client.api.util.log.LogHelper;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;

import java.util.List;
import java.util.Locale;

public class McloudFileApi extends ClientApi {
    private static String serviceName = "com.suntek.mway.rcs.app.service.api.impl.mcloud.McloudApiService";
    IMcloudFileApi myApi;

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            LogHelper.d("client api connect service");
            myApi = IMcloudFileApi.Stub.asInterface(service);
            notifyServiceConnected();
        }

        public void onServiceDisconnected(ComponentName className) {
            if(isNormallyClosed || reconnectionTimes > MAX_RECONECTION_TIMES) {
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

    public McloudFileApi() {
        super(serviceName);
        super.initServiceConnect(mConnection);
    }

    public IMcloudOperationCtrl downloadFileFromUrl(String remoteUrl, String fileName, TransNode.TransOper transOper, int chatMessageId) throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper.i(String.format(Locale.getDefault(),
                "enter method:downloadFileFromUrl. [remoteUrl,fileName,transOper,chatMessageId]=%s,%s,%d,%d", remoteUrl, fileName, transOper.ordinal(), chatMessageId));
        try {
            IMcloudOperationCtrl operation = myApi.downloadFileFromUrl(remoteUrl, fileName, transOper.ordinal(), chatMessageId);
            LogHelper.d("operation=" + operation);
            return operation;
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
            return null;
        }
    }

    public void getShareFileList(int beginIndex, int endIndex) throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper.i(String.format(Locale.getDefault(),
                "enter method:getShareFileList. [beginIndex,endIndex]=%d,%d",
                beginIndex, endIndex));
        try {
            myApi.getShareFileList(beginIndex, endIndex);
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
    }

    public IMcloudOperationCtrl putFile(String localPath, String remotePath, TransNode.TransOper transOper) throws ServiceDisconnectedException, FileSuffixException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper.i(String.format(Locale.getDefault(),
                "enter method:putFile. [localPath,remotePath,transOper]=%s,%s,%d",
                localPath, remotePath, transOper.ordinal()));
        VerificationUtil.isCloudFile(localPath);
        try {
            IMcloudOperationCtrl operation = myApi.putFile(localPath, remotePath, transOper.ordinal());
            LogHelper.d("operation=" + operation);
            return operation;
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
            return null;
        }
    }

    public void shareFile(String fullPathInID, String shareDesc) throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper.i(String.format(Locale.getDefault(),
                "enter method:shareFile. [fullPathInID,shareDesc]=%s,%s",
                fullPathInID, shareDesc));
        try {
            myApi.shareFile(fullPathInID, shareDesc);
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
    }

    public void shareFileAndSend(String fullPathInID, String shareDesc,
            String contact, long threadId, String smsContentTemp)
            throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper
                .i(String.format(
                        Locale.getDefault(),
                        "enter method:shareFileAndSend. [fullPathInID,shareDesc,contact,threadId,smsContentTemp]=%s,%s,%s,%d,%s",
                        fullPathInID, shareDesc, contact, threadId, smsContentTemp));
        try {
            myApi.shareFileAndSend(fullPathInID, shareDesc, contact, threadId, smsContentTemp);
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
    }

    public void shareFileAndSendGroup(String fullPathInID, String shareDesc,
            long threadId, String conversationId, String groupId)
            throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper
                .i(String.format(
                        Locale.getDefault(),
                        "enter method:shareFileAndSendGroup. [fullPathInID,shareDesc,threadId,conversationId,groupId]=%s,%s,%d,%s,%s",
                        fullPathInID, shareDesc, threadId, conversationId, groupId));
        try {
            myApi.shareFileAndSendGroup(fullPathInID, shareDesc, threadId,
                    conversationId, groupId);
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
    }

    public void shareFileAndSendOne2Many(String fullPathInID, String shareDesc,
            List<String> contacts, long threadId, String smsContentTemp) throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper
                .i(String.format(
                        Locale.getDefault(),
                        "enter method:shareFileAndSendOne2Many. [fullPathInID,shareDesc,contacts,threadId,smsContentTemp]=%s,%s,%s,%d,%s",
                        fullPathInID, shareDesc, contacts.toString(), threadId, smsContentTemp));
        try {
            myApi.shareFileAndSendOne2Many(fullPathInID, shareDesc, contacts,
                    threadId, smsContentTemp);
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
    }

    public void getRemoteFileList(String remotePath, int beginIndex, int endIndex, FileNode.Order fileOrder) throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper.i(String.format(Locale.getDefault(),
                "enter method:getRemoteFileList. [remotePath,beginIndex,endIndex,fileOrder]=%s,%d,%d,%d",
                remotePath, beginIndex, endIndex, fileOrder.ordinal()));
        try {
            myApi.getRemoteFileList(remotePath, beginIndex, endIndex, fileOrder.ordinal());
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
    }
    
    public String getLocalRootPath() throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper.i("enter method:getLocalRootPath");
        try {
            return myApi.getLocalRootPath();
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
            return "";
        }
    }
}
