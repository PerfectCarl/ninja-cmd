package ninja.command;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import net_alchim31_livereload.LRServer;
import ninja.build.DelayedRestartTrigger;
import ninja.build.RunClassInSeparateJvmMachine;
import ninja.build.WatchAndRestartMachine;
import ninja.core.Lib;

public class RunCommand extends Command {

	/**
	 * Directory containing the build files.
	 * 
	 * For webapps this is usually something like
	 * /User/username/workspace/project/target/classes
	 */
	private final String buildOutputDirectory = "target/classes";

	/**
	 * All directories to watch for changes.
	 */
	private File[] watchDirs;

	/**
	 * Watch all directories on runtime classpath of project. For single maven
	 * projects this usually will be identical to the buildOutputDirectory and
	 * this property is therefore redundant. However, for multi-module projects,
	 * all dependent project build output directories will be included (assuming
	 * you launched maven from parent project directory). A simple way to use
	 * this plugin and watch for changes across an entire multi-module project.
	 */
	private final boolean watchAllClassPathDirs = true;

	/**
	 * Watch all jars on runtime classpath of project. A simple way to monitor
	 * all transitive dependencies and trigger a restart if they change. Since
	 * Java's filesystem monitoring API only watches directories, this property
	 * adds the parent directory of where the jar is stored and also adds an
	 * include rule to always match that jar filename. If other files in the
	 * directory of jars on your classpath (unlikely if you're using maven in a
	 * normal manner), then just be cautious you'll want to add exclude rules to
	 * not include them.
	 */
	private final boolean watchAllClassPathJars = false;

	/**
	 * Includes in Java regex format. Negative regex patterns are difficult to
	 * write, so include rules are processed before exclude rules. Positive
	 * matches shortcut the matching process and that file will be included.
	 */
	private List<String> includes = new ArrayList<String>();

	/**
	 * Exludes in Java regex format. If you want to exclude all freemarker
	 * templates use something like (.*)ftl.html$ for instance.
	 */
	private final List<String> excludes = new ArrayList<String>();

	/**
	 * Context path for SuperDevMode.
	 */
	private String contextPath;

	/**
	 * Mode for SuperDevMode.
	 */
	private final String mode = "dev";

	/**
	 * Port for SuperDevMode
	 */
	private final Integer port = 8080;

	/**
	 * Amount of time to wait for file changes to settle down before triggering
	 * a restart in SuperDevMode.
	 */
	// @Parameter(property = "ninja.settleDownMillis", defaultValue="500",
	// required = false)
	// increased from 500ms to 700ms
	private final Long settleDownMillis = 700L;

	private String NINJA_JETTY_CLASSNAME = "ninja.standalone.NinjaJetty";

	private boolean compile = false;

	private final static String COMMAND_LINE_PARAMETER_NINJA_PORT = "ninja.port";
	private final static String MODE_KEY_NAME = "ninja.mode";

	@Override
	public void execute() throws Exception {
		log.info("Starting Ninja...");
		loadCurrentProject();
		if (project.hasDependency("org.ninjaframework:ninja-servlet")) {
			info("Your project cannot be declared as a servlet and be run in standalone mode.\nTry using 'org.ninjaframework:ninja-standalone:x.y.z' as a dependency.");
		}
		configureParameters();
		if (args.length > 1)
			compile = "compile".equalsIgnoreCase(args[1]);
		try {
			configureShutdown();
			configureLivereload();
			RunClassInSeparateJvmMachine machine = new RunClassInSeparateJvmMachine("NinjaJetty", NINJA_JETTY_CLASSNAME,
					classpathItems, buildJvmArguments(), project.getBaseDir(), compile);

			restartTrigger = new DelayedRestartTrigger(machine);
			machine.setTrigger(restartTrigger);
			restartTrigger.setSettleDownMillis(settleDownMillis);

			restartTrigger.start();

			watcher = new WatchAndRestartMachine(directoriesToRecursivelyWatch, includesAsSet, excludesAsSet,
					restartTrigger);

			// initial startup of machine
			machine.restart();

			watcher.run();

		} catch (IOException e) {
			error(e);
		}
	}

	private List<String> buildJvmArguments() {
		List<String> jvmArguments = new ArrayList<>();

		String systemPropertyDevMode = "-D" + MODE_KEY_NAME + "=" + mode;

		jvmArguments.add(systemPropertyDevMode);

		String portSelection = "-D" + COMMAND_LINE_PARAMETER_NINJA_PORT + "=" + port;

		jvmArguments.add(portSelection);

		if (contextPath != null) {
			String systemPropertyContextPath = "-Dninja.context=" + contextPath;
			jvmArguments.add(systemPropertyContextPath);
		}
		// Disabling the bytecode verification to speed up start up
		jvmArguments.add("-noverify");
		return jvmArguments;
	}

