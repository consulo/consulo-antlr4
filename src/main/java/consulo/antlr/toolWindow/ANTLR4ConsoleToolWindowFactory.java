package consulo.antlr.toolWindow;

import com.intellij.execution.ui.ConsoleView;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import consulo.ui.annotation.RequiredUIAccess;
import org.antlr.intellij.plugin.ANTLRv4PluginController;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 06/12/2021
 */
public class ANTLR4ConsoleToolWindowFactory implements ToolWindowFactory
{
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
}
