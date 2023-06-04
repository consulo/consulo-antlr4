package org.antlr.intellij.plugin.psi;

import consulo.language.psi.PsiElement;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.logging.Logger;
import consulo.language.ast.IElementType;
import consulo.language.ast.ASTNode;
import org.antlr.intellij.adaptor.parser.PsiElementFactory;
import org.antlr.intellij.plugin.ANTLRv4TokenTypes;
import org.antlr.intellij.plugin.parser.ANTLRv4Lexer;
import org.jetbrains.annotations.NotNull;

public class LexerRuleSpecNode extends RuleSpecNode {
	public static final Logger LOG = Logger.getInstance("org.antlr.intellij.plugin.psi.LexerRuleSpecNode");
	public LexerRuleSpecNode(@NotNull ASTNode node) {
		super(node);
	}

	@Override
	public IElementType getRuleRefType() {
		return ANTLRv4TokenTypes.TOKEN_ELEMENT_TYPES.get(ANTLRv4Lexer.TOKEN_REF);
	}

	@Override
	public GrammarElementRefNode getId() {
		GrammarElementRefNode tr = PsiTreeUtil.getChildOfType(this, LexerRuleRefNode.class);
		if ( tr==null ) {
			LOG.error("can't find LexerRuleRefNode child of "+this.getText(), (Throwable)null);
		}
		return tr;
	}

	public static class Factory implements PsiElementFactory {
		public static Factory INSTANCE = new Factory();

		@Override
		public PsiElement createElement(ASTNode node) {
			return new LexerRuleSpecNode(node);
		}
	}
}
