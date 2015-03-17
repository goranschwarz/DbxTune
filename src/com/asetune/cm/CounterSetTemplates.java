package com.asetune.cm;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.asetune.CounterController;
import com.asetune.CounterControllerNoOp;
import com.asetune.GetCountersNoGui;
import com.asetune.GetCountersNoOp;
import com.asetune.utils.PropPropEntry;


public class CounterSetTemplates
{
	public static enum Type { SMALL, MEDIUM, LARGE, ALL, OFF };
	
	private static LinkedHashMap<String, String>        _nameShortToLongMap = new LinkedHashMap<String, String>();;
	private static LinkedHashMap<String, String>        _nameLongToShortMap = new LinkedHashMap<String, String>();;
	private static LinkedHashMap<String, CountersModel> _registeredCms      = new LinkedHashMap<String, CountersModel>();

	public static PropPropEntry SYSTEM_TEMPLATE_SUMMARY_CM     = null; //new PropPropEntry();

	public static PropPropEntry SYSTEM_TEMPLATE_PCS_ON_SMALL   = null; //new PropPropEntry();
	public static PropPropEntry SYSTEM_TEMPLATE_PCS_ON_MEDIUM  = null; //new PropPropEntry();
	public static PropPropEntry SYSTEM_TEMPLATE_PCS_ON_LARGE   = null; //new PropPropEntry();
	public static PropPropEntry SYSTEM_TEMPLATE_PCS_ON_ALL     = null; //new PropPropEntry();

	public static PropPropEntry SYSTEM_TEMPLATE_PCS_OFF_SMALL  = null; //new PropPropEntry();
	public static PropPropEntry SYSTEM_TEMPLATE_PCS_OFF_MEDIUM = null; //new PropPropEntry();
	public static PropPropEntry SYSTEM_TEMPLATE_PCS_OFF_LARGE  = null; //new PropPropEntry();
	public static PropPropEntry SYSTEM_TEMPLATE_PCS_OFF_ALL    = null; //new PropPropEntry();

	/**
	 * This is typically called when instantiating a CM, then it's included in the template list.
	 * @param cm
	 */
	public static void register(CountersModel cm)
	{
		if (cm == null)
			throw new IllegalArgumentException("The passed CountersModel can't be null.");

		_nameShortToLongMap.put(cm.getName(),        cm.getDisplayName());
		_nameLongToShortMap.put(cm.getDisplayName(), cm.getName());

		_registeredCms.put(cm.getName(), cm);

		
		String systemSummaryCm            = "";

		String systemTemplatePcsOnSmall   = "";
		String systemTemplatePcsOnMedium  = "";
		String systemTemplatePcsOnLarge   = "";
		String systemTemplatePcsOnAll     = "";

		String systemTemplatePcsOffSmall  = "";
		String systemTemplatePcsOffMedium = "";
		String systemTemplatePcsOffLarge  = "";
		String systemTemplatePcsOffAll    = "";

		for (CountersModel cmIter : _registeredCms.values())
		{
			if (cmIter == CounterController.getSummaryCm())
			{
				systemSummaryCm = getTemplateForCm(cmIter, Type.SMALL);
			}
			else
			{
				systemTemplatePcsOnSmall   += getTemplateForCm(cmIter, Type.SMALL);
				systemTemplatePcsOnMedium  += getTemplateForCm(cmIter, Type.MEDIUM);
				systemTemplatePcsOnLarge   += getTemplateForCm(cmIter, Type.LARGE);
				systemTemplatePcsOnAll     += getTemplateForCm(cmIter, Type.ALL);

				systemTemplatePcsOffSmall  += getTemplateForCm(cmIter, Type.OFF);
				systemTemplatePcsOffMedium += getTemplateForCm(cmIter, Type.OFF);
				systemTemplatePcsOffLarge  += getTemplateForCm(cmIter, Type.OFF);
				systemTemplatePcsOffAll    += getTemplateForCm(cmIter, Type.OFF);
			}
		}

		SYSTEM_TEMPLATE_SUMMARY_CM     = new PropPropEntry(systemSummaryCm);

		SYSTEM_TEMPLATE_PCS_ON_SMALL   = new PropPropEntry(systemTemplatePcsOnSmall);
		SYSTEM_TEMPLATE_PCS_ON_MEDIUM  = new PropPropEntry(systemTemplatePcsOnMedium);
		SYSTEM_TEMPLATE_PCS_ON_LARGE   = new PropPropEntry(systemTemplatePcsOnLarge);
		SYSTEM_TEMPLATE_PCS_ON_ALL     = new PropPropEntry(systemTemplatePcsOnAll);

		SYSTEM_TEMPLATE_PCS_OFF_SMALL  = new PropPropEntry(systemTemplatePcsOffSmall);
		SYSTEM_TEMPLATE_PCS_OFF_MEDIUM = new PropPropEntry(systemTemplatePcsOffMedium);
		SYSTEM_TEMPLATE_PCS_OFF_LARGE  = new PropPropEntry(systemTemplatePcsOffLarge);
		SYSTEM_TEMPLATE_PCS_OFF_ALL    = new PropPropEntry(systemTemplatePcsOffAll);
	}

