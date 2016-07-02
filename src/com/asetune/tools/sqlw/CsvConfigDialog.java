/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */

package com.asetune.tools.sqlw;

import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.UIManager;

import net.miginfocom.swing.MigLayout;

import com.asetune.gui.ParameterDialog;
import com.asetune.gui.swing.GCheckBox;
import com.asetune.gui.swing.GLabel;
import com.asetune.utils.Configuration;
import com.asetune.utils.StringUtil;
import com.asetune.utils.SwingUtils;

public class CsvConfigDialog
	extends JDialog
	implements ActionListener
{
	private static final long serialVersionUID = 1L;

	private Map<String, String>     _outputMap      = null; 
//	private Map<String, JTextField> _componentMap   = null; 

	private GLabel      _description         = new GLabel("<html>"
			+ "Various settings for how to copy CSV (Comma Separated Values)<br>"
			+ "from the output/ResultSet panel into the clipboard.<br>"
			+ "This so you can do Ctrl-V into another editor."
			+ "</html>");

	private GLabel      _useRfc4180_lbl      = new GLabel("<html>See: <a href='http://tools.ietf.org/html/rfc4180'>http://tools.ietf.org/html/rfc4180</a></html>");
	private GCheckBox   _useRfc4180_chk      = new GCheckBox("Use RFC 4180");

	private JLabel      _columnSeparator_lbl = new JLabel("Column Separator");
	private JTextField  _columnSeparator_txt = new JTextField(10);

	private JLabel      _rowSeparator_lbl    = new JLabel("Row Separator");
	private JTextField  _rowSeparator_txt    = new JTextField(10);

	private JLabel      _nullValue_lbl       = new JLabel("NULL Value Replacement");
	private JTextField  _nullValue_txt       = new JTextField(10);

	private JCheckBox   _copyHeaders_chk     = new JCheckBox("Copy Headers");

	private JCheckBox   _copyMessages_chk    = new JCheckBox("Copy Messages, etc");

	private JButton     _ok             = new JButton("OK");
	private JButton     _cancel         = new JButton("Cancel");
	private JButton     _apply          = new JButton("Apply");
	private boolean     _showApply      = true;

	private CsvConfigDialog(Window owner, String title, boolean showApply)
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
		CsvConfigDialog params = new CsvConfigDialog(owner, "CSV Config", false);
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

		String rfc4180_tooltip = 
				"<html>"
				+ "Use RFC 4180 when copying the results in the tables<br>"
				+ "<br>"
				+ "In short this means:"
				+ "<ul>"
				+ "  <li>Rows/columns that has CR/NL in the text will be quotifed.<br>Meaning it will look like: <code>first col,\"second col with line<br>break in text\",third col</code></li>"
				+ "  <li>Rows/columns that has ',' in the text will be quotifed.<br>Meaning it will look like: <code>first col,\"second col with comma, in text\",third col</code></li>"
				+ "  <li>Rows/columns that has '\"' in the text will be quotifed, and the quotes inside the text will be escaped with two quotes<br>Meaning it will look like: <code>first col,\"second col with a quote \"\" in text\",third col</code></li>"
				+ "</ul>"
				+ "Note: Column separator will automatically be set to ','<br>"
				+ "See: <a href='http://tools.ietf.org/html/rfc4180'>http://tools.ietf.org/html/rfc4180</a> for more info."
				+ "</html>";
				
		// Some tool tip
		_description        .setToolTipText(rfc4180_tooltip);
		_useRfc4180_chk     .setToolTipText(rfc4180_tooltip);
		_useRfc4180_lbl     .setToolTipText(_useRfc4180_chk.getToolTipText());
		_columnSeparator_lbl.setToolTipText("<html>Character to use as column separators, typically ',' or '\\t'</html>");
		_columnSeparator_txt.setToolTipText(_columnSeparator_lbl.getToolTipText());
		_rowSeparator_lbl   .setToolTipText("<html>Character to use for terminating a row, typically '\\n' or '\\r\\n'</html>");
		_rowSeparator_txt   .setToolTipText(_rowSeparator_lbl.getToolTipText());
		_nullValue_lbl      .setToolTipText("<html>If data in the table is 'NULL' then write this string instead of NULL, this so we can separate empty strings and NULL Values<br>A good value to choose could be &lt;NULL&gt;</html>");
		_nullValue_txt      .setToolTipText(_nullValue_lbl.getToolTipText());
		_copyHeaders_chk    .setToolTipText("<html>If you want to write the column headers prior to the data rows.</html>");
		_copyMessages_chk   .setToolTipText("<html>If you want to include any messages that comes from errors or print statements in the output.</html>");

		// Listen for "return" and any other "key pressing"
		// This simply enables/disable the "apply" button if anything was changed.
		panel.add(_description        , "span, wrap 20");
		panel.add(_useRfc4180_chk     , "");
		panel.add(_useRfc4180_lbl     , "wrap 15");
		panel.add(_columnSeparator_lbl, "");
		panel.add(_columnSeparator_txt, "wrap");
		panel.add(_rowSeparator_lbl   , "");
		panel.add(_rowSeparator_txt   , "wrap");
		panel.add(_nullValue_lbl      , "");
		panel.add(_nullValue_txt      , "wrap 15");
		panel.add(_copyHeaders_chk    , "wrap");
		panel.add(_copyMessages_chk   , "wrap");
		
		setStartValues();
		checkForChanges();
		
		_useRfc4180_chk     .addActionListener(_actionListener);
		_columnSeparator_txt.addActionListener(_actionListener);
		_columnSeparator_txt.addKeyListener   (_keyListener);
		_rowSeparator_txt   .addActionListener(_actionListener);
		_rowSeparator_txt   .addKeyListener   (_keyListener);
		_nullValue_txt      .addActionListener(_actionListener);
		_nullValue_txt      .addKeyListener   (_keyListener);
		_copyHeaders_chk    .addActionListener(_actionListener);
		_copyMessages_chk   .addActionListener(_actionListener);

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
		boolean useRfc4180 = Configuration.getCombinedConfiguration().getBooleanProperty(QueryWindow.PROPKEY_COPY_RESULTS_CSV_RFC_4180,    QueryWindow.DEFAULT_COPY_RESULTS_CSV_RFC_4180);
		String  colSep     = Configuration.getCombinedConfiguration().getPropertyRawVal( QueryWindow.PROPKEY_COPY_RESULTS_CSV_COL_SEP,     QueryWindow.DEFAULT_COPY_RESULTS_CSV_COL_SEP);
		String  rowSep     = Configuration.getCombinedConfiguration().getPropertyRawVal( QueryWindow.PROPKEY_COPY_RESULTS_CSV_ROW_SEP,     QueryWindow.DEFAULT_COPY_RESULTS_CSV_ROW_SEP);
		String  nullValue  = Configuration.getCombinedConfiguration().getProperty(       QueryWindow.PROPKEY_COPY_RESULTS_CSV_NULL_OUTPUT, QueryWindow.DEFAULT_COPY_RESULTS_CSV_NULL_OUTPUT);
		boolean headers    = Configuration.getCombinedConfiguration().getBooleanProperty(QueryWindow.PROPKEY_COPY_RESULTS_CSV_HEADERS,     QueryWindow.DEFAULT_COPY_RESULTS_CSV_HEADERS);
		boolean messages   = Configuration.getCombinedConfiguration().getBooleanProperty(QueryWindow.PROPKEY_COPY_RESULTS_CSV_MESSAGES,    QueryWindow.DEFAULT_COPY_RESULTS_CSV_MESSAGES);

		_useRfc4180_chk     .setSelected(useRfc4180);
		_columnSeparator_txt.setText(StringUtil.escapeControlChars(colSep));
		_rowSeparator_txt   .setText(StringUtil.escapeControlChars(rowSep));
		_nullValue_txt      .setText(StringUtil.escapeControlChars(nullValue));
		_copyHeaders_chk    .setSelected(headers);
		_copyMessages_chk   .setSelected(messages);
	}
	/*---------------------------------------------------
	** END: component initialization
	**---------------------------------------------------
	*/

	
	private void apply()
	{
		_apply.setEnabled(false);

		final Configuration tmpConf = Configuration.getInstance(Configuration.USER_TEMP);

		tmpConf.setProperty(QueryWindow.PROPKEY_COPY_RESULTS_CSV_RFC_4180,    _useRfc4180_chk     .isSelected());
		tmpConf.setProperty(QueryWindow.PROPKEY_COPY_RESULTS_CSV_COL_SEP,     StringUtil.unEscapeControlChars(_columnSeparator_txt.getText()));
		tmpConf.setProperty(QueryWindow.PROPKEY_COPY_RESULTS_CSV_ROW_SEP,     StringUtil.unEscapeControlChars(_rowSeparator_txt   .getText()));
		tmpConf.setProperty(QueryWindow.PROPKEY_COPY_RESULTS_CSV_NULL_OUTPUT, StringUtil.unEscapeControlChars(_nullValue_txt      .getText()));
		tmpConf.setProperty(QueryWindow.PROPKEY_COPY_RESULTS_CSV_HEADERS,     _copyHeaders_chk    .isSelected());
		tmpConf.setProperty(QueryWindow.PROPKEY_COPY_RESULTS_CSV_MESSAGES,    _copyMessages_chk   .isSelected());
		
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

		boolean rfc4180 = _useRfc4180_chk.isSelected();
		_columnSeparator_txt.setEnabled(!rfc4180);
		_rowSeparator_txt   .setEnabled(!rfc4180);

		
		
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

