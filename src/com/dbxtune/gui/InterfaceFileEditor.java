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
package com.dbxtune.gui;

import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.invoke.MethodHandles;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fife.ui.autocomplete.AutoCompletion;
import org.fife.ui.autocomplete.CompletionProvider;
import org.fife.ui.autocomplete.DefaultCompletionProvider;
import org.fife.ui.autocomplete.ShorthandCompletion;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.fife.ui.rtextarea.SearchContext;
import org.fife.ui.rtextarea.SearchEngine;
import org.fife.ui.rtextarea.SearchResult;

import com.dbxtune.ui.rsyntaxtextarea.RSyntaxTextAreaX;
import com.dbxtune.ui.rsyntaxtextarea.RSyntaxUtilitiesX;
import com.dbxtune.utils.FileUtils;
import com.dbxtune.utils.PlatformUtils;
import com.dbxtune.utils.StringUtil;
import com.dbxtune.utils.SwingUtils;

import net.miginfocom.swing.MigLayout;

public class InterfaceFileEditor
extends JDialog
implements ActionListener, DocumentListener
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static final long serialVersionUID = 1L;

	public static final int	OK	    = 0;
	public static final int	CANCEL	= 1;

	private int _retStatus = CANCEL;

	public static final String ACTION_OK     = "OK";
	public static final String ACTION_CANCEL = "CANCEL";

	
	private String _filename = null;
	private String _winTitle = "Interface File Editor";

	private String _currentSrv  = "";
	private String _lastEditSrv = null;
	
	private boolean _changed = false;
	private int     _originTextHasCode = 0;

	private RSyntaxTextAreaX _textArea   = new RSyntaxTextAreaX();
	private RTextScrollPane  _textScroll = new RTextScrollPane(_textArea);

	private JButton _ok      = new JButton("Save & Close");
	private JButton _cancel  = new JButton("Cancel");
	
	public InterfaceFileEditor(Window owner)
	{
		super(owner);
		init();
	}

	public InterfaceFileEditor(Window owner, String ifile, String currentServer)
	{
		super(owner);
		init();
		setFilename(ifile);
		_currentSrv = currentServer;
	}

	private void init()
	{
		JPanel panel = new JPanel(new MigLayout());

		setTitle(_winTitle);
		setModal(true);

		_textArea.setToolTipText("Use Ctrl+Space to get Completion");
		_ok      .setToolTipText("Save Changes to file and close the window");
		_cancel  .setToolTipText("Discard Changes and close the window");
		
		_textArea.setRows(30);
		_textArea.setColumns(60);
		_textArea.getDocument().addDocumentListener(this);

		RSyntaxUtilitiesX.installRightClickMenuExtentions(_textScroll, this);

		panel.add(_textScroll, "push, grow, wrap");
		panel.add(_ok,         "split, tag ok");
		panel.add(_cancel,     "tag cancel");

		// Setup Auto-Completion for SQL
		CompletionProvider acProvider = createCompletionProvider();
		AutoCompletion ac = new AutoCompletion(acProvider);
		ac.install(_textArea);

		_textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);

		setContentPane(panel);

		// ADD Ctrl-s (as save)
		_textArea.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK), "save");

		_textArea.getActionMap().put("save", new AbstractAction("save")
		{
			private static final long	serialVersionUID	= 1L;
			@Override
			public void actionPerformed(ActionEvent e)
			{
				save();
			}
		});

		_ok    .addActionListener(this);
		_cancel.addActionListener(this);

		_ok    .setActionCommand(ACTION_OK);
		_cancel.setActionCommand(ACTION_CANCEL);

		pack();
		setLocationRelativeTo(null);
	}

	/**
	 * Create a simple provider that adds some SQL completions.
	 *
	 * @return The completion provider.
	 */
	private CompletionProvider createCompletionProvider()
	{
		// A DefaultCompletionProvider is the simplest concrete implementation
		// of CompletionProvider. This provider has no understanding of
		// language semantics. It simply checks the text entered up to the
		// caret position for a match against known completions. This is all
		// that is needed in the majority of cases.
		DefaultCompletionProvider provider = new DefaultCompletionProvider();

		// Add completions for all SQL keywords. A BasicCompletion is just a straightforward word completion.
		//provider.addCompletion(new BasicCompletion(provider, "SELECT * FROM "));

		// Add a couple of "shorthand" completions. These completions don't
		// require the input text to be the same thing as the replacement text.
		if ( PlatformUtils.getCurrentPlattform() == PlatformUtils.Platform_WIN )
		{
			provider.addCompletion(new ShorthandCompletion(provider, "1", "[SRVNAME]\nquery=TCP, hostname, portnum\n",                                          "Full Server entry, with query row"));
			provider.addCompletion(new ShorthandCompletion(provider, "2", "[SRVNAME]\nquery=TCP, hostname, portnum\nmaster=TCP, hostname, portnum\n",           "Full Server entry, with query and master row"));
			provider.addCompletion(new ShorthandCompletion(provider, "3", "query=TCP, hostname, portnum\n",                                                     "query row"));
			provider.addCompletion(new ShorthandCompletion(provider, "4", "master=TCP, hostname, portnum\n",                                                    "master row"));
		}
		else
		{
			provider.addCompletion(new ShorthandCompletion(provider, "1", "SRVNAME\n\tquery tcp ether hostname portnum\n",                                      "Full Server entry, with query row"));
			provider.addCompletion(new ShorthandCompletion(provider, "2", "SRVNAME\n\tquery tcp ether hostname portnum\n\tmaster tcp ether hostname portnum\n", "Full Server entry, with query and master row"));
			provider.addCompletion(new ShorthandCompletion(provider, "3", "\tquery tcp ether hostname portnum\n",                                               "query row"));
			provider.addCompletion(new ShorthandCompletion(provider, "4", "\tmaster tcp ether hostname portnum\n",                                              "master row"));
		}

		return provider;
	}

	/**
	 * implements ActionListener
	 */
	@Override
	public void actionPerformed(ActionEvent e)
	{
		//Object source    = e.getSource();
		String actionCmd = e.getActionCommand();

		_logger.debug("ACTION '"+actionCmd+"'.");

		if (ACTION_OK.equals(actionCmd))
		{
			boolean ok = save();
			if (ok)
			{
				_retStatus = OK;
				setVisible(false);
			}
		}

		if (ACTION_CANCEL.equals(actionCmd))
		{
			setVisible(false);
		}
	}

	/**
	 * implements ActionListener
	 */
	@Override
	public void insertUpdate(DocumentEvent e)
	{
		checkChangedStatus();
	}

	@Override
	public void removeUpdate(DocumentEvent e)
	{
		checkChangedStatus();
	}

	@Override
	public void changedUpdate(DocumentEvent e)
	{
		whatEntryWasEdit(e);
		checkChangedStatus();
	}

	private void whatEntryWasEdit(DocumentEvent e)
	{
		// NOTE: this isn't pretty... might not even work on Mac (if it still uses \r as line break)
		// and it will use extra memory...
		String[] lines = _textArea.getText().split("\n");
		
		for (int lineNum=_textArea.getCaretLineNumber(); lineNum>=0; lineNum--)
		{
			String strAtLine = lines[lineNum];

			// If empty line, get out of here
			if (StringUtil.isNullOrBlank(strAtLine))
				break;
			
			if (PlatformUtils.getCurrentPlattform() == PlatformUtils.Platform_WIN)
			{
//				if (strAtLine.matches("^[[].*[]].*"))
				if ( strAtLine.startsWith("[") && strAtLine.indexOf("]") >= 0 )
				{
					_lastEditSrv = strAtLine.substring(strAtLine.indexOf("[")+1, strAtLine.indexOf("]")).trim();
					break;
				}
			}
			else
			{
				if (strAtLine.matches("^query="))  continue; // this isn't really needed in here, it's just if the platform check fails
				if (strAtLine.matches("^master=")) continue; // this isn't really needed in here, it's just if the platform check fails
				if (strAtLine.matches("^[A-Za-z0-9].*"))
				{
					int endPos = strAtLine.indexOf(" ");
					if (endPos == -1)
						endPos = strAtLine.length();
					_lastEditSrv = strAtLine.substring(0, endPos).trim();
					break;
				}
			}
		}
	}

	private void checkChangedStatus()
	{
		//_changed = _textArea.canUndo();
		_changed = (_textArea.getText().hashCode() != _originTextHasCode);
		_ok.setEnabled(_changed);
	}
	private void resetChangedStatus(boolean resetUndoBuffer)
	{
		if (resetUndoBuffer)
			_textArea.discardAllEdits();

		_originTextHasCode = _textArea.getText().hashCode();
		_changed = false;
		_ok.setEnabled(_changed);
	}
	public boolean hasChanged()
	{
		return _changed;
	}

	/**
	 * Set what filename to use, you still have to call open() to read the file.
	 * @param ifile
	 */
	public void setFilename(String ifile)
	{
		_filename = ifile;
		setTitle(_winTitle + " : " + _filename);
	}

	/**
	 * Save the filename
	 * @return true if we successfully saved the file.
	 */
	public boolean save()
	{
		try
		{
			File f = new File(_filename);
			FileWriter writer = new FileWriter(f);
			_textArea.write(writer); // Use TextComponent write

			resetChangedStatus(false);

			return true;
		}
		catch (IOException ioex)
		{
			SwingUtils.showErrorMessage("Save File", "<html>Problems Saving file '"+_filename+"'.<br><br><b>"+ioex+"</b></html>", ioex);
			return false;
		}
	}

	/** get filename we are currently editing */
	public String getFilename()
	{
		return _filename;
	}

	/** Open current file */
	public int open()
	{
		return open(_filename);
	}

	/** Open file */
	public int open(String filename)
	{
		setFilename(filename);
		try 
		{
			_textArea.getDocument().removeDocumentListener(this);

			File f = new File(_filename);
			FileReader reader = new FileReader(f);
			_textArea.read(reader, "");  // Use TextComponent read

			if ( ! FileUtils.canWrite(filename) )
			{
				String htmlStr = "<html>" +
					"<h3>Warning</h3>" +
					"Name service file '"+filename+"' is <b>read only</b><br>" +
					"So adding/changing entries will be impossible!<br>" +
					"</html>";
				SwingUtils.showWarnMessage(null, "read only", htmlStr, null);
			}

			resetChangedStatus(true);
			markCurrentServer();
		}
		catch (IOException ioex) 
		{
			SwingUtils.showErrorMessage("Open File", "Problems Open file '"+_filename+"'.", ioex);
		}
		_textArea.getDocument().addDocumentListener(this);


		setVisible(true);

		return _retStatus;
	}
	
	/**
	 * What entry did was the last one user was "close" to... (cursor was close to this entry)
	 * @return
	 */
	public String getLastEditServerEntry()
	{
		return _lastEditSrv;		
	}

	public void markCurrentServer()
	{
		if ( ! StringUtil.isNullOrBlank(_currentSrv) )
		{
			String searchFor = _currentSrv;
			if (PlatformUtils.getCurrentPlattform() == PlatformUtils.Platform_WIN)
				searchFor = "[" + _currentSrv + "]";

			// Mark server
//			_textArea.markAll( searchFor, true, true, false);
			
			// Position at start of the editor
			_textArea.setCaretPosition(0);

			// Create an object defining our search parameters.
			SearchContext context = new SearchContext();

			context.setSearchFor(searchFor);
			context.setMatchCase(true);
			context.setRegularExpression(false);
			context.setSearchForward(true);
			context.setWholeWord(true);
			context.setMarkAll(true);

//			boolean found = SearchEngine.find(_textArea, context);
//			if ( !found )
			SearchResult found = SearchEngine.find(_textArea, context);
			if ( !found.wasFound() )
			{
				Runnable doLater = new Runnable()
				{
					@Override
					public void run()
					{
						JOptionPane.showMessageDialog(null, "Server '"+_currentSrv+"' not found.");
					}
				};
				SwingUtilities.invokeLater(doLater);
			}
		}
	}
}
