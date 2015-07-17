package ninja.cmd;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.ivy.Ivy;
import org.apache.ivy.core.cache.ResolutionCacheManager;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.DefaultDependencyDescriptor;
import org.apache.ivy.core.module.descriptor.DefaultModuleDescriptor;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.ArtifactDownloadReport;
import org.apache.ivy.core.report.ResolveReport;
import org.apache.ivy.core.resolve.IvyNode;
import org.apache.ivy.core.resolve.ResolveEngine;
import org.apache.ivy.core.resolve.ResolveOptions;
import org.apache.ivy.core.retrieve.RetrieveOptions;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.plugins.parser.ModuleDescriptorParserRegistry;
import org.apache.ivy.plugins.report.XmlReportParser;
import org.apache.ivy.plugins.resolver.ChainResolver;
import org.apache.ivy.plugins.resolver.FileSystemResolver;
import org.apache.ivy.plugins.resolver.IBiblioResolver;
import org.apache.ivy.plugins.resolver.URLResolver;
import org.apache.ivy.util.DefaultMessageLogger;
import org.apache.ivy.util.Message;
import org.apache.ivy.util.filter.FilterHelper;

import ninja.core.Dependency;
import ninja.core.Lib;
import ninja.core.LibList;
import ninja.core.Project;
import ninja.core.ProjectFileParser;
import ninja.core.Repository;

public class IvyManager {

	private File userHome = new File(System.getProperty("user.home"));
	private Ivy ivy;
	private String[] configurations = new String[] { "master", "runtime", "javadoc", "sources", "transitive-javadoc",
			"transitive-sources" };
	private Project project;
	private boolean initialized = false;
	private boolean forceResolve = false;
	private HumanReadyLogger ivylogger;

	public IvyManager(Project project) {
		this.project = project;
	}

	private void init() throws Exception {
		if (initialized)
			return;
		this.ivy = configureIvy();
		initialized = true;
	}

	private Ivy configureIvy() throws Exception {

		boolean verbose = System.getProperty("verbose") != null;
		boolean debug = System.getProperty("debug") != null;
		HumanReadyLogger humanReadyLogger = new HumanReadyLogger();

		IvySettings ivySettings = new IvySettings();
		// new SettingsParser(humanReadyLogger).parse(ivySettings, new File(
		// framework, "framework/dependencies.yml"));
		// new SettingsParser(humanReadyLogger).parse(ivySettings, new File(
		// application, "conf/dependencies.yml"));
		// ivySettings.setDefaultResolver("mavenCentral");
		ivySettings.setDefaultUseOrigin(true);
		// PlayConflictManager conflictManager = new PlayConflictManager();
		// ivySettings.addConflictManager("playConflicts", conflictManager);
		// ivySettings.addConflictManager("defaultConflicts",
		// conflictManager.deleguate);
		// ivySettings.setDefaultConflictManager(conflictManager);

		// use the biblio resolver, if you consider resolving
		// POM declared dependencies
		// setupIbiblio(ivySettings);
		setupRepositories(ivySettings);
		Ivy ivy = Ivy.newInstance(ivySettings);

		// Default ivy config see:
		// http://play.lighthouseapp.com/projects/57987-play-framework/tickets/807
		if (userHome != null) {
			File ivyDefaultSettings = new File(userHome, ".ivy2/ivysettings.xml");
			if (ivyDefaultSettings != null && ivyDefaultSettings.exists()) {
				ivy.configure(ivyDefaultSettings);
			}
		}

		if (debug) {
			ivy.getLoggerEngine().pushLogger(new DefaultMessageLogger(Message.MSG_DEBUG));
		} else if (verbose) {
			ivy.getLoggerEngine().pushLogger(new DefaultMessageLogger(Message.MSG_INFO));
		} else {
			ivylogger = humanReadyLogger;
			ivy.getLoggerEngine().setDefaultLogger(ivylogger);
		}

		ivy.pushContext();
		return ivy;
	}

