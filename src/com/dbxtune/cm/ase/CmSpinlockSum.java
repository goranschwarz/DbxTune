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
package com.dbxtune.cm.ase;

import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import com.dbxtune.ICounterController;
import com.dbxtune.IGuiController;
import com.dbxtune.cm.CmSettingsHelper;
import com.dbxtune.cm.CmSybMessageHandler;
import com.dbxtune.cm.CounterSample;
import com.dbxtune.cm.CounterSetTemplates;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.cm.CounterSetTemplates.Type;
import com.dbxtune.cm.ase.gui.CmSpinlockSumPanel;
import com.dbxtune.config.dict.MonTablesDictionary;
import com.dbxtune.config.dict.MonTablesDictionaryManager;
import com.dbxtune.gui.MainFrame;
import com.dbxtune.gui.TabularCntrPanel;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.conn.info.DbmsVersionInfo;
import com.dbxtune.sql.conn.info.DbmsVersionInfoSybaseAse;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.StringUtil;
import com.dbxtune.utils.Ver;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmSpinlockSum
extends CountersModel
{
	private static Logger        _logger          = Logger.getLogger(CmSpinlockSum.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmSpinlockSum.class.getSimpleName();
	public static final String   SHORT_NAME       = "Spinlock Sum";
	public static final String   HTML_DESC        = 
//		"<html>" +
//		"<p>What spinlocks do we have contention on.</p>" +
//		"This could be a bit heavy to use when there is a 'low' refresh interval.<br>" +
//		"For the moment consider this as <b>experimental</b>." +
//		"</html>";

			"<HTML> \n" +
			"What spinlocks do we have contention on.<BR> \n " +
			"Based on table <CODE>master.dbo.sysmonitors</CODE>.<BR> \n" +
			"<BR> \n" +
			"<B>Note</B>: The origin text for below information can be found at: <A HREF=\"http://wiki.scn.sap.com/wiki/display/SYBASE/Spinlocks+and+CPU+usage+in+SAP+ASE\">http://wiki.scn.sap.com/wiki/display/SYBASE/Spinlocks+and+CPU+usage+in+SAP+ASE</A> <BR> \n" +
			"<B>Note</B>: To check for <I>possible solutions</I> for some known spinlock contentions, see: at the end of this information<BR> \n" +
			" \n" +
			"<BR> \n" +
			"<B>Another place to look at</B>: 'https://help.sap.com/docs/SAP_ASE' search for 'Common Spinlock Contention Objects and Their Resolutions' <BR> \n" +
			"<A HREF=\"https://help.sap.com/docs/SAP_ASE?q=Common%20Spinlock%20Contention%20Objects%20and%20Their%20Resolutions\">https://help.sap.com/docs/SAP_ASE?q=Common%20Spinlock%20Contention%20Objects%20and%20Their%20Resolutions</A> <BR> \n" +
			"<A HREF=\"https://help.sap.com/docs/SAP_ASE/91d32d977a174c68829880bc020fc352/e711bd4354b8480f8d44ee6959498f9c.html?q=spinlock%20monitoring\">https://help.sap.com/docs/SAP_ASE/91d32d977a174c68829880bc020fc352/e711bd4354b8480f8d44ee6959498f9c.html?q=spinlock%20monitoring</A> <BR> \n" +
			" \n" +
			"<H1>Spinlocks and CPU usage in SAP ASE</H1> \n" +
			" \n" +
			"<H2>Purpose</H2> \n" +
			"The purpose of this page is to clarify the understanding of how SAP ASE uses spinlocks and what the effects on overall CPU usage may be. \n" +
			" \n" +
			"<H2>Overview</H2> \n" +
			"Often high CPU  in SAP ASE can be traced to spinlock usage. This page will show how to identify that condition and suggest ways to tune ASE. \n" +
			" \n" +
			"<H2>What is a Spinlock?</H2> \n" +
			"<UL> \n" +
			"    <LI>In a multi-engine server synchronization mechanisms are needed to protect shared resources<BR> \n" +
			"        &#9830; ASE uses spinlocks as one of its synchronization mechanisms<BR> \n" +
			"        &#9830; A spinlock is a data structure with a field that can only be updated atomically (that is, only one engine at a time can make changes to it).<BR> \n" +
			"    </LI> \n" +
			" \n" +
			"    <LI>When a task modifies a data item which is shared it must first hold a spinlock<BR> \n" +
			"        &#9830; Shared items are such things as run queues, data cache page lists, lock structures, etc.<BR> \n" +
			"    </LI> \n" +
			" \n" +
			"    <LI>The spinlock prevents another task from modifying the value at the same time<BR> \n" +
			"        &#9830; This of course assumes that the other task also performs its access under the protection of a spinlock<BR> \n" +
			"    </LI> \n" +
			" \n" +
			"    <LI>A task needing a spinlock will <I>spin</I> (block) until the lock is granted<BR> \n" +
			"        &#9830; When multiple engines are spinning at the same time CPU usage can rise substantially.<BR> \n" +
			"    </LI> \n" +
			" \n" +
			"    <LI>Spinlocks must be as fast and efficient as possible<BR> \n" +
			"        &#9830; In order to reduce contention a process which loops typically acquires and releases its spinlock each time through the loop.<BR> \n" +
			"        &#9830; Therefore the spinlock code is written in platform-specific assembly language.<BR> \n" +
			"    </LI> \n" +
			"</UL> \n" +
			" \n" +
			" \n" +
			"<H2>Comparison of Spinlocks to Other Synchronization Mechanisms</H2> \n" +
			"<P> \n" +
			"<TABLE BORDER=1 CELLSPACING=1 CELLPADDING=1> \n" +
			"  <TR ALIGN='left' VALIGN='left'> <TH>Type                       </TH> <TH>Complexity</TH> <TH>CPU overhead</TH> <TH>Wait time            </TH> </TR> \n" +
			"  <TR ALIGN='left' VALIGN='left'> <TD>Spinlock                   </TD> <TD>Low       </TD> <TD>High        </TD> <TD>Very low             </TD> </TR> \n" +
			"  <TR ALIGN='left' VALIGN='left'> <TD>Latch                      </TD> <TD>Moderate  </TD> <TD>Low         </TD> <TD>Should be small      </TD> </TR> \n" +
			"  <TR ALIGN='left' VALIGN='left'> <TD>Table/page/row/address Lock</TD> <TD>High      </TD> <TD>Low         </TD> <TD>Can vary considerably</TD> </TR> \n" +
			"</TABLE> \n" +
			"</P> \n" +
			" \n" +
			" \n" +
			"<H2>Spinlocks and CPU Usage</H2> \n" +
			"<UL> \n" +
			"    <LI>Spids trying to get a spinlock will never yield the engine until they have it.<BR> \n" +
			"        So one spid, waiting on a spinlock, will cause 100% user busy on one engine until it gets the spinlock. \n" +
			"    </LI> \n" +
			" \n" +
			"    <LI>Spinlock contention percentage is measured as waits/grabs<BR> \n" +
			"        Example: 10,000 grabs with 3,000 waits = 30% contention \n" +
			"    </LI> \n" +
			" \n" +
			"    <LI>For looking at performance issues, <B>use total spins</B>, not contention<BR> \n" +
			"        Example: Assume two spinlocks<BR> \n" +
			"        &#9830; One had 100 grabs with 40 waits and 200 spins = 40% contention<BR> \n" +
			"        &#9830; Second had 100,000 grabs with 400 waits and 20,000 spins = 4% contention<BR> \n" +
			"        The second used up more many cpu cycles spinning, even though contention was lower.<BR> \n" +
			"        We should then look at tuning for the second example, not the first.<BR> \n" +
			"    </LI> \n" +
			" \n" +
			"    <LI> \n" +
			"    As more engines spin on the same spinlock, the wait time and number of spins increases; sometimes geometrically \n" +
			"    </LI> \n" +
			"</UL> \n" +
			" \n" +
			" \n" +
			"<H2>Troubleshooting Spinlocks</H2> \n" +
			"<UL> \n" +
			"    <LI>Spinlock contention/spinning is one of the major causes of high CPU</LI> \n" +
			"    <LI>Step 1 is determining if, in fact, the high cpu is being caused by spinlock usage.</LI> \n" +
			"    <LI>Step 2 is determining which spinlock or spinlocks are causing the condition.</LI> \n" +
			"    <LI>Step 3 is determining what tuning to use to help reduce the problem.</LI> \n" +
			"</UL> \n" +
			"<B>Note</B>: You will never get to 0% spinlock contention unless you only run with one engine. That is, do not think that spinlock contention can be eliminated. It can only possibly be reduced. \n" +
			" \n" +
			"<H2>Step 1 - Checking for spinlock contention/spinning</H2> \n" +
			"<UL> \n" +
			"    <LI>Using sp_sysmon (or AseTune) to determine if high cpu is due to spinlocks</LI> \n" +
			"    <LI>Check \"CPU Busy\" (or \"User Busy\" in 15.7 Threaded Mode).</LI> \n" +
			"    <LI>If engines  are not showing high busy% then spinlocks are not a big issue.</LI> \n" +
			"    <LI>Check \"Total Cache Hits\" in the \"Data Cache Management\" section.<BR> \n" +
			"        If the cache hits per second is high, and goes up with cpu busy %, then you likely are looking at table scanning/query plans and not spinlocks. \n" +
			"    </LI> \n" +
			"    <LI>In general, if cpu usage increases but measurements of throughput such as committed xacts, cache hits, lock requests, scans, etc. go down then it is very possible that spinlock usage is an issue \n" +
			"</UL> \n" +
			" \n" +
			"<H2>Step 2 - which spinlock or spinlocks are causing the contention?</H2> \n" +
			"<UL> \n" +
			"    <LI>Using sp_sysmon, if AseTune 'Spinlock Sum/Act' check column 'description'.</LI>\n" +
			" \n" +
			"    <LI>There are several spinlocks listed, but only contention % is shown<BR> \n" +
			"        &diams; Object Manager Spinlock Contention<BR> \n" +
			"        &diams; Object Spinlock Contention<BR> \n" +
			"        &diams; Index Spinlock Contention<BR> \n" +
			"        &diams; Index Hash Spinlock Contention<BR> \n" +
			"        &diams; Partition Spinlock Contention<BR> \n" +
			"        &diams; Partition Hash Spinlock Contention<BR> \n" +
			"        &diams; Lock Hashtables Spinlock Contention<BR> \n" +
			"        &diams; Data Caches Spinlock Contention<BR> \n" +
			"    </LI> \n" +
			" \n" +
			"    <LI>High contention on any of these may indicate a problem<BR> \n" +
			"        But, you may have contention on other spinlocks not reported in sp_sysmon \n" +
			"    </LI> \n" +
			" \n" +
			"    <LI>Using MDA table monSpinockActivity<BR> \n" +
			"        This table was added in 15.7  ESD#2 \n" +
			"    </LI> \n" +
			" \n" +
			"    <LI>Query using standard SQL.</LI> \n" +
			"</UL> \n" +
			" \n" +
			"One possible query showing the top 10 spinlocks by number of spins over a one-minute interval<BR> \n" +
			"Or in AseTune: right click on the GUI \"tab\" for \"Spinlock Sum\" and choose Properties to see the SQL Statement used<BR> \n" +
			"<PRE> \n" +
			"    select * into #t1 from monSpinlockActivity \n" +
			" \n" +
			"    waitfor delay \"00:01:00\" \n" +
			" \n" +
			"    select * into #t2 from monSpinlockActivity \n" +
			" \n" +
			"    select top 10 \n" +
			"        convert(char(30),a.SpinlockName) as SpinlockName, \n" +
			"        (b.Grabs - a.Grabs) as Grabs, (b.Spins - a.Spins) as Spins, \n" +
			"        (b.Waits � a.Waits) as Waits, \n" +
			"        case when a.Grabs = b.Grabs then 0.00 else convert (numeric(5,2),(100.0 * (b.Waits - a.Waits))/(b.Grabs - a.Grabs)) end as Contention \n" +
			"    from #t1 a, #t2 b \n" +
			"    where a.SpinlockName = b.SpinlockName \n" +
			"    order by 3 desc \n" +
			"</PRE> \n" +
			" \n" +
			"<H3>Possible Issues with monSpinlockActivity</H3> \n" +
			"<UL> \n" +
			"    <LI>Spinlocks with multiple instances will get aggregated<BR> \n" +
			"        For example, all default data cache partition spinlocks will show up as one line<BR> \n" +
			"        This can make it impossible to see if just one cache partition is causing the problem<BR> \n" +
			"    </LI> \n" +
			" \n" +
			"    <LI>You must set the 'enable spinlock monitoring' configuration variable<BR> \n" +
			"        Tests show that this adds about a 1 percent overhead to a busy server.<BR> \n" +
			"    </LI> \n" +
			" \n" +
			"    <LI>monSpinlockActivity  does show the current and last owner KPIDs. This can be useful to check if certain processes are the ones heavily hitting certain spinlocks.</LI> \n" +
			"</UL> \n" +
			" \n" +
			" \n" +
			"<H2>Step 3 - what tuning to can be done to help reduce the problem</H2> \n" +
			"<UL> \n" +
			"    <LI>This is going to depend a great deal on which spinlock(s) the high spins are on.</LI> \n" +
			"    <LI>Note as well that it is quite possible to reduce contention on one spinlock only to have it increase on another</LI> \n" +
			"</UL> \n" +
			" \n" +
			"<H2>Some of the more common spinlocks and possible remedies</H2> \n" +
			"	<H3>Object Manager Spinlock (Resource->rdesmgr_spin)</H3> \n" +
			"	<UL> \n" +
			"		<LI>Make sure that sufficient �number of open objects� have been configured.</LI> \n" +
			"       <LI>Identify <I>hot</I> objects by using monOpenObjectActivity. In AseTune - Performance Counter: Object/Access -&gt; Objects</LI> \n" +
			"		<LI>Use dbcc tune (des_bind) to bind the hot objects to the DES cache.</LI> \n" +
			"		<LI>The reason this works is that the spinlock is used to protect the DES keep count in order to make sure an in-use DES does not get scavenged. When the DES is bound that whole process gets skipped.</LI> \n" +
			"	</UL> \n" +
			" \n" +
			"    <H3>Data Cache spinlocks</H3> \n" +
			"    <UL> \n" +
			"        <LI>The best single method to reduce data cache spinlock usage is to increae the number of partitions in the data cache.</LI> \n" +
			"        <LI>Note that if a cache can be set to \"relaxed LRU\" the spinlock usage may be decreased dramatically. <BR> \n" +
			"            This is because the relaxed LRU cache does not maintain the LRU->MRU chain, and so does not need to grab the spinlock to move pages to the MRU side. \n" +
			"        </LI> \n" +
			"        <LI>There are definite requirements for this to help (a cache that has high turnover is a very poor candidate for relaxed LRU).</LI> \n" +
			"    </UL> \n" +
			" \n" +
			"    <H3>Procedure Cache Spinlock (Resource->rproccache_spin)</H3> \n" +
			"    <UL> \n" +
			"        <LI>This spinlock is used when allocating or freeing pages from the global procedure cache memory pool (this includes statement cache).</LI> \n" +
			"        <LI>Some possible causes include<BR> \n" +
			"            &diams; Proc cache too small � procs and statements being frequently removed/replaced.<BR> \n" +
			"            &diams; Procedure recompilations<BR> \n" +
			"            &diams; Large scale allocations<BR> \n" +
			"        </LI> \n" +
			"        <LI>To reduce pressure on the spinlock</LI> \n" +
			"        <LI>Eliminate the cause(s) for procedure recompilations (maybe TF 299)</LI> \n" +
			"        <LI>If you are running a version prior to ASE 15.7 ESD#4 <B>upgrade</B>. <BR>" +
			"            ASE 15.7 ESD#4 and 4.2 have some fixes to hold the spinlock for less time." +
			"        </LI> \n" +
			"        <LI>Trace flags 753 and 757 can help reduce large-scale allocations</LI> \n" +
			"        <LI>In ASE versions past 15.7 SP100, use the configuration option \"enable large chunk elc\".</LI> \n" +
			"        <LI>Use dbcc proc_cache(free_unused) as temporary help to reduce spinlock/cpu usage.</LI> \n" +
			"    </UL> \n" +
			" \n" +
			"    <H3>Procedure Cache Manager Spinlock (Resource->rprocmgr_spin)</H3> \n" +
			"    <UL> \n" +
			"        <LI>This spinlock is used whenever moving procedures and dynamic SQL into or out of procedure cache.</LI> \n" +
			"        <LI>This spinlock was also used prior to ASE 15.7 ESD#1 when updating the memory accounting structures (pmctrl).<BR> \n" +
			"            &diams; Due to contention a separate spinlock was created.<BR> \n" +
			"        </LI> \n" +
			"        <LI>Causes of high contention include:<BR> \n" +
			"            &diams; Heavy use of dynamic SQL<BR> \n" +
			"            &diams; Procedure cache sized too small<BR> \n" +
			"        </LI> \n" +
			"        <LI>Possible remedies are the same as for rproccache_spin</LI> \n" +
			"    </UL> \n" +
			" \n" +
			"    <H3>Lock Manager spinlocks (fglockspins , addrlockspins, tablockspins)</H3> \n" +
			"    <UL> \n" +
			"        <LI>These spinlocks are used to protect the lock manager hashtables.</LI> \n" +
			"        <LI>If the lock HWMs are set too high, that means more locks and more contention</LI> \n" +
			"        <LI>Configuration tunables are the primary way to address this<BR> \n" +
			"            &diams; lock spinlock ratio<BR> \n" +
			"            &diams; lock address spinlock ratio<BR> \n" +
			"            &diams; lock table spinlock ratio<BR> \n" +
			"            &diams; lock hashtable size<BR> \n" +
			"        </LI> \n" +
			"    </UL> \n" +
			" \n" +
			"<H2>What not to do</H2> \n" +
			"<UL> \n" +
			"        <LI><B>Resist the urge to add more engines because cpu is high</B><BR> \n" +
			"            &diams; Adding additional engines when the high cpu busy is caused by spinlock contention will only make matter worse<BR> \n" +
			"            &diams; Adding more \"spinners\" will simply increase the amount of time it takes each spid to obtain the spinlock, slowing things down even more.<BR> \n" +
			"        </LI> \n" +
			"</UL> \n" +
			"</HTML> \n" +
			"";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"sysmonitors"};
	public static final String[] NEED_ROLES       = new String[] {"sa_role"};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {"contention"};
	public static final String[] DIFF_COLUMNS     = new String[] {"grabs", "waits", "spins"};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = true;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 300;
	public static final int      DEFAULT_QUERY_TIMEOUT          = 30;

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

		return new CmSpinlockSum(counterController, guiController);
	}

	public CmSpinlockSum(ICounterController counterController, IGuiController guiController)
	{
		super(counterController,
				CM_NAME, GROUP_NAME, /*sql*/null, /*pkList*/null, 
				DIFF_COLUMNS, PCT_COLUMNS, MON_TABLES, 
				NEED_ROLES, NEED_CONFIG, NEED_SRV_VERSION, NEED_CE_VERSION, 
				NEGATIVE_DIFF_COUNTERS_TO_ZERO, IS_SYSTEM_CM, DEFAULT_POSTPONE_TIME);

		setDisplayName(SHORT_NAME);
		setDescription(HTML_DESC);

		setIconFile(GUI_ICON_FILE);

		setCounterController(counterController);
		setGuiController(guiController);

		addCmDependsOnMe(CmSysmon.CM_NAME); // go and check CmSysmon if it needs refresh as part of this refresh (if CmSysmon needs to be refreshed, lets refresh since he depends on me)

		addTrendGraphs();
		
		CounterSetTemplates.register(this);
	}


	//------------------------------------------------------------
	// Implementation
	//------------------------------------------------------------
	private static final String  PROP_PREFIX                       = CM_NAME;

	public static final String  PROPKEY_sample_resetAfter          = PROP_PREFIX + ".sample.reset.after";
	public static final boolean DEFAULT_sample_resetAfter          = false;

	public static final String  PROPKEY_sample_fglockspins          = PROP_PREFIX + ".sample.fglockspins";
	public static final boolean DEFAULT_sample_fglockspins          = false;

	@Override
	protected void registerDefaultValues()
	{
		super.registerDefaultValues();

		Configuration.registerDefaultValue(PROPKEY_sample_resetAfter,  DEFAULT_sample_resetAfter);
		Configuration.registerDefaultValue(PROPKEY_sample_fglockspins, DEFAULT_sample_fglockspins);
	}

	
	private void addTrendGraphs()
	{
	}

	@Override
	protected TabularCntrPanel createGui()
	{
		return new CmSpinlockSumPanel(this);
	}

	@Override
	public String[] getDependsOnConfigForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		return NEED_CONFIG;
	}

	@Override
	protected CmSybMessageHandler createSybMessageHandler()
	{
		CmSybMessageHandler msgHandler = super.createSybMessageHandler();

		// in some ASE 15.5 ESD#4 versions message 'spin_type, spin_name' is thrown...
		// this in conjunction when dbcc traceon(3604) has been enabled.
		// I dont know why this is happening, most likly a bug...
		// So just discard this message
		msgHandler.addDiscardMsgStr("spin_type, spin_name");

		return msgHandler;
	}


	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		DbmsVersionInfoSybaseAse aseVersionInfo = (DbmsVersionInfoSybaseAse) versionInfo;