	/**
	 * Get a Template String
	 * <p>
	 * NOTE: this is only called right after initialization when the CM is registered with the Template "thing"
	 * 
	 * @param type
	 * @return
	 */
	private static String getTemplateForCm(CountersModel cm, Type type)
	{
		String  name         = cm.getDisplayName();
		int     queryTimeout = cm.getDefaultQueryTimeout();
		int     postpone     = cm.getDefaultPostponeTime();
		boolean paused       = cm.getDefaultIsDataPollingPaused();
		boolean bg           = cm.getDefaultIsBackgroundDataPollingEnabled();
		boolean resetNC20    = cm.getDefaultIsNegativeDiffCountersToZero();
		boolean storePcs     = cm.getDefaultIsPersistCountersEnabled(); 
		boolean pcsAbs       = cm.getDefaultIsPersistCountersAbsEnabled();
		boolean pcsDiff      = cm.getDefaultIsPersistCountersDiffEnabled();
		boolean pcsRate      = cm.getDefaultIsPersistCountersRateEnabled();

		// Get the CM's LEVEL
		Type cmTemplateLevel = cm.getTemplateLevel();

		// if level in CM is greater or equal to the level we check for...
		storePcs = type.ordinal() >= cmTemplateLevel.ordinal();
		
		if (cmTemplateLevel == Type.OFF || type == Type.OFF)
			storePcs = false;

		return name + "={" +
			PROPKEY_queryTimeout + "=" + queryTimeout + ", " +
			PROPKEY_postpone     + "=" + postpone     + ", " +
			PROPKEY_paused       + "=" + paused       + ", " +
			PROPKEY_bg           + "=" + bg           + ", " +
			PROPKEY_resetNC20    + "=" + resetNC20    + ", " +
			PROPKEY_storePcs     + "=" + storePcs     + ", " +
			PROPKEY_pcsAbs       + "=" + pcsAbs       + ", " +
			PROPKEY_pcsDiff      + "=" + pcsDiff      + ", " +
			PROPKEY_pcsRate      + "=" + pcsRate      + "}; ";
	}

	public static final String PROPKEY_queryTimeout = "queryTimeout";
	public static final String PROPKEY_postpone     = "postpone";
	public static final String PROPKEY_paused       = "paused";
	public static final String PROPKEY_bg           = "bg";
	public static final String PROPKEY_resetNC20    = "resetNC20";
	public static final String PROPKEY_storePcs     = "storePcs";
	public static final String PROPKEY_pcsAbs       = "pcsAbs";
	public static final String PROPKEY_pcsDiff      = "pcsDiff";
	public static final String PROPKEY_pcsRate      = "pcsRate";

	/**
	 * get the "short" name of a Performance Counter based on it's "long" name
	 * @param longName The long name of the Performance Counter
	 * @return The "short" name of the Performance Counter, null if it can't be found.
	 */
	public static String getShortName(String longName)
	{
		return _nameLongToShortMap.get(longName);
	}

	/**
	 * get the "long" name of a Performance Counter based on it's "short" name
	 * @param shortName The short name of the Performance Counter
	 * @return The "long" name of the Performance Counter, null if it can't be found.
	 */
	public static String getLongName(String shortName)
	{
		return _nameShortToLongMap.get(shortName);
	}

	/**
	 * Get all "short" names
	 * @return a Set of all "short" names
	 */
	public static Set<String> getShortNames()
	{
		return _nameShortToLongMap.keySet();
	}

