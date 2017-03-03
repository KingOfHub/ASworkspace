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
package com.suntek.mway.rcs.client.api.util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;

import com.suntek.mway.rcs.client.aidl.constant.MediaConstants;
import com.suntek.mway.rcs.client.aidl.provider.SuntekMessageData;
import com.suntek.mway.rcs.client.aidl.utils.MediaUtils;
import com.suntek.mway.rcs.client.aidl.utils.SettingUtils;
import com.suntek.mway.rcs.client.api.util.log.LogHelper;

public class VerificationUtil {
    private static final String SIP_PREFIX = "sip:";
    private static final String TEL_PREFIX = "tel:";

    public static void ApiIsNull(Object api) throws ServiceDisconnectedException {
        if (api == null) {
            throw new ServiceDisconnectedException("Service unavailable, myApi is null");
        }
    }

    public static boolean isNumber(String number) {
        if (number == null) {
            return false;
        }
        return VerificationUtil
                .formatWithout86(VerificationUtil.getNumberFromUri(number))
                .replaceAll(" ", "").replaceAll("-", "").matches("\\d+");
    }

    public static boolean isAllNumber(List<String> numbers) {
        boolean sign = true;
        for (String number : numbers) {
            if (!isNumber(number)) {
                sign = false;
            }
        }
        return sign;
    }

    public static String formatNumber(String number) {
        return number.replaceAll(" ", "").replaceAll("-", "");
    }

    public static List<String> formatNumbers(List<String> numbers) {
        List<String> re = new ArrayList<String>();
        for (String number : numbers) {
            re.add(formatNumber(number));
        }
        return re;
    }

    public static boolean isBurnFlagCorrect(int burnFlag) {
        return burnFlag == SuntekMessageData.MSG_BURN_AFTER_READ_FLAG
                || burnFlag == SuntekMessageData.MSG_BURN_AFTER_READ_NOT;
    }

    public static void isImageFile(String filename) throws FileSuffixException {
        if (!MediaUtils.isImageFile(filename)) {
            throw new FileSuffixException(
                    "File extension is incorrect, the correct extension is '"
                            + MediaConstants.IMAGE_SUFFIX + "'");
        }
    }

    public static void isAudioFile(String filename) throws FileSuffixException {
        if (!MediaUtils.isAudioFile(filename)) {
            throw new FileSuffixException(
                    "File extension is incorrect, the correct extension is '"
                            + MediaConstants.AUDIO_SUFFIX + "'");
        }
    }

    public static void isVideoFile(String filename) throws FileSuffixException {
        if (!MediaUtils.isVideoFile(filename)) {
            throw new FileSuffixException(
                    "File extension is incorrect, the correct extension is '"
                            + MediaConstants.VIDEO_SUFFIX + "'");
        }
    }
    
    public static void isCloudFile(String filename) throws FileSuffixException {
        if (!MediaUtils.isCloudFileAllowedFile(filename)) {
            throw new FileSuffixException(
                    "File extension is incorrect, the incorrect extension is '"
                            + MediaConstants.CLOUD_FILE_EXCLUDE_SUFFIX + "'");
        }
    }

    public static void isFileSizeToLarge(String filename, long maxSize)
            throws FileTransferException {
        File file = new File(filename);
        if (file.exists() && file.isFile() && file.length() > (maxSize * 1024)) {
            throw new FileTransferException("File too large "
                    + (file.length() / 1024)
                    + " KB. Max size of file to be transfer is " + maxSize
                    + " KB.");
        }
    }

    public static void isAudioDurationToLong(Context context, String filename, long maxDuration, int recordTime)
            throws FileDurationException {
        File file = new File(filename);
        if (file.exists() && file.isFile()){
            int duration = MediaUtils.getAmrFileDuration(context, file);
            if(duration >= (maxDuration + 1) * 1000 || recordTime > maxDuration){
                LogHelper.i("throw FileDurationException, duration=" + duration);
                throw new FileDurationException("File duration too long "
                        + duration
                        + " s. Max duration is " + maxDuration
                        + " s.");
            }
        }
    }

    public static void isVedioDurationToLong(Context context, String filename, long maxDuration, int recordTime)
            throws FileDurationException {
        File file = new File(filename);
        if (file.exists() && file.isFile()){
            int duration = MediaUtils.getVideoFileDuration(context, file);
            if(duration >= (maxDuration + 1) * 1000 || recordTime > maxDuration){
                LogHelper.i("throw FileDurationException, duration=" + duration);
                throw new FileDurationException("File duration too long "
                        + duration
                        + " s. Max duration is " + maxDuration
                        + " s.");
            }
        }
    }

    public static final String DMS_FT_MAX_SIZE = "ftMaxSize";

    public static long getFtMaxSize(Context context) {
        return SettingUtils.getSetting(context, DMS_FT_MAX_SIZE,
                MediaConstants.FT_MAX_SIZE);
    }

    public static final String DMS_IMAGE_FT_MAX_SIZE = "imageFtMaxSize";

    public static long getImageFtMaxSize(Context context) {
        return SettingUtils.getSetting(context, DMS_IMAGE_FT_MAX_SIZE,
                MediaConstants.IMAGE_FT_MAX_SIZE);
    }

    public static final String DMS_VIDEO_FT_MAX_SIZE = "videoFtMaxSize";

    public static long getVideoFtMaxSize(Context context) {
        return SettingUtils.getSetting(context, DMS_VIDEO_FT_MAX_SIZE, MediaConstants.VIDEO_FT_MAX_SIZE);
    }

    public static final String DMS_VIDEO_MAX_TIME = "videoMaxTime";

    public static long getVideoMaxTime(Context context) {
        return SettingUtils.getSetting(context, DMS_VIDEO_MAX_TIME, MediaConstants.VIDEO_MAX_TIME);
    }

    public static final String DMS_AUDIO_MAX_TIME = "audioMaxTime";

    public static long getAudioMaxTime(Context context) {
        return SettingUtils.getSetting(context, DMS_AUDIO_MAX_TIME, MediaConstants.AUDIO_MAX_TIME);
    }

    public static String formatWithout86(String mobile) {
        String formatStr = mobile;
        if (formatStr != null) {
            formatStr = formatStr.replaceAll(" ", "");
            if (formatStr.startsWith("+86")) {
                formatStr = formatStr.substring(3);
            }
            if (formatStr.startsWith("86")) {
                formatStr = formatStr.substring(2);
            }
            if (formatStr.startsWith(" 86")) {
                formatStr = formatStr.substring(3);
            }
        }
        if (formatStr == null) {
            formatStr = "";
        }
        return formatStr.trim();
    }

    public static String getNumberFromUri(String uriStr) {
        if (uriStr == null) {
            return "";
        }
        int index = uriStr.lastIndexOf("<");
        if (index != -1) {
            int index2 = uriStr.indexOf(">");
            if (index2 != -1) {
                uriStr = uriStr.substring(index + 1, index2);
            } else {
                uriStr = uriStr.substring(index + 1);
            }
        }
        if (uriStr.endsWith(">")) {
            uriStr = uriStr.substring(0, uriStr.length() - 1);
        }
        if (uriStr.startsWith(TEL_PREFIX)) {
            uriStr = uriStr.substring(4);
        } else if (uriStr.startsWith(SIP_PREFIX)) {
            uriStr = uriStr.substring(4, uriStr.indexOf("@"));
        }

        int pos = uriStr.indexOf("@");
        if(pos != -1){
            uriStr = uriStr.substring(0, pos);
        }
        return uriStr;
    }
}
