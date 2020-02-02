package info.iskariot.pingger.java.streamOverlayHelper.util;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.IllegalComponentStateException;
import java.awt.Insets;
import java.awt.MouseInfo;
import java.awt.Panel;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

/**
 * A fancy color Selector
 *
 * @author Pingger
 *
 */
public class ColorSelector extends Window
{
	private static final long									serialVersionUID	= -1140740026888928641L;
	/** The consumer, that is called on color change */
	private final Consumer<Color>								cc;
	/** the primary color display */
	private ColorDisplay										colorDisplay;
	/**
	 * the VisualColorSelectors for each enabled ColorComponent are stored in this
	 */
	private final HashMap<ColorComponent, VisualColorSelector>	componentSelectors;
	/** the current color */
	private Color												currentColor;
	/**
	 * <code>true</code> when the alpha selector should be enabled,
	 * <code>false</code> otherwise
	 */
	private final boolean										hasAlpha;
	/** used to synchronize 'onChange' operations and prevent bad recursion */
	private transient AtomicBoolean								inChange			= new AtomicBoolean(false);
	/** contains the sliders for each enabled ColorComponent */
	private final HashMap<ColorComponent, JSlider>				sliders;
	/** contains the spinners for each enabled ColorComponent */
	private final HashMap<ColorComponent, JSpinner>				spinners;

