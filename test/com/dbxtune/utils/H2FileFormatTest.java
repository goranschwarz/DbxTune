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
package com.dbxtune.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

import org.junit.Assume;
import org.junit.Test;

import com.dbxtune.utils.H2FileFormat.H2JarInfo;
import com.dbxtune.utils.H2FileFormat.OldFormatAction;
import com.dbxtune.utils.H2FileFormat.SpaceCheckResult;

public class H2FileFormatTest
{
	private static File writeHeader(String header)
	throws IOException
	{
		File f = File.createTempFile("h2FileFormatTest_", ".mv.db");
		f.deleteOnExit();
		byte[] buf = new byte[4096 * 2]; // header block + \0 padding
		byte[] hdr = (header + "\n").getBytes(StandardCharsets.ISO_8859_1);
		System.arraycopy(hdr, 0, buf, 0, hdr.length);
		Files.write(f.toPath(), buf);
		return f;
	}

	@Test
	public void readFormat()
	throws IOException
	{
		assertEquals(1, H2FileFormat.readFormat(writeHeader("H:2,block:6,blockSize:1000,chunk:aa,created:19740fd1ebf,format:1,version:aa,fletcher:8fc75088")));
		assertEquals(2, H2FileFormat.readFormat(writeHeader("H:2,block:6,blockSize:1000,chunk:aa,clean:1,created:19740fd1ebf,format:2,version:aa,fletcher:8fc75088")));
		assertEquals(3, H2FileFormat.readFormat(writeHeader("H:2,block:f2,blockSize:1000,chunk:b,clean:1,created:1a0bfa69fab,format:3,version:b,fletcher:a0b31cb5")));

		// Not a MVStore header
		assertEquals(-1, H2FileFormat.readFormat(writeHeader("this is not a h2 file")));
		assertEquals(-1, H2FileFormat.readFormat(writeHeader("")));
	}

	@Test
	public void readFormatRead()
	throws IOException
	{
		// 'formatRead' overrides 'format', when present
		assertEquals(3, H2FileFormat.readFormatRead(writeHeader("H:2,blockSize:1000,format:4,formatRead:3,fletcher:0")));
		assertEquals(2, H2FileFormat.readFormatRead(writeHeader("H:2,blockSize:1000,format:2,fletcher:0")));
	}

	@Test
	public void currentFormat()
	throws IOException
	{
		// H2 2.2 and later writes/reads format 3
		assertEquals(3, H2FileFormat.getCurrentFormat());
		assertEquals(3, H2FileFormat.getCurrentFormatReadMin());
		assertEquals(3, H2FileFormat.getCurrentFormatReadMax());

		assertTrue ( H2FileFormat.needsUpgrade(writeHeader("H:2,blockSize:1000,format:2,fletcher:0")) );
		assertFalse( H2FileFormat.needsUpgrade(writeHeader("H:2,blockSize:1000,format:3,fletcher:0")) );
		assertFalse( H2FileFormat.needsUpgrade(writeHeader("not a h2 file")) );
		assertTrue ( H2FileFormat.isTooNew    (writeHeader("H:2,blockSize:1000,format:4,fletcher:0")) );
	}

	@Test
	public void jarInfo()
	{
		File oldJar = new File("lib/h2-2.1.214.jar");
		File newJar = new File("lib/h2-2.4.240.jar");
		Assume.assumeTrue("H2 JARs not found in 'lib' (run from project dir)", oldJar.exists() && newJar.exists());

		H2JarInfo oldInfo = H2FileFormat.getJarInfo(oldJar);
		assertNotNull(oldInfo);
		assertEquals("2.1.214", oldInfo.version);
		assertEquals(2, oldInfo.formatReadMin);
		assertEquals(2, oldInfo.formatReadMax);

		H2JarInfo newInfo = H2FileFormat.getJarInfo(newJar);
		assertNotNull(newInfo);
		assertEquals("2.4.240", newInfo.version);
		assertEquals(3, newInfo.formatReadMin);
		assertEquals(3, newInfo.formatReadMax);

		// Not a H2 JAR
		File otherJar = new File("lib").listFiles((d, n) -> n.startsWith("commons-") && n.endsWith(".jar"))[0];
		assertNull(H2FileFormat.getJarInfo(otherJar));

		// Search: the H2 we are running with (2.4.240) is skipped
		List<File> dirs = Arrays.asList(new File("lib"));
		assertEquals(oldJar.getAbsoluteFile(), H2FileFormat.findH2JarForFormat(2, dirs).jarFile.getAbsoluteFile());
		assertNull(H2FileFormat.findH2JarForFormat(1, dirs));

		// Format 3: the running H2 is never returned, but another H2 2.2+ JAR in 'lib' may be (for example h2-2.5.252.jar)
		H2JarInfo fmt3 = H2FileFormat.findH2JarForFormat(3, dirs);
		if (fmt3 != null)
		{
			assertTrue(fmt3.canRead(3));
			assertFalse(fmt3.jarFile.getAbsoluteFile().equals(H2FileFormat.getCurrentH2Jar().getAbsoluteFile()));
		}
	}

