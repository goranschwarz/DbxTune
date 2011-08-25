/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package asemon.gui.wizard;


import java.awt.Dimension;
import java.util.Iterator;
import java.util.Map;

import javax.swing.UIManager;

import org.apache.log4j.Logger;
import org.netbeans.api.wizard.WizardDisplayer;
import org.netbeans.spi.wizard.Wizard;
import org.netbeans.spi.wizard.WizardException;
import org.netbeans.spi.wizard.WizardPage;

import asemon.utils.Configuration;

public class WizardOffline
{
	private static Logger _logger = Logger.getLogger(WizardOffline.class);
    private static final long serialVersionUID = 1L;
//	private Map	             _settings;
//	private WizardController _controller;

	protected static final String MigLayoutHelpConstraints = "wmin 100, span, growx, gapbottom 15, wrap";
	protected static final String MigLayoutConstraints1 = "";
	protected static final String MigLayoutConstraints2 = "[] [grow] []";
	protected static final String MigLayoutConstraints3 = "";
	
	protected static final Dimension preferredSize = new Dimension(400, 300);
	
	public WizardOffline()
	{
		System.clearProperty("wizard.sidebar.image");
		System.setProperty("wizard.sidebar.image", "asemon/gui/wizard/WizardOffline.png");

//		BufferedImage img = ImageIO.read (getClass().getResource ("WizardOffline.png"));
//		UIManager.put ("wizard.sidebar.image", img);

		Class<?>[] wca = { 
				WizardOfflinePage1.class,
				WizardOfflinePage2.class,
				WizardOfflinePage3.class,
				WizardOfflinePage4.class,
				WizardOfflinePage5.class
				};
		Wizard wiz = WizardPage.createWizard("Create an offline sample session.", wca, new WizardOffllinePageSummary());
		
		@SuppressWarnings("unchecked")
		Map<String,String> gatheredSettings = (Map) WizardDisplayer.showWizard(wiz);
//		System.out.println("gatheredSettings="+gatheredSettings);
		try {
			finish(gatheredSettings);
		} catch (WizardException e) {
			e.printStackTrace();
		}

		System.clearProperty("wizard.sidebar.image");
	}

	

	private void fixProp(Map<String,String> settings, String prefix, String key, boolean encrypt)
	{
		String newKey = prefix + key;
		String val    = "";

		// Check for "UNSAFE" values that has been stored in the Map by the wizard.
		// If there are any, just print into and throw the exception.
		try
		{
			val = settings.get(key);
		}
		catch(ClassCastException e)
		{
			for (Iterator<String> it=settings.keySet().iterator(); it.hasNext();)
			{
				String k = it.next();
				Object o = settings.get(k);
				if ( ! (o instanceof String) )
				{
					_logger.error("Internal ERROR: ClassCastException from settingsMap with key='"+k+"', val='"+o+"', type='"+o.getClass().getName()+"'.");
				}
			}
			throw e;
		}

		if (encrypt)
			settings.put(newKey, Configuration.encryptPropertyValue(newKey, val+""));
		else
			settings.put(newKey, val); 

		settings.remove(key);
	}

	protected Object finish(Map<String,String> settings) throws WizardException
	{
    	if (settings == null)
    		return null;

    	Configuration offlineProps = new Configuration();
		Configuration aseMonProps = Configuration.getInstance(Configuration.CONF);
    	
		offlineProps.setFilename(settings.get("storeFile"));
		offlineProps.setEmbeddedMessage("File generated by Offline Wizard.");
		//offlineProps.save();

		//Process some props for PCS functionality, or change name in WizardPanel?
		fixProp(settings, "PersistWriterJdbc.", "jdbcDriver",           false);
		fixProp(settings, "PersistWriterJdbc.", "jdbcUrl",              false);
		fixProp(settings, "PersistWriterJdbc.", "jdbcUsername",         false);
		fixProp(settings, "PersistWriterJdbc.", "jdbcPassword",         true);
		fixProp(settings, "PersistWriterJdbc.", "startH2NetworkServer", false);

		fixProp(settings, "conn.",    "aseHost",     false);
		fixProp(settings, "conn.",    "aseName",     false);
		fixProp(settings, "conn.",    "asePassword", true);
		fixProp(settings, "conn.",    "asePort",     false);
		fixProp(settings, "conn.",    "aseUsername", false);

		fixProp(settings, "offline.", "sampleTime",  false);
		fixProp(settings, "offline.", "storeFile",   false);


		//Add the PCS Writer, or add it as a wizard property?
		String writerClass = aseMonProps.getProperty("PersistentCounterHandler.WriterClass");
		if(writerClass == null)
			writerClass = "asemon.pcs.PersistWriterJdbc";
		settings.put("PersistentCounterHandler.WriterClass", writerClass);
		
//		settings.put("CM.sysMon.test", "testtest");
		
		for (Iterator<String> iterator = settings.keySet().iterator(); iterator.hasNext();)
		{
			String key = iterator.next();
			String val = settings.get(key).toString();
			
			if(key.startsWith("to-be-discarded.")) // these entries will be discarded
			{
				// get first part of the key, which should be the CmName
				String cmName = val;
				
				// Check if that cm is a User Defined Counter in the current configuration
				// And then write all the UDC information to the destination file.
				if(getUdcConfigName(cmName) != null)
				{
					String udcPrefix = "udc." + getUdcConfigName(cmName);

					for (String udcKey : aseMonProps.getKeys(udcPrefix))
					{
						String udcVal = aseMonProps.getProperty(udcKey);
						offlineProps.put(udcKey, udcVal);
					}
				}
			}
			else
			{
				// write the key/value
				offlineProps.put(key, val);
			}
		}
		offlineProps.save();
		
		//Really you would construct some object or do something with the
		//contents of the map
		return settings;
	}
    
    private String getUdcConfigName(String name)
    {
    	String        udcConfigName  = null;
    	String        udcPrefix      = "udc.";
		Configuration aseMonProps = Configuration.getInstance(Configuration.CONF);
    	
		for (String udcKey : aseMonProps.getKeys(udcPrefix))
		{
			String udcName = udcKey.substring(udcPrefix.length(), udcKey.indexOf(".", udcPrefix.length()));
			if(udcKey.equals(udcPrefix + name + ".name"))
			{
				udcConfigName = udcName;
				return udcConfigName;
			}
			
		}
		return udcConfigName;
    }
    
	public static void main(String[] args)
	{
    	try 
    	{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    		//UIManager.setLookAndFeel(new SubstanceOfficeSilver2007LookAndFeel());
		} 
    	catch (Exception e) 
		{
			e.printStackTrace();
		}
    	new WizardOffline();
//    	WizardOffline provider = new WizardOffline();
//		Wizard wizard = provider.createWizard();
//		Object result = WizardDisplayer.showWizard(wizard);
	}

}

