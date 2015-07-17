package ninja.command;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import ninja.cmd.IvyManager;
import ninja.core.Lib;
import ninja.core.Project;

public class DepCommand extends Command {

	private IvyManager ivyManager;

	@Override
	public void execute() throws Exception {
		loadCurrentProject();
		ivyManager = new IvyManager(project);
		ivyManager.retrieve();
		String dest = project.getBaseDir().toPath().relativize(project.getLibDir().toPath()).toString();
		info(String.format("The project dependencies have been copied to '%s'", dest));
	}

	@Override
	public String getCommand() {
		return "dep";
	}

	public static void copyLibs(Project project, File destinationFolder) throws IOException {
		destinationFolder.mkdirs();
		for (Lib lib : project.getLibs()) {
			FileUtils.copyFileToDirectory(new File(lib.getJarPath()), destinationFolder);
		}
	}

	public static void copyDocs(Project project, File destinationFolder) throws IOException {
		destinationFolder.mkdirs();
		for (Lib lib : project.getLibs()) {
			File file = null;
			if (StringUtils.isNotEmpty(lib.getSrcPath()))
				file = new File(lib.getSrcPath());
			if (StringUtils.isNotEmpty(lib.getJavadocPath()))
				file = new File(lib.getJavadocPath());
			if (file != null)
				FileUtils.copyFileToDirectory(file, destinationFolder);
		}
	}

	@Override
	public String getHelp() {
		return "copy all the project dependency to './target/lib/'";
	}
}
