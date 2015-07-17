package ninja.command;

import org.apache.commons.io.FileUtils;

public class CleanCommand extends Command {

	@Override
	public void execute() throws Exception {
		info("Deleting 'target' folder...");
		loadCurrentProject();
		FileUtils.deleteDirectory(project.getTargetDir());
		info("Done");

	}

	@Override
	public String getCommand() {
		return "clean";
	}

	@Override
	public String getHelp() {
		return "delete the temporary folder './target/'";
	}

}
