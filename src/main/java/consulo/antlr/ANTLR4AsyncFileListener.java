package consulo.antlr;

import consulo.annotation.component.ExtensionImpl;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.ui.UIAccess;
import consulo.virtualFileSystem.event.AsyncFileListener;
import consulo.virtualFileSystem.event.VFileCreateEvent;
import consulo.virtualFileSystem.event.VFileDeleteEvent;
import consulo.virtualFileSystem.event.VFileEvent;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * @author VISTALL
 * @since 2026-10-15
 */
@ExtensionImpl
public class ANTLR4AsyncFileListener implements AsyncFileListener {
    @Override
    public @Nullable ChangeApplier prepareChange(List<? extends VFileEvent> list) {
        boolean wantRefresh = false;
        for (VFileEvent event : list) {
            if (event instanceof VFileCreateEvent || event instanceof VFileDeleteEvent) {
                wantRefresh = true;
                break;
            }
        }

        if (!wantRefresh) {
            return null;
        }

        return new ChangeApplier() {
            @Override
            public void afterVfsChange() {
                for (Project project : ProjectManager.getInstance().getOpenProjects()) {
                    UIAccess uiAccess = project.getUIAccess();

                    if (uiAccess.isValid()) {
                        uiAccess.give(() -> ToolWindowManager.getInstance(project).revalidateToolWindows());
                    }
                }
            }
        };
    }
}
