package ninja.command;

import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ninja.cmd.Main;
import ninja.core.Project;
import ninja.core.ProjectLoader;

public abstract class Command {
	static protected final Logger log = LoggerFactory.getLogger(Command.class);
	public Project project;
	protected String[] args;

	public abstract void execute() throws Exception;

	abstract public String getCommand();

	abstract public String getHelp();

	protected File getProgramPath() {
		String path = Main.class.getProtectionDomain().getCodeSource().getLocation().getPath();
		if (path.endsWith(".jar")) {
			path = path.substring(0, path.lastIndexOf("/"));
		}
		return new File(path);
	}

	protected void info(String string) {
		log.info(string);
	}

	protected void fatal(String message) {
		log.error(message);
		System.exit(1);
	}

	static protected void error(Exception e) {
		log.error(e.getMessage(), e);
	}

	protected void error(String string) {
		log.error(string);
	}

	protected String getRelativePath(File file) {
		return file.getAbsolutePath();
	}

	protected void loadCurrentProject() throws IOException {
		String currentFolder = System.getProperty("user.dir");
		String currentProjectFile = currentFolder + "/project.ninja";
		File f = new File(currentProjectFile);
		if (!f.exists()) {
			fatal(String.format("The project file '%s' cannot be located", currentProjectFile));
		}
		ProjectLoader loader = new ProjectLoader();
		this.project = loader.load(f);
	}

	public void setArgs(String[] args) {
		this.args = args;

	}

	public void displayUsage() {
	}

	protected final String leadingUsage = "            ";

	protected void usage(String string) {
		info("              > " + string);
	}

	protected void usageInfo(String string) {
		info("                " + string);
	}

}
