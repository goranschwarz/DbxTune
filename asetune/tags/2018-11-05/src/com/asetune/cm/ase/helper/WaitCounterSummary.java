package com.asetune.cm.ase.helper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.asetune.cm.CountersModel;
import com.asetune.gui.MainFrame;
import com.asetune.gui.ParameterDialog;
import com.asetune.utils.Configuration;
import com.asetune.utils.StringUtil;
import com.asetune.utils.SwingUtils;

public class WaitCounterSummary
{
	public enum Type {WaitTime, Waits, WaitTimePerWait};
	
	private Map<Integer, WaitCounterEntry> _eventEntries = new LinkedHashMap<Integer, WaitCounterEntry>();
	private Map<String,  WaitCounterEntry> _classEntries = new LinkedHashMap<String,  WaitCounterEntry>();
	
	public static WaitCounterSummary create(CountersModel cm, List<Integer> skipWaitEventIdList, List<String> skipWaitEventClassList, List<String> skipUserNameList, boolean skipSystemSpids)
	{
//System.out.println("SKIP.... skipWaitEventIdList:    "+skipWaitEventIdList);
//System.out.println("SKIP.... skipWaitEventClassList: "+skipWaitEventClassList);
//System.out.println("SKIP.... skipUserNameList:       "+skipUserNameList);
//System.out.println("SKIP.... skipSystemSpids:        "+skipSystemSpids);
		
		List<String>       cols = cm.getColNames      (CountersModel.DATA_RATE);
		List<List<Object>> data = cm.getDataCollection(CountersModel.DATA_RATE);
		
		if (data == null || cols == null)
			return null;

		// Get column positions
		int UserName_pos        = cols.indexOf("UserName");
		int ClassName_pos       = cols.indexOf("WaitClassDesc");
		int EventName_pos       = cols.indexOf("WaitEventDesc");
		int WaitEventID_pos     = cols.indexOf("WaitEventID");
		int WaitTime_pos        = cols.indexOf("WaitTime"); 
		int Waits_pos           = cols.indexOf("Waits");
		int WaitTimePerWait_pos = cols.indexOf("WaitTimePerWait"); 

		if (ClassName_pos       < 0) throw new RuntimeException("Column 'ClassName' is not available in the RATE dataset.");
		if (EventName_pos       < 0) throw new RuntimeException("Column 'EventName' is not available in the RATE dataset.");
		if (WaitEventID_pos     < 0) throw new RuntimeException("Column 'WaitEventID' is not available in the RATE dataset.");
		if (WaitTime_pos        < 0) throw new RuntimeException("Column 'WaitTime' is not available in the RATE dataset.");
		if (Waits_pos           < 0) throw new RuntimeException("Column 'Waits_pos' is not available in the RATE dataset.");
		if (WaitTimePerWait_pos < 0) throw new RuntimeException("Column 'WaitTimePerWait' is not available in the RATE dataset.");

		// Create the object which will be returned
		WaitCounterSummary wcs = new WaitCounterSummary();

		// Loop rows
		for (List<Object> row : data)
		{
			String UserName        = null; // "introduced in ASE 15.0.2 ESD#3";
			String ClassName       = (String)row.get(ClassName_pos);
			String EventName       = (String)row.get(EventName_pos);
			Number WaitEventID     = (Number)row.get(WaitEventID_pos);
			Number WaitTime        = (Number)row.get(WaitTime_pos);
			Number Waits           = (Number)row.get(Waits_pos);
			Number WaitTimePerWait = (Number)row.get(WaitTimePerWait_pos);

			// Check SKIP List: ID
			if (skipWaitEventIdList != null)
			{
				boolean skipThisEvent = false;
				for (Integer skipThisWaitId : skipWaitEventIdList)
				{
					if ( WaitEventID.intValue() == skipThisWaitId.intValue() )
					{
						skipThisEvent = true;
						break;
					}
				}
				if (skipThisEvent)
					continue;
			}

			// Check SKIP List: Class
			if (skipWaitEventClassList != null)
			{
				boolean skipThisEvent = false;
				for (String skipThisWaitClass : skipWaitEventClassList)
				{
					if ( skipThisWaitClass.equalsIgnoreCase(ClassName))
					{
						skipThisEvent = true;
						break;
					}
				}
				if (skipThisEvent)
					continue;
			}

			// SKIP System Users/Threads
			// UserName was introduced in ASE 15.0.2 ESD#3, so if the column wasn't found don't get it 
			if (skipSystemSpids && UserName_pos > 0)
			{
				UserName = (String)row.get(UserName_pos);

				if ( StringUtil.isNullOrBlank(UserName) )
					continue;
			}

			// Check SKIP List: UserName
			// UserName was introduced in ASE 15.0.2 ESD#3, so if the column wasn't found don't do this
			if (skipUserNameList != null && UserName_pos >= 0)
			{
				boolean skipThisEvent = false;
				for (String skipThisUserName : skipUserNameList)
				{
					if ( skipThisUserName.equalsIgnoreCase(UserName))
					{
						skipThisEvent = true;
						break;
					}
				}
				if (skipThisEvent)
					continue;
			}

			// Add the counter
			wcs.add(WaitEventID, ClassName, EventName, WaitTime, Waits, WaitTimePerWait);
		}

		return wcs;
	}

//
// If we want both DIFF and RATE values to be added to the WaitCounterSummary
// Then it would probably be more correct to use index positioning instead of iterator on the getDataCollection()
// 
//	private WaitCounterSummary createWaitCounterSummaryXXX()
//	{
//		
//		CounterTableModel data = getCounterDataRate();
//		if (data == null)
//			return null;
//
//		// Get column positions
//		int UserName_pos        = data.findColumn("UserName");
//		int ClassName_pos       = data.findColumn("WaitClassDesc");
//		int EventName_pos       = data.findColumn("WaitEventDesc");
//		int WaitEventID_pos     = data.findColumn("WaitEventID");
//		int WaitTime_pos        = data.findColumn("WaitTime"); 
//		int Waits_pos           = data.findColumn("Waits");
//		int WaitTimePerWait_pos = data.findColumn("WaitTimePerWait"); 
//
//		// Create the object which will be returned
//		WaitCounterSummary wcs = new WaitCounterSummary();
//
//		// Loop rows
//		for(int r=0; r<getRowCount(); r++)
//		{
//			String UserName        = null; // "introduced in ASE 15.0.2 ESD#3";
//			String ClassName       = (String)data.getValueAt(r, ClassName_pos);
//			String EventName       = (String)data.getValueAt(r, EventName_pos);
//			Number WaitEventID     = (Number)data.getValueAt(r, WaitEventID_pos);
//			Number WaitTime        = (Number)data.getValueAt(r, WaitTime_pos);
//			Number Waits           = (Number)data.getValueAt(r, Waits_pos);
//			Number WaitTimePerWait = (Number)data.getValueAt(r, WaitTimePerWait_pos);
//			
//			// SKIP Wait EventId 250
//			if ( WaitEventID.intValue() == 250 )
//				continue;
//
//			// SKIP System Threads
//			// UserName was introduced in ASE 15.0.2 ESD#3, so if the column wasn't found don't get it 
//			if (UserName_pos > 0)
//				UserName = (String)getValueAt(r, UserName_pos);
//
//			if ( UserName != null && "".equals(UserName) )
//				continue;
//
//			// Add the counter
//			wcs.add(WaitEventID, ClassName, EventName, WaitTime, Waits, WaitTimePerWait);
//		}
//
//		return wcs;
//	}



