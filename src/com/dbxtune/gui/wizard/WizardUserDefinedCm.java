/*******************************************************************************
 * Copyright (C) 2010-2025 Goran Schwarz
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

/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package com.dbxtune.gui.wizard;


import java.awt.Dimension;
import java.util.Map;

import javax.swing.UIManager;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.netbeans.api.wizard.WizardDisplayer;
import org.netbeans.spi.wizard.Wizard;
import org.netbeans.spi.wizard.WizardException;
import org.netbeans.spi.wizard.WizardPage;

import com.dbxtune.Version;
import com.dbxtune.utils.AseConnectionFactory;


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
//		System.setProperty("wizard.sidebar.image", "com/dbxtune/gui/wizard/WizardUserDefinedCm.png");
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
		Wizard wiz = WizardPage.createWizard("Create: User Defined Counter Model.", wca, new WizardUserDefinedCmResultProducer());
		
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
		// Set Log4j Log Level
		Configurator.setRootLevel(Level.TRACE);

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

