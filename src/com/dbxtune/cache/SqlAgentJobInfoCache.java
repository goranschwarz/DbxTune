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

/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package com.dbxtune.cache;

import java.lang.invoke.MethodHandles;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.LinkedHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.DbUtils;
import com.dbxtune.utils.StringUtil;
import com.dbxtune.utils.TimeUtils;

public class SqlAgentJobInfoCache
{
    /** Log4j logging. */
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	/** Instance variable */
	private static SqlAgentJobInfoCache _instance = null;

	
	public static final String PROPKEY_refreshInterwallInMinutes = "SqlAgentJobInfoCache.refresh.interval.minutes";
	public static final long   DEFAULT_refreshInterwallInMinutes = 60 * 4; // 4 hours

	
//TODO; // Make something of this... 
// and initialize it from CounterControllerSqlServer initCounters() or initializeDbmsProperties() or similar
// Use it in various Cm's that has 'program_name' column to lookup:
// program_name=SQLAgent - TSQL JobStep (Job 0x38AAD6888E5C5E408DE573B0A25EE970 : Step 2)
//                                           ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^        ^
// into "SQLAgent - TSQL JobStep (Job 0x38AAD6888E5C5E408DE573B0A25EE970 [name] : Step 2 [name])
//                                                                        ^^^^            ^^^^
// Possibly remove 0x and replace it with the UUID

	public static boolean hasInstance()
	{
		return (_instance != null);
	}

	public static void setInstance(SqlAgentJobInfoCache instance)
	{
		_instance = instance;
	}

	public static SqlAgentJobInfoCache getInstance()
	{
		if (_instance == null)
			_instance = new SqlAgentJobInfoCache();

//		if (_instance == null)
//			throw new RuntimeException("No SqlAgentJobInfoCache has been set...");

		return _instance;
	}

