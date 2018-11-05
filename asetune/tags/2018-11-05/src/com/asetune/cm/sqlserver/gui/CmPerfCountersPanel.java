package com.asetune.cm.sqlserver.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;
import javax.swing.JPanel;

import com.asetune.cm.CountersModel;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.gui.swing.GTableFilter;
import com.asetune.utils.Configuration;
import com.asetune.utils.SwingUtils;

import net.miginfocom.swing.MigLayout;

public class CmPerfCountersPanel
extends TabularCntrPanel
{
//	private static final Logger  _logger	           = Logger.getLogger(CmPerfCountersPanel.class);
	private static final long    serialVersionUID      = 1L;

	private static final String  PROP_PREFIX           = CmPerfCountersPanel.class.getSimpleName();

	public static final String  PROPKEY_filter_usableCounters = PROP_PREFIX + ".filter.usableCounters";
	public static final boolean DEFAULT_filter_usableCounters = true;

	public static final String  TOOLTIP_filetr_usableCounters = 
//		"<html>Set filters to only show the following counters"
//		+ "<ul>"
//		+ "  <li> <i>cumulative</i>    - <b>PERF_COUNTER_BULK_COUNT</b>     </li>"
//		+ "  <li> <i>last observed</i> - <b>PERF_COUNTER_LARGE_RAWCOUNT</b> </li>"
//		+ "</ul>"
//		+ "The other counters would still be sampled and is available when you clear this filter."
//		+ "</html>";
		"<html>Set filters to hide PERF_LARGE_RAW_BASE</html>";

	public CmPerfCountersPanel(CountersModel cm)
	{
		super(cm);

		init();
	}
	
	private void init()
	{
		// https://blogs.msdn.microsoft.com/psssql/2013/09/23/interpreting-the-counter-values-from-sys-dm_os_performance_counters/
		// colors for: PERF_LARGE_RAW_BASE          --
		// colors for: PERF_LARGE_RAW_FRACTION      -- 
		// colors for: PERF_AVERAGE_BULK            -- 
		// colors for: PERF_COUNTER_BULK_COUNT      -- cumulative
		// colors for: PERF_COUNTER_LARGE_RAWCOUNT  -- last observed
		
//		Configuration conf = Configuration.getCombinedConfiguration();
//		String colorStr = null;
//
//		// YELLOW = SYSTEM process
//		if (conf != null) colorStr = conf.getProperty(getName()+".color.system");
//		addHighlighter( new ColorHighlighter(new HighlightPredicate()
//		{
//			@Override
//			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
//			{
//				String sid = (String) adapter.getValue(adapter.getColumnIndex("sid"));
//				if ("0x0100000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000".equals(sid))
//					return true;
//				return false;
//			}
//		}, SwingUtils.parseColor(colorStr, Color.YELLOW), null));
//
//		// GREEN = RUNNING or RUNNABLE process
//		if (conf != null) colorStr = conf.getProperty(getName()+".color.running");
//		addHighlighter( new ColorHighlighter(new HighlightPredicate()
//		{
//			@Override
//			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
//			{
//				String status = (String) adapter.getValue(adapter.getColumnIndex("status"));
//				if ( status != null && (status.startsWith("running") || status.startsWith("runnable")) )
//					return true;
//				return false;
//			}
//		}, SwingUtils.parseColor(colorStr, Color.GREEN), null));
	}


	@Override
	protected JPanel createLocalOptionsPanel()
	{
//		final String filterStr = "WHERE cntr_type_name IN ('PERF_COUNTER_BULK_COUNT', 'PERF_COUNTER_LARGE_RAWCOUNT')";
		final String filterStr = "WHERE cntr_type_name NOT IN ('PERF_LARGE_RAW_BASE')";
		
		JPanel panel = SwingUtils.createPanel("Local Options", true);
		panel.setLayout(new MigLayout("ins 0, gap 0", "", "0[0]0"));

		Configuration conf = Configuration.getCombinedConfiguration();
		JCheckBox filterUsableCounters = new JCheckBox("Show only counters of type 'xxx'", conf == null ? DEFAULT_filter_usableCounters : conf.getBooleanProperty(PROPKEY_filter_usableCounters, DEFAULT_filter_usableCounters));

		filterUsableCounters.setName(PROPKEY_filter_usableCounters);
		filterUsableCounters.setToolTipText(TOOLTIP_filetr_usableCounters);
		panel.add(filterUsableCounters, "wrap");

		if (filterUsableCounters.isSelected())
		{
			GTableFilter freeTextFilter = getFilterFreeText();
			if (freeTextFilter != null)
				freeTextFilter.setText(filterStr);
		}

		filterUsableCounters.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				// Need TMP since we are going to save the configuration somewhere
				Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
				if (conf == null) return;
				boolean val = ((JCheckBox)e.getSource()).isSelected();
				conf.setProperty(PROPKEY_filter_usableCounters, val);
				conf.save();

				GTableFilter freeTextFilter = getFilterFreeText();
				if (freeTextFilter != null)
				{
					if (val)
						freeTextFilter.setText(filterStr);
					else
						freeTextFilter.setText("");
						
					freeTextFilter.applyFilter();
				}
			}
		});
		
		return panel;
	}
}