	public int getEventIdSize()
	{
		return _eventEntries.size();
	}

	public int getClassNameSize()
	{
		return _classEntries.size();
	}

	public Map<Integer, WaitCounterEntry> getEventIdMap()
	{
		return _eventEntries;
	}

	public Map<String,  WaitCounterEntry> getClassNameMap()
	{
		return _classEntries;
	}

	public void add(Number WaitEventID, String ClassName, String EventName, Number WaitTime, Number Waits, Number WaitTimePerWait)
	{
		add(WaitEventID.intValue(), ClassName, EventName, WaitTime.doubleValue(), Waits.doubleValue(), WaitTimePerWait.doubleValue());
	}

	public void add(int WaitEventID, String ClassName, String EventName, double WaitTime, double Waits, double WaitTimePerWait)
	{
		// Skip values that are "empty" or "no counter values"
		if (WaitTime == 0 && Waits == 0 && WaitTimePerWait == 0)
			return;

		// EventId / Name
		WaitCounterEntry eventIdEntry = _eventEntries.get(WaitEventID);
		if (eventIdEntry != null)
		{
			eventIdEntry.add(WaitTime, Waits, WaitTimePerWait);
		}
		else
		{
			eventIdEntry = new WaitCounterEntry(WaitEventID, ClassName, EventName, WaitTime, Waits, WaitTimePerWait);
			_eventEntries.put(WaitEventID, eventIdEntry);
		}

		// ClassName
		WaitCounterEntry classEntry = _classEntries.get(ClassName);
		if (classEntry != null)
		{
			classEntry.add(WaitTime, Waits, WaitTimePerWait);
		}
		else
		{
			classEntry = new WaitCounterEntry(-1, ClassName, null, WaitTime, Waits, WaitTimePerWait);
			_classEntries.put(ClassName, classEntry);
		}
	}

