package asemon.gui;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.jdesktop.swingx.treetable.AbstractTreeTableModel;

import asemon.pcs.PersistReader;
import asemon.pcs.PersistReader.SessionInfo;
import asemon.utils.StringUtil;
import asemon.utils.TimeUtils;

/**
 * A tree table model to simulate a off line sampled asemon sessions.
 * <p>
 * Describe more.
 * 
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
public class OfflineSessionModel 
//extends DefaultTreeTableModel
extends AbstractTreeTableModel
{
	private static Logger _logger = Logger.getLogger(OfflineSessionModel.class);
	
	private ArrayList<SessionLevel> _sessions = new ArrayList<SessionLevel>();

//	private Time _dayLevelStartTime = Time.valueOf("01:00:00");   // HH:MM:SS when a period should start
//	private int  _dayLevelHours     = 24;                  // Number of hours reflected in the day level

	private int  _dayLevelCount     = 1;      // how many days    should the day    level reflect
	private int  _hourLevelCount    = 1;      // how many hours   should the hour   level reflect
	private int  _minuteLevelCount  = 10;     // how many minutes should the minute level reflect
	
	private SimpleDateFormat _sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");

	/**
	 * 
	 */
	public OfflineSessionModel() 
	{
		super();
	}

//	public void refresh()
//	{
//		_sessions.clear();
//		init();
//	}
	private void printTree()
	{
		for (SessionLevel session : _sessions)
		{
			System.out.println( "-> " + session.toString() );
			printTree(session, 1);
		}
	}
	private void printTree(SessionLevel sl, int incCount)
	{
		for (int i=0; i<sl.getChildCount(); i++)
		{
			Object o = sl.getChild(i);
			if (o instanceof SessionLevel)
			{
				SessionLevel child = (SessionLevel)o;
				System.out.println( StringUtil.replicate(" +", incCount) + " > " + child.toString() );
				if (child.getChildCount() > 0)
					printTree(child, incCount+1);
			}
		}
	}

	public void setDayLevelCount   (int count) { _dayLevelCount    = count; }
	public void setHourLevelCount  (int count) { _hourLevelCount   = count; }
	public void setMinuteLevelCount(int count) { _minuteLevelCount = count; }

	public int getDayLevelCount   () { return _dayLevelCount; }
	public int getHourLevelCount  () { return _hourLevelCount; }
	public int getMinuteLevelCount() { return _minuteLevelCount; }


	public void init(List<SessionInfo> sessionList)
	{
		PersistReader reader = PersistReader.getInstance();
		if (reader == null)
			throw new RuntimeException("The 'PersistReader' has not been initialized.");

		// get a LIST of sessions
		if (sessionList == null)
			sessionList = reader.getSessionList();
			
		// Loop the sessions and load all samples 
		for (SessionInfo sessionInfo : sessionList)
		{
			SessionLevel sessionLevel = new SessionLevel(
					sessionInfo._sessionId, 
					sessionInfo._sessionId,
					sessionInfo._lastSampleTime, 
					sessionInfo._numOfSamples);
			_sessions.add(sessionLevel);

			List<Timestamp> sessionSamples = sessionInfo._sampleList;

			// Load all samples for this sampleId
			if (sessionSamples == null)
				sessionSamples = reader.getSessionSamplesList(sessionInfo._sessionId);
			
			if (sessionSamples != null && sessionSamples.size() > 0)
			{
				sessionLevel.addAllSamples(sessionSamples);
				sessionLevel.makeBabies();
			}
		}
			
		// Debug print the tree...
		if (_logger.isDebugEnabled())
			printTree();
	}
