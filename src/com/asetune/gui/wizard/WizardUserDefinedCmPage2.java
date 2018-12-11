/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package com.asetune.gui.wizard;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;

import org.fife.ui.rtextarea.RTextScrollPane;
import org.netbeans.spi.wizard.Wizard;
import org.netbeans.spi.wizard.WizardPage;
import org.netbeans.spi.wizard.WizardPanelNavResult;

import com.asetune.CounterController;
import com.asetune.Version;
import com.asetune.cm.CounterSample;
import com.asetune.cm.CountersModel;
import com.asetune.gui.swing.MultiLineLabel;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.tools.WindowType;
import com.asetune.tools.sqlw.QueryWindow;
import com.asetune.ui.rsyntaxtextarea.AsetuneSyntaxConstants;
import com.asetune.ui.rsyntaxtextarea.RSyntaxTextAreaX;
import com.asetune.ui.rsyntaxtextarea.RSyntaxUtilitiesX;
import com.asetune.utils.StringUtil;
import com.asetune.utils.Ver;

import net.miginfocom.swing.MigLayout;



public class WizardUserDefinedCmPage2
extends WizardPage
implements ActionListener, FocusListener, CaretListener
{
    private static final long serialVersionUID = 1L;
//	private static Logger _logger          = Logger.getLogger(WizardUserDefinedCmPage2.class);

	private static final String WIZ_NAME = "SQL-Statements";
	private static final String WIZ_DESC = "SQL Statement(s)";// 
	private static final String WIZ_HELP1 = "SQL Statements that will be executed on init. (for example: create some work tables, set ASE options, etc...)\nThis is only executed once, before first sample.";
	private static final String WIZ_HELP2 = "SQL Statements that will be executed on every sample. (should produce a resultset)";
	private static final String WIZ_HELP3 = "SQL Statements that will be executed on close. (for example: drop tables created in init)\nThis is only executed once, when the connetion is closed.";

	private static final String NEED_VERSION_DEFAULT = "Works on all DB Server versions";
	private static final String NEED_VERSION_TOOLTIP = "<html>" + 
	                                                      "Lowest ASE Version where this Counter Model works on.<br>" +
	                                                      "Specify this as a number/integer.<br>" +
//	                                                      "Example 1: 15030, which is 15.0.3<br>" +
//	                                                      "Example 2: 15031, which is 15.0.3 ESD#1<br>" +
	                                                      "Example 1: "+Ver.ver(15,0,3)    +", which is 15.0.3<br>" +
	                                                      "Example 2: "+Ver.ver(15,0,3,1)  +", which is 15.0.3 ESD#1<br>" +
	                                                      "Example 3: "+Ver.ver(15,0,3,4,2)+", which is 15.0.3 ESD#4.2<br>" +
	                                                      "Example 4: "+Ver.ver(15,7,0,100)+", which is 15.7 SP100<br>" +
	                                                      "Example 5: "+Ver.ver(16,0,0,1,1)+", which is 16.0 SP01 PL01<br>" +
	                                                      "Note: always use 7 digits as the version number. Or 0 if you mean 'any version'<br>" +
	                                                  "</html>";

	private static final String NEED_ROLE_DEFAULT   = "<mon_role, sa_role, xxx_role, ...>";
	private static final String NEED_ROLE_TOOLTIP   = "<html>" + 
	                                                      "A ',' separated list of role(s) that needs to be granted for this User Definde Counter.<br>" +
	                                                      "Syntax: xxx_role, yyy_role, zzz_role<br>" +
	                                                      "Example: sa_role, mon_role.</i><br>" +
	                        		                      "Here is a list of System roles: sa_role, sso_role, oper_role, sybase_ts_role, navigator_role, replication_role, dtm_tm_role, ha_role, mon_role, js_admin_role, messaging_role, js_client_role, js_user_role, webservices_role, keycustodian_role<br>" +
	                                                  "</html>";

	private static final String NEED_CONFIG_DEFAULT = "<enable monitoring, config option 2, ...>";
	private static final String NEED_CONFIG_TOOLTIP = "<html>" + 
	                                                      "A ',' separated list of sp_configure parameters that needs to be configured for this User Definde Counter.<br>" +
	                                                      "Syntax: config option 1[=defValue], config option 2[=defValue]<br>" +
	                                                      "Note: do not use \" surounding the configuration name.<br>" +
	                                                      "Example: deadlock pipe active=1, deadlock pipe max=500 <i>Use default values during no-gui initialization.</i><br>" +
	                        		                      "Example: statement cache size, enable stmt cache monitoring=1 <i>for 'statement cache size' do <b>NOT</b> use default values. If you do it will be enabled by \"mistake\".</i><br>" +
	                                                  "</html>";

	private static final String MON_TABLES_DEFAULT = "<monXXX, monYYY>";
	private static final String MON_TABLES_TOOLTIP = "A ',' separated list of monXXX tables, used for tooltip to describe what the columns represents.";

	
	private boolean    _firtsTimeRender = true;

//	private JTextArea   _sqlInit_txt          = new JTextArea();        // A field to enter a query in
//	private JTextArea   _sql_txt              = new JTextArea();        // A field to enter a query in
//	private JTextArea   _sqlClose_txt         = new JTextArea();        // A field to enter a query in
	private RSyntaxTextAreaX _sqlInit_txt      = new RSyntaxTextAreaX();  // A field to enter a query in
	private RSyntaxTextAreaX _sql_txt          = new RSyntaxTextAreaX();  // A field to enter a query in
	private RSyntaxTextAreaX _sqlClose_txt     = new RSyntaxTextAreaX();  // A field to enter a query in
	private JLabel      _needVersion_lbl      = new JLabel("Min DB Version");
	private JTextField  _needVersion_txt      = new JTextField(NEED_VERSION_DEFAULT);
	private JLabel      _needRole_lbl         = new JLabel("Needs Role");
	private JTextField  _needRole_txt         = new JTextField(NEED_ROLE_DEFAULT);
	private JLabel      _needConfig_lbl       = new JLabel("Needs Config");
	private JTextField  _needConfig_txt       = new JTextField(NEED_CONFIG_DEFAULT);
	private JLabel      _monTables_lbl        = new JLabel("Monitor Tables");
	private JTextField  _monTables_txt        = new JTextField(MON_TABLES_DEFAULT);
	private JCheckBox   _negDiffCntToZero_chk = new JCheckBox("If Difference Calculation renders a negative number, set to Zero", true);

	public static String getDescription() { return WIZ_DESC; }
	@Override
	public Dimension getPreferredSize() { return WizardUserDefinedCm.preferredSize; }

	public WizardUserDefinedCmPage2()
	{
		super(WIZ_NAME, WIZ_DESC);
		
		JButton button;
		setLayout(new MigLayout("", "[grow]", ""));

		_sqlInit_txt         .setName("sqlInit");
		_sql_txt             .setName("sql");
		_sqlClose_txt        .setName("sqlClose");
		_needVersion_txt     .setName("needVersion");
		_needRole_txt        .setName("needRole");
		_needConfig_txt      .setName("needConfig");
		_monTables_txt       .setName("toolTipMonTables");
		_negDiffCntToZero_chk.setName("negativeDiffCountersToZero");
		
		_needVersion_txt     .setToolTipText(NEED_VERSION_TOOLTIP);
		_needRole_txt        .setToolTipText(NEED_ROLE_TOOLTIP);
		_needConfig_txt      .setToolTipText(NEED_CONFIG_TOOLTIP);
		_monTables_txt       .setToolTipText(MON_TABLES_TOOLTIP);
		_negDiffCntToZero_chk.setToolTipText("<html>" +
				"If Difference Calculation renders a negative number, between two counter samples, set the counter to Zero" +
				"<ul>" +
				"<li>Uncheck this if you have counters that increments and then decremets and you want to know how much the value decreased by." +
				"<li>Check this is the counters have a tendency to be reset (for example by sp_sysmon) or similar<br>" +
				"    Or if the counter is set to zero when it wraps over the max values of a integer or other number.<br>" +
				"    But if the counters just wraps around the max boundary into a negative value, the diff calculation still works." +
				"</ul></html>");

		_sqlInit_txt .setSyntaxEditingStyle(AsetuneSyntaxConstants.SYNTAX_STYLE_SYBASE_TSQL);
		_sql_txt     .setSyntaxEditingStyle(AsetuneSyntaxConstants.SYNTAX_STYLE_SYBASE_TSQL);
		_sqlClose_txt.setSyntaxEditingStyle(AsetuneSyntaxConstants.SYNTAX_STYLE_SYBASE_TSQL);

		_sqlInit_txt .setHighlightCurrentLine(false);
		_sql_txt     .setHighlightCurrentLine(false);
		_sqlClose_txt.setHighlightCurrentLine(false);

		RTextScrollPane sqlScroll = new RTextScrollPane(_sql_txt);
		sqlScroll.setLineNumbersEnabled(true);

		RSyntaxUtilitiesX.installRightClickMenuExtentions(sqlScroll, this);

		add( new MultiLineLabel(WIZ_HELP1), "wmin 100, span, pushx, growx, wrap" );
		add(_sqlInit_txt, "growx, pushx, wrap");

		button = new JButton("Test/Edit above in SQL Window");
		button.addActionListener(this);
		button.putClientProperty("NAME", "BUTTON_TEST_SQL_OPEN");
		add(button, "span, align right, wrap 10");

		add( new MultiLineLabel(WIZ_HELP2), "wmin 100, span, pushx, growx, wrap" );
//		add(new JScrollPane(_sql_txt), "growx, pushx, height 100%, wrap");
		add(sqlScroll, "growx, pushx, height 100%, wrap");

		add(_needVersion_lbl, "split, width 80lp!");
		add(_needVersion_txt, "split, growx, pushx");

		button = new JButton("Test/Edit above in SQL Window");
		button.addActionListener(this);
		button.putClientProperty("NAME", "BUTTON_TEST_SQL_REFRESH");
		add(button, "align right, wrap");

		add(_needRole_lbl,    "split, width 80lp!");
		add(_needRole_txt,    "split, growx, wrap");

		add(_needConfig_lbl,  "split, width 80lp!");
		add(_needConfig_txt,  "split, growx, wrap");

		add(_monTables_lbl,   "split, width 80lp!");
		add(_monTables_txt,   "split, growx, wrap");

		add(_negDiffCntToZero_chk, "wrap 30");

		add( new MultiLineLabel(WIZ_HELP3), "wmin 100, span, pushx, growx, wrap" );
		add(_sqlClose_txt, "growx, pushx, wrap");

		button = new JButton("Test/Edit above in SQL Window");
		button.addActionListener(this);
		button.putClientProperty("NAME", "BUTTON_TEST_SQL_CLOSE");
		add(button, "span, align right, wrap 10");
		
		// The auto registration doesn't seems to work anymore so to be safe add listeners
		_sqlInit_txt    .addCaretListener(this);
		_sql_txt        .addCaretListener(this);
		_sqlClose_txt   .addCaretListener(this);
		_needVersion_txt.addCaretListener(this);
		_needRole_txt   .addCaretListener(this);
		_needConfig_txt .addCaretListener(this);
		_monTables_txt  .addCaretListener(this);
		
		_sqlInit_txt    .addFocusListener(this);
		_sql_txt        .addFocusListener(this);
		_sqlClose_txt   .addFocusListener(this);
		_needVersion_txt.addFocusListener(this);
		_needRole_txt   .addFocusListener(this);
		_needConfig_txt .addFocusListener(this);
		_monTables_txt  .addFocusListener(this);
	}

	@Override
	public void focusGained(FocusEvent e)
	{
		userInputReceived( (e.getSource() instanceof Component ? (Component)e.getSource() : null), e);
	}
	@Override
	public void focusLost(FocusEvent e)
	{
		userInputReceived( (e.getSource() instanceof Component ? (Component)e.getSource() : null), e);
	}
	@Override
	public void caretUpdate(CaretEvent e)
	{
		userInputReceived( (e.getSource() instanceof Component ? (Component)e.getSource() : null), e);
	}

	private void applyFromTemplate()
	{
		String cmName = (String) getWizardData("cmTemplate");
		if (cmName == null)
			return;
//		CountersModel cm = GetCounters.getInstance().getCmByName(cmName);
		CountersModel cm = CounterController.getInstance().getCmByName(cmName);
		if (cm != null)
		{
			_sqlInit_txt    .setText( cm.getSqlInit() );
			_sql_txt        .setText( cm.getSql() );
			_sqlClose_txt   .setText( cm.getSqlClose() );
			_needVersion_txt.setText( cm.getDependsOnVersion()+"" );
			_needConfig_txt .setText( StringUtil.toCommaStr(cm.getDependsOnRole()) );
			_needConfig_txt .setText( StringUtil.toCommaStr(cm.getDependsOnConfig()) );
			_monTables_txt  .setText( StringUtil.toCommaStr(cm.getMonTablesInQuery()) );

			_negDiffCntToZero_chk.setSelected(cm.isNegativeDiffCountersToZero());
		}
	}
	
	/** Called when we enter the page */
	@Override
	protected void renderingPage()
    {
		if (_firtsTimeRender)
		{
			applyFromTemplate();
		}
	    _firtsTimeRender = false;
    }

	@Override
	protected String validateContents(Component comp, Object event)
	{
//		String name = null;
//		if (comp != null)
//			name = comp.getName();
//
		//System.out.println("validateContents: name='"+name+"',\n\ttoString='"+comp+"'\n\tcomp='"+comp+"',\n\tevent='"+event+"'.");

		String problem = "";
		if ( _sql_txt.getText().trim().length() <= 0) problem += "SQL Statement ";

		if (problem.length() > 0  &&  problem.endsWith(", "))
		{
			// Discard last ', '
			problem = problem.substring(0, problem.length()-2);
		}
		if ( problem.length() > 0 )
			return "Following fields cant be empty: "+problem;

		// NEED VERSION
		if ( ! _needVersion_txt.getText().equals(NEED_VERSION_DEFAULT) )
		{
			String srvVersionStr = _needVersion_txt.getText();
			if ( ! srvVersionStr.equals("") )
			{
				long srvVersionNum = -1;
				try 
				{
					srvVersionNum = Long.parseLong(srvVersionStr); 
				}
				catch (NumberFormatException ignore) 
				{ 
					return "DBMS Version needs to be a number."; 
				}
				if (srvVersionNum > 0 && srvVersionStr.length() != Long.toString(Ver.ver(15,0,3,1,1)).length() )
					return "DBMS Version needs to be a number, Example "+Ver.ver(15,0,3,1,1)+" (15.0.3 ESD#1.1).";
			}
		}

		// NEED ROLE
		if (_needRole_txt.getText().equals(NEED_ROLE_DEFAULT))
			return "Substitute '"+NEED_ROLE_DEFAULT+"' to something usefull. Or make it empty";

		if (_needRole_txt.getText().indexOf("<") >= 0 || _needRole_txt.getText().indexOf(">") >= 0)
			return "Take away the '<' and/or '>' chars in 'needs role'.";


		// NEED CONFIG
		if (_needConfig_txt.getText().equals(NEED_CONFIG_DEFAULT))
			return "Substitute '"+NEED_CONFIG_DEFAULT+"' to something usefull. Or make it empty";

		if (_needConfig_txt.getText().indexOf("<") >= 0 || _needConfig_txt.getText().indexOf(">") >= 0)
			return "Take away the '<' and/or '>' chars in 'needs config'.";

		if (_needConfig_txt.getText().indexOf("\"") >= 0)
			return "Take away any \" quote chars in 'needs config'.";

		
		// MONITOR TABLES
		if (_monTables_txt.getText().equals(MON_TABLES_DEFAULT))
			return "Substitute '"+MON_TABLES_DEFAULT+"' to something usefull. Or make it empty";
	
		if (_monTables_txt.getText().indexOf("<") >= 0 || _monTables_txt.getText().indexOf(">") >= 0)
			return "Take away the '<' and/or '>' chars in 'monitor tables'.";

		
		if ( problem.length() > 0 )
			return problem;

		return null;
	}

	@Override
	public void actionPerformed(ActionEvent ae)
	{
		JComponent src = (JComponent) ae.getSource();
		String name = (String)src.getClientProperty("NAME");
		if (name == null)
			name = "-null-";

//		System.out.println("Source("+name+"): " + src);

		String sql = null;
		if (name.equals("BUTTON_TEST_SQL_OPEN"))    sql = _sqlInit_txt .getText();
		if (name.equals("BUTTON_TEST_SQL_REFRESH")) sql = _sql_txt     .getText();
		if (name.equals("BUTTON_TEST_SQL_CLOSE"))   sql = _sqlClose_txt.getText();

		if (sql != null)
		{
			try 
			{
//				Connection conn = AseConnectionFactory.getConnection(null, Version.getAppName()+"-wiz-udc", null);
				DbxConnection conn = DbxConnection.connect(SwingUtilities.getWindowAncestor(this), Version.getAppName()+"-wiz-udc");
				QueryWindow qw = new QueryWindow(conn, sql, null, true, WindowType.JDIALOG_MODAL, null);
//				qw.setModal(true);
//				qw.setModalExclusionType(Dialog.ModalExclusionType.APPLICATION_EXCLUDE);
//				qw.setModalExclusionType(Dialog.ModalExclusionType.TOOLKIT_EXCLUDE);
				qw.setSize(500, 500);
				qw.setLocationRelativeTo(this);
				qw.setVisible(true);

				// Get the SQL that was used in QueryWindow and put it back in...
				String retSql = qw.getSql();
				if (name.equals("BUTTON_TEST_SQL_OPEN"))    _sqlInit_txt .setText(retSql);
				if (name.equals("BUTTON_TEST_SQL_REFRESH")) _sql_txt     .setText(retSql);
				if (name.equals("BUTTON_TEST_SQL_CLOSE"))   _sqlClose_txt.setText(retSql);
			}
			catch (Exception ex) 
			{
				JOptionPane.showMessageDialog(
					this, 
					"Problems open SQL Query Window\n" + ex.getMessage(),
					"Error", JOptionPane.ERROR_MESSAGE);
			}
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public WizardPanelNavResult allowNext(String stepName, Map settings, Wizard wizard)
	{
		putWizardData("CounterSample", null);
		try 
		{
//			Connection conn = AseConnectionFactory.getConnection(null, Version.getAppName()+"-wiz-udc", null);
			DbxConnection conn = DbxConnection.connect(SwingUtilities.getWindowAncestor(this), Version.getAppName()+"-wiz-udc");
			CountersModel cm = new CountersModel();
//			CounterSample sample = new CounterSample("asetune-wiz-udc-test", true, null, null);
			CounterSample sample = cm.createCounterSample("asetune-wiz-udc-test", true, null, null);
			sample.getSample(cm, conn, _sql_txt.getText(), null);

			if (sample.getColumnCount() <= 0)
			{
				JOptionPane.showMessageDialog(
						this, 
						"Problems when validating SQL 'refresh' statement\n" +
						"NO Columns were returned from the SQL statement.",
						"Error", JOptionPane.ERROR_MESSAGE);

					return WizardPanelNavResult.REMAIN_ON_PAGE;	
			}
			putWizardData("CounterSample", sample);
		}
		catch (Exception ex) 
		{
			JOptionPane.showMessageDialog(
				this, 
				"Problems when validating SQL 'refresh' statement\n" + ex.getMessage(),
				"Error", JOptionPane.ERROR_MESSAGE);

			return WizardPanelNavResult.REMAIN_ON_PAGE;	
		}

		if ( _needVersion_txt.getText().equals(NEED_VERSION_DEFAULT) )
		{
			_needVersion_txt.setText("0");
		}

		// the automatic com.setName() did not work on RSyntaxTextArea, so we need to do this manually
		// First remove them from the dataMap, then put new ones in there
		String sqlInitKey  = _sqlInit_txt .getName();
		String sqlExecKey  = _sql_txt     .getName();
		String sqlCloseKey = _sqlClose_txt.getName();

		String sqlInitVal  = _sqlInit_txt .getText().trim();
		String sqlExecVal  = _sql_txt     .getText().trim();
		String sqlCloseVal = _sqlClose_txt.getText().trim();

		Map<String, String> dataMap = getWizardDataMap();
		dataMap.remove(sqlInitKey);
		dataMap.remove(sqlExecKey);
		dataMap.remove(sqlCloseKey);

		if (sqlInitVal.length()  > 0) dataMap.put(sqlInitKey,  sqlInitVal);
		if (sqlExecVal.length()  > 0) dataMap.put(sqlExecKey,  sqlExecVal);
		if (sqlCloseVal.length() > 0) dataMap.put(sqlCloseKey, sqlCloseVal);
		
		return WizardPanelNavResult.PROCEED;
	}
}
