package org.antlr.intellij.plugin;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.editor.highlight.SyntaxHighlighter;
import consulo.language.editor.highlight.SyntaxHighlighterFactory;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import org.jetbrains.annotations.NotNull;

@ExtensionImpl
public class ANTLRv4SyntaxHighlighterFactory extends SyntaxHighlighterFactory
{
	@NotNull
	@Override
	public SyntaxHighlighter getSyntaxHighlighter(Project project, VirtualFile virtualFile)
	{
		return new ANTLRv4SyntaxHighlighter();
	}

	@Nonnull
	@Override
	public Language getLanguage()
	{
		return ANTLRv4Language.INSTANCE;
	}
}
