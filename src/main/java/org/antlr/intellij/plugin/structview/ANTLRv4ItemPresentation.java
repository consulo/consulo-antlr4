package org.antlr.intellij.plugin.structview;

import javax.swing.Icon;

import org.antlr.intellij.plugin.ANTLRv4FileRoot;
import org.antlr.intellij.plugin.ANTLRv4TokenTypes;
import org.antlr.intellij.plugin.Icons;
import org.antlr.intellij.plugin.parser.ANTLRv4Parser;
import org.antlr.intellij.plugin.psi.GrammarSpecNode;
import org.antlr.intellij.plugin.psi.MyPsiUtils;
import org.antlr.intellij.plugin.psi.ParserRuleRefNode;
import org.jetbrains.annotations.Nullable;
import com.intellij.lang.ASTNode;
import com.intellij.navigation.ItemPresentation;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import consulo.awt.TargetAWT;
import consulo.ui.image.Image;

public class ANTLRv4ItemPresentation implements ItemPresentation {
	protected final PsiElement element;

	protected ANTLRv4ItemPresentation(PsiElement element) {
		this.element = element;
	}

	@Nullable
	public String getLocationString() {
		return null;
	}

	@Override
	public String getPresentableText() {
		if (element instanceof ANTLRv4FileRoot) {
			GrammarSpecNode gnode = PsiTreeUtil.findChildOfType(element, GrammarSpecNode.class);
			PsiElement id = MyPsiUtils.findChildOfType(gnode, ANTLRv4TokenTypes.RULE_ELEMENT_TYPES.get(ANTLRv4Parser.RULE_id));
			if ( id!=null ) {
				return id.getText();
			}
			return "<n/a>";
		}
		ASTNode node = element.getNode();
		return node.getText();
	}

	@Nullable
	public Image getIcon() {
		if ( element instanceof ParserRuleRefNode ) {
			return Icons.PARSER_RULE;
		}
		if ( element instanceof ANTLRv4FileRoot ) {
			return Icons.FILE;
		}
		return Icons.LEXER_RULE;
	}
}
