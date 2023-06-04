package org.antlr.intellij.plugin;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.CodeDocumentationAwareCommenter;
import consulo.language.Language;
import consulo.language.psi.PsiComment;
import consulo.language.ast.IElementType;
import jakarta.annotation.Nonnull;
import org.antlr.intellij.plugin.parser.ANTLRv4Lexer;
import org.jetbrains.annotations.Nullable;

/**
 * Created by jason on 1/7/15.
 */

//DOC_COMMENT
//        :	'/**' .*? ('*/' | EOF)
//        ;
//BLOCK_COMMENT
//        :	'/*' .*? ('*/' | EOF)  -> channel(HIDDEN)
//        ;
//
//LINE_COMMENT
//        :	'//' ~[\r\n]*  -> channel(HIDDEN)
//        ;

@ExtensionImpl
public class ANTLRv4Commenter implements CodeDocumentationAwareCommenter {
    @Nullable
    @Override
    public String getLineCommentPrefix() {
        return "//";
    }

    @Nullable
    @Override
    public String getBlockCommentPrefix() {
        return "/*";
    }

    @Nullable
    @Override
    public String getBlockCommentSuffix() {
        return "*/";
    }

    @Nullable
    @Override
    public String getCommentedBlockCommentPrefix() {
        return null;
    }

    @Nullable
    @Override
    public String getCommentedBlockCommentSuffix() {
        return null;
    }

    @Nullable
    @Override
    public IElementType getLineCommentTokenType() {
        return ANTLRv4TokenTypes.TOKEN_ELEMENT_TYPES.get(ANTLRv4Lexer.LINE_COMMENT);
    }

    @Nullable
    @Override
    public IElementType getBlockCommentTokenType() {
        return ANTLRv4TokenTypes.TOKEN_ELEMENT_TYPES.get(ANTLRv4Lexer.BLOCK_COMMENT);

    }

    @Nullable
    @Override
    public IElementType getDocumentationCommentTokenType() {
        return ANTLRv4TokenTypes.TOKEN_ELEMENT_TYPES.get(ANTLRv4Lexer.DOC_COMMENT);
    }

    @Nullable
    @Override
    public String getDocumentationCommentPrefix() {
        return "/**";
    }

    @Nullable
    @Override
    public String getDocumentationCommentLinePrefix() {
        //TODO: this isnt specified in the grammar. remove?
        return "*";
    }

    @Nullable
    @Override
    public String getDocumentationCommentSuffix() {
        return "*/";
    }

    @Override
    public boolean isDocumentationComment(PsiComment element) {
        return element != null && element.getTokenType() == getDocumentationCommentTokenType();
    }

	@Nonnull
	@Override
	public Language getLanguage()
	{
		return ANTLRv4Language.INSTANCE;
	}
}
