package consulo.antlr;

import consulo.annotation.component.ExtensionImpl;
import consulo.colorScheme.AttributesFlyweightBuilder;
import consulo.colorScheme.EditorColorSchemeExtender;
import consulo.colorScheme.EditorColorsScheme;
import consulo.ui.color.RGBColor;
import jakarta.annotation.Nonnull;
import org.antlr.intellij.plugin.ANTLRv4SyntaxHighlighter;

/**
 * @author VISTALL
 * @since 20.05.2024
 */
@ExtensionImpl
public class ANTLR4LightAdditionalTextAttributesProvider implements EditorColorSchemeExtender {
    @Override
    public void extend(Builder builder) {
        builder.add(ANTLRv4SyntaxHighlighter.RULENAME, AttributesFlyweightBuilder.create()
            .withForeground(new RGBColor(0x47, 0x47, 0x8E))
            .build());
    }

    @Nonnull
    @Override
    public String getColorSchemeId() {
        return EditorColorsScheme.DEFAULT_SCHEME_NAME;
    }
}
