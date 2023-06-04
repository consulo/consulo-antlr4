package org.antlr.intellij.plugin.psi;

import consulo.language.impl.psi.ASTWrapperPsiElement;
import consulo.language.ast.ASTNode;
import consulo.language.psi.PsiElement;
import org.antlr.intellij.adaptor.parser.PsiElementFactory;
import org.jetbrains.annotations.NotNull;

public class AtAction extends ASTWrapperPsiElement {
	public AtAction(@NotNull ASTNode node) {
		super(node);
	}

	public static class Factory implements PsiElementFactory {
		public static Factory INSTANCE = new Factory();

		@Override
		public PsiElement createElement(ASTNode node) {
			return new AtAction(node);
		}
	}
}
