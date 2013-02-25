package com.asetune.gui;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.UIManager;

import net.miginfocom.swing.MigLayout;

import org.apache.log4j.PropertyConfigurator;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rtextarea.RTextScrollPane;

import com.asetune.Version;
import com.asetune.ui.rsyntaxtextarea.AsetuneSyntaxConstants;
import com.asetune.utils.AseConnectionFactory;
import com.asetune.utils.SwingUtils;
import com.sybase.ase.planviewer.ASEPlanViewer;

public class AsePlanViewer
extends JFrame
implements ActionListener
{
    private static final long serialVersionUID = 3859509683688523815L;

    private final String      SHOW_SQL_PANEL = "Show SQL";
    private final String      HIDE_SQL_PANEL = "Hide SQL";
    
    private Connection        _conn          = null;
	private String            _sql           = null;
	private List<String>	  _plan          = null;
	private boolean	          _hasPlan       = false;
	private JPanel            _sqlPanel      = null;
	private JPanel            _planPanel     = null;
	private JPanel            _butPanel      = null;
	private JButton           _toggleSql_but = new JButton(HIDE_SQL_PANEL);
	private JButton           _close_but     = new JButton("Close");

	private AseMessageHandler _aseMsg  = new AseMessageHandler();
	
	private String            _inputXmlPlan = null;

	public AsePlanViewer(String xmlPlan)
	{
		this(null, null, xmlPlan);
	}

	public AsePlanViewer(Connection conn, String sql)
	{
		this(conn, sql, null);
	}

	public AsePlanViewer(Connection conn, String sql, String xmlPlan)
	{
		super("Plan Viewer"); // Set window title

//		ImageIcon icon = new ImageIcon(getClass().getResource("swing/images/query16.gif"));
//		super.setIconImage(icon.getImage());

		_conn         = conn;
		_sql          = sql;
		_inputXmlPlan = xmlPlan;

		init();
	}

	protected void init()
	{
		// Setup a message handler
//FIXME: should this be here or NOT
//		((SybConnection)_conn).setSybMessageHandler(_aseMsg);

		_planPanel = getPlanViewer();

		if (_planPanel != null)
		{
			// SQL PANEL
			_sqlPanel = SwingUtils.createPanel("Sql Text (Outer panel)", true);
			_sqlPanel.setLayout(new MigLayout());

//			JTextArea sqlText = new JTextArea();
			RSyntaxTextArea sqlText = new RSyntaxTextArea(7, 30);
			RTextScrollPane sqlScroll = new RTextScrollPane(sqlText);
			sqlText.setText(_sql);
			sqlText.setEditable(false);
			sqlText.setOpaque(false);

			sqlText.setSyntaxEditingStyle(AsetuneSyntaxConstants.SYNTAX_STYLE_SYBASE_TSQL);
//FIXME: fix_a_splitpane, or use Tooltip is the SQL text is to big...

//			_sqlPanel.add(sqlText,    "grow, push, wrap");
			_sqlPanel.add(sqlScroll,  "grow, push, wrap");
			_sqlPanel.setMinimumSize(new Dimension(100, 200));

			
			// SHOWPLAN PANEL is get prior to the IF statement

			
			// PUTTON PANEL
			_butPanel = SwingUtils.createPanel("Buttons", false);
			_butPanel.setLayout(new MigLayout());
			
			_butPanel.add(_toggleSql_but, "left");
			_butPanel.add(_close_but,     "push, right");

			_toggleSql_but.addActionListener(this);
			_close_but    .addActionListener(this);
			


			// ADD to Content pane
			Container contentPane = getContentPane();
			contentPane.setLayout(new MigLayout());

//			contentPane.add(_sqlPanel,  "hidemode 3, gap 5 5 5 5, grow, push, wrap");
//			contentPane.add(_planPanel, "grow, push, wrap");
//			contentPane.add(_butPanel,  "grow, push, wrap");

			contentPane.add(_sqlPanel,  "dock north, hidemode 3, gap 5 5 5 5");
			contentPane.add(_planPanel, "dock center");
			contentPane.add(_butPanel,  "dock south");
			
			pack();
	//		Dimension size = _viewPanel.getPreferredSize();
	//		setSize(size);
			setSize(900, 900);
//			setVisible(true);
		}
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		Object source = e.getSource();

		// BUTTON: TOOGLE SQL ON/OFF
		if (_toggleSql_but.equals(source))
		{
			if (_toggleSql_but.getText().equals(SHOW_SQL_PANEL))
			{
				_sqlPanel.setVisible(true);
				_toggleSql_but.setText(HIDE_SQL_PANEL);
			}
			else
			{
				_sqlPanel.setVisible(false);
				_toggleSql_but.setText(SHOW_SQL_PANEL);
			}
		}

		// BUTTON: CLOSE
		if (_close_but.equals(source))
		{
			dispose();
		}
	}

	protected JPanel getPlanViewer()
	{
		if (getPlan())
		{
			StringBuffer stringbuffer = new StringBuffer(256);
			String s = System.getProperty("line.separator");
			int planSize = _plan.size();
			for (int i=0; i<planSize; i++)
			{
				stringbuffer.append((String) _plan.get(i));
				stringbuffer.append(s);
			}

			try 
			{
				ASEPlanViewer view = new ASEPlanViewer(_conn);

				JPanel panel = view.getPanel();

//System.out.println("## all #######################################################");
//System.out.println(stringbuffer);
//System.out.println("############################################################");
//				view.generatePlan(stringbuffer, new StringBuffer((String) _sql));
//				view.generatePlan(stringbuffer, new StringBuffer((String) _plan.get(0)));
				view.generatePlan(stringbuffer, new StringBuffer());
//				view.setShowExec(true);
//				view.setShowExecIO(true);
//				view.setShowOpt(true);
				
				return panel;
			}
			catch (UnsupportedClassVersionError e)
			{
				SwingUtils.showErrorMessage("ASE Plan Viewer", "Problems loading the ASE Plan Viewer.\n" +
						"I'm guessing you're running a JVM that is earlier than 6.0\n" +
						"So please upgrade the JVM to atleast version 6 and try again.", e);
			}
			catch (Exception e)
			{
				SwingUtils.showErrorMessage("ASE Plan Viewer", "Problems loading the ASE Plan Viewer", e);
			}
		}
		return null;
	}

	private boolean getPlan()
	{
		// STATIC FILE INPUT
		if (_inputXmlPlan != null)
		{
			_plan = new ArrayList<String>();

			try
			{
				BufferedReader reader = new BufferedReader(new StringReader(_inputXmlPlan));

				String str = null;
				while ((str = reader.readLine()) != null)
					_plan.add(str);
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}

			_hasPlan = true;
			return _hasPlan;
		}
		else
		{
			Statement statement = null;
			ResultSet resultset = null;
			int saveTextSize = 32768;
	
			try
			{
				statement = _conn.createStatement();
				resultset = statement.executeQuery("select @@textsize");
				if(resultset.next())
					saveTextSize = resultset.getInt(1);
				if(saveTextSize < 105906176)
					statement.executeQuery("set textsize 105906176");
				statement.executeUpdate("set plan for show_execio_xml to message on");
				statement.executeUpdate("set nodata on");
				statement.executeUpdate("set showplan on");
		
				_aseMsg.startIntercepting();
				StringBuffer stringbuffer = new StringBuffer();
				String as[] = _sql.split("\n");
				for(int j = 0; j < as.length; j++)
				if(as[j].trim().toLowerCase().equals("go"))
					stringbuffer.append(" \n");
				else
					stringbuffer.append(as[j]).append(" \n");
		
				statement.execute(stringbuffer.toString());
				while(statement.getMoreResults() || statement.getUpdateCount() != -1) ;
		
				String s = _aseMsg.stopIntercepting();
		
				_plan = new ArrayList<String>();
				_plan.add(s);
				statement.executeUpdate("set showplan off");
				statement.executeUpdate("set nodata off");
				resultset = statement.executeQuery("select showplan_in_xml(0)");
				do
				{
					if(!resultset.next())
						break;
					String s1 = resultset.getString(1);
					if(s1 != null && s1.trim().length() != 0)
						_plan.add(s1);
				} while(true);
		
				statement.executeUpdate("set plan for show_execio_xml off");
				if(saveTextSize < 105906176)
					statement.executeUpdate("set textsize "+saveTextSize);
				_hasPlan = true;
			}
			catch (SQLException e)
			{
				_plan = null;
				_hasPlan = false;
			}
			return _hasPlan;
		}
	}

	
	public static void main(String args[]) throws Exception
	{
		Properties log4jProps = new Properties();
		//log4jProps.setProperty("log4j.rootLogger", "INFO, A1");
		log4jProps.setProperty("log4j.rootLogger", "TRACE, A1");
		log4jProps.setProperty("log4j.appender.A1", "org.apache.log4j.ConsoleAppender");
		log4jProps.setProperty("log4j.appender.A1.layout", "org.apache.log4j.PatternLayout");
		log4jProps.setProperty("log4j.appender.A1.layout.ConversionPattern", "%d - %-5p - %-30c{1} - %m%n");
		PropertyConfigurator.configure(log4jProps);

		// Create the factory object that holds the database connection using
		// the data specified on the command line
    	try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    		//UIManager.setLookAndFeel(new SubstanceOfficeSilver2007LookAndFeel());
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		Connection conn = AseConnectionFactory.getConnection("localhost", 15700, null, "sa", "", Version.getAppName()+"-AsePlanViewer", null);

//		String sql = "select * from sybsystemprocs..sysobjects order by crdate \n" +
//				"select * from sybsystemprocs..syscomments \n" +
//				"exec sp_help";
		String sql = "select * from sybsystemprocs..sysobjects order by crdate \n";

		// Create a QueryWindow component that uses the factory object.
		AsePlanViewer pv = new AsePlanViewer(conn, sql);
		pv.setVisible(true);
		

		String xmlPlan = 
			"<?xml version=\"1.0\" encoding=\"UTF-8\"?> \n" +
			" \n" +
			"<query> \n" +
			"	<statementId>919502085</statementId> \n" +
			"	<text> \n" +
			"		<![CDATA[ \n" +
			"			SQL Text: select * from sysdatabases]]> \n" +
			"	</text> \n" +
			"	 \n" +
			"	<plan> \n" +
			"		<planId>5512</planId> \n" +
			"		<planStatus> available </planStatus> \n" +
			"		<execCount>1</execCount> \n" +
			"		<maxTime>0</maxTime> \n" +
			"		<avgTime>0</avgTime> \n" +
			"		 \n" +
			"		<compileParameters/> \n" +
			"		 \n" +
			"		<execParameters/> \n" +
			"		<opTree> \n" +
			"			<Emit> \n" +
			"			<VA>1</VA> \n" +
			"			<est> \n" +
			"				<rowCnt>5</rowCnt> \n" +
			"				<lio>0</lio> \n" +
			"				<pio>0</pio> \n" +
			"				<rowSz>263</rowSz> \n" +
			"			</est> \n" +
			"			<arity>1</arity> \n" +
			"				<TableScan> \n" +
			"					<VA>0</VA> \n" +
			"					<est> \n" +
			"						<rowCnt>5</rowCnt> \n" +
			"						<lio>3</lio> \n" +
			"						<pio>3</pio> \n" +
			"						<rowSz>263</rowSz> \n" +
			"					</est> \n" +
			"					<varNo>0</varNo> \n" +
			"					<objName>sysdatabases</objName> \n" +
			"					<scanType>TableScan</scanType> \n" +
			"					<scanOrder> ForwardScan </scanOrder> \n" +
			"					<positioning> StartOfTable </positioning> \n" +
			"					<dataIOSizeInKB>4</dataIOSizeInKB> \n" +
			"					<dataBufReplStrategy> LRU </dataBufReplStrategy> \n" +
			"				</TableScan> \n" +
			"			</Emit> \n" +
			"		 \n" +
			"		</opTree> \n" +
			"	 \n" +
			"	</plan> \n" +
			"	</query> \n";	
		AsePlanViewer pv2 = new AsePlanViewer(xmlPlan);
		pv2.setVisible(true);
	}
}