	private void setupRepositories(IvySettings ivySettings) {
		int i = 0;
		ChainResolver chain = new ChainResolver();
		chain.setName("chain");
		chain.setDual(true);

		IBiblioResolver br = new IBiblioResolver();
		br.setM2compatible(true);
		br.setUsepoms(true);
		br.setName("mavenCentral");
		chain.add(br);

		FileSystemResolver localM2 = new FileSystemResolver();
		String userHome = System.getProperty("user.home");
		String pattern = userHome
				+ "/.m2/repository/[organisation]/[module]/[revision]/[module]-[revision](-[classifier]).[ext]";
		localM2.addIvyPattern(pattern);
		localM2.addArtifactPattern(pattern);
		localM2.setName("localMaven");
		localM2.setM2compatible(true);
		localM2.setCheckmodified(true);
		localM2.setChangingPattern(".*-SNAPSHOT");
		chain.add(localM2);

		if (project.getRepositories() != null)
			for (Repository rep : project.getRepositories()) {
				URLResolver resolver = new URLResolver();
				resolver.setName("custom" + i);
				resolver.addIvyPattern(rep.root);
				resolver.addArtifactPattern(rep.root);
				chain.add(resolver);
				i++;
			}
		ivySettings.addResolver(chain);
		ivySettings.setDefaultResolver(chain.getName());
	}

	private void setupIbiblio(IvySettings ivySettings) {
		IBiblioResolver br = new IBiblioResolver();
		br.setM2compatible(true);
		br.setUsepoms(true);
		br.setName("mavenCentral");

		ivySettings.addResolver(br);
		ivySettings.setDefaultResolver(br.getName());
	}

	public void retrieve() throws Exception {
		init();
		File libDir = project.getLibDir();
		if (libDir.exists())
			FileUtils.deleteDirectory(libDir);

		retrieve("ninja.cmd", project.getName(), project.getVersion(), libDir);

	}

	private void retrieve(String groupId, String artifactId, String version, File path)
			throws ParseException, IOException {
		// Step 1: you always need to resolve before you can retrieve
		//
		ResolveOptions ro = new ResolveOptions();
		// this seems to have no impact, if you resolve by module descriptor (in
		// contrast to resolve by ModuleRevisionId)
		ro.setTransitive(true);
		// if set to false, nothing will be downloaded
		ro.setDownload(true);

		DefaultModuleDescriptor md = createDescriptor(groupId, artifactId, version);

		// now resolve
		ResolveReport rr = ivy.resolve(md, ro);
		if (rr.hasError()) {
			throw new RuntimeException(rr.getAllProblemMessages().toString());
		}

		// Step 2: retrieve
		ModuleDescriptor m = rr.getModuleDescriptor();

		ivy.retrieve(m.getModuleRevisionId(), path + "/[artifact](-[classifier])-[revision].[ext]",
				new RetrieveOptions()
						// this is from the envelop module
						.setConfs(new String[] { "default" })
						.setArtifactFilter(FilterHelper.getArtifactTypeFilter(new String[] { "jar", "bundle" })));

		// ivy.retrieve(m.getModuleRevisionId(), new RetrieveOptions()
		// this is from the envelop module
		// .setConfs(new String[] { "default" }));

	}

	private void resolve(String groupId, String artifactId, String version) throws ParseException, IOException {
		// Step 1: you always need to resolve before you can retrieve
		//
		ResolveOptions ro = new ResolveOptions();
		// this seems to have no impact, if you resolve by module descriptor (in
		// contrast to resolve by ModuleRevisionId)
		ro.setTransitive(true);
		// if set to false, nothing will be downloaded
		ro.setDownload(true);

		DefaultModuleDescriptor md = createDescriptor(groupId, artifactId, version);

		// now resolve
		ResolveReport rr = ivy.resolve(md, ro);
		if (rr.hasError()) {
			throw new RuntimeException(rr.getAllProblemMessages().toString());
		}

		// Step 2: retrieve
		ModuleDescriptor m = rr.getModuleDescriptor();

		ResolveOptions resolveOptions = new ResolveOptions();
		resolveOptions.setConfs(new String[] { "default" });
		resolveOptions.setArtifactFilter(FilterHelper.getArtifactTypeFilter(
				new String[] { "jar", "bundle", "source", "javadoc", "transitive-javadoc", "transitive-sources" }));

		ivy.resolve(md, resolveOptions);

	}

