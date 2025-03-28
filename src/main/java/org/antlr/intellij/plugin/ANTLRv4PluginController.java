package org.antlr.intellij.plugin;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.progress.ProgressManager;
import consulo.application.progress.Task;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorFactory;
import consulo.codeEditor.event.EditorFactoryAdapter;
import consulo.codeEditor.event.EditorFactoryEvent;
import consulo.codeEditor.event.EditorMouseEvent;
import consulo.codeEditor.event.EditorMouseListener;
import consulo.component.messagebus.MessageBusConnection;
import consulo.disposer.Disposable;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.execution.ui.console.ConsoleView;
import consulo.execution.ui.console.TextConsoleBuilder;
import consulo.execution.ui.console.TextConsoleBuilderFactory;
import consulo.fileEditor.FileEditorManager;
import consulo.fileEditor.event.FileEditorManagerAdapter;
import consulo.fileEditor.event.FileEditorManagerEvent;
import consulo.fileEditor.event.FileEditorManagerListener;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.ui.view.MessageView;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.content.ContentFactory;
import consulo.ui.ex.content.ContentManager;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.util.dataholder.Key;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import consulo.virtualFileSystem.event.VirtualFileEvent;
import consulo.virtualFileSystem.event.VirtualFileListener;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.antlr.intellij.adaptor.parser.SyntaxErrorListener;
import org.antlr.intellij.plugin.parsing.ParsingResult;
import org.antlr.intellij.plugin.parsing.ParsingUtils;
import org.antlr.intellij.plugin.parsing.RunANTLROnGrammarFile;
import org.antlr.intellij.plugin.preview.PreviewPanel;
import org.antlr.intellij.plugin.preview.PreviewState;
import org.antlr.intellij.plugin.profiler.ProfilerPanel;
import org.antlr.v4.parse.ANTLRParser;
import org.antlr.v4.tool.Grammar;
import org.antlr.v4.tool.LexerGrammar;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * This object is the controller for the ANTLR plug-in. It receives
 * events and can send them on to its contained components. For example,
 * saving the grammar editor or flipping to a new grammar sends an event
 * to this object, which forwards on update events to the preview tool window.
 * <p>
 * The main components are related to the console tool window forever output and
 * the main panel of the preview tool window.
 * <p>
 * This controller also manages the cache of grammar/editor combinations
 * needed for the preview window. Updates must be made atomically so that
 * the grammars and editors are consistently associated with the same window.
 */
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
@Singleton
public class ANTLRv4PluginController implements Disposable {
    public static final Key<GrammarEditorMouseAdapter> EDITOR_MOUSE_LISTENER_KEY = Key.create("EDITOR_MOUSE_LISTENER_KEY");
    public static final Logger LOG = Logger.getInstance(ANTLRv4PluginController.class);

    private static final Key<ConsoleView> CONSOLE_VIEW_KEY = Key.create("antlr4-console-view");

    public static final String PREVIEW_WINDOW_ID = "ANTLR";

    public boolean projectIsClosed = false;

    public Project myProject;

    public Map<String, PreviewState> grammarToPreviewState =
        Collections.synchronizedMap(new HashMap<String, PreviewState>());

    public PreviewPanel previewPanel;    // same for all grammar editor

    public MyVirtualFileAdapter myVirtualFileAdapter = new MyVirtualFileAdapter();
    public MyFileEditorManagerAdapter myFileEditorManagerAdapter = new MyFileEditorManagerAdapter();

    @Inject
    public ANTLRv4PluginController(Project project) {
        myProject = project;
    }

    @Nonnull
    public static ANTLRv4PluginController getInstance(Project project) {
        return project.getInstance(ANTLRv4PluginController.class);
    }

    @Override
    public void dispose() {
        projectIsClosed = true;

        for (PreviewState it : grammarToPreviewState.values()) {
            previewPanel.inputPanel.releaseEditor(it);
        }

        previewPanel = null;
        myProject = null;
        grammarToPreviewState = null;
    }

    // ------------------------------