//	public void init()
//	{
//		PersistReader reader = PersistReader.getInstance();
//		if (reader == null)
//			throw new RuntimeException("The 'PersistReader' has not been initialized.");
//		Connection conn = reader.getConnection();
//
//		try
//		{
//			Statement stmnt   = conn.createStatement();
//			ResultSet rs      = stmnt.executeQuery(GET_ALL_SESSIONS);
//			while (rs.next())
//			{
//				Timestamp sessionStartTime = rs.getTimestamp("SessionStartTime");
//				Timestamp lastSampleTime   = rs.getTimestamp("LastSampleTime");
//				int       numOfSamples     = rs.getInt("NumOfSamples");
//
//				SessionLevel rec = new SessionLevel(sessionStartTime, sessionStartTime, lastSampleTime, numOfSamples);
//
//				_sessions.add(rec);
//			}
//			rs.close();
//			stmnt.close();
//			
//			for (Iterator it = _sessions.iterator(); it.hasNext();) 
//			{
//				SessionLevel session = (SessionLevel) it.next();
//				
//				PreparedStatement pstmnt = conn.prepareStatement(GET_SESSION);
//				pstmnt.setTimestamp(1, session.getStartTime());
//
//				rs      = pstmnt.executeQuery();
//				while (rs.next())
//				{
//					session.addAllSamples(rs.getTimestamp("SessionSampleTime"));
//				}
//				rs.close();
//				pstmnt.close();
//				
//				session.makeBabies();
//			}
//			
//			// Debug print the tree...
//			printTree();
//		}
//		catch (SQLException e)
//		{
//			_logger.error("Problems inititialize...", e);
//		}
//	}
	public Object getChild(Object parent, int index) 
	{
		_logger.debug("getChildCount(parent='"+parent+"', index='"+index+"')");
		if (parent instanceof List)
		{
			return _sessions.get(index);
		}
		if (parent instanceof SessionLevel)
		{
//			return parent.get;
			return ((SessionLevel) parent).getChild(index);
		}
		return null;
	}

	public int getChildCount(Object parent) 
	{
		_logger.debug("getChildCount(parent='"+parent+"')");
		if (parent instanceof SessionLevel)
		{
			return ((SessionLevel) parent).getChildCount();
		}
		if (parent instanceof List)
		{
			return _sessions.size();
		}

		return 0;
	}

	public int getColumnCount() 
	{
		return 5;
	}

	public String getColumnName(int column) 
	{
		_logger.debug("getColumnName(column='"+column+"')");
		switch (column) 
		{
		case 0:  return "Sessions";
		case 1:  return "Start Time";
		case 2:  return "End Time";
		case 3:  return "Duration";
		case 4:  return "Samples";
		default: return super.getColumnName(column);
		}
	}

	public Class<?> getColumnClass(int column) 
	{
		_logger.debug("getColumnClass(column='"+column+"')");
		switch (column) 
		{
		case 0:  return String.class;
		case 1:  return String.class;
		case 2:  return String.class;
		case 3:  return String.class;
		case 4:  return Integer.class;
		default: return super.getColumnClass(column);
		}
	}

	public Object getValueAt(Object node, int column) 
	{
		_logger.debug("getValueAt(node='"+node+"', column='"+column+"')");
		if (node instanceof SessionLevel) 
		{
			SessionLevel rec = (SessionLevel) node;
			switch (column) 
			{
			case 0: return rec;
//			case 0: return "123456789-123456789-123456789-12345";
			case 1: return rec.getStartTime() != null ? _sdf.format(rec.getStartTime()) : "none";
			case 2: return rec.getEndTime()   != null ? _sdf.format(rec.getEndTime())   : "none";
			case 3: return rec.getDuration();
			case 4: return new Integer(rec.getDisplayChildCount());
			}
		}

		if (node instanceof Timestamp && column == 0) 
		{
			switch (column) 
			{
			case 0: return node;
			case 1: return node;
			case 2: return "-";
			case 3: return "1 sample";
			case 4: return null;
			}
		}
		return null;
	}

//public int getColumnCount();
//public Object getValueAt(Object node, int column);
//public Object getChild(Object parent, int index);
//public int getChildCount(Object parent);
//public int getIndexOfChild(Object parent, Object child);
//public boolean isLeaf(Object node);

	public int getIndexOfChild(Object parent, Object child) 
	{
		_logger.debug("getIndexOfChild(parent='"+parent+"', child='"+child+"'.)");
		if (parent instanceof SessionLevel && child instanceof SessionLevel) 
		{
		}
//		if (parent instanceof File && child instanceof File) 
//		{
//			File parentFile = (File) parent;
//			File[] files = parentFile.listFiles();
//
//			Arrays.sort(files);
//
//			for (int i = 0, len = files.length; i < len; i++) 
//			{
//				if (files[i].equals(child)) {
//					return i;
//				}
//			}
//		}

		return -1;
	}

	public Object getRoot() 
	{
		_logger.debug("getRoot()");
//		return _sessions.get(0);
//		return null;
		return _sessions;
	}

