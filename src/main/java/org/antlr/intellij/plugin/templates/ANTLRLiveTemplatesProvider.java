package org.antlr.intellij.plugin.templates;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.editor.template.DefaultLiveTemplatesProvider;
import org.jetbrains.annotations.Nullable;

@ExtensionImpl
public class ANTLRLiveTemplatesProvider implements DefaultLiveTemplatesProvider
{
	// make sure module shows liveTemplates as source dir or whatever dir holds "lexer"
	public static final String[] TEMPLATES = {"lexer/user"};

	@Override
	public String[] getDefaultLiveTemplateFiles() {
		return TEMPLATES;
	}
}
