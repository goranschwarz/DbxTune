/*******************************************************************************
 * Copyright (C) 2010-2027 Goran Schwarz
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
package com.dbxtune.gui.wizard;


import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.netbeans.api.wizard.WizardDisplayer;
import org.netbeans.spi.wizard.Wizard;
import org.netbeans.spi.wizard.WizardException;
import org.netbeans.spi.wizard.WizardPage;
import org.reflections.Reflections;

import com.dbxtune.AseTune;
import com.dbxtune.CounterController;
import com.dbxtune.Version;
import com.dbxtune.alarm.AlarmHandler;
import com.dbxtune.central.DbxTuneCentral;
import com.dbxtune.alarm.writers.AlarmWriterToFile;
import com.dbxtune.alarm.writers.AlarmWriterToMail;
import com.dbxtune.alarm.writers.IAlarmWriter;
import com.dbxtune.cm.CmSettingsHelper;
import com.dbxtune.cm.CounterModelHostMonitor;
import com.dbxtune.cm.CounterSetTemplates;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.cm.os.CmOsUtils;
import com.dbxtune.gui.MainFrame;
import com.dbxtune.pcs.PersistWriterJdbc;
import com.dbxtune.pcs.PersistWriterToDbxCentral;
import com.dbxtune.pcs.PersistWriterToHttpJson;
import com.dbxtune.pcs.PersistentCounterHandler;
import com.dbxtune.pcs.report.DailySummaryReportFactory;
import com.dbxtune.pcs.report.senders.ReportSenderToMail;
import com.dbxtune.ui.rsyntaxtextarea.RSyntaxTextAreaX;
import com.dbxtune.ui.rsyntaxtextarea.RSyntaxUtilitiesX;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.StringUtil;
import com.dbxtune.utils.SwingUtils;
import com.dbxtune.utils.TimeUtils;


public class WizardOffline
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	protected static final String MigLayoutHelpConstraints = "wmin 100, span, growx, gapbottom 15, wrap";
	protected static final String MigLayoutConstraints1 = "";
	protected static final String MigLayoutConstraints2 = "[] [grow] []";
	protected static final String MigLayoutConstraints3 = "";
	
	protected static final Dimension preferredSize = new Dimension(880, 600);

	// Static flag, set before wizard pages are instantiated
	private static boolean _externalNoGuiConfigWizard = false;

	public static boolean isExternalNoGuiConfigWizard() 
	{
		return _externalNoGuiConfigWizard;
	}
	
	public WizardOffline()
	{
		this(false);
	}
	public WizardOffline(boolean externalNoGuiGonfigWizard)
	{
		System.clearProperty("wizard.sidebar.image");
		System.setProperty("wizard.sidebar.image", "com/dbxtune/gui/wizard/WizardOffline.png");

		_externalNoGuiConfigWizard = externalNoGuiGonfigWizard;

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
		
		if (_externalNoGuiConfigWizard)
		{
			//System.exit(0);

			// Instead of EXIT, send a "CLOSE" event to the EDT (Event Dispatch Thread)
			WindowEvent closingEvent = new WindowEvent(MainFrame.getInstance(), WindowEvent.WINDOW_CLOSING);
			Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(closingEvent);
		}
	}

	/** Used by createFileNoGui(): no wizard is shown, just write the file from the passed settings */
	private WizardOffline(Map<String,String> settings)
	throws WizardException
	{
		finish(settings);
	}

	/** Mail address written in files from createFileNoGui(), to be changed by the person installing */
	public static final String NOGUI_DUMMY_MAIL_ADDRESS  = "fixme@fixme.com";
	public static final String NOGUI_DUMMY_SMTP_HOSTNAME = "smtp.fixme.com";

	/**
	 * Create a 'Recording Session - Template file' without a GUI.
	 * <p>
	 * Uses the same presets as when the wizard is started with <code>--cfgWizard</code> (from DbxInstaller), and the CM defaults:
	 * <ul>
	 *   <li>DBMS and SSH connection information: from command line switches (not in the file)</li>
	 *   <li>H2 recording database, with the H2 network server started</li>
	 *   <li>CMs recorded: those with a template level at or below <code>templateLevel</code> (also written as <code>CounterSet.template.default</code>)</li>
	 *   <li>Sample time 30 seconds</li>
	 *   <li>Alarm writers: AlarmWriterToFile and AlarmWriterToMail (mail addresses: {@link #NOGUI_DUMMY_MAIL_ADDRESS})</li>
	 *   <li>Recordings sent to DbxCentral, Daily Summary Report enabled</li>
	 * </ul>
	 * The CMs must be created (CounterController.getInstance().getCmList()) before this is called.
	 *
	 * @param filename       File to write
	 * @param templateLevel  SMALL, MEDIUM, LARGE or ALL
	 * @param noHostMon      true = do not record any Host Monitoring CM (CmOsXxx), whatever the template level says
	 * @return The filename written
	 */
	public static String createFileNoGui(String filename, CounterSetTemplates.Type templateLevel, boolean noHostMon)
	throws WizardException
	{
		Map<String,Object> settings = new LinkedHashMap<>();

		// Page 1 + 5: DBMS and SSH connection -- "Use Command Line Switches" (nothing in the file)

		// Page 2: Recording database
		settings.put(PersistWriterJdbc.PROPKEY_PART_jdbcDriver,           "org.h2.Driver");
		settings.put(PersistWriterJdbc.PROPKEY_PART_jdbcUrl,              "jdbc:h2:file:${DBXTUNE_SAVE_DIR}/${SERVERNAME}_${DATE:format=yyyy-MM-dd;roll=true}");
		settings.put(PersistWriterJdbc.PROPKEY_PART_jdbcUsername,         "sa");
		settings.put(PersistWriterJdbc.PROPKEY_PART_jdbcPassword,         "");
		settings.put(PersistWriterJdbc.PROPKEY_PART_startH2NetworkServer, "true");

		// Page 3: What CMs to record (same rule as the "Use template" combo in the wizard)
		settings.put(CounterSetTemplates.PROPKEY_templateDefaultName, templateLevel.toString());
		for (CountersModel cm : CounterController.getInstance().getCmList())
		{
			boolean storePcs = cm.getTemplateLevel().ordinal() <= templateLevel.ordinal();
			if (cm.getName().equals(CounterController.getSummaryCmName()))
				storePcs = true;
			if (noHostMon && cm instanceof CounterModelHostMonitor)
				storePcs = false;

			String cmName = cm.getName();
			settings.put("to-be-discarded.udc." + cmName, cmName);
			settings.put(cmName + "." + CountersModel.PROPKEY_queryTimeout,         Configuration.USE_DEFAULT_PREFIX + cm.getDefaultQueryTimeout());
			settings.put(cmName + "." + CountersModel.PROPKEY_postponeTime,         Configuration.USE_DEFAULT_PREFIX + cm.getDefaultPostponeTime());
			settings.put(cmName + "." + CountersModel.PROPKEY_persistCounters,      storePcs + "");
			settings.put(cmName + "." + CountersModel.PROPKEY_persistCounters_abs,  Configuration.USE_DEFAULT_PREFIX + cm.getDefaultIsPersistCountersAbsEnabled());
			settings.put(cmName + "." + CountersModel.PROPKEY_persistCounters_diff, Configuration.USE_DEFAULT_PREFIX + cm.getDefaultIsPersistCountersDiffEnabled());
			settings.put(cmName + "." + CountersModel.PROPKEY_persistCounters_rate, Configuration.USE_DEFAULT_PREFIX + cm.getDefaultIsPersistCountersRateEnabled());

			// Page 4: CM local options (defaults)
			List<CmSettingsHelper> localSettings = cm.getLocalSettings();
			if (localSettings != null)
			{
				for (CmSettingsHelper cmsh : localSettings)
					settings.put(cmsh.getPropName(), Configuration.USE_DEFAULT_PREFIX + cmsh.getDefaultValue());
			}
		}

		// Page 6: Capture SQL (only for AseTune and SqlServerTune)
		if ("AseTune".equals(Version.getAppName()) || "SqlServerTune".equals(Version.getAppName()))
		{
			Object[][] sqlCapDefaults = {
				{ PersistentCounterHandler.PROPKEY_ddl_doDdlLookupAndStore                 , PersistentCounterHandler.DEFAULT_ddl_doDdlLookupAndStore                  },
				{ PersistentCounterHandler.PROPKEY_ddl_enabledForDatabaseObjects           , PersistentCounterHandler.DEFAULT_ddl_enabledForDatabaseObjects            },
				{ PersistentCounterHandler.PROPKEY_ddl_enabledForStatementCache            , PersistentCounterHandler.DEFAULT_ddl_enabledForStatementCache             },
				{ PersistentCounterHandler.PROPKEY_ddl_afterDdlLookupSleepTimeInMs         , PersistentCounterHandler.DEFAULT_ddl_afterDdlLookupSleepTimeInMs          },
				{ PersistentCounterHandler.PROPKEY_ddl_addDependantObjectsToDdlInQueue     , PersistentCounterHandler.DEFAULT_ddl_addDependantObjectsToDdlInQueue      },
				{ PersistentCounterHandler.PROPKEY_sqlCap_doSqlCaptureAndStore             , PersistentCounterHandler.DEFAULT_sqlCap_doSqlCaptureAndStore              },
				{ PersistentCounterHandler.PROPKEY_sqlCap_sleepTimeInMs                    , PersistentCounterHandler.DEFAULT_sqlCap_sleepTimeInMs                     },
				{ PersistentCounterHandler.PROPKEY_sqlCap_doSqlText                        , PersistentCounterHandler.DEFAULT_sqlCap_doSqlText                         },
				{ PersistentCounterHandler.PROPKEY_sqlCap_doStatementInfo                  , PersistentCounterHandler.DEFAULT_sqlCap_doStatementInfo                   },
				{ PersistentCounterHandler.PROPKEY_sqlCap_doPlanText                       , PersistentCounterHandler.DEFAULT_sqlCap_doPlanText                        },
				{ PersistentCounterHandler.PROPKEY_sqlCap_saveStatement_gt_execTime        , PersistentCounterHandler.DEFAULT_sqlCap_saveStatement_gt_execTime         },
				{ PersistentCounterHandler.PROPKEY_sqlCap_saveStatement_gt_logicalReads    , PersistentCounterHandler.DEFAULT_sqlCap_saveStatement_gt_logicalReads     },
				{ PersistentCounterHandler.PROPKEY_sqlCap_saveStatement_gt_physicalReads   , PersistentCounterHandler.DEFAULT_sqlCap_saveStatement_gt_physicalReads    },
				{ PersistentCounterHandler.PROPKEY_sqlCap_sendDdlForLookup                 , PersistentCounterHandler.DEFAULT_sqlCap_sendDdlForLookup                  },
				{ PersistentCounterHandler.PROPKEY_sqlCap_sendDdlForLookup_gt_execTime     , PersistentCounterHandler.DEFAULT_sqlCap_sendDdlForLookup_gt_execTime      },
				{ PersistentCounterHandler.PROPKEY_sqlCap_sendDdlForLookup_gt_logicalReads , PersistentCounterHandler.DEFAULT_sqlCap_sendDdlForLookup_gt_logicalReads  },
				{ PersistentCounterHandler.PROPKEY_sqlCap_sendDdlForLookup_gt_physicalReads, PersistentCounterHandler.DEFAULT_sqlCap_sendDdlForLookup_gt_physicalReads },
			};
			for (Object[] entry : sqlCapDefaults)
				settings.put((String) entry[0], Configuration.USE_DEFAULT_PREFIX + entry[1]);
		}

		// Page 7: Sample time
		settings.put("sampleTime", "30");

		// Page 8: Alarm writers: File + Mail (mandatory and "probable" settings, with defaults or the dummy mail values)
		settings.put("to-be-discarded.enableAlarmHandling", "true");
		settings.put(AlarmHandler.PROPKEY_enable,           "true");

		Map<String,String> dummyMailValues = new LinkedHashMap<>();
		dummyMailValues.put(AlarmWriterToMail.PROPKEY_smtpHostname, NOGUI_DUMMY_SMTP_HOSTNAME);
		dummyMailValues.put(AlarmWriterToMail.PROPKEY_to,           NOGUI_DUMMY_MAIL_ADDRESS);
		dummyMailValues.put(AlarmWriterToMail.PROPKEY_from,         NOGUI_DUMMY_MAIL_ADDRESS);

		Configuration alarmWritersConf = new Configuration();
		IAlarmWriter[] alarmWriters = { new AlarmWriterToFile(), new AlarmWriterToMail() };
		List<String> alarmWriterClasses = new ArrayList<>();
		for (IAlarmWriter writer : alarmWriters)
		{
			alarmWriterClasses.add(writer.getClass().getName());
			for (CmSettingsHelper cmsh : writer.getAvailableSettings())
			{
				String key   = cmsh.getPropName();
				String dummy = dummyMailValues.get(key);
				if (dummy != null)
					alarmWritersConf.setProperty(key, dummy);
				else if (cmsh.isMandatory() || cmsh.isProbable())
					alarmWritersConf.setProperty(key, Configuration.USE_DEFAULT_PREFIX + cmsh.getDefaultValue());
			}
		}
		alarmWritersConf.setProperty(AlarmHandler.PROPKEY_WriterClass, StringUtil.toCommaStr(alarmWriterClasses));
		settings.put("to-be-discarded.alarmWritersPanelConfig", alarmWritersConf);

		// Page 10: Send recordings to DbxCentral (HTTP)
		String dbxCentralUrlKey = PersistWriterToHttpJson.replaceKey(PersistWriterToHttpJson.PROPKEY_url, "PersistWriterToDbxCentral", "");
		settings.put("to-be-discarded.pcsWriterClassCsv",          PersistWriterToDbxCentral.class.getName());
		settings.put(PersistWriterToDbxCentral.PROPKEY_writerType, PersistWriterToDbxCentral.WriterType.HTTP.toString().toLowerCase());
		settings.put(dbxCentralUrlKey,                             Configuration.USE_DEFAULT_PREFIX + PersistWriterToHttpJson.DEFAULT_url); // USE_DEFAULT: the port is resolved on the machine the collector runs on (Windows 80, else 8080)

		// Page 11: Daily Summary Report, sent with ReportSenderToMail (all settings, with defaults or the dummy mail values)
		// Defaults as 'USE_DEFAULT:' so no machine specific values (like the save dir) end up in the file
		settings.put(DailySummaryReportFactory.PROPKEY_create                , "true");
		settings.put(DailySummaryReportFactory.PROPKEY_filter_keep_servername, Configuration.USE_DEFAULT_PREFIX + DailySummaryReportFactory.DEFAULT_filter_keep_servername);
		settings.put(DailySummaryReportFactory.PROPKEY_filter_skip_servername, Configuration.USE_DEFAULT_PREFIX + DailySummaryReportFactory.DEFAULT_filter_skip_servername);
		settings.put(DailySummaryReportFactory.PROPKEY_reportClassname       , Configuration.USE_DEFAULT_PREFIX + DailySummaryReportFactory.DEFAULT_reportClassname);
		settings.put(DailySummaryReportFactory.PROPKEY_senderClassname       , DailySummaryReportFactory.DEFAULT_senderClassname); // NOT 'USE_DEFAULT:', saveFileWithComments() reads it to write the sender's settings (else they are written as comments)
		settings.put(DailySummaryReportFactory.PROPKEY_save                  , Configuration.USE_DEFAULT_PREFIX + DailySummaryReportFactory.DEFAULT_save);
		settings.put(DailySummaryReportFactory.PROPKEY_saveDir               , Configuration.USE_DEFAULT_PREFIX + DailySummaryReportFactory.DEFAULT_saveDir);

		dummyMailValues.clear();
		dummyMailValues.put(ReportSenderToMail.PROPKEY_smtpHostname, NOGUI_DUMMY_SMTP_HOSTNAME);
		dummyMailValues.put(ReportSenderToMail.PROPKEY_to,           NOGUI_DUMMY_MAIL_ADDRESS);
		dummyMailValues.put(ReportSenderToMail.PROPKEY_from,         NOGUI_DUMMY_MAIL_ADDRESS);
		for (CmSettingsHelper cmsh : new ReportSenderToMail().getAvailableSettings())
		{
			String dummy = dummyMailValues.get(cmsh.getPropName());
			settings.put(cmsh.getPropName(), dummy != null ? dummy : Configuration.USE_DEFAULT_PREFIX + cmsh.getDefaultValue());
		}

		// Page 12: File to save to
		settings.put("storeFile", filename);
		settings.put("to-be-discarded.saveSorted",  "false");
		settings.put("to-be-discarded.previewFile", "false");

		@SuppressWarnings({ "unchecked", "rawtypes" })
		Map<String,String> settingsMap = (Map) settings;
		new WizardOffline(settingsMap);

		return filename;
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

				_logger.error("Internal ERROR: settings Map must contain STRING only values. The following value violates this: key='" + k + "', val='" + o + "', type='" + className + "'.");
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
					_logger.error("Internal ERROR: ClassCastException from settingsMap with key='" + k + "', val='" + o + "', type='" + o.getClass().getName() + "'.");
				}
			}
			throw e;
		}

		if (encrypt)
			val = Configuration.encryptPropertyValue(newKey, val + "");

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
			writerClass = "com.dbxtune.pcs.PersistWriterJdbc";
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
		boolean saveAsSorted = false;
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
				System.out.println("WIZARD-OFFLINE-FINNISH:WARNING(autoconvert: Boolean->String) key='" + key + "', value='" + obj + "'.");
				val = obj + "";
			}
			else if (obj instanceof Configuration)
			{
//				valConf = (Configuration) obj;
			}
			else
			{
				System.out.println("WIZARD-OFFLINE-FINNISH: key='" + key + "', is NOT String/Configuration it is type=" + (obj == null ? "-value-is-null-" : obj.getClass().getName()) );
				continue;
			}

			if(key.startsWith("to-be-discarded.")) // these entries will be discarded
			{
				// set preview file
				if (key.equals("to-be-discarded.saveSorted"))
					saveAsSorted = val.equalsIgnoreCase("true");

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
//		offlineProps.save(true);
		String savedFilename = saveFile(offlineProps, saveAsSorted);

		if (_logger.isDebugEnabled())
			offlineProps.print(System.out, "WizardOffline (after save) offlineProps:");

		if (previewFile)
		{
			SwingUtilities.invokeLater(new Runnable() 
			{
				@Override
				public void run() 
				{
					SimpleFileEditor sfe = new SimpleFileEditor(savedFilename);
					sfe.setVisible(true);
				}
			});
		}

		//Really you would construct some object or do something with the
		//contents of the map
		return settings;
	}

	/**
	 * Save the file somewhere
	 */
	private String saveFile(Configuration conf, boolean sortCmByName)
	{
//		String savetoFile = conf.getFilename();
//
//		// Simply save the file using the default save method in Configuration
//		conf.save(true);
//		
//		return savetoFile;

		return saveFileWithComments(conf, sortCmByName);
	}

	/**
	 * Save the file somewhere
	 */
	private String saveFileWithComments(Configuration originConf, boolean sortCmByName)
	{
		String savetoFile = originConf.getFilename();

		// Take a copy of the input configuration... This will be used to: 
		//  - copy from
		//  - print
		//  - delete the just printed entries
		// At the end, we just print all the ones that we have not yet handled above
		
		Configuration cc = new Configuration();
//		cc.add(originConf);
		cc.putAll(originConf);
		
		// Get All CmNames from the Configuration (or from CounterCollector)
		// Get All CmOsNames from the package (scans "com.dbxtune.cm.os" and returns all that starts with CmOs)
		// The remove all CmOs from the CmList
		List<String> cmNames   = Collections.emptyList();
		if (sortCmByName)
		{
			cmNames = cc.getUniqueKeys("Cm");
		}
		else
		{
			cmNames = CounterController.hasInstance() ? CounterController.getInstance().getCmListAsStrings() : cc.getUniqueKeys("Cm");
		}
		List<String> cmOsNames = getCmOsNames();
		cmNames.removeAll(cmOsNames);
		
		
		File f = new File(savetoFile);
		try (FileOutputStream os = new FileOutputStream(f); BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(os, "8859_1")))
		{
			// Write Header
			String s = originConf.getEmbeddedMessage();
			writeln(bw, "#======================================================="); 
			if (s != null)
			{
				writeln(bw, "# " + s);
			}
			writeln(bw, "# Last save time: " + TimeUtils.toStringYmdHms(System.currentTimeMillis()));
			writeln(bw, "#-------------------------------------------------------");
			writeln(bw, "");

			//-----------------------------------------------
			// Write the different sections
			//-----------------------------------------------

			// offline.sampleTime
			
			// CounterSet.template.default

			// Normal CM's
			// - First the CM
			// - Then Settings
			// - Then alarms
			
			// OS Monitor
			// - First the CM
			// - Then Settings
			// - Then alarms
			
			// AlarmHandler
			// AlarmHandler.WriterClass
			// AlarmWriter*
			
			// DailySummaryReport
			// DailySummaryReport.sender.classname
			// ReportSender*
			
			// PersistentCounterHandler.WriterClass
			// PersistWriter*

			// OTHER STUFF NOT COVERED IN ABOVE SECTIONS
			
			writeln(bw, "");
			writeln(bw, "");
			writeln(bw, "##---------------------------------------------------------");
			writeln(bw, "## How many second to sleep between sampling data");
			writeln(bw, "##---------------------------------------------------------");
			writeKey(bw, cc, "offline.sampleTime");


			//---------------------------------------------------------------------------------
			writeln(bw, "");
			writeln(bw, "");
			writeln(bw, "");
			writeln(bw, "");
			writeln(bw, "##---------------------------------------------------------");
			writeln(bw, "## What is the default Counter template to use.");
			writeln(bw, "## This will be used for future CounterModels that are not yet created.");
			writeln(bw, "## So if a new CM is part of template 'SMALL|MEDIUM|LARGE|ALL', it will still be created, even if not part of this config file.");
			writeln(bw, "##---------------------------------------------------------");
			writeKey(bw, cc, CounterSetTemplates.PROPKEY_templateDefaultName);


			//---------------------------------------------------------------------------------
			writeln(bw, "");
			writeln(bw, "");
			writeln(bw, "");
			writeln(bw, "");
			writeln(bw, "##---------------------------------------------------------");
			writeln(bw, "##---- BEGIN: DBMS -- Connection Information");
			writeln(bw, "##---------------------------------------------------------");
			writeln(bw, "## Specified with cmd line parameters: -U|--user <user> -P|--passwd <passwd> -S|--server <srvname[:port]>");
			if (cc.containsKey("conn.dbmsHost"    )) { writeKey(bw, cc, "conn.dbmsHost");     } else { writeln(bw, "#conn.dbmsHost     = xxx"); }
			if (cc.containsKey("conn.dbmsPort"    )) { writeKey(bw, cc, "conn.dbmsPort");     } else { writeln(bw, "#conn.dbmsPort     = xxx"); }
			if (cc.containsKey("conn.dbmsUsername")) { writeKey(bw, cc, "conn.dbmsUsername"); } else { writeln(bw, "#conn.dbmsUsername = xxx"); }
			if (cc.containsKey("conn.dbmsPassword")) { writeKey(bw, cc, "conn.dbmsPassword"); } else { writeln(bw, "#conn.dbmsPassword = xxx"); }
			writeln(bw, "## NOTE: If no 'conn.dbmsPassword' is specified, the password will be fetched from ${HOME}/.passwd.enc");
			writeln(bw, "##       which can be maintained using: dbxPassword.sh or dbxPassword.bat");
			writeln(bw, "##---------------------------------------------------------");
			writeln(bw, "##---- END: DBMS information");
			writeln(bw, "##---------------------------------------------------------");


			//---------------------------------------------------------------------------------
			writeln(bw, "");
			writeln(bw, "");
			writeln(bw, "");
			writeln(bw, "");
			writeln(bw, "##---------------------------------------------------------");
			writeln(bw, "##---- BEGIN: OS Monitoring -- Connection Information");
			writeln(bw, "##---------------------------------------------------------");
			writeln(bw, "## Specified with cmd line parameters: -u|--sshUser <user> -p|--sshPasswd <passwd> -s|--sshServer <srvname[:port]> -k|sshKeyFile <keyfile>");
			if (cc.containsKey("conn.sshHostname")) { writeKey(bw, cc, "conn.sshHostname"); } else { writeln(bw, "#conn.sshHostname = xxx"); }
			if (cc.containsKey("conn.sshPort"    )) { writeKey(bw, cc, "conn.sshPort");     } else { writeln(bw, "#conn.sshPort     = xxx"); }
			if (cc.containsKey("conn.sshUsername")) { writeKey(bw, cc, "conn.sshUsername"); } else { writeln(bw, "#conn.sshUsername = xxx"); }
			if (cc.containsKey("conn.sshPassword")) { writeKey(bw, cc, "conn.sshPassword"); } else { writeln(bw, "#conn.sshPassword = xxx"); }
			if (cc.containsKey("conn.sskKeyFile" )) { writeKey(bw, cc, "conn.sskKeyFile");  } else { writeln(bw, "#conn.sskKeyFile  = xxx"); }
			writeln(bw, "## NOTE: If no 'conn.sshPassword' is specified, the password will be fetched from ${HOME}/.passwd.enc");
			writeln(bw, "##       which can be maintained using: dbxPassword.sh or dbxPassword.bat");
			writeln(bw, "##---------------------------------------------------------");
			writeln(bw, "##---- END: OS Monitoring information");
			writeln(bw, "##---------------------------------------------------------");


			//---------------------------------------------------------------------------------
			writeln(bw, "");
			writeln(bw, "");
			writeln(bw, "");
			writeln(bw, "");
			writeln(bw, "##---------------------------------------------------------");
			writeln(bw, "## Below are all DBMS Counter Models and it's parameters");
			writeln(bw, "##---------------------------------------------------------");
			writeln(bw, "");
			for (String cmName : cmNames)
			{
				writeCmSection(bw, cc, cmName);
			}


			//---------------------------------------------------------------------------------
			writeln(bw, "");
			writeln(bw, "");
			writeln(bw, "");
			writeln(bw, "");
			writeln(bw, "##---------------------------------------------------------");
			writeln(bw, "## Below are all OS Counter Models and it's parameters");
			writeln(bw, "##---------------------------------------------------------");
			writeln(bw, "");
			for (String cmOsName : cmOsNames)
			{
				writeCmSection(bw, cc, cmOsName);
			}


			//---------------------------------------------------------------------------------
			writeln(bw, "");
			writeln(bw, "");
			writeln(bw, "");
			writeln(bw, "");
			writeln(bw, "##---------------------------------------------------------");
			writeln(bw, "##---- BEGIN: User Defined Counters");
			writeln(bw, "##---------------------------------------------------------");
			writeln(bw, "## User Defined Counters can be created with the GUI: Menu -> Tools -> Create 'User Defined Counter' Wizard... ");
			// Possibly: implement below to show/write User Defined Counter Models
			//            - Get All CM's from CounterController
			//            - Remove any System Provided CM's ( cm.isSystemCm() or starting with "Cm" ) 
			//            - What's left would be User Defined Counters :)
			//           But since "no one" (or few) are using them, we can implement this at a later stage
			writeln(bw, "##---------------------------------------------------------");
			writeln(bw, "##---- END: User Defined Counters");
			writeln(bw, "##---------------------------------------------------------");


			//---------------------------------------------------------------------------------
			writeln(bw, "");
			writeln(bw, "");
			writeln(bw, "");
			writeln(bw, "");
			writeln(bw, "##---------------------------------------------------------");
			writeln(bw, "##---- BEGIN: PCS - Persistent Counter Store");
			writeln(bw, "##---------------------------------------------------------");

			// Remove all 'PersistentCounterHandler.sqlCapture.*' for Collectors that DOES NOT Support that
			List<String> DbxSuportsSqlCapture = Arrays.asList(new String[] {AseTune.APP_NAME} );
			if ( ! DbxSuportsSqlCapture.contains(Version.getAppName()) )
			{
				List<String> sqlCaptureKeys = cc.getKeys("PersistentCounterHandler.sqlCapture");
				for (String key : sqlCaptureKeys)
				{
					cc.remove(key);
				}
			}

			// Get keys and Writers
			List<String> pcsKeys    = cc.getKeys("PersistentCounterHandler");
			List<String> pcsWriters = StringUtil.parseCommaStrToList(cc.getProperty("PersistentCounterHandler.WriterClass", ""), true);

			// Write all keys 'PersistentCounterHandler.*'
			for (String key : pcsKeys)
			{
				writeKey(bw, cc, key);
			}
			writeln(bw, "");

			// Writers
			for (String writerKey : pcsWriters)
			{
				writerKey = StringUtils.substringAfterLast(writerKey, ".");
				writeln(bw, "## Writer: " + writerKey + " -------------------------");
				List<String> writerKeys = cc.getKeys(writerKey);
				for (String key : writerKeys)
				{
					// The default URL depends on the platform the collector runs on (DbxCentral's web port)
					if (key.equals(PersistWriterToHttpJson.replaceKey(PersistWriterToHttpJson.PROPKEY_url, "PersistWriterToDbxCentral", "")))
					{
						writeln(bw, "## Default URL is 'http://localhost:<port>/api/pcs/receiver', where <port> is DbxCentral's web port on the machine the collector runs on:");
						writeln(bw, "##   - Windows:    " + DbxTuneCentral.DEFAULT_WEB_PORT_WINDOWS + " (or property '" + DbxTuneCentral.PROPKEY_WEB_PORT_WINDOWS + "')");
						writeln(bw, "##   - Linux/Unix: " + DbxTuneCentral.DEFAULT_WEB_PORT         + " (or property '" + DbxTuneCentral.PROPKEY_WEB_PORT         + "')");
					}
					writeKey(bw, cc, key);
				}
				writeln(bw, "");
			}
			writeln(bw, "##---------------------------------------------------------");
			writeln(bw, "##---- END: PCS - Persistent Counter Store");
			writeln(bw, "##---------------------------------------------------------");


			
			//---------------------------------------------------------------------------------
			writeln(bw, "");
			writeln(bw, "");
			writeln(bw, "");
			writeln(bw, "");
			writeln(bw, "##---------------------------------------------------------");
			writeln(bw, "##---- BEGIN: Fallback mail Properties");
			writeln(bw, "##---------------------------------------------------------");
			String[] prefixArr = new String[] {"AlarmWriterToMail", "ReportSenderToMail"};
			writeMailTemplate(bw, cc, "to"                  , "mail", "someone@acme.com", prefixArr);
			writeMailTemplate(bw, cc, "from"                , "mail", "dbxtune"         , prefixArr);
			writeMailTemplate(bw, cc, "smtp.hostname"       , "mail", "smtp.acme.com"   , prefixArr);
			writeMailTemplate(bw, cc, "smtp.port"           , "mail", "###"             , prefixArr);
			writeMailTemplate(bw, cc, "smtp.username"       , "mail", "someUser"        , prefixArr);
			writeMailTemplate(bw, cc, "smtp.password"       , "mail", "**secret**"      , prefixArr);
			writeMailTemplate(bw, cc, "ssl.port"            , "mail", "###"             , prefixArr);
			writeMailTemplate(bw, cc, "ssl.use"             , "mail", "{true|false}"    , prefixArr);
			writeMailTemplate(bw, cc, "start.tls"           , "mail", "{true|false}"    , prefixArr);
			writeMailTemplate(bw, cc, "start.tls.required"  , "mail", "{true|false}"    , prefixArr);
			writeMailTemplate(bw, cc, "smtp.connect.timeout", "mail", "###"             , prefixArr);
			writeln(bw, "##---------------------------------------------------------");
			writeln(bw, "##---- END: Fallback mail Properties");
			writeln(bw, "##---------------------------------------------------------");



			//---------------------------------------------------------------------------------
			writeln(bw, "");
			writeln(bw, "");
			writeln(bw, "");
			writeln(bw, "");
			writeln(bw, "##---------------------------------------------------------");
			writeln(bw, "##---- BEGIN: Alarm Handler");
			writeln(bw, "##---------------------------------------------------------");
			List<String> alarmKeys    = cc.getKeys("AlarmHandler");
			List<String> alarmWriters = StringUtil.parseCommaStrToList(cc.getProperty("AlarmHandler.WriterClass", ""), true);

			// Write all keys 'AlarmHandler.*'
			for (String key : alarmKeys)
			{
				writeKey(bw, cc, key);
			}
			writeln(bw, "");

			// Writers
			for (String alarmKey : alarmWriters)
			{
				alarmKey = StringUtils.substringAfterLast(alarmKey, ".");
				writeln(bw, "## Writer: " + alarmKey + " -------------------------");
				List<String> writerKeys = cc.getKeys(alarmKey);
				for (String key : writerKeys)
				{
					writeKey(bw, cc, key);
				}
				writeln(bw, "");
			}
			writeln(bw, "##---------------------------------------------------------");
			writeln(bw, "##---- END: Alarm Handler");
			writeln(bw, "##---------------------------------------------------------");
			
			
			//---------------------------------------------------------------------------------
			writeln(bw, "");
			writeln(bw, "");
			writeln(bw, "");
			writeln(bw, "");
			writeln(bw, "##---------------------------------------------------------");
			writeln(bw, "##---- BEGIN: DSR - Daily Summary Report");
			writeln(bw, "##---------------------------------------------------------");
			List<String> dsrKeys    = cc.getKeys("DailySummaryReport");
			List<String> dsrWriters = StringUtil.parseCommaStrToList(cc.getProperty("DailySummaryReport.sender.classname", ""), true);

			// Write all keys 'DailySummaryReport.*'
			for (String key : dsrKeys)
			{
				writeKey(bw, cc, key);
			}
			writeln(bw, "");

			// Writers
			for (String senderKey : dsrWriters)
			{
				senderKey = StringUtils.substringAfterLast(senderKey, ".");
				writeln(bw, "## Sender: " + senderKey + " -------------------------");
				List<String> senderKeys = cc.getKeys(senderKey);
				for (String key : senderKeys)
				{
					writeKey(bw, cc, key);
				}
				writeln(bw, "");
			}
			// If we still got "ReportSenderToMail" entries (print them as comments)
			if (true)
			{
				String senderKey = "ReportSenderToMail";
				writeln(bw, "## Sender: " + senderKey + " -------------------------");
				List<String> senderKeys = cc.getKeys(senderKey);
				for (String key : senderKeys)
				{
					writeKey(bw, cc, key, true); // AS COMMENT
				}
				writeln(bw, "");
			}
			writeln(bw, "##---------------------------------------------------------");
			writeln(bw, "##---- END: DSR - Daily Summary Report");
			writeln(bw, "##---------------------------------------------------------");
			
			
			//---------------------------------------------------------------------------------
			writeln(bw, "");
			writeln(bw, "");
			writeln(bw, "");
			writeln(bw, "");
			writeln(bw, "##---------------------------------------------------------");
			writeln(bw, "##---- BEGIN: Other stuff not covered in above sections");
			writeln(bw, "##---------------------------------------------------------");
			for (String unhandledKey : cc.getKeys())
			{
				writeKey(bw, cc, unhandledKey);
			}
			writeln(bw, "##---------------------------------------------------------");
			writeln(bw, "##---- END: Other stuff not covered in above sections");
			writeln(bw, "##---------------------------------------------------------");


			// Finally flush the file
			os.flush();
		}
		catch (IOException ex)
		{
			_logger.error("Problems writing to file '" + savetoFile + "'.", ex);
		}
		
		return savetoFile;
	}

	private void writeMailTemplate(BufferedWriter bw, Configuration conf, String findKey, String mailBase, String keyNotFoundVal, String[] searchPrefixes)
	throws IOException
	{
		// Search all Prefixes
		String  foundVal = null;
		for (String searchPrefix : searchPrefixes)
		{
			String searchKey = searchPrefix + "." + findKey;
			if (conf.hasProperty(searchKey))
			{
				foundVal = conf.getPropertySuper(searchKey);
				break;
			}
		}

		// Write Found value, or default value if not found
		String writeKey = mailBase + "." + findKey;
		if (foundVal != null)
		{
			boolean escUnicode = true;

			writeKey = Configuration.saveConvert(writeKey, true, escUnicode);

			/* No need to escape embedded and trailing spaces, or '=', ':', '#', '!' for value, hence pass false to flags. */
			foundVal = Configuration.saveConvert(foundVal, false, escUnicode, false);

			bw.write(StringUtil.left(writeKey, 30, true) + " = " + foundVal);
			bw.newLine();
		}
		else
		{
			bw.write(StringUtil.left("#" + writeKey, 30, true) + " = " + keyNotFoundVal);
			bw.newLine();
		}
	}

	private static void writeCmSection(BufferedWriter bw, Configuration conf, String cmName)
	throws IOException
	{
		// Construct a List of all "major" settings for a CM
		String[] cmOptionsArr = {
				cmName + ".persistCounters",
				cmName + ".persistCounters.abs",
				cmName + ".persistCounters.diff",
				cmName + ".persistCounters.rate",
				cmName + ".postponeTime",
				cmName + ".queryTimeout"
		};
		List<String> cmOptions = new ArrayList<>(Arrays.asList(cmOptionsArr));
		
		// All Alarms
		List<String> allAlarmKeysForThisCm = conf.getKeys(cmName + ".alarm.");

		// NOT Common and Alarms
		List<String> allOtherKeysForThisCm = conf.getKeys(cmName + ".");
		allOtherKeysForThisCm.removeAll(cmOptions);
		allOtherKeysForThisCm.removeAll(allAlarmKeysForThisCm);

		String tabGroup = "";
		String tabName  = "";
		if (CounterController.hasInstance())
		{
			CountersModel cm = CounterController.getInstance().getCmByName(cmName);
			tabGroup = cm.getGroupName();
			tabName  = cm.getDisplayName();
		}

		String printName = cmName;
		if (StringUtil.hasValue(tabName)) printName += " -- DisplayName='" + tabName + "'";
		if (StringUtil.hasValue(tabName)) printName += " -- TabGroup='" + tabGroup + "'";
		writeln(bw, "##-----------------------------------------------------------------------------------------------------------");
		writeln(bw, "##---- BEGIN: " + printName);
		writeln(bw, "##-----------------------------------------------------------------------------------------------------------");
		writeln(bw, "##---- Options ----");
		for (String key : cmOptions)
		{
			writeKey(bw, conf, key);
		}

		writeln(bw, "##---- Settings ----");
		for (String key : allOtherKeysForThisCm)
		{
			writeKey(bw, conf, key);
		}

		writeln(bw, "##---- Alarms ----");
		for (String key : allAlarmKeysForThisCm)
		{
			writeKey(bw, conf, key);
		}

		writeln(bw, "##-----------------------------------------------------------------------------------------------------------");
		writeln(bw, "##---- END: " + printName);
		writeln(bw, "##-----------------------------------------------------------------------------------------------------------");
		writeln(bw, "");
		writeln(bw, "");
	}
	
	private static void writeln(BufferedWriter bufferedwriter, String s)
	throws IOException
	{
		bufferedwriter.write(s);
		bufferedwriter.newLine();
	}
	private static void writeKey(BufferedWriter bw, Configuration conf, String key)
	throws IOException
	{
		writeKey(bw, conf, key, false);
	}
	private static void writeKey(BufferedWriter bw, Configuration conf, String key, boolean asComment)
	throws IOException
	{
		// Get the *REAL* raw values from the prop (as stored in Properties)
		String val = conf.getPropertySuper(key);
		if (val == null)
			val = "";

		// If it looks like a DEFAULT value... Comment it out ;)
		if (asComment || val.startsWith(Configuration.USE_DEFAULT_PREFIX))
			bw.write("#");

		writeKeyVal(bw, key, val);
		
		boolean removeKey = true;
		if (removeKey)
		{
			conf.remove(key);
		}
	}
	private static void writeKeyVal(BufferedWriter bw, String key, String val)
	throws IOException
	{
		boolean escUnicode = true;

		key = Configuration.saveConvert(key, true, escUnicode);

		/* No need to escape embedded and trailing spaces, or '=', ':', '#', '!' for value, hence pass false to flags. */
		val = Configuration.saveConvert(val, false, escUnicode, false);

		bw.write(key + " = " + val);
		bw.newLine();
	}

	public static List<String> getCmOsNames() 
	{
		Reflections reflections = new Reflections("com.dbxtune.cm.os");

//Set<Class<? extends Object>> allClasses = reflections.getSubTypesOf(Object.class);
//System.out.println("allClasses: " + allClasses);
//for (Class<? extends Object> e : allClasses)
//{
//	System.out.println("e=" + e.getSimpleName());
//}
		// This do NOT seem to work
		List<String> names = reflections.getSubTypesOf(Object.class).stream()
			.map(Class::getSimpleName)
			.filter(name -> name.startsWith("CmOs"))
			.collect(Collectors.toList());

		// Fallback 
		if (names.isEmpty())
		{
			names.addAll(CmOsUtils.getCmOsNames());
		}
		return names;
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
//		System.out.println("getCmOsNames(): " + getCmOsNames());
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
//		WizardOffline provider = new WizardOffline();
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
				SwingUtils.showErrorMessage("Problems open file", "Problems open the file '" + filename + "'.", e);
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
//					String envName = DbxTune.getInstance().getAppSaveDirEnvName();  // DBXTUNE_SAVE_DIR
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
				SwingUtils.showErrorMessage("Problems saving file", "Problems saving the file '" + filename + "'.", ex);
			}
		}
	}
}

