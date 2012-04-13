package com.asetune.cm.ase.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JTable;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.HighlightPredicate;

import com.asetune.Version;
import com.asetune.cm.CountersModel;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.pcs.PersistentCounterHandler;
import com.asetune.utils.Configuration;
import com.asetune.utils.SwingUtils;

public class CmObjectActivityPanel
extends TabularCntrPanel
{
//	private static final Logger  _logger	           = Logger.getLogger(CmObjectActivityPanel.class);
	private static final long    serialVersionUID      = 1L;

//	private static final String  PROP_PREFIX           = CmObjectActivity.CM_NAME;

	public CmObjectActivityPanel(CountersModel cm)
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

		if (conf != null) colorStr = conf.getProperty(getName()+".color.index");
		addHighlighter( new ColorHighlighter(new HighlightPredicate()
		{
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
				Number indexId = (Number) adapter.getValue(adapter.getColumnIndex("IndexID"));
				if ( indexId != null && indexId.intValue() > 0)
					return true;
				return false;
			}
		}, SwingUtils.parseColor(colorStr, Color.ORANGE), null));
	}

	@Override
	protected JPanel createLocalOptionsPanel()
	{
		JPanel panel = SwingUtils.createPanel("Local Options", true);
		panel.setLayout(new MigLayout("ins 0, gap 0", "", "0[0]0"));

		Configuration conf = Configuration.getCombinedConfiguration();
		boolean defaultOpt = conf == null ? true : conf.getBooleanProperty(getName()+".TabRowCount", true);
		JCheckBox sampleRowCount_chk = new JCheckBox("Sample Table Row Count", defaultOpt);

		sampleRowCount_chk.setName(getName()+".TabRowCount");
		sampleRowCount_chk.setToolTipText("<html>" +
				"Sample Table Row Count using ASE functions <code>row_count()</code> and <code>data_pages()</code>.<br>" +
				"<b>Note 1</b>: Only in ASE 15.0.2 or higher.<br>" +
				"<b>Note 2</b>: You can also set the property 'CmObjectActivity.TabRowCount=true|false' in the configuration file.<br>" +
				"<b>Note 3</b>: To check if this is enabled or not, use the Properties dialog in this tab pane, right click + properties...<br>" +
				"</html>");
		panel.add(sampleRowCount_chk, "wrap");

		sampleRowCount_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				// Need TMP since we are going to save the configuration somewhere
				Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
				if (conf == null) return;
				conf.setProperty(getName()+".TabRowCount", ((JCheckBox)e.getSource()).isSelected());
				conf.save();
				
				// This will force the CM to re-initialize the SQL statement.
				CountersModel cm = getCm().getCounterController().getCmByName(getName());
				if (cm != null)
					cm.setSql(null);
			}
		});
		
		return panel;
	}

	@Override
	public boolean ddlRequestInfo()
	{
		return true;
	}
	@Override
	public void ddlRequestInfoSave(JTable table)
	{
		if (table == null)
			return;

		if ( ! PersistentCounterHandler.hasInstance() )
			return;

		PersistentCounterHandler pch = PersistentCounterHandler.getInstance();

		int DBName_pos     = -1;
		int ObjectName_pos = -1;
//		int IndexID_pos    = -1;
		for (int c=0; c<table.getColumnCount(); c++)
		{
			if ( "DBName".equals(table.getColumnName(c)) )
				DBName_pos = c;

			if ( "ObjectName".equals(table.getColumnName(c)) )
				ObjectName_pos = c;

//			if ( "IndexID".equals(table.getColumnName(c)) )
//				IndexID_pos = c;

//			if (DBName_pos >= 0 && ObjectName_pos >= 0 && IndexID_pos >= 0)
//				break;
			if (DBName_pos >= 0 && ObjectName_pos >= 0)
				break;
		}

		// HOW MANY TOP ROWS SHOULD WE GRAB FROM THE JTABLE
		int NUM_OF_DDLS_TO_PERSIST = 10;
		
		int rows = Math.min(NUM_OF_DDLS_TO_PERSIST, table.getRowCount());
		for (int r=0; r<rows; r++)
		{
			Object DBName_obj     = table.getValueAt(r, DBName_pos);
			Object ObjectName_obj = table.getValueAt(r, ObjectName_pos);
//			Object IndexID_obj    = table.getValueAt(r, IndexID_pos);

			// Skip index rows... (change the loop to do this)
//			if (IndexID_obj instanceof Number)
//			{
//				if ( ((Number)IndexID_obj).intValue() > 0 )
//					continue;
//			}
			if (DBName_obj instanceof String && ObjectName_obj instanceof String)
				pch.addDdl((String)DBName_obj, (String)ObjectName_obj, getName()+".guiSorted, row="+r);
		}
		
	}
}
