package com.asetune.cm.rs.gui;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.renderer.DefaultTableRenderer;
import org.jdesktop.swingx.renderer.LabelProvider;
import org.jdesktop.swingx.renderer.StringValue;
import org.jdesktop.swingx.renderer.StringValues;

import com.asetune.Version;
import com.asetune.cm.CountersModel;
import com.asetune.cm.rs.CmAdminStats;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.gui.swing.GTable;
import com.asetune.utils.Configuration;
import com.asetune.utils.SwingUtils;

public class CmAdminStatsPanel
extends TabularCntrPanel
{
//	private static final Logger  _logger	           = Logger.getLogger(CmAdminStatsPanel.class);
	private static final long    serialVersionUID      = 1L;

	JCheckBox _sample_resetAfter_chk;

	public static final String  TOOLTIP_sample_resetAfter = 
		"<html>Clear counters after we have sampled data from RepServer.<br>" +
		   "<b>Executes</b>: admin statistics, 'RESET'" +
		"</html>";

	public CmAdminStatsPanel(CountersModel cm)
	{
		super(cm);

		if (cm.getIconFile() != null)
			setIcon( SwingUtils.readImageIcon(Version.class, cm.getIconFile()) );

		init();
	}
	
	private void init()
	{
	}

//	private boolean _initialized = false;
//	private final static NoDataRenderer NODATA_RENDERER = new NoDataRenderer();
//	@Override
//	public void reset()
//	{
//		super.reset();
//		_initialized = false;
//	}
//	@Override
//	protected void updateExtendedInfoPanel()
//	{
//		GTable table = getDataTable();
//
////		if (table.getColumnCount() > 1 && !_initialized)
//		if (table.getColumnCount() > 1)
//		{
//System.out.println("###### CmAdminStatsPanel: SETTING: setCellRenderer()");
//    		table.getColumn(7  -1).setCellRenderer(NODATA_RENDERER);
//    		table.getColumn(8  -1).setCellRenderer(NODATA_RENDERER);
//    		table.getColumn(9  -1).setCellRenderer(NODATA_RENDERER);
//    		table.getColumn(10 -1).setCellRenderer(NODATA_RENDERER);
//    		table.getColumn(11 -1).setCellRenderer(NODATA_RENDERER);
//    		_initialized = true;
//		}
//
////		_xrstm.addStrColumn ("Instance",       1,  false, 255, "FIXME: description");
////		_xrstm.addIntColumn ("InstanceId",     2,  false,      "FIXME: description");
////		_xrstm.addIntColumn ("ModTypeInstVal", 3,  false,      "FIXME: description");
////		_xrstm.addStrColumn ("Type",           4,  false,  10, "FIXME: description");
////		_xrstm.addStrColumn ("Name",           5,  false,  31, "FIXME: description");
////		_xrstm.addLongColumn("Obs",            6,  false,      "FIXME: description");
////		_xrstm.addLongColumn("Total",          7,  false,      "FIXME: description");
////		_xrstm.addLongColumn("Last",           8,  false,      "FIXME: description");
////		_xrstm.addLongColumn("Max",            9,  false,      "FIXME: description");
////		_xrstm.addLongColumn("AvgTtlObs",      10, false,      "FIXME: description");
////		_xrstm.addLongColumn("RateXsec",       11, false,      "FIXME: description");
//	}

	@Override
	protected JPanel createLocalOptionsPanel()
	{
		JPanel panel = SwingUtils.createPanel("Local Options", true);
		panel.setLayout(new MigLayout("ins 0, gap 0", "", "0[0]0"));

		Configuration conf = Configuration.getCombinedConfiguration();
		_sample_resetAfter_chk     = new JCheckBox("Clear Counters",  conf == null ? CmAdminStats.DEFAULT_sample_resetAfter  : conf.getBooleanProperty(CmAdminStats.PROPKEY_sample_resetAfter,  CmAdminStats.DEFAULT_sample_resetAfter));

		_sample_resetAfter_chk.setName(CmAdminStats.PROPKEY_sample_resetAfter);
		_sample_resetAfter_chk.setToolTipText(TOOLTIP_sample_resetAfter);
		panel.add(_sample_resetAfter_chk, "wrap");

		_sample_resetAfter_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				// Need TMP since we are going to save the configuration somewhere
				Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
				if (conf == null) return;
				conf.setProperty(CmAdminStats.PROPKEY_sample_resetAfter, ((JCheckBox)e.getSource()).isSelected());
				conf.save();
				
				// ReInitialize the SQL
				getCm().setSql(null);
			}
		});
		
		return panel;
	}

//	private static class NoDataRenderer 
//	implements TableCellRenderer
//	{
//
//		private TableCellRenderer _goodValue;
//		private TableCellRenderer _nullValue;
//		private StringValue       _nullStr = new StringValue()
//		{
//			private static final long serialVersionUID = 1L;
//
//			@Override
//			public String getString(Object value)
//			{
//				return "(NULL)";
//			}
//		};
//
//		public NoDataRenderer()
//		{
////			_goodValue = new DefaultTableRenderer(new NumCheckBoxProvider(StringValues.EMPTY, SwingConstants.LEFT));
//			_goodValue = new DefaultTableRenderer();
//			_nullValue = new DefaultTableRenderer(new LabelProvider(_nullStr));
//		}
//
//		@Override
//		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
//		{
////			if (value instanceof Number)
////			{
////				if ( ((Number)value).intValue() == -999 )
////					return _nullValue.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
////			}
//			if ( value == null )
//				return _nullValue.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
//
//			return _goodValue.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
//		}
//	}
}
