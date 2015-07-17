package ninja.xtend;

import org.eclipse.xtend.core.compiler.batch.XtendBatchCompiler;
import org.eclipse.xtext.diagnostics.Severity;
import org.eclipse.xtext.validation.Issue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XtendCompiler extends XtendBatchCompiler {
	static protected final Logger log = LoggerFactory.getLogger(XtendBatchCompiler.class);

	@Override
	protected void reportIssues(Iterable<Issue> issues) {
		for (Issue issue : issues) {
			if (issue.getSeverity() == Severity.ERROR) {
				log.error(String.format("Error in %s:%d", issue.getUriToProblem(), issue.getLineNumber()));
				log.error(String.format("  %s", issue.getMessage()));
			} else {
				log.info(String.format("Error in %s:%d", issue.getUriToProblem(), issue.getLineNumber()));
				log.info(String.format("  %s", issue.getMessage()));
			}
		}
	}
}
