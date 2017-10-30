package com.asetune.gui;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.Timer;
import javax.swing.UIManager;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.PropertyConfigurator;
import org.fife.ui.rtextarea.RTextScrollPane;

import com.asetune.Version;
import com.asetune.cache.XmlPlanCache;
import com.asetune.ui.rsyntaxtextarea.AsetuneSyntaxConstants;
import com.asetune.ui.rsyntaxtextarea.RSyntaxTextAreaX;
import com.asetune.ui.rsyntaxtextarea.RSyntaxUtilitiesX;
import com.asetune.utils.StringUtil;
import com.asetune.utils.SwingUtils;
import com.sybase.ase.planviewer.ASEPlanViewer;

import net.miginfocom.swing.MigLayout;

public class AsePlanViewer
extends JFrame
implements ActionListener
{
    private static final long serialVersionUID = 3859509683688523815L;

    private final String      SHOW_SQL_PANEL = "Show SQL";
    private final String      HIDE_SQL_PANEL = "Hide SQL";
    
    private Connection        _conn              = null;
	private String            _sql               = null;
	private List<String>	  _plan              = null;
	private boolean	          _hasPlan           = false;
	private JPanel            _sqlPanel          = null;
	private JPanel            _planPanel         = null;
	private JPanel            _butPanel          = null;
	private JButton           _toggleSql_but     = new JButton(HIDE_SQL_PANEL);
	private JButton           _cloneWindow_but   = new JButton("Clone Window");
	private JButton           _loadClipboard_but = new JButton("Load Clipboard");
	private JButton           _loadFile_but      = new JButton("Load File");
	private JButton           _loadName_but      = new JButton("Load Name");
	private JTextField        _loadName_txt      = new JTextField();
	private JButton           _close_but         = new JButton("Close");

	private RSyntaxTextAreaX   _sqlText = new RSyntaxTextAreaX(7, 30);
	private RTextScrollPane   _sqlScroll = new RTextScrollPane(_sqlText);

	private AseMessageHandler _aseMsg  = new AseMessageHandler();
	
	private String            _inputXmlPlan = null;
	private String            _statementId  = "";
	private String            _planId       = "";
	private ASEPlanViewer     _view = null;

	private static String     _lastFileLoaded = null; // static so that several instances can share the last directory

	private final static String EMPTY_PLAN = getEmptySimplePlan();
	private final static String TITLE      = "Plan Viewer";

	//----------------------------------------------------------------
	// BEGIN: instance
	private static AsePlanViewer _instance = null;
	public static AsePlanViewer getInstance()
	{
		if (_instance == null)
		{
			//throw new RuntimeException("AsePlanViewer dosn't have an instace yet, please set with setInstance(instance).");
			_instance = new AsePlanViewer();
			// Well first time load of the above... will take some time, and if we load another one instantly it will cause issue, so lets sleep for a short while and see if it helps
			// NOTE: This is not needed if we do *Deferred* calls, but lets have it in here anyway
			try { Thread.sleep(500); } catch (InterruptedException ignore) {}
		}
		return _instance;
	}
	public static void setInstance(AsePlanViewer instance)
	{
		_instance = instance;
	}
	public static boolean hasInstance()
	{
		return _instance != null;
	}
	// END: instance
	//----------------------------------------------------------------

	//----------------------------------------------------------------
	// BEGIN: Constructors
	public AsePlanViewer()
	{
		this(null, null, EMPTY_PLAN);
	}

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
		super(TITLE); // Set window title

//		ImageIcon icon = new ImageIcon(getClass().getResource("swing/images/query16.gif"));
//		super.setIconImage(icon.getImage());

		_conn         = conn;
		_sql          = sql;
		_inputXmlPlan = xmlPlan;

		init();
	}
	// END: Constructors
	//----------------------------------------------------------------

	protected void init()
	{
		// Setup a message handler
//FIXME: should this be here or NOT
//		((SybConnection)_conn).setSybMessageHandler(_aseMsg);

//		if (_inputXmlPlan != null)
//			_inputXmlPlan = _inputXmlPlan.replace("<plan>", "<!-- <plan> The plan tag is not handled by AsePlanViewer -->").replace("</plan>", "<!-- </plan> The plan tag is not handled by AsePlanViewer-->");
//
//		if (_sql == null && _inputXmlPlan != null)
//		{
//			int startPos = _inputXmlPlan.indexOf("<text>");
//			int endPos   = _inputXmlPlan.indexOf("</text>");
//			
//			if (startPos >= 0 && endPos >= 0)
//			{
//				startPos += "<text>".length();
//
//				String sql = _inputXmlPlan.substring(startPos, endPos);
//				sql = sql.replace("<![CDATA[", "").replace("]]>", "");
//				sql = sql.replace("Subordinate SQL Text: ", "").replace("SQL Text: ", "");
//				
//				_sql = sql.trim();
//			}
//		}
		
		// Set Icon
		ImageIcon icon16 = SwingUtils.readImageIcon(Version.class, "images/ase_plan_viewer_16.png");
		ImageIcon icon32 = SwingUtils.readImageIcon(Version.class, "images/ase_plan_viewer_32.png");

		ArrayList<Image> iconList = new ArrayList<Image>();
		if (icon16 != null) iconList.add(icon16.getImage());
		if (icon32 != null) iconList.add(icon32.getImage());
		setIconImages(iconList);


		// Plan
		_planPanel = getPlanViewer();

		if (_planPanel != null)
		{
			// SQL PANEL
			_sqlPanel = SwingUtils.createPanel("Sql Text (Outer panel)", true);
			_sqlPanel.setLayout(new MigLayout());

//			JTextArea sqlText = new JTextArea();
			_sqlText.setText(_sql);
			_sqlText.setCaretPosition(0);
//			_sqlText.setEditable(false);
			_sqlText.setOpaque(false);

			_sqlText.setSyntaxEditingStyle(AsetuneSyntaxConstants.SYNTAX_STYLE_SYBASE_TSQL);
//FIXME: fix_a_splitpane, or use Tooltip is the SQL text is to big...

			RSyntaxUtilitiesX.installRightClickMenuExtentions(_sqlScroll, this);


//			_sqlPanel.add(sqlText,    "grow, push, wrap");
			_sqlPanel.add(_sqlScroll,  "grow, push, wrap");
			_sqlPanel.setMinimumSize(new Dimension(100, 200));

			
			// SHOWPLAN PANEL is get prior to the IF statement

			
			// PUTTON PANEL
			_butPanel = SwingUtils.createPanel("Buttons", false);
			_butPanel.setLayout(new MigLayout());
			
			_butPanel.add(_toggleSql_but,     "left");
			_butPanel.add(_cloneWindow_but,   "");
//			_butPanel.add(new JLabel(),       "pushx, growx");
			_butPanel.add(_loadName_but,      "gap 20");
			_butPanel.add(_loadName_txt,      "pushx, growx");
			_butPanel.add(_loadClipboard_but, "gap 20");
			_butPanel.add(_loadFile_but,      "");
			_butPanel.add(_close_but,         "gap 20");

			_toggleSql_but    .addActionListener(this);
			_cloneWindow_but  .addActionListener(this);
			_loadName_txt     .addActionListener(this);
			_loadName_but     .addActionListener(this);
			_loadClipboard_but.addActionListener(this);
			_loadFile_but     .addActionListener(this);
			_close_but        .addActionListener(this);
			


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

	/**
	 * Internal action handler for various internal Components
	 */
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
			if (this.equals(_instance))
				this.setVisible(false);
			else
				dispose();
		}

		// BUTTON: CLONE
		if (_cloneWindow_but.equals(source))
		{
			String currentTitle = getTitle();
			AsePlanViewer clone = new AsePlanViewer(_inputXmlPlan);
			clone.setVisible(true);
			clone.setTitle(currentTitle);
		}

		// BUTTON: LOAD Name
		if (_loadName_but.equals(source))
		{
			loadXmlFromCacheDeferred(_loadName_txt.getText());
		}
		if (_loadName_txt.equals(source))
		{
			_loadName_but.doClick();
		}

		// BUTTON: LOAD from clipboard
		if (_loadClipboard_but.equals(source))
		{
			try
			{
				String data = (String) Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
				
				if (data.indexOf("<?xml version=") == -1)
				{
					// Or maybe we can open a Editor so we can edit the content...
					SwingUtils.showErrorMessage(this, "Error loading XML", "The clipboard do not contain xml.\n\n"+data, null);
				}

				loadXml(data);
				setTitle(TITLE + " - From Clipboard");
			}
			catch (Exception ex)
			{
				SwingUtils.showErrorMessage(this, "Error loading XML", "Can't load the Clipboard'. Caught: "+ex, ex);
			}
		}

		// BUTTON: LOAD FILE
		if (_loadFile_but.equals(source))
		{
			String envName = "DBXTUNE_SAVE_DIR";
			String saveDir = StringUtil.getEnvVariableValue(envName);
					
			JFileChooser fc = new JFileChooser(_lastFileLoaded);
//			JFileChooser fc = new JFileChooser();
			if (saveDir != null)
				fc.setCurrentDirectory(new File(saveDir));

			int returnVal = fc.showOpenDialog(null);
			if (returnVal == JFileChooser.APPROVE_OPTION) 
	        {
				File file = fc.getSelectedFile();
				_lastFileLoaded = file.toString();
				//This is where a real application would open the file.
				//String filename = file.getAbsolutePath();

				try
				{
					loadXmlFile(file);
				}
				catch(Exception ex)
				{
					SwingUtils.showErrorMessage(this, "Error loading file", "Can't load the file '"+file+"'. Caught: "+ex, ex);
				}
	        }
		}
	}

	/**
	 * Load a XML File
	 * @param file
	 * @throws Exception
	 */
	public void loadXmlFile(File file)
	throws Exception
	{
		_inputXmlPlan = FileUtils.readFileToString(file);
		getPlan();
		_view.generatePlan(listToStringBuffer(_plan), new StringBuffer());
		setTitle(TITLE + " - " + file);
		setVisible(true);
	}



	/**
	 * Load the following XML String, but do it deferred...<br>
	 * It will be displayed in 500ms<br>
	 * If the same xmlString is passed within the 500ms time frame, it will be "skipped"<br>
	 * If a new xmlString is passed, we restart the clock<br>
	 * <br>
	 * This is perfect if calling from ToolTip, since that can do the <b>multiple</b> times.
	 * @param xmlString
	 */
	public void loadXmlDeferred(String xmlString)
	{
		if (_deferredXmlPlan != null && _deferredXmlPlan.equals(xmlString))
			return;

//System.out.println("+++ loadXmlDeferred() xmlString.lenth()="+xmlString.length());
		_deferredXmlPlan = xmlString;

		if ( ! _deferredXmlPlanTimer.isRunning() )
			_deferredXmlPlanTimer.start();
		else
			_deferredXmlPlanTimer.restart();
	}
	//----------------------------------
	// BEGIN: Deferred plan
	private String _deferredXmlPlan = ""; 
	private Timer  _deferredXmlPlanTimer = new Timer(500, new ActionListener() // Setup timer for not overloading the AseShowplan component
	{
		@Override
		public void actionPerformed(ActionEvent paramActionEvent)
		{
			_deferredXmlPlanTimer.stop();
			loadXml(_deferredXmlPlan);
//System.out.println(">>>>>>>>>> _deferredXmlPlanTimer : SHOW XML PLAN... _deferredXmlPlan.lenth()="+_deferredXmlPlan.length());
			_deferredXmlPlan = "";
		}
	});
	// END: Deferred plan
	//----------------------------------

	/**
	 * Load the following XML String
	 * @param xmlString
	 */
	public void loadXml(String xmlString)
	{
		_inputXmlPlan = xmlString;
		getPlan();
		try
		{
			_view.generatePlan(listToStringBuffer(_plan), new StringBuffer());
			setTitle(TITLE + " - StatementId=" + _statementId + ", PlanId=" + _planId);
			setVisible(true);
		}
		catch (Exception ex)
		{
			SwingUtils.showErrorMessage(this, "Error loading XML", "Can't load the XML String'. Caught: "+ex, ex);

			// get a new instance next time
			setInstance(null);
			setVisible(false);
			dispose();
		}
	}

	/**
	 * Load and display a XML plan from the <code>XmlPlanCache</code><br>
	 * @param planName
	 * @throws RuntimeException if the XmlPlanCache isn't initialized
	 */
	public String loadXmlFromCache(String planName)
	{
		return loadXmlFromCache(planName, 0);
	}

	
	/**
	 * Load and display a XML plan from the <code>XmlPlanCache</code><br>
	 * @param planName
	 * @param planId
	 * @throws RuntimeException if the XmlPlanCache isn't initialized
	 */
	public String loadXmlFromCache(String planName, int planId)
	{
		String xmlString = XmlPlanCache.getInstance().getPlan(planName, planId);
		loadXml(xmlString);
		setTitle(TITLE + " - " + planName + (planId<=0 ? "" : ":"+planId) );
		setVisible(true);
		return xmlString;
	}


	/**
	 * Load the XML String from Cache (or grab it from the cache), but the GUI ShowPlan parsing is deferred...<br>
	 * It will be displayed in 500ms<br>
	 * If the same <code>planName</code> is passed within the 500ms time frame, it will be "skipped"<br>
	 * If a new <code>planName</code> is passed, we restart the clock<br>
	 * <br>
	 * This is perfect if calling from ToolTip, since that can do the <b>multiple</b> times.
	 * @param planName
	 */
	public String loadXmlFromCacheDeferred(String planName)
	{
		return loadXmlFromCacheDeferred(planName, 0);
	}
	/**
	 * Load the XML String from Cache (or grab it from the cache), but the GUI ShowPlan parsing is deferred...<br>
	 * It will be displayed in 500ms<br>
	 * If the same <code>planName</code> is passed within the 500ms time frame, it will be "skipped"<br>
	 * If a new <code>planName</code> is passed, we restart the clock<br>
	 * <br>
	 * This is perfect if calling from ToolTip, since that can do the <b>multiple</b> times.
	 * @param planName
	 * @param planId
	 */
	public String loadXmlFromCacheDeferred(String planName, int planId)
	{
//System.out.println("+++ loadXmlFromCacheDeferred() planName='"+planName+"', planId="+planId);
		String xmlString = XmlPlanCache.getInstance().getPlan(planName, planId);
		if (_deferredCachedXmlPlan != null && _deferredCachedXmlPlan.equals(xmlString))
			return _deferredCachedXmlPlan;

		_deferredCachedXmlPlan = xmlString;
		_deferredPlanName      = planName;
		_deferredPlanId        = planId;

		if ( ! _deferredCachedXmlPlanTimer.isRunning() )
			_deferredCachedXmlPlanTimer.start();
		else
			_deferredCachedXmlPlanTimer.restart();
		
		return _deferredCachedXmlPlan;
	}
	//----------------------------------
	// BEGIN: Deferred Cached plan
	private String _deferredCachedXmlPlan = ""; 
	private String _deferredPlanName      = ""; 
	private int    _deferredPlanId        = 0; 
	private Timer  _deferredCachedXmlPlanTimer = new Timer(500, new ActionListener() // Setup timer for not overloading the AseShowplan component
	{
		@Override
		public void actionPerformed(ActionEvent paramActionEvent)
		{
			_deferredCachedXmlPlanTimer.stop();
			loadXml(_deferredCachedXmlPlan);
			setTitle(TITLE + " - " + _deferredPlanName + (_deferredPlanId<=0 ? "" : ":"+_deferredPlanId) );
			_deferredCachedXmlPlan = "";
			_deferredPlanName      = "";
			_deferredPlanId        = 0;
//System.out.println(">>>>>>>>>> _deferredCachedXmlPlan : SHOW XML PLAN...");
		}
	});
	// END: Deferred plan
	//----------------------------------

	
	/**
	 * Stuff the internal List into a StringBuffer
	 * @param list
	 * @return
	 */
	private StringBuffer listToStringBuffer(List<String> list)
	{
		StringBuffer sb = new StringBuffer(256);
		String s = System.getProperty("line.separator");
		int planSize = _plan.size();
		for (int i=0; i<planSize; i++)
		{
			sb.append((String) _plan.get(i));
			sb.append(s);
		}
		return sb;
	}
	
	protected JPanel getPlanViewer()
	{
		if (getPlan())
		{
//			StringBuffer db = listToStringBuffer(_plan);
//			StringBuffer stringbuffer = new StringBuffer(256);
//			String s = System.getProperty("line.separator");
//			int planSize = _plan.size();
//			for (int i=0; i<planSize; i++)
//			{
//				stringbuffer.append((String) _plan.get(i));
//				stringbuffer.append(s);
//			}

			try 
			{
				if (_view == null)
					_view = new ASEPlanViewer(_conn);

				JPanel panel = _view.getPanel();

//System.out.println("## all #######################################################");
//System.out.println(stringbuffer);
//System.out.println("############################################################");
//				_view.generatePlan(stringbuffer, new StringBuffer((String) _sql));
//				_view.generatePlan(stringbuffer, new StringBuffer((String) _plan.get(0)));
				_view.generatePlan(listToStringBuffer(_plan), new StringBuffer());
//				_view.setShowExec(true);
//				_view.setShowExecIO(true);
//				_view.setShowOpt(true);
				
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
			if (_inputXmlPlan.indexOf("<plan>") >= 0)
			{
				// If it's a "cloned" entry we don't want to make comments inside a comment... 
				if (_inputXmlPlan.indexOf("<!-- <plan>") == -1)
				{
					_inputXmlPlan = _inputXmlPlan.replace("<plan>",  "<!-- <plan> The plan tag is not handled by AsePlanViewer -->");
					_inputXmlPlan = _inputXmlPlan.replace("</plan>", "<!-- </plan> The plan tag is not handled by AsePlanViewer-->");
				}
			}
			
			_statementId = "";
			if (_inputXmlPlan.indexOf("<statementId>") >= 0)
			{
				int startPos = _inputXmlPlan.indexOf("<statementId>");
				int endPos   = _inputXmlPlan.indexOf("</statementId>");
				if (startPos >= 0 && endPos >= 0)
				{
					startPos += "<statementId>".length();
					_statementId = _inputXmlPlan.substring(startPos, endPos);
				}
			}

			_planId = "";
			if (_inputXmlPlan.indexOf("<planId>") >= 0)
			{
				int startPos = _inputXmlPlan.indexOf("<planId>");
				int endPos   = _inputXmlPlan.indexOf("</planId>");
				if (startPos >= 0 && endPos >= 0)
				{
					startPos += "<planId>".length();
					_planId = _inputXmlPlan.substring(startPos, endPos);
				}
			}

			// Get SQL from the XML
//			if (_sql == null && _inputXmlPlan != null)
			if (true)
			{
				int startPos = _inputXmlPlan.indexOf("<text>");
				int endPos   = _inputXmlPlan.indexOf("</text>");
				
				if (startPos >= 0 && endPos >= 0)
				{
					startPos += "<text>".length();

					String sql = _inputXmlPlan.substring(startPos, endPos);
					sql = sql.replace("<![CDATA[", "").replace("]]>", "");
					sql = sql.replace("Subordinate SQL Text: ", "").replace("SQL Text: ", "");
					
					_sql = sql.trim();
				}
			}
			_sqlText.setText(_sql);
			_sqlText.setCaretPosition(0);


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
	
			if (_conn == null)
			{
				//_logger.debug("Sorry no connection...");
				return false;
			}

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
				{
					if(as[j].trim().toLowerCase().equals("go"))
						stringbuffer.append(" \n");
					else
						stringbuffer.append(as[j]).append(" \n");
				}
		
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

	private static String getEmptySimplePlan()
	{
		return 
			"<?xml version=\"1.0\" encoding=\"UTF-8\"?> \n" + 
			"<query> \n" + 
			"	<planVersion> 1.0 </planVersion> \n" + 
			"	<statementNum>1</statementNum> \n" + 
			"	<lineNum>1</lineNum> \n" + 
			"	<text> \n" + 
			"		<![CDATA[ \n" + 
			"			SQL Text: /* ACTION: Open a XML Plan with one on the buttons at the buttom. */\n" +
			"/*         This below SQL is just a dummy statement to open the tool. */\n" +
			"select 'empty plan'  \n" + 
			"			 \n" + 
			"		]]> \n" + 
			"	</text> \n" + 
			"	<abstractPlan> \n" + 
			"		<![CDATA[> \n" + 
			"		 \n" + 
			"		]]> \n" + 
			"	</abstractPlan> \n" + 
			"	<costs> \n" + 
			"		<lio> 0 </lio> \n" + 
			"		<pio> 0 </pio> \n" + 
			"		<cpu> 0 </cpu> \n" + 
			"	</costs> \n" + 
			"	<resource> \n" + 
			"		<threads> 0 </threads> \n" + 
			"		<auxSdes>0</auxSdes> \n" + 
			"	</resource> \n" + 
			"	<optimizerMetrics> \n" + 
			"		<optTimeMs>0</optTimeMs> \n" + 
			"		<optTicks>0</optTicks> \n" + 
			"		<plansEvaluated>0</plansEvaluated> \n" + 
			"		<plansValid>0</plansValid> \n" + 
			"		<procCacheBytes>0</procCacheBytes> \n" + 
			"	</optimizerMetrics> \n" + 
			"	<opTree> \n" + 
			"		<lavaContext/> \n" + 
			"		<Emit> \n" + 
			"		<VA>1</VA> \n" + 
			"		<est> \n" + 
			"			<rowCnt>0</rowCnt> \n" + 
			"			<rowSz>0</rowSz> \n" + 
			"		</est> \n" + 
			"		<act> \n" + 
			"			<rowCnt>1</rowCnt> \n" + 
			"		</act> \n" + 
			"		<arity>1</arity> \n" + 
			"			<Scalar> \n" + 
			"			<VA>0</VA> \n" + 
			"			<est> \n" + 
			"				<rowCnt>-1</rowCnt> \n" + 
			"				<lio>-1</lio> \n" + 
			"				<pio>-1</pio> \n" + 
			"				<rowSz>-1</rowSz> \n" + 
			"			</est> \n" + 
			"			<act> \n" + 
			"				<rowCnt>1</rowCnt> \n" + 
			"			</act> \n" + 
			"			</Scalar> \n" + 
			"		</Emit> \n" + 
			"	</opTree> \n" + 
			"</query> \n";
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
		
//		Connection conn = AseConnectionFactory.getConnection("localhost", 15700, null, "sa", "", Version.getAppName()+"-AsePlanViewer", null);
//		Connection conn = AseConnectionFactory.getConnection("192.168.0.110", 1600, null, "sa", "sybase", Version.getAppName()+"-AsePlanViewer", null);

//		String sql = "select * from sybsystemprocs..sysobjects order by crdate \n" +
//				"select * from sybsystemprocs..syscomments \n" +
//				"exec sp_help";
		String sql = "select * from sybsystemprocs..sysobjects order by crdate \n";

		// Create a QueryWindow component that uses the factory object.
//		AsePlanViewer pv = new AsePlanViewer(conn, sql);
//		pv.setVisible(true);
		

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
