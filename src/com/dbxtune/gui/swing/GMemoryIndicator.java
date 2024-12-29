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
 * the Free Software Foundation, version 3 of the License.
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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.font.FontRenderContext;
import java.awt.font.LineMetrics;
import java.awt.geom.Rectangle2D;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.Locale;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;

import com.dbxtune.utils.SwingUtils;

/**
 * A status bar component displaying the current JVM heap.
 */
public class GMemoryIndicator extends JPanel
{
	private static final long serialVersionUID = 1L;

	private HeapIcon   _heapIcon;
	private Timer      _timer;
	private TimerEvent _timerEvent;
	private long       _usedMem;
	private long       _totalMem;
	private long       _maxTotalMem;
	private long       _freeMem1;
	private long       _freeMem2;

	private boolean    _useSystemColors;
	private Color      _iconForeground;
	private Color      _iconBorderColor;

	/**
	 * Constructor.
	 *
	 * @param app
	 *            The GUI application.
	 */
	public GMemoryIndicator()
	{
		this(5000);
	}
	public GMemoryIndicator(int refreshIntervallInMs)
	{
		_heapIcon = new HeapIcon(this);
		setLayout(new BorderLayout());
		setBorder(BorderFactory.createEmptyBorder(2, 2, 4, 2));
		add(new JLabel(_heapIcon));

		setUseSystemColors(true);
		setIconForeground(Color.BLUE);
		setIconBorderColor(Color.BLACK);

		setVisible(true); // kicks off the timer

		refreshMemoryData();
		setRefreshInterval(refreshIntervallInMs); // Must be called!

		ToolTipManager.sharedInstance().registerComponent(this);
	}

	protected static final long bytesToKb(long bytes)
	{
		return bytes / 1024L;
	}
	protected static final long bytesToMb(long bytes)
	{
		return bytes / 1024L / 1024L;
	}

	/**
	 * Updates heap memory information.
	 */
	protected void refreshMemoryData()
	{
		MemoryMXBean memBean = ManagementFactory.getMemoryMXBean() ;
		MemoryUsage heap     = memBean.getHeapMemoryUsage();

//		System.out.println();
//		System.out.println("###################################################################");
//		System.out.println("heap   .init     = "+heap.getInit()         / 1024 / 1024); // not really interested
//		System.out.println("heap   .used     = "+heap.getUsed()         / 1024 / 1024); // how much memory is USED
//		System.out.println("heap   .commited = "+heap.getCommitted()    / 1024 / 1024); // how much has the JVM used/allocated from the MAX memory (same as Runtime.getRuntime().totalMemory())
//		System.out.println("heap   .max      = "+heap.getMax()          / 1024 / 1024); // more or less "-Xmx####m", the max maximum memory that the JVM can allocate 

		_maxTotalMem = heap.getMax();       // Max memory the JVM can alloc from the OS
		_totalMem    = heap.getCommitted(); // Currently memory grabbed/allocated from OS
		_usedMem     = heap.getUsed();      // Memory currently used by objects inside the JVM 
		_freeMem1    = _totalMem    - _usedMem;
		_freeMem2    = _maxTotalMem - _usedMem;
//		_freeMem     = _totalMem    - _usedMem;

//		_totalMem = Runtime.getRuntime().totalMemory();
//		_freeMem  = Runtime.getRuntime().freeMemory();
//		_usedMem = _totalMem - _freeMem;

//		System.out.println("---------------------------------------------");
//		System.out.println("    _totalMem    = "+_totalMem / 1024 / 1024);
//		System.out.println("    _freeMem     = "+_freeMem  / 1024 / 1024);
//		System.out.println("    _usedMem     = "+_usedMem  / 1024 / 1024);
	}

	public Color getIconBorderColor()
	{
		Color c = _iconBorderColor;

		if ( getUseSystemColors() )
		{
			c = UIManager.getColor("Label.foreground");
		}

		return c;
	}

	public Color getIconForeground()
	{
		Color c = _iconForeground;

		if ( getUseSystemColors() )
		{
			c = UIManager.getColor("ProgressBar.foreground");
		}

		return c;

	}

	/**
	 * Returns the refresh interval of the heap indicator.
	 *
	 * @return The refresh interval, in milliseconds.
	 * @see #setRefreshInterval
	 */
	public int getRefreshInterval()
	{
		return _timer == null ? -1 : _timer.getDelay();
	}

