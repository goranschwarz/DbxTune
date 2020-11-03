/*******************************************************************************
 * Copyright (C) 2010-2019 Goran Schwarz
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
package com.asetune.gui.swing;

import java.text.Collator;
import java.util.Comparator;

import javax.swing.table.TableModel;

import org.jdesktop.swingx.sort.TableSortController;

import com.jidesoft.comparator.NumberComparator;

//------------------------------------------------
// workaround to handle "(NULL)" values on Timestamps and other issues, fallback is to compare them as strings in case of **Failures**
// This workaround was inspired by: http://hg.netbeans.org/main-golden/rev/38b758d03e6d
//
// NOTE: Remove this when we have implemented a "renderer" that displays (NULL) instead of storing/returning (NULL) in the model. 
//------------------------------------------------
public class GTableSortController
<M extends TableModel> extends TableSortController<M>
{
	@SuppressWarnings({ "rawtypes" })
	public static final Comparator GTABLE_COMPARATOR = new GTableComparator();


	public GTableSortController(M model)
	{
		super(model);
	}

	@Override
	public Comparator<?> getComparator(int column)
	{
		Comparator<?> comparator = super.getComparator(column);
		if ( comparator != null )
		{
			if (comparator == COMPARABLE_COMPARATOR)
				return GTABLE_COMPARATOR;
			return comparator;
		}

		Class<?> columnClass = getModel().getColumnClass(column);
		if ( columnClass == String.class )
		{
			return Collator.getInstance();
		}

		if ( Comparable.class.isAssignableFrom(columnClass) )
		{
//			return COMPARABLE_COMPARATOR;
			return GTABLE_COMPARATOR;
		}
		return Collator.getInstance();
	}





	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static class GTableComparator implements Comparator
	{
		@Override
		public int compare(Object o1, Object o2)
		{
//System.out.println("XXXXXXXXXXXXX: compare(o1="+o1+", o2="+o2+") --- ResultSetJXTableComparator");
			try
			{
		        if      (o1 == null && o2 == null) return 0;
		        else if (o1 == null)               return -1;
		        else if (o2 == null)               return 1;

		        return ((Comparable)o1).compareTo(o2);
			}
			catch (Exception ex)
			{
				// If both are Numbers... but of different types like: Integer & Long
				if (o1 instanceof Number && o2 instanceof Number)
				{
					return NumberComparator.getInstance().compare(o1, o2);
				}

				String s1 = o1 != null ? o1.toString() : ""; // NOI18N
				String s2 = o2 != null ? o2.toString() : ""; // NOI18N
				return s1.compareTo(s2);
			}
		}
	}
}
