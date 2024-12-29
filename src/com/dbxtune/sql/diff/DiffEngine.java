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

import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import com.dbxtune.sql.diff.comp.PkComparator;
import com.dbxtune.utils.StringUtil;

/**
 * Core part of the Diff inspired by DiffKit
 * 
 * @author gorans
 */
public class DiffEngine
{
	private DiffContext _context;
	
	public DiffEngine(DiffContext context)
	{
		_context = context;
	}

	protected void diff() 
	throws SQLException, DiffException 
	{
		DiffSink sink = _context.getSink();

		if (sink == null)
			throw new DiffException("No Sink has been assigned to the DiffContext object. context.validate() needs to be called before diff().");
		
		DiffTable l_dt = _context.getLeftDt();
		DiffTable r_dt = _context.getRightDt();

//		Comparator<Object[]> rowComparator = context_._tableComparison.getRowComparator();

//		PkComparator pkComp = context.getPkComparator();
//		PkComparator pkComp = new PkComparator(false, "id");
		PkComparator pkComp = _context.getPkComparator();

		Object[] l_row = null;
		Object[] r_row = null;
		
		boolean[] onlyOnColumnsArray = null;
		List<String> diffColList = _context.getDiffColumns();
		if (diffColList != null && !diffColList.isEmpty())
		{
			onlyOnColumnsArray = new boolean[l_dt.getColumnCount()];
			int arrPos = 0;
			for (String colName : l_dt.getColumnNames())
			{
				onlyOnColumnsArray[arrPos] = false;
				if (diffColList.contains(colName))
				{
					onlyOnColumnsArray[arrPos] = true;
					
					if (_context.isDebugEnabled())
						_context.addDebugMessage("Column name '" + colName + "' at possition " + arrPos + " will NOT be difference checked.");
				}
				arrPos++;
			}

			if (_context.isDebugEnabled())
			{
				_context.addDebugMessage("getDiffColumns() content: " + StringUtil.toCommaStr(diffColList));
				_context.addDebugMessage("            colSkipArray: " + StringUtil.toCommaStr(onlyOnColumnsArray));
				_context.addDebugMessage("            leftColumns : " + StringUtil.toCommaStr(l_dt.getColumnNames()));
				_context.addDebugMessage("            rightColumns: " + StringUtil.toCommaStr(r_dt.getColumnNames()));
			}
		}

		// Print progress after each X row, this will be changed to 100 after 100 rows 
		long lastProgresUpdate = 0;
		long progresUpdatePeriodMs = 100;
		NumberFormat nf = NumberFormat.getInstance();
		
		while(sink.getCurrentDiffCount() < sink.getMaxDiffCount())
		{
			if (_context.isTraceEnabled())
				_context.addTraceMessage("####################################################################################");
			
			// Get next row (if we do not already have a record, and we want to move one side "down" to next hopefully matching PK)
			if (l_row == null) l_row = l_dt.getNextRow();
			if (r_row == null) r_row = r_dt.getNextRow();

			// END OF ROWS
			if (l_row == null && r_row == null)
			{
				if (_context.isTraceEnabled())
					_context.addTraceMessage("  <<<<<<<<<<<<<<< BREAK ----- NO MORE ROWS");
				break;
			}

			// Check for cancellation
			// Print progress
			if ( _context.getProgressDialog() != null )
			{
				long l_rowCount = l_dt.getRowCount();
				long r_rowCount = r_dt.getRowCount();

				if (_context.getProgressDialog().isCancelled())
				{
					String msg = "Reading rows was cancelled after " + l_rowCount + " Left Rows and " + r_rowCount + " Right Rows was read. (sending cancel)";
					_context.getProgressDialog().setState(msg);
					_context.addWarningMessage(msg);
					
					l_dt.sendCancel();
					r_dt.sendCancel();
					
					break;
				}

				if ( l_rowCount < 100 || System.currentTimeMillis() - lastProgresUpdate > progresUpdatePeriodMs )
				{
					long l_expRowCount = l_dt.getExpectedRowCount();
					long r_expRowCount = r_dt.getExpectedRowCount();

					String l_expRowCountStr = "";
					String r_expRowCountStr = "";
					String pctDoneStr       = "";
					
					if (l_expRowCount >= 0) l_expRowCountStr = " [preCnt=" + nf.format(l_expRowCount) + "]";
					if (r_expRowCount >= 0) r_expRowCountStr = " [preCnt=" + nf.format(r_expRowCount) + "]";

					// Calculate Percent done
					if (l_expRowCount >= 0)
						pctDoneStr = String.format("%.1f%% -- ", ((l_rowCount*1.0) / (l_expRowCount*1.0))*100.0 );

					lastProgresUpdate = System.currentTimeMillis();
					_context.getProgressDialog().setState(pctDoneStr + "Reading Left Row " + nf.format(l_rowCount) + l_expRowCountStr + " and Right Row " + nf.format(r_rowCount) + r_expRowCountStr);
				}
			}
			
			// Handle "data on one side only"
			if (l_row == null && r_row != null)
			{
				if (_context.isTraceEnabled())
					_context.addTraceMessage("  <<-- NO-LEFT-ROW = RIGHT-----VALUE----- add RIGHT row as EXTRA ROW");
				
//				sink.addRightExtraRow(r_row);
				sink.addLeftMissingRow(r_row);
				r_row = null;
			}
			
			if (r_row == null && l_row != null)
			{
				if (_context.isTraceEnabled())
					_context.addTraceMessage("  -->> NO-RIGHT-ROW = LEFT-----VALUE----- add LEFT row as EXTRA ROW");

//				sink.addLeftExtraRow(l_row);
				sink.addRightMissingRow(l_row);
				l_row = null;
			}

			// We have a Record on both side... Check if it's the SAME PK
			if (l_row != null && r_row != null)
			{
				if (_context.isTraceEnabled())
					_context.addTraceMessage("  ROW POSITIONS: l_rowPos="+l_dt.getRowCount()+", r_rowPos="+r_dt.getRowCount());

				int compRes = pkComp.compare(l_row, r_row);
				
				if (_context.isTraceEnabled())
					_context.addTraceMessage("    comparatorRes=|" + compRes + "|: l_rowPos=" + l_dt.getRowCount() + ", r_rowPos=" + r_dt.getRowCount());

		         // LEFT < RIGHT
				if (compRes < 0)
				{
					if (_context.isTraceEnabled())
						_context.addTraceMessage("    <<<<<<<<<<<<<<<<<<<<<<<<<<< LEFT PK EXISTS, but Right PK is missing... keep on reading LEFT SIDE <<<<<<");

					sink.addRightMissingRow(l_row);
					l_row = null;
				}
		         // LEFT > RIGHT
				else if (compRes > 0)
				{
					if (_context.isTraceEnabled())
						_context.addTraceMessage("    >>>>>>>>>>>>>>>>>>>>>>>>>>> RIGHT PK EXISTS, but Left PK is missing... keep on reading RIGHT SIDE >>>>>>");

					sink.addLeftMissingRow(r_row);
					r_row = null;
				}
				// at this point you know the keys are aligned
				else
				{
					if (_context.isTraceEnabled())
						_context.addTraceMessage("  * DO DIFF ROW CONTENT ================");
					
					diffRow(l_row, r_row, onlyOnColumnsArray);

					//sink.addDiffColumnValue(pk, colPos, left, right);
					l_row = null;
					r_row = null;
				}
			}

			
//			if (l_rowPos >= l_rows.size()) break;
//			if (r_rowPos >= r_rows.size()) break;
			
//			l_rowPos++;
//			r_rowPos++;
		}

		if (_context.isDebugEnabled())
			_context.addDebugMessage("l_rc="+l_dt.getRowCount()+", r_rc="+r_dt.getRowCount());

		
		// Did we abort the DIFF
		if (sink.getCurrentDiffCount() >= sink.getMaxDiffCount())
		{
			_context.addWarningMessage("The DIFF was aborted due to 'maxDiffCount' was reached after " + sink.getCurrentDiffCount() + " differences. This happened after LeftRowCount=" + l_dt.getRowCount() + ", RightRowCount=" + r_dt.getRowCount());
		}

		
		// close the ResultSets in DiffTables
		l_dt.close();
		r_dt.close();

		if ( _context.getProgressDialog() != null )
			_context.getProgressDialog().setState("Closing the ResultSets...");
		
		if (_context.isDebugEnabled())
			_context.addDebugMessage("Closing the ResultSets...");
	}