//	public boolean isLeaf(Object node) 
//	{
//		if (node instanceof File) 
//		{
//			//do not use isFile(); some system files return false
//			return ((File) node).list() == null;
//		}
//
//		return true;
//	}
	public boolean isLeaf(Object node) 
	{
		_logger.debug("isLeaf(node='"+node+"')");

		if (node instanceof SessionLevel) 
			return false;
		if (node instanceof List)
			return false;

		return true;
	}
	

	
	
	///////////////////////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////
	// ---- SUB CLASSES ---- SUB CLASSES ---- SUB CLASSES ---- SUB CLASSES ----
	///////////////////////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////
	
	protected class SessionLevel
	{
		Timestamp _sampleId        = null;
		Timestamp _periodStartTime = null;
		Timestamp _periodEndTime   = null;
		Timestamp _startTime       = null;
		Timestamp _endTime         = null;
		int       _numOfSamples    = 0;
		String    _duration        = "";
		ArrayList<Object>    _children   = new ArrayList<Object>();
		ArrayList<Timestamp> _allSamples = new ArrayList<Timestamp>();
		
		SessionLevel(){}
		SessionLevel(Timestamp sampleId, Timestamp startTime, Timestamp endTime, int numOfSamples)
		{
			setSampleId(sampleId);
			setStartTime(startTime);
			setEndTime(endTime);
			setDisplayChildCount(numOfSamples);
			setDuration();
			_logger.debug("new SessionLevel(startTime='"+_startTime+"', endTime='"+_endTime+"', duration='"+_duration+"', numOfSamples='"+_numOfSamples+"')");
		}


		public Timestamp getSampleId()          { return _sampleId; }
		public Timestamp getStartTime()         { return _startTime; }
		public Timestamp getEndTime()           { return _endTime;   }
		public int       getDisplayChildCount() { return _numOfSamples; }
		public String    getDuration()          { return _duration; }

		public void setSampleId(Timestamp sampleId)   { _sampleId  = sampleId; }
		public void setStartTime(Timestamp startTime) { _startTime = startTime; setPeriodStartTime(); }
		public void setEndTime  (Timestamp endTime)   { _endTime   = endTime; setDuration(); setDisplayChildCount(); }
		public void setDisplayChildCount(int count)   { _numOfSamples = count;  }
		public void setDisplayChildCount()
		{
			_numOfSamples = 0;
			for (Iterator<Object> it = _children.iterator(); it.hasNext();) 
			{
				Object o = it.next();
				if (o instanceof SessionLevel) 
				{
					SessionLevel s = (SessionLevel) o;
					s.setDisplayChildCount();
					_numOfSamples += s.getDisplayChildCount();
				}
				else
				{
					_numOfSamples++;
				}
			}
		}
		public void setDuration()
		{
			if (_startTime != null && _endTime != null)
				_duration = TimeUtils.msToTimeStr(getDurationFormatstr(), _endTime.getTime() - _startTime.getTime()); 
			else
				_duration = "";
		}
		public String getDurationFormatstr()  { return "%HH:%MM:%SS"; }

		public String getDisplayString()      { return getDuration(); }

		public void setPeriodStartTime()      { _periodStartTime = _startTime; }
		public void setPeriodEndTime()        { _periodEndTime   = _endTime; }
		public Timestamp getPeriodStartTime() { return _periodStartTime != null ? _periodStartTime : _startTime; }
		public Timestamp getPeriodEndTime()   { return _periodEndTime   != null ? _periodEndTime   : _endTime; }

//		private void addAllSamples(Timestamp sample)
//		{
//			_allSamples.add(sample);
//		}
		private void addAllSamples(List<Timestamp> sampleList)
		{
			_allSamples.addAll(sampleList);
		}

		public void addChild(SessionLevel level) { _children.add(level); }
//		public void addChild(DayLevel     level) { _children.add(level); }
//		public void addChild(HourLevel    level) { _children.add(level); }
//		public void addChild(MinuteLevel  level) { _children.add(level); }
		public void addChild(Timestamp sample)   { _children.add(sample);}


		private void makeBabies()
		{
			DayLevel    dayLevel    = new DayLevel();
			HourLevel   hourLevel   = new HourLevel();
			MinuteLevel minuteLevel = new MinuteLevel();

			if (_allSamples.size() == 0)
				return;
			Timestamp firstTs = (Timestamp)_allSamples.get(0);

			dayLevel   .setSampleId(_sampleId);
			hourLevel  .setSampleId(_sampleId);
			minuteLevel.setSampleId(_sampleId);

			dayLevel   .setStartTime(firstTs);
			hourLevel  .setStartTime(firstTs);
			minuteLevel.setStartTime(firstTs);

			this     .addChild(dayLevel);
			dayLevel .addChild(hourLevel);
			hourLevel.addChild(minuteLevel);
			
			for (int i=0; i<_allSamples.size(); i++)
			{
				Timestamp prevTs = (Timestamp)_allSamples.get( i>0 ? i-1 : 0  );
				Timestamp currTs = (Timestamp)_allSamples.get(i);

				// Check if we need to create new periods
				if( ! dayLevel.isWithinPeriod(currTs) )
				{
					dayLevel.setEndTime(prevTs);

					dayLevel = new DayLevel();
					dayLevel.setSampleId(_sampleId);
					dayLevel.setStartTime(currTs);

					this.addChild(dayLevel); // this = SessionLevel
				}
				if( ! hourLevel.isWithinPeriod(currTs) )
				{
					hourLevel.setEndTime(prevTs);

					hourLevel = new HourLevel();
					hourLevel.setSampleId(_sampleId);
					hourLevel.setStartTime(currTs);

					dayLevel.addChild(hourLevel);
				}
				if( ! minuteLevel.isWithinPeriod(currTs) )
				{
					minuteLevel.setEndTime(prevTs);

					minuteLevel = new MinuteLevel();
					minuteLevel.setSampleId(_sampleId);
					minuteLevel.setStartTime(currTs);

					hourLevel.addChild(minuteLevel);
				}

				// Add current sample to the minute level
				minuteLevel.addChild( currTs );
			}

			Timestamp lastTs  = (Timestamp)_allSamples.get(_allSamples.size()-1);

			minuteLevel.setEndTime(lastTs);
			hourLevel  .setEndTime(lastTs);
			dayLevel   .setEndTime(lastTs);
		}

		public int getChildCount() 
		{
			return _children.size();
		}

		public Object getChild(int index)
		{
			if (index < _children.size())
			{
				Object o = _children.get(index);
				if (o == null)
				{
					return "-----NULL----";
				}
				else
				{
					return o;
				}
			}
			return "Problems: index="+index+", _children.size()="+_children.size();
		}

		public String toString()
		{
			return("SessionLevel(startTime='"+_startTime+"', endTime='"+_endTime+"', periodStartTime='"+_periodStartTime+"', periodEndTime='"+_periodEndTime+"', numOfSamples='"+_numOfSamples+"'");
		}

		/**
		 * checks if the passed Timestamp should be within this sample or if we need to create a new one.
		 * @param ts 
		 * @return false if we need a new one, true if it should be within this one.
		 */
		public boolean isWithinPeriod(Timestamp ts)
		{
			return true;
		}
	}



	protected class DayLevel
	extends SessionLevel
	{
		private int _atDay = 0;

		DayLevel(){}
//		DayLevel(Timestamp startTime, Timestamp endTime, int numOfSamples)
//		{
//			super(startTime, endTime, numOfSamples);
//			_logger.debug("new DayLevel(startTime='"+_startTime+"', lastSampleTime='"+_endTime+"', numOfSamples='"+_numOfSamples+"')");
//		}

		public String toString()
		{
			return("DayLevel(startTime='"+_startTime+"', endTime='"+_endTime+"', periodStartTime='"+_periodStartTime+"', periodEndTime='"+_periodEndTime+"', numOfSamples='"+_numOfSamples+"'");
		}

		public String getDisplayString()
		{
			return "Day " + _atDay * _dayLevelCount;
		}

		public void setPeriodStartTime()
		{
			Calendar cal = GregorianCalendar.getInstance();
			cal.setTimeInMillis(_startTime.getTime());

			_atDay = cal.get(Calendar.DAY_OF_MONTH) / _dayLevelCount;

			cal.set(Calendar.MILLISECOND, 0);
			cal.set(Calendar.SECOND,      0);
			cal.set(Calendar.MINUTE,      0);
			cal.set(Calendar.HOUR_OF_DAY, 0);
			cal.set(Calendar.DAY_OF_MONTH, _atDay * _dayLevelCount);
			_periodStartTime = new Timestamp(cal.getTimeInMillis());

//			cal.roll(Calendar.DAY_OF_MONTH, _dayLevelCount);
//			_periodEndTime = new Timestamp(cal.getTimeInMillis());
			_periodEndTime = new Timestamp(cal.getTimeInMillis() + ((1000*60*60*24)*_dayLevelCount));
		}
		
		public boolean isWithinPeriod(Timestamp ts)
		{
			long days = (ts.getTime() - _periodStartTime.getTime()) / 1000 / 60 / 60 / 24;
			return (days < _dayLevelCount);
		}
	}

	
	protected class HourLevel
	extends SessionLevel
	{
		private int _atHour = 0;

		HourLevel(){}
//		HourLevel(Timestamp startTime, Timestamp endTime, int numOfSamples)
//		{
//			super(startTime, endTime, numOfSamples);
//			_logger.debug("new HourLevel(startTime='"+_startTime+"', lastSampleTime='"+_endTime+"', numOfSamples='"+_numOfSamples+"')");
//		}

		public String toString()
		{
			return("HourLevel(startTime='"+_startTime+"', endTime='"+_endTime+"', periodStartTime='"+_periodStartTime+"', periodEndTime='"+_periodEndTime+"', numOfSamples='"+_numOfSamples+"'");
		}

		public String getDisplayString()
		{
			if (_hourLevelCount == 1)
				return "Hour " + _atHour * _hourLevelCount;
			else
				return "Hour " + _atHour * _hourLevelCount + " - " + (_atHour +  1) * _hourLevelCount;
		}

		public void setPeriodStartTime()
		{
			Calendar cal = GregorianCalendar.getInstance();
			cal.setTimeInMillis(_startTime.getTime());

			_atHour = cal.get(Calendar.HOUR_OF_DAY) / _hourLevelCount;

			cal.set(Calendar.MILLISECOND, 0);
			cal.set(Calendar.SECOND, 0);
			cal.set(Calendar.MINUTE, 0);
			cal.set(Calendar.HOUR_OF_DAY, _atHour * _hourLevelCount);
			_periodStartTime = new Timestamp(cal.getTimeInMillis());

//			cal.roll(Calendar.HOUR_OF_DAY, _hourLevelCount);
//			_periodEndTime = new Timestamp(cal.getTimeInMillis());
			_periodEndTime = new Timestamp(cal.getTimeInMillis() + ((1000*60*60)*_hourLevelCount));
		}

		public boolean isWithinPeriod(Timestamp ts)
		{
			long hours = (ts.getTime() - _periodStartTime.getTime()) / 1000 / 60 / 60;
			return (hours < _hourLevelCount);
		}
	}

	
	protected class MinuteLevel
	extends SessionLevel
	{
		private int _atHour   = 0;
		private int _atMinute = 0;

		MinuteLevel(){}
//		MinuteLevel(Timestamp startTime, Timestamp endTime, int numOfSamples)
//		{
//			super(startTime, endTime, numOfSamples);
//			_logger.debug("new MinuteLevel(startTime='"+_startTime+"', lastSampleTime='"+_endTime+"', numOfSamples='"+_numOfSamples+"')");
//		}

		public String toString()
		{
			return("MinuteLevel(startTime='"+_startTime+"', endTime='"+_endTime+"', periodStartTime='"+_periodStartTime+"', periodEndTime='"+_periodEndTime+"', numOfSamples='"+_numOfSamples+"'");
		}

		public String getDisplayString()
		{
			if (_hourLevelCount == 1)
				return             "Minute " + _atMinute * _minuteLevelCount + " - " + (_atMinute +  1) * _minuteLevelCount;
			else
				return _atHour + ", Minute " + _atMinute * _minuteLevelCount + " - " + (_atMinute +  1) * _minuteLevelCount;
		}

		public void setPeriodStartTime()
		{
			Calendar cal = GregorianCalendar.getInstance();
			cal.setTimeInMillis(_startTime.getTime());

			_atHour   = cal.get(Calendar.HOUR_OF_DAY);
			_atMinute = cal.get(Calendar.MINUTE) / _minuteLevelCount;

			cal.set(Calendar.MILLISECOND, 0);
			cal.set(Calendar.SECOND, 0);
			cal.set(Calendar.MINUTE, _atMinute * _minuteLevelCount);
			_periodStartTime = new Timestamp(cal.getTimeInMillis());

			//			cal.roll(Calendar.MINUTE, _minuteLevelCount);
//			_periodEndTime = new Timestamp(cal.getTimeInMillis());
			_periodEndTime = new Timestamp(cal.getTimeInMillis() + ((1000*60)*_minuteLevelCount));
		}

		public boolean isWithinPeriod(Timestamp ts)
		{
			long minutes = (ts.getTime() - _periodStartTime.getTime()) / 1000 / 60;
			return (minutes < _minuteLevelCount);
		}
	}
	
}
