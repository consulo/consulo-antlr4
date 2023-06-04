package org.antlr.intellij.adaptor.parser;

import consulo.language.ast.ASTNode;
import consulo.language.psi.PsiElement;

/**
 * This interface supports constructing a {@link PsiElement} from an {@link ASTNode}.
 */
public interface PsiElementFactory {
	PsiElement createElement(ASTNode node);
}
