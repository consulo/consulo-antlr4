package org.antlr.intellij.plugin;

import consulo.annotation.component.ExtensionImpl;
import consulo.virtualFileSystem.fileType.FileTypeFactory;
import consulo.virtualFileSystem.fileType.FileTypeConsumer;
import org.jetbrains.annotations.NotNull;

@ExtensionImpl
public class ANTLRv4FileTypeFactory extends FileTypeFactory{
    @Override
    public void createFileTypes(@NotNull FileTypeConsumer fileTypeConsumer) {
        fileTypeConsumer.consume(ANTLRv4FileType.INSTANCE, "g4");
    }
}
