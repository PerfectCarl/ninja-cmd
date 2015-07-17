package ninja.xtend;

import java.io.File;

import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtend.core.XtendRuntimeModule;
import org.eclipse.xtend.core.XtendStandaloneSetupGenerated;
import org.eclipse.xtend.core.compiler.batch.XtendBatchCompiler;
import org.eclipse.xtend.core.xtend.XtendPackage;
import org.eclipse.xtext.xbase.annotations.XbaseWithAnnotationsStandaloneSetup;

import com.google.inject.Guice;
import com.google.inject.Injector;

import ninja.core.Lib;
import ninja.core.Project;

public class XtendCompilerHelper {

	public boolean compile(Project project, String javaVersion) {

		XtendBatchCompiler compiler = setupXtendCompiler();
		// compiler.setOutputWriter(new PrintWriter(System.out));
		// compiler.setErrorWriter(new PrintWriter(System.out));
		compiler.setJavaSourceVersion(javaVersion);
		compiler.setBasePath(project.getSourceDir().getAbsolutePath());

		compiler.setSourcePath(project.getSourceDir().getAbsolutePath());
		compiler.setOutputPath(new File(project.getTargetDir(), "xtend").getAbsolutePath());
		// compiler.setVerbose(true);
		compiler.setWriteTraceFiles(false);
		StringBuilder classPath = new StringBuilder();
		for (Lib lib : project.getLibs()) {
			// if (lib.isPrimary()) {
			classPath.append(File.pathSeparator);
			classPath.append(lib.getJarPath());
			// }
		}
		compiler.setClassPath(classPath.toString());
		return compiler.compile();
	}

	private XtendBatchCompiler setupXtendCompiler() {
		XbaseWithAnnotationsStandaloneSetup.doSetup();
		EPackage.Registry.INSTANCE.put(XtendPackage.eINSTANCE.getNsURI(), XtendPackage.eINSTANCE);
		Injector injector = Guice.createInjector(new XtendRuntimeModule());
		new XtendStandaloneSetupGenerated().register(injector);
		XtendBatchCompiler compiler = injector.getInstance(XtendCompiler.class);
		return compiler;
	}

	private XtendBatchCompiler setupXtendCompiler2() {
		if (!Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().containsKey("ecore"))
			Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("ecore",
					new org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl());

		if (!Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().containsKey("xmi"))
			Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("xmi",
					new org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl());

		if (!Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().containsKey("xtextbin"))
			Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("xtextbin",
					new org.eclipse.xtext.resource.impl.BinaryGrammarResourceFactoryImpl());

		if (!EPackage.Registry.INSTANCE.containsKey(org.eclipse.xtext.XtextPackage.eNS_URI))
			EPackage.Registry.INSTANCE.put(org.eclipse.xtext.XtextPackage.eNS_URI,
					org.eclipse.xtext.XtextPackage.eINSTANCE);
		Injector injector = Guice.createInjector(new org.eclipse.xtend.core.XtendRuntimeModule());
		XtendBatchCompiler compiler = injector.getInstance(XtendBatchCompiler.class);
		return compiler;
	}
}
