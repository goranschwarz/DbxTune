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

/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */

package com.dbxtune.tools.sqlw;

import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.dbxtune.Version;
import com.dbxtune.gui.swing.GCheckBox;
import com.dbxtune.gui.swing.GLabel;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.StringUtil;
import com.dbxtune.utils.SwingUtils;
import com.dbxtune.xmenu.SQLWindow;

import net.miginfocom.swing.MigLayout;

public class CrudDmlConfigDialog
	extends JDialog
	implements ActionListener
{
	private static final long serialVersionUID = 1L;

	private Map<String, String>     _outputMap      = null; 
//	private Map<String, JTextField> _componentMap   = null; 

	private GLabel      _description         = new GLabel("<html>"
			+ "Various settings for how to Generate DML (Data Modification Language)<br>"
			+ "from Selected rows into the clipboard or other destination.<br>"
			+ "This so you can do Ctrl-V and then execute the Statements in another connection."
			+ "</html>");

	private GCheckBox   _replaceNewlines_chk     = new GCheckBox("Replace newlines with");
	private JTextField  _replaceNewlines_txt     = new JTextField(15);

	private GCheckBox   _stmntTerminator_chk     = new GCheckBox("Add Statement Terminator");
	private JTextField  _stmntTerminator_txt     = new JTextField(15);

	private GCheckBox   _replaceNullValues_chk   = new GCheckBox("Replace NULL values with");
	private JTextField  _replaceNullValues_txt   = new JTextField(15);

	private GCheckBox   _addQuotedIdent_chk      = new GCheckBox("Add Quoted Identifiers");
	private JTextField  _addQuotedIdentBegin_txt = new JTextField(3);
	private JTextField  _addQuotedIdentEnd_txt   = new JTextField(3);

	private GCheckBox   _includeDbname_chk       = new GCheckBox("Include Database Name");
	private GCheckBox   _includeSchema_chk       = new GCheckBox("Include Schema Name");

	private JButton     _ok             = new JButton("OK");
	private JButton     _cancel         = new JButton("Cancel");
	private JButton     _apply          = new JButton("Apply");
	private JButton     _toDefault      = new JButton("To Defaults");
	private boolean     _showApply      = true;

	private CrudDmlConfigDialog(Window owner, String title, boolean showApply)
	{
		super(owner, title);
		setModal(true);
		_showApply = showApply;
		initComponents();
		pack();
		
		// Focus to 'OK', escape to 'CANCEL'
		SwingUtils.installEscapeButton(this, _cancel);
		SwingUtils.setFocus(_ok);
	}


	public static Map<String, String> showDialog(Window owner)
	{
		CrudDmlConfigDialog params = new CrudDmlConfigDialog(owner, "DML Config", false);
		params.setLocationRelativeTo(owner);
		params.setVisible(true);
		params.dispose();

		return params._outputMap;
	}

	/*---------------------------------------------------
	** BEGIN: component initialization
	**---------------------------------------------------
	*/
	protected void initComponents() 
	{
		JPanel panel = new JPanel();
		panel.setLayout(new MigLayout("insets 20 20","[][grow]",""));   // insets Top Left Bottom Right

		String description_tooltip = 
				"<html>"
				+ "</html>";
				
		// Some tool tip
		_description        .setToolTipText(description_tooltip);

		_replaceNewlines_chk.setToolTipText("<html>Replace newlines (within String Values) with a real newline character or some other string</html>");
		_replaceNewlines_txt.setToolTipText(_replaceNewlines_chk.getToolTipText());

		_stmntTerminator_chk.setToolTipText("<html>Character to use for terminating a row, typically '\\n' or '\\r\\n'</html>");
		_stmntTerminator_txt.setToolTipText(_stmntTerminator_chk.getToolTipText());

		_replaceNullValues_chk      .setToolTipText("<html>If data in the table is 'NULL' then write this string instead of NULL</html>");
		_replaceNullValues_txt      .setToolTipText(_replaceNullValues_chk.getToolTipText());

		_addQuotedIdent_chk         .setToolTipText("<html>Add Quoted Identifier to db/schema/table and column names<br>"
				                                         + "<br>"
				                                         + "If you are using " + Version.getAppName() + " the most portable way is to:"
				                                         + "<ul>"
				                                         + "  <li>Use the square bracket quoting: [name]</li>"
				                                         + "  <li>And when you execute it, the <i>brackets</i> will be replaced by the DBMS specific Quoted Identifyer Chars</li>"
				                                         + "</ul>"
				                                         + "</html>");
		_addQuotedIdentBegin_txt    .setToolTipText(_addQuotedIdent_chk.getToolTipText());
		_addQuotedIdentEnd_txt      .setToolTipText(_addQuotedIdent_chk.getToolTipText());

		_includeDbname_chk      .setToolTipText("<html>Include Database name when generating the DML Text</html>");
		_includeSchema_chk      .setToolTipText("<html>Include Schema name when generating the DML Text</html>");
		
		_toDefault                  .setToolTipText("Set all fields to default values");

		// Listen for "return" and any other "key pressing"
		// This simply enables/disable the "apply" button if anything was changed.
		panel.add(_description            , "span, wrap 20");

		panel.add(_replaceNewlines_chk    , "");
		panel.add(_replaceNewlines_txt    , "wrap");

		panel.add(_stmntTerminator_chk    , "");
		panel.add(_stmntTerminator_txt    , "wrap");
		
		panel.add(_replaceNullValues_chk  , "");
		panel.add(_replaceNullValues_txt  , "wrap");
		
		panel.add(_addQuotedIdent_chk     , "");
		panel.add(_addQuotedIdentBegin_txt, "split");
		panel.add(_addQuotedIdentEnd_txt  , "wrap");

		panel.add(_includeDbname_chk      , "wrap");
		panel.add(_includeSchema_chk      , "wrap");
		
//		panel.add(_toDefault              , "wrap");
		
		setStartValues();
		checkForChanges();
		
		_replaceNewlines_chk.addActionListener(_actionListener);
		_replaceNewlines_txt.addActionListener(_actionListener);
		_replaceNewlines_txt.addKeyListener   (_keyListener);
		
		_stmntTerminator_chk.addActionListener(_actionListener);
		_stmntTerminator_txt.addActionListener(_actionListener);
		_stmntTerminator_txt.addKeyListener   (_keyListener);
		
		_replaceNullValues_chk.addActionListener(_actionListener);
		_replaceNullValues_txt.addActionListener(_actionListener);
		_replaceNullValues_txt.addKeyListener   (_keyListener);

		_addQuotedIdent_chk     .addActionListener(_actionListener);
		_addQuotedIdentBegin_txt.addActionListener(_actionListener);
		_addQuotedIdentBegin_txt.addKeyListener   (_keyListener);
		_addQuotedIdentEnd_txt  .addActionListener(_actionListener);
		_addQuotedIdentEnd_txt  .addKeyListener   (_keyListener);

		_includeDbname_chk      .addActionListener(_actionListener);
		_includeSchema_chk      .addActionListener(_actionListener);
		
		// ADD the OK, Cancel, Apply buttons
		panel.add(new JLabel(), "wrap 20");
		
		panel.add(_toDefault,   "");
		panel.add(new JLabel(), "split, pushx, growx");
		panel.add(_ok,          "tag ok");
		panel.add(_cancel,      "tag cancel");
		if (_showApply)
		panel.add(_apply,       "tag apply");

		// Initial state for buttons
		_apply.setEnabled(false);
		
		setContentPane(panel);

		// ADD ACTIONS TO COMPONENTS
		_ok           .addActionListener(this);
		_cancel       .addActionListener(this);
		_apply        .addActionListener(this);
		_toDefault    .addActionListener(this);
	}
	
	private void setStartValues()
	{
		boolean doReplaceNewline   = Configuration.getCombinedConfiguration().getBooleanProperty(ResultSetJXTable.PROPKEY_TABLE_GENERATE_ROWS_TO_SQL_NL_REPLACE      , ResultSetJXTable.DEFAULT_TABLE_GENERATE_ROWS_TO_SQL_NL_REPLACE);
		String  replaceNewlineStr  = Configuration.getCombinedConfiguration().getProperty       (ResultSetJXTable.PROPKEY_TABLE_GENERATE_ROWS_TO_SQL_NL_REPLACE_STR  , ResultSetJXTable.DEFAULT_TABLE_GENERATE_ROWS_TO_SQL_NL_REPLACE_STR);

		boolean doStmntTerminator  = Configuration.getCombinedConfiguration().getBooleanProperty(ResultSetJXTable.PROPKEY_TABLE_GENERATE_ROWS_TO_SQL_STMNT_TERM      , ResultSetJXTable.DEFAULT_TABLE_GENERATE_ROWS_TO_SQL_STMNT_TERM);
		String  stmntTerminatorStr = Configuration.getCombinedConfiguration().getProperty       (ResultSetJXTable.PROPKEY_TABLE_GENERATE_ROWS_TO_SQL_STMNT_TERM_STR  , ResultSetJXTable.DEFAULT_TABLE_GENERATE_ROWS_TO_SQL_STMNT_TERM_STR);

		boolean doNullReplace      = Configuration.getCombinedConfiguration().getBooleanProperty(ResultSetJXTable.PROPKEY_TABLE_GENERATE_ROWS_TO_SQL_NULL_REPLACE    , ResultSetJXTable.DEFAULT_TABLE_GENERATE_ROWS_TO_SQL_NULL_REPLACE);
		String  nullReplaceStr     = Configuration.getCombinedConfiguration().getProperty       (ResultSetJXTable.PROPKEY_TABLE_GENERATE_ROWS_TO_SQL_NULL_REPLACE_STR, ResultSetJXTable.DEFAULT_TABLE_GENERATE_ROWS_TO_SQL_NULL_REPLACE_STR);

		boolean addQuotedIdent     = Configuration.getCombinedConfiguration().getBooleanProperty(ResultSetJXTable.PROPKEY_TABLE_GENERATE_ROWS_TO_SQL_ADD_QI          , ResultSetJXTable.DEFAULT_TABLE_GENERATE_ROWS_TO_SQL_ADD_QI);
		String  qiBeginStr         = Configuration.getCombinedConfiguration().getProperty       (ResultSetJXTable.PROPKEY_TABLE_GENERATE_ROWS_TO_SQL_ADD_QI_BEGIN_STR, ResultSetJXTable.DEFAULT_TABLE_GENERATE_ROWS_TO_SQL_ADD_QI_BEGIN_STR);
		String  qiEndStr           = Configuration.getCombinedConfiguration().getProperty       (ResultSetJXTable.PROPKEY_TABLE_GENERATE_ROWS_TO_SQL_ADD_QI_END_STR  , ResultSetJXTable.DEFAULT_TABLE_GENERATE_ROWS_TO_SQL_ADD_QI_END_STR);

		boolean includeDbname      = Configuration.getCombinedConfiguration().getBooleanProperty(ResultSetJXTable.PROPKEY_TABLE_GENERATE_ROWS_TO_SQL_INCLUDE_DBNAME  , ResultSetJXTable.DEFAULT_TABLE_GENERATE_ROWS_TO_SQL_INCLUDE_DBNAME);
		boolean includeSchema      = Configuration.getCombinedConfiguration().getBooleanProperty(ResultSetJXTable.PROPKEY_TABLE_GENERATE_ROWS_TO_SQL_INCLUDE_SCHEMA  , ResultSetJXTable.DEFAULT_TABLE_GENERATE_ROWS_TO_SQL_INCLUDE_SCHEMA);


		_replaceNewlines_chk     .setSelected(doReplaceNewline);
		_replaceNewlines_txt     .setText(StringUtil.escapeControlChars(replaceNewlineStr));
		
		_stmntTerminator_chk     .setSelected(doStmntTerminator);
		_stmntTerminator_txt     .setText(StringUtil.escapeControlChars(stmntTerminatorStr));

		_replaceNullValues_chk   .setSelected(doNullReplace);
		_replaceNullValues_txt   .setText(StringUtil.escapeControlChars(nullReplaceStr));

		_addQuotedIdent_chk      .setSelected(addQuotedIdent);
		_addQuotedIdentBegin_txt .setText(StringUtil.escapeControlChars(qiBeginStr));
		_addQuotedIdentEnd_txt   .setText(StringUtil.escapeControlChars(qiEndStr));

		_includeDbname_chk       .setSelected(includeDbname);
		_includeSchema_chk       .setSelected(includeSchema);
	}
	/*---------------------------------------------------
	** END: component initialization
	**---------------------------------------------------
	*/

	
	private void apply()
	{
		_apply.setEnabled(false);

		final Configuration tmpConf = Configuration.getInstance(Configuration.USER_TEMP);

		tmpConf.setProperty(ResultSetJXTable.PROPKEY_TABLE_GENERATE_ROWS_TO_SQL_NL_REPLACE      , _replaceNewlines_chk.isSelected());
		tmpConf.setProperty(ResultSetJXTable.PROPKEY_TABLE_GENERATE_ROWS_TO_SQL_NL_REPLACE_STR  , _replaceNewlines_txt.getText());

		tmpConf.setProperty(ResultSetJXTable.PROPKEY_TABLE_GENERATE_ROWS_TO_SQL_STMNT_TERM      , _stmntTerminator_chk.isSelected());
		tmpConf.setProperty(ResultSetJXTable.PROPKEY_TABLE_GENERATE_ROWS_TO_SQL_STMNT_TERM_STR  , _stmntTerminator_txt.getText());

		tmpConf.setProperty(ResultSetJXTable.PROPKEY_TABLE_GENERATE_ROWS_TO_SQL_NULL_REPLACE    , _replaceNullValues_chk.isSelected());
		tmpConf.setProperty(ResultSetJXTable.PROPKEY_TABLE_GENERATE_ROWS_TO_SQL_NULL_REPLACE_STR, _replaceNullValues_txt.getText());

		tmpConf.setProperty(ResultSetJXTable.PROPKEY_TABLE_GENERATE_ROWS_TO_SQL_ADD_QI          , _addQuotedIdent_chk.isSelected());
		tmpConf.setProperty(ResultSetJXTable.PROPKEY_TABLE_GENERATE_ROWS_TO_SQL_ADD_QI_BEGIN_STR, _addQuotedIdentBegin_txt.getText());
		tmpConf.setProperty(ResultSetJXTable.PROPKEY_TABLE_GENERATE_ROWS_TO_SQL_ADD_QI_END_STR  , _addQuotedIdentEnd_txt  .getText());

		tmpConf.setProperty(ResultSetJXTable.PROPKEY_TABLE_GENERATE_ROWS_TO_SQL_INCLUDE_DBNAME  , _includeDbname_chk  .isSelected());
		tmpConf.setProperty(ResultSetJXTable.PROPKEY_TABLE_GENERATE_ROWS_TO_SQL_INCLUDE_SCHEMA  , _includeSchema_chk  .isSelected());

		tmpConf.save();
	}
	
	private void setDefaults()
	{
		_replaceNewlines_chk.setSelected  (ResultSetJXTable.DEFAULT_TABLE_GENERATE_ROWS_TO_SQL_NL_REPLACE);
		_replaceNewlines_txt.setText      (ResultSetJXTable.DEFAULT_TABLE_GENERATE_ROWS_TO_SQL_NL_REPLACE_STR);

		_stmntTerminator_chk.setSelected  (ResultSetJXTable.DEFAULT_TABLE_GENERATE_ROWS_TO_SQL_STMNT_TERM);
		_stmntTerminator_txt.setText      (ResultSetJXTable.DEFAULT_TABLE_GENERATE_ROWS_TO_SQL_STMNT_TERM_STR);

		_replaceNullValues_chk.setSelected(ResultSetJXTable.DEFAULT_TABLE_GENERATE_ROWS_TO_SQL_NULL_REPLACE);
		_replaceNullValues_txt.setText    (ResultSetJXTable.DEFAULT_TABLE_GENERATE_ROWS_TO_SQL_NULL_REPLACE_STR);

		_addQuotedIdent_chk.setSelected   (ResultSetJXTable.DEFAULT_TABLE_GENERATE_ROWS_TO_SQL_ADD_QI);
		_addQuotedIdentBegin_txt.setText  (ResultSetJXTable.DEFAULT_TABLE_GENERATE_ROWS_TO_SQL_ADD_QI_BEGIN_STR);
		_addQuotedIdentEnd_txt  .setText  (ResultSetJXTable.DEFAULT_TABLE_GENERATE_ROWS_TO_SQL_ADD_QI_END_STR);

		_includeDbname_chk.setSelected   (ResultSetJXTable.DEFAULT_TABLE_GENERATE_ROWS_TO_SQL_INCLUDE_DBNAME);
		_includeSchema_chk.setSelected   (ResultSetJXTable.DEFAULT_TABLE_GENERATE_ROWS_TO_SQL_INCLUDE_SCHEMA);
	}


	@Override
	public void actionPerformed(ActionEvent e)
	{
		Object source = e.getSource();

		// --- BUTTON: OK ---
		if (_ok.equals(source))
		{
			apply();
			setVisible(false);
		}

		// --- BUTTON: CANCEL ---
		if (_cancel.equals(source))
		{
			setVisible(false);
		}

		// --- BUTTON: APPLY ---
		if (_apply.equals(source))
		{
			apply();
		}

		// --- BUTTON: DEFAULTS ---
		if (_toDefault.equals(source))
		{
			setDefaults();			
//			apply();
		}
	}

	private ActionListener     _actionListener  = new ActionListener()
	{
		@Override
		public void actionPerformed(ActionEvent actionevent)
		{
			checkForChanges();
		}
	};
	private KeyListener        _keyListener  = new KeyListener()
	{
		 // Changes in the fields are visible first when the key has been released.
		@Override public void keyPressed (KeyEvent keyevent) {}
		@Override public void keyTyped   (KeyEvent keyevent) {}
		@Override public void keyReleased(KeyEvent keyevent) { checkForChanges(); }
	};

	private void checkForChanges()
	{
//		boolean enabled = false;
//
//		boolean replaceNewlines = _replaceNewlines_chk.isSelected();
//
//		_apply.setEnabled(enabled);
	}
}

