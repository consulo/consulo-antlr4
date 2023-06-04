/**
 * @author VISTALL
 * @since 04/06/2023
 */
open module org.antlr.intellij.plugin {
	requires consulo.ide.api;

	requires antlr.runtime;
	requires antlr4;
	requires antlr4.runtime;
	requires ST4;

	// TODO remove in future
	requires java.desktop;
	requires forms.rt;
}