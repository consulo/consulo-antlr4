package org.antlr.intellij.plugin.structview;

import consulo.language.impl.psi.ASTWrapperPsiElement;
import consulo.fileEditor.structureView.StructureViewTreeElement;
import consulo.fileEditor.structureView.tree.SortableTreeElement;
import consulo.fileEditor.structureView.tree.TreeElement;
import consulo.language.psi.PsiElement;
import consulo.navigation.NavigationItem;
import consulo.language.psi.PsiNamedElement;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.navigation.ItemPresentation;
import org.antlr.intellij.plugin.ANTLRv4FileRoot;
import org.antlr.intellij.plugin.psi.LexerRuleRefNode;
import org.antlr.intellij.plugin.psi.LexerRuleSpecNode;
import org.antlr.intellij.plugin.psi.ParserRuleRefNode;
import org.antlr.intellij.plugin.psi.ParserRuleSpecNode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ANTLRv4StructureViewElement implements StructureViewTreeElement, SortableTreeElement {
	private PsiElement element;

	public ANTLRv4StructureViewElement(PsiElement element) {
		this.element = element;
	}

	@Override
	public Object getValue() {
		return element;
	}

	@Override
	public void navigate(boolean requestFocus) {
		if (element instanceof NavigationItem) {
			((NavigationItem) element).navigate(requestFocus);
		}
	}

	@Override
	public boolean canNavigate() {
		return element instanceof NavigationItem &&
			   ((NavigationItem)element).canNavigate();
	}

	@Override
	public boolean canNavigateToSource() {
		return element instanceof NavigationItem &&
			   ((NavigationItem)element).canNavigateToSource();
	}

	@Override
	public String getAlphaSortKey() {
		return element instanceof PsiNamedElement ? ((PsiNamedElement) element).getName() : null;
	}

	@Override
	public ItemPresentation getPresentation() {
		return new ANTLRv4ItemPresentation(element);
	}

	@Override
	public TreeElement[] getChildren() {
		if (element instanceof ANTLRv4FileRoot) {
			// now jump into grammar to look for rules
			Collection<ASTWrapperPsiElement> rules =
				PsiTreeUtil.collectElementsOfType(element, new Class[]{LexerRuleSpecNode.class, ParserRuleSpecNode.class});
//			System.out.println("rules="+rules);
			List<TreeElement> treeElements = new ArrayList<TreeElement>(rules.size());
			for (ASTWrapperPsiElement el : rules) {
				PsiElement rule = PsiTreeUtil.findChildOfAnyType(el, new Class[]{LexerRuleRefNode.class, ParserRuleRefNode.class});
				treeElements.add(new ANTLRv4StructureViewElement(rule));
			}
			return treeElements.toArray(new TreeElement[treeElements.size()]);
		}
		return EMPTY_ARRAY;
	}

	// probably not critical
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		ANTLRv4StructureViewElement that = (ANTLRv4StructureViewElement)o;

		if (element != null ? !element.equals(that.element) : that.element != null) return false;

		return true;
	}

	@Override
	public int hashCode() {
		return element != null ? element.hashCode() : 0;
	}

}
