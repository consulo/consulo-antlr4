package consulo.antlr.toolWindow;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import consulo.ui.annotation.RequiredUIAccess;
import org.antlr.intellij.plugin.ANTLRv4PluginController;
import org.antlr.intellij.plugin.preview.PreviewPanel;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 06/12/2021
 */
public class ANTLR4PreviewToolWindowFactory implements ToolWindowFactory
{
	@RequiredUIAccess
	@Override
	public void createToolWindowContent(@Nonnull Project project, @Nonnull ToolWindow toolWindow)
	{
		ANTLRv4PluginController pluginController = ANTLRv4PluginController.getInstance(project);

		PreviewPanel previewPanel = pluginController.getPreviewPanel();

		ContentFactory contentFactory = toolWindow.getContentManager().getFactory();
		Content content = contentFactory.createContent(previewPanel, "", false);

		toolWindow.getContentManager().addContent(content);
	}
}