	/**
	 * Returns the text to display for the tooltip.
	 *
	 * @return The tooltip text.
	 */
	@Override
	public String getToolTipText()
	{
		long usedMb        = bytesToMb(getUsedMemory());
		long totalMb       = bytesToMb(getTotalMemory());
		long maxMb         = bytesToMb(getMaxTotalMemory());
		long freeToTotalMb = bytesToMb(getFreeMemory1());
		long freeToMaxMb   = bytesToMb(getFreeMemory2());

//		return "Memory: Used "+usedMb+" MB, Free "+freeMb+" MB, Total Allocated "+totalMb+" MB.";
		return "<html> "
				+ "JVM Heap Memory Usage"
				+ "<table>"
				+ "<tr> <td><b>Used           </b></td> <td>" + usedMb         + " MB</td> <td>How many MB is in-use by objects</td> </tr>"
				+ "<tr> <td><b>Free (to total)</b></td> <td>" + freeToTotalMb  + " MB</td> <td>How many MB we have left until we reach <code>Total Allocated</code></td> </tr>"
				+ "<tr> <td><b>Free (to max)  </b></td> <td>" + freeToMaxMb    + " MB</td> <td>How many MB we have left until we reach <code>Max Allocation</code></td> </tr>"
				+ "<tr> <td><b>Total Allocated</b></td> <td>" + totalMb        + " MB</td> <td>How many MB the JVM currently has grabbed from the OS</td> </tr>"
				+ "<tr> <td><b>Max Allocation </b></td> <td>" + maxMb          + " MB</td> <td>Max MB the JVM can grab from the OS</td> </tr>"
				+ "</table> "
				+ "</html>";
	}

	/**
	 * Returns the total amount of memory the JVM can allocate at most.
	 *
	 * @return The total memory available to the JVM, in bytes.
	 * @see #getUsedMemory
	 */
	public long getMaxTotalMemory()
	{
		return _maxTotalMem;
	}

	/**
	 * Returns the total amount of memory available to the JVM.
	 *
	 * @return The total memory available to the JVM, in bytes.
	 * @see #getUsedMemory
	 */
	public long getTotalMemory()
	{
		return _totalMem;
	}

	/**
	 * Returns the amount of memory currently being used by the JVM.
	 *
	 * @return The memory being used by the JVM, in bytes.
	 * @see #getTotalMemory
	 */
	public long getUsedMemory()
	{
		return _usedMem;
	}

	/**
	 * Returns the amount of memory currently being free for usage by the JVM.
	 *
	 * @return The memory being free by the JVM, in bytes.
	 * @see #getTotalMemory
	 */
	public long getFreeMemory1()
	{
		return _freeMem1;
	}

	/**
	 * Returns the amount of memory currently being free for usage by the JVM.
	 *
	 * @return The memory being free by the JVM, in bytes.
	 * @see #getTotalMemory
	 */
	public long getFreeMemory2()
	{
		return _freeMem2;
	}

	/**
	 * Returns whether or not system colors are used when painting the heap
	 * indicator.
	 *
	 * @return Whether or not to use system colors.
	 * @see #setUseSystemColors
	 */
	public boolean getUseSystemColors()
	{
		return _useSystemColors;
	}


	protected void installTimer(int interval)
	{
		if ( _timer == null )
		{
			_timerEvent = new TimerEvent();
			_timer = new Timer(interval, _timerEvent);
		}
		else
		{
			_timer.stop();
			_timer.setDelay(interval);
		}
		_timer.start();
	}

	/**
	 * Show a popup with memory information
	 */
	@Override
	protected void processMouseEvent(MouseEvent e)
	{
		switch (e.getID())
		{
		case MouseEvent.MOUSE_CLICKED:
			if ( e.getClickCount() == 2 )
			{
				long oldMem = getUsedMemory();
				Runtime.getRuntime().gc();
				refreshMemoryData();
				long newMem = getUsedMemory();
				long difference = oldMem - newMem;
				String text = "Garbage collection freed: "+bytesToMb(difference)+" MB.";
				JOptionPane.showMessageDialog(this, text, "JVM Heap Information", JOptionPane.INFORMATION_MESSAGE);
			}
			break;
		default:
		}
		super.processMouseEvent(e);
	}

	public void setIconBorderColor(Color iconBorderColor)
	{
		_iconBorderColor = iconBorderColor;
		repaint();
	}

	public void setIconForeground(Color iconForeground)
	{
		_iconForeground = iconForeground;
		repaint();
	}

	/**
	 * Sets the refresh interval for the heap indicator.
	 *
	 * @param interval
	 *            The new refresh interval, in milliseconds.
	 * @see #getRefreshInterval
	 */
	public void setRefreshInterval(int interval)
	{
		if ( interval <= 0 || interval == getRefreshInterval() )
			return;
		installTimer(interval);
	}

	/**
	 * Sets whether or not to use system colors when painting the heap
	 * indicator.
	 *
	 * @param useSystemColors
	 *            Whether or not to use system colors.
	 * @see #getUseSystemColors
	 */
	public void setUseSystemColors(boolean useSystemColors)
	{
		if ( useSystemColors != getUseSystemColors() )
		{
			_useSystemColors = useSystemColors;
			repaint();
		}
	}

	@Override
	public void setVisible(boolean visible)
	{
		if ( visible )
			installTimer(getRefreshInterval());
		else
			uninstallTimer();
		super.setVisible(visible);
	}

