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
package com.suntek.mway.rcs.client.api.profile.impl;

import java.util.Locale;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;

import com.suntek.mway.rcs.client.api.ClientApi;
import com.suntek.mway.rcs.client.aidl.plugin.IProfileApi;
import com.suntek.mway.rcs.client.aidl.plugin.callback.IProfileListener;
import com.suntek.mway.rcs.client.aidl.plugin.entity.profile.Avatar;
import com.suntek.mway.rcs.client.aidl.plugin.entity.profile.Profile;
import com.suntek.mway.rcs.client.api.profile.callback.ProfileListener;
import com.suntek.mway.rcs.client.api.profile.callback.QRImgListener;
import com.suntek.mway.rcs.client.api.util.ServiceDisconnectedException;
import com.suntek.mway.rcs.client.api.util.VerificationUtil;
import com.suntek.mway.rcs.client.api.util.log.LogHelper;

/**
 * <p>Title: ProfileApi class</p>
 * <p>
 * Description: The class <code>ProfileApi</code> offers the functions of
 * operating the profile information.
 * In order to use ProfileApi, one must to initialize the API
 * in the method onCreate() in UI(Activity for example).<p></p>
 * Here is the pseudo code example:<p></p>
 * profileApi = new ProfileApi();<br/>
 * profileApi.init(this);
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
public class ProfileApi extends ClientApi  {

    /** The service name. */
    private static String serviceName = "com.suntek.mway.rcs.app.service.api.impl.profile.ProfileApiService";

    /** The api. */
    private static IProfileApi myApi;

    /** The service connection. */
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            myApi = IProfileApi.Stub.asInterface(service);
            notifyServiceConnected();
            LogHelper.d("IProfileApi have success connect, api="+myApi);
        }

        public void onServiceDisconnected(ComponentName className) {
            if(isNormallyClosed || reconnectionTimes > MAX_RECONECTION_TIMES) {
                LogHelper.d("IProfileApi api disconnect service");
                myApi = null;
                notifyServiceDisconnected();
            } else {
                LogHelper.d("illegal call IProfileApi api disconnect service :"
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
     * Instantiates a new profile api.
     */
    public ProfileApi() {
        super(serviceName);
        super.initServiceConnect(mConnection);
    }

    /**
     * Sets the my profile.
     *
     * @param profile the profile
     * @param listener the listener
     */
    public void setMyProfile(Profile profile, ProfileListener listener) throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper.i(String.format( Locale.getDefault(),"enter method setMyProfile. [profile]=%s", profile.toString()));
        try {
            myApi.setMyProfile(profile, listener);
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(),ex);
        }
    }

    /**
     * Sets the my head pic.
     *
     * @param imgObj the img obj
     * @param listener the listener
     */
    public void setMyHeadPic(Avatar imgObj,ProfileListener listener) throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper.i(String.format( Locale.getDefault(),"enter method setMyHeadPic. [imgObj]=%s", imgObj.toString()));
        try {
            myApi.setMyHeadPic(imgObj, listener);
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(),ex);
        }
    }

    /**
     * Gets the my profile.
     *
     * @param listener the listener
     * @return the my profile
     */
    public void getMyProfile(ProfileListener listener) throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper.i(String.format( Locale.getDefault(),"enter method getMyProfile. "));
        try {
            myApi.getMyProfile( listener);
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(),ex);
        }
    }

    /**
     * Gets the my head pic.
     *
     * @param listener the listener
     * @return the my head pic
     */
    public void getMyHeadPic(ProfileListener listener) throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper.i(String.format( Locale.getDefault(),"enter method getMyHeadPic. "));
        try {
            myApi.getMyHeadPic( listener);
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(),ex);
        }
    }

    /**
     * Gets the head pic by contact.
     *
     * @param contactId the contact id
     * @param listener the listener
     * @return the head pic by contact
     */
    public void getHeadPicByContact(long contactId,ProfileListener listener) throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper.i(String.format( Locale.getDefault(),"enter method getHeadPicByContact. [contactId]=%d", contactId));
        try {
            myApi.getHeadPicByContact(contactId, listener);
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(),ex);
        }
    }

    /**
     * Gets the head pic by number.
     *
     * @param number the number
     * @param listener the listener
     * @return the head pic by number
     */
    public void getHeadPicByNumber(String number,int pixel,ProfileListener listener) throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper.i(String.format( Locale.getDefault(),"enter method getHeadPicByNumber. [number]=%s", number));
        if (!VerificationUtil.isNumber(number)) {
            LogHelper.i("number field value error");
            return;
        }
        try {
            myApi.getHeadPicByNumber(VerificationUtil.formatNumber(number), pixel, listener);
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(),ex);
        }
    }

    /**
     * Refresh my qr img.
     *
     * @param profile the profile
     * @param includeEInfo the include e info
     * @param listener the listener
     */
    public void refreshMyQRImg(Profile profile, boolean includeEInfo,QRImgListener listener) throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper.i(String.format( Locale.getDefault(),"enter method refreshMyQRImg. [profile,includeEInfo]=%s,%b", profile.toString(),includeEInfo));
        try {
            myApi.refreshMyQRImg(profile,includeEInfo, listener);
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(),ex);
        }
    }

    /**
     * update the the contacts head photo at fixed time every day.
     *
     * @param hhmm The timing of the trigger. the time format is HH:mm in 24-hour time system
     * @param listener a callback whose method named onAvatarGet will be called
     * @return
     */
    public void updateContactsHeadPicAtFixedRateEveryDay(String hhmm,
            IProfileListener listener) throws ServiceDisconnectedException{
        VerificationUtil.ApiIsNull(myApi);
        LogHelper.i(String.format( Locale.getDefault(),"enter method updateContactsHeadPicAtFixedRateEveryDay. [hhmm]=%s", hhmm));
        try {
            myApi.updateContactsHeadPicAtFixedRateEveryDay(hhmm, listener);
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(),ex);
        }
    }

    public String getUpdateTimeOfContactsHeadPic() throws ServiceDisconnectedException{
        VerificationUtil.ApiIsNull(myApi);
        LogHelper.i(String.format( Locale.getDefault(),"enter method getUpdateTimeOfContactsHeadPic"));
        try {
            return myApi.getUpdateTimeOfContactsHeadPic();
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(),ex);
        }
        return null;
    }

}
