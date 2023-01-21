/*******************************************************************************
 * Copyright (C) 2010-2020 Goran Schwarz
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
package com.asetune.alarm.writers;

import java.io.StringWriter;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.app.event.EventCartridge;
import org.apache.velocity.app.event.InvalidReferenceEventHandler;
import org.apache.velocity.context.Context;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.util.introspection.Info;
import org.fife.ui.autocomplete.CompletionProvider;
import org.fife.ui.autocomplete.DefaultCompletionProvider;

import com.asetune.CounterController;
import com.asetune.Version;
import com.asetune.alarm.AlarmHandler;
import com.asetune.alarm.events.AlarmEvent;
import com.asetune.alarm.events.AlarmEvent.ServiceState;
import com.asetune.alarm.events.AlarmEvent.Severity;
import com.asetune.alarm.events.AlarmEventDummy;
import com.asetune.ui.autocomplete.completions.ShorthandCompletionX;
import com.asetune.utils.StringUtil;

public class WriterUtils
{
	private static Logger _logger = Logger.getLogger(WriterUtils.class);


//	/**
//	 * Take a list of AlarmEvent and fill in the template values...
//	 * 
//	 * @param writerName                  writer who calls this method
//	 * @param activeAlarmList             List of "active" alarms
//	 * @param template                    the Velocity template
//	 * @param doTrim                      remove whitespaces newlines etc from start and end
//	 * @param trMap                       A Map of Strings that needs transalations: "&" -> "&amp";
//	 * @param dbxCentralUrl               Where can the DbxCentral be found;
//	 * @return                            The resolved template
//	 * @throws ParseErrorException        If we had parser exceptions
//	 * @throws MethodInvocationException  If any exceptions where thrown when calling a method on a methodName
//	 * @throws ResourceNotFoundException  Resource not found...
//	 */
//	public static String createMessageFromTemplate(String writerName, List<AlarmEvent> activeAlarmList, String template, boolean doTrim, Map<String, String> trMap, String dbxCentralUrl)
//	throws ParseErrorException, MethodInvocationException, ResourceNotFoundException
//	{
//		return createMessageFromTemplate(writerName, null, activeAlarmList, template, doTrim, trMap, dbxCentralUrl);
//	}

	/**
	 * Take the AlarmEvent and fill in the template values...
	 * 
	 * @param action                      RAISE or RE-RAISE or CANCEL
	 * @param alarmEvent                  The AlarmEvent object
	 * @param template                    the Velocity template
	 * @param doTrim                      remove whitespaces newlines etc from start and end
	 * @param trMap                       A Map of Strings that needs transalations: "&" -> "&amp";
	 * @param dbxCentralUrl               Where can the DbxCentral be found;
	 * @return                            The resolved template
	 * @throws ParseErrorException        If we had parser exceptions
	 * @throws MethodInvocationException  If any exceptions where thrown when calling a method on a methodName
	 * @throws ResourceNotFoundException  Resource not found...
	 */
	public static String createMessageFromTemplate(String action, AlarmEvent alarmEvent, String template, boolean doTrim, Map<String, String> trMap, String dbxCentralUrl)
	throws ParseErrorException, MethodInvocationException, ResourceNotFoundException
	{
		List<AlarmEvent> activeAlarmList = null;
		if (AlarmHandler.hasInstance())
			activeAlarmList = AlarmHandler.getInstance().getAlarmList();

		return createMessageFromTemplate(action, alarmEvent, activeAlarmList, template, doTrim, trMap, dbxCentralUrl);
	}

	/**
	 * Take the AlarmEvent and fill in the template values...
	 * 
	 * @param action                      RAISE or RE-RAISE or CANCEL  (or the writer who calls this method) 
	 * @param alarmEvent                  The AlarmEvent object
	 * @param activeAlarmList             List of "active" alarms
	 * @param template                    the Velocity template
	 * @param doTrim                      remove whitespaces newlines etc from start and end
	 * @param trMap                       A Map of Strings that needs transalations: "&" -> "&amp";
	 * @param dbxCentralUrl               Where can the DbxCentral be found;
	 * 
	 * @return                            The resolved template
	 * @throws ParseErrorException        If we had parser exceptions
	 * @throws MethodInvocationException  If any exceptions where thrown when calling a method on a methodName
	 * @throws ResourceNotFoundException  Resource not found...
	 */
	public static String createMessageFromTemplate(String action, AlarmEvent alarmEvent, List<AlarmEvent> activeAlarmList, String template, boolean doTrim, Map<String, String> trMap, String dbxCentralUrl)
	throws ParseErrorException, MethodInvocationException, ResourceNotFoundException
	{
		Properties config = new Properties();
//		config.setProperty("eventhandler.invalidreference.exception", "true");

		VelocityEngine engine = new VelocityEngine(config);
		engine.init();

		// Set the LOG LEVEL to FATAL for the Velocity PARSER... This so that the Parser exceptions will not be written to the Errolog
		// NOTE: This is specific to Log4J
		Logger logLevel = LogManager.getLogger("org.apache.velocity.parser");
		if (logLevel != null)
			logLevel.setLevel(Level.FATAL);


		boolean debug = false;
		if (debug || _logger.isDebugEnabled())
		{
			Enumeration<?> loggers = LogManager.getCurrentLoggers();
			while(loggers.hasMoreElements()) 
			{
				Logger logger = (Logger) loggers.nextElement();
				if (logger.getName().startsWith("org.apache.velocity"))
				{
					_logger.debug("Setting Velocity logger '"+logger.getName()+"' to DEBUG.");
					logger.setLevel(Level.DEBUG);
				}
			}
		}

		boolean isHtmlTemplate = StringUtils.startsWithIgnoreCase(template, "<html>");
		
		
		VelocityContext context = new VelocityContext();

		// Add access to: com.asetune.utils.StringUtil
		context.put("StringUtil", StringUtil.class);
		
		// Add access to: com.asetune.Version
		context.put("Version", Version.class);
		
		// Add access to: java.lang.System
		context.put("System", System.class);
		
		// Add access to: org.apache.commons.lang3.StringUtils
		context.put("StringUtils", StringUtils.class);
		

		// Add TYPE to context
		context.put("type", action);

		// Dbx Central URL
		context.put("dbxCentralUrl", dbxCentralUrl);

		// serverDisplayName
		String serverDisplayName = null;
		if (CounterController.hasInstance())
			serverDisplayName = CounterController.getInstance().getServerDisplayName();
		if (StringUtil.isNullOrBlank(serverDisplayName) && alarmEvent != null)
			serverDisplayName = alarmEvent.getServiceName();
		context.put("serverDisplayName"        , serverDisplayName);
//FIXME; change the template to be ${serverDisplayName}


//		// ADD information from DbxCentral SERVER_LIST file (if we got any)
//		String serverName = alarmEvent.getServiceName();
//		try
//		{
//			Map<String, DbxCentralServerDescription> dbxCentralSrvMap = DbxCentralServerDescription.getFromFile();
//			DbxCentralServerDescription entry = dbxCentralSrvMap.get(serverName);
//			if (entry != null)
//			{
//				context.put("dbxCentralServerNameDescription", entry.getDescription());
//			}
//			else
//			{
//				_logger.info("No DbxCentral 'SERVER_LIST' was found for serverName '" + serverName + "' in file '" + DbxCentralServerDescription.getDefaultFile() + "'. I wont be able to add template tags for 'SERVER_LIST' for serverName='" + serverName + "'.");
//			}
//		}
//		catch(IOException ex)
//		{
//			_logger.info("Problems reading DBX Central 'SERVER_LIST' file '" + DbxCentralServerDescription.getDefaultFile() + "'. I wont be able to add template tags for 'SERVER_LIST' for serverName='" + serverName + "'.");
//		}
		
		
		if (alarmEvent != null)
		{
			// put (basic) AlarmEvent fields
			context.put("alarmClass"                 , StringUtil.toStr( alarmEvent.getAlarmClass()                 ,trMap ));
			context.put("serviceType"                , StringUtil.toStr( alarmEvent.getServiceType()                ,trMap ));
			context.put("serviceName"                , StringUtil.toStr( alarmEvent.getServiceName()                ,trMap ));
			context.put("serviceInfo"                , StringUtil.toStr( alarmEvent.getServiceInfo()                ,trMap ));
			context.put("extraInfo"                  , StringUtil.toStr( alarmEvent.getExtraInfo()                  ,trMap ));
			context.put("category"                   , StringUtil.toStr( alarmEvent.getCategory()                   ,trMap ));
			context.put("severity"                   , StringUtil.toStr( alarmEvent.getSeverity()                   ,trMap ));
			context.put("state"                      , StringUtil.toStr( alarmEvent.getState()                      ,trMap ));
			context.put("data"                       , StringUtil.toStr( alarmEvent.getData()                       ,trMap ));
			context.put("description"                , StringUtil.toStr( alarmEvent.getDescription()                ,trMap ));

			// And some extra/extended AlarmEvent fields
			context.put("duration"                   , StringUtil.toStr( alarmEvent.getFullDuration(true)           ,trMap ));
			context.put("alarmDuration"              , StringUtil.toStr( alarmEvent.getAlarmDuration()              ,trMap ));
			context.put("fullDuration"               , StringUtil.toStr( alarmEvent.getFullDuration()               ,trMap ));
			context.put("fullDurationAdjustmentInSec", StringUtil.toStr( alarmEvent.getFullDurationAdjustmentInSec(),trMap ));
			context.put("reRaiseCount"               , StringUtil.toStr( alarmEvent.getReRaiseCount()               ,trMap ));
			context.put("crTimeStr"                  , StringUtil.toStr( alarmEvent.getCrTimeStr()                  ,trMap ));
			context.put("reRaiseTimeStr"             , StringUtil.toStr( alarmEvent.getReRaiseTimeStr()             ,trMap ));
			context.put("timeToLive"                 , StringUtil.toStr( alarmEvent.getTimeToLive()                 ,trMap ));
			context.put("alarmClassAbriviated"       , StringUtil.toStr( alarmEvent.getAlarmClassAbriviated()       ,trMap ));
			context.put("extendedDescription"        , StringUtil.toStr( isHtmlTemplate ? alarmEvent.getExtendedDescriptionHtml()        : alarmEvent.getExtendedDescription() ,trMap ));
			context.put("reRaiseDescription"         , StringUtil.toStr( alarmEvent.getReRaiseDescription()         ,trMap ));
			context.put("reRaiseExtendedDescription" , StringUtil.toStr( isHtmlTemplate ? alarmEvent.getReRaiseExtendedDescriptionHtml() : alarmEvent.getReRaiseExtendedDescription() ,trMap ));
			context.put("reRaiseData"                , StringUtil.toStr( alarmEvent.getReRaiseData()                ,trMap ));
			context.put("cancelTimeStr"              , StringUtil.toStr( alarmEvent.getCancelTimeStr()              ,trMap ));
			context.put("crAgeInMs"                  , StringUtil.toStr( alarmEvent.getCrAgeInMs()                  ,trMap ));
			context.put("isActive"                   , StringUtil.toStr( alarmEvent.isActive()                      ,trMap ));
			context.put("activeAlarmCount"           , StringUtil.toStr( alarmEvent.getActiveAlarmCount()           ,trMap ));
		}
		
		if (activeAlarmList != null)
		{
			context.put("activeAlarmList" , activeAlarmList);
		}


		InvalidReferenceEventHandler invalidReferenceEventHandler = new InvalidReferenceEventHandler()
		{
			@Override
			public Object invalidGetMethod(Context context, String reference, Object object, String property, Info info)
			{
				_logger.debug("invalid-Get-Method(context, reference='"+reference+"', object='"+object+"', info='"+info+"')");
				reportInvalidReference(reference, null, info);
				return null;
			}

			@Override
			public boolean invalidSetMethod(Context context, String leftreference, String rightreference, Info info)
			{
				_logger.debug("invalid-Set-Method(context, leftreference='"+leftreference+"', rightreference='"+rightreference+"', info='"+info+"')");
				reportInvalidReference(leftreference, null, info);
				return false;
			}

			@Override
			public Object invalidMethod(Context context, String reference, Object object, String method, Info info)
			{
				_logger.debug("invalid-Method(context, reference='"+reference+"', object='"+object+"', method='"+method+"', info='"+info+"')");
				if (reference == null)
					reportInvalidReference(object.getClass().getName() + "." + method, method, info);
				else
					reportInvalidReference(reference, method, info);
				return null;
			}

			private void reportInvalidReference(String reference, String method, Info info)
			{
				String lineStr   = "[line "+info.getLine()+", column "+info.getColumn()+"]";
				String methodStr = StringUtil.isNullOrBlank(method) ? "" : ", method='"+method+"'";
				throw new ParseErrorException("Reference '"+reference+"'"+methodStr+" do not exists. at "+lineStr, info);
			}
		};
		
		EventCartridge ec = new EventCartridge();
		ec.addEventHandler(invalidReferenceEventHandler);
		ec.attachToContext(context);

		// Here is where the substitution happens
		StringWriter writer = new StringWriter();
		
		Velocity.evaluate(context, writer, "AlarmTemplateWriter", template);

		String output = writer.toString();
		if (doTrim)
			return output.trim();
		else
			return output;
	}

	public static CompletionProvider createCompletionProvider()
	{
		// A DefaultCompletionProvider is the simplest concrete implementation
		// of CompletionProvider. This provider has no understanding of
		// language semantics. It simply checks the text entered up to the
		// caret position for a match against known completions. This is all
		// that is needed in the majority of cases.
		DefaultCompletionProvider provider = new DefaultCompletionProvider();

		Map<String, String> desc = new HashMap<>();
		desc.put("generalDescription"         , "<html> <h2>Just overview information... no variable</h2>"
		                                            + "The text in this editor is a <b>template</b>, and all ${someValue} will be changed into real values.<br>"
		                                            + "Watch the top text panel for how you template will be resolved.<br>"
		                                            + "<br>"
		                                            + "<br>"
		                                            + "You can also use <br>"
		                                            + "More information about how the Velocity Template Engine works, you can find here <a href='http://velocity.apache.org/engine/2.0/user-guide.html'>http://velocity.apache.org/engine/2.0/user-guide.html</a><br>"
		                                            + "<br>"
		                                            + "There is a couple of things that might not be described in the best way, so below are som hints...<br>"
		                                            + "<ul>"
		                                            + "   <li>You can use Java String operations on the template variables<br>"
		                                            + "       Example 1: <code>${type.toLowerCase()}</code> results in <code>raise</code> instead of RAISE<br>"
		                                            + "       Example 2: <code>${type.substring(1,4)}</code> results in <code>AIS</code> instead of RAISE</li>"
		                                            + "       Example 3: <code>${alarmClass.replace('Dummy', 'Awsome')}</code> results in <code>AlarmEventAwsome</code> instead of <code>AlarmEventDummy</code>"
		                                            + "   </li>"
		                                            + "   <li>You can use special variable <code>StringUtils<code/> for extended String functionality<br>"
		                                            + "       see: <a href='https://commons.apache.org/proper/commons-lang/apidocs/org/apache/commons/lang3/StringUtils.html'>https://commons.apache.org/proper/commons-lang/apidocs/org/apache/commons/lang3/StringUtils.html</a><br>"
		                                            + "       Example 1: <code>${StringUtils.isNumeric(${data})}</code> which return true or false so you can use it in a if statement.<br>"
		                                            + "   </li>"
		                                            + "   <li>You can use special variable <code>StringUtil<code/> for some other extended String functionality<br>"
		                                            + "       one of those will be to use the java <code>format(...)</code> to format data.<br>"
		                                            + "       Example 1: <code>${StringUtil.format('%-30s %-10s', ${alarmClass}, ${type})}</code> which will result in left justified strings with a lenght of 30 and 10.<br>"
		                                            + "   </li>"
		                                            + "</ul>"
		                                            + "The template engine also allows you to do conditional logic like <code>#if (${data} == -1) XXXX #else YYYY #end</code><br>"
		                                            + "For more info see: <a href='http://velocity.apache.org/engine/2.0/user-guide.html#conditionals'>http://velocity.apache.org/engine/2.0/user-guide.html#conditionals</a><br>"
		                                            + "<br>"
		                                            + "Below is a small example of that; it will result in different output depending content of the ${type} variable<br>"
		                                            + "<pre>"
		                                            + "#set( $myType = ${type} )\n"
		                                            + "#set( $myType = 'RE-RAISE' ) ## uncomment this line (two # at the start of the line) to test template with 'RE-RAISE'\n"
		                                            + "#if     ( ${myType}=='RAISE' )\n"
		                                            + "    ${type} - ${alarmClass} - ${serviceName} - ${description}\n"
		                                            + "#elseif ( ${myType}=='RE-RAISE' )\n"
		                                            + "    ${type} - (${duration}) - ${reRaiseCount}:${reRaiseDescription} - ${alarmClass} - ${serviceName} - ${description}\n"
		                                            + "#elseif ( ${myType}=='CANCEL' )\n"
		                                            + "    ${type} (${duration}) - ${alarmClass} - ${serviceName} - ${description}\n"
		                                            + "#else\n"
		                                            + "    UNKNWON Action type...\n"
		                                            + "#end\n"
		                                            + "</pre>"
		                                            + "</html>");
		desc.put("type"                       , "<html> <h2>type</h2>"
		                                            + "This is the Alarm Type, typically <b>RAISE</b>, <b>RERAISE</b> or <b>CANCEL</b><br>"
		                                            + "<br>"
		                                            + "If you want to generate different text depending on the <i>type</i> then you can use<br>"
		                                            + "Conditionals supported by the Velocity Template Engine.<br>"
		                                            + "Below is an example of that:"
		                                            + "<pre>"
		                                            + "#set( $myType = ${type} )\n"
		                                            + "#set( $myType = 'RE-RAISE' ) ## uncomment this line (two # at the start of the line) to test template with 'RE-RAISE'\n"
		                                            + "#if     ( ${myType}=='RAISE' )\n"
		                                            + "    ${type} - ${alarmClass} - ${serviceName} - ${description}\n"
		                                            + "#elseif ( ${myType}=='RE-RAISE' )\n"
		                                            + "    ${type} - (${duration}) - ${reRaiseCount}:${reRaiseDescription} - ${alarmClass} - ${serviceName} - ${description}\n"
		                                            + "#elseif ( ${myType}=='CANCEL' )\n"
		                                            + "    ${type} (${duration}) - ${alarmClass} - ${serviceName} - ${description}\n"
		                                            + "#else\n"
		                                            + "	UNKNWON Action type...\n"
		                                            + "#end\n"
		                                            + "</pre>"
		                                            + "</html>");
		desc.put("alarmClass"                 , "<html> <h2>alarmClass                 </h2> Class name of the alarm.                                                                                                                             <br><br>Example: <code>AlarmEventHighCpuUtilazation</code>      </html>");
		desc.put("serviceType"                , "<html> <h2>serviceType                </h2> Type of Service, this would typically be <code>"+Version.getAppName()+"<code> </html>");
		desc.put("serviceName"                , "<html> <h2>serviceName                </h2> Name of the service, this would be the DBMS Server Name, or possibly the hostname of the server we are monitoring.                                   <br><br>Example: <code>GORAN_1_DS</code>     </html>");
		desc.put("serviceInfo"                , "<html> <h2>serviceInfo                </h2> Name of the Counter Model that detected the problem.                                                                                                 <br><br>Example: <code>CmSummary</code>      </html>");
		desc.put("extraInfo"                  , "<html> <h2>extraInfo                  </h2> In some cases a Alarm attches extra parameters/information. For instance CmOpenDatabases puts the database name in here.                             <br><br>Example: <code>PML</code>            </html>");
		desc.put("category"                   , "<html> <h2>category                   </h2> What <b>Category</b> this alarm has. Known severities are 'OTHER', 'CPU', 'SPACE', 'SRV_CONFIG', 'LOCK' and 'DOWN'.                                  <br><br>Example: <code>CPU</code>            </html>");
		desc.put("severity"                   , "<html> <h2>severity                   </h2> What <b>Severity</b> this alarm has. Known severities are 'INFO', 'WARNING' and 'ERROR'.                                                             <br><br>Example: <code>WARNING</code>        </html>");
		desc.put("state"                      , "<html> <h2>state                      </h2> What Service <b>State</b> is attached to the alarm. Known states are 'UP', 'AFFECTED' and 'DOWN'.                                                    <br><br>Example: <code>DOWN</code>           </html>");
		desc.put("data"                       , "<html> <h2>data                       </h2> Raw datapoint the alarm was based on. For Example in a AlarmEventHighCpuUtilazation, it will be the CPU Usage in percent   </html>");
		desc.put("description"                , "<html> <h2>description                </h2> A Short text description trying to describe the alam, hopefully with some data points in there as well  </html>");
		desc.put("duration"                   , "<html> <h2>duration                   </h2> In a CANCEL action, this will be for how long the Alarm was active for(including adjustment for setFullDurationAdjustmentInSec()), in a RE-RAISE it will be the time since the Alarm was originally raised.     <br><br>Example: <code>09:27</code> for 10 minutes and 27 minutes   </html>");
		desc.put("alarmDuration"              , "<html> <h2>alarmDuration              </h2> In a CANCEL action, this will be for how long the Alarm was active for, in a RE-RAISE it will be the time since the Alarm was originally raised.     <br><br>Example: <code>09:27</code> for 10 minutes and 27 minutes   </html>");
		desc.put("fullDuration"               , "<html> <h2>fullDuration               </h2> In a CANCEL action, this will be for how long the Alarm was active for (including adjustment for setFullDurationAdjustmentInSec()), in a RE-RAISE it will be the time since the Alarm was originally raised.     <br><br>Example: <code>09:27</code> for 10 minutes and 27 minutes   </html>");
		desc.put("fullDurationAdjustmentInSec", "<html> <h2>fullDurationAdjustmentInSec</h2> value of alarmEvent.getFullDurationAdjustmentInSec()</html>");
		desc.put("reRaiseCount"               , "<html> <h2>reRaiseCount               </h2> How many times has this Alarm been re-raised. (a re-raise is sent every time the alars is still <b>above</b> the threshold, you have specified.  </html>");
		desc.put("crTimeStr"                  , "<html> <h2>crTimeStr                  </h2> When the Alarm was originally Created.                                                                                                               <br><br>Example: <code>2017-09-30 00:34:45.123</code>      </html>");
		desc.put("reRaiseTimeStr"             , "<html> <h2>reRaiseTimeStr             </h2> When the Alarm was re/raised.                                                                                                                        <br><br>Example: <code>2017-09-30 00:34:45.123</code>      </html>");
		desc.put("timeToLive"                 , "<html> <h2>timeToLive                 </h2> Number of milliseconds an alarm is expected to live. This is used if a Counter Collector/Model has the <i>postpone</i> field set. Then that CounterModel will not be sending an new Alarm for next couple of seconds.  </html>");
		desc.put("alarmClassAbriviated"       , "<html> <h2>alarmClassAbriviated       </h2> Same as the <code>alarmClass</code> field, but it's abriviated. (removing <cade>AlarmEvent</code>)                                                   <br><br>Example: <code>HighCpuUtilazation</code> instaed of <code>AlarmEventHighCpuUtilazation</code>     </html>");
		desc.put("extendedDescription"        , "<html> <h2>extendedDescription        </h2> In some cases the Alarm might choose to fill in some <b>extra</b> information, which might be usable for example in the <code>AlarmWriterToMail</code> or similar writers. </html>");
		desc.put("reRaiseDescription"         , "<html> <h2>reRaiseDescription         </h2> Same as the <code>description</code>, but this would be the latest description when the Alarm was re-raised.   </html>");
		desc.put("reRaiseExtendedDescription" , "<html> <h2>reRaiseExtendedDescription </h2> Same as the <code>extendedDescription</code>, but this would be the latest extendedDescription when the Alarm was re-raised.   </html>");
		desc.put("reRaiseData"                , "<html> <h2>reRaiseData                </h2> Same as the <code>data</code>, but this would be the latest extendedDescription when the Alarm was re-raised.   </html>");
		desc.put("cancelTimeStr"              , "<html> <h2>cancelTimeStr              </h2> What time the Alarm was cancelled, this would only be availabe when <code>type</code> is CANCEL.                                                     <br><br>Example: <code>2017-09-30 00:55:12.345</code>      </html>");
		desc.put("crAgeInMs"                  , "<html> <h2>crAgeInMs                  </h2> How many milleseconds has pased since the Alarm was Created.  </html>");
		desc.put("isActive"                   , "<html> <h2>isActive                   </h2> Boolean status flag if the Alarm is still Active, which would be true when <code>type</code> is RAISE and RE-RAISE.                                  <br><br>Example: <code>true</code> or <code>false</code>      </html>");
		desc.put("activeAlarmCount"           , "<html> <h2>activeAlarmCount           </h2> Get number of <b>active</b> alarms in the AlarmHandler. This can be used to simply print out how many active alarms we have for the moment. </html>");
		

		provider.addCompletion(new ShorthandCompletionX(provider, "_[generalDescription]"      , ""                              ,  null, desc.get("generalDescription"        )));
		provider.addCompletion(new ShorthandCompletionX(provider, "type"                       , "${type}"                       ,  null, desc.get("type"                      )));
		provider.addCompletion(new ShorthandCompletionX(provider, "alarmClass"                 , "${alarmClass}"                 ,  null, desc.get("alarmClass"                )));
		provider.addCompletion(new ShorthandCompletionX(provider, "serviceType"                , "${serviceType}"                ,  null, desc.get("serviceType"               )));
		provider.addCompletion(new ShorthandCompletionX(provider, "serviceName"                , "${serviceName}"                ,  null, desc.get("serviceName"               )));
		provider.addCompletion(new ShorthandCompletionX(provider, "serviceInfo"                , "${serviceInfo}"                ,  null, desc.get("serviceInfo"               )));
		provider.addCompletion(new ShorthandCompletionX(provider, "extraInfo"                  , "${extraInfo}"                  ,  null, desc.get("extraInfo"                 )));
		provider.addCompletion(new ShorthandCompletionX(provider, "category"                   , "${category}"                   ,  null, desc.get("category"                  )));
		provider.addCompletion(new ShorthandCompletionX(provider, "severity"                   , "${severity}"                   ,  null, desc.get("severity"                  )));
		provider.addCompletion(new ShorthandCompletionX(provider, "state"                      , "${state}"                      ,  null, desc.get("state"                     )));
		provider.addCompletion(new ShorthandCompletionX(provider, "data"                       , "${data}"                       ,  null, desc.get("data"                      )));
		provider.addCompletion(new ShorthandCompletionX(provider, "description"                , "${description}"                ,  null, desc.get("description"               )));
		provider.addCompletion(new ShorthandCompletionX(provider, "duration"                   , "${duration}"                   ,  null, desc.get("duration"                  )));
		provider.addCompletion(new ShorthandCompletionX(provider, "alarmDuration"              , "${alarmDuration}"              ,  null, desc.get("alarmDuration"             )));
		provider.addCompletion(new ShorthandCompletionX(provider, "fullDuration"               , "${fullDuration}"               ,  null, desc.get("fullDuration"              )));
		provider.addCompletion(new ShorthandCompletionX(provider, "fullDurationAdjustmentInSec", "${fullDurationAdjustmentInSec}",  null, desc.get("fullDurationAdjustmentInSec")));
		provider.addCompletion(new ShorthandCompletionX(provider, "reRaiseCount"               , "${reRaiseCount}"               ,  null, desc.get("reRaiseCount"              )));
		provider.addCompletion(new ShorthandCompletionX(provider, "crTimeStr"                  , "${crTimeStr}"                  ,  null, desc.get("crTimeStr"                 )));
		provider.addCompletion(new ShorthandCompletionX(provider, "reRaiseTimeStr"             , "${reRaiseTimeStr}"             ,  null, desc.get("reRaiseTimeStr"            )));
		provider.addCompletion(new ShorthandCompletionX(provider, "timeToLive"                 , "${timeToLive}"                 ,  null, desc.get("timeToLive"                )));
		provider.addCompletion(new ShorthandCompletionX(provider, "alarmClassAbriviated"       , "${alarmClassAbriviated}"       ,  null, desc.get("alarmClassAbriviated"      )));
		provider.addCompletion(new ShorthandCompletionX(provider, "extendedDescription"        , "${extendedDescription}"        ,  null, desc.get("extendedDescription"       )));
		provider.addCompletion(new ShorthandCompletionX(provider, "reRaiseDescription"         , "${reRaiseDescription}"         ,  null, desc.get("reRaiseDescription"        )));
		provider.addCompletion(new ShorthandCompletionX(provider, "reRaiseExtendedDescription" , "${reRaiseExtendedDescription}" ,  null, desc.get("reRaiseExtendedDescription")));
		provider.addCompletion(new ShorthandCompletionX(provider, "reRaiseData"                , "${reRaiseData}"                ,  null, desc.get("reRaiseData"               )));
		provider.addCompletion(new ShorthandCompletionX(provider, "cancelTimeStr"              , "${cancelTimeStr}"              ,  null, desc.get("cancelTimeStr"             )));
		provider.addCompletion(new ShorthandCompletionX(provider, "crAgeInMs"                  , "${crAgeInMs}"                  ,  null, desc.get("crAgeInMs"                 )));
		provider.addCompletion(new ShorthandCompletionX(provider, "isActive"                   , "${isActive}"                   ,  null, desc.get("isActive"                  )));
		provider.addCompletion(new ShorthandCompletionX(provider, "activeAlarmCount"           , "${activeAlarmCount}"           ,  null, desc.get("activeAlarmCount"           )));

		provider.addCompletion(new ShorthandCompletionX(provider, "serverDisplayName"          , "${serverDisplayName}"          ,  null, "<html>The command line switch <i>--displayName</i> or the ServerName. This can for example be used in the <b>mail subject</b> if the servernames are cryptical.</html>"));
		provider.addCompletion(new ShorthandCompletionX(provider, "dbxCentralUrl"              , "${dbxCentralUrl}"              ,  null, "<html>Some writers want to add a <i>link</i> where the DbxCentral can be located. (easy to click)</html>"));
		provider.addCompletion(new ShorthandCompletionX(provider, "activeAlarmList"            , "#foreach( $alarm in $activeAlarmList )\n${alarm.serviceName} - ${alarm.state} - ${alarm.description}\n#end" ,  null, "<html>Some writers want to have access to the 'activeAlarmList', where you can loop around the active alarms...</html>"));

		provider.addCompletion(new ShorthandCompletionX(provider, "StringUtil"                 , "${StringUtil.format(\"%-20s\", ${type})}" ,  null, "<html>Access DbxTune StringUtil, which for example has format(...) see: <a href='https://docs.oracle.com/javase/7/docs/api/java/util/Formatter.html'>https://docs.oracle.com/javase/7/docs/api/java/util/Formatter.html</a></html>"));
		provider.addCompletion(new ShorthandCompletionX(provider, "Version"                    , "${Version.getAppName()}"                  ,  null, "<html>Access DbxTune Version, which has: getAppName(), getBuildStr() </html>"));
		provider.addCompletion(new ShorthandCompletionX(provider, "StringUtils"                , "${StringUtils.xxx(${type})}"              ,  null, "<html>Access Apache Commons StringUtils, which has some methods, see: <a href='https://commons.apache.org/proper/commons-lang/apidocs/org/apache/commons/lang3/StringUtils.html'>https://commons.apache.org/proper/commons-lang/apidocs/org/apache/commons/lang3/StringUtils.html</a></html>"));
		
		return provider;
	}

	public static void main(String[] args)
	{
		Properties log4jProps = new Properties();
//		log4jProps.setProperty("log4j.rootLogger", "INFO, A1");
		log4jProps.setProperty("log4j.rootLogger", "TRACE, A1");
		log4jProps.setProperty("log4j.appender.A1", "org.apache.log4j.ConsoleAppender");
		log4jProps.setProperty("log4j.appender.A1.layout", "org.apache.log4j.PatternLayout");
		log4jProps.setProperty("log4j.appender.A1.layout.ConversionPattern", "%d - %-5p - %-30c{1} - %m%n");
		PropertyConfigurator.configure(log4jProps);

		AlarmEvent ae = new AlarmEventDummy("GORAN_1_DS", "SomeCmName", "SomeExtraInfo", AlarmEvent.Category.OTHER, Severity.WARNING, ServiceState.AFFECTED, -1, 999, "This is an Alarm Example with the data value of '999'", "Extended Description goes here", 0);

		String str = createMessageFromTemplate(AlarmWriterAbstract.ACTION_RAISE, ae, "TEST: ${type} - ${alarmClass} --- $display.truncate(\"This is a long string.\", 10)", true, null, "http://DUMMY-dbxcentral:8080");
		System.out.println("OUT: "+str);
	}
}


