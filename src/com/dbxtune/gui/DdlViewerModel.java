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
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdesktop.swingx.treetable.AbstractTreeTableModel;

import com.dbxtune.pcs.DdlDetails;

public class DdlViewerModel
extends AbstractTreeTableModel
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	
//	private ArrayList<DbEntry> _dblist = new ArrayList<DbEntry>();
	private DbList _dblist = new DbList();

	/**
	 * 
	 */
	public DdlViewerModel(List<DdlDetails> ddlList) 
	{
		super();
		init(ddlList);
	}

	public void init(List<DdlDetails> ddlList)
	{
		DbEntry   dbe = new DbEntry("");
		TypeEntry te  = new TypeEntry("");
		String dbname = "";
		String type = "";

		for (DdlDetails d : ddlList)
		{
			if ( ! dbname.equals(d.getDbname()) )
			{
				dbname = d.getDbname();
				type   = "getNewOne";
				dbe = new DbEntry(dbname);
				_dblist.add(dbe);
//				System.out.println(">   init:newDbEntry='"+dbname+"'.");
			}
				
			if ( ! type.equals(d.getType()) )
			{
				type = d.getType();
				te  = new TypeEntry(type);
				dbe.add(te);
//				System.out.println(">>  init:newTypeEntry='"+type+"'.");
			}

			dbe._sumChildCount++;
			te ._sumChildCount++;

//			System.out.println(">>> init:newObjectEntry name='"+d.getObjectName()+"', owner='"+d.getOwner()+"'.");
			ObjectEntry oe = new ObjectEntry();
			oe._dbname       = d.getDbname();
			oe._name         = d.getObjectName();
			oe._owner        = d.getOwner();
			oe._type         = d.getType();
			oe._crdate       = d.getCrdate();
			oe._source       = d.getSource();
			oe._dependParent = d.getDependParent();
			oe._dependLevel  = d.getDependLevel();
			oe._dependList   = d.getDependList().toString();

			te.add(oe);
		}
	}

	@Override
	public Object getChild(Object parent, int index) 
	{
//System.out.println("getChildCount(parent='"+parent+"', index='"+index+"', parent.objType='"+(parent==null?"null":parent.getClass().getName())+"')");
		_logger.debug("getChildCount(parent='"+parent+"', index='"+index+"')");
		if (parent instanceof DbList)
		{
			return _dblist.get(index);
		}
		if (parent instanceof TypeList)
		{
			return ((TypeList) parent).get(index);
		}

		if (parent instanceof DbEntry)
		{
			return ((DbEntry) parent)._typeList.get(index);
		}
		if (parent instanceof TypeEntry)
		{
			return ((TypeEntry) parent)._objectList.get(index);
		}

		return null;
	}

	@Override
	public int getChildCount(Object parent) 
	{
		_logger.debug("getChildCount(parent='"+parent+"')");
		if (parent instanceof DbEntry)
		{
			return ((DbEntry) parent).getChildCount();
		}
		if (parent instanceof TypeEntry)
		{
			return ((TypeEntry) parent).getChildCount();
		}

		if (parent instanceof DbList)
		{
			return _dblist.size();
		}
		if (parent instanceof TypeList)
		{
			return ((TypeList) parent).size();
		}

		return 0;
	}

	@Override
	public int getColumnCount() 
	{
		return 7;
	}

	@Override
	public String getColumnName(int column) 
	{
		_logger.debug("getColumnName(column='"+column+"')");
		if      (column == 0) return "DBName";
		else if (column == 1) return "Type";
		else if (column == 2) return "Cnt";
		else if (column == 3) return "Owner";
		else if (column == 4) return "Creation Date and Time";
		else if (column == 5) return "Depends Level";
		else if (column == 6) return "Depend Parent";
//		else if (column == 3) return "Name";
//		else if (column == 6) return "Depends List";
//		else if (column == 7) return "Source";
		else
		{
			return super.getColumnName(column);
		}
	}

	@Override
	public Class<?> getColumnClass(int column) 
	{
		_logger.debug("getColumnClass(column='"+column+"')");
		switch (column) 
		{
		case 0:  return String   .class;
		case 1:  return String   .class;
		case 2:  return Integer  .class;
		case 3:  return String   .class;
//		case 4:  return Timestamp.class;
		case 4:  return String   .class;
		case 5:  return Integer  .class;
		case 6:  return String   .class;
		default: return super.getColumnClass(column);
		}
	}

	@Override
	public Object getValueAt(Object node, int column) 
	{
		_logger.debug("getValueAt(node='"+node+"', column='"+column+"')");
		if (node instanceof DbEntry)
		{
			DbEntry rec = (DbEntry) node;
			switch (column) 
			{
			case 0: return rec._dbname;// + " ("+rec._sumChildCount+")";
			case 1: return "DB";
			case 2: return rec._sumChildCount;
			case 3: return null;
			case 4: return null;
			case 5: return null;
			case 6: return null;
			}
		}

		if (node instanceof TypeEntry)
		{
			TypeEntry rec = (TypeEntry) node;
			switch (column) 
			{
			case 0: return rec.getTypeName();// + " ("+rec._sumChildCount+")";
			case 1: return rec._type;
			case 2: return rec._sumChildCount;
			case 3: return null;
			case 4: return null;
			case 5: return null;
			case 6: return null;
			}
		}

		if (node instanceof ObjectEntry)
		{
			ObjectEntry rec = (ObjectEntry) node;
			switch (column) 
			{
			case 0: return rec._name;
			case 1: return rec._type;
			case 2: return null;
			case 3: return rec._owner;
			case 4: return rec._crdate;
			case 5: return rec._dependLevel;
			case 6: return rec._dependParent;
			}
		}

		return null;
	}
