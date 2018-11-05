package com.asetune.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class JsonUtilsTest
{
	@Test
	public void simpleTest()
	{
		assertEquals("Blank or null should return false", false, JsonUtils.isPossibleJson(null));
		assertEquals("Blank or null should return false", false, JsonUtils.isPossibleJson(""));

		assertEquals("Not A JSON String",                 false, JsonUtils.isPossibleJson("   not a json"));

		assertEquals("Not A JSON String",            false, JsonUtils.isPossibleJson("{}"));
		assertEquals("Not A JSON String",            false, JsonUtils.isPossibleJson("[]"));
		assertEquals("Not A JSON String",            false, JsonUtils.isPossibleJson("\"key\":\"value\""));
		assertEquals("Not A JSON String",            false, JsonUtils.isPossibleJson("  xx    \"{key\":\"value\"}"));
		assertEquals("Not A JSON String",            false, JsonUtils.isPossibleJson("        \"{key\"=\"value\"}"));

		assertEquals("Not A JSON String, but close", false, JsonUtils.isPossibleJson("\"{key\":\"value\"}"));
		assertEquals("Not A JSON String, but close", false, JsonUtils.isPossibleJson("      \"{key\":\"value\"}"));

		assertEquals("A JSON String",                true, JsonUtils.isPossibleJson("{\"key\":\"value\"}"));
		assertEquals("A JSON String",                true, JsonUtils.isPossibleJson("      {\"key\":\"value\"}"));
		assertEquals("A JSON String",                true, JsonUtils.isPossibleJson("  [    {\"key\":\"value\"}    ]"));
	}


}
