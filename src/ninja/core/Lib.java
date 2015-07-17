package ninja.core;

import java.io.Serializable;

public class Lib implements Serializable {
	private static final long serialVersionUID = 1L;

	private String name;
	private String srcPath;
	private String jarPath;
	private String javadocPath;

	private String org;
	private String artifactId;
	private String version;

	private boolean primary = false;

	public String getOrg() {
		return org;
	}

	public String getArtifactId() {
		return artifactId;
	}

	public Lib() {
	}

	public String getSrcPath() {
		return srcPath;
	}

	public void setSrcPath(String srcPath) {
		this.srcPath = srcPath;
	}

	public String getJarPath() {
		return jarPath;
	}

	public void setJarPath(String jarPath) {
		this.jarPath = jarPath;
	}

	public String getJavadocPath() {
		return javadocPath;
	}

	public void setJavadocPath(String javadocPath) {
		this.javadocPath = javadocPath;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Lib(String name) {
		this.name = name;
		String[] libs = name.split(":");
		org = libs[0];
		artifactId = libs[1];
		version = libs[2];
	}

	public String getVersion() {
		return version;
	}

	public String getName() {
		return name;
	}

	public boolean is(String depName) {
		String[] deps = depName.split(":");

		if (deps.length < 2)
			return false;
		return deps[0].equals(org) && deps[1].equals(artifactId);
	}

	@Override
	public String toString() {
		return "Lib [name=" + name + "]";
	}

	public boolean isPrimary() {
		return primary;
	}

	public void setPrimary(boolean primary) {
		this.primary = primary;
	}

}
