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
package com.asetune.test;

import java.io.StringWriter;
import java.util.Iterator;

import com.asetune.central.controllers.Helper;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonDummy
{

	public static void main(String[] args)
	{
		try
		{
			// Create an JSON Object Mapper
			ObjectMapper om = Helper.createObjectMapper();

			
			StringWriter sw = new StringWriter();

			JsonFactory jfactory = new JsonFactory();
			JsonGenerator gen = jfactory.createGenerator(sw);
			gen.setPrettyPrinter(new DefaultPrettyPrinter());

			gen.setCodec(om);
			
			gen.writeStartArray(); // ------------- START ARRAY ------------- 

			String jsonText = "{ #aaa#:#AAA#,\n #bbb#:#BBB#,\n #ccc#:#CCC#,\n #ddd#:#DDD#,\n #subObjext#:{#s11#:#S11#, #s12#:#S12#} }".replace('#', '"');
			JsonNode cmLastSampleJsonTree = om.readTree(jsonText);
//			TreeNode cmLastSampleJsonTree = jfactory.createParser(jsonText).readValueAsTree();

			
			for (Iterator<JsonNode> iterator = cmLastSampleJsonTree.iterator(); iterator.hasNext();)
			{
				JsonNode j = iterator.next();
				System.out.println("   key=????, value=|" + j.asText() + "|... toStr=|" + j + "|, xxxx=|" + j.traverse() + "|");
			}

			
			gen.writeStartObject(); // ------------- START OBJECT -------------

			gen.writeStringField("cmName", "CM-NAME");
//			w.writeStringField("lastSample", jsonText);
			gen.writeFieldName("lastSample");
	
			gen.writeTree(cmLastSampleJsonTree);
//			w.writeString(jsonText);                 // This writes it as a *escaped* string... (so no PURE JSON structure)
//			w.writeRawValue(jsonText);               // This writes "whatever" string we pass in the JSON Stream... (if it's a faulty Syntax... we are out of luck)
//			w.copyCurrentStructure(cmLastSampleJsonTree.traverse()); // this did NOT work... but it should/would be the preferred solution
			gen.writeEndObject(); // ------------- END OBJECT -------------
			
			
			gen.writeEndArray(); // ------------- END ARRAY -------------
			gen.close();
			
			String payload = sw.toString();
			System.out.println("LastSampleForCmController: PAYLOAD=|" + payload + "|.");
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}
}
