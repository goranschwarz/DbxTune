/*******************************************************************************
 * Copyright (C) 2010-2019 Goran Schwarz
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
package com.asetune.test;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.text.BadLocationException;

import org.fife.rsta.ui.GoToDialog;
import org.fife.rsta.ui.search.FindDialog;
import org.fife.rsta.ui.search.ReplaceDialog;
import org.fife.rsta.ui.search.SearchEvent;
import org.fife.rsta.ui.search.SearchListener;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.fife.ui.rtextarea.SearchContext;
import org.fife.ui.rtextarea.SearchEngine;
import org.fife.ui.rtextarea.SearchResult;

import com.asetune.utils.StringUtil;

/**
 * An application that demonstrates use of the RSTAUI project. Please don't take
 * this as good application design; it's just a simple example.
 * <p>
 * 
 * Unlike the library itself, this class is public domain.
 * 
 * @author Robert Futrell
 * @version 1.0
 */
public class RSTAUIDemoApp extends JFrame implements ActionListener, SearchListener
{
	private static final long	serialVersionUID	= 1L;

	private RSyntaxTextArea	textArea;
	private FindDialog		findDialog;
	private ReplaceDialog	replaceDialog;


	public RSTAUIDemoApp()
	{

		initSearchDialogs();

		setJMenuBar(createMenuBar());
		JPanel cp = new JPanel(new BorderLayout());
		setContentPane(cp);

		textArea = new RSyntaxTextArea(25, 60);
		textArea.setSyntaxEditingStyle(RSyntaxTextArea.SYNTAX_STYLE_JAVASCRIPT);

//		System.out.println("getActionMap: "+textArea.getActionMap());
//		Object[] am = textArea.getActionMap().allKeys();
//		for (int i=0; i<am.length; i++)
//			System.out.println("getActionMap["+i+"]: "+textArea.getActionMap().get(am[i]));

		System.out.println("getInputMap: "+textArea.getInputMap());
		KeyStroke[] im = textArea.getInputMap().allKeys();
		for (int i=0; i<im.length; i++)
		{
			Object actionKey = textArea.getInputMap().get(im[i]);
			Object actionObj = textArea.getActionMap().get(actionKey);
			KeyStroke kstroke = im[i];
			
			String xxx = 
				"getInputMap[" + i + "]: " +
				"keyStroke "      + StringUtil.left(kstroke  +"", 30, true, "'") + ", " +
				"maps to action " + StringUtil.left(actionKey+"", 30, true, "'") + ", " +
				"action Object: " + actionObj;
			System.out.println(xxx);
			textArea.append(xxx);
			textArea.append("\n");
		}
		
		RTextScrollPane sp = new RTextScrollPane(textArea);
		cp.add(sp);

		setTitle("RSTAUI Demo Application");
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		pack();
		setLocationRelativeTo(null);

	}

	private JMenuBar createMenuBar()
	{
		JMenuBar mb = new JMenuBar();
		JMenu menu = new JMenu("Search");
		menu.add(new JMenuItem(new ShowFindDialogAction()));
		menu.add(new JMenuItem(new ShowReplaceDialogAction()));
		menu.add(new JMenuItem(new GoToLineAction()));
		mb.add(menu);
		return mb;
	}

	/**
	 * Creates our Find and Replace dialogs.
	 */
	public void initSearchDialogs()
	{

		findDialog = new FindDialog(this, this);
		replaceDialog = new ReplaceDialog(this, this);

		// This ties the properties of the two dialogs together (match
		// case, regex, etc.).
		replaceDialog.setSearchContext(findDialog.getSearchContext());

	}