    public void installListeners() {
        LOG.info("installListeners " + myProject.getName());
        // Listen for .g4 file saves
        VirtualFileManager.getInstance().addVirtualFileListener(myVirtualFileAdapter, this);

        // Listen for editor window changes
        MessageBusConnection msgBus = myProject.getMessageBus().connect(myProject);
        msgBus.subscribe(
            FileEditorManagerListener.class,
            myFileEditorManagerAdapter
        );

        EditorFactory factory = EditorFactory.getInstance();
        factory.addEditorFactoryListener(
            new EditorFactoryAdapter() {
                @Override
                public void editorCreated(@NotNull EditorFactoryEvent event) {
                    final Editor editor = event.getEditor();
                    final Document doc = editor.getDocument();
                    VirtualFile vfile = FileDocumentManager.getInstance().getFile(doc);
                    if (vfile != null && vfile.getName().endsWith(".g4")) {
                        GrammarEditorMouseAdapter listener = new GrammarEditorMouseAdapter();
                        editor.putUserData(EDITOR_MOUSE_LISTENER_KEY, listener);
                        editor.addEditorMouseListener(listener);
                    }
                }

                @Override
                public void editorReleased(@NotNull EditorFactoryEvent event) {
                    Editor editor = event.getEditor();
                    if (editor.getProject() != null && editor.getProject() != myProject) {
                        return;
                    }
                    GrammarEditorMouseAdapter listener = editor.getUserData(EDITOR_MOUSE_LISTENER_KEY);
                    if (listener != null) {
                        editor.removeEditorMouseListener(listener);
                        editor.putUserData(EDITOR_MOUSE_LISTENER_KEY, null);
                    }
                }
            }, this
        );
    }

    /**
     * The test ANTLR rule action triggers this event. This can occur
     * only occur when the current editor the showing a grammar, because
     * that is the only time that the action is enabled. We will see
     * a file changed event when the project loads the first grammar file.
     */
    public void setStartRuleNameEvent(VirtualFile grammarFile, String startRuleName) {
        LOG.info("setStartRuleNameEvent " + startRuleName + " " + myProject.getName());
        PreviewState previewState = getPreviewState(grammarFile);
        previewState.startRuleName = startRuleName;
        if (previewPanel != null) {
            previewPanel.getInputPanel().setStartRuleName(grammarFile, startRuleName); // notify the view
            previewPanel.updateParseTreeFromDoc(grammarFile);
        }
        else {
            LOG.error("setStartRuleNameEvent called before preview panel created");
        }
    }

    public void grammarFileSavedEvent(VirtualFile grammarFile) {
        LOG.info("grammarFileSavedEvent " + grammarFile.getPath() + " " + myProject.getName());
        updateGrammarObjectsFromFile(grammarFile); // force reload
        if (previewPanel != null) {
            previewPanel.grammarFileSaved(grammarFile);
        }
        else {
            LOG.error("grammarFileSavedEvent called before preview panel created");
        }
        runANTLRTool(grammarFile);
    }

    public void currentEditorFileChangedEvent(VirtualFile oldFile, VirtualFile newFile) {
        LOG.info("currentEditorFileChangedEvent " + (oldFile != null ? oldFile.getPath() : "none") +
            " -> " + (newFile != null ? newFile.getPath() : "none") + " " + myProject.getName());
        if (newFile == null) { // all files must be closed I guess
            return;
        }
        if (newFile.getName().endsWith(".g")) {
            LOG.info("currentEditorFileChangedEvent ANTLR 4 cannot handle .g files, only .g4");
            getPreviewWindow().hide(null);
            return;
        }
        if (!newFile.getName().endsWith(".g4")) {
            getPreviewWindow().hide(null);
            return;
        }
        PreviewState previewState = getPreviewState(newFile);
        if (previewState.g == null && previewState.lg == null) { // only load grammars if none is there
            updateGrammarObjectsFromFile(newFile);
        }
        if (previewPanel != null) {
            previewPanel.grammarFileChanged(oldFile, newFile);
        }
    }

    public void mouseEnteredGrammarEditorEvent(VirtualFile vfile, EditorMouseEvent e) {
        if (previewPanel != null) {
            ProfilerPanel profilerPanel = previewPanel.getProfilerPanel();
            if (profilerPanel != null) {
                profilerPanel.mouseEnteredGrammarEditorEvent(vfile, e);
            }
        }
    }

    public void editorFileClosedEvent(VirtualFile vfile) {
        // hopefully called only from swing EDT
        String grammarFileName = vfile.getPath();
        LOG.info("editorFileClosedEvent " + grammarFileName + " " + myProject.getName());
        if (!vfile.getName().endsWith(".g4")) {
            getPreviewWindow().hide(null);
            return;
        }

        // Dispose of state, editor, and such for this file
        PreviewState previewState = grammarToPreviewState.get(grammarFileName);
        if (previewState == null) { // project closing must have done already
            return;
        }

        previewState.g = null; // wack old ref to the Grammar for text in editor
        previewState.lg = null;

        previewPanel.closeGrammar(vfile);

        grammarToPreviewState.remove(grammarFileName);

        // close tool window
        getPreviewWindow().hide(null);
    }

