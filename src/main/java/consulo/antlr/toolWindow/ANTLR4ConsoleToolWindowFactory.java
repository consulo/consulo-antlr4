package consulo.antlr.toolWindow;

import consulo.annotation.component.ExtensionImpl;
import consulo.antlr4.icon.Antlr4IconGroup;
import consulo.execution.ui.console.ConsoleView;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.project.ui.wm.ToolWindowFactory;
import consulo.ui.ex.content.Content;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.content.ContentFactory;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.ui.ex.toolWindow.ToolWindowAnchor;
import consulo.ui.image.Image;
import org.antlr.intellij.plugin.ANTLRv4PluginController;

import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 06/12/2021
 */
@ExtensionImpl
public class ANTLR4ConsoleToolWindowFactory implements ToolWindowFactory
{
	@Nonnull
	@Override
	public String getId()
	{
		return "Tool Output";
	}

	@RequiredUIAccess
	@Override
	public void createToolWindowContent(@Nonnull Project project, @Nonnull ToolWindow toolWindow)
	{
		ANTLRv4PluginController pluginController = ANTLRv4PluginController.getInstance(project);

		ConsoleView console = pluginController.getConsole();

		ContentFactory contentFactory = toolWindow.getContentManager().getFactory();
		Content content = contentFactory.createContent(console.getComponent(), "", false);

		toolWindow.getContentManager().addContent(content);
	}

	@Nonnull
	@Override
	public ToolWindowAnchor getAnchor()
	{
		return ToolWindowAnchor.BOTTOM;
	}

	@Nonnull
	@Override
	public Image getIcon()
	{
		return Antlr4IconGroup.antlr();
	}

	@Nonnull
	@Override
	public LocalizeValue getDisplayName()
	{
		return LocalizeValue.localizeTODO("Tool Output");
	}
}
