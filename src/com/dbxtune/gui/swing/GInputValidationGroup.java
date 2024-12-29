/*******************************************************************************
 * Copyright (C) 2010-2025 Goran Schwarz
 * 
 * This file is part of DbxTune
 * DbxTune is a family of sub-products *Tune, hence the Dbx
 * Here are some of the tools: AseTune, IqTune, RsTune, RaxTune, HanaTune, 
 *          SqlServerTune, PostgresTune, MySqlTune, MariaDbTune, Db2Tune, ...
 * 
 * DbxTune is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * DbxTune is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with DbxTune.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.dbxtune.gui.swing;

import java.util.HashMap;
import java.util.Map;

import javax.swing.AbstractButton;

/**
 * Used in conjunction with GInputValidator
 * <p>
 * 
 * <b>Example 1: to Enable/disable button "OK" if any fields in the validation group do not pass</b>
 * <pre>
 * JTextField field1_txt = new JTextField("");
 * JTextField field2_txt = new JTextField("");
 *
 * JButton               ok = new JButton("OK");
 * GInputValidationGroup vg = new GInputValidationGroup(ok); // The button to enable/disable
 * 
 * new GInputValidator(field1_txt, vg, new GInputValidator.IntegerInputValidator());
 * new GInputValidator(field2_txt, vg, new GInputValidator.SimpleDateFormatInputValidator());
 * </pre>
 * 
 * <b>Example 2: Another method to do the same thing</b>
 * <pre>
 * JTextField field1_txt = new JTextField("");
 * JTextField field2_txt = new JTextField("");
 *
 * JButton               ok = new JButton("OK");
 * GInputValidationGroup vg = new GInputValidationGroup(this); // The class implements: GroupStateNotifications
 * 
 * new GInputValidator(field1_txt, vg, new GInputValidator.IntegerInputValidator());
 * new GInputValidator(field2_txt, vg, new GInputValidator.SimpleDateFormatInputValidator());
 * 
 * public void groupStateChanged(boolean groupIsValid)
 * {
 *     ok.setEnabled(groupIsValid)
 * }
 * </pre>
 * 
 * <b>Example 2: simple subclass </b>
 * <pre>
 * JTextField field1_txt = new JTextField("");
 * JTextField field2_txt = new JTextField("");
 *
 * JButton               ok = new JButton("OK");
 * GInputValidationGroup vg = new GInputValidationGroup() // call method groupStateChanged() in the subclass of GInputValidationGroup
 * {
 *     public void groupStateChanged(boolean groupIsValid)
 *     {
 *         ok.setEnabled(groupIsValid)
 *     }
 * };
 * 
 * new GInputValidator(field1_txt, vg, new GInputValidator.IntegerInputValidator());
 * new GInputValidator(field2_txt, vg, new GInputValidator.SimpleDateFormatInputValidator());
 * </pre>
 * @author gorans
 *
 */
public class GInputValidationGroup
{
	public interface GroupStateNotifications
	{
		public void groupStateChanged(boolean groupIsValid);
	}
	
	public GInputValidationGroup()
	{
	}

	public GInputValidationGroup(GroupStateNotifications groupState)
	{
		_groupState = groupState;
	}

	public GInputValidationGroup(AbstractButton button)
	{
		_button = button;
	}

	private GroupStateNotifications _groupState;
	private AbstractButton _button;
	
	private Map<GInputValidator, Boolean> _map = new HashMap<>();
	private boolean _lastGroupIsValid = true;

	public void register(GInputValidator validator)
	{
		_map.put(validator, true);
	}
	public void unRegister(GInputValidator validator)
	{
		_map.remove(validator);
	}
	
	public void setState(GInputValidator validator)
	{
		_map.put(validator, validator.isValid());
		
		fireChanges();
	}

	private void fireChanges()
	{
		boolean groupIsValid = true;
		
		for (Boolean isValid : _map.values())
		{
			if ( ! isValid )
			{
				groupIsValid = false;
				break;
			}
		}

		if (_lastGroupIsValid != groupIsValid)
		{
			// Call the interface if we have one active
			if (_groupState != null)
				_groupState.groupStateChanged(groupIsValid);
			
			// Call any subclasses
			groupStateChanged(groupIsValid);
			
			_lastGroupIsValid = groupIsValid;
		}
	}

	/**
	 * Override this method to get groupStateChanged notification, or instantiate with GInputValidationGroup(GroupState groupState)
	 * @param groupIsValid
	 */
	public void groupStateChanged(boolean groupIsValid)
	{
		if (_button != null)
			_button.setEnabled(groupIsValid);
	}
	
}
