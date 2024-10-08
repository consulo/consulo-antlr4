package org.antlr.intellij.plugin.preview;

import consulo.codeEditor.*;
import consulo.codeEditor.event.EditorMouseEvent;
import consulo.codeEditor.markup.HighlighterLayer;
import consulo.codeEditor.markup.HighlighterTargetArea;
import consulo.codeEditor.markup.MarkupModel;
import consulo.codeEditor.markup.RangeHighlighter;
import consulo.colorScheme.EffectType;
import consulo.colorScheme.TextAttributes;
import consulo.document.Document;
import consulo.document.event.DocumentAdapter;
import consulo.document.event.DocumentEvent;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.fileChooser.FileChooserDescriptorFactory;
import consulo.language.editor.hint.HintManager;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.color.ColorValue;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.awt.ComponentWithBrowseButton;
import consulo.ui.ex.awt.TextComponentAccessor;
import consulo.ui.ex.awt.TextFieldWithBrowseButton;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.style.StandardColors;
import consulo.util.dataholder.Key;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import org.antlr.intellij.adaptor.parser.SyntaxError;
import org.antlr.intellij.plugin.ANTLRv4PluginController;
import org.antlr.intellij.plugin.Icons;
import org.antlr.intellij.plugin.actions.MyActionUtils;
import org.antlr.intellij.plugin.parsing.ParsingUtils;
import org.antlr.intellij.plugin.parsing.PreviewParser;
import org.antlr.intellij.plugin.profiler.ProfilerPanel;
import org.antlr.runtime.CommonToken;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.misc.Pair;
import org.antlr.v4.runtime.misc.Utils;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.antlr.v4.tool.Rule;
import org.antlr.v4.tool.ast.GrammarAST;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// Not a view itself but delegates to one.

public class InputPanel {
	public static final Key<SyntaxError> SYNTAX_ERROR = Key.create("SYNTAX_ERROR");
	public static final int MAX_STACK_DISPLAY = 30;
	public static final int MAX_HINT_WIDTH = 110;

	private JRadioButton inputRadioButton;
	private JRadioButton fileRadioButton;
	private JTextArea placeHolder;
	private JTextArea errorConsole;
	private JLabel startRuleLabel;
	private JPanel radioButtonPanel;
	private JPanel startRuleAndInputPanel;
	private TextFieldWithBrowseButton fileChooser;
	protected JPanel outerMostPanel;

	public static final Logger LOG = Logger.getInstance("ANTLR InputPanel");
	public static final int TOKEN_INFO_LAYER = HighlighterLayer.SELECTION; // Show token info over errors
	public static final int ERROR_LAYER = HighlighterLayer.ERROR;

	/**
	 * switchToGrammar() was seeing an empty slot instead of a previous
	 * editor or placeHolder. Figured it was an order of operations thing
	 * and synchronized add/remove ops. Works now w/o error.
	 */
	public final Object swapEditorComponentLock = new Object();

	public static final String missingStartRuleLabelText =
		"%s start rule: <select from navigator or grammar>";
	public static final String startRuleLabelText = "%s start rule: %s";

	public PreviewPanel previewPanel;

	/**
	 * state for grammar in current editor, not editor where user is typing preview input!
	 */
	public PreviewState previewState;

	PreviewEditorMouseListener editorMouseListener;

