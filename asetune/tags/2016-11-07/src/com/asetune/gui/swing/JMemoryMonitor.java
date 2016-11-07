package com.asetune.gui.swing;

import java.awt.Color;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.font.FontRenderContext;
import java.awt.font.LineMetrics;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Locale;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.Timer;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;

import org.apache.log4j.Logger;

/**
 * Simple memory monitor component lifted from JEdit with minor modifications.
 */
public class JMemoryMonitor extends JComponent
{
	private static final long serialVersionUID = 1L;

	private static final Logger logger_ = 
        Logger.getLogger(JMemoryMonitor.class);

    //--------------------------------------------------------------------------
    // Constants
    //--------------------------------------------------------------------------
    
    /**
     * Test string for determining dimensions appropriate for max display.
     */
    private static final String TEST_STRING = "999/999Mb";
    
    //--------------------------------------------------------------------------
    // Fields
    //--------------------------------------------------------------------------
    
    /**
     * Metrics.
     */
    private LineMetrics _lineMetrics;
    
    /**
     * Background gradient start color.
     */
    private Color _progressForeground;
    
    /**
     * Background gradient end color.
     */
    private Color _progressBackground;
    
    /**
     * Timer that refreshes the monitor even x number of seconds.
     */
    private Timer _timer;
    
    /**
     * Custom font for the label.
     */
    private Font _labelFont;

    //--------------------------------------------------------------------------
    // Constructors
    //--------------------------------------------------------------------------
        
    /**
     * Creates a JMemoryMonitor.
     */
    public JMemoryMonitor()
    {
        // Reset the colors/fonts/etc whenever the look and feel changes
        UIManager.addPropertyChangeListener(new PropertyChangeListener()
        {
            @Override
			public void propertyChange(PropertyChangeEvent evt)
            {
                setDefaults();
            }
        });
        
        setDefaults();
    }

    //--------------------------------------------------------------------------
    // Protected 
    //--------------------------------------------------------------------------
    
    /**
     * Sets all the configuration defaults.
     */
    protected void setDefaults()
    {
        _labelFont = UIManager.getFont("Label.font");
        
//        // GTK+ LAF returns null for this
//        if (_labelFont == null)
//            _labelFont = FontUtil.getPreferredSerifFont();
        
        logger_.debug("Label font: " + _labelFont);
        
        setDoubleBuffered(true);
        setForeground(UIManager.getColor("Label.foreground"));
        setBackground(UIManager.getColor("Label.background"));
        setFont(_labelFont);
        
        FontRenderContext frc = new FontRenderContext(null, false, false);
        _lineMetrics = _labelFont.getLineMetrics(TEST_STRING, frc);

        _progressBackground = UIManager.getColor("ScrollBar.thumb");
        _progressForeground = UIManager.getColor("ScrollBar.thumbHighlight");

        // =====================================================================
        // WORKAROUND: GTK LAF does not have these props so just use labels 
        if (_progressBackground == null)
            _progressBackground = getBackground();
        if (_progressForeground == null)
            _progressForeground = getForeground();
        // =====================================================================
        
        setPreferredSize(new Dimension(75, 15));
    }

    //--------------------------------------------------------------------------
    // Overrides javax.swing.JComponent
    //--------------------------------------------------------------------------

