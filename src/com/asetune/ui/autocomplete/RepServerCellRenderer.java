package com.asetune.ui.autocomplete;

import java.awt.Component;

import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

import org.fife.ui.autocomplete.BasicCompletion;

public class RepServerCellRenderer
extends DefaultListCellRenderer
{
	private static final long serialVersionUID = 1L;

//	@Override
//	public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus)
//	{
//		Component component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
//
//		if (value instanceof BasicCompletion && component instanceof JLabel)
//		{
//			JLabel          label = (JLabel)component;
//			BasicCompletion bc    = (BasicCompletion)value;
//			
//			Icon icon = bc.getIcon();
//			if (icon != null)
//				label.setIcon(icon);
//		}
//		return component;
//	}
}
