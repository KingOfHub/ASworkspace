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
package com.suntek.mway.rcs.client.api.impl.groupchat;

import java.util.List;
import java.util.Locale;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;

import com.suntek.mway.rcs.client.api.ClientApi;
import com.suntek.mway.rcs.client.api.exception.MemberFullException;
import com.suntek.mway.rcs.client.aidl.im.IGroupManagerApi;
import com.suntek.mway.rcs.client.aidl.im.OprResponse;
import com.suntek.mway.rcs.client.api.impl.callback.ConferenceCallback;
import com.suntek.mway.rcs.client.aidl.provider.model.GroupChatModel;
import com.suntek.mway.rcs.client.aidl.provider.model.GroupChatUser;
import com.suntek.mway.rcs.client.api.util.ServiceDisconnectedException;
import com.suntek.mway.rcs.client.api.util.VerificationUtil;
import com.suntek.mway.rcs.client.api.util.log.LogHelper;
public class ConfApi extends ClientApi {
    private static String serviceName = "com.suntek.mway.rcs.app.service.api.impl.im.GroupManagerApiService";

    IGroupManagerApi myApi;
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            LogHelper.d("client api connect service");
            myApi = IGroupManagerApi.Stub.asInterface(service);
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

    public ConfApi() {
        // TODO Auto-generated constructor stub
        super(serviceName);
        super.initServiceConnect(mConnection);
    }