	private DefaultModuleDescriptor createDescriptor(String groupId, String artifactId, String version) {
		DefaultModuleDescriptor md = DefaultModuleDescriptor
				.newDefaultInstance(ModuleRevisionId.newInstance(groupId, artifactId + "-envelope", version));
		ModuleRevisionId ri = ModuleRevisionId.newInstance(groupId, artifactId, version);
		DefaultDependencyDescriptor dd = new DefaultDependencyDescriptor(md, ri, false, false, true);
		for (int i = 0; i < configurations.length; i++) {
			dd.addDependencyConfiguration("default", configurations[i]);
		}
		md.addDependency(dd);
		return md;

	}

	private LibList getLibs() throws ParseException, IOException {
		HashMap<String, Lib> result = new HashMap<String, Lib>();
		HashMap<String, Dependency> primaryDeps = new HashMap<String, Dependency>();

		ModuleDescriptorParserRegistry.getInstance().addParser(new ProjectFileParser());
		File convertedIvyModule = new File(project.getBaseDir(), "project.ninja");

		// Resolve
		ResolveEngine resolveEngine = ivy.getResolveEngine();
		ResolveOptions resolveOptions = new ResolveOptions();
		resolveOptions.setConfs(new String[] { "default" });
		resolveOptions.setArtifactFilter(
				FilterHelper.getArtifactTypeFilter(new String[] { "jar", "bundle", "source", "javadoc" }));

		ResolveReport report = resolveEngine.resolve(convertedIvyModule.toURI().toURL(), resolveOptions);

		for (Iterator iter = report.getDependencies().iterator(); iter.hasNext();) {
			IvyNode node = (IvyNode) iter.next();
			if (node.isLoaded() && !node.isCompletelyEvicted()) {
				ArtifactDownloadReport[] adr = report.getArtifactsReports(node.getResolvedId());
				for (ArtifactDownloadReport artifact : adr) {

					addArtifact(result, artifact);

					// lib.jarPath =
					// artifact.getLocalFile().getAbsolutePath());
					// }
					// }
				}
			}
		}
		for (Dependency dep : project.getDependencies()) {
			primaryDeps.put(dep.name, dep);
		}
		Collection<Lib> values = result.values();
		LibList res = new LibList(values.size());
		for (Lib lib : values) {
			lib.setPrimary(primaryDeps.get(lib.getName()) != null);
			res.add(lib);
			// downloadDoc(lib);
		}
		return res;
	}

	private Lib addArtifact(HashMap<String, Lib> result, ArtifactDownloadReport artifact) throws IOException {
		Artifact art = artifact.getArtifact();
		ModuleRevisionId id = art.getModuleRevisionId();
		String name = id.getOrganisation() + ":" + id.getName() + ":" + id.getRevision();

		Lib lib = result.get(name);
		if (lib == null) {
			lib = new Lib(name);
			result.put(name, lib);
		}

		String path = artifact.getLocalFile().getCanonicalPath();
		if ("jar".equals(art.getType()) || "bundle".equals(art.getType()))
			lib.setJarPath(path);
		else {
			if ("javadoc".equals(art.getType()))
				lib.setJavadocPath(path);
			else if ("source".equals(art.getType()))
				lib.setSrcPath(path);
			else
				System.out.println("Not recognized type: " + art.getType());
		}
		return null;
	}

