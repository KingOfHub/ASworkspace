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
import android.text.TextUtils;

import com.suntek.mway.rcs.client.api.ClientApi;
import com.suntek.mway.rcs.client.aidl.constant.APIConstant;
import com.suntek.mway.rcs.client.aidl.contacts.RCSContact;
import com.suntek.mway.rcs.client.api.exception.OperatorException;
import com.suntek.mway.rcs.client.aidl.im.IInstantMessageApi;
import com.suntek.mway.rcs.client.aidl.provider.model.ChatMessage;
import com.suntek.mway.rcs.client.aidl.provider.model.FavoriteMessage;
import com.suntek.mway.rcs.client.aidl.provider.model.GroupChatModel;
import com.suntek.mway.rcs.client.aidl.provider.model.MessageSessionModel;
import com.suntek.mway.rcs.client.aidl.provider.model.SimpleMsg;
import com.suntek.mway.rcs.client.aidl.provider.model.TopMessageData;
import com.suntek.mway.rcs.client.api.util.FileDurationException;
import com.suntek.mway.rcs.client.api.util.FileSuffixException;
import com.suntek.mway.rcs.client.api.util.FileTransferException;
import com.suntek.mway.rcs.client.api.util.ServiceDisconnectedException;
import com.suntek.mway.rcs.client.api.util.VerificationUtil;
import com.suntek.mway.rcs.client.api.util.log.LogHelper;

public class MessageApi extends ClientApi {

    private static String serviceName = "com.suntek.mway.rcs.app.service.api.impl.im.MessageApiService";

