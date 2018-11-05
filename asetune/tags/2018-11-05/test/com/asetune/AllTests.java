package com.asetune;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({
	com.asetune.cm.AllTests.class,
	com.asetune.graph.AllTests.class,
	com.asetune.utils.AllTests.class,
})
public class AllTests
{

}
