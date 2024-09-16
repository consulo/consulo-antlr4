package org.antlr.intellij.plugin.actions;

import consulo.codeEditor.*;
import consulo.codeEditor.markup.MarkupModel;
import consulo.codeEditor.markup.RangeHighlighter;
import consulo.language.editor.LangDataKeys;
import consulo.language.editor.PlatformDataKeys;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import consulo.util.lang.function.Condition;
import consulo.virtualFileSystem.VirtualFile;
import org.antlr.intellij.plugin.ANTLRv4FileType;
import org.antlr.intellij.plugin.profiler.ProfilerPanel;
import org.antlr.intellij.plugin.psi.*;
import org.antlr.v4.runtime.atn.DecisionEventInfo;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public class MyActionUtils {
	public static void selectedFileIsGrammar(AnActionEvent e) {
		VirtualFile vfile = getGrammarFileFromEvent(e);
		e.getPresentation().setEnabledAndVisible(vfile != null);
	}

	public static VirtualFile getGrammarFileFromEvent(AnActionEvent e) {
		VirtualFile[] files = e.getData(LangDataKeys.VIRTUAL_FILE_ARRAY);
		if ( files==null || files.length==0 ) return null;
		VirtualFile vfile = files[0];
		if ( vfile!=null && vfile.getFileType() == ANTLRv4FileType.INSTANCE) {
			return vfile;
		}
		return null;
	}

	public static int getMouseOffset(MouseEvent mouseEvent, Editor editor) {
		Point point=new Point(mouseEvent.getPoint());
		LogicalPosition pos=editor.xyToLogicalPosition(point);
		return editor.logicalPositionToOffset(pos);
	}

	public static int getMouseOffset(Editor editor) {
		Point mousePosition = editor.getContentComponent().getMousePosition();
		LogicalPosition pos=editor.xyToLogicalPosition(mousePosition);
		int offset = editor.logicalPositionToOffset(pos);
		return offset;
	}

	public static void moveCursor(Editor editor, int cursorOffset) {
		CaretModel caretModel = editor.getCaretModel();
		caretModel.moveToOffset(cursorOffset);
		ScrollingModel scrollingModel = editor.getScrollingModel();
		scrollingModel.scrollToCaret(ScrollType.MAKE_VISIBLE);
		editor.getContentComponent().requestFocus();
	}

	@NotNull
	public static List<RangeHighlighter> getRangeHighlightersAtOffset(Editor editor, int offset) {
		MarkupModel markupModel = editor.getMarkupModel();
		// collect all highlighters and combine to make a single tool tip
		List<RangeHighlighter> highlightersAtOffset = new ArrayList<RangeHighlighter>();
		for (RangeHighlighter r : markupModel.getAllHighlighters()) {
			int a = r.getStartOffset();
			int b = r.getEndOffset();
//			System.out.printf("#%d: %d..%d %s\n", i, a, b, r.toString());
			if (offset >= a && offset < b) { // cursor is over some kind of highlighting
				highlightersAtOffset.add(r);
			}
		}
		return highlightersAtOffset;
	}

	public static DecisionEventInfo getHighlighterWithDecisionEventType(List<RangeHighlighter> highlighters, Class decisionEventType) {
		for (RangeHighlighter r : highlighters) {
			DecisionEventInfo eventInfo = r.getUserData(ProfilerPanel.DECISION_EVENT_INFO_KEY);
			if (eventInfo != null) {
				if (eventInfo.getClass() == decisionEventType) {
					return eventInfo;
				}
			}
		}
		return null;
	}

	public static ParserRuleRefNode getParserRuleSurroundingRef(AnActionEvent e) {
		PsiElement selectedPsiNode = getSelectedPsiElement(e);
		RuleSpecNode ruleSpecNode = getRuleSurroundingRef(selectedPsiNode, ParserRuleSpecNode.class);
		if ( ruleSpecNode==null ) return null;
		// find the name of rule under ParserRuleSpecNode
		return PsiTreeUtil.findChildOfType(ruleSpecNode, ParserRuleRefNode.class);
	}

	public static ParserRuleRefNode getParserRuleSurroundingRef(PsiElement element) {
		RuleSpecNode ruleSpecNode = getRuleSurroundingRef(element, ParserRuleSpecNode.class);
		if ( ruleSpecNode==null ) return null;
		// find the name of rule under ParserRuleSpecNode
		return PsiTreeUtil.findChildOfType(ruleSpecNode, ParserRuleRefNode.class);
	}

	public static LexerRuleRefNode getLexerRuleSurroundingRef(AnActionEvent e) {
		PsiElement selectedPsiNode = getSelectedPsiElement(e);
		RuleSpecNode ruleSpecNode = getRuleSurroundingRef(selectedPsiNode, LexerRuleSpecNode.class);
		if ( ruleSpecNode==null ) return null;
		// find the name of rule under ParserRuleSpecNode
		return PsiTreeUtil.findChildOfType(ruleSpecNode, LexerRuleRefNode.class);
	}

	public static RuleSpecNode getRuleSurroundingRef(PsiElement selectedPsiNode,
	                                                 final Class<? extends RuleSpecNode> ruleSpecNodeClass)
	{
//		System.out.println("selectedPsiNode: "+selectedPsiNode);
		if ( selectedPsiNode==null ) { // didn't select a node in parse tree
			return null;
		}

		// find root of rule def
		if ( !selectedPsiNode.getClass().equals(ruleSpecNodeClass) ) {
			selectedPsiNode = PsiTreeUtil.findFirstParent(selectedPsiNode, new Condition<PsiElement>() {
				@Override
				public boolean value(PsiElement psiElement) {
					return psiElement.getClass().equals(ruleSpecNodeClass);
				}
			});
			if ( selectedPsiNode==null ) { // not in rule I guess.
				return null;
			}
			// found rule
		}
		return (RuleSpecNode)selectedPsiNode;
	}

	public static PsiElement getSelectedPsiElement(AnActionEvent e) {
		Editor editor = e.getData(PlatformDataKeys.EDITOR);

		if ( editor==null ) { // not in editor
			PsiElement selectedNavElement = e.getData(LangDataKeys.PSI_ELEMENT);
			// in nav bar?
			if ( selectedNavElement==null || !(selectedNavElement instanceof ParserRuleRefNode) ) {
				return null;
			}
			return selectedNavElement;
		}

		// in editor
		PsiFile file = e.getData(LangDataKeys.PSI_FILE);
		if ( file==null ) {
			return null;
		}

		int offset = editor.getCaretModel().getOffset();
		PsiElement el = file.findElementAt(offset);
		return el;
	}

	/** Only show if selection is a lexer or parser rule */
	public static void showOnlyIfSelectionIsRule(AnActionEvent e, String title) {
		Presentation presentation = e.getPresentation();
		VirtualFile grammarFile = getGrammarFileFromEvent(e);
		if ( grammarFile==null ) {
			presentation.setEnabled(false);
			return;
		}

		PsiElement el = getSelectedPsiElement(e);
		if ( el==null ) {
			presentation.setEnabled(false);
			return;
		}

		ParserRuleRefNode parserRule = getParserRuleSurroundingRef(e);
		LexerRuleRefNode lexerRule = getLexerRuleSurroundingRef(e);

		if ( (lexerRule!=null && el instanceof LexerRuleRefNode) ||
			 (parserRule!=null && el instanceof ParserRuleRefNode) )
		{
			String ruleName = el.getText();
			presentation.setText(String.format(title,ruleName));
		}
		else {
			presentation.setEnabled(false);
		}
	}

}