	public void debugPrint()
	{
		for (Integer waitId : _eventEntries.keySet())
		{
			WaitCounterEntry wce = _eventEntries.get(waitId);
			
			System.out.println(
				"DEBUG EventID - WaitCounterEntry: ClassName='"+wce._ClassName+"', WaitEventID='"+wce._WaitEventID+"', EventName='"+wce._EventName+"'.\n" +
				"     WaitTime        Sum = " + wce.getSumWaitTime()              + "\n" +
				"     Waits           Sum = " + wce.getSumWaits()                 + "\n" +
				"     WaitTimePerWait Sum = " + wce.getSumWaitTimePerWait()       + "\n" + 
				"   --NUM_OF_ADDS ------------------- = " + wce.getNumberOfAdds() + "\n" +
				"     WaitTime        Avg = " + wce.getAvgWaitTime()              + "\n" +
				"     Waits           Avg = " + wce.getAvgWaits()                 + "\n" +
				"     WaitTimePerWait Avg = " + wce.getAvgWaitTimePerWait()       + "\n" + 
				"");
		}

		for (String className : _classEntries.keySet())
		{
			WaitCounterEntry wce = _classEntries.get(className);
			
			System.out.println(
				"DEBUG ClassName - WaitCounterEntry: ClassName='"+wce._ClassName+".\n" +
				"     WaitTime        Sum = " + wce.getSumWaitTime()              + "\n" +
				"     Waits           Sum = " + wce.getSumWaits()                 + "\n" +
				"     WaitTimePerWait Sum = " + wce.getSumWaitTimePerWait()       + "\n" + 
				"   --NUM_OF_ADDS ------------------- = " + wce.getNumberOfAdds() + "\n" +
				"     WaitTime        Avg = " + wce.getAvgWaitTime()              + "\n" +
				"     Waits           Avg = " + wce.getAvgWaits()                 + "\n" +
				"     WaitTimePerWait Avg = " + wce.getAvgWaitTimePerWait()       + "\n" + 
				"");
		}
	}


	public class WaitCounterEntry
	{
		private int    _WaitEventID     = -1;
		private String _ClassName       = null;
		private String _EventName       = null;
		private double _WaitTime        = 0;
		private double _Waits           = 0;
		private double _WaitTimePerWait = 0;
		
		private int    _numberOfAdds    = 0;

