package ninja.command;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;

import org.apache.commons.io.FileUtils;
import org.eclipse.jdt.core.compiler.batch.BatchCompiler;

import ninja.core.Lib;
import ninja.xtend.XtendCompilerHelper;

public class CompileCommand extends Command {

	private String javaVersion = "1.7";

	@Override
	public void execute() throws Exception {
		info("Compiling source files...");
		loadCurrentProject();
		if (project.hasDependency("org.eclipse.xtend:org.eclipse.xtend.lib")) {
			compileXtend();
			compileJava(new File(project.getTargetDir(), "xtend").getAbsolutePath());
		} else
			compileJava(null);
		copyResourcesFiles();
	}

	private void compileXtend() {
		XtendCompilerHelper compiler = new XtendCompilerHelper();
		if (compiler.compile(project, javaVersion)) {
			info("Compiled successfully Xtend source");
		} else {
			info("Compiled Xtend source with errors");
		}
	}

	private void copyResourcesFiles() throws IOException {
		Collection<File> files = FileUtils.listFiles(project.getSourceDir(), null, true);
		for (File file : files) {
			String name = project.getSourceDir().toPath().relativize(file.toPath()).toString();
			File destFile = new File(project.getClassesDir(), name);
			if (!file.getName().endsWith("java") && !file.getName().endsWith("xtend")) {
				FileUtils.copyFile(file, destFile);
			}
		}

	}

	private void compileJava(String extraSrcPath) {

		// BatchCompiler.compile("-help", new PrintWriter(System.out), new
		// PrintWriter(System.err), null);

		StringBuilder str = new StringBuilder();
		str.append(" -");
		str.append(javaVersion);
		str.append(" ");
		str.append(" -d ");
		str.append(getRelativePath(project.getClassesDir()));
		for (Lib lib : project.getLibs()) {
			if (lib.isPrimary()) {
				str.append(" -cp ");
				str.append(lib.getJarPath());
			}
		}
		// Collection<File> javaFiles =
		// FileUtils.listFiles(project.getSourceDir(), new String[] { "java" },
		// true);
		// int filesCount = javaFiles.size();
		// if (extraSrcPath != null) {
		// }
		//
		// }
		// if (filesCount == 0) {
		// info("No java files to compile");
		// } else {
		// for (File file : javaFiles) {
		// str.append(" ");
		// str.append(getRelativePath(file));
		// }
		str.append(" ");
		str.append(project.getSourceDir().getAbsolutePath());
		if (extraSrcPath != null) {
			str.append(" ");
			str.append(extraSrcPath);
		}
		String cmd = str.toString();
		boolean success = BatchCompiler.compile(cmd, new PrintWriter(System.out), new PrintWriter(System.err), null);
		if (success) {
			info("Compiled successfully java source");
		} else {
			info("Compiled with errors");
		}
		// }
	}

	@Override
	public String getCommand() {
		return "compile";
	}

	public static void compile() {
		try {
			new CompileCommand().execute();
		} catch (Exception e) {
			error(e);
		}
	}

	@Override
	public String getHelp() {
		return "compile the application into './target/classes/'";
	}

}
