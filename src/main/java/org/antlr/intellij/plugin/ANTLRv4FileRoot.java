package org.antlr.intellij.plugin;

import org.jetbrains.annotations.NotNull;
import consulo.language.impl.psi.PsiFileBase;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.language.file.FileViewProvider;
import consulo.language.psi.PsiElement;

public class ANTLRv4FileRoot extends PsiFileBase {
    public ANTLRv4FileRoot(@NotNull FileViewProvider viewProvider) {
        super(viewProvider, ANTLRv4Language.INSTANCE);
    }

    @NotNull
    @Override
    public FileType getFileType() {
        return ANTLRv4FileType.INSTANCE;
    }

    @Override
    public String toString() {
        return "ANTLR v4 grammar file";
    }

	@NotNull
	@Override
	public PsiElement[] getChildren() {
		return super.getChildren();
	}
}
