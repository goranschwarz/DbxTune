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
package com.dbxtune.gui;

import java.lang.invoke.MethodHandles;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdesktop.swingx.treetable.AbstractTreeTableModel;

import com.dbxtune.CounterController;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.pcs.PersistReader;
import com.dbxtune.pcs.PersistReader.CmCounterInfo;
import com.dbxtune.pcs.PersistReader.CmNameSum;
import com.dbxtune.pcs.PersistReader.SampleCmCounterInfo;
import com.dbxtune.pcs.PersistReader.SessionInfo;
import com.dbxtune.utils.StringUtil;
import com.dbxtune.utils.TimeUtils;


/**
 * A tree table model to simulate a off line sampled dbxtune sessions.
 * <p>
 * Describe more.
 * 
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
public class OfflineSessionModel 
//extends DefaultTreeTableModel
extends AbstractTreeTableModel
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	
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
	public OfflineSessionModel(boolean showCmCounterInfoColumns, boolean showCmCounterRowsAsSamples) 
	{
		super();
		_showCmCounterInfoColumns   = showCmCounterInfoColumns;
		_showCmCounterRowsAsSamples = showCmCounterRowsAsSamples; 
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

//	/**
//	 * Get a Object path to a specific sampleTime in the tree.
//	 * @param ts
//	 * @return
//	 */
//	public TreePath getTreePathForSampleTime(Timestamp ts)
//	{
//		TreePath treePath = null;
//		for (SessionLevel sl : _sessions)
//		{
//			if (ts.getTime() >= sl._startTime.getTime()  && ts.getTime() <= sl._endTime.getTime())
//			{
//				_logger.debug("#### FOUND: "+sl);
//				treePath = new TreePath(sl);
//				treePath = getTreePathForSampleTime(treePath, sl, ts);
//			}
//		}
//
//		return treePath;
//	}
//	private TreePath getTreePathForSampleTime(TreePath treePath, SessionLevel sl, Timestamp ts)
//	{
//		for (int i=0; i<sl.getChildCount(); i++)
//		{
//			Object o = sl.getChild(i);
//			if (o instanceof SessionLevel)
//			{
//				SessionLevel child = (SessionLevel)o;
//				if (ts.getTime() >= child._startTime.getTime()  && ts.getTime() <= child._endTime.getTime())
//				{
//					if (child.getChildCount() > 0)
//					{
//						_logger.debug(" >>> FOUND: "+child);
//						treePath = treePath.pathByAddingChild(child);
//						getTreePathForSampleTime(treePath, child, ts);
//					}
//				}
//			}
//			else if (o instanceof Timestamp)
//			{
//				Timestamp sampleTime = (Timestamp)o;
//				if (sampleTime.getTime() == ts.getTime())
//				{
//					_logger.debug("  == FOUND: "+ts);
//					treePath = treePath.pathByAddingChild(sampleTime);
//				}
//			}
//		}
//		return treePath;
//	}
	/**
	 * Get a Object path to a specific sampleTime in the tree.
	 * @param ts
	 * @return
	 */
