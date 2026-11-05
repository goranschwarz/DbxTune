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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import com.dbxtune.pcs.report.content.IndexRedundancy.Finding;
import com.dbxtune.pcs.report.content.IndexRedundancy.Idx;

public class IndexRedundancyTest
{
	private static List<String> cols(String commaList)
	{
		List<String> list = new ArrayList<>();
		if ( ! commaList.isEmpty() )
			for (String s : commaList.split(","))
				list.add(s.trim());
		return list;
	}

	private static Idx ix(String name, String keys)                  { return new Idx(name, cols(keys), new ArrayList<>()); }
	private static Idx ix(String name, String keys, String includes) { return new Idx(name, cols(keys), cols(includes)); }
	private static Idx unique(Idx i)   { i.unique = true; return i; }
	private static Idx pk(Idx i)       { i.primaryKey = true; i.unique = true; return i; }
	private static Idx uc(Idx i)       { i.uniqueConstraint = true; i.unique = true; return i; }
	private static Idx clustered(Idx i){ i.clustered = true; return i; }

	private static List<Finding> run(Idx... idx)                  { return IndexRedundancy.analyze(Arrays.asList(idx), null); }
	private static List<Finding> run(String ck, Idx... idx)       { return IndexRedundancy.analyze(Arrays.asList(idx), cols(ck)); }

	private static String names(List<Idx> list)
	{
		List<String> n = new ArrayList<>();
		for (Idx i : list) n.add(i.name);
		Collections.sort(n);
		return String.join(",", n);
	}

	private static void assertOne(List<Finding> f, String kept, String dropped, String added)
	{
		assertEquals("number of findings: " + f.size(), 1, f.size());
		assertEquals(kept,    f.get(0).kept.name);
		assertEquals(dropped, names(f.get(0).dropped));
		assertEquals(added,   String.join(",", f.get(0).addIncludes));
	}

	private static String allWhy(Finding f) { return String.join(" | ", f.why); }

	// ---------------- R1: exact duplicate
	@Test public void r1_exactDuplicate()
	{
		Idx a = ix("a", "c1,c2"); a.reads = 5;
		Idx b = ix("b", "c1,c2"); b.reads = 100;
		assertOne(run(a, b), "b", "a", "");
	}

	@Test public void r1_sameKeys_includesCovered()
	{
		assertOne(run(ix("a", "c1", "c2"), ix("b", "c1", "c2,c3")), "b", "a", "");
	}

	@Test public void r1_tieGoesToLowestId()
	{
		Idx a = ix("a", "c1"); a.id = 7;
		Idx b = ix("b", "c1"); b.id = 3;
		assertOne(run(a, b), "b", "a", "");
	}

	@Test public void r1_nonUniqueDroppedNextToUnique()
	{
		assertOne(run(ix("a", "c1"), unique(ix("b", "c1"))), "b", "a", "");
	}

	@Test public void r1_twoUniqueSameKeys()
	{
		Idx a = unique(ix("a", "c1")); a.reads = 1;
		Idx b = unique(ix("b", "c1")); b.reads = 2;
		assertOne(run(a, b), "b", "a", "");
	}

	@Test public void r1_ncSameKeysAsClustered()
	{
		List<Finding> f = run("c1", clustered(ix("cl", "c1")), ix("a", "c1", "c5"));
		assertOne(f, "cl", "a", "");
		assertTrue(allWhy(f.get(0)), allWhy(f.get(0)).contains("clustered index"));
	}

	// ---------------- R2: covered prefix
	@Test public void r2_prefix()
	{
		assertOne(run(ix("a", "c1"), ix("b", "c1,c2")), "b", "a", "");
	}

	@Test public void r2_includeInLongerKey()
	{
		assertOne(run(ix("a", "c1", "c2"), ix("b", "c1,c2")), "b", "a", "");
	}

	@Test public void r2_chain_allGoToTheLongest()
	{
		assertOne(run(ix("a", "c1"), ix("b", "c1,c2"), ix("c", "c1,c2,c3")), "c", "a,b", "");
	}

	// ---------------- R3: same keys, different include
	@Test public void r3_merge()
	{
		Idx a = ix("a", "c1", "x"); a.reads = 10;
		Idx b = ix("b", "c1", "y"); b.reads = 1;
		assertOne(run(a, b), "a", "b", "y");
	}

	@Test public void r3_threeWayMerge()
	{
		Idx a = ix("a", "c1", "x"); a.reads = 10;
		Idx b = ix("b", "c1", "y");
		Idx c = ix("c", "c1", "z");
		assertOne(run(a, b, c), "a", "b,c", "y,z");
	}

	// ---------------- R4: prefix, extra include
	@Test public void r4_merge()
	{
		assertOne(run(ix("a", "c1", "x"), ix("b", "c1,c2")), "b", "a", "x");
	}

	@Test public void r4_chain_carriesIncludesToTheEnd()
	{
		assertOne(run(ix("a", "c1", "x"), ix("b", "c1,c2", "y"), ix("c", "c1,c2,c3")), "c", "a,b", "x,y");
	}

	@Test public void prefersNoMergeOverMerge()
	{
		// 'b' has the column, 'c' is longer but would need it added
		assertOne(run(ix("a", "c1", "x"), ix("b", "c1,c2", "x"), ix("c", "c1,c9")), "b", "a", "");
	}

