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
import com.dbxtune.alarm.events.oracle.AlarmEventOraInvalidObjects;
import com.dbxtune.cm.CmSettingsHelper;
import com.dbxtune.cm.CmSettingsHelper.RegExpInputValidator;
import com.dbxtune.cm.CounterSetTemplates;
import com.dbxtune.cm.CounterSetTemplates.Type;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.config.dict.MonTablesDictionary;
import com.dbxtune.config.dict.MonTablesDictionaryManager;
import com.dbxtune.gui.MainFrame;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.conn.info.DbmsVersionInfo;
import com.dbxtune.sql.conn.info.DbmsVersionInfoOracle;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.StringUtil;
import com.dbxtune.utils.Ver;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmInvalidObjects
extends CountersModel
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmInvalidObjects.class.getSimpleName();
	public static final String   SHORT_NAME       = "Invalid Objects";
	public static final String   HTML_DESC        =
		"<html>" +
		"Objects that are not usable: UNUSABLE indexes and INVALID objects, counted per schema and object type. This is empty when everything is fine.<br>" +
		"<br>" +
		"<ul>" +
		"  <li><b>UNUSABLE INDEX</b> - an index (or index partition) that Oracle does not maintain and does not use. It happens after <code>ALTER TABLE ... MOVE</code>, partition operations and direct path loads with errors.<br>" +
		"      Queries that used the index get slow (full scans), and DML on the table can fail with ORA-01502 (always for a unique index). Fix: <code>ALTER INDEX ... REBUILD</code></li>" +
		"  <li><b>INVALID OBJECT</b> - a view, procedure, package, trigger... that does not compile. It happens when something it depends on is changed or dropped.<br>" +
		"      Many are harmless: Oracle compiles them again when they are used next time (and after an upgrade a number of invalid objects is normal until <code>utlrp.sql</code> has been run).</li>" +
		"</ul>" +
		"<code>EXAMPLE_NAME</code> is one of the objects (the first in alphabetical order), to have somewhere to start.<br>" +
		"<br>" +
		"<b>Note</b>: The schemas that comes with Oracle (SYS, SYSTEM, XDB...) are shown, but gives no alarm. Oracle 23 for example has an unusable index partition in SYS out of the box.<br>" +
		"<b>Note</b>: In a Container Database, connect to CDB$ROOT to see all PDBs.<br>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_OBJECT_ACCESS;
	public static final String   GUI_ICON_FILE    = "images/" + CM_NAME + ".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {CM_NAME, "DBA_OBJECTS", "DBA_INDEXES", "DBA_IND_PARTITIONS"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = true;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 3600;
	public static final int      DEFAULT_QUERY_TIMEOUT          = 30;

	@Override public int     getDefaultPostponeTime()                 { return DEFAULT_POSTPONE_TIME; }
	@Override public int     getDefaultQueryTimeout()                 { return DEFAULT_QUERY_TIMEOUT; }
	@Override public boolean getDefaultIsNegativeDiffCountersToZero() { return NEGATIVE_DIFF_COUNTERS_TO_ZERO; }
	@Override public Type    getTemplateLevel()                       { return Type.MEDIUM; }

	/**
	 * FACTORY  method to create the object
	 */
	public static CountersModel create(ICounterController counterController, IGuiController guiController)
	{
		if (guiController != null && guiController.hasGUI())
			guiController.splashWindowProgress("Loading: Counter Model '" + CM_NAME + "'");

		return new CmInvalidObjects(counterController, guiController);
	}

	public CmInvalidObjects(ICounterController counterController, IGuiController guiController)
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

		// CON_ID is always 0 in a non-CDB (and in 11.2), in a CDB the same schema can exist in many containers
		pkCols.add("CON_ID");
		pkCols.add("PROBLEM");
		pkCols.add("OWNER");
		pkCols.add("OBJECT_TYPE");

		return pkCols;
	}

	/** If we can't read the views: disable the CM, and tell what to do about it */
	@Override
	public boolean checkDependsOnOther(DbxConnection conn)
	{
		return OracleCmUtils.checkViewsAreReadable(this, conn, "DBA_OBJECTS", "DBA_INDEXES", "DBA_IND_PARTITIONS", "DBA_IND_SUBPARTITIONS");
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		DbmsVersionInfoOracle oraVersionInfo = (DbmsVersionInfoOracle) versionInfo;

		// 11.2 and above: a non-CDB, or when we are connected to a PDB
		String sql = getSql("DBA", "CAST(0 as NUMBER(10))", "CAST(NULL as VARCHAR2(128))", "", "");

		// 12.1 and above, connected to CDB$ROOT: the objects in EVERY container
		if (versionInfo.getLongVersion() >= Ver.ver(12,1) && oraVersionInfo.isCdbRoot())
		{
			String sqlCdb = getSql("CDB", "x.CON_ID", "c.NAME", "x.CON_ID, ", "left join V$CONTAINERS c on c.CON_ID = x.CON_ID \n");

			// The SQL for 12.1 and above can NOT be tested on all versions: so "dry-run" it, and if it fails use the 11.2 SQL (which works on all versions)
			// conn is null when we just want to "view" the SQL (the CM Properties dialog)
			if (conn == null)
			{
				sql = sqlCdb;
			}
			else
			{
				try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery("select * from (" + sqlCdb + ") where 1=2"))
				{
					sql = sqlCdb;
				}
				catch (SQLException ex)
				{
					_logger.warn(CM_NAME + ": The SQL for a CDB (version " + Ver.versionNumToStr(versionInfo.getLongVersion()) + ") failed, only the objects in CDB$ROOT will be shown. Caught: " + ex.getMessage().trim());
				}
			}
		}

		return sql;
	}

	private static String getSql(String viewPrefix, String CON_ID, String CON_NAME, String conIdComma, String joinContainers)
	{
		// One row for every schema + object type that has: INVALID objects, UNUSABLE indexes, index partitions or index subpartitions
		// count(*) and min() never sees a NULL here (so no warning about NULL in an aggregate)
		// 'x' is the inner query: the CDB views has CON_ID, the DBA views has not
		String conIdInner = conIdComma.replace("x.", "");

		return ""
			+ "select \n"
			+ "     " + CON_ID   + " as CON_ID \n"
			+ "    ," + CON_NAME + " as CON_NAME \n"
			+ "    ,x.PROBLEM \n"
			+ "    ,x.OWNER \n"
			+ "    ,x.OBJECT_TYPE \n"
			+ "    ,x.OBJECT_COUNT \n"
			+ "    ,x.EXAMPLE_NAME \n"
			+ "from (select " + conIdInner + "CAST('INVALID OBJECT' as VARCHAR2(20)) as PROBLEM, OWNER, CAST(OBJECT_TYPE as VARCHAR2(30)) as OBJECT_TYPE, count(*) as OBJECT_COUNT, CAST(min(OBJECT_NAME) as VARCHAR2(261)) as EXAMPLE_NAME \n"
			+ "      from " + viewPrefix + "_OBJECTS \n"
			+ "      where STATUS = 'INVALID' \n"
			+ "      group by " + conIdInner + "OWNER, OBJECT_TYPE \n"
			+ "      union all \n"
			+ "      select " + conIdInner + "CAST('UNUSABLE INDEX' as VARCHAR2(20)), OWNER, CAST('INDEX' as VARCHAR2(30)), count(*), CAST(min(INDEX_NAME) as VARCHAR2(261)) \n"
			+ "      from " + viewPrefix + "_INDEXES \n"
			+ "      where STATUS = 'UNUSABLE' \n"
			+ "      group by " + conIdInner + "OWNER \n"
			+ "      union all \n"
			+ "      select " + conIdInner + "CAST('UNUSABLE INDEX' as VARCHAR2(20)), INDEX_OWNER, CAST('INDEX PARTITION' as VARCHAR2(30)), count(*), CAST(min(INDEX_NAME || ':' || PARTITION_NAME) as VARCHAR2(261)) \n"
			+ "      from " + viewPrefix + "_IND_PARTITIONS \n"
			+ "      where STATUS = 'UNUSABLE' \n"
			+ "      group by " + conIdInner + "INDEX_OWNER \n"
			+ "      union all \n"
			+ "      select " + conIdInner + "CAST('UNUSABLE INDEX' as VARCHAR2(20)), INDEX_OWNER, CAST('INDEX SUBPARTITION' as VARCHAR2(30)), count(*), CAST(min(INDEX_NAME || ':' || SUBPARTITION_NAME) as VARCHAR2(261)) \n"
			+ "      from " + viewPrefix + "_IND_SUBPARTITIONS \n"
			+ "      where STATUS = 'UNUSABLE' \n"
			+ "      group by " + conIdInner + "INDEX_OWNER \n"
			+ "     ) x \n"
			+ joinContainers
			+ "order by 1, 3 desc, 4, 5 \n"
			+ "";
	}

	@Override
	public void addMonTableDictForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			mtd.addTable(CM_NAME, "Objects that are not usable: UNUSABLE indexes and INVALID objects, counted per schema and object type.");

			mtd.addColumn(CM_NAME, "CON_ID",       "<html>Container ID. 0 = This is not a Container Database (or the version is below 12.1), or we are connected to a PDB.</html>");
			mtd.addColumn(CM_NAME, "CON_NAME",     "<html>Name of the Container (PDB) the objects are in. NULL if CON_ID is 0.</html>");
			mtd.addColumn(CM_NAME, "PROBLEM",      "<html><b>UNUSABLE INDEX</b> = indexes (or index partitions) with STATUS = UNUSABLE.<br><b>INVALID OBJECT</b> = objects with STATUS = INVALID (they do not compile).</html>");
			mtd.addColumn(CM_NAME, "OWNER",        "<html>The schema that owns the objects.</html>");
			mtd.addColumn(CM_NAME, "OBJECT_TYPE",  "<html>Type of object: INDEX, INDEX PARTITION, INDEX SUBPARTITION, or for invalid objects: VIEW, PROCEDURE, PACKAGE BODY, TRIGGER, SYNONYM...</html>");
			mtd.addColumn(CM_NAME, "OBJECT_COUNT", "<html>How many objects of this type the schema has with the problem.</html>");
			mtd.addColumn(CM_NAME, "EXAMPLE_NAME", "<html>Name of one of the objects (the first in alphabetical order). For a partition: 'indexName:partitionName'.</html>");
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

		String skipOwnerRegex = conf.getProperty(PROPKEY_alarm_SkipOwnerRegex, DEFAULT_alarm_SkipOwnerRegex);

		for (int r=0; r<cm.getAbsRowCount(); r++)
		{
			String problem     = cm.getAbsString(r, "PROBLEM");
			String owner       = cm.getAbsString(r, "OWNER");
			String objectType  = cm.getAbsString(r, "OBJECT_TYPE");
			String conName     = cm.getAbsString(r, "CON_NAME");
			int    objectCount = cm.getAbsValueAsInteger(r, "OBJECT_COUNT", 0);

			if (problem == null || owner == null)
				continue;

			// Schemas we do not care about
			if (StringUtil.hasValue(skipOwnerRegex) && owner.matches(skipOwnerRegex))
				continue;

			// The two alarms works the same way: the name of the alarm, and it's threshold, depends on the PROBLEM of the row
			boolean isUnusableIndex = "UNUSABLE INDEX".equals(problem);
			String  alarmName       = isUnusableIndex ? "UnusableIndexes" : "InvalidObjects";

			if ( ! isSystemAlarmsForColumnEnabledAndInTimeRange(alarmName) )
				continue;

			int threshold = isUnusableIndex
				? conf.getIntProperty(PROPKEY_alarm_UnusableIndexes, DEFAULT_alarm_UnusableIndexes)
				: conf.getIntProperty(PROPKEY_alarm_InvalidObjects,  DEFAULT_alarm_InvalidObjects);

			if (debugPrint || _logger.isDebugEnabled())
				System.out.println("##### sendAlarmRequest(" + cm.getName() + "): " + alarmName + ": owner='" + owner + "', objectType='" + objectType + "', threshold=" + threshold + ", OBJECT_COUNT='" + objectCount + "'.");

			if (objectCount > threshold)
			{
				String ownerLabel = StringUtil.hasValue(conName) ? conName + ":" + owner : owner;

				AlarmEvent ae = new AlarmEventOraInvalidObjects(cm, problem, ownerLabel, objectType, objectCount, cm.getAbsString(r, "EXAMPLE_NAME"), threshold);

				// The below is called from AlarmHandler on: Raise, RE-RAISE & CANCEL
				ae.setAlarmDescriptionProvider(this, new AlarmDescriptionProviderBase(cm.getPk(), cm.getAbsPkValue(r), threshold)
				{
					@Override
					public void setValues(CountersModel cm, AlarmEvent alarmEvent, AlarmPhase phase)
					{
						int    rowId = cm.getAbsRowIdForPkValue(getRowPk());
						String label = "Schema '" + ownerLabel + "': the number of " + problem + "(s) of type '" + objectType + "'";

						if (rowId != -1)
						{
							// Always: (RAISE, RE-RAISE, CANCEL) - Set the row as a table
							String extendedDescText = cm.toTextTableString(DATA_ABS, rowId);
							String extendedDescHtml = cm.toHtmlTableString(DATA_ABS, rowId, true, false, false);
							alarmEvent.setExtendedDescription(extendedDescText, extendedDescHtml);

							// Set CANCEL message (values that was found AFTER last raise/re-raise event)
							if (AlarmPhase.CANCEL.equals(phase))
								alarmEvent.setCancelDescription(getAlarmCancelText(label, cm.getAbsValueAsInteger(rowId, "OBJECT_COUNT", 0)));
						}
						else
						{
							// The row is gone: there are no such objects any more
							if (AlarmPhase.CANCEL.equals(phase))
								alarmEvent.setCancelDescription(getAlarmCancelText(label, 0));
						}
					}
				});

				// Information about how to disable this alarm
				ae.createAlarmOptionsMessage(this, alarmName);

				AlarmHandler.getInstance().addAlarm(ae);
			}
		} // end: loop-rows
	}

	public static final String  PROPKEY_alarm_UnusableIndexes = CM_NAME + ".alarm.system.if.UnusableIndexes.gt";
	public static final int     DEFAULT_alarm_UnusableIndexes = 0;

	public static final String  PROPKEY_alarm_InvalidObjects  = CM_NAME + ".alarm.system.if.InvalidObjects.gt";
	public static final int     DEFAULT_alarm_InvalidObjects  = 999_999; // more or less disabled: invalid objects are common, and mostly harmless

	// The schemas that comes with Oracle: they are not ours to fix (and Oracle 23 has an unusable index partition in SYS out of the box)
	public static final String  PROPKEY_alarm_SkipOwnerRegex  = CM_NAME + ".alarm.system.skip.owner";
	public static final String  DEFAULT_alarm_SkipOwnerRegex  = "(SYS|SYSTEM|PUBLIC|XDB|MDSYS|CTXSYS|ORDSYS|ORDDATA|ORDPLUGINS|OLAPSYS|WMSYS|LBACSYS|DVSYS|DVF|GSMADMIN_INTERNAL|APPQOSSYS|OJVMSYS|AUDSYS|DBSNMP|DBSFWUSER|OUTLN|EXFSYS|SI_INFORMTN_SCHEMA|FLOWS_FILES|APEX_.*|GGSYS|SYSMAN)";

	@Override
	public List<CmSettingsHelper> getLocalAlarmSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();

		CmSettingsHelper.Type isAlarmSwitch = CmSettingsHelper.Type.IS_ALARM_SWITCH;

		list.add(new CmSettingsHelper("UnusableIndexes", isAlarmSwitch, PROPKEY_alarm_UnusableIndexes, Integer.class, conf.getIntProperty(PROPKEY_alarm_UnusableIndexes, DEFAULT_alarm_UnusableIndexes), DEFAULT_alarm_UnusableIndexes, "If a schema has more than ## UNUSABLE indexes (or index partitions) then send 'AlarmEventOraInvalidObjects'." ));
		list.add(new CmSettingsHelper("InvalidObjects" , isAlarmSwitch, PROPKEY_alarm_InvalidObjects , Integer.class, conf.getIntProperty(PROPKEY_alarm_InvalidObjects , DEFAULT_alarm_InvalidObjects ), DEFAULT_alarm_InvalidObjects , "If a schema has more than ## INVALID objects of a type then send 'AlarmEventOraInvalidObjects'. The default (999999) means: never." ));
		list.add(new CmSettingsHelper("Skip Owner"     ,                PROPKEY_alarm_SkipOwnerRegex , String .class, conf.getProperty   (PROPKEY_alarm_SkipOwnerRegex , DEFAULT_alarm_SkipOwnerRegex ), DEFAULT_alarm_SkipOwnerRegex , "For both alarms: skip schemas where 'OWNER' matches this regexp. The default is the schemas that comes with Oracle.", new RegExpInputValidator()));

		return list;
	}
}