//	public TreePath getTreePathForSampleTime(Timestamp ts)
//	{
//		ArrayList<Object> treePath = new ArrayList<Object>();
//		for (SessionLevel sl : _sessions)
//		{
//			if (ts.getTime() >= sl._startTime.getTime()  && ts.getTime() <= sl._endTime.getTime())
//			{
//				_logger.debug("#### FOUND: "+sl);
//				treePath.add(new TreePath(sl));
//				getTreePathForSampleTime(treePath, sl, ts);
//			}
//		}
//
//		Object tpa[] = new Object[treePath.size()];
////		Object xxx[] = treePath.toArray(tpa);
//		return new TreePath( treePath.toArray(tpa) );
////		TreePath tpa[] = new TreePath[treePath.size()];
////		return treePath.toArray(tpa);
//	}
//	private void getTreePathForSampleTime(ArrayList<Object> treePath, SessionLevel sl, Timestamp ts)
//	{
//		for (int i=0; i<sl.getChildCount(); i++)
//		{
//			Object o = sl.getChild(i);
//			if (o instanceof SessionLevel)
//			{
//				SessionLevel child = (SessionLevel)o;
//				if (ts.getTime() >= child._startTime.getTime()  && ts.getTime() <= child._endTime.getTime())
//				{
//					if (child.getChildCount() > 0)
//					{
//						_logger.debug(" >>> FOUND: "+child);
//						treePath.add(new TreePath(child));
//						getTreePathForSampleTime(treePath, child, ts);
//					}
//				}
//			}
//			else if (o instanceof Timestamp)
//			{
//				Timestamp sampleTime = (Timestamp)o;
//				if (sampleTime.getTime() == ts.getTime())
//				{
//					_logger.debug("  == FOUND: "+ts);
//					treePath.add(new TreePath(sampleTime));
//				}
//			}
//		}
//	}
	
	/**
	 * Get a List object of a specific sampleTime.<br>
	 * The list will contain:<br>
	 * 0: SessionLevel (at Day Level)<br>
	 * 1: SessionLevel (at Hour Level)<br>
	 * 2: SessionLevel (at Minute Level)<br>
	 * 3: Timestamp (the actual sample time we were looking for)<br>
	 * <p>
	 * I couldn't figure out how the TreePath thing worked...
	 * @param ts the timestamp
	 * @return a SessionLevel object, which is the parent of the input Timestamp
	 */
	public List<Object> getObjectPathForSampleTime(Timestamp ts)
	{
		ArrayList<Object> objectPath = new ArrayList<Object>();
		for (SessionLevel sl : _sessions)
		{
			if (ts.getTime() >= sl._startTime.getTime()  && ts.getTime() <= sl._endTime.getTime())
			{
				_logger.debug("#### FOUND: "+sl);
				getObjectPathForSampleTime(objectPath, sl, ts);
			}
		}
		return objectPath;
	}
	private void getObjectPathForSampleTime(ArrayList<Object> objectPath, SessionLevel sl, Timestamp ts)
	{
		for (int i=0; i<sl.getChildCount(); i++)
		{
			Object o = sl.getChild(i);
			if (o instanceof SessionLevel)
			{
				SessionLevel child = (SessionLevel)o;
				if (ts.getTime() >= child._startTime.getTime()  && ts.getTime() <= child._endTime.getTime())
				{
					if (child.getChildCount() > 0)
					{
						_logger.debug(" >>> FOUND: "+child);
						objectPath.add(child);
						getObjectPathForSampleTime(objectPath, child, ts);
					}
				}
			}
			else if (o instanceof Timestamp)
			{
				Timestamp sampleTime = (Timestamp)o;
				if (sampleTime.getTime() == ts.getTime())
				{
					_logger.debug("  == FOUND: "+ts);
					objectPath.add(sampleTime);
				}
			}
		}
	}

	public void setDayLevelCount   (int count) { _dayLevelCount    = count; }
	public void setHourLevelCount  (int count) { _hourLevelCount   = count; }
	public void setMinuteLevelCount(int count) { _minuteLevelCount = count; }

	public int getDayLevelCount   () { return _dayLevelCount; }
	public int getHourLevelCount  () { return _hourLevelCount; }
	public int getMinuteLevelCount() { return _minuteLevelCount; }