    /**
     * Make sure to run after updating grammars in previewState
     */
    public void runANTLRTool(final VirtualFile grammarFile) {
        String title = "ANTLR Code Generation";
        boolean canBeCancelled = true;
        boolean forceGeneration = false;
        Task gen =
            new RunANTLROnGrammarFile(grammarFile,
                myProject,
                title,
                canBeCancelled,
                forceGeneration);
        ProgressManager.getInstance().run(gen);
    }

    /**
     * Look for state information concerning this grammar file and update
     * the Grammar objects.  This does not necessarily update the grammar file
     * in the current editor window.  Either we are already looking at
     * this grammar or we will have seen a grammar file changed event.
     * (I hope!)
     */
    public void updateGrammarObjectsFromFile(VirtualFile grammarFile) {
        updateGrammarObjectsFromFile_(grammarFile);

        // if grammarFileName is a separate lexer, we need to look for
        // its matching parser, if any, that is loaded in an editor
        // (don't go looking on disk).
        PreviewState s = getAssociatedParserIfLexer(grammarFile.getPath());
        if (s != null) {
            // try to load lexer again and associate with this parser grammar.
            // must update parser too as tokens have changed
            updateGrammarObjectsFromFile_(s.grammarFile);
        }
    }

    public String updateGrammarObjectsFromFile_(VirtualFile grammarFile) {
        String grammarFileName = grammarFile.getPath();
        PreviewState previewState = getPreviewState(grammarFile);
        Grammar[] grammars = ParsingUtils.loadGrammars(grammarFileName, myProject);
        if (grammars != null) {
            synchronized (previewState) { // build atomically
                previewState.lg = (LexerGrammar) grammars[0];
                previewState.g = grammars[1];
            }
        }
        return grammarFileName;
    }

    public PreviewState getAssociatedParserIfLexer(String grammarFileName) {
        for (PreviewState s : grammarToPreviewState.values()) {
            if (s != null && s.lg != null &&
                (grammarFileName.equals(s.lg.fileName) || s.lg == ParsingUtils.BAD_LEXER_GRAMMAR)) {
                // s has a lexer with same filename, see if there is a parser grammar
                // (not a combined grammar)
                if (s.g != null && s.g.getType() == ANTLRParser.PARSER) {
                    //					System.out.println(s.lg.fileName+" vs "+grammarFileName+", g="+s.g.name+", type="+s.g.getTypeString());
                    return s;
                }
            }
        }
        return null;
    }

    public ParsingResult parseText(final VirtualFile grammarFile, String inputText) throws IOException {
        String grammarFileName = grammarFile.getPath();
        if (!new File(grammarFileName).exists()) {
            LOG.error("parseText grammar doesn't exist " + grammarFileName);
            return null;
        }

        // Wipes out the console and also any error annotations
        previewPanel.inputPanel.clearParseErrors();

        final PreviewState previewState = getPreviewState(grammarFile);

        long start = System.nanoTime();
        previewState.parsingResult =
            ParsingUtils.parseText(previewState.g, previewState.lg,
                previewState.startRuleName,
                grammarFile, inputText);
        if (previewState.parsingResult == null) {
            return null;
        }
        long stop = System.nanoTime();

        previewPanel.profilerPanel.setProfilerData(previewState, stop - start);

        SyntaxErrorListener syntaxErrorListener = previewState.parsingResult.syntaxErrorListener;
        previewPanel.inputPanel.showParseErrors(syntaxErrorListener.getSyntaxErrors());

        return previewState.parsingResult;
    }

    public PreviewPanel getPreviewPanel() {
        if (previewPanel == null) {
            previewPanel = new PreviewPanel(myProject);
        }
        return previewPanel;
    }

    @RequiredUIAccess
    public ConsoleView getOrCreateConsoleView() {
        ContentManager contentManager = MessageView.getInstance(myProject).getContentManager();

        for (Content content : contentManager.getContents()) {
            ConsoleView view = content.getUserData(CONSOLE_VIEW_KEY);
            if (view != null) {
                return view;
            }
        }

        TextConsoleBuilderFactory factory = TextConsoleBuilderFactory.getInstance();
        TextConsoleBuilder consoleBuilder = factory.createBuilder(myProject);
        ConsoleView console = consoleBuilder.getConsole();

        Content content = ContentFactory.getInstance().createContent(console.getComponent(), "ANTLR4 Output", true);
        content.setDisposer(console);
        content.putUserData(CONSOLE_VIEW_KEY, console);

        contentManager.addContent(content);

        return console;
    }

    public void printToConsole(Consumer<ConsoleView> consoleViewConsumer) {
        myProject.getApplication().invokeLater(() -> {
            ConsoleView view = getOrCreateConsoleView();

            consoleViewConsumer.accept(view);
        });
    }

