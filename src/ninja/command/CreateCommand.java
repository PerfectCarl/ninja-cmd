package ninja.command;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

public class CreateCommand extends Command {

	private File dest;
	private String projectName;

	@Override
	public void execute() throws Exception {

		Scanner scanner = new Scanner(System.in);
		try {
			projectName = "";
			info("Creating your new ninja app\n----- ");
			String template = "empty";
			if (args.length >= 2) {
				template = args[1];
			}
			File f = new File(getProgramPath(), "create/" + template);
			if (!f.exists()) {
				info(String.format("There is not template for '%s'. " + "The available templates are :\n"
						+ "   - empty [default]  : empty streamlined app\n"
						+ "   - empty.original\n : empty app created by maven artifact"
						+ "No application has been created ", template));
			} else {
				while (!isValid(projectName)) {
					System.out.print("  . Project name (no space): ");
					projectName = scanner.nextLine();
				}

				dest = new File(projectName);
				if (dest.exists()) {
					fatal(String.format(
							"  > The project '%s' already exists in the current folder. Please pick a different name",
							projectName));
				}

				FileUtils.copyDirectory(f, dest);
				replaceTemplate("pom.xml");
				replaceTemplate("project.ninja");
				replaceTemplate("src/main/java/conf/application.conf");

				info("  > Project created.\n  > Execute 'ninja run' to launch your web application.");
			}
		} finally {
			scanner.close();
		}
	}

	private void replaceTemplate(String filepath) {
		try {
			File inputFile = new File(dest, filepath);
			String content = FileUtils.readFileToString(inputFile);
			content = content.replaceAll("%PROJECT_NAME%", projectName);
			FileUtils.writeStringToFile(inputFile, content);
		} catch (IOException e) {
			error(e);
		}
	}

	private boolean isValid(String name) {
		if (StringUtils.isEmpty(name))
			return false;
		if (name.contains(" ") || name.contains("/") || name.contains(":") || name.contains("\\"))
			return false;
		return true;
	}

	@Override
	public String getCommand() {
		return "create";
	}

	@Override
	public String getHelp() {
		return "create a new application from predefined templates. The templates are 'empty', 'empty.original' and 'blog'";
	}

	@Override
	public void displayUsage() {
		usage("ninja create [template]");
		usageInfo("Existing templates:");
		usageInfo(" - empty      : an empty application with Freemaker templates and no DB access");
		usageInfo(" - blog       : a simple blog website with Freemaker templates, JPA DB access and form validation ");
		usageInfo(" - xtend-jade : a empty Xtend application Jade templates and JPA DB access");
		usageInfo(" - module     : a module for Ninja");

	}

}
