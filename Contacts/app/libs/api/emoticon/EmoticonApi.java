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
package com.suntek.mway.rcs.client.api.emoticon;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;

import com.suntek.mway.rcs.client.api.ClientApi;
import com.suntek.mway.rcs.client.aidl.plugin.callback.IEmoticonCallbackApi;
import com.suntek.mway.rcs.client.aidl.plugin.callback.IEmoticonCanSendCallback;
import com.suntek.mway.rcs.client.aidl.plugin.callback.IEmoticonPackagesNetCallbackApi;
import com.suntek.mway.rcs.client.aidl.plugin.callback.IEmoticonSetSuccessDownListener;
import com.suntek.mway.rcs.client.aidl.plugin.entity.emoticon.EmojiPackageBO;
import com.suntek.mway.rcs.client.aidl.plugin.entity.emoticon.EmoticonBO;
import com.suntek.mway.rcs.client.aidl.plugin.entity.emoticon.ResultBO;
import com.suntek.mway.rcs.client.aidl.plugin.entity.emoticon.UserBO;
import com.suntek.mway.rcs.client.aidl.emoticon.IEmoticonApi;
import com.suntek.mway.rcs.client.api.util.ServiceDisconnectedException;
import com.suntek.mway.rcs.client.api.util.VerificationUtil;
import com.suntek.mway.rcs.client.api.util.log.LogHelper;

public class EmoticonApi extends ClientApi {
    private static String serviceName = "com.suntek.mway.rcs.app.service.api.impl.emoticon.EmoticonApiService";
    IEmoticonApi myApi;

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            LogHelper.d("client api connect service");
            notifyServiceConnected();
            myApi = IEmoticonApi.Stub.asInterface(service);
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

    public EmoticonApi() {
        super(serviceName);
        super.initServiceConnect(mConnection);
    }

    public EmoticonBO getEmoticon(String emoticonId) throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper.i(String.format(Locale.getDefault(),
                "enter method:getEmoticon. [emoticonId]=%s", emoticonId));
        try {
            return myApi.getEmoticon(emoticonId);
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
        return null;
    }

    public List<EmojiPackageBO> queryEmojiPackages() throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper.i(String.format(Locale.getDefault(),
                "enter method:queryEmojiPackages. "));
        try {
            return myApi.queryEmojiPackages();
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
        return null;
    }

    public List<EmoticonBO> queryEmoticons(String packageId) throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper.i(String.format(Locale.getDefault(),
                "enter method:queryEmoticons. [packageId]=%s", packageId));
        try {
            return myApi.queryEmoticons(packageId);
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
        return null;
    }

