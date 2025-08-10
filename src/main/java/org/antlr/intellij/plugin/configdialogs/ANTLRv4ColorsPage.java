package org.antlr.intellij.plugin.configdialogs;

import consulo.annotation.component.ExtensionImpl;
import consulo.colorScheme.setting.AttributesDescriptor;
import consulo.language.editor.colorScheme.setting.ColorSettingsPage;
import consulo.language.editor.highlight.SyntaxHighlighter;
import consulo.localize.LocalizeValue;
import org.antlr.intellij.plugin.ANTLRv4SyntaxHighlighter;
import org.jetbrains.annotations.NotNull;

@ExtensionImpl
public class ANTLRv4ColorsPage implements ColorSettingsPage {
	private static final AttributesDescriptor[] ATTRIBUTES =
		{
			new AttributesDescriptor("Lexer Rule", ANTLRv4SyntaxHighlighter.TOKENNAME),
			new AttributesDescriptor("Parser Rule", ANTLRv4SyntaxHighlighter.RULENAME),
		};

	@NotNull
	@Override
	public SyntaxHighlighter getHighlighter() {
		return new ANTLRv4SyntaxHighlighter();
	}

	@NotNull
	@Override
	public String getDemoText() {
		return
			"grammar Foo;\n" +
			"\n" +
			"compilationUnit : STUFF EOF;\n" +
			"\n" +
			"STUFF : 'stuff' -> pushMode(OTHER_MODE);\n" +
			"WS : [ \\t]+ -> channel(HIDDEN);\n" +
			"NEWLINE : [\\r\\n]+ -> type(WS);\n" +
			"BAD_CHAR : . -> skip;\n" +
			"\n" +
			"mode OTHER_MODE;\n" +
			"\n" +
			"KEYWORD : 'keyword' -> popMode;\n";
	}

	@NotNull
	@Override
	public AttributesDescriptor[] getAttributeDescriptors() {
		return ATTRIBUTES;
	}

	@NotNull
	@Override
	public LocalizeValue getDisplayName() {
		return LocalizeValue.localizeTODO("ANTLR");
	}
}
