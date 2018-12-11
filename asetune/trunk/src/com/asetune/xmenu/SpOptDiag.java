package com.asetune.xmenu;

import java.sql.Connection;

import com.asetune.cm.sql.VersionInfo;
import com.asetune.tools.sqlw.QueryWindow;
import com.asetune.utils.AseConnectionUtils;
import com.asetune.utils.Configuration;
import com.asetune.utils.Ver;

public class SpOptDiag
extends SQLWindow
{
	public SpOptDiag() 
	{
		super();
	}

	@Override
	protected Configuration getConfiguration()
	{
		Configuration conf = new Configuration();
		conf.setProperty(QueryWindow.PROPKEY_asPlainText, true);
		return conf;
	}

	@Override
	protected String modifySql(String sql)
	{
		Connection conn = getConnection();
		long srvVersion = AseConnectionUtils.getAseVersionNumber(conn);

		// Should we install Procs
		try
		{
//			if (srvVersion >= 15700)
//			if (srvVersion >= 1570000)
			if (srvVersion >= Ver.ver(15,7))
			{
				// do not install
				// but replace use 'sp_showoptstats' instead of 'sp__optdiag'

				sql = sql.replace("sp__optdiag", "sp_showoptstats");
			}
			else
			{
				// do SP_OPTDIAG, but only on UNPARTITIONED tables
//				sql="declare @partitions int \n" +
//					"select @partitions = count(*) \n" +
//					"from "+entry.getDbname()+"..sysobjects o, "+entry.getDbname()+"..sysusers u, "+entry.getDbname()+"..syspartitions p \n" +
//					"where o.name = '"+entry.getObjectName()+"' \n" +
//					"  and u.name = '"+entry.getOwner()+"' \n" +
//					"  and o.id  = p.id \n" +
//					"  and o.uid = o.uid \n" +
//					"  and p.indid = 0 \n" +
//					"                  \n" +
//					"if (@partitions > 1) \n" +
//					"    print 'Table is partitioned, and this is not working so well with sp__optdiag, sorry.' \n" +
//					"else \n" +
//					"    exec "+entry.getDbname()+"..sp__optdiag '"+entry.getOwner()+"."+entry.getObjectName()+"' \n" +
//					"";

//				if (srvVersion >= 15000)
//					AseConnectionUtils.checkCreateStoredProc(conn, 15000, "sybsystemprocs", "sp__optdiag", VersionInfo.SP__OPTDIAG_CRDATE, VersionInfo.class, "sp__optdiag_v1_15_0.sql", "sa_role");
//				else
//					AseConnectionUtils.checkCreateStoredProc(conn, 12503, "sybsystemprocs", "sp__optdiag", VersionInfo.SP__OPTDIAG_CRDATE, VersionInfo.class, "sp__optdiag_v1_9_4.sql", "sa_role");
//				if (srvVersion >= 1500000)
//					AseConnectionUtils.checkCreateStoredProc(conn, 1500000, "sybsystemprocs", "sp__optdiag", VersionInfo.SP__OPTDIAG_CRDATE, VersionInfo.class, "sp__optdiag_v1_15_0.sql", "sa_role");
//				else
//					AseConnectionUtils.checkCreateStoredProc(conn, 1250300, "sybsystemprocs", "sp__optdiag", VersionInfo.SP__OPTDIAG_CRDATE, VersionInfo.class, "sp__optdiag_v1_9_4.sql", "sa_role");
				if (srvVersion >= Ver.ver(15,0))
					AseConnectionUtils.checkCreateStoredProc(conn, Ver.ver(15,0),     "sybsystemprocs", "sp__optdiag", VersionInfo.SP__OPTDIAG_CRDATE, VersionInfo.class, "sp__optdiag_v1_15_0.sql", "sa_role");
				else
					AseConnectionUtils.checkCreateStoredProc(conn, Ver.ver(12,5,0,3), "sybsystemprocs", "sp__optdiag", VersionInfo.SP__OPTDIAG_CRDATE, VersionInfo.class, "sp__optdiag_v1_9_4.sql", "sa_role");
			}
		}
		catch (Exception e)
		{
			// the checkCreateStoredProc, writes information to error log, so we don't need to do it again.
		}
		
		return sql;
	}
}