	/**
	 * Listens for events from our search dialogs and actually does the dirty
	 * work.
	 */
	@Override
	public void actionPerformed(ActionEvent e)
	{
		System.out.println("actionPerformed(): e="+e);
		
//		String command = e.getActionCommand();
//		SearchContext context = findDialog.getSearchContext();
//
//		if ( FindDialog.ACTION_FIND.equals(command) )
//		{
//			if ( !SearchEngine.find(textArea, context).wasFound() )
//			{
//				UIManager.getLookAndFeel().provideErrorFeedback(textArea);
//			}
//		}
//		else if ( ReplaceDialog.ACTION_REPLACE.equals(command) )
//		{
//			if ( !SearchEngine.replace(textArea, context).wasFound() )
//			{
//				UIManager.getLookAndFeel().provideErrorFeedback(textArea);
//			}
//		}
//		else if ( ReplaceDialog.ACTION_REPLACE_ALL.equals(command) )
//		{
//			int count = SearchEngine.replaceAll(textArea, context).getCount();
//			JOptionPane.showMessageDialog(null, count + " occurrences replaced.");
//		}

	}

	@Override
	public void searchEvent(SearchEvent e)
	{
		System.out.println("searchEvent(): e="+e);
		SearchContext context = e.getSearchContext();

		SearchResult sr = SearchEngine.find(textArea, context);
		
			
//		if ( FindDialog.ACTION_FIND.equals(command) )
//		{
//			if ( !SearchEngine.find(textArea, context).wasFound() )
//			{
//				UIManager.getLookAndFeel().provideErrorFeedback(textArea);
//			}
//		}
//		else if ( ReplaceDialog.ACTION_REPLACE.equals(command) )
//		{
//			if ( !SearchEngine.replace(textArea, context).wasFound() )
//			{
//				UIManager.getLookAndFeel().provideErrorFeedback(textArea);
//			}
//		}
//		else if ( ReplaceDialog.ACTION_REPLACE_ALL.equals(command) )
//		{
//			int count = SearchEngine.replaceAll(textArea, context).getCount();
//			JOptionPane.showMessageDialog(null, count + " occurrences replaced.");
//		}
	}


	public static void main(String[] args)
	{
		SwingUtilities.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
				new RSTAUIDemoApp().setVisible(true);
			}
		});
	}

	private class GoToLineAction extends AbstractAction
	{
		private static final long	serialVersionUID	= 1L;

		public GoToLineAction()
		{
			super("Go To Line...");
			int c = getToolkit().getMenuShortcutKeyMask();
			putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_L, c));
		}

		@Override
		public void actionPerformed(ActionEvent e)
		{
			if ( findDialog.isVisible() )
			{
				findDialog.setVisible(false);
			}
			if ( replaceDialog.isVisible() )
			{
				replaceDialog.setVisible(false);
			}
			GoToDialog dialog = new GoToDialog(RSTAUIDemoApp.this);
			dialog.setMaxLineNumberAllowed(textArea.getLineCount());
			dialog.setVisible(true);
			int line = dialog.getLineNumber();
			if ( line > 0 )
			{
				try
				{
					textArea.setCaretPosition(textArea.getLineStartOffset(line - 1));
				}
				catch (BadLocationException ble)
				{ // Never happens
					UIManager.getLookAndFeel().provideErrorFeedback(textArea);
					ble.printStackTrace();
				}
			}
		}

	}

	private class ShowFindDialogAction extends AbstractAction
	{
		private static final long	serialVersionUID	= 1L;

		public ShowFindDialogAction()
		{
			super("Find...");
			int c = getToolkit().getMenuShortcutKeyMask();
			putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_F, c));
		}

		@Override
		public void actionPerformed(ActionEvent e)
		{
			if ( replaceDialog.isVisible() )
			{
				replaceDialog.setVisible(false);
			}
			findDialog.setVisible(true);
		}

	}

	private class ShowReplaceDialogAction extends AbstractAction
	{
		private static final long	serialVersionUID	= 1L;

		public ShowReplaceDialogAction()
		{
			super("Replace...");
			int c = getToolkit().getMenuShortcutKeyMask();
			putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_H, c));
		}

		@Override
		public void actionPerformed(ActionEvent e)
		{
			if ( findDialog.isVisible() )
			{
				findDialog.setVisible(false);
			}
			replaceDialog.setVisible(true);
		}

	}

	@Override
	public String getSelectedText()
	{
		return null;
	}
}