		public int    getWaitEventID()        { return _WaitEventID; }
		public String getClassName()          { return _ClassName; }
		public String getEventNameLabel()     { return "["+_WaitEventID+"] " + _EventName; }
		public String getEventName()          { return _EventName; }

		public int    getNumberOfAdds()       { return _numberOfAdds; }

		public double getSumWaitTime()        { return _WaitTime; }
		public double getSumWaits()           { return _Waits; }
		public double getSumWaitTimePerWait() { return _WaitTimePerWait; }
		
		public double getAvgWaitTime()        { return _WaitTime        / _numberOfAdds; }
		public double getAvgWaits()           { return _Waits           / _numberOfAdds; }
		public double getAvgWaitTimePerWait() { return _WaitTimePerWait / _numberOfAdds; }
		
		public WaitCounterEntry(int WaitEventID, String ClassName, String EventName, double WaitTime, double Waits, double WaitTimePerWait)
		{
//System.out.println("----CounterEntry----NEW---: ClassName='"+_ClassName+"', EventName='"+_EventName+"', WaitEventID='"+_WaitEventID+"' ---- WaitTime="+_WaitTime+", Waits="+_Waits+", WaitTimePerWait="+_WaitTimePerWait+", NUM_OF_ADDS="+_numberOfAdds);
			_WaitEventID     = WaitEventID;
			_ClassName       = ClassName;
			_EventName       = EventName;
			
			add(WaitTime, Waits, WaitTimePerWait);
		}

//		public void add(Number WaitEventID, String ClassName, String EventName, Number WaitTime, Number Waits, Number WaitTimePerWait)
//		{
//		}
		public void add(double WaitTime, double Waits, double WaitTimePerWait)
		{
//System.out.println("----CounterEntry-ADD-START: ClassName='"+_ClassName+"', EventName='"+_EventName+"', WaitEventID='"+_WaitEventID+"' ---- WaitTime="+_WaitTime+", Waits="+_Waits+", WaitTimePerWait="+_WaitTimePerWait+", NUM_OF_ADDS="+_numberOfAdds+1);
			_WaitTime        = _WaitTime        + WaitTime;
			_Waits           = _Waits           + Waits;
			_WaitTimePerWait = _WaitTimePerWait + WaitTimePerWait;

			_numberOfAdds++;
//System.out.println("----CounterEntry-ADD--END-: ClassName='"+_ClassName+"', EventName='"+_EventName+"', WaitEventID='"+_WaitEventID+"' ---- WaitTime="+_WaitTime+", Waits="+_Waits+", WaitTimePerWait="+_WaitTimePerWait+", NUM_OF_ADDS="+_numberOfAdds);
		}
	}


	public static void openDataSourceDialog(String propkey, String defaultValue)
	{
		final Configuration tmpConf = Configuration.getInstance(Configuration.USER_TEMP);

		String key1 = "Data Source Column Name("+Type.WaitTime+", "+Type.Waits+", "+Type.WaitTimePerWait+")";

		LinkedHashMap<String, String> in = new LinkedHashMap<String, String>();
		in.put(key1, Configuration.getCombinedConfiguration().getProperty(propkey, defaultValue));

		Map<String,String> results = ParameterDialog.showParameterDialog(MainFrame.getInstance(), "Data Source Column", in, false);

		if (results != null)
		{
			String newValue = results.get(key1);

			// Check if we can use the value
			try { Type.valueOf(newValue); }
			catch (Throwable t) 
			{
				String msg = 
						"<html>Problems converting value '<code>"+newValue+"</code>' into a Type.<br>" +
						"I will use the default value '<code>"+defaultValue+"</code>' instead.<br>" +
						"<br>" +
						"Please open the dialog and try again.<br>" +
						"</html>";
				SwingUtils.showErrorMessage(MainFrame.getInstance(), "Problems Converting value", msg, t);
				newValue = defaultValue;
			}

			tmpConf.setProperty(propkey, newValue);

			tmpConf.save();
		}
	}	
}
