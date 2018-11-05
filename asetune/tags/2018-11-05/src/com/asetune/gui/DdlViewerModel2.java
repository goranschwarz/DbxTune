package com.asetune.gui;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.jdesktop.swingx.treetable.AbstractMutableTreeTableNode;
import org.jdesktop.swingx.treetable.DefaultMutableTreeTableNode;
import org.jdesktop.swingx.treetable.DefaultTreeTableModel;

import com.asetune.pcs.DdlDetails;

public class DdlViewerModel2
extends DefaultTreeTableModel

{
//	private static Logger _logger = Logger.getLogger(OfflineSessionModel.class);
	
//	private ArrayList<DbEntry> _dblist = new ArrayList<DbEntry>();
//	private DbList _dblist = new DbList();
	private DefaultMutableTreeTableNode _dblist  = new DefaultMutableTreeTableNode();
	private List<DdlDetails>            _ddlList = null;

	public List<DdlDetails> getDdlDetails() { return _ddlList; };
	
	/**
	 * 
	 */
	public DdlViewerModel2(List<DdlDetails> ddlList) 
	{
		super();
		init(ddlList);
	}

	public void init(List<DdlDetails> ddlList)
	{
		_ddlList = ddlList;

		DbEntry   dbe = new DbEntry("");
		TypeEntry te  = new TypeEntry("");
		String dbname = "";
		String type = "";

		List<String> columns = new ArrayList<String>();
		columns.add("DBName");
		columns.add("Type");
		columns.add("Cnt");
		columns.add("Owner");
		columns.add("Creation Date and Time");
		columns.add("Depends Level");
		columns.add("Depend Parent");
		setColumnIdentifiers(columns);

		setRoot(_dblist);

		for (DdlDetails d : ddlList)
		{
			if ( ! dbname.equals(d.getDbname()) )
			{
				dbname = d.getDbname();
				type   = "getNewOne";
				dbe = new DbEntry(dbname);
				_dblist.add(dbe);
//				_dblist.add( new DefaultMutableTreeTableNode(dbname));
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
			oe._sampleTime   = d.getSampleTime();
			oe._source       = d.getSource();
			oe._dependParent = d.getDependParent();
			oe._dependLevel  = d.getDependLevel();
			oe._dependList   = d.getDependList().toString();

			te.add(oe);
		}
	}

//	public Object getChild(Object parent, int index) 
//	{
////System.out.println("getChildCount(parent='"+parent+"', index='"+index+"', parent.objType='"+(parent==null?"null":parent.getClass().getName())+"')");
//		_logger.debug("getChildCount(parent='"+parent+"', index='"+index+"')");
//		if (parent instanceof DbList)
//		{
//			return _dblist.get(index);
//		}
//		if (parent instanceof TypeList)
//		{
//			return ((TypeList) parent).get(index);
//		}
//
//		if (parent instanceof DbEntry)
//		{
//			return ((DbEntry) parent)._typeList.get(index);
//		}
//		if (parent instanceof TypeEntry)
//		{
//			return ((TypeEntry) parent)._objectList.get(index);
//		}
//
//		return null;
//	}

//	public int getChildCount(Object parent) 
//	{
//		_logger.debug("getChildCount(parent='"+parent+"')");
//		if (parent instanceof DbEntry)
//		{
//			return ((DbEntry) parent).getChildCount();
//		}
//		if (parent instanceof TypeEntry)
//		{
//			return ((TypeEntry) parent).getChildCount();
//		}
//
//		if (parent instanceof DbList)
//		{
//			return _dblist.size();
//		}
//		if (parent instanceof TypeList)
//		{
//			return ((TypeList) parent).size();
//		}
//
//		return 0;
//	}

//	public int getColumnCount() 
//	{
//		return 7;
//	}

//	public String getColumnName(int column) 
//	{
//		_logger.debug("getColumnName(column='"+column+"')");
//		if      (column == 0) return "DBName";
//		else if (column == 1) return "Type";
//		else if (column == 2) return "Cnt";
//		else if (column == 3) return "Owner";
//		else if (column == 4) return "Creation Date and Time";
//		else if (column == 5) return "Depends Level";
//		else if (column == 6) return "Depend Parent";
////		else if (column == 3) return "Name";
////		else if (column == 6) return "Depends List";
////		else if (column == 7) return "Source";
//		else
//		{
//			return super.getColumnName(column);
//		}
//	}

//	public Class<?> getColumnClass(int column) 
//	{
//		_logger.debug("getColumnClass(column='"+column+"')");
//		switch (column) 
//		{
//		case 0:  return String   .class;
//		case 1:  return String   .class;
//		case 2:  return Integer  .class;
//		case 3:  return String   .class;
////		case 4:  return Timestamp.class;
//		case 4:  return String   .class;
//		case 5:  return Integer  .class;
//		case 6:  return String   .class;
//		default: return super.getColumnClass(column);
//		}
//	}

//	public Object getValueAt(Object node, int column) 
//	{
//		_logger.debug("getValueAt(node='"+node+"', column='"+column+"')");
//		if (node instanceof DbEntry)
//		{
//			DbEntry rec = (DbEntry) node;
//			switch (column) 
//			{
//			case 0: return rec._dbname;// + " ("+rec._sumChildCount+")";
//			case 1: return "DB";
//			case 2: return rec._sumChildCount;
//			case 3: return null;
//			case 4: return null;
//			case 5: return null;
//			case 6: return null;
//			}
//		}
//
//		if (node instanceof TypeEntry)
//		{
//			TypeEntry rec = (TypeEntry) node;
//			switch (column) 
//			{
//			case 0: return rec.getTypeName();// + " ("+rec._sumChildCount+")";
//			case 1: return rec._type;
//			case 2: return rec._sumChildCount;
//			case 3: return null;
//			case 4: return null;
//			case 5: return null;
//			case 6: return null;
//			}
//		}
//
//		if (node instanceof ObjectEntry)
//		{
//			ObjectEntry rec = (ObjectEntry) node;
//			switch (column) 
//			{
//			case 0: return rec._name;
//			case 1: return rec._type;
//			case 2: return null;
//			case 3: return rec._owner;
//			case 4: return rec._crdate;
//			case 5: return rec._dependLevel;
//			case 6: return rec._dependParent;
//			}
//		}
//
//		return null;
//	}
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

//	public int getIndexOfChild(Object parent, Object child) 
//	{
//		_logger.debug("getIndexOfChild(parent='"+parent+"', child='"+child+"'.)");
////		if (parent instanceof SessionLevel && child instanceof SessionLevel) 
////		{
////		}
//
//		return -1;
//	}

//	public Object getRoot() 
//	{
//		_logger.debug("getRoot()");
//		return _dblist;
//	}

//	public boolean isLeaf(Object node) 
//	{
//		_logger.debug("isLeaf(node='"+node+"')");
//
//		if (node instanceof ObjectEntry) 
//			return true;
//
//		return false;
//	}
	

	
	
	///////////////////////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////
	// ---- SUB CLASSES ---- SUB CLASSES ---- SUB CLASSES ---- SUB CLASSES ----
	///////////////////////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////

//	@SuppressWarnings("serial") protected static class DbList extends DefaultMutableTreeTableNode     {}
//	@SuppressWarnings("serial") protected static class TypeList extends DefaultMutableTreeTableNode   {}
//	@SuppressWarnings("serial") protected static class ObjectList extends DefaultMutableTreeTableNode {}

	protected static class DbEntry
	extends AbstractMutableTreeTableNode
	{
		public String _dbname;
		public int    _sumChildCount = 0;
//		public TypeList _typeList = new TypeList();

		public DbEntry(String dbname)   { super(); _dbname = dbname; setUserObject(this); }
//		public int  getChildCount()     { return _typeList.size(); }
//		public void add(TypeEntry te)   { _typeList.add(te); }
		public void add(TypeEntry te)   { super.add(te); }
		public String getDisplayString(){ return _dbname; }
		@Override
		public String toString()        { return getDisplayString(); }
//		public String toString()        { return "DbEntry("+_dbname+")"; }

		@Override
		public Object getValueAt(int column)
		{
			switch (column) 
			{
				case 0: return _dbname;// + " ("+rec._sumChildCount+")";
				case 1: return "DB";
				case 2: return _sumChildCount;
				case 3: return null;
				case 4: return null;
				case 5: return null;
				case 6: return null;
			}
			return null;
		}
		@Override
		public int getColumnCount()
		{
			return 7;
		}
	}
	protected static class TypeEntry
	extends AbstractMutableTreeTableNode
	{
		public String _type;
		public int    _sumChildCount = 0;
//		public ObjectList _objectList = new ObjectList();

		public TypeEntry(String type)   { super(); _type = type; setUserObject(this); }
//		public int  getChildCount()     { return _objectList.size(); }
//		public void add(ObjectEntry oe) { _objectList.add(oe); };
		public void add(ObjectEntry oe) { super.add(oe); };
		public String getTypeName()     { return DdlViewer.getTypeName(_type); }
		public String getDisplayString(){ return getTypeName(); }
		@Override
		public String toString()        { return getDisplayString(); }
//		public String toString()        { return "TypeEntry("+_type+")"; }
		@Override
		public Object getValueAt(int column)
		{
			switch (column) 
			{
				case 0: return getTypeName();// + " ("+rec._sumChildCount+")";
				case 1: return _type;
				case 2: return _sumChildCount;
				case 3: return null;
				case 4: return null;
				case 5: return null;
				case 6: return null;
			}
			return null;
		}
		@Override
		public int getColumnCount()
		{
			return 7;
		}
	}
	protected static class ObjectEntry
	extends AbstractMutableTreeTableNode
	{
		public ObjectEntry() {super();}
		public String    _dbname;
		public String    _name;
		public String    _owner;
		public String    _type;
		public Timestamp _crdate;
		public Timestamp _sampleTime;
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
		@Override
		public String toString()        { return getDisplayString(); }
//		public String toString()        { return "ObjectEntry("+_dbname+":"+_type+":"+_name+")"; }

		@Override
		public Object getValueAt(int column)
		{
			switch (column) 
			{
				case 0: return _name;
				case 1: return _type;
				case 2: return null;
				case 3: return _owner;
//				case 4: return _crdate;
				case 4:
				{
					if ("SS".equals(_type))
						return _sampleTime;
					else
						return _crdate;
				}
				case 5: return _dependLevel;
				case 6: return _dependParent;
			}
			return null;
		}
		@Override
		public int getColumnCount()
		{
			return 7;
		}
	}
}
