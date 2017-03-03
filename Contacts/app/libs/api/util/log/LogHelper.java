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
package com.suntek.mway.rcs.client.api.util.log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.os.Environment;
import android.util.Log;

/**
 * <p>Title: LogHelper.java</p>
 * <p>Description: The Class LogHelper.</p>
 * <p>Copyright: Copyright (c) 2010</p>
 * <p>Company: pci-suntek</p>
 *
 * @author hhao
 * @version 1.0
 */
public final class LogHelper {

    /** The Constant OPEN_DEBUG. */
    public static final String OPEN_DEBUG = "_SETTING_SERVICE_OPEN_DEBUG";

    /** Is debug mode. */
    private static boolean mIsDebugMode = true;

    /** Will the log write to file. */
    private static Boolean MYLOG_WRITE_TO_FILE = true;

    /** The log tag name. */
    private static String TAG = "RCS_Service_API";

    /** The Constant CLASS_METHOD_LINE_FORMAT. */
    private static final String CLASS_METHOD_LINE_FORMAT = "%s.%s()  Line:%d  (%s)";

    /** The logfile dir path in sdcard. */
    public static String MYLOG_PATH_SDCARD_DIR = Environment.getExternalStorageDirectory().getPath() + "/Android/data/com.suntek.mway.rcs.service.api/logs";

    /** The log file's name. */
    private static String MYLOGFILEName = "rcs_service_api_log";

    /** My log format style. */
    private static SimpleDateFormat myLogSdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    /** The log format style. */
    private static SimpleDateFormat logfile = new SimpleDateFormat("yyyy-MM-dd");

    /** The log's level. */
    private final static int logLevel = Log.VERBOSE;

    private static boolean isSensitiveLog = false;

    /**
     * Log.
     *
     * @param str the str
     * @param level the level
     */
    private static void log(String str, int level) {
        log(str, level, null);
    }

    /**
     * Log.
     *
     * @param str the str
     * @param level the level
     * @param throwable the throwable
     */
    private static void log(String str, int level, Throwable throwable) {
        if (mIsDebugMode) {
            if (logLevel <= level) {
                //Get the method name from the stackTrace.
                StackTraceElement[] array = Thread.currentThread().getStackTrace();
                StackTraceElement traceElement = (array!=null && array.length > 5 ? array[5]: array[array.length-1]);
                String logText = String.format(CLASS_METHOD_LINE_FORMAT,
                        traceElement.getClassName(),
                        traceElement.getMethodName(),
                        traceElement.getLineNumber(),
                        traceElement.getFileName());
                // ERROR, WARN, INFO, DEBUG, VERBOSE
                String logTag = "";
                if (level == Log.VERBOSE) {
                    Log.v(TAG, logText + "->" + str);
                    logTag = "[VERBOSE]";
                } else if (level == Log.DEBUG) {
                    Log.d(TAG, logText + "->" + str);
                    logTag = "[DEBUG]";
                } else if (level == Log.INFO) {
                    Log.i(TAG, logText + "->" + str);
                    logTag = "[INFO ]";
                } else if (level == Log.WARN) {
                    Log.w(TAG, logText + "->" + str);
                    logTag = "[WARN ]";
                } else if (level == Log.ERROR) {
                    if (throwable != null) {
                        Log.e(TAG, logText + "->" + str, throwable);
                    } else {
                        Log.e(TAG, logText + "->" + str);
                    }
                    logTag = "[ERROR]";
                }
                if (MYLOG_WRITE_TO_FILE
                        && android.os.Environment.getExternalStorageState()
                                .equals(android.os.Environment.MEDIA_MOUNTED)) {
                    if (throwable != null) {
                        str += "\n" + Log.getStackTraceString(throwable);
                    }
                    writeLogtoFile(MYLOGFILEName, TAG + " " + logTag, logText
                            + "->" + str);
                }
            }

        }
    }

    /**
     * Trace.
     *
     * @param str the str
     */
    public static void trace(String str) {
        log(str, Log.DEBUG);
    }

    /**
     * VERBOSE.
     *
     * @param str the str
     */
    public static void v(String str) {
        log(str, Log.VERBOSE);
    }

    /**
     * DEBUG.
     *
     * @param str the str
     */
    public static void d(String str) {
        log(str, Log.DEBUG);
    }

    /**
     * WARN.
     *
     * @param str the str
     */
    public static void w(String str) {
        log(str, Log.WARN);
    }

    /**
     * INFO.
     *
     * @param str the str
     */
    public static void i(String str) {
        log(str, Log.INFO);
    }

    /**
     * ERROR.
     *
     * @param str the str
     */
    public static void e(String str) {
        log(str, Log.ERROR);
    }

    /**
     * ERROR.
     *
     * @param str the str
     * @param throwable the throwable
     */
    public static void e(String str, Throwable throwable) {
        log(str, Log.ERROR, throwable);
    }

    public static String sensitive(String str) {
        if (isSensitiveLog) {
            return "*****";
        } else {
            return str;
        }
    }

    /**
     * Open and write the logfile
     * @param filename the filename
     * @param tag the tag
     * @param text the text
     * @return *
     */
    private synchronized static void writeLogtoFile(String filename,
            String tag, String text) {
        File filePath = new File(MYLOG_PATH_SDCARD_DIR);
        if (!filePath.exists()) {
            filePath.mkdirs();
        }
        Date nowtime = new Date();
        String needWriteFiel = logfile.format(nowtime);
        String needWriteMessage = myLogSdf.format(nowtime) + "    " + tag
                + "    " + text;
        File file = new File(MYLOG_PATH_SDCARD_DIR, filename + "_"
                + needWriteFiel + ".log");
        FileWriter filerWriter = null;
        BufferedWriter bufWriter = null;
        try {
            //Append the content to the file.
            filerWriter = new FileWriter(file, true);
            bufWriter = new BufferedWriter(filerWriter);
            bufWriter.write(needWriteMessage);
            bufWriter.newLine();
        } catch (Exception e) {
            printStackTrace(e);
        }finally {
            try {
                if(bufWriter!=null) bufWriter.close();
            }catch(Exception ex) {}
            try {
                if(filerWriter!=null) filerWriter.close();
            }catch(Exception ex) {}
        }
    }

    /**
     * Prints the stack trace.
     *
     * @param throwable the throwable
     */
    public static void printStackTrace(Throwable throwable) {
        if (mIsDebugMode) {
            Log.w(TAG, "", throwable);
        }
    }

}
