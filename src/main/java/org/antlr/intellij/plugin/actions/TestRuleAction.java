package org.antlr.intellij.plugin.actions;

import consulo.codeEditor.Editor;
import consulo.document.Document;
import consulo.language.editor.LangDataKeys;
import consulo.language.editor.PlatformDataKeys;
import consulo.language.psi.PsiElement;
import consulo.project.Project;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import consulo.logging.Logger;
import consulo.codeEditor.LogicalPosition;
import consulo.document.FileDocumentManager;
import consulo.application.dumb.DumbAware;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.psi.PsiFile;
import consulo.ui.ex.action.AnAction;
import org.antlr.intellij.plugin.ANTLRv4PluginController;
import org.antlr.intellij.plugin.psi.ParserRuleRefNode;

import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;

public class TestRuleAction extends AnAction implements DumbAware {
	public static final Logger LOG = Logger.getInstance("ANTLR TestRuleAction");

	/** Only show if selection is a grammar and in a rule */
	@Override
	public void update(AnActionEvent e) {
		Presentation presentation = e.getPresentation();
		presentation.setText("Test ANTLR Rule"); // default text

		VirtualFile grammarFile = MyActionUtils.getGrammarFileFromEvent(e);
		if ( grammarFile==null ) { // we clicked somewhere outside text or non grammar file
			presentation.setEnabled(false);
			presentation.setVisible(false);
			return;
		}

		ParserRuleRefNode r = null;
		InputEvent inputEvent = e.getInputEvent();
		if ( inputEvent instanceof MouseEvent ) { // this seems to be after update() called 2x and we have selected the action
			r = MyActionUtils.getParserRuleSurroundingRef(e);
		}
		else {
			// If editor component, mouse event not happened yet to update caret so must ask for mouse position
			Editor editor = e.getData(PlatformDataKeys.EDITOR);
			if ( editor!=null ) {
				Point mousePosition = editor.getContentComponent().getMousePosition();
				if ( mousePosition!=null ) {
					LogicalPosition pos = editor.xyToLogicalPosition(mousePosition);
					int offset = editor.logicalPositionToOffset(pos);
					PsiFile file = e.getData(LangDataKeys.PSI_FILE);
					if ( file!=null ) {
						PsiElement el = file.findElementAt(offset);
						if ( el!=null ) {
							r = MyActionUtils.getParserRuleSurroundingRef(el);
						}
					}
				}
			}
			if ( r==null ) {
				r = MyActionUtils.getParserRuleSurroundingRef(e);
			}
		}
		if ( r==null ) {
			presentation.setEnabled(false);
			return;
		}

		presentation.setVisible(true);
		String ruleName = r.getText();
		if ( Character.isLowerCase(ruleName.charAt(0)) ) {
			presentation.setEnabled(true);
			presentation.setText("Test Rule "+ruleName);
		}
		else {
			presentation.setEnabled(false);
		}
	}

	@Override
	public void actionPerformed(final AnActionEvent e) {
		Project project = e.getData(Project.KEY);
		if (project==null ) {
			LOG.error("actionPerformed no project for "+e);
			return; // whoa!
		}
		VirtualFile grammarFile = MyActionUtils.getGrammarFileFromEvent(e);
		if ( grammarFile==null ) return;

		LOG.info("actionPerformed "+grammarFile);

		ANTLRv4PluginController controller = ANTLRv4PluginController.getInstance(project);
		controller.getPreviewWindow().show(null);

		ParserRuleRefNode r = MyActionUtils.getParserRuleSurroundingRef(e);
		if ( r==null ) {
			return; // weird. no rule name.
		}
		String ruleName = r.getText();
		FileDocumentManager docMgr = FileDocumentManager.getInstance();
		Document doc = docMgr.getDocument(grammarFile);
		if ( doc!=null ) {
			docMgr.saveDocument(doc);
		}

		controller.setStartRuleNameEvent(grammarFile, ruleName);
	}

}
