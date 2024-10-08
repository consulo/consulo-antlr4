package org.antlr.intellij.plugin.templates;

import consulo.language.editor.template.context.BaseTemplateContextType;
import consulo.language.editor.template.context.TemplateContextType;
import consulo.language.editor.util.PsiUtilBase;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import org.antlr.intellij.plugin.ANTLRv4Language;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class ANTLRLiveTemplateContext extends BaseTemplateContextType {
	public ANTLRLiveTemplateContext(@NotNull @NonNls String id,
									@NotNull LocalizeValue presentableName,
									@Nullable Class<? extends TemplateContextType> baseContextType)
	{
		super(id, presentableName, baseContextType);
	}

	protected abstract boolean isInContext(@NotNull PsiFile file, @NotNull PsiElement element, int offset);

	@Override
	public boolean isInContext(@NotNull PsiFile file, int offset) {
		// offset is where cursor or insertion point is I guess
		if ( !PsiUtilBase.getLanguageAtOffset(file, offset).isKindOf(ANTLRv4Language.INSTANCE) ) {
			return false;
		}
		if ( offset==file.getTextLength() ) { // allow at EOF
			offset--;
		}
		PsiElement element = file.findElementAt(offset);

//		String trace = DebugUtil.currentStackTrace();
//		System.out.println("isInContext: element " + element +", text="+element.getText());
//		System.out.println(trace);

		if ( element==null ) {
			return false;
		}

		return isInContext(file, element, offset);
	}
}
