package org.antlr.intellij.plugin.actions;

import consulo.application.dumb.DumbAware;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.virtualFileSystem.VirtualFile;
import org.antlr.intellij.plugin.configdialogs.ConfigANTLRPerGrammar;
import org.antlr.v4.Tool;

public class ConfigureANTLRAction extends AnAction implements DumbAware
{
	public static final Logger LOG = Logger.getInstance("ConfigureANTLRAction");

	@Override
	public void update(AnActionEvent e) {
		MyActionUtils.selectedFileIsGrammar(e);
	}

	@Override
	public void actionPerformed(AnActionEvent e) {
		Project project = e.getData(Project.KEY);
		if (project==null ) {
			LOG.error("actionPerformed no project for "+e);
			return; // whoa!
		}
		VirtualFile grammarFile = MyActionUtils.getGrammarFileFromEvent(e);
		if ( grammarFile==null ) return;
		LOG.info("actionPerformed "+grammarFile);

		ConfigANTLRPerGrammar configDialog = new ConfigANTLRPerGrammar(project, grammarFile.getPath());
		configDialog.setTitle("Configure ANTLR Tool "+ Tool.VERSION+" for "+ grammarFile.getName());

		configDialog.show();

		if ( configDialog.getExitCode()==DialogWrapper.OK_EXIT_CODE ) {
			configDialog.saveValues(project, grammarFile.getPath());
		}
	}
}
