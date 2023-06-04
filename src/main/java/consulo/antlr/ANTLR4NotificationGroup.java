package consulo.antlr;

import consulo.annotation.component.ExtensionImpl;
import consulo.project.ui.notification.NotificationGroup;
import consulo.project.ui.notification.NotificationGroupContributor;
import jakarta.annotation.Nonnull;
import org.antlr.intellij.plugin.parsing.RunANTLROnGrammarFile;

import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 26/05/2023
 */
@ExtensionImpl
public class ANTLR4NotificationGroup implements NotificationGroupContributor
{
	@Override
	public void contribute(@Nonnull Consumer<NotificationGroup> consumer)
	{
		consumer.accept(RunANTLROnGrammarFile.groupDisplayId);
	}
}
