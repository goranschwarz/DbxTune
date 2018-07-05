In this directory you can create User Defined Alarms!

A User Defined "interrogator" is where you can:
 - check (absolute/difference/rate) data for a specific Counter Collector (which internally is called a CM)
 - generate various alarms, based on your own logic
 
Since it's "your own logic" I needed som programing lunguage or scripting languare to support "extensive user defined logic"
I simply choosed Java as that language (for best inegration with DbxTune, since it's written in java)
Althow, you do not need to compile your classes into object classes/files and put them in a JAR file (or in the classpath)
Your User Defined Code (written in Java) is simply "compiled on-the-fly", this to emulate a "script" language.

In DbxTune you can create or edit the source code for User Defined Alarms.
There you also can choose from various Source Code Templates.

Some extra information about this directory structure:
- If you want to create User Defined Alarms: The best way to put them is in the "alarms" directory
- If your code has various Exceptions: A good place to store them is the "exceptions" directory

Good luck and you have any feedback, please send them to: goran_schwarz@hotmail.com
/Goran



Extra info:::
If you want *one* Java files for an interrogator that is common for *all* CM's.
Then you can create a source file called 'GenericFallbackAlarmInterrogator.java'
In that file you will then have to check for what CM Name the call is made for, etc...
NOTE: This class is not supported when creating/editing Alarm Interrogators from the GUI...

Below is an exampele of the "generic" interrogator.
#### BEGIN-Example #######################################################################
package asetune;

import com.asetune.alarm.IUserDefinedAlarmInterrogator;
import com.asetune.cm.CountersModel;
import com.asetune.cm.CounterSample;

public class GenericFallbackAlarmInterrogator
implements IUserDefinedAlarmInterrogator
{
	public void interrogateCounterData(CountersModel cm)
	{
		System.out.println(">>>> ----------------------------------------------");
		System.out.println("     GenericFallbackAlarmInterrogator: interrogateCounterData(cm) was called.");
		System.out.println("     GenericFallbackAlarmInterrogator: cmName = '" + cm.getName() + "'. absRowCount = " + cm.getAbsRowCount() + " --->> cm="+cm);
		System.out.println("<<<< ----------------------------------------------");
		
		// Do some logic for: CmOpenDatabases
		if ("CmOpenDatabases".equals(cm.getName()))
		{
			// Code goes here
		}

		// Do some logic for: CmEngines
		if ("CmEngines".equals(cm.getName()))
		{
			// Code goes here
		}
	}
}
#### END-Example #########################################################################
