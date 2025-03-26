package consulo.antlr.toolWindow;

import consulo.annotation.component.ExtensionImpl;
import consulo.antlr4.icon.Antlr4IconGroup;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.project.ui.wm.ToolWindowFactory;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.content.ContentFactory;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.ui.ex.toolWindow.ToolWindowAnchor;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;
import org.antlr.intellij.plugin.ANTLRv4PluginController;
import org.antlr.intellij.plugin.preview.PreviewPanel;

/**
 * @author VISTALL
 * @since 06/12/2021
 */
@ExtensionImpl
public class ANTLR4ToolWindowFactory implements ToolWindowFactory {
    @Nonnull
    @Override
    public String getId() {
        return ANTLRv4PluginController.PREVIEW_WINDOW_ID;
    }

    @RequiredUIAccess
    @Override
    public void createToolWindowContent(@Nonnull Project project, @Nonnull ToolWindow toolWindow) {
        ANTLRv4PluginController pluginController = ANTLRv4PluginController.getInstance(project);

        PreviewPanel previewPanel = pluginController.getPreviewPanel();

        ContentFactory contentFactory = toolWindow.getContentManager().getFactory();
        Content content = contentFactory.createContent(previewPanel, "", false);

        toolWindow.getContentManager().addContent(content);
    }

    @Nonnull
    @Override
    public ToolWindowAnchor getAnchor() {
        return ToolWindowAnchor.BOTTOM;
    }

    @Nonnull
    @Override
    public Image getIcon() {
        return Antlr4IconGroup.toolwindowantlr();
    }

    @Nonnull
    @Override
    public LocalizeValue getDisplayName() {
        return LocalizeValue.localizeTODO("ANTLR4");
    }
}
