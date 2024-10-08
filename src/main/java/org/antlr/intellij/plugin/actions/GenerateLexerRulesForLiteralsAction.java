package org.antlr.intellij.plugin.actions;

import consulo.codeEditor.Editor;
import consulo.document.Document;
import consulo.language.editor.refactoring.action.BaseRefactoringAction;
import consulo.language.psi.PsiElement;
import consulo.logging.Logger;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.language.editor.LangDataKeys;
import consulo.language.editor.PlatformDataKeys;
import consulo.ui.ex.action.Presentation;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.psi.PsiFile;
import org.antlr.intellij.plugin.generators.LiteralChooser;
import org.antlr.intellij.plugin.parser.ANTLRv4Parser;
import org.antlr.intellij.plugin.parsing.ParsingResult;
import org.antlr.intellij.plugin.parsing.ParsingUtils;
import org.antlr.intellij.plugin.psi.MyPsiUtils;
import org.antlr.intellij.plugin.refactor.RefactorUtils;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.misc.Utils;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.pattern.ParseTreeMatch;
import org.antlr.v4.runtime.tree.pattern.ParseTreePattern;
import org.antlr.v4.runtime.tree.xpath.XPath;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;

public class GenerateLexerRulesForLiteralsAction extends AnAction {
	public static final Logger LOG = Logger.getInstance("GenerateLexerRulesForLiterals");

	/** Only show if selection is a literal */
	@Override
	public void update(AnActionEvent e) {
		Presentation presentation = e.getPresentation();
		VirtualFile grammarFile = MyActionUtils.getGrammarFileFromEvent(e);
		if ( grammarFile==null ) {
			presentation.setEnabled(false);
			return;
		}
		PsiFile file = e.getData(LangDataKeys.PSI_FILE);
		Editor editor = e.getData(PlatformDataKeys.EDITOR);
		PsiElement selectedElement = BaseRefactoringAction.getElementAtCaret(editor, file);
		if ( selectedElement==null ) { // we clicked somewhere outside text
			presentation.setEnabled(false);
			return;
		}
	}

	@Override
	public void actionPerformed(AnActionEvent e) {
		LOG.info("actionPerformed GenerateLexerRulesForLiteralsAction");
		final Project project = e.getData(Project.KEY);

		final PsiFile psiFile = e.getData(LangDataKeys.PSI_FILE);
		if (psiFile == null) {
			return;
		}
		String inputText = psiFile.getText();
		ParsingResult results = ParsingUtils.parseANTLRGrammar(inputText);

		final Parser parser = results.parser;
		final ParseTree tree = results.tree;
		Collection<ParseTree> literalNodes = XPath.findAll(tree, "//ruleBlock//STRING_LITERAL", parser);
		LinkedHashMap<String, String> lexerRules = new LinkedHashMap<String, String>();
		for (ParseTree node : literalNodes) {
			String literal = node.getText();
			String ruleText = String.format("%s : %s ;",
											RefactorUtils.getLexerRuleNameFromLiteral(literal), literal);
			lexerRules.put(literal, ruleText);
		}

		// remove those already defined
		String lexerRulesXPath = "//lexerRule";
		String treePattern = "<TOKEN_REF> : <STRING_LITERAL>;";
		ParseTreePattern p = parser.compileParseTreePattern(treePattern, ANTLRv4Parser.RULE_lexerRule);
		List<ParseTreeMatch> matches = p.findAll(tree, lexerRulesXPath);

		for (ParseTreeMatch match : matches) {
			ParseTree lit = match.get("STRING_LITERAL");
			if (lexerRules.containsKey(lit.getText())) { // we have rule for this literal already
				lexerRules.remove(lit.getText());
			}
		}

		final LiteralChooser chooser =
			new LiteralChooser(project, new ArrayList<String>(lexerRules.values()));
		chooser.show();
		List<String> selectedElements = chooser.getSelectedElements();
		// chooser disposed automatically.

		final Editor editor = e.getData(PlatformDataKeys.EDITOR);
		final Document doc = editor.getDocument();
		final CommonTokenStream tokens = (CommonTokenStream) parser.getTokenStream();
//		System.out.println(selectedElements);
		if (selectedElements != null) {
			String text = doc.getText();
			int cursorOffset = editor.getCaretModel().getOffset();
			// make sure it's not in middle of rule; put between.
//					System.out.println("offset "+cursorOffset);
			Collection<ParseTree> allRuleNodes = XPath.findAll(tree, "//ruleSpec", parser);
			for (ParseTree r : allRuleNodes) {
				Interval extent = r.getSourceInterval(); // token indexes
				int start = tokens.get(extent.a).getStartIndex();
				int stop = tokens.get(extent.b).getStopIndex();
//						System.out.println("rule "+r.getChild(0).getText()+": "+start+".."+stop);
				if (cursorOffset < start) {
					// before this rule, so must be between previous and this one
					cursorOffset = start; // put right before this rule
					break;
				}
				else if (cursorOffset >= start && cursorOffset <= stop) {
					// cursor in this rule
					cursorOffset = stop + 2; // put right before this rule (after newline)
					if (cursorOffset >= text.length()) {
						cursorOffset = text.length();
					}
					break;
				}
			}

			String allRules = Utils.join(selectedElements.iterator(), "\n");
			text =
				text.substring(0, cursorOffset) +
					"\n" + allRules + "\n" +
					text.substring(cursorOffset, text.length());
			MyPsiUtils.replacePsiFileFromText(project, psiFile, text);
		}
	}

}
