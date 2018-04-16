package ch.icosys.popjava.core.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.Date;

import ch.icosys.popjava.core.system.POPJavaConfiguration;

/**
 * This class is used to write log file
 */
public class LogWriter {
	
	private static final String LOG_FOLDER_NAME = "logFolder";
	private static final String DEFAULT_LOCATION = "/usr/local/popj/"+LOG_FOLDER_NAME;
	private static final Configuration conf = Configuration.getInstance();

	/**
	 * Process identifier
	 */
	private static String pid = "";
	
	/**
	 * Log folder where the log files will be written
	 */
	public static String logFolder;
	
	/**
	 * Prefix of the log file
	 */
	public static String prefix = "pop-java-";

	static {
		String pidTemp = ManagementFactory.getRuntimeMXBean().getName();
		StringBuilder pidSb = new StringBuilder();
		for (int index = 0; index < pidTemp.length(); index++) {
			char c = pidTemp.charAt(index);
			if (Character.isLetterOrDigit(c)){
				pidSb.append(c);
			}
		}
		pid = pidSb.toString();
		
		String popLocation = POPJavaConfiguration.getPopJavaLocation();
		
		if(!popLocation.isEmpty()){
			logFolder = String.format("%s%s%s", popLocation, File.separator, LOG_FOLDER_NAME);
		}else{
			logFolder = DEFAULT_LOCATION;
		}
		
		//Create log folder if it does not exist yet
		if(!new File(logFolder).mkdirs()){//Fall back to tmp if all fails
		    logFolder = System.getProperty("java.io.tmpdir");
		}
	}

	/**
	 * Write a new log information line in the file
	 * @param info		Information to write
	 * @param filePath	Path of the log file
	 */
	public static void writeLogInfo(String info, String filePath) {
		info = pid + "-" + (new Date()).toString() +":"+System.currentTimeMillis() + "-" + info;
		info += "\r\n";
		writeLogfile(info, filePath);

	}

	public static synchronized void printDebug(String message){
		if (conf.isDebug()) {
			System.out.println(message);
		}
	}
	
	/**
	 * Write a new debug information line in the file
	 * @param info	Information to write
	 */
	public synchronized static void writeDebugInfo(String info) {
		
		if (conf.isDebug()) {
			System.out.println(info);
			logInfo(info);
		}
	}
	
	/**
	 * Write a new debug information line in the file
	 * @param format	Format of information to write {@link java.lang.String#format}
	 * @param args  Arguments for the format
	 */
	public synchronized static void writeDebugInfo(String format, Object... args) {
		
		if (conf.isDebug()) {
			String formattedInfo = String.format(format, args);
			System.out.println(formattedInfo);
			logInfo(formattedInfo);
		}
	}
	
	private static void logInfo(String info){
		if (conf.isDebug()) {
			info = pid + "-" + (new Date()).toString()+":"+System.currentTimeMillis() + "-" + info;
			info += "\r\n";
			String path = String.format("%s%s%s.txt", logFolder, File.separator, prefix);
			writeLogfile(info, path);
		}
	}
	
	/**
	 * Writes an exception to the same log as writeDebugInfo. The complete
	 * backtrace is logged.
	 * @param e The exception to be logged
	 */
	public static void writeExceptionLog(Throwable e){
		if(conf.isDebug()){
			e.printStackTrace();
		}
		logInfo("Exception "+ e.getClass().getName()+" "+e.getMessage());
		for (StackTraceElement trace : e.getStackTrace()) {
			logInfo(trace.getClassName() + " " + trace.getLineNumber());
		}
	}

	/**
	 * Write new log information into a file
	 * @param info	Information to write
	 * @param path	Path of the file
	 */
	public synchronized static void writeLogfile(String info, String path) {
		path = path.replace(".txt", pid + ".txt");
		BufferedWriter out = null;
		try {
			out = new BufferedWriter(new FileWriter(path, true));
			out.write(info);
		}catch (IOException e) {
			e.printStackTrace();
		}finally{
			if(out != null){
				try {
					out.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

	}
	
	/**
	 * Remove all file in the log directory
	 * @return	true if the action is succeed
	 */
	public static boolean deleteLogDir() {
		File dir = new File(logFolder);
		String[] children = dir.list();
		for (String child : children) {
			new File(dir, child).delete();
		}
		return true;
	}

}
