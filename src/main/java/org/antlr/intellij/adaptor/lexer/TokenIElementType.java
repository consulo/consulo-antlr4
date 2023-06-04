package org.antlr.intellij.adaptor.lexer;

import consulo.language.Language;
import consulo.language.ast.IElementType;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**  Represents a token in the language of the plug-in. The "token type" of
 *   leaf nodes in PSI tree.
 */
public class TokenIElementType extends IElementType
{
	private final int type;

	public TokenIElementType(int type, @NotNull @NonNls String debugName, @Nullable Language language) {
		super(debugName, language);
		this.type = type;
	}

	public int getType() {
		return type;
	}
}
