package org.antlr.intellij.plugin;

import consulo.codeEditor.markup.GutterIconRenderer;
import consulo.language.editor.Pass;
import consulo.language.editor.gutter.GutterIconNavigationHandler;
import consulo.language.editor.gutter.LineMarkerInfo;
import consulo.language.editor.gutter.LineMarkerProvider;
import consulo.language.psi.PsiElement;
import org.antlr.intellij.plugin.psi.RuleSpecNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.event.MouseEvent;

public abstract class ANTLRv4LineMarkerProvider implements LineMarkerProvider {
	@Nullable
	@Override
	public LineMarkerInfo getLineMarkerInfo(@NotNull PsiElement element) {
		final GutterIconNavigationHandler<PsiElement> navHandler =
			new GutterIconNavigationHandler<PsiElement>() {
				@Override
				public void navigate(MouseEvent e, PsiElement elt) {
					System.out.println("don't click on me");
				}
			};
		if ( element instanceof RuleSpecNode ) {
			return new LineMarkerInfo<PsiElement>(element, element.getTextRange(), Icons.FILE,
												  Pass.UPDATE_ALL, null, navHandler,
												  GutterIconRenderer.Alignment.LEFT);
		}
		return null;
	}
}