//BEGIN: move this to a better place
	private boolean _showCmCounterRowsAsSamples = true;
	private boolean _showCmCounterInfoColumns = false;
	private List<String> _cmShortNameSumCols = new ArrayList<String>();
	private List<String> _cmLongNameSumCols  = new ArrayList<String>();
	private void addCmNameSumCols(Map<String,CmNameSum> cmNameSumMap)
	{
		for (String cmName : cmNameSumMap.keySet())
		{
			if ( ! _cmShortNameSumCols.contains(cmName) )
				_cmShortNameSumCols.add(cmName);
		}
	}
	private void fixCmNameSumColsOrder()
	{
		List<String> spillList              = new ArrayList<String>(_cmShortNameSumCols);
		List<String> new_cmShortNameSumCols = new ArrayList<String>();
		List<String> new_cmLongNameSumCols  = new ArrayList<String>();

		// FIXME: maybe get the TAB ORDER instead...
		for (CountersModel cm : CounterController.getInstance().getCmList())
		{
			String shortCmName = cm.getName();
			String longCmName  = cm.getDisplayName();

			if (spillList.contains(shortCmName))
			{
				spillList.remove(shortCmName);

				new_cmShortNameSumCols.add(shortCmName);
				new_cmLongNameSumCols .add(longCmName);
			}
		}
		// Add any names that are still in the "spill" list
		// meaning: they couldn't be found in the GetCounters.getCmList()
		for (String spill : spillList)
		{
			new_cmShortNameSumCols.add(spill);
			new_cmLongNameSumCols .add(spill);
		}

		// Finally start to use the new list
		_cmShortNameSumCols = new_cmShortNameSumCols;
		_cmLongNameSumCols  = new_cmLongNameSumCols;

//		final AbstractTreeTableModel xxxtm = this;
//		Thread xxx = new Thread()
//		{
//			@Override
//			public void run()
//			{
//				while(true)
//				{
//					_dummyInt++;
//					for (TreeModelListener tml : xxxtm.getTreeModelListeners())
//					{
//						tml.treeNodesChanged( new TreeModelEvent(source, path));
//					}
////					xxxtm.setValueAt(value, node, column);
//					try { Thread.sleep(1000); }
//					catch (InterruptedException ignore) {}
//				}
//			}
//		};
//		xxx.setDaemon(true);
//		xxx.start();
	}

//	public void fireTreeNodesChanged(TreeNode changed)
//	{
//		LinkedList<TreeNode> list = new LinkedList<TreeNode>();
//		for (TreeNode n = changed; n != null; n = n.getParent())
//			list.addFirst(n);
//
//		TreeModelEvent event = new TreeModelEvent(changed, list.toArray());
//
//		for (TreeModelListener tml : getTreeModelListeners())
//			tml.treeNodesChanged(event);
//	}
	//END: move this to a better place

//	public void init(List<SessionInfo> sessionList)
//	{
//		PersistReader reader = PersistReader.getInstance();
//		if (reader == null)
//			throw new RuntimeException("The 'PersistReader' has not been initialized.");
//
//		// get a LIST of sessions
//		if (sessionList == null)
//			sessionList = reader.getSessionList();
//			
//		// Loop the sessions and load all samples 
//		for (SessionInfo sessionInfo : sessionList)
//		{
//			SessionLevel sessionLevel = new SessionLevel(
//					sessionInfo._sessionId, 
//					sessionInfo._sessionId,
//					sessionInfo._lastSampleTime, 
//					sessionInfo._numOfSamples);
//			_sessions.add(sessionLevel);
//
//			List<Timestamp>                     sessionSamples         = sessionInfo._sampleList;
//			Map<Timestamp, SampleCmCounterInfo> sampleCmCounterInfoMap = sessionInfo._sampleCmCounterInfoMap;
//
//			// Load all samples for this sampleId
//			if (sessionSamples == null)
//				sessionSamples = reader.getSessionSamplesList(sessionInfo._sessionId);
//
//			if (_showCmCounterInfoColumns)
//				if (sampleCmCounterInfoMap == null)
//					sampleCmCounterInfoMap = reader.getSessionSampleCmCounterInfoMap(sessionInfo._sessionId);
//_xxx_sampleCmCounterInfoMap = sampleCmCounterInfoMap;
//
//			if (sessionSamples != null && sessionSamples.size() > 0)
//			{
//				sessionLevel.addAllSamples(sessionSamples);
//				sessionLevel.makeBabies();
//			}
//
//			// load CM name SUM
//			Map<String,CmNameSum> sessionSamplesCmNameSumMap = sessionInfo._sampleCmNameSumMap;
//
//			if (sessionSamplesCmNameSumMap == null)
//				sessionSamplesCmNameSumMap = reader.getSessionSampleCmNameSumMap(sessionInfo._sessionId);
//
////			System.out.println("<<<<<sessionSamplesCmNameSumMap: "+sessionSamplesCmNameSumMap);
//			if (sessionSamplesCmNameSumMap != null)
//					addCmNameSumCols(sessionSamplesCmNameSumMap);
//		}
//		fixCmNameSumColsOrder();
//			
//		// Debug print the tree...
//		if (_logger.isDebugEnabled())
//			printTree();
//	}
//private Map<Timestamp, SampleCmCounterInfo> _xxx_sampleCmCounterInfoMap = null;
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