    /**
     * @see java.awt.Component#addNotify()
     */
    @Override
	public void addNotify()
    {
        super.addNotify();
        _timer = new Timer(2000, new RefreshAction());
        _timer.start();
        ToolTipManager.sharedInstance().registerComponent(this);
    }

    
    /**
     * @see java.awt.Component#removeNotify()
     */
    @Override
	public void removeNotify()
    {
        _timer.stop();
        ToolTipManager.sharedInstance().unregisterComponent(this);
        super.removeNotify();
    }
    
    
    /**
     * @see javax.swing.JComponent#getToolTipText()
     */
    @Override
	public String getToolTipText()
    {
        Runtime runtime = Runtime.getRuntime();
        int freeMemory  = (int) (runtime.freeMemory() / 1024 / 1024);
        int totalMemory = (int) (runtime.totalMemory() / 1024 / 1024);
        int usedMemory  = (totalMemory - freeMemory);
        return usedMemory + "M of " + totalMemory + "M";
    }

    
    /**
     * @see javax.swing.JComponent#getToolTipLocation(java.awt.event.MouseEvent)
     */
    @Override
	public Point getToolTipLocation(MouseEvent event)
    {
        return new Point(event.getX(), -20);
    }

    
    /**
     * @see javax.swing.JComponent#paintComponent(java.awt.Graphics)
     */
    @Override
	public void paintComponent(Graphics g)
    {
//        SwingUtil.makeAntiAliased(g, true);
        Insets insets = new Insets(0, 0, 0, 0);

        //
        // Calc memory stats
        //
        
        Runtime runtime = Runtime.getRuntime();
        int freeMemory = (int) (runtime.freeMemory() / 1024 / 1024);
        int totalMemory = (int) (runtime.totalMemory() / 1024 / 1024);
        int usedMemory = (totalMemory - freeMemory);

        int width = getWidth() - insets.left - insets.right;
        int height = getHeight() - insets.top - insets.bottom - 1;
        float fraction = ((float) usedMemory) / totalMemory;

        g.setColor(_progressBackground);

        //
        // Fill gradient
        //
        
        Graphics2D g2d = (Graphics2D) g;
        
        GradientPaint redtowhite =
            new GradientPaint(
                insets.left, 
                insets.top,
                _progressBackground,  
                width,
                insets.top,
                _progressForeground); 
                
        g2d.setPaint(redtowhite);
        
        g2d.fill(
            new RoundRectangle2D.Double(
                insets.left, 
                insets.top, 
                (int) (width * fraction), 
                height, 
                1, 
                1));

        //
        // Draw text
        //
        
        String str = usedMemory + "M of " + totalMemory + "M";
        FontRenderContext frc = new FontRenderContext(null, false, false);
        Rectangle2D bounds = g.getFont().getStringBounds(str, frc);
        Graphics g2 = g.create();
        
        g2.setClip(
            insets.left,
            insets.top,
            (int) (width * fraction),
            height);

        g2.setColor(Color.black);

        int x = insets.left + (int) (width - bounds.getWidth()) / 2;
        int y = (int) (height + insets.top + _lineMetrics.getAscent()) / 2;

        g2.drawString(str, x, y);
        g2.dispose();
        g2 = g.create();

        g2.setClip(
            insets.left + (int) (width * fraction),
            insets.top,
            getWidth() - insets.left - (int) (width * fraction),
            height);

        g2.setColor(getForeground());
        g2.drawString(str, x, y);
        g2.dispose();

//		int width = getIconWidth() - 1;
//		int height = getIconHeight() - 1;
//		g.setColor(plugin.getIconBorderColor());
		g.setColor(Color.BLUE);
		g.drawLine(x, y + 1, x, y + height - 1);
		g.drawLine(x + width, y + 1, x + width, y + height - 1);
		g.drawLine(x + 1, y, x + width - 1, y);
		g.drawLine(x + 1, y + height, x + width - 1, y + height);
//		g.setColor(plugin.getIconForeground());
		g.setColor(Color.BLACK);
//		long usedMem = plugin.getUsedMemory();
//		long totalMem = plugin.getTotalMemory();
		long usedMem = usedMemory;
		long totalMem = totalMemory;
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
    }

    //--------------------------------------------------------------------------
    // RefreshAction
    //--------------------------------------------------------------------------

    /** 
     * Refreshes are triggered by the Timer attached to this action.
     */
    class RefreshAction extends AbstractAction
    {
		private static final long serialVersionUID = 1L;

		/**
         * @see java.awt.event.ActionListener#actionPerformed(
         *      java.awt.event.ActionEvent)
         */
        @Override
		public void actionPerformed(ActionEvent evt)
        {
            repaint();
        }
    }
}