/**
 * @author VISTALL
 * @since 04/06/2023
 */
open module org.antlr.intellij.plugin {
	requires consulo.application.api;
	requires consulo.code.editor.api;
	requires consulo.color.scheme.api;
	requires consulo.component.api;
	requires consulo.disposer.api;
	requires consulo.document.api;
	requires consulo.execution.api;
	requires consulo.file.chooser.api;
	requires consulo.file.editor.api;
	requires consulo.language.api;
	requires consulo.language.impl;
	requires consulo.language.editor.api;
	requires consulo.language.editor.refactoring.api;
	requires consulo.localize.api;
	requires consulo.logging.api;
	requires consulo.module.content.api;
	requires consulo.navigation.api;
	requires consulo.project.api;
	requires consulo.project.ui.api;
	requires consulo.project.ui.view.api;
	requires consulo.ui.api;
	requires consulo.ui.ex.api;
	requires consulo.ui.ex.awt.api;
	requires consulo.virtual.file.system.api;
	requires consulo.util.collection;
	requires consulo.util.dataholder;
	requires consulo.util.lang;

	requires antlr4;
	requires antlr.runtime;
	requires org.antlr.antlr4.runtime;
	requires ST4;

	// TODO remove in future
	requires java.desktop;
	requires forms.rt;
}