	private List<Lib> getLibs(Dependency dep, boolean needsResolve) throws ParseException, IOException {

		String[] confs = new String[] { "default" };

		// System.out.println(String.format("Retreiving %s -> %s : %s...",
		// dep.groupId, dep.artifactId, dep.version));
		// retrieve(groupId, artifactId, version);
		if (needsResolve)
			resolve(dep.groupId, dep.artifactId, dep.version);

		// DefaultModuleDescriptor md = DefaultModuleDescriptor
		// .newDefaultInstance(
		// // give it some related name (so it can be cached)
		// ModuleRevisionId.newInstance(groupId, artifactId, version));
		// ResolutionCacheManager cacheMgr = ivy.getResolutionCacheManager();
		// XmlReportParser parser = new XmlReportParser();
		// Collection all = new LinkedHashSet();
		// String resolveId = ResolveOptions.getDefaultResolveId(md);
		// File[] reports = cacheMgr
		// .getConfigurationResolveReportsInCache(resolveId);
		//
		// for (File report : reports) {
		// parser.parse(report);
		// all.addAll(Arrays.asList(parser.getArtifactReports()));
		// }
		//
		// StringBuffer buf = new StringBuffer();
		// for (Iterator iter = all.iterator(); iter.hasNext();) {
		// ArtifactDownloadReport artifact = (ArtifactDownloadReport) iter
		// .next();
		// if (artifact.getLocalFile() != null) {
		// buf.append(artifact.getLocalFile().getCanonicalPath());
		// }
		// }
		// DefaultModuleDescriptor md = DefaultModuleDescriptor
		// .newDefaultInstance(ModuleRevisionId.newInstance(groupId,
		// artifactId + "-caller", "working"));
		// DefaultDependencyDescriptor dd = new DefaultDependencyDescriptor(md,
		// ModuleRevisionId.newInstance(groupId, artifactId, version),
		// false, false, true);
		// // for (int i = 0; i < confs.length; i++) {
		// // dd.addDependencyConfiguration("default", confs[i]);
		// // }
		// md.addDependency(dd);
		//
		// // 2. add dependencies for what we are really looking for
		// ModuleRevisionId ri = ModuleRevisionId.newInstance(groupId,
		// artifactId,
		// version);
		// // don't go transitive here, if you want the single artifact
		// DefaultDependencyDescriptor dd = new DefaultDependencyDescriptor(md,
		// ri, false, false, true);

		// // map to master to just get the code jar. See generated ivy module
		// xmls
		// // from maven repo
		// // on how configurations are mapped into ivy. Or check
		// // e.g.
		// //
		// http://lightguard-jp.blogspot.de/2009/04/ivy-configurations-when-pulling-from.html
		// // dd.addDependencyConfiguration("default", "runtime");
		// md.addDependency(dd);

		// 1st create an ivy module (this always(!) has a "default"
		// configuration already)

		DefaultModuleDescriptor md = createDescriptor(dep.groupId, dep.artifactId, dep.version);

		String pathSeparator = System.getProperty("path.separator");
		// StringBuffer buf = new StringBuffer();
		Collection all = new LinkedHashSet();
		ResolutionCacheManager cacheMgr = ivy.getResolutionCacheManager();

		XmlReportParser parser = new XmlReportParser();
		for (int i = 0; i < confs.length; i++) {
			String resolveId = ResolveOptions.getDefaultResolveId(md);
			File report = cacheMgr.getConfigurationResolveReportInCache(resolveId, confs[i]);
			parser.parse(report);

			all.addAll(Arrays.asList(parser.getArtifactReports()));
		}
		// System.out.println(String.format("Found %s -> %s : %s ", dep.groupId,
		// dep.artifactId, dep.version));
		int i = 0;
		HashMap<String, Lib> result = new HashMap<String, Lib>();
		for (Iterator iter = all.iterator(); iter.hasNext();) {
			ArtifactDownloadReport artifact = (ArtifactDownloadReport) iter.next();
			if (artifact.getLocalFile() != null) {
				String path = artifact.getLocalFile().getCanonicalPath();
				// System.out.println(" -> " + path);
				i++;
				Artifact art = artifact.getArtifact();
				ModuleRevisionId id = art.getModuleRevisionId();
				String name = id.getOrganisation() + ":" + id.getName() + ":" + id.getRevision();

				Lib lib = result.get(name);
				if (lib == null) {
					lib = new Lib(name);
					result.put(name, lib);
				}
				if ("jar".equals(art.getType()) || "bundle".equals(art.getType()))
					lib.setJarPath(path);
				else {
					if ("javadoc".equals(art.getType()))
						lib.setJavadocPath(path);
					else if ("source".equals(art.getType()))
						lib.setSrcPath(path);
					else
						System.out.println("Not recognized type: " + art.getType());
				}
				// buf.append(pathSeparator);
			}
		}
		// System.out.println(" count " + i);
		// String result = buf.toString();

		// System.out.println(String.format("Found %s -> %s : %s ", dep.groupId,
		// dep.artifactId, dep.version, result));
		Collection<Lib> values = result.values();
		ArrayList<Lib> res = new ArrayList<Lib>(values.size());
		for (Lib lib : values) {
			res.add(lib);
		}
		return res;
	}

