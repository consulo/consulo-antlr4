package org.antlr.intellij.plugin.actions;

import consulo.application.progress.ProgressManager;
import consulo.logging.Logger;
import consulo.project.ui.notification.NotificationType;
import consulo.project.ui.notification.Notifications;
import consulo.ui.ex.action.AnAction;
import consulo.language.editor.PlatformDataKeys;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.application.dumb.DumbAware;
import consulo.project.Project;
import consulo.language.psi.PsiDocumentManager;
import consulo.project.ui.notification.Notification;
import consulo.ui.ex.action.AnActionEvent;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import org.antlr.intellij.plugin.configdialogs.ConfigANTLRPerGrammar;
import org.antlr.intellij.plugin.parsing.RunANTLROnGrammarFile;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

/** Generate parser from ANTLR grammar;
 *  learned how to do from Grammar-Kit by Gregory Shrago.
 */
public class GenerateParserAction extends AnAction implements DumbAware {
	public static final Logger LOG = Logger.getInstance("ANTLR GenerateAction");

	@Override
	public void update(AnActionEvent e) {
		MyActionUtils.selectedFileIsGrammar(e);
	}

	@Override
	public void actionPerformed(final AnActionEvent e) {
		Project project = e.getData(PlatformDataKeys.PROJECT);
		if ( project==null ) {
			LOG.error("actionPerformed no project for "+e);
			return; // whoa!
		}
		VirtualFile grammarFile = MyActionUtils.getGrammarFileFromEvent(e);
		LOG.info("actionPerformed "+(grammarFile==null ? "NONE" : grammarFile));
		if ( grammarFile==null ) return;
		String title = "ANTLR Code Generation";
		boolean canBeCancelled = true;

		// commit changes to PSI and file system
		PsiDocumentManager psiMgr = PsiDocumentManager.getInstance(project);
		FileDocumentManager docMgr = FileDocumentManager.getInstance();
		Document doc = docMgr.getDocument(grammarFile);
		if ( doc==null ) return;

		boolean unsaved = !psiMgr.isCommitted(doc) || docMgr.isDocumentUnsaved(doc);
		if ( unsaved ) {
			// save event triggers ANTLR run if autogen on
			psiMgr.commitDocument(doc);
			docMgr.saveDocument(doc);
		}

		boolean forceGeneration = true; // from action, they really mean it
		RunANTLROnGrammarFile gen =
			new RunANTLROnGrammarFile(grammarFile,
									  project,
									  title,
									  canBeCancelled,
									  forceGeneration);

		boolean autogen =
			ConfigANTLRPerGrammar.getBooleanProp(project,
												 grammarFile.getPath(),
												 ConfigANTLRPerGrammar.PROP_AUTO_GEN,
												 false);
		if ( !unsaved || (unsaved && !autogen) ) {
			// if everything already saved (not stale) then run ANTLR
			// if had to be saved and autogen NOT on, then run ANTLR
			// Otherwise, the save file event will have or will run ANTLR.
			ProgressManager.getInstance().run(gen); //, "Generating", canBeCancelled, e.getData(PlatformDataKeys.PROJECT));

			// refresh from disk to see new files
			Set<File> generatedFiles = new HashSet<File>();
			generatedFiles.add(new File(gen.getOutputDirName()));
			LocalFileSystem.getInstance().refreshIoFiles(generatedFiles, true, true, null);
			// pop up a notification
			Notification notification =
				new Notification(RunANTLROnGrammarFile.groupDisplayId,
								 "parser for " + grammarFile.getName() + " generated",
								 "to " + gen.getOutputDirName(),
								 NotificationType.INFORMATION);
			Notifications.Bus.notify(notification, project);
		}
	}
}
