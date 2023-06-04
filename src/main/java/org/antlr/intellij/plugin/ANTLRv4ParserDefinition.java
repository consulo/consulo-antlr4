package org.antlr.intellij.plugin;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.ast.IFileElementType;
import consulo.language.parser.ParserDefinition;
import consulo.language.psi.PsiFile;
import jakarta.annotation.Nonnull;
import org.antlr.intellij.plugin.adaptors.ANTLRv4GrammarParser;
import org.antlr.intellij.plugin.adaptors.ANTLRv4LexerAdaptor;
import org.antlr.intellij.plugin.parser.ANTLRv4Lexer;
import org.jetbrains.annotations.NotNull;
import consulo.language.ast.ASTNode;
import consulo.language.parser.PsiParser;
import consulo.language.lexer.Lexer;
import consulo.language.file.FileViewProvider;
import consulo.language.psi.PsiElement;
import consulo.language.ast.TokenSet;
import consulo.language.version.LanguageVersion;

/** The general interface between IDEA and ANTLR. */
@ExtensionImpl
public class ANTLRv4ParserDefinition implements ParserDefinition
{
	public static final IFileElementType FILE =
		new IFileElementType(ANTLRv4Language.INSTANCE);

	@Nonnull
	@Override
	public Language getLanguage()
	{
		return ANTLRv4Language.INSTANCE;
	}

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