//			List<Timestamp>                     sessionSamples         = sessionInfo._sampleList;
//			Map<Timestamp, SampleCmCounterInfo> sampleCmCounterInfoMap = sessionInfo._sampleCmCounterInfoMap;

			// Load all samples for this sampleId
			if (sessionInfo._sampleList == null)
				sessionInfo._sampleList = reader.getSessionSamplesList(sessionInfo._sessionId);

			if (_showCmCounterInfoColumns)
				if (sessionInfo._sampleCmCounterInfoMap == null)
					sessionInfo._sampleCmCounterInfoMap = reader.getSessionSampleCmCounterInfoMap(sessionInfo._sessionId);
_xxx_sampleCmCounterInfoMap = sessionInfo._sampleCmCounterInfoMap;

			if (sessionInfo._sampleList != null && sessionInfo._sampleList.size() > 0)
			{
				sessionLevel.addAllSamples(sessionInfo._sampleList);
				sessionLevel.makeBabies();
			}

			// load CM name SUM
//			Map<String,CmNameSum> sessionSamplesCmNameSumMap = sessionInfo._sampleCmNameSumMap;

			if (sessionInfo._sampleCmNameSumMap == null)
				sessionInfo._sampleCmNameSumMap = reader.getSessionSampleCmNameSumMap(sessionInfo._sessionId);

//			System.out.println("<<<<<sessionSamplesCmNameSumMap: "+sessionSamplesCmNameSumMap);
			if (sessionInfo._sampleCmNameSumMap != null)
					addCmNameSumCols(sessionInfo._sampleCmNameSumMap);
		}
		fixCmNameSumColsOrder();
			
		// Debug print the tree...
		if (_logger.isDebugEnabled())
			printTree();
	}
// FIXME: fix this, why having a _xxx_variable, this can't be good... use the above structure in some way...
private Map<Timestamp, SampleCmCounterInfo> _xxx_sampleCmCounterInfoMap = null;
	
	@Override
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

	@Override
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

	@Override
	public int getColumnCount() 
	{
		return 5 + (_showCmCounterInfoColumns ? _cmLongNameSumCols.size() : 0);
	}