	public InputPanel(final PreviewPanel previewPanel) {
		$$$setupUI$$$();

		this.previewPanel = previewPanel;

		FileChooserDescriptor singleFileDescriptor =
			FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor();
		ComponentWithBrowseButton.BrowseFolderActionListener<JTextField> browseActionListener =
			new ComponentWithBrowseButton.BrowseFolderActionListener<JTextField>(
				"Select input file", null,
				fileChooser,
				previewPanel.project,
				singleFileDescriptor,
				TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT
			) {
				protected void onFileChoosen(VirtualFile chosenFile) {
					// In 13.x, this is defined but they fixed typo (onFileChosen) and
					// deprecated. In 15.x this method is gone so I add back for
					// backward compatibility.
					choose(chosenFile);
				}

				protected void onFileChosen(@NotNull VirtualFile chosenFile) {
					choose(chosenFile);
				}

				protected void choose(VirtualFile chosenFile) {
					// this next line is the code taken from super; pasted in
					// to avoid compile error on super.onFileCho[o]sen
					TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT.setText(fileChooser.getChildComponent(),
					                                                    chosenFileToResultingText(chosenFile));
					if ( previewState!=null ) {
						previewState.inputFileName = chosenFile.getPath();
					}
					selectFileEvent();
				}
			};
		fileChooser.addBrowseFolderListener(previewPanel.project, browseActionListener);
		fileChooser.getButton().addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				fileRadioButton.setSelected(true);
			}
		});
		fileChooser.setTextFieldPreferredWidth(40);

		inputRadioButton.addActionListener(
			new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					selectInputEvent();
				}
			}
		                                  );
		fileRadioButton.addActionListener(
			new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					selectFileEvent();
				}
			}
		                                 );

		resetStartRuleLabel();

		editorMouseListener = new PreviewEditorMouseListener(this);
	}

	public JPanel getComponent() {
		return outerMostPanel;
	}

	private void createUIComponents() {
		// TODO: place custom component creation code here
	}

	public JTextArea getErrorConsole() {
		return errorConsole;
	}

	public JLabel getStartRuleLabel() {
		return startRuleLabel;
	}

	public void selectInputEvent() {
		inputRadioButton.setSelected(true);

		// get state for grammar in current editor, not editor where user is typing preview input!
//		ANTLRv4PluginController controller = ANTLRv4PluginController.getInstance(previewPanel.project);
//		final PreviewState previewState = controller.getPreviewState();
//		if (previewState == null) {
//			return;
//		}

		// wipe old and make new one
		if ( previewState!=null ) {
			releaseEditor(previewState);
			createManualInputPreviewEditor(previewState);
		}
		previewPanel.clearParseTree();
		clearErrorConsole();
	}

	public void createManualInputPreviewEditor(final PreviewState previewState) {
		final EditorFactory factory = EditorFactory.getInstance();
		Document doc = factory.createDocument(previewState.manualInputText);
		doc.addDocumentListener(
			new DocumentAdapter() {
				@Override
				public void documentChanged(DocumentEvent e) {
					previewState.manualInputText = e.getDocument().getCharsSequence();
				}
			}
		                       );

		Editor editor = createPreviewEditor(previewState.grammarFile, doc);
		setEditorComponent(editor.getComponent()); // do before setting state
		previewState.setInputEditor(editor);
	}

	public void selectFileEvent() {
		fileRadioButton.setSelected(true);

		if ( previewState==null ) {
			return;
		}

		String inputFileName = fileChooser.getText();
		String inputText = "";
		if ( inputFileName.trim().length()>0 ) {
			try {
				inputText = StringUtil.convertLineSeparators(Files.readString(Path.of(inputFileName)));
			} catch (IOException ioe) {
				LOG.error("can't load input file "+inputFileName, ioe);
			}
		}
		// get state for grammar in current editor, not editor where user is typing preview input!
		ANTLRv4PluginController controller = ANTLRv4PluginController.getInstance(previewPanel.project);

		// wipe old and make new one
		releaseEditor(previewState);
		final EditorFactory factory = EditorFactory.getInstance();
		Document doc = factory.createDocument(inputText);
		doc.setReadOnly(true);
		Editor editor = createPreviewEditor(controller.getCurrentGrammarFile(), doc);
		setEditorComponent(editor.getComponent()); // do before setting state
		previewState.setInputEditor(editor);
		clearErrorConsole();
		previewPanel.updateParseTreeFromDoc(controller.getCurrentGrammarFile());
	}

	public Editor createPreviewEditor(final VirtualFile grammarFile, Document doc) {
		LOG.info("createEditor: create new editor for "+grammarFile.getPath()+" "+previewPanel.project.getName());
		final EditorFactory factory = EditorFactory.getInstance();
		doc.addDocumentListener(
			new DocumentAdapter() {
				VirtualFile grammarFileForThisPreviewEditor;

				{
					{ // faux ctor
						this.grammarFileForThisPreviewEditor = grammarFile;
					}
				}

				@Override
				public void documentChanged(DocumentEvent event) {
					previewPanel.updateParseTreeFromDoc(grammarFileForThisPreviewEditor);
				}
			}
		                       );
		final Editor editor = factory.createEditor(doc, previewPanel.project);
		// force right margin
		// TODO ((EditorMarkupModel) editor.getMarkupModel()).setErrorStripeVisible(true);
		EditorSettings settings = editor.getSettings();
		settings.setWhitespacesShown(true);
		settings.setLineNumbersShown(true);
		settings.setLineMarkerAreaShown(true);
		installListeners(editor);

//		EditorGutter gutter = editor.getGutter();
//		gutter.registerTextAnnotation(
//			new TextAnnotationGutterProvider() {
//				@Nullable
//				@Override
//				public String getLineText(int line, Editor editor) {
//					return "foo";
//				}
//
//				@Nullable
//				@Override
//				public String getToolTip(int line, Editor editor) {
//					return "tool tip";
//				}
//
//				@Override
//				public EditorFontType getStyle(int line, Editor editor) {
//					return null;
//				}
//
//				@Nullable
//				@Override
//				public ColorKey getColor(int line, Editor editor) {
//					return EditorColors.MODIFIED_LINES_COLOR;
//				}
//
//				@Nullable
//				@Override
//				public Color getBgColor(int line, Editor editor) {
//					return JBColor.WHITE;
//				}
//
//				@Override
//				public List<AnAction> getPopupActions(int line, Editor editor) {
//					return null;
//				}
//
//				@Override
//				public void gutterClosed() {
//
//				}
//			}
//		);

		return editor;
	}

	public void grammarFileSaved() {
		clearParseErrors();
	}

	public void switchToGrammar(PreviewState previewState, VirtualFile grammarFile) {
		String grammarFileName = grammarFile.getPath();
		LOG.info("switchToGrammar "+grammarFileName+" "+previewPanel.project.getName());
		this.previewState = previewState;

		if ( previewState.inputFileName!=null && previewState.inputFileName.length()>0 ) {
			fileChooser.setText(previewState.inputFileName);
			selectFileEvent();
		}
		else {
			selectInputEvent();
		}

		clearParseErrors();

		if ( previewState.startRuleName!=null ) {
			setStartRuleName(grammarFile, previewState.startRuleName);
		}
		else {
			resetStartRuleLabel();
		}
	}

	public void setEditorComponent(JComponent editor) {
		BorderLayout layout = (BorderLayout) outerMostPanel.getLayout();
		String EDITOR_SPOT_COMPONENT = BorderLayout.CENTER;
		// atomically remove old
		synchronized (swapEditorComponentLock) {
			Component editorSpotComp = layout.getLayoutComponent(EDITOR_SPOT_COMPONENT);
			if ( editorSpotComp!=null ) {
				editorSpotComp.setVisible(false);
				outerMostPanel.remove(editorSpotComp); // remove old editor if it's there
			}
			outerMostPanel.add(editor, EDITOR_SPOT_COMPONENT);
		}
	}

	public Editor getInputEditor() {
		if ( previewState==null ) {
			// seems there are some out of sequence issues with InputPanels
			// being created but before we get a switchToGrammar event, which
			// creates the previewState.
			return null;
		}
		Editor editor = previewState.getInputEditor();
		if ( editor==null ) {
			createManualInputPreviewEditor(previewState); // ensure we always have an input window
			editor = previewState.getInputEditor();
		}

		return editor;
	}

	public String getChosenFileName() {
		return fileChooser.getText();
	}

	public boolean fileInputIsSelected() {
		return fileRadioButton.isSelected();
	}

	public void releaseEditor(PreviewState previewState) {
		uninstallListeners(previewState.getInputEditor());

		// release the editor
		previewState.releaseEditor();

		// restore the GUI
		setEditorComponent(placeHolder);
	}

	public void installListeners(Editor editor) {
		editor.addEditorMouseMotionListener(editorMouseListener);
		editor.addEditorMouseListener(editorMouseListener);
	}

	public void uninstallListeners(Editor editor) {
		if ( editor==null ) return;
		editor.removeEditorMouseListener(editorMouseListener);
		editor.removeEditorMouseMotionListener(editorMouseListener);
	}

	public void setStartRuleName(VirtualFile grammarFile, String startRuleName) {
		startRuleLabel.setText(String.format(startRuleLabelText, grammarFile.getName(), startRuleName));
		startRuleLabel.setForeground(JBColor.BLACK);
		final Font oldFont = startRuleLabel.getFont();
		startRuleLabel.setFont(oldFont.deriveFont(Font.BOLD));
		startRuleLabel.setIcon(TargetAWT.to(Icons.FILE));
	}

	public void resetStartRuleLabel() {
		String grammarName = "?.g4";
		if ( previewState!=null ) {
			grammarName = previewState.grammarFile.getName();
		}
		startRuleLabel.setText(String.format(missingStartRuleLabelText, grammarName));
		startRuleLabel.setForeground(JBColor.RED);
		startRuleLabel.setIcon(TargetAWT.to(Icons.FILE));
	}

	public void clearErrorConsole() {
		errorConsole.setText("");
	}

	public void displayErrorInParseErrorConsole(SyntaxError e) {
		String msg = getErrorDisplayString(e);
		errorConsole.insert(msg+'\n', errorConsole.getText().length());
	}

	public void displayErrorInParseErrorConsole(String msg) {
		errorConsole.insert(msg+'\n', errorConsole.getText().length());
	}

	public void clearParseErrors() {
		Editor editor = getInputEditor();
		if ( editor==null ) return;

		clearInputEditorHighlighters();

		HintManager.getInstance().hideAllHints();

		clearErrorConsole();
	}

	/**
	 * Clear all input highlighters
	 */
	public void clearInputEditorHighlighters() {
		Editor editor = getInputEditor();
		if ( editor==null ) return;

		MarkupModel markupModel = editor.getMarkupModel();
		markupModel.removeAllHighlighters();
	}

	/**
	 * Clear decision stuff but leave syntax errors
	 */
	public static void clearDecisionEventHighlighters(Editor editor) {
		removeHighlighters(editor, ProfilerPanel.DECISION_EVENT_INFO_KEY);
	}

	/**
	 * Remove any previous underlining or boxing, but not errors or decision event info
	 */
	public static void clearTokenInfoHighlighters(Editor editor) {
		MarkupModel markupModel = editor.getMarkupModel();
		for (RangeHighlighter r : markupModel.getAllHighlighters()) {
			if ( r.getUserData(ProfilerPanel.DECISION_EVENT_INFO_KEY)==null &&
				r.getUserData(SYNTAX_ERROR)==null ) {
				markupModel.removeHighlighter(r);
			}
		}
	}

	/**
	 * Display error messages to the console and also add annotations
	 * to the preview input window.
	 */
	public void showParseErrors(final List<SyntaxError> errors) {
		if ( errors.size()==0 ) {
			clearInputEditorHighlighters();
			return;
		}
		for (SyntaxError e : errors) {
			annotateErrorsInPreviewInputEditor(e);
			displayErrorInParseErrorConsole(e);
		}
	}

	/**
	 * Show token information if the ctrl-key is down and mouse movement occurs
	 */
	public void showTokenInfoUponCtrlKey(Editor editor, PreviewState previewState, int offset) {
		Token tokenUnderCursor = ParsingUtils.getTokenUnderCursor(previewState, offset);
		if ( tokenUnderCursor==null ) {
			PreviewParser parser = (PreviewParser) previewState.parsingResult.parser;
			CommonTokenStream tokenStream = (CommonTokenStream) parser.getInputStream();
			tokenUnderCursor = ParsingUtils.getSkippedTokenUnderCursor(tokenStream, offset);
		}

		if ( tokenUnderCursor==null ) {
			return;
		}

//		System.out.println("token = "+tokenUnderCursor);
		String channelInfo = "";
		int channel = tokenUnderCursor.getChannel();
		if ( channel!=Token.DEFAULT_CHANNEL ) {
			String chNum = channel==Token.HIDDEN_CHANNEL ? "hidden" : String.valueOf(channel);
			channelInfo = ", Channel "+chNum;
		}
        StandardColors color = StandardColors.BLUE;
		String tokenInfo =
			String.format("#%d Type %s, Line %d:%d%s",
			              tokenUnderCursor.getTokenIndex(),
			              previewState.g.getTokenDisplayName(tokenUnderCursor.getType()),
			              tokenUnderCursor.getLine(),
			              tokenUnderCursor.getCharPositionInLine(),
			              channelInfo
			             );
		if ( channel==-1 ) {
			tokenInfo = "Skipped";
			color = StandardColors.GRAY;
		}

		Interval sourceInterval = Interval.of(tokenUnderCursor.getStartIndex(),
		                                      tokenUnderCursor.getStopIndex()+1);
		highlightAndOfferHint(editor, offset, sourceInterval,
		                      color, EffectType.LINE_UNDERSCORE, tokenInfo);
	}

	/**
	 * Show tokens/region associated with parse tree parent of this token
	 * if the alt-key is down and mouse movement occurs.
	 */
	public void showParseRegion(EditorMouseEvent event, Editor editor,
								PreviewState previewState, int offset) {
		Token tokenUnderCursor = ParsingUtils.getTokenUnderCursor(previewState, offset);
		if ( tokenUnderCursor==null ) {
			return;
		}

		ParseTree tree = previewState.parsingResult.tree;
		TerminalNode nodeWithToken =
			(TerminalNode) ParsingUtils.getParseTreeNodeWithToken(tree, tokenUnderCursor);
		if ( nodeWithToken==null ) {
			// hidden token
			return;
		}

		PreviewParser parser = (PreviewParser) previewState.parsingResult.parser;
		CommonTokenStream tokenStream = (CommonTokenStream) parser.getInputStream();
		ParserRuleContext parent = (ParserRuleContext) nodeWithToken.getParent();
		Interval tokenInterval = parent.getSourceInterval();
		Token startToken = tokenStream.get(tokenInterval.a);
		Token stopToken = tokenStream.get(tokenInterval.b);
		Interval sourceInterval =
			Interval.of(startToken.getStartIndex(), stopToken.getStopIndex()+1);
//		int ruleIndex = parent.getRuleIndex();
//		String ruleName = parser.getRuleNames()[ruleIndex];
//        System.out.println("parent " + ruleName + " region " + sourceInterval);

		List<String> stack = parser.getRuleInvocationStack(parent);
		Collections.reverse(stack);

		if ( stack.size()>MAX_STACK_DISPLAY ) {
			// collapse contiguous dups to handle left-recursive stacks
			List<Pair<String, Integer>> smaller = new ArrayList<Pair<String, Integer>>();
			int last = 0;
			smaller.add(new Pair<String, Integer>(stack.get(0), 1)); // init to having first element, count of 1
			for (int i = 1; i<stack.size(); i++) {
				String s = stack.get(i);
				if ( smaller.get(last).a.equals(s) ) {
					smaller.set(last, new Pair<String, Integer>(s, smaller.get(last).b+1));
				}
				else {
					smaller.add(new Pair<String, Integer>(s, 1));
					last++;
				}
			}
			stack = new ArrayList<String>();
			for (int i = 0; i<smaller.size(); i++) {
				Pair<String, Integer> pair = smaller.get(i);
				if ( pair.b>1 ) {
					stack.add(pair.a+"^"+pair.b);
				}
				else {
					stack.add(pair.a);
				}
			}
		}
		String stackS = Utils.join(stack.toArray(), "\n");
		highlightAndOfferHint(editor, offset, sourceInterval,
		                      StandardColors.BLUE, EffectType.ROUNDED_BOX, stackS);


		// Code for a balloon.

//		JBPopupFactory popupFactory = JBPopupFactory.getInstance();
//		BalloonBuilder builder =
//		    popupFactory.createHtmlTextBalloonBuilder(Utils.join(stack.toArray(), "<br>"),
//												  MessageType.INFO, null);
//		builder.setHideOnClickOutside(true);
//		Balloon balloon = builder.createBalloon();
//		MouseEvent mouseEvent = event.getMouseEvent();
//		Point point = mouseEvent.getPoint();
//		point.translate(10, -15);
//		RelativePoint where = new RelativePoint(mouseEvent.getComponent(), point);
//		balloon.show(where, Balloon.Position.above);
	}

	public void highlightAndOfferHint(Editor editor, int offset,
									  Interval sourceInterval,
									  ColorValue color,
									  EffectType effectType, String hintText) {
		CaretModel caretModel = editor.getCaretModel();
		final TextAttributes attr = new TextAttributes();
		attr.setForegroundColor(color);
		attr.setEffectColor(color);
		attr.setEffectType(effectType);
		MarkupModel markupModel = editor.getMarkupModel();
		markupModel.addRangeHighlighter(
			sourceInterval.a,
			sourceInterval.b,
			InputPanel.TOKEN_INFO_LAYER, // layer
			attr,
			HighlighterTargetArea.EXACT_RANGE
		                               );

		if ( hintText.contains("<") ) {
			hintText = hintText.replaceAll("<", "&lt;");
		}

		// HINT
		caretModel.moveToOffset(offset); // info tooltip only shows at cursor :(
		HintManager.getInstance().showInformationHint(editor, hintText);
	}

	public void setCursorToGrammarElement(Project project, PreviewState previewState, int offset) {
		Token tokenUnderCursor = ParsingUtils.getTokenUnderCursor(previewState, offset);
		if ( tokenUnderCursor==null ) {
			return;
		}

		PreviewParser parser = (PreviewParser) previewState.parsingResult.parser;
		Integer atnState = parser.inputTokenToStateMap.get(tokenUnderCursor);
		if ( atnState==null ) { // likely an error token
			//LOG.error("no ATN state for input token " + tokenUnderCursor);
			return;
		}

		Interval region = previewState.g.getStateToGrammarRegion(atnState);
		CommonToken token =
			(CommonToken) previewState.g.tokenStream.get(region.a);
		jumpToGrammarPosition(project, token.getStartIndex());
	}

	public void setCursorToGrammarRule(Project project, PreviewState previewState, int offset) {
		Token tokenUnderCursor = ParsingUtils.getTokenUnderCursor(previewState, offset);
		if ( tokenUnderCursor==null ) {
			return;
		}

		ParseTree tree = previewState.parsingResult.tree;
		TerminalNode nodeWithToken =
			(TerminalNode) ParsingUtils.getParseTreeNodeWithToken(tree, tokenUnderCursor);
		if ( nodeWithToken==null ) {
			// hidden token
			return;
		}

		ParserRuleContext parent = (ParserRuleContext) nodeWithToken.getParent();
		int ruleIndex = parent.getRuleIndex();
		Rule rule = previewState.g.getRule(ruleIndex);
		GrammarAST ruleNameNode = (GrammarAST) rule.ast.getChild(0);
		int start = ((CommonToken) ruleNameNode.getToken()).getStartIndex();

		jumpToGrammarPosition(project, start);
	}

	public void jumpToGrammarPosition(Project project, int start) {
		final ANTLRv4PluginController controller = ANTLRv4PluginController.getInstance(project);
		if ( controller==null ) return;
		final Editor grammarEditor = controller.getEditor(previewState.grammarFile);
		if ( grammarEditor==null ) return;

		CaretModel caretModel = grammarEditor.getCaretModel();
		caretModel.moveToOffset(start);
		ScrollingModel scrollingModel = grammarEditor.getScrollingModel();
		scrollingModel.scrollToCaret(ScrollType.MAKE_VISIBLE);
		grammarEditor.getContentComponent().requestFocus();
	}

	public void setCursorToHierarchyViewElement(int offset) {
		previewPanel.jTreeViewer.selectNodeAtOffset(offset);
	}

	/**
	 * Display syntax errors, hints in tooltips if under the cursor
	 */
	public static void showTooltips(EditorMouseEvent event, Editor editor,
									@NotNull PreviewState previewState, int offset) {
		if ( previewState.parsingResult==null ) return; // no results?

		// Turn off any tooltips if none under the cursor
		// find the highlighter associated with this offset
		List<RangeHighlighter> highlightersAtOffset = MyActionUtils.getRangeHighlightersAtOffset(editor, offset);
		if ( highlightersAtOffset.size()==0 ) {
			return;
		}

		List<String> msgList = new ArrayList<String>();
		boolean foundDecisionEvent = false;
		for (int i = 0; i<highlightersAtOffset.size(); i++) {
			RangeHighlighter r = highlightersAtOffset.get(i);
			DecisionEventInfo eventInfo = r.getUserData(ProfilerPanel.DECISION_EVENT_INFO_KEY);
			String msg;
			if ( eventInfo!=null ) {
				// TODO: move decision event stuff to profiler?
				if ( eventInfo instanceof AmbiguityInfo ) {
					msg = "Ambiguous upon alts "+eventInfo.configs.getAlts().toString();
				}
				else if ( eventInfo instanceof ContextSensitivityInfo ) {
					msg = "Context-sensitive";
				}
				else if ( eventInfo instanceof LookaheadEventInfo ) {
					int k = eventInfo.stopIndex-eventInfo.startIndex+1;
					msg = "Deepest lookahead k="+k;
				}
				else if ( eventInfo instanceof PredicateEvalInfo ) {
					PredicateEvalInfo evalInfo = (PredicateEvalInfo) eventInfo;
					msg = ProfilerPanel.getSemanticContextDisplayString(evalInfo,
					                                                    previewState,
					                                                    evalInfo.semctx, evalInfo.predictedAlt,
					                                                    evalInfo.evalResult);
					msg = msg+(!evalInfo.fullCtx ? " (DFA)" : "");
				}
				else {
					msg = "Unknown decision event: "+eventInfo;
				}
				foundDecisionEvent = true;
			}
			else {
				// error tool tips
				SyntaxError errorUnderCursor = r.getUserData(SYNTAX_ERROR);
				msg = getErrorDisplayString(errorUnderCursor);
				if ( msg.length()>MAX_HINT_WIDTH ) {
					msg = msg.substring(0, MAX_HINT_WIDTH)+"...";
				}
				if ( msg.indexOf('<')>=0 ) {
					msg = msg.replaceAll("<", "&lt;");
				}
			}
			msgList.add(msg);
		}
		String combinedMsg = Utils.join(msgList.iterator(), "\n");
		HintManager hintMgr = HintManager.getInstance();
		if ( foundDecisionEvent ) {
			showDecisionEventToolTip(editor, offset, hintMgr, combinedMsg.toString());
		}
		else {
			showPreviewEditorErrorToolTip(editor, offset, hintMgr, combinedMsg.toString());
		}
	}

	public static void showPreviewEditorErrorToolTip(Editor editor, int offset, HintManager hintMgr, String msg) {
		hintMgr.showErrorHint(editor, msg);
	}

	public static void showDecisionEventToolTip(Editor editor, int offset, HintManager hintMgr, String msg) {
		hintMgr.showInformationHint(editor, msg);
	}

	public void annotateErrorsInPreviewInputEditor(SyntaxError e) {
		Editor editor = getInputEditor();
		if ( editor==null ) return;
		MarkupModel markupModel = editor.getMarkupModel();

		int a, b; // Start and stop index
		RecognitionException cause = e.getException();
		if ( cause instanceof LexerNoViableAltException ) {
			a = ((LexerNoViableAltException) cause).getStartIndex();
			b = ((LexerNoViableAltException) cause).getStartIndex()+1;
		}
		else {
			Token offendingToken = (Token) e.getOffendingSymbol();
			a = offendingToken.getStartIndex();
			b = offendingToken.getStopIndex()+1;
		}
		final TextAttributes attr = new TextAttributes();
		attr.setForegroundColor(StandardColors.RED);
		attr.setEffectColor(StandardColors.RED);
		attr.setEffectType(EffectType.WAVE_UNDERSCORE);
		RangeHighlighter highlighter =
			markupModel.addRangeHighlighter(a,
			                                b,
			                                ERROR_LAYER, // layer
			                                attr,
			                                HighlighterTargetArea.EXACT_RANGE);
		highlighter.putUserData(SYNTAX_ERROR, e);
	}


	public static void removeHighlighters(Editor editor, Key<?> key) {
		// Remove anything with user data accessible via key
		MarkupModel markupModel = editor.getMarkupModel();
		for (RangeHighlighter r : markupModel.getAllHighlighters()) {
			if ( r.getUserData(key)!=null ) {
				markupModel.removeHighlighter(r);
			}
		}
	}

	public static String getErrorDisplayString(SyntaxError e) {
		return "line "+e.getLine()+":"+e.getCharPositionInLine()+" "+e.getMessage();
	}

	/**
	 * Method generated by IntelliJ IDEA GUI Designer
	 * >>> IMPORTANT!! <<<
	 * DO NOT edit this method OR call it in your code!
	 *
	 * @noinspection ALL
	 */
	private void $$$setupUI$$$() {
		outerMostPanel = new JPanel();
		outerMostPanel.setLayout(new BorderLayout(0, 0));
		outerMostPanel.setMinimumSize(new Dimension(100, 70));
		outerMostPanel.setPreferredSize(new Dimension(200, 100));
		startRuleAndInputPanel = new JPanel();
		startRuleAndInputPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 0));
		startRuleAndInputPanel.setMinimumSize(new Dimension(233, 60));
		outerMostPanel.add(startRuleAndInputPanel, BorderLayout.NORTH);
		startRuleAndInputPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), null));
		startRuleLabel = new JLabel();
		startRuleLabel.setText("Label");
		startRuleAndInputPanel.add(startRuleLabel);
		radioButtonPanel = new JPanel();
		radioButtonPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
		startRuleAndInputPanel.add(radioButtonPanel);
		inputRadioButton = new JRadioButton();
		inputRadioButton.setSelected(true);
		inputRadioButton.setText("Input");
		radioButtonPanel.add(inputRadioButton);
		fileRadioButton = new JRadioButton();
		fileRadioButton.setText("File");
		radioButtonPanel.add(fileRadioButton);
		fileChooser = new TextFieldWithBrowseButton();
		radioButtonPanel.add(fileChooser);
		placeHolder = new JTextArea();
		placeHolder.setBackground(Color.lightGray);
		placeHolder.setEditable(false);
		placeHolder.setEnabled(true);
		placeHolder.setText("");
		outerMostPanel.add(placeHolder, BorderLayout.WEST);
		final JScrollPane scrollPane1 = new JScrollPane();
		outerMostPanel.add(scrollPane1, BorderLayout.SOUTH);
		errorConsole = new JTextArea();
		errorConsole.setEditable(false);
		errorConsole.setLineWrap(true);
		errorConsole.setRows(3);
		scrollPane1.setViewportView(errorConsole);
		ButtonGroup buttonGroup;
		buttonGroup = new ButtonGroup();
		buttonGroup.add(fileRadioButton);
		buttonGroup.add(inputRadioButton);
	}

	/**
	 * @noinspection ALL
	 */
	public JComponent $$$getRootComponent$$$() {
		return outerMostPanel;
	}
}