	private void configureParameters() {

		classpathItems = new ArrayList<>();

		alertAndStopExecutionIfDirectoryWithCompiledClassesOfThisProjectDoesNotExist(buildOutputDirectory);

		classpathItems.add(buildOutputDirectory);

		for (Lib lib : project.getLibs()) {
			// Not including commons-logging to prevent sl4j from doing
			// potentially bad things
			// Not very sure about that: commons-logging is not used and this
			// might
			// some leftover code
			if (!lib.is("commons-logging:commons-logging"))
				classpathItems.add(lib.getJarPath());
			else
				info("Ignoring commons-logging");
		}

		excludes.add("(.*)/assets/(.*)");
		excludes.add("(.*)/target/classes/css/(.*)");
		// Add all template extensions
		excludeExtension("ftl.html");
		excludeExtension("jade");
		excludeExtension("html");
		excludeExtension("rythm");
		excludeExtension("mustache");
		excludeExtension("rocker.html");
		includesAsSet = new LinkedHashSet<>(includes);
		excludesAsSet = new LinkedHashSet<>(excludes);

		directoriesToRecursivelyWatch = new LinkedHashSet<>();

		// add only absolute paths so we can catch duplicates

		// add buildOutputDirectory
		directoriesToRecursivelyWatch.add(FileSystems.getDefault().getPath(buildOutputDirectory).toAbsolutePath());

		// add any watch directories
		if (this.watchDirs != null) {
			for (File watchDir : this.watchDirs) {
				directoriesToRecursivelyWatch.add(watchDir.toPath().toAbsolutePath());
			}
		}

		// add stuff from classpath
		File file = project.getSourceDir();
		if (file.isDirectory() && this.watchAllClassPathDirs) {
			// Class files from source files
			directoriesToRecursivelyWatch.add(file.toPath().toAbsolutePath());
		}

		// for (String depName : project.getDependencies()) {
		// file = ivyManager.getClasspathItem(depName);
		for (Lib lib : project.getLibs()) {
			file = new File(lib.getJarPath());

			if (file != null && file.getName().endsWith(".jar") && this.watchAllClassPathJars) {
				File parentDir = file.getParentFile();
				Path parentPath = parentDir.toPath().toAbsolutePath();

				// safe string for rules below (windows path must be escaped for
				// use in regular expressions
				String rulePrefix = parentDir.getAbsolutePath() + File.separator;
				rulePrefix = rulePrefix.replace("\\", "\\\\");

				// if not previously added then add an exclusion rule for
				// everything in it
				if (!directoriesToRecursivelyWatch.contains(parentPath)) {

					excludesAsSet.add(rulePrefix + "(.*)$");

				}

				// we also need to add this jar with an inclusion rule so that
				// we always match it
				includesAsSet.add(rulePrefix + file.getName() + "$");

				directoriesToRecursivelyWatch.add(parentPath);
			}
		}
	}

	private void excludeExtension(String extension) {
		String ext = extension.replace(".", "\\.");
		excludes.add("(.*)/views/(.*)" + ext + "$");
		excludes.add("(.*)/classes/(.*)" + ext + "$");
	}

	public void alertAndStopExecutionIfDirectoryWithCompiledClassesOfThisProjectDoesNotExist(
			String directoryWithCompiledClassesOfThisProject) {

		if (!new File(directoryWithCompiledClassesOfThisProject).exists()) {

			error(String.format("Directory with classes '%s 'does not exist.",
					directoryWithCompiledClassesOfThisProject));
			error("Either use your IDE to build the classes or run 'ninja compile'");

			// BAM!
			System.exit(1);
		}

	}

	LRServer livereloadServer;
	private DelayedRestartTrigger restartTrigger;

	private WatchAndRestartMachine watcher;

	private List<String> classpathItems;

	private Set<Path> directoriesToRecursivelyWatch;

	private Set<String> includesAsSet;

	private Set<String> excludesAsSet;

	private void configureShutdown() {
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			@Override
			public void run() {
				info("Shutting down server...");
				stop();
				info("Server shut down.");
			}
		}) {
		});
	}

	private void configureLivereload() throws Exception {
		Thread liveReloadThread = new Thread() {
			@Override
			public void run() {

				info("Livereload enabled. (See http://livereload.com/)");

				Path docroot = FileSystems.getDefault().getPath(project.getClassesDir().toString());
				livereloadServer = new LRServer(docroot);
				// LRServer.setExclusions(exclusions);
				livereloadServer.run();

			}
		};
		if ("dev".equals(mode))
			liveReloadThread.start();
	}

	private void stop() {
		stopLivereload();
		if (watcher != null)
			watcher.shutdown();
		if (restartTrigger != null)
			restartTrigger.shutdown();

	}

	private void stopLivereload() {
		if (livereloadServer != null)
			livereloadServer.stop();
	}

	@Override
	public String getCommand() {
		return "run";
	}

	@Override
	public String getHelp() {
		return "run the application in web server. Use 'ninja run compile' to automatically compile the source files.";
	}

	@Override
	public void displayUsage() {
		usage("ninja run [compile]");

		usageInfo("Parameters : ");
		usageInfo(
				" - compile : (optional) compile all the source files when a file changes. Useful if you are using an IDE");
	}

}
