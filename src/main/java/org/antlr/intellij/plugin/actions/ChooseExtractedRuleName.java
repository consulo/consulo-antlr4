package org.antlr.intellij.plugin.actions;

import consulo.project.Project;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awt.JBTextField;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class ChooseExtractedRuleName extends DialogWrapper
{
	JBTextField nameField;
	public String ruleName;

	protected ChooseExtractedRuleName(@Nullable Project project) {
		super(project, true);
		init();
	}

	@Override
	protected void doOKAction() {
		super.doOKAction();
		ruleName = nameField.getText();
	}

	@Override
	protected JComponent createCenterPanel() {
		nameField = new JBTextField("newRule");
		double h = nameField.getSize().getHeight();
		nameField.setPreferredSize(new Dimension(250,(int)h));
		setTitle("Name the extracted rule");
		nameField.selectAll();
		return nameField;
	}

	@Nullable
	@Override
	public JComponent getPreferredFocusedComponent() {
		return nameField;
	}
}
