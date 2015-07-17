package ninja.core;

import java.io.File;
import java.util.ArrayList;

import ninja.cmd.IvyManager;

public class Project {
	private String name;

	private File baseDir;
	private File sourceDir;
	private File targetDir;
	private File classesDir;
	private File libDir;

	private LibList libs;

	private ArrayList<Dependency> dependencies = new ArrayList<Dependency>();
	private ArrayList<Repository> repositories = new ArrayList<Repository>();

	private IvyManager ivyManager;

	private File projectFile;

	private String ninjaVersion;

	public Project() {

	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public ArrayList<Dependency> getDependencies() {
		return dependencies;
	}

	public void setDependencies(ArrayList<Dependency> dependencies) {
		this.dependencies = dependencies;
	}

	public ArrayList<Repository> getRepositories() {
		return repositories;
	}

	public void setRepositories(ArrayList<Repository> repositories) {
		this.repositories = repositories;
	}

	public File getBaseDir() {
		return baseDir;
	}

	public void setBase(String base) {
		this.baseDir = new File(base);
		this.sourceDir = new File(base, "/src/main/java");
		this.targetDir = new File(base, "/target");
		this.classesDir = new File(base, "/target/classes");
		this.libDir = new File(base, "/target/lib");
	}

	public File getSourceDir() {
		return sourceDir;
	}

	public File getClassesDir() {
		return classesDir;
	}

	public File getTargetDir() {
		return targetDir;
	}

	public LibList getLibs() {
		if (libs == null) {

			try {
				ivyManager = new IvyManager(this);
				libs = ivyManager.populateLibs();

			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				libs = new LibList();
			}

		}
		return libs;
	}

	public File getLibDir() {
		return libDir;
	}

	public boolean hasDependency(String depName) {
		Dependency dep = new Dependency(depName);
		for (Dependency d : getDependencies()) {
			if (d.groupId.equals(dep.groupId) && d.artifactId.equals(dep.artifactId))
				return true;
		}
		return false;
	}

	public void setProjectFile(File file) {
		this.projectFile = file;

	}

	public File getProjectFile() {
		return projectFile;
	}

	public String getVersion() {
		// TODO Auto-generated method stub
		return "1.0";
	}

	public String getNinjaVersion() {
		if (ninjaVersion == null) {
			Dependency standaloneDep = new Dependency("org.ninjaframework:ninja-standalone");
			Dependency servletDep = new Dependency("org.ninjaframework:ninja-servlet");
			for (Dependency d : getDependencies()) {
				if (d.groupId.equals(standaloneDep.groupId) && d.artifactId.equals(standaloneDep.artifactId)) {
					ninjaVersion = d.version;
					break;
				}
				if (d.groupId.equals(servletDep.groupId) && d.artifactId.equals(servletDep.artifactId)) {
					ninjaVersion = d.version;
					break;
				}
			}
		}
		return ninjaVersion;
	}
}
