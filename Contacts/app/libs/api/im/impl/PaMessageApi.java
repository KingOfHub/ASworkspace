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
package com.suntek.mway.rcs.client.api.im.impl;

import java.util.List;
import java.util.Locale;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;

import com.suntek.mway.rcs.client.api.ClientApi;
import com.suntek.mway.rcs.client.aidl.contacts.RCSContact;
import com.suntek.mway.rcs.client.aidl.im.IPaMessageApi;
import com.suntek.mway.rcs.client.api.util.FileDurationException;
import com.suntek.mway.rcs.client.api.util.FileSuffixException;
import com.suntek.mway.rcs.client.api.util.FileTransferException;
import com.suntek.mway.rcs.client.api.util.ServiceDisconnectedException;
import com.suntek.mway.rcs.client.api.util.VerificationUtil;
import com.suntek.mway.rcs.client.api.util.log.LogHelper;

public class PaMessageApi extends ClientApi{

private static String serviceName = "com.suntek.mway.rcs.app.service.api.impl.im.PaMessageApiService";

    IPaMessageApi myApi;

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            LogHelper.d("IPaMessageApi api connect service");
            myApi = IPaMessageApi.Stub.asInterface(service);
            notifyServiceConnected();
        }

        public void onServiceDisconnected(ComponentName className) {
            if(isNormallyClosed || reconnectionTimes > MAX_RECONECTION_TIMES) {
                LogHelper.d("IPaMessageApi api disconnect service");
                myApi = null;
                notifyServiceDisconnected();
            } else {
                LogHelper.d("illegal call IPaMessageApi api disconnect service :"
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

    public PaMessageApi() {
        super(serviceName);
        super.initServiceConnect(mConnection);
    }

    /**
     * Send a text message to the public accounts.
     *
     * @param thread_id             the message threadId
     * @param sms_id             sms_id system SMS id, -1 if didn't use system SMS
     * @param uuid             the public account uuid
     * @param text             the message info
     * @throws RemoteException the remote exception
     */
    public void sendTextMessage(long thread_id, long sms_id, String uuid, String text) throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper.i(String.format( Locale.getDefault(),"enter method sendTextMessage. [thread_id,sms_id,uuid,text]=%d,%d,%s,%s", thread_id,sms_id,uuid,text));
        try{
            myApi.sendTextMessage(thread_id, sms_id, uuid, text);
        }catch(Exception e){
            LogHelper.e(e.getMessage(), e);
        }
    }

    /**
     * Send a picture message to the public accounts.
     *
     * @param thread_id             the message threadId
     * @param sms_id             sms_id system SMS id, -1 if didn't use system SMS
     * @param uuid             the public account uuid
     * @param filepath             path of picture file
     * @param quality             Hint to the compressor, 0-100. 0 meaning compress for small size, 100 meaning compress for max quality.
     * @throws FileSuffixException
     * @throws FileTransferException
     * @throws RemoteException the remote exception
     */
    public void sendImageFile(long thread_id, long sms_id, String uuid , String filepath, int quality) throws ServiceDisconnectedException, FileSuffixException, FileTransferException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper.i(String.format( Locale.getDefault(),"enter method sendImageFile. [thread_id,sms_id,uuid,filepath,quality]=%d,%d,%s,%s,%d", thread_id,sms_id,uuid,filepath,quality));
        VerificationUtil.isImageFile(filepath);
//        VerificationUtil.isFileSizeToLarge(filepath,
//                VerificationUtil.getImageFtMaxSize(context));
        if (quality < 0 || quality > 100) {
            LogHelper.i("quality field value must be between 0 to 100");
            return;
        }
        if (quality == 100) {
            VerificationUtil.isFileSizeToLarge(filepath, this.getImageFtMaxSize());
        }

        try{
            myApi.sendImageFile(thread_id, sms_id, uuid , filepath, quality);
        }catch(Exception e){
            LogHelper.e(e.getMessage(), e);
        }
    }

    /**
     * Send a audio message to the public accounts.
     *
     * @param thread_id             the message threadId
     * @param sms_id             sms_id system SMS id, -1 if didn't use system SMS
     * @param uuid             the public account uuid
     * @param filepath             path of audio file
     * @param recordTime             the length that audio playing
     * @throws FileSuffixException
     * @throws FileTransferException
     * @throws FileDurationException
     * @throws RemoteException the remote exception
     */
    public void sendAudioFile(long thread_id, long sms_id, String uuid, String filepath, int recordTime, boolean isRecord) throws ServiceDisconnectedException, FileSuffixException, FileTransferException, FileDurationException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper.i(String.format( Locale.getDefault(),"enter method sendAudioFile. [thread_id,sms_id,uuid,filepath,recordTime,isRecord]=%d,%d,%s,%s,%d,%b", thread_id,sms_id,uuid,filepath,recordTime,isRecord));
        VerificationUtil.isAudioFile(filepath);
        if(isRecord){
            VerificationUtil.isAudioDurationToLong(context, filepath, this.getAudioMaxTime(), recordTime);
        }
        VerificationUtil.isFileSizeToLarge(filepath, this.getVideoFtMaxSize());
//        VerificationUtil.isFileSizeToLarge(filepath,
//                VerificationUtil.getVideoFtMaxSize(context));
        try {
            myApi.sendAudioFile(thread_id, sms_id, uuid, filepath, recordTime);
        } catch (Exception e) {
            LogHelper.e(e.getMessage(),e);
        }
    }

    /**
     * Send a vedio message to the public accounts.
     *
     * @param thread_id             the message threadId
     * @param sms_id             sms_id system SMS id, -1 if didn't use system SMS
     * @param uuid             the public account uuid
     * @param filepath             path of vedio file
     * @param length             the length that video playing
     * @throws FileSuffixException
     * @throws FileTransferException
     * @throws FileDurationException
     * @throws RemoteException the remote exception
     */
    public void sendVideoFile(long thread_id, long sms_id, String uuid, String filepath, int length, boolean isRecord) throws ServiceDisconnectedException, FileSuffixException, FileTransferException, FileDurationException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper.i(String.format( Locale.getDefault(),"enter method sendVideoFile. [thread_id,sms_id,uuid,filepath,length,isRecord]=%d,%d,%s,%s,%d,%b", thread_id,sms_id,uuid,filepath,length,isRecord));
        VerificationUtil.isVideoFile(filepath);
        if(isRecord){
            VerificationUtil.isVedioDurationToLong(context, filepath, this.getVideoMaxTime(), length);
        }
        VerificationUtil.isFileSizeToLarge(filepath, this.getVideoFtMaxSize());
