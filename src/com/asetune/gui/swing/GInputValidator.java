/*******************************************************************************
 * Copyright (C) 2010-2020 Goran Schwarz
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
package com.asetune.gui.swing;

import java.awt.Color;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.text.JTextComponent;

import org.apache.commons.lang3.math.NumberUtils;

import com.asetune.utils.StringUtil;
import com.asetune.utils.SwingUtils;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

/**
 * Validate various input fields
 * <p>
 * For example if you want to validate the input text for a JTextField, 
 * then simply create a <code>new GInputValidator(field1_txt, vg, new GInputValidator.IntegerInputValidator());</code>
 * with a proper <code>InputValidator</code> instance.
 * <p>
 * If the inputed field is not properly validated, then the Border of the TextField will be red, 
 * and the tooltip will be displayed with the error message produced when validating.
 * <p>
 * If you also use the <code>GInputValidationGroup</code> you can easily enable/disable the "OK" button, 
 * or do other stuff when all fields in the group are valid/unvalid
 * 
 *  @author Goran Schwarz
 * 
 */
public class GInputValidator
{
	public interface InputValidator
	{
		public boolean isValid(String newValueStr) throws ValidationException;
	}

	public static class ValidationException extends Exception
	{
		private static final long serialVersionUID = 1L;

		public ValidationException(String message)
		{
			super(message);
		}
	}


	private int                  _timeoutVal = 10_000;
	private JTextComponent       _txt;
	private Border               _originBorder  = null;
	private String               _originToolTip = null;
	private LineBorder           _errorBorder   = null;
	private boolean              _showBaloonToolTip = false;
	private List<InputValidator> _validators = new ArrayList<>();
	
	private GInputValidationGroup _group;

	private boolean    _isValid = true;
	
	public boolean isValid() { return _isValid; }
	
	public GInputValidator(JTextComponent txt, InputValidator validator)
	{
		this(txt, null, validator);
	}

	public GInputValidator(JTextComponent txt, GInputValidationGroup group, InputValidator validator)
	{
		_txt = txt;
		_group = group;
		_originToolTip = _txt.getToolTipText();
		_originBorder  = _txt.getBorder();
		_errorBorder   = new LineBorder(Color.red, 1);

		if (group != null)
			_group.register(this);

		_validators.add(validator);
		
		_txt.addFocusListener(new FocusListener()
		{
			@Override public void focusLost  (FocusEvent e) { fireValidation(); }
			@Override public void focusGained(FocusEvent e) {}
		});

		_txt.addKeyListener(new KeyListener()
		{
			@Override public void keyReleased(KeyEvent e) { fireValidation(); }
			@Override public void keyTyped   (KeyEvent e) {}
			@Override public void keyPressed (KeyEvent e) {}
		});
	}
	
	public GInputValidator setShowBaloonTooltip()
	{
		_showBaloonToolTip = true;
		return this;
	}
	public GInputValidator setShowBaloonTooltip(int timeoutVal)
	{
		_timeoutVal = timeoutVal;
		_showBaloonToolTip = true;
		return this;
	}
	
	private void fireValidation()
	{
		String input = _txt.getText();
		
		for (InputValidator iv : _validators)
		{
			try
			{
				_isValid = iv.isValid(input);
				_txt.setBorder(_originBorder);
				_txt.setToolTipText(_originToolTip);
			}
			catch (ValidationException ex)
			{
				_isValid = false;
				_txt.setBorder(_errorBorder);
				
				String msg = "<html>Validation error: <b>Value did not pass validation</b><br><pre>"+ex.getMessage()+"</pre></html>";
				_txt.setToolTipText(msg);
				if (_showBaloonToolTip)
				{
					SwingUtils.showTimedBalloonTip(_txt, _timeoutVal, true, msg);
				}
				else
				{
				    final ToolTipManager ttm = ToolTipManager.sharedInstance();
				    final int oldDelay = ttm.getInitialDelay();
				    ttm.setInitialDelay(0);

					MouseEvent phantom = new MouseEvent(_txt, MouseEvent.MOUSE_MOVED, System.currentTimeMillis(), 0, 10, 10, 0, false);
					ttm.mouseMoved(phantom);
					SwingUtilities.invokeLater(new Runnable()
					{
						@Override
						public void run()
						{
							// Restore the old delay time
							ttm.setInitialDelay(oldDelay);
						}
					});
				}
			}

			if (_group != null)
				_group.setState(this);
		}
	}

