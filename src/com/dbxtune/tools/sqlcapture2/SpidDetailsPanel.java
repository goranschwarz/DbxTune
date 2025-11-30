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
package com.dbxtune.tools.sqlcapture2;

import javax.swing.JPanel;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import com.dbxtune.cm.CountersModel;
import com.dbxtune.gui.MainFrame;

public class SpidDetailsPanel
extends JPanel
implements TableModelListener
{
	private static final long serialVersionUID = 1L;

	private CountersModel _cm = null; 
	public SpidDetailsPanel(CountersModel cm)
	{
		_cm = cm;
	}

	public SpidDetailsPanel()
	{
	}

	// implementing: TableModelListener
	@Override
	@SuppressWarnings("unused")
	public void tableChanged(TableModelEvent e)
	{
//		TableModel tm = (TableModel) e.getSource();
		Object source = e.getSource();
		int column    = e.getColumn();
		int firstRow  = e.getFirstRow();
		int lastRow   = e.getLastRow();
		int type      = e.getType();
//		System.out.println("=========TableModelEvent: type="+type+", column="+column+", firstRow="+firstRow+", lastRow="+lastRow);
//		System.out.println("=========TableModelEvent: sourceClass='"+source.getClass().getName()+"', source='"+source+"'.");

		// event: AbstactTableModel.fireTableStructureChanged
		if (column == -1 && firstRow == -1 && lastRow == -1)
		{
		}

		// Do not update values if we are viewing in-memory storage
		if (MainFrame.isInMemoryViewOn())
			return;

//		CountersModel cm = GetCounters.getInstance().getCmByName(CmSummary.CM_NAME);
//		if (cm != null && cm.hasAbsData() )
//			setSummaryData(cm);
		setData(_cm, false);
	}

	private void setData(CountersModel cm, boolean postProcessing)
	{
//		_atAtServerName_txt    .setText(cm.getAbsString (0, "atAtServerName"));
		
	}

}
// RS> Col# Label            JDBC Type Name           Guessed DBMS type Source Table           
// RS> ---- ---------------- ------------------------ ----------------- -----------------------
// RS> 1    spid             java.sql.Types.SMALLINT  smallint          master.dbo.sysprocesses
// RS> 2    enginenum        java.sql.Types.INTEGER   int               master.dbo.sysprocesses
// RS> 3    status           java.sql.Types.CHAR      char(12)          master.dbo.sysprocesses
// RS> 4    suid             java.sql.Types.INTEGER   int               master.dbo.sysprocesses
// RS> 5    suserName        java.sql.Types.VARCHAR   varchar(30)       -none-                 
// RS> 6    hostname         java.sql.Types.VARCHAR   varchar(30)       master.dbo.sysprocesses
// RS> 7    hostprocess      java.sql.Types.VARCHAR   varchar(30)       master.dbo.sysprocesses
// RS> 8    cmd              java.sql.Types.VARCHAR   varchar(30)       master.dbo.sysprocesses
// RS> 9    cpu              java.sql.Types.INTEGER   int               master.dbo.sysprocesses
// RS> 10   physical_io      java.sql.Types.INTEGER   int               master.dbo.sysprocesses
// RS> 11   memusage         java.sql.Types.INTEGER   int               master.dbo.sysprocesses
// RS> 12   LocksHeld        java.sql.Types.INTEGER   int               monProcessActivity     
// RS> 13   blocked          java.sql.Types.SMALLINT  smallint          master.dbo.sysprocesses
// RS> 14   dbid             java.sql.Types.SMALLINT  smallint          master.dbo.sysprocesses
// RS> 15   dbname           java.sql.Types.VARCHAR   varchar(30)       -none-                 
// RS> 16   uid              java.sql.Types.INTEGER   int               master.dbo.sysprocesses
// RS> 17   userName         java.sql.Types.VARCHAR   varchar(1)        -none-                 
// RS> 18   gid              java.sql.Types.INTEGER   int               master.dbo.sysprocesses
// RS> 19   tran_name        java.sql.Types.VARCHAR   varchar(64)       master.dbo.sysprocesses
// RS> 20   time_blocked     java.sql.Types.INTEGER   int               master.dbo.sysprocesses
// RS> 21   network_pktsz    java.sql.Types.INTEGER   int               master.dbo.sysprocesses
// RS> 22   fid              java.sql.Types.SMALLINT  smallint          master.dbo.sysprocesses
// RS> 23   execlass         java.sql.Types.VARCHAR   varchar(30)       master.dbo.sysprocesses
// RS> 24   priority         java.sql.Types.VARCHAR   varchar(10)       master.dbo.sysprocesses
// RS> 25   affinity         java.sql.Types.VARCHAR   varchar(30)       master.dbo.sysprocesses
// RS> 26   id               java.sql.Types.INTEGER   int               master.dbo.sysprocesses
// RS> 27   procname         java.sql.Types.VARCHAR   varchar(255)      -none-                 
// RS> 28   stmtnum          java.sql.Types.INTEGER   int               master.dbo.sysprocesses
// RS> 29   linenum          java.sql.Types.INTEGER   int               master.dbo.sysprocesses
// RS> 30   origsuid         java.sql.Types.INTEGER   int               master.dbo.sysprocesses
// RS> 31   block_xloid      java.sql.Types.INTEGER   int               master.dbo.sysprocesses
// RS> 32   clientname       java.sql.Types.VARCHAR   varchar(30)       master.dbo.sysprocesses
// RS> 33   clienthostname   java.sql.Types.VARCHAR   varchar(30)       master.dbo.sysprocesses
// RS> 34   clientapplname   java.sql.Types.VARCHAR   varchar(30)       master.dbo.sysprocesses
// RS> 35   sys_id           java.sql.Types.SMALLINT  smallint          master.dbo.sysprocesses
// RS> 36   ses_id           java.sql.Types.INTEGER   int               master.dbo.sysprocesses
// RS> 37   loggedindatetime java.sql.Types.TIMESTAMP datetime          master.dbo.sysprocesses
// RS> 38   ipaddr           java.sql.Types.VARCHAR   varchar(64)       master.dbo.sysprocesses
// RS> 39   program_name     java.sql.Types.VARCHAR   varchar(30)       -none-                 
// RS> 40   CPUTime          java.sql.Types.INTEGER   int               monProcessActivity     
// RS> 41   WaitTime         java.sql.Types.INTEGER   int               monProcessActivity     
// RS> 42   LogicalReads     java.sql.Types.INTEGER   int               monProcessActivity     
// RS> 43   PhysicalReads    java.sql.Types.INTEGER   int               monProcessActivity     
// RS> 44   PagesRead        java.sql.Types.INTEGER   int               monProcessActivity     
// RS> 45   PhysicalWrites   java.sql.Types.INTEGER   int               monProcessActivity     
// RS> 46   PagesWritten     java.sql.Types.INTEGER   int               monProcessActivity     
// RS> 47   MemUsageKB       java.sql.Types.INTEGER   int               monProcessActivity     
// RS> 48   ScanPgs          java.sql.Types.INTEGER   int               monProcessActivity     
// RS> 49   IdxPgs           java.sql.Types.INTEGER   int               monProcessActivity     
// RS> 50   TmpTbl           java.sql.Types.INTEGER   int               monProcessActivity     
// RS> 51   UlcBytWrite      java.sql.Types.INTEGER   int               monProcessActivity     
// RS> 52   UlcFlush         java.sql.Types.INTEGER   int               monProcessActivity     
// RS> 53   ULCFlushFull     java.sql.Types.INTEGER   int               monProcessActivity     
// RS> 54   Transactions     java.sql.Types.INTEGER   int               monProcessActivity     
// RS> 55   Commits          java.sql.Types.INTEGER   int               monProcessActivity     
// RS> 56   Rollbacks        java.sql.Types.INTEGER   int               monProcessActivity     
// RS> 57   PacketsSent      java.sql.Types.INTEGER   int               monProcessNetIO        
// RS> 58   PacketsReceived  java.sql.Types.INTEGER   int               monProcessNetIO        
// RS> 59   BytesSent        java.sql.Types.INTEGER   int               monProcessNetIO        
// RS> 60   BytesReceived    java.sql.Types.INTEGER   int               monProcessNetIO        
