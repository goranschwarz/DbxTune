package com.asetune.cm.ase.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.HighlightPredicate;

import com.asetune.Version;
import com.asetune.cm.CountersModel;
import com.asetune.gui.ChangeToJTabDialog;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.utils.Configuration;
import com.asetune.utils.SwingUtils;

public class CmBlockingPanel
extends TabularCntrPanel
{
//	private static final Logger  _logger	           = Logger.getLogger(CmBlockingPanel.class);
	private static final long    serialVersionUID      = 1L;

//	private static final String  PROP_PREFIX           = CmBlocking.CM_NAME;

	public CmBlockingPanel(CountersModel cm)
	{
		super(cm);

		if (cm.getIconFile() != null)
			setIcon( SwingUtils.readImageIcon(Version.class, cm.getIconFile()) );

		init();
	}
	
	private void init()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		String colorStr = null;

		// PINK = spid is BLOCKED by some other user
		if (conf != null) colorStr = conf.getProperty(getName()+".color.blocked");
		addHighlighter( new ColorHighlighter(new HighlightPredicate()
		{
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
				String blockedState = adapter.getString(adapter.getColumnIndex("BlockedState"));
				if ("Blocked".equals(blockedState))
					return true;
				return false;
			}
		}, SwingUtils.parseColor(colorStr, Color.PINK), null));

		// Mark the row as RED if blocks other users from working
		if (conf != null) colorStr = conf.getProperty(getName()+".color.blocking");
		addHighlighter( new ColorHighlighter(new HighlightPredicate()
		{
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
				String blockedState = adapter.getString(adapter.getColumnIndex("BlockedState"));
				if ("Blocking".equals(blockedState))
					return true;
				return false;
			}
		}, SwingUtils.parseColor(colorStr, Color.RED), null));
	}

	@Override
	protected JPanel createLocalOptionsPanel()
	{
		JPanel panel = SwingUtils.createPanel("Local Options", true);
		panel.setLayout(new MigLayout("ins 0, gap 0", "", "0[0]0"));

		JButton resetMoveToTab_but = new JButton("Reset 'Move to Tab' settings.");

		resetMoveToTab_but.setToolTipText(
				"<html>" +
				"Reset the option: To automatically switch to this tab when you have <b>blocking locks</b>.<br>" +
				"Next time this happens, a popup will ask you what you want to do." +
				"</html>");
		resetMoveToTab_but.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				ChangeToJTabDialog.resetSavedSettings(getPanelName());
			}
		});
		
		panel.add(resetMoveToTab_but, "wrap");

		return panel;
	}
}