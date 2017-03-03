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
package com.suntek.mway.rcs.client.api.voip.impl;

import java.util.ArrayList;
import java.util.Locale;

import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.os.IBinder;

import com.suntek.mway.rcs.client.api.ClientApi;
import com.suntek.mway.rcs.client.api.RCSServiceListener;
import com.suntek.mway.rcs.client.aidl.plugin.IRichScreenApi;
import com.suntek.mway.rcs.client.aidl.plugin.entity.richscrn.PhoneList;
import com.suntek.mway.rcs.client.aidl.plugin.entity.richscrn.ResultInfo;
import com.suntek.mway.rcs.client.aidl.plugin.entity.richscrn.ResultUtil;
import com.suntek.mway.rcs.client.aidl.plugin.entity.richscrn.RichScrnShowing;
import com.suntek.mway.rcs.client.api.util.ServiceDisconnectedException;
import com.suntek.mway.rcs.client.api.util.VerificationUtil;
import com.suntek.mway.rcs.client.api.util.log.LogHelper;

public class RichScreenApi extends ClientApi {
    /** The service name. */
    private static String serviceName = "com.suntek.mway.rcs.app.service.api.impl.richscreen.RichScreenApiService";

    /** The api. */
    private IRichScreenApi myApi;

    /** The service connection. */
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            myApi = IRichScreenApi.Stub.asInterface(service);
            notifyServiceConnected();
            LogHelper.d("RichScreenApi have success connect, api="+myApi);
        }

        public void onServiceDisconnected(ComponentName className) {
            if(isNormallyClosed || reconnectionTimes > MAX_RECONECTION_TIMES) {
                LogHelper.d("IRichScreenApi api disconnect service");
                myApi = null;
                notifyServiceDisconnected();
            } else {
                LogHelper.d("illegal call IRichScreenApi api disconnect service :"
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

    public RichScreenApi(String theService) {
        super(serviceName);
        super.initServiceConnect(mConnection);
    }

    @Override
    public void init(Context context, RCSServiceListener listener) {
        LogHelper.i(String.format( Locale.getDefault(),"enter method:init. [context]=%s", context.toString()));
        // TODO Auto-generated constructor stub
        super.init(context, listener);
//
//        // success bind service and then go to init richScreen plugin service:
//        try {
//            if (super.isBinded()) myApi.init(100);
//        } catch (Exception ex) {
//            // TODO Auto-generated catch block
//            LogHelper.e(ex.getMessage(), ex);
//        }
    }

    /**
     * Gets the rich screen object.
     *
     * @param missdn
     *            the missdn of peer , When a call event occurs.
     * @param phoneEvent
     *            the phone call event
     * @return the rich screen object
     */
    public RichScrnShowing getRichScrnObj( String missdn, String phoneEvent ) throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        RichScrnShowing richScrnShowingObject = null;
        try {
            ResultUtil resultObj = myApi.getRichScrnObj(missdn, phoneEvent);

            //sdk local could't get the RichScrnShowingObject
            if(null == resultObj || !resultObj.isResultFlag() || null == resultObj.getResultObj()) {
                //go to download new RichScrnObj....
                if(downloadRichScrnObj(missdn, phoneEvent)) {
                    resultObj = myApi.getRichScrnObj(missdn, phoneEvent);
                }
            }

            if(resultObj!=null && resultObj.isResultFlag()) {
                LogHelper.i("success get the richScrnShowingObject about number:"+missdn+" at phone call event "+phoneEvent);
                richScrnShowingObject = (RichScrnShowing)resultObj.getResultObj();
            }
            else  {
                LogHelper.w("could not get the richScrnShowingObject about number:"+missdn+" at phone call event "+phoneEvent);
            }
        } catch (Exception ex) {
            // TODO Auto-generated catch block
            LogHelper.e(ex.getMessage(), ex);
        }
        return richScrnShowingObject;
    }

    /**
     * Download rich screen object.
     *
     * @param missdn
     *            the missdn
     * @param phoneEvent
     *            the phone event
     * @return the result info
     */
    public boolean downloadRichScrnObj( String missdn, String phoneEvent ) {
        boolean flag = false;
        try {
            ResultInfo downlodRlt = myApi.downloadRichScrnObj(missdn, phoneEvent);

            if(downlodRlt != null && downlodRlt.isSuccess()) {
                LogHelper.i("success download the richScrnShowingObject about number:"+missdn+" at phone call event "+phoneEvent);
                flag = true;
            }
            else LogHelper.w("could not download the richScrnShowingObject about number:"+missdn+" at phone call event "+phoneEvent);
        } catch (Exception ex) {
            // TODO Auto-generated catch block
            LogHelper.e(ex.getMessage(), ex);
        }
        return flag;
    }

    /**
     * Clear rich screen local cache.
     *
     * @param phoneEvent
     *            the phone event
     * @return the result info
     */
    public boolean clearRichScrnLocalCache( String phoneEvent ) throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        boolean flag = false;
        try {
            ResultInfo result =  myApi.clearRichScrnLocalCache(phoneEvent);
            if(result != null && result.isSuccess()) {
                LogHelper.i("success clear the richScrnShowingObject at phone call event "+phoneEvent);
                flag = true;
            }
            else LogHelper.w("could not clear the richScrnShowingObject at phone call event "+phoneEvent);
        } catch (Exception ex) {
            // TODO Auto-generated catch block
            LogHelper.e(ex.getMessage(), ex);
        }
        return flag;
    }

    public ResultInfo collectRichScrnObj(String sourceType, String cId) throws ServiceDisconnectedException {
        LogHelper.i(String.format( Locale.getDefault(),"enter method:collectRichScrnObj. [sourceType,cId]=%s,%s", sourceType,cId));
        VerificationUtil.ApiIsNull(myApi);
        try {
            return myApi.collectRichScrnObj(sourceType, cId);
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
        return null;
    }

    public ResultInfo downloadHomeLocRules(final String phoneEvent) throws ServiceDisconnectedException {
        LogHelper.i(String.format( Locale.getDefault(),"enter method:downloadHomeLocRules. [phoneEvent]=%s", phoneEvent));
        VerificationUtil.ApiIsNull(myApi);
        try {
            return myApi.downloadHomeLocRules(phoneEvent);
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
        return null;
    }

    public ResultInfo init(int cacheSize) throws ServiceDisconnectedException {
        LogHelper.i(String.format( Locale.getDefault(),"enter method:init. [cacheSize]=%d", cacheSize));
        VerificationUtil.ApiIsNull(myApi);
        try {
            return myApi.init(cacheSize);
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
        return null;
    }

    public ResultInfo richScrnCMCCSSOLogin() throws ServiceDisconnectedException {
        LogHelper.i(String.format( Locale.getDefault(),"enter method:richScrnCMCCSSOLogin. "));
        VerificationUtil.ApiIsNull(myApi);
        try {
            return myApi.richScrnCMCCSSOLogin();
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
        return null;
    }

    public ResultInfo richScrnChangeNetWork() throws ServiceDisconnectedException {
        LogHelper.i(String.format( Locale.getDefault(),"enter method:richScrnChangeNetWork. "));
        VerificationUtil.ApiIsNull(myApi);
        try {
            return myApi.richScrnChangeNetWork();
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
        return null;
    }

    public void startSiteApk(ArrayList<String> mobile) throws ServiceDisconnectedException {
        LogHelper.i(String.format( Locale.getDefault(),"enter method:startSiteApk. [mobile]=%s", mobile));
        VerificationUtil.ApiIsNull(myApi);
        try {
            PhoneList phoneList = new PhoneList();
            phoneList.setPhoneList(mobile);
            myApi.startSiteApk(phoneList);
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
    }

    public enum PhoneEventEnum {
        INITIATE_A_VOICE_CALL("111 00 00000"), // Initiate a voice call
        INCOMING_VOICE_CALL_THE_TERMINAL_STARTS_RINGING("122 00 18000"), // Incoming voice call, the terminal starts ringing
        SWITCHED_VOICE_CALLS_CALLINGSIDE("113 00 20000"), // Switched voice calls (calling side)
        SWITCHED_VOICE_CALLS_CALLED_SIDE("123 00 20000"), // Switched voice calls (called side)
        VOICE_CALL_HANG_UP_CALLING_SIDE("114 00 20000"), // Voice call hang up (calling side)
        VOICE_CALL_HANG_UP_CALLED_SIDE("124 00 20000"), // Voice call hang up (called side)
        OUTGOING_VIDEO_CALL("211 00 00000"), // Outgoing video call
        VIDEO_CALL_COMES_IN_THE_TERMINAL_STARTS_RINGING("222 00 00000"), // Video call comes in, the terminal starts ringing
        VIDEO_CALL_IS_CONNECTED_CALLING_SIDE("213 00 20000"), // Video call is connected (calling side)
        VIDEO_CALL_IS_CONNECTED_CALLED_SIDE_SELECT_THE_VIDEO_CALL(
                "223 00 20000"), // Video call is connected (called side, select the video call)
        VIDEO_CALL_HANG_UP_CALLING_SIDE("214 00 20000"), // Video call hang up (calling side)
        VIDEO_CALL_HANG_UP_CALLED_SIDE("224 00 20000"), // Video call hang up (called side)
        ADDRESS_BOOK_IS_BING_VIEWED_VIEW_ENHANCED_SCREEN("933 10 11000"), // Terminal communication record, is being viewed, the owner asked to see the enhanced screen
        ADDRESS_BOOK_IS_BING_VIEWED_ENHANCED_OSD_SETTINGS("933 10 12000"), // Terminal communication record, is being viewed, the owner requires setting the enhanced screen
        ADDRESS_BOOK_HAVE_BEEN_UPDATED("934 10 20000"), // Terminal communication record have been updated (such as new or revised a contact)
        COMPLETED_RESTORE_FACTORY_SETTINGS("934 90 30000"), //Terminal has been completed to restore factory settings
        SIM_CARD_HAS_BEEN_REPLACED("944 90 40000"), // SIM card terminal has been replaced
        NON_SPECIFIC_EVENTS("000 00 00000"), // Non-specific events
        ;
        private String phoneEvent;

        private PhoneEventEnum(String phoneEvent) {
            this.phoneEvent = phoneEvent;
        }

        public String getPhoneEvent() {
            return phoneEvent;
        }
    }

}
