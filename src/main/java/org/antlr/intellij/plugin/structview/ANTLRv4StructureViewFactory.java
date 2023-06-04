package org.antlr.intellij.plugin.structview;

import consulo.annotation.component.ExtensionImpl;
import consulo.fileEditor.structureView.StructureViewBuilder;
import consulo.fileEditor.structureView.TreeBasedStructureViewBuilder;
import consulo.language.Language;
import consulo.language.editor.structureView.PsiTreeElementBase;
import consulo.language.editor.structureView.StructureViewModelBase;
import consulo.fileEditor.structureView.StructureViewTreeElement;
import consulo.language.editor.structureView.PsiStructureViewFactory;
import consulo.codeEditor.Editor;
import consulo.fileEditor.structureView.StructureViewModel;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import org.antlr.intellij.plugin.ANTLRv4FileRoot;
import org.antlr.intellij.plugin.ANTLRv4Language;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;

@ExtensionImpl
public class ANTLRv4StructureViewFactory implements PsiStructureViewFactory {
	/** fake a blank Treeview with a warning */
	public static class DummyViewTreeElement extends PsiTreeElementBase<PsiElement>
	{
		public DummyViewTreeElement(PsiElement psiElement) {
			super(psiElement);
		}

		@NotNull
		@Override
		public Collection<StructureViewTreeElement> getChildrenBase() {
			return Collections.emptyList();
		}

		@Nullable
		@Override
		public String getPresentableText() {
			return "Sorry .g not supported (use .g4)";
		}
	}

	@Nullable
    @Override
    public StructureViewBuilder getStructureViewBuilder(final PsiFile psiFile) {
        return new TreeBasedStructureViewBuilder() {
			@NotNull
			@Override
			public StructureViewModel createStructureViewModel(@Nullable Editor editor) {
				VirtualFile grammarFile = psiFile.getVirtualFile();
				if ( grammarFile==null || !grammarFile.getName().endsWith(".g4") ) {
					return new StructureViewModelBase(psiFile, new DummyViewTreeElement(psiFile));
				}
                return new ANTLRv4StructureViewModel((ANTLRv4FileRoot)psiFile);
			}
        };
    }

	@Nonnull
	@Override
	public Language getLanguage()
	{
		return ANTLRv4Language.INSTANCE;
	}
}
