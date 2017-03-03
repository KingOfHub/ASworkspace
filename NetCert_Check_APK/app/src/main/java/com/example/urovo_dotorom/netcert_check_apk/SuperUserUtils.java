package com.example.urovo_dotorom.netcert_check_apk;

import java.io.BufferedInputStream;
import java.io.IOException;

/**
 * All methods/sub-classes in this class must be either private or protected
 * TODO: This must be removed in the final release build
 * 
 * @author Pranay
 * 
 */
public class SuperUserUtils {
	public static class CommandLineResult {
		public final StringBuffer stdout = new StringBuffer();
		public final StringBuffer stderr = new StringBuffer();
		public Integer resultCode = null;
	}

	public static final boolean canExecuteSU() {
		try {
			CommandLineResult result = execute(new String[] { "su", "-c" , "exit" });
			System.out.println(result.stdout);
			System.out.println(result.stderr);
			android.util.Log.v("==================", result.stdout+"");
			android.util.Log.v("==================", result.stderr+"");			
			return result != null && result.resultCode == 0;
		} catch (Throwable e) {
			return false;
		}
	}

	/**
	 * Executes One Liners
	 * 
	 * @param command
	 * @throws InterruptedException
	 * @throws IOException
	 */
	protected static synchronized CommandLineResult execute(
			final String[] command) throws InterruptedException, IOException,
			Throwable {
		final Process process = Runtime.getRuntime().exec(command);
		final CommandLineResult clr = new CommandLineResult();
		new Thread("CommandLineInputReader") {
			@Override
			public void run() {
				final BufferedInputStream bis = new BufferedInputStream(
						process.getInputStream());
				int result;
				try {
					while ((result = bis.read()) != -1) {
						clr.stdout.append((char) result);
					}
				} catch (final Throwable e) {
				} finally {
					try {
						bis.close();
					} catch (final IOException e) {
						e.printStackTrace();
					}
				}
			}
		}.start();

		new Thread("CommandLineErrorReader") {
			@Override
			public void run() {
				final BufferedInputStream bis = new BufferedInputStream(
						process.getErrorStream());
				int result;
				try {
					while ((result = bis.read()) != -1) {
						clr.stderr.append((char) result);
					}
				} catch (final Throwable e) {
				} finally {
					try {
						bis.close();
					} catch (final IOException e) {
						e.printStackTrace();
					}
				}
			}
		}.start();

		clr.resultCode = process.waitFor();
		return clr;
	}
}