    public String createGroupChat(String subject,List<String> users) throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper.i(String.format( Locale.getDefault(),"enter method createGroupChat. [subject,users]=%s,%s", subject,users.toString()));
        try {
            return myApi.createGroupChat(subject, users);
        } catch (Exception ex) {
            // TODO Auto-generated catch block
            LogHelper.e(ex.getMessage(),ex);
        }
        return null;
    }

    public int agreeToJoinGroup(String conversationId ,String contributionId ,String chatUri ,
            String subject , String numberData, long inviteTime) throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper.i(String.format( Locale.getDefault(),"enter method agreeToJoinGroup. [conversationId,contributionId,chatUri,subject,numberData,inviteTime]=%s,%s,%s,%s,%s,%d", conversationId,contributionId,chatUri,subject,numberData,inviteTime));
        try {
            return myApi.agreeToJoinGroup(conversationId, contributionId, chatUri, subject, numberData, inviteTime);
        } catch (Exception ex) {
            // TODO Auto-generated catch block
            LogHelper.e(ex.getMessage(),ex);
        }
        return OprResponse.OTHRE_ERROR;
    }

    public int refuseToJoinGroup(String conversationId) throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper.i(String.format( Locale.getDefault(),"enter method refuseToJoinGroup. [conversationId]=%s", conversationId));
        try {
            return myApi.refuseToJoinGroup(conversationId);
        } catch (Exception ex) {
            // TODO Auto-generated catch block
            LogHelper.e(ex.getMessage(),ex);
        }
        return OprResponse.OTHRE_ERROR;
    }

    public int updateGroupSubject(String groupId,String newSubject) throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper.i(String.format( Locale.getDefault(),"enter method updateGroupSubject. [groupId,newSubject]=%s,%s", groupId,newSubject));
        try {
            return myApi.updateGroupSubject(groupId, newSubject);
        } catch (Exception ex) {
            // TODO Auto-generated catch block
            LogHelper.e(ex.getMessage(),ex);
        }
        return OprResponse.OTHRE_ERROR;
    }

    public void modifyGroupMemo(String groupId,String memo) throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper.i(String.format( Locale.getDefault(),"enter method modifyGroupMemo. [groupId,memo]=%s,%s", groupId,memo));
        try {
            myApi.modifyGroupMemo(groupId, memo);
        } catch (Exception ex) {
            // TODO Auto-generated catch block
            LogHelper.e(ex.getMessage(),ex);
        }
    }

    public int disbandGroupChat(String groupId) throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper.i(String.format( Locale.getDefault(),"enter method disbandGroupChat. [groupId]=%s", groupId));
        try {
            return myApi.disbandGroupChat(groupId);
        } catch (Exception ex) {
            // TODO Auto-generated catch block
            LogHelper.e(ex.getMessage(),ex);
        }
        return OprResponse.OTHRE_ERROR;
    }

    public int kickedOutOfGroupChat(String groupId,String number) throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper.i(String.format( Locale.getDefault(),"enter method kickedOutOfGroupChat. [groupId,number]=%s,%s", groupId,number));
        try {
            return myApi.kickedOutOfGroupChat(groupId, number);
        } catch (Exception ex) {
            // TODO Auto-generated catch block
            LogHelper.e(ex.getMessage(),ex);
        }
        return OprResponse.OTHRE_ERROR;
    }

    public int assignGroupChairman(String groupId,String number) throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper.i(String.format( Locale.getDefault(),"enter method assignGroupChairman. [groupId,number]=%s,%s", groupId,number));
        try {
            return myApi.assignGroupChairman(groupId, number);
        } catch (Exception ex) {
            // TODO Auto-generated catch block
            LogHelper.e(ex.getMessage(),ex);
        }
        return OprResponse.OTHRE_ERROR;
    }
    public int quitGroupChat(String groupId,String number) throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper.i(String.format( Locale.getDefault(),"enter method quitGroupChat. [groupId,number]=%s,%s", groupId,number));
        try {
            return myApi.quitGroupChat(groupId, number);
        } catch (Exception ex) {
            // TODO Auto-generated catch block
            LogHelper.e(ex.getMessage(),ex);
        }
        return OprResponse.OTHRE_ERROR;
    }

    @Deprecated
    public void quitGroupChatEx(String groupId,String oldChairman,String newChairman) throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper.i(String.format( Locale.getDefault(),"enter method quitGroupChat. [groupId,oldChairman,newChairman]=%s,%s,%s", groupId,oldChairman,newChairman));
        try {
            myApi.quitGroupChatEx(groupId, oldChairman, newChairman);
        } catch (Exception ex) {
            // TODO Auto-generated catch block
            LogHelper.e(ex.getMessage(),ex);
        }
    }

    public int setMyAlias(String groupId,String alias) throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper.i(String.format( Locale.getDefault(),"enter method setMyAlias. [groupId,alias]=%s,%s", groupId,alias));
        try {
            return myApi.setMyAlias(groupId, alias);
        } catch (Exception ex) {
            // TODO Auto-generated catch block
            LogHelper.e(ex.getMessage(),ex);
        }
        return OprResponse.OTHRE_ERROR;
    }
    public String getGroupChatMemberDisplayName(String groupId,String number) throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper.i(String.format( Locale.getDefault(),"enter method getGroupChatMemberDisplayName. [groupId,number]=%s,%s", groupId,number));
        try {
            return myApi.getGroupChatMemberDisplayName(groupId, number);
        } catch (Exception ex) {
            // TODO Auto-generated catch block
            LogHelper.e(ex.getMessage(),ex);
        }
        return null;
    }

    public GroupChatUser getGroupChairman(String groupId) throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper.i(String.format( Locale.getDefault(),"enter method getGroupChairman. [groupId]=%s", groupId));
        try {
            return myApi.getGroupChairman(groupId);
        } catch (Exception ex) {
            // TODO Auto-generated catch block
            LogHelper.e(ex.getMessage(),ex);
        }
        return null;
    }
    public int inviteToJoinGroupChat(String groupId, String number) throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper.i(String.format( Locale.getDefault(),"enter method inviteToJoinGroupChat. [groupId,number]=%s,%s", groupId,number));
        try {
            return myApi.inviteOneMemberToGroupChat(groupId, number);
        } catch (RemoteException ex) {
            // TODO Auto-generated catch block
            LogHelper.e(ex.getMessage(),ex);
        }
        return OprResponse.OTHRE_ERROR;
    }

    public int inviteToJoinGroupChat(String groupId, List<String> numbers) throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper.i(String.format( Locale.getDefault(),"enter method inviteToJoinGroupChat. [groupId,numbers]=%s,%s", groupId,numbers.toString()));
        try {
            return myApi.inviteToJoinGroupChat(groupId, numbers);
        } catch (RemoteException ex) {
            // TODO Auto-generated catch block
            LogHelper.e(ex.getMessage(),ex);
        }
        return OprResponse.OTHRE_ERROR;
    }

    public void queryMemberHeadPic(String groupId, String number,int pixel, ConferenceCallback confCallback) throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper.i(String.format( Locale.getDefault(),"enter method queryMemberHeadPic. [groupId,number]=%s,%s", groupId,number));
        try {
            myApi.queryMemberHeadPic(groupId, number,pixel, confCallback);
        } catch (Exception ex) {
            // TODO Auto-generated catch block
            LogHelper.e(ex.getMessage(),ex);
        }
    }

    public void refreshMemberHeadPic(String groupId, String number,int pixel, ConferenceCallback confCallback) throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper.i(String.format( Locale.getDefault(),"enter method refreshMemberHeadPic. [groupId,number]=%s,%s", groupId,number));
        try {
            myApi.refreshMemberHeadPic(groupId, number,pixel, confCallback);
        } catch (Exception ex) {
            // TODO Auto-generated catch block
            LogHelper.e(ex.getMessage(),ex);
        }

    }
    public GroupChatModel  getLastGroupModel()throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper.i(String.format( Locale.getDefault(),"enter method getAllGroupId"));
        try {
            GroupChatModel gcm = myApi.getLastGroupModel();
            return gcm;
        } catch (Exception ex) {
            // TODO Auto-generated catch block
            LogHelper.e(ex.getMessage(),ex);
        }
        return null;
    }

    public void updateGroupPolicy(String groupId, int policy) throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        try {
            myApi.updateGroupPolicy(groupId, policy);
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
    }

    public List<GroupChatUser> getGroupChatUsersAllowChairman(String groupId)
            throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper.i(String.format(Locale.getDefault(),
                "enter method:getGroupChatUsersAllowChairman. [groupId]=%s",
                groupId));
        try {
            return myApi.getGroupChatUsersAllowChairman(groupId);
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
        return null;
    }

    public List<GroupChatUser> getGroupChatUsersByGroupId(String groupId)
            throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper.i(String.format(Locale.getDefault(),
                "enter method:getGroupChatUsersByGroupId. [groupId]=%s",
                groupId));
        try {
            return myApi.getGroupChatUsersByGroupId(groupId);
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
        return null;
    }

    public GroupChatModel getGroupChatByThreadId(String threadId)
            throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper
                .i(String.format(Locale.getDefault(),
                        "enter method:getGroupChatByThreadId. [threadId]=%s",
                        threadId));
        try {
            return myApi.getGroupChatByThreadId(threadId);
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
        return null;
    }

    public GroupChatModel getGroupChatByChatUri(String chatUri)
            throws ServiceDisconnectedException {
        VerificationUtil.ApiIsNull(myApi);
        LogHelper.i(String.format(Locale.getDefault(),
                "enter method:getGroupChatByChatUri. [chatUri]=%s", chatUri));
        try {
            return myApi.getGroupChatByChatUri(chatUri);
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
        return null;
    }

    public String getGroupChatDomainName() throws ServiceDisconnectedException {
        LogHelper.i(String.format( Locale.getDefault(),"enter method:getGroupChatDomainName. "));
        VerificationUtil.ApiIsNull(myApi);
        try {
            return myApi.getGroupChatDomainName();
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
        return null;
    }

    public String getPublicAccountDomainName() throws ServiceDisconnectedException {
        LogHelper.i(String.format( Locale.getDefault(),"enter method:getPublicAccountDomainName. "));
        VerificationUtil.ApiIsNull(myApi);
        try {
            return myApi.getPublicAccountDomainName();
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
        return null;
    }

    public int refuseAssigedAsChairman(String chatUri, long inviteTime, String conversationId, String contributionId) throws ServiceDisconnectedException {
        LogHelper.i(String.format( Locale.getDefault(),"enter method:refuseAssigedAsChairman. [chatUri,inviteTime,conversationId,contributionId]=%s,%d,%s,%s", chatUri,inviteTime,conversationId,contributionId));
        VerificationUtil.ApiIsNull(myApi);
        try {
            return myApi.refuseAssigedAsChairman(chatUri, inviteTime, conversationId, contributionId);
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
        return OprResponse.OTHRE_ERROR;
    }

    public int acceptAssignedAsChairman(String chatUri, long inviteTime, String conversationId, String contributionId) throws ServiceDisconnectedException {
        LogHelper.i(String.format( Locale.getDefault(),"enter method:acceptAssignedAsChairman. [chatUri,inviteTime,conversationId,contributionId]=%s,%d,%s,%s", chatUri,inviteTime,conversationId,contributionId));
        VerificationUtil.ApiIsNull(myApi);
        try {
            return myApi.acceptAssignedAsChairman(chatUri, inviteTime, conversationId, contributionId);
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
        return OprResponse.OTHRE_ERROR;
    }
    
    public int rejoinGroupChat(String chatUri) throws ServiceDisconnectedException {
        LogHelper.i(String.format( Locale.getDefault(),"enter rejoinGroupChat. [chatUri]=%s", chatUri));
        VerificationUtil.ApiIsNull(myApi);
        try {
            return myApi.rejoinGroupChat(chatUri);
        } catch (Exception ex) {
            LogHelper.e(ex.getMessage(), ex);
        }
        return 0;
    }
}
