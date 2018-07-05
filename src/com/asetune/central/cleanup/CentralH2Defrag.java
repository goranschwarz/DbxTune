package com.asetune.central.cleanup;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import com.asetune.central.pcs.CentralPcsWriterHandler;
import com.asetune.central.pcs.CentralPersistWriterJdbc;
import com.asetune.central.pcs.ICentralPersistWriter;
import com.asetune.sql.conn.ConnectionProp;
import com.asetune.utils.ShutdownHandler;

import it.sauronsoftware.cron4j.Task;
import it.sauronsoftware.cron4j.TaskExecutionContext;

public class CentralH2Defrag
extends Task
{
	private static Logger _logger = Logger.getLogger(CentralH2Defrag.class);

	public static final String  PROPKEY_start = "CentralH2Defrag.start";
	public static final boolean DEFAULT_start = true;

	public static final String PROPKEY_cron = "CentralH2Defrag.cron";
	public static final String DEFAULT_cron = "30 02 1 * *"; // 02:30 first day of the month
//	public static final String DEFAULT_cron = "59 * * * *"; // testing set this to nearest minute

	public static final String  PROPKEY_LOG_FILE_PATTERN = "CentralH2Defrag.log.file.pattern";
	public static final String  DEFAULT_LOG_FILE_PATTERN    = "%d - %-5p - %m%n";
	
//	public static final String  PROPKEY_dryRun = "CentralH2Defrag.dryRun";
//	public static final boolean DEFAULT_dryRun = false;
//	public static final boolean DEFAULT_dryRun = true; // For test purposes

	public static final String EXTRA_LOG_NAME = CentralH2Defrag.class.getSimpleName() + "-TaskLogger";

//	private static final String _prefix = "H2-SHUTDOWN-DEFRAG: ";

//	private boolean _dryRun = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_dryRun, DEFAULT_dryRun);

	@Override
	public void execute(TaskExecutionContext context) throws RuntimeException
	{
		_logger.info("");
		_logger.info("#############################################################################################");
		_logger.info("Begin task: H2 Shutdown Defrag");

		if ( ! CentralPcsWriterHandler.hasInstance() )
		{
			_logger.info("Skipping cleanup, no CentralPcsWriterHandler was found.");
			return;
		}

		ConnectionProp connProps = null;
		CentralPcsWriterHandler centralPcsHandler = CentralPcsWriterHandler.getInstance();
		for (ICentralPersistWriter w : centralPcsHandler.getWriters())
		{
			if (w instanceof CentralPersistWriterJdbc)
			{
				connProps = ((CentralPersistWriterJdbc) w).getStorageConnectionProps();
				break;
			}
		}
		if ( connProps == null )
		{
			_logger.info("Skipping H2 Shutdown Defrag, no CentralPersistWriterJdbc Connection Properties Object was found.");
			return;
		}
		
		String url = connProps.getUrl();
		if ( url != null && ! url.startsWith("jdbc:h2:") )
		{
			_logger.info("Skipping H2 Shutdown Defrag, the database is not H2, url must tart with 'jdbc:h2:'. Current URL='"+url+"'.");
			return;
		}
		
		
//		DbxConnection conn = null;
//		try
//		{
//			_logger.info("Open DBMS connection to CentralPersistWriterJdbc. connProps="+connProps);
//			conn = DbxConnection.connect(null, connProps);
//		}
//		catch (Exception e)
//		{
//			_logger.error("Skipping H2 Shutdown Defrag. Problems connecting to CentralPersistWriterJdbc.", e);
//			return;
//		}

		
//		if (conn != null)
//		{
			try
			{
				_logger.info("Executing H2 Shutdown Defrag for CentralPersistWriterJdbc, which will also restart Dbx Central");

				
				// The file will be created, the shutdown will look for the file, delete it, and do shutdown defrag...
				_logger.info("H2 Shutdown Defrag: creating file '"+CentralPersistWriterJdbc.H2_SHUTDOWN_WITH_DEFRAG_FILENAME+"'.");
				FileUtils.write(new File(CentralPersistWriterJdbc.H2_SHUTDOWN_WITH_DEFRAG_FILENAME), "this will do: H2 SHUTDOWN DEFRAG");

				boolean doRestart = true;
				String reason = "Restart (with DEFRAG) Requested from "+CentralH2Defrag.class.getSimpleName()+".";
				ShutdownHandler.shutdown(reason, doRestart, null);
			}
			catch (Exception e)
			{
				_logger.error("Problems when executing H2 Shutdown Defrag in CentralPersistWriterJdbc", e);
			}
//		}

		
//		if (conn != null)
//		{
//			_logger.info("Closing DBMS connection to CentralPersistWriterJdbc");
//			conn.closeNoThrow();
//		}
		
		
		_logger.info("End task: H2 Shutdown Defrag");
	}
}
