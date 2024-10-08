package org.antlr.intellij.adaptor.lexer;

import consulo.language.ast.IElementType;
import consulo.language.Language;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Represents a specific ANTLR rule in the language of the plug-in and is the
 *  intellij "token type" of an interior PSI tree node. The IntelliJ equivalent
 *  of ANTLR RuleNode.getRuleIndex() method.
 */
public class RuleIElementType extends IElementType {
	private final int ruleIndex;

	public RuleIElementType(int ruleIndex, @NotNull @NonNls String debugName, @Nullable Language language) {
		super(debugName, language);
		this.ruleIndex = ruleIndex;
	}

	public int getRuleIndex() {
		return ruleIndex;
	}
}
