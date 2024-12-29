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
package com.dbxtune.alarm.ui.config;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Image;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.fife.ui.autocomplete.AutoCompletion;
import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.parser.DefaultParseResult;
import org.fife.ui.rsyntaxtextarea.parser.DefaultParserNotice;
import org.fife.ui.rsyntaxtextarea.parser.ExtendedHyperlinkListener;
import org.fife.ui.rsyntaxtextarea.parser.ParseResult;
import org.fife.ui.rsyntaxtextarea.parser.Parser;
import org.fife.ui.rtextarea.RTextScrollPane;

import com.dbxtune.Version;
import com.dbxtune.alarm.events.AlarmEvent;
import com.dbxtune.alarm.events.AlarmEventDummy;
import com.dbxtune.alarm.events.AlarmEvent.Category;
import com.dbxtune.alarm.events.AlarmEvent.ServiceState;
import com.dbxtune.alarm.events.AlarmEvent.Severity;
import com.dbxtune.alarm.ui.view.DummyEventDialog;
import com.dbxtune.alarm.ui.view.DummyEventDialog.AlarmEventSetCallback;
import com.dbxtune.alarm.writers.AlarmWriterAbstract;
import com.dbxtune.alarm.writers.IAlarmWriter;
import com.dbxtune.alarm.writers.WriterUtils;
import com.dbxtune.gui.swing.GLabel;
import com.dbxtune.ui.rsyntaxtextarea.RSyntaxTextAreaX;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.SwingUtils;

import net.miginfocom.swing.MigLayout;

