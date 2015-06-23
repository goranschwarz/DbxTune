package com.asetune.cm.sqlserver.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;

import com.asetune.Version;
import com.asetune.cm.CountersModel;
import com.asetune.cm.sqlserver.CmExecQueryStats;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.ui.rsyntaxtextarea.AsetuneSyntaxConstants;
import com.asetune.utils.Configuration;
import com.asetune.utils.SwingUtils;

public class CmExecQueryStatsPanel
extends TabularCntrPanel
{
//	private static final Logger  _logger	           = Logger.getLogger(CmPlanCacheDetailsPanel.class);
	private static final long    serialVersionUID      = 1L;

	public static final String  TOOLTIP_sample_afterPrevSample  = 
		"<html>"
		+ "Only show executions that has accured <b>after</b> the previous sample.<br>"
		+ "<br>"
		+ "<b>Note:</b> Diff calculations will <b>only</b> be accurate if a row/record is present in previous samples.<br>"
		+ "If previous record was not present <i>diff</i> counters will be the same as the <i>absolute</i> value, and <i>rate</i> is simply calculated by taking the <i>diff</i> counter divided by sample time<br>"
		+ "</html>";

	public static final String  TOOLTIP_sample_lastXminutes = 
		"<html>"
		+ "Only show executions that has accured at the last X minutes (default is 10 minutes).<br>"
		+ "<br>"
		+ "<b>Note:</b> This will just restrict number or rows a bit, It's probably better than <i>'Show only SQL executed since last sample time'</i> but same rules applies.<br>"
		+ "<b>Note:</b> Number of minutes can be changed using the property: <code>"+CmExecQueryStats.PROPKEY_sample_lastXminutesTime+"</code><br>"
		+ "</html>";
			
	public static final String  TOOLTIP_sample_lastXminutesTime = 
			"<html>"
			+ "Number of minutes to show if this is enabled (default is 10 minutes).<br>"
			+ "</html>";
				
	public static final String  TOOLTIP_sample_extraWhereClause = 
		"<html>" +
		"Add extra where clause to the query that fetches information.<br>" +
		"To check SQL statement that are used: Right click on the 'tab', and choose 'Properties'<br>" +
		"<br>" +
//		"<b>Examples:</b><br>" +
//		"<b>- Only users with the login 'sa'</b><br>" +
//		"<code>S.SESSION_USER_NAME = 'SYSTEM' </code><br>" +
//		"<br>" +
//		"<b>- Only for some specific SCHEMA</b><br>" +
//		"<code>S.SCHEMA_NAME = 'SOME_NAME' </code><br>" +
//		"<br>" +
//		"<b>- Only for some specific Table</b><br>" +
//		"<code>S.ACCESSED_TABLE_NAMES like '%TABLE1%' </code><br>" +
//		"<br>" +
//		"<b>- Only for some specific SQL Statemets</b><br>" +
//		"<code>S.STATEMENT_STRING like '%SOME_STRING%' </code><br>" +
//		"<br>" +
		"</html>";

	public CmExecQueryStatsPanel(CountersModel cm)
	{
		super(cm);

//		if (cm.getIconFile() != null)
//			setIcon( SwingUtils.readImageIcon(Version.class, cm.getIconFile()) );

		init();
	}
	
	private void init()
	{
//		Configuration conf = Configuration.getCombinedConfiguration();
//		String colorStr = null;
//
//		// HIGHLIGHTER that changes color when a new SPID number is on next row...
//
//		if (conf != null) 
//			colorStr = conf.getProperty(getName()+".color.group");
//
//		addHighlighter( new ColorHighlighter(new HighlightPredicate()
//		{
//			boolean[] _rowIsHighlighted = new boolean[0];
//			int       _lastRowId        = 0;     // Used to sheet on table refresh
//
//			@Override
//			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
//			{
//				if (adapter.row == 0)
//					return false;
//
//				// Resize array if it's to small
//				if (_rowIsHighlighted.length < adapter.getRowCount())
//					_rowIsHighlighted = new boolean[adapter.getRowCount()];
//
//				// Lets try to sheet a bit, if we are of some row as last invocation, reuse that decision
//				if (_lastRowId == adapter.row)
//					return _rowIsHighlighted[adapter.row];
//				_lastRowId = adapter.row;
//
//				// Lets get values of "change color" column
//				int    spidCol      = adapter.getColumnIndex("SPID");
//				int    thisModelRow = adapter.convertRowIndexToModel(adapter.row);
//				int    prevModelRow = adapter.convertRowIndexToModel(adapter.row - 1);
//
//				Object thisSpid    = adapter.getValueAt(thisModelRow, spidCol);
//				Object prevSpid    = adapter.getValueAt(prevModelRow, spidCol);
//
//				// Previous rows highlight will be a decision to keep or invert the highlight
//				boolean prevRowIsHighlighted = _rowIsHighlighted[adapter.row - 1];
//
//				if (thisSpid != null && thisSpid.equals(prevSpid))
//				{
//					// Use same highlight value as previous row
//					boolean isHighlighted = prevRowIsHighlighted;
//					_rowIsHighlighted[adapter.row] = isHighlighted;
//
//					return isHighlighted;
//				}
//				else
//				{
//					// Invert previous highlight value
//					boolean isHighlighted = ! prevRowIsHighlighted;
//					_rowIsHighlighted[adapter.row] = isHighlighted;
//
//					return isHighlighted;
//				}
//			}
//		}, SwingUtils.parseColor(colorStr, HighlighterFactory.GENERIC_GRAY), null));
	}

	private JCheckBox       _sampleLastXminutes_chk;
	private JCheckBox       _sampleAfterPrevSample_chk;
	private RSyntaxTextArea _sampleExtraWhereClause_txt;
	private JButton         _sampleExtraWhereClause_but;

	@Override
	protected JPanel createLocalOptionsPanel()
	{
		JPanel panel = SwingUtils.createPanel("Local Options", true);
		panel.setLayout(new MigLayout("ins 0, gap 0", "", "0[0]0"));
//		panel.setToolTipText(
//			"<html>" +
//				"All the options in this panel executes additional SQL lookups in the database <b>after</b> the result set has been delivered.<br>" +
//				"This means that we are doing 1 extra SQL lookup for every checkbox option per row on the result set table.<br>" +
//				"<br>" +
//				"NOTE: So if you check all the options, the time to do refresh on this tab will <b>increase</b>." +
//			"</html>");

		Configuration conf = Configuration.getCombinedConfiguration();

		_sampleLastXminutes_chk     = new JCheckBox("Show only SQL executed last 10 minutes",         conf == null ? CmExecQueryStats.DEFAULT_sample_lastXminutes    : conf.getBooleanProperty(CmExecQueryStats.PROPKEY_sample_lastXminutes,    CmExecQueryStats.DEFAULT_sample_lastXminutes));
		_sampleAfterPrevSample_chk  = new JCheckBox("Show only SQL executed since last sample time",  conf == null ? CmExecQueryStats.DEFAULT_sample_afterPrevSample : conf.getBooleanProperty(CmExecQueryStats.PROPKEY_sample_afterPrevSample, CmExecQueryStats.DEFAULT_sample_afterPrevSample));
		_sampleExtraWhereClause_txt = new RSyntaxTextArea();
		_sampleExtraWhereClause_but = new JButton("Apply Extra Where Clause");

		_sampleLastXminutes_chk    .setToolTipText(TOOLTIP_sample_lastXminutes);
		_sampleAfterPrevSample_chk .setToolTipText(TOOLTIP_sample_afterPrevSample);
		_sampleExtraWhereClause_but.setToolTipText(TOOLTIP_sample_extraWhereClause);
		_sampleExtraWhereClause_txt.setToolTipText(TOOLTIP_sample_extraWhereClause);


		// Set initial values for some fields
		String sampleExtraWhereClause = (conf == null ? CmExecQueryStats.DEFAULT_sample_extraWhereClause : conf.getProperty(CmExecQueryStats.PROPKEY_sample_extraWhereClause, CmExecQueryStats.DEFAULT_sample_extraWhereClause));

		_sampleExtraWhereClause_txt.setText(sampleExtraWhereClause);
		_sampleExtraWhereClause_txt.setHighlightCurrentLine(false);
		_sampleExtraWhereClause_txt.setSyntaxEditingStyle(AsetuneSyntaxConstants.SYNTAX_STYLE_SYBASE_TSQL);

		panel.add(_sampleLastXminutes_chk,     "wrap");
		panel.add(_sampleAfterPrevSample_chk,  "wrap");

		panel.add(_sampleExtraWhereClause_txt, "grow, push, wrap");
		panel.add(_sampleExtraWhereClause_but, "wrap");

		
		_sampleLastXminutes_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				// Need TMP since we are going to save the configuration somewhere
				Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
				if (conf == null) return;
				conf.setProperty(CmExecQueryStats.PROPKEY_sample_lastXminutes, ((JCheckBox)e.getSource()).isSelected());
				conf.save();
				
				// ReInitialize the SQL
				getCm().setSql(null);
			}
		});

		_sampleAfterPrevSample_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				// Need TMP since we are going to save the configuration somewhere
				Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
				if (conf == null) return;
				conf.setProperty(CmExecQueryStats.PROPKEY_sample_afterPrevSample, ((JCheckBox)e.getSource()).isSelected());
				conf.save();
				
				// ReInitialize the SQL
				getCm().setSql(null);
			}
		});

		_sampleExtraWhereClause_but.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				// Need TMP since we are going to save the configuration somewhere
				Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
				if (conf == null) return;
				conf.setProperty(CmExecQueryStats.PROPKEY_sample_extraWhereClause, _sampleExtraWhereClause_txt.getText().trim());
				conf.save();
				
				// ReInitialize the SQL
				getCm().setSql(null);
			}
		});

		return panel;
	}

