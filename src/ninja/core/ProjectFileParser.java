package ninja.core;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;

import org.apache.ivy.core.module.descriptor.Configuration;
import org.apache.ivy.core.module.descriptor.DefaultDependencyDescriptor;
import org.apache.ivy.core.module.descriptor.DefaultModuleDescriptor;
import org.apache.ivy.core.module.descriptor.MDArtifact;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.plugins.parser.AbstractModuleDescriptorParser;
import org.apache.ivy.plugins.parser.ModuleDescriptorParser;
import org.apache.ivy.plugins.parser.ParserSettings;
import org.apache.ivy.plugins.repository.Resource;

public class ProjectFileParser extends AbstractModuleDescriptorParser {

	@Override
	public ModuleDescriptor parseDescriptor(ParserSettings ivySettings, URL descriptorURL, Resource res,
			boolean validate) throws ParseException, IOException {
		try {
			ArrayList<String> confs = new ArrayList<String>();
			confs.add("*");

			long lastModified = (res != null ? res.getLastModified() : 0L);
			Project project = new ProjectLoader().load(new File(descriptorURL.toURI()));

			ModuleRevisionId id = ModuleRevisionId.newInstance("ninja.cmd", project.getName(), project.getVersion());
			DefaultModuleDescriptor descriptor = new DefaultModuleDescriptor(id, "release", null, true) {
				@Override
				public ModuleDescriptorParser getParser() {
					return new ProjectFileParser();
				}
			};
			descriptor.addConfiguration(new Configuration("default"));
			descriptor.addArtifact("default", new MDArtifact(descriptor, id.getName(), "jar", "zip"));
			descriptor.setLastModified(lastModified);
			for (Dependency dp : project.getDependencies()) {
				ModuleRevisionId depId = ModuleRevisionId.newInstance(dp.groupId, dp.artifactId, dp.version);
				DefaultDependencyDescriptor depDescriptor = new DefaultDependencyDescriptor(descriptor, depId, false,
						false, true);
				for (String conf : confs) {
					depDescriptor.addDependencyConfiguration("default", conf);
				}
				descriptor.addDependency(depDescriptor);
			}
			return descriptor;
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public void toIvyFile(InputStream is, Resource res, File destFile, ModuleDescriptor md)
			throws ParseException, IOException {
		((DefaultModuleDescriptor) md).toIvyFile(destFile);
	}

	@Override
	public boolean accept(Resource res) {
		return res.exists() && res.getName().endsWith(".ninja");
	}

}