	/**
	 * Called just before this <code>Plugin</code> is removed from an
	 * <code>GUIApplication</code>. This gives the plugin a chance to clean up
	 * any loose ends (kill any threads, close any files, remove listeners,
	 * etc.).
	 *
	 * @return Whether the uninstall went cleanly.
	 * @see #install
	 */
	public boolean uninstall()
	{
		uninstallTimer();
		return true;
	}

	protected void uninstallTimer()
	{
		if ( _timer != null )
		{
			_timer.stop();
			_timer.removeActionListener(_timerEvent);
			_timerEvent = null; // May help GC.
			_timer = null; // May help GC.
		}
	}

	/**
	 * Timer event that gets fired. This refreshes the GC icon.
	 */
	private class TimerEvent implements ActionListener
	{
		@Override
		public void actionPerformed(ActionEvent e)
		{
			refreshMemoryData();
			repaint();
		}
	}

	//------------------------------------------------------------------------------------
	//------------------------------------------------------------------------------------
	// Private subclasses
	//------------------------------------------------------------------------------------
	//------------------------------------------------------------------------------------
	private static class HeapIcon implements Icon
	{
		private GMemoryIndicator _memoryIndicator;

		private LineMetrics _lineMetrics;
		private Font _labelFont;
		
		public HeapIcon(GMemoryIndicator parent)
		{
			_memoryIndicator = parent;

//			_labelFont = UIManager.getFont("Label.font");
			_labelFont = new Font("Dialog", Font.BOLD, SwingUtils.hiDpiScale(10));
			FontRenderContext frc = new FontRenderContext(null, false, false);
			_lineMetrics = _labelFont.getLineMetrics("Free: XXX MB", frc);
		}

		@Override
		public int getIconHeight()
		{
			int height = _memoryIndicator.getHeight() - SwingUtils.hiDpiScale(8);
			if (height < SwingUtils.hiDpiScale(12))
				height = SwingUtils.hiDpiScale(12);
			return height;
//			return 12;
		}

		@Override
		public int getIconWidth()
		{
			return SwingUtils.hiDpiScale(45);
		}

		@Override
		public void paintIcon(Component c, Graphics g, int x, int y)
		{
			int width = getIconWidth() - 1;
			int height = getIconHeight() - 1;
			g.setColor(_memoryIndicator.getIconBorderColor());
			g.drawLine(x, y + 1, x, y + height - 1);
			g.drawLine(x + width, y + 1, x + width, y + height - 1);
			g.drawLine(x + 1, y, x + width - 1, y);
			g.drawLine(x + 1, y + height, x + width - 1, y + height);
			g.setColor(_memoryIndicator.getIconForeground());
			long usedMem = _memoryIndicator.getUsedMemory();
//			long totalMem = _memoryIndicator.getTotalMemory();    // how much we currently have allocated
			long totalMem = _memoryIndicator.getMaxTotalMemory(); // how much we CAN allocate from the OS
			int x2 = (int) (width * ((float) usedMem / (float) totalMem));
			x++;
			// Not sure why panel's orientation doesn't change, do JPanels not
			// set orientations??
			// if (c.getComponentOrientation().isLeftToRight()) {
			if ( ComponentOrientation.getOrientation(Locale.getDefault()).isLeftToRight() )
			{
				g.fillRect(x, y + 1, x2 - x, height - 1);
			}
			else
			{
				g.fillRect((x + width) - x2, y + 1, x2 - x, height - 1);
			}

			// Print free MB
			// REALLY, The code below, I have no clue what the math does...
//			String str = "Free: "+bytesToMb(_memoryIndicator.getFreeMemory())+" MB";
//			String str = bytesToMb(_memoryIndicator.getFreeMemory())+" MB";
			String str = bytesToMb(_memoryIndicator.getUsedMemory())+" MB";
			FontRenderContext frc = new FontRenderContext(null, false, false);
			Rectangle2D bounds = g.getFont().getStringBounds(str, frc);
			Graphics g2 = g.create();

//			float fraction = ((float) _memoryIndicator.getUsedMemory()) / _memoryIndicator.getTotalMemory();

			Insets insets = new Insets(0, 0, 0, 0);
			g2.setClip(
				insets.left,
				insets.top,
				width,
				height);
//				(int) (width * fraction),
//				height);

			g2.setColor(Color.black);

			int sx = insets.left + (int) (width - bounds.getWidth()) / 2;
			int sy = (int) (height + insets.top + _lineMetrics.getAscent()) / 2;

			if (sx < 3)
				sx = 3; // make sure the left side is readable, the right side MB isn't that important 
			g2.drawString(str, sx, sy);
			g2.dispose();

//			g2 = g.create();
//			g2.setClip(
//				insets.left + (int) (width * fraction),
//				insets.top,
//				width - insets.left - (int) (width * fraction),
//				height);
//
//			g2.setColor(_memoryIndicator.getForeground());
//			g2.drawString(str, sx, sy);
//			g2.dispose();
		}
	}
}
