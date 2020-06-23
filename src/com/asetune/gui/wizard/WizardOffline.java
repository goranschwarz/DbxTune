/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package com.asetune.gui.wizard;


import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.apache.log4j.Logger;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.netbeans.api.wizard.WizardDisplayer;
import org.netbeans.spi.wizard.Wizard;
import org.netbeans.spi.wizard.WizardException;
import org.netbeans.spi.wizard.WizardPage;

import com.asetune.pcs.PersistWriterJdbc;
import com.asetune.pcs.PersistentCounterHandler;
import com.asetune.ui.rsyntaxtextarea.RSyntaxTextAreaX;
import com.asetune.ui.rsyntaxtextarea.RSyntaxUtilitiesX;
import com.asetune.utils.Configuration;
import com.asetune.utils.StringUtil;
import com.asetune.utils.SwingUtils;


public class WizardOffline
{
	private static Logger _logger = Logger.getLogger(WizardOffline.class);

    protected static final String MigLayoutHelpConstraints = "wmin 100, span, growx, gapbottom 15, wrap";
	protected static final String MigLayoutConstraints1 = "";
	protected static final String MigLayoutConstraints2 = "[] [grow] []";
	protected static final String MigLayoutConstraints3 = "";
	
	protected static final Dimension preferredSize = new Dimension(880, 600);
	
	public WizardOffline()
	{
		System.clearProperty("wizard.sidebar.image");
		System.setProperty("wizard.sidebar.image", "com/asetune/gui/wizard/WizardOffline.png");

//		BufferedImage img = ImageIO.read (getClass().getResource ("WizardOffline.png"));
//		UIManager.put ("wizard.sidebar.image", img);

		Class<?>[] wca = { 
				WizardOfflinePage0verview.class,
				WizardOfflinePage1 .class,  // ASE info
				WizardOfflinePage2 .class,  // JDBC storage specification
				WizardOfflinePage3 .class,  // What CM's should be recorded.
				WizardOfflinePage4 .class,  // Local CM options
				WizardOfflinePage5 .class,  // SSH info.
				WizardOfflinePage6 .class,  // Capture SQL
				WizardOfflinePage7 .class,  // Sample Time
				WizardOfflinePage8 .class,  // Alarm Writers
				WizardOfflinePage9 .class,  // Alarm Configuration
				WizardOfflinePage10.class,  // Other PCS Writers 
				WizardOfflinePage11.class,  // Daily Report 
				WizardOfflinePage12.class   // Save this to filename
				};
		Wizard wiz = WizardPage.createWizard("Create: Offline Sample Session.", wca, new WizardOffllinePageSummary());
		
		@SuppressWarnings({ "unchecked", "rawtypes" })
		Map<String,String> gatheredSettings = (Map) WizardDisplayer.showWizard(wiz);
//		System.out.println("gatheredSettings="+gatheredSettings);
		try {
			finish(gatheredSettings);
		} catch (WizardException e) {
			e.printStackTrace();
		}

		System.clearProperty("wizard.sidebar.image");
	}

	
	/**
	 * Validate the settings Map, it should only be String/Configuration values. If so print out the key/value/classname
	 * @param settings
	 */
    private void checkSettings(Map<String,String> settings)
	{
		for (Iterator<String> it=settings.keySet().iterator(); it.hasNext();)
		{
			String k = it.next();
			Object o = settings.get(k);
			
			if ( o == null )
				continue;
			
			boolean allow = false;;
			if (o instanceof String)        allow = true;
			if (o instanceof Configuration) allow = true;

			if ( ! allow )
			{
				String className = "-null-";
				if (o != null)
					className = o.getClass().getName();

				_logger.error("Internal ERROR: settings Map must contain STRING only values. The following value violates this: key='"+k+"', val='"+o+"', type='"+className+"'.");
			}
		}
	}