	public static String resolveProgramNameForJobName(String programName)
	{
		return resolveProgramNameForJobName(programName, 128);
	}
	/**
	 * Typically inject/replace "JobId" and "Step" into "more readable names"
	 * @param programName
	 * @return
	 */
	// "SQLAgent - TSQL JobStep (Job 0x38AAD6888E5C5E408DE573B0A25EE970 : Step 2)"
	public static String resolveProgramNameForJobName(String programName, int maxLen)
	{
		if (_instance == null)
		{
			_logger.info("SqlAgentJobInfoCache is NOT initialized, the 'program_name' will NOT be changed, just returning the original name.");
			return programName;
		}

		if (StringUtil.isNullOrBlank(programName))
			return programName;

		if ( ! programName.startsWith("SQLAgent - TSQL JobStep (Job") )
			return programName;
		
		String jobIdHex  = StringUtil.substringBetweenTwoChars(programName, "JobStep (Job ", " : Step ");
		String stepIdStr = StringUtil.substringBetweenTwoChars(programName, " : Step ", ")");

		int stepId = StringUtil.parseInt(stepIdStr, -1);

		String jobUuid  = null;
		String jobName  = null;
		String stepName = null;

		// get/lookup the 'jobName' and 'stepName'
		AgentJob     agentJob  = _instance._jobs.get(jobIdHex);
		AgentJobStep sysStep = null;
		if (agentJob != null)
		{
			jobUuid = agentJob._job_id;
			jobName = agentJob._name;
			sysStep = agentJob._steps.get(stepId);

			if (sysStep != null)
			{
				stepName = sysStep._step_name;
			}
		}

//System.out.println("------------------------- programName=|"+programName+"|, jobIdHex='"+jobIdHex+"', stepIdStr='"+stepIdStr+"', stepId="+stepId+"... agentJob="+agentJob+", sysStep="+sysStep+"... jobName='"+jobName+"', stepName='"+stepName+"'.");

		String originProgramName = programName;
		
		// Replace "0x......."  --> "[uuid=xxx, name='jobName']"
		// Replace " : Step #)" --> " : Step # [stepName])"
		
		if (jobName != null)
			programName = programName.replace(jobIdHex, "[uuid='" + jobUuid + "', name='" + jobName + "']");
		
		if (stepName != null)
			programName = programName.replace(" : Step " + stepId + ")", " : Step " + stepId + " [name='" + stepName + "'])");

//System.out.println("                ----1---- programName["+programName.length()+"]=|"+programName+"|.");

		// TO LONG... Ok, lets try to make it shorter
		if (programName.length() > maxLen)
		{
			int jobNameLen  = jobName  == null ? 0 : jobName .length();
			int stepNameLen = stepName == null ? 0 : stepName.length();

			int maxJobNameLen  = 40;
			int maxStepNameLen = 40;
			
			int overflow_jobName  = jobNameLen  - maxJobNameLen ; // -10 == 10 chars shorter than max size 
			int overflow_stepName = stepNameLen - maxStepNameLen; //   5 == 5 chars to long
				
			// if "underflow" adjust the max size between the fields
			if (overflow_jobName < 0 || overflow_stepName < 0)
			{
				if (overflow_jobName < 0)
					maxStepNameLen += (overflow_jobName * -1); // val * -1 : translate negative val to positive 

				if (overflow_stepName < 0)
					maxJobNameLen += (overflow_stepName * -1); // val * -1 : translate negative val to positive 
			}
//System.out.println("                  maxJobNameLen["+jobNameLen+"]["+overflow_jobName+"]="+maxJobNameLen+", maxStepNameLen["+stepNameLen+"]["+overflow_stepName+"]="+maxStepNameLen);
			programName = originProgramName;

			// Remove the UUID
			if (jobNameLen > 0)
			{
				jobName = StringUtil.truncate(jobName, maxJobNameLen, true, null);
				programName = programName.replace(jobIdHex, "'" + jobName + "'");
			}
			
			if (stepNameLen > 0)
			{
				stepName = StringUtil.truncate(stepName, maxStepNameLen, true, null);
				programName = programName.replace(" : Step " + stepId + ")", " : Step " + stepId + " '" + stepName + "')");
			}
//System.out.println("                ----2---- programName["+programName.length()+"]=|"+programName+"|.");
		}

		return programName;
	}
	
	
	/**
	 * Reset the Cache, this so we can get new ones later<br>
	 * Most possible called from disconnect() or similar
	 */
	public void reset()
	{
		_jobs = new LinkedHashMap<>();
		_lastRefreshTime = 0;
		_isRefreshEnabled = true;
	}
	
	
	/** Cache to hold all the Jobs and JobSteps */
	private HashMap<String, AgentJob> _jobs = new LinkedHashMap<>();

	/** When did we call refresh the last time */
	private long _lastRefreshTime = 0;

	/** If refresh had errors, we don't want to refresh again */
	private boolean _isRefreshEnabled = true;

	/** Refresh information */
	public void refresh(DbxConnection conn)
	{
		long refreshInterwallInMinutes = Configuration.getCombinedConfiguration().getLongProperty(PROPKEY_refreshInterwallInMinutes, DEFAULT_refreshInterwallInMinutes);
		
		long minutesSinceRefresh = TimeUtils.secondsDiffNow(_lastRefreshTime) / 60;
		if (minutesSinceRefresh < refreshInterwallInMinutes)
		{
			if (_logger.isDebugEnabled())
				_logger.debug("Skipping refresh of 'SQL Agent Jobs', next refresh will be done in " + (refreshInterwallInMinutes - minutesSinceRefresh) + " minutes");
			return;
		}
		
		if ( ! _isRefreshEnabled )
		{
			if (_logger.isDebugEnabled())
				_logger.debug("Skipping refresh of 'SQL Agent Jobs', we have had issues previously, so refresh is DISABLED.");
			return;
		}
		
		_logger.info("Refreshing SQL Agent Jobs...");
		initialize(conn);
	}
	
