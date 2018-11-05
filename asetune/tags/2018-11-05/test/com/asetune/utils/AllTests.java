package com.asetune.utils;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ 
	StringUtilTest.class, 
	JsonUtilsTest.class, 
	VersionShortTest.class, 
	VerTest.class 
})
public class AllTests
{

}