	/**
	 * 
	 * @param settings
	 * @param prefix    The new prefix
	 * @param key       The key in settings.
	 * @param alwaysAdd If the key doesn't exists in 'settings', add it anyway
	 * @param encrypt   If it's a password, we want to encrypt this.
	 */
	private void fixProp(Map<String,String> settings, String prefix, String key, boolean alwaysAdd, boolean encrypt)
	{
		String newKey = prefix + key;
		String originVal = "";
		String val       = "";

		// Check for "UNSAFE" values that has been stored in the Map by the wizard.
		// If there are any, just print into and throw the exception.
		try
		{
			val = settings.get(key);
			originVal = val;
		}
		catch(ClassCastException e)
		{
			for (Iterator<String> it=settings.keySet().iterator(); it.hasNext();)
			{
				String k = it.next();
				Object o = settings.get(k);
				if ( ! (o instanceof String) )
				{
					_logger.error("Internal ERROR: ClassCastException from settingsMap with key='"+k+"', val='"+o+"', type='"+o.getClass().getName()+"'.");
				}
			}
			throw e;
		}

		if (encrypt)
			val = Configuration.encryptPropertyValue(newKey, val+"");

		if (alwaysAdd)
			settings.put(newKey, val);
		else
		{
			if (settings.containsKey(key))
			{
				if (originVal != null && ! originVal.trim().equals(""))
					settings.put(newKey, val);
			}
		}

		settings.remove(key);
	}

	protected Object finish(Map<String,String> settings) throws WizardException
	{
    	if (settings == null)
    		return null;

    	final Configuration offlineProps = new Configuration();
//		Configuration conf = Configuration.getInstance(Configuration.CONF);
		Configuration conf = Configuration.getCombinedConfiguration();

		offlineProps.setFilename(settings.get("storeFile"));
		offlineProps.setEmbeddedMessage("File generated by Offline Wizard.");
		settings.remove("storeFile"); // Noo need to keep this anymore...
		
		//Process some props for PCS functionality, or change name in WizardPanel?
		fixProp(settings, PersistWriterJdbc.PROPKEY_BASE, PersistWriterJdbc.PROPKEY_PART_jdbcDriver,           true, false);
		fixProp(settings, PersistWriterJdbc.PROPKEY_BASE, PersistWriterJdbc.PROPKEY_PART_jdbcUrl,              true, false);
		fixProp(settings, PersistWriterJdbc.PROPKEY_BASE, PersistWriterJdbc.PROPKEY_PART_jdbcUsername,         true, false);
		fixProp(settings, PersistWriterJdbc.PROPKEY_BASE, PersistWriterJdbc.PROPKEY_PART_jdbcPassword,         true, true);
		fixProp(settings, PersistWriterJdbc.PROPKEY_BASE, PersistWriterJdbc.PROPKEY_PART_startH2NetworkServer, true, false);

		fixProp(settings, "conn.",    "dbmsHost",     false, false);
		fixProp(settings, "conn.",    "dbmsName",     false, false);
		fixProp(settings, "conn.",    "dbmsPassword", false, true);
		fixProp(settings, "conn.",    "dbmsPort",     false, false);
		fixProp(settings, "conn.",    "dbmsUsername", false, false);

		fixProp(settings, "conn.",    "sshHostname",  false, false);
		fixProp(settings, "conn.",    "sshPort",      false, false);
		fixProp(settings, "conn.",    "sshUsername",  false, false);
		fixProp(settings, "conn.",    "sshPassword",  false, true);
		fixProp(settings, "conn.",    "sskKeyFile",   false, false);

		fixProp(settings, "offline.", "sampleTime",           true,  false);
		fixProp(settings, "offline.", "startRecordingAtTime", false, false);
		fixProp(settings, "offline.", "shutdownAfterXHours",  false, false);
//		fixProp(settings, "offline.", "storeFile",            true,  false); // not really used, but lets keep it for now...

		// Validate the settings map that it only contains String values
    	checkSettings(settings);

		//Add the PCS Writer, or add it as a wizard property?
		String writerClass = conf.getProperty(PersistentCounterHandler.PROPKEY_WriterClass);
		if(writerClass == null)
			writerClass = "com.asetune.pcs.PersistWriterJdbc";
		settings.put(PersistentCounterHandler.PROPKEY_WriterClass, writerClass);

		// Add any "user defined" PCS Writers (including DbxCentral)
		String pcsWriterClass = settings.get("to-be-discarded.pcsWriterClassCsv");
		if (StringUtil.hasValue(pcsWriterClass))
		{
			List<String> curWritersList = StringUtil.commaStrToList( settings.get(PersistentCounterHandler.PROPKEY_WriterClass) );
			List<String> pcsWritersList = StringUtil.commaStrToList( pcsWriterClass );

			List<String> newWritersList = new ArrayList<>();
			newWritersList.addAll(curWritersList);
			newWritersList.addAll(pcsWritersList);

			settings.put(PersistentCounterHandler.PROPKEY_WriterClass, StringUtil.toCommaStr(newWritersList));
		}
		
//		settings.put("CM.sysMon.test", "testtest");

		boolean previewFile = false;
		for (Iterator<String> iterator = settings.keySet().iterator(); iterator.hasNext();)
		{
			String key = iterator.next();
//			String val = settings.get(key) + "";
			Object obj = settings.get(key);

//			Configuration valConf = null;
			String val = "";
			if (obj instanceof String)
			{
				val = (String) obj;
			}
			else if (obj instanceof Boolean)
			{
				System.out.println("WIZARD-OFFLINE-FINNISH:WARNING(autoconvert: Boolean->String) key='"+key+"', value='"+obj+"'.");
				val = obj + "";
			}
			else if (obj instanceof Configuration)
			{
//				valConf = (Configuration) obj;
			}
			else
			{
				System.out.println("WIZARD-OFFLINE-FINNISH: key='"+key+"', is NOT String/Configuration it is type=" + (obj == null ? "-value-is-null-" : obj.getClass().getName()) );
				continue;
			}
			
			if(key.startsWith("to-be-discarded.")) // these entries will be discarded
			{
				// set preview file
				if (key.equals("to-be-discarded.previewFile"))
					previewFile = val.equalsIgnoreCase("true");

				if(key.startsWith("to-be-discarded.udc."))
				{
					// The value of the property should contain the UserDefined-CmName
					String cmName = val;

					// Check if that cm is a User Defined Counter in the current configuration
					// And then write all the UDC information to the destination file.
					if(getUdcConfigName(cmName) != null)
					{
						String udcPrefix = "udc." + getUdcConfigName(cmName);

						for (String udcKey : conf.getKeys(udcPrefix))
						{
							String udcVal = conf.getProperty(udcKey);
							offlineProps.put(udcKey, udcVal);
						}
					}

					// Check if that cm is a Host Monitor User Defined Counter in the current configuration
					// And then write all the UDC information to the destination file.
					if(getHostMonUdcConfigName(cmName) != null)
					{
						String udcPrefix = "hostmon.udc." + getHostMonUdcConfigName(cmName);

						for (String udcKey : conf.getKeys(udcPrefix))
						{
							String udcVal = conf.getProperty(udcKey);
							offlineProps.put(udcKey, udcVal);
						}
					}
				}

				// AlarmWriters and AlarmConfiguration
				if (key.equals("to-be-discarded.alarmWritersPanelConfig"))
				{
					// Put the Alarm Writer configuration in the output map/file
					offlineProps.add( (Configuration) obj );
				}
				if (key.equals("to-be-discarded.alarmPanelConfig"))
				{
					// Put the Alarm configuration in the output map/file
					offlineProps.add( (Configuration) obj );
				}
				if (key.equals("to-be-discarded.dailySummaryReportConfig"))
				{
					// Put the Alarm configuration in the output map/file
					offlineProps.add( (Configuration) obj );
				}
			}
			else
			{
				// write the key/value
				offlineProps.put(key, val);
			}
		}
		offlineProps.save(true);

		if (_logger.isDebugEnabled())
			offlineProps.print(System.out, "WizardOffline (after save) offlineProps:");

		if (previewFile)
		{
			SwingUtilities.invokeLater(new Runnable() 
			{
				@Override
				public void run() 
				{
					SimpleFileEditor sfe = new SimpleFileEditor(offlineProps.getFilename());
					sfe.setVisible(true);
				}
			});
		}

		//Really you would construct some object or do something with the
		//contents of the map
		return settings;
	}