	private int diffRow(Object[] l_row, Object[] r_row, boolean[] onlyOnColumnsArray)
	throws DiffException
	{
		if (l_row.length != r_row.length)
			throw new DiffException("Column length for right/left are different expecting same column count. l_row.length="+l_row.length+", r_row.length="+r_row.length);

		List<Integer> colIds = null;
		Object[] pk = null;
		for (int c=0; c<l_row.length; c++)
		{
			Object l = l_row[c];
			Object r = r_row[c];

			if (onlyOnColumnsArray != null && onlyOnColumnsArray[c] == false)
			{
				if (_context.isTraceEnabled())
					_context.addTraceMessage("      Skipping diff for: c="+c+", pk="+StringUtil.toCommaStr(pk)+", left=|"+l+"|, right=|"+r+"|.");

				continue;
			}
				
			if (l != null && (l instanceof Comparable || l instanceof byte[]) )
			{
//System.out.println(" COL-DIFF[Comparable]: c="+c+", pk="+StringUtil.toCommaStr(pk)+", left=|"+l+"|, right=|"+r+"|. (l=|"+l.getClass().getCanonicalName()+"|, r=|"+r.getClass().getCanonicalName()+"|)");
				int retval = _context.getColumnComparator(c).compare(l, r);
				if (retval != 0)
				{
					if (pk == null)
						pk = _context.getLeftDt().getPkCopy();

					if (colIds == null)
						colIds = new ArrayList<>();
					
					if (_context.isTraceEnabled())
						_context.addTraceMessage("      <<--[" + retval + "]-->> COL DIFF: c="+c+", pk="+StringUtil.toCommaStr(pk)+", left=|"+l+"|, right=|"+r+"|.");

//System.out.println("      <<--[" + retval + "]-->> COL DIFF: c="+c+", pk="+StringUtil.toCommaStr(pk)+", left=|"+l+"|, right=|"+r+"|.");
					colIds.add(c);
				}
			}
			else
			{
//System.out.println(" COL-DIFF[NoInstanceOfComparable]: c="+c+", pk="+StringUtil.toCommaStr(pk)+", left=|"+l+"|, right=|"+r+"|. (l=|"+l.getClass().getCanonicalName()+"|, r=|"+r.getClass().getCanonicalName()+"|)");
				boolean different = false;
				
//				if ( l != r)
//					different = true;
				
				if (!different && l != null && ! l.equals(r))
					different = true;

				if (!different && r != null && ! r.equals(l))
					different = true;
					
				if (different)
				{
					if (pk == null)
						pk = _context.getLeftDt().getPkCopy();

					if (colIds == null)
						colIds = new ArrayList<>();
					
					if (_context.isTraceEnabled())
						_context.addTraceMessage("      <<--[NoInstanceOfComparable]-->> COL DIFF: c="+c+", pk="+StringUtil.toCommaStr(pk)+", left=|"+l+"|, right=|"+r+"|.");

//System.out.println("      <<--[NoInstanceOfComparable]-->> COL DIFF: c="+c+", pk="+StringUtil.toCommaStr(pk)+", left=|"+l+"|, right=|"+r+"|. (l=|"+l.getClass().getCanonicalName()+"|, r=|"+r.getClass().getCanonicalName()+"|)");
					colIds.add(c);
				}
			}
		}

		// we have X number of columns that did NOT match
		if (colIds != null)
		{
			_context.getSink().addDiffColumnValues(pk, colIds, colIds, l_row, r_row);
			
			return colIds.size();
		}
		
		return 0;
	}
}






