/*******************************************************************************
 * Copyright (C) 2010-2019 Goran Schwarz
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

import javax.swing.JSlider;

/**
 * An extension of JSlider to select a range of values using two thumb controls.
 * The thumb controls are used to select the lower and upper value of a range
 * with predetermined minimum and maximum values.
 * 
 * <p>
 * Note that RangeSlider makes use of the default BoundedRangeModel, which
 * supports an inner range defined by a value and an extent. The upper value
 * returned by RangeSlider is simply the lower value plus the extent.
 * </p>
 * 
 * from: https://github.com/ernieyu/Swing-range-slider/blob/master/src/slider/RangeSlider.java
 */
public class RangeSlider 
extends JSlider
{
	private static final long serialVersionUID = 1L;

	/**
	 * Constructs a RangeSlider with default minimum and maximum values of 0 and
	 * 100.
	 */
	public RangeSlider()
	{
		initSlider();
	}

	/**
	 * Constructs a RangeSlider with the specified default minimum and maximum
	 * values.
	 */
	public RangeSlider(int min, int max)
	{
		super(min, max);
		initSlider();
	}

	/**
	 * Initializes the slider by setting default properties.
	 */
	private void initSlider()
	{
		setOrientation(HORIZONTAL);
	}

	/**
	 * Overrides the superclass method to install the UI delegate to draw two
	 * thumbs.
	 */
	@Override
	public void updateUI()
	{
		setUI(new RangeSliderUI(this));
		// Update UI for slider labels. This must be called after updating the
		// UI of the slider. Refer to JSlider.updateUI().
		updateLabelUIs();
	}

	/**
	 * Returns the lower value in the range.
	 */
	@Override
	public int getValue()
	{
		return super.getValue();
	}

	/**
	 * Sets the lower value in the range.
	 */
	@Override
	public void setValue(int value)
	{
		int oldValue = getValue();
		if ( oldValue == value )
		{
			return;
		}

		// Compute new value and extent to maintain upper value.
		int oldExtent = getExtent();
		int newValue  = Math.min(Math.max(getMinimum(), value), oldValue + oldExtent);
		int newExtent = oldExtent + oldValue - newValue;

		// Set new value and extent, and fire a single change event.
		getModel().setRangeProperties(newValue, newExtent, getMinimum(), getMaximum(), getValueIsAdjusting());
	}

	/**
	 * Returns the upper value in the range.
	 */
	public int getUpperValue()
	{
		return getValue() + getExtent();
	}

	/**
	 * Sets the upper value in the range.
	 */
	public void setUpperValue(int value)
	{
		// Compute new extent.
		int lowerValue = getValue();
		int newExtent  = Math.min(Math.max(0, value - lowerValue), getMaximum() - lowerValue);

		// Set extent to set upper value.
		setExtent(newExtent);
	}


	//--------------------------------------------------------------
	// Below is to be "compatible" with: com.jidesoft.swing.RangeSlider
	//--------------------------------------------------------------
    public static final String CLIENT_PROPERTY_MOUSE_POSITION = "RangeSlider.mousePosition";
    public static final String CLIENT_PROPERTY_ADJUST_ACTION = "RangeSlider.adjustAction";
    public static final String PROPERTY_LOW_VALUE = "lowValue";
    public static final String PROPERTY_HIGH_VALUE = "highValue";
    /**
     * Returns the range slider's low value.
     *
     * @return the range slider's low value.
     */
    public int getLowValue() {
        return getModel().getValue();
    }

    /**
     * Returns the range slider's high value.
     *
     * @return the range slider's high value.
     */
    public int getHighValue() {
        return getModel().getValue() + getModel().getExtent();
    }

    /**
     * Sets the range slider's low value.  This method just forwards the value to the model.
     *
     * @param lowValue the new low value
     */
    public void setLowValue(int lowValue) {
        int old = getLowValue();
        int high;
        if ((lowValue + getModel().getExtent()) > getMaximum()) {
            high = getMaximum();
        }
        else {
            high = getHighValue();
        }
        int extent = high - lowValue;

        Object property = getClientProperty(CLIENT_PROPERTY_ADJUST_ACTION);
        getModel().setRangeProperties(lowValue, extent,
                getMinimum(), getMaximum(), property == null || (!property.equals("scrollByBlock") && !property.equals("scrollByUnit")));
        firePropertyChange(PROPERTY_LOW_VALUE, old, getLowValue());

    }

    /**
     * Sets the range slider's high value.  This method just forwards the value to the model.
     *
     * @param highValue the new high value
     */
    public void setHighValue(int highValue) {
        int old = getHighValue();
        getModel().setExtent(highValue - getLowValue());
        firePropertyChange(PROPERTY_HIGH_VALUE, old, getHighValue());
    }

}
