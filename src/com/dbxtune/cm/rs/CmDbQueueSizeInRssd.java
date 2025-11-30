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
package com.dbxtune.cm.rs;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.naming.NameNotFoundException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.ICounterController;
import com.dbxtune.IGuiController;
import com.dbxtune.alarm.AlarmHandler;
import com.dbxtune.alarm.events.AlarmEvent;
import com.dbxtune.alarm.events.rs.AlarmEventRsDbQueueSize;
import com.dbxtune.central.pcs.CentralPersistReader;
import com.dbxtune.cm.CmSettingsHelper;
import com.dbxtune.cm.CmSybMessageHandler;
import com.dbxtune.cm.CounterSetTemplates;
import com.dbxtune.cm.CounterSetTemplates.Type;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.config.dict.MonTablesDictionary;
import com.dbxtune.config.dict.MonTablesDictionaryManager;
import com.dbxtune.graph.TrendGraphDataPoint;
import com.dbxtune.graph.TrendGraphDataPoint.LabelType;
import com.dbxtune.gui.MainFrame;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.conn.info.DbmsVersionInfo;
import com.dbxtune.utils.Configuration;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmDbQueueSizeInRssd
extends CountersModel
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmDbQueueSizeInRssd.class.getSimpleName();
	public static final String   SHORT_NAME       = "DB Queue Size";
	public static final String   HTML_DESC        = 
		"<html>" +
		"<p>DB Queue Size in the Replication Server Stable Queue</p>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_DISK;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"db_queue_size"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
		"size",
		"saved"
		};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = false;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 0;
	public static final int      DEFAULT_QUERY_TIMEOUT          = CountersModel.DEFAULT_sqlQueryTimeout;;

	@Override public int     getDefaultPostponeTime()                 { return DEFAULT_POSTPONE_TIME; }
	@Override public int     getDefaultQueryTimeout()                 { return DEFAULT_QUERY_TIMEOUT; }
	@Override public boolean getDefaultIsNegativeDiffCountersToZero() { return NEGATIVE_DIFF_COUNTERS_TO_ZERO; }
	@Override public Type    getTemplateLevel()                       { return Type.ALL; }

	/**
	 * FACTORY  method to create the object
	 */
	public static CountersModel create(ICounterController counterController, IGuiController guiController)
	{
		if (guiController != null && guiController.hasGUI())
			guiController.splashWindowProgress("Loading: Counter Model '"+CM_NAME+"'");

		return new CmDbQueueSizeInRssd(counterController, guiController);
	}

	public CmDbQueueSizeInRssd(ICounterController counterController, IGuiController guiController)
	{
		super(counterController, guiController,
				CM_NAME, GROUP_NAME, /*sql*/null, /*pkList*/null, 
				DIFF_COLUMNS, PCT_COLUMNS, MON_TABLES, 
				NEED_ROLES, NEED_CONFIG, NEED_SRV_VERSION, NEED_CE_VERSION, 
				NEGATIVE_DIFF_COUNTERS_TO_ZERO, IS_SYSTEM_CM, DEFAULT_POSTPONE_TIME);

		setDisplayName(SHORT_NAME);
		setDescription(HTML_DESC);

		setIconFile(GUI_ICON_FILE);

		setCounterController(counterController);
		setGuiController(guiController);
		
		addTrendGraphs();
		
		CounterSetTemplates.register(this);
	}


	//------------------------------------------------------------
	// Implementation
	//------------------------------------------------------------
	public static final String GRAPH_NAME_QUEUE_SIZE = "RssdQueueSize";

	private void addTrendGraphs()
	{
		//-----
		addTrendGraph(GRAPH_NAME_QUEUE_SIZE,
			"Queue Size from the RSSD (col 'size', Absolute Value)", // Menu CheckBox text
			"Queue Size from the RSSD (col 'size', Absolute Value)", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MB, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.SPACE,
			false, // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

	}

	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
		if (GRAPH_NAME_QUEUE_SIZE.equals(tgdp.getName()))
		{
			int rows = 0;
			for (int i = 0; i < this.size(); i++)
			{
				Double qSize = this.getAbsValueAsDouble(i, "size");
				if (qSize > 0.0)
					rows++;
			}

			// Get out: if Nothing to do...
			if (rows == 0)
			{
				return;
			}
			
			// Write 1 "line" for every SQMRBacklogSeg row
			Double[] dArray = new Double[rows];
			String[] lArray = new String[dArray.length];
			int i2 = 0;
			for (int i = 0; i < this.size(); i++)
			{
				Double qSize = this.getAbsValueAsDouble(i, "size");
				if (qSize > 0.0)
				{
					lArray[i2] = this.getAbsString       (i, "name") + "(" + this.getAbsString(i, "q_type_str") + ")";
					dArray[i2] = this.getAbsValueAsDouble(i, "size");
					i2++;
				}
			}

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), lArray, dArray);
		}

