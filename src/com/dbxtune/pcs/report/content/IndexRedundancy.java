/*******************************************************************************
 * Copyright (C) 2010-2027 Goran Schwarz
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
package com.dbxtune.pcs.report.content;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Finds indexes on ONE table that can be dropped or merged, from the index definitions only.
 * <p>
 * No database, no HTML and no vendor syntax in here. The caller decides which indexes are safe to
 * hand in (not filtered, not partitioned, ...), and builds the scripts.
 * <p>
 * A key column is a text like {@code [c1]} or {@code [c1] DESC}. Two key columns only match when the
 * text is the same, so {@code a} and {@code a DESC} are different. An include column is compared by
 * name.
 */
public class IndexRedundancy
{
	/** One index, as handed in by the caller */
	public static class Idx
	{
		public final String       name;
		public final List<String> keys;
		public final List<String> includes;
		public boolean primaryKey;
		public boolean uniqueConstraint;
		public boolean unique;
		public boolean clustered;
		/** For a clustered index: true if it holds the data rows (SQL Server, ASE allpages). false for a placement index (ASE datarows/datapages) */
		public boolean clusteredHoldsRows = true;
		public long    reads = -1;
		public int     id    = Integer.MAX_VALUE;

		public Idx(String name, List<String> keys, List<String> includes)
		{
			this.name     = name;
			this.keys     = keys;
			this.includes = includes == null ? new ArrayList<>() : includes;
		}

		@Override public String toString() { return name + keys + (includes.isEmpty() ? "" : " INCLUDE" + includes); }
	}

	/** Keep {@link #kept}, drop {@link #dropped}. If {@link #addIncludes} has columns, they are first added to the kept index: a merge. */
	public static class Finding
	{
		public final Idx          kept;
		public final List<Idx>    dropped     = new ArrayList<>();
		public final List<String> addIncludes = new ArrayList<>();
		public final List<String> why         = new ArrayList<>();

		private Finding(Idx kept) { this.kept = kept; }

		public boolean isMerge() { return ! addIncludes.isEmpty(); }
	}

	/**
	 * @param indexes      the indexes of one table
	 * @param clusterKeys  key columns of the clustered index, empty or null for a heap. Passed separately since the clustered
	 *                     index itself may not be one of the indexes handed in, for example when it is partitioned.
	 * @return one Finding per index that is kept, the most useful first
	 */
	public static List<Finding> analyze(List<Idx> indexes, List<String> clusterKeys)
	{
		List<String> ck = clusterKeys == null ? new ArrayList<>() : clusterKeys;

		// Shortest keys first, and for the same keys the "worst" index first: what is dropped here can move its work to a longer or better index
		List<Idx> order = new ArrayList<>(indexes);
		order.sort(Comparator.<Idx>comparingInt(i -> i.keys.size()).thenComparing(Comparator.comparing(IndexRedundancy::keepRank).reversed()));

		Map<Idx, Finding> byKept  = new HashMap<>();
		Set<Idx>          removed = new LinkedHashSet<>();

		for (Idx a : order)
		{
			Finding fromA  = byKept.get(a); // What was already moved to this index
			Idx     target = null;
			int     targetMissing = Integer.MAX_VALUE;

			for (Idx b : indexes)
			{
				if (b == a || removed.contains(b) || ! canDropInto(a, b))
					continue;

				List<String> missing = missingIn(a, fromA, b, byKept.get(b), ck);
				if ( ! missing.isEmpty() && ! canGetIncludes(b))
					continue;

				// Fewest columns to add. Then the longest keys: that one will not be dropped itself. Then the best index.
				boolean better = target == null
						|| missing.size() < targetMissing
						|| (missing.size() == targetMissing && b.keys.size() >  target.keys.size())
						|| (missing.size() == targetMissing && b.keys.size() == target.keys.size() && keepRank(b).compareTo(keepRank(target)) < 0);
				if (better)
				{
					target = b;
					targetMissing = missing.size();
				}
			}

			if (target == null)
				continue;

			Finding to = byKept.computeIfAbsent(target, Finding::new);
			for (String col : missingIn(a, fromA, target, to, ck))
				to.addIncludes.add(col);

			to.dropped.add(a);
			to.why.addAll(whyDrop(a, target, ck));

			// What was moved to 'a' now goes to the target
			if (fromA != null)
			{
				to.dropped.addAll(fromA.dropped);
				to.why    .addAll(fromA.why);
				byKept.remove(a);
			}
			removed.add(a);
		}

		List<Finding> result = new ArrayList<>(byKept.values());

		// In index id order: a rollback that recreates them in this order gets the old index ids back
		for (Finding f : result)
			f.dropped.sort(Comparator.comparingInt(i -> i.id));

		result.sort(Comparator.comparing((Finding f) -> f.isMerge()).thenComparing(f -> f.kept.name));
		return result;
	}

	/** Could 'a' go away if 'b' stays, looking at the keys and what the indexes enforce? */
	private static boolean canDropInto(Idx a, Idx b)
	{
		if (a.primaryKey || a.uniqueConstraint || a.clustered)
			return false;

		if ( ! isPrefix(a.keys, b.keys) )
			return false;

		boolean sameKeys = a.keys.size() == b.keys.size();

		// A unique index enforces a rule. Only another unique index with exactly the same keys enforces the same rule.
		if (a.unique && ! (b.unique && sameKeys))
			return false;

		// For the same keys, only the worse one goes
		if (sameKeys && keepRank(b).compareTo(keepRank(a)) > 0)
			return false;

		return true;
	}

