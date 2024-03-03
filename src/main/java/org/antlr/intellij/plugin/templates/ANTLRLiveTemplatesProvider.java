package org.antlr.intellij.plugin.templates;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.editor.template.DefaultLiveTemplatesProvider;
import jakarta.annotation.Nonnull;

@ExtensionImpl
public class ANTLRLiveTemplatesProvider implements DefaultLiveTemplatesProvider
{
	public static final String[] TEMPLATES = {"/org/antlr/intellij/plugin/templates/user.xml"};

	@Nonnull
	@Override
	public String[] getDefaultLiveTemplateFiles() {
		return TEMPLATES;
	}
}
