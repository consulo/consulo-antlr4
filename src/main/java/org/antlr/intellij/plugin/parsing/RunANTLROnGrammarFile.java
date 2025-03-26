package org.antlr.intellij.plugin.parsing;

import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.Task;
import consulo.execution.ui.console.ConsoleViewContentType;
import consulo.logging.Logger;
import consulo.module.content.ProjectRootManager;
import consulo.project.Project;
import consulo.project.ui.notification.Notification;
import consulo.project.ui.notification.NotificationGroup;
import consulo.project.ui.notification.NotificationType;
import consulo.project.ui.notification.Notifications;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import org.antlr.intellij.plugin.ANTLRv4PluginController;
import org.antlr.intellij.plugin.configdialogs.ConfigANTLRPerGrammar;
import org.antlr.intellij.plugin.preview.PreviewState;
import org.antlr.v4.Tool;
import org.antlr.v4.codegen.CodeGenerator;
import org.antlr.v4.runtime.misc.Utils;
import org.antlr.v4.tool.Grammar;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.stringtemplate.v4.misc.Misc;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.*;

// learned how to do from Grammar-Kit by Gregory Shrago
public class RunANTLROnGrammarFile extends Task.Modal {
    public static final Logger LOG = Logger.getInstance("RunANTLROnGrammarFile");
    public static final String OUTPUT_DIR_NAME = "gen";
    public static final String MISSING = "";
    public static final NotificationGroup groupDisplayId = NotificationGroup.balloonGroup("ANTLR 4 Parser Generation");

    public VirtualFile grammarFile;
    public Project project;
    public boolean forceGeneration;

    public RunANTLROnGrammarFile(VirtualFile grammarFile,
                                 @Nullable final Project project,
                                 @NotNull final String title,
                                 final boolean canBeCancelled,
                                 boolean forceGeneration) {
        super(project, title, canBeCancelled); //, inBackground ? new BackgroundFromStartOption() : null);
        this.grammarFile = grammarFile;
        this.project = project;
        this.forceGeneration = forceGeneration;
    }

    @Override
    public void run(@NotNull ProgressIndicator indicator) {
        indicator.setIndeterminate(true);
        String qualFileName = grammarFile.getPath();
        boolean autogen = ConfigANTLRPerGrammar.getBooleanProp(project, qualFileName, ConfigANTLRPerGrammar.PROP_AUTO_GEN, false);
//		System.out.println("autogen is "+autogen+", force="+forceGeneration);
        if (forceGeneration || (autogen && isGrammarStale())) {
            antlr(grammarFile);
        }
        else {
            ANTLRv4PluginController controller = ANTLRv4PluginController.getInstance(project);
            final PreviewState previewState = controller.getPreviewState(grammarFile);
            // is lexer file? gen .tokens file no matter what as tokens might have changed;
            // a parser that feeds off of that file will need to see the changes.
            if (previewState.g == null && previewState.lg != null) {
                Grammar g = previewState.lg;
                String language = g.getOptionString(ConfigANTLRPerGrammar.PROP_LANGUAGE);
                Tool tool = ParsingUtils.createANTLRToolForLoadingGrammars();
                CodeGenerator gen = CodeGenerator.create(tool, g, language);
                gen.writeVocabFile();
            }
        }
    }

    // TODO: lots of duplication with antlr() function.
    public boolean isGrammarStale() {
        String qualFileName = grammarFile.getPath();
        String sourcePath = ConfigANTLRPerGrammar.getParentDir(grammarFile);
        sourcePath = ConfigANTLRPerGrammar.getProp(project, qualFileName, ConfigANTLRPerGrammar.PROP_LIB_DIR, sourcePath);
        String fullyQualifiedInputFileName = sourcePath + File.separator + grammarFile.getName();

        ANTLRv4PluginController controller = ANTLRv4PluginController.getInstance(project);
        final PreviewState previewState = controller.getPreviewState(grammarFile);
        Grammar g = previewState.getMainGrammar();
        // Grammar should be updated in the preview state before calling this function
        if (g == null) {
            return false;
        }

        String language = g.getOptionString(ConfigANTLRPerGrammar.PROP_LANGUAGE);
        CodeGenerator generator = CodeGenerator.create(null, g, language);
        String recognizerFileName = generator.getRecognizerFileName();

        VirtualFile contentRoot = ConfigANTLRPerGrammar.getContentRoot(project, grammarFile);
        String package_ = ConfigANTLRPerGrammar.getProp(project, qualFileName, ConfigANTLRPerGrammar.PROP_PACKAGE, MISSING);
        String outputDirName = ConfigANTLRPerGrammar.getOutputDirName(project, qualFileName, contentRoot, package_);
        String fullyQualifiedOutputFileName = outputDirName + File.separator + recognizerFileName;

        File inF = new File(fullyQualifiedInputFileName);
        File outF = new File(fullyQualifiedOutputFileName);
        boolean stale = inF.lastModified() > outF.lastModified();
        LOG.info((!stale ? "not" : "") + "stale: " + fullyQualifiedInputFileName + " -> " + fullyQualifiedOutputFileName);
        return stale;
    }