	public List<Lib> populateLibs2() throws Exception {
		init();
		List<Lib> result = new ArrayList<Lib>();
		boolean needsResolve = true;
		File resolveTagFile = new File(project.getTargetDir(), "dependency-resolving.done");
		if (resolveTagFile.exists()) {
			needsResolve = project.getProjectFile().lastModified() > resolveTagFile.lastModified();
		}
		for (Dependency dep : project.getDependencies()) {
			List<Lib> libs;
			try {
				libs = getLibs(dep, needsResolve || forceResolve);
				result.addAll(libs);
			} catch (ParseException | IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		if (resolveTagFile.exists()) {
			resolveTagFile.delete();
		}
		FileUtils.writeStringToFile(resolveTagFile, "");
		return result;
	}

	public LibList populateLibs() throws Exception {
		init();
		LibList result = new LibList();
		boolean needsResolve = true;
		File resolveTagFile = new File(project.getTargetDir(), "dependency-resolving.done");
		if (resolveTagFile.exists()) {
			// ivylogger.setMute(true);
			needsResolve = project.getProjectFile().lastModified() > resolveTagFile.lastModified();
		} else {
			System.out.println(("| Fetching dependencies ..."));
			// ivylogger.setMute(false);
		}
		needsResolve = needsResolve || forceResolve;
		if (!needsResolve && resolveTagFile.exists())
			result = deserializeLibs(resolveTagFile);
		else {
			result = getLibs();
			// Sort the libraries for conveniency
			result.sort(new Comparator<Lib>() {

				@Override
				public int compare(Lib o1, Lib o2) {
					return o1.getArtifactId().compareTo(o2.getArtifactId());
				}

			});
			if (resolveTagFile.exists()) {
				resolveTagFile.delete();
			}
			project.getTargetDir().mkdirs();
			serializeLibs(result, resolveTagFile);
		}
		return result;
	}

	private LibList deserializeLibs(File file) {
		try {
			FileInputStream fileIn = new FileInputStream(file);
			ObjectInputStream in = new ObjectInputStream(fileIn);
			LibList result = (LibList) in.readObject();
			in.close();
			fileIn.close();
			return result;
		} catch (IOException i) {
			i.printStackTrace();

		} catch (ClassNotFoundException c) {
			System.out.println("Employee class not found");
			c.printStackTrace();
		}
		return null;
	}

	private void serializeLibs(LibList libs, File file) {
		try {
			FileOutputStream fileOut = new FileOutputStream(file);
			ObjectOutputStream out = new ObjectOutputStream(fileOut);
			out.writeObject(libs);
			out.close();
			fileOut.close();
		} catch (IOException i) {
			i.printStackTrace();
		}
	}

}