//		if (GRAPH_NAME_QUEUE_SIZE.equals(tgdp.getName()))
//		{
//			// Write 1 "line" for every device
//			Double[] dArray = new Double[this.size()];
//			String[] lArray = new String[dArray.length];
//			for (int i = 0; i < dArray.length; i++)
//			{
//				lArray[i] = this.getAbsString       (i, "name") + "(" + this.getAbsString(i, "q_type_str") + ")";
//				dArray[i] = this.getAbsValueAsDouble(i, "size");
//			}
//
//			// Set the values
//			tgdp.setDataPoint(this.getTimestamp(), lArray, dArray);
////			tgdp.setDate(this.getTimestamp());
////			tgdp.setLabel(lArray);
////			tgdp.setData(dArray);
//		}
	}
	
	@Override
	protected CmSybMessageHandler createSybMessageHandler()
	{
		CmSybMessageHandler msgHandler = super.createSybMessageHandler();
		
		msgHandler.addDiscardMsgNum(0);
		msgHandler.addDiscardMsgNum(15539); // Gateway connection to 'GORAN_1_ERSSD.GORAN_1_ERSSD' is created.
		msgHandler.addDiscardMsgNum(15540); // Gateway connection to 'GORAN_1_ERSSD.GORAN_1_ERSSD' is dropped.

		return msgHandler;
	}

