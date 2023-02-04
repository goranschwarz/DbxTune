/*******************************************************************************
 * Copyright (C) 2010-2020 Goran Schwarz
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
package com.asetune.hostmon;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Map;

import javax.swing.Timer;

import org.apache.log4j.Logger;

import com.asetune.hostmon.HostMonitorConnection.ExecutionWrapper;
import com.asetune.ssh.SshConnection;
import com.asetune.utils.Configuration;
import com.asetune.utils.StringUtil;

/**
 * FIXME: describe me
 * 
 * @author gorans
 *
 */
public abstract class HostMonitor
implements Runnable
{
	private static Logger _logger = Logger.getLogger(HostMonitor.class);
	
	public static final String PROPERTY_NAME = "HostMonitor";
	
//	public static final String  PROPKEY_forcePty = "HostMonitor.ssh.requestPty.force";
	

	/**The Operating System command that will be used to get valid information*/
//	private String _command = null;

	/** The thread used to execute this Host Monitor */
	private Thread _thread  = null;

	/** shows that the thread is still running, also used by shutdown() to try to stop the thread */
	private boolean _running = false;

	/** indicates that someone has paused the thread, and it should not start up again */
	private boolean _paused = false;

	/** a SSH Connection to the host */
//	private SshConnection _conn = null;
	private HostMonitorConnection _hostMonConn = null;

	/** meta data of what should be parsed and what should be delivered to the "client" */
	private HostMonitorMetaData _metaData = null;

	/** This is the object we add OsTable objects to when we call closeCurrentSample() */
	private OsTableSampleHolder _sampleHolder  = null;

	/** The object we are using to add records to current sample */
	private OsTable             _currentSample = null;
	
	private Timer _closeSampleTimeout = null;
	private int   _closeSampleTimeoutDelay = 500;

	/** true until, closeCurrentSample() is called the firts time */
	private boolean _firstTimeSample = true;
	
	/** Set this when we have some kind off communication exception, so the "client" can check if we have problems */
	private ArrayList<Exception> _exceptionList = new ArrayList<Exception>();

	/** What version is the OS utility of, that is if the OS has different versions of the utility that has different number of columns etc */
	private int _utilVersion = -1;

	/** What "extra information" does the OS utility has... */
	private String        _utilExtraInfoStr  = "";
	private Configuration _utilExtraInfoConf = Configuration.emptyConfiguration();

	private OsVendor _osVendor   = OsVendor.NotSet;

	static
	{
//		_logger.setLevel(Level.DEBUG);
	}

	public enum OsVendor
	{
		NotSet, Linux, Solaris, Aix, Hp, Veritas, Windows, Unknown //, Windows
	};
	/**
	 * Implicitly called by any that extends this class
	 */
	public HostMonitor(int utilVersion, String utilExtraInfo)
	{
		setUtilVersion(utilVersion);
		setUtilExtraInfoStr(utilExtraInfo);
		
		setMetaData( createMetaData(getUtilVersion(), getUtilExtraInfoConf()) );
		
		_sampleHolder = new OsTableSampleHolder(getMetaData());
		_currentSample = new OsTable(getMetaData());
	}

//	/** Set the underlying SSH Connection */
//	public void setConnection(SshConnection conn)
//	{
//		_conn = conn;
//	}
//	/** Get the underlying SSH Connection */
//	public SshConnection getConnection()
//	{
//		return _conn;
//	}
//
//	/** Get the hostname of the underlying SSH Connection, if not connected it returns null */
//	public String getHostname()
//	{
//		if (_conn != null)
//			return _conn.getHost();
//	
//		return null;
//	}
	/** Set the underlying SSH Connection */
	public void setConnection(HostMonitorConnection conn)
	{
		_hostMonConn = conn;
	}
	/** Get the underlying SSH Connection */
	public HostMonitorConnection getConnection()
	{
		return _hostMonConn;
	}

	/** Get the hostname of the underlying SSH Connection, if not connected it returns null */
	public String getHostname()
	{
		if (_hostMonConn != null)
			return _hostMonConn.getHostname();
	
		return null;
	}

	public void setUtilVersion(int utilVersion)
	{
		_utilVersion = utilVersion;
	}
	
	public int getUtilVersion()
	{
		return _utilVersion;
	}
	
	public void setUtilExtraInfoStr(String utilExtraInfoStr)
	{
		if (_logger.isDebugEnabled())
			_logger.debug("setUtilExtraInfoStr(): utilExtraInfoStr=|"+utilExtraInfoStr+"|.");

		if (StringUtil.isNullOrBlank(utilExtraInfoStr))
			return;
		
		_utilExtraInfoStr  = utilExtraInfoStr;
		setUtilExtraInfoConf(null);

		try
		{
			Configuration conf = new Configuration();
			InputStream stream = new ByteArrayInputStream(utilExtraInfoStr.getBytes(StandardCharsets.UTF_8));
			conf.load(stream);
			
			setUtilExtraInfoConf(conf);
		}
		catch (IOException ex) 
		{
			_logger.info("Problems converting the 'utilExtraInfoStr' String to a property. utilExtraInfoStr=|"+utilExtraInfoStr+"|. Continuing... Configuration object will just be empty." );
		}
	}
	public void setUtilExtraInfoConf(Configuration utilExtraInfo)
	{
		_utilExtraInfoConf = utilExtraInfo;
	}

	public String getUtilExtraInfoStr()
	{
		return _utilExtraInfoStr;
	}

	public Configuration getUtilExtraInfoConf()
	{
		if (_utilExtraInfoConf == null)
			return Configuration.EMPTY_CONFIGURATION;
		return _utilExtraInfoConf;
	}

	/** is this monitor connected to the passed vendor ? */
	public boolean isConnectedToVendor(OsVendor vendor)
	{
		return _osVendor.equals(vendor);
	}

	/** Set the vendor we are connected to */
	public void setConnectedToVendor(OsVendor vendor)
	{
		_osVendor = vendor;
	}
	/** get the vendor we are connected to */
	public OsVendor getConnectedToVendor()
	{
		return _osVendor;
	}

	/**
	 * A sample is considered to be closed/finished after X ms<br>
	 * This simply means when to more records has been received for X ms, 
	 * then consider it to be a complete sample.
	 * <p>
	 * For example iostat and vmstat does not say "end of sample" or anything 
	 * else that can help you to decide if we have reached the end of this sample interval.
	 * And if it delivers a new sample after 1 second, should it be treated as a new row/record 
	 * for the sample, or should we consider it to me a complete sample?
	 * <p>
	 * Default value is 500ms of inactivity on the "stream"
	 * @param delayInMs The time to wait
	 */
	public void setCloseSampleTimeoutDelay(int delayInMs)
	{
		_closeSampleTimeoutDelay = delayInMs;
	}
	/** Get the "stream" inactivity timeout, see setCloseSampleTimeoutDelay() for more info */
	public int getCloseSampleTimeoutDelay()
	{
		return _closeSampleTimeoutDelay;
	}

	/**
	 * Get last Exception that was added to this class by any implementors
	 * @return null if no exceptions has been added
	 */
	public Exception getException()
	{
		if (_exceptionList.isEmpty())
			return null;
		Exception ex = _exceptionList.get(0);
		_exceptionList.clear();
		return ex;
	}

	/** Called by any implementors to add a Exception that happend while executing OS Command */
	public void addException(Exception ex)
	{
		_exceptionList.add(ex);
	}

	/** 
	 * Get the OsTableSampleHolder object where all sampled OsTables are held<br>
	 * I don't know when to use this, because the normal way would be to just call 
	 * getSummaryTable(), which does all the things you want... 
	 */
	public OsTableSampleHolder getSampleHolder()
	{
		return _sampleHolder;
	}

	/**
	 * Get the OsTable we are currently adding information to.
	 * @return
	 */
	public OsTable getCurrentSample()
	{
		return _currentSample;
	}

	/**
	 * Get a average or summary calculated OsTable of all the sampled OsTable's we 
	 * have sampled since you last called this method.
	 * <p>
	 * If the underlying command has not produced any records (no samples has been added),
	 * this method will return a OsTable object with no records in it. But it holds a valid 
	 * column specification, which can be used by any JTable or source to simply present a "empty" table.
	 * 
	 * @return OsTable
	 */
	public OsTable getSummaryTable()
	{
		OsTable summary = getSampleHolder().getTableSummary();
		return summary;
	}

	/** 
	 * Create a new HostMonitorMetaData structure, this structure is a description 
	 * of what columns to parse, what columns to deliver to the "client".
	 * <p>
	 * Here is an example of how to do this:
	 * <pre>
	 * public HostMonitorMetaData createMetaData(int utilVersion)
	 * {
	 * 	HostMonitorMetaData md = new HostMonitorMetaData();
	 * 
	 * 	// Device:         rrqm/s   wrqm/s     r/s     w/s   rsec/s   wsec/s avgrq-sz avgqu-sz   await  svctm  %util
	 * 	// sda               0.02     1.49    0.16    0.95     3.27    19.52    20.57     0.04   36.29   2.10   0.23
	 * 
	 * 	md.addStrColumn( "device",        1,  1, false,   30, "Disk device name");
	 * 	md.addIntColumn( "samples",       2,  0, true,        "Number of 'sub' sample entries of iostat this value is based on");
	 * 
	 * 	md.addStatColumn("rrqmPerSec",    3,  2, true, 5,  1, "The number of read requests merged per second that were queued to the device");
	 * 	md.addStatColumn("wrqmPerSec",    4,  3, true, 5,  1, "The number of write requests merged per second that were queued to the device.");
	 * 	md.addStatColumn("readsPerSec",   5,  4, true, 10, 1, "The number of read requests that were issued to the device per second.");
	 * 	md.addStatColumn("writesPerSec",  6,  5, true, 10, 1, "The number of write requests that were issued to the device per second.");
	 * 	md.addStatColumn("kbReadPerSec",  7,  6, true, 10, 1, "The number of kilobytes read from the device per second.");
	 * 	md.addStatColumn("kbWritePerSec", 8,  7, true, 10, 1, "The number of kilobytes writ to the device per second.");
	 * 
	 * 	md.addStatColumn("avgrq-sz",      9,  8, true, 5,  1, "The average size (in  sectors) of the requests that were issued to the device.");
	 * 	md.addStatColumn("avgqu-sz",     10,  9, true, 5,  1, "The average queue length of the requests that were issued to the device.");
	 * 
	 * 	md.addStatColumn("await",        11, 10, true, 5,  1, "The average time (in milliseconds) for I/O requests issued to the device to be served. This includes the time spent by the requests in queue and the time spent servicing them.");
	 * 	md.addStatColumn("svctm",        12, 11, true, 5,  1, "The average service time (in milliseconds) for I/O requests that were issued to the device.");
	 * 	md.addStatColumn("utilPct",      13, 12, true, 4,  1, "Percentage of CPU time during which I/O requests were issued to the device (bandwidth utilization for the device). Device saturation occurs when this value is close to 100%.");
	 * 
	 * 	// Set PrimaryKey columns
	 * 	md.setPkCol("device");
	 * 
	 * 	// Special option for the 'sample' column
	 * 	md.setStatusCol("samples",    HostMonitorMetaData.STATUS_COL_SUB_SAMPLE);
	 * 
	 * 	// Set Percent columns
	 * 	md.setPercentCol("utilPct");
	 * 
	 * 	md.setParseRegexp(HostMonitorMetaData.REGEXP_IS_SPACE);
	 * 
	 * 	return md;
	 * }
	 * </pre>
	 * Override this method by any subclass that implements a HostMonitor 
	 * @param utilExtraInfoConf 
	 * @return
	 */
	abstract public HostMonitorMetaData createMetaData(int utilVersion, Configuration utilExtraInfoConf);

	public HostMonitorMetaData createMetaData()
	{
		return createMetaData(getUtilVersion(), getUtilExtraInfoConf());		
	}

	/** Get what the SQL and Parse columns looks like */
	public HostMonitorMetaData getMetaData()
	{
		return _metaData;
	}
	/** Set what the SQL and Parse columns looks like */
	public void setMetaData(HostMonitorMetaData metaData)
	{
		_metaData = metaData;
		if (_sampleHolder  != null) _sampleHolder .setMetaData(_metaData);
		if (_currentSample != null) _currentSample.setMetaData(_metaData);
	}

	
	/**
	 * Is this Operating System Command a streaming command.
	 * <p>
	 * Commands like 'iostat 2', 'vmstat 1' and 'mpstat 2' is continuing to deliver 
	 * rows until it's terminated, then it's a streaming command.<br>
	 * For streaming commands a background thread is started that reads the never ending streaming 
	 * rows from the command.
	 * <p>
	 * For Commands like 'ls -l' or 'cat filename' is a non-streaming commands and is 
	 * executed every time the client/caller is requesting data. 
	 * @return true for streaming commands, false for non-streaming commands
	 */
	public boolean isOsCommandStreaming()
	{
		return _metaData.isOsCommandStreaming();
	}

	/**
	 * This will be true untill first time closeCurrentSample() is called.
	 * @return
	 */
	public boolean isFirstTimeSample()
	{
		return _firstTimeSample;
	}

	/**
	 * Call this when no more rows should be put into the current OsTable object<br>
	 * The current OsTable will be pushed onto the OsTableSampleHolder queue, waiting 
	 * to make any average or summary calculation.
	 * <p>
	 * The default is that this method is called after 500ms of "inactivity" on the input stream.<br>
	 * If you want to control this yourself, setCloseSampleTimeoutDelay(0), 
	 * which disables this functionality. Then call this method whenever you think it's suitable.
	 */
	public void closeCurrentSample()
	{
		int rows = _currentSample.getRowCount();
		if (rows > 0)
		{
			_firstTimeSample = false;
			_logger.trace("Closing current sample, that has '"+rows+"' rows.");

			_sampleHolder.add(_currentSample);
			_currentSample = new OsTable(getMetaData());
		}
		else
		{
			_logger.debug("SKIPPING: Closing current sample, it had '"+rows+"' rows.");
		}
	}

	/**
	 * This method is called from parseRow() to check if the row should be skipped/discarded<br>
	 * This is basically a FILTER OUT check on specific rows/column values
	 * <p>
	 * For example it can be used to filter out "column header" values<br>
	 * or SKIP specific device names on IO reports.
	 * <p>
	 * This will be used if you don't know exactly what to look for, but at least 
	 * you know that devices (or other data sets) that you want to discard.  
	 * 
	 * @param md
	 * @param row
	 * @param preParsed
	 * @param type
	 * @return TRUE if the row should be discarded, FALSE if we should KEEP the row
	 */
	public boolean skipRow(HostMonitorMetaData md, String row, String[] preParsed, int type)
	{
		// If skip entries, then search for records to SKIP
		Map<String, Integer> skipRows = md.getSkipRowsMap();

		// If no skip entries is installed, then we will allow the row.
		if (skipRows == null)
			return false;

		// Search the skip entries
		for (Map.Entry<String,Integer> entry : skipRows.entrySet()) 
		{
			String skipVal = entry.getKey();
			int    skipPos = entry.getValue();

			// If the "skip value" matches the array position of the "column"
			// return true to SKIP the record
			if (preParsed[skipPos].matches(skipVal))
				return true;
		}
		// Nothing in the skipMap matched the record... so ALLOW the row
		return false;
	}

	/**
	 * This method is called from parseRow() to check if the row should be ALLOWED<br>
	 * This is basically a FILTER IN check on specific rows/column values
	 * <p>
	 * If NO allow entries are installed to the MetaData, then ALL records will be visible.
	 * <p>
	 * For example it can be used to allow only specific device names on IO reports, 
	 * all other devices should not be visible. 
	 * <p>
	 * This will be used if you KNOW what you WANT
	 * 
	 * @param md
	 * @param row
	 * @param preParsed
	 * @param type
	 * @return TRUE if the row should KEEP the row, FALSE if it should be discarded
	 */
	public boolean allowRow(HostMonitorMetaData md, String row, String[] preParsed, int type)
	{
		// If allow entries, then search for records to ALLOW
		Map<String, Integer> allowRows = md.getAllowRowsMap();

		// If no allow entries is installed, then we will allow the row.
		if (allowRows == null)
			return true;

		// Search the allow entries
		for (Map.Entry<String,Integer> entry : allowRows.entrySet()) 
		{
			String allowVal = entry.getKey();
			int    allowPos = entry.getValue();

			// If the "allow value" matches the array position of the "column"
			// return array... to allow values
			if (preParsed[allowPos].matches(allowVal))
				return true;
		}
		// Nothing in the allowMap matched the record... so it needs to be SKIPPED
		return false;
	}

	/**
	 * This method is called from parseAndApply(), in here you can accept, reject or rearrange rows.
	 * <p>
	 * The default implementation just checks that the pre parsed String[] is of the correct size.
	 * <code>if (preParsed.length != md.getParseColumnCount()) return null;</code>
	 * <p>
	 * If there is a header line above the values, for example iostat or vmstat has a "header" 
	 * with the correct number of records, you need to override this method and also skip the header<br>
	 * Here is a example of that:
	 * <pre>
	 * public String[] parseRow(HostMonitorMetaData md, String row, String[] preParsed, int type)
	 * {
	 *     if ( preParsed.length == md.getParseColumnCount() )
	 *     {
	 *         if (preParsed[0].matches("device")) 
	 *             return null;
	 *         return preParsed;
	 *     }
	 *     return null;
	 * }
	 * </pre>
	 * If you want even greater control, please override parseAndApply()
	 * 
	 * @param md Meta Data information
	 * @param row The original String wich was read from the OS Command
	 * @param preParsed a pre-parsed String[], the parse/split was made based on HostMonitorMetaData.getParseRegexp()
	 * @param type SDTOUT | STDERR
	 * @return String[] of the values in the order which is described by the HostMonitorMetaData, if this row should be SKIPPED, simply return a null value
	 */
	public String[] parseRow(HostMonitorMetaData md, String row, String[] preParsed, int type)
	{
		// value count does NOT match parse count
		// return null to SKIP the record
		if (preParsed.length != md.getParseColumnCount())
			return null;

		// check if the row should be SKIPPED
		// If there are "column header" values that should be discarded
		// OR we can FILTER OUT specific rows/column values
		// (for example device names) that we DO NOT want to be visible
		if (skipRow(md, row, preParsed, type))
			return null;

		// now check the ALLOWED rows
		// This means we FILTER IN for specific rows/column values 
		// (for example device names) that we WANT to be visible
		// If NO allow entries are installed, then ALL records will be visible.
		if ( ! allowRow(md, row, preParsed, type))
			return null;

		// OK, nothing to discard, lets ALLOW the row
		return preParsed;
	}

	/**
	 * Used when splitting one input row like a CSV format into that contains several instances into one row for every instance.
	 * 
	 * @param md Meta Data information
	 * @param row The original String wich was read from the OS Command
	 * @param preParsed a pre-parsed String[], the parse/split was made based on HostMonitorMetaData.getParseRegexp()
	 * @param type SDTOUT | STDERR
	 * @return String[][] of the values in the order which is described by the HostMonitorMetaData, if this row should be SKIPPED, simply return a null value
	 */
	public String[][] parseRowOneToMany(HostMonitorMetaData md, String row, String[] preParsed, int type)
	{
		return md.parseRowOneToMany(md, row, preParsed, type);
	}
	
	/**
	 * If we want to change anything in the row before it gets parsed/splitter...
	 * For instance 'mpstat' sometimes has. HH:MM:SS [PM/AM] CPU, if the AM/PM IS missing we will get faulty numbers of columns...
	 * So: the solution might be to "strip off" AM/PM from the input, or similar 
	 * 
	 * @param md
	 * @param row
	 * @param type
	 * @return
	 */
	public String adjustRow(HostMonitorMetaData md, String row, int type)
	{
		if ( ! md.hasSourceRowAdjuster() )
			return row;
		
		SourceRowAdjuster srcRowAdjuster = md.getSourceRowAdjuster();
		return srcRowAdjuster.adjustRow(row, type);
	}

	/**
	 * Parse the input row from the OS Command and add it to the "sample" table, which 
	 * can be fetched by a user at a later stage.
	 * <p>
	 * Workflow of this method is:
	 * <ul>
	 *   <li> Split the input record using a regexp, from HostMonitorMetaData.getParseRegexp() </li>  
	 *   <li> call parseRow(), which ACCEPTS, REJECTS or rearranges entries from the OS Command </li>
	 *   <li> Create a <code>OsTableRow</code> Object, with all the desired columns. </li>
	 *   <li> Add the <code>OsTableRow</code> Object, to the "sample" table. </li>
	 *   <li> start/restart a delay until the closeCurrentSample() method is called. </li>
	 * </ul>
	 * You will have to do approximately the same thing if you override this method.
	 * 
	 * @param md MetaData about which columns etc
	 * @param row A row from the OS Command.
	 * @param type SDTOUT | STDERR
	 */
	public void parseAndApply(HostMonitorMetaData md, String row, int type)
	{
		if (row == null)
			return;
		
//System.out.println(">>parseAndApply(): type=" + type + ", row=|" + row + "|.");

		// do "dynamic" initialization (reading first row), for example if we parse a CSV and the "dynamic" column names is in header
		if (type == SshConnection.STDOUT_DATA && md.isInitializeUsingFirstRowEnabled() && !md.isFirstRowInitDone())
		{
			md.doInitializeUsingFirstRow(row);
			return;
		}

		// some inputs need to "adjust" the input row, for example if numbers are localized with the "wrong" decimal separator: ',' instead of '.'
		String trimedRow = row.trim();
		trimedRow = adjustRow(md, trimedRow, type);

		// Make a simple pre-parse, which splits the input string into a String[] 
		String[] isa = trimedRow.split(md.getParseRegexp()); // Internal String Array

		// UnPivot mode: oneInputRow produces manyTableRows 
		if (md.isUnPivot())
		{
			// Parse: Accept or Re-arrange columns
			String[][] rowsArr = parseRowOneToMany(md, row, isa, type);
			if (rowsArr != null)
			{
				for (int r=0; r<rowsArr.length; r++)
				{
					String[] strArr = rowsArr[r];
					if ( strArr != null )
					{
						if (_logger.isDebugEnabled())
							_logger.debug("++ALLOW++: this row has '"+strArr.length+"' entries parseCount='"+md.getParseColumnCount()+"'. Row '"+row+"'. The Input/PreParsed String Array, size("+isa.length+")=["+StringUtil.toCommaStr(isa)+"], The parseRow() returned String Array, size("+strArr.length+")=["+StringUtil.toCommaStr(strArr)+"].");

						// Make a OsTableRow object of the input.
						// and ADD it to the "sample" table
						try
						{
							OsTableRow entry = new OsTableRow(md, strArr);
							_currentSample.addRow(entry);
							
							// If we want to use the current entry in a streaming command to calculate something "in-between" samples
							// This can for example be used to calculate our own implementation of 1,5,15 minutes Average load on Windows...
							addRowForCurrentSubSampleHookin(entry);

							// start/restart the "auto close sample"
							if (_closeSampleTimeout != null)
								_closeSampleTimeout.restart();
						}
						catch (OsRecordParseException e)
						{
							addException(e);
							_logger.error("Problems when applying the parsed String Array ["+StringUtil.toCommaStr(strArr)+"].", e);
						}
					}
					else
					{
						//_logger.trace("-DISCARD-: The String Array ["+StringUtil.toCommaStr(isa)+"].");
						_logger.debug("-DISCARD-: This row has '"+isa.length+"' entries. MetaDataExpected parseCount='"+md.getParseColumnCount()+"'. Row '"+row+"'. The Input/PreParsed String Array, size("+isa.length+")=["+StringUtil.toCommaStr(isa)+"].");
					}
				}
			}
		}
		// Normal mode: oneInputRow produces oneTableRow
		else  
		{
			// Parse: Accept or Re-arrange columns
			String[] strArr = parseRow(md, row, isa, type);
			if ( strArr != null )
			{
				if (_logger.isDebugEnabled())
					_logger.debug("++ALLOW++: this row has '"+strArr.length+"' entries parseCount='"+md.getParseColumnCount()+"'. Row '"+row+"'. The Input/PreParsed String Array, size("+isa.length+")=["+StringUtil.toCommaStr(isa)+"], The parseRow() returned String Array, size("+strArr.length+")=["+StringUtil.toCommaStr(strArr)+"].");

				// Make a OsTableRow object of the input.
				// and ADD it to the "sample" table
				try
				{
					OsTableRow entry = new OsTableRow(md, strArr);
					_currentSample.addRow(entry);

					// If we want to use the current entry in a streaming command to calculate something "in-between" samples
					// This can for example be used to calculate our own implementation of 1,5,15 minutes Average load on Windows...
					addRowForCurrentSubSampleHookin(entry);

					// start/restart the "auto close sample"
					if (_closeSampleTimeout != null)
						_closeSampleTimeout.restart();
				}
				catch (OsRecordParseException e)
				{
					addException(e);
					_logger.error("Problems when applying the parsed String Array ["+StringUtil.toCommaStr(strArr)+"].", e);
				}
			}
			else
			{
				//_logger.trace("-DISCARD-: The String Array ["+StringUtil.toCommaStr(isa)+"].");
				_logger.debug("-DISCARD-: This row has '"+isa.length+"' entries. MetaDataExpected parseCount='"+md.getParseColumnCount()+"'. Row '"+row+"'. The Input/PreParsed String Array, size("+isa.length+")=["+StringUtil.toCommaStr(isa)+"].");
			}
		}
	}
	
	/**
	 * For a streaming command, we might want to explore the data captured "in-between" full samples to do various calculations<br>
	 * This method would be the place to do it, simply override and implement
	 * 
	 * @param entry     The OsTableRow entry that was just added...
	 */
	public void addRowForCurrentSubSampleHookin(OsTableRow entry)
	{
		// do nothing... or override and implement it in any subclasses
	}

//	private void internalAddRow(HostMonitorMetaData md, String[] strArr)
//	{
//		// Parse: Accept or Re-arrange columns
//		String[] strArr = parseRow(md, row, isa, type);
//
//		if ( strArr != null )
//		{
//			if (_logger.isDebugEnabled())
//				_logger.debug("++ALLOW++: this row has '"+strArr.length+"' entries parseCount='"+md.getParseColumnCount()+"'. Row '"+row+"'. The Input/PreParsed String Array, size("+isa.length+")=["+StringUtil.toCommaStr(isa)+"], The parseRow() returned String Array, size("+strArr.length+")=["+StringUtil.toCommaStr(strArr)+"].");
//
//			// Make a OsTableRow object of the input.
//			// and ADD it to the "sample" table
//			try
//			{
//				OsTableRow entry = new OsTableRow(md, strArr);
//				_currentSample.addRow(entry);
//
//				// start/restart the "auto close sample"
//				if (_closeSampleTimeout != null)
//					_closeSampleTimeout.restart();
//			}
//			catch (OsRecordParseException e)
//			{
//				addException(e);
//				_logger.error("Problems when applying the parsed String Array ["+StringUtil.toCommaStr(strArr)+"].", e);
//			}
//		}
//		else
//		{
//			//_logger.trace("-DISCARD-: The String Array ["+StringUtil.toCommaStr(isa)+"].");
//			_logger.debug("-DISCARD-: This row has '"+isa.length+"' entries. MetaDataExpected parseCount='"+md.getParseColumnCount()+"'. Row '"+row+"'. The Input/PreParsed String Array, size("+isa.length+")=["+StringUtil.toCommaStr(isa)+"].");
//		}
//	}

	/**
	 * Get the default sleep time from the Configuration.<br>
	 * If the COnfiguration dosn't exist a default value will be returned.
	 * <p>
	 * Get the sleep time from the configuration file using the key <code><i>ModuleName</i>.sleep</code>
	 * <p>
	 * Default value is 2
	 * 
	 * @return a sleep time which could be used in when composing the OS Command.
	 */
	public int getSleepTime()
	{
		int sleepTime = 2;
//		Configuration conf = Configuration.getInstance(Configuration.CONF);
		Configuration conf = Configuration.getCombinedConfiguration();
		if (conf != null)
			sleepTime = conf.getIntProperty("hostmon."+getModuleName()+".sleep", sleepTime);
		return sleepTime;
	}

	/**
	 * Get the Operating System Command that will be used to get valid information
	 * <p>
	 * This implementation gets the command from the configuration file using the key <code><i>ModuleName</i>.cmd</code>
	 * @return OS Command, or null if the key is not in the configuration file.
	 */
	public String getCommand()
	{
		String oscmd = null;
//		Configuration conf = Configuration.getInstance(Configuration.CONF);
		Configuration conf = Configuration.getCombinedConfiguration();
		if (conf != null)
			oscmd = conf.getProperty("hostmon."+getModuleName()+".cmd");
		
		if (oscmd != null)
			_logger.debug(getModuleName()+" found OS Command '"+oscmd+"' in the configuration file.");

		return oscmd;
	}
	

	/**
	 * Override this to set what thread name that should be used
	 * @return
	 */
	protected String getThreadName()
	{
		return getModuleName();
	}

	/**
	 * Override this to set a NAME of this Monitor Module
	 * @return
	 */
	abstract public String getModuleName();

	/**
	 * Print any start message when this thread starts, you can override it to print your own.
	 */
	protected void printStartMessage()
	{
		_logger.info("Starting the Host Monitoring module '"+getModuleName()+"'.");
	}

	/**
	 * Print any stop message when this thread starts, you can override it to print your own.
	 */
	protected void printStopMessage()
	{
		_logger.info("Stopped the Host Monitoring module '"+getModuleName()+"'.");
	}

	/**
	 * Start the monitoring thread
	 * @throws Exception
	 */
	public void start()
	throws Exception
	{
		if ( ! isOsCommandStreaming() )
		{
			_logger.warn(getModuleName()+" the command '"+getCommand()+"' is not marked as a streaming command, therefore you should not a background thread for this, instead use the method executeAndParse() to get a sample. Optionally use method getCurrentSample() to get the last sample results.");
			return;
		}

		if (isPaused())
		{
			_logger.warn(getModuleName()+" has been paused, to be able to start the Host Monitor '"+getModuleName()+"' you need to invoke the method setPaused(false).");
			return;
		}

		// For "dynamic initialization"... reset the meta data
		if (getMetaData() != null && getMetaData().isInitializeUsingFirstRowEnabled())
			setMetaData(null);
			
		if (getMetaData() == null)
			setMetaData(createMetaData());

		if (getMetaData() == null)
			throw new Exception("No MetaData Description has been created, this must be done before starting. This can be done by overriding method createMetaData() or use setMetaData().");
		
		_thread = new Thread(this, getThreadName());
		_thread.setDaemon(true);
		_thread.start();
	}

	/**
	 * Shutdown/stop the monitoring thread
	 */
	public void shutdown()
	{
		_running = false;
		if (_thread != null)
		{
			_logger.info(getModuleName()+" Was asked to to a shutdown.");
			_thread.interrupt();
		}
	}

	/**
	 * Check if the monitoring thread is running
	 */
	public boolean isRunning()
	{
		if (_thread != null)
		{
			if ( ! _thread.isAlive() )
				_running = false;
		}
		return _running;
	}


	/**
	 * If you want the Thread to be paused, set this to true<br>
	 * It doesn't pause the collector itself, it's just a flag that it should not be restarted<br>
	 * @param pause
	 */
	public void setPaused(boolean pause)
	{
		_paused = pause;
	}	

	public boolean isPaused()
	{
		return _paused;
	}	

//	/**
//	 * Just an internal wrapper so we can switch out SSH execution to ProcessBuilder (and execute any local commands)
//	 */
//	public static interface ExecutionWrapper
//	{
//		public void executeCommand(String cmd) throws IOException;
//		
//		public String getCharset(); // Should this be here???
//		
//		public InputStream getStdout();
//		public InputStream getStderr();
//
//		public Integer getExitStatus();
//
//		public int waitForData() throws InterruptedException;
////		public int waitFor() throws InterruptedException;
//		
//		public boolean isClosed(); // Check if the underlying Stream is closed or not
//		public void close();
//	}
//	private ExecutionWrapper _execWrapper;
//
//	private static class ExecutionWrapperShh
//	implements ExecutionWrapper
//	{
//		private SshConnection _sshConn;
//		private HostMonitor2 _hostMon;
//		
//		private Session _sshSession;
//
//		public ExecutionWrapperShh(HostMonitor2 hostMon, SshConnection sshConn)
//		{
//			_sshConn = sshConn;
//			_hostMon = hostMon;
//		}
//
//		@Override
//		public void executeCommand(String cmd) throws IOException
//		{
//			_sshSession = _sshConn.execCommand(_hostMon.getCommand());
//		}
//
//		@Override
//		public String getCharset()
//		{
//			return _sshConn.getOsCharset();
//		}
//
//		@Override
//		public int waitForData() throws InterruptedException
//		{
//			Thread.sleep(250);
//			return 0;
//		}
//
//		@Override
//		public InputStream getStdout()
//		{
//			return _sshSession.getStdout();
//		}
//
//		@Override
//		public InputStream getStderr()
//		{
//			return _sshSession.getStderr();
//		}
//
//		@Override
//		public Integer getExitStatus()
//		{
//			return _sshSession.getExitStatus();
//		}
//
//		@Override
//		public boolean isClosed()
//		{
//			return _sshSession.getState() == 4; // STATE_CLOSED = 4;
//		}
//
//		@Override
//		public void close()
//		{
//			_sshSession.close();
//		}
//	}
//
//	private static class ExecutionWrapperLocalCmd
//	implements ExecutionWrapper
//	{
//		private ProcessBuilder _pb;
//		private Process        _proc;
//		private InputStream    _stdout;
//		private InputStream    _stderr;
//		private int            _exitStatus = -1;
//
//		@Override
//		public void executeCommand(String cmd) throws IOException
//		{
//			// Parse the command into a String array, if cmd has parameters it needs to be "split" into and array
//			String[] params = StringUtil.translateCommandline(cmd, false);
//
//			// Create the ProcessBuilder
//			_pb = new ProcessBuilder(params);
//
//			// Change environment, this could be usable if the 'dbxtune.sql.pretty.print.cmd' is a shell script or a bat file
//			Map<String, String> env = _pb.environment();
//			env.put("SOME_ENV_VAR", "xxxx");
//			
//			// Set current working directory to DBXTUNE_HOME
//			//_pb.directory(new File(progDir));
//			
//			// Get the STDIN and STDOUT 
//			_stdout = _proc.getInputStream();
//			_stderr = _proc.getErrorStream();
//			
//			// Start the process
//			_proc = _pb.start();
//		}
//
//		@Override
//		public String getCharset()
//		{
//			return null;
//		}
//
//		@Override
//		public int waitForData() throws InterruptedException
//		{
//			Thread.sleep(250);
//			return 0;
//		}
//
//		@Override
//		public InputStream getStdout()
//		{
//			return _stdout;
//		}
//
//		@Override
//		public InputStream getStderr()
//		{
//			return _stderr;
//		}
//
//		@Override
//		public Integer getExitStatus()
//		{
//			_exitStatus = _proc.exitValue();
//			return _exitStatus;
//		}
//
//		@Override
//		public boolean isClosed()
//		{
//			return ! _proc.isAlive();
//		}
//
//		@Override
//		public void close()
//		{
//			try
//			{
//				_stdout.close();
//				_stderr.close();
//			}
//			catch (IOException ignore)
//			{
//			}
//
////			_proc.destroy();
////			_pb.close();
//		}
//	}
	
	/**
	 * A background thread that reads output from a OS Command that streams data (like vmstat, iostat etc)
	 * <p>
	 * <ul>
	 * <li>At start execute the OS command </li>
	 * <li>Then loop until the background thread is shutdown() </li>
	 * <li>read results from the streaming OS Command, and call parseAndApply() for each row returned. </li>
	 * <ul>
	 */
	@Override
	public void run()
	{
		printStartMessage();

		_running = true;

//		Session sess = null;
		ExecutionWrapper execWrapper = null;
		try
		{
//			boolean requestPty = false;
//			if (isConnectedToVendor(OsVendor.Windows))
//				requestPty = true;
//
//			if (Configuration.getCombinedConfiguration().hasProperty(PROPKEY_forcePty))
//			{
//				requestPty = Configuration.getCombinedConfiguration().getBooleanProperty("HostMonitor.ssh.requestPty.force", requestPty);
//				_logger.info("Using property '" + PROPKEY_forcePty + "' to set PTY Terminal, requestPty=" + requestPty);
//			}
//			
//			_logger.info("Executing command '"+getCommand()+"', requestPty=" + requestPty + ", for the module '"+getModuleName()+"'.");
//
//			sess = _conn.execCommand(getCommand(), requestPty);

			_logger.info("Executing command '"+getCommand()+"', for the module '"+getModuleName()+"'.");
//			sess = _conn.execCommand(getCommand());
			execWrapper = _hostMonConn.executeCommand(getCommand());
		}
//		catch (IOException e)
		catch (Exception e)
		{
			addException(e);
			_logger.error("Problems when executing OS Command '"+getCommand()+"', Caught: "+e.getMessage(), e);
			_running = false;
			
			// In some cases we might want to close the SSH Connection and start "all over"
			_hostMonConn.handleException(e);
//			// In some cases we might want to close the SSH Connection and start "all over"
//			if (e.getMessage().contains("SSH_OPEN_CONNECT_FAILED"))
//			{
//				try { _conn.reconnect(); }
//				catch(IOException ex) { _logger.error("Problems SSH reconnect. Caught: " + ex); }
//			}
			
			return;
		}

		if (_closeSampleTimeoutDelay > 0)
		{
			_logger.debug("Creating 'Close-Sample-Timeout' with the timeout value of '"+_closeSampleTimeoutDelay+"' ms.");
			_closeSampleTimeout = new Timer(_closeSampleTimeoutDelay, new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					_closeSampleTimeout.stop();
					closeCurrentSample();
				}
			});
		}
		

