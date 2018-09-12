package consulo.antlr;

import javax.annotation.Nonnull;

import org.antlr.intellij.plugin.Icons;
import org.antlr.intellij.plugin.psi.LexerRuleRefNode;
import org.antlr.intellij.plugin.psi.ParserRuleRefNode;
import com.intellij.psi.PsiElement;
import consulo.annotations.RequiredReadAction;
import consulo.ide.IconDescriptor;
import consulo.ide.IconDescriptorUpdater;

/**
 * @author VISTALL
 * @since 2018-09-12
 */
public class ANTLR4IconDescriptorUpdater implements IconDescriptorUpdater
{
	@RequiredReadAction
	@Override
	public void updateIcon(@Nonnull IconDescriptor iconDescriptor, @Nonnull PsiElement psiElement, int i)
	{
		if(psiElement instanceof LexerRuleRefNode)
		{
			iconDescriptor.setMainIcon(Icons.LEXER_RULE);
		}
		else if(psiElement instanceof ParserRuleRefNode)
		{
			iconDescriptor.setMainIcon(Icons.PARSER_RULE);
		}
	}
}
