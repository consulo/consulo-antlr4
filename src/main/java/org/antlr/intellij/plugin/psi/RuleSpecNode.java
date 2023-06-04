package org.antlr.intellij.plugin.psi;

import consulo.language.ast.ASTNode;
import consulo.language.ast.IElementType;
import consulo.language.impl.psi.ASTWrapperPsiElement;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiNamedElement;
import consulo.language.util.IncorrectOperationException;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

/** Root of lexer, parser rule defs */
public abstract class RuleSpecNode extends ASTWrapperPsiElement implements PsiNamedElement {
	protected String name = null; // an override to input text ID

	public RuleSpecNode(@NotNull final ASTNode node) {
		super(node);
	}

	@Override
	public String getName() {
		if ( name!=null ) return name;
		GrammarElementRefNode id = getId();
		if ( id!=null ) {
			return id.getText();
		}
		return "unknown-name";
	}

	public abstract GrammarElementRefNode getId();

	@Override
	public PsiElement setName(@NonNls @NotNull String name) throws IncorrectOperationException
	{
		/*
		From doc: "Creating a fully correct AST node from scratch is
		          quite difficult. Thus, surprisingly, the easiest way to
		          get the replacement node is to create a dummy file in the
		          custom language so that it would contain the necessary
		          node in its parse tree, build the parse tree and
		          extract the necessary node from it.
		 */
//		System.out.println("rename "+this+" to "+name);
		GrammarElementRefNode id = getId();
		id.replace(MyPsiUtils.createLeafFromText(getProject(),
												 getContext(),
												 name, getRuleRefType()));
		this.name = name;
		return this;
	}

	public abstract IElementType getRuleRefType();

	@Override
	public void subtreeChanged() {
		super.subtreeChanged();
		name = null;
	}

	@Override
	public int getTextOffset() {
		GrammarElementRefNode id = getId();
		if ( id!=null ) return id.getTextOffset();
		return super.getTextOffset();
	}
}
