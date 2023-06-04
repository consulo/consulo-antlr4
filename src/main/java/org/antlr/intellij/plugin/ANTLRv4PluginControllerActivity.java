package org.antlr.intellij.plugin;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.dumb.DumbAware;
import consulo.project.Project;
import consulo.project.startup.BackgroundStartupActivity;
import consulo.ui.UIAccess;
import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 26/05/2023
 */
@ExtensionImpl
public class ANTLRv4PluginControllerActivity implements BackgroundStartupActivity, DumbAware
{
	@Override
	public void runActivity(@Nonnull Project project, @Nonnull UIAccess uiAccess)
	{
		ANTLRv4PluginController controller = ANTLRv4PluginController.getInstance(project);

		controller.installListeners();
	}
}
