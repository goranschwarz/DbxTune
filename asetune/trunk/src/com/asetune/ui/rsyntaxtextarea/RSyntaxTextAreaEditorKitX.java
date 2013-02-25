package com.asetune.ui.rsyntaxtextarea;

import java.awt.event.ActionEvent;

import javax.swing.UIManager;
import javax.swing.text.BadLocationException;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextAreaEditorKit;
import org.fife.ui.rtextarea.RTextArea;
import org.fife.ui.rtextarea.RecordableTextAction;

public class RSyntaxTextAreaEditorKitX
{
//	/**
//	 * Selects the word around the caret.
//	 */
//	@SuppressWarnings("serial")
//	public static class SelectWordAction extends RecordableTextAction
//	{
//		protected Action	start;
//		protected Action	end;
//
//		public SelectWordAction()
//		{
//			super(RTextAreaEditorKit.selectWordAction);
//			createActions();
//		}
//
//		public void actionPerformedImpl(ActionEvent e, RTextArea textArea)
//		{
//			start.actionPerformed(e);
//			end.actionPerformed(e);
//		}
//
//		protected void createActions()
//		{
//			start = new BeginWordAction("pigdog", false);
//			end = new EndWordAction("pigdog", true);
//		}
//
//		public final String getMacroID()
//		{
//			return DefaultEditorKit.selectWordAction;
//		}
//	}
	/**
	 * Selects the word around the caret.
	 */
	@SuppressWarnings("serial")
	public static class SelectWordAction extends RSyntaxTextAreaEditorKit.SelectWordAction
	{
		@Override
		protected void createActions()
		{
			start = new BeginWordAction("pigdog", false);
			end = new EndWordAction("pigdog", true);
		}
	}

	/**
	 * Positions the caret at the next word.
	 */
	@SuppressWarnings("serial")
	public static class NextWordAction extends RSyntaxTextAreaEditorKit.NextWordAction 
	{
		public NextWordAction(String name, boolean select)
		{
			super(name, select);
		}

		@Override
		protected int getNextWord(RTextArea textArea, int offs)
		throws BadLocationException 
		{
//			return Utilities.getNextWord(textArea, offs);
			return RSyntaxUtilitiesX.getNextWord(textArea, offs);
		}
	}
    /**
     * Positions the caret at the beginning of the previous word.
     */
	@SuppressWarnings("serial")
    public static class PreviousWordAction extends RSyntaxTextAreaEditorKit.PreviousWordAction 
    {
		public PreviousWordAction(String name, boolean select) 
		{
			super(name, select);
		}

		@Override
		protected int getPreviousWord(RTextArea textArea, int offs)
		throws BadLocationException 
		{
//			return Utilities.getPreviousWord(textArea, offs);
			return RSyntaxUtilitiesX.getPreviousWord(textArea, offs);
		}
    }

	/**
	 * Positions the caret at the beginning of the word.
	 */
	@SuppressWarnings("serial")
	protected static class BeginWordAction extends RecordableTextAction
	{
		private boolean	select;

		protected BeginWordAction(String name, boolean select)
		{
			super(name);
			this.select = select;
		}

		@Override
		public void actionPerformedImpl(ActionEvent e, RTextArea textArea)
		{
			try
			{
				int offs = textArea.getCaretPosition();
				int begOffs = getWordStart(textArea, offs);
//System.out.println("BeginWordAction.actionPerformedImpl: offs="+offs+", beginOffs="+begOffs+", select="+select);
				if ( select )
					textArea.moveCaretPosition(begOffs);
				else
					textArea.setCaretPosition(begOffs);
			}
			catch (BadLocationException ble)
			{
				UIManager.getLookAndFeel().provideErrorFeedback(textArea);
			}
		}

		@Override
		public final String getMacroID()
		{
			return getName();
		}

		protected int getWordStart(RTextArea textArea, int offs) throws BadLocationException
		{
//			return RSyntaxUtilities.getWordStart((RSyntaxTextArea) textArea, offs);
			return RSyntaxUtilitiesX.getWordStart((RSyntaxTextArea) textArea, offs);
		}
	}

	/**
	 * Positions the caret at the end of the word.
	 */
	@SuppressWarnings("serial")
	protected static class EndWordAction extends RecordableTextAction
	{
		private boolean	select;

		protected EndWordAction(String name, boolean select)
		{
			super(name);
			this.select = select;
		}

		@Override
		public void actionPerformedImpl(ActionEvent e, RTextArea textArea)
		{
			try
			{
				int offs = textArea.getCaretPosition();
				int endOffs = getWordEnd(textArea, offs);
				if ( select )
					textArea.moveCaretPosition(endOffs);
				else
					textArea.setCaretPosition(endOffs);
			}
			catch (BadLocationException ble)
			{
				UIManager.getLookAndFeel().provideErrorFeedback(textArea);
			}
		}

		@Override
		public final String getMacroID()
		{
			return getName();
		}

		protected int getWordEnd(RTextArea textArea, int offs) throws BadLocationException
		{
//			return RSyntaxUtilities.getWordEnd((RSyntaxTextArea) textArea, offs);
			return RSyntaxUtilitiesX.getWordEnd((RSyntaxTextArea) textArea, offs);
		}
	}
}