//	@Override
//	public void checkLocalComponents()
//	{
//		CountersModel cm = getCm();
//		if (cm != null)
//		{
//			if (cm.isRuntimeInitialized())
//			{
//				// disable some options if we do not have 'sybase_ts_role'
//				if ( cm.isRoleActive(AseConnectionUtils.SYBASE_TS_ROLE) )
//				{
//					_sampleDbccSqltext_chk   .setEnabled(true);
//					_sampleDbccStacktrace_chk.setEnabled(true);
//				}
//				else
//				{
//					_sampleDbccSqltext_chk   .setEnabled(false);
//					_sampleDbccStacktrace_chk.setEnabled(false);
//
//					_sampleDbccSqltext_chk   .setSelected(false);
//					_sampleDbccStacktrace_chk.setSelected(false);
//
//					Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
//					if (conf != null)
//					{
//						conf.setProperty(CmPlanCacheDetails.PROPKEY_sample_dbccSqlText,    false);
//						conf.setProperty(CmPlanCacheDetails.PROPKEY_sample_dbccStacktrace, false);
//					}
//				}
//
//				// disable some options if we do not have ASE RELEASE 12.5.4 and 'sa_role'
//				if ( cm.getServerVersion() >= CmPlanCacheDetails.NEED_SRV_VERSION_sample_freezeMda 
//				     && cm.isRoleActive(AseConnectionUtils.SA_ROLE) )
//				{
//					_sampleFreezeMda_chk.setEnabled(true);
//				}
//				else
//				{
//					_sampleFreezeMda_chk.setEnabled(false);
//					_sampleFreezeMda_chk.setSelected(false);
//
//					Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
//					if (conf != null)
//					{
//						conf.setProperty(CmPlanCacheDetails.PROPKEY_sample_freezeMda, false);
//						cm.setSql(null);
//					}
//				}
//			} // end isRuntimeInitialized
//		} // end (cm != null)
//	}
}