//		long    srvVersion       = aseVersionInfo.getLongVersion();
		boolean isClusterEnabled = aseVersionInfo.isClusterEdition();

		List <String> pkCols = new LinkedList<String>();

		if (isClusterEnabled)
			pkCols.add("instanceid");

		pkCols.add("spinName");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		DbmsVersionInfoSybaseAse aseVersionInfo = (DbmsVersionInfoSybaseAse) versionInfo;
		long    srvVersion       = aseVersionInfo.getLongVersion();
		boolean isClusterEnabled = aseVersionInfo.isClusterEdition();

		Configuration conf = Configuration.getCombinedConfiguration();
		boolean sample_resetAfter = conf.getBooleanProperty(PROPKEY_sample_resetAfter, DEFAULT_sample_resetAfter);

		// sum(int) may cause: "Arithmetic overflow occurred"
		// max int is: 2147483647, so if we sum several rows we may overflow the integer
		// so on pre 15.0 use numeric instead, over 15.0 use bigint
		String datatype    = "numeric(19,0)"; // 19.0 is unsigned bigint, so lets use that... 
		String optGoalPlan = "";

		if (srvVersion >= Ver.ver(15,0))
		{
			datatype    = "bigint";
		}
		if (srvVersion >= Ver.ver(15,0,2))
		{
			optGoalPlan = "plan '(use optgoal allrows_dss)' \n";
		}

		addDropTempTable("#spin_names");
		addDropTempTable("#sysmonitorsP");
		addDropTempTable("#sysmonitorsW");
		addDropTempTable("#sysmonitorsS");

		/*
		 * Retrieve the spinlocks. There are three spinlock counters collected by dbcc monitor:
		 *	- spinlock_p_0 -> Spinlock "grabs" - as in attempted grabs for the spinlock - includes waits
		 *	- spinlock_w_0 -> Spinlock "waits" - usually a good sign of contention
		 *	- spinlock_s_0 -> Spinlock "spins" - this is the CPU spins that drives up CPU utilization
		 *	                  The higher the spin count, the more CPU usage and the more serious
		 *	                  the performance impact of the spinlock on other processes not waiting
		 */
		String preDropTmpTables =
			"\n " +
			"/*------ drop tempdb objects if we failed doing that in previous execution -------*/ \n" +
			"if ((select object_id('#spin_names'))   is not null) drop table #spin_names \n" +
			"if ((select object_id('#sysmonitorsP')) is not null) drop table #sysmonitorsP \n" +
			"if ((select object_id('#sysmonitorsW')) is not null) drop table #sysmonitorsW \n" +
			"if ((select object_id('#sysmonitorsS')) is not null) drop table #sysmonitorsS \n" +
			"go \n" +
			"\n";
		
		String sqlCreateTmpTabCache = 
			"\n " +
			"/*------ temp table to hold 'cache names' and other 'named' stuff -------*/ \n" +
			"create table #spin_names     \n " +
			"(                            \n " +
			"  spin_name  varchar(50),    \n " +
			"  spin_type  varchar(20),    \n " +
			"  spin_desc  varchar(100),   \n " +
			"  start_id   int,            \n " +
			"  instanceid tinyint         /* if ASE SMP, this will always be value 1 */\n " +
			")                            \n " +
			"\n" +
			"/*------ DATA CACHES -------*/ \n" +
			"insert into #spin_names(spin_name, spin_type, spin_desc, start_id, instanceid) \n" +
			"select comment, 'CACHELET', 'Data cache, spinlock instance', 0, " + (isClusterEnabled ? "instanceid" : "1") + " \n" + 
			"  from master..syscurconfigs \n" +
			" where config=19 and value > 0 \n" +
			"\n" +
			"/*------ SPINLOCK INSTANCES, for other stuff -------*/ \n" +
			"insert into #spin_names(spin_name, spin_type, spin_desc, start_id, instanceid) \n" +
			"select 'Kernel->erunqspinlock', 'KERNEL-INST', 'Engine Runqueue, spinlock instance', 0, " + (isClusterEnabled ? "instanceid" : "1") + " \n" +
			"  from master..syscurconfigs \n" + 
			" where config=19 and value > 0 \n" +
			"   and comment = 'default data cache' /* if ASE CE, then this will produce number of Cluster Instances, ASE SMP = 1 row */ \n" + 
			"\n";

		String sqlUpdateTmpTabCache = 
			"/*------ SET start_id -------*/ \n" +
			"update #spin_names \n" +
			"   set start_id = isnull((select min(M.field_id) \n" +
			"                          from #sysmonitorsP M\n" +
			"                          where #spin_names.spin_name  = M.field_name \n" +
			"                            and #spin_names.instanceid = " + (isClusterEnabled ? "M.instanceid" : "1") + ") \n" +
			"                  ,start_id) \n";

		String sqlDropTmpTabCache = 
			"\n" +
			"drop table #spin_names \n";


		String instanceid = ""; // If cluster, we need to use column 'instanceid' as well
		if (isClusterEnabled) 
			instanceid = ", instanceid";


		// If ASE-CE, we might want to exclude some fields, for example 'fglockspins' has more than 100.000 records...
		String restrictTmpSysmonitorsWhere = "";
		if (isClusterEnabled)
		{
			String disregardFields = "";
			if (conf.getBooleanProperty(PROPKEY_sample_fglockspins, DEFAULT_sample_fglockspins) == false)
				disregardFields += "'fglockspins', ";
			
			if ( ! disregardFields.equals(""))
			{
				disregardFields = StringUtil.removeLastComma(disregardFields);
				restrictTmpSysmonitorsWhere = " and field_name not in("+disregardFields+")";
			}
		}

		// if 12.5.x: group_name = 'spinlock_p_0' or 'spinlock_w_0' or 'spinlock_s_0'  
		// if 15.7.x: group_name = 'spinlock_p'   or 'spinlock_w'   or 'spinlock_s'  
		String spinPostfix = "_0";
		if (srvVersion >= Ver.ver(15,7))
			spinPostfix = "";
		
		String sqlCreateTmpSysmonitors = 
			"/*------ Copy 'spinlock_[p|w|s]' rows to local tempdb, this reduces IO in joins below -------*/ \n" +
			"/*------ Deal with overflow by bumping up to a higher datatype: and adding the negative delta on top of Integer.MAX_VALUE -------*/ \n" +
			"declare @int_max "+datatype+"    set @int_max =  2147483647 \n" +
			"declare @int_min "+datatype+"    set @int_min = -2147483648 \n" +
			"select field_name=convert(varchar(79),field_name), field_id, value = CASE WHEN (value < 0) THEN @int_max + (value - @int_min) ELSE convert("+datatype+", value) END "+instanceid+" into #sysmonitorsP FROM master..sysmonitors WHERE group_name = 'spinlock_p"+spinPostfix+"' "+restrictTmpSysmonitorsWhere+" \n" +
			"select field_name=convert(varchar(79),field_name), field_id, value = CASE WHEN (value < 0) THEN @int_max + (value - @int_min) ELSE convert("+datatype+", value) END "+instanceid+" into #sysmonitorsW FROM master..sysmonitors WHERE group_name = 'spinlock_w"+spinPostfix+"' "+restrictTmpSysmonitorsWhere+" \n" +
			"select field_name=convert(varchar(79),field_name), field_id, value = CASE WHEN (value < 0) THEN @int_max + (value - @int_min) ELSE convert("+datatype+", value) END "+instanceid+" into #sysmonitorsS FROM master..sysmonitors WHERE group_name = 'spinlock_s"+spinPostfix+"' "+restrictTmpSysmonitorsWhere+" \n";
		sqlCreateTmpSysmonitors += 
			"-- A 'go' here will make the second batch optimize better \n" +
			"go \n";
		
		String sqlDropTmpTabPWS   = 
			"\n" +
			"drop table #sysmonitorsP \n"+
			"drop table #sysmonitorsW \n" +
			"drop table #sysmonitorsS \n";
		
		String sqlResetCountersAfterSample = "";
		if (sample_resetAfter)
		{
			sqlResetCountersAfterSample = 
				" \n" +
				"/*------ RESET THE in-memory monitor counters (just for spinlocks) -------*/ \n" +
				"DBCC monitor('clear', 'spinlock_s', 'on') \n";
		}

		String sqlSampleSpins =
			"/*------ SAMPLE THE in-memory monitor counters into table master..sysmonitors -------*/ \n" +
			"DBCC monitor('sample', 'all',        'on') \n" +
			"DBCC monitor('sample', 'spinlock_s', 'on') \n" +
			"\n" +
			sqlCreateTmpSysmonitors +
			sqlResetCountersAfterSample +
			" \n" +
			sqlUpdateTmpTabCache +
			" \n" +
			"/*------ get data: for all spinlocks -------*/ \n" +
			"SELECT \n" +
			"  type         = \n" +
			"    CASE \n" +
			"      WHEN P.field_name like 'Dbtable->%'  THEN convert(varchar(20), 'DBTABLE')  \n" +
			"      WHEN P.field_name like 'Dbt->%'      THEN convert(varchar(20), 'DBTABLE')  \n" +
			"      WHEN P.field_name like 'Dbtable.%'   THEN convert(varchar(20), 'DBTABLE')  \n" +
			"      WHEN P.field_name like 'Resource->%' THEN convert(varchar(20), 'RESOURCE') \n" +
			"      WHEN P.field_name like 'Kernel->%'   THEN convert(varchar(20), 'KERNEL')   \n" +
			"      WHEN P.field_name in (select spin_name from #spin_names where spin_type = 'CACHELET')   \n" +
			"                                           THEN convert(varchar(20), 'CACHE') \n" +
			"      ELSE convert(varchar(20), 'OTHER')  \n" +
			"    END, \n" +
			(isClusterEnabled ? "P.instanceid, \n" : "") +
			"  spinName     = convert(varchar(50), P.field_name), \n" +
			"  instances    = count(P.field_id), \n" +
			"  grabs        = sum(convert("+datatype+", P.value)), \n" +
			"  waits        = sum(convert("+datatype+", W.value)), \n" +
			"  spins        = sum(convert("+datatype+", S.value)), \n" +
			"  contention   = convert(numeric(4,1), null), \n" +
			"  spinsPerWait = convert(numeric(12,1), null), \n" +
			"  description  = convert(varchar(100), '') \n" +
			"FROM #sysmonitorsP P, #sysmonitorsW W, #sysmonitorsS S \n" +
			"WHERE P.field_id   = W.field_id   \n" +
			"  AND P.field_name = W.field_name \n" +
			"  AND P.field_id   = S.field_id   \n" +
			"  AND P.field_name = S.field_name \n";
		if (isClusterEnabled) 
		{
			sqlSampleSpins +=
			"  AND P.instanceid = W.instanceid \n" +
			"  AND P.instanceid = S.instanceid \n" +
			"GROUP BY P.instanceid, P.field_name \n" +
			"ORDER BY 3, 2 \n" +
			optGoalPlan;
		}
		else 
		{
			sqlSampleSpins +=
			"GROUP BY P.field_name \n" +
			"ORDER BY 2 \n" +
			optGoalPlan;
		}

		//---------------------------------------------
		// For CACHES and other FINER GRANULARITY spinlocks
		// do the calculation on EACH Cache partition or spinlock instance
		//---------------------------------------------
		String sqlForCaches =
			"\n" +
			"/*------ get data: For some selected spinlocks with multiple instances -------*/ \n" +
			"SELECT \n" + 
			"  type         = N.spin_type, \n" +
			(isClusterEnabled ? "P.instanceid, \n" : "") +
			"  spinName     = convert(varchar(50), convert(varchar(40),P.field_name) + ' # ' + convert(varchar(5), P.field_id-N.start_id)), \n" +
			"  instances    = convert(int,1), \n" +
			"  grabs        = convert("+datatype+", P.value), \n" +
			"  waits        = convert("+datatype+", W.value), \n" +
			"  spins        = convert("+datatype+", S.value), \n" +
			"  contention   = convert(numeric(4,1), null), \n" +
			"  spinsPerWait = convert(numeric(12,1), null), \n" +
			"  description  = N.spin_desc \n" +
			"FROM #sysmonitorsP P, #sysmonitorsW W, #sysmonitorsS S, #spin_names N \n" +
			"WHERE P.field_id   = W.field_id   \n" +
			"  AND P.field_name = W.field_name \n" +
			"  AND P.field_id   = S.field_id   \n" +
			"  AND P.field_name = S.field_name \n" +
		    "  AND P.field_name = N.spin_name \n";
		if (isClusterEnabled) 
		{
			sqlForCaches +=
			"  AND P.instanceid = W.instanceid \n" +
			"  AND P.instanceid = S.instanceid \n" +
		    "  AND P.instanceid = N.instanceid \n" +
			"ORDER BY 3, 2 \n" +
			optGoalPlan;
		}
		else 
		{
			sqlForCaches +=
			"  AND N.instanceid = 1 \n" +
			"ORDER BY 2 \n" +
			optGoalPlan;
		}

		String sql = preDropTmpTables + sqlCreateTmpTabCache + sqlSampleSpins + sqlForCaches + sqlDropTmpTabCache + sqlDropTmpTabPWS;

		return sql;
	}

	@Override
	public String getSqlInitForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		long srvVersion = versionInfo.getLongVersion();

		//---------------------------------------------
		// SQL INIT (executed first time only)
		// AFTER 12.5.2 traceon(8399) is no longer needed
		//---------------------------------------------
		String sqlInit = "DBCC traceon(3604) \n";

		if (srvVersion < Ver.ver(12,5,2))
			sqlInit += "DBCC traceon(8399) \n";

		if (srvVersion >= Ver.ver(15,0,2) || (srvVersion >= Ver.ver(12,5,4,1) && srvVersion < Ver.ver(15,0)) )
		{
			sqlInit = "set switch on 3604 with no_info \n";
		}

		sqlInit += "DBCC monitor('select', 'all',        'on') \n" +
		           "DBCC monitor('select', 'spinlock_s', 'on') \n";

		return sqlInit;
	}
	@Override
	public String getSqlCloseForVersion(DbxConnection conn, DbmsVersionInfo versionInfo) 
	{
		String sqlClose = 
			"--DBCC monitor('select', 'all',        'off') \n" +
			"--DBCC monitor('select', 'spinlock_s', 'off') \n";
		return sqlClose;
	};

	/** Used by the: Create 'Offline Session' Wizard */
	@Override
	public List<CmSettingsHelper> getLocalSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();
		
		list.add(new CmSettingsHelper("Clear Counters",              PROPKEY_sample_resetAfter  , Boolean.class, conf.getBooleanProperty(PROPKEY_sample_resetAfter  , DEFAULT_sample_resetAfter  ), DEFAULT_sample_resetAfter,  CmSpinlockSumPanel.TOOLTIP_sample_resetAfter  ));

