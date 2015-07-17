package ninja.command;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.FileUtils;

import ninja.core.Lib;

public class PackageCommand extends Command {

	private int count;
	private HashMap<String, String> zipManifest = new HashMap<>();
	private int conflicts;
	private File packageFolder;

	@Override
	public void execute() throws Exception {
		loadCurrentProject();

		info("Packaging application...");
		packageFolder = new File(project.getTargetDir(), "package");
		if (packageFolder.exists())
			FileUtils.deleteDirectory(packageFolder);
		packageFolder.mkdirs();

		if (project.hasDependency("org.ninjaframework:ninja-standalone"))
			packageStandaloneApp();
		else if (project.hasDependency("org.ninjaframework:ninja-servlet"))
			packageWar();
		else
			info("Your application must either declare 'org.ninjaframework:ninja-standalone' or 'org.ninjaframework:ninja-servlet' as dependency");

	}

	private void packageStandaloneApp() throws IOException, InterruptedException {

		count = 0;
		conflicts = 0;
		zipManifest.clear();
		FileUtils.copyDirectory(project.getClassesDir(), packageFolder);
		// FIXME This is a hack for the views
		FileUtils.copyDirectory(new File(project.getSourceDir(), "views"), new File(packageFolder, "views"));

		for (Lib lib : project.getLibs()) {
			// System.out.println("Unzipping: " + lib.jarPath);
			unzip(new File(lib.getJarPath()), packageFolder);
		}

		String jarName = project.getName() + ".jar";
		// FileUtils.deleteDirectory(new File(packageFolder, "META-INF"));
		new File(packageFolder, "META-INF/MANIFEST.MF").delete();
		new File(packageFolder, "META-INF/INDEX.LIST").delete();
		Runtime rt = Runtime.getRuntime();
		Process pr = rt.exec(String.format("jar -cfe ../%s ninja.standalone.NinjaJetty .", jarName), null,
				packageFolder);
		int result = pr.waitFor();
		if (result == 0)
			info(String.format("Jar created '%s'.", jarName));
		else
			info(String.format("Error while creating '%s'.", jarName));
		info(String.format("%d files, %d conflicts", count, conflicts));
	}

	public void unzip(File zipFile, File outputFolder) {

		byte[] buffer = new byte[1024];

		try {

			// create output directory is not exists
			File folder = outputFolder;
			if (!folder.exists()) {
				folder.mkdir();
			}

			// get the zip file content
			ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile));
			// get the zipped file list entry
			ZipEntry ze = zis.getNextEntry();
			String zipFilename = zipFile.getName();
			while (ze != null) {
				if (ze.isDirectory()) {
					ze = zis.getNextEntry();
					continue;
				}
				String fileName = ze.getName();
				File newFile = new File(outputFolder, fileName);

				String parentFolder = newFile.getParentFile().getName();
				// (!"META-INF".equals(parentFolder))
				if (newFile.exists() && !"about.html".equals(fileName) && !"META-INF/DEPENDENCIES".equals(fileName)
						&& !"META-INF/LICENSE".equals(fileName) && !"META-INF/NOTICE".equals(fileName)
						&& !"META-INF/NOTICE.txt".equals(fileName) && !"META-INF/MANIFEST.MF".equals(fileName)
						&& !"META-INF/LICENSE.txt".equals(fileName) && !"META-INF/INDEX.LIST".equals(fileName)) {
					String conflicted = zipManifest.get(newFile.getAbsolutePath());
					if (conflicted == null)
						conflicted = "unknown";
					info(String.format("Conflicts for '%s' with '%s'. File alreay exists '%s", zipFilename, conflicted,
							newFile));
					conflicts++;
				} else
					zipManifest.put(newFile.getAbsolutePath(), zipFile.getName());
				// create all non exists folders
				// else you will hit FileNotFoundException for compressed folder
				new File(newFile.getParent()).mkdirs();

				FileOutputStream fos = new FileOutputStream(newFile);

				int len;
				while ((len = zis.read(buffer)) > 0) {
					fos.write(buffer, 0, len);
				}

				fos.close();
				ze = zis.getNextEntry();
				count++;
			}

			zis.closeEntry();
			zis.close();

		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	private void packageWar() throws IOException, InterruptedException {
		FileUtils.copyDirectory(new File(project.getBaseDir(), "/src/main/webapp"), packageFolder);

		File classesDir = new File(packageFolder, "WEB-INF/classes");
		FileUtils.copyDirectory(project.getClassesDir(), classesDir);
		// FIXME This is a hack for the views
		FileUtils.copyDirectory(new File(project.getSourceDir(), "views"), new File(classesDir, "views"));
		DepCommand.copyLibs(project, new File(packageFolder, "WEB-INF/lib"));
		// String metainf = "Manifest-Version: 1.0\nArchiver-Version:
		// Nina-cmd\n\n";
		// File metainfFile = new File(packageFolder, "META-INF/MANIFEST.MF");
		// metainfFile.getParentFile().mkdirs();
		// FileUtils.writeStringToFile(metainfFile, metainf);
		String jarName = project.getName() + ".war";
		Runtime rt = Runtime.getRuntime();
		Process pr = rt.exec(String.format("jar -cf ../%s .", jarName), null, packageFolder);
		int result = pr.waitFor();
		if (result == 0)
			info(String.format("War created '%s'.", jarName));
		else
			info(String.format("Error while creating '%s'.", jarName));
	}

	@Override
	public String getCommand() {
		return "package";
	}

	@Override
	public String getHelp() {
		return "package your application for distribution as a standalone 'uber' jar file or a war file depending on your project dependency (either 'org.ninjaframework:ninja-standalone' or 'org.ninjaframework:ninja-servlet')";
	}
}
