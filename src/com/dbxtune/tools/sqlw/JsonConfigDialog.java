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
import javax.swing.JPanel;

import com.dbxtune.gui.swing.GLabel;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.SwingUtils;

import net.miginfocom.swing.MigLayout;

public class JsonConfigDialog
	extends JDialog
	implements ActionListener
{
	private static final long serialVersionUID = 1L;

	private Map<String, String>     _outputMap      = null; 
//	private Map<String, JTextField> _componentMap   = null; 

	private GLabel      _description         = new GLabel("<html>"
			+ "Various settings for how to copy to JSON (JavaScript Object Notation)<br>"
			+ "from the output/ResultSet panel into the clipboard.<br>"
			+ "This so you can do Ctrl-V into another editor."
			+ "</html>");

	private JCheckBox   _copyOnlyRs_chk      = new JCheckBox("Only Copy ResultSets");
	private JCheckBox   _copyMetaData_chk    = new JCheckBox("Include MetaData");
	private JCheckBox   _doPrettyPrint_chk   = new JCheckBox("Pretty Print");

	private JCheckBox   _tsAsIso8601_chk     = new JCheckBox("Timestamps as ISO-8601");
	
	private JButton     _ok             = new JButton("OK");
	private JButton     _cancel         = new JButton("Cancel");
	private JButton     _apply          = new JButton("Apply");
	private boolean     _showApply      = false;

	private JsonConfigDialog(Window owner, String title, boolean showApply)
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
		JsonConfigDialog params = new JsonConfigDialog(owner, "JSON Config", false);
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

		// Some tool tip
//		_description        .setToolTipText("");
		_copyOnlyRs_chk     .setToolTipText("Only Copy ResultSets, skip Messages and Other Informational Texts.");
		_copyMetaData_chk   .setToolTipText("Include MetaData etc in the JSON. (This will create a bigger JSON text, but with more information about columns etc)");
		_doPrettyPrint_chk  .setToolTipText("One long 'chunk' of text, or in a more readable format (with newlines between fields).");
		_tsAsIso8601_chk    .setToolTipText("Should JDBC Datatype 'TIMESTAMP' be printed as a Number (milliseconds since epoch) or as a String in ISO 8601 format (yyyy-MM-dd'T'HH:mm:ss.SSSXXX).");
		
		// Listen for "return" and any other "key pressing"
		// This simply enables/disable the "apply" button if anything was changed.
		panel.add(_description        , "span, wrap 20");

		panel.add(_copyOnlyRs_chk     , "wrap");
		panel.add(_copyMetaData_chk   , "wrap");
		panel.add(_doPrettyPrint_chk  , "wrap 15");

		panel.add(_tsAsIso8601_chk    , "wrap");

		setStartValues();
		checkForChanges();
		
		_copyOnlyRs_chk     .addActionListener(_actionListener);
		_copyMetaData_chk   .addActionListener(_actionListener);
		_doPrettyPrint_chk  .addActionListener(_actionListener);
		_tsAsIso8601_chk    .addActionListener(_actionListener);

		// ADD the OK, Cancel, Apply buttons
		panel.add(_ok,     "tag ok,     gap top 20, skip, split, bottom, right, push");
		panel.add(_cancel, "tag cancel,                   split, bottom");
		if (_showApply)
		panel.add(_apply,  "tag apply,                    split, bottom");

		// Initial state for buttons
		_apply.setEnabled(false);
		
		setContentPane(panel);

		// ADD ACTIONS TO COMPONENTS
		_ok           .addActionListener(this);
		_cancel       .addActionListener(this);
		_apply        .addActionListener(this);
	}
	
	private void setStartValues()
	{
		boolean copyOnlyRs    = Configuration.getCombinedConfiguration().getBooleanProperty(QueryWindow.PROPKEY_COPY_RESULTS_JSON_ONLY_RS       , QueryWindow.DEFAULT_COPY_RESULTS_JSON_ONLY_RS);
		boolean copyMetaData  = Configuration.getCombinedConfiguration().getBooleanProperty(QueryWindow.PROPKEY_COPY_RESULTS_JSON_METADATA      , QueryWindow.DEFAULT_COPY_RESULTS_JSON_METADATA);
		boolean doPrettyPrint = Configuration.getCombinedConfiguration().getBooleanProperty(QueryWindow.PROPKEY_COPY_RESULTS_JSON_PRETTY_PRINT  , QueryWindow.DEFAULT_COPY_RESULTS_JSON_PRETTY_PRINT);
		boolean tsAsIso_8601  = Configuration.getCombinedConfiguration().getBooleanProperty(QueryWindow.PROPKEY_COPY_RESULTS_JSON_TS_AS_ISO_8601, QueryWindow.DEFAULT_COPY_RESULTS_JSON_TS_AS_ISO_8601);

		_copyOnlyRs_chk     .setSelected(copyOnlyRs);
		_copyMetaData_chk   .setSelected(copyMetaData);
		_doPrettyPrint_chk  .setSelected(doPrettyPrint);
		_tsAsIso8601_chk    .setSelected(tsAsIso_8601);
	}
	/*---------------------------------------------------
	** END: component initialization
	**---------------------------------------------------
	*/

	
	private void apply()
	{
		_apply.setEnabled(false);

		final Configuration tmpConf = Configuration.getInstance(Configuration.USER_TEMP);

		tmpConf.setProperty(QueryWindow.PROPKEY_COPY_RESULTS_JSON_ONLY_RS       , _copyOnlyRs_chk    .isSelected());
		tmpConf.setProperty(QueryWindow.PROPKEY_COPY_RESULTS_JSON_METADATA      , _copyMetaData_chk  .isSelected());
		tmpConf.setProperty(QueryWindow.PROPKEY_COPY_RESULTS_JSON_PRETTY_PRINT  , _doPrettyPrint_chk .isSelected());
		tmpConf.setProperty(QueryWindow.PROPKEY_COPY_RESULTS_JSON_TS_AS_ISO_8601, _tsAsIso8601_chk   .isSelected());
		
		tmpConf.save();
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
		boolean enabled = false;

//		boolean rfc4180 = _useRfc4180_chk.isSelected();
//		_columnSeparator_txt.setEnabled(!rfc4180);
//		_rowSeparator_txt   .setEnabled(!rfc4180);

		
		
//		for (Map.Entry<String,JTextField> entry : _componentMap.entrySet()) 
//		{
//			String key      = entry.getKey();
//			String valueNow = entry.getValue().getText();
//			String valueIn  = _inputMap.get(key);
//
//			if ( ! valueNow.equals(valueIn) )
//			{
//				enabled = true;
//				break;
//			}
//		}
		_apply.setEnabled(enabled);
	}

	//--------------------------------------------------
	// TEST-CODE
	//--------------------------------------------------
//	public static void main(String[] args)
//	{
//		try
//		{
//			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
//		}
//		catch (Exception e)
//		{
//			e.printStackTrace();
//		}
//
//		LinkedHashMap<String,String> in = new LinkedHashMap<String,String>();
//		in.put("Input Fileld One", "123");
//		in.put("Ke theis",         "1");
//		in.put("And thrre",        "nisse");
//		in.put("Yupp",             "kalle");
//		Map<String,String> results = showParameterDialog(null, "Test Parameters", in, true);
//		
//		if (results == null)
//			System.out.println("Cancel");
//		else
//		{
//			for (Map.Entry<String,String> entry : results.entrySet()) 
//			{
//				String key = entry.getKey();
//				String val = entry.getValue();
//
//				System.out.println("key='"+key+"', val='"+val+"'.");
//			}
//		}
//	}
}