	/**
	 * Initialize the 
	 * @param conn
	 */
	public void initialize(DbxConnection conn)
	{
		// First check if we can do select on tables: sysjobs, sysjobsteps 
		// if we are authorized to do so...
		
		// Get values from: sysjobs, sysjobsteps
		String sql = ""
			    + "SELECT \n"
			    + "     job_id_hex = convert(varchar(36), cast(j.job_id as varbinary(16)), 1) \n" // -- col: 1
			    + "    ,j.job_id \n"             // -- col: 2
			    + "    ,j.name \n"               // -- col: 3
			    + "    ,j.[description] \n"      // -- col: 4
			    + "    ,j.[date_created] \n"     // -- col: 5
			    + "    ,j.[date_modified] \n"    // -- col: 6
			    + "    ,j.[version_number] \n"   // -- col: 7
			    + "    ,js.[step_id] \n"         // -- col: 8
			    + "    ,js.[step_name] \n"       // -- col: 9
			    + "    ,js.[subsystem] \n"       // -- col: 10
			    + "    ,js.[command] \n"         // -- col: 11
			    + "FROM msdb.dbo.sysjobs j \n"
			    + "INNER JOIN msdb.dbo.sysjobsteps js  ON j.job_id = js.job_id \n"
			    + "ORDER BY j.job_id, js.step_id \n"
			    + "";

		// Set refresh time
		_lastRefreshTime = System.currentTimeMillis();

		// Clear the cache
		_jobs = new LinkedHashMap<>();

		_isRefreshEnabled = true;

		boolean isSelectabe_sysjobs     = DbUtils.checkIfTableIsSelectable(conn, "msdb.dbo.sysjobs");
		boolean isSelectabe_sysjobsteps = DbUtils.checkIfTableIsSelectable(conn, "msdb.dbo.sysjobsteps");
		
		if ( ! isSelectabe_sysjobs || ! isSelectabe_sysjobsteps)
		{
			_isRefreshEnabled = false;

			String srvname  = conn.getDbmsServerNameNoThrow();
			String username = conn.getConnPropOrDefault().getUsername();
			_logger.warn("SQL Agent Jobs: select 'check' was unsuccessfull on table 'msdb.dbo.sysjobs' or 'msdb.dbo.sysjobsteps' at server '" + srvname + "'. Solution: USE msdb; CREATE USER [" + username + "] FOR LOGIN [" + username + "]; ALTER ROLE db_datareader ADD MEMBER [" + username + "];");
			
			return;
		}
		

		// Get records
		try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
		{
			String jobIdHex_save = "";

			int rowCount = 0;
			int jobCount = 0;

			AgentJob agentJob = null;

			while(rs.next())
			{
				rowCount++;

				String jobIdHex = rs.getString(1);
				String jobId    = rs.getString(2);
				
				if ( agentJob == null || ! jobIdHex_save.equals(jobIdHex) )
				{
					jobIdHex_save = jobIdHex;

					jobCount++;
					agentJob = new AgentJob();

					agentJob._job_id_hex     = jobIdHex;
					agentJob._job_id         = jobId;
					agentJob._name           = rs.getString   (3);
					agentJob._description    = rs.getString   (4);
					agentJob._date_created   = rs.getTimestamp(5);
					agentJob._date_modified  = rs.getTimestamp(6);
					agentJob._version_number = rs.getInt      (7);
					
					_jobs.put(jobIdHex, agentJob);
				}

				AgentJobStep agentJobStep = new AgentJobStep();
				agentJobStep._job_id    = jobId;
				agentJobStep._step_id   = rs.getInt   (8);
				agentJobStep._step_name = rs.getString(9);
				agentJobStep._subsystem = rs.getString(10);
				agentJobStep._command   = rs.getString(11);
				
				agentJob.addStep(agentJobStep);
			}
			
			_logger.info("Fetched " + jobCount + " SQL Agent Jobs, with a total of " + rowCount + " Job Steps, which will be cached until refresh!");
//System.out.println(this.toString());
		}
		catch (SQLException ex)
		{
			_isRefreshEnabled = false;
			_logger.warn("Problems getting SQL Agent Job Information, disabling refresh. ErrorCode=" + ex.getErrorCode() + ", SQLState=" + ex.getSQLState() + "Msg=|" + ex.getMessage() + "|, SQL=|" + sql + "|.", ex);
		}
		
		// Build up a HashMap<job_id:asBinaryString, meaning 0x...>
	}

	@Override
	public String toString()
	{
		String str = "\n";
		
		for (AgentJob agentJob : _jobs.values())
		{
			str += "AgentJob: [_job_id_hex=" + agentJob._job_id_hex + ", uuid=" + agentJob._job_id + ", name='" + agentJob._name + "'] \n";

			for (AgentJobStep agentJobStep : agentJob._steps.values())
				str += "    Step: [_step_id=" + agentJobStep._step_id + ", name='" + agentJobStep._step_name + "'] \n";
		}
		return super.toString() + str;
	}
	
