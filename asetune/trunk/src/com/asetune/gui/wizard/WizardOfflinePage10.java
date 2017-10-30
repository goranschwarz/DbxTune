/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package com.asetune.gui.wizard;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;
import javax.swing.JLabel;

import org.netbeans.spi.wizard.WizardPage;

import com.asetune.gui.swing.MultiLineLabel;

import net.miginfocom.swing.MigLayout;


public class WizardOfflinePage10
extends WizardPage
implements ActionListener
{
    private static final long serialVersionUID = 1L;
	private static final String WIZ_NAME = "pcsWriters";
	private static final String WIZ_DESC = "Other PCS Writers";
	private static final String WIZ_HELP = "<html>Configure other writers that can send Counter data to <i>various</i> places.<html>";

//	private JLabel     _notEnabled_lbl          = new JLabel("Alarm Writers isn't enabled, so this page can be skipped.");
	private JCheckBox  _pcsWriters_chk = new JCheckBox("<html>Send Counter Data to <i>other</i> places than a database.</html>", true);

	public static String getDescription() { return WIZ_DESC; }
	@Override
	public Dimension getPreferredSize() { return WizardOffline.preferredSize; }

	public WizardOfflinePage10()
	{
		super(WIZ_NAME, WIZ_DESC);

		setLayout(new MigLayout(WizardOffline.MigLayoutConstraints1, WizardOffline.MigLayoutConstraints2, WizardOffline.MigLayoutConstraints3));

		// Add a helptext
		add( new MultiLineLabel(WIZ_HELP), WizardOffline.MigLayoutHelpConstraints );

		add(_pcsWriters_chk,          "hidemode 3, wrap");
		add(new JLabel(""),          "hidemode 3, wrap 30");
		add(new JLabel("NOT YET IMPLEMENTED"),          "hidemode 3, wrap");

		initData();
	}

	private void initData()
	{
	}

	@Override
	public void actionPerformed(ActionEvent ae)
	{
	}

	@Override
	protected String validateContents(Component comp, Object event)
	{
		return null;
	}
}