//	if      (column == 0) return "DBName";
//	else if (column == 1) return "Type";
//	else if (column == 2) return "Owner";
//	else if (column == 3) return "Name";
//	else if (column == 4) return "CrDate";
//	else if (column == 5) return "Depends Level";
//	else if (column == 6) return "Depends List";

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
//		if (parent instanceof SessionLevel && child instanceof SessionLevel) 
//		{
//		}

		return -1;
	}

	@Override
	public Object getRoot() 
	{
		_logger.debug("getRoot()");
		return _dblist;
	}

	@Override
	public boolean isLeaf(Object node) 
	{
		_logger.debug("isLeaf(node='"+node+"')");

		if (node instanceof ObjectEntry) 
			return true;

		return false;
	}
	

	
	
	///////////////////////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////
	// ---- SUB CLASSES ---- SUB CLASSES ---- SUB CLASSES ---- SUB CLASSES ----
	///////////////////////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////

	@SuppressWarnings("serial") protected static class DbList extends ArrayList<DbEntry>         {}
	@SuppressWarnings("serial") protected static class TypeList extends ArrayList<TypeEntry>     {}
	@SuppressWarnings("serial") protected static class ObjectList extends ArrayList<ObjectEntry> {}

	protected static class DbEntry
	{
		public String _dbname;
		public int    _sumChildCount = 0;
//		public ArrayList<TypeEntry> _typeList = new ArrayList<TypeEntry>();
		public TypeList _typeList = new TypeList();

		public DbEntry(String dbname)   { _dbname = dbname; }
		public int  getChildCount()     { return _typeList.size(); }
		public void add(TypeEntry te)   { _typeList.add(te); }
		public String getDisplayString(){ return _dbname; }
//		public String toString()        { return getDisplayString(); }
		@Override
		public String toString()        { return "DbEntry("+_dbname+")"; }
	}
	protected static class TypeEntry
	{
		public String _type;
		public int    _sumChildCount = 0;
//		public ArrayList<ObjectEntry> _objectList = new ArrayList<ObjectEntry>();
		public ObjectList _objectList = new ObjectList();

		public TypeEntry(String type)   { _type = type; }
		public int  getChildCount()     { return _objectList.size(); }
		public void add(ObjectEntry oe) { _objectList.add(oe); };
		public String getTypeName()     { return DdlViewer.getTypeName(_type); }
		public String getDisplayString(){ return getTypeName(); }
//		public String toString()        { return getDisplayString(); }
		@Override
		public String toString()        { return "TypeEntry("+_type+")"; }
	}
	protected static class ObjectEntry
	{
		public String    _dbname;
		public String    _name;
		public String    _owner;
		public String    _type;
		public Timestamp _crdate;
		public String    _source;
		public String    _dependParent;
		public int       _dependLevel;
		public String    _dependList;

		public boolean   _objectTextIsLoaded = false;
		public String    _objectText;
		public boolean   _dependsTextIsLoaded = false;
		public String    _dependsText;
		public boolean   _optdiagTextIsLoaded = false;
		public String    _optdiagText;
		public boolean   _extraInfoTextIsLoaded = false;
		public String    _extraInfoText;

		public String getDisplayString(){ return _name; }
//		public String toString()        { return getDisplayString(); }
		@Override
		public String toString()        { return "ObjectEntry("+_dbname+":"+_type+":"+_name+")"; }
	}
}
