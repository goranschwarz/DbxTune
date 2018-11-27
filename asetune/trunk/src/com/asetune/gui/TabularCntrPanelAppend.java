/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package com.asetune.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;
import javax.swing.JPanel;

import com.asetune.cm.CountersModelAppend;
import com.asetune.utils.SwingUtils;

import net.miginfocom.swing.MigLayout;


public class TabularCntrPanelAppend 
extends TabularCntrPanel
{
	private static final long serialVersionUID = 1L;
//	private static Logger _logger = Logger.getLogger(TabularCntrPanelAppend.class);

	private JCheckBox _showAllRows_chk;

	public TabularCntrPanelAppend(CountersModelAppend cm)
	{
		super(cm);
		init();
	}

    private void init()
    {
    }
    
	@Override
	protected JPanel createLocalOptionsPanel()
	{
		final CountersModelAppend cm = (CountersModelAppend) getCm();
		
		JPanel panel = SwingUtils.createPanel("Local Options", true);
		panel.setLayout(new MigLayout("ins 0, gap 0", "", "0[0]0"));

		_showAllRows_chk = new JCheckBox("Show All Rows");

		_showAllRows_chk.setToolTipText("<html>Show <b>all</b> records in the table, or just the last records fetched.</html>");
		_showAllRows_chk.setSelected( cm.showAllRecords() );
		
		_showAllRows_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				cm.setShowAllRecords( ((JCheckBox)e.getSource()).isSelected() );
				cm.fireTableDataChanged();
			}
		});
		
		// ADD to panel
		panel.add(_showAllRows_chk,   "wrap");

		return panel;
	}

//	@Override
//	public void checkLocalComponents()
//	{
//	}
	
}