	@SuppressWarnings("unused")
	private static class AgentJob
	{
		private String    _job_id_hex;
		private String    _job_id;
		private String    _name;
		private String    _description;
		private Timestamp _date_created;
		private Timestamp _date_modified;
		private int       _version_number;

		private LinkedHashMap<Integer, AgentJobStep> _steps = new LinkedHashMap<>();

		public void addStep(AgentJobStep agentJobStep)
		{
			_steps.put( agentJobStep._step_id, agentJobStep);
//System.out.println("-----ADD[size="+_steps.size()+"]: AgentJob[id='"+_job_id+"', name='"+_name+"'] ::: Step[stepId="+agentJobStep._step_id+", setpName='"+agentJobStep._step_name+"']");
		}
	}
	@SuppressWarnings("unused")
	private static class AgentJobStep
	{
		private String _job_id;
		private int    _step_id;
		private String _step_name;
		private String _subsystem;
		private String _command;
//		private int    _flags;
//		private String _additional_parameters;
//		private int    _cmdexec_success_code;
//		private int    _on_success_action;
//		private int    _on_success_step_id;
//		private int    _on_fail_action;
//		private int    _on_fail_step_id;
//		private String _server;
//		private String _database_name;
//		private String _database_user_name;
//		private int    _retry_attempts;
//		private int    _retry_interval;
//		private int    _os_run_priority;
//		private String _output_file_name;
//		private int    _last_run_outcome;
//		private int    _last_run_duration;
//		private int    _last_run_retries;
//		private int    _last_run_date;
//		private int    _last_run_time;
//		private String _proxy_id;
//		private String _step_uid;
	}
	
//	public static void main(String[] args)
//	{
//		SqlAgentJobInfoCache sqlAgentCache = new SqlAgentJobInfoCache();
//
//		SqlAgentJobInfoCache.setInstance(sqlAgentCache);
//		
//		AgentJob job = new AgentJob();
//		job._name = "Job 1";
//		job._job_id = "-01-";
//		sqlAgentCache._jobs.put("0x0001", job);
//
//		AgentJobStep jobStep = new AgentJobStep();
//		jobStep._step_id = 1;
//		jobStep._step_name = "Step 1";
//
//		job.addStep(jobStep);
//
//		// Job2
//		job = new AgentJob();
//		job._name = "JOB-56789-123456789-123456789-123456789-123456789-";
//		job._job_id = "-02-";
//		sqlAgentCache._jobs.put("0x0002", job);
//
//		jobStep = new AgentJobStep();
//		jobStep._step_id = 1;
//		jobStep._step_name = "STEP-6789-123456789-123456789-123456789-123456789-";
////		jobStep._step_name = "STEP-6789-123456789-123456789-123456";
//		
//		job.addStep(jobStep);
//		
//		
//		System.out.println("NULL=|"      + SqlAgentJobInfoCache.resolveProgramNameForJobName(null) + "|");
//		System.out.println("EMPTY=|"     + SqlAgentJobInfoCache.resolveProgramNameForJobName("") + "|");
//		System.out.println("XXX.1=|"     + SqlAgentJobInfoCache.resolveProgramNameForJobName("SQLAgent - TSQL JobStep (Job 0x0001 : Step 1)") + "|");
//		System.out.println("XXX.2=|"     + SqlAgentJobInfoCache.resolveProgramNameForJobName("SQLAgent - TSQL JobStep (Job 0x0001 : Step 2)") + "|");
//		System.out.println("YYY.1=|"     + SqlAgentJobInfoCache.resolveProgramNameForJobName("SQLAgent - TSQL JobStep (Job 0x0002 : Step 1)") + "|");
//		System.out.println("UNTOUCHED=|" + SqlAgentJobInfoCache.resolveProgramNameForJobName("SQLAgent - TSQL JobStep (Job 0x38AAD6888E5C5E408DE573B0A25EE970 : Step 2)") + "|");
//	}
}
