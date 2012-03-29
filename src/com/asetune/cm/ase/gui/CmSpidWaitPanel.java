package com.asetune.cm.ase.gui;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.HighlightPredicate;
import org.jdesktop.swingx.decorator.HighlighterFactory;

import com.asetune.Version;
import com.asetune.cm.CountersModel;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.utils.Configuration;
import com.asetune.utils.SwingUtils;

public class CmSpidWaitPanel
extends TabularCntrPanel
{
//	private static final Logger  _logger	           = Logger.getLogger(CmSpidWaitPanel.class);
	private static final long    serialVersionUID      = 1L;

//	private static final String  PROP_PREFIX           = CmSpidWait.CM_NAME;

	public CmSpidWaitPanel(CountersModel cm)
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

		// HIGHLIGHTER that changes color when a new SPID number is on next row...

		if (conf != null) 
			colorStr = conf.getProperty(getName()+".color.group");

		addHighlighter( new ColorHighlighter(new HighlightPredicate()
		{
			boolean[] _rowIsHighlighted = new boolean[0];
			int       _lastRowId        = 0;     // Used to sheet on table refresh

			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
				if (adapter.row == 0)
					return false;

				// Resize array if it's to small
				if (_rowIsHighlighted.length < adapter.getRowCount())
					_rowIsHighlighted = new boolean[adapter.getRowCount()];

				// Lets try to sheet a bit, if we are of some row as last invocation, reuse that decision
				if (_lastRowId == adapter.row)
					return _rowIsHighlighted[adapter.row];
				_lastRowId = adapter.row;

				// Lets get values of "change color" column
				int    spidCol      = adapter.getColumnIndex("SPID");
				int    thisModelRow = adapter.convertRowIndexToModel(adapter.row);
				int    prevModelRow = adapter.convertRowIndexToModel(adapter.row - 1);

				Object thisSpid    = adapter.getValueAt(thisModelRow, spidCol);
				Object prevSpid    = adapter.getValueAt(prevModelRow, spidCol);

				// Previous rows highlight will be a decision to keep or invert the highlight
				boolean prevRowIsHighlighted = _rowIsHighlighted[adapter.row - 1];

				if (thisSpid.equals(prevSpid))
				{
					// Use same highlight value as previous row
					boolean isHighlighted = prevRowIsHighlighted;
					_rowIsHighlighted[adapter.row] = isHighlighted;

					return isHighlighted;
				}
				else
				{
					// Invert previous highlight value
					boolean isHighlighted = ! prevRowIsHighlighted;
					_rowIsHighlighted[adapter.row] = isHighlighted;

					return isHighlighted;
				}
			}
		}, SwingUtils.parseColor(colorStr, HighlighterFactory.GENERIC_GRAY), null));
	}

	@Override
	protected JPanel createLocalOptionsPanel()
	{
		JPanel panel = SwingUtils.createPanel("Local Options", true);
		panel.setLayout(new MigLayout("ins 0, gap 0", "", "0[0]0"));

		String tooltip = 
			"<html>" +
			"Add extra where clause to the query that fetches WaitTime for SPID's<br>" +
			"To check initial SQL statement that are used: Right click on the 'tab', and choose 'Properties'<br>" +
			"The extra string will replace the string 'RUNTIME_REPLACE::EXTRA_WHERE_CLAUSE'.<br>" +
			"<br>" +
			"<b>Examples:</b><br>" +
			"<b>- Only users with the login 'sa'</b><br>" +
			"<code>SPID in (select spid from master..sysprocesses where suser_name(suid) = 'sa')                     </code><br>" +
			"<br>" +
			"<b>- Same as above, but in a more efficent way (only in ASE 15.0.2 ESD#5 or higher)</b><br>" +
			"<code>suser_name(ServerUserID) = 'sa'                                                                   </code><br>" +
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
		final RSyntaxTextArea extraWhereClause_txt = new RSyntaxTextArea();
		final JButton         extraWhereClause_but = new JButton("Apply Extra Where Clause");

		Configuration conf = Configuration.getCombinedConfiguration();
		String extraWhereClause = (conf == null ? "" : conf.getProperty(getName()+".sample.extraWhereClause", ""));

		extraWhereClause_txt.setText(extraWhereClause);
		extraWhereClause_txt.setHighlightCurrentLine(false);
		extraWhereClause_txt.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_SQL);
		extraWhereClause_txt.setName(getName()+".sample.extraWhereClause");

		extraWhereClause_but.setToolTipText(tooltip);
		extraWhereClause_txt.setToolTipText(tooltip);

		panel.add(extraWhereClause_txt, "grow, push, wrap");
		panel.add(extraWhereClause_but, "wrap");

		extraWhereClause_but.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				// Need TMP since we are going to save the configuration somewhere
				Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
				if (conf == null) return;
				conf.setProperty(getName()+".sample.extraWhereClause", extraWhereClause_txt.getText().trim());
				conf.save();
			}
		});
		
		return panel;
	}
}
