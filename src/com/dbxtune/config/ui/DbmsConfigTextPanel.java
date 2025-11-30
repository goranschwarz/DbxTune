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
package com.dbxtune.config.ui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Timestamp;
import java.util.List;

import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import org.fife.ui.rtextarea.RTextScrollPane;

import com.dbxtune.CounterController;
import com.dbxtune.DbxTune;
import com.dbxtune.config.dbms.IDbmsConfigText;
import com.dbxtune.gui.ModelMissmatchException;
import com.dbxtune.gui.ResultSetTableModel;
import com.dbxtune.gui.TableModelViewDialog;
import com.dbxtune.hostmon.HostMonitorConnection;
import com.dbxtune.pcs.PersistReader;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.ui.rsyntaxtextarea.RSyntaxTextAreaX;
import com.dbxtune.utils.ConnectionProvider;
import com.dbxtune.utils.StringUtil;
import com.dbxtune.utils.SwingUtils;


public class DbmsConfigTextPanel
extends JPanel
{
	private static final long serialVersionUID = 1L;

//	private ConfigType   _type                = null;
	private String      _name                 = null;
	private IDbmsConfigText _dbmsConfigText   = null;
//	private JTextArea	_textConfig           = new JTextArea();
//	private JScrollPane _textConfigScroll     = new JScrollPane(_textConfig);

//	private RSyntaxTextArea _textConfig       = new RSyntaxTextArea();
	private RSyntaxTextAreaX _textConfig      = new RSyntaxTextAreaX();
	private RTextScrollPane _textConfigScroll = new RTextScrollPane(_textConfig, true);

	private ConnectionProvider     _connProvider    = null;

//	public DbmsConfigTextPanel(ConnectionProvider connProvider, ConfigType type)
//	{
//		_connProvider = connProvider;
//		_type = type;
//
//		setLayout( new BorderLayout() );
//		add(_textConfigScroll, BorderLayout.CENTER);
//		
//		init();
//	}

	public DbmsConfigTextPanel(DbmsConfigViewDialog connProvider, IDbmsConfigText dbmsConfigText)
	{
		_connProvider   = connProvider;
		_dbmsConfigText = dbmsConfigText;
		_name           = dbmsConfigText.getName();

		setLayout( new BorderLayout() );
		add(_textConfigScroll, BorderLayout.CENTER);
		
		init();
	}

	public void refresh()
	throws Exception
	{
		Timestamp     ts        = null;
		boolean       hasGui    = DbxTune.hasGui();
		boolean       isOffline = false;
		DbxConnection conn      = null;
		HostMonitorConnection hostMonConn = null;

//		if (GetCounters.getInstance().isMonConnected())
//		{
//			ts        = null;
//			isOffline = false;
//			conn      = GetCounters.getInstance().getMonConnection();
//		}
//		else
//		{
//			ts        = null; // NOTE: this will not work, get the value from somewhere
//			isOffline = true;
//			conn      = PersistReader.getInstance().getConnection();
//		}

		conn = _connProvider.getConnection();
		if (CounterController.hasInstance())
			hostMonConn = CounterController.getInstance().getHostMonConnection(); 

		if (PersistReader.hasInstance())
		{
			if (PersistReader.getInstance().isConnected())
			{
				ts        = null; // NOTE: this will not work, get the value from somewhere
				isOffline = true;
			}
		}
		
//		AseConfigText aseConfigText = AseConfigText.getInstance(_type);
////		aseConfigText.refresh(conn, ts);
//		aseConfigText.initialize(conn, hasGui, isOffline, ts);
		_dbmsConfigText.initialize(conn, hostMonConn, hasGui, isOffline, ts);

		// refresh when the configuration was taken.
//		_textConfig.setText( aseConfigText.getConfig() );
		_textConfig.setText( _dbmsConfigText.getConfig() );


		// and set input to "top" so it's a bit more readable if it's a long text
		_textConfig.setCaretPosition(0);
	}

	/**
	 * Translate the selected text into GUI Tables
	 * <p>
	 * If there are more than one table. The one with the most columns and rows will be choosen!
	 * <p>
	 * If there are many table (with the same column names), All will be "merged" into a single table.
	 * 
	 * @param guessDataTypes If we want to parse the input to find numbers.
	 */
	private void textToJTable(boolean guessDataTypes)
	{
		String selectedText = _textConfig.getSelectedText();
		if (StringUtil.isNullOrBlank(selectedText))
		{
			SwingUtils.showInfoMessage(_textConfig, "No Input", "Select some text that you want to translate into a GUI Table.");
			return;
		}
		else
		{
			List<ResultSetTableModel> rstmList = ResultSetTableModel.parseTextTables(selectedText);
			if (rstmList.isEmpty())
			{
				SwingUtils.showInfoMessage(_textConfig, "No Input", "The selected text did not look like a 'text table'.");
				return;
			}
			else
			{
				ResultSetTableModel choosenRstm = rstmList.get(0);
				
				// If there are several tables in the selected text... choose the one with most COLUMNS * ROWS
				if (rstmList.size() > 1)
				{
					boolean isMergable = true;
					
					for (ResultSetTableModel rstm : rstmList)
					{
						// If all columns are the *same*, then we might be able to "merge" all tables into 1
						if ( ! choosenRstm.getColumnNames().equals(rstm.getColumnNames()) )
							isMergable = false;

						int choosenWaight = choosenRstm.getColumnCount() * choosenRstm.getRowCount();
						int thisWaight    = rstm       .getColumnCount() * rstm       .getRowCount();
						if (thisWaight > choosenWaight)
							choosenRstm = rstm;
					}
					
					if (isMergable)
					{
						// Copy all columns, but no rows
						choosenRstm = new ResultSetTableModel(choosenRstm, "Merged Table", false);
						
						// Copy data
						for (ResultSetTableModel rstm : rstmList)
						{
							try 
							{
								choosenRstm.add(rstm);
							} 
							catch (ModelMissmatchException ex) 
							{
								ex.printStackTrace();
							}
						}
					}
				}

				// The below do NOT seems to work, we get class cast exception from the JTable when sorting...
				// So I need to work on this
				// Transform Columns with Numbers to REAL Numbers
				if (guessDataTypes)
				{
					try 
					{
						choosenRstm.guessDatatypes();
					} 
					catch (Exception ignore) 
					{
						ignore.printStackTrace();
					}
				}
				
				// Show a Table Viewer
				TableModelViewDialog.showDialog(_textConfig, choosenRstm);
			}
		}
	}

	private void init()
	{
//		AseConfigText aseConfigText = AseConfigText.getInstance(_type);
//		if ( aseConfigText.isInitialized() )
//		{
//			_textConfig.setText( aseConfigText.getConfig() );
//
//			// and set input to "top" so it's a bit more readable if it's a long text
//			_textConfig.setCaretPosition(0);
//		}

		// Add menu entry "Selected text to JTable"
		final JMenuItem toJTableNumbers_mi = new JMenuItem("Selected text to JTable - Guess Types");
		toJTableNumbers_mi.setToolTipText("<html>This tries to parse the strings to find numbers, so that sorting is better.<br>But in some cases the <i>Guess</i> is not correct.<br>Then use the 'All Strings' option instead.</html>");
		toJTableNumbers_mi.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				textToJTable(true);
			}
		});
		
		// Add menu entry "Selected text to JTable"
		final JMenuItem toJTableStrings_mi = new JMenuItem("Selected text to JTable - All Strings");
		toJTableStrings_mi.setToolTipText("<html>All fields will be treated as strings...<br>This means that sorting the columns might not be what you want...<br>The sorting is done on strings, hence not correct for number columns.</html>");
		toJTableStrings_mi.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				textToJTable(false);
			}
		});

		JPopupMenu popupMenu = _textConfig.getPopupMenu();
		popupMenu.addSeparator();
		popupMenu.add(toJTableNumbers_mi);
		popupMenu.add(toJTableStrings_mi);
		

		if ( _dbmsConfigText.isInitialized() )
		{
			_textConfig.setSyntaxEditingStyle(_dbmsConfigText.getSyntaxEditingStyle());

			_textConfig.setText( _dbmsConfigText.getConfig() );

			// and set input to "top" so it's a bit more readable if it's a long text
			_textConfig.setCaretPosition(0);
		}
		else
		{
			// NOTE: if offline and we are reading an "older" database which didn't have the configs
			//       the below String will still show...
			//       I think this is a small issue, which I'm not fixing for the moment...
			_textConfig.setText( "Not yet Initialized, please connect first." );
		}
	}
}
