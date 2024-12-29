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
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * DbxTune is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with DbxTune.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.dbxtune.sql.diff;

import java.util.ArrayList;
import java.util.List;

import com.dbxtune.utils.StringUtil;

public class DiffSink
{
	private int _maxDiffCount;
	private int _currentDiffCount = 0;
	
	private DiffContext _context;
	private DiffTable   _leftDt;
	private DiffTable   _rightDt;

//	private List<Object[]> _leftExtraRows  = new ArrayList<>();
//	private List<Object[]> _rightExtraRows = new ArrayList<>();
	private List<Object[]> _leftMissingRows  = new ArrayList<>();
	private List<Object[]> _rightMissingRows = new ArrayList<>();
	
	private List<DiffColumnValues> _diffEntries = new ArrayList<>();

	
	public DiffSink(DiffContext context, DiffTable left, DiffTable right, int maxDiffCount)
	{
		_context = context;
		_leftDt  = left;
		_rightDt = right;
		_maxDiffCount = maxDiffCount;
	}

	public DiffContext getContext() { return _context; }
	public DiffTable   getLeftDt()  { return _leftDt;  }
	public DiffTable   getRightDt() { return _rightDt; }

	public int getMaxDiffCount()
	{
		return _maxDiffCount;
	}
	
	public int getCurrentDiffCount()
	{
		return _currentDiffCount;
	}
	
	
	public void addLeftMissingRow(Object[] row)
	{
		_leftMissingRows.add(row);
		_currentDiffCount++;
	}

	public List<Object[]> getLeftMissingRows()
	{
		return _leftMissingRows;
	}

	public void addRightMissingRow(Object[] row)
	{
		_rightMissingRows.add(row);
		_currentDiffCount++;
	}

	public List<Object[]> getRightMissingRows()
	{
		return _rightMissingRows;
	}
	
	
	
//	public void addLeftExtraRow(Object[] row)
//	{
//		_leftExtraRows.add(row);
//		_currentDiffCount++;
//	}
//
//	public List<Object[]> getLeftExtraRows()
//	{
//		return _leftExtraRows;
//	}
//
//	public void addRightExtraRow(Object[] row)
//	{
//		_rightExtraRows.add(row);
//		_currentDiffCount++;
//	}
//
//	public List<Object[]> getRightExtraRows()
//	{
//		return _rightExtraRows;
//	}
	
	
	
	

	public void addDiffColumnValues(Object[] pk, List<Integer> leftColsPos, List<Integer> rightColsPos, Object[] l_row, Object[] r_row)
	{
		_diffEntries.add( new DiffColumnValues(pk, 
				leftColsPos,  getLeftDt(),  l_row, 
				rightColsPos, getRightDt(), r_row) );

		_currentDiffCount++;
	}

//	public void addDiffColumnValue(Object[] pk, int leftColPos, int rightColPos, Object left, Object right)
//	throws SQLException
//	{
//		String leftColName  = _leftDt.getMetaData().getColumnName(leftColPos  + 1);
//		String rightColName = _leftDt.getMetaData().getColumnName(rightColPos + 1);
//		
//		_diffEntries.add( new DiffColumnValue(pk, 
//				leftColPos,  leftColName,  left, 
//				rightColPos, rightColName, right) );
//
//		_currentDiffCount++;
//	}
	
	public List<DiffColumnValues> getDiffColumnValues()
	{
		return _diffEntries;
	}


	
//	protected class DiffColumnValue
//	{
//		Object[] _pk;
//		
//		int      _leftColPos;
//		String   _leftColName;
//		Object   _leftColVal;
//		
//		int      _rightColPos;
//		String   _rightColName;
//		Object   _rightColVal;
//
//		public DiffColumnValue(Object[] pk, int leftColPos, String leftColName, Object left, int rightColPos, String rightColName, Object right)
//		{
//			_pk          = pk;
//
//			_leftColPos  = leftColPos;
//			_leftColName = leftColName;
//			_leftColVal  = left;
//
//			_rightColPos  = rightColPos;
//			_rightColName = rightColName;
//			_rightColVal  = right;
//		}
//		
//		@Override
//		public String toString()
//		{
////			return super.toString() + ": pk=|"+StringUtil.toCommaStr(_pk)+"|, leftColPos="+_leftColPos+", leftColName='"+_leftColName+"', _leftColVal=|"+_leftColVal+"|, rightColPos="+_rightColPos+", rightColName='"+_rightColName+"', _rightColVal=|"+_rightColVal+"|.";
//			return super.toString() + ": pk=|"+StringUtil.toCommaStr(_pk)+"|, leftColPos="+_leftColPos+", rightColPos="+_rightColPos+", leftColName='"+_leftColName+"', rightColName='"+_rightColName+"', _leftColVal=|"+_leftColVal+"|, _rightColVal=|"+_rightColVal+"|.";
//		}
//	}
	public class DiffColumnValues
	{
		private Object[]      _pk;

		private List<Integer> _leftColsPos;
		private DiffTable     _leftDt;
		private Object[]      _leftVals;

		private List<Integer> _rightColsPos;
		private DiffTable     _rightDt;
		private Object[]      _rightVals;

		public Object[]      getPkValues()        { return _pk; }

		public List<Integer> getLeftColumnsPos()  { return _leftColsPos; }
		public Object[]      getLeftValues()      { return _leftVals; }
		public DiffTable     getLeftDt()          { return _leftDt; }
		
		public List<Integer> getRightColumnsPos() { return _rightColsPos; }
		public Object[]      getRightValues()     { return _rightVals; }
		public DiffTable     getRightDt()         { return _rightDt; }
		
		public DiffColumnValues(Object[] pk, List<Integer> leftColsPos, DiffTable leftDt, Object[] left, List<Integer> rightColsPos, DiffTable rightDt, Object[] right)
		{
			_pk          = pk;

			_leftColsPos = leftColsPos;
			_leftDt      = leftDt;
			_leftVals    = left;

			_rightColsPos = rightColsPos;
			_rightDt      = rightDt;
			_rightVals    = right;
		}
		
		@Override
		public String toString()
		{
			List<Object> left  = new ArrayList<>();
			List<Object> right = new ArrayList<>();
			
			for (Integer c : _leftColsPos)  left .add(_leftVals [c]);
			for (Integer c : _rightColsPos) right.add(_rightVals[c]);
			
			return super.toString() + ": pk=|"+StringUtil.toCommaStr(_pk)+"|, leftColPos="+_leftColsPos+", rightColPos="+_rightColsPos+", _leftColVal="+left+", _rightVals="+right+".";
		}
	}
}
