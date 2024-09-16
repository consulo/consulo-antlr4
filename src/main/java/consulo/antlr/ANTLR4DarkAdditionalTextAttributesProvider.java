package consulo.antlr;

import consulo.annotation.component.ExtensionImpl;
import consulo.colorScheme.AdditionalTextAttributesProvider;
import consulo.colorScheme.EditorColorsScheme;
import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 20.05.2024
 */
@ExtensionImpl
public class ANTLR4DarkAdditionalTextAttributesProvider implements AdditionalTextAttributesProvider
{
	@Nonnull
	@Override
	public String getColorSchemeName()
	{
		return EditorColorsScheme.DARCULA_SCHEME_NAME;
	}

	@Nonnull
	@Override
	public String getColorSchemeFile()
	{
		return "/consulo/antlr/ANTLRv4Darcula.xml";
	}
}
