/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package com.asetune.gui.wizard;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JLabel;

import net.miginfocom.swing.MigLayout;

import org.netbeans.spi.wizard.WizardPage;

import com.asetune.gui.swing.MultiLineLabel;


public class WizardOfflinePage5
extends WizardPage
implements ActionListener
{
    private static final long serialVersionUID = 1L;
	private static final String WIZ_NAME = "sql-capture";
	private static final String WIZ_DESC = "Capture SQL";
	private static final String WIZ_HELP = "Should individual SQL Statements be captured.";

//	private JTextField _sampleTime = new JTextField("60");

	public static String getDescription() { return WIZ_DESC; }
	public Dimension getPreferredSize() { return WizardOffline.preferredSize; }

	public WizardOfflinePage5()
	{
		super(WIZ_NAME, WIZ_DESC);
		
		setLayout(new MigLayout(WizardOffline.MigLayoutConstraints1, WizardOffline.MigLayoutConstraints2, WizardOffline.MigLayoutConstraints3));

		// Add a helptext
		add( new MultiLineLabel(WIZ_HELP), WizardOffline.MigLayoutHelpConstraints );

		add(new JLabel("<html>This is <b>NOT YET</b> implemented, stay tuned.</html>"), "growx, wrap 10");
		add(new JLabel("<html>Just press <b>Next</b> to continue.</html>"), "growx");

		initData();
	}

	private void initData()
	{
	}

	@Override
	protected String validateContents(Component comp, Object event)
	{
		return null;
	}

	public void actionPerformed(ActionEvent ae)
	{
	}
}

