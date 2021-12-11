package org.antlr.intellij.plugin;

import consulo.localize.LocalizeValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.intellij.openapi.fileTypes.LanguageFileType;
import consulo.ui.image.Image;

public class ANTLRv4FileType extends LanguageFileType {
	public static final ANTLRv4FileType INSTANCE = new ANTLRv4FileType();

	private ANTLRv4FileType() {
		super(ANTLRv4Language.INSTANCE);
	}

	@NotNull
	@Override
	public String getId() {
		return "ANTLR v4 grammar file";
	}

	@NotNull
	@Override
	public LocalizeValue getDescription() {
		return LocalizeValue.localizeTODO("ANTLR v4 grammar file");
	}

	@NotNull
	@Override
	public String getDefaultExtension() {
		return "g4";
	}

	@Nullable
	@Override
	public Image getIcon() {
		return Icons.FILE;
	}
}