//        VerificationUtil.isFileSizeToLarge(filepath,
//                VerificationUtil.getVideoFtMaxSize(context));
        try {
            myApi.sendVideoFile(thread_id, sms_id, uuid, filepath, length);
        } catch (Exception e) {
            LogHelper.e(e.getMessage(),e);
        }
    }

    /**
     * Send a location message to the public accounts.
     *
     * @param thread_id             the message threadId
     * @param sms_id             sms_id system SMS id, -1 if didn't use system SMS
     * @param uuid             the public account uuid
     * @param lat             the longitude
     * @param lng             the latitude
     * @param text             the text of location
     * @throws RemoteException the remote exception
     */
    public void sendLocation(long thread_id, long sms_id, String uuid, double lat, double lng, String text) throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper.i(String.format( Locale.getDefault(),"enter method sendLocation. [thread_id,sms_id,uuid,lat,lng,text]=%d,%d,%s,%f,%f,%s", thread_id,sms_id,uuid,lat,lng,text));
        try {
            myApi.sendLocation(thread_id, sms_id, uuid, lat, lng, text);
        } catch (Exception e) {
            LogHelper.e(e.getMessage(),e);
        }
    }

    /**
     * Send a business card message.
     *
     * @param thread_id             the message threadId
     * @param sms_id             sms_id system SMS id, -1 if didn't use system SMS
     * @param uuid             the public account uuid
     * @param rcsContact             throws ApiIsNullException {
        VerificationUtil.ApiIsNull(myApi);@link RCSContact} Object
     * @throws RemoteException the remote exception
     */
    public void sendVCard(long thread_id, long sms_id, String uuid, RCSContact rcsContact) throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper.i(String.format( Locale.getDefault(),"enter method sendVCard. [thread_id,sms_id,uuid,rcsContact]=%d,%d,%s,%s", thread_id,sms_id,uuid,rcsContact.toString()));
        try {
            myApi.sendVCard(thread_id, sms_id, uuid, rcsContact);
        } catch (Exception e) {
            LogHelper.e(e.getMessage(),e);
        }
    }

    /**
     * Send a menu message to the public accounts.
     *
     * @param uuid             the public account uuid
     * @param text             the message info
     * @throws RemoteException the remote exception
     */
    public void sendMenuMessage(String uuid, String text) throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        try {
            myApi.sendMenuMessage(uuid, text);
        } catch (Exception e) {
            LogHelper.e(e.getMessage(),e);
        }
    }

    /**
     * Send a business card message.
     *
     * @param thread_id             the message threadId
     * @param sms_id             sms_id system SMS id, -1 if didn't use system SMS
     * @param uuid             the public account uuid
     * @param vcardFilePath             throws ApiIsNullException {
        VerificationUtil.ApiIsNull(myApi);@link RCSContact} Object
     * @throws RemoteException the remote exception
     */
    public void sendVCardByPath(long thread_id, long sms_id, String uuid, String vcardFilePath) throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper.i(String.format( Locale.getDefault(),"enter method vcardFilePath. [thread_id,sms_id,uuid,vcardFilePath]=%d,%d,%s,%s", thread_id,sms_id,uuid,vcardFilePath));
        try {
            myApi.sendVCardByPath(thread_id, sms_id, uuid, vcardFilePath);
        } catch (Exception e) {
            LogHelper.e(e.getMessage(),e);
        }
    }

    public void deleteMessage(String messageId) throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        try {
            myApi.deleteMessage(messageId);
        } catch (Exception e) {
            LogHelper.e(e.getMessage(), e);
        }
    }

    public void deleteMessageByThreadId(long thread_id) throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        try {
            myApi.deleteMessageByThreadId(thread_id);
        } catch (Exception e) {
            LogHelper.e(e.getMessage(), e);
        }
    }

    public void deleteMessageByUuid(String uuid) throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        try {
            myApi.deleteMessageByUuid(uuid);
        } catch (Exception e) {
            LogHelper.e(e.getMessage(), e);
        }
    }

    public void sendVCardList(long thread_id, long sms_id, String uuid, List<RCSContact> contactList) throws ServiceDisconnectedException {
        LogHelper.i(String.format( Locale.getDefault(),"enter method:sendVCardList. [thread_id,sms_id,uuid,contactList]=%d,%d,%s,%s", thread_id,sms_id,uuid,contactList.toString()));
        VerificationUtil.ApiIsNull(myApi);
        try {
            myApi.sendVCardList(thread_id, sms_id, uuid, contactList);
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
    }

    public long getImageFtMaxSize() throws ServiceDisconnectedException {
        LogHelper.i(String.format( Locale.getDefault(),"enter method:getImageFtMaxSize. "));
        VerificationUtil.ApiIsNull(myApi);
        try {
            return myApi.getImageFtMaxSize();
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
        return 0;
    }

    public long getAudioMaxTime() throws ServiceDisconnectedException {
        LogHelper.i(String.format( Locale.getDefault(),"enter method:getAudioMaxTime. "));
        VerificationUtil.ApiIsNull(myApi);
        try {
            return myApi.getAudioMaxTime();
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
        return 0;
    }

    public long getVideoMaxTime() throws ServiceDisconnectedException {
        LogHelper.i(String.format( Locale.getDefault(),"enter method:getVideoMaxTime. "));
        VerificationUtil.ApiIsNull(myApi);
        try {
            return myApi.getVideoMaxTime();
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
        return 0;
    }

    public long getVideoFtMaxSize() throws ServiceDisconnectedException {
        LogHelper.i(String.format( Locale.getDefault(),"enter method:getVideoFtMaxSize. "));
        VerificationUtil.ApiIsNull(myApi);
        try {
            return myApi.getVideoFtMaxSize();
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
        return 0;
    }
}