    IInstantMessageApi myApi;

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            LogHelper.d("client api connect service");
            notifyServiceConnected();
            myApi = IInstantMessageApi.Stub.asInterface(service);
        }

        public void onServiceDisconnected(ComponentName className) {
            if (isNormallyClosed || reconnectionTimes > MAX_RECONECTION_TIMES) {
                LogHelper.d("client api disconnect service");
                myApi = null;
                notifyServiceDisconnected();
            } else {
                LogHelper.d("illegal call client api disconnect service :" + reconnectionTimes);
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

    public MessageApi() {
        // TODO Auto-generated constructor stub
        super(serviceName);
        super.initServiceConnect(mConnection);
    }

    public void sendTextMessage(long thread_id, String number, String text, int burnFlag,
            int barCycle) throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper
                .i(String.format(
                        Locale.getDefault(),
                        "enter method sendTextMessage. [thread_id,number,text,burnFlag,barCycle]=%d,%s,%s,%d,%d",
                        thread_id, number, text, burnFlag, barCycle));
        if ("".equals(text.trim())) {
            LogHelper.i("text value is null/Space");
            return;
        }
        if (!VerificationUtil.isNumber(number)) {
            LogHelper.i("number field value error");
            return;
        }
        if (!VerificationUtil.isBurnFlagCorrect(burnFlag)) {
            LogHelper.i("burnFlag field must be 0 or 1");
            return;
        }
        if (barCycle < 0) {
            LogHelper.i("barCycle field must be a positive int");
            return;
        }
        try {
            myApi.sendTextMessage(thread_id, VerificationUtil.formatNumber(number), text, burnFlag,
                    barCycle);
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
    }

    public void sendImageFile(long thread_id, long sms_id, String number, String filePath,
            int burnFlag, int barCycle, int quality) throws ServiceDisconnectedException,
            FileSuffixException, FileTransferException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper
                .i(String.format(
                        Locale.getDefault(),
                        "enter method sendImageFile. [thread_id,sms_id,number,filePath,burnFlag,barCycle,quality]=%d,%d,%s,%s,%d,%d,%d",
                        thread_id, sms_id, number, filePath, burnFlag, barCycle, quality));
        VerificationUtil.isImageFile(filePath);

        if (!VerificationUtil.isNumber(number)) {
            LogHelper.i("number field value error");
            return;
        }
        if (!VerificationUtil.isBurnFlagCorrect(burnFlag)) {
            LogHelper.i("burnFlag field must be 0 or 1");
            return;
        }
        if (barCycle < 0) {
            LogHelper.i("barCycle field must be a positive int");
            return;
        }
        if (quality < 0 || quality > 100) {
            LogHelper.i("quality field value must be between 0 to 100");
            return;
        }

        if (quality == 100) {
            VerificationUtil.isFileSizeToLarge(filePath, this.getImageFtMaxSize());
        }

        try {
            myApi.sendImageFile(thread_id, sms_id, VerificationUtil.formatNumber(number), filePath,
                    burnFlag, barCycle, quality);
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
    }

    public void sendAudioFile(long thread_id, long sms_id, String number, String filePath,
            int recordTime, int burnFlag, int barCycle, boolean isRecord)
            throws ServiceDisconnectedException, FileSuffixException, FileTransferException,
            FileDurationException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper
                .i(String.format(
                        Locale.getDefault(),
                        "enter method sendAudioFile. [thread_id,sms_id,number,filePath,recordTime,burnFlag,barCycle,isRecord]=%d,%d,%s,%s,%d,%d,%d,%b",
                        thread_id, sms_id, number, filePath, recordTime, burnFlag, barCycle,
                        isRecord));
        VerificationUtil.isAudioFile(filePath);
        if (isRecord) {
            VerificationUtil.isAudioDurationToLong(context, filePath, this.getAudioMaxTime(),
                    recordTime);// VerificationUtil.getAudioMaxTime(context)
        }
        VerificationUtil.isFileSizeToLarge(filePath, this.getVideoFtMaxSize());// VerificationUtil.getVideoFtMaxSize(context)
        if (!VerificationUtil.isNumber(number)) {
            LogHelper.i("number field value error");
            return;
        }
        if (!VerificationUtil.isBurnFlagCorrect(burnFlag)) {
            LogHelper.i("burnFlag field must be 0 or 1");
            return;
        }
        if (barCycle < 0) {
            LogHelper.i("barCycle field must be a positive int");
            return;
        }
        try {
            myApi.sendAudioFile(thread_id, sms_id, VerificationUtil.formatNumber(number), filePath,
                    recordTime, burnFlag, barCycle);
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
    }

    public void sendVideoFile(long thread_id, long sms_id, String number, String filePath,
            int length, int burnFlag, int barCycle, boolean isRecord)
            throws ServiceDisconnectedException, FileSuffixException, FileTransferException,
            FileDurationException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper
                .i(String.format(
                        Locale.getDefault(),
                        "enter method sendVideoFile. [thread_id,sms_id,number,filePath,length,burnFlag,barCycle,isRecord]=%d,%d,%s,%s,%d,%d,%d,%b",
                        thread_id, sms_id, number, filePath, length, burnFlag, barCycle, isRecord));
        VerificationUtil.isVideoFile(filePath);
        if (isRecord) {
            VerificationUtil.isVedioDurationToLong(context, filePath, this.getVideoMaxTime(),
                    length);// VerificationUtil.getVideoMaxTime(context)
        }
        VerificationUtil.isFileSizeToLarge(filePath, this.getVideoFtMaxSize());// VerificationUtil.getVideoFtMaxSize(context)
        if (!VerificationUtil.isNumber(number)) {
            LogHelper.i("number field value error");
            return;
        }
        if (!VerificationUtil.isBurnFlagCorrect(burnFlag)) {
            LogHelper.i("burnFlag field must be 0 or 1");
            return;
        }
        if (barCycle < 0) {
            LogHelper.i("barCycle field must be a positive int");
            return;
        }
        try {
            myApi.sendVideoFile(thread_id, sms_id, VerificationUtil.formatNumber(number), filePath,
                    length, burnFlag, barCycle);
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
    }

    public void sendLocation(long thread_id, long sms_id, String number, double lat, double lng,
            String text) throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper
                .i(String.format(
                        Locale.getDefault(),
                        "enter method sendLocation. [thread_id,sms_id,number,lat,lng,text]=%d,%d,%s,%f,%f,%s",
                        thread_id, sms_id, number, lat, lng, text));
        if (!VerificationUtil.isNumber(number)) {
            LogHelper.i("number field value error");
            return;
        }
        try {
            myApi.sendLocation(thread_id, sms_id, VerificationUtil.formatNumber(number), lat, lng,
                    text);
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
    }

    public void sendPaidEmo(long thread_id, long sms_id, String number, String emoid, String emoName)
            throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper.i(String.format(Locale.getDefault(),
                "enter method sendPaidEmo. [thread_id,sms_id,number,emoid,emoName]=%d,%d,%s,%s,%s",
                thread_id, sms_id, number, emoid, emoName));
        if (!VerificationUtil.isNumber(number)) {
            LogHelper.i("number field value error");
            return;
        }
        try {
            myApi.sendPaidEmo(thread_id, sms_id, number, emoid, emoName);
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
    }

    public void sendVCard(long thread_id, long sms_id, String number, RCSContact rcsContact)
            throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper.i(String.format(Locale.getDefault(),
                "enter method sendVCard. [thread_id,sms_id,number,rcsContact]=%d,%d,%s,%s",
                thread_id, sms_id, number, rcsContact.toString()));
        if (!VerificationUtil.isNumber(number)) {
            LogHelper.i("number field value error");
            return;
        }
        try {
            myApi.sendVCard(thread_id, sms_id, number, rcsContact);
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
    }

    public void sendVCard(long thread_id, long sms_id, String number, String vcardFilePath)
            throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper.i(String.format(Locale.getDefault(),
                "enter method sendVCard. [thread_id,sms_id,number,vcardFilePath]=%d,%d,%s,%s",
                thread_id, sms_id, number, vcardFilePath));
        if (!VerificationUtil.isNumber(number)) {
            LogHelper.i("number field value error");
            return;
        }
        try {
            myApi.sendVCardByPath(thread_id, sms_id, VerificationUtil.formatNumber(number),
                    vcardFilePath);
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
    }

    public void sendOne2ManyTextMessage(long thread_id, List<String> numbers, String text,
            int burnFlag, int barCycle) throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        if ("".equals(text.trim())) {
            LogHelper.i("text value is null/Space");
            return;
        }
        LogHelper
                .i(String.format(
                        Locale.getDefault(),
                        "enter method sendOne2ManyTextMessage. [thread_id,numbers,text,burnFlag,barCycle]=%d,%s,%s,%d,%d",
                        thread_id, numbers.toString(), text, burnFlag, barCycle));
        if (!VerificationUtil.isAllNumber(numbers)) {
            LogHelper.i("number field value error");
            return;
        }
        if (!VerificationUtil.isBurnFlagCorrect(burnFlag)) {
            LogHelper.i("burnFlag field must be 0 or 1");
            return;
        }
        if (barCycle < 0) {
            LogHelper.i("barCycle field must be a positive int");
            return;
        }
        try {
            myApi.sendOne2ManyTextMessage(thread_id, VerificationUtil.formatNumbers(numbers), text,
                    burnFlag, barCycle);
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
    }

    public void sendOne2ManyImageFile(long thread_id, long sms_id, List<String> numbers,
            String filePath, int burnFlag, int barCycle, int quality)
            throws ServiceDisconnectedException, FileSuffixException, FileTransferException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper
                .i(String.format(
                        Locale.getDefault(),
                        "enter method sendOne2ManyImageFile. [thread_id,sms_id,numbers,filePath,burnFlag,barCycle,quality]=%d,%d,%s,%s,%d,%d,%d",
                        thread_id, sms_id, numbers.toString(), filePath, burnFlag, barCycle,
                        quality));
        VerificationUtil.isImageFile(filePath);
        if (!VerificationUtil.isAllNumber(numbers)) {
            LogHelper.i("number field value error");
            return;
        }
        if (!VerificationUtil.isBurnFlagCorrect(burnFlag)) {
            LogHelper.i("burnFlag field must be 0 or 1");
            return;
        }
        if (barCycle < 0) {
            LogHelper.i("barCycle field must be a positive int");
            return;
        }
        if (quality < 0 || quality > 100) {
            LogHelper.i("quality field value must be between 0 to 100");
            return;
        }

        if (quality == 100) {
            VerificationUtil.isFileSizeToLarge(filePath, this.getImageFtMaxSize());
        }

        try {
            myApi.sendOne2ManyImageFile(thread_id, sms_id, VerificationUtil.formatNumbers(numbers),
                    filePath, burnFlag, barCycle, quality);
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
    }

    public void sendOne2ManyAudioFile(long thread_id, long sms_id, List<String> numbers,
            String filePath, int recordTime, int burnFlag, int barCycle, boolean isRecord)
            throws ServiceDisconnectedException, FileSuffixException, FileTransferException,
            FileDurationException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper
                .i(String.format(
                        Locale.getDefault(),
                        "enter method sendOne2ManyAudioFile. [thread_id,sms_id,numbers,filePath,recordTime,burnFlag,barCycle,isRecord]=%d,%d,%s,%s,%d,%d,%d,%b",
                        thread_id, sms_id, numbers.toString(), filePath, recordTime, burnFlag,
                        barCycle, isRecord));
        VerificationUtil.isAudioFile(filePath);
        if (isRecord) {
            VerificationUtil.isAudioDurationToLong(context, filePath, this.getAudioMaxTime(),
                    recordTime);
        }
        VerificationUtil.isFileSizeToLarge(filePath, this.getVideoFtMaxSize());
        if (!VerificationUtil.isAllNumber(numbers)) {
            LogHelper.i("number field value error");
            return;
        }
        if (!VerificationUtil.isBurnFlagCorrect(burnFlag)) {
            LogHelper.i("burnFlag field must be 0 or 1");
            return;
        }
        if (barCycle < 0) {
            LogHelper.i("barCycle field must be a positive int");
            return;
        }
        try {
            myApi.sendOne2ManyAudioFile(thread_id, sms_id, VerificationUtil.formatNumbers(numbers),
                    filePath, recordTime, burnFlag, barCycle);
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
    }

    public void sendOne2ManyVideoFile(long thread_id, long sms_id, List<String> numbers,
            String filePath, int length, int burnFlag, int barCycle, boolean isRecord)
            throws ServiceDisconnectedException, FileSuffixException, FileTransferException,
            FileDurationException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper
                .i(String.format(
                        Locale.getDefault(),
                        "enter method sendOne2ManyVideoFile. [thread_id,sms_id,numbers,filePath,length,burnFlag,barCycle,isRecord]=%d,%d,%s,%s,%d,%d,%d,%b",
                        thread_id, sms_id, numbers.toString(), filePath, length, burnFlag,
                        barCycle, isRecord));
        VerificationUtil.isVideoFile(filePath);
        if (isRecord) {
            VerificationUtil.isVedioDurationToLong(context, filePath, this.getVideoMaxTime(),
                    length);
        }
        VerificationUtil.isFileSizeToLarge(filePath, this.getVideoFtMaxSize());
        if (!VerificationUtil.isAllNumber(numbers)) {
            LogHelper.i("number field value error");
            return;
        }
        if (!VerificationUtil.isBurnFlagCorrect(burnFlag)) {
            LogHelper.i("burnFlag field must be 0 or 1");
            return;
        }
        if (barCycle < 0) {
            LogHelper.i("barCycle field must be a positive int");
            return;
        }
        try {
            myApi.sendOne2ManyVideoFile(thread_id, sms_id, VerificationUtil.formatNumbers(numbers),
                    filePath, length, burnFlag, barCycle);
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
    }

    public void sendOne2ManyLocation(long thread_id, long sms_id, List<String> numbers, double lat,
            double lng, String text) throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper
                .i(String.format(
                        Locale.getDefault(),
                        "enter method sendOne2ManyLocation. [thread_id,sms_id,numbers,lat,lng,text]=%d,%d,%s,%f,%f,%s",
                        thread_id, sms_id, numbers.toString(), lat, lng, text));
        if (!VerificationUtil.isAllNumber(numbers)) {
            LogHelper.i("number field value error");
            return;
        }
        try {
            myApi.sendOne2ManyLocation(thread_id, sms_id, VerificationUtil.formatNumbers(numbers),
                    lat, lng, text);
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
    }

    public void sendGroupMessage(long thread_id, String conversationId, long sms_id, String msg,
            String groupId) throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        if ("".equals(msg.trim())) {
            LogHelper.i("msg value is null/Space");
            return;
        }
        LogHelper
                .i(String.format(
                        Locale.getDefault(),
                        "enter method sendGroupMessage. [thread_id,conversationId,sms_id,msg,groupId]=%d,%s,%d,%s,%s",
                        thread_id, conversationId, sms_id, msg, groupId));
        try {
            myApi.sendGroupMessage(thread_id, conversationId, sms_id, msg, groupId);
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
    }

    public void sendGroupImageFile(long thread_id, String conversationId, long sms_id,
            String filepath, String groupId, int quality) throws ServiceDisconnectedException,
            FileSuffixException, FileTransferException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper
                .i(String.format(
                        Locale.getDefault(),
                        "enter method sendGroupImageFile. [thread_id,conversationId,sms_id,filepath,groupId,quality]=%d,%s,%d,%s,%s,%d",
                        thread_id, conversationId, sms_id, filepath, groupId, quality));
        VerificationUtil.isImageFile(filepath);
        if (quality < 0 || quality > 100) {
            LogHelper.i("quality field value must be between 0 to 100");
            return;
        }

        if (quality == 100) {
            VerificationUtil.isFileSizeToLarge(filepath, this.getImageFtMaxSize());
        }

        try {
            myApi.sendGroupImageFile(thread_id, conversationId, sms_id, filepath, groupId, quality);
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
    }

    public void sendGroupAudioFile(long thread_id, String conversationId, long sms_id,
            String filepath, int recordTime, String groupId, boolean isRecord)
            throws ServiceDisconnectedException, FileSuffixException, FileTransferException,
            FileDurationException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper
                .i(String.format(
                        Locale.getDefault(),
                        "enter method sendGroupAudioFile. [thread_id,conversationId,sms_id,filepath,recordTime,groupId,isRecord]=%d,%s,%d,%s,%d,%s,%b",
                        thread_id, conversationId, sms_id, filepath, recordTime, groupId, isRecord));
        VerificationUtil.isAudioFile(filepath);
        if (isRecord) {
            VerificationUtil.isAudioDurationToLong(context, filepath, this.getAudioMaxTime(),
                    recordTime);
        }
        VerificationUtil.isFileSizeToLarge(filepath, this.getVideoFtMaxSize());
        try {
            myApi.sendGroupAudioFile(thread_id, conversationId, sms_id, filepath, recordTime,
                    groupId);
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
    }

    public void sendGroupVideoFile(long thread_id, String conversationId, long sms_id,
            String filepath, int length, String groupId, boolean isRecord)
            throws ServiceDisconnectedException, FileSuffixException, FileTransferException,
            FileDurationException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper
                .i(String.format(
                        Locale.getDefault(),
                        "enter method sendGroupVideoFile. [thread_id,conversationId,sms_id,filepath,length,groupId,isRecord]=%d,%s,%d,%s,%d,%s,%b",
                        thread_id, conversationId, sms_id, filepath, length, groupId, isRecord));
        VerificationUtil.isVideoFile(filepath);
        if (isRecord) {
            VerificationUtil.isVedioDurationToLong(context, filepath, this.getVideoMaxTime(),
                    length);
        }
        VerificationUtil.isFileSizeToLarge(filepath, this.getVideoFtMaxSize());
        try {
            myApi.sendGroupVideoFile(thread_id, conversationId, sms_id, filepath, length, groupId);
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
    }

    public void sendGroupLocation(long thread_id, String conversationId, long sms_id, double lat,
            double lng, String text, String groupId) throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper
                .i(String.format(
                        Locale.getDefault(),
                        "enter method sendGroupLocation. [thread_id,conversationId,sms_id,lat,lng,text,groupId]=%d,%s,%d,%f,%f,%s,%s",
                        thread_id, conversationId, sms_id, lat, lng, text, groupId));
        try {
            myApi.sendGroupLocation(thread_id, conversationId, sms_id, lat, lng, text, groupId);
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
    }

    public void sendGroupVCard(long thread_id, String conversationId, long sms_id,
            RCSContact rcsContact, String groupId) throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper
                .i(String.format(
                        Locale.getDefault(),
                        "enter method sendGroupVCard. [thread_id,conversationId,sms_id,rcsContact,groupId]=%d,%s,%d,%s,%s",
                        thread_id, conversationId, sms_id, rcsContact.toString(), groupId));
        try {
            myApi.sendGroupVCard(thread_id, conversationId, sms_id, rcsContact, groupId);
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
    }

    public void sendGroupVCard(long thread_id, String conversationId, long sms_id,
            String vcardFilePath, String groupId) throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper
                .i(String.format(
                        Locale.getDefault(),
                        "enter method sendGroupVCard. [thread_id,conversationId,sms_id,vcardFilePath,groupId]=%d,%s,%d,%s,%s",
                        thread_id, conversationId, sms_id, vcardFilePath, groupId));
        try {
            myApi.sendGroupVCardByPath(thread_id, conversationId, sms_id, vcardFilePath, groupId);
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
    }

    public void acceptFile(ChatMessage chatMessage) throws OperatorException,
            ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper.i(String.format(Locale.getDefault(), "enter method acceptFile. [chatMessage]=%s",
                chatMessage.toString()));
        try {
            myApi.acceptFile(chatMessage);
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
            throw new OperatorException(ex.getMessage());
        }
    }

    public boolean interruptFile(ChatMessage chatMessage) throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper.i(String.format(Locale.getDefault(),
                "enter method interruptFile. [chatMessage]=%s", chatMessage.toString()));
        boolean flag = false;
        try {
            return myApi.interruptFile(chatMessage);
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
        return flag;
    }

    public MessageSessionModel getMessageSessionByThreadId(long threadId)
            throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper.i(String.format(Locale.getDefault(),
                "enter method getMessageSessionByThreadId. [threadId]=%d", threadId));
        try {
            return myApi.getMessageSessionByThreadId(threadId);
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
            return null;
        }
    }

    public List<MessageSessionModel> getMessageSessionList(int offset, int number)
            throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper.i(String.format(Locale.getDefault(),
                "enter method getMessageSessionList. [offset,number]=%d,%d", offset, number));
        try {
            return myApi.getMessageSessionList(offset, number);
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
            return null;
        }
    }

    public List<ChatMessage> getChatMessageList(long threadId, boolean less, int specifiedMsgId,
            int count) throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper.i(String.format(Locale.getDefault(),
                "enter method getChatMessageList. [threadId,specifiedMsgId,count]=%d,%d,%d",
                threadId, specifiedMsgId, count));
        try {
            return myApi.getChatMessageList(threadId, less, specifiedMsgId, count);
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
            return null;
        }
    }

    public boolean removeMessageByThreadId(long threadId) throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper.i(String.format(Locale.getDefault(),
                "enter method removeMessageByThreadId. [threadId]=%d", threadId));
        try {
            myApi.removeMessageByThreadId(threadId);
            return true;
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
            return false;
        }
    }

    public void removeOneMessage(String messageId) throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper.i(String.format(Locale.getDefault(),
                "enter method removeOneMessage. [messageId]=%s", messageId));
        try {
            myApi.removeOneMessage(messageId);
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
    }

    public void removeAllMessage() throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper.i(String.format(Locale.getDefault(), "enter method removeAllMessage. "));
        try {
            myApi.removeAllMessage();
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
    }

    public List<ChatMessage> searchMessageByText(String text, int offset, int number,
            boolean timaAsc) throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        try {
            return myApi.searchMessageByText(text, offset, number, timaAsc);
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
            return null;
        }
    }

    public ChatMessage getTheLastMessage(long threadId) throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper.i(String.format(Locale.getDefault(),
                "enter method getTheLastMessage. [threadId]=%d", threadId));
        try {
            return myApi.getTheLastMessage(threadId);
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
            return null;
        }
    }

    public String getThreadIdByNumber(List<String> numbers) throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper.i(String.format(Locale.getDefault(),
                "enter method getThreadIdByNumber. [numbers]=%s", numbers.toString()));
        if (!VerificationUtil.isAllNumber(numbers)) {
            LogHelper.i("number field value error");
            return null;
        }
        try {
            return myApi.getThreadIdByNumber(VerificationUtil.formatNumbers(numbers));
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
            return null;
        }
    }

    public long getNewThreadId() throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper.i(String.format(Locale.getDefault(), "enter method getNewThreadId. "));
        try {
            return myApi.getNewThreadId();
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
            return -1;
        }
    }

    public ChatMessage getMessageById(String rowId) throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper.i(String.format(Locale.getDefault(), "enter method getMessageById. [rowId]=%s",
                rowId));
        try {
            return myApi.getMessageById(rowId);
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
            return null;
        }
    }

    public ChatMessage getMessageByMessageId(String messageId) throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper.i(String.format(Locale.getDefault(),
                "enter method getMessageByMessageId. [messageId]=%s", messageId));
        try {
            return myApi.getMessageByMessageId(messageId);
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
            return null;
        }
    }

    public GroupChatModel getGroupChatById(String groupId) throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        try {
            return myApi.getGroupChatById(groupId);
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
            return null;
        }
    }

    public GroupChatModel getGroupChatByThreadId(long threadId) throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper.i(String.format(Locale.getDefault(),
                "enter method getGroupChatByThreadId. [threadId]=%d", threadId));
        try {
            return myApi.getGroupChatByThreadId(threadId);
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
            return null;
        }
    }

    public String getFilepath(ChatMessage message) throws ServiceDisconnectedException {

        // LogHelper.e("error log, enter method getFilepath. [message]=" +
        // message.toString());

        // System.out.println("========enter method getFilepath. [message]=" +
        // message);

        VerificationUtil.ApiIsNull(myApi);
        LogHelper.i(String.format(Locale.getDefault(), "enter method getFilepath. [message]=%s",
                message.toString()));
        try {
            return myApi.getFilepath(message);
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
            return null;
        }
    }

    public String getThumbFilepath(ChatMessage message) throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper.i(String.format(Locale.getDefault(),
                "enter method getThumbFilepath. [message]=%s", message.toString()));
        try {
            return myApi.getThumbFilepath(message);
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
            return null;
        }
    }

    public void topMessage(long threadId) throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper.i(String.format(Locale.getDefault(), "enter method topMessage. [threadId]=%d",
                threadId));
        try {
            myApi.topMessage(threadId);
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
    }

    public void cancelTopMessage(long threadId) throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper.i(String.format(Locale.getDefault(),
                "enter method cancelTopMessage. [threadId]=%d", threadId));
        try {
            myApi.cancelTopMessage(threadId);
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
    }

    public void backupAllMessage() throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper.i(String.format(Locale.getDefault(), "enter method backupAllMessage. "));
        try {
            myApi.backupAllMessage();
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
    }

    public void restoreAllMessage() throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper.i(String.format(Locale.getDefault(), "enter method restoreAllMessage. "));
        try {
            myApi.restoreAllMessage();
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
    }

    public void collectMessage(List<SimpleMsg> simpleMsgList) throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper.i(String.format(Locale.getDefault(),
                "enter method collectMessage. [simpleMsgList]=%s", simpleMsgList.toString()));
        try {
            myApi.collectMessage(simpleMsgList);
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
    }

    public List<TopMessageData> getTopMsgsInOrder(boolean asc) throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper.i(String.format(Locale.getDefault(),
                "enter method getTopMsgsInOrder. [threadId]=%b", asc));
        try {
            return myApi.getTopMsgsInOrder(asc);
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
        return null;
    }

    public void accuseMessage(final long thread_id, String id) throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper.i(String.format(Locale.getDefault(),
                "enter method accuseMessage. [thread_id,id]=%d,%s", thread_id, id));
        try {
            myApi.accuseMessage(thread_id, id);
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
    }

    public List<ChatMessage> qryNotifyArchiveMsgList() throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper
                .i(String.format(Locale.getDefault(), "enter method getMessageOfSpecialService. "));
        try {
            return myApi.getMessageOfSpecialService();
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
        return null;
    }

    public List<ChatMessage> qryNonFriendMsgList() throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper.i(String.format(Locale.getDefault(), "enter method getMessageOfStrangeNumber. "));
        try {
            return myApi.getMessageOfStrangeNumber();
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
        return null;
    }

    public List<MessageSessionModel> qryNonFriendMsgSessionList()
            throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper
                .i(String.format(Locale.getDefault(), "enter method qryNonFriendMsgSessionList. "));
        try {
            return myApi.qryNonFriendMsgSessionList();
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
        return null;
    }

    public List<MessageSessionModel> qryNotifyArchiveMsgSessionList()
            throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper.i(String.format(Locale.getDefault(),
                "enter method qryNotifyArchiveMsgSessionList. "));
        try {
            return myApi.qryNotifyArchiveMsgSessionList();
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
        return null;
    }

    public void sendGroupPaidEmo(long thread_id, String conversationId, long sms_id, String emoid,
            String emoName, String groupId) throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper
                .i(String.format(
                        Locale.getDefault(),
                        "enter method:sendGroupPaidEmo. [thread_id,conversationId,sms_id,emoid,emoName,groupId]=%d,%s,%d,%s,%s,%s",
                        thread_id, conversationId, sms_id, emoid, emoName, groupId));
        try {
            myApi.sendGroupPaidEmo(thread_id, conversationId, sms_id, emoid, emoName, groupId);
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
    }

    public void sendOne2ManyPaidEmoMessage(long thread_id, long sms_id, List<String> numbers,
            String emoid, String emoName) throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper
                .i(String.format(
                        Locale.getDefault(),
                        "enter method:sendOne2ManyPaidEmoMessage. [thread_id,sms_id,numbers,emoid,emoName]=%d,%d,%s,%s,%s",
                        thread_id, sms_id, numbers.toString(), emoid, emoName));
        if (!VerificationUtil.isAllNumber(numbers)) {
            LogHelper.i("number field value error");
            return;
        }
        try {
            myApi.sendOne2ManyPaidEmoMessage(thread_id, sms_id,
                    VerificationUtil.formatNumbers(numbers), emoid, emoName);
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
    }

    public List<ChatMessage> getMsgListGreatOrLessThanSpecifiedForBlack(long threadId,
            boolean less, int specifiedId, int number) throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper
                .i(String.format(
                        Locale.getDefault(),
                        "enter method:getMsgListGreatThanSpecifiedForBlack. [threadId,less,specifiedId,number]=%d,%b,%d,%d",
                        threadId, less, specifiedId, number));
        try {
            return myApi.getMsgListGreatOrLessThanSpecifiedForBlack(threadId, less, specifiedId,
                    number);
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
        return null;
    }

    public MessageSessionModel getMessageSessionByThreadIdForBlack(String threadId)
            throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper.i(String.format(Locale.getDefault(),
                "enter method:getMessageSessionByThreadIdForBlack. [threadId]=%s", threadId));
        try {
            return myApi.getMessageSessionByThreadIdForBlack(threadId);
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
        return null;
    }

    public List<MessageSessionModel> getMessageSessionListForBlack(int offset, int number)
            throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper.i(String.format(Locale.getDefault(),
                "enter method:getMessageSessionListForBlack. [thread_id,number]=%d,%d", offset,
                number));
        try {
            return myApi.getMessageSessionListForBlack(offset, number);
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
        return null;
    }

    public ChatMessage getTheLastMsgOfThreadForBlack(long threadId)
            throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper.i(String.format(Locale.getDefault(),
                "enter method:getTheLastMsgOfThreadForBlack. [threadId]=%d", threadId));
        try {
            return myApi.getTheLastMsgOfThreadForBlack(threadId);
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
        return null;
    }

    public void sendOne2ManyVCard(long thread_id, long sms_id, List<String> numbers,
            RCSContact rcsContact) throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper
                .i(String.format(
                        Locale.getDefault(),
                        "enter method:sendOne2ManyVCard. [thread_id,sms_id,numbers,rcsContact]=%d,%d,%s,%s",
                        thread_id, sms_id, numbers.toString(), rcsContact.toString()));
        if (!VerificationUtil.isAllNumber(numbers)) {
            LogHelper.i("number field value error");
            return;
        }
        try {
            myApi.sendOne2ManyVCard(thread_id, sms_id, VerificationUtil.formatNumbers(numbers),
                    rcsContact);
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
    }

    public void sendOne2ManyVCard(long thread_id, long sms_id, List<String> numbers,
            String vcardFilePath) throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper
                .i(String.format(
                        Locale.getDefault(),
                        "enter method:sendOne2ManyVCard. [thread_id,sms_id,numbers,vcardFilePath]=%d,%d,%s,%s",
                        thread_id, sms_id, numbers.toString(), vcardFilePath));
        if (!VerificationUtil.isAllNumber(numbers)) {
            LogHelper.i("number field value error");
            return;
        }
        try {
            myApi.sendOne2ManyVCardByPath(thread_id, sms_id,
                    VerificationUtil.formatNumbers(numbers), vcardFilePath);
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
    }

    public int recoveBlackMsgByMessageId(String messageId) throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper.i(String.format(Locale.getDefault(),
                "enter method:recoveBlackMsgByMessageId. [messageId]=%s", messageId));
        try {
            return myApi.recoveBlackMsgByMessageId(messageId);
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
        return 0;
    }

    public int recoveBlackMsgByThreadId(long threadId) throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper.i(String.format(Locale.getDefault(),
                "enter method:recoveBlackMsgByThreadId. [threadId]=%d", threadId));
        try {
            return myApi.recoveBlackMsgByThreadId(threadId);
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
        return 0;
    }

    public int recoveBlackMsgAll() throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper.i(String.format(Locale.getDefault(), "enter method:recoveBlackMsgAll. "));
        try {
            return myApi.recoveBlackMsgAll();
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
        return 0;
    }

    public void retransmitMessageById(String id) throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper.i(String.format(Locale.getDefault(),
                "enter method:retransmitMessageById. [id]=%s", id));
        try {
            myApi.retransmitMessageById(id);
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
    }

    public void sendDisplayNotification(final String conversationId, final String number,
            final String messageId) throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper
                .i(String.format(
                        Locale.getDefault(),
                        "enter method:sendDisplayNotification. [conversationId, number, messageId]=%s,%s,%s",
                        conversationId, number, messageId));
        if (!VerificationUtil.isNumber(number)) {
            LogHelper.i("number field value error");
            return;
        }
        try {
            myApi.sendDisplayNotification(conversationId, VerificationUtil.formatNumber(number),
                    messageId);
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
    }

    public List<GroupChatModel> getAllGroupChat() throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper.i(String.format(Locale.getDefault(), "enter method:getAllGroupChat. "));
        try {
            return myApi.getAllGroupChat();
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
        return null;
    }

    public void burnMessage(String id) throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper.i(String.format(Locale.getDefault(), "enter method:burnMessage. [id]=%s", id));
        try {
            myApi.burnMessage(id);
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
    }

    public void cancelCollectSimpleMsg(List<SimpleMsg> simpleMsgList)
            throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper.i(String.format(Locale.getDefault(),
                "enter method:cancelCollectSimpleMsg. [simpleMsgList]=%s", simpleMsgList));
        try {
            myApi.cancelCollectSimpleMsg(simpleMsgList);
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
    }

    public List<FavoriteMessage> getFavouriteMessageList() throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper.i(String.format(Locale.getDefault(), "enter method:getFavouriteMessageList. "));
        try {
            return myApi.getFavouriteMessageList();
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
        return null;
    }

    public void burnMessageAtOnce(String id) throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper.i(String.format(Locale.getDefault(), "enter method:burnMessageAtOnce. [id]=%s",
                id));
        try {
            myApi.burnMessageAtOnce(id);
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
    }

    public String getAccuseNumber() throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper.i(String.format(Locale.getDefault(), "enter method:getAccuseNumber. "));
        try {
            return myApi.getAccuseNumber();
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
        return null;
    }

    public byte[] getImageThumbnails(String filepath) throws ServiceDisconnectedException {

        // LogHelper.e("error log, enter method getImageThumbnails. [filepath]="
        // + filepath);

        // System.out.println("========enter method getImageThumbnails. [filepath]="
        // + filepath);

        VerificationUtil.ApiIsNull(myApi);
        LogHelper.i(String.format(Locale.getDefault(), "enter method:getImageThumbnails. "));
        try {
            return myApi.getImageThumbnails(filepath);
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
        return null;
    }

    public byte[] getVideoThumbnails(String filepath) throws ServiceDisconnectedException {
        // LogHelper.e("error log, enter method getVideoThumbnails. [filepath]="
        // + filepath);

        VerificationUtil.ApiIsNull(myApi);
        LogHelper.i(String.format(Locale.getDefault(), "enter method:getVideoThumbnails. "));
        try {
            return myApi.getVideoThumbnails(filepath);
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
        return null;
    }

    public int getAllUnreadCount() throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper.i(String.format(Locale.getDefault(), "enter method:getAllUnreadCount. "));
        try {
            return myApi.getAllUnreadCount();
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
        return 0;
    }

    public int getUnreadMsgCountByThreadId(String threadId) throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper.i(String.format(Locale.getDefault(),
                "enter method:getUnreadMsgCountByThreadId. [threadId]=%s", threadId));
        try {
            return myApi.getUnreadMsgCountByThreadId(threadId);
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
        return 0;
    }

    public void removeUnreadMessageByThreadId(String threadId) throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper.i(String.format(Locale.getDefault(),
                "enter method:removeUnreadMessageByThreadId. [threadId]=%s", threadId));
        try {
            myApi.removeUnreadMessageByThreadId(threadId);
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
    }

    public int getMsgSendPolicy() throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        try {
            return myApi.getMsgSendPolicy();
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
        return APIConstant.MSG_SEND_POLICY_AUTO;
    }

    public void setMsgSendPolicy(int policy) throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper.i(String.format(Locale.getDefault(),
                "enter method:setMsgSendPolicy. [policy]=%s", policy));

        switch (policy) {
            case APIConstant.MSG_SEND_POLICY_AUTO:
            case APIConstant.MSG_SEND_POLICY_SMS:
            case APIConstant.MSG_SEND_POLICY_IM:
                try {
                    myApi.setMsgSendPolicy(policy);
                } catch (Exception ex) {
                    LogHelper.e(ex.getMessage(), ex);
                }
                break;

            default:
                LogHelper.d("method:setMsgSendPolicy input param not defined, param error.");
                break;
        }
    }

    public int updateMessageRead(String id) throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper.i(String.format(Locale.getDefault(), "enter method:updateMessageRead. [id]=%s",
                id));
        try {
            return myApi.updateMessageRead(id);
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
        return 0;
    }

    public void uploadFile(ChatMessage chatMessage) throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper.i(String.format(Locale.getDefault(), "enter method:uploadFile. [chatMessage]=%s",
                chatMessage.toString()));
        try {
            myApi.uploadFile(chatMessage);
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
    }

    public int getPlayTime(int msgType, String data) throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper.i(String.format(Locale.getDefault(),
                "enter method:getPlayTime. [msgType,data]=%d,%s", msgType, data));
        try {
            return myApi.getPlayTime(msgType, data);
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
        return 0;
    }

    public GroupChatModel getGroupChatByConversationId(String conversationId)
            throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper.i(String.format(Locale.getDefault(),
                "enter method:getGroupChatByConversationId. [conversationId]=%s", conversationId));
        try {
            return myApi.getGroupChatByConversationId(conversationId);
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
        return null;
    }

    public void setSMSSentRemind(int policy) throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper.i(String.format(Locale.getDefault(),
                "enter method:setSMSSentRemind. [policy]=%d", policy));
        try {
            myApi.setSMSSentRemind(policy);
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
    }

    public int getSMSSentRemind() throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper.i(String.format(Locale.getDefault(), "enter method:getSMSSentRemind. "));
        try {
            return myApi.getSMSSentRemind();
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
        return 0;
    }

    public void burnAllMsgAtOnce() throws ServiceDisconnectedException {
        LogHelper.i(String.format(Locale.getDefault(), "enter method:burnAllMsgAtOnce. "));
        VerificationUtil.ApiIsNull(myApi);
        try {
            myApi.burnAllMsgAtOnce();
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
    }

    public void sendComposingMsg(long threadId, String contact, String contentType, int seconds)
            throws ServiceDisconnectedException {
        LogHelper
                .i(String.format(
                        Locale.getDefault(),
                        "enter method:sendComposingMsg. [threadId,contact,contentType,seconds]=%d,%s,%s,%d",
                        threadId, contact, contentType, seconds));
        VerificationUtil.ApiIsNull(myApi);
        if (!VerificationUtil.isNumber(contact)) {
            LogHelper.i("number field value error");
            return;
        }
        try {
            myApi.sendComposingMsg(threadId, VerificationUtil.formatNumber(contact), contentType,
                    seconds);
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
    }

    public void sendCancelComposingMsg(long threadId, String contact, String contentType,
            long lastActive) throws ServiceDisconnectedException {
        LogHelper
                .i(String.format(
                        Locale.getDefault(),
                        "enter method:sendCancelComposingMsg. [threadId,contact,contentType,lastActive]=%d,%s,%s,%d",
                        threadId, contact, contentType, lastActive));
        VerificationUtil.ApiIsNull(myApi);
        if (!VerificationUtil.isNumber(contact)) {
            LogHelper.i("number field value error");
            return;
        }
        try {
            myApi.sendCancelComposingMsg(threadId, VerificationUtil.formatNumber(contact),
                    contentType, lastActive);
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
    }

    public void sendComposingMsgToGroup(long threadId, int id, String contentType, int seconds)
            throws ServiceDisconnectedException {
        LogHelper
                .i(String.format(
                        Locale.getDefault(),
                        "enter method:sendComposingMsgToGroup. [threadId,id,contentType,seconds]=%d,%d,%s,%d",
                        threadId, id, contentType, seconds));
        VerificationUtil.ApiIsNull(myApi);
        try {
            myApi.sendComposingMsgToGroup(threadId, id, contentType, seconds);
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
    }

    public void sendCancelComposingMsgToGroup(long threadId, int id, String contentType,
            long lastActive) throws ServiceDisconnectedException {
        LogHelper
                .i(String.format(
                        Locale.getDefault(),
                        "enter method:sendCancelComposingMsgToGroup. [threadId,id,contentType,lastActive]=%d,%d,%s,%d",
                        threadId, id, contentType, lastActive));
        VerificationUtil.ApiIsNull(myApi);
        try {
            myApi.sendCancelComposingMsgToGroup(threadId, id, contentType, lastActive);
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
    }

    public void sendVCardList(final long thread_id, final long sms_id, final String number,
            final List<RCSContact> contactList, final int chatType)
            throws ServiceDisconnectedException {
        LogHelper
                .i(String.format(
                        Locale.getDefault(),
                        "enter method:sendVCardList. [thread_id, sms_id, number, contactList, chatType]=%d,%d,%s,%s,%d",
                        thread_id, sms_id, number, contactList.toString(), chatType));
        VerificationUtil.ApiIsNull(myApi);
        if (!VerificationUtil.isNumber(number)) {
            LogHelper.i("number field value error");
            return;
        }
        try {
            myApi.sendVCardList(thread_id, sms_id, VerificationUtil.formatNumber(number),
                    contactList, chatType);
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
    }

    public void sendOne2ManyVCardList(final long thread_id, final long sms_id,
            final List<String> numbers, final List<RCSContact> contactList)
            throws ServiceDisconnectedException {
        LogHelper
                .i(String.format(
                        Locale.getDefault(),
                        "enter method:sendOne2ManyVCardList. [thread_id, sms_id, numbers, contactList]=%d,%d,%s,%s",
                        thread_id, sms_id, numbers.toString(), contactList.toString()));
        VerificationUtil.ApiIsNull(myApi);
        if (!VerificationUtil.isAllNumber(numbers)) {
            LogHelper.i("number field value error");
            return;
        }
        try {
            myApi.sendOne2ManyVCardList(thread_id, sms_id, VerificationUtil.formatNumbers(numbers),
                    contactList);
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
    }

    public void sendGroupVCardList(final long thread_id, final String conversationId,
            final long sms_id, final List<RCSContact> contactList, final String groupId)
            throws ServiceDisconnectedException {
        LogHelper
                .i(String.format(
                        Locale.getDefault(),
                        "enter method:sendGroupVCardList. [thread_id, conversationId, sms_id, contactList, groupId]=%d,%s,%d,%s,%s",
                        thread_id, conversationId, sms_id, contactList.toString(), groupId));
        VerificationUtil.ApiIsNull(myApi);
        try {
            myApi.sendGroupVCardList(thread_id, conversationId, sms_id, contactList, groupId);
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
    }

    public long getImageFtMaxSize() throws ServiceDisconnectedException {
        LogHelper.i(String.format(Locale.getDefault(), "enter method:getImageFtMaxSize. "));
        VerificationUtil.ApiIsNull(myApi);
        try {
            return myApi.getImageFtMaxSize();
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
        return 0;
    }

    public long getAudioMaxTime() throws ServiceDisconnectedException {
        LogHelper.i(String.format(Locale.getDefault(), "enter method:getAudioMaxTime. "));
        VerificationUtil.ApiIsNull(myApi);
        try {
            return myApi.getAudioMaxTime();
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
        return 0;
    }

    public long getVideoMaxTime() throws ServiceDisconnectedException {
        LogHelper.i(String.format(Locale.getDefault(), "enter method:getVideoMaxTime. "));
        VerificationUtil.ApiIsNull(myApi);
        try {
            return myApi.getVideoMaxTime();
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
        return 0;
    }

    public long getVideoFtMaxSize() throws ServiceDisconnectedException {
        LogHelper.i(String.format(Locale.getDefault(), "enter method:getVideoFtMaxSize. "));
        VerificationUtil.ApiIsNull(myApi);
        try {
            return myApi.getVideoFtMaxSize();
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
        return 0;
    }

    public void removeMsgWithNotificationByThread(long threadId)
            throws ServiceDisconnectedException {
        LogHelper.i(String.format(Locale.getDefault(),
                "enter method:removeMsgWithNotificationByThread. [threadId]=%d", threadId));
        VerificationUtil.ApiIsNull(myApi);
        try {
            myApi.removeMsgWithNotificationByThread(threadId);
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
    }

    public void backupMessageList(List<SimpleMsg> simpleMsgList)
            throws ServiceDisconnectedException {
        LogHelper.i(String.format(Locale.getDefault(),
                "enter  method:backupMessageList. [simpleMsgList]=%s", simpleMsgList.toString()));
        VerificationUtil.ApiIsNull(myApi);
        try {
            myApi.backupMessageList(simpleMsgList);
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
    }

    public ChatMessage getMessageByTransferId(String transferId)
            throws ServiceDisconnectedException {
        LogHelper.i(String.format(Locale.getDefault(),
                "enter method:getMessageByTransferId. [transferId]=%s", transferId));
        VerificationUtil.ApiIsNull(myApi);
        try {
            return myApi.getMessageByTransferId(transferId);
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
        return null;
    }
    
    public void cancelBackup() throws ServiceDisconnectedException {
        LogHelper.i("enter method:cancelBackup.");
        VerificationUtil.ApiIsNull(myApi);
        try {
            myApi.cancelBackup();
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
    }
    
    public void forwardImageFile(long thread_id, long sms_id, String number, String id,
            int burnFlag, int barCycle) throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper
                .i(String.format(
                        Locale.getDefault(),
                        "enter method forwardImageFile. [thread_id,sms_id,number,id,burnFlag,barCycle]=%d,%d,%s,%s,%d,%d",
                        thread_id, sms_id, number, id, burnFlag, barCycle));

        if(TextUtils.isEmpty(id)){
            LogHelper.i("id field value error");
            return;
        }
        
        if (!VerificationUtil.isNumber(number)) {
            LogHelper.i("number field value error");
            return;
        }
        if (!VerificationUtil.isBurnFlagCorrect(burnFlag)) {
            LogHelper.i("burnFlag field must be 0 or 1");
            return;
        }
        if (barCycle < 0) {
            LogHelper.i("barCycle field must be a positive int");
            return;
        }

        try {
            myApi.forwardImageFile(thread_id, sms_id, VerificationUtil.formatNumber(number), id,
                    burnFlag, barCycle);
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
    }
    
    public void forwardVideoFile(long thread_id, long sms_id, String number, String id,
            int length, int burnFlag, int barCycle, boolean isRecord)
            throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper
                .i(String.format(
                        Locale.getDefault(),
                        "enter method forwardVideoFile. [thread_id,sms_id,number,id,length,burnFlag,barCycle,isRecord]=%d,%d,%s,%s,%d,%d,%d,%b",
                        thread_id, sms_id, number, id, length, burnFlag, barCycle, isRecord));

        if(TextUtils.isEmpty(id)){
            LogHelper.i("id field value error");
            return;
        }
        
        if (!VerificationUtil.isNumber(number)) {
            LogHelper.i("number field value error");
            return;
        }
        if (!VerificationUtil.isBurnFlagCorrect(burnFlag)) {
            LogHelper.i("burnFlag field must be 0 or 1");
            return;
        }
        if (barCycle < 0) {
            LogHelper.i("barCycle field must be a positive int");
            return;
        }
        try {
            myApi.forwardVideoFile(thread_id, sms_id, VerificationUtil.formatNumber(number), id,
                    length, burnFlag, barCycle);
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
    }

    public void forwardOne2ManyImageFile(long thread_id, long sms_id, List<String> numbers,
            String id, int burnFlag, int barCycle)
            throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper
                .i(String.format(
                        Locale.getDefault(),
                        "enter method forwardOne2ManyImageFile. [thread_id,sms_id,numbers,id,burnFlag,barCycle]=%d,%d,%s,%s,%d,%d",
                        thread_id, sms_id, numbers.toString(), id, burnFlag, barCycle));
        
        if(TextUtils.isEmpty(id)){
            LogHelper.i("id field value error");
            return;
        }
        
        if (!VerificationUtil.isAllNumber(numbers)) {
            LogHelper.i("number field value error");
            return;
        }
        if (!VerificationUtil.isBurnFlagCorrect(burnFlag)) {
            LogHelper.i("burnFlag field must be 0 or 1");
            return;
        }
        if (barCycle < 0) {
            LogHelper.i("barCycle field must be a positive int");
            return;
        }
        
        try {
            myApi.forwardOne2ManyImageFile(thread_id, sms_id, VerificationUtil.formatNumbers(numbers),
                    id, burnFlag, barCycle);
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
    }
    
    public void forwardOne2ManyVideoFile(long thread_id, long sms_id, List<String> numbers,
            String id, int length, int burnFlag, int barCycle, boolean isRecord)
            throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper
                .i(String.format(
                        Locale.getDefault(),
                        "enter method forwardOne2ManyVideoFile. [thread_id,sms_id,numbers,id,length,burnFlag,barCycle,isRecord]=%d,%d,%s,%s,%d,%d,%d,%b",
                        thread_id, sms_id, numbers.toString(), id, length, burnFlag,
                        barCycle, isRecord));
        
        if(TextUtils.isEmpty(id)){
            LogHelper.i("id field value error");
            return;
        }
        
        if (!VerificationUtil.isAllNumber(numbers)) {
            LogHelper.i("number field value error");
            return;
        }
        if (!VerificationUtil.isBurnFlagCorrect(burnFlag)) {
            LogHelper.i("burnFlag field must be 0 or 1");
            return;
        }
        if (barCycle < 0) {
            LogHelper.i("barCycle field must be a positive int");
            return;
        }
        try {
            myApi.forwardOne2ManyVideoFile(thread_id, sms_id, VerificationUtil.formatNumbers(numbers),
                    id, length, burnFlag, barCycle);
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
    }
    
    public void forwardGroupImageFile(long thread_id, String conversationId, long sms_id,
            String id, String groupId) throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper
                .i(String.format(
                        Locale.getDefault(),
                        "enter method forwardGroupImageFile. [thread_id,conversationId,sms_id,id,groupId]=%d,%s,%d,%s,%s",
                        thread_id, conversationId, sms_id, id, groupId));
        
        if(TextUtils.isEmpty(id)){
            LogHelper.i("id field value error");
            return;
        }

        try {
            myApi.forwardGroupImageFile(thread_id, conversationId, sms_id, id, groupId);
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
    }
    
    public void forwardGroupVideoFile(long thread_id, String conversationId, long sms_id,
            String id, int length, String groupId, boolean isRecord)
            throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper
                .i(String.format(
                        Locale.getDefault(),
                        "enter method forwardGroupVideoFile. [thread_id,conversationId,sms_id,id,length,groupId,isRecord]=%d,%s,%d,%s,%d,%s,%b",
                        thread_id, conversationId, sms_id, id, length, groupId, isRecord));
        
        if(TextUtils.isEmpty(id)){
            LogHelper.i("id field value error");
            return;
        }
        
        try {
            myApi.forwardGroupVideoFile(thread_id, conversationId, sms_id, id, length, groupId);
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
    }
}
