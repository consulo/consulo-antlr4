package org.antlr.intellij.plugin;

import org.antlr.intellij.plugin.adaptors.ANTLRv4GrammarParser;
import org.antlr.intellij.plugin.adaptors.ANTLRv4LexerAdaptor;
import org.antlr.intellij.plugin.parser.ANTLRv4Lexer;
import org.jetbrains.annotations.NotNull;
import com.intellij.lang.ASTNode;
import com.intellij.lang.ParserDefinition;
import com.intellij.lang.PsiParser;
import com.intellij.lexer.Lexer;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IFileElementType;
import com.intellij.psi.tree.TokenSet;
import consulo.lang.LanguageVersion;

/** The general interface between IDEA and ANTLR. */
public class ANTLRv4ParserDefinition implements ParserDefinition {
	public static final IFileElementType FILE =
		new IFileElementType(ANTLRv4Language.INSTANCE);

	@NotNull
	@Override
	public Lexer createLexer(LanguageVersion languageVersion) {
		ANTLRv4Lexer lexer = new ANTLRv4Lexer(null);
		return new ANTLRv4LexerAdaptor(ANTLRv4Language.INSTANCE, lexer);
	}

	@NotNull
	public PsiParser createParser(LanguageVersion languageVersion) {
		return new ANTLRv4GrammarParser();
	}

	@NotNull
	public TokenSet getWhitespaceTokens(LanguageVersion languageVersion) {
		return ANTLRv4TokenTypes.WHITESPACES;
	}

	@NotNull
	public TokenSet getCommentTokens(LanguageVersion languageVersion) {
		return ANTLRv4TokenTypes.COMMENTS;
	}

	@NotNull
	public TokenSet getStringLiteralElements(LanguageVersion languageVersion) {
		return TokenSet.EMPTY;
	}

	@Override
	public IFileElementType getFileNodeType() {
		return FILE;
	}

	@Override
	public PsiFile createFile(FileViewProvider viewProvider) {
		return new ANTLRv4FileRoot(viewProvider);
	}

	public SpaceRequirements spaceExistanceTypeBetweenTokens(ASTNode left, ASTNode right) {
		return SpaceRequirements.MAY;
	}

	/** Convert from internal parse node (AST they call it) to final PSI node. This
	 *  converts only internal rule nodes apparently, not leaf nodes. Leaves
	 *  are just tokens I guess.
	 */
	@NotNull
	public PsiElement createElement(ASTNode node) {
		return ANTLRv4ASTFactory.createInternalParseTreeNode(node);
	}
}
