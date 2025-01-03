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
package com.dbxtune.test;

//import org.postgresql.PGConnection;
//import org.postgresql.PGProperty;
//import org.postgresql.replication.PGReplicationStream;

public class PgReplicationStreamTest
{
	private String _repSlotName;

	private String _dbmsHostname;
	private int    _dbmsPort;
	private String _dbmsDbname;
	private String _dbmsUsername;
	private String _dbmsPassword;

	public PgReplicationStreamTest()
	{
	}

	private void init()
	{
		
	}
	
	private void start()
	{
	}

	private void process()
	{
// NOTE: The below needs Postgres JDBC Driver in the CLASSPATH, so for simplicity, this is now comment
//       If we want to test it again, include PG JDBC Driver in "Build Path"
//		try
//		{
//			String repSlotName = "demo_logical_slot";
//			String url = "jdbc:postgresql://pg-3a-cos9:5432/gorans";
//
//			Properties props = new Properties();
//			props.setProperty("user"    , "postgres");
//			props.setProperty("password", "pg-3a-cos9__postgres__CYtXQHYi8dzHjVCtgNIqikGr");
//			
//			
//			PGProperty.ASSUME_MIN_SERVER_VERSION.set(props, "9.4");
//			PGProperty.REPLICATION              .set(props, "database");
//			PGProperty.PREFER_QUERY_MODE        .set(props, "simple");
//
//			Connection con = DriverManager.getConnection(url, props);
//			PGConnection replConnection = con.unwrap(PGConnection.class);
//
//			replConnection.getReplicationAPI()
//				.createReplicationSlot()
//				.logical()
//				.withSlotName(repSlotName)
//				.withOutputPlugin("test_decoding")
//				.make();
//
//			PGReplicationStream stream = replConnection.getReplicationAPI()
//				.replicationStream()
//				.logical()
//				.withSlotName(repSlotName)
//				.withSlotOption("include-xids", false)
//				.withSlotOption("skip-empty-xacts", true)
//				.withStatusInterval(20, TimeUnit.SECONDS)
//				.start();
//
//			while (true) 
//			{
//				//non blocking receive message
//				ByteBuffer msg = stream.readPending();
//
//				if (msg == null) 
//				{
//					TimeUnit.MILLISECONDS.sleep(10L);
//					continue;
//				}
//
//				int offset = msg.arrayOffset();
//				byte[] source = msg.array();
//				int length = source.length - offset;
//				System.out.println(new String(source, offset, length));
//
//				//feedback
//				stream.setAppliedLSN(stream.getLastReceiveLSN());
//				stream.setFlushedLSN(stream.getLastReceiveLSN());
//			}
//		}
//		catch (Exception ex)
//		{
//			ex.printStackTrace();
//		}
	}
	
	public static void main(String[] args)
	{
		
		PgReplicationStreamTest pgReplStream = new PgReplicationStreamTest();

		pgReplStream.init();
		pgReplStream.start();
		pgReplStream.process();
		
		
	}
}
