package org.antlr.intellij.plugin.structview;

import consulo.annotation.access.RequiredReadAction;
import consulo.language.ast.ASTNode;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.localize.LocalizeValue;
import org.antlr.intellij.plugin.ANTLRv4FileRoot;
import org.antlr.intellij.plugin.ANTLRv4TokenTypes;
import org.antlr.intellij.plugin.Icons;
import org.antlr.intellij.plugin.parser.ANTLRv4Parser;
import org.antlr.intellij.plugin.psi.GrammarSpecNode;
import org.antlr.intellij.plugin.psi.MyPsiUtils;
import org.antlr.intellij.plugin.psi.ParserRuleRefNode;
import consulo.navigation.ItemPresentation;
import consulo.language.psi.PsiElement;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

public class ANTLRv4ItemPresentation implements ItemPresentation {
    protected final PsiElement element;

    protected ANTLRv4ItemPresentation(PsiElement element) {
        this.element = element;
    }

    @Override
    public @Nullable String getLocationString() {
        return null;
    }

    @Override
    @RequiredReadAction
    public LocalizeValue getPresentableText() {
        if (element instanceof ANTLRv4FileRoot) {
            GrammarSpecNode gNode = PsiTreeUtil.findChildOfType(element, GrammarSpecNode.class);
            PsiElement id = MyPsiUtils.findChildOfType(gNode, ANTLRv4TokenTypes.RULE_ELEMENT_TYPES.get(ANTLRv4Parser.RULE_id));
            if (id != null) {
                return LocalizeValue.of(id.getText());
            }
            return LocalizeValue.localizeTODO("<n/a>");
        }
        ASTNode node = element.getNode();
        return LocalizeValue.of(node.getText());
    }

    @Nullable
    @Override
    public Image getIcon() {
        if (element instanceof ParserRuleRefNode) {
            return Icons.PARSER_RULE;
        }
        if (element instanceof ANTLRv4FileRoot) {
            return Icons.FILE;
        }
        return Icons.LEXER_RULE;
    }
}