    /**
     * Run ANTLR tool on file according to preferences in intellij for this file.
     * Returns set of generated files or empty set if error.
     */
    public void antlr(VirtualFile vfile) {
        if (vfile == null) {
            return;
        }

        LOG.info("antlr(\"" + vfile.getPath() + "\")");
        List<String> args = getANTLRArgsAsList(project, vfile);

        String sourcePath = ConfigANTLRPerGrammar.getParentDir(vfile);
        String fullyQualifiedInputFileName = sourcePath + File.separator + vfile.getName();
        args.add(fullyQualifiedInputFileName); // add grammar file last

        String lexerGrammarFileName = ParsingUtils.getLexerNameFromParserFileName(fullyQualifiedInputFileName);
        if (new File(lexerGrammarFileName).exists()) {
            // build the lexer too as the grammar surely uses it if it exists
            args.add(lexerGrammarFileName);
        }

        LOG.info("args: " + Utils.join(args.iterator(), " "));

        Tool antlr = new Tool(args.toArray(new String[args.size()]));

        ANTLRv4PluginController controller = ANTLRv4PluginController.getInstance(project);
        String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime());
        controller.printToConsole(c -> c.print(timeStamp + ": antlr4 " + Misc.join(args.iterator(), " ") + "\n", ConsoleViewContentType.SYSTEM_OUTPUT));
        antlr.removeListeners();
        RunANTLRListener listener = new RunANTLRListener(antlr, controller);
        antlr.addListener(listener);

        try {
            antlr.processGrammarsOnCommandLine();
        }
        catch (Throwable e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            String msg = sw.toString();
            Notification notification =
                new Notification(groupDisplayId,
                    "can't generate parser for " + vfile.getName(),
                    e.toString(),
                    NotificationType.INFORMATION);
            Notifications.Bus.notify(notification, project);
            controller.printToConsole(c -> c.print(timeStamp + ": antlr4 " + msg + "\n", ConsoleViewContentType.SYSTEM_OUTPUT));
            listener.hasOutput = true; // show console below
        }

        if (listener.hasOutput) {
            ANTLRv4PluginController.showConsoleWindow(project);
        }
    }

    public static List<String> getANTLRArgsAsList(Project project, VirtualFile vfile) {
        Map<String, String> argMap = getANTLRArgs(project, vfile);
        List<String> args = new ArrayList<String>();
        for (String option : argMap.keySet()) {
            args.add(option);
            String value = argMap.get(option);
            if (value.length() != 0) {
                args.add(value);
            }
        }
        return args;
    }

    public static Map<String, String> getANTLRArgs(Project project, VirtualFile vfile) {
        Map<String, String> args = new HashMap<String, String>();
        String qualFileName = vfile.getPath();
        String sourcePath = ConfigANTLRPerGrammar.getParentDir(vfile);

        String package_ = ConfigANTLRPerGrammar.getProp(project, qualFileName, ConfigANTLRPerGrammar.PROP_PACKAGE, MISSING);
        if (package_ == MISSING) {
            package_ = ProjectRootManager.getInstance(project).getFileIndex().getPackageNameByDirectory(vfile.getParent());
            if (StringUtil.isEmptyOrSpaces(package_)) {
                package_ = MISSING;
            }
        }
        if (package_ != MISSING) {
            args.put("-package", package_);
        }

        String language = ConfigANTLRPerGrammar.getProp(project, qualFileName, ConfigANTLRPerGrammar.PROP_LANGUAGE, MISSING);
        if (language != MISSING) {
            args.put("-Dlanguage=" + language, "");
        }

        // create gen dir at root of project by default, but add in package if any
        VirtualFile contentRoot = ConfigANTLRPerGrammar.getContentRoot(project, vfile);
        String outputDirName = ConfigANTLRPerGrammar.getOutputDirName(project, qualFileName, contentRoot, package_);
        args.put("-o", outputDirName);

        String libDir = ConfigANTLRPerGrammar.getProp(project,
            qualFileName,
            ConfigANTLRPerGrammar.PROP_LIB_DIR,
            sourcePath);
        File f = new File(libDir);
        if (!f.isAbsolute()) { // if not absolute file spec, it's relative to project root
            libDir = contentRoot.getPath() + File.separator + libDir;
        }
        args.put("-lib", libDir);

        String encoding = ConfigANTLRPerGrammar.getProp(project, qualFileName, ConfigANTLRPerGrammar.PROP_ENCODING, MISSING);
        if (encoding != MISSING) {
            args.put("-encoding", encoding);
        }

        if (ConfigANTLRPerGrammar.getBooleanProp(project, qualFileName, ConfigANTLRPerGrammar.PROP_GEN_LISTENER, true)) {
            args.put("-listener", "");
        }
        else {
            args.put("-no-listener", "");
        }
        if (ConfigANTLRPerGrammar.getBooleanProp(project, qualFileName, ConfigANTLRPerGrammar.PROP_GEN_VISITOR, true)) {
            args.put("-visitor", "");
        }
        else {
            args.put("-no-visitor", "");
        }

        return args;
    }

    public String getOutputDirName() {
        VirtualFile contentRoot = ConfigANTLRPerGrammar.getContentRoot(project, grammarFile);
        Map<String, String> argMap = getANTLRArgs(project, grammarFile);
        String package_ = argMap.get("-package");
        if (package_ == null) {
            package_ = MISSING;
        }
        return ConfigANTLRPerGrammar.getOutputDirName(project, grammarFile.getPath(), contentRoot, package_);
    }
}
