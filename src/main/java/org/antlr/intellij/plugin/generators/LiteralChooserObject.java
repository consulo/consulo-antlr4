package org.antlr.intellij.plugin.generators;

import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.awt.SimpleColoredComponent;
import consulo.ui.image.Image;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class LiteralChooserObject {
	private final String text;
	private final Image icon;

	public LiteralChooserObject(final String text) {
		this(text, null);
	}

	public LiteralChooserObject(final String text, @Nullable final Image icon) {
		this.text = text;
		this.icon = icon;
	}

	public void renderTreeNode(SimpleColoredComponent component, JTree tree) {
		String literal = getText();
		SimpleTextAttributes attributes =
			new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, tree.getForeground());
		component.append(literal, attributes);
		component.setIcon(icon);
	}

	public String getText() {
		return text;
	}
}