	// ---------------- Never
	@Test public void never_dropsPrimaryKey()
	{
		List<Finding> f = run(pk(ix("pk", "c1")), ix("b", "c1,c2"));
		assertTrue(f.isEmpty());
	}

	@Test public void never_dropsUniqueConstraint()
	{
		assertTrue(run(uc(ix("uc", "c1")), ix("b", "c1,c2")).isEmpty());
	}

	@Test public void never_dropsClustered()
	{
		assertTrue(run(clustered(ix("cl", "c1")), ix("b", "c1,c2")).isEmpty());
	}

	@Test public void never_dropsUniquePrefix()
	{
		assertTrue(run(unique(ix("u", "c1")), ix("b", "c1,c2")).isEmpty());
	}

	@Test public void never_dropsUniqueIntoNonUniqueSameKeys()
	{
		List<Finding> f = run(unique(ix("u", "c1")), ix("b", "c1"));
		assertOne(f, "u", "b", ""); // the non-unique one goes, never the unique one
	}

	@Test public void never_mergesIntoPrimaryKey()
	{
		assertTrue(run(pk(ix("pk", "c1")), ix("a", "c1", "x")).isEmpty());
	}

	@Test public void never_mergesIntoUniqueConstraint()
	{
		assertTrue(run(uc(ix("uc", "c1")), ix("a", "c1", "x")).isEmpty());
	}

	@Test public void never_differentOrder()
	{
		assertTrue(run(ix("a", "c1,c2"), ix("b", "c2,c1")).isEmpty());
	}

	@Test public void never_differentDirection()
	{
		assertTrue(run(ix("a", "c1"), ix("b", "c1 DESC")).isEmpty());
		assertTrue(run(ix("a", "c1,c2"), ix("b", "c1,c2 DESC")).isEmpty());
	}

	@Test public void never_notFirstColumn()
	{
		assertTrue(run(ix("a", "c2"), ix("b", "c1,c2")).isEmpty());
	}

	@Test public void never_differentIndexes()
	{
		assertTrue(run(ix("a", "c1"), ix("b", "c2")).isEmpty());
	}

	@Test public void singleIndex()
	{
		assertTrue(run(ix("a", "c1")).isEmpty());
		assertTrue(IndexRedundancy.analyze(new ArrayList<>(), null).isEmpty());
	}

	@Test public void uniqueKeptWithInclude_canBeMergeTarget()
	{
		assertOne(run(unique(ix("u", "c1")), ix("a", "c1", "x")), "u", "a", "x");
	}

	@Test public void keptIndexIsNeverDropped()
	{
		List<Finding> f = run(ix("a", "c1"), ix("a2", "c1"), ix("b", "c1,c2"), ix("c", "c1,c2,c3"), ix("d", "c9"), ix("d2", "c9"));
		for (Finding x : f)
			for (Finding y : f)
				for (Idx dropped : y.dropped)
					assertFalse(dropped.name, x.kept == dropped);
		assertEquals(2, f.size());
	}

	// ---------------- Hidden clustering key
	@Test public void hidden_sameOrder_saysWhy()
	{
		List<Finding> f = run("id", clustered(pk(ix("pk", "id"))), ix("a", "c1"), ix("b", "c1,id"));
		assertOne(f, "b", "a", "");
		assertTrue(allWhy(f.get(0)), allWhy(f.get(0)).contains("already has it at the end"));
	}

	@Test public void hidden_differentOrder_warns()
	{
		List<Finding> f = run("id", clustered(pk(ix("pk", "id"))), ix("a", "c1"), ix("b", "c1,c2"));
		assertOne(f, "b", "a", "");
		assertTrue(allWhy(f.get(0)), allWhy(f.get(0)).contains("ordered differently"));
	}

	@Test public void hidden_includeOfClusteringKey_isDropNotMerge()
	{
		List<Finding> f = run("id", clustered(pk(ix("pk", "id"))), ix("a", "c1", "id"), ix("b", "c1,c2"));
		assertOne(f, "b", "a", "");
		assertTrue(allWhy(f.get(0)), allWhy(f.get(0)).contains("is not needed"));
	}

	@Test public void hidden_mergeLeavesOutClusteringKey()
	{
		Idx a = ix("a", "c1", "id,x"); a.reads = 1;
		Idx b = ix("b", "c1", "y");    b.reads = 9;
		assertOne(run("id", a, b), "b", "a", "x");
	}

	@Test public void heap_noHiddenText()
	{
		List<Finding> f = run(ix("a", "c1"), ix("b", "c1,c2"));
		assertFalse(allWhy(f.get(0)), allWhy(f.get(0)).contains("clustering key"));
	}

	@Test public void uniqueKept_hiddenOnlyInLeaf()
	{
		// Unique b has no hidden key: (c1,c2) while a really is (c1,id)
		List<Finding> f = run("id", clustered(pk(ix("pk", "id"))), ix("a", "c1"), unique(ix("b", "c1,c2")));
		assertOne(f, "b", "a", "");
		assertTrue(allWhy(f.get(0)), allWhy(f.get(0)).contains("ordered differently"));
	}

	@Test public void everyFindingHasWhy()
	{
		for (Finding f : run(ix("a", "c1"), ix("b", "c1,c2"), ix("x", "c5", "p"), ix("y", "c5", "q")))
			assertFalse(f.why.isEmpty());
	}
}
