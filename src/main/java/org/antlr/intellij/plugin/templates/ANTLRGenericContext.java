package org.antlr.intellij.plugin.templates;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.editor.template.context.EverywhereContextType;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import org.jetbrains.annotations.NotNull;

@ExtensionImpl
public class ANTLRGenericContext extends ANTLRLiveTemplateContext {
	public ANTLRGenericContext() {
		super("ANTLR", LocalizeValue.of("ANTLR"), EverywhereContextType.class);
	}

	@Override
	protected boolean isInContext(@NotNull PsiFile file, @NotNull PsiElement element, int offset) {
		return false;
	}
}
