/*******************************************************************************
 * Copyright (C) 2010-2025 Goran Schwarz
 * 
 * This file is part of DbxTune
 * DbxTune is a family of sub-products *Tune, hence the Dbx
 * Here are some of the tools: AseTune, IqTune, RsTune, RaxTune, HanaTune, 
 *          SqlServerTune, PostgresTune, MySqlTune, MariaDbTune, Db2Tune, ...
 * 
 * DbxTune is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 * 
 * DbxTune is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with DbxTune.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.dbxtune.ui.rsyntaxtextarea;

import java.awt.event.ActionEvent;

import javax.swing.UIManager;
import javax.swing.text.BadLocationException;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextAreaEditorKit;
import org.fife.ui.rtextarea.RTextArea;
import org.fife.ui.rtextarea.RecordableTextAction;

import com.dbxtune.utils.SqlUtils;

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

	/**
	 * Enable Disable "mark words" on double click
	 */
	@SuppressWarnings("serial")
	public static class MarkWordOnDoubleClickAction extends RecordableTextAction
	{
		protected MarkWordOnDoubleClickAction(String name)
		{
			super(name);
		}

		@Override
		public void actionPerformedImpl(ActionEvent e, RTextArea textArea)
		{
			RSyntaxTextArea syntaxTextArea = (textArea instanceof RSyntaxTextArea) ? (RSyntaxTextArea) textArea : null;
			if (syntaxTextArea != null)
			{
				RSyntaxTextAreaX.setHiglightWordModeEnabled(syntaxTextArea, ! RSyntaxTextAreaX.isHiglightWordModeEnabled(syntaxTextArea) );
			}
		}

		@Override
		public final String getMacroID()
		{
			return getName();
		}
	}

	/**
	 * Format SQL
	 */
	@SuppressWarnings("serial")
	public static class FormatSqlAction extends RecordableTextAction
	{
		protected FormatSqlAction(String name)
		{
			super(name);
		}

		@Override
		public void actionPerformedImpl(ActionEvent e, RTextArea textArea)
		{
			if (!textArea.isEditable() || !textArea.isEnabled())
			{
				UIManager.getLookAndFeel().provideErrorFeedback(textArea);
				return;
			}

			String str = textArea.getSelectedText();
			boolean useSelection = true;
			if (str == null)
			{
				str = textArea.getText();
				useSelection = false;
			}

			if (str != null)
			{
				textArea.beginAtomicEdit();
				try 
				{
					int selStart = textArea.getSelectionStart();
					int selEnd   = textArea.getSelectionEnd();

					try
					{
						str = SqlUtils.format(str);
						if (str != null)
							str = str.replace("\r\n", "\n"); // transform windows newline into Unix/Linux newlines (this works better with RSyntaxtTextArea)

						if (useSelection)
							textArea.replaceSelection(str);
						else
							textArea.setText(str);
					}
					catch (Exception ex)
					{
						UIManager.getLookAndFeel().provideErrorFeedback(textArea);
System.out.println("Problems pretty print SQL. Caught: "+ex);
ex.printStackTrace();
						return;
					}
					
					if (useSelection)
					{
						int newSqlLen = str.length();
						int selectionLen = selEnd - selStart;
						
						selEnd = selEnd + (newSqlLen - selectionLen);
						
						textArea.setSelectionStart(selStart);
						textArea.setSelectionEnd  (selEnd);
					}
				}
				finally 
				{
					textArea.endAtomicEdit(); 
				}
			}
		}

		@Override
		public final String getMacroID()
		{
			return getName();
		}
	}

	/**
	 * ConvertTabsToSpaces
	 */
	@SuppressWarnings("serial")
	public static class ConvertTabsToSpaces extends RecordableTextAction
	{
		protected ConvertTabsToSpaces(String name)
		{
			super(name);
		}

		@Override
		public void actionPerformedImpl(ActionEvent e, RTextArea textArea)
		{
			if (!textArea.isEditable() || !textArea.isEnabled())
			{
				UIManager.getLookAndFeel().provideErrorFeedback(textArea);
				return;
			}

			// Note: This does the WHOLE document
			System.out.println("ConvertTabsToSpaces -- NOT YET IMPLEMENTED");

//			textArea.convertTabsToSpaces();
			
//			String str = textArea.getSelectedText();
//			if (str != null)
//			{
//				textArea.beginAtomicEdit();
//				try 
//				{
//					System.out.println("ConvertTabsToSpaces -- NOT YET IMPLEMENTED");
//
////					int selStart = textArea.getSelectionStart();
////					int selEnd   = textArea.getSelectionEnd();
////
////					str = str.toUpperCase();
////					textArea.replaceSelection(str);
////					
////					textArea.setSelectionStart(selStart);
////					textArea.setSelectionEnd  (selEnd);
//				}
//				finally 
//				{
//					textArea.endAtomicEdit(); 
//				}
//			}
		}

		@Override
		public final String getMacroID()
		{
			return getName();
		}
	}

	/**
	 * ConvertSpacesToTabs
	 */
	@SuppressWarnings("serial")
	public static class ConvertSpacesToTabs extends RecordableTextAction
	{
		protected ConvertSpacesToTabs(String name)
		{
			super(name);
		}

		@Override
		public void actionPerformedImpl(ActionEvent e, RTextArea textArea)
		{
			if (!textArea.isEditable() || !textArea.isEnabled())
			{
				UIManager.getLookAndFeel().provideErrorFeedback(textArea);
				return;
			}

			System.out.println("ConvertSpacesToTabs -- NOT YET IMPLEMENTED");

			// Note: This does the WHOLE document
			textArea.convertSpacesToTabs();
						
//			String str = textArea.getSelectedText();
//			if (str != null)
//			{
//				textArea.beginAtomicEdit();
//				try 
//				{
//					System.out.println("ConvertSpacesToTabs -- NOT YET IMPLEMENTED");
////					int selStart = textArea.getSelectionStart();
////					int selEnd   = textArea.getSelectionEnd();
////
////					str = str.toUpperCase();
////					textArea.replaceSelection(str); 
////					
////					textArea.setSelectionStart(selStart);
////					textArea.setSelectionEnd  (selEnd);
//				}
//				finally 
//				{
//					textArea.endAtomicEdit(); 
//				}
//			}
		}

		@Override
		public final String getMacroID()
		{
			return getName();
		}
	}

	/**
	 * to uppercase
	 */
	@SuppressWarnings("serial")
	public static class ToUpperCaseAction extends RecordableTextAction
	{
		protected ToUpperCaseAction(String name)
		{
			super(name);
		}

		@Override
		public void actionPerformedImpl(ActionEvent e, RTextArea textArea)
		{
			if (!textArea.isEditable() || !textArea.isEnabled())
			{
				UIManager.getLookAndFeel().provideErrorFeedback(textArea);
				return;
			}

			String str = textArea.getSelectedText();
			if (str != null)
			{
				textArea.beginAtomicEdit();
				try 
				{
					int selStart = textArea.getSelectionStart();
					int selEnd   = textArea.getSelectionEnd();

					str = str.toUpperCase();
					textArea.replaceSelection(str); 
					
					textArea.setSelectionStart(selStart);
					textArea.setSelectionEnd  (selEnd);
				}
				finally 
				{
					textArea.endAtomicEdit(); 
				}
			}
		}

		@Override
		public final String getMacroID()
		{
			return getName();
		}
	}

	/**
	 * to lowercase
	 */
	@SuppressWarnings("serial")
	public static class ToLowerCaseAction extends RecordableTextAction
	{
		protected ToLowerCaseAction(String name)
		{
			super(name);
		}

		@Override
		public void actionPerformedImpl(ActionEvent e, RTextArea textArea)
		{
			if (!textArea.isEditable() || !textArea.isEnabled())
			{
				UIManager.getLookAndFeel().provideErrorFeedback(textArea);
				return;
			}

			String str = textArea.getSelectedText();
			if (str != null)
			{
				textArea.beginAtomicEdit();
				try 
				{
					int selStart = textArea.getSelectionStart();
					int selEnd   = textArea.getSelectionEnd();

					str = str.toLowerCase();
					textArea.replaceSelection(str); 
					
					textArea.setSelectionStart(selStart);
					textArea.setSelectionEnd  (selEnd);
				}
				finally 
				{
					textArea.endAtomicEdit(); 
				}
			}
		}

		@Override
		public final String getMacroID()
		{
			return getName();
		}
	}
}