	/** A primary key, a unique constraint and a clustered index can't get INCLUDE columns */
	private static boolean canGetIncludes(Idx i)
	{
		return ! (i.primaryKey || i.uniqueConstraint || i.clustered);
	}

	/** The columns of 'a' (and of what was moved to it), that the target does not have */
	private static List<String> missingIn(Idx a, Finding fromA, Idx b, Finding toB, List<String> ck)
	{
		List<String> result = new ArrayList<>();
		if (b.clustered) // A clustered index has every column
			return result;

		Set<String> has = new LinkedHashSet<>();
		for (String c : b.keys)     has.add(colName(c));
		for (String c : b.includes) has.add(colName(c));
		for (String c : ck)         has.add(colName(c)); // In every nonclustered index, see the class header
		if (toB != null)
			for (String c : toB.addIncludes) has.add(colName(c));

		List<String> wanted = new ArrayList<>(a.includes);
		if (fromA != null)
			wanted.addAll(fromA.addIncludes);

		for (String c : wanted)
			if (has.add(colName(c)))
				result.add(c);
		return result;
	}

	private static List<String> whyDrop(Idx a, Idx b, List<String> ck)
	{
		List<String> why = new ArrayList<>();

		if (a.keys.size() == b.keys.size())
			why.add(a.name + " and " + b.name + " have the same key columns: " + str(a.keys) + ".");
		else
			why.add("The key columns of " + a.name + " " + str(a.keys) + " are the first key columns of " + b.name + " " + str(b.keys) + ", so " + b.name + " can do the same seeks.");

		if (b.clustered && b.clusteredHoldsRows)
			why.add(b.name + " is the clustered index, so it has every column. But it is wider than " + a.name + ", so a scan that used " + a.name + " reads more pages.");
		else if (b.clustered)
			why.add(b.name + " is the clustered index, but on this table it is a placement index: it does not hold the data rows, it has the same keys as a nonclustered index.");

		// The clustering key at the end of a non-unique nonclustered index
		if ( ! ck.isEmpty() && ! a.unique && ! a.clustered && a.keys.size() != b.keys.size())
		{
			List<String> hidden = hiddenKeys(a.keys, ck);
			if ( ! hidden.isEmpty() )
			{
				List<String> effA = effectiveKeys(a, ck);
				List<String> effB = effectiveKeys(b, ck);
				if (isPrefix(effA, effB))
					why.add(str(hidden) + " is the clustering key, so " + a.name + " already has it at the end of its key: it really is " + str(effA) + ", the same order as " + b.name + ".");
				else
					why.add(str(hidden) + " is the clustering key, so " + a.name + " really is " + str(effA) + ", ordered differently from " + b.name + ". A query that seeks on all of " + str(effA) + ", or sorts on it, works best with " + a.name + ", check for those.");
			}
		}

		for (String c : a.includes)
		{
			if ( ! b.clustered && containsCol(ck, c) && ! containsCol(b.keys, c) && ! containsCol(b.includes, c) )
				why.add("INCLUDE " + c + " in " + a.name + " is not needed: it is part of the clustering key, which every nonclustered index has.");
		}
		return why;
	}

	/** The key of a non-unique nonclustered index ends with the clustering key. A unique one has it in the leaf level only, so not in the key. */
	private static List<String> effectiveKeys(Idx i, List<String> ck)
	{
		List<String> result = new ArrayList<>(i.keys);
		if ( ! i.unique && ! i.clustered )
			result.addAll(hiddenKeys(i.keys, ck));
		return result;
	}

	private static List<String> hiddenKeys(List<String> keys, List<String> ck)
	{
		List<String> result = new ArrayList<>();
		for (String c : ck)
			if ( ! containsCol(keys, c) )
				result.add(c);
		return result;
	}

	private static boolean isPrefix(List<String> shorter, List<String> longer)
	{
		if (shorter.isEmpty() || shorter.size() > longer.size())
			return false;
		for (int i=0; i<shorter.size(); i++)
			if ( ! shorter.get(i).equals(longer.get(i)) )
				return false;
		return true;
	}

	private static boolean containsCol(List<String> cols, String col)
	{
		String name = colName(col);
		for (String c : cols)
			if (colName(c).equals(name))
				return true;
		return false;
	}

	/** A column list as text: ([c1], [c2] DESC) */
	public static String str(List<String> cols)
	{
		return "(" + String.join(", ", cols) + ")";
	}

	/** [c1] DESC -> [c1] */
	private static String colName(String col)
	{
		// Also Postgres: "a DESC NULLS LAST", "a NULLS FIRST"
		return col.trim().replaceAll("(?i)( (ASC|DESC))?( NULLS (FIRST|LAST))?$", "").trim();
	}

	/** The lower, the more reason to keep it. Primary key, unique constraint, unique, clustered, most reads, lowest id */
	private static String keepRank(Idx i)
	{
		int kind = i.primaryKey ? 0 : i.uniqueConstraint ? 1 : i.unique ? 2 : i.clustered ? 3 : 4;
		return String.format("%d %020d %010d", kind, Long.MAX_VALUE - Math.max(i.reads, 0), i.id);
	}
}