    public static void showConsoleWindow(final Project project) {
        project.getApplication().invokeLater(() -> {
            MessageView messageView = MessageView.getInstance(project);
            messageView.runWhenInitialized(() -> messageView.getToolWindow().show());
        });
    }

    public ToolWindow getPreviewWindow() {
        return ToolWindowManager.getInstance(myProject).getToolWindow(PREVIEW_WINDOW_ID);
    }

    @NotNull
    public PreviewState getPreviewState(VirtualFile grammarFile) {
        // make sure only one thread tries to add a preview state object for a given file
        String grammarFileName = grammarFile.getPath();
        // Have we seen this grammar before?
        PreviewState stateForCurrentGrammar = grammarToPreviewState.get(grammarFileName);
        if (stateForCurrentGrammar != null) {
            return stateForCurrentGrammar; // seen this before
        }

        // not seen, must create state
        stateForCurrentGrammar = new PreviewState(myProject, grammarFile);
        grammarToPreviewState.put(grammarFileName, stateForCurrentGrammar);

        return stateForCurrentGrammar;
    }

    public Editor getEditor(VirtualFile vfile) {
        final FileDocumentManager fdm = FileDocumentManager.getInstance();
        final Document doc = fdm.getDocument(vfile);
        if (doc == null) {
            return null;
        }

        EditorFactory factory = EditorFactory.getInstance();
        final Editor[] editors = factory.getEditors(doc, previewPanel.project);
        if (editors.length == 0) {
            // no editor found for this file. likely an out-of-sequence issue
            // where Intellij is opening a project and doesn't fire events
            // in order we'd expect.
            return null;
        }
        return editors[0]; // hope just one
    }


    /**
     * Get the state information associated with the grammar in the current
     * editor window. If there is no grammar in the editor window, return null.
     * If there is a grammar, return any existing preview state else
     * create a new one in store in the map.
     * <p>
     * Too dangerous; turning off but might be useful later.
     * public @org.jetbrains.annotations.Nullable PreviewState getPreviewState() {
     * VirtualFile currentGrammarFile = getCurrentGrammarFile();
     * if ( currentGrammarFile==null ) {
     * return null;
     * }
     * String currentGrammarFileName = currentGrammarFile.getPath();
     * if ( currentGrammarFileName==null ) {
     * return null; // we are not looking at a grammar file
     * }
     * return getPreviewState(currentGrammarFile);
     * }
     */

    // These "get current editor file" routines should only be used
    // when you are sure the user is in control and is viewing the
    // right file (i.e., don't use these during project loading etc...)
    public static VirtualFile getCurrentEditorFile(Project project) {
        FileEditorManager fmgr = FileEditorManager.getInstance(project);
        // "If more than one file is selected (split), the file with most recent focused editor is returned first." from IDE doc on method
        VirtualFile files[] = fmgr.getSelectedFiles();
        if (files.length == 0) {
            return null;
        }
        return files[0];
    }

    public VirtualFile getCurrentGrammarFile() {
        return getCurrentGrammarFile(myProject);
    }

    public static VirtualFile getCurrentGrammarFile(Project project) {
        VirtualFile f = getCurrentEditorFile(project);
        if (f == null) {
            return null;
        }
        if (f.getName().endsWith(".g4")) {
            return f;
        }
        return null;
    }

    private class GrammarEditorMouseAdapter implements EditorMouseListener {
        @Override
        public void mouseClicked(EditorMouseEvent e) {
            Document doc = e.getEditor().getDocument();
            VirtualFile vfile = FileDocumentManager.getInstance().getFile(doc);
            if (vfile != null && vfile.getName().endsWith(".g4")) {
                mouseEnteredGrammarEditorEvent(vfile, e);
            }
        }
    }

    private class MyVirtualFileAdapter implements VirtualFileListener {
        @Override
        public void contentsChanged(VirtualFileEvent event) {
            final VirtualFile vfile = event.getFile();
            if (!vfile.getName().endsWith(".g4")) {
                return;
            }
            if (!projectIsClosed) {
                grammarFileSavedEvent(vfile);
            }
        }
    }

    private class MyFileEditorManagerAdapter extends FileEditorManagerAdapter {
        @Override
        public void selectionChanged(FileEditorManagerEvent event) {
            if (!projectIsClosed) {
                currentEditorFileChangedEvent(event.getOldFile(), event.getNewFile());
            }
        }

        @Override
        public void fileClosed(FileEditorManager source, VirtualFile file) {
            if (!projectIsClosed) {
                editorFileClosedEvent(file);
            }
        }
    }

}
