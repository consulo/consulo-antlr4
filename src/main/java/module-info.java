/**
 * @author VISTALL
 * @since 04/06/2023
 */
open module org.antlr.intellij.plugin {
	requires consulo.ide.api;

	requires antlr4;
	requires antlr.runtime;
	requires org.antlr.antlr4.runtime;
	requires ST4;

	// TODO remove in future
	requires java.desktop;
	requires forms.rt;
}