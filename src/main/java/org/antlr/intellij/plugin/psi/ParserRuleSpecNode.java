package org.antlr.intellij.plugin.psi;

import consulo.logging.Logger;
import consulo.language.psi.PsiElement;
import consulo.language.ast.IElementType;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.language.ast.ASTNode;
import org.antlr.intellij.adaptor.parser.PsiElementFactory;
import org.antlr.intellij.plugin.ANTLRv4TokenTypes;
import org.antlr.intellij.plugin.parser.ANTLRv4Lexer;
import org.jetbrains.annotations.NotNull;

public class ParserRuleSpecNode extends RuleSpecNode {
	public static final Logger LOG = Logger.getInstance("org.antlr.intellij.plugin.psi.ParserRuleSpecNode");
	public ParserRuleSpecNode(@NotNull ASTNode node) {
		super(node);
	}

	@Override
	public IElementType getRuleRefType() {
		return ANTLRv4TokenTypes.TOKEN_ELEMENT_TYPES.get(ANTLRv4Lexer.RULE_REF);
	}

	@Override
	public GrammarElementRefNode getId() {
		GrammarElementRefNode rr = PsiTreeUtil.getChildOfType(this, ParserRuleRefNode.class);
		if ( rr==null ) {
			LOG.error("can't find ParserRuleRefNode child of "+this.getText(), (Throwable)null);
		}
		return rr;
	}

	public static class Factory implements PsiElementFactory {
		public static Factory INSTANCE = new Factory();

		@Override
		public PsiElement createElement(ASTNode node) {
			return new ParserRuleSpecNode(node);
		}
	}
}