//		if (isClusterEnabled())
//		{
		list.add(new CmSettingsHelper("Include Field 'fglockspins'", PROPKEY_sample_fglockspins , Boolean.class, conf.getBooleanProperty(PROPKEY_sample_fglockspins , DEFAULT_sample_fglockspins ), DEFAULT_sample_fglockspins, CmSpinlockSumPanel.TOOLTIP_sample_fglockspins ));
//		}

		return list;
	}


	@Override
	public void localCalculation(CounterSample prevSample, CounterSample newSample, CounterSample diffData)
	{
		MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

		long grabs,        waits,        spins;
		int  grabsId = -1, waitsId = -1, spinsId = -1, contentionId = -1, spinsPerWaitId = -1;
		int  pos_name = -1,  pos_desc = -1;

		// Find column Id's
		List<String> colNames = diffData.getColNames();
		if (colNames == null)
			return;
		for (int colId = 0; colId < colNames.size(); colId++)
		{
			String colName = (String) colNames.get(colId);
			if      (colName.equals("grabs"))        grabsId        = colId;
			else if (colName.equals("waits"))        waitsId        = colId;
			else if (colName.equals("spins"))        spinsId        = colId;
			else if (colName.equals("contention"))   contentionId   = colId;
			else if (colName.equals("spinsPerWait")) spinsPerWaitId = colId;
			else if (colName.equals("spinName"))     pos_name       = colId;
			else if (colName.equals("description"))  pos_desc       = colId;
		}

		// Loop on all diffData rows
		for (int rowId = 0; rowId < diffData.getRowCount(); rowId++)
		{
			grabs = ((Number) diffData.getValueAt(rowId, grabsId)).longValue();
			waits = ((Number) diffData.getValueAt(rowId, waitsId)).longValue();
			spins = ((Number) diffData.getValueAt(rowId, spinsId)).longValue();

			// contention
			if (grabs > 0)
			{
				BigDecimal contention = new BigDecimal( ((1.0 * (waits)) / grabs) * 100 ).setScale(1, BigDecimal.ROUND_HALF_EVEN);

				// Keep only 3 decimals
				// row.set(AvgServ_msId, new Double (AvgServ_ms/1000) );
				diffData.setValueAt(contention, rowId, contentionId);
			}
			else
				diffData.setValueAt(new BigDecimal(0), rowId, contentionId);

			// spinsPerWait
			if (waits > 0)
			{
				BigDecimal spinWarning = new BigDecimal( ((1.0 * (spins)) / waits) ).setScale(1, BigDecimal.ROUND_HALF_EVEN);

				diffData.setValueAt(spinWarning, rowId, spinsPerWaitId);
			}
			else
				diffData.setValueAt(new BigDecimal(0), rowId, spinsPerWaitId);

			// set description
			if (mtd != null && pos_name >= 0 && pos_desc >= 0)
			{
				Object o = diffData.getValueAt(rowId, pos_name);

				if (o instanceof String)
				{
					String name = (String)o;

					String desc = mtd.getSpinlockDescription(name);
					if (desc != null)
					{
						newSample.setValueAt(desc, rowId, pos_desc);
						diffData .setValueAt(desc, rowId, pos_desc);
					}
				}
			}
		}
	}

	/**
	 * if counters has been reset outside, shorten the sample interval
	 */
	@Override
	public CounterSample computeDiffCnt(CounterSample oldSample, CounterSample newSample, List<Integer> deletedRows, List<Integer> newDeltaRows, List<String> pkCols, boolean[] isDiffCol, boolean isCountersCleared)
	{
// The below dosn't work if we don't have a counter that is predictive, like clock_ticks or similar
// if we use a counter that *could* become bigger in the second sample, the below wont work
// so for know trust the isCountersCleared input parameter, until I figure out a better way

//		// Sanity check if "isCountersCleared" is valid...
//		// get a known value we know will increment all the time from old/new value and compare
//		if (isCountersCleared)
//		{
//			// PK: spinName
//			String key = "default data cache"; // TODO: if ASE-CE we should try to get in "instance" as the first part of the key
//
//			Object oldVal = oldSample.getRowValue(key, "grabs");
//			Object newVal = newSample.getRowValue(key, "grabs");
//
//			if (oldVal != null && newVal != null)
//			{
//				if (oldVal instanceof Number && newVal instanceof Number)
//				{
//					// This will work here since "value" is converted to a unsigned int
//					isCountersCleared =  ((Number)newVal).longValue() < ((Number)oldVal).longValue();
//					
//					// should we set/reset the CM isCountersCleared or not...
//					//setIsCountersCleared(isCountersCleared);
//
//System.out.println(getName()+":computeDiffCnt(): CountersClearedCheck(after) PK='"+key+"', oldVal='"+oldVal+"', newVal='"+newVal+"', isCountersCleared="+isCountersCleared+".");
//					_logger.debug(getName()+":computeDiffCnt(): CountersClearedCheck(after) PK='"+key+"', oldVal='"+oldVal+"', newVal='"+newVal+"', isCountersCleared="+isCountersCleared+".");
//				}
//			}
//		}
//System.out.println(getName()+":computeDiffCnt(): isCountersCleared="+isCountersCleared+".");

		// Let super do all the work
		CounterSample diff = super.computeDiffCnt(oldSample, newSample, deletedRows, newDeltaRows, pkCols, isDiffCol, isCountersCleared);
		
		// Adjust interval (make it shorter) if counters has been cleared
		if (isCountersCleared && getCounterClearTime() != null)
		{
			long interval = getSampleTime().getTime() - getCounterClearTime().getTime();
			newSample.setSampleInterval(interval);
			diff     .setSampleInterval(interval);
			//rate will grab the interval from diff
			
			setSampleInterval(interval);
		}

		return diff;
	}

	/**
	 * called by CounterModel.computeDiffCnt() to calculate a specific "cell" 
	 * <p>
	 * in ASE 12.5.x datatype will be numeric(19,0), which in java becomes BigDecimal<br>
	 * in ASE >= 15  datatype will be bigint,        which in java becomes Long<br>
	 * <br>
	 * So lets do special calculation on them
	 */
	@Override
	protected Number diffColumnValue(Number prevColVal, Number newColVal, boolean negativeDiffCountersToZero, String counterSetName, String colName, boolean isCountersCleared)
	{
		Number diffColVal = null;

		// If counters has been cleared, simply return the new value... no need to do diff calculation, and if we do they will be wrong...
		if (isCountersCleared)
			return newColVal;

		// ASE 12.5.x data
		if (newColVal instanceof BigDecimal)
		{
			diffColVal = new BigDecimal(newColVal.doubleValue() - prevColVal.doubleValue());
			if (diffColVal.doubleValue() < 0)
			{
				// Do special stuff for diff counters on CmSpinlockSum and ASE is 12.5.x, then counters will be delivered as numeric(19,0)
				// but is really signed int, then we need to check for wrapped signed int values
				// prevColVal is "near" UNSIGNED-INT-MAX and newColVal is "near" 0
				// Then do special calculation: (UNSIGNED-INT-MAX - prevColVal) + newColVal + 1      (+1 to handle passing value 0)
				// NOTE: we might also want to check COUNTER-RESET-DATE (if it has been done since last sample, then we can't trust the counters)
				if ( ! isCountersCleared )
				{
					Number beforeReCalc = diffColVal;
//					int  threshold      = 10000000;    // 10 000 000
					long maxUnsignedInt = 4294967295L; // 4 294 967 295

//					if (prevColVal.doubleValue() > (maxUnsignedInt - threshold) && newColVal.doubleValue() < threshold)
						diffColVal = new BigDecimal((maxUnsignedInt - prevColVal.doubleValue()) + newColVal.doubleValue() + 1);
					_logger.debug("diffColumnValue(): CM='"+counterSetName+"', BigDecimal(ASE-numeric) : CmSpinlockSum(colName='"+colName+"', isCountersCleared="+isCountersCleared+"):  AFTER: do special calc. newColVal.doubleValue()='"+newColVal.doubleValue()+"', prevColVal.doubleValue()='"+prevColVal.doubleValue()+"', beforeReCalc.doubleValue()='"+beforeReCalc.doubleValue()+"', diffColVal.doubleValue()='"+diffColVal.doubleValue()+"'.");
				}

				if (diffColVal.doubleValue() < 0)
					if (negativeDiffCountersToZero)
						diffColVal = new BigDecimal(0);
			}
		}
		// ASE >= 15.x data
		else if (newColVal instanceof Long)
		{
			diffColVal = new Long(newColVal.longValue() - prevColVal.longValue());
			if (diffColVal.longValue() < 0)
			{
				// Do special stuff for diff counters on CmSpinlockSum and ASE is above 15.x, then counters will be delivered as bigint
				// but is really signed int, then we need to check for wrapped signed int values
				// prevColVal is "near" UNSIGNED-INT-MAX and newColVal is "near" 0
				// Then do special calculation: (UNSIGNED-INT-MAX - prevColVal) + newColVal + 1      (+1 to handle passing value 0)
				// NOTE: we might also want to check COUNTER-RESET-DATE (if it has been done since last sample, then we can't trust the counters)
				if ( ! isCountersCleared )
				{
					Number beforeReCalc = diffColVal;
//					int  threshold      = 10000000;    // 10 000 000
					long maxUnsignedInt = 4294967295L; // 4 294 967 295
					
//					if (prevColVal.longValue() > (maxUnsignedInt - threshold) && newColVal.longValue() < threshold)
						diffColVal = new Long((maxUnsignedInt - prevColVal.longValue()) + newColVal.longValue() + 1);
					_logger.debug("diffColumnValue(): CM='"+counterSetName+"', Long(ASE-bigint) : CmSpinlockSum(colName='"+colName+"', isCountersCleared="+isCountersCleared+"):  AFTER: do special calc. newColVal.longValue()='"+newColVal.longValue()+"', prevColVal.longValue()='"+prevColVal.longValue()+"', beforeReCalc.longValue()='"+beforeReCalc.longValue()+"', diffColVal.longValue()='"+diffColVal.longValue()+"'.");
				}

				if (diffColVal.longValue() < 0)
					if (negativeDiffCountersToZero)
						diffColVal = new Long(0);
			}
		}
		else
		{
			diffColVal = super.diffColumnValue(prevColVal, newColVal, negativeDiffCountersToZero, counterSetName, colName, isCountersCleared);
		}

		return diffColVal;
	}


	@Override
	public String getToolTipTextOnTableCell(MouseEvent e, String colName, Object cellValue, int modelRow, int modelCol) 
	{
		String tooltip = CmSpinlockSum.getToolTipTextOnTableCell(this, e, colName, cellValue, modelRow, modelCol);

		// If nothing was found, call super
		if (tooltip == null)
			tooltip = super.getToolTipTextOnTableCell(e, colName, cellValue, modelRow, modelCol);

		return tooltip;
	}

	/**
	 * This one is also used from CmSpinlockActivity, thats why it's static
	 * 
	 * @param cm
	 * @param e
	 * @param colName
	 * @param cellValue
	 * @param modelRow
	 * @param modelCol
	 * @return
	 */
	public static String getToolTipTextOnTableCell(CountersModel cm, MouseEvent e, String colName, Object cellValue, int modelRow, int modelCol) 
	{
		if (cm == null)
			return null;

		if (    "spinName"    .equals(colName) || "description".equals(colName)   // CmSpinlockSum 
		     || "SpinlockName".equals(colName) || "Description".equals(colName) ) // CmSpinlockActivity
		{
			boolean source_isSpinlockSum = "CmSpinlockSum".equals(cm.getName());
			
			int type_mpos        = cm.findColumn(source_isSpinlockSum ? "type"     : "Type");
			int spinName_mpos    = cm.findColumn(source_isSpinlockSum ? "spinName" : "SpinlockName");

			if (type_mpos >= 0 && spinName_mpos >= 0)
			{
				String type     = (String) cm.getValueAt(modelRow, type_mpos);
				String spinName = (String) cm.getValueAt(modelRow, spinName_mpos);
				
				// Data Cache spinlocks
				if ("CACHE".equals(type))
				{
					return
    						"<HTML>" +
    						"<H3>What tuning can be done to help reduce the problem</H3> \n" +
    						"<UL> \n" +
    						"    <LI>The best single method to reduce data cache spinlock usage is to increae the number of partitions in the data cache.</LI> \n" +
    						"    <LI>Note that if a cache can be set to \"relaxed LRU\" the spinlock usage may be decreased dramatically. <BR> \n" +
    						"        This is because the relaxed LRU cache does not maintain the LRU->MRU chain, and so does not need to grab the spinlock to move pages to the MRU side. \n" +
    						"    </LI> \n" +
    						"    <LI>There are definite requirements for this to help (a cache that has high turnover is a very poor candidate for relaxed LRU).</LI> \n" +
    						"</UL> \n" +
    						"<B>Note</B>:   Its quite possible to reduce contention on one spinlock only to have it increase on another <BR> \n" +
    						"<B>Source</B>: The origin text for above information can be found at: <A HREF=\"http://wiki.scn.sap.com/wiki/display/SYBASE/Spinlocks+and+CPU+usage+in+SAP+ASE\">http://wiki.scn.sap.com/wiki/display/SYBASE/Spinlocks+and+CPU+usage+in+SAP+ASE</A> <BR> \n" +
    						"</HTML>";
				}

				// Data Cache spinlocks, but on the individual CACHELET
				if ("CACHELET".equals(type))
				{
					return
    						"<HTML>" +
    						"<H3>What tuning can be done to help reduce the problem</H3> \n" +
    						"<UL> \n" +
    						"    <LI>The best single method to reduce data cache spinlock usage is to increae the number of partitions in the data cache.</LI> \n" +
    						"    <LI>Note that if a cache can be set to \"relaxed LRU\" the spinlock usage may be decreased dramatically. <BR> \n" +
    						"        This is because the relaxed LRU cache does not maintain the LRU->MRU chain, and so does not need to grab the spinlock to move pages to the MRU side. \n" +
    						"    </LI> \n" +
    						"    <LI>There are definite requirements for this to help (a cache that has high turnover is a very poor candidate for relaxed LRU).</LI> \n" +
    						"</UL> \n" +
    						"<B>Note</B>:   Its quite possible to reduce contention on one spinlock only to have it increase on another <BR> \n" +
    						"<B>Source</B>: The origin text for above information can be found at: <A HREF=\"http://wiki.scn.sap.com/wiki/display/SYBASE/Spinlocks+and+CPU+usage+in+SAP+ASE\">http://wiki.scn.sap.com/wiki/display/SYBASE/Spinlocks+and+CPU+usage+in+SAP+ASE</A> <BR> \n" +
    						"</HTML>";
				}
				
				// Object Manager Spinlock (Resource->rdesmgr_spin)
				if ("Resource->rdesmgr_spin".equals(spinName))
				{
					return 
    						"<HTML>" +
    						"<H3>What tuning can be done to help reduce the problem</H3> \n" +
    						"<UL> \n" +
    						"    <LI>Make sure that sufficient \"number of open objects\" have been configured.</LI> \n" +
    						"    <LI>Identify <I>hot</I> objects by using monOpenObjectActivity. In AseTune - Performance Counter: Object/Access -&gt; Objects</LI> \n" +
    						"    <LI>Use dbcc tune (des_bind) to bind the hot objects to the DES cache.</LI> \n" +
    						"    <LI>The reason this works is that the spinlock is used to protect the DES keep count in order to make sure an in-use DES does not get scavenged. When the DES is bound that whole process gets skipped.</LI> \n" +
    						"</UL> \n" +
    						"<B>Note</B>:   Its quite possible to reduce contention on one spinlock only to have it increase on another <BR> \n" +
    						"<B>Source</B>: The origin text for above information can be found at: <A HREF=\"http://wiki.scn.sap.com/wiki/display/SYBASE/Spinlocks+and+CPU+usage+in+SAP+ASE\">http://wiki.scn.sap.com/wiki/display/SYBASE/Spinlocks+and+CPU+usage+in+SAP+ASE</A> <BR> \n" +
    						"</HTML>";
				}

				// Procedure Cache Spinlock (Resource->rproccache_spin)
				if ("Resource->rproccache_spin".equals(spinName))
				{
					return
    						"<HTML>" +
    						"<H3>What tuning can be done to help reduce the problem</H3> \n" +
    						"<UL> \n" +
    						"    <LI>This spinlock is used when allocating or freeing pages from the global procedure cache memory pool (this includes statement cache).</LI> \n" +
    						"    <LI>Some possible causes include<BR> \n" +
    						"        &diams; Proc cache too small � procs and statements being frequently removed/replaced.<BR> \n" +
    						"        &diams; Procedure recompilations<BR> \n" +
    						"        &diams; Large scale allocations<BR> \n" +
    						"    </LI> \n" +
    						"    <LI>To reduce pressure on the spinlock</LI> \n" +
    						"    <LI>Eliminate the cause(s) for procedure recompilations (maybe TF 299)</LI> \n" +
    						"    <LI>If you are running a version prior to ASE 15.7 ESD#4 <B>upgrade</B>. <BR>" +
    						"        ASE 15.7 ESD#4 and 4.2 have some fixes to hold the spinlock for less time." +
    						"    </LI> \n" +
    						"    <LI>Trace flags 753 and 757 can help reduce large-scale allocations</LI> \n" +
    						"    <LI>In ASE versions past 15.7 SP100, use the configuration option \"enable large chunk elc\".</LI> \n" +
    						"    <LI>Use dbcc proc_cache(free_unused) as temporary help to reduce spinlock/cpu usage.</LI> \n" +
    						"</UL> \n" +
    						"<B>Note</B>:   Its quite possible to reduce contention on one spinlock only to have it increase on another <BR> \n" +
    						"<B>Source</B>: The origin text for above information can be found at: <A HREF=\"http://wiki.scn.sap.com/wiki/display/SYBASE/Spinlocks+and+CPU+usage+in+SAP+ASE\">http://wiki.scn.sap.com/wiki/display/SYBASE/Spinlocks+and+CPU+usage+in+SAP+ASE</A> <BR> \n" +
    						"</HTML>";
				}

				// Procedure Cache Manager Spinlock (Resource->rprocmgr_spin)
				if ("Resource->rprocmgr_spin".equals(spinName))
				{
					return
							"<HTML>" +
							"<H3>What tuning can be done to help reduce the problem</H3> \n" +
							"<UL> \n" +
							"    <LI>This spinlock is used whenever moving procedures and dynamic SQL into or out of procedure cache.</LI> \n" +
							"    <LI>This spinlock was also used prior to ASE 15.7 ESD#1 when updating the memory accounting structures (pmctrl).<BR> \n" +
							"        &diams; Due to contention a separate spinlock was created.<BR> \n" +
							"    </LI> \n" +
							"    <LI>Causes of high contention include:<BR> \n" +
							"        &diams; Heavy use of dynamic SQL<BR> \n" +
							"        &diams; Procedure cache sized too small<BR> \n" +
							"    </LI> \n" +
							"    <LI>Possible remedies are the same as for rproccache_spin</LI> \n" +
							"</UL> \n" +
    						"<B>Note</B>:   Its quite possible to reduce contention on one spinlock only to have it increase on another <BR> \n" +
    						"<B>Source</B>: The origin text for above information can be found at: <A HREF=\"http://wiki.scn.sap.com/wiki/display/SYBASE/Spinlocks+and+CPU+usage+in+SAP+ASE\">http://wiki.scn.sap.com/wiki/display/SYBASE/Spinlocks+and+CPU+usage+in+SAP+ASE</A> <BR> \n" +
							"</HTML>";
				}

				// Lock Manager spinlocks (fglockspins , addrlockspins, tablockspins)
				if ("fglockspins".equals(spinName) || "addrlockspins".equals(spinName) || "tablockspins".equals(spinName))
				{
					return
    						"<HTML>" +
    						"<H3>What tuning can be done to help reduce the problem</H3> \n" +
    						"<UL> \n" +
    						"    <LI>These spinlocks are used to protect the lock manager hashtables.</LI> \n" +
    						"    <LI>If the lock HWMs are set too high, that means more locks and more contention</LI> \n" +
    						"    <LI>Configuration tunables are the primary way to address this<BR> \n" +
    						"        &diams; lock spinlock ratio<BR> \n" +
    						"        &diams; lock address spinlock ratio<BR> \n" +
    						"        &diams; lock table spinlock ratio<BR> \n" +
    						"        &diams; lock hashtable size<BR> \n" +
    						"    </LI> \n" +
    						"</UL> \n" +
    						"<B>Note</B>:   Its quite possible to reduce contention on one spinlock only to have it increase on another <BR> \n" +
    						"<B>Source</B>: The origin text for above information can be found at: <A HREF=\"http://wiki.scn.sap.com/wiki/display/SYBASE/Spinlocks+and+CPU+usage+in+SAP+ASE\">http://wiki.scn.sap.com/wiki/display/SYBASE/Spinlocks+and+CPU+usage+in+SAP+ASE</A> <BR> \n" +
    						"</HTML>";
				}
			}
		} // end: spinName, description

		return null;
	}

}
