package org.antlr.intellij.plugin;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.findUsage.FindUsagesProvider;
import consulo.language.psi.PsiElement;
import consulo.language.psi.util.PsiTreeUtil;
import jakarta.annotation.Nonnull;
import org.antlr.intellij.plugin.psi.LexerRuleRefNode;
import org.antlr.intellij.plugin.psi.LexerRuleSpecNode;
import org.antlr.intellij.plugin.psi.ParserRuleRefNode;
import org.antlr.intellij.plugin.psi.ParserRuleSpecNode;
import org.jetbrains.annotations.NotNull;

@ExtensionImpl
public class ANTLRv4FindUsagesProvider implements FindUsagesProvider {
	@Override
	public boolean canFindUsagesFor(@NotNull PsiElement psiElement) {
//		System.out.println("find usages for "+psiElement+": "+psiElement.getText());
		return psiElement instanceof LexerRuleSpecNode ||
			   psiElement instanceof ParserRuleSpecNode;
//		return psiElement instanceof PsiNamedElement;
	}

	@NotNull
	@Override
	public String getType(@NotNull PsiElement element) {
		if (element instanceof ParserRuleSpecNode) {
			return "parser rule";
		}
		if (element instanceof LexerRuleSpecNode) {
			return "lexer rule";
		}
		return "n/a";
	}

	@NotNull
	@Override
	public String getDescriptiveName(@NotNull PsiElement element) {
		PsiElement rule = PsiTreeUtil.findChildOfAnyType(element,
														 new Class[]{LexerRuleRefNode.class, ParserRuleRefNode.class});
		if ( rule!=null ) return rule.getText();
		return "n/a";
	}

	@NotNull
	@Override
	public String getNodeText(@NotNull PsiElement element, boolean useFullName) {
		return getDescriptiveName(element);
	}

	@Nonnull
	@Override
	public Language getLanguage()
	{
		return ANTLRv4Language.INSTANCE;
	}
}