	/**
	 * Creates a new {@link ColorSelector} with the given {@link Consumer} initial
	 * {@link Color} and Alpha-Value-Switch.
	 *
	 * @param cc
	 *            the Consumer to which color-Changes are notified
	 * @param initialColor
	 *            the initialColor
	 * @param alpha
	 *            <code>true</code> if alpha should be editable, <code>false</code>
	 *            otherwise
	 * @param window
	 *            the Parent Window
	 */
	public ColorSelector(Consumer<Color> cc, Color initialColor, boolean alpha, Window window)
	{
		super(window);
		if (cc == null || initialColor == null)
		{ throw new IllegalArgumentException("null values are not allowed"); }

		this.cc = cc;
		this.currentColor = initialColor;
		this.hasAlpha = alpha;
		// Create Maps
		sliders = new HashMap<>();
		spinners = new HashMap<>();
		componentSelectors = new HashMap<>();
		// Setup UI
		setupUI();
		onChange();
		pack();
		// Setup Window
		setAlwaysOnTop(true);
		addWindowFocusListener(new WFL());
		addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowOpened(WindowEvent e)
			{
				pack();
				System.out.println("Show!");
				setLocation(MouseInfo.getPointerInfo().getLocation());
				setVisible(true);
				setVisible(true);
			}
		});
	}

	/**
	 * @return the currently selected Color
	 */
	public synchronized Color getColor()
	{
		return currentColor;
	}

	/**
	 * Sets a new {@link Color}, WHICH IS NOTIFIED TO THE {@link Consumer}
	 *
	 * @param newColor
	 *            the new {@link Color} to set
	 */
	public synchronized void setColor(Color newColor)
	{
		currentColor = newColor;
		onChange();
	}

	/**
	 * Returns the components value in a way to make it easy for the
	 * awt/swing-components to use them
	 *
	 * @param c
	 *            the {@link ColorComponent} to retrieve
	 * @return the (possibly formatted) value of the Component of the Color
	 */
	private Number getComponentValue(ColorComponent c)
	{
		switch (c)
		{
			case ALPHA:
				return (float) currentColor.getAlpha();

			case BLUE:
				return (float) currentColor.getBlue();

			case GREEN:
				return (float) currentColor.getGreen();

			case RED:
				return (float) currentColor.getRed();

			case BRIGHTNESS:
				return Math
						.round(Color.RGBtoHSB(currentColor.getRed(), currentColor.getGreen(), currentColor.getBlue(), null)[2]
								* ColorComponent.BRIGHTNESS.upperBound.floatValue() * 100
						) / 100.0f;

			case HUE:
				return Math
						.round(Color.RGBtoHSB(currentColor.getRed(), currentColor.getGreen(), currentColor.getBlue(), null)[0]
								* ColorComponent.HUE.upperBound.floatValue() * 100
						) / 100.0f;

			case SATURATION:
				return Math
						.round(Color.RGBtoHSB(currentColor.getRed(), currentColor.getGreen(), currentColor.getBlue(), null)[1]
								* ColorComponent.SATURATION.upperBound.floatValue() * 100
						) / 100.0f;

			default:
				return 0;
		}
	}

	/**
	 * Updates the UI with the currently selected {@link Color}
	 */
	private synchronized void onChange()
	{
		// When already in change -> skip
		if (inChange.get())
		{ return; }
		// Now in change
		inChange.set(true);
		try
		{
			// Send new color to consumer
			cc.accept(currentColor);
			// Update the Sliders, Spinners and Visual Selectors for each component
			for (ColorComponent c : ColorComponent.values())
			{
				if (!sliders.containsKey(c))
				{
					continue;
				}
				sliders.get(c).setValue(getComponentValue(c).intValue());
				spinners.get(c).setValue(getComponentValue(c));
				componentSelectors.get(c).repaint();
			}
			// Update the colorDisplay
			colorDisplay.repaint();
		}
		finally
		{
			// done with change
			inChange.set(false);
		}
	}

	/**
	 * Sets a specific ColorComponent.
	 *
	 * @param c
	 *            the {@link ColorComponent} to change
	 * @param number
	 *            the value to set
	 */
	private synchronized void setComponent(ColorComponent c, Number number)
	{
		// prevent component changing, while already in change
		if (inChange.get())
		{ return; }
		int alpha;
		float[] hsb;
		Color t;
		// Based on RGB/HSB-Type, some conversion needs to be performed
		switch (c)
		{
			case ALPHA:
				if (currentColor.getAlpha() == number.intValue())
				{ return; }
				currentColor = new Color(currentColor.getRed(), currentColor.getGreen(), currentColor.getBlue(), number.intValue());
				break;

			case BLUE:
				if (currentColor.getBlue() == number.intValue())
				{ return; }
				currentColor = new Color(currentColor.getRed(), currentColor.getGreen(), number.intValue(), currentColor.getAlpha());
				break;

			case GREEN:
				if (currentColor.getGreen() == number.intValue())
				{ return; }
				currentColor = new Color(currentColor.getRed(), number.intValue(), currentColor.getBlue(), currentColor.getAlpha());
				break;

			case RED:
				if (currentColor.getRed() == number.intValue())
				{ return; }
				currentColor = new Color(number.intValue(), currentColor.getGreen(), currentColor.getBlue(), currentColor.getAlpha());
				break;

			case BRIGHTNESS:
				hsb = Color.RGBtoHSB(currentColor.getRed(), currentColor.getGreen(), currentColor.getBlue(), null);
				if (hsb[2] == number.floatValue() / ColorComponent.BRIGHTNESS.upperBound.floatValue())
				{ return; }
				hsb[2] = number.floatValue() / ColorComponent.BRIGHTNESS.upperBound.floatValue();
				alpha = currentColor.getAlpha();
				t = Color.getHSBColor(hsb[0], hsb[1], hsb[2]);
				currentColor = new Color(t.getRed(), t.getGreen(), t.getBlue(), alpha);
				break;

			case HUE:
				hsb = Color.RGBtoHSB(currentColor.getRed(), currentColor.getGreen(), currentColor.getBlue(), null);
				if (hsb[0] == number.floatValue() / ColorComponent.HUE.upperBound.floatValue())
				{ return; }
				hsb[0] = number.floatValue() / ColorComponent.HUE.upperBound.floatValue();
				alpha = currentColor.getAlpha();
				t = Color.getHSBColor(hsb[0], hsb[1], hsb[2]);
				currentColor = new Color(t.getRed(), t.getGreen(), t.getBlue(), alpha);
				break;

			case SATURATION:
				hsb = Color.RGBtoHSB(currentColor.getRed(), currentColor.getGreen(), currentColor.getBlue(), null);
				if (hsb[1] == number.floatValue() / ColorComponent.SATURATION.upperBound.floatValue())
				{ return; }
				hsb[1] = number.floatValue() / ColorComponent.SATURATION.upperBound.floatValue();
				alpha = currentColor.getAlpha();
				t = Color.getHSBColor(hsb[0], hsb[1], hsb[2]);
				currentColor = new Color(t.getRed(), t.getGreen(), t.getBlue(), alpha);
				break;

			default:
				break;

		}
		// fire change event!
		onChange();
	}

	/**
	 * Setup the user interface
	 */
	private void setupUI()
	{
		/** Define rudimentary layout */
		Panel main = new Panel(new BorderLayout(8, 8));
		Panel top = new Panel(new GridBagLayout());
		Panel bottom = new Panel(new GridLayout(1, 2, 8, 8));
		main.add(top, BorderLayout.NORTH);
		main.add(bottom, BorderLayout.CENTER);
		// Create VisualSelector Panels
		JPanel colorSelectors = new JPanel(new GridLayout(0, 1, 4, 4));
		JPanel hsbSelectors = new JPanel(new GridLayout(0, 1, 4, 4));
		int row = 0;
		GridBagConstraints gbc = new GridBagConstraints(
				0, row, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(8, 8, 8, 8), 0, 0
		);
		/*
		 * For each ColorComponent in a specific order:
		 * - Create the Slider,Spinner-Combo at the top
		 * - Create the VisualSelector at the Bottom
		 * - set the correct listeners
		 */
		for (ColorComponent c : new ColorComponent[] {
				ColorComponent.RED, ColorComponent.GREEN, ColorComponent.BLUE, ColorComponent.ALPHA, ColorComponent.HUE, ColorComponent.SATURATION,
				ColorComponent.BRIGHTNESS
		})
		{
			gbc.gridy = row;
			// When Alpha is disabled -> skip creating the components
			if (c == ColorComponent.ALPHA && !hasAlpha)
			{
				continue;
			}
			JSlider jsl = new JSlider(c.lowerBound.intValue(), c.upperBound.intValue(), getComponentValue(c).intValue());
			jsl.addChangeListener(e -> setComponent(c, jsl.getValue()));
			// Those dang casts are necessary
			SpinnerNumberModel snm = new SpinnerNumberModel(
					getComponentValue(c), (Comparable<?>) c.lowerBound, (Comparable<?>) c.upperBound, (Number) (c.flt ? 0.1 : 1)
			);
			snm.addChangeListener(e -> setComponent(c, snm.getNumber()));
			JSpinner jsp = new JSpinner(snm);
			VisualColorSelector ccs = new VisualColorSelector(c);
			switch (c)
			{
				case ALPHA:
				case BLUE:
				case GREEN:
				case RED:
					colorSelectors.add(ccs);
					break;

				case BRIGHTNESS:
				case HUE:
				case SATURATION:
					hsbSelectors.add(ccs);
					break;

				default:
					break;
			}
			// Add to Maps
			sliders.put(c, jsl);
			spinners.put(c, jsp);
			componentSelectors.put(c, ccs);
			// Add to UI
			gbc.gridx = 0;
			top.add(new JLabel(c.name()), gbc);
			gbc.gridx = 1;
			top.add(jsl, gbc);
			gbc.gridx = 2;
			top.add(jsp, gbc);

			row++;
		}
		// Add Color Display
		gbc.gridx = 0;
		gbc.gridy = row;
		gbc.gridwidth = 3;
		top.add(colorDisplay = new ColorDisplay(), gbc);
		// Finalize adding the component Selectors
		bottom.add(colorSelectors);
		bottom.add(hsbSelectors);
		// Add the Main Panel to the Window
		add(main);
		// pack(); is done in the constructor
	}

	private enum ColorComponent
	{
		/** Alpha RGBA Component */
		ALPHA(0.0, 255.0, false),
		/** Blue RGBA Component */
		BLUE(0.0, 255.0, false),
		/** Brighness HSB Component */
		BRIGHTNESS(0.0, 100.0, true),
		/** Green RGBA Component */
		GREEN(0.0, 255.0, false),
		/** HUE HSB Component */
		HUE(0.0, 360.0, false),
		/** Red RGBA Component */
		RED(0.0, 255.0, false),
		/** Saturation HSB Component */
		SATURATION(0.0, 100.0, true);

		/** defines if the spinner increments by 1 or 0.1 */
		public final boolean	flt;
		/** lower Bound for Slider and Spinner (also for VisualComponentSelector) */
		public final Number		lowerBound;
		/** upper Bound for Slider and Spinner (also for VisualComponentSelector) */
		public final Number		upperBound;

		/**
		 * Specifies a Color Component
		 *
		 * @param lb
		 *            lower Bound for value
		 * @param ub
		 *            upper Bound for value
		 * @param fltValue
		 *            float?
		 */
		ColorComponent(double lb, double ub, boolean fltValue)
		{
			// casts prevent later issues
			lowerBound = (float) lb;
			upperBound = (float) ub;
			flt = fltValue;
		}
	}

	/**
	 * Just displays a plain Color, possibly with a "transparency background"
	 *
	 * @author Pingger
	 *
	 */
	private class ColorDisplay extends JPanel
	{
		private static final long serialVersionUID = 8326031548169038381L;

		/**
		 * Creates a new {@link ColorDisplay}
		 */
		public ColorDisplay()
		{
			setDoubleBuffered(true);
			setPreferredSize(new Dimension(0, 64));
		}

		@Override
		public void paint(Graphics g)
		{
			Point p;
			try
			{
				p = getLocationOnScreen();
			}
			catch (IllegalComponentStateException icsexc)
			{
				return;// Not visible on screen -> no need to draw ...
			}
			g.setColor(Color.WHITE);
			g.clearRect(0, 0, getWidth(), getHeight());
			/**
			 * Transparency Background
			 */
			if (currentColor.getAlpha() != 255)
			{
				g.setColor(Color.LIGHT_GRAY);
				/*
				 * Align "transparency grid" to screen, prevents weird looking behaviour when
				 * multiple components are stacked close to each other
				 */
				for (int x = -(p.x % 16); x <= getWidth(); x += 16)
				{
					for (int y = -(p.y % 16); y <= getHeight(); y += 16)
					{
						if ((p.x + p.y + x + y) % 32 <= 15)
						{
							continue;
						}
						g.fillRect(x, y, 16, 16);
					}
				}
			}
			/**
			 * The actual color
			 */
			g.setColor(currentColor);
			g.fillRect(0, 0, getWidth(), getHeight());
		}
	}

	/**
	 * A visual color Selector, that previews what color is selected at the what
	 * value of the given {@link ColorComponent} and allows easy selecting using the
	 * mouse
	 *
	 * @author Pingger
	 *
	 */
	private class VisualColorSelector extends JPanel implements MouseListener, MouseMotionListener
	{
		private static final long		serialVersionUID	= -8614539825055684634L;
		private final ColorComponent	c;

		public VisualColorSelector(ColorComponent c)
		{
			this.c = c;
			// Makes the UI look so much better, when dragging a slider or VisualColorSelector
			setDoubleBuffered(true);
			addMouseListener(this);
			addMouseMotionListener(this);
			setPreferredSize(new Dimension(0, 32));
		}

		@Override
		public void mouseClicked(MouseEvent e)
		{
			mouseEvent(e.getX(), e.getY());
		}

		@Override
		public void mouseDragged(MouseEvent e)
		{
			mouseEvent(e.getX(), e.getY());
		}

		@Override
		public void mouseEntered(MouseEvent e)
		{
			// ignore
		}

		@Override
		public void mouseExited(MouseEvent e)
		{
			// ignore
		}

		@Override
		public void mouseMoved(MouseEvent e)
		{
			// ignore
		}

		@Override
		public void mousePressed(MouseEvent e)
		{
			mouseEvent(e.getX(), e.getY());
		}

		@Override
		public void mouseReleased(MouseEvent e)
		{
			//ignore
		}

		@Override
		public void paint(Graphics g)
		{
			Point p;
			try
			{
				p = getLocationOnScreen();
			}
			catch (IllegalComponentStateException icsexc)
			{
				return;// Not visible on screen -> no need to draw ...
			}
			g.setColor(Color.WHITE);
			g.clearRect(0, 0, getWidth(), getHeight());
			if (currentColor.getAlpha() != 255 || c == ColorComponent.ALPHA)
			{
				g.setColor(Color.LIGHT_GRAY);
				/*
				 * Align "transparency grid" to screen, prevents weird looking behaviour when
				 * multiple components are stacked close to each other
				 */
				for (int x = -(p.x % 16); x <= getWidth(); x += 16)
				{
					for (int y = -(p.y % 16); y <= getHeight(); y += 16)
					{
						if ((p.x + p.y + x + y) % 32 <= 15)
						{
							continue;
						}
						g.fillRect(x, y, 16, 16);
					}
				}
			}
			switch (c)
			{
				case ALPHA:
					paintRGBA(0, 0, getWidth(), getHeight(), g, 3);
					break;

				case BLUE:
					paintRGBA(0, 0, getWidth(), getHeight(), g, 2);
					break;

				case BRIGHTNESS:
					paintHSB(0, 0, getWidth(), getHeight(), g, 2);
					break;

				case GREEN:
					paintRGBA(0, 0, getWidth(), getHeight(), g, 1);
					break;

				case HUE:
					paintHSB(0, 0, getWidth(), getHeight(), g, 0);
					break;

				case RED:
					paintRGBA(0, 0, getWidth(), getHeight(), g, 0);
					break;

				case SATURATION:
					paintHSB(0, 0, getWidth(), getHeight(), g, 1);
					break;

				default:
					break;
			}
		}

		/**
		 * The logic behing the MouseEvents.
		 *
		 * @param x
		 *            x location relative to component
		 * @param y
		 *            y location relative to component
		 */
		private void mouseEvent(int x, int y)
		{
			// Allows dragging outside the Component, but still performing the expected Behaviour
			x = Math.min(getWidth(), Math.max(0, x));
			y = Math.min(getHeight(), Math.max(0, y));
			setComponent(c, (float) (1.0 * x / getWidth() * c.upperBound.floatValue()));
		}

		/**
		 * Paints a HSB gradient
		 *
		 * @param X
		 *            the start X
		 * @param Y
		 *            the start Y
		 * @param W
		 *            the width
		 * @param H
		 *            the height
		 * @param g
		 *            the graphics to paint to
		 * @param component
		 *            the component to paint (0 Hue, 1 Saturation, 2 Brightness)
		 */
		private void paintHSB(int X, int Y, int W, int H, Graphics g, int component)
		{
			int a = currentColor.getAlpha();
			for (int x = X; x < W; x++)
			{
				float[] hsb = Color.RGBtoHSB(currentColor.getRed(), currentColor.getGreen(), currentColor.getBlue(), null);
				hsb[component] = (float) (1.0 * x / W);
				Color t = Color.getHSBColor(hsb[0], hsb[1], hsb[2]);
				g.setColor(new Color(t.getRed(), t.getGreen(), t.getBlue(), a));
				g.fillRect(x, Y, 1, H);
			}
		}

		/**
		 * Paints a RGBA gradient
		 *
		 * @param X
		 *            the start X
		 * @param Y
		 *            the start Y
		 * @param W
		 *            the width
		 * @param H
		 *            the height
		 * @param gfx
		 *            the graphics to paint to
		 * @param component
		 *            the component to paint (0 Red, 1 Green, 2 Blue, 3 Alpha)
		 */
		private void paintRGBA(int X, int Y, int W, int H, Graphics gfx, int component)
		{
			int[] rgba = new int[] {
					currentColor.getRed(), currentColor.getGreen(), currentColor.getBlue(), currentColor.getAlpha()
			};
			for (int x = X; x < W; x++)
			{
				rgba[component] = (int) (255.0 * x / W);
				gfx.setColor(new Color(rgba[0], rgba[1], rgba[2], rgba[3]));
				gfx.fillRect(x, Y, 1, H);
			}
		}
	}

	/**
	 * The Window focus Listener causing the window to be hidden upon losing focus
	 *
	 * @author Pingger
	 *
	 */
	private class WFL implements WindowFocusListener
	{

		@Override
		public void windowGainedFocus(WindowEvent e)
		{
			System.out.println("Focus");
		}

		@Override
		public void windowLostFocus(WindowEvent e)
		{
			System.out.println("No Focus");
			setVisible(false);
		}
	}
}