/*
#set( $myType = ${type} )
#set( $myType = 'RE-RAISE' ) ## uncomment this line (two # at the start of the line) to test template with 'RE-RAISE'
#if     ( ${myType}=='RAISE' )
    ${type} - ${alarmClass} - ${serviceName} - ${description}
#elseif ( ${myType}=='RE-RAISE' )
    ${type} - (${duration}) - ${reRaiseCount}:${reRaiseDescription} - ${alarmClass} - ${serviceName} - ${description}
#elseif ( ${myType}=='CANCEL' )
    ${type} (${duration}) - ${alarmClass} - ${serviceName} - ${description}
#else
	UNKNWON Action type...
#end
#set ($phone = 123456789)
$display.printf("%s %s %s %s", $phoneString.substring(0,2), $phoneString.substring(2,4), $phoneString.substring(4,6), $phoneString.substring(6,8))
$number.format('00 00 00 00',${phone})



$StringUtil.format("%-10s %-30s %-10s %-10s %-30s %-11s %-30s", "EventType", "AlarmClass", "State", "Severity", "ServiceName", "ServiceType", "Description")
$StringUtil.format("%s %s %s %s %s %s %s", ${StringUtils.repeat("-", 10)}, ${StringUtils.repeat("-", 30)}, ${StringUtils.repeat("-", 10)}, ${StringUtils.repeat("-", 10)}, ${StringUtils.repeat("-", 30)}, ${StringUtils.repeat("-", 11)}, ${StringUtils.repeat("-", 30)})
$StringUtil.format("%-10s %-30s %-10s %-10s %-30s %-11s %-30s", ${type}, ${alarmClass}, ${state}, ${severity}, ${serviceName}, ${serviceType}, ${description})

 */
