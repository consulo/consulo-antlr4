package consulo.antlr;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.util.CachedValueProvider;
import consulo.application.util.CachedValuesManager;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.search.FileTypeIndex;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFileManager;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.antlr.intellij.plugin.ANTLRv4FileType;

/**
 * @author VISTALL
 * @since 2026-10-15
 */
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
@Singleton
public class ANTLR4FileTypeDetectService {
    private final Project myProject;
    private final CachedValuesManager myCachedValuesManager;

    @Inject
    public ANTLR4FileTypeDetectService(Project project, CachedValuesManager cachedValuesManager) {
        myProject = project;
        myCachedValuesManager = cachedValuesManager;
    }

    public boolean hasANTLR4File() {
        return myCachedValuesManager.getCachedValue(myProject, () -> {
            boolean hasFile = FileTypeIndex.containsFileOfType(ANTLRv4FileType.INSTANCE, GlobalSearchScope.everythingScope(myProject));
            return CachedValueProvider.Result.create(hasFile, VirtualFileManager.VFS_STRUCTURE_MODIFICATIONS);
        });
    }
}
