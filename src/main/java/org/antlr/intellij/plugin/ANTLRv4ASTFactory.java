package org.antlr.intellij.plugin;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.ast.ASTNode;
import consulo.language.ast.IElementType;
import consulo.language.impl.ast.ASTLeafFactory;
import consulo.language.impl.ast.LeafElement;
import consulo.language.impl.psi.ASTWrapperPsiElement;
import consulo.language.psi.PsiElement;
import consulo.language.version.LanguageVersion;
import jakarta.annotation.Nonnull;
import org.antlr.intellij.adaptor.parser.PsiElementFactory;
import org.antlr.intellij.plugin.parser.ANTLRv4Lexer;
import org.antlr.intellij.plugin.parser.ANTLRv4Parser;
import org.antlr.intellij.plugin.psi.*;

import java.util.HashMap;
import java.util.Map;

@ExtensionImpl
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