	private String getUdcConfigName(String name)
    {
    	String        udcConfigName  = null;
    	String        udcPrefix      = "udc.";
//		Configuration conf = Configuration.getInstance(Configuration.CONF);
		Configuration conf = Configuration.getCombinedConfiguration();
    	
		for (String udcKey : conf.getKeys(udcPrefix))
		{
			String udcName = udcKey.substring(udcPrefix.length(), udcKey.indexOf(".", udcPrefix.length()));
			if(udcKey.equals(udcPrefix + name + ".name"))
			{
				udcConfigName = udcName;
				return udcConfigName;
			}
			
		}
		return udcConfigName;
    }
    
    private String getHostMonUdcConfigName(String name)
    {
    	String        udcConfigName  = null;
    	String        udcPrefix      = "hostmon.udc.";
//		Configuration conf = Configuration.getInstance(Configuration.CONF);
		Configuration conf = Configuration.getCombinedConfiguration();
    	
		for (String udcKey : conf.getKeys(udcPrefix))
		{
			String udcName = udcKey.substring(udcPrefix.length(), udcKey.indexOf(".", udcPrefix.length()));
			if(udcKey.equals(udcPrefix + name + ".osCommand"))
			{
				udcConfigName = udcName;
				return udcConfigName;
			}
			
		}
		return udcConfigName;
    }
    
