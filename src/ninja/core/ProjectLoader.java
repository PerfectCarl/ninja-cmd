package ninja.core;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

public class ProjectLoader {

	public Project load(File f) throws IOException {
		Constructor constructor = new Constructor(Project.class);
		TypeDescription projectDesc = new TypeDescription(Project.class);
		projectDesc.putListPropertyType("dependencies", Dependency.class);
		projectDesc.putListPropertyType("repositories", Repository.class);
		constructor.addTypeDescription(projectDesc);

		Yaml yaml = new Yaml(constructor);
		/*
		 * Project result = yaml.loadAs(FileUtils.readFileToString(f),
		 * Project.class);
		 */
		Project result = (Project) yaml.load(FileUtils.readFileToString(f));

		result.setBase(parent(f).getAbsolutePath());
		result.setName(parent(f).getName());
		result.setProjectFile(f);
		// Let's bump some transitive dependencie as primary
		result.getDependencies().add(new Dependency("com.google.inject:guice:4.0"));
		String version = result.getNinjaVersion();
		result.getDependencies().add(new Dependency("org.ninjaframework:ninja-core:" + version));
		return result;
	}

	private File parent(File f) {
		return f.getParentFile();
	}
}