//	@Override
//	protected TabularCntrPanel createGui()
//	{
//		return new CmRaSysmonPanel(this);
//	}

	@Override
	public String[] getDependsOnConfigForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		return NEED_CONFIG;
	}

	@Override
	public void addMonTableDictForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		try 
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();
			mtd.addTable("db_queue_size",  "");

			mtd.addColumn("db_queue_size", "name",          "<html>FIXME</html>");
			mtd.addColumn("db_queue_size", "q_number",      "<html>FIXME</html>");
			mtd.addColumn("db_queue_size", "q_type",        "<html>FIXME</html>");
			mtd.addColumn("db_queue_size", "size",          "<html>FIXME</html>");
			mtd.addColumn("db_queue_size", "saved",         "<html>FIXME</html>");
			mtd.addColumn("db_queue_size", "detect_loss",   "<html>FIXME</html>");
			mtd.addColumn("db_queue_size", "ignore_loss",   "<html>FIXME</html>");
			mtd.addColumn("db_queue_size", "first_seg",     "<html>FIXME</html>");
			mtd.addColumn("db_queue_size", "status",        "<html>FIXME</html>");
			mtd.addColumn("db_queue_size", "xnl_large_msg", "<html>FIXME</html>");
		}
		catch (NameNotFoundException e) 
		{
			_logger.warn("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e); 
		//	System.out.println("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e); 
		}
	}

	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("q_number");
		pkCols.add("q_type");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		long srvVersion = versionInfo.getLongVersion();

		String sql = rmpQueue(srvVersion);

		addCleanupCmdOnException("disconnect");

		return 
			  "connect rssd \n"
			+ "go\n"
			+ sql + "\n"
			+ "go\n"
			+ "disconnect \n"
			+ "go\n";
	}
	
	/**
	 * NOTE: Below code is grabbed from RSSD Stored Proc: RMP_QUEUE (C:\Sybase\RMP-15_5\scripts\rmp1571.sql)
	 * This so we don't have to create our own procedure in the RSSD
	 * 
	 * RS> Col# Label         JDBC Type Name           Guessed DBMS type
	 * RS> ---- ------------- ------------------------ -----------------
	 * RS> 1    name          java.sql.Types.VARCHAR   varchar(255)     
	 * RS> 2    q_number      java.sql.Types.INTEGER   int              
	 * RS> 3    q_type        java.sql.Types.INTEGER   int              
	 * RS> 4    size          java.sql.Types.INTEGER   int              
	 * RS> 5    saved         java.sql.Types.INTEGER   int              
	 * RS> 6    detect_loss   java.sql.Types.INTEGER   int              
	 * RS> 7    ignore_loss   java.sql.Types.INTEGER   int              
	 * RS> 8    first_seg     java.sql.Types.INTEGER   int              
	 * RS> 9    status        java.sql.Types.CHAR      char(4)          
	 * RS> 10   xnl_large_msg java.sql.Types.VARCHAR   varchar(9)       	 
	 *  
	 * @param srvVersion
	 * @return
	 */
	public static String rmpQueue(long srvVersion)
	{
		return 
		"/* NOTE: Below code is grabbed from RSSD Stored Proc: RMP_QUEUE */ \n" + 
		" \n" + 
		"create table #subs (rawid binary(8), intid int) \n" + 
		" \n" + 
		"/* \n" + 
		"** If this a byte-swap machine, swap the order of the last \n" + 
		"** four bytes of the subid \n" + 
		"*/ \n" + 
		"if (convert(int, 0x00000100) = 65536) \n" + 
		"begin \n" + 
		"	insert #subs \n" + 
		"	select rawid = subid, \n" + 
		"		   intid = substring(subid, 8, 1) + \n" + 
		"				   substring(subid, 7, 1) + \n" + 
		"				   substring(subid, 6, 1) + \n" + 
		"				   substring(subid, 5, 1) \n" + 
		"	from rs_subscriptions \n" + 
		"end \n" + 
		"else \n" + 
		"begin \n" + 
		"	insert #subs \n" + 
		"	select rawid = subid, intid = substring(subid, 5, 4) \n" + 
		"	from rs_subscriptions \n" + 
		"end \n" + 
		" \n" + 
		"create table #queues \n" + 
		"	 (	name 		varchar(255), \n" + 
		"		q_type_str	varchar(255), \n" + 
		"		q_number 	int, \n" + 
		"		q_type 		int, \n" + 
		"		q_state		int, \n" + 
		"		size 		int, \n" + 
		"		saved 		int, \n" + 
		"		detect_loss 	int, \n" + 
		"		ignore_loss 	int, \n" + 
		"		first_seg	int, \n" + 
		"		q_objid		binary(8), \n" + 
		"		q_objid_temp	binary(8), \n" + 
		"		xnl_large_msg	varchar(9) NULL ) \n" + 
		" \n" + 
		"insert #queues \n" + 
		"select distinct dsname + '.' + dbname, 'Inbound', \n" + 
		"	number, 1, q.state, 0, 0, 0, 0, 0, 0, 0, '' \n" + 
		"from rs_queues q, rs_databases d \n" + 
		"where number = d.dbid and type=1 \n" + 
		" \n" + 
		"insert #queues \n" + 
		"select distinct \n" + 
		"        isnull(convert(varchar(61), name), dsname+'.'+dbname), 'Outbound', \n" + 
		"	number, 0, q.state, 0, 0, 0, 0, 0, 0, 0, '' \n" + 
		"from rs_queues q, rs_databases, rs_sites \n" + 
		"where number *= dbid \n" + 
		"  and number *= id \n" + 
		"  and type=0 \n" + 
		" \n" + 
		"insert #queues \n" + 
		"select distinct d.dsname+'.'+d.dbname, 'Materialization-'+sub.subname, \n" + 
		"	number, q.type, q.state, 0, 0, 0, 0, 0, 0, 0, '' \n" + 
		"from rs_queues q, rs_subscriptions sub, rs_databases d, #subs \n" + 
		"where d.dbid=number \n" + 
		"  and sub.dbid=d.dbid \n" + 
		"  and #subs.rawid = sub.subid \n" + 
		"  and #subs.intid = q.type \n" + 
		"  and materializing=1 \n" + 
		" \n" + 
		"insert #queues \n" + 
		"select distinct d.dsname+'.'+d.dbname, 'Dematerialization-'+sub.subname, \n" + 
		"	number, q.type, q.state, 0, 0, 0, 0, 0, 0, 0, '' \n" + 
		"from rs_queues q, rs_subscriptions sub, rs_databases d, #subs \n" + 
		"where d.dbid=number \n" + 
		"  and sub.dbid=d.dbid \n" + 
		"  and #subs.rawid = sub.subid \n" + 
		"  and #subs.intid = q.type \n" + 
		"  and dematerializing=1 \n" + 
		" \n" + 
		"update #queues \n" + 
		"	set size = (select count(*) \n" + 
		"	from rs_segments \n" + 
		"	where #queues.q_number = rs_segments.q_number \n" + 
		"  	  and #queues.q_type = rs_segments.q_type \n" + 
		"  	  and used_flag > 0) \n" + 
		" \n" + 
		"update #queues \n" + 
		"	set saved = (select count(*) \n" + 
		"	from rs_segments \n" + 
		"	where #queues.q_number = rs_segments.q_number \n" + 
		"  	  and #queues.q_type = rs_segments.q_type \n" + 
		"  	  and used_flag > 1) \n" + 
		" \n" + 
		"update #queues \n" + 
		"	set detect_loss = detect_loss + (select count(*) \n" + 
		"		from rs_oqid \n" + 
		"		where #queues.q_number = rs_oqid.q_number \n" + 
		"		  and #queues.q_type = rs_oqid.q_type \n" + 
		"		  and valid = 1), \n" + 
		"		ignore_loss = ignore_loss + (select count(*) \n" + 
		"	from rs_oqid \n" + 
		"	where #queues.q_number = rs_oqid.q_number \n" + 
		"	  and #queues.q_type = rs_oqid.q_type \n" + 
		"	  and valid = 2) \n" + 
		" \n" + 
		"update #queues \n" + 
		"	set detect_loss = detect_loss + (select count(*) \n" + 
		"		from rs_exceptslast \n" + 
		"		where #queues.q_number = error_db \n" + 
		"	  	  and status = 1), \n" + 
		"		ignore_loss = ignore_loss + (select count(*) \n" + 
		"	from rs_exceptslast \n" + 
		"	where #queues.q_number = error_db \n" + 
		"	  and status = 2) \n" + 
		" \n" + 
		"update #queues \n" + 
		"	set first_seg = (select isnull(min(logical_seg),0) \n" + 
		"			from rs_segments \n" + 
		"			where #queues.q_number = rs_segments.q_number \n" + 
		"			  and #queues.q_type = rs_segments.q_type) \n" + 
		" \n" + 
		"/* Build the queue objid number to search for wide message flag in rs_config */ \n" + 
		"update #queues \n" + 
		"	set q_objid = convert ( binary(4), q_number ) \n" + 
		"		+ convert ( binary(4), q_type ) \n" + 
		" \n" + 
		"/* Store a copy of the queue objid in case the bytes need to be reversed */ \n" + 
		"update #queues \n" + 
		"	set q_objid_temp = convert ( binary(4), q_number ) \n" + 
		"		+ convert ( binary(4), q_type ) \n" + 
		" \n" + 
		"/* If this a byte-swap machine, reverse the objid */ \n" + 
		"if (convert(int, 0x00000100) = 65536) \n" + 
		"	update #queues \n" + 
		"	set q_objid =  substring ( q_objid_temp, 8, 1 ) + \n" + 
		"		substring ( q_objid_temp, 7, 1 ) + \n" + 
		"		substring ( q_objid_temp, 6, 1 ) + \n" + 
		"		substring ( q_objid_temp, 5, 1 ) + \n" + 
		"		substring ( q_objid_temp, 4, 1 ) + \n" + 
		"		substring ( q_objid_temp, 3, 1 ) + \n" + 
		"		substring ( q_objid_temp, 2, 1 ) + \n" + 
		"		substring ( q_objid_temp, 1, 1 ) \n" + 
		" \n" + 
		"update #queues \n" + 
		"	set xnl_large_msg = (select charvalue \n" + 
		"				from rs_config \n" + 
		"				where rs_config.objid = #queues.q_objid \n" + 
		"				and rs_config.optionname = \n" + 
		"					'sqm_xact_with_large_msg' ) \n" + 
		" \n" + 
		"update #queues \n" + 
		"	set xnl_large_msg = 'shutdown' where xnl_large_msg = NULL \n" + 
		" \n" + 
		"select name, q_type_str, q_number, q_type, size, saved, detect_loss, ignore_loss, first_seg, status='DOWN', xnl_large_msg \n" + 
		"from #queues \n" + 
		"where q_number != 0 and q_state != 1 \n" + 
		"union \n" + 
		"select name, q_type_str, q_number, q_type, size, saved, detect_loss, ignore_loss, first_seg, status='UP',   xnl_large_msg \n" + 
		"from #queues \n" + 
		"where q_number != 0 and q_state = 1 \n" + 
		" \n" + 
		"/* Cleanup */ \n" + 
		"drop table #queues \n" + 
		"drop table #subs \n" +
		"";
	}
	
	
	//--------------------------------------------------------------
	// Alarm handling
	//--------------------------------------------------------------
	@Override
	public void sendAlarmRequest()
	{
		if ( ! hasAbsData() )
			return;
		
		if ( ! AlarmHandler.hasInstance() )
			return;

		CountersModel cm = this;

		boolean debugPrint = Configuration.getCombinedConfiguration().getBooleanProperty("sendAlarmRequest.debug", _logger.isDebugEnabled());

		for (int r=0; r<cm.getAbsRowCount(); r++)
		{
			String name = cm.getAbsString(r, "name");
			String type = cm.getAbsString(r, "q_type_str"); // "Inbound" or "Outbound"

			//-------------------------------------------------------
			// Space Used in Segs (MB)
			//-------------------------------------------------------
			if (isSystemAlarmsForColumnEnabledAndInTimeRange("size"))
			{
				Double size = cm.getAbsValueAsDouble(r, "size");
				
				if (size != null)
				{
					int threshold = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_alarm_size, DEFAULT_alarm_size);

					if (debugPrint || _logger.isDebugEnabled())
						System.out.println("##### sendAlarmRequest("+cm.getName()+"): threshold="+threshold+", name='"+name+"', type='"+type+"', size="+size+".");

					if (size.intValue() > threshold)
					{
						String extendedDescText = "";
						String extendedDescHtml = cm.getGraphDataHistoryAsHtmlImage(GRAPH_NAME_QUEUE_SIZE);

						AlarmEvent ae = new AlarmEventRsDbQueueSize(cm, threshold, name, type, size.intValue());
						ae.setExtendedDescription(extendedDescText, extendedDescHtml);

						AlarmHandler.getInstance().addAlarm(ae);
					}
				}
			}
		} // end: loop all rows
	} // end: method

	@Override
	public boolean isGraphDataHistoryEnabled(String name)
	{
		// ENABLED for the following graphs
		if (GRAPH_NAME_QUEUE_SIZE.equals(name)) return true;

		// default: DISABLED
		return false;
	}
	@Override
	public int getGraphDataHistoryTimeInterval(String name)
	{
		// Keep interval: default is 60 minutes
		return super.getGraphDataHistoryTimeInterval(name);
	}

	
	public static final String  PROPKEY_alarm_size = CM_NAME + ".alarm.system.if.size.gt";
	public static final int     DEFAULT_alarm_size = 8192;
	
	@Override
	public List<CmSettingsHelper> getLocalAlarmSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();

		CmSettingsHelper.Type isAlarmSwitch = CmSettingsHelper.Type.IS_ALARM_SWITCH;
		
		list.add(new CmSettingsHelper("size", isAlarmSwitch, PROPKEY_alarm_size, Integer.class,  conf.getIntProperty(PROPKEY_alarm_size, DEFAULT_alarm_size), DEFAULT_alarm_size, "If 'size' is GREATER than ####, send 'AlarmEventRsDbQueueSize'."));

		return list;
	}
}