public class TemplateEditor
extends JDialog
implements ActionListener, DocumentListener, CaretListener, AlarmEventSetCallback
{
//	private static Logger _logger = Logger.getLogger(TemplateEditor.class);
	private static final long	serialVersionUID	= -1L;
	
	private static final String TEMPLATE_HELP = "http://velocity.apache.org/engine/2.0/user-guide.html#conditionals";

	private String _return = null;

	private JSplitPane _splitPane;

	private JPanel _topPanel;
	private JPanel _editorPanel;
	private JPanel _okCancelPanelPanel;

	private GLabel            _warning_lbl       = new GLabel();
	private GLabel            _editorRowCol_lbl  = new GLabel();
	private RSyntaxTextAreaX  _editor_txt        = new RSyntaxTextAreaX(10, 80);
	private RSyntaxTextArea   _example_lbl       = new RSyntaxTextArea(5, 80);

	private JButton           _setAlarmEvent_but = new JButton("Set AlarmEvent Params");
	private JComboBox<String> _setAlarmType_cbx  = new JComboBox<>(new String[]{AlarmWriterAbstract.ACTION_RAISE, AlarmWriterAbstract.ACTION_RE_RAISE, AlarmWriterAbstract.ACTION_CANCEL});

	private JButton           _templateHelp_but  = new JButton("Velocity Template Language Documentation");
	private JButton           _sendTestAlarm_but = new JButton("Send Test Alarm");
	
	private String            _currentWriterClassName = "";
	private Configuration     _currentConfig          = null;
	private String            _currentPropKey         = null;

	// PANEL: OK-CANCEL
	private JButton          _ok          = new JButton("OK");
	private JButton          _cancel      = new JButton("Cancel");
	
	private AlarmEvent       _exampleAlarmEvent     = new AlarmEventDummy("GORAN_1_DS", "SomeCmName", "SomeExtraInfo", Category.OTHER, Severity.WARNING, ServiceState.AFFECTED, -1, 999, "This is an Alarm Example with the data value of '999'", "Extended Description goes here", 0);
	private List<AlarmEvent> _exampleAlarmEventList = new ArrayList<>();

	private static final String DIALOG_TITLE = "AlarmEvent Template Editor";

	public TemplateEditor(Window owner, String currentWriterClassName, String currentPropKey, Configuration configuration)
	{
//		super(owner, DIALOG_TITLE, ModalityType.APPLICATION_MODAL);
//		super(owner, DIALOG_TITLE, ModalityType.TOOLKIT_MODAL);
		super(owner, DIALOG_TITLE, ModalityType.DOCUMENT_MODAL);
//		super(owner, DIALOG_TITLE, ModalityType.MODELESS);
//		super(owner, DIALOG_TITLE);
//		setModal(true);

		_currentWriterClassName = currentWriterClassName;
		_currentPropKey         = currentPropKey;
		_currentConfig          = configuration;

		// Add some examples, which will be used...
		_exampleAlarmEvent     = new AlarmEventDummy("GORAN_1_DS", "SomeCmName", "SomeExtraInfo", Category.OTHER, Severity.WARNING, ServiceState.AFFECTED, -1, 999, "This is an Alarm Example with the data value of '999'", "Extended Description goes here", 0);
		_exampleAlarmEventList = new ArrayList<>();
		for (int i=0; i<10; i++)
		{
			AlarmEvent ae = new AlarmEventDummy("GORAN_"+i+"_DS", "SomeCmName-"+i, "SomeExtraInfo-"+i, Category.OTHER, Severity.WARNING, ServiceState.AFFECTED, -1, 150+i, "This is an Alarm Example with the data value of '"+(150+i)+"'", "Extended Description goes here - "+i, 0);
			_exampleAlarmEventList.add(ae);
		}

		initComponents();
	}

	/**
	 * Show the dialog
	 * @param owner   The owner object, if null we try to grab the Window from the KeyboardFocusManager
	 * @param currentWriterClassName 
	 * @param configuration 
	 * @return
	 */
	public static String showDialog(Window owner, String initialText, String currentWriterClassName, String currentPropKey, Configuration configuration)
	{
		if (owner == null)
			owner = SwingUtils.getParentWindowByFocus();
			
		TemplateEditor dialog = new TemplateEditor(owner, currentWriterClassName, currentPropKey, configuration);
		dialog.setText(initialText);
		dialog.setLocationRelativeTo(owner);
		dialog.setVisible(true);
		dialog.dispose();
		
		return dialog._return;
	}

	/**
	 * Set text in the text editor
	 * @param text
	 */
	public void setText(String text)
	{
		_editor_txt.setText(text);
		_editor_txt.setCaretPosition(0);
	}

	/*---------------------------------------------------
	** BEGIN: component initialization
	**---------------------------------------------------
	*/
	public static ImageIcon getIcon16() { return SwingUtils.readImageIcon(Version.class, "images/alarm_template_editor_16.png"); }
	public static ImageIcon getIcon32() { return SwingUtils.readImageIcon(Version.class, "images/alarm_template_editor_32.png"); }

	
	protected void initComponents()
	{
//		super(_owner);
//		if (_owner != null)
//			setIconImage(_owner.getIconImage());

//		setTitle(DIALOG_TITLE);

		// Set the icon, if we "just" do setIconImage() on the JDialog
		// it will not be the "correct" icon in the Alt-Tab list on Windows
		// So we need to grab the owner, and set that since the icon is grabbed from the owner...
		ImageIcon icon16 = getIcon16();
		ImageIcon icon32 = getIcon32();
		if (icon16 != null || icon32 != null)
		{
			ArrayList<Image> iconList = new ArrayList<Image>();
			if (icon16 != null) iconList.add(icon16.getImage());
			if (icon32 != null) iconList.add(icon32.getImage());

			setIconImages(iconList);
		}

		
		JPanel panel = new JPanel();
		//panel.setLayout(new MigLayout("debug, insets 0 0 0 0, wrap 1","",""));   // insets Top Left Bottom Right
//		panel.setLayout(new MigLayout("insets 0, wrap 1","",""));   // insets Top Left Bottom Right
		panel.setLayout(new BorderLayout());

		_topPanel           = createTopPanel();
		_editorPanel        = createEditorPanel();
		_okCancelPanelPanel = createOkCancelPanel();

		_splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		_splitPane.setTopComponent(_topPanel);
		_splitPane.setBottomComponent(_editorPanel);
		_splitPane.setDividerLocation(0.5f);
		_splitPane.setResizeWeight   (0.5f);
		
		panel.add(_splitPane,          BorderLayout.CENTER);
		panel.add(_okCancelPanelPanel, BorderLayout.SOUTH);
		
//		panel.add(_splitPane,          "grow, push");
////		panel.add(_topPanel,           "grow, push");
////		panel.add(_editorPanel,        "grow, push, height 100%");
//		panel.add(_okCancelPanelPanel, "bottom, right, push");


		loadProps();

		setContentPane(panel);

		initComponentActions();
		
		setFocus(_cancel);
	}

	private JPanel createTopPanel()
	{
		JPanel panel = SwingUtils.createPanel("Information", true);
//		panel.setLayout(new MigLayout("insets 0 0 0 0", "", ""));
		panel.setLayout(new MigLayout("", "", ""));

//		panel.setToolTipText("<html>" +
//				"Templates will help you to set/restore values in the below table...<br>" +
//				"<UL>" +
//				"<li> To <b>Load</b> a template, just choose one from the drop down list. </li>" +
//				"<li> To <b>Save</b> current settings as a template, just press the 'Save..' button and choose a name to save as. </li>" +
//				"<li> To <b>Remove</b> a template, just press the 'Remove..' button and button and choose a template name to deleted. </li>" +
//				"</UL>" +
//				"If all selected values in the table is matching a template, that template name will be displayed in the drop down list.<br>" +
//				"<br>" +
//				"<b>Note:</b> User Defined Counters is <b>not</b> saved/restored in the templates, only <i>System Performance Counters</i><br>" +
//				"</html>");

		
		String htmlDesc = "<html>"
				+ "Press <b>&lt;Ctrl&gt;+&lt;Space&gt;</b> to get available template variables. and <b>help</b> on them."
				+ "</html>";
		
		String alarmDesc = "<html><code>"
				+ "new AlarmEventDummy(\"GORAN_1_DS\", \"SomeCmName\", \"SomeExtraInfo\", Category.OTHER, Severity.WARNING, ServiceState.AFFECTED, -1, 999, \"This is an Alarm Example with the data value of '999'\", \"Extended Description goes here\");"
				+ "</code></html>";

		RTextScrollPane scroll = new RTextScrollPane(_example_lbl, true);

		JLabel outputExample_lbl = new JLabel("<html><b>Output&nbsp;Example:</b></html>");
		JLabel alarmExample_lbl  = new JLabel("<html><b>Alarm&nbsp;Example:</b></html>");
		JLabel description_lbl   = new JLabel(htmlDesc);

		panel.add(outputExample_lbl,      "");
		panel.add(scroll,                 "grow, push, wrap");

		panel.add(alarmExample_lbl,       "");
		panel.add(new JLabel(alarmDesc),  "growx, pushx, wrap 20");
		
		panel.add(description_lbl,        "span, split");
		panel.add(new JLabel(),           "growx, pushx"); // Dummy component
		panel.add(new JLabel("Type"),     ""); // Dummy component
		panel.add(_setAlarmType_cbx,      "");
		panel.add(_setAlarmEvent_but,     "wrap");
		
//		_example_lbl.setBackground(_editor_txt.getBackground());
		_example_lbl.setEditable(false);
		_example_lbl.setLineWrap(true);
		_example_lbl.setHighlightCurrentLine(false);
//		_example_lbl.setWhitespaceVisible(true);

		_setAlarmType_cbx .addActionListener(this);
		_setAlarmEvent_but.addActionListener(this);
		
		return panel;
	}

	private JPanel createEditorPanel()
	{
		JPanel panel = SwingUtils.createPanel("Template Editor", true);
		panel.setLayout(new MigLayout("insets 0 0 0 0", "", ""));
//		panel.setLayout(new MigLayout("", "", ""));

//		LanguageSupportFactory.get().register(_editor_txt);
		RTextScrollPane scroll = new RTextScrollPane(_editor_txt, true);

		_editor_txt.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON);
//		_editor_txt.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_UNIX_SHELL);
		scroll.setLineNumbersEnabled(true);
//		scroll.setIconRowHeaderEnabled(true);

		// INSTALL: The Parser which will populate the EXAMPLE in the top panel
		_editor_txt.addParser(new LocalVelocityParser());
		_editor_txt.setParserDelay(500); // Compile after 500ms

//		RSyntaxTextArea.setTemplatesEnabled(true);
//		CodeTemplateManager ctm = RSyntaxTextArea.getCodeTemplateManager();
//		ctm.addTemplate(  new StaticCodeTemplate("sout", "System.out.println(", null)  );

		AutoCompletion ac = new AutoCompletion( WriterUtils.createCompletionProvider() );
		ac.install(_editor_txt);

		ac.setShowDescWindow(true); // enable the "extra" descriptive window to the right of completion.
		ac.setChoicesWindowSize(
				Configuration.getCombinedConfiguration().getIntProperty("TemplateEditor.setChoicesWindowSize.width", 200), 
				Configuration.getCombinedConfiguration().getIntProperty("TemplateEditor.setChoicesWindowSize.height", 600));
		ac.setDescriptionWindowSize(
				Configuration.getCombinedConfiguration().getIntProperty("TemplateEditor.setDescriptionWindowSize.width", 600), 
				Configuration.getCombinedConfiguration().getIntProperty("TemplateEditor.setDescriptionWindowSize.height", 600));

		panel.add(scroll,            "grow, push, wrap");
		panel.add(_editorRowCol_lbl, "split, hidemode 3");
		panel.add(_warning_lbl,      "hidemode 3, wrap");

		_editorRowCol_lbl.setVisible(false);

		_warning_lbl.setUseFocusableTips(true);
		_warning_lbl.setVisible(false);

		
		// Listen for changes in the editor: and fill in the example
//		_editor_txt.getDocument().addDocumentListener(this);

		// Listener for Caret change so we can update ROW:COL label
		_editor_txt.addCaretListener(this);
		
		return panel;
	}

	private JPanel createOkCancelPanel()
	{
		JPanel panel = new JPanel();
		panel.setLayout(new MigLayout("","",""));   // insets Top Left Bottom Right

		// ADD the OK, Cancel, Apply buttons
		panel.add(_templateHelp_but,  "");
		panel.add(_sendTestAlarm_but, "");
		panel.add(new JLabel(),       "growx, pushx");
		panel.add(_ok,                "tag ok, right");
		panel.add(_cancel,            "tag cancel");

		// ADD ACTIONS TO COMPONENTS
		_ok               .addActionListener(this);
		_cancel           .addActionListener(this);
		_templateHelp_but .addActionListener(this);
		_sendTestAlarm_but.addActionListener(this);

		_templateHelp_but .setToolTipText("Open the default web browser at "+TEMPLATE_HELP);;
		_sendTestAlarm_but.setToolTipText("<html>Send a Test Alarm using the above template<br>Using AlarmWriter: <code>"+_currentWriterClassName+"</code></html>");
		
		return panel;
	}

	private void initComponentActions()
	{
		//---- Top PANEL -----

		//---- Tab PANEL -----

		this.addWindowListener(new java.awt.event.WindowAdapter()
		{
			@Override
			public void windowClosing(WindowEvent e)
			{
				saveProps();
			}
		});
	}
	/*---------------------------------------------------
	** END: component initialization
	**---------------------------------------------------
	*/



	/*---------------------------------------------------
	** BEGIN: Action Listeners, and helper methods for it
	**---------------------------------------------------
	*/
	@Override
	public void actionPerformed(ActionEvent e)
    {
		Object source = e.getSource();

		// --- BUTTON: CANCEL ---
		if (_cancel.equals(source))
		{
			_return = null;
			setVisible(false);
		}

		// --- BUTTON: OK ---
		if (_ok.equals(source))
		{
			doApply();
			saveProps();
			setVisible(false);
		}
		
		if (_setAlarmType_cbx.equals(source))
		{
			updateExampleText();
		}

		if (_setAlarmEvent_but.equals(source))
		{
			// Open a dialog, which can set member: _exampleAlarmEvent to a new AlarmEvent
			DummyEventDialog.showDialog(this, this);
//			DummyEventDialog.showDialog(null, this);
		}

		if (_templateHelp_but.equals(source))
		{
			if (Desktop.isDesktopSupported())
			{
				final Desktop desktop = Desktop.getDesktop();
				if ( desktop.isSupported(Desktop.Action.BROWSE) )
				{
					Thread bg = new Thread()
					{
						@Override
						public void run()
						{
							try 
							{
								URL url = new URL(TEMPLATE_HELP);
								desktop.browse(url.toURI()); 
							}
							catch (Exception ex) { ex.printStackTrace(); }
						}
					};
					bg.setName("StartBrowserForTemplateHelp");
					bg.setDaemon(true);
					bg.start();
				}
			}
		}
		
		if (_sendTestAlarm_but.equals(source))
		{
			String writerClassName = _currentWriterClassName;
			IAlarmWriter alarmWriterClass;
			Configuration conf = _currentConfig;

			conf.setProperty(_currentPropKey, _editor_txt.getText());
			try
			{
				System.out.println("Instantiating and Initializing AlarmWriterClass='"+_currentWriterClassName+"'.");
				try
				{
					Class<?> c = Class.forName( writerClassName );
					alarmWriterClass = (IAlarmWriter) c.newInstance();
				}
				catch (ClassCastException ex)
				{
					throw new ClassCastException("When trying to load alarmWriter class '"+writerClassName+"'. The alarmWriter do not seem to follow the interface '"+IAlarmWriter.class.getName()+"'");
				}
				catch (ClassNotFoundException ex)
				{
					throw new ClassNotFoundException("Tried to load alarmWriter class '"+writerClassName+"'.", ex);
				}

				// Now initialize the User Defined AlarmWriter
				alarmWriterClass.init(conf);
//				alarmWriterClass.printFilterConfig();
//				alarmWriterClass.printConfig();
//				alarmWriterClass.startService();

				Object sendType = _setAlarmType_cbx.getSelectedItem();
				if (AlarmWriterAbstract.ACTION_RAISE.equals(sendType))
				{
					alarmWriterClass.raise(_exampleAlarmEvent);
				}
				else if (AlarmWriterAbstract.ACTION_RE_RAISE.equals(sendType))
				{
					alarmWriterClass.reRaise(_exampleAlarmEvent);
				}
				else if (AlarmWriterAbstract.ACTION_CANCEL.equals(sendType))
				{
					alarmWriterClass.cancel(_exampleAlarmEvent);
				}
				else
				{
					throw new Exception("Unknow send type '"+sendType+"'.");
				}
				
			}
			catch(Exception ex)
			{
				SwingUtils.showErrorMessage("Send dummy alarm", "Some problem sending the dummy alarm to '"+writerClassName+"'.", ex);
			}
		}
    }

	/*---------------------------------------------------
	** END: Action Listeners
	**---------------------------------------------------
	*/

	@Override
	public void setAlarmEvent(AlarmEvent alarmEvent)
	{
		_exampleAlarmEvent = alarmEvent;
		updateExampleText();
	}
	
	/*---------------------------------------------------
	** BEGIN: CaretListener
	**---------------------------------------------------
	*/
	@Override
	public void caretUpdate(CaretEvent e)
	{
		// line and caret is 0 based, so add 1 for readability
		int line = 1 + _editor_txt.getCaretLineNumber();
		int col  = 1 + _editor_txt.getCaretOffsetFromLineStart();
		
		String str = "Caret at: row="+line+", col="+col;
		
		_editorRowCol_lbl.setText(str);
	}
	/*---------------------------------------------------
	** END: CaretListener
	**---------------------------------------------------
	*/

	/*---------------------------------------------------
	** BEGIN: DocumentListener, and helper methods for it
	**---------------------------------------------------
	*/
	private void updateExampleText()
	{
//		for (int p=0; p<_editor_txt.getParserCount(); p++)
//		{
//			_editor_txt.getParser(p).
//		}
		
		for (int p=0; p<_editor_txt.getParserCount(); p++)
		{
			try { _editor_txt.forceReparsing(p); } catch (Throwable ignore) {} // protect from errors in RSyntaxTextArea
		}

//		_warning_lbl.setText("");
//		_warning_lbl.setToolTipText("");
//		_warning_lbl.setVisible(false);
//		_warning_lbl.setForeground(Color.BLACK);
//		
//		try
//		{
//			String type = _setAlarmType_cbx.getSelectedItem()+"";
//			String str = WriterUtils.createMessageFromTemplate(type, _exampleAlarmEvent, _editor_txt.getText(), true);
//			_example_lbl.setText(str);
//		}
//		catch (Exception ex)
//		{
//			String type = "";
//			if (ex instanceof ParseErrorException)
//				type = "Velocity Parser Error: ";
//			
//			_warning_lbl.setText(type + ex.getMessage());
//			_warning_lbl.setToolTipText("<html><pre>" + ex + "</pre></html>");
//			_warning_lbl.setVisible(true);
//			_warning_lbl.setForeground(Color.RED);
//		}
	}
	@Override public void insertUpdate(DocumentEvent e)  { updateExampleText(); }
	@Override public void removeUpdate(DocumentEvent e)  { updateExampleText(); }
	@Override public void changedUpdate(DocumentEvent e) { updateExampleText(); }
	/*---------------------------------------------------
	** END: DocumentListener
	**---------------------------------------------------
	*/


	private void doApply()
	{
		_return = _editor_txt.getText();
	}

	private void setFocus(final JComponent comp)
	{
		// The components needs to be visible for the requestFocus()
		// to work, so lets the EventThreda do it for us after the windows is visible.
		Runnable deferredAction = new Runnable()
		{
			@Override
			public void run()
			{
				comp.requestFocus();
			}
		};
		SwingUtilities.invokeLater(deferredAction);
	}

	/*---------------------------------------------------
	** BEGIN: Property handling
	**---------------------------------------------------
	*/
	private void saveProps()
  	{
		Configuration tmpConf = Configuration.getInstance(Configuration.USER_TEMP);
		String base = this.getClass().getSimpleName()+".";

		if (tmpConf != null)
		{
			tmpConf.setLayoutProperty(base + "window.width",  this.getSize().width);
			tmpConf.setLayoutProperty(base + "window.height", this.getSize().height);
			tmpConf.setLayoutProperty(base + "window.pos.x",  this.getLocationOnScreen().x);
			tmpConf.setLayoutProperty(base + "window.pos.y",  this.getLocationOnScreen().y);

			tmpConf.save();
		}
  	}

  	private void loadProps()
  	{
		int     width     = 900;  // initial window with   if not opened before
		int     height    = 600;  // initial window height if not opened before
		int     x         = -1;
		int     y         = -1;

		Configuration tmpConf = Configuration.getCombinedConfiguration();
		String base = this.getClass().getSimpleName()+".";

		setSize(width, height);

		if (tmpConf == null)
			return;

		width  = tmpConf.getLayoutProperty(base + "window.width",  width);
		height = tmpConf.getLayoutProperty(base + "window.height", height);
		x      = tmpConf.getLayoutProperty(base + "window.pos.x",  -1);
		y      = tmpConf.getLayoutProperty(base + "window.pos.y",  -1);

		if (width != -1 && height != -1)
		{
			setSize(width, height);
		}
		if (x != -1 && y != -1)
		{
			if ( ! SwingUtils.isOutOfScreen(x, y, width, height) )
				this.setLocation(x, y);
		}
		else
		{
			SwingUtils.centerWindow(this);
		}
	}
	/*---------------------------------------------------
	** END: Property handling
	**---------------------------------------------------
	*/
	private class LocalVelocityParser
	implements Parser
	{
		@Override
		public ParseResult parse(RSyntaxDocument doc, String style)
		{
			DefaultParseResult pr = new DefaultParseResult(this);
			pr.clearNotices();
			_warning_lbl.setText("");
			_warning_lbl.setToolTipText("");
			_warning_lbl.setVisible(false);
			_warning_lbl.setForeground(Color.BLACK);
			
			_editorRowCol_lbl.setVisible(false);

			try
			{
				String type = _setAlarmType_cbx.getSelectedItem()+"";
				String str = WriterUtils.createMessageFromTemplate(type, _exampleAlarmEvent, _exampleAlarmEventList, _editor_txt.getText(), true, null, "http://DUMMY-dbxtune:8080");
				_example_lbl.setText(str);
			}
			catch (Exception ex)
			{
				String type = ex.getClass().getSimpleName();

				int    line   = -1;
				int    offset = -1;
				int    length = -1;
				String msg    = ex.getMessage();

				
				if (ex instanceof ParseErrorException)
				{
					ParseErrorException pee = (ParseErrorException)ex;

					type   = "Velocity Parser Error: ";
					line   = pee.getLineNumber()   - 1;
					offset = pee.getColumnNumber() - 1;
//System.out.println("ppe.getInvalidSyntax()="+pee.getInvalidSyntax());
				}
				else if (ex instanceof MethodInvocationException)
				{
					MethodInvocationException mie = (MethodInvocationException)ex;
					
					type   = "Velocity Method Invocation Error: ";
					line   = mie.getLineNumber()   - 1;
					offset = mie.getColumnNumber() - 1;
//System.out.println("mie.getMethodName()=" + mie.getMethodName() + ", mie.getReferenceName()=" + mie.getReferenceName());
				}
//System.out.println("LINE="+line+", COL="+offset+", length="+length+", msg='"+msg+"'.");

				DefaultParserNotice notice;
				if (length == -1)
					notice = new DefaultParserNotice(this, msg, line);
				else
					notice = new DefaultParserNotice(this, msg, line, offset, length);

				pr.addNotice(notice);
				pr.setError(ex);
				
				_warning_lbl.setText(type + msg);
				_warning_lbl.setToolTipText("<html><pre>" + ex + "</pre></html>");
				_warning_lbl.setVisible(true);
				_warning_lbl.setForeground(Color.RED);

				_editorRowCol_lbl.setVisible(true);
			}
			
			return pr;

//			int line = 0;
//			int offset = -1;
//			int length = -1;
//			// org.codehaus.commons.compiler.CompileException: Line 2, Column 13: Non-abstract class "xxx.XxxYyy" must implement method "public abstract void com.dbxtune.alarm.IUserDefinedAlarmInterrogator.interrogateCounterData(com.dbxtune.cm.CountersModel)"
//			if (msg.indexOf("Line ") != -1)
//			{
//				int linePos    = msg.indexOf("Line ");
//				int colonPos   = msg.indexOf(", Column ", linePos);
//				int firstComma = msg.indexOf(",",         linePos);
//				int firstColon = msg.indexOf(":",         linePos);
//				if ( linePos != -1 && colonPos != -1 && firstComma != -1 && firstColon != -1 )
//				{
//					linePos += "Line ".length();
//					colonPos += ", Column ".length();
//
//					line   = StringUtil.parseInt( msg.substring(linePos, firstComma),  1) - 1; // line starts at 0
//					offset = StringUtil.parseInt( msg.substring(colonPos, firstColon), 0) - 1; // offset starts at 0
//					
////					if (offset != -1)
////						try { length = _textarea.getLineEndOffset(line); } catch(BadLocationException ignore) {}
//				}
//			}
		}
		
		@Override
		public boolean isEnabled()
		{
			return true;
		}
		
		@Override
		public URL getImageBase()
		{
			return null;
		}
		
		@Override
		public ExtendedHyperlinkListener getHyperlinkListener()
		{
			return null;
		}
	}
}
