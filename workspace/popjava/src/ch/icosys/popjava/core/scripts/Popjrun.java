package ch.icosys.popjava.core.scripts;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ch.icosys.popjava.core.broker.Broker;
import ch.icosys.popjava.core.mapgen.POPJObjectMap;
import ch.icosys.popjava.core.util.Configuration;

public class Popjrun {

	private static final String HELP_MESSAGE = "POP-Java Application Runner v1.0\n\n"
			+ "This program is used to run a POP-Java application or to generate object map\n\n"
			+ "Usage: popjrun <options> <objectmap> <mainclass>\n\n" + "OPTIONS:\n"
			+ "   -h, --help                Show this message\n" + "   -v, --verbose             Verbose mode\n"
			+ "   -k, --killall             Kill all parallel object (zombie) (not implemented)\n"
			+ "   -c, --classpath <files>   Include JAR or compiled Java class needed to run the application. Files must be separated by a "
			+ File.pathSeparatorChar + "\n" + "    -b, --broker             Run Broker with specified object" + "\n\n"
			+ "OPTIONS FOR OBJECT MAP GENERATION:\n"
			+ "   -l, --listlong <parclass> Generate the object map for the given parclasses. Parclasses can be a .class, .jar, .obj or .module file. Parclasses must be separated by "
			+ File.pathSeparatorChar;

	private static final String JAR_FOLDER = "JarFile";

	// private static final String JAR_OBJMAPGEN = JAR_FOLDER +
	// File.separatorChar + "popjobjectmapgen.jar";
	private static final String JAR_POPJAVA = JAR_FOLDER + File.separatorChar + Popjavac.POP_JAVA_JAR_FILE;

	private static final String DEFAULT_POP_JAVA_LOCATION;

	static {
		if (ScriptUtils.isWindows()) {
			DEFAULT_POP_JAVA_LOCATION = "C:\\Users\\asraniel\\workspace\\PopJava\\release\\";
		} else {
			DEFAULT_POP_JAVA_LOCATION = "/usr/local/popj/";
		}
	}

	private static boolean verbose = false;

	private static void printHelp() {
		System.out.println(HELP_MESSAGE);
	}

	// http://stackoverflow.com/questions/6356340/killing-a-process-using-java
	// http://stackoverflow.com/questions/81902/how-to-find-and-kill-running-win-processes-from-within-java
	private static void killAll() {
		if (ScriptUtils.isWindows()) {

		} else {

		}
	}

	/**
	 * Returns the path to POPJava (ex: /usr/local/popj/)
	 * 
	 * @return location of pop java's jar
	 */
	private static String getPopJavaLocation() {
		String popJavaLocation = System.getenv("POPJAVA_LOCATION");

		if (popJavaLocation == null || popJavaLocation.isEmpty()) {
			popJavaLocation = DEFAULT_POP_JAVA_LOCATION;
		} else {
			popJavaLocation += File.separatorChar;
		}

		return popJavaLocation;
	}

	private static String createClassPath(String classPath) {

		String popJavaLocation = getPopJavaLocation();

		String popJavaClassPath = popJavaLocation + JAR_POPJAVA;

		if (classPath.isEmpty()) {
			classPath = popJavaClassPath + File.pathSeparatorChar + ".";
		} else {
			classPath += File.pathSeparatorChar + popJavaClassPath;
		}

		return classPath;
	}

	private static boolean help = false;

	private static boolean killAll = false;

	private static boolean broker = false;

	private static String listLong = "";

	private static String classPath = "";

	private static List<String> parseArguments(String[] args) {
		label: for (int i = 0; i < args.length; i++) {
			switch (args[i]) {
			case "-h":
			case "--help":
				help = true;
				break;
			case "-k":
			case "--killall":
				killAll = true;
				break;
			case "-v":
			case "--verbose":
				verbose = true;
				break;
			case "-b":
			case "--broker":
				broker = true;
				break;
			case "-l":
			case "--listlong":
				if (args.length > i + 1) {
					listLong = args[i + 1];
					i++;
				} else {
					System.err.println("Listlong command needs a parameter following it");
					System.exit(0);
				}
				break;
			case "-c":
			case "--classpath":
				if (args.length > i + 1) {
					classPath = createClassPath(args[i + 1]);
					i++;
				} else {
					System.err.println("Classpath parameter needs a parameter following it");
					System.exit(0);
				}
				break;
			default:
				break label;
			}
		}

		List<String> arguments = new ArrayList<>(Arrays.asList(args));

		return arguments;
	}

	public static void main(String[] args) throws ClassNotFoundException, IllegalAccessException,
			IllegalArgumentException, InvocationTargetException, IOException {

		List<String> arguments = parseArguments(args);

		if (args.length == 0 || help) {
			printHelp();
			return;
		}

		if (killAll) {
			killAll();
			return;
		}

		if (!listLong.isEmpty()) {
			listLong(listLong);
			return;
		}

		String objectMap = "";

		if (arguments.size() >= 2) {
			objectMap = arguments.get(0);
			arguments.remove(0);
		}

		if (arguments.size() == 0) {
			System.err.println("No arguments where specified to run POP-Java application");
			return;
		}

		String main = arguments.get(0);
		arguments.remove(0);

		runForkedApplication(main, objectMap, arguments);
	}

	private static void runForkedApplication(String main, String objectMap, List<String> arguments) {
		// String java = System.getProperty("java.home") + "/bin/java";
		String java = "java";

		if (classPath.isEmpty()) {
			classPath = createClassPath("");
		}

		if (broker) {
			arguments.add(0, objectMap);
		} else {
			arguments.add(0, "-codeconf=" + objectMap);
		}
		arguments.add(0, main);

		if (broker) {
			arguments.add(0, Broker.class.getName());
		}

		arguments.add(0, classPath);
		arguments.add(0, "-cp");
		Configuration conf = Configuration.getInstance();
		if (conf.isActivateJmx()) {
			arguments.add(0, "-Dcom.sun.management.jmxremote.port=3333");
			arguments.add(0, "-Dcom.sun.management.jmxremote.ssl=false");
			arguments.add(0, "-Dcom.sun.management.jmxremote.authenticate=false");
		}

		arguments.add(0, "-javaagent:" + getPopJavaLocation() + JAR_POPJAVA);

		arguments.add(0, java);

		runPopApplication(arguments);
	}

	private static void listLong(String files) {
		String[] command = new String[2];
		command[0] = "-cwd=" + System.getProperty("user.dir");
		command[1] = "-file=" + files;

		POPJObjectMap.main(command);
	}

	private static class StreamReader implements Runnable {

		private InputStream in;

		public StreamReader(InputStream in) {
			this.in = in;
		}

		@Override
		public void run() {
			BufferedReader out = new BufferedReader(new InputStreamReader(in));
			String line;
			try {
				while (!Thread.currentThread().isInterrupted() && (line = out.readLine()) != null) {
					System.out.println(line);
				}
				out.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	private static int runPopApplication(List<String> arguments) {
		String argArray[] = new String[arguments.size()];
		arguments.toArray(argArray);

		if (verbose) {
			for (String arg : argArray) {
				System.out.print(arg + " ");
			}
			System.out.println();
		}

		ProcessBuilder builder = new ProcessBuilder(arguments);
		builder.redirectErrorStream(true);
		try {
			Process process = builder.start();

			InputStream in = process.getInputStream();
			Thread reader = new Thread(new StreamReader(in));
			reader.start();

			process.waitFor();

			return process.exitValue();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return -1;
	}
}
