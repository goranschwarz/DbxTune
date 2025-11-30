/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package com.dbxtune.gui.swing;

public class GTabbedPaneWindowProps 
{
    private static final long serialVersionUID = 1L;
	public boolean undocked  = false;
	public int     width     = -1;
	public int     height    = -1;
	public int     posX      = -1;
	public int     posY      = -1;
	
	public GTabbedPaneWindowProps()
	{
	}
	public GTabbedPaneWindowProps(boolean undocked)
	{
		this.undocked = undocked;
		this.width    = -1;
		this.height   = -1;
		this.posX     = -1;
		this.posY     = -1;
	}
	public GTabbedPaneWindowProps(boolean undocked, int with, int height, int posX, int posY)
	{
		this.undocked = undocked;
		this.width    = with;
		this.height   = height;
		this.posX     = posX;
		this.posY     = posY;
	}
	
	@Override
	public String toString()
	{
		return "undocked="+undocked+", width="+width+", height="+height+", posX="+posX+", posY="+posY+".";
	}
}