//public int compare(T[] lhs_, T[] rhs_) {
//  T lhsValue = lhs_[_lhsIdx];
//  T rhsValue = rhs_[_rhsIdx];
//  boolean lhsNull = (lhsValue == null) ? true : false;
//  boolean rhsNull = (rhsValue == null) ? true : false;
//  if (lhsNull && rhsNull)
//     return 0;
//  else if (lhsNull)
//     return -1;
//  else if (rhsNull)
//     return 1;
//
//  return _comparator.compare(lhsValue, rhsValue);
//}

///**
//* Subclass commons ComparatorChain just to get a decent looking toString()
//* 
//* @author jpanico
//*/
//public class DKComparatorChain extends ComparatorChain { <<<<<<<<<<<<<<<<<< CommonCollections
//
// private static final long serialVersionUID = 1L;
//
// public DKComparatorChain() {
//    super();
// }
//
// public DKComparatorChain(Comparator... comparators_) {
//    super(Arrays.asList(comparators_));
// }
//
// public String toString() {
//    return String.format("%s[%s]", ClassUtils.getShortClassName(this.getClass()),
//       comparatorChain);
// }
//}

///**
//* Copyright 2010-2011 Joseph Panico
//*
//* Licensed under the Apache License, Version 2.0 (the "License");
//* you may not use this file except in compliance with the License.
//* You may obtain a copy of the License at
//*
//*   http://www.apache.org/licenses/LICENSE-2.0
//*
//* Unless required by applicable law or agreed to in writing, software
//* distributed under the License is distributed on an "AS IS" BASIS,
//* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//* See the License for the specific language governing permissions and
//* limitations under the License.
//*/
//package org.diffkit.diff.engine;
//
//import java.io.IOException;
//import java.util.Arrays;
//import java.util.Comparator;
//import java.util.Map;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import org.diffkit.common.DKValidate;
//import org.diffkit.common.annot.NotThreadSafe;
//import org.diffkit.common.annot.Stateless;
//import org.diffkit.diff.engine.DKContext.UserKey;
//
///**
//* @author jpanico
//*/
//@NotThreadSafe
//@Stateless
//public class DKDiffEngine {
// private static final long PROGRESS_BATCH_SIZE = 1000;
// private final Logger _log = LoggerFactory.getLogger(this.getClass());
// private static final Logger USER_LOG = LoggerFactory.getLogger("user");
// private final boolean _isDebug = _log.isDebugEnabled();
//
// public DKContext diff(DKSource lhs_, DKSource rhs_, DKSink sink_,
//                       DKTableComparison tableComparison_,
//                       Map<UserKey, ?> userDictionary_) throws IOException {
//    _log.debug("lhs_->{}", lhs_);
//    _log.debug("rhs_->{}", rhs_);
//    _log.debug("sink_->{}", sink_);
//    _log.debug("tableComparison_->{}", tableComparison_.getDescription());
//    _log.debug("userDictionary_->{}", userDictionary_);
//
//    DKValidate.notNull(lhs_, rhs_, sink_, tableComparison_);
//    DKContext context = new DKContext(lhs_, rhs_, sink_, tableComparison_,
//       userDictionary_);
//    _log.debug("context->{}", context);
//    this.diff(context);
//    return context;
// }
//
// protected void diff(DKContext context_) throws IOException {
//    long maxDiffs = context_._tableComparison.getMaxDiffs();
//    _log.debug("maxDiffs->{}", maxDiffs);
//    context_.open();
//    int oneSide = -1;
//    Object[][] rows = new Object[2][];
//    Comparator<Object[]> rowComparator = context_._tableComparison.getRowComparator();
//    _log.debug("rowComparator->{}", rowComparator);
//    while (context_._sink.getDiffCount() < maxDiffs) {
//       if (_isDebug)
//          _log.debug("diffCount->{}", context_._sink.getDiffCount());
//       boolean oneSided = false;
//       context_._rowStep++;
//       context_._columnStep = 0;
//       if (context_._rowStep % PROGRESS_BATCH_SIZE == 0)
//          USER_LOG.info("->{}", context_._rowStep);
//       if (rows[DKSide.LEFT_INDEX] == null)
//          rows[DKSide.LEFT_INDEX] = context_._lhs.getNextRow();
//       if (rows[DKSide.LEFT_INDEX] == null) {
//          oneSided = true;
//          oneSide = DKSide.RIGHT_INDEX;
//       }
//       if (rows[DKSide.RIGHT_INDEX] == null)
//          rows[DKSide.RIGHT_INDEX] = context_._rhs.getNextRow();
//       if (rows[DKSide.RIGHT_INDEX] == null) {
//          if (oneSided)
//             break;
//          oneSided = true;
//          oneSide = DKSide.LEFT_INDEX;
//       }
//       if (_isDebug) {
//          _log.debug("oneSided->{}", oneSided);
//          _log.debug("oneSide->{}", oneSide);
//       }
//       if (oneSided) {
//          this.recordRowDiff(rows[oneSide], oneSide, context_, context_._sink);
//          rows[oneSide] = null;
//          continue;
//       }
//       assert ((rows[DKSide.LEFT_INDEX] != null) && (rows[DKSide.RIGHT_INDEX] != null));
//       int comparison = rowComparator.compare(rows[DKSide.LEFT_INDEX],
//          rows[DKSide.RIGHT_INDEX]);
//       // LEFT < RIGHT
//       if (comparison < 0) {
//          this.recordRowDiff(rows[DKSide.LEFT_INDEX], DKSide.LEFT_INDEX, context_,
//             context_._sink);
//          rows[DKSide.LEFT_INDEX] = null;
//       }
//       // LEFT > RIGHT
//       else if (comparison > 0) {
//          this.recordRowDiff(rows[DKSide.RIGHT_INDEX], DKSide.RIGHT_INDEX, context_,
//             context_._sink);
//          rows[DKSide.RIGHT_INDEX] = null;
//       }
//       // at this point you know the keys are aligned
//       else {
//          this.diffRow(rows[DKSide.LEFT_INDEX], rows[DKSide.RIGHT_INDEX], context_,
//             context_._sink);
//          rows[DKSide.LEFT_INDEX] = null;
//          rows[DKSide.RIGHT_INDEX] = null;
//       }
//    }
//    context_.close();
// }
//
// private void diffRow(Object[] lhs_, Object[] rhs_, DKContext context_, DKSink sink_)
//    throws IOException {
//    DKDiff.Kind kind = context_._tableComparison.getKind();
//    if (kind == DKDiff.Kind.ROW_DIFF)
//       return;
//    int[] diffIndexes = context_._tableComparison.getDiffIndexes();
//    _log.debug("diffIndexes->{}", Arrays.toString(diffIndexes));
//    if ((diffIndexes == null) || (diffIndexes.length == 0))
//       return;
//    DKColumnComparison[] columnComparisons = context_._tableComparison.getMap();
//    _log.debug("columnComparisons->{}", Arrays.toString(columnComparisons));
//    // not supposed to happen, but play it safe
//    if ((columnComparisons == null) || (columnComparisons.length == 0))
//       return;
//    DKColumnDiffRow diffRow = null;
//    for (int i = 0; i < diffIndexes.length; i++) {
//       context_._columnStep++;
//       context_._lhsColumnIdx = columnComparisons[diffIndexes[i]]._lhsColumn.getIndex();
//       context_._rhsColumnIdx = columnComparisons[diffIndexes[i]]._rhsColumn.getIndex();
//       if (columnComparisons[diffIndexes[i]].isDiff(lhs_, rhs_, context_)) {
//          if (diffRow == null)
//             // key side arbitrary; keyValeus guaranteed to match on both
//             // sides
//             diffRow = new DKColumnDiffRow(context_._rowStep, lhs_, rhs_,
//                context_._tableComparison);
//          DKColumnDiff columnDiff = diffRow.createDiff(context_._columnStep,
//             columnComparisons[diffIndexes[i]].getLHValue(lhs_),
//             columnComparisons[diffIndexes[i]].getRHValue(rhs_));
//          sink_.record(columnDiff, context_);
//       }
//    }
// }
//
// private void recordRowDiff(Object[] row_, int sideIdx_, DKContext context_,
//                            DKSink sink_) throws IOException {
//    _log.debug("row_->{} sideIdx_->{}", row_, sideIdx_);
//    DKDiff.Kind kind = context_._tableComparison.getKind();
//    if (kind == DKDiff.Kind.COLUMN_DIFF)
//       return;
//    if (row_ == null)
//       return;
//    DKTableComparison tableComparison = context_.getTableComparison();
//    long rowStep = context_.getRowStep();
//    DKSide side = DKSide.getEnumForConstant(sideIdx_);
//    DKRowDiff rowDiff = new DKRowDiff(rowStep, row_, side, tableComparison);
//    sink_.record(rowDiff, context_);
// }
//
//}