//		InputStream stdout = sess.getStdout();
//		InputStream stderr = sess.getStderr();
		InputStream stdout = execWrapper.getStdout();
		InputStream stderr = execWrapper.getStderr();

//		Charset osCharset = Charset.forName(_conn.getOsCharset());
		Charset osCharset = Charset.forName(_hostMonConn.getOsCharset());

		BufferedReader stdoutReader = new BufferedReader(new InputStreamReader(stdout, osCharset));
		BufferedReader stderrReader = new BufferedReader(new InputStreamReader(stderr, osCharset));

		while(_running)
		{
//			int timeoutVal = 60 * 1000;
			
			try
			{
				// Wait for input if both STDOUT & STDERR is empty
				if ((stdout.available() == 0) && (stderr.available() == 0))
				{
					// Wait for ### ms and then try again
					// or implement some kind of wait/notifyAll paradigm for when the underlying Streams receives data (This is what I did when I used https://github.com/SoftwareAG/ganymed-ssh-2)
					try
					{
						execWrapper.waitForData();
					}
					catch (InterruptedException e)
					{
						// TODO: handle exception
					}
					
					if ( execWrapper.isClosed() )
					{
						if (stdout.available() > 0 || stderr.available() > 0)
							continue;
						break;
					}
					
//					/* Even though currently there is no data available, it may be that new data arrives
//					 * and the session's underlying channel is closed before we call waitForCondition().
//					 * This means that EOF and STDOUT_DATA (or STDERR_DATA, or both) may
//					 * be set together.
//					 */
//
//					if (_logger.isDebugEnabled())
//						_logger.debug("----SSH-WAIT-FOR-INPUT[" + getModuleName() + "]");
//
//					int conditions = sess.waitForCondition(
//							  ChannelCondition.STDOUT_DATA 
//							| ChannelCondition.STDERR_DATA
//							| ChannelCondition.EOF, 
//							timeoutVal);
//
//					// Wait no longer than XX seconds
//					if ((conditions & ChannelCondition.TIMEOUT) == ChannelCondition.TIMEOUT)
//					{
//						// A timeout occurred.
//						_logger.warn("Timeout while waiting for data from peer, continuing waiting on data. return ChannelCondition from waitForCondition(...), condition=" + conditions + ", asStr=" + toString_ChannelCondition(conditions) + ", timeoutInMs=" + timeoutVal);
//
//						continue;
//
//						// A timeout occurred.
////						throw new IOException("Timeout while waiting for data from peer.");
////						throw new IOException("Unexpected return ChannelCondition from waitForCondition(...), condition=" + conditions + ", asStr=" + toString_ChannelCondition(conditions) + ", timeoutInMs=" + timeoutVal);
//					}
//
//					// Here we do not need to check separately for CLOSED, since CLOSED implies EOF
//					if (   ((conditions & ChannelCondition.EOF)    == ChannelCondition.EOF) 
//					    || ((conditions & ChannelCondition.CLOSED) == ChannelCondition.CLOSED) 
//					   )
//					{
//						// The remote side won't send us further data...
//						if ((conditions & (ChannelCondition.STDOUT_DATA | ChannelCondition.STDERR_DATA)) == 0)
//						{
//							// ... and we have consumed all data in the local arrival window.
//							_logger.info(getModuleName()+" Received EOF from the command '"+getCommand()+"'.");
//							addException(new Exception("Received EOF from the command at time: "+new Timestamp(System.currentTimeMillis())+", \nThe module will be restarted, and the command '"+getCommand()+"' re-executed."));
//		/*<--*/				break;
//						}
//					}
//
//					// OK, either STDOUT_DATA or STDERR_DATA (or both) is set.
//
//					// You can be paranoid and check that the library is not going nuts:
//					// if ((conditions & (ChannelCondition.STDOUT_DATA | ChannelCondition.STDERR_DATA)) == 0)
//					//	throw new IllegalStateException("Unexpected condition result (" + conditions + ")");
				}

				/* If you below replace "while" with "if", then the way the output appears on the local
				 * stdout and stder streams is more "balanced". Additionally reducing the buffer size
				 * will also improve the interleaving, but performance will slightly suffer.
				 * OKOK, that all matters only if you get HUGE amounts of stdout and stderr data =)
				 */
				
				// STDOUT
				while (stdout.available() > 0) // or possibly:	while (stdoutReader.ready())
				{
					long startTs = -1;
					if (_logger.isDebugEnabled())
					{
						startTs = System.currentTimeMillis();
						_logger.debug("SSH-STDOUT[" + getModuleName() + "][available=" + stdout.available() + "]: -start-");
					}
					
					// NOW READ input
					String row = stdoutReader.readLine();

					if (_logger.isDebugEnabled())
						_logger.debug("SSH-STDOUT[" + getModuleName() + "][ms=" + (System.currentTimeMillis()-startTs) + ", available=" + stdout.available() + "]: row=|" + row + "|.");

					// discard empty rows
					if (StringUtil.isNullOrBlank(row))
						continue;

System.out.println(this.getClass().getSimpleName()+".run(): reading-STDIN: row=|"+row+"|");
//					System.out.println(row);
					parseAndApply(getMetaData(), row, SshConnection.STDOUT_DATA);
				}

				// STDERR
				while (stderr.available() > 0)
				{
					long startTs = -1;
					if (_logger.isDebugEnabled())
					{
						startTs = System.currentTimeMillis();
						_logger.debug("SSH-STDERR[" + getModuleName() + "][available=" + stdout.available() + "]: -start-");
					}
					
					// NOW READ input
					String row = stderrReader.readLine();

					if (_logger.isDebugEnabled())
						_logger.debug("SSH-STDERR[" + getModuleName() + "][ms=" + (System.currentTimeMillis()-startTs) + ", available=" + stdout.available() + "]: row=|" + row + "|.");

					// discard empty rows
					if (StringUtil.isNullOrBlank(row))
						continue;

					if (row != null && row.toLowerCase().indexOf("command not found") >= 0 || row.toLowerCase().indexOf("access denied") >= 0)
					{
						_logger.error(getModuleName()+". The command '"+getCommand()+"' in current $PATH, got following message on STDERR: "+row);
						addException(new Exception("The command '"+getCommand()+"'\n"
								+ "in current $PATH\n"
								+ "got following message on STDERR: "+row));
					}
					
//					System.err.println(row);
					parseAndApply(getMetaData(), row, SshConnection.STDERR_DATA);
				}
			}
//			catch (IOException e)
//			{
//				addException(e);
//				_logger.error("Problems when reading output from the OS Command '"+getCommand()+"', Caught: "+e.getMessage(), e);
//				_running = false;
//			}
			catch (InterruptedIOException ex)
			{
				if (_running)
					_logger.warn("If this was during 'shutdown/stop' sequence, it's OK, if not it will be restarted. This happened when reading output from the OS Command '"+getCommand()+"', Caught: "+ex.getMessage());
				_running = false;
			}
			catch (Exception e)
			{
				addException(e);
				_logger.error("Problems when reading output from the OS Command '"+getCommand()+"', Caught: "+e.getMessage(), e);
				_running = false;
			}
		}

