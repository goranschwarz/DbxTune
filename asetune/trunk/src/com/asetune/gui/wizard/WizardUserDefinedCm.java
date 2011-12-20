/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package com.asetune.gui.wizard;


import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.Properties;

import javax.swing.UIManager;

import org.apache.log4j.PropertyConfigurator;
import org.netbeans.api.wizard.WizardDisplayer;
import org.netbeans.spi.wizard.Wizard;
import org.netbeans.spi.wizard.WizardException;
import org.netbeans.spi.wizard.WizardPage;

import com.asetune.Version;
import com.asetune.utils.AseConnectionFactory;
import com.asetune.utils.SwingUtils;


public class WizardUserDefinedCm
{
    private static final long serialVersionUID = 1L;
//	private Map	             _settings;
//	private WizardController _controller;

	protected static final String MigLayoutHelpConstraints = "wmin 100, span, growx, gapbottom 15, wrap";
	protected static final String MigLayoutConstraints1 = "";
	protected static final String MigLayoutConstraints2 = "[] [grow] []";
	protected static final String MigLayoutConstraints3 = "";
	
	protected static final Dimension preferredSize = new Dimension(570, 470);
	
	public WizardUserDefinedCm()
	{
		System.clearProperty("wizard.sidebar.image");
//		System.setProperty("wizard.sidebar.image", "com/asetune/gui/wizard/WizardUserDefinedCm.png");
//      The image 'WizardUserDefinedCm.png' does not exist.

//		BufferedImage img = ImageIO.read (getClass().getResource ("WizardUserDefinedCm.png"));
//		UIManager.put ("wizard.sidebar.image", img);

		Class<?>[] wca = { 
				WizardUserDefinedCmPage0verview.class,
				WizardUserDefinedCmPage1.class,   // Naming the UDC
				WizardUserDefinedCmPage2.class,   // SQL Statement to execute
				WizardUserDefinedCmPage3.class,   // Primary Key
				WizardUserDefinedCmPage4.class,   // Diff Calculation on columns
				WizardUserDefinedCmPage5.class,   // Percent columns, if any...
				WizardUserDefinedCmPage6.class,   // Attach a Graph to this UDC
				WizardUserDefinedCmPageSummary.class
				};
		Wizard wiz = WizardPage.createWizard("Create an User Defined Counter Model.", wca, new WizardUserDefinedCmResultProducer());
		
		@SuppressWarnings({ "unchecked", "rawtypes" })
		Map<String,String> gatheredSettings = (Map) WizardDisplayer.showWizard(wiz);
//		System.out.println("gatheredSettings="+gatheredSettings);
		try {
			finish(gatheredSettings);
		} catch (WizardException e) {
			e.printStackTrace();
		}
		System.clearProperty("wizard.sidebar.image");
	}

	

	protected Object finish(Map<String,String> settings) throws WizardException
	{
		
		//Really you would construct some object or do something with the
		//contents of the map
		return settings;
	}
    
	public static void main(String[] args)
	{
		Properties log4jProps = new Properties();
		//log4jProps.setProperty("log4j.rootLogger", "INFO, A1");
		log4jProps.setProperty("log4j.rootLogger", "TRACE, A1");
		log4jProps.setProperty("log4j.appender.A1", "org.apache.log4j.ConsoleAppender");
		log4jProps.setProperty("log4j.appender.A1.layout", "org.apache.log4j.PatternLayout");
		log4jProps.setProperty("log4j.appender.A1.layout.ConversionPattern", "%d - %-5p - %-30c{1} - %m%n");
		PropertyConfigurator.configure(log4jProps);

		// Create the factory object that holds the database connection using
		// the data specified on the command line
    	try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		AseConnectionFactory.setAppName ( Version.getAppName()+"-Wizard-UDC" );
		AseConnectionFactory.setUser    ( "sa" );
		AseConnectionFactory.setPassword( "" );
		AseConnectionFactory.setHostPort( "goransxp", "5000" );

		new WizardUserDefinedCm();
	}

}