	public static void main(String[] args)
	{
    	try 
    	{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    		//UIManager.setLookAndFeel(new SubstanceOfficeSilver2007LookAndFeel());
		} 
    	catch (Exception e) 
		{
			e.printStackTrace();
		}
    	new WizardOffline();
//    	WizardOffline provider = new WizardOffline();
//		Wizard wizard = provider.createWizard();
//		Object result = WizardDisplayer.showWizard(wizard);
	}

	/**
	 * open a file view
	 * @author gorans
	 */
	private static class SimpleFileEditor 
	extends JFrame 
	{
		private static final long serialVersionUID = 1L;

		private String _filename = null;
		private RSyntaxTextAreaX _textArea = new RSyntaxTextAreaX();

		public SimpleFileEditor(String filename)
		{
			super(filename);
			_filename = filename;
			JPanel panel = new JPanel(new BorderLayout());

			setJMenuBar(createMenuBar());

			StringBuffer fileData = new StringBuffer(1000);
			try
			{
				BufferedReader reader = new BufferedReader(new FileReader(filename));
				char[] buf = new char[1024];
				int numRead=0;
				while((numRead=reader.read(buf)) != -1)
				{
					String readData = String.valueOf(buf, 0, numRead);
					fileData.append(readData);
					buf = new char[1024];
				}
				reader.close();
			}
			catch(IOException e)
			{
				fileData.append("Problems open the file '").append(filename).append("'.");
				SwingUtils.showErrorMessage("Problems open file", "Problems open the file '"+filename+"'.", e);
			}

			String fileContent = fileData.toString();
//			System.out.println("------------------------------------------------------");
//			System.out.println(fileContent);
//			System.out.println("------------------------------------------------------");
			
			// ADD Ctrl-s (as save)
			_textArea.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK), "save");

			_textArea.getActionMap().put("save", new AbstractAction("save")
			{
				private static final long	serialVersionUID	= 1L;
				@Override
				public void actionPerformed(ActionEvent e)
				{
					saveToFile(_filename);
				}
			});

			_textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_PROPERTIES_FILE);
			_textArea.setText(fileContent);
			_textArea.setCaretPosition(0);
			
			RTextScrollPane textArea_scroll = new RTextScrollPane(_textArea);
			panel.add(textArea_scroll);
		
			RSyntaxUtilitiesX.installRightClickMenuExtentions(textArea_scroll, this);

			setContentPane(panel);
			setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			pack();

			Dimension s = getSize();
			setSize(SwingUtils.getSizeWithingScreenLimit(s.width, s.height, 100));
			SwingUtils.centerWindow(this);
		}

		private JMenuBar createMenuBar()
		{
			JMenuBar menuBar = new JMenuBar();

			JMenu     file = new JMenu("File");
			JMenuItem save   = new JMenuItem("Save   Ctrl+S");
			JMenuItem saveAs = new JMenuItem("Save as...");
			JMenuItem exit   = new JMenuItem("Exit");

			save.addActionListener(new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					saveToFile(_filename);
				}
			});

			saveAs.addActionListener(new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
//					String envName = DbxTune.getInstance().getAppSaveDirEnvName();  // ASETUNE_SAVE_DIR
					String envName = "DBXTUNE_SAVE_DIR";
					String saveDir = StringUtil.getEnvVariableValue(envName);
							
					JFileChooser fc = new JFileChooser(_filename);
					if (saveDir != null)
						fc.setCurrentDirectory(new File(saveDir));

					int returnVal = fc.showOpenDialog(null);
					if (returnVal == JFileChooser.APPROVE_OPTION) 
			        {
						File file = fc.getSelectedFile();

						//This is where a real application would open the file.
						String filename = file.getAbsolutePath();

						saveToFile(filename);
			        }
				}
			});

			exit.addActionListener(new ActionListener() 
			{
				@Override
				public void actionPerformed(ActionEvent event) 
				{
					setVisible(false);
					dispose();
				}
			});

			file.add(save);
			file.add(saveAs);
			file.add(exit);

			menuBar.add(file);

			return menuBar;
		}

		private void saveToFile(String filename)
		{
			// Save file
			try
			{
				OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(filename));
				out.write(_textArea.getText());

				out.flush();
				out.close();
				
				_filename = filename;
				setTitle(_filename);
			} 
			catch(Exception ex)
			{
				SwingUtils.showErrorMessage("Problems saving file", "Problems saving the file '"+filename+"'.", ex);
			}
		}
	}
}