    public EmoticonBO decodeEmoticon(String eid) throws ServiceDisconnectedException {
        LogHelper.i(String.format( Locale.getDefault(),"enter method:decodeEmoticon. [eid]=%s", eid));
        VerificationUtil.ApiIsNull(myApi);
        try {
            return myApi.decodeEmoticon(eid);
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
        return null;
    }

    public byte[] decrypt2Bytes(String emoticonId, int emoFileType) throws ServiceDisconnectedException {
        LogHelper.i(String.format( Locale.getDefault(),"enter method:decrypt2Bytes. [emoticonId,emoFileType]=%s,%s", emoticonId,emoFileType));
        VerificationUtil.ApiIsNull(myApi);
        try {
            return myApi.decrypt2Bytes(emoticonId, emoFileType);
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
        return null;
    }

    public void doAcceptEmoticon(String emoticonId, IEmoticonCallbackApi callback) throws ServiceDisconnectedException {
        LogHelper.i(String.format( Locale.getDefault(),"enter method:doAcceptEmoticon. [emoticonId]=%s", emoticonId));
        VerificationUtil.ApiIsNull(myApi);
        try {
            myApi.doAcceptEmoticon(emoticonId, callback);
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
    }

    public boolean emojiPackageExist(String packageId) throws ServiceDisconnectedException {
        LogHelper.i(String.format( Locale.getDefault(),"enter method:emojiPackageExist. [packageId]=%s", packageId));
        VerificationUtil.ApiIsNull(myApi);
        try {
            return myApi.emojiPackageExist(packageId);
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
        return false;
    }

    public String encodeEmoticon(String emoticonId) throws ServiceDisconnectedException {
        LogHelper.i(String.format( Locale.getDefault(),"enter method:encodeEmoticon. [emoticonId]=%s", emoticonId));
        VerificationUtil.ApiIsNull(myApi);
        try {
            return myApi.encodeEmoticon(emoticonId);
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
        return null;
    }

    public UserBO getCurrentUser() throws ServiceDisconnectedException {
        LogHelper.i(String.format( Locale.getDefault(),"enter method:getCurrentUser. "));
        VerificationUtil.ApiIsNull(myApi);
        try {
            return myApi.getCurrentUser();
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
        return null;
    }

    public boolean getCurrentUserState() throws ServiceDisconnectedException {
        LogHelper.i(String.format( Locale.getDefault(),"enter method:getCurrentUserState. "));
        VerificationUtil.ApiIsNull(myApi);
        try {
            return myApi.getCurrentUserState();
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
        return false;
    }

    public EmojiPackageBO getEmojiPackage(String packageId) throws ServiceDisconnectedException {
        LogHelper.i(String.format( Locale.getDefault(),"enter method:getEmojiPackage. [packageId]=%s", packageId));
        VerificationUtil.ApiIsNull(myApi);
        try {
            return myApi.getEmojiPackage(packageId);
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
        return null;
    }

    public EmojiPackageBO getEmojiPackageWithDetail(String packageId) throws ServiceDisconnectedException {
        LogHelper.i(String.format( Locale.getDefault(),"enter method:getEmojiPackageWithDetail. [packageId]=%s", packageId));
        VerificationUtil.ApiIsNull(myApi);
        try {
            return myApi.getEmojiPackageWithDetail(packageId);
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
        return null;
    }

    public String getEmoticonId(String emoticonId) throws ServiceDisconnectedException {
        LogHelper.i(String.format( Locale.getDefault(),"enter method:getEmoticonId. [emoticonId]=%s", emoticonId));
        VerificationUtil.ApiIsNull(myApi);
        try {
            return myApi.getEmoticonId(emoticonId);
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
        return null;
    }

    public String getStorageRootPath() throws ServiceDisconnectedException {
        LogHelper.i(String.format( Locale.getDefault(),"enter method:getStorageRootPath. "));
        VerificationUtil.ApiIsNull(myApi);
        try {
            return myApi.getStorageRootPath();
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
        return null;
    }

    public void isCanSend(String emoticonId, String phone, IEmoticonCanSendCallback cansendCallback) throws ServiceDisconnectedException {
        LogHelper.i(String.format( Locale.getDefault(),"enter method:isCanSend. [emoticonId, phone]=%s, %s", emoticonId, phone));
        VerificationUtil.ApiIsNull(myApi);
        try {
            myApi.isCanSend(emoticonId, phone, cansendCallback);
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
    }

    public void queryEmojiPackagesNet(IEmoticonPackagesNetCallbackApi callback) throws ServiceDisconnectedException {
        LogHelper.i(String.format( Locale.getDefault(),"enter method:queryEmojiPackagesNet. [callback]=%s", callback.toString()));
        VerificationUtil.ApiIsNull(myApi);
        try {
            myApi.queryEmojiPackagesNet(callback);
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
    }

    public List<EmojiPackageBO> queryEmojiPackagesWithDetail() throws ServiceDisconnectedException {
        LogHelper.i(String.format( Locale.getDefault(),"enter method:queryEmojiPackagesWithDetail."));
        VerificationUtil.ApiIsNull(myApi);
        try {
            return myApi.queryEmojiPackagesWithDetail();
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
        return null;
    }

    public List<EmoticonBO> queryEmoticonName(String emoticonName) throws ServiceDisconnectedException {
        LogHelper.i(String.format( Locale.getDefault(),"enter method:queryEmoticonName. [emoticonName]=%s", emoticonName));
        VerificationUtil.ApiIsNull(myApi);
        try {
            return myApi.queryEmoticonName(emoticonName);
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
        return null;
    }

    public void setSuccessDownListener(IEmoticonSetSuccessDownListener downListener) throws ServiceDisconnectedException {
        LogHelper.i(String.format( Locale.getDefault(),"enter method:setSuccessDownListener. "));
        VerificationUtil.ApiIsNull(myApi);
        try {
            myApi.setSuccessDownListener(downListener);
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
    }

    public void unreSuccessListenter() throws ServiceDisconnectedException {
        LogHelper.i(String.format( Locale.getDefault(),"enter method:unreSuccessListenter. "));
        VerificationUtil.ApiIsNull(myApi);
        try {
            myApi.unreSuccessListenter();
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
    }
}