//	public String getColumnName(int column) 
//	{
//		_logger.debug("getColumnName(column='"+column+"')");
//		switch (column) 
//		{
//		case 0:  return "Sessions";
//		case 1:  return "Start Time";
//		case 2:  return "End Time";
//		case 3:  return "Duration";
//		case 4:  return "Samples";
//		default: return super.getColumnName(column);
//		}
//	}
	@Override
	public String getColumnName(int column) 
	{
		_logger.debug("getColumnName(column='"+column+"')");
		if      (column == 0) return "Sessions";
		else if (column == 1) return "Start Time";
		else if (column == 2) return "End Time";
		else if (column == 3) return "Duration";
		else if (column == 4) return "Samples";
		else
		{
			if (_showCmCounterInfoColumns)
				return _cmLongNameSumCols.get(column - 5);
			else
				return super.getColumnName(column);
		}
	}

	@Override
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

	@Override
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
			case 4: return Integer.valueOf(rec.getDisplayChildCount());
			}
		}

		if (node instanceof Timestamp && column == 0) 
		{
//			return node;
			switch (column) 
			{
			case 0: return node;
			case 1: return node;
			case 2: return "-";
			case 3: return "1 sample";
			case 4: return null;
			}
		}

		if (column > 4)
		{
			// FIXME: re-do all this ...
			Timestamp ts = null;
			if (node instanceof SessionLevel) ts = ((SessionLevel)node).getStartTime();
			if (node instanceof Timestamp)    ts = (Timestamp)node;
			if (ts == null) return null;
			
			String    colName     = getColumnName(column);
			int       colIndex    = _cmLongNameSumCols.indexOf(colName);
			if (colIndex < 0) 
				return null;
			String    shortCmName = _cmShortNameSumCols.get(colIndex);
			if (_xxx_sampleCmCounterInfoMap != null)
			{
				SampleCmCounterInfo scmci = null;
				if (node instanceof SessionLevel)
					scmci = ((SessionLevel)node).getSummaryCmCounterInfo();
				else
					scmci = _xxx_sampleCmCounterInfoMap.get(ts);
					
				if (scmci == null) return "---";
//				if (scmci == null) {System.out.println("scmci=NULL, ts='"+ts+"'.");return null;}
				CmCounterInfo cmci = scmci._ciMap.get(shortCmName);
				if (cmci == null) return "---";
//				if (cmci == null) System.out.println("scmci._ciMap="+scmci._ciMap);
//				if (cmci == null) {System.out.println("cmci =NULL, shortCmName='"+shortCmName+"'.");return null;}
//				if (cmci == null) {return "cmci =NULL, shortCmName='"+shortCmName+"'.";}
				if (   cmci._absRows == cmci._diffRows && cmci._absRows == cmci._rateRows)
				{
					if (cmci._absRows == 0)
						return "---";
					return Integer.toString(cmci._absRows);
				}
				else
					return cmci._absRows + "-" + cmci._diffRows + "-" + cmci._rateRows;
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

	@Override
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

	@Override
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
	@Override
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
//SessionLevel _root = null;
//Map<Timestamp,SampleCmCounterInfo> _sampleCmCounterInfoMap = null;
SampleCmCounterInfo _summaryCmCounterInfo = null;
public SampleCmCounterInfo getSummaryCmCounterInfo() {return _summaryCmCounterInfo;}
public void setSummaryCmCounterInfo(SampleCmCounterInfo summaryCmCounterInfo) {_summaryCmCounterInfo = summaryCmCounterInfo;}

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

//		public SampleCmCounterInfo getCmCounterInfo(Timestamp ts)
//		{
//			return _root._sampleCmCounterInfoMap.get(ts);
//		}

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

			SampleCmCounterInfo allLevelScmci    = new SampleCmCounterInfo(firstTs, firstTs);
			SampleCmCounterInfo dayLevelScmci    = new SampleCmCounterInfo(firstTs, firstTs);
			SampleCmCounterInfo hourLevelScmci   = new SampleCmCounterInfo(firstTs, firstTs);
			SampleCmCounterInfo minuteLevelScmci = new SampleCmCounterInfo(firstTs, firstTs);

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

					if (_showCmCounterInfoColumns)
					{
						dayLevel.setSummaryCmCounterInfo(dayLevelScmci);
						dayLevelScmci = new SampleCmCounterInfo(firstTs, currTs);
					}

					dayLevel = new DayLevel();
					dayLevel.setSampleId(_sampleId);
					dayLevel.setStartTime(currTs);

					this.addChild(dayLevel); // this = SessionLevel
				}
				if( ! hourLevel.isWithinPeriod(currTs) )
				{
					hourLevel.setEndTime(prevTs);

					if (_showCmCounterInfoColumns)
					{
						hourLevel.setSummaryCmCounterInfo(hourLevelScmci);
						hourLevelScmci = new SampleCmCounterInfo(firstTs, currTs);
					}

					hourLevel = new HourLevel();
					hourLevel.setSampleId(_sampleId);
					hourLevel.setStartTime(currTs);

					dayLevel.addChild(hourLevel);
				}
				if( ! minuteLevel.isWithinPeriod(currTs) )
				{
					minuteLevel.setEndTime(prevTs);

					if (_showCmCounterInfoColumns)
					{
						minuteLevel.setSummaryCmCounterInfo(minuteLevelScmci);
						minuteLevelScmci = new SampleCmCounterInfo(firstTs, currTs);
					}

					minuteLevel = new MinuteLevel();
					minuteLevel.setSampleId(_sampleId);
					minuteLevel.setStartTime(currTs);

					hourLevel.addChild(minuteLevel);
				}

				// Do summary
				if (_showCmCounterInfoColumns)
				{
					SampleCmCounterInfo scmci = _xxx_sampleCmCounterInfoMap.get(currTs);
					allLevelScmci   .merge(scmci, _showCmCounterRowsAsSamples);
					dayLevelScmci   .merge(scmci, _showCmCounterRowsAsSamples);
					hourLevelScmci  .merge(scmci, _showCmCounterRowsAsSamples);
					minuteLevelScmci.merge(scmci, _showCmCounterRowsAsSamples);
				}

				// Add current sample to the minute level
				minuteLevel.addChild( currTs );
			}

			Timestamp lastTs  = (Timestamp)_allSamples.get(_allSamples.size()-1);

			minuteLevel.setEndTime(lastTs);
			hourLevel  .setEndTime(lastTs);
			dayLevel   .setEndTime(lastTs);

			// Close summary
			if (_showCmCounterInfoColumns)
			{
				setSummaryCmCounterInfo(allLevelScmci);
				dayLevel   .setSummaryCmCounterInfo(dayLevelScmci);
				hourLevel  .setSummaryCmCounterInfo(hourLevelScmci);
				minuteLevel.setSummaryCmCounterInfo(minuteLevelScmci);
			}
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

		@Override
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

		@Override
		public String toString()
		{
			return("DayLevel(startTime='"+_startTime+"', endTime='"+_endTime+"', periodStartTime='"+_periodStartTime+"', periodEndTime='"+_periodEndTime+"', numOfSamples='"+_numOfSamples+"'");
		}

		@Override
		public String getDisplayString()
		{
			return "Day " + _atDay * _dayLevelCount;
		}

		@Override
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
		
		@Override
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

		@Override
		public String toString()
		{
			return("HourLevel(startTime='"+_startTime+"', endTime='"+_endTime+"', periodStartTime='"+_periodStartTime+"', periodEndTime='"+_periodEndTime+"', numOfSamples='"+_numOfSamples+"'");
		}

		@Override
		public String getDisplayString()
		{
			if (_hourLevelCount == 1)
				return "Hour " + _atHour * _hourLevelCount;
			else
				return "Hour " + _atHour * _hourLevelCount + " - " + (_atHour +  1) * _hourLevelCount;
		}

		@Override
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

		@Override
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

		@Override
		public String toString()
		{
			return("MinuteLevel(startTime='"+_startTime+"', endTime='"+_endTime+"', periodStartTime='"+_periodStartTime+"', periodEndTime='"+_periodEndTime+"', numOfSamples='"+_numOfSamples+"'");
		}

		@Override
		public String getDisplayString()
		{
			if (_hourLevelCount == 1)
				return             "Minute " + _atMinute * _minuteLevelCount + " - " + (_atMinute +  1) * _minuteLevelCount;
			else
				return _atHour + ", Minute " + _atMinute * _minuteLevelCount + " - " + (_atMinute +  1) * _minuteLevelCount;
		}

		@Override
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

		@Override
		public boolean isWithinPeriod(Timestamp ts)
		{
			long minutes = (ts.getTime() - _periodStartTime.getTime()) / 1000 / 60;
			return (minutes < _minuteLevelCount);
		}
	}
}
