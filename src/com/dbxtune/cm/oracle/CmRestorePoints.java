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
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DbxTune is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DbxTune.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.dbxtune.cm.oracle;

import java.lang.invoke.MethodHandles;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.naming.NameNotFoundException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.ICounterController;
import com.dbxtune.IGuiController;
import com.dbxtune.alarm.AlarmHandler;
import com.dbxtune.alarm.events.AlarmDescriptionProviderBase;
import com.dbxtune.alarm.events.AlarmEvent;
import com.dbxtune.alarm.events.oracle.AlarmEventOraRestorePointOld;
import com.dbxtune.cm.CmSettingsHelper;
import com.dbxtune.cm.CounterSetTemplates;
import com.dbxtune.cm.CounterSetTemplates.Type;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.config.dict.MonTablesDictionary;
import com.dbxtune.config.dict.MonTablesDictionaryManager;
import com.dbxtune.gui.MainFrame;
import com.dbxtune.gui.TabularCntrPanel;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.conn.info.DbmsVersionInfo;
import com.dbxtune.sql.conn.info.DbmsVersionInfoOracle;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.StringUtil;
import com.dbxtune.utils.Ver;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmRestorePoints
extends CountersModel
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmRestorePoints.class.getSimpleName();
	public static final String   SHORT_NAME       = "Restore Points";
	public static final String   HTML_DESC        =
		"<html>" +
		"The restore points of the database. A restore point is a name for a point in time, that the database can be taken back to with <code>FLASHBACK DATABASE</code>. Normally this tab is empty.<br>" +
		"<br>" +
		"A <b>guaranteed</b> restore point is what someone makes before an upgrade or a release: Oracle then keeps ALL flashback logs since that time, until the restore point is dropped.<br>" +
		"If it is forgotten the recovery area fills up, and when that is full the database stops. So an old guaranteed restore point gives an alarm.<br>" +
		"<br>" +
		"<code>STORAGE_MB</code> is the space in the recovery area that is kept because of the restore point (only for a guaranteed one).<br>" +
		"A restore point that is not guaranteed costs nothing: it is only a name for an SCN.<br>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_DISK;
	public static final String   GUI_ICON_FILE    = "images/" + CM_NAME + ".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {CM_NAME, "V$RESTORE_POINT"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = true;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 600;
	public static final int      DEFAULT_QUERY_TIMEOUT          = 30;

	@Override public int     getDefaultPostponeTime()                 { return DEFAULT_POSTPONE_TIME; }
	@Override public int     getDefaultQueryTimeout()                 { return DEFAULT_QUERY_TIMEOUT; }
	@Override public boolean getDefaultIsNegativeDiffCountersToZero() { return NEGATIVE_DIFF_COUNTERS_TO_ZERO; }
	@Override public Type    getTemplateLevel()                       { return Type.SMALL; }

	/**
	 * FACTORY  method to create the object
	 */
	public static CountersModel create(ICounterController counterController, IGuiController guiController)
	{
		if (guiController != null && guiController.hasGUI())
			guiController.splashWindowProgress("Loading: Counter Model '" + CM_NAME + "'");

		return new CmRestorePoints(counterController, guiController);
	}

	public CmRestorePoints(ICounterController counterController, IGuiController guiController)
	{
		super(counterController, guiController,
				CM_NAME, GROUP_NAME, /*sql*/null, /*pkList*/null,
				DIFF_COLUMNS, PCT_COLUMNS, MON_TABLES,
				NEED_ROLES, NEED_CONFIG, NEED_SRV_VERSION, NEED_CE_VERSION,
				NEGATIVE_DIFF_COUNTERS_TO_ZERO, IS_SYSTEM_CM, DEFAULT_POSTPONE_TIME);

		setDisplayName(SHORT_NAME);
		setDescription(HTML_DESC);

		setIconFile(GUI_ICON_FILE);

		setShowClearTime(false);

		setCounterController(counterController);
		setGuiController(guiController);

		CounterSetTemplates.register(this);
	}


	//------------------------------------------------------------
	// Implementation
	//------------------------------------------------------------
	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		List <String> pkCols = new LinkedList<String>();

		// CON_ID is always 0 below 12.2, from 12.2 a PDB can have restore points of it's own (with the same name as in another PDB)
		pkCols.add("CON_ID");
		pkCols.add("NAME");

		return pkCols;
	}

	/** If we can't read the view: disable the CM, and tell what to do about it */
	@Override
	public boolean checkDependsOnOther(DbxConnection conn)
	{
		// Anyone can "select" from V$RESTORE_POINT, but Oracle checks the privilege when it READS it (ORA-01031)
		// So the normal check (select ... where 1=2) says yes for a login without privileges: here we have to read it
		try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery("select count(*) from V$RESTORE_POINT"))
		{
			return true;
		}
		catch (SQLException ex)
		{
			String msg = "The login can't read 'V$RESTORE_POINT'.";
			String fix = "GRANT SELECT_CATALOG_ROLE TO <user>;";

			_logger.warn("When trying to initialize Counters Model '" + getName() + "', named '" + getDisplayName() + "': " + msg + " Fix: " + fix + " Caught: " + ex.getMessage().trim());

			setActive(false, msg + "\n" + "Fix: " + fix);

			TabularCntrPanel tcp = getTabPanel();
			if (tcp != null)
			{
				tcp.setUseFocusableTips(true);
				tcp.setToolTipText("<html>" + msg + "<br>Fix: <code>" + fix.replace("<", "&lt;").replace(">", "&gt;") + "</code></html>");
			}

			return false;
		}
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		DbmsVersionInfoOracle oraVersionInfo = (DbmsVersionInfoOracle) versionInfo;

		// 12.2 and above: restore points for a PDB, and V$RESTORE_POINT has CON_ID. Connected to CDB$ROOT we show which container it is
		boolean isCdbRoot = versionInfo.getLongVersion() >= Ver.ver(12,2) && oraVersionInfo.isCdbRoot();

		// Note: TIME is a TIMESTAMP: when the restore point was made, in the clock of the database server (so the age is counted with SYSDATE)
		//       CAST(... as NUMBER(18,1)) is needed to keep decimals (a NUMBER without precision is treated as a BIGINT)
		String sql = ""
			+ "select \n"
			+ "     " + (isCdbRoot ? "r.CON_ID" : "CAST(0    as NUMBER(10))")    + " as CON_ID \n"
			+ "    ," + (isCdbRoot ? "c.NAME"   : "CAST(NULL as VARCHAR2(128))") + " as CON_NAME \n"
			+ "    ,r.NAME \n"
			+ "    ,r.GUARANTEE_FLASHBACK_DATABASE                                   as GUARANTEED \n"
			+ "    ,CAST(r.TIME as DATE)                                             as CREATED \n"
			+ "    ,CAST((SYSDATE - CAST(r.TIME as DATE)) * 24 as NUMBER(10,1))      as AGE_HOURS \n"
			+ "    ,CAST(r.STORAGE_SIZE / 1048576 as NUMBER(18,1))                   as STORAGE_MB \n"
			+ "    ,r.PRESERVED \n"
			+ "    ,r.SCN \n"
			+ "from V$RESTORE_POINT r \n"
			+ (isCdbRoot ? "left join V$CONTAINERS c on c.CON_ID = r.CON_ID \n" : "")
			+ "order by r.TIME \n"
			+ "";

		return sql;
	}

	@Override
	public void addMonTableDictForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			mtd.addTable(CM_NAME, "The restore points of the database: names for points in time, that the database can be taken back to with FLASHBACK DATABASE.");

			mtd.addColumn(CM_NAME, "CON_ID",     "<html>Container ID. 0 = the restore point is for the whole database (or the version is below 12.2, or we are connected to a PDB).</html>");
			mtd.addColumn(CM_NAME, "CON_NAME",   "<html>Name of the Container (PDB) the restore point is for. NULL if CON_ID is 0.</html>");
			mtd.addColumn(CM_NAME, "NAME",       "<html>Name of the restore point.</html>");
			mtd.addColumn(CM_NAME, "GUARANTEED", "<html>YES = a guaranteed restore point: Oracle keeps all flashback logs since it was made, until it is dropped.<br>NO = a normal restore point: only a name for an SCN, it costs nothing.</html>");
			mtd.addColumn(CM_NAME, "CREATED",    "<html>When the restore point was made (the clock of the database server).</html>");
			mtd.addColumn(CM_NAME, "AGE_HOURS",  "<html>Number of hours since the restore point was made.<br><b>Formula</b>: (SYSDATE - TIME) * 24</html>");
			mtd.addColumn(CM_NAME, "STORAGE_MB", "<html>Space in MB in the recovery area that is kept because of the restore point (flashback logs). 0 for a restore point that is not guaranteed.<br><b>Formula</b>: STORAGE_SIZE / 1024 / 1024</html>");
			mtd.addColumn(CM_NAME, "PRESERVED",  "<html>YES = the restore point stays until it is dropped (a guaranteed one always does). NO = Oracle can remove it when the control file needs the space.</html>");
			mtd.addColumn(CM_NAME, "SCN",        "<html>The System Change Number that the restore point stands for.</html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e);
		}
	}

	@Override
	public void sendAlarmRequest()
	{
		if ( ! hasAbsData() )
			return;

		if ( ! AlarmHandler.hasInstance() )
			return;

		boolean debugPrint = Configuration.getCombinedConfiguration().getBooleanProperty("sendAlarmRequest.debug", _logger.isDebugEnabled());

		CountersModel cm   = this;
		Configuration conf = Configuration.getCombinedConfiguration();

		//-------------------------------------------------------
		// GuaranteedRestorePointAgeInHours: a guaranteed restore point that someone forgot to drop
		//-------------------------------------------------------
		if (isSystemAlarmsForColumnEnabledAndInTimeRange("GuaranteedRestorePointAgeInHours"))
		{
			double threshold = conf.getDoubleProperty(PROPKEY_alarm_GuaranteedRestorePointAgeInHours, DEFAULT_alarm_GuaranteedRestorePointAgeInHours);

			for (int r=0; r<cm.getAbsRowCount(); r++)
			{
				String name       = cm.getAbsString(r, "NAME");
				String conName    = cm.getAbsString(r, "CON_NAME");
				String guaranteed = cm.getAbsString(r, "GUARANTEED");
				Double ageHours   = cm.getAbsValueAsDouble(r, "AGE_HOURS");
				Double storageMb  = cm.getAbsValueAsDouble(r, "STORAGE_MB");

				if (name == null || ageHours == null)
					continue;

				if (debugPrint || _logger.isDebugEnabled())
					System.out.println("##### sendAlarmRequest(" + cm.getName() + "): GuaranteedRestorePointAgeInHours: name='" + name + "', threshold=" + threshold + ", GUARANTEED='" + guaranteed + "', AGE_HOURS='" + ageHours + "', STORAGE_MB='" + storageMb + "'.");

				if ("YES".equals(guaranteed) && ageHours > threshold)
				{
					// name   or in a CDB: container:name
					String restorePointLabel = StringUtil.hasValue(conName) ? conName + ":" + name : name;

					AlarmEvent ae = new AlarmEventOraRestorePointOld(cm, restorePointLabel, cm.getAbsString(r, "CREATED"), ageHours, (storageMb == null ? 0 : storageMb), threshold);

					// The below is called from AlarmHandler on: Raise, RE-RAISE & CANCEL
					ae.setAlarmDescriptionProvider(this, new AlarmDescriptionProviderBase(cm.getPk(), cm.getAbsPkValue(r), threshold)
					{
						@Override
						public void setValues(CountersModel cm, AlarmEvent alarmEvent, AlarmPhase phase)
						{
							int    rowId = cm.getAbsRowIdForPkValue(getRowPk());
							String label = "Restore point '" + restorePointLabel + "'";

							if (rowId != -1)
							{
								// Always: (RAISE, RE-RAISE, CANCEL) - Set the row as a table
								String extendedDescText = cm.toTextTableString(DATA_ABS, rowId);
								String extendedDescHtml = cm.toHtmlTableString(DATA_ABS, rowId, true, false, false);
								alarmEvent.setExtendedDescription(extendedDescText, extendedDescHtml);

								// Set CANCEL message (values that was found AFTER last raise/re-raise event)
								if (AlarmPhase.CANCEL.equals(phase))
									alarmEvent.setCancelDescription(getAlarmCancelText(label + ": age", toAlarmCancelValue(cm.getAbsValueAsDouble(rowId, "AGE_HOURS")) + " hours, GUARANTEED='" + cm.getAbsString(rowId, "GUARANTEED") + "'", " hours"));
							}
							else
							{
								// The row is gone: the restore point was dropped (which is how this alarm normally ends)
								if (AlarmPhase.CANCEL.equals(phase))
									alarmEvent.setCancelDescription(getAlarmCancelTextGone(label));
							}
						}
					});

					// Information about how to disable this alarm
					ae.createAlarmOptionsMessage(this, "GuaranteedRestorePointAgeInHours");

					AlarmHandler.getInstance().addAlarm(ae);
				}
			} // end: loop-rows
		} // end: GuaranteedRestorePointAgeInHours
	}

	public static final String  PROPKEY_alarm_GuaranteedRestorePointAgeInHours = CM_NAME + ".alarm.system.if.GuaranteedRestorePointAgeInHours.gt";
	public static final double  DEFAULT_alarm_GuaranteedRestorePointAgeInHours = 48;

	@Override
	public List<CmSettingsHelper> getLocalAlarmSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();

		CmSettingsHelper.Type isAlarmSwitch = CmSettingsHelper.Type.IS_ALARM_SWITCH;

		list.add(new CmSettingsHelper("GuaranteedRestorePointAgeInHours", isAlarmSwitch, PROPKEY_alarm_GuaranteedRestorePointAgeInHours, Double.class, conf.getDoubleProperty(PROPKEY_alarm_GuaranteedRestorePointAgeInHours, DEFAULT_alarm_GuaranteedRestorePointAgeInHours), DEFAULT_alarm_GuaranteedRestorePointAgeInHours, "If 'GUARANTEED' is YES and 'AGE_HOURS' is greater than ##.# hours then send 'AlarmEventOraRestorePointOld'. A guaranteed restore point keeps flashback logs until it is dropped, and fills the recovery area." ));

		return list;
	}
}