	@Test
	public void compareVersions()
	{
		assertTrue(H2FileFormat.compareVersions("2.4.240", "2.1.214") > 0);
		assertTrue(H2FileFormat.compareVersions("2.1.214", "2.10.1" ) < 0); // numeric, not string compare
		assertTrue(H2FileFormat.compareVersions("1.4.200", "2.0.202") < 0);
		assertEquals(0, H2FileFormat.compareVersions("2.5.252", "2.5.252"));
	}

	@Test
	public void parseAction()
	{
		assertEquals(OldFormatAction.COPY_UPGRADE, H2FileFormat.parseAction("COPY_UPGRADE",   OldFormatAction.ERROR));
		assertEquals(OldFormatAction.COPY_UPGRADE, H2FileFormat.parseAction(" copy_upgrade ", OldFormatAction.ERROR));
		assertEquals(OldFormatAction.COPY_UPGRADE, H2FileFormat.parseAction(null,             OldFormatAction.COPY_UPGRADE));
		assertEquals(OldFormatAction.ERROR,        H2FileFormat.parseAction("",               OldFormatAction.ERROR));
		assertEquals(OldFormatAction.ERROR,        H2FileFormat.parseAction("no-such-action", OldFormatAction.COPY_UPGRADE));
	}

	@Test
	public void checkFreeSpace()
	throws IOException
	{
		File dir = Files.createTempDirectory("h2FileFormatTest_space_").toFile();
		try
		{
			File dbFile = new File(dir, "DBXTUNE_CENTRAL_DB.mv.db");
			Files.write(dbFile.toPath(), new byte[1024]);

			// Enough space
			SpaceCheckResult ok = H2FileFormat.checkFreeSpace(dbFile, 1.0, 0);
			assertTrue(ok.ok);

			// Some recordings, with different ages (oldest = SRV_2020-01-01)
			File r1 = createFile(dir, "SRV_2020-01-03.mv.db",  3000, 3);
			File r2 = createFile(dir, "SRV_2020-01-01.mv.db",  1000, 1);
			File r3 = createFile(dir, "SRV_2020-01-02.mv.db",  2000, 2);
			File lo = createFile(dir, "DBXTUNE_CENTRAL_DB.mv.db.h2fmt2.20200101_000000.bak", 500, 1);
			createFile(dir, "some_other_file.txt", 500, 1);

			// Force "not enough space"
			SpaceCheckResult res = H2FileFormat.checkFreeSpace(dbFile, 1_000_000_000_000.0, 0);
			assertFalse(res.ok);
			assertNotNull(res.message);

			assertEquals(3,  res.recordingCandidates.size());
			assertEquals(r2, res.recordingCandidates.get(0)); // oldest first
			assertEquals(r3, res.recordingCandidates.get(1));
			assertEquals(r1, res.recordingCandidates.get(2));
			assertEquals(-1, res.recordingCandidatesNeeded);  // all of them is not enough

			assertEquals(1,  res.leftoverFiles.size());
			assertEquals(lo, res.leftoverFiles.get(0));

			assertTrue(res.message.contains("NOT ENOUGH FREE DISK SPACE"));
			assertTrue(res.message.contains("SRV_2020-01-01.mv.db"));
			assertTrue(res.message.contains("rm "));
		}
		finally
		{
			for (File f : dir.listFiles())
				f.delete();
			dir.delete();
		}
	}

	private static File createFile(File dir, String name, int size, int orderNum)
	throws IOException
	{
		File f = new File(dir, name);
		Files.write(f.toPath(), new byte[size]);
		f.setLastModified(System.currentTimeMillis() - (100L - orderNum) * 24 * 3600 * 1000); // lower 'orderNum' = older file
		return f;
	}
}