	/**
	 * Get all "long" names
	 * @return a Set of all "long" names
	 */
	public static Set<String> getLongNames()
	{
		return _nameLongToShortMap.keySet();
	}

	/**
	 * @return a Map of "short" to "long" values of the Performance Counters
	 */
	public static Map<String, String> getShortToLongMap()
	{
		return _nameShortToLongMap;
	}

	/**
	 * @return a Map of "long" to "short" values of the Performance Counters
	 */
	public static Map<String, String> getLongToShortMap()
	{
		return _nameLongToShortMap;
	}

	/**
	 *
	 * @param templateName
	 * @return
	 * @throws Exception
	 */
	public static List<String> getTemplateList(String templateName)
	throws Exception
	{
		List<String> activeCmList = new ArrayList<String>();

//		activeCmList.add(SummaryPanel.CM_NAME);
		activeCmList.add(CounterController.getSummaryCmName());

		PropPropEntry ppe = null;
		if      (templateName.equalsIgnoreCase("small"))  ppe = CounterSetTemplates.SYSTEM_TEMPLATE_PCS_ON_SMALL;
		else if (templateName.equalsIgnoreCase("medium")) ppe = CounterSetTemplates.SYSTEM_TEMPLATE_PCS_ON_MEDIUM;
		else if (templateName.equalsIgnoreCase("large"))  ppe = CounterSetTemplates.SYSTEM_TEMPLATE_PCS_ON_LARGE;
		else if (templateName.equalsIgnoreCase("all"))    ppe = CounterSetTemplates.SYSTEM_TEMPLATE_PCS_ON_ALL;
//		else if (templateName.equalsIgnoreCase("all"))
//		{
//			ppe = new PropPropEntry();
//			for (String longName : getLongNames())
//			{
//				String postpone = SYSTEM_TEMPLATE_PCS_ON_ALL.getProperty(longName, "postpone", "0");
//				ppe.put(longName, "storePcs", "true");
//				ppe.put(longName, "postpone", postpone);
//			}
//		}
		else
		{
			throw new Exception("Unknown template name '"+templateName+"'.");
		}

		// Some sort of TEMPLATE was found
		if (ppe != null)
		{
			for (String name : ppe.keySet())
			{
				boolean storePcs = ppe.getBooleanProperty(name, "storePcs", false);
				int     postpone = ppe.getIntProperty    (name, "postpone", 0);
				if (storePcs)
				{
					if (postpone <= 0)
						activeCmList.add(name);
					else
						activeCmList.add(name+":"+postpone);
				}
			}
		}

		return activeCmList;
	}


	public static void main(String[] args)
	{
//		GetCountersNoOp counters = new GetCountersNoOp();
//		CounterController.setInstance(counters);
//		counters.init();
		CounterControllerNoOp counters = new CounterControllerNoOp(true);
		CounterController.setInstance(counters);
		counters.init();

		System.out.println("SYSTEM_TEMPLATE_PCS_ON_SMALL  : \n"+SYSTEM_TEMPLATE_PCS_ON_SMALL .toString(25, 6));
		System.out.println("SYSTEM_TEMPLATE_PCS_ON_MEDIUM : \n"+SYSTEM_TEMPLATE_PCS_ON_MEDIUM.toString(25, 6));
		System.out.println("SYSTEM_TEMPLATE_PCS_ON_LARGE  : \n"+SYSTEM_TEMPLATE_PCS_ON_LARGE .toString(25, 6));
		System.out.println("SYSTEM_TEMPLATE_PCS_ON_ALL    : \n"+SYSTEM_TEMPLATE_PCS_ON_ALL   .toString(25, 6));

		System.out.println();
		System.out.println("SYSTEM_TEMPLATE_PCS_OFF_SMALL : \n"+SYSTEM_TEMPLATE_PCS_OFF_SMALL .toString(25, 6));
		System.out.println("SYSTEM_TEMPLATE_PCS_OFF_MEDIUM: \n"+SYSTEM_TEMPLATE_PCS_OFF_MEDIUM.toString(25, 6));
		System.out.println("SYSTEM_TEMPLATE_PCS_OFF_LARGE : \n"+SYSTEM_TEMPLATE_PCS_OFF_LARGE .toString(25, 6));
		System.out.println("SYSTEM_TEMPLATE_PCS_OFF_ALL   : \n"+SYSTEM_TEMPLATE_PCS_OFF_ALL   .toString(25, 6));
	}
}



