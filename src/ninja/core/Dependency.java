package ninja.core;

public class Dependency {
	public String name = "";
	public String groupId = "";
	public String artifactId = "";
	public String version = "";

	public Dependency(String name) {
		this.name = name;
		// FIXME slow in loops
		String[] strings = name.split(":");
		groupId = strings[0];
		artifactId = strings[1];
		if (strings.length > 2)
			version = strings[2];
	}
}
