package com.asetune.cm.ase.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JPanel;

import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.HighlightPredicate;

import com.asetune.cm.CountersModel;
import com.asetune.cm.ase.CmLocks;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.ui.rsyntaxtextarea.AsetuneSyntaxConstants;
import com.asetune.ui.rsyntaxtextarea.RSyntaxTextAreaX;
import com.asetune.utils.Configuration;
import com.asetune.utils.SwingUtils;

import net.miginfocom.swing.MigLayout;

public class CmLocksPanel
extends TabularCntrPanel
{
//	private static final Logger  _logger	           = Logger.getLogger(CmLocksPanel.class);
	private static final long    serialVersionUID      = 1L;

	public static final String  TOOLTIP_sample_extraWhereClause = 
		"<html>" +
		"Add extra where clause to the query that fetches information for Locks<br>" +
		"To check SQL statement that are used: Right click on the 'tab', and choose 'Properties'<br>" +
		"<br>" +
		"<b>Examples:</b><br>" +
		"<b>- Only users with the login 'sa'</b><br>" +
		"<code>SPID in (select spid from master..sysprocesses where suser_name(suid) = 'sa')                     </code><br>" +
		"<br>" +
		"<b>- Only with programs that has logged in via 'isql'</b><br>" +
		"<code>SPID in (select spid from master..sysprocesses where program_name = 'isql')                       </code><br>" +
		"<br>" +
		"<b>- Only with clients that has logged in from the host 'host99'</b><br>" +
		"<code>SPID in (select spid from master..sysprocesses where hostname = 'host99')                         </code><br>" +
		"<br>" +
		"<b>- Only with clients that has logged in from the IP address '192.168.0.1'</b><br>" +
		"<code>SPID in (select spid from master..sysprocesses where ipaddr = '192.168.0.123')                    </code><br>" +
		"<br>" +
		"<b>- Only with clients that has logged in to ASE in the last 60 seconds</b><br>" +
		"<code>SPID in (select spid from master..sysprocesses where datediff(ss,loggedindatetime,getdate()) < 60)</code><br>" +
		"</html>";
		
	public CmLocksPanel(CountersModel cm)
	{
		super(cm);

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
			@Override
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
				String blockedState = adapter.getString(adapter.getColumnIndex("BlockedState"));
				if (blockedState.indexOf("Blocked") >= 0)
					return true;
				return false;
			}
		}, SwingUtils.parseColor(colorStr, Color.PINK), null));

		// Mark the row as RED if blocks other users from working
		if (conf != null) colorStr = conf.getProperty(getName()+".color.blocking");
		addHighlighter( new ColorHighlighter(new HighlightPredicate()
		{
			@Override
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
				String blockedState = adapter.getString(adapter.getColumnIndex("BlockedState"));
				if (blockedState.indexOf("Blocking") >= 0)
					return true;
				return false;
			}
		}, SwingUtils.parseColor(colorStr, Color.RED), null));
	}

	private RSyntaxTextAreaX _sampleExtraWhereClause_txt;
	private JButton          _sampleExtraWhereClause_but;

	@Override
	protected JPanel createLocalOptionsPanel()
	{
		JPanel panel = SwingUtils.createPanel("Local Options", true);
		panel.setLayout(new MigLayout("flowy, ins 0, gap 0", "", "0[0]0"));
//		panel.setToolTipText(
//			"<html>" +
//				"All the options in this panel executes additional SQL lookups in the database <b>after</b> the result set has been delivered.<br>" +
//				"This means that we are doing 1 extra SQL lookup for every checkbox option per row on the result set table.<br>" +
//				"<br>" +
//				"NOTE: So if you check all the options, the time to do refresh on this tab will <b>increase</b>." +
//			"</html>");

		Configuration conf = Configuration.getCombinedConfiguration();

		_sampleExtraWhereClause_txt = new RSyntaxTextAreaX();
		_sampleExtraWhereClause_but = new JButton("Apply Extra Where Clause");

		_sampleExtraWhereClause_but.setToolTipText(TOOLTIP_sample_extraWhereClause);
		_sampleExtraWhereClause_txt.setToolTipText(TOOLTIP_sample_extraWhereClause);


		// Set initial values for some fields
		String sampleExtraWhereClause = (conf == null ? CmLocks.DEFAULT_sample_extraWhereClause : conf.getProperty(CmLocks.PROPKEY_sample_extraWhereClause, CmLocks.DEFAULT_sample_extraWhereClause));

		_sampleExtraWhereClause_txt.setText(sampleExtraWhereClause);
		_sampleExtraWhereClause_txt.setHighlightCurrentLine(false);
		_sampleExtraWhereClause_txt.setSyntaxEditingStyle(AsetuneSyntaxConstants.SYNTAX_STYLE_SYBASE_TSQL);

		panel.add(_sampleExtraWhereClause_txt, "grow, push");
		panel.add(_sampleExtraWhereClause_but, "wrap");

		_sampleExtraWhereClause_but.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				// Need TMP since we are going to save the configuration somewhere
				Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
				if (conf == null) return;
				conf.setProperty(CmLocks.PROPKEY_sample_extraWhereClause, _sampleExtraWhereClause_txt.getText().trim());
				conf.save();
				
				// ReInitialize the SQL
				getCm().setSql(null);
			}
		});

		return panel;
	}
}
