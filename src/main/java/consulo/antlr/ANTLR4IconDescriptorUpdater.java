package consulo.antlr;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.icon.IconDescriptor;
import consulo.language.icon.IconDescriptorUpdater;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import org.antlr.intellij.plugin.Icons;
import org.antlr.intellij.plugin.psi.LexerRuleRefNode;
import org.antlr.intellij.plugin.psi.ParserRuleRefNode;

/**
 * @author VISTALL
 * @since 2018-09-12
 */
@ExtensionImpl
public class ANTLR4IconDescriptorUpdater implements IconDescriptorUpdater {
    @RequiredReadAction
    @Override
    public void updateIcon(@Nonnull IconDescriptor iconDescriptor, @Nonnull PsiElement psiElement, int i) {
        if (psiElement instanceof LexerRuleRefNode) {
            iconDescriptor.setMainIcon(Icons.LEXER_RULE);
        }
        else if (psiElement instanceof ParserRuleRefNode) {
            iconDescriptor.setMainIcon(Icons.PARSER_RULE);
        }
    }
}
