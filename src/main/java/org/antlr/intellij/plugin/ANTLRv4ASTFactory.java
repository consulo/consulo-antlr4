package org.antlr.intellij.plugin;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;

import org.antlr.intellij.adaptor.parser.PsiElementFactory;
import org.antlr.intellij.plugin.parser.ANTLRv4Lexer;
import org.antlr.intellij.plugin.parser.ANTLRv4Parser;
import org.antlr.intellij.plugin.psi.AtAction;
import org.antlr.intellij.plugin.psi.GrammarSpecNode;
import org.antlr.intellij.plugin.psi.LexerRuleRefNode;
import org.antlr.intellij.plugin.psi.LexerRuleSpecNode;
import org.antlr.intellij.plugin.psi.ParserRuleRefNode;
import org.antlr.intellij.plugin.psi.ParserRuleSpecNode;
import org.antlr.intellij.plugin.psi.RulesNode;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.tree.LeafElement;
import com.intellij.psi.tree.IElementType;
import consulo.lang.LanguageVersion;
import consulo.psi.tree.ASTLeafFactory;

public class ANTLRv4ASTFactory implements ASTLeafFactory
{
	private static final Map<IElementType, PsiElementFactory> ruleElementTypeToPsiFactory = new HashMap<IElementType, PsiElementFactory>();
	static {
		// later auto gen with tokens from some spec in grammar?
		ruleElementTypeToPsiFactory.put(ANTLRv4TokenTypes.RULE_ELEMENT_TYPES.get(ANTLRv4Parser.RULE_rules), RulesNode.Factory.INSTANCE);
		ruleElementTypeToPsiFactory.put(ANTLRv4TokenTypes.RULE_ELEMENT_TYPES.get(ANTLRv4Parser.RULE_parserRuleSpec), ParserRuleSpecNode.Factory.INSTANCE);
		ruleElementTypeToPsiFactory.put(ANTLRv4TokenTypes.RULE_ELEMENT_TYPES.get(ANTLRv4Parser.RULE_lexerRule), LexerRuleSpecNode.Factory.INSTANCE);
		ruleElementTypeToPsiFactory.put(ANTLRv4TokenTypes.RULE_ELEMENT_TYPES.get(ANTLRv4Parser.RULE_grammarSpec), GrammarSpecNode.Factory.INSTANCE);
		ruleElementTypeToPsiFactory.put(ANTLRv4TokenTypes.RULE_ELEMENT_TYPES.get(ANTLRv4Parser.RULE_action), AtAction.Factory.INSTANCE);
	}

	/**
	 * Create PSI nodes out of tokens so even parse tree sees them as such.
	 * Does not see whitespace tokens.
	 */
	@Nonnull
	@Override
	public LeafElement createLeaf(@Nonnull IElementType type, @Nonnull LanguageVersion languageVersion, @Nonnull CharSequence text)
	{
		if(type == ANTLRv4TokenTypes.TOKEN_ELEMENT_TYPES.get(ANTLRv4Lexer.RULE_REF))
		{
			return new ParserRuleRefNode(type, text);
		}
		else if(type == ANTLRv4TokenTypes.TOKEN_ELEMENT_TYPES.get(ANTLRv4Lexer.TOKEN_REF))
		{
			return new LexerRuleRefNode(type, text);
		}
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean test(IElementType elementType)
	{
		return elementType == ANTLRv4TokenTypes.TOKEN_ELEMENT_TYPES.get(ANTLRv4Lexer.RULE_REF) || elementType == ANTLRv4TokenTypes.TOKEN_ELEMENT_TYPES.get(ANTLRv4Lexer.TOKEN_REF);
	}

	public static PsiElement createInternalParseTreeNode(ASTNode node) {
		PsiElement t;
		IElementType tokenType = node.getElementType();
		PsiElementFactory factory = ruleElementTypeToPsiFactory.get(tokenType);
		if (factory != null) {
			t = factory.createElement(node);
		}
		else {
			t = new ASTWrapperPsiElement(node);
		}

		return t;
	}
}
