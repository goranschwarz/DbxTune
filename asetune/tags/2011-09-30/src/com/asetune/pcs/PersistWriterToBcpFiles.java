/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package com.asetune.pcs;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import com.asetune.cm.CountersModel;
import com.asetune.cm.CountersModelAppend;
import com.asetune.utils.Configuration;
import com.asetune.utils.MandatoryPropertyException;
import com.asetune.utils.OSCommand;


public class PersistWriterToBcpFiles
    extends PersistWriterBase
{
    /** Log4j logging. */
	private static Logger _logger          = Logger.getLogger(PersistWriterToBcpFiles.class);

	/*---------------------------------------------------
	** DEFINITIONS
	**---------------------------------------------------
	*/
	private String   COL_SEP = "\t";
	private String   ROW_SEP = "\n";
	
	/*---------------------------------------------------
	** class members
	**---------------------------------------------------
	*/
	private String _name       = "PersistWriterToBcpFiles";

	private int    _moveFilesAfterXSeconds = 3600; // append to counter files for X seconds before making new files.
	private String _saveToDir           = null;
	private String _moveFilesOsCmd      = null;
	private String _moveFilesDateFormat = "yyyyMMdd.HHmmss";

	private String _ddlFilesOsCmd       = null;

	private long   _lastCounterFileCloseTime = System.currentTimeMillis();
	
	private Configuration _config    = null;
	private String        _configStr = null;

	private List<String> _bcpFiles = new LinkedList<String>();
	private List<String> _ddlFiles = new LinkedList<String>();

	/*---------------------------------------------------
	** Constructors
	**---------------------------------------------------
	*/
	public PersistWriterToBcpFiles()
	{
	}


	/*---------------------------------------------------
	** Methods
	**---------------------------------------------------
	*/

	@Override
	public String getName()
	{
		return _name;
	}

	@Override
	public void close()
	{
	}

	@Override
	public void startServices()
	{
	}
	@Override
	public void stopServices()
	{
	}

	@Override
	public Configuration getConfig()
	{
		return _config;
	}

	@Override
	public String getConfigStr()
	{
		return _configStr;
	}

	@Override
	public void init(Configuration props) throws Exception
	{
		_config = props;

		String propPrefix = "PersistWriterToBcpFiles.";
		String propname = null;

		// property: name
		propname = propPrefix+"name";
		_name = props.getProperty(propname, _name);

		// WRITE init message, jupp a little late, but I wanted to grab the _name
		_logger.info("Initializing the PersistentCounterHandler.WriterClass component named '"+_name+"'.");
		
		_saveToDir = props.getProperty(propPrefix+"saveToDir");
		if (_saveToDir == null)
		{
			_saveToDir = System.getProperty("ASETUNE_SAVE_DIR");
			if (_saveToDir == null)
			{
				_saveToDir = System.getProperty("ASETUNE_HOME");

				if (_saveToDir == null)
				{
					String err = "Directory 'PersistWriterToBcpFiles.saveToDir' name was not specified and ASETUNE_SAVE_DIR or ASETUNE_HOME was not set, can't save information about DDL table creation for CounterModel '"+getName()+"'."; 
					_logger.error(err);
					throw new MandatoryPropertyException(err);
				}
			}
		}

		_moveFilesAfterXSeconds = props.getIntProperty(propPrefix+"moveFilesAfterXSeconds", _moveFilesAfterXSeconds);

		_moveFilesOsCmd = props.getProperty(propPrefix+"moveFilesOsCmd");

		_moveFilesDateFormat = props.getProperty(propPrefix+"moveFilesDateFormat", _moveFilesDateFormat);

		_ddlFilesOsCmd = props.getProperty(propPrefix+"ddlFilesOsCmd");

		_logger.info("Configuration for PersistentCounterHandler.WriterClass component named '"+_name+"': "+_configStr);

		_configStr = 
			"saveToDir="               + _saveToDir +
			",moveFilesAfterXSeconds=" + _moveFilesAfterXSeconds +
			",moveFilesOsCmd="         + _moveFilesOsCmd +
			",moveFilesDateFormat="    + _moveFilesDateFormat +
			",ddlFilesOsCmd="          + _ddlFilesOsCmd +
			"";
	}

	/*---------------------------------------------------
	** Methods
	**---------------------------------------------------
	*/
	public void beginOfSample()
	{
	}

	/** Empty implementation */
	public void endOfSample(boolean caughtErrors)
	{
		if (_ddlFiles.size() > 0)
		{
			_logger.info("Calling installation/creation scripts for all the DDL files.");
	
			// Install DDL into db, if not already done
			for (String ddlFile : _ddlFiles)
			{
				if (ddlFile != null)
				{
					installDdlFile(ddlFile);
				}
			}
			// Remove ALL files from the "active" file list.
			_ddlFiles.clear();
		}

		// Check if we should move the file...
		int secondsSinceLastFileClose = (int) (System.currentTimeMillis() - _lastCounterFileCloseTime) / 1000;
		if (secondsSinceLastFileClose > _moveFilesAfterXSeconds)
		{
			_lastCounterFileCloseTime = System.currentTimeMillis();

			SimpleDateFormat dateFormater = new SimpleDateFormat(_moveFilesDateFormat);
			String dateStamp = dateFormater.format( new Date(System.currentTimeMillis()) );

			_logger.info("Moving all files into a 'final' state where they are ready to be BCP:ed into a table.");

			// move the "temp" files that holds the counters
			// into a timestamped file...
			// That file can now be "bcp:ed" into a dataserver.
			for (String bcpFile : _bcpFiles)
			{
				if (bcpFile != null)
				{
					moveCounterDataFile(bcpFile, dateStamp);
				}
			}

			// Remove ALL files from the "active" file list.
			_bcpFiles.clear();
		}
	}

	@Override
	public void saveSample(PersistContainer cont)
	{
		Timestamp parentSampleTime = cont.getMainSampleTime();
		String    aseServerName    = cont.getServerName();

		try
		{
			//
			// FIRST CHECK IF THE TABLE EXISTS, IF NOT CREATE IT
			//
			String tabName = getTableName(SESSIONS, null, false);

			if ( ! isDdlCreated(tabName) )
			{
				String sql = getTableDdlString(SESSIONS, null);
				sql += "\n";
				sql += "go";
				sql += "\n";

				// Save the DDL String to a file
				//TODO

				// Write SQL Table definition
				BufferedWriter ddlTabWriter     = null;
				String ddlFileName = _saveToDir + "/ddl_" + tabName + ".sql";

				// Open all files
				try
				{
					// Open The tabledef in write over
					ddlTabWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(ddlFileName)));
					_logger.info("Writing table DDL information for '" + tabName + "'.");

					ddlTabWriter.write(sql);

					ddlTabWriter.close();

					_ddlFiles.add(ddlFileName);
				}
				catch (FileNotFoundException e)
				{
					_logger.warn("Problems opening/creating the a file. "+e);
					return;
				}
				catch (IOException e)
				{
					_logger.warn("Error writing to file.", e);
					return;
				}

				markDdlAsCreated(tabName);
			}
			
			// Write SQL Table definition
			BufferedWriter sessWriter  = null;

			String sessFileName  = _saveToDir + "/tmp_" + tabName + ".bcp.appender";

			// Open The tabledef in write over
			sessWriter  = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(sessFileName, true)));

			_logger.debug("Writing Counter information for CounterModel '" + tabName + "'.");


			//------------------------------
			// Write to the files
			//------------------------------
			sessWriter.write(parentSampleTime.toString());
			sessWriter.write(COL_SEP);
			sessWriter.write(aseServerName);
			sessWriter.write(ROW_SEP);

			// Close the file
			sessWriter.close();

			// Add the file name to the list of "active" files
			// This is used later when moving files...
			if ( ! _bcpFiles.contains(sessFileName) )
				_bcpFiles.add(sessFileName);

			//--------------------------------------
			// COUNTERS
			//--------------------------------------
			for (CountersModel cm : cont._counterObjects)
			{
				saveCounterData(cm, parentSampleTime, aseServerName);
			}
		}
		catch (FileNotFoundException e)
		{
			_logger.warn("Problems opening/creating the a file. "+e);
			return;
		}
		catch (IOException e)
		{
			_logger.warn("Error writing to file.", e);
			return;
		}
		catch (SQLException e)
		{
			_logger.warn("Problems with SQL stuff.", e);
			return;
		}
	}
	
	@Override
	public boolean saveDdl(CountersModel cm)
  	{
		// Write SQL Table definition
		BufferedWriter ddlTabWriter     = null;

		String ddlFileName = _saveToDir + "/ddl_" + cm.getName() + ".sql";

		

		//------------------------------
		// Write SQL table definition file
		//------------------------------
		_logger.info("Writing table DDL information for CounterModel '" + cm.getName() + "'.");

		try
		{
			// Open The tabledef in write over
			ddlTabWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(ddlFileName)));

			//-------------
			// ABS
			//-------------
			ddlTabWriter.write(getTableDdlString(ABS, cm));
			ddlTabWriter.write("go\n");
			ddlTabWriter.write("\n");
			ddlTabWriter.write(getIndexDdlString(ABS, cm));
			ddlTabWriter.write("go\n");
			ddlTabWriter.write("\n");
			ddlTabWriter.write("\n\n\n\n\n\n");

			//-------------
			// DIFF
			//-------------
			ddlTabWriter.write(getTableDdlString(DIFF, cm));
			ddlTabWriter.write("go\n");
			ddlTabWriter.write("\n");
			ddlTabWriter.write(getIndexDdlString(DIFF, cm));
			ddlTabWriter.write("go\n");
			ddlTabWriter.write("\n");
			ddlTabWriter.write("\n\n\n\n\n\n");

			//-------------
			// RATE
			//-------------
			ddlTabWriter.write(getTableDdlString(RATE, cm));
			ddlTabWriter.write("go\n");
			ddlTabWriter.write("\n");
			ddlTabWriter.write(getIndexDdlString(RATE, cm));
			ddlTabWriter.write("go\n");
			ddlTabWriter.write("\n");
			ddlTabWriter.write("\n\n\n\n\n\n");

			
			if (ddlTabWriter != null) 
				ddlTabWriter.close();

			_ddlFiles.add(ddlFileName);

			return true;
		}
		catch (FileNotFoundException e)
		{
			_logger.warn("Problems opening/creating the a file. "+e);
			return false;
		}
		catch (IOException e)
		{
			_logger.warn("Error writing to file.", e);
			return false;
		}
		catch (SQLException e)
		{
			_logger.warn("Problems with SQL stuff.", e);
			return false;
		}

  	} // end: method


	private void saveCounterData(CountersModel cm, Timestamp parentSampleTime, String aseServerName)
  	{
		if (cm == null)
		{
			_logger.debug("saveCounterData: cm == null.");
			return;
		}

		if (cm instanceof CountersModelAppend) 
			return;

		if ( ! cm.hasDiffData() )
		{
			_logger.info("No diffData is available, skipping writing Counters for name='"+cm.getName()+"'.");
			return;
		}

		// Write SQL Table definition
		BufferedWriter absWriter  = null;
		BufferedWriter diffWriter = null;
		BufferedWriter rateWriter = null;

		String absFileName  = _saveToDir + "/tmp_" + cm.getName() + ".abs.bcp.appender";
		String diffFileName = _saveToDir + "/tmp_" + cm.getName() + ".diff.bcp.appender";
		String rateFileName = _saveToDir + "/tmp_" + cm.getName() + ".rate.bcp.appender";

		// Open all files
		try
		{
			// Open The tabledef in write over
			absWriter  = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(absFileName, true)));
			diffWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(diffFileName, true)));
			rateWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(rateFileName, true)));

			_logger.debug("Writing Counter information for CounterModel '" + cm.getName() + "'.");

			//------------------------------
			// Write to the files
			//------------------------------
			save(cm, CountersModel.DATA_ABS,  absWriter,  parentSampleTime, aseServerName);
			save(cm, CountersModel.DATA_DIFF, diffWriter, parentSampleTime, aseServerName);
			save(cm, CountersModel.DATA_RATE, rateWriter, parentSampleTime, aseServerName);

			// Add the file name to the list of "active" files
			// This is used later when moving files...
			if ( ! _bcpFiles.contains(absFileName)  ) _bcpFiles.add(absFileName);
			if ( ! _bcpFiles.contains(diffFileName) ) _bcpFiles.add(diffFileName);
			if ( ! _bcpFiles.contains(rateFileName) ) _bcpFiles.add(rateFileName);


			if (absWriter  != null) absWriter.close();
			if (diffWriter != null) diffWriter.close();
			if (rateWriter != null) rateWriter.close();
		}
		catch (FileNotFoundException e)
		{
			_logger.warn("Problems opening/creating the a file. "+e);
			return;
		}
		catch (IOException e)
		{
			_logger.warn("Error writing or closing file.", e);
		}

  	} // end: method

	private void save(CountersModel cm, int whatData, BufferedWriter writer, Timestamp parentSampleTime, String aseServerName)
	throws IOException
	{
		Object       colObj    = null;
		StringBuffer rowSb     = new StringBuffer();

		List<List<Object>> rows = cm.getDataCollection(whatData);
		List<String>       cols = cm.getColNames(whatData);

		if (rows == null || cols == null)
		{
			_logger.error("Rows or Columns cant be null. rows='"+rows+"', cols='"+cols+"'");
			return;
		}

		int rowsCount = rows.size();
		int colsCount = cols.size();
		
		// Loop all rows
		for (int r=0; r<rowsCount; r++)
		{
			// Compose 1 row 
			rowSb.setLength(0);

			// Add sqlSaveTime as the first column
			rowSb.append(parentSampleTime.toString());
			rowSb.append(COL_SEP);

			// When THIS sample was taken
			// probably the same time as parentSampleTime, but it can vary some milliseconds or so
			rowSb.append(cm.getTimestamp()+"");
			rowSb.append(COL_SEP);

			// How long the sample was for, in Milliseconds
			rowSb.append(cm.getLastSampleInterval());
			rowSb.append(COL_SEP);

			// Name of the ASE Server the sample was taken from
			rowSb.append(aseServerName);
			rowSb.append(COL_SEP);

			// loop all columns
			for (int c=0; c<colsCount; c++)
			{
				colObj =  rows.get(r).get(c);

				if (colObj != null)
					rowSb.append(colObj);
				else
					rowSb.append("");

				// No COL_SEP on last column
				if ( (c+1) == colsCount )
				{
					// nothing
				}
				else
				{
					rowSb.append(COL_SEP);
				}
			}
			
			// Write that row
			if (rowSb.length() > 0)
			{
				//--------------------
				// BCP FILE
				//--------------------
				if (writer != null)
				{
					writer.write(rowSb.toString());
					writer.newLine();
				}
			}
		} // end: loop rows
	}

	private boolean moveCounterDataFile(String bcpFile, String dateStamp)
	{
		File f = new File(bcpFile);
		if ( ! f.exists() )
		{
			_logger.warn("The file '"+bcpFile+"' does not exists.");
			return false;
		}

		String newFileName = "";
		newFileName = bcpFile    .replaceFirst("tmp_",          "final_"+dateStamp+"_");
		newFileName = newFileName.replaceFirst(".bcp.appender", ".bcp");

		f.renameTo( new File(newFileName) );

		// Call OS to do something, like "bcp"
		if (_moveFilesOsCmd != null)
		{
			String osCmdStr = _moveFilesOsCmd + " " + newFileName;
			try
			{
				OSCommand osCmd = OSCommand.execute(osCmdStr);
				int    retCode = osCmd.returnCode();
	
				if (retCode != 0)
				{
					String retStr  = osCmd.getOutput();
					_logger.error("Problems when executing the OS Command '"+osCmdStr+"'. The following output was received: "+retStr);
				}
			}
			catch(IOException e)
			{
				_logger.error("Problems when executing the OS Command '"+osCmdStr+"'. Caught: "+e);
			}
		}

		return true;
	} // end: method

	private boolean installDdlFile(String ddlFile)
	{
		File f = new File(ddlFile);
		if ( ! f.exists() )
		{
			_logger.warn("The file '"+ddlFile+"' does not exists.");
			return false;
		}

		// Call OS to do something, like "bcp"
		if (_ddlFilesOsCmd != null)
		{
			String osCmdStr = _ddlFilesOsCmd + " " + ddlFile;
			try
			{
				OSCommand osCmd = OSCommand.execute(osCmdStr);
				int    retCode = osCmd.returnCode();
	
				if (retCode != 0)
				{
					String retStr  = osCmd.getOutput();
					_logger.error("Problems when executing the OS Command '"+osCmdStr+"'. The following output was received: "+retStr);
				}
			}
			catch(IOException e)
			{
				_logger.error("Problems when executing the OS Command '"+osCmdStr+"'. Caught: "+e);
			}
		}
		return true;
	}
}
