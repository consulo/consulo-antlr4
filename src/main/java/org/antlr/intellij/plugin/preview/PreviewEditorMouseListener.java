package org.antlr.intellij.plugin.preview;

import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorPopupHelper;
import consulo.codeEditor.event.EditorMouseEvent;
import consulo.codeEditor.event.EditorMouseEventArea;
import consulo.codeEditor.markup.RangeHighlighter;
import consulo.ui.ex.popup.JBPopup;
import consulo.codeEditor.event.EditorMouseListener;
import consulo.codeEditor.event.EditorMouseMotionListener;
import org.antlr.intellij.plugin.actions.MyActionUtils;
import org.antlr.v4.runtime.atn.AmbiguityInfo;
import org.antlr.v4.runtime.atn.LookaheadEventInfo;

import java.awt.event.MouseEvent;
import java.util.List;

class PreviewEditorMouseListener implements EditorMouseListener, EditorMouseMotionListener
{
	private InputPanel inputPanel;

	public PreviewEditorMouseListener(InputPanel inputPanel) {
		this.inputPanel = inputPanel;
	}

	@Override
	public void mouseExited(EditorMouseEvent e) {
		InputPanel.clearTokenInfoHighlighters(e.getEditor());
	}

	@Override
	public void mouseClicked(EditorMouseEvent e) {
		final int offset = getEditorCharOffsetAndRemoveTokenHighlighters(e);
		if ( offset<0 ) return;

		final Editor editor=e.getEditor();
		if ( inputPanel.previewState==null ) {
			return;
		}

		if ( e.getMouseEvent().getButton()==MouseEvent.BUTTON3 ) { // right click
			rightClick(e, inputPanel.previewState, editor, offset);
			return;
		}

		MouseEvent mouseEvent=e.getMouseEvent();
		if ( mouseEvent.isControlDown() ) {
			inputPanel.setCursorToGrammarElement(e.getEditor().getProject(), inputPanel.previewState, offset);
			inputPanel.setCursorToHierarchyViewElement(offset);
		}
		else if ( mouseEvent.isAltDown() ) {
			inputPanel.setCursorToGrammarRule(e.getEditor().getProject(), inputPanel.previewState, offset);
		}
		else {
			inputPanel.setCursorToHierarchyViewElement(offset);
		}
		InputPanel.clearDecisionEventHighlighters(editor);
	}

	public void rightClick(final EditorMouseEvent e,
						   final PreviewState previewState,
						   Editor editor, int offset)
	{
		if (previewState.parsingResult == null) return;
		final List<RangeHighlighter> highlightersAtOffset = MyActionUtils.getRangeHighlightersAtOffset(editor, offset);
		if (highlightersAtOffset.size() == 0) {
			return;
		}

		final AmbiguityInfo ambigInfo =
			(AmbiguityInfo)MyActionUtils.getHighlighterWithDecisionEventType(highlightersAtOffset,
																			 AmbiguityInfo.class);
		final LookaheadEventInfo lookaheadInfo =
			(LookaheadEventInfo)MyActionUtils.getHighlighterWithDecisionEventType(highlightersAtOffset,
																				  LookaheadEventInfo.class);
		if ( ambigInfo!=null ) {
			JBPopup popup = ShowAmbigTreesDialog.createAmbigTreesPopup(previewState, ambigInfo);
			EditorPopupHelper.getInstance().showPopupInBestPositionFor(editor, popup);
		}
		else if ( lookaheadInfo != null ) {
			JBPopup popup = ShowAmbigTreesDialog.createLookaheadTreesPopup(previewState, lookaheadInfo);
			EditorPopupHelper.getInstance().showPopupInBestPositionFor(editor, popup);
		}
	}

	@Override
	public void mouseMoved(EditorMouseEvent e){
		int offset = getEditorCharOffsetAndRemoveTokenHighlighters(e);
		if ( offset<0 ) return;

		Editor editor=e.getEditor();
		if ( inputPanel.previewState==null ) {
			return;
		}

		MouseEvent mouseEvent=e.getMouseEvent();
		InputPanel.clearTokenInfoHighlighters(e.getEditor());
		if ( mouseEvent.isControlDown() && inputPanel.previewState.parsingResult!=null ) {
			inputPanel.showTokenInfoUponCtrlKey(editor, inputPanel.previewState, offset);
		}
		else if ( mouseEvent.isAltDown() && inputPanel.previewState.parsingResult!=null ) {
			inputPanel.showParseRegion(e, editor, inputPanel.previewState, offset);
		}
		else { // just moving around, show any errors or hints
			InputPanel.showTooltips(e, editor, inputPanel.previewState, offset);
		}
	}

	public int getEditorCharOffsetAndRemoveTokenHighlighters(EditorMouseEvent e) {
		if ( e.getArea()!=EditorMouseEventArea.EDITING_AREA ) {
			return -1;
		}

		MouseEvent mouseEvent=e.getMouseEvent();
		Editor editor=e.getEditor();
		int offset = MyActionUtils.getMouseOffset(mouseEvent, editor);
//		System.out.println("offset="+offset);

		if ( offset >= editor.getDocument().getTextLength() ) {
			return -1;
		}

		// Mouse has moved so make sure we don't show any token information tooltips
		InputPanel.clearTokenInfoHighlighters(e.getEditor());
		return offset;
	}

	// ------------------------

	@Override
	public void mousePressed(EditorMouseEvent e) {
	}

	@Override
	public void mouseReleased(EditorMouseEvent e) {
	}

	@Override
	public void mouseEntered(EditorMouseEvent e) {
	}

	@Override
	public void mouseDragged(EditorMouseEvent e) {
	}
}
