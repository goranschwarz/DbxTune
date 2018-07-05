package com.asetune.alarm;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import com.asetune.alarm.events.AlarmEvent;


/**
 * This class will probably be collapsed into the AlarmHandler class.
 * It do not give us anything for the moment...
 * 
 * @author qgoschw
 *
 */
public class AlarmContainer
implements Serializable
{
	private static Logger _logger          = Logger.getLogger(AlarmContainer.class);
	private static final long serialVersionUID = 1L;

	/** a list of: IAlarmEvent */
	private List<AlarmEvent> _alarms = new LinkedList<AlarmEvent>();


	public AlarmContainer() 
	{
	}

//	/** Take another AlarmContainer and populate this one, but only save the entries that has not yet expired */
//	public AlarmContainer(AlarmContainer alarmCont)
//	{
//		for (AlarmEvent alarmEvent : alarmCont.getAlarmList())
//		{
//			if ( ! alarmEvent.hasTimeToLiveExpired() )
//				add(alarmEvent);
//		}
//	}

	public void add(AlarmEvent alarmEvent)
	{
		// If the alarm already exists... 
		while(_alarms.remove(alarmEvent)); // First remove old/last alarm (so we can add the new, with a newer timestamp)
		_alarms.add(alarmEvent);
	}

	public void handleReRaise(AlarmEvent repeatedAlarmEvent)
	{
		if (repeatedAlarmEvent == null)
			return;

		int pos = _alarms.indexOf(repeatedAlarmEvent);
		if (pos != -1)
		{
			AlarmEvent existing = _alarms.get(pos);
			existing.incrementReRaiseCount();
			existing.setReRaiseTime(); // existing.setReRaiseTime(repeatedAlarmEvent.getCrTime());
			
			repeatedAlarmEvent.setReRaiseCount(existing.getReRaiseCount());

			existing.setReRaiseDescription        (repeatedAlarmEvent.getDescription());
			existing.setReRaiseExtendedDescription(repeatedAlarmEvent.getExtendedDescription());
			existing.setReRaiseData               (repeatedAlarmEvent.getData());
		}
	}
	
	public boolean contains(AlarmEvent alarmEvent)
	{
		return _alarms.contains(alarmEvent);
	}

	public void remove(AlarmEvent alarmEvent)
	{
		// Remove ALL entries (while=if there are more than one)
		while(_alarms.remove(alarmEvent));
	}

	public void clear()
	{
		_alarms.clear();
	}
	
	public int size()
	{
		return _alarms.size();
	}

	public List<AlarmEvent> getAlarmList()
	{
		return _alarms;
	}

//	public void removeExpiredAlarms()
//	{
//		List<AlarmEvent> list = getAlarmList();
//		for (AlarmEvent alarmEvent : list)
//		{
//			if ( alarmEvent.hasTimeToLiveExpired() )
//			{
//				_logger.debug("removeExpiredAlarms(): Removing: "+alarmEvent);
//				list.remove(alarmEvent);
//			}
//		}
//	}

	public List<AlarmEvent> getCancelList(AlarmContainer currentScanAlarms)
	{
		if ( _logger.isDebugEnabled() )
		{
			for (AlarmEvent alarmEvent : _alarms)
				_logger.debug("Active--Alarms: "+alarmEvent);
			if (_alarms.size() == 0)
				_logger.debug("Active--Alarms: IS EMPTY");

			for (AlarmEvent alarmEvent : currentScanAlarms._alarms)
				_logger.debug("CurrentScan---Alarms: "+alarmEvent);
			if (currentScanAlarms._alarms.size() == 0)
				_logger.debug("CurrentScan---Alarms: IS EMPTY");
		}
		
		///////////////////
		// Do the work
		///////////////////
		
		List<AlarmEvent> cancelList = new LinkedList<>();
//		Set<AlarmEvent> cancelList = new HashSet<>();
		
		// Copy alarms from the Active alarm list -> cancelList
		// - that is NOT part of the currectScan (overlapping alarms in active and current scan should NOT be canceled)
		// - also copy alarms that has expired (If time to live > 0, this means that means that the alarm will survive several "scans")
		for (AlarmEvent ae : _alarms)
		{
//System.out.println("+++++ getCancelList(): addToCancelList="+(! currentScanAlarms.contains(ae) && ae.hasTimeToLiveExpired())+": NotInThisScan="+!currentScanAlarms.contains(ae)+", expired="+ae.hasTimeToLiveExpired()+", alarmEven="+ae);
			if ( ! currentScanAlarms.contains(ae) && ae.hasTimeToLiveExpired())
					cancelList.add(ae);
		}
		
		
//		// First **copy** all alarm from the Saved/History
//		List<AlarmEvent> cancelList = new LinkedList<AlarmEvent>(_alarms);
//
//		// remove alarms that has been raised in the last/current Scan (repeted alarms in current scan should NOT be canceled)
//		cancelList.removeAll(currentScanAlarms._alarms);
		

		if ( _logger.isDebugEnabled() )
		{
			for (AlarmEvent alarmEvent : cancelList)
				_logger.debug("Cancel-Alarms: "+alarmEvent);
			if (cancelList.size() == 0)
				_logger.debug("Cancel-Alarms: IS EMPTY");
		}

		return cancelList;
	}
	
	public void save(String filename)
	throws FileNotFoundException, IOException
	{
		FileOutputStream fos = new FileOutputStream(filename);
		ObjectOutputStream oos = new ObjectOutputStream(fos);
		
		oos.writeObject(this);
		
		oos.flush();
		oos.close();
		fos.close();

		// To do the below I need to make a JavaBean out of the class
		//XMLEncoder e = new XMLEncoder( new BufferedOutputStream( new FileOutputStream(filename+".xml")));
		//e.writeObject(this);
		////e.writeObject(new JButton("Hello, world"));
		//e.close();
	}

	public static AlarmContainer load(String filename)
	throws FileNotFoundException, IOException, ClassNotFoundException
	{
		FileInputStream fis = new FileInputStream(filename);
		ObjectInputStream ois = new ObjectInputStream(fis);

		AlarmContainer ac = (AlarmContainer) ois.readObject();

		ois.close();
		fis.close();

		// To do the below I need to make a JavaBean out of the class
		//XMLDecoder e = new XMLDecoder( new BufferedInputStream( new FileInputStream(filename+".xml")));
		//AlarmContainer xac = (AlarmContainer) e.readObject();
		//e.close();

		if ( _logger.isDebugEnabled() )
		{
			for (AlarmEvent alarmEvent : ac._alarms)
				_logger.debug("Restored--Alarm: "+alarmEvent);
		}

		return ac;
	}
}