//		if (sess != null)
//		{
//			// Sometimes I have seen exception here... so map that away
//			try 
//			{
//				int osRetCode = sess.getExitStatus();
//				if (osRetCode != 0)
//					_logger.error("OS Return Code " + osRetCode + ". Expected return code is 0 for command: " + getCommand());
//			}
//			catch (Exception ignore) { /* ignore */ }
//
//			_logger.info("Closing Streaming SSH Session for '" + getModuleName() + "' with command '" + getCommand() + "'.");
//			sess.close();
//		}
		// Sometimes I have seen exception here... so map that away
		try 
		{
			int osRetCode = execWrapper.getExitStatus();
			if (osRetCode != 0)
				_logger.error("OS Return Code " + osRetCode + ". Expected return code is 0 for command: " + getCommand());
		}
		catch (Exception ignore) { /* ignore */ }

		_logger.info("Closing Streaming SSH Session for '" + getModuleName() + "' with command '" + getCommand() + "'.");
		execWrapper.close();
		
		_running = false;
		printStopMessage();
	}

//	private static String toString_ChannelCondition(int conditions)
//	{
//		String str = "";
//		
//		if ((conditions & ChannelCondition.TIMEOUT    ) == ChannelCondition.TIMEOUT    ) str += ", TIMEOUT";       // 1  = TIMEOUT
//		if ((conditions & ChannelCondition.CLOSED     ) == ChannelCondition.CLOSED     ) str += ", CLOSED";        // 2  = CLOSED
//		if ((conditions & ChannelCondition.STDOUT_DATA) == ChannelCondition.STDOUT_DATA) str += ", STDOUT_DATA";   // 4  = STDOUT_DATA
//		if ((conditions & ChannelCondition.STDERR_DATA) == ChannelCondition.STDERR_DATA) str += ", STDERR_DATA";   // 8  = STDERR_DATA
//		if ((conditions & ChannelCondition.EOF        ) == ChannelCondition.EOF        ) str += ", EOF";           // 16 = EOF        
//		if ((conditions & ChannelCondition.EXIT_STATUS) == ChannelCondition.EXIT_STATUS) str += ", EXIT_STATUS";   // 32 = EXIT_STATUS
//		if ((conditions & ChannelCondition.EXIT_SIGNAL) == ChannelCondition.EXIT_SIGNAL) str += ", EXIT_SIGNAL";   // 64 = EXIT_SIGNAL
//
//		if ( ! str.isEmpty() )
//			str = str.substring(2);
//
//		return str;
//	}

	/**
	 * This method should be used for non streaming OS Commands<br>
	 * Meaning it's a OS Command that terminates after it has printed all it's records to the stdout or stderr
	 * <p>
	 * NOTE: This probably needs more work before it can be used:
	 *       - Should we try to connect if not connected
	 *       - how should the user initiate it's sub class
	 *       - etc...
	 * 
	 * @return a OsTable Object from the parsed output of an OS Command
	 */
	public OsTable executeAndParse()
	{
		if (isOsCommandStreaming())
		{
			_logger.warn(getModuleName()+" the command '"+getCommand()+"' is marked as a streaming command, therefore you should start() a background thread for this. Then use the method getSummaryTable() to get results.");

			// FIXME: Should the deliver null or an empty OsTable on errors or Exception
			return null; 
		}

//		Session sess = null;
		ExecutionWrapper execWrapper = null;
		try
		{
			_logger.debug("Executing command '"+getCommand()+"' for the module '"+getModuleName()+"'.");
//			sess = _conn.execCommand(getCommand());
			execWrapper = _hostMonConn.executeCommand(getCommand());
		}
		catch (Exception e)
		{
			addException(e);
			_logger.error("Problems when executing OS Command '"+getCommand()+"', Caught: "+e.getMessage(), e);

			// FIXME: Should the deliver null or an empty OsTable on errors or Exception
			return null; 
		}

		// Create a new sample
		_currentSample = new OsTable(getMetaData());

		
//		InputStream stdout = sess.getStdout();
//		InputStream stderr = sess.getStderr();
		InputStream stdout = execWrapper.getStdout();
		InputStream stderr = execWrapper.getStderr();
		
//		Charset osCharset = Charset.forName(_conn.getOsCharset());
		Charset osCharset = Charset.forName(_hostMonConn.getOsCharset());

		BufferedReader stdoutReader = new BufferedReader(new InputStreamReader(stdout, osCharset));
		BufferedReader stderrReader = new BufferedReader(new InputStreamReader(stderr, osCharset));

		while(true)
		{
			try
			{
				if ((stdout.available() == 0) && (stderr.available() == 0))
				{
//					/* Even though currently there is no data available, it may be that new data arrives
//					 * and the session's underlying channel is closed before we call waitForCondition().
//					 * This means that EOF and STDOUT_DATA (or STDERR_DATA, or both) may
//					 * be set together.
//					 */
//
//					int conditions = sess.waitForCondition(
//							  ChannelCondition.STDOUT_DATA 
//							| ChannelCondition.STDERR_DATA
//							| ChannelCondition.EOF, 
//							30*1000);
//
//					// Wait no longer than 30 seconds
//					if ((conditions & ChannelCondition.TIMEOUT) != 0)
//					{
//						// A timeout occurred.
//						throw new IOException("Timeout while waiting for data from peer.");
//					}
//
//					// Here we do not need to check separately for CLOSED, since CLOSED implies EOF
//					if ((conditions & ChannelCondition.EOF) != 0)
//					{
//						// The remote side won't send us further data...
//						if ((conditions & (ChannelCondition.STDOUT_DATA | ChannelCondition.STDERR_DATA)) == 0)
//						{
//							// NORMAL EXIT: ... and we have consumed all data in the local arrival window.
//		/*<--*/				break;
//						}
//					}

					try
					{
						execWrapper.waitForData();
					}
					catch (InterruptedException e)
					{
						// TODO: handle exception
					}
					
					if ( execWrapper.isClosed() )
					{
						if (stdout.available() > 0 || stderr.available() > 0)
							continue;
						break;
					}
				}

				/* If you below replace "while" with "if", then the way the output appears on the local
				 * stdout and stder streams is more "balanced". Additionally reducing the buffer size
				 * will also improve the interleaving, but performance will slightly suffer.
				 * OKOK, that all matters only if you get HUGE amounts of stdout and stderr data =)
				 */
				while (stdout.available() > 0)
				{
//					int len = stdout.read(buffer);
//					if (len > 0) // this check is somewhat paranoid
//					{
//						// NOTE if charset convertion is needed, use: new String(buffer, CHARSET)
//						String row = null;
//						BufferedReader sr = new BufferedReader(new StringReader(new String(buffer, 0, len, osCharset)));
//						while ((row = sr.readLine()) != null)
//						{
////							System.out.println(row);
//							parseAndApply(getMetaData(), row, SshConnection.STDOUT_DATA);
//						}
//					}
//					long startTs = -1;
					if (_logger.isDebugEnabled())
					{
//						startTs = System.currentTimeMillis();
						_logger.debug("SSH-STDOUT[" + getModuleName() + "][available=" + stdout.available() + "]: -start-");
					}
					
					// NOW READ input
//					String row = stdoutReader.readLine();
					String row = null;
					while ((row = stdoutReader.readLine()) != null)
					{
						// discard empty rows
						if (StringUtil.isNullOrBlank(row))
							continue;

						if (_logger.isDebugEnabled())
							_logger.debug("Received on STDOUT: "+row);

						parseAndApply(getMetaData(), row, SshConnection.STDOUT_DATA);
					}

//					if (_logger.isDebugEnabled())
//						_logger.debug("SSH-STDOUT[" + getModuleName() + "][ms=" + (System.currentTimeMillis()-startTs) + ", available=" + stdout.available() + "]: row=|" + row + "|.");

					// discard empty rows
//					if (StringUtil.isNullOrBlank(row))
//						continue;

//					parseAndApply(getMetaData(), row, SshConnection.STDOUT_DATA);
				}

				while (stderr.available() > 0)
				{
//					int len = stderr.read(buffer);
//					if (len > 0) // this check is somewhat paranoid
//					{
//						String row = null;
//						BufferedReader sr = new BufferedReader(new StringReader(new String(buffer, 0, len, osCharset)));
//						while ((row = sr.readLine()) != null)
//						{
//							if (row != null && row.toLowerCase().indexOf("command not found") >= 0 || row.toLowerCase().indexOf("access denied") >= 0)
//							{
//								_logger.error(getModuleName()+" was the command '"+getCommand()+"' in current $PATH, got following message on STDERR: "+row);
//								addException(new Exception("Was the command '"+getCommand()+"' in current $PATH, got following message on STDERR: "+row));
//							}
////							System.err.println(row);
//							parseAndApply(getMetaData(), row, SshConnection.STDERR_DATA);
//							_logger.error("Received on STDERR: "+row);
//						}
//					}
//					long startTs = -1;
					if (_logger.isDebugEnabled())
					{
//						startTs = System.currentTimeMillis();
						_logger.debug("SSH-STDERR[" + getModuleName() + "][available=" + stdout.available() + "]: -start-");
					}
					
					// NOW READ input
//					String row = stderrReader.readLine();
					String row = null;
					while ((row = stderrReader.readLine()) != null)
					{
						// discard empty rows
						if (StringUtil.isNullOrBlank(row))
							continue;
						
						_logger.error("Received on STDERR: "+row);
						
						if (row != null && row.toLowerCase().indexOf("command not found") >= 0 || row.toLowerCase().indexOf("access denied") >= 0)
						{
							_logger.error(getModuleName()+". The command '"+getCommand()+"' in current $PATH, got following message on STDERR: "+row);
							addException(new Exception("The command '"+getCommand()+"'\n"
									+ "in current $PATH\n"
									+ "got following message on STDERR: "+row));
						}
						else
						{
							parseAndApply(getMetaData(), row, SshConnection.STDERR_DATA);
						}
					}

//					if (_logger.isDebugEnabled())
//						_logger.debug("SSH-STDERR[" + getModuleName() + "][ms=" + (System.currentTimeMillis()-startTs) + ", available=" + stdout.available() + "]: row=|" + row + "|.");

					// discard empty rows
//					if (StringUtil.isNullOrBlank(row))
//						continue;

//					if (row != null && row.toLowerCase().indexOf("command not found") >= 0 || row.toLowerCase().indexOf("access denied") >= 0)
//					{
//						_logger.error(getModuleName()+". The command '"+getCommand()+"' in current $PATH, got following message on STDERR: "+row);
//						addException(new Exception("The command '"+getCommand()+"'\n"
//								+ "in current $PATH\n"
//								+ "got following message on STDERR: "+row));
//					}
					
//					System.err.println(row);
//					parseAndApply(getMetaData(), row, SshConnection.STDERR_DATA);
//					_logger.error("Received on STDERR: "+row);
				}
			}
//			catch (IOException e)
//			{
//				addException(e);
//				_logger.error("Problems when reading output from the OS Command '"+getCommand()+"', Caught: "+e.getMessage(), e);
//			}
			catch (Exception e)
			{
				addException(e);
				_logger.error("Problems when reading output from the OS Command '"+getCommand()+"', Caught: "+e.getMessage(), e);
			}
		}

//		if (sess != null)
//		{
//			// Sometimes I have seen exception here... so map that away
//			try 
//			{
//				int osRetCode = sess.getExitStatus();
//				if (osRetCode != 0)
//					_logger.error("OS Return Code " + osRetCode + ". Expected return code is 0 for command: " + getCommand());
//			}
//			catch (Exception ignore) { /* ignore */ }
//
//			sess.close();
//		}
		// Sometimes I have seen exception here... so map that away
		try 
		{
			int osRetCode = execWrapper.getExitStatus();
			if (osRetCode != 0)
				_logger.error("OS Return Code " + osRetCode + ". Expected return code is 0 for command: " + getCommand());
		}
		catch (Exception ignore) { /* ignore */ }

		execWrapper.close();

		// Now return the object, which the OS Commands output was put into
		return _currentSample;
	}
}
