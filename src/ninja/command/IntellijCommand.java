package ninja.command;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import ninja.core.Lib;

public class IntellijCommand extends Command {

	private String classpath;
	private final String jpdaPort = "8787";

	@Override
	public void execute() throws Exception {
		loadCurrentProject();
		classpath = generateClasspath();
		generateProject();

		info("Your project is ready to be imported in IntelliJ IDEA via 'XXXX'.");
		info("Launch configurations for debugging are also available.");
		info("Happy coding! ");
	}

	private String generateClasspath() {
		StringBuilder builder = new StringBuilder();

		for (Lib lib : project.getLibs()) {
			String srcPath = lib.getSrcPath();
			String jarPath = lib.getJarPath();
			if (StringUtils.isNotEmpty(srcPath))
				builder.append(
						String.format("<classpathentry kind='lib' path='%s' sourcepath='%s' />\n\t", jarPath, srcPath));
			else if (StringUtils.isNotEmpty(lib.getJavadocPath())) {
				builder.append(String.format(
						"<classpathentry kind='lib' path='%s'> \n\t\t<attributes>\n\t\t\t<attribute name='javadoc_location' value='jar:file:%s!/'/>\n\t\t</attributes>\n\t</classpathentry>\n\t",
						jarPath, lib.getJavadocPath()));
			} else
				builder.append(String.format("<classpathentry kind='lib' path='%s' />\n\t", jarPath));
		}

		return builder.toString();
	}

	private void generateProject() {
		copyTemplate("imlTemplate.iml");
		copyTemplate("iprTemplate.ipr");

	}

	private void copyTemplate(String filename) {
		copyTemplate(filename, "");
	}

	private void copyTemplate(String filename, String destFilename) {
		try {
			File inputFile = new File(getProgramPath(), "/intellij/" + filename);
			File outputFile;
			// FileUtils.copyFile(, outputFile);
			if (StringUtils.isEmpty(destFilename))
				outputFile = new File(project.getBaseDir(), filename);
			else
				outputFile = new File(project.getBaseDir(), destFilename);

			String content = FileUtils.readFileToString(inputFile);
			content = content.replaceAll("%PROJECT_NAME%", project.getName());
			content = content.replaceAll("%PROJECT_DIR%", "BINBGBING");
			content = content.replaceAll("%PROJECT_TARGET_CLASSES%", "BINBGBING");
			content = content.replaceAll("%CLASSPATH%", classpath);
			String relativePath = project.getBaseDir().toPath().relativize(project.getClassesDir().toPath()).toString();
			content = content.replaceAll("%CLASSES_OUTPUT%", relativePath);
			relativePath = project.getBaseDir().toPath().relativize(project.getSourceDir().toPath()).toString();
			content = content.replaceAll("%SRC%", relativePath);
			content = content.replaceAll("%JPDA_PORT%", jpdaPort);
			String extraSrc = "";
			String builder = "";
			String nature = "";
			boolean usesXtend = project.hasDependency("org.eclipse.xtend:org.eclipse.xtend.lib");
			if (usesXtend) {
				extraSrc = "<classpathentry kind='src' path='src/main/xtend-gen'/>";
				nature = "<nature>org.eclipse.xtext.ui.shared.xtextNature</nature>";
				builder = "	<buildCommand>\n" + "		<name>org.eclipse.xtext.ui.shared.xtextBuilder</name>\n"
						+ "		<arguments>\n</arguments>\n" + "</buildCommand>";
			}
			content = content.replaceAll("%EXTRA_SRC%", extraSrc);
			content = content.replaceAll("%BUILDER%", builder);
			content = content.replaceAll("%NATURE%", nature);
			FileUtils.writeStringToFile(outputFile, content);
		} catch (IOException e) {
			error(e);
		}

	}

	@Override
	public String getCommand() {
		return "intellij";
	}

	@Override
	public String getHelp() {
		return "create the Intellij IDEA project files for your application";
	}
}
