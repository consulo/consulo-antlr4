package org.antlr.intellij.plugin.parsing;

import consulo.execution.ui.console.ConsoleViewContentType;
import org.antlr.intellij.plugin.ANTLRv4PluginController;
import org.antlr.v4.Tool;
import org.antlr.v4.tool.ANTLRMessage;
import org.antlr.v4.tool.ANTLRToolListener;
import org.stringtemplate.v4.ST;

import java.util.ArrayList;
import java.util.List;

/**
 * Used to track errors during antlr run on a grammar for generation,
 * not for annotation of grammar.
 */
public class RunANTLRListener implements ANTLRToolListener {
    public final List<String> all = new ArrayList<String>();
    public Tool tool;
    public ANTLRv4PluginController myController;
    public boolean hasOutput = false;

    public RunANTLRListener(Tool tool, ANTLRv4PluginController controller) {
        this.tool = tool;
        myController = controller;
    }

    @Override
    public void info(String msg) {
        if (tool.errMgr.formatWantsSingleLineMessage()) {
            msg = msg.replace('\n', ' ');
        }

        final String finalMsg = msg;
        myController.printToConsole(consoleView -> consoleView.print(finalMsg + "\n", ConsoleViewContentType.NORMAL_OUTPUT));
        hasOutput = true;
    }

    @Override
    public void error(ANTLRMessage msg) {
        track(msg, ConsoleViewContentType.ERROR_OUTPUT);
    }

    @Override
    public void warning(ANTLRMessage msg) {
        track(msg, ConsoleViewContentType.NORMAL_OUTPUT);
    }

    private void track(ANTLRMessage msg, ConsoleViewContentType errType) {
        ST msgST = tool.errMgr.getMessageTemplate(msg);
        String outputMsg = msgST.render();
        if (tool.errMgr.formatWantsSingleLineMessage()) {
            outputMsg = outputMsg.replace('\n', ' ');
        }

        final String finalOutputMsg = outputMsg;
        myController.printToConsole(consoleView -> consoleView.print(finalOutputMsg + "\n", errType));
        hasOutput = true;
    }
}
