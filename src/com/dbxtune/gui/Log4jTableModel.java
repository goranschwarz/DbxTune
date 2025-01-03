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
package com.dbxtune.gui;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.swing.table.AbstractTableModel;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.central.DbxTuneCentral;
import com.dbxtune.utils.StringUtil;

public class Log4jTableModel
extends AbstractTableModel
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
    private static final long serialVersionUID = 2152458182894425275L;

    //	LogManager.
	private LinkedList<Log4jLogRecord> _records = new LinkedList<Log4jLogRecord>();
	private ArrayList<SkipMessageEntry> _skipMessagesList = null;
	private int        _maxRecords = 500;
	private boolean    _noGuiMode  = false;

	public void setMaxRecords(int max) { _maxRecords = max; }
	public int  getMaxRecords()        { return _maxRecords; }

	public void    setNoGuiMode(boolean val) { _noGuiMode = val; }
	public boolean isNoGuiMode()             { return _noGuiMode;	}
	public boolean isGuiMode()               { return ! _noGuiMode;	}

	// Constructors
	public Log4jTableModel()
	{
		loadSkipMessages();
	}
	public Log4jTableModel(Log4jTableModel tm)
	{
		// Copy settings from the "old" TableModel to the new TableModel
		setNoGuiMode (tm.isNoGuiMode());
		setMaxRecords(tm.getMaxRecords());
		_skipMessagesList = tm._skipMessagesList;
	}

	public enum SkipMesssageType
	{
		REGEX, 
		INDEXOF
	};
	
	private static class SkipMessageEntry
	{
		SkipMesssageType _type;
		String           _str;
		Pattern          _pattern;
		LocalTime        _beginTime;
		LocalTime        _endTime;

		public SkipMessageEntry(SkipMesssageType type, String str)
		throws PatternSyntaxException
		{
			this(type, str, null, null);
		}

		public SkipMessageEntry(SkipMesssageType type, String str, LocalTime beginTime, LocalTime endTime)
		throws PatternSyntaxException
		{
			_type = type;
			_str  = str;

			if (SkipMesssageType.REGEX.equals(type))
			{
				_pattern = Pattern.compile(str);
			}
			else if (SkipMesssageType.INDEXOF.equals(type))
			{
				_str  = str;
			}
			
			_beginTime = beginTime;
			_endTime   = endTime;
		}

		public boolean isType(SkipMesssageType type)
		{
			return _type.equals(type);
		}

//		public SkipMessageEntry(Pattern pattern)
//		{
//			_pattern = pattern;
//		}
//		public SkipMessageEntry(Pattern pattern, LocalTime beginTime, LocalTime endTime)
//		{
//			_pattern   = pattern;
//			_beginTime = beginTime;
//			_endTime   = endTime;
//		}
	}

	/**
	 * Load "skip messages" from a file or a static list... compile them into regex
	 */
	private void loadSkipMessages()
	{
		_skipMessagesList = new ArrayList<>();

		String dirName  = DbxTuneCentral.getAppConfDir();
//		String dirName  = DbxTune.getInstance().getAppConfDir();
		String fileName = "Log4jTableModel.nogui.skiplist";

		// From file
		// - open files into a linked list of regexStr
		// - compile ... with try/catch to check for valid regEx in above list... errors: print ...
		File f = new File(dirName + "/" + fileName);
		if (f.exists())
		{
			_logger.info("User Defined LogAppender 'skiplist' file was found. using filename: '" + f.getAbsolutePath() + "'.");
			try
			{
				List<String> skipList = FileUtils.readLines(null, StandardCharsets.UTF_8);
				for (String line : skipList)
				{
					if (StringUtil.isNullOrBlank(line))
						continue;
					
					_logger.info("Loading User Defined LogAppender no-gui skiplist entry '" + line + "'.");
					try
					{
						_skipMessagesList.add( new SkipMessageEntry(SkipMesssageType.REGEX, line) );
					}
					catch(PatternSyntaxException ex)
					{
						_logger.error("Problems loading User Defined LogAppender no-gui skiplist regex entry '" + line + "'. Caught: " + ex);
					}
				}
			}
			catch (IOException ex)
			{
				_logger.error("Problems reading User Defined LogAppender 'skiplist' file '" + f.getAbsolutePath() + "'. Caught: " + ex, ex);
			}
		}
		else
		{
			_logger.info("No User Defined LogAppender 'skiplist' file was found. Continuing with static settings. checked for filename '" + f.getAbsolutePath() + "'.");

			// From static list
			_skipMessagesList.add(new SkipMessageEntry(SkipMesssageType.REGEX, "When trying to initialize Counters Model"));
			_skipMessagesList.add(new SkipMessageEntry(SkipMesssageType.REGEX, "The environment variable 'DBXTUNE_UD_ALARM_SOURCE_DIR' is NOT set."));
			_skipMessagesList.add(new SkipMessageEntry(SkipMesssageType.REGEX, "The environment variable 'DBXTUNE_NORMALIZER_SOURCE_DIR' is NOT set."));
			_skipMessagesList.add(new SkipMessageEntry(SkipMesssageType.REGEX, "The Directory '.*' does NOT exists. No User Defined Normalizer classes will be Compiled."));
			_skipMessagesList.add(new SkipMessageEntry(SkipMesssageType.REGEX, "Rejected .* plan names due to '<planStatus> not executed </planStatus>'. For the last "));
			_skipMessagesList.add(new SkipMessageEntry(SkipMesssageType.REGEX, "The persistent queue has [1-5] entries. The persistent writer might not keep in pace"));
			_skipMessagesList.add(new SkipMessageEntry(SkipMesssageType.REGEX, "The configuration '.*' might be to low. For the last"));

//			_skipMessagesList.add(new SkipMessageEntry(SkipMesssageType.INDEXOF, "plan names due to '<planStatus> not executed </planStatus>'. For the last "));

			// Skip some messages between 04:00 and 07:00 (DbxCentral might be down for database compaction)
			_skipMessagesList.add( new SkipMessageEntry(
					SkipMesssageType.REGEX, "Problems connecting/sending JSON-REST call to ", 
					LocalTime.of(04,00), 
					LocalTime.of(07,00) ) );
			
			_skipMessagesList.add( new SkipMessageEntry(
					SkipMesssageType.REGEX, "The persistent queue has .* entries. The persistent writer might not keep in pace.", 
					LocalTime.of(04,00), 
					LocalTime.of(07,00) ) );
		}
	}

	/**
	 * Check if the "skip message" is within the desired time limit<br>
	 * If the is <i>no</i> time limit, the true is returned. 
	 * @param entry
	 * @return
	 */
	private boolean skipMessageIfWithinTime(SkipMessageEntry entry)
	{
		if (entry._beginTime != null && entry._endTime != null)
		{
			LocalTime msgTime = LocalTime.now();
			// TODO: Test if the below works... otherwise just use "now"
//			LocalTime msgTime = Instant.ofEpochMilli(record.getMillis())
//					.atZone(ZoneId.systemDefault())
//					.toLocalTime();

			// isBetween begin/end time
			if (msgTime.isAfter(entry._beginTime) && msgTime.isBefore(entry._endTime))
				return true; // SKIP message
			else
				return false; // KEEP message
		}
		else
		{
			return true; // SKIP message
		}
	}
	/**
	 * Should the incoming message be "skipped"
	 * @param record
	 * @return
	 */
	public boolean skipMessage(Log4jLogRecord record)
	{
		if ( isGuiMode() )
		{
			// if GUI mode ALWAYS keep message
			return false; // KEEP message
		}
		else
		{
			//--------------------------
			// NO-GUI Mode is below
			//--------------------------

			// Only keep severe messages
			if ( record.isWarningLevel() || record.isSevereLevel() )
			{
				String msg = record.getMessage();
				if (msg == null)
					return true; // SKIP message

				if (_skipMessagesList != null)
				{
					for (SkipMessageEntry entry : _skipMessagesList)
					{
						if (entry.isType(SkipMesssageType.REGEX))
						{
							Matcher m = entry._pattern.matcher(msg);
							if (m.find())
							{
								return skipMessageIfWithinTime(entry);
							}
						}
						else if (entry.isType(SkipMesssageType.INDEXOF))
						{
							if (msg.contains(entry._str))
							{
								return skipMessageIfWithinTime(entry);
							}
						}
					}
				}
				
				// TODO: SKIP some "common" messages
				// FIXME
//				if (msg.contains("x")) return true;
//				if (msg.contains("y")) return true;
//				if (msg.contains("z")) return true;

				// if we get here it's a KEEPER
				return false; // KEEP message
			}
			else
			{
				// Skip all messages that are not WARN or Severe
				return true; // SKIP message
			}
		}
	}

	public void addMessage(Log4jLogRecord record)
	{
		// Skip info messages (in NO-GUI mode we only want to save more severe messages)
		if ( skipMessage(record) )
			return;

		_records.add(record);
		fireTableRowsInserted(_records.size()-1, _records.size()-1);

		if (_records.size() > _maxRecords)
		{
			_records.removeFirst();
			fireTableRowsDeleted(0, 0);
		}
//		System.out.println("addMessage(): "+record);
		
//		if (MainFrame.hasInstance())
//			MainFrame.getInstance().actionPerformed(new ActionEvent(this, 0, MainFrame.ACTION_OPEN_LOG_VIEW));
	}

	public Log4jLogRecord getRecord(int row)
	{
		return _records.get(row);
	}

	public void clear()
	{
		_records.clear();
		fireTableDataChanged();
	}


	@Override
	public Class<?> getColumnClass(int col)
	{
		switch (col)
		{
		case 0: return Long.class;
		case 1: return String.class;
		case 2: return String.class;
		case 3: return String.class;
		case 4: return String.class;
		case 5: return String.class;
		case 6: return Boolean.class;
		case 7: return String.class;
		}
		return super.getColumnClass(col);
	}
	@Override
	public String getColumnName(int col)
	{
		switch (col)
		{
		case 0: return "Seq";
		case 1: return "Time";
		case 2: return "Level";
		case 3: return "Thread Name";
		case 4: return "Class Name";
		case 5: return "Location";
		case 6: return "Thrown";
		case 7: return "Message";
		}
		return super.getColumnName(col);
	}
	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex)
	{
		return false;
	}
	@Override
	public int getColumnCount()
	{
		return 8;
	}
	@Override
	public int getRowCount()
	{
		return _records.size();
	}
//	@Override
//	public Object getValueAt(int row, int col)
//	{
//		Log4jLogRecord r = _records.get(row);
//		switch (col)
//		{
//		case 0: return Long.toString(r.getSequenceNumber());
//		case 1: return new Timestamp(r.getMillis());
//		case 2: return r.getLevel();
//		case 3: return r.getThreadDescription();
//		case 4: return r.getCategory();
//		case 5: return r.getLocation();
//		case 6: return Boolean.valueOf( r.getThrownStackTrace() != null ? true : false);
//		case 7: return r.getMessage();
////		case 4: return r.getNDC();
//		}
//		return null;
//	}	
	@Override
	public Object getValueAt(int row, int col)
	{
		Log4jLogRecord r = _records.get(row);
		switch (col)
		{
		case 0: return r.getSequence();
		case 1: return new Timestamp(r.getTimeMs());
		case 2: return r.getLevel();
		case 3: return r.getThreadName();
		case 4: return r.getClassName();
		case 5: return r.getLocation();
		case 6: return r.getThrowable() != null ? true : false;
		case 7: return r.getMessage();
//		case 4: return r.getNDC();
		}
		return null;
	}	
}
