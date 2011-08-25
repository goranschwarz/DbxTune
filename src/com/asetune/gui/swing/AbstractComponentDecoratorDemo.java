/* Copyright (c) 2006-2007 Timothy Wall, All Rights Reserved
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * <p/>
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 */
package com.asetune.gui.swing;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.Scrollable;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;

public class AbstractComponentDecoratorDemo {
	static Timer timer = new Timer();

	static class WarningBadge extends AbstractComponentDecorator {
		private final int SIZE = 16;
		public WarningBadge(JTextField f) {
			super(f);
		}
		/** Position the decoration at the right edge of the text field. */
		public Rectangle getDecorationBounds() {
			Rectangle r = super.getDecorationBounds();
			r.x = getComponent().getWidth() - SIZE - 1;
			Insets insets = getComponent().getInsets();
			if (insets != null)
				r.x -= insets.right;
			r.y = (getComponent().getHeight() - SIZE) / 2;
			return r;
		}
		public void paint(Graphics graphics) {
			Rectangle r = getDecorationBounds();
			Graphics2D g = (Graphics2D)graphics;
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
							   RenderingHints.VALUE_ANTIALIAS_ON);
			GeneralPath triangle = new GeneralPath();
			triangle.moveTo(r.x + SIZE/2, r.y);
			triangle.lineTo(r.x + SIZE-1, r.y + SIZE-1);
			triangle.lineTo(r.x, r.y + SIZE-1);
			triangle.closePath();
			g.setColor(Color.yellow);
			g.fill(triangle);
			g.setColor(Color.black);
			g.draw(triangle);
			g.drawLine(r.x + SIZE/2, r.y + 4, r.x + SIZE/2, r.y + SIZE*3/4 - 2);
			g.drawLine(r.x + SIZE/2, r.y + SIZE*3/4+1, r.x + SIZE/2, r.y + SIZE - 4);
		}
	}

	static class Dimmer extends AbstractComponentDecorator {
		public Dimmer(JComponent target) {
			super(target);
		}
		public void paint(Graphics g) {
			g = g.create();
			((Graphics2D)g).setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, .75f));
			Color bg = getComponent().getBackground();
			g.setColor(bg);
			Rectangle r = getDecorationBounds();
			g.fillRect(r.x, r.y, r.width, r.height);
		}
	}

	private static final int SIZE = 1000;
	private static final int BLOCK = 20;
	private static final int LINE = 20;

	static class Labeler extends AbstractComponentDecorator {
		private Point location = new Point(0, 0);
		public Labeler(JComponent target) {
			super(target);
		}
		public void revalidate() {
			if (isVisible()) {
				Point location = getLocation();
				Dimension size = getPreferredSize();
				setDecorationBounds(location.x, location.y, size.width, size.height);
			}
		}
		public Point getLocation() { return location; }
		public void setLocation(int x, int y) {
			if (!new Point(x, y).equals(location)) {
				location = new Point(x, y);
				Dimension d = getPreferredSize();
				setDecorationBounds(x, y, d.width, d.height);
			}
		}
		public Dimension getPreferredSize() {
			return getComponent().getPreferredSize();
		}
		public Rectangle getDecorationBounds() {
			Rectangle r = new Rectangle(super.getDecorationBounds());
			Rectangle visible = getComponent().getVisibleRect();
			// adjust for horizontal scrolling
			if (r.x < visible.x)
				r.x = visible.x;
			return r;
		}
		public void paint(Graphics g) {
			Rectangle r = getDecorationBounds();
			for (int i=0;i < SIZE;i+= LINE) {
				g.drawString("label " + (i/LINE + 1),
							 r.x, r.y + i + g.getFontMetrics().getAscent() + 2);
			}
		}
	}

	static class Lines extends JComponent implements Scrollable {
		public Dimension getPreferredScrollableViewportSize() {
			return new Dimension(100, LINE*3);
		}
		public Dimension getPreferredSize() {
			return new Dimension(SIZE, SIZE);
		}
		public void paintComponent(Graphics g) {
			for (int i=0;i < SIZE;i+=LINE) {
				g.drawLine(0, i, SIZE-1, i);
				for (int dot=0;dot < SIZE;dot += BLOCK) {
					g.fillRect(i-2,dot-2,5,5);
				}
			}
		}
		public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
			return 1;
		}
		public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
			return LINE/3;
		}
		public boolean getScrollableTracksViewportWidth() {
			return false;
		}
		public boolean getScrollableTracksViewportHeight() {
			return false;
		}
	}

	static final class Background extends AbstractComponentDecorator {
		private float angle;
		public Background(JComponent target) {
			super(target, -1);
			timer.schedule(new TimerTask() {
				public void run() {
					angle += 2*Math.PI/90;
					repaint();
				}
			}, 0, 50);
		}
		public void paint(Graphics graphics) {
			Rectangle r = getDecorationBounds();
			Graphics2D g = (Graphics2D)graphics;
			float x1 = (float)(r.width/2 + Math.cos(angle) * 100);
			float x2 = (float)(r.width/2 + Math.cos(angle+Math.PI) * 100);
			float y1 = (float)(r.height/2 + Math.sin(angle) * 100);
			float y2 = (float)(r.height/2 + Math.sin(angle+Math.PI) * 100);
			Paint p = new GradientPaint(x1, y1, Color.green,
										x2, y2, Color.blue, true);
			g.setPaint(p);
			g.fillRect(r.x, r.y, r.width, r.height);
		}
	}

	static class Spotlight extends AbstractComponentDecorator {
		private Point where;
		private int size;
		private int delta = 1;
		private int dx=delta, dy=delta;
		public Spotlight(JComponent t, final int size) {
			super(t);
			where = new Point(t.getWidth()/2, t.getHeight()/2);
			this.size = size;
			timer.schedule(new TimerTask() {
				public void run() {
					if (where.x + size >= getComponent().getWidth())
						dx = -delta;
					else if (where.x < 0)
						dx = delta;
					if (where.y + size >= getComponent().getHeight())
						dy = -delta;
					else if (where.y < 0)
						dy = delta;
					where.x += dx;
					where.y += dy;
					repaint();
				}
			}, 0, 20);
		}
		public void paint(Graphics graphics) {
			Graphics2D g = (Graphics2D)graphics;
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
							   RenderingHints.VALUE_ANTIALIAS_ON);
			Rectangle r = getDecorationBounds();
			Color c = new Color(20, 20, 20, 196);
			g.setColor(c);
			Shape spot = new Ellipse2D.Float(r.x + where.x, r.y + where.y, size, size);
			Area area = new Area(r);
			area.subtract(new Area(spot));
			g.fill(area);
			g.setColor(new Color(255, 255, 0, 128));
			g.fill(spot);
		}
	}

	static class Marquee extends AbstractComponentDecorator implements MouseListener, MouseMotionListener {
		final int LINE_WIDTH = 4;
		private float phase = 0f;
		private Point origin = new Point(0, 0);
		public Marquee(JComponent target) {
			super(target, 100);
			setDecorationBounds(new Rectangle(0, 0, 0, 0));
			target.addMouseListener(this);
			target.addMouseMotionListener(this);
			// Make the ants march
			timer.schedule(new TimerTask() {
				public void run() {
					phase += 1.0f;
					repaint();
				}
			}, 0, 50);
		}
		public void paint(Graphics graphics) {
			Graphics2D g = (Graphics2D)graphics;
			//g.setColor(UIManager.getColor("Table.selectionBackground"));
			g.setColor(Color.red);
			Rectangle r = getDecorationBounds();
			g.setStroke(new BasicStroke(LINE_WIDTH, BasicStroke.CAP_BUTT,
										BasicStroke.JOIN_ROUND, 10.0f,
										new float[]{4.0f}, phase));
			g.drawRect(r.x, r.y, r.width, r.height);
		}
		public void mouseClicked(MouseEvent e) { }
		public void mousePressed(MouseEvent e) {
			setDecorationBounds(new Rectangle(e.getX()-LINE_WIDTH/2,
											  e.getY()-LINE_WIDTH/2, 0, 0));
			origin.setLocation(e.getX(), e.getY());
		}
		public void mouseReleased(MouseEvent e) {
			setDecorationBounds(new Rectangle(0, 0, 0, 0));
		}
		public void mouseEntered(MouseEvent e) { }
		public void mouseExited(MouseEvent e) { }
		public void mouseDragged(MouseEvent e) {
			int width = Math.abs(origin.x - e.getX());
			int height = Math.abs(origin.y - e.getY());
			int x = Math.min(e.getX(), origin.x);
			int y = Math.min(e.getY(), origin.y);
			setDecorationBounds(new Rectangle(x, y, width, height));
		}
		public void mouseMoved(MouseEvent e) { }
	}

	static class DraftWatermark extends AbstractComponentDecorator {
		public DraftWatermark(JComponent target) {
			super(target);
		}
		public void paint(Graphics graphics) {
			Rectangle r = getDecorationBounds();
			Graphics2D g = (Graphics2D)graphics;
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
							   RenderingHints.VALUE_ANTIALIAS_ON);
			Font f = g.getFont();
			g.setFont(f.deriveFont(Font.BOLD, f.getSize() * 2.0f));
			g.setColor(new Color(128,128,128,128));
			double theta = -Math.PI/6;
			g.rotate(theta);
			int dx = (int)Math.abs(r.height * Math.sin(theta));
			g.translate(dx, r.height);
			g.drawString("DRAFT", 0, r.height*3/4);
		}
	}

	static class GradientBackground extends AbstractComponentDecorator {
		public GradientBackground(JComponent c) {
			super(c, -1);
		}
		public void paint(Graphics graphics) {
			Graphics2D g = (Graphics2D)graphics;
			JComponent jc = getComponent();
			int h = jc.getHeight()/2;
			Color c = jc.getBackground().darker();
			GradientPaint gp =
				new GradientPaint(0, h, c, jc.getWidth()/2, h, jc.getBackground());
			g.setPaint(gp);
			Insets insets = jc.getInsets();
			g.fillRect(insets.left, insets.top, jc.getWidth()-insets.right, jc.getHeight()-insets.bottom);
		}
	}

	static class FocusHandler {
		private class FocusBorder extends AbstractComponentDecorator {
			final int WIDTH = 5;
			public FocusBorder(JComponent c) {
				super(c);
			}
			protected Rectangle getDecorationBounds() {
				Rectangle b = super.getDecorationBounds();
				b.x -= WIDTH;
				b.y -= WIDTH;
				b.width += WIDTH*2;
				b.height += WIDTH*2;
				return b;
			}
			public void paint(Graphics g) {
				Rectangle r = getDecorationBounds();
				g = g.create();
				g.setColor(new Color(0, 255, 0, 128));
				((Graphics2D)g).setStroke(new BasicStroke(WIDTH));
				g.drawRect(r.x, r.y, r.width-1, r.height-1);
				g.dispose();
			}
		}
		private FocusBorder border;
		public FocusHandler() {
			KeyboardFocusManager mgr = KeyboardFocusManager.getCurrentKeyboardFocusManager();
			mgr.addPropertyChangeListener("permanentFocusOwner", new PropertyChangeListener() {
				public void propertyChange(PropertyChangeEvent e) {
					if (border != null) {
						border.dispose();
					}
					if (e.getNewValue() instanceof JComponent) {
						border = new FocusBorder((JComponent)e.getNewValue());
					}
				}
			});
		}
	}

	private static JPanel createDecoratedPanel(int margin) {
		JPanel p = new JPanel(new BorderLayout());
		p.setBorder(BorderFactory.createEmptyBorder(margin,margin,margin,margin));
		JLabel banner = new JLabel("Decorator Demo");
		banner.setBorder(new CompoundBorder(new EmptyBorder(0,0,8,0), new LineBorder(Color.black)));
		Font f = banner.getFont();
		banner.setFont(f.deriveFont(Font.BOLD, f.getSize()*2));
		GradientBackground g = new GradientBackground(banner);
		p.add(banner, BorderLayout.NORTH);
		JPanel box = new JPanel(new GridLayout(0, 1));
		p.add(box, BorderLayout.SOUTH);

		JComponent lines = new Lines() {
			Labeler labeler = new Labeler(this);
			public void setBounds(Rectangle bounds) {
				super.setBounds(bounds);
				// Update labeler immediately to avoid unnecessary repaints
				// when the labeler bounds change
				labeler.revalidate();
				labeler.repaint();
			}
		};
		JScrollPane scroll = new JScrollPane(lines);
		scroll.setBorder(new TitledBorder("Partially-hovering labels"));
		p.add(scroll, BorderLayout.CENTER);

		JTextField tf = new JTextField("Badge decoration");
		tf.setBorder(new TitledBorder("Draw a badge icon with tooltip"));
		WarningBadge w = new WarningBadge(tf);
		w.setToolTipText("The tooltip is tied to the decoration");
		box.add(tf);

		JLabel draftLabel = new JLabel("Stamp Over");
		draftLabel.setBorder(new TitledBorder("Watermark"));
		DraftWatermark draft = new DraftWatermark(draftLabel);
		box.add(draftLabel);

		JLabel label = new JLabel("This component has been dimmed");
		label.setBorder(new TitledBorder("Dim the entire component"));
		Dimmer d = new Dimmer(label);
		box.add(label);

		JLabel selection = new JLabel("Click and drag to select");
		selection.setBorder(new TitledBorder("Marquee Selection"));
		Marquee band = new Marquee(selection);
		box.add(selection);

		JLabel bgLabel = new JLabel("This background is decorated");
		bgLabel.setBorder(new TitledBorder("Dynamic Background"));
		Background bg = new Background(bgLabel);
		box.add(bgLabel);

		JLabel spot = new JLabel("A common screensaver theme");
		spot.setBorder(new TitledBorder("Dynamic Decoration"));
		Spotlight s = new Spotlight(spot, 20);
		box.add(spot);

		return p;
	}

	public static void main(String[] args) {
		JFrame frame = new JFrame("Decorator Demo");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		final int MARGIN = 10;
		JPanel p = createDecoratedPanel(MARGIN);

		boolean asTab = false;
		if (asTab) {
			JTabbedPane tp = new JTabbedPane();
			tp.add("blank", new JLabel("blank"));
			tp.add("decorators", p);
			tp.add("scrolled", new JScrollPane(createDecoratedPanel(MARGIN)));

			frame.getContentPane().add(tp);
		}
		else {
			frame.getContentPane().add(p);
		}
		frame.pack();
		frame.setVisible(true);
	}
}