	/**
	 * Add another validator
	 * @param validator
	 * @return
	 */
	public GInputValidator addValidator(InputValidator validator)
	{
		_validators.add(validator);
		return this;
	}

	/**
	 * Remove any specific validator
	 * @param validator
	 * @return
	 */
	public boolean removeValidator(InputValidator validator)
	{
		return _validators.remove(validator);
	}
	
	
	/** RegExp - Input Validator */
	public static class RegExpInputValidator
	implements InputValidator
	{
		@Override
		public boolean isValid(String val) throws ValidationException
		{
			try { Pattern.compile(val); }
			catch(PatternSyntaxException ex) { throw new ValidationException("The RegExp '"+val+"' seems to be faulty. Caught: "+ex.getMessage()); }
			return true;
		}
	}

	/** URL - Input Validator */
	public static class UrlInputValidator
	implements InputValidator
	{
		@Override
		public boolean isValid(String val) throws ValidationException
		{
			try { new URL(val); }
			catch(MalformedURLException ex) { throw new ValidationException("The URL '"+val+"' seems to be malformed. Caught: "+ex.getMessage()); }
			return true;
		}
	}
	/** JSON - Input valiValidatordator */
	public static class JsonInputValidator
	implements InputValidator
	{
		@Override
		public boolean isValid(String val) throws ValidationException
		{
			try { Gson gson = new Gson(); gson.fromJson(val, Object.class); }
			catch(JsonSyntaxException ex) { throw new ValidationException("The JSON content seems to be faulty. Caught: "+ex.getMessage()); }
			return true;
		}
	}

	/** Integer - Input Validator */
	public static class IntegerInputValidator
	implements InputValidator
	{
		@Override
		public boolean isValid(String val) throws ValidationException
		{
			try { Integer.parseInt(val); }
			catch(NumberFormatException ex) { throw new ValidationException("The value '"+val+"' is not a valid Integer: "+ex.getMessage()); }
			return true;
		}
	}

	/** SimpleDateFormat - Input Validator */
	public static class SimpleDateFormatInputValidator
	implements InputValidator
	{
		@Override
		public boolean isValid(String val) throws ValidationException
		{
			try { new SimpleDateFormat(val); }
			catch(IllegalArgumentException ex) { throw new ValidationException("The value '"+val+"' is not a valid SimpleDateFormat: "+ex.getMessage()); }
			return true;
		}
	}

	/** Map with number - Input Validator */
	public static class MapNumberValidator
	implements InputValidator
	{
		@Override
		public boolean isValid(String val) throws ValidationException
		{
			Map<String, String> map = StringUtil.parseCommaStrToMap(val);
			
			for (String mapKey : map.keySet())
			{
				String mapVal = map.get(mapKey);

				// MAP-KEY: Check key (if it passes regexp check) 
				try { Pattern.compile(mapKey); }
				catch(PatternSyntaxException ex) { throw new ValidationException("The RegExp '"+mapVal+"' seems to be faulty, for key '"+mapKey+"'. Caught: "+ex.getMessage()); }

				// MAP-VAL: Check number
				try { NumberUtils.createNumber(mapVal); }
				catch (NumberFormatException ex) { throw new ValidationException("The number value '"+mapVal+"' is not a number for key '"+mapKey+"'. Caught: "+ex.getMessage()); }
			}
			
			return true;
		}
	}